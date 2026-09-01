package com.tina.app.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import com.tina.app.data.SettingsRepository
import com.tina.app.notes.htmlPreview
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

enum class LibraryFilter { ALL, INBOX, TASKS, EVENTS, NOTES, DONE }

enum class TriageAction { TODAY, TOMORROW, THIS_WEEK, SOMEDAY, MAKE_EVENT, MAKE_NOTE }

data class LibraryUiState(
    /** Untriaged captures, shown as cards with the triage chips. */
    val triage: List<Item> = emptyList(),
    /** Everything else in scope, most recently touched first. */
    val rows: List<Item> = emptyList(),
    val inboxCount: Int = 0,
)

/** Library = Notes + Inbox + Search + Done: one list, one query, one filter rail. */
class LibraryViewModel(
    private val repository: ItemRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val tz = TimeZone.currentSystemDefault()
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(LibraryFilter.ALL)
    private var lastDeleted: Item? = null
    private var lastTriaged: Item? = null

    // ponytail: in-memory filter over the whole table; the LIKE query goes when nothing calls it
    val state: StateFlow<LibraryUiState> = combine(
        repository.observeAll(),
        query,
        filter,
        settingsRepository.settings,
    ) { items, q, f, settings ->
        val matching = items.filter { it.matches(q) }
        val untriaged = matching.filter { it.type == ItemType.INBOX }
        val scoped = when (f) {
            LibraryFilter.ALL -> matching.filter { it.type != ItemType.INBOX }
            LibraryFilter.INBOX -> emptyList()
            LibraryFilter.TASKS -> matching.filter { it.type == ItemType.TASK }
            LibraryFilter.EVENTS -> matching.filter { it.type == ItemType.EVENT }
            LibraryFilter.NOTES -> matching.filter { it.type == ItemType.NOTE }
            LibraryFilter.DONE -> matching.filter { it.completed }
        }.let { rows ->
            if (f == LibraryFilter.DONE || settings.searchCompleted) rows else rows.filter { !it.completed }
        }
        LibraryUiState(
            triage = if (f == LibraryFilter.ALL || f == LibraryFilter.INBOX) untriaged else emptyList(),
            rows = scoped.sortedWith(compareByDescending<Item> { it.pinned }.thenByDescending { it.updatedAt }),
            inboxCount = items.count { it.type == ItemType.INBOX },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    private fun Item.matches(q: String): Boolean {
        if (q.isBlank()) return true
        val needle = q.trim()
        return title.contains(needle, ignoreCase = true) ||
            tags.any { it.contains(needle.removePrefix("#"), ignoreCase = true) } ||
            (body?.let { htmlPreview(it).contains(needle, ignoreCase = true) } == true)
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: LibraryFilter) {
        filter.value = value
    }

    /** One tap, no dialogs: the row leaves the inbox immediately. */
    fun triage(item: Item, action: TriageAction) {
        lastTriaged = item
        val now = Clock.System.now().toLocalDateTime(tz)
        val today = now.date
        val updated = when (action) {
            TriageAction.TODAY -> item.copy(type = ItemType.TASK, dueDate = today.toEpochDays().toInt())
            TriageAction.TOMORROW -> item.copy(
                type = ItemType.TASK,
                dueDate = today.plus(1, DateTimeUnit.DAY).toEpochDays().toInt(),
            )
            TriageAction.THIS_WEEK -> item.copy(
                type = ItemType.TASK,
                // due by the end of the current week (Sunday for an ISO week)
                dueDate = today.plus(7 - today.dayOfWeek.isoDayNumber, DateTimeUnit.DAY).toEpochDays().toInt(),
            )
            TriageAction.SOMEDAY -> item.copy(type = ItemType.TASK, dueDate = null)
            TriageAction.MAKE_EVENT -> {
                val start = if (now.hour >= 23) {
                    LocalDateTime(today.plus(1, DateTimeUnit.DAY), LocalTime(0, 0))
                } else {
                    LocalDateTime(today, LocalTime(now.hour + 1, 0))
                }
                val startMillis = start.toInstant(tz).toEpochMilliseconds()
                item.copy(type = ItemType.EVENT, startAt = startMillis, endAt = startMillis + 60 * 60_000L)
            }
            TriageAction.MAKE_NOTE -> item.copy(type = ItemType.NOTE, body = item.body ?: item.title)
        }
        viewModelScope.launch { repository.update(updated) }
    }

    fun undoTriage() {
        val snapshot = lastTriaged ?: return
        lastTriaged = null
        viewModelScope.launch { repository.update(snapshot) }
    }

    fun toggleComplete(item: Item) {
        viewModelScope.launch {
            if (item.completed) repository.uncomplete(item.id) else repository.complete(item.id)
        }
    }

    fun togglePin(item: Item) {
        viewModelScope.launch { repository.update(item.copy(pinned = !item.pinned)) }
    }

    fun delete(item: Item) {
        lastDeleted = item
        viewModelScope.launch { repository.delete(item.id) }
    }

    fun undoDelete() {
        val item = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { repository.restore(item) }
    }

    fun rename(item: Item, title: String) {
        viewModelScope.launch { repository.rename(item.id, title) }
    }
}
