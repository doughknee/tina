package com.tina.app.agenda

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.tina.app.resources.calendar_jump_today
import com.tina.app.resources.calendar_nothing
import com.tina.app.resources.calendar_view_month
import com.tina.app.resources.calendar_view_week
import com.tina.app.resources.deleted
import com.tina.app.resources.duplicate_copies
import com.tina.app.resources.inbox_waiting
import com.tina.app.resources.months_full
import com.tina.app.resources.search
import com.tina.app.resources.section_afternoon
import com.tina.app.resources.section_anytime
import com.tina.app.resources.section_evening
import com.tina.app.resources.section_morning
import com.tina.app.resources.section_overdue
import com.tina.app.resources.section_series
import com.tina.app.resources.settings
import com.tina.app.resources.span_day_of
import com.tina.app.resources.today_empty
import com.tina.app.resources.undo
import com.tina.app.resources.weekdays_full
import com.tina.app.ui.ItemRow
import com.tina.app.ui.KeyBus
import com.tina.app.ui.KeyCommand
import com.tina.app.ui.SectionCardItem
import com.tina.app.ui.dateLabel
import com.tina.app.ui.recurrenceLabel
import com.tina.app.ui.rememberUndoWindow
import com.tina.app.ui.showUndo
import com.tina.app.ui.timeLabel
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
 * Agenda = Today + Calendar. One list for the selected date under a date header that
 * is a week strip or a month grid; the title toggles between them.
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
    var monthMode by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val undoWindow = rememberUndoWindow()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val deletedText = stringResource(Res.string.deleted)
    val undoText = stringResource(Res.string.undo)
    val monthNames = stringArrayResource(Res.array.months_full)
    val today = state?.today ?: selectedDate

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

    // keep the dot query in step with whichever header is on screen
    LaunchedEffect(monthState, weekState, monthMode) {
        if (monthMode) {
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

    fun jumpTo(date: LocalDate) {
        viewModel.select(date)
        scope.launch {
            if (monthMode) monthState.animateScrollToMonth(date.ym) else weekState.animateScrollToWeek(date)
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

    fun deleteWithUndo(item: Item) {
        viewModel.delete(item)
        scope.launch {
            if (snackbarHostState.showUndo(deletedText, undoText, undoWindow)) viewModel.undoDelete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val visibleMonth = if (monthMode) {
                        monthState.firstVisibleMonth.yearMonth
                    } else {
                        weekState.firstVisibleWeek.days[3].date.ym
                    }
                    Row(
                        Modifier.combinedClickable(onClick = { monthMode = !monthMode }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(monthNames[visibleMonth.month.number - 1], style = MaterialTheme.typography.titleLarge)
                        Icon(
                            if (monthMode) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            stringResource(if (monthMode) Res.string.calendar_view_week else Res.string.calendar_view_month),
                            Modifier.padding(start = 4.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    AnimatedVisibility(visible = selectedDate != today) {
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
            WeekdayHeader(firstDayOfWeekIso = settings.firstDayOfWeek.isoDayNumber)
            AnimatedContent(
                targetState = monthMode,
                transitionSpec = { (fadeIn() togetherWith fadeOut()).using(SizeTransform(clip = false)) },
                label = "date-header",
            ) { grid ->
                if (grid) {
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
                } else {
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
            }

            val ui = state ?: return@Column
            com.tina.app.notifications.ReminderPermissionBanner(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (ui.groups.isEmpty() && ui.inboxCount == 0) {
                EmptyDay(selectedDate, today)
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

            LazyColumn(Modifier.fillMaxWidth().weight(1f), state = listState) {
                if (ui.inboxCount > 0) {
                    item(key = "inbox-entry") {
                        InboxEntryRow(ui.inboxCount, onOpenInbox, Modifier.animateItem())
                    }
                }
                ui.groups.forEach { group ->
                    item(key = "header-${group.key}") {
                        GroupHeader(group.key, ui.today)
                    }
                    if (group.key == GroupKey.Anytime) {
                        itemsIndexed(localAnytime, key = { _, row -> anytimeKey(row) }) { index, row ->
                            ReorderableItem(reorderableState, key = anytimeKey(row)) { _ ->
                                SectionCardItem(index, localAnytime.size) {
                                    AgendaRowContent(
                                        row = row,
                                        today = ui.today,
                                        viewModel = viewModel,
                                        onOpenItem = onOpenItem,
                                        onDelete = ::deleteWithUndo,
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
                        itemsIndexed(group.rows, key = { _, row -> "${group.key}-${row.item.id}" }) { index, row ->
                            SectionCardItem(index, group.rows.size, Modifier.animateItem()) {
                                AgendaRowContent(
                                    row = row,
                                    today = ui.today,
                                    viewModel = viewModel,
                                    onOpenItem = onOpenItem,
                                    onDelete = ::deleteWithUndo,
                                    showTime = true,
                                    overdue = group.key == GroupKey.Overdue,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun anytimeKey(row: AgendaRow): String = "any-${row.item.id}"

/** One renderer for every row shape the range builder emits. */
@Composable
private fun AgendaRowContent(
    row: AgendaRow,
    today: LocalDate,
    viewModel: AgendaViewModel,
    onOpenItem: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    showTime: Boolean,
    modifier: Modifier = Modifier,
    overdue: Boolean = false,
) {
    val use24h = LocalSettings.current.use24h
    val item = row.item
    val timeText = when (row) {
        is AgendaRow.Single -> if (showTime) row.time?.let { timeLabel(it, use24h) } else null
        is AgendaRow.Series -> item.recurrence?.let { recurrenceLabel(it) }
        is AgendaRow.Span -> row.dayIndex?.let { stringResource(Res.string.span_day_of, it, row.dayCount) }
            ?: "${dateLabel(row.first, today)} – ${dateLabel(row.last, today)}"
        is AgendaRow.Duplicate -> if (showTime) item.dueLocalTime?.let { timeLabel(it, use24h) } else null
    }
    ItemRow(
        item = item,
        today = today,
        timeText = timeText,
        dateText = if (overdue) item.dueLocalDate?.let { dateLabel(it, today) } else null,
        badge = (row as? AgendaRow.Duplicate)?.let { "×${it.count}" },
        badgeDescription = (row as? AgendaRow.Duplicate)?.let { stringResource(Res.string.duplicate_copies, it.count) },
        onToggleComplete = if (item.type == ItemType.TASK) {
            { viewModel.toggleComplete(item) }
        } else null,
        onDelete = { onDelete(item) },
        onRename = { viewModel.rename(item, it) },
        onReschedule = if (item.type == ItemType.TASK) {
            { viewModel.reschedule(item, it) }
        } else null,
        onOpen = { onOpenItem(item) },
        modifier = modifier,
    )
}

@Composable
private fun GroupHeader(key: GroupKey, today: LocalDate) {
    val label = when (key) {
        GroupKey.Overdue -> stringResource(Res.string.section_overdue)
        GroupKey.Series -> stringResource(Res.string.section_series)
        GroupKey.Anytime -> stringResource(Res.string.section_anytime)
        is GroupKey.TimeOfDay -> stringResource(
            when (key.section) {
                DaySection.MORNING -> Res.string.section_morning
                DaySection.AFTERNOON -> Res.string.section_afternoon
                DaySection.EVENING -> Res.string.section_evening
            },
        )
        is GroupKey.Day -> dateLabel(key.date, today)
        is GroupKey.Horizon -> key.bucket.name.lowercase().replaceFirstChar { it.uppercase() }
    }
    Text(
        label,
        style = MaterialTheme.typography.titleSmall,
        color = if (key == GroupKey.Overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun InboxEntryRow(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
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
private fun EmptyDay(selected: LocalDate, today: LocalDate) {
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
            if (selected == today) {
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
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    CircleShape,
                ),
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
                    Modifier
                        .width(14.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
                )
                hasContent -> Box(
                    Modifier.size(5.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
                )
            }
        }
    }
}
