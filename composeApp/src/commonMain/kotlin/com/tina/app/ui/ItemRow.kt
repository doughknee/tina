package com.tina.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tina.app.data.Item
import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import com.tina.app.resources.Res
import com.tina.app.resources.date_next_week
import com.tina.app.resources.date_none
import com.tina.app.resources.date_today
import com.tina.app.resources.date_tomorrow
import com.tina.app.resources.delete
import com.tina.app.resources.ai_suggestion_pending
import com.tina.app.resources.mark_done
import com.tina.app.resources.open_details
import com.tina.app.resources.priority_high
import com.tina.app.resources.priority_medium
import com.tina.app.resources.type_event
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource

/**
 * The one list row used everywhere. Tap = inline title edit, swipe right =
 * complete, swipe left = delete, date chip = quick reschedule, chevron = detail.
 */
@Composable
fun ItemRow(
    item: Item,
    today: LocalDate,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
    timeText: String? = null,
    dateText: String? = null,
    onToggleComplete: (() -> Unit)? = null,
    onReschedule: ((LocalDate?) -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
    selected: Boolean = false,
    leading: Boolean = true,
    /** Replaces the checkbox/dot with a 22dp type icon (Library rows). */
    leadingIcon: ImageVector? = null,
    /** Small outlined label after the title, e.g. "×2" for merged duplicates. */
    badge: String? = null,
    badgeDescription: String? = null,
    extraContent: (@Composable () -> Unit)? = null,
)
{
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.settledValue) {
        when (dismissState.settledValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                if (onToggleComplete != null) {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onToggleComplete()
                }
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = onToggleComplete != null,
        backgroundContent = {
            // rows are transparent now; draw nothing until a swipe is actually underway
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            val (color, icon, alignment) = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Triple(
                    MaterialTheme.colorScheme.primaryContainer,
                    Icons.Outlined.Check,
                    Alignment.CenterStart,
                )
                else -> Triple(
                    MaterialTheme.colorScheme.errorContainer,
                    Icons.Outlined.Delete,
                    Alignment.CenterEnd,
                )
            }
            val bg by animateColorAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) Color.Transparent else color,
            )
            Box(Modifier.fillMaxSize().background(bg).padding(horizontal = 24.dp), contentAlignment = alignment) {
                Icon(icon, contentDescription = null)
            }
        },
    ) {
        RowContent(
            item = item,
            today = today,
            timeText = timeText,
            dateText = dateText,
            onToggleComplete = onToggleComplete,
            onRename = onRename,
            onReschedule = onReschedule,
            onOpen = onOpen,
            selected = selected,
            leading = leading,
            leadingIcon = leadingIcon,
            badge = badge,
            badgeDescription = badgeDescription,
            extraContent = extraContent,
        )
    }
}

@Composable
private fun RowContent(
    item: Item,
    today: LocalDate,
    timeText: String?,
    dateText: String?,
    onToggleComplete: (() -> Unit)?,
    onRename: (String) -> Unit,
    onReschedule: ((LocalDate?) -> Unit)?,
    onOpen: (() -> Unit)?,
    selected: Boolean,
    leading: Boolean,
    leadingIcon: ImageVector?,
    badge: String?,
    badgeDescription: String?,
    extraContent: (@Composable () -> Unit)?,
) {
    val haptic = LocalHapticFeedback.current
    var editing by remember(item.id) { mutableStateOf(false) }
    var editText by remember(item.id) { mutableStateOf(item.title) }
    val focusRequester = remember { FocusRequester() }

    val twoLine = timeText != null || item.priority != Priority.NONE
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceContainerHighest
                else Color.Transparent,
            )
            .clickable {
                editText = item.title
                editing = true
            }
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            Modifier.defaultMinSize(minHeight = if (twoLine) 72.dp else 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!leading) {
                // no leading widget (inbox rows: the type is the whole point of triage)
            } else if (leadingIcon != null) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (onToggleComplete != null) {
                Checkbox(
                    checked = item.completed,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onToggleComplete()
                    },
                )
            } else {
                val eventText = stringResource(Res.string.type_event)
                Box(
                    Modifier
                        .size(40.dp)
                        .semantics {
                            if (item.type == ItemType.EVENT) contentDescription = eventText
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // 2dp halo ring so low-chroma dots stay visible on any surface
                    Box(
                        Modifier
                            .size(14.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(
                                    item.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                                    CircleShape,
                                ),
                        )
                    }
                }
            }

            Column(Modifier.weight(1f).padding(start = 4.dp, top = 8.dp, bottom = 8.dp)) {
                if (editing) {
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                    BasicTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        textStyle = TextStyle.Default.merge(
                            MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            editing = false
                            if (editText.isNotBlank() && editText != item.title) onRename(editText.trim())
                        }),
                    )
                } else {
                    val suggestions by com.tina.app.ai.SuggestionCache.patches.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.title,
                            modifier = Modifier.sharedItemTitle(item.id),
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                            color = if (item.completed) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        if (item.id in suggestions) {
                            Icon(
                                Icons.Outlined.AutoAwesome,
                                stringResource(Res.string.ai_suggestion_pending),
                                Modifier.padding(start = 6.dp).size(14.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (badge != null) {
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                    .semantics { contentDescription = badgeDescription ?: badge },
                            )
                        }
                    }
                }
                val priorityLabel = when (item.priority) {
                    Priority.HIGH -> stringResource(Res.string.priority_high)
                    Priority.MEDIUM -> stringResource(Res.string.priority_medium)
                    else -> null
                }
                if (timeText != null || priorityLabel != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (timeText != null) {
                            Text(
                                timeText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (priorityLabel != null) {
                            Icon(
                                Icons.Outlined.PriorityHigh,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(start = if (timeText != null) 8.dp else 0.dp)
                                    .size(16.dp),
                                tint = if (item.priority == Priority.HIGH) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                },
                            )
                            Text(
                                priorityLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (dateText != null && onReschedule != null) {
                Box(Modifier.padding(start = 4.dp)) {
                    RescheduleChip(dateText, today, onReschedule)
                }
            } else if (onOpen != null) {
                IconButton(onClick = onOpen) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        stringResource(Res.string.open_details),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        extraContent?.invoke()
    }
}

@Composable
private fun RescheduleChip(label: String, today: LocalDate, onReschedule: (LocalDate?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { open = true },
            label = {
                AnimatedContent(
                    targetState = label,
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                ) { Text(it) }
            },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.date_today)) },
                onClick = { open = false; onReschedule(today) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.date_tomorrow)) },
                onClick = { open = false; onReschedule(today.plus(1, DateTimeUnit.DAY)) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.date_next_week)) },
                onClick = {
                    open = false
                    val delta = (DayOfWeek.MONDAY.isoDayNumber - today.dayOfWeek.isoDayNumber + 7).let {
                        if (it % 7 == 0) 7 else it % 7
                    }
                    onReschedule(today.plus(delta, DateTimeUnit.DAY))
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.date_none)) },
                onClick = { open = false; onReschedule(null) },
            )
        }
    }
}
