package com.tina.app.agenda

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.plusMonths
import com.tina.app.LocalSettings
import com.tina.app.data.Item
import com.tina.app.data.ItemType
import com.tina.app.resources.Res
import com.tina.app.resources.agenda_everything
import com.tina.app.resources.calendar_jump_today
import com.tina.app.resources.calendar_nothing
import com.tina.app.resources.calendar_view_month
import com.tina.app.resources.calendar_view_week
import com.tina.app.resources.date_today
import com.tina.app.resources.deleted
import com.tina.app.resources.dup_keep
import com.tina.app.resources.dup_merge
import com.tina.app.resources.dup_merged
import com.tina.app.resources.dup_title
import com.tina.app.resources.duplicate_copies
import com.tina.app.resources.horizon_later
import com.tina.app.resources.inbox_captured
import com.tina.app.resources.inbox_waiting
import com.tina.app.resources.months_full
import com.tina.app.resources.fewer_rows
import com.tina.app.resources.more_rows
import com.tina.app.resources.occurrence_skipped
import com.tina.app.resources.range_all
import com.tina.app.resources.range_day
import com.tina.app.resources.range_month
import com.tina.app.resources.range_week
import com.tina.app.resources.search
import com.tina.app.resources.section_afternoon
import com.tina.app.resources.section_anytime
import com.tina.app.resources.section_evening
import com.tina.app.resources.section_morning
import com.tina.app.resources.section_overdue
import com.tina.app.resources.section_repeating_week
import com.tina.app.resources.section_series
import com.tina.app.resources.series_complete_day
import com.tina.app.resources.series_count_month
import com.tina.app.resources.series_count_week
import com.tina.app.resources.series_done_of
import com.tina.app.resources.series_end
import com.tina.app.resources.series_ended
import com.tina.app.resources.series_hidden
import com.tina.app.resources.series_next
import com.tina.app.resources.series_skip
import com.tina.app.resources.settings
import com.tina.app.resources.span_day_of
import com.tina.app.resources.today_empty
import com.tina.app.resources.triage_someday
import com.tina.app.resources.triage_this_week
import com.tina.app.resources.undo
import com.tina.app.resources.weekdays_full
import com.tina.app.ui.ConnectedButtonGroup
import com.tina.app.ui.ItemRow
import com.tina.app.ui.KeyBus
import com.tina.app.ui.KeyCommand
import com.tina.app.ui.SectionCardItem
import com.tina.app.ui.dateLabel
import com.tina.app.ui.expandEnter
import com.tina.app.ui.expandExit
import com.tina.app.ui.recurrenceLabel
import com.tina.app.ui.rememberAppMotion
import com.tina.app.ui.relativeAge
import com.tina.app.ui.rememberUndoWindow
import com.tina.app.ui.showUndo
import com.tina.app.ui.timeLabel
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val LocalDate.ym: YearMonth get() = YearMonth(year, month)

/**
 * Agenda = Today + Calendar with four zoom levels. One list under a date header whose shape
 * follows the range: week strip for Day, the whole week pilled for Week, the month grid for
 * Month, nothing for All. Every range renders the same row types from [buildAgenda].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenItem: (Item) -> Unit,
    onCaptureForDate: (LocalDate) -> Unit,
    viewModel: AgendaViewModel = koinViewModel(),
) {
    val settings = LocalSettings.current
    val state by viewModel.state.collectAsState()
    val dots by viewModel.dots.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val granularity by viewModel.granularity.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()
    val expandedSeries by viewModel.expandedSeries.collectAsState()
    var monthMode by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val undoWindow = rememberUndoWindow()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val deletedText = stringResource(Res.string.deleted)
    val undoText = stringResource(Res.string.undo)
    val monthNames = stringArrayResource(Res.array.months_full)
    val today = state?.today ?: selectedDate
    val motion = rememberAppMotion()
    var duplicateSheet by remember { mutableStateOf<AgendaRow.Duplicate?>(null) }

    val startMonth = remember { today.ym.minusMonths(60) }
    val endMonth = remember { today.ym.plusMonths(60) }
    val monthState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = selectedDate.ym,
        firstDayOfWeek = settings.firstDayOfWeek,
    )
    val weekState = rememberWeekCalendarState(
        startDate = startMonth.firstDay,
        endDate = endMonth.lastDay,
        firstVisibleWeekDate = selectedDate,
        firstDayOfWeek = settings.firstDayOfWeek,
    )
    val gridShown = granularity == Granularity.MONTH || (granularity == Granularity.DAY && monthMode)

    // keep the dot query in step with whichever header is on screen
    LaunchedEffect(monthState, weekState, gridShown) {
        if (gridShown) {
            snapshotFlow { monthState.firstVisibleMonth }.collect { month ->
                viewModel.setVisibleRange(
                    month.yearMonth.firstDay.plus(-7, DateTimeUnit.DAY),
                    month.yearMonth.lastDay.plus(7, DateTimeUnit.DAY),
                )
            }
        } else {
            snapshotFlow { weekState.firstVisibleWeek }.collect { week ->
                viewModel.setVisibleRange(
                    week.days.first().date.plus(-7, DateTimeUnit.DAY),
                    week.days.last().date.plus(7, DateTimeUnit.DAY),
                )
            }
        }
    }
    // the month grid follows a swiped-to month
    LaunchedEffect(selectedDate, granularity) {
        if (granularity == Granularity.MONTH) monthState.animateScrollToMonth(selectedDate.ym)
    }

    fun jumpTo(date: LocalDate) {
        viewModel.select(date)
        scope.launch {
            if (gridShown) monthState.animateScrollToMonth(date.ym) else weekState.animateScrollToWeek(date)
        }
    }

    LaunchedEffect(Unit) {
        KeyBus.events.collect { command ->
            val current = viewModel.selectedDate.value
            when (command) {
                KeyCommand.LEFT -> jumpTo(current.plus(-1, DateTimeUnit.DAY))
                KeyCommand.RIGHT -> jumpTo(current.plus(1, DateTimeUnit.DAY))
                KeyCommand.UP -> jumpTo(current.plus(-7, DateTimeUnit.DAY))
                KeyCommand.DOWN -> jumpTo(current.plus(7, DateTimeUnit.DAY))
                else -> Unit
            }
        }
    }

    fun withUndo(message: String, action: () -> Unit, undo: () -> Unit) {
        action()
        scope.launch {
            if (snackbarHostState.showUndo(message, undoText, undoWindow)) undo()
        }
    }

    fun deleteWithUndo(item: Item) = withUndo(deletedText, { viewModel.delete(item) }, viewModel::undoDelete)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (granularity) {
                        Granularity.ALL -> stringResource(Res.string.agenda_everything)
                        Granularity.WEEK -> weekTitle(selectedDate, monthNames)
                        else -> {
                            val visibleMonth = if (gridShown) monthState.firstVisibleMonth.yearMonth.month
                            else weekState.firstVisibleWeek.days[3].date.month
                            monthNames[visibleMonth.number - 1]
                        }
                    }
                    val toggleable = granularity == Granularity.DAY
                    Row(
                        Modifier.combinedClickable(enabled = toggleable, onClick = { monthMode = !monthMode }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(title, style = MaterialTheme.typography.titleLargeEmphasized)
                        if (toggleable) {
                            Icon(
                                if (monthMode) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                stringResource(if (monthMode) Res.string.calendar_view_week else Res.string.calendar_view_month),
                                Modifier.padding(start = 4.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    AnimatedVisibility(visible = selectedDate != today && granularity != Granularity.ALL) {
                        AssistChip(
                            onClick = { jumpTo(today) },
                            label = { Text(stringResource(Res.string.calendar_jump_today)) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Outlined.Search, stringResource(Res.string.search))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(Res.string.settings))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            RangeSwitcher(granularity, viewModel::setGranularity)

            AnimatedContent(
                targetState = when {
                    granularity == Granularity.ALL -> HeaderKind.NONE
                    gridShown -> HeaderKind.GRID
                    granularity == Granularity.WEEK -> HeaderKind.PILLED_WEEK
                    else -> HeaderKind.STRIP
                },
                transitionSpec = { motion.fadeSwap().using(SizeTransform(clip = false)) },
                label = "date-header",
            ) { kind ->
                when (kind) {
                    HeaderKind.NONE -> Unit
                    HeaderKind.GRID -> Column {
                        WeekdayHeader(settings.firstDayOfWeek.isoDayNumber)
                        HorizontalCalendar(
                            state = monthState,
                            dayContent = { day: CalendarDay ->
                                DayCell(
                                    date = day.date,
                                    inMonth = day.position == DayPosition.MonthDate,
                                    isSelected = day.date == selectedDate,
                                    isToday = day.date == today,
                                    hasContent = dots[day.date].orEmpty().isNotEmpty(),
                                    onClick = { viewModel.select(day.date) },
                                    onLongClick = { onCaptureForDate(day.date) },
                                )
                            },
                        )
                    }
                    HeaderKind.STRIP -> Column {
                        WeekdayHeader(settings.firstDayOfWeek.isoDayNumber)
                        WeekCalendar(
                            state = weekState,
                            dayContent = { day: WeekDay ->
                                DayCell(
                                    date = day.date,
                                    inMonth = true,
                                    isSelected = day.date == selectedDate,
                                    isToday = day.date == today,
                                    hasContent = dots[day.date].orEmpty().isNotEmpty(),
                                    onClick = { viewModel.select(day.date) },
                                    onLongClick = { onCaptureForDate(day.date) },
                                )
                            },
                        )
                    }
                    // the seven days from the selected date, pilled as one unit
                    HeaderKind.PILLED_WEEK -> PilledWeek(
                        start = selectedDate,
                        today = today,
                        hasContent = { dots[it].orEmpty().isNotEmpty() },
                        onSelect = viewModel::select,
                        onLongClick = onCaptureForDate,
                    )
                }
            }

            val ui = state ?: return@Column
            com.tina.app.notifications.ReminderPermissionBanner(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // a horizontal swipe on the list moves to the next range of the same size
            val swipeThreshold = with(density) { 96.dp.toPx() }
            val swipeModifier = Modifier.pointerInput(granularity) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (abs(total) > swipeThreshold) viewModel.shiftRange(if (total < 0) 1 else -1) },
                ) { _, dragAmount -> total += dragAmount }
            }

            if (ui.groups.isEmpty() && ui.inboxCount == 0) {
                Box(Modifier.fillMaxSize().then(swipeModifier)) { EmptyRange(ui.range, selectedDate, today) }
                return@Column
            }

            val anytimeRows = ui.groups.firstOrNull { it.key == GroupKey.Anytime }?.rows.orEmpty()
            var localAnytime by remember { mutableStateOf(anytimeRows) }
            LaunchedEffect(anytimeRows) { localAnytime = anytimeRows }

            val listState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
                val mutable = localAnytime.toMutableList()
                val fromIndex = mutable.indexOfFirst { anytimeKey(it) == from.key }
                val toIndex = mutable.indexOfFirst { anytimeKey(it) == to.key }
                if (fromIndex >= 0 && toIndex >= 0) {
                    mutable.add(toIndex, mutable.removeAt(fromIndex))
                    localAnytime = mutable
                }
            }

            LazyColumn(Modifier.fillMaxWidth().weight(1f).then(swipeModifier), state = listState) {
                if (ui.inboxCount > 0) {
                    item(key = "inbox-entry") {
                        InboxEntryRow(ui.inboxCount, onOpenInbox, Modifier.animateItem())
                    }
                }
                ui.groups.forEach { group ->
                    val expanded = group.key in expandedGroups
                    val visibleRows = if (expanded) group.rows else group.rows.dropLast(group.hiddenCount)
                    item(key = "header-${group.key}") {
                        GroupHeader(group, ui.range.granularity, ui.today, Modifier.animateItem())
                    }
                    if (group.key == GroupKey.Anytime && ui.range.granularity == Granularity.DAY) {
                        itemsIndexed(localAnytime, key = { _, row -> anytimeKey(row) }) { index, row ->
                            ReorderableItem(reorderableState, key = anytimeKey(row)) { _ ->
                                SectionCardItem(index, localAnytime.size) {
                                    AgendaRowContent(
                                        row = row,
                                        state = ui,
                                        viewModel = viewModel,
                                        onOpenItem = onOpenItem,
                                        onOpenDuplicate = { duplicateSheet = it },
                                        onDelete = ::deleteWithUndo,
                                        withUndo = ::withUndo,
                                        seriesExpanded = row.item.id in expandedSeries,
                                        showTime = false,
                                        modifier = Modifier.longPressDraggableHandle(
                                            onDragStarted = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDragStopped = {
                                                viewModel.persistAnytimeOrder(localAnytime.map { it.item.id })
                                            },
                                        ),
                                    )
                                }
                            }
                        }
                    } else {
                        val count = visibleRows.size + if (group.hiddenCount > 0) 1 else 0
                        itemsIndexed(visibleRows, key = { _, row -> "${group.key}-${row.item.id}" }) { index, row ->
                            SectionCardItem(index, count, Modifier.animateItem()) {
                                AgendaRowContent(
                                    row = row,
                                    state = ui,
                                    viewModel = viewModel,
                                    onOpenItem = onOpenItem,
                                    onOpenDuplicate = { duplicateSheet = it },
                                    onDelete = ::deleteWithUndo,
                                    withUndo = ::withUndo,
                                    seriesExpanded = row.item.id in expandedSeries,
                                    showTime = group.key != GroupKey.Anytime,
                                    overdue = group.key == GroupKey.Overdue,
                                )
                            }
                        }
                        // Rule 5: nothing is hidden without a visible count
                        if (group.hiddenCount > 0) {
                            item(key = "more-${group.key}") {
                                SectionCardItem(count - 1, count, Modifier.animateItem()) {
                                    MoreRow(
                                        hidden = group.hiddenCount,
                                        expanded = expanded,
                                        onClick = { viewModel.toggleGroup(group.key) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    duplicateSheet?.let { dup ->
        val mergedText = stringResource(Res.string.dup_merged)
        DuplicateSheet(
            duplicate = dup,
            today = today,
            onDismiss = { duplicateSheet = null },
            onMerge = {
                duplicateSheet = null
                withUndo(mergedText, { viewModel.mergeDuplicates(dup.others) }, { viewModel.restoreAll(dup.others) })
            },
        )
    }
}

private enum class HeaderKind { NONE, STRIP, PILLED_WEEK, GRID }

private fun anytimeKey(row: AgendaRow): String = "any-${row.item.id}"

@Composable
private fun RangeSwitcher(selected: Granularity, onSelect: (Granularity) -> Unit) {
    ConnectedButtonGroup(
        count = Granularity.entries.size,
        selectedIndex = selected.ordinal,
        onSelect = { onSelect(Granularity.entries[it]) },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) { index, _ ->
        Text(
            stringResource(
                when (Granularity.entries[index]) {
                    Granularity.DAY -> Res.string.range_day
                    Granularity.WEEK -> Res.string.range_week
                    Granularity.MONTH -> Res.string.range_month
                    Granularity.ALL -> Res.string.range_all
                },
            ),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun weekTitle(start: LocalDate, monthNames: List<String>): String {
    val end = start.plus(6, DateTimeUnit.DAY)
    val startMonth = monthNames[start.month.number - 1].take(3)
    val endMonth = monthNames[end.month.number - 1].take(3)
    return if (start.month == end.month) "$startMonth ${start.day}–${end.day}"
    else "$startMonth ${start.day} – $endMonth ${end.day}"
}

/** One renderer for every row shape the range builder emits. */
@Composable
private fun AgendaRowContent(
    row: AgendaRow,
    state: AgendaUiState,
    viewModel: AgendaViewModel,
    onOpenItem: (Item) -> Unit,
    onOpenDuplicate: (AgendaRow.Duplicate) -> Unit,
    onDelete: (Item) -> Unit,
    withUndo: (String, () -> Unit, () -> Unit) -> Unit,
    seriesExpanded: Boolean,
    showTime: Boolean,
    modifier: Modifier = Modifier,
    overdue: Boolean = false,
) {
    val use24h = LocalSettings.current.use24h
    val today = state.today
    val item = row.item

    if (row is AgendaRow.Series) {
        SeriesRow(row, state, viewModel, onOpenItem, onDelete, withUndo, seriesExpanded, modifier)
        return
    }

    val timeText = when (row) {
        is AgendaRow.Single -> if (showTime) row.time?.let { timeLabel(it, use24h) } else null
        is AgendaRow.Span -> row.dayIndex?.let { stringResource(Res.string.span_day_of, it, row.dayCount) }
            ?: "${dateLabel(row.first, today)} – ${dateLabel(row.last, today)}"
        is AgendaRow.Duplicate -> if (showTime) item.dueLocalTime?.let { timeLabel(it, use24h) } else null
        is AgendaRow.Series -> null
    }
    // an occurrence of a repeat: the checkbox marks that day, never the whole series
    val occurrence = (row as? AgendaRow.Single)?.takeIf { item.recurrence != null && it.date != null }
    ItemRow(
        item = if (occurrence != null) item.copy(completed = occurrence.done) else item,
        today = today,
        timeText = timeText,
        dateText = if (overdue) item.dueLocalDate?.let { dateLabel(it, today) } else null,
        badge = (row as? AgendaRow.Duplicate)?.let { "×${it.count}" },
        badgeDescription = (row as? AgendaRow.Duplicate)?.let { stringResource(Res.string.duplicate_copies, it.count) },
        onToggleComplete = when {
            occurrence != null && item.type == ItemType.TASK -> {
                {
                    if (occurrence.done) viewModel.clearOccurrence(item.id, occurrence.date!!)
                    else viewModel.completeOccurrence(item.id, occurrence.date!!)
                }
            }
            item.type == ItemType.TASK -> {
                { viewModel.toggleComplete(item) }
            }
            else -> null
        },
        onDelete = { onDelete(item) },
        onRename = { viewModel.rename(item, it) },
        onReschedule = if (item.type == ItemType.TASK && occurrence == null) {
            { viewModel.reschedule(item, it) }
        } else null,
        onOpen = if (row is AgendaRow.Duplicate) {
            { onOpenDuplicate(row) }
        } else {
            { onOpenItem(item) }
        },
        modifier = modifier,
    )
}

/**
 * A rolled-up repeat. Week shows the dot strip and completes per day; Month and All show
 * the rule with a count and expand the occurrences inline. Long-press offers skip / end.
 */
@Composable
private fun SeriesRow(
    row: AgendaRow.Series,
    state: AgendaUiState,
    viewModel: AgendaViewModel,
    onOpenItem: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    withUndo: (String, () -> Unit, () -> Unit) -> Unit,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val item = row.item
    val today = state.today
    val week = state.range.granularity == Granularity.WEEK
    val rule = item.recurrence?.let { recurrenceLabel(it) }.orEmpty()
    val doneCount = row.doneMask?.count { it } ?: 0
    val supporting = when (state.range.granularity) {
        Granularity.WEEK -> "$rule · ${stringResource(Res.string.series_done_of, doneCount, row.occurrencesInRange)}"
        Granularity.MONTH -> "$rule · ${stringResource(Res.string.series_count_month, row.occurrencesInRange)}"
        else -> "$rule · ${stringResource(Res.string.series_next, dateLabel(row.nextDue, today))}"
    }
    var menuOpen by remember { mutableStateOf(false) }
    val skippedText = stringResource(Res.string.occurrence_skipped)
    val endedText = stringResource(Res.string.series_ended)
    val completeDayText = stringResource(Res.string.series_complete_day)
    val description = "${item.title}, $supporting"

    Column(modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        Box {
            ItemRow(
                item = item,
                today = today,
                timeText = supporting,
                leadingIcon = Icons.Outlined.Repeat,
                // Rule 6: the checkbox completes the next occurrence only
                onToggleComplete = { viewModel.completeOccurrence(item.id, row.nextDue) },
                onDelete = { onDelete(item) },
                onRename = { viewModel.rename(item, it) },
                onLongClick = { menuOpen = true },
                onOpen = { onOpenItem(item) },
                trailing = if (week) {
                    {
                        DotStrip(
                            start = state.range.start ?: today,
                            mask = row.doneMask.orEmpty(),
                            dates = row.dates,
                            dayLabel = completeDayText,
                            onToggle = { date, done ->
                                if (done) viewModel.clearOccurrence(item.id, date) else viewModel.completeOccurrence(item.id, date)
                            },
                        )
                    }
                } else {
                    {
                        IconButton(onClick = { viewModel.toggleSeries(item.id) }) {
                            Icon(
                                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.series_skip)) },
                    onClick = {
                        menuOpen = false
                        withUndo(skippedText, { viewModel.skipOccurrence(item.id, row.nextDue) }) {
                            viewModel.clearOccurrence(item.id, row.nextDue)
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.series_end)) },
                    onClick = {
                        menuOpen = false
                        withUndo(endedText, { viewModel.endSeries(item) }) { viewModel.restoreItem(item) }
                    },
                )
            }
        }
        if (!week) {
            AnimatedVisibility(visible = expanded, enter = expandEnter(), exit = expandExit()) {
                Column(Modifier.padding(start = 56.dp, end = 16.dp, bottom = 8.dp)) {
                    row.dates.forEach { date ->
                        val done = date in row.doneDates
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = done,
                                onCheckedChange = {
                                    if (done) viewModel.clearOccurrence(item.id, date) else viewModel.completeOccurrence(item.id, date)
                                },
                            )
                            Text(dateLabel(date, today), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

/** Seven dots, one per day of the week; tapping a dot completes that day. 48dp tall for touch. */
@Composable
private fun DotStrip(
    start: LocalDate,
    mask: List<Boolean>,
    dates: List<LocalDate>,
    dayLabel: String,
    onToggle: (LocalDate, Boolean) -> Unit,
) {
    val actions = (0..6).mapNotNull { offset ->
        val date = start.plus(offset, DateTimeUnit.DAY)
        if (date in dates) CustomAccessibilityAction("$dayLabel ${date.dayOfWeek.name.lowercase()}") {
            onToggle(date, mask.getOrNull(offset) == true); true
        } else null
    }
    Row(
        Modifier.height(48.dp).semantics { customActions = actions },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(7) { offset ->
            val date = start.plus(offset, DateTimeUnit.DAY)
            val active = date in dates
            val done = mask.getOrNull(offset) == true
            Box(
                Modifier
                    .size(width = 16.dp, height = 48.dp)
                    .clickable(enabled = active) { onToggle(date, done) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .let { m ->
                            when {
                                done -> m.background(MaterialTheme.colorScheme.primary, CircleShape)
                                active -> m.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                else -> m
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun MoreRow(hidden: Int, expanded: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (expanded) stringResource(Res.string.fewer_rows) else stringResource(Res.string.more_rows, hidden),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DuplicateSheet(
    duplicate: AgendaRow.Duplicate,
    today: LocalDate,
    onDismiss: () -> Unit,
    onMerge: () -> Unit,
) {
    val all = listOf(duplicate.primary) + duplicate.others
    val nowMillis = remember { Clock.System.now().toEpochMilliseconds() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(
                stringResource(Res.string.dup_title, all.size),
                style = MaterialTheme.typography.titleMediumEmphasized,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            all.forEach { copy ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(copy.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(Res.string.inbox_captured, relativeAge(nowMillis - copy.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                // ponytail: "keep both" just closes the sheet; the pair keeps merging in the list
                OutlinedButton(onClick = onDismiss) { Text(stringResource(Res.string.dup_keep)) }
                FilledTonalButton(onClick = onMerge) { Text(stringResource(Res.string.dup_merge)) }
            }
        }
    }
}

@Composable
private fun GroupHeader(group: AgendaGroup, granularity: Granularity, today: LocalDate, modifier: Modifier = Modifier) {
    val key = group.key
    val label = when (key) {
        GroupKey.Overdue -> stringResource(Res.string.section_overdue)
        GroupKey.Series -> stringResource(
            if (granularity == Granularity.WEEK) Res.string.section_repeating_week else Res.string.section_series,
        )
        GroupKey.Anytime -> stringResource(Res.string.section_anytime)
        is GroupKey.TimeOfDay -> stringResource(
            when (key.section) {
                DaySection.MORNING -> Res.string.section_morning
                DaySection.AFTERNOON -> Res.string.section_afternoon
                DaySection.EVENING -> Res.string.section_evening
            },
        )
        is GroupKey.Day -> {
            val weekdays = stringArrayResource(Res.array.weekdays_full)
            "${weekdays[key.date.dayOfWeek.isoDayNumber - 1].take(3)} ${key.date.day}"
        }
        is GroupKey.Horizon -> stringResource(
            when (key.bucket) {
                HorizonBucket.TODAY -> Res.string.date_today
                HorizonBucket.THIS_WEEK -> Res.string.triage_this_week
                HorizonBucket.LATER -> Res.string.horizon_later
                HorizonBucket.SOMEDAY -> Res.string.triage_someday
            },
        )
    }
    // counts on the wide ranges; the day range reads fine without them
    val counted = if (granularity == Granularity.DAY || key == GroupKey.Series) label else "$label · ${group.rows.size}"
    val hiddenOccurrences = if (key == GroupKey.Series) {
        group.rows.filterIsInstance<AgendaRow.Series>().sumOf { it.occurrencesInRange - 1 }
    } else 0
    val isToday = key is GroupKey.Day && key.date == today
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (isToday) "${stringResource(Res.string.date_today)} · $label" else counted,
            style = MaterialTheme.typography.titleSmall,
            color = when {
                key == GroupKey.Overdue -> MaterialTheme.colorScheme.error
                isToday -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f),
        )
        if (hiddenOccurrences > 0) {
            Text(
                stringResource(Res.string.series_hidden, hiddenOccurrences),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InboxEntryRow(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            Modifier.defaultMinSize(minHeight = 56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(Res.string.inbox_waiting, count),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyRange(range: AgendaRange, selected: LocalDate, today: LocalDate) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Celebration,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (range.granularity == Granularity.DAY && selected == today || range.granularity == Granularity.ALL) {
                stringResource(Res.string.today_empty)
            } else {
                stringResource(Res.string.calendar_nothing, dateLabel(selected, today))
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun WeekdayHeader(firstDayOfWeekIso: Int) {
    val names = stringArrayResource(Res.array.weekdays_full)
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        repeat(7) { offset ->
            val iso = ((firstDayOfWeekIso - 1 + offset) % 7)
            Text(
                names[iso].take(1),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Week range header: the seven days from [start], drawn as one pill. */
@Composable
private fun PilledWeek(
    start: LocalDate,
    today: LocalDate,
    hasContent: (LocalDate) -> Boolean,
    onSelect: (LocalDate) -> Unit,
    onLongClick: (LocalDate) -> Unit,
) {
    val names = stringArrayResource(Res.array.weekdays_full)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.largeIncreased,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { offset ->
                    val date = start.plus(offset, DateTimeUnit.DAY)
                    Text(
                        names[date.dayOfWeek.isoDayNumber - 1].take(1),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (date == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { offset ->
                    val date = start.plus(offset, DateTimeUnit.DAY)
                    Box(Modifier.weight(1f)) {
                        DayCell(
                            date = date,
                            inMonth = true,
                            isSelected = date == today,
                            isToday = date == today,
                            hasContent = hasContent(date),
                            onClick = { onSelect(date) },
                            onLongClick = { onLongClick(date) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The custom day cell, kept on purpose: a 40dp circle, filled primary when selected, with a
 * pill under the selected day and a dot under days that have anything on them.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    hasContent: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.day.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
        Box(Modifier.height(6.dp), contentAlignment = Alignment.Center) {
            when {
                isSelected -> Box(
                    Modifier.width(14.dp).height(4.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
                )
                hasContent -> Box(
                    Modifier.size(5.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
                )
            }
        }
    }
}
