package com.tina.app.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.SettingsRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class AgendaUiState(
    val today: LocalDate,
    val selected: LocalDate,
    val groups: List<AgendaGroup>,
    val inboxCount: Int,
)

/** Today and Calendar were rendering the same rows for the same date; this is the one list. */
class AgendaViewModel(
    private val repository: ItemRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val tz = TimeZone.currentSystemDefault()
    private var lastDeleted: Item? = null

    private fun today(): LocalDate = Clock.System.now().toLocalDateTime(tz).date

    val selectedDate = MutableStateFlow(today())

    /** Padded to what the month grid can show, so its dots come from one query. */
    private val visibleRange = MutableStateFlow(today().plus(-45, DateTimeUnit.DAY) to today().plus(45, DateTimeUnit.DAY))

    private val ticker = flow {
        while (true) {
            emit(today())
            delay(60_000)
        }
    }.distinctUntilChanged()

    // ponytail: the whole table in memory, then buildAgenda; fine at one person's scale
    val state: StateFlow<AgendaUiState?> = combine(
        ticker,
        selectedDate,
        repository.observeAll(),
        repository.observeInboxCount(),
        settingsRepository.settings,
    ) { today, selected, items, inbox, settings ->
        AgendaUiState(
            today = today,
            selected = selected,
            groups = buildAgenda(
                items,
                AgendaRange.day(selected),
                today,
                tz,
                AgendaSettings(
                    afternoonStartMinutes = settings.afternoonStartMinutes,
                    eveningStartMinutes = settings.eveningStartMinutes,
                    showCompleted = settings.showCompletedInToday,
                ),
            ),
            inboxCount = inbox,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Up to three item colors (null = theme default) per date, for the strip and grid dots. */
    val dots: StateFlow<Map<LocalDate, List<Long?>>> = combine(visibleRange, repository.observeAll()) { (start, end), items ->
        val rangeStart = start.atStartOfDayIn(tz).toEpochMilliseconds()
        val rangeEnd = end.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()
        val byDate = mutableMapOf<LocalDate, MutableList<Long?>>()
        items.forEach { item ->
            when {
                item.completed -> Unit
                item.startAt != null -> repository.occurrencesOf(item, rangeStart, rangeEnd, tz).forEach { occurrence ->
                    val date = Instant.fromEpochMilliseconds(occurrence).toLocalDateTime(tz).date
                    byDate.getOrPut(date) { mutableListOf() }.add(item.color)
                }
                else -> item.dueLocalDate?.let { byDate.getOrPut(it) { mutableListOf() }.add(item.color) }
            }
        }
        byDate.mapValues { (_, colors) -> colors.take(3) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun select(date: LocalDate) {
        selectedDate.value = date
    }

    fun setVisibleRange(start: LocalDate, end: LocalDate) {
        visibleRange.value = start to end
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

    /** Persist manual order of the Anytime group after a drag. */
    fun persistAnytimeOrder(ids: List<Long>) {
        viewModelScope.launch {
            ids.forEachIndexed { index, id -> repository.setSortOrder(id, index.toLong()) }
        }
    }
}
