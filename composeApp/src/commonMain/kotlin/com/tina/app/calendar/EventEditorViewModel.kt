package com.tina.app.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import kotlin.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class EventEditorViewModel(
    private val itemId: Long,
    private val repository: ItemRepository,
) : ViewModel() {
    private val tz = TimeZone.currentSystemDefault()

    val item = repository.observe(itemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun edit(transform: (Item) -> Item) {
        viewModelScope.launch {
            repository.get(itemId)?.let { repository.update(transform(it)) }
        }
    }

    private fun Long.toLocal(): LocalDateTime = Instant.fromEpochMilliseconds(this).toLocalDateTime(tz)
    private fun LocalDateTime.toMillis(): Long = toInstant(tz).toEpochMilliseconds()

    fun setTitle(title: String) = edit { it.copy(title = title) }
    fun setBody(body: String) = edit { it.copy(body = body.ifBlank { null }) }
    fun setColor(color: Long?) = edit { it.copy(color = color) }
    fun applyImprovement(updated: Item) {
        viewModelScope.launch { repository.update(updated) }
    }
    fun setReminder(minutes: Int?) = edit { it.copy(reminderOffsetMinutes = minutes) }
    fun setRrule(rrule: String?) = edit { it.copy(recurrence = rrule) }

    fun setAllDay(allDay: Boolean) = edit { item ->
        val startDate = item.startAt?.toLocal()?.date ?: return@edit item
        if (allDay) {
            val start = LocalDateTime(startDate, LocalTime(0, 0))
            item.copy(
                allDay = true,
                startAt = start.toMillis(),
                endAt = LocalDateTime(startDate.plus(1, DateTimeUnit.DAY), LocalTime(0, 0)).toMillis(),
            )
        } else {
            val start = LocalDateTime(startDate, LocalTime(9, 0))
            item.copy(allDay = false, startAt = start.toMillis(), endAt = start.toMillis() + 60 * 60_000L)
        }
    }

    /** Moving the start keeps the duration. */
    fun setStartDate(date: LocalDate) = edit { item ->
        val startMillis = item.startAt ?: return@edit item
        val duration = (item.endAt ?: startMillis) - startMillis
        val newStart = LocalDateTime(date, startMillis.toLocal().time).toMillis()
        item.copy(startAt = newStart, endAt = newStart + duration)
    }

    fun setStartTime(time: LocalTime) = edit { item ->
        val startMillis = item.startAt ?: return@edit item
        val duration = (item.endAt ?: startMillis) - startMillis
        val newStart = LocalDateTime(startMillis.toLocal().date, time).toMillis()
        item.copy(startAt = newStart, endAt = newStart + duration)
    }

    fun setEndDate(date: LocalDate) = edit { item ->
        val end = item.endAt?.toLocal() ?: return@edit item
        clampEnd(item, LocalDateTime(date, end.time).toMillis())
    }

    fun setEndTime(time: LocalTime) = edit { item ->
        val end = item.endAt?.toLocal() ?: return@edit item
        clampEnd(item, LocalDateTime(end.date, time).toMillis())
    }

    private fun clampEnd(item: Item, newEnd: Long): Item {
        val start = item.startAt ?: return item
        return item.copy(endAt = if (newEnd <= start) start + 30 * 60_000L else newEnd)
    }

    fun delete(deletedMessage: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val item = repository.get(itemId)
            repository.delete(itemId)
            if (item != null) com.tina.app.ui.PendingUndo.request(deletedMessage) { repository.restore(item) }
            onDeleted()
        }
    }
}
