package com.tina.app.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.tina.app.LocalSettings
import com.tina.app.capture.typeLabel
import com.tina.app.data.Item
import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import com.tina.app.resources.Res
import com.tina.app.resources.back
import com.tina.app.resources.cancel
import com.tina.app.resources.delete
import com.tina.app.resources.detail_completed
import com.tina.app.resources.detail_date
import com.tina.app.resources.detail_notes
import com.tina.app.resources.detail_priority
import com.tina.app.resources.detail_reminder
import com.tina.app.resources.detail_tags
import com.tina.app.resources.detail_tags_hint
import com.tina.app.resources.detail_time
import com.tina.app.resources.details
import com.tina.app.resources.ok
import com.tina.app.resources.pr_high
import com.tina.app.resources.pr_low
import com.tina.app.resources.pr_medium
import com.tina.app.resources.pr_none
import com.tina.app.resources.reminder_at_time
import com.tina.app.resources.reminder_hour_before
import com.tina.app.resources.reminder_min_before
import com.tina.app.resources.reminder_off
import com.tina.app.ui.dateLabel
import com.tina.app.ui.sharedItemTitle
import com.tina.app.ui.timeLabel
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val REMINDER_OPTIONS = listOf(null, 0, 5, 10, 15, 30, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    itemId: Long,
    onBack: () -> Unit,
    viewModel: DetailViewModel = koinViewModel(key = "detail-$itemId") { parametersOf(itemId) },
) {
    val item by viewModel.item.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(Res.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.delete(onDeleted = onBack) }) {
                        Icon(Icons.Outlined.Delete, stringResource(Res.string.delete))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val current = item ?: return@Scaffold
        DetailContent(
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
private fun DetailContent(item: Item, viewModel: DetailViewModel, modifier: Modifier = Modifier) {
    val tz = TimeZone.currentSystemDefault()
    val today = remember { Clock.System.now().toLocalDateTime(tz).date }
    val use24h = LocalSettings.current.use24h
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var titleText by remember(item.id) { mutableStateOf(item.title) }
    var tagsText by remember(item.id) { mutableStateOf(item.tags.joinToString(", ")) }
    var bodyText by remember(item.id) { mutableStateOf(item.body ?: "") }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        OutlinedTextField(
            value = titleText,
            onValueChange = {
                titleText = it
                if (it.isNotBlank()) viewModel.setTitle(it.trim())
            },
            modifier = Modifier.fillMaxWidth().sharedItemTitle(item.id),
            textStyle = MaterialTheme.typography.titleLarge,
        )

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ItemType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = item.type == type,
                    onClick = { viewModel.setType(type) },
                    shape = SegmentedButtonDefaults.itemShape(index, ItemType.entries.size),
                ) { Text(typeLabel(type)) }
            }
        }

        if (item.type == ItemType.TASK || item.type == ItemType.INBOX) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { showDatePicker = true },
                    leadingIcon = { Icon(Icons.Outlined.Today, contentDescription = null) },
                    label = {
                        Text(
                            item.dueLocalDate?.let { dateLabel(it, today) }
                                ?: stringResource(Res.string.detail_date),
                        )
                    },
                    trailingIcon = if (item.dueDate != null) {
                        {
                            IconButton(onClick = { viewModel.setDate(null) }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    } else null,
                )
                AssistChip(
                    onClick = { showTimePicker = true },
                    leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                    label = {
                        Text(
                            item.dueLocalTime?.let { timeLabel(it, use24h) }
                                ?: stringResource(Res.string.detail_time),
                        )
                    },
                    trailingIcon = if (item.dueTime != null) {
                        {
                            IconButton(onClick = { viewModel.setTime(null) }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    } else null,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.detail_priority), style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Priority.entries.forEachIndexed { index, priority ->
                        SegmentedButton(
                            selected = item.priority == priority,
                            onClick = { viewModel.setPriority(priority) },
                            shape = SegmentedButtonDefaults.itemShape(index, Priority.entries.size),
                        ) {
                            Text(
                                when (priority) {
                                    Priority.NONE -> stringResource(Res.string.pr_none)
                                    Priority.LOW -> stringResource(Res.string.pr_low)
                                    Priority.MEDIUM -> stringResource(Res.string.pr_medium)
                                    Priority.HIGH -> stringResource(Res.string.pr_high)
                                },
                            )
                        }
                    }
                }
            }

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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.completed, onCheckedChange = { viewModel.toggleCompleted() })
                Text(stringResource(Res.string.detail_completed))
            }
        }

        OutlinedTextField(
            value = tagsText,
            onValueChange = {
                tagsText = it
                viewModel.setTags(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.detail_tags)) },
            placeholder = { Text(stringResource(Res.string.detail_tags_hint)) },
        )

        OutlinedTextField(
            value = bodyText,
            onValueChange = {
                bodyText = it
                viewModel.setBody(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.detail_notes)) },
            minLines = 4,
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = item.dueLocalDate?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.setDate(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                    }
                    showDatePicker = false
                }) { Text(stringResource(Res.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(Res.string.cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = item.dueLocalTime?.hour ?: 9,
            initialMinute = item.dueLocalTime?.minute ?: 0,
            is24Hour = use24h,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setTime(LocalTime(timePickerState.hour, timePickerState.minute))
                    showTimePicker = false
                }) { Text(stringResource(Res.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(Res.string.cancel)) }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }
}
