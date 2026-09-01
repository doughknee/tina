package com.tina.app.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

enum class TodaySection { OVERDUE, MORNING, AFTERNOON, EVENING, ANYTIME }

data class TodayEntry(
    val item: Item,
    /** Display/sort time within the day, null = anytime. */
    val time: LocalTime?,
)

data class TodayUiState(
    val today: LocalDate? = null,
    val sections: List<Pair<TodaySection, List<TodayEntry>>> = emptyList(),
    val inboxCount: Int = 0,
)

class TodayViewModel(private val repository: ItemRepository) : ViewModel() {
    private val tz = TimeZone.currentSystemDefault()
    private var lastDeleted: Item? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TodayUiState> = dateTicker().flatMapLatest { today ->
        val dayStart = today.atStartOfDayIn(tz).toEpochMilliseconds()
        val dayEnd = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()
        combine(
            repository.observeTasksForDay(today, tz),
            repository.observeEventsIntersecting(dayStart, dayEnd),
            repository.observeInboxCount(),
        ) { tasks, events, inboxCount ->
            TodayUiState(today, buildSections(tasks, events, today, dayStart, dayEnd), inboxCount)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    private fun dateTicker() = flow {
        while (true) {
            emit(Clock.System.now().toLocalDateTime(tz).date)
            delay(60_000)
        }
    }.distinctUntilChanged()

    private fun buildSections(
        tasks: List<Item>,
        events: List<Item>,
        today: LocalDate,
        dayStart: Long,
        dayEnd: Long,
    ): List<Pair<TodaySection, List<TodayEntry>>> {
        val todayEpoch = today.toEpochDays().toInt()

        val overdue = tasks
            .filter { !it.completed && it.dueDate != null && it.dueDate < todayEpoch }
            .map { TodayEntry(it, it.dueLocalTime) }

        val timed = mutableListOf<TodayEntry>()
        val anytime = mutableListOf<TodayEntry>()

        tasks.filter { it.dueDate == null || it.dueDate >= todayEpoch || it.completed }.forEach { task ->
            val time = task.dueLocalTime
            if (time != null) timed += TodayEntry(task, time) else anytime += TodayEntry(task, null)
        }
        events.forEach { event ->
            repository.occurrencesOf(event, dayStart, dayEnd, tz).forEach { occurrence ->
                if (event.allDay) {
                    anytime += TodayEntry(event, null)
                } else {
                    timed += TodayEntry(event, Instant.fromEpochMilliseconds(occurrence).toLocalDateTime(tz).time)
                }
            }
        }

        fun minutes(t: LocalTime?) = t?.let { it.hour * 60 + it.minute } ?: -1
        val byTime = compareBy<TodayEntry>({ minutes(it.time) }, { it.item.sortOrder })

        val sections = listOf(
            TodaySection.OVERDUE to overdue.sortedWith(compareBy({ it.item.dueDate }, { minutes(it.time) })),
            TodaySection.MORNING to timed.filter { minutes(it.time) < 12 * 60 }.sortedWith(byTime),
            TodaySection.AFTERNOON to timed.filter { minutes(it.time) in (12 * 60) until (17 * 60) }.sortedWith(byTime),
            TodaySection.EVENING to timed.filter { minutes(it.time) >= 17 * 60 }.sortedWith(byTime),
            TodaySection.ANYTIME to anytime.sortedWith(
                compareBy({ it.item.completed }, { it.item.sortOrder }),
            ),
        )
        return sections.filter { it.second.isNotEmpty() }
    }

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

    /** Persist manual order of the Anytime section after a drag. */
    fun persistAnytimeOrder(ids: List<Long>) {
        viewModelScope.launch {
            ids.forEachIndexed { index, id -> repository.setSortOrder(id, index.toLong()) }
        }
    }
}
