package com.tina.app.capture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import com.tina.app.LocalSettings
import com.tina.app.resources.Res
import com.tina.app.resources.ai_refined
import com.tina.app.resources.app_title
import com.tina.app.resources.capture_placeholder
import com.tina.app.resources.capture_recent
import com.tina.app.resources.capture_save
import com.tina.app.resources.capture_try
import com.tina.app.resources.capture_try_1
import com.tina.app.resources.capture_try_2
import com.tina.app.resources.capture_try_3
import com.tina.app.resources.capture_voice
import com.tina.app.resources.captured
import com.tina.app.resources.time_day_ago
import com.tina.app.resources.time_hr_ago
import com.tina.app.resources.time_just_now
import com.tina.app.resources.time_min_ago
import com.tina.app.resources.chip_remove
import com.tina.app.resources.priority_high
import com.tina.app.resources.priority_low
import com.tina.app.resources.priority_medium
import com.tina.app.resources.settings
import com.tina.app.resources.type_event
import com.tina.app.resources.type_inbox
import com.tina.app.resources.type_note
import com.tina.app.resources.type_task
import com.tina.app.resources.undo
import com.tina.app.ui.dateLabel
import com.tina.app.ui.durationLabel
import com.tina.app.ui.recurrenceLabel
import com.tina.app.ui.timeLabel
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun typeLabel(type: ItemType): String = when (type) {
    ItemType.INBOX -> stringResource(Res.string.type_inbox)
    ItemType.TASK -> stringResource(Res.string.type_task)
    ItemType.EVENT -> stringResource(Res.string.type_event)
    ItemType.NOTE -> stringResource(Res.string.type_note)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onOpenSettings: () -> Unit = {},
    viewModel: CaptureViewModel = koinViewModel(),
) {
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val capturedText = stringResource(Res.string.captured)
    val undoText = stringResource(Res.string.undo)

    LaunchedEffect(Unit) {
        // let the field attach before requesting focus (desktop logs a warning otherwise)
        withFrameNanos { }
        focusRequester.requestFocus()
    }

    val refinedText = stringResource(Res.string.ai_refined)
    LaunchedEffect(Unit) {
        viewModel.refinedEvents.collect { original ->
            val result = snackbarHostState.showSnackbar(
                message = refinedText,
                actionLabel = undoText,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoRefinement(original)
        }
    }

    fun saveNow() {
        viewModel.save {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = capturedText,
                    actionLabel = undoText,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.undoLastSave()
            }
        }
    }

    val speech = rememberSpeechCapture { spoken ->
        viewModel.onTextChange(
            if (viewModel.text.isBlank()) spoken else "${viewModel.text} $spoken",
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(Res.string.settings))
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState, Modifier.imePadding())
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                TextField(
                    value = viewModel.text,
                    onValueChange = viewModel::onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            // physical keyboards: Enter saves, Shift+Enter makes a newline
                            if (event.key == Key.Enter &&
                                event.type == KeyEventType.KeyDown &&
                                !event.isShiftPressed
                            ) {
                                saveNow()
                                true
                            } else {
                                false
                            }
                        },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    placeholder = {
                        Text(
                            stringResource(Res.string.capture_placeholder),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(onSend = { saveNow() }),
                    maxLines = 6,
                    trailingIcon = {
                        Row {
                            if (speech.available) {
                                IconButton(onClick = speech.start) {
                                    Icon(Icons.Outlined.Mic, stringResource(Res.string.capture_voice))
                                }
                            }
                            FilledIconButton(onClick = { saveNow() }) {
                                Icon(Icons.AutoMirrored.Outlined.Send, stringResource(Res.string.capture_save))
                            }
                        }
                    },
                )

                CaptureChips(viewModel)

                // suggestions + recents only while idle; gone the instant typing starts
                AnimatedVisibility(
                    visible = viewModel.text.isBlank(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    CaptureEmptyState(viewModel)
                }
            }

            SaveBurst(
                trigger = viewModel.saveCount,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun CaptureEmptyState(viewModel: CaptureViewModel) {
    val recent by viewModel.recent.collectAsState()
    val use24h = LocalSettings.current.use24h
    val now = remember(recent) { Clock.System.now() }
    val today = remember(recent) { now.toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val suggestions = listOf(
        stringResource(Res.string.capture_try_1),
        stringResource(Res.string.capture_try_2),
        stringResource(Res.string.capture_try_3),
    )

    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text(
            stringResource(Res.string.capture_try).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { viewModel.prefill(suggestion) },
                    label = { Text(suggestion) },
                    icon = {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(Res.string.capture_recent).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            recent.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        when (item.type) {
                            ItemType.TASK -> Icons.Outlined.TaskAlt
                            ItemType.EVENT -> Icons.Outlined.Event
                            ItemType.NOTE -> Icons.Outlined.Description
                            ItemType.INBOX -> Icons.Outlined.Inbox
                        },
                        contentDescription = null,
                        Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        val schedule = when {
                            item.type == ItemType.EVENT && item.startAt != null -> {
                                val start = kotlin.time.Instant.fromEpochMilliseconds(item.startAt!!)
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                                listOfNotNull(
                                    dateLabel(start.date, today),
                                    if (item.allDay) null else timeLabel(start.time, use24h),
                                ).joinToString(" ")
                            }
                            item.dueLocalDate != null -> dateLabel(item.dueLocalDate!!, today)
                            else -> null
                        }
                        Text(
                            listOfNotNull(
                                typeLabel(item.type),
                                schedule,
                                relativeAge(now.toEpochMilliseconds() - item.createdAt),
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun relativeAge(ageMillis: Long): String {
    val minutes = ageMillis / 60_000
    return when {
        minutes < 1 -> stringResource(Res.string.time_just_now)
        minutes < 60 -> stringResource(Res.string.time_min_ago, minutes.toString())
        minutes < 60 * 24 -> stringResource(Res.string.time_hr_ago, (minutes / 60).toString())
        else -> stringResource(Res.string.time_day_ago, (minutes / (60 * 24)).toString())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureChips(viewModel: CaptureViewModel) {
    if (viewModel.text.isBlank()) return
    val effective = viewModel.effective()
    val today = remember(viewModel.text) {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val removeText = stringResource(Res.string.chip_remove)

    @Composable
    fun removableChip(label: String, onRemove: () -> Unit) {
        InputChip(
            selected = false,
            onClick = onRemove,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Outlined.Close, removeText, Modifier.size(16.dp)) },
        )
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = viewModel::cycleType,
            label = { Text(typeLabel(effective.type)) },
        )
        val use24h = LocalSettings.current.use24h
        effective.date?.let { removableChip(dateLabel(it, today)) { viewModel.removeChip(ChipKind.DATE) } }
        effective.time?.let { removableChip(timeLabel(it, use24h)) { viewModel.removeChip(ChipKind.TIME) } }
        effective.durationMinutes?.let { removableChip(durationLabel(it)) { viewModel.removeChip(ChipKind.DURATION) } }
        if (effective.priority != Priority.NONE) {
            val label = when (effective.priority) {
                Priority.HIGH -> stringResource(Res.string.priority_high)
                Priority.MEDIUM -> stringResource(Res.string.priority_medium)
                else -> stringResource(Res.string.priority_low)
            }
            removableChip(label) { viewModel.removeChip(ChipKind.PRIORITY) }
        }
        effective.rrule?.let { removableChip(recurrenceLabel(it)) { viewModel.removeChip(ChipKind.RECURRENCE) } }
        effective.tags.forEach { tag -> removableChip("#$tag") { viewModel.removeTag(tag) } }
    }
}

/** Big check-in-a-circle that springs in and fades out after every save. */
@Composable
private fun SaveBurst(trigger: Int, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        alpha.snapTo(1f)
        scale.snapTo(0f)
        launch {
            scale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            )
        }
        launch {
            kotlinx.coroutines.delay(450)
            alpha.animateTo(0f, tween(350))
        }
    }
    if (alpha.value > 0f) {
        Box(
            modifier
                .size(160.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
