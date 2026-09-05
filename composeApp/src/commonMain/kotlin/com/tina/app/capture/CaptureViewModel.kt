package com.tina.app.capture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import com.tina.app.data.Settings
import com.tina.app.data.SettingsRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class ChipKind { DATE, TIME, DURATION, PRIORITY, RECURRENCE }

data class Starters(val titles: List<String> = emptyList(), val tags: List<String> = emptyList())

class CaptureViewModel(
    private val repository: ItemRepository,
    private val settingsRepository: SettingsRepository,
    private val refiner: com.tina.app.ai.CaptureRefiner,
    private val review: com.tina.app.ui.ReviewPrompter = com.tina.app.ui.NoReviewPrompter,
) : ViewModel() {
    private val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** Emits the pre-refinement item whenever the AI upgraded a capture (for the undo snackbar). */
    val refinedEvents = kotlinx.coroutines.flow.MutableSharedFlow<com.tina.app.data.Item>(extraBufferCapacity = 4)

    /** Last three captures, newest first — shown while the field is empty. */
    val recent = repository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * One-tap starters from the user's own history: titles captured more than once (minus
     * what is already in recents) and the most-used tags. No model involved, so it is instant.
     */
    val starters: kotlinx.coroutines.flow.StateFlow<Starters> =
        kotlinx.coroutines.flow.combine(repository.observeAll(), recent) { items, recent ->
            val recentKeys = recent.map { com.tina.app.agenda.normalizeTitle(it.title) }.toSet()
            val titles = items
                .filter { it.title.isNotBlank() }
                .groupBy { com.tina.app.agenda.normalizeTitle(it.title) }
                .filter { (key, group) -> group.size >= 2 && key !in recentKeys }
                .entries
                .sortedByDescending { (_, group) -> group.size }
                .take(3)
                .map { (_, group) -> group.maxBy { it.createdAt }.title }
            val tags = items.flatMap { it.tags }
                .groupingBy { it }.eachCount().entries
                .sortedByDescending { it.value }
                .take(2)
                .map { it.key }
            Starters(titles, tags)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, Starters())

    /**
     * The field's whole state, selection included, so a programmatic replace (starter chip,
     * calendar long-press) can put the caret at the end. One owner: mirroring this into a
     * second state in the bar raced fast typing and dropped characters.
     */
    var fieldValue by mutableStateOf(TextFieldValue())
        private set

    init {
        // An unsent draft survives the process being killed in the background: it is restored
        // here, then every change is written through. The first emission is the restore itself.
        viewModelScope.launch {
            val saved = settingsRepository.captureDraft()
            if (saved.isNotEmpty() && text.isEmpty()) fieldValue = TextFieldValue(saved, TextRange(saved.length))
            androidx.compose.runtime.snapshotFlow { text }.drop(1).collect { settingsRepository.setCaptureDraft(it) }
        }
    }

    val text: String get() = fieldValue.text
    var removedKinds by mutableStateOf(emptySet<ChipKind>())
        private set
    var removedTags by mutableStateOf(emptySet<String>())
        private set
    var typeOverride by mutableStateOf<ItemType?>(null)
        private set

    /** Idea mode: the field is a note's title and [body] its text. Plan mode parses. */
    var ideaMode by mutableStateOf(false)
        private set
    var body by mutableStateOf("")
        private set

    /** Increments on every save; drives the celebration animation. */
    var saveCount by mutableStateOf(0)
        private set

    var lastSavedId: Long? = null
        private set
    private val tz get() = TimeZone.currentSystemDefault()

    val parsed: ParsedCapture
        get() = parseCapture(text, Clock.System.now().toLocalDateTime(tz), settings.value.firstDayOfWeek)

    fun effective(): ParsedCapture {
        if (ideaMode) {
            // everything in the title renders as a wall of bold; a long thought splits into title + body
            val (title, split) = com.tina.app.notes.splitIdea(text)
            val extra = body.trim().ifEmpty { null }
            return ParsedCapture(
                title = if (extra == null) title else text.trim(),
                type = ItemType.NOTE,
                body = extra ?: split,
                tags = parsed.tags,
            )
        }
        var p = parsed
        if (ChipKind.DATE in removedKinds) p = p.copy(date = null)
        if (ChipKind.TIME in removedKinds) p = p.copy(time = null)
        if (ChipKind.DURATION in removedKinds) p = p.copy(durationMinutes = null)
        if (ChipKind.PRIORITY in removedKinds) p = p.copy(priority = Priority.NONE)
        if (ChipKind.RECURRENCE in removedKinds) p = p.copy(rrule = null)
        if (removedTags.isNotEmpty()) p = p.copy(tags = p.tags - removedTags)
        // removing the time demotes an event back to the date rules
        if (p.type == ItemType.EVENT && p.time == null && p.rrule == null) p = p.copy(type = ItemType.TASK)
        typeOverride?.let { p = p.copy(type = it) }
        return p
    }

    fun onFieldChange(value: TextFieldValue) {
        fieldValue = value
    }

    /** Replace the text and park the caret at the end (speech, starters, calendar). */
    fun onTextChange(value: String) {
        fieldValue = TextFieldValue(value, TextRange(value.length))
    }

    /** Seed the field (e.g. from a calendar long-press) without disturbing other state. */
    fun prefill(value: String) = onTextChange(value)

    fun switchIdeaMode(value: Boolean) {
        ideaMode = value
        typeOverride = null
    }

    fun onBodyChange(value: String) {
        body = value
    }

    fun removeChip(kind: ChipKind) {
        removedKinds = removedKinds + kind
    }

    fun removeTag(tag: String) {
        removedTags = removedTags + tag
    }

    fun cycleType() {
        typeOverride = when (effective().type) {
            ItemType.INBOX -> ItemType.TASK
            ItemType.TASK -> ItemType.EVENT
            // notes are made in Idea mode, not by cycling
            ItemType.EVENT -> ItemType.INBOX
            ItemType.NOTE -> ItemType.INBOX
        }
    }

    fun save(onSaved: () -> Unit = {}) {
        if (text.isBlank()) return
        val effective = effective()
        val raw = text
        viewModelScope.launch {
            lastSavedId = repository.capture(effective, tz, settings.value.defaultReminderMinutes, settings.value.undatedToSort)
            lastSavedId?.let { id ->
                refiner.refineInBackground(id, raw) { original, _ -> refinedEvents.tryEmit(original) }
            }
            fieldValue = TextFieldValue()
            body = ""
            removedKinds = emptySet()
            removedTags = emptySet()
            typeOverride = null
            saveCount++
            onSaved()
            review.onCapture()
        }
    }

    /** Throw the draft away: text, removed chips, type override. */
    fun discard() {
        fieldValue = TextFieldValue()
        body = ""
        removedKinds = emptySet()
        removedTags = emptySet()
        typeOverride = null
    }

    fun undoLastSave() {
        val id = lastSavedId ?: return
        lastSavedId = null
        viewModelScope.launch { repository.delete(id) }
    }

    fun undoRefinement(original: com.tina.app.data.Item) {
        viewModelScope.launch { repository.update(original) }
    }

    fun applyImprovement(updated: com.tina.app.data.Item) {
        viewModelScope.launch { repository.update(updated) }
    }
}
