package com.tina.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.tina.app.resources.mark_done
import com.tina.app.resources.open_details
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
            val (color, icon, alignment) = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Triple(
                    MaterialTheme.colorScheme.primaryContainer,
                    Icons.Filled.Check,
                    Alignment.CenterStart,
                )
                else -> Triple(
                    MaterialTheme.colorScheme.errorContainer,
                    Icons.Filled.Delete,
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
    extraContent: (@Composable () -> Unit)?,
) {
    val haptic = LocalHapticFeedback.current
    var editing by remember(item.id) { mutableStateOf(false) }
    var editText by remember(item.id) { mutableStateOf(item.title) }
    val focusRequester = remember { FocusRequester() }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                editText = item.title
                editing = true
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onToggleComplete != null) {
                Checkbox(
                    checked = item.completed,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onToggleComplete()
                    },
                )
            } else {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(
                                item.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                                CircleShape,
                            ),
                    )
                }
            }

            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
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
                }
                if (timeText != null || item.priority != Priority.NONE) {
                    Text(
                        listOfNotNull(
                            timeText,
                            when (item.priority) {
                                Priority.HIGH -> "!!"
                                Priority.MEDIUM -> "!"
                                else -> null
                            },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (dateText != null && onReschedule != null) {
                RescheduleChip(dateText, today, onReschedule)
            }
            if (onOpen != null) {
                IconButton(onClick = onOpen) {
                    Icon(
                        Icons.Filled.ChevronRight,
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
        AssistChip(onClick = { open = true }, label = { Text(label) })
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
