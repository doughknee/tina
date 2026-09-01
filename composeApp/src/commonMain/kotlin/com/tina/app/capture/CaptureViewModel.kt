package com.tina.app.capture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class ChipKind { DATE, TIME, DURATION, PRIORITY, RECURRENCE }

class CaptureViewModel(private val repository: ItemRepository) : ViewModel() {
    var text by mutableStateOf("")
        private set
    var removedKinds by mutableStateOf(emptySet<ChipKind>())
        private set
    var removedTags by mutableStateOf(emptySet<String>())
        private set
    var typeOverride by mutableStateOf<ItemType?>(null)
        private set

    /** Increments on every save; drives the celebration animation. */
    var saveCount by mutableStateOf(0)
        private set

    private var lastSavedId: Long? = null
    private val tz get() = TimeZone.currentSystemDefault()

    val parsed: ParsedCapture
        get() = parseCapture(text, Clock.System.now().toLocalDateTime(tz))

    fun effective(): ParsedCapture {
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
        if (p.type == ItemType.NOTE && p.body == null) p = p.copy(body = text)
        return p
    }

    fun onTextChange(value: String) {
        text = value
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
            ItemType.EVENT -> ItemType.NOTE
            ItemType.NOTE -> ItemType.INBOX
        }
    }

    fun save(onSaved: () -> Unit = {}) {
        if (text.isBlank()) return
        val effective = effective()
        viewModelScope.launch {
            lastSavedId = repository.capture(effective, tz)
            text = ""
            removedKinds = emptySet()
            removedTags = emptySet()
            typeOverride = null
            saveCount++
            onSaved()
        }
    }

    fun undoLastSave() {
        val id = lastSavedId ?: return
        lastSavedId = null
        viewModelScope.launch { repository.delete(id) }
    }
}
