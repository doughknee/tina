package com.tina.app.ask

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.ai.AiChat
import com.tina.app.ai.AskAction
import com.tina.app.ai.ChatMessage
import com.tina.app.ai.ChatRole
import com.tina.app.ai.ImproveField
import com.tina.app.ai.ImprovePatch
import com.tina.app.ai.ReasoningLevel
import com.tina.app.ai.applyImprovePatch
import com.tina.app.ai.buildAskContext
import com.tina.app.ai.buildAskSystemPrompt
import com.tina.app.ai.extractAskActions
import com.tina.app.ai.MAX_ASK_ACTIONS
import com.tina.app.ai.needsConfirmation
import com.tina.app.capture.ParsedCapture
import com.tina.app.data.ChatDao
import com.tina.app.data.ChatEntity
import com.tina.app.data.ChatMessageEntity
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import com.tina.app.data.SettingsRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val ROLE_USER = "user"
private const val ROLE_ASSISTANT = "assistant"

private sealed interface UndoStep {
    /** Put a prior version back (re-inserting if the action deleted it). */
    data class Restore(val item: Item, val wasDeleted: Boolean) : UndoStep

    /** Remove an item the action created. */
    data class Remove(val id: Long) : UndoStep
}

class AskViewModel(
    private val repository: ItemRepository,
    private val chat: AiChat,
    private val settingsRepository: SettingsRepository,
    private val chatDao: ChatDao,
) : ViewModel() {
    val messages = mutableStateListOf<ChatMessage>()
    val history = chatDao.observeChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var currentChatId by mutableStateOf<Long?>(null)
        private set
    var sending by mutableStateOf(false)
        private set
    /** Why the last question failed, or null; the screen turns it into a sentence. */
    var lastError by mutableStateOf<com.tina.app.ai.AiError?>(null)
        private set
    var reasoning by mutableStateOf(ReasoningLevel.BALANCED)
        private set

    /** A batch that needs a tap before it runs (deletions, or more than a few changes). */
    var pendingActions by mutableStateOf<List<AskAction>>(emptyList())
        private set
    private var pendingReminder = 0

    /** Bumps once per applied batch; the screen shows the undo snackbar off it. */
    var appliedNonce by mutableStateOf(0)
        private set
    var appliedCount by mutableStateOf(0)
        private set

    /** Bumps when a chat is deleted; the screen offers undo. */
    var chatDeletedNonce by mutableStateOf(0)
        private set

    private var modelOverride: String? = null
    private var undoBatch: List<UndoStep> = emptyList()
    private var deletedChat: Pair<ChatEntity, List<ChatMessageEntity>>? = null

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    fun effectiveModel(settingsModel: String): String = modelOverride ?: settingsModel

    fun setModelOverride(model: String) {
        modelOverride = model
        persistChatMeta()
    }

    fun setReasoningLevel(level: ReasoningLevel) {
        reasoning = level
        persistChatMeta()
    }

    fun setWriteEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAiAskWriteEnabled(enabled) }
    }

    fun newChat() {
        currentChatId = null
        messages.clear()
        lastError = null
    }

    fun openChat(id: Long) {
        viewModelScope.launch {
            val entity = chatDao.chat(id) ?: return@launch
            currentChatId = id
            reasoning = ReasoningLevel.entries.firstOrNull { it.name == entity.reasoning }
                ?: ReasoningLevel.BALANCED
            modelOverride = entity.model
            lastError = null
            messages.clear()
            messages += chatDao.messages(id).map {
                ChatMessage(
                    if (it.role == ROLE_USER) ChatRole.USER else ChatRole.ASSISTANT,
                    it.content,
                )
            }
        }
    }

    fun deleteChat(id: Long) {
        viewModelScope.launch {
            val entity = chatDao.chat(id) ?: return@launch
            deletedChat = entity to chatDao.messages(id)
            chatDao.deleteMessages(id)
            chatDao.deleteChat(id)
            if (currentChatId == id) newChat()
            chatDeletedNonce++
        }
    }

    fun undoDeleteChat() {
        val (entity, entityMessages) = deletedChat ?: return
        deletedChat = null
        viewModelScope.launch {
            chatDao.insertChat(entity)
            entityMessages.forEach { chatDao.insertMessage(it) }
        }
    }

    fun send(text: String) {
        // a second send while one is in flight would land in the transcript with no reply
        if (sending) return
        messages += ChatMessage(ChatRole.USER, text)
        viewModelScope.launch {
            val id = ensureChat(text)
            chatDao.insertMessage(
                ChatMessageEntity(chatId = id, role = ROLE_USER, content = text, createdAt = now()),
            )
            askInternal()
        }
    }

    fun retry() {
        viewModelScope.launch { askInternal() }
    }

    fun applyPending() {
        val batch = pendingActions
        pendingActions = emptyList()
        viewModelScope.launch { applyActions(batch, TimeZone.currentSystemDefault(), pendingReminder) }
    }

    fun dismissPending() {
        pendingActions = emptyList()
    }

    fun undoLastBatch() {
        val batch = undoBatch
        undoBatch = emptyList()
        viewModelScope.launch {
            // last change first, so two edits to one item land back on the original
            batch.asReversed().forEach { step ->
                when (step) {
                    is UndoStep.Restore ->
                        if (step.wasDeleted) repository.restore(step.item)
                        else repository.update(step.item)
                    is UndoStep.Remove -> repository.delete(step.id)
                }
            }
        }
    }

    private suspend fun ensureChat(firstText: String): Long {
        currentChatId?.let { return it }
        val id = chatDao.insertChat(
            ChatEntity(
                title = firstText.trim().take(48),
                model = modelOverride,
                reasoning = reasoning.name,
                createdAt = now(),
                updatedAt = now(),
            ),
        )
        currentChatId = id
        return id
    }

    private fun persistChatMeta() {
        val id = currentChatId ?: return
        viewModelScope.launch {
            chatDao.chat(id)?.let {
                chatDao.updateChat(it.copy(model = modelOverride, reasoning = reasoning.name))
            }
        }
    }

    private suspend fun askInternal() {
        if (sending) return
        sending = true
        lastError = null
        val settings = settingsRepository.settings.first()
        val tz = TimeZone.currentSystemDefault()
        val nowLocal = Clock.System.now().toLocalDateTime(tz)
        val writeEnabled = settings.aiAskWriteEnabled
        // fresh context per question so answers always reflect current data
        val context = buildAskContext(repository.allItems(), tz)
        val system = buildAskSystemPrompt(context, nowLocal, reasoning, writeEnabled)
        val reply = try {
            chat.chat(system, messages.toList(), modelOverride)
        } catch (e: com.tina.app.ai.AiException) {
            null.also { lastError = e.error }
        }
        if (reply == null) {
            // lastError is set above
        } else {
            val (visible, rawActions) = if (writeEnabled) {
                extractAskActions(reply)
            } else {
                reply.trim() to emptyList()
            }
            val actions = rawActions.take(MAX_ASK_ACTIONS)
            val shown = when {
                visible.isNotBlank() -> visible
                // model sent only actions: the applied-changes snackbar is the reply
                actions.isEmpty() -> reply.trim()
                else -> null
            }
            shown?.let { messages += ChatMessage(ChatRole.ASSISTANT, it) }
            currentChatId?.let { id ->
                shown?.let {
                    chatDao.insertMessage(
                        ChatMessageEntity(chatId = id, role = ROLE_ASSISTANT, content = it, createdAt = now()),
                    )
                }
                chatDao.chat(id)?.let { chatDao.updateChat(it.copy(updatedAt = now())) }
            }
            if (actions.isNotEmpty()) {
                if (needsConfirmation(actions)) {
                    pendingReminder = settings.defaultReminderMinutes
                    pendingActions = actions
                } else {
                    applyActions(actions, tz, settings.defaultReminderMinutes)
                }
            }
        }
        sending = false
    }

    private suspend fun applyActions(actions: List<AskAction>, tz: TimeZone, defaultReminder: Int) {
        val steps = mutableListOf<UndoStep>()
        var applied = 0
        actions.forEach { action ->
            if (action.op == "create") {
                val title = action.title?.trim().orEmpty()
                if (title.isNotEmpty()) {
                    val parsed = ParsedCapture(
                        title = title,
                        type = ItemType.entries.firstOrNull { it.name.equals(action.type, true) }
                            ?: ItemType.TASK,
                        date = action.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                        time = action.time?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
                        durationMinutes = action.durationMinutes?.takeIf { it > 0 },
                    )
                    val id = repository.capture(parsed, tz, defaultReminder)
                    steps += UndoStep.Remove(id)
                    applied++
                }
                return@forEach
            }
            val item = action.id?.let { repository.get(it) } ?: return@forEach
            when (action.op) {
                "complete" -> {
                    steps += UndoStep.Restore(item, wasDeleted = false)
                    repository.complete(item.id)
                    applied++
                }
                "uncomplete" -> {
                    steps += UndoStep.Restore(item, wasDeleted = false)
                    repository.uncomplete(item.id)
                    applied++
                }
                "delete" -> {
                    steps += UndoStep.Restore(item, wasDeleted = true)
                    repository.delete(item.id)
                    applied++
                }
                "rename" -> action.title?.trim()?.takeIf { it.isNotEmpty() }?.let { title ->
                    steps += UndoStep.Restore(item, wasDeleted = false)
                    repository.update(item.copy(title = title))
                    applied++
                }
                "set_priority" -> Priority.entries
                    .firstOrNull { it.name.equals(action.priority, true) }?.let { priority ->
                        steps += UndoStep.Restore(item, wasDeleted = false)
                        repository.update(item.copy(priority = priority))
                        applied++
                    }
                "set_tags" -> action.tags?.let { raw ->
                    val tags = raw.map { it.trim().lowercase().removePrefix("#") }
                        .filter { it.isNotEmpty() }
                    steps += UndoStep.Restore(item, wasDeleted = false)
                    repository.update(item.copy(tags = tags))
                    applied++
                }
                "reschedule" -> {
                    val date = action.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    val time = action.time?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                    // a date the model garbled must not turn into "clear the schedule"
                    if (action.date != null && date == null) return@forEach
                    steps += UndoStep.Restore(item, wasDeleted = false)
                    val updated = if (date == null) {
                        item.copy(dueDate = null, dueTime = null, startAt = null, endAt = null)
                    } else {
                        applyImprovePatch(
                            item,
                            ImprovePatch(date = date, time = time),
                            buildSet {
                                add(ImproveField.DATE)
                                if (time != null) add(ImproveField.TIME)
                            },
                            tz,
                        )
                    }
                    repository.update(updated)
                    applied++
                }
            }
        }
        if (applied > 0) {
            undoBatch = steps
            appliedCount = applied
            appliedNonce++
        }
    }
}
