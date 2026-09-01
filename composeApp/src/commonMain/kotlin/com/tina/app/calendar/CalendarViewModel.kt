package com.tina.app.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class CalendarEntry(val item: Item, val time: LocalTime?)

class CalendarViewModel(private val repository: ItemRepository) : ViewModel() {
    private val tz = TimeZone.currentSystemDefault()
    private var lastDeleted: Item? = null

    val today: LocalDate get() = Clock.System.now().toLocalDateTime(tz).date

    val selectedDate = MutableStateFlow(today)

    /** Date range the calendar is currently showing, padded by the screen. */
    private val visibleRange = MutableStateFlow(defaultRange())

    private fun defaultRange(): Pair<LocalDate, LocalDate> {
        val now = today
        return now.plus(-45, DateTimeUnit.DAY) to now.plus(45, DateTimeUnit.DAY)
    }

    fun select(date: LocalDate) {
        selectedDate.value = date
    }

    fun setVisibleRange(start: LocalDate, end: LocalDate) {
        visibleRange.value = start to end
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val agenda: StateFlow<List<CalendarEntry>> = selectedDate.flatMapLatest { day ->
        val dayStart = day.atStartOfDayIn(tz).toEpochMilliseconds()
        val dayEnd = day.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()
        combine(
            repository.observeTasksDueBetween(day, day),
            repository.observeEventsIntersecting(dayStart, dayEnd),
        ) { tasks, events ->
            val entries = mutableListOf<CalendarEntry>()
            tasks.forEach { entries += CalendarEntry(it, it.dueLocalTime) }
            events.forEach { event ->
                repository.occurrencesOf(event, dayStart, dayEnd, tz).forEach { occurrence ->
                    entries += CalendarEntry(
                        event,
                        if (event.allDay) null else Instant.fromEpochMilliseconds(occurrence).toLocalDateTime(tz).time,
                    )
                }
            }
            entries.sortedWith(
                compareBy(
                    { it.time == null },
                    { it.time?.let { t -> t.hour * 60 + t.minute } ?: 0 },
                    { it.item.sortOrder },
                ),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Up to three item colors (null = theme default) per date, for the month dots. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val dots: StateFlow<Map<LocalDate, List<Long?>>> = visibleRange.flatMapLatest { (start, end) ->
        val rangeStart = start.atStartOfDayIn(tz).toEpochMilliseconds()
        val rangeEnd = end.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()
        combine(
            repository.observeTasksDueBetween(start, end),
            repository.observeEventsIntersecting(rangeStart, rangeEnd),
        ) { tasks, events ->
            val byDate = mutableMapOf<LocalDate, MutableList<Long?>>()
            tasks.forEach { task ->
                task.dueLocalDate?.let { byDate.getOrPut(it) { mutableListOf() }.add(task.color) }
            }
            events.forEach { event ->
                repository.occurrencesOf(event, rangeStart, rangeEnd, tz).forEach { occurrence ->
                    val date = Instant.fromEpochMilliseconds(occurrence).toLocalDateTime(tz).date
                    byDate.getOrPut(date) { mutableListOf() }.add(event.color)
                }
            }
            byDate.mapValues { (_, colors) -> colors.take(3) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun toggleComplete(item: Item) {
        viewModelScope.launch {
            if (item.completed) repository.uncomplete(item.id) else repository.complete(item.id)
        }
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

    fun reschedule(item: Item, date: LocalDate?) {
        viewModelScope.launch { repository.reschedule(item.id, date) }
    }
}
