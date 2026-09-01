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
import com.tina.app.capture.ParsedCapture
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import com.tina.app.data.SettingsRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
) : ViewModel() {
    val messages = mutableStateListOf<ChatMessage>()
    var sending by mutableStateOf(false)
        private set
    var lastFailed by mutableStateOf(false)
        private set
    var reasoning by mutableStateOf(ReasoningLevel.BALANCED)
        private set

    /** Bumps once per applied batch; the screen shows the undo snackbar off it. */
    var appliedNonce by mutableStateOf(0)
        private set
    var appliedCount by mutableStateOf(0)
        private set

    private var modelOverride: String? = null
    private var undoBatch: List<UndoStep> = emptyList()

    fun effectiveModel(settingsModel: String): String = modelOverride ?: settingsModel

    fun setModelOverride(model: String) {
        modelOverride = model
    }

    fun setReasoningLevel(level: ReasoningLevel) {
        reasoning = level
    }

    fun send(text: String) {
        messages += ChatMessage(ChatRole.USER, text)
        ask()
    }

    fun retry() = ask()

    fun clear() {
        messages.clear()
        lastFailed = false
    }

    fun undoLastBatch() {
        val batch = undoBatch
        undoBatch = emptyList()
        viewModelScope.launch {
            batch.forEach { step ->
                when (step) {
                    is UndoStep.Restore ->
                        if (step.wasDeleted) repository.restore(step.item)
                        else repository.update(step.item)
                    is UndoStep.Remove -> repository.delete(step.id)
                }
            }
        }
    }

    private fun ask() {
        if (sending) return
        sending = true
        lastFailed = false
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val tz = TimeZone.currentSystemDefault()
            val now = Clock.System.now().toLocalDateTime(tz)
            val writeEnabled = settings.aiAskWriteEnabled
            // fresh context per question so answers always reflect current data
            val context = buildAskContext(repository.allItems(), tz)
            val system = buildAskSystemPrompt(context, now, reasoning, writeEnabled)
            val reply = chat.chat(system, messages.toList(), modelOverride)
            if (reply == null) {
                lastFailed = true
            } else if (writeEnabled) {
                val (text, actions) = extractAskActions(reply)
                when {
                    text.isNotBlank() -> messages += ChatMessage(ChatRole.ASSISTANT, text)
                    // model sent only actions: the applied-changes snackbar is the reply
                    actions.isEmpty() -> messages += ChatMessage(ChatRole.ASSISTANT, reply.trim())
                }
                if (actions.isNotEmpty()) applyActions(actions, tz, settings.defaultReminderMinutes)
            } else {
                messages += ChatMessage(ChatRole.ASSISTANT, reply.trim())
            }
            sending = false
        }
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
                    steps += UndoStep.Restore(item, wasDeleted = false)
                    val date = action.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    val time = action.time?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
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
