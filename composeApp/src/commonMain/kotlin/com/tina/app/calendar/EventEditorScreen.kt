package com.tina.app.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.tina.app.LocalSettings
import com.tina.app.data.Item
import com.tina.app.data.RecurrenceRule
import com.tina.app.data.byDayCode
import com.tina.app.data.parseRrule
import com.tina.app.resources.Res
import com.tina.app.resources.back
import com.tina.app.resources.cancel
import com.tina.app.resources.color_default
import com.tina.app.resources.delete
import com.tina.app.resources.detail_notes
import com.tina.app.resources.detail_reminder
import com.tina.app.resources.event_all_day
import com.tina.app.resources.event_color
import com.tina.app.resources.event_ends
import com.tina.app.resources.event_repeat
import com.tina.app.resources.event_starts
import com.tina.app.resources.ok
import com.tina.app.resources.reminder_at_time
import com.tina.app.resources.reminder_hour_before
import com.tina.app.resources.reminder_min_before
import com.tina.app.resources.reminder_off
import com.tina.app.resources.repeat_custom
import com.tina.app.resources.repeat_daily
import com.tina.app.resources.repeat_every_n
import com.tina.app.resources.repeat_monthly
import com.tina.app.resources.repeat_none
import com.tina.app.resources.repeat_weekly
import com.tina.app.resources.repeat_yearly
import com.tina.app.resources.weekdays_full
import com.tina.app.ui.dateLabel
import com.tina.app.ui.timeLabel
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val REMINDER_OPTIONS = listOf(null, 0, 5, 10, 15, 30, 60)
private val COLOR_PRESETS = listOf(
    0xFFD32F2FL, 0xFFF57C00L, 0xFFFBC02DL, 0xFF388E3CL, 0xFF1976D2L, 0xFF7B1FA2L,
)

private enum class PickerTarget { START_DATE, START_TIME, END_DATE, END_TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(
    itemId: Long,
    onBack: () -> Unit,
    viewModel: EventEditorViewModel = koinViewModel { parametersOf(itemId) },
) {
    val item by viewModel.item.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(item?.title.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.delete(onDeleted = onBack) }) {
                        Icon(Icons.Filled.Delete, stringResource(Res.string.delete))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val current = item ?: return@Scaffold
        EventEditorContent(
            item = current,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventEditorContent(item: Item, viewModel: EventEditorViewModel, modifier: Modifier = Modifier) {
    val tz = TimeZone.currentSystemDefault()
    val use24h = LocalSettings.current.use24h
    val today = remember { Clock.System.now().toLocalDateTime(tz).date }
    var titleText by remember(item.id) { mutableStateOf(item.title) }
    var bodyText by remember(item.id) { mutableStateOf(item.body ?: "") }
    var picker by remember { mutableStateOf<PickerTarget?>(null) }
    var showCustomRepeat by remember { mutableStateOf(false) }

    val start = item.startAt?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(tz) }
    val end = item.endAt?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(tz) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        OutlinedTextField(
            value = titleText,
            onValueChange = {
                titleText = it
                if (it.isNotBlank()) viewModel.setTitle(it.trim())
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.titleLarge,
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.event_all_day),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = item.allDay, onCheckedChange = viewModel::setAllDay)
        }

        if (start != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.event_starts), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { picker = PickerTarget.START_DATE },
                        leadingIcon = { Icon(Icons.Filled.Today, contentDescription = null) },
                        label = { Text(dateLabel(start.date, today)) },
                    )
                    if (!item.allDay) {
                        AssistChip(
                            onClick = { picker = PickerTarget.START_TIME },
                            leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                            label = { Text(timeLabel(start.time, use24h)) },
                        )
                    }
                }
            }
        }

        if (end != null && !item.allDay) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.event_ends), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { picker = PickerTarget.END_DATE },
                        leadingIcon = { Icon(Icons.Filled.Today, contentDescription = null) },
                        label = { Text(dateLabel(end.date, today)) },
                    )
                    AssistChip(
                        onClick = { picker = PickerTarget.END_TIME },
                        leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                        label = { Text(timeLabel(end.time, use24h)) },
                    )
                }
            }
        }

        RepeatSection(
            rrule = item.recurrence,
            onSelect = viewModel::setRrule,
            onCustom = { showCustomRepeat = true },
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.detail_reminder), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                REMINDER_OPTIONS.forEach { minutes ->
                    FilterChip(
                        selected = item.reminderOffsetMinutes == minutes,
                        onClick = { viewModel.setReminder(minutes) },
                        label = {
                            Text(
                                when (minutes) {
                                    null -> stringResource(Res.string.reminder_off)
                                    0 -> stringResource(Res.string.reminder_at_time)
                                    60 -> stringResource(Res.string.reminder_hour_before)
                                    else -> stringResource(Res.string.reminder_min_before, minutes)
                                },
                            )
                        },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.event_color), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ColorSwatch(
                    color = null,
                    selected = item.color == null,
                    onClick = { viewModel.setColor(null) },
                )
                COLOR_PRESETS.forEach { preset ->
                    ColorSwatch(
                        color = preset,
                        selected = item.color == preset,
                        onClick = { viewModel.setColor(preset) },
                    )
                }
            }
        }

        OutlinedTextField(
            value = bodyText,
            onValueChange = {
                bodyText = it
                viewModel.setBody(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.detail_notes)) },
            minLines = 3,
        )
    }

    when (picker) {
        PickerTarget.START_DATE, PickerTarget.END_DATE -> {
            val initial = if (picker == PickerTarget.START_DATE) start?.date else end?.date
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = initial?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
            )
            DatePickerDialog(
                onDismissRequest = { picker = null },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                            if (picker == PickerTarget.START_DATE) viewModel.setStartDate(date)
                            else viewModel.setEndDate(date)
                        }
                        picker = null
                    }) { Text(stringResource(Res.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { picker = null }) { Text(stringResource(Res.string.cancel)) }
                },
            ) {
                DatePicker(state = pickerState)
            }
        }
        PickerTarget.START_TIME, PickerTarget.END_TIME -> {
            val initial = if (picker == PickerTarget.START_TIME) start?.time else end?.time
            val timeState = rememberTimePickerState(
                initialHour = initial?.hour ?: 9,
                initialMinute = initial?.minute ?: 0,
                is24Hour = use24h,
            )
            AlertDialog(
                onDismissRequest = { picker = null },
                confirmButton = {
                    TextButton(onClick = {
                        val time = kotlinx.datetime.LocalTime(timeState.hour, timeState.minute)
                        if (picker == PickerTarget.START_TIME) viewModel.setStartTime(time)
                        else viewModel.setEndTime(time)
                        picker = null
                    }) { Text(stringResource(Res.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { picker = null }) { Text(stringResource(Res.string.cancel)) }
                },
                text = { TimePicker(state = timeState) },
            )
        }
        null -> Unit
    }

    if (showCustomRepeat) {
        CustomRepeatDialog(
            initial = item.recurrence?.let { parseRrule(it) },
            onDismiss = { showCustomRepeat = false },
            onConfirm = { rrule ->
                viewModel.setRrule(rrule)
                showCustomRepeat = false
            },
        )
    }
}

@Composable
private fun ColorSwatch(color: Long?, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .background(color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary, CircleShape)
            .let {
                if (selected) it.border(3.dp, MaterialTheme.colorScheme.outline, CircleShape) else it
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (color == null && !selected) {
            Icon(
                Icons.Filled.Block,
                stringResource(Res.string.color_default),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RepeatSection(rrule: String?, onSelect: (String?) -> Unit, onCustom: () -> Unit) {
    val parsed = rrule?.let { parseRrule(it) }
    val isSimple = parsed != null && parsed.interval == 1 && parsed.byDay.isEmpty() &&
        parsed.count == null && parsed.until == null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.event_repeat), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = rrule == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(Res.string.repeat_none)) },
            )
            FilterChip(
                selected = isSimple && parsed.freq == RecurrenceRule.Freq.DAILY,
                onClick = { onSelect("FREQ=DAILY") },
                label = { Text(stringResource(Res.string.repeat_daily)) },
            )
            FilterChip(
                selected = isSimple && parsed.freq == RecurrenceRule.Freq.WEEKLY,
                onClick = { onSelect("FREQ=WEEKLY") },
                label = { Text(stringResource(Res.string.repeat_weekly)) },
            )
            FilterChip(
                selected = isSimple && parsed.freq == RecurrenceRule.Freq.MONTHLY,
                onClick = { onSelect("FREQ=MONTHLY") },
                label = { Text(stringResource(Res.string.repeat_monthly)) },
            )
            FilterChip(
                selected = isSimple && parsed.freq == RecurrenceRule.Freq.YEARLY,
                onClick = { onSelect("FREQ=YEARLY") },
                label = { Text(stringResource(Res.string.repeat_yearly)) },
            )
            FilterChip(
                selected = rrule != null && !isSimple,
                onClick = onCustom,
                label = { Text(stringResource(Res.string.repeat_custom)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRepeatDialog(
    initial: RecurrenceRule?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var interval by remember { mutableStateOf(initial?.interval ?: 1) }
    var freq by remember { mutableStateOf(initial?.freq ?: RecurrenceRule.Freq.WEEKLY) }
    var byDay by remember { mutableStateOf(initial?.byDay ?: emptySet()) }
    val weekdayNames = stringArrayResource(Res.array.weekdays_full)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val parts = mutableListOf("FREQ=${freq.name}")
                if (interval > 1) parts += "INTERVAL=$interval"
                if (freq == RecurrenceRule.Freq.WEEKLY && byDay.isNotEmpty()) {
                    parts += "BYDAY=" + byDay.sortedBy { it.isoDayNumber }.joinToString(",") { byDayCode(it) }
                }
                onConfirm(parts.joinToString(";"))
            }) { Text(stringResource(Res.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
        title = { Text(stringResource(Res.string.repeat_custom)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.repeat_every_n), Modifier.weight(1f))
                    IconButton(onClick = { if (interval > 1) interval-- }) {
                        Icon(Icons.Filled.Remove, contentDescription = null)
                    }
                    Text(interval.toString(), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { interval++ }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    RecurrenceRule.Freq.entries.forEachIndexed { index, f ->
                        SegmentedButton(
                            selected = freq == f,
                            onClick = { freq = f },
                            shape = SegmentedButtonDefaults.itemShape(index, RecurrenceRule.Freq.entries.size),
                        ) {
                            Text(
                                when (f) {
                                    RecurrenceRule.Freq.DAILY -> stringResource(Res.string.repeat_daily)
                                    RecurrenceRule.Freq.WEEKLY -> stringResource(Res.string.repeat_weekly)
                                    RecurrenceRule.Freq.MONTHLY -> stringResource(Res.string.repeat_monthly)
                                    RecurrenceRule.Freq.YEARLY -> stringResource(Res.string.repeat_yearly)
                                },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                if (freq == RecurrenceRule.Freq.WEEKLY) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            FilterChip(
                                selected = day in byDay,
                                onClick = {
                                    byDay = if (day in byDay) byDay - day else byDay + day
                                },
                                label = { Text(weekdayNames[day.isoDayNumber - 1].take(2)) },
                            )
                        }
                    }
                }
            }
        },
    )
}
