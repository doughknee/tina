package com.tina.app.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import kotlin.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

enum class TriageAction { TODAY, TOMORROW, THIS_WEEK, SOMEDAY, MAKE_EVENT, MAKE_NOTE }

/** Sorting: the one thing the Agenda deliberately does not show. */
class InboxViewModel(private val repository: ItemRepository) : ViewModel() {
    private val tz = TimeZone.currentSystemDefault()
    private var lastTriaged: Item? = null
    private var lastDeleted: Item? = null

    val items: StateFlow<List<Item>> = repository.observeInbox()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val count: StateFlow<Int> = repository.observeInboxCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

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
