package com.tina.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tina.app.data.RecurrenceRule
import com.tina.app.data.byDayCode
import com.tina.app.data.parseRrule
import com.tina.app.resources.Res
import com.tina.app.resources.cancel
import com.tina.app.resources.event_repeat
import com.tina.app.resources.ok
import com.tina.app.resources.repeat_custom
import com.tina.app.resources.repeat_daily
import com.tina.app.resources.repeat_every_n
import com.tina.app.resources.repeat_monthly
import com.tina.app.resources.repeat_none
import com.tina.app.resources.repeat_weekly
import com.tina.app.resources.repeat_yearly
import com.tina.app.resources.weekdays_full
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/**
 * The one repeat editor, for events and tasks alike: a rail of the common rules plus a
 * custom dialog for intervals and weekdays. Edits the rule in place; occurrences already
 * marked done keep their marks.
 */
@Composable
fun RepeatSection(rrule: String?, onSelect: (String?) -> Unit, onCustom: () -> Unit) {
    val parsed = rrule?.let { parseRrule(it) }
    val isSimple = parsed != null && parsed.interval == 1 && parsed.byDay.isEmpty() &&
        parsed.count == null && parsed.until == null
    val selectedIndex = when {
        rrule == null -> 0
        isSimple && parsed.freq == RecurrenceRule.Freq.DAILY -> 1
        isSimple && parsed.freq == RecurrenceRule.Freq.WEEKLY -> 2
        isSimple && parsed.freq == RecurrenceRule.Freq.MONTHLY -> 3
        isSimple && parsed.freq == RecurrenceRule.Freq.YEARLY -> 4
        else -> 5
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.event_repeat),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChipRail(selectedIndex = selectedIndex) {
            item {
                FilterChip(
                    selected = rrule == null,
                    onClick = { onSelect(null) },
                    label = { Text(stringResource(Res.string.repeat_none)) },
                )
            }
            item {
                FilterChip(
                    selected = isSimple && parsed.freq == RecurrenceRule.Freq.DAILY,
                    onClick = { onSelect("FREQ=DAILY") },
                    label = { Text(stringResource(Res.string.repeat_daily)) },
                )
            }
            item {
                FilterChip(
                    selected = isSimple && parsed.freq == RecurrenceRule.Freq.WEEKLY,
                    onClick = { onSelect("FREQ=WEEKLY") },
                    label = { Text(stringResource(Res.string.repeat_weekly)) },
                )
            }
            item {
                FilterChip(
                    selected = isSimple && parsed.freq == RecurrenceRule.Freq.MONTHLY,
                    onClick = { onSelect("FREQ=MONTHLY") },
                    label = { Text(stringResource(Res.string.repeat_monthly)) },
                )
            }
            item {
                FilterChip(
                    selected = isSimple && parsed.freq == RecurrenceRule.Freq.YEARLY,
                    onClick = { onSelect("FREQ=YEARLY") },
                    label = { Text(stringResource(Res.string.repeat_yearly)) },
                )
            }
            item {
                FilterChip(
                    selected = rrule != null && !isSimple,
                    onClick = onCustom,
                    label = { Text(stringResource(Res.string.repeat_custom)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRepeatDialog(
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
                        Icon(Icons.Outlined.Remove, contentDescription = null)
                    }
                    Text(interval.toString(), style = MaterialTheme.typography.titleMediumEmphasized)
                    IconButton(onClick = { interval++ }) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                    }
                }
                ConnectedButtonGroup(
                    count = RecurrenceRule.Freq.entries.size,
                    selectedIndex = freq.ordinal,
                    onSelect = { freq = RecurrenceRule.Freq.entries[it] },
                ) { index, _ ->
                    Text(
                        when (RecurrenceRule.Freq.entries[index]) {
                            RecurrenceRule.Freq.DAILY -> stringResource(Res.string.repeat_daily)
                            RecurrenceRule.Freq.WEEKLY -> stringResource(Res.string.repeat_weekly)
                            RecurrenceRule.Freq.MONTHLY -> stringResource(Res.string.repeat_monthly)
                            RecurrenceRule.Freq.YEARLY -> stringResource(Res.string.repeat_yearly)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
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
