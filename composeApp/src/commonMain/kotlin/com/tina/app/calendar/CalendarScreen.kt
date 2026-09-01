package com.tina.app.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import com.tina.app.ui.rememberUndoWindow
import com.tina.app.ui.showUndo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.tina.app.resources.calendar_capture_hint
import com.tina.app.resources.calendar_jump_today
import com.tina.app.resources.calendar_nothing
import com.tina.app.resources.calendar_view_month
import com.tina.app.resources.calendar_view_week
import com.tina.app.resources.cancel
import com.tina.app.resources.deleted
import com.tina.app.resources.months_full
import com.tina.app.resources.ok
import com.tina.app.resources.settings
import com.tina.app.resources.undo
import com.tina.app.resources.weekdays_full
import com.tina.app.ui.ItemRow
import com.tina.app.ui.SectionCardItem
import com.tina.app.ui.timeLabel
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val LocalDate.ym: YearMonth get() = YearMonth(year, month)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onOpenSettings: () -> Unit,
    onOpenItem: (Item) -> Unit,
    onCaptureForDate: (LocalDate) -> Unit,
    viewModel: CalendarViewModel = koinViewModel(),
) {
    val settings = LocalSettings.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val agenda by viewModel.agenda.collectAsState()
    val dots by viewModel.dots.collectAsState()
    val today = viewModel.today
    var monthMode by remember { mutableStateOf(true) }
    var showJumpPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val undoWindow = rememberUndoWindow()
    val scope = rememberCoroutineScope()
    val deletedText = stringResource(Res.string.deleted)
    val undoText = stringResource(Res.string.undo)
    val monthNames = stringArrayResource(Res.array.months_full)

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

    // Keep the dot query in sync with what is on screen.
    LaunchedEffect(monthState) {
        snapshotFlow { monthState.firstVisibleMonth }.collect { month ->
            viewModel.setVisibleRange(
                month.yearMonth.firstDay.plus(-7, DateTimeUnit.DAY),
                month.yearMonth.lastDay.plus(7, DateTimeUnit.DAY),
            )
        }
    }

    fun jumpTo(date: LocalDate) {
        viewModel.select(date)
        scope.launch {
            if (monthMode) monthState.animateScrollToMonth(date.ym)
            else weekState.animateScrollToWeek(date)
        }
    }

    LaunchedEffect(Unit) {
        com.tina.app.ui.KeyBus.events.collect { command ->
            val current = viewModel.selectedDate.value
            when (command) {
                com.tina.app.ui.KeyCommand.LEFT -> jumpTo(current.plus(-1, DateTimeUnit.DAY))
                com.tina.app.ui.KeyCommand.RIGHT -> jumpTo(current.plus(1, DateTimeUnit.DAY))
                com.tina.app.ui.KeyCommand.UP -> jumpTo(current.plus(-7, DateTimeUnit.DAY))
                com.tina.app.ui.KeyCommand.DOWN -> jumpTo(current.plus(7, DateTimeUnit.DAY))
                else -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val visibleMonth = monthState.firstVisibleMonth.yearMonth
                    Text(
                        "${monthNames[visibleMonth.month.number - 1]} ${visibleMonth.year}",
                        modifier = Modifier.combinedClickableTitle { showJumpPicker = true },
                    )
                },
                actions = {
                    TextButton(onClick = { jumpTo(today) }) {
                        Text(stringResource(Res.string.calendar_jump_today))
                    }
                    IconButton(onClick = { monthMode = !monthMode }) {
                        Icon(
                            if (monthMode) Icons.Outlined.ViewWeek else Icons.Outlined.CalendarViewMonth,
                            stringResource(
                                if (monthMode) Res.string.calendar_view_week else Res.string.calendar_view_month,
                            ),
                        )
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

            if (monthMode) {
                HorizontalCalendar(
                    state = monthState,
                    dayContent = { day: CalendarDay ->
                        DayCell(
                            date = day.date,
                            inMonth = day.position == DayPosition.MonthDate,
                            isSelected = day.date == selectedDate,
                            isToday = day.date == today,
                            dotColors = dots[day.date].orEmpty(),
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
                            dotColors = dots[day.date].orEmpty(),
                            onClick = { viewModel.select(day.date) },
                            onLongClick = { onCaptureForDate(day.date) },
                        )
                    },
                )
            }

            HorizontalDivider(Modifier.padding(top = 4.dp))

            AgendaHeader(selectedDate)

            if (agenda.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        stringResource(
                            Res.string.calendar_nothing,
                            com.tina.app.ui.dateLabel(selectedDate, today),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(Res.string.calendar_capture_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    itemsIndexed(agenda, key = { _, it -> "${it.item.id}-${it.time}" }) { index, entry ->
                        SectionCardItem(index, agenda.size, Modifier.animateItem()) {
                            ItemRow(
                                item = entry.item,
                                today = today,
                                timeText = entry.time?.let { timeLabel(it, settings.use24h) },
                                onToggleComplete = if (entry.item.type == ItemType.TASK) {
                                    { viewModel.toggleComplete(entry.item) }
                                } else null,
                                onDelete = {
                                    viewModel.delete(entry.item)
                                    scope.launch {
                                        if (snackbarHostState.showUndo(deletedText, undoText, undoWindow)) viewModel.undoDelete()
                                    }
                                },
                                onRename = { viewModel.rename(entry.item, it) },
                                onReschedule = if (entry.item.type == ItemType.TASK) {
                                    { viewModel.reschedule(entry.item, it) }
                                } else null,
                                onOpen = { onOpenItem(entry.item) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showJumpPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { showJumpPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        jumpTo(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                    }
                    showJumpPicker = false
                }) { Text(stringResource(Res.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showJumpPicker = false }) { Text(stringResource(Res.string.cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Title needs a plain clickable; wrapped so the import list stays tidy. */
@Composable
private fun Modifier.combinedClickableTitle(onClick: () -> Unit): Modifier =
    this.then(Modifier.combinedClickableNoIndication(onClick))

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.combinedClickableNoIndication(onClick: () -> Unit): Modifier =
    this.then(Modifier.combinedClickable(onClick = onClick))

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

@Composable
private fun AgendaHeader(date: LocalDate) {
    val weekdays = stringArrayResource(Res.array.weekdays_full)
    val months = stringArrayResource(Res.array.months_full)
    Text(
        "${weekdays[date.dayOfWeek.isoDayNumber - 1]}, ${months[date.month.number - 1]} ${date.day}",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    dotColors: List<Long?>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .let { m ->
                when {
                    isSelected -> m.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    isToday -> m.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else -> m
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.day.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            if (isSelected && dotColors.isNotEmpty()) {
                // one calm pill instead of dots once the day is focused
                Box(
                    Modifier
                        .size(width = 12.dp, height = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.onPrimaryContainer,
                            RoundedCornerShape(2.dp),
                        ),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    dotColors.forEach { color ->
                        Box(
                            Modifier
                                .size(4.dp)
                                .background(
                                    color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                                    CircleShape,
                                ),
                        )
                    }
                }
            }
        }
    }
}
