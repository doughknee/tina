package com.tina.app.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * Corner morphing that makes a run of rows read as one card: only the outer
 * corners of the group are round. 2dp gaps replace dividers.
 */
fun shapeFor(index: Int, count: Int): Shape = when {
    count == 1 -> RoundedCornerShape(20.dp)
    index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    index == count - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    else -> RoundedCornerShape(4.dp)
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 28.dp, bottom = 8.dp),
    )
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
}

/** One card slice: correct corner shape, container color, and search highlight. */
@Composable
fun SettingsRowSurface(
    index: Int,
    count: Int,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val container by animateColorAsState(
        if (highlighted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(300),
    )
    Surface(color = container, shape = shapeFor(index, count), modifier = modifier.fillMaxWidth()) {
        content()
    }
}

private val transparentListItem: androidx.compose.material3.ListItemColors
    @Composable get() = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)

@Composable
private fun RowText(title: String) {
    Text(title, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun RowSupporting(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun SwitchRow(row: SettingsRow.Switch) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val hapticsOn = com.tina.app.LocalSettings.current.haptics
    ListItem(
        headlineContent = { RowText(row.title) },
        supportingContent = row.supporting?.let { { RowSupporting(it) } },
        trailingContent = { Switch(checked = row.checked, onCheckedChange = null) },
        colors = transparentListItem,
        // whole row is the target, not just the thumb
        modifier = Modifier
            .toggleable(value = row.checked, role = Role.Switch) {
                if (hapticsOn) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
                }
                row.onCheckedChange(it)
            }
            .semantics(mergeDescendants = true) {},
    )
}

@Composable
fun NavigationRow(row: SettingsRow.Navigation) {
    ListItem(
        headlineContent = { RowText(row.title) },
        supportingContent = row.supporting?.let { { RowSupporting(it) } },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = transparentListItem,
        modifier = Modifier
            .clickable(onClick = row.onClick)
            .semantics(mergeDescendants = true) {
                row.supporting?.let { stateDescription = it }
            },
    )
}

@Composable
fun ExternalRow(row: SettingsRow.External) {
    ListItem(
        headlineContent = { RowText(row.title) },
        supportingContent = row.supporting?.let { { RowSupporting(it) } },
        trailingContent = {
            Icon(
                Icons.Outlined.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = transparentListItem,
        modifier = Modifier.clickable(onClick = row.onClick).semantics(mergeDescendants = true) {},
    )
}

@Composable
fun ValueRow(row: SettingsRow.Value) {
    ListItem(
        headlineContent = { RowText(row.title) },
        supportingContent = row.supporting?.let { { RowSupporting(it) } },
        colors = transparentListItem,
        modifier = Modifier.semantics(mergeDescendants = true) {
            row.supporting?.let { stateDescription = it }
        },
    )
}

@Composable
fun TimePickerRow(row: SettingsRow.TimeRow) {
    ListItem(
        headlineContent = { RowText(row.title) },
        supportingContent = row.supporting?.let { { RowSupporting(it) } },
        trailingContent = {
            Text(
                row.timeLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        colors = transparentListItem,
        modifier = Modifier.clickable(onClick = row.onClick).semantics(mergeDescendants = true) {
            stateDescription = row.timeLabel
        },
    )
}

@Composable
fun DestructiveRow(row: SettingsRow.Destructive) {
    ListItem(
        headlineContent = { RowText(row.title) },
        supportingContent = row.supporting?.let { { RowSupporting(it) } },
        trailingContent = {
            Button(
                onClick = row.onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) { Text(row.actionLabel) }
        },
        colors = transparentListItem,
        modifier = Modifier.semantics(mergeDescendants = true) {},
    )
}

/**
 * Segmented single choice.
 * TODO(expressive): material3 1.9.0 ships no ButtonGroup/ToggleButton, so this uses
 * SingleChoiceSegmentedButtonRow at the same 48dp height. Swap when they land.
 */
@Composable
fun ButtonGroupRow(row: SettingsRow.ButtonGroupRow) {
    Column(Modifier.padding(16.dp)) {
        Text(row.title, style = MaterialTheme.typography.bodyLarge)
        row.supporting?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            row.options.forEachIndexed { index, option ->
                val selected = index == row.selectedIndex
                SegmentedButton(
                    selected = selected,
                    onClick = { row.onSelect(index) },
                    shape = SegmentedButtonDefaults.itemShape(index, row.options.size),
                    icon = {
                        if (selected && option.icon != null) {
                            Icon(option.icon, contentDescription = null, Modifier.size(18.dp))
                        } else if (selected) {
                            SegmentedButtonDefaults.ActiveIcon()
                        }
                    },
                    modifier = Modifier.semantics { this.stateDescription = option.label },
                ) {
                    Text(option.label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun ChipRailRow(row: SettingsRow.ChipRailRow) {
    val state = rememberLazyListState()
    LaunchedEffect(row.selectedIndex) {
        if (row.selectedIndex >= 0) state.animateScrollToItem(row.selectedIndex)
    }
    Column(Modifier.padding(vertical = 16.dp)) {
        Text(row.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
        row.supporting?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        LazyRow(
            Modifier.padding(top = 12.dp),
            state = state,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(row.options.size) { index ->
                val selected = index == row.selectedIndex
                FilterChip(
                    selected = selected,
                    onClick = { row.onSelect(index) },
                    label = { Text(row.options[index]) },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else null,
                    modifier = Modifier.semantics { this.selected = selected },
                )
            }
        }
    }
}

/**
 * Hold-to-confirm instead of a dialog. A tap cannot fire it and releasing early
 * cancels; TalkBack gets a long-press action rather than a moving target.
 */
@Composable
fun HoldToConfirm(
    label: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Int = 2000,
) {
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val hapticsOn = com.tina.app.LocalSettings.current.haptics
    var halfway by remember { mutableStateOf(false) }

    fun buzz() {
        if (hapticsOn) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.error)
            .semantics { onLongClick(label = label) { buzz(); onConfirm(); true } }
            .pointerInput(holdMillis) {
                detectTapGestures(
                    onPress = {
                        halfway = false
                        val completed = runCatching {
                            progress.animateTo(1f, androidx.compose.animation.core.tween(holdMillis))
                            true
                        }.getOrDefault(false)
                        if (completed && progress.value >= 1f) {
                            buzz()
                            onConfirm()
                        }
                        progress.snapTo(0f)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.value)
                .height(44.dp)
                .background(MaterialTheme.colorScheme.onErrorContainer),
        )
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onError)
    }
    LaunchedEffect(progress.value) {
        if (!halfway && progress.value >= 0.5f) {
            halfway = true
            buzz()
        }
    }
}

/** Radio row used by the choice subpages (Open app to, Undo window, Contrast). */
@Composable
fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    ListItem(
        headlineContent = { RowText(label) },
        trailingContent = {
            androidx.compose.material3.RadioButton(selected = selected, onClick = null)
        },
        colors = transparentListItem,
        modifier = Modifier
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .semantics(mergeDescendants = true) {},
    )
}

/** Renders any row kind inside its group slice. */
@Composable
fun SettingsRowContent(row: SettingsRow) {
    when (row) {
        is SettingsRow.Switch -> SwitchRow(row)
        is SettingsRow.Navigation -> NavigationRow(row)
        is SettingsRow.External -> ExternalRow(row)
        is SettingsRow.Value -> ValueRow(row)
        is SettingsRow.TimeRow -> TimePickerRow(row)
        is SettingsRow.Destructive -> DestructiveRow(row)
        is SettingsRow.ButtonGroupRow -> ButtonGroupRow(row)
        is SettingsRow.ChipRailRow -> ChipRailRow(row)
        is SettingsRow.Custom -> row.content()
    }
}

/** A whole section: header outside, rows as one shaped card. */
@Composable
fun SettingsSectionBlock(
    section: SettingsSection,
    highlightedRowId: String? = null,
    rowModifier: (SettingsRow) -> Modifier = { Modifier },
) {
    val rows = section.visibleRows
    if (rows.isEmpty()) return
    Column {
        SettingsSectionHeader(section.title)
        SettingsGroup {
            rows.forEachIndexed { index, row ->
                val standalone = row is SettingsRow.Custom && !row.inGroup
                if (standalone) {
                    Row(Modifier.fillMaxWidth().then(rowModifier(row)), verticalAlignment = Alignment.CenterVertically) {
                        SettingsRowContent(row)
                    }
                } else {
                    SettingsRowSurface(
                        index = index,
                        count = rows.size,
                        highlighted = row.id == highlightedRowId,
                        modifier = rowModifier(row),
                    ) {
                        SettingsRowContent(row)
                    }
                }
            }
        }
    }
}
