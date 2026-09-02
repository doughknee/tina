package com.tina.app.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tina.app.data.Decisions
import com.tina.app.data.Item
import com.tina.app.data.STALE_AFTER_DAYS
import com.tina.app.resources.Res
import com.tina.app.resources.date_today
import com.tina.app.resources.date_tomorrow
import com.tina.app.resources.deleted
import com.tina.app.resources.inbox_captured
import com.tina.app.resources.settings
import com.tina.app.resources.sort_due
import com.tina.app.resources.sort_empty
import com.tina.app.resources.sort_empty_sub
import com.tina.app.resources.sort_new
import com.tina.app.resources.sort_overdue
import com.tina.app.resources.sort_snoozed
import com.tina.app.resources.sort_snoozed_until
import com.tina.app.resources.sort_someday
import com.tina.app.resources.sort_untouched
import com.tina.app.resources.sorted
import com.tina.app.resources.tab_sort
import com.tina.app.resources.triage_done
import com.tina.app.resources.triage_drop
import com.tina.app.resources.triage_keep
import com.tina.app.resources.triage_make_event
import com.tina.app.resources.triage_make_note
import com.tina.app.resources.triage_someday
import com.tina.app.resources.triage_this_week
import com.tina.app.resources.undo
import com.tina.app.ui.ItemRow
import com.tina.app.LocalSettings
import com.tina.app.ui.SectionCardItem
import com.tina.app.ui.SwipeAction
import com.tina.app.ui.SwipeTone
import com.tina.app.ui.dateLabel
import com.tina.app.ui.relativeAge
import com.tina.app.ui.rememberUndoWindow
import com.tina.app.ui.showUndo
import com.tina.app.ui.timeLabel
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class Group(val title: StringResource) {
    NEW(Res.string.sort_new),
    OVERDUE(Res.string.sort_overdue),
    SNOOZED(Res.string.sort_snoozed),
    SOMEDAY(Res.string.sort_someday),
}

/** The chips a group offers, in order; the first two double as the swipes. */
private fun chipsFor(group: Group): List<Pair<TriageAction, StringResource>> = when (group) {
    Group.NEW -> listOf(
        TriageAction.TODAY to Res.string.date_today,
        TriageAction.SOMEDAY to Res.string.triage_someday,
        TriageAction.TOMORROW to Res.string.date_tomorrow,
        TriageAction.THIS_WEEK to Res.string.triage_this_week,
        TriageAction.MAKE_EVENT to Res.string.triage_make_event,
        TriageAction.MAKE_NOTE to Res.string.triage_make_note,
    )
    Group.OVERDUE -> listOf(
        TriageAction.TODAY to Res.string.date_today,
        TriageAction.DONE to Res.string.triage_done,
        TriageAction.TOMORROW to Res.string.date_tomorrow,
        TriageAction.SOMEDAY to Res.string.triage_someday,
    )
    Group.SNOOZED -> listOf(
        TriageAction.DONE to Res.string.triage_done,
        TriageAction.KEEP to Res.string.triage_keep,
        TriageAction.TOMORROW to Res.string.date_tomorrow,
    )
    Group.SOMEDAY -> listOf(
        TriageAction.TODAY to Res.string.date_today,
        TriageAction.THIS_WEEK to Res.string.triage_this_week,
        TriageAction.TOMORROW to Res.string.date_tomorrow,
    )
}

/** Sort: every decision owed, grouped, each a card with one-tap answers. Answering animates it out. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onOpenSettings: () -> Unit,
    onOpenItem: (Item) -> Unit,
    viewModel: InboxViewModel,
) {
    val decisions by viewModel.decisions.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val undoWindow = rememberUndoWindow()
    val scope = rememberCoroutineScope()
    val sortedText = stringResource(Res.string.sorted)
    val deletedText = stringResource(Res.string.deleted)
    val undoText = stringResource(Res.string.undo)
    val use24h = LocalSettings.current.use24h
    val tz = TimeZone.currentSystemDefault()
    val today = remember { Clock.System.now().toLocalDateTime(tz).date }
    val nowMillis = remember(decisions) { Clock.System.now().toEpochMilliseconds() }

    fun withUndo(message: String, action: () -> Unit, undo: () -> Unit) {
        action()
        scope.launch {
            if (snackbarHostState.showUndo(message, undoText, undoWindow)) undo()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.tab_sort), style = MaterialTheme.typography.titleLargeEmphasized) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(Res.string.settings))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (decisions.isEmpty) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Outlined.Inbox,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.sort_empty),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    stringResource(Res.string.sort_empty_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp),
                )
            }
            return@Scaffold
        }

        val groups = listOf(
            Group.NEW to decisions.new,
            Group.OVERDUE to decisions.overdue,
            Group.SNOOZED to decisions.snoozed,
            Group.SOMEDAY to decisions.someday,
        ).filter { it.second.isNotEmpty() }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
        ) {
            groups.forEach { (group, list) ->
                item(key = "header-${group.name}") {
                    SortHeader(stringResource(group.title), list.size, Modifier.animateItem())
                }
                items(list, key = { "${group.name}-${it.id}" }) { item ->
                    val chips = chipsFor(group)
                    val timeText = when (group) {
                        Group.NEW -> stringResource(Res.string.inbox_captured, relativeAge(nowMillis - item.createdAt))
                        Group.OVERDUE -> stringResource(Res.string.sort_due, item.dueLocalDate?.let { dateLabel(it, today) } ?: "")
                        Group.SNOOZED -> stringResource(
                            Res.string.sort_snoozed_until,
                            item.snoozedUntil?.let { timeLabel(Instant.fromEpochMilliseconds(it).toLocalDateTime(tz).time, use24h) } ?: "",
                        )
                        Group.SOMEDAY -> {
                            val days = ((nowMillis - item.updatedAt) / (24L * 60 * 60 * 1000)).toInt()
                            if (days >= STALE_AFTER_DAYS) stringResource(Res.string.sort_untouched, days)
                            else stringResource(Res.string.inbox_captured, relativeAge(nowMillis - item.createdAt))
                        }
                    }
                    val (rightAction, rightLabel) = chips[0]
                    val (leftAction, leftLabel) = chips[1]
                    SectionCardItem(0, 1, Modifier.padding(bottom = 12.dp).animateItem()) {
                        ItemRow(
                            item = item,
                            today = today,
                            leading = false,
                            timeText = timeText,
                            onDelete = { withUndo(deletedText, { viewModel.delete(item) }, viewModel::undoDelete) },
                            onRename = { viewModel.rename(item, it) },
                            onOpen = { onOpenItem(item) },
                            // the two-second rule: a swipe answers without hunting for a chip; both are undoable
                            swipeRight = SwipeAction(iconFor(rightAction), SwipeTone.PRIMARY, stringResource(rightLabel)) {
                                withUndo(sortedText, { viewModel.triage(item, rightAction) }, viewModel::undoTriage)
                            },
                            swipeLeft = SwipeAction(iconFor(leftAction), SwipeTone.TERTIARY, stringResource(leftLabel)) {
                                withUndo(sortedText, { viewModel.triage(item, leftAction) }, viewModel::undoTriage)
                            },
                            extraContent = {
                                LazyRow(
                                    Modifier.padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(chips.size) { i ->
                                        val (action, label) = chips[i]
                                        SuggestionChip(
                                            onClick = {
                                                withUndo(sortedText, { viewModel.triage(item, action) }, viewModel::undoTriage)
                                            },
                                            label = { Text(stringResource(label), style = MaterialTheme.typography.labelMedium) },
                                        )
                                    }
                                    if (group == Group.SOMEDAY) {
                                        item {
                                            SuggestionChip(
                                                onClick = { withUndo(deletedText, { viewModel.delete(item) }, viewModel::undoDelete) },
                                                label = { Text(stringResource(Res.string.triage_drop), style = MaterialTheme.typography.labelMedium) },
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun iconFor(action: TriageAction) = when (action) {
    TriageAction.TODAY, TriageAction.TOMORROW, TriageAction.THIS_WEEK -> Icons.Outlined.Today
    TriageAction.DONE -> Icons.Outlined.Check
    TriageAction.KEEP -> Icons.Outlined.Check
    TriageAction.SOMEDAY -> Icons.Outlined.Snooze
    TriageAction.MAKE_EVENT, TriageAction.MAKE_NOTE -> Icons.Outlined.Today
}

@Composable
private fun SortHeader(title: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp).semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "  $count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
