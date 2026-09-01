package com.tina.app.ui.capture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tina.app.LocalSettings
import com.tina.app.capture.CaptureViewModel
import com.tina.app.capture.ChipKind
import com.tina.app.capture.rememberSpeechCapture
import com.tina.app.capture.typeLabel
import com.tina.app.data.Item
import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import com.tina.app.resources.Res
import com.tina.app.resources.ai_refined
import com.tina.app.resources.ask_placeholder
import com.tina.app.resources.capture_placeholder
import com.tina.app.resources.capture_recent
import com.tina.app.resources.capture_start
import com.tina.app.resources.starter_today
import com.tina.app.resources.starter_tomorrow
import com.tina.app.resources.starter_next_week
import com.tina.app.resources.starter_every_day
import com.tina.app.resources.starter_at_9
import com.tina.app.resources.capture_save
import com.tina.app.resources.capture_type_state
import com.tina.app.resources.capture_voice
import com.tina.app.resources.captured
import com.tina.app.resources.chip_remove
import com.tina.app.resources.mode_ask
import com.tina.app.resources.mode_capture
import com.tina.app.resources.priority_high
import com.tina.app.resources.priority_low
import com.tina.app.resources.priority_medium
import com.tina.app.resources.undo
import com.tina.app.ui.dateLabel
import com.tina.app.ui.durationLabel
import com.tina.app.ui.recurrenceLabel
import com.tina.app.ui.relativeAge
import com.tina.app.ui.rememberUndoWindow
import com.tina.app.ui.showUndo
import com.tina.app.ui.timeLabel
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/**
 * The capture field, pinned above the nav bar on every top-level screen so capture is never
 * more than zero taps away. The leading toggle flips it into ask mode, where the same field
 * feeds the Ask sheet instead; that toggle is reset by the shell on start and on every
 * destination change so the fast path can never be left switched off.
 */
@Composable
fun CaptureBar(
    askMode: Boolean,
    onAskModeChange: (Boolean) -> Unit,
    onAskSend: (String) -> Unit,
    askBusy: Boolean,
    snackbarHostState: SnackbarHostState,
    focusRequester: FocusRequester,
    /** The shell shows the suggestions sheet while the empty field has focus. */
    onFocusChanged: (Boolean) -> Unit,
    viewModel: CaptureViewModel,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val undoWindow = rememberUndoWindow()
    val settings = LocalSettings.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val capturedText = stringResource(Res.string.captured)
    val undoText = stringResource(Res.string.undo)
    val refinedText = stringResource(Res.string.ai_refined)

    // a half-typed capture survives a detour into ask mode: the two modes keep separate text
    var askField by remember { mutableStateOf(TextFieldValue()) }
    val askInput = askField.text
    var focused by remember { mutableStateOf(false) }
    val text = if (askMode) askInput else viewModel.text
    val field = if (askMode) askField else viewModel.fieldValue

    // "Keyboard on open": the only automatic focus; widgets and shortcuts go through CaptureFocus
    LaunchedEffect(Unit) {
        if (settings.autoFocusCapture) {
            withFrameNanos { }
            focusRequester.requestFocus()
        }
    }

    // Android: the keyboard going away is the user saying "done" — drop focus so the
    // suggestions panel folds instead of squatting on half the screen. Desktop has no IME.
    // isImeVisible is Android-only; the inset height is the common-code equivalent
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(imeVisible) {
        if (com.tina.app.ui.settings.Platform.isAndroid && !imeVisible && focused) focusManager.clearFocus()
    }

    LaunchedEffect(Unit) {
        viewModel.refinedEvents.collect { original ->
            if (snackbarHostState.showUndo(refinedText, undoText, undoWindow)) viewModel.undoRefinement(original)
        }
    }

    fun send() {
        if (askMode) {
            val question = askInput.trim()
            if (question.isEmpty() || askBusy) return
            askField = TextFieldValue()
            onAskSend(question)
            return
        }
        viewModel.save {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            if (!settings.keepKeyboardUp) {
                focusManager.clearFocus()
                keyboard?.hide()
            }
            scope.launch {
                if (snackbarHostState.showUndo(capturedText, undoText, undoWindow)) viewModel.undoLastSave()
            }
        }
    }

    val speech = rememberSpeechCapture { spoken ->
        viewModel.onTextChange(if (viewModel.text.isBlank()) spoken else "${viewModel.text} $spoken")
    }
    val micVisible = !askMode && speech.available && settings.voiceCapture

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        AnimatedVisibility(
            visible = !askMode && viewModel.text.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            CaptureChips(viewModel)
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledIconToggleButton(
                    checked = askMode,
                    onCheckedChange = onAskModeChange,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        if (askMode) Icons.Outlined.AutoAwesome else Icons.Outlined.Edit,
                        stringResource(if (askMode) Res.string.mode_ask else Res.string.mode_capture),
                    )
                }

                val placeholder = stringResource(
                    if (askMode) Res.string.ask_placeholder else Res.string.capture_placeholder,
                )
                BasicTextField(
                    value = field,
                    onValueChange = { if (askMode) askField = it else viewModel.onFieldChange(it) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            focused = it.isFocused
                            onFocusChanged(it.isFocused)
                        }
                        .onPreviewKeyEvent { event ->
                            // physical keyboards: Enter sends, Shift+Enter makes a newline
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyDown && !event.isShiftPressed) {
                                send()
                                true
                            } else {
                                false
                            }
                        },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(
                                    placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )

                if (micVisible) {
                    IconButton(onClick = speech.start, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.Mic, stringResource(Res.string.capture_voice), Modifier.size(24.dp))
                    }
                }
                AnimatedVisibility(
                    visible = text.isNotBlank(),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    FilledIconButton(
                        onClick = ::send,
                        modifier = Modifier.size(40.dp),
                        enabled = !(askMode && askBusy),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, stringResource(Res.string.capture_save))
                    }
                }
            }
        }
    }
}

/**
 * The capture sheet: recents on top, then one-tap starters right above the field. Starters
 * come from the user's own history and the parser's own vocabulary, so a tap either
 * re-captures something familiar or drops in a token the parser understands and shows
 * the resulting chip immediately.
 */
@Composable
fun CaptureSuggestions(viewModel: CaptureViewModel, onOpenItem: (Item) -> Unit) {
    val recent by viewModel.recent.collectAsState()
    val starters by viewModel.starters.collectAsState()
    val use24h = LocalSettings.current.use24h
    val now = remember(recent) { Clock.System.now() }
    val today = remember(recent) { now.toLocalDateTime(TimeZone.currentSystemDefault()).date }

    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
        if (recent.isNotEmpty()) {
            Text(
                stringResource(Res.string.capture_recent).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
            recent.take(3).forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenItem(item) }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
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
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val schedule = when {
                            item.type == ItemType.EVENT && item.startAt != null -> {
                                val start = Instant.fromEpochMilliseconds(item.startAt!!)
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

        Text(
            stringResource(Res.string.capture_start).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = if (recent.isEmpty()) 0.dp else 12.dp),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // things captured more than once: a tap re-captures them
            starters.titles.forEach { title ->
                SuggestionChip(
                    onClick = { viewModel.prefill("$title ") },
                    label = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = { Icon(Icons.Outlined.Replay, null, Modifier.size(18.dp)) },
                )
            }
            starters.tags.forEach { tag ->
                SuggestionChip(
                    onClick = { viewModel.prefill("#$tag ") },
                    label = { Text("#$tag") },
                )
            }
            // parser tokens: the chip appears under the field the moment one is inserted
            listOf(
                Res.string.starter_today to "today ",
                Res.string.starter_tomorrow to "tomorrow ",
                Res.string.starter_next_week to "next week ",
                Res.string.starter_every_day to "every day ",
                Res.string.starter_at_9 to "at 9am ",
            ).forEach { (label, token) ->
                SuggestionChip(
                    onClick = { viewModel.prefill(token) },
                    label = { Text(stringResource(label)) },
                )
            }
        }
    }
}

/** Live parse chips: what tina understood, each removable, the type cycling on tap. */
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
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val typeText = typeLabel(effective.type)
        val typeState = stringResource(Res.string.capture_type_state, typeText)
        AssistChip(
            onClick = viewModel::cycleType,
            label = { Text(typeText) },
            leadingIcon = {
                Icon(
                    when (effective.type) {
                        ItemType.TASK -> Icons.Outlined.TaskAlt
                        ItemType.EVENT -> Icons.Outlined.Event
                        ItemType.NOTE -> Icons.Outlined.Description
                        ItemType.INBOX -> Icons.Outlined.Inbox
                    },
                    contentDescription = null,
                    Modifier.size(18.dp),
                )
            },
            modifier = Modifier.semantics { stateDescription = typeState },
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
fun SaveBurst(trigger: Int, modifier: Modifier = Modifier) {
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
