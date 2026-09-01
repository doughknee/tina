package com.tina.app.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.OccurrenceRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class AgendaUiState(
    val today: LocalDate,
    val selected: LocalDate,
    val range: AgendaRange,
    val groups: List<AgendaGroup>,
    val inboxCount: Int,
)

/** Today and Calendar were rendering the same rows for the same date; this is the one list. */
class AgendaViewModel(
    private val repository: ItemRepository,
    private val occurrences: OccurrenceRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val tz = TimeZone.currentSystemDefault()
    private var lastDeleted: Item? = null

    private fun today(): LocalDate = Clock.System.now().toLocalDateTime(tz).date

    val selectedDate = MutableStateFlow(today())

    /** Persisted so the zoom level survives launches. */
    val granularity: StateFlow<Granularity> = settingsRepository.settings
        .map { s -> Granularity.entries.firstOrNull { it.name == s.agendaRange } ?: Granularity.DAY }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Granularity.DAY)

    /** Per-group "+N more" and per-series inline expansion. Both reset on range change. */
    val expandedGroups = MutableStateFlow<Set<GroupKey>>(emptySet())
    val expandedSeries = MutableStateFlow<Set<Long>>(emptySet())

    /** Padded to what the month grid can show, so its dots come from one query. */
    private val visibleRange = MutableStateFlow(today().plus(-45, DateTimeUnit.DAY) to today().plus(45, DateTimeUnit.DAY))

    private val ticker = flow {
        while (true) {
            emit(today())
            delay(60_000)
        }
    }.distinctUntilChanged()

    private fun rangeFor(granularity: Granularity, date: LocalDate) = when (granularity) {
        Granularity.DAY -> AgendaRange.day(date)
        Granularity.WEEK -> AgendaRange.week(date)
        Granularity.MONTH -> AgendaRange.month(date)
        Granularity.ALL -> AgendaRange.All
    }

    private val anchor = combine(ticker, selectedDate, granularity) { today, selected, g -> Triple(today, selected, g) }
    private val marks = combine(occurrences.observeDone(), occurrences.observeSkipped()) { done, skipped -> done to skipped }

    // ponytail: the whole table in memory, then buildAgenda; fine at one person's scale
    val state: StateFlow<AgendaUiState?> = combine(
        anchor,
        repository.observeAll(),
        repository.observeInboxCount(),
        settingsRepository.settings,
        marks,
    ) { (today, selected, granularity), items, inbox, settings, (done, skipped) ->
        val range = rangeFor(granularity, selected)
        AgendaUiState(
            today = today,
            selected = selected,
            range = range,
            groups = buildAgenda(
                items = items,
                range = range,
                today = today,
                tz = tz,
                settings = AgendaSettings(
                    afternoonStartMinutes = settings.afternoonStartMinutes,
                    eveningStartMinutes = settings.eveningStartMinutes,
                    showCompleted = settings.showCompletedInToday,
                ),
                completedOccurrences = done,
                skippedOccurrences = skipped,
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

    fun setGranularity(value: Granularity) {
        expandedGroups.value = emptySet()
        expandedSeries.value = emptySet()
        viewModelScope.launch { settingsRepository.setAgendaRange(value.name) }
    }

    /** Horizontal swipe: the next range of the same size. ALL does not paginate. */
    fun shiftRange(steps: Int) {
        val current = selectedDate.value
        selectedDate.value = when (granularity.value) {
            Granularity.DAY -> current.plus(steps, DateTimeUnit.DAY)
            Granularity.WEEK -> current.plus(steps * 7, DateTimeUnit.DAY)
            Granularity.MONTH -> LocalDate(current.year, current.month, 1).plus(steps, DateTimeUnit.MONTH)
            Granularity.ALL -> return
        }
        expandedGroups.value = emptySet()
    }

    fun setVisibleRange(start: LocalDate, end: LocalDate) {
        visibleRange.value = start to end
    }

    fun toggleGroup(key: GroupKey) {
        expandedGroups.value = expandedGroups.value.let { if (key in it) it - key else it + key }
    }

    fun toggleSeries(itemId: Long) {
        expandedSeries.value = expandedSeries.value.let { if (itemId in it) it - itemId else it + itemId }
    }

    fun toggleComplete(item: Item) {
        viewModelScope.launch {
            if (item.completed) repository.uncomplete(item.id) else repository.complete(item.id)
        }
    }

    /** Rule 6: completing a rolled-up series row completes the next occurrence only. */
    fun completeOccurrence(itemId: Long, date: LocalDate) {
        viewModelScope.launch { occurrences.complete(itemId, date) }
    }

    fun skipOccurrence(itemId: Long, date: LocalDate) {
        viewModelScope.launch { occurrences.skip(itemId, date) }
    }

    fun clearOccurrence(itemId: Long, date: LocalDate) {
        viewModelScope.launch { occurrences.clear(itemId, date) }
    }

    /** Strips the rule; the item stays as a one-off. Undo restores the rule. */
    fun endSeries(item: Item) {
        viewModelScope.launch { repository.update(item.copy(recurrence = null)) }
    }

    fun restoreItem(item: Item) {
        viewModelScope.launch { repository.update(item) }
    }

    /** Duplicate merge: the extra records go to Trash, which is what makes it undoable. */
    fun mergeDuplicates(others: List<Item>) {
        viewModelScope.launch { others.forEach { repository.delete(it.id) } }
    }

    fun restoreAll(items: List<Item>) {
        viewModelScope.launch { items.forEach { repository.restore(it) } }
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
