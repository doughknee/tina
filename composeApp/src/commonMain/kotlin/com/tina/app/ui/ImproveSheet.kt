package com.tina.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tina.app.LocalSettings
import com.tina.app.ai.AiImprover
import com.tina.app.ai.ImproveField
import com.tina.app.ai.ImprovePatch
import com.tina.app.ai.SuggestionCache
import com.tina.app.ai.applyImprovePatch
import com.tina.app.capture.typeLabel
import com.tina.app.data.Item
import com.tina.app.data.Priority
import com.tina.app.resources.Res
import com.tina.app.resources.fld_date
import com.tina.app.resources.fld_duration
import com.tina.app.resources.fld_notes
import com.tina.app.resources.fld_priority
import com.tina.app.resources.fld_reminder
import com.tina.app.resources.fld_repeat
import com.tina.app.resources.fld_tags
import com.tina.app.resources.fld_time
import com.tina.app.resources.fld_title
import com.tina.app.resources.fld_type
import com.tina.app.resources.improve_apply
import com.tina.app.resources.improve_custom_hint
import com.tina.app.resources.improve_error
import com.tina.app.resources.improve_loading
import com.tina.app.resources.improve_none
import com.tina.app.resources.improve_refine
import com.tina.app.resources.improve_retry
import com.tina.app.resources.improve_title
import com.tina.app.resources.pr_high
import com.tina.app.resources.pr_low
import com.tina.app.resources.pr_medium
import com.tina.app.resources.pr_none
import com.tina.app.resources.reminder_at_time
import com.tina.app.resources.reminder_hour_before
import com.tina.app.resources.reminder_min_before
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private fun presentFields(p: ImprovePatch): Set<String> = buildSet {
    if (p.title != null) add(ImproveField.TITLE)
    if (p.type != null) add(ImproveField.TYPE)
    if (p.date != null) add(ImproveField.DATE)
    if (p.time != null) add(ImproveField.TIME)
    if (p.durationMinutes != null) add(ImproveField.DURATION)
    if (p.priority != null) add(ImproveField.PRIORITY)
    if (p.tags != null) add(ImproveField.TAGS)
    if (p.rrule != null) add(ImproveField.RRULE)
    if (p.body != null) add(ImproveField.BODY)
    if (p.reminderOffsetMinutes != null) add(ImproveField.REMINDER)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImproveSheet(
    item: Item,
    onApply: (updated: Item, original: Item) -> Unit,
    onDismiss: () -> Unit,
) {
    val improver: AiImprover = koinInject()
    val scope = rememberCoroutineScope()
    val use24h = LocalSettings.current.use24h
    val tz = TimeZone.currentSystemDefault()
    val today = remember { Clock.System.now().toLocalDateTime(tz).date }

    var patch by remember { mutableStateOf(SuggestionCache.patches.value[item.id]) }
    var loading by remember { mutableStateOf(patch == null) }
    var error by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(patch?.let(::presentFields) ?: emptySet()) }
    val answers = remember { mutableStateMapOf<String, String>() }
    var round by remember { mutableStateOf(1) }

    fun load(block: suspend () -> ImprovePatch?) {
        loading = true
        error = false
        scope.launch {
            val result = block()
            if (result == null) {
                error = true
            } else {
                patch = result
                selected = presentFields(result)
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        if (patch == null) load { improver.suggest(item) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.improve_title), style = MaterialTheme.typography.titleMediumEmphasized)

            when {
                loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 16.dp),
                ) {
                    LoadingIndicator(Modifier.size(32.dp))
                    Text(
                        stringResource(Res.string.improve_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                error -> {
                    Text(
                        stringResource(Res.string.improve_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(onClick = { load { improver.suggest(item) } }) {
                        Text(stringResource(Res.string.improve_retry))
                    }
                }
                else -> patch?.let { p ->
                    p.rationale?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (p.isEmpty && p.questions.isEmpty()) {
                        Text(
                            stringResource(Res.string.improve_none),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    ChangeChips(item, p, use24h, today, selected) { key ->
                        selected = if (key in selected) selected - key else selected + key
                    }
                    if (round == 1) {
                        p.questions.forEach { q ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(q.question, style = MaterialTheme.typography.titleSmall)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    q.options.forEach { option ->
                                        FilterChip(
                                            selected = answers[q.question] == option,
                                            onClick = { answers[q.question] = option },
                                            label = { Text(option) },
                                        )
                                    }
                                }
                                if (q.allowCustom) {
                                    val custom = answers[q.question]
                                        ?.takeUnless { it in q.options } ?: ""
                                    OutlinedTextField(
                                        value = custom,
                                        onValueChange = { answers[q.question] = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = {
                                            Text(stringResource(Res.string.improve_custom_hint))
                                        },
                                        singleLine = true,
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (round == 1 && p.questions.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    round = 2
                                    load {
                                        improver.finalize(
                                            item,
                                            answers.filterValues { it.isNotBlank() },
                                        )
                                    }
                                },
                                enabled = answers.values.any { it.isNotBlank() },
                            ) {
                                Text(stringResource(Res.string.improve_refine))
                            }
                        }
                        Button(
                            onClick = {
                                val updated = applyImprovePatch(item, p, selected, tz)
                                SuggestionCache.remove(item.id)
                                onApply(updated, item)
                                onDismiss()
                            },
                            enabled = selected.isNotEmpty(),
                        ) {
                            Text(stringResource(Res.string.improve_apply))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeChips(
    item: Item,
    p: ImprovePatch,
    use24h: Boolean,
    today: kotlinx.datetime.LocalDate,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    val reminderLabel: @Composable (Int) -> String = { m ->
        when (m) {
            0 -> stringResource(Res.string.reminder_at_time)
            60 -> stringResource(Res.string.reminder_hour_before)
            else -> stringResource(Res.string.reminder_min_before, m)
        }
    }
    val priorityLabel: @Composable (Priority) -> String = {
        stringResource(
            when (it) {
                Priority.NONE -> Res.string.pr_none
                Priority.LOW -> Res.string.pr_low
                Priority.MEDIUM -> Res.string.pr_medium
                Priority.HIGH -> Res.string.pr_high
            },
        )
    }
    val chips = buildList {
        p.title?.let { add(ImproveField.TITLE to "${stringResource(Res.string.fld_title)} → $it") }
        p.type?.let { add(ImproveField.TYPE to "${stringResource(Res.string.fld_type)} → ${typeLabel(it)}") }
        p.date?.let { add(ImproveField.DATE to "${stringResource(Res.string.fld_date)} → ${dateLabel(it, today)}") }
        p.time?.let { add(ImproveField.TIME to "${stringResource(Res.string.fld_time)} → ${timeLabel(it, use24h)}") }
        p.durationMinutes?.let {
            add(ImproveField.DURATION to "${stringResource(Res.string.fld_duration)} → ${durationLabel(it)}")
        }
        p.priority?.let {
            add(ImproveField.PRIORITY to "${stringResource(Res.string.fld_priority)} → ${priorityLabel(it)}")
        }
        p.tags?.let { tags ->
            add(ImproveField.TAGS to "${stringResource(Res.string.fld_tags)} → ${tags.joinToString(" ") { "#$it" }}")
        }
        p.rrule?.let { add(ImproveField.RRULE to "${stringResource(Res.string.fld_repeat)} → ${recurrenceLabel(it)}") }
        p.body?.let {
            add(ImproveField.BODY to "${stringResource(Res.string.fld_notes)} → ${it.take(40)}${if (it.length > 40) "…" else ""}")
        }
        p.reminderOffsetMinutes?.let {
            add(ImproveField.REMINDER to "${stringResource(Res.string.fld_reminder)} → ${reminderLabel(it)}")
        }
    }
    if (chips.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chips.forEach { (key, label) ->
                FilterChip(
                    selected = key in selected,
                    onClick = { onToggle(key) },
                    label = { Text(label) },
                )
            }
        }
    }
}
