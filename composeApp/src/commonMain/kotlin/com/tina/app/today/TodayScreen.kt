package com.tina.app.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.tina.app.LocalSettings
import com.tina.app.data.ItemType
import com.tina.app.resources.Res
import com.tina.app.resources.deleted
import com.tina.app.resources.inbox
import com.tina.app.resources.section_afternoon
import com.tina.app.resources.section_anytime
import com.tina.app.resources.section_evening
import com.tina.app.resources.section_morning
import com.tina.app.resources.section_overdue
import com.tina.app.resources.settings
import com.tina.app.resources.tab_today
import com.tina.app.resources.today_empty
import com.tina.app.resources.undo
import com.tina.app.ui.ItemRow
import com.tina.app.ui.dateLabel
import com.tina.app.ui.timeLabel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onOpenSettings: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenItem: (com.tina.app.data.Item) -> Unit,
    viewModel: TodayViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val use24h = LocalSettings.current.use24h
    val deletedText = stringResource(Res.string.deleted)
    val undoText = stringResource(Res.string.undo)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    fun deleteWithUndo(delete: () -> Unit, undo: () -> Unit) {
        delete()
        scope.launch {
            val result = snackbarHostState.showSnackbar(deletedText, undoText, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) undo()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.tab_today)) },
                actions = {
                    IconButton(onClick = onOpenInbox) {
                        BadgedBox(
                            badge = {
                                if (state.inboxCount > 0) Badge { Text(state.inboxCount.toString()) }
                            },
                        ) {
                            Icon(Icons.Filled.Inbox, stringResource(Res.string.inbox))
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, stringResource(Res.string.settings))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val today = state.today
        if (today == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
        com.tina.app.notifications.ReminderPermissionBanner(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (state.sections.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Outlined.Celebration,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.today_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            return@Column
        }

        val anytimeEntries = state.sections.firstOrNull { it.first == TodaySection.ANYTIME }?.second.orEmpty()
        var localAnytime by remember { mutableStateOf(anytimeEntries) }
        LaunchedEffect(anytimeEntries) { localAnytime = anytimeEntries }

        val listState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
            val mutable = localAnytime.toMutableList()
            val fromIndex = mutable.indexOfFirst { anytimeKey(it) == from.key }
            val toIndex = mutable.indexOfFirst { anytimeKey(it) == to.key }
            if (fromIndex >= 0 && toIndex >= 0) {
                mutable.add(toIndex, mutable.removeAt(fromIndex))
                localAnytime = mutable
            }
        }

        LazyColumn(Modifier.fillMaxWidth().weight(1f), state = listState) {
            state.sections.forEach { (section, entries) ->
                item(key = "header-$section") {
                    Text(
                        stringResource(
                            when (section) {
                                TodaySection.OVERDUE -> Res.string.section_overdue
                                TodaySection.MORNING -> Res.string.section_morning
                                TodaySection.AFTERNOON -> Res.string.section_afternoon
                                TodaySection.EVENING -> Res.string.section_evening
                                TodaySection.ANYTIME -> Res.string.section_anytime
                            },
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (section == TodaySection.OVERDUE) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                    )
                }

                if (section == TodaySection.ANYTIME) {
                    items(localAnytime, key = ::anytimeKey) { entry ->
                        ReorderableItem(reorderableState, key = anytimeKey(entry)) { _ ->
                            ItemRow(
                                item = entry.item,
                                today = today,
                                timeText = null,
                                dateText = null,
                                onToggleComplete = if (entry.item.type == ItemType.TASK) {
                                    { viewModel.toggleComplete(entry.item) }
                                } else null,
                                onDelete = {
                                    deleteWithUndo({ viewModel.delete(entry.item) }, viewModel::undoDelete)
                                },
                                onRename = { viewModel.rename(entry.item, it) },
                                onReschedule = { viewModel.reschedule(entry.item, it) },
                                onOpen = { onOpenItem(entry.item) },
                                modifier = Modifier.longPressDraggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        viewModel.persistAnytimeOrder(localAnytime.map { it.item.id })
                                    },
                                ),
                            )
                        }
                    }
                } else {
                    items(entries, key = { "$section-${it.item.id}-${it.time}" }) { entry ->
                        ItemRow(
                            item = entry.item,
                            today = today,
                            timeText = entry.time?.let { timeLabel(it, use24h) },
                            dateText = if (section == TodaySection.OVERDUE) {
                                entry.item.dueLocalDate?.let { dateLabel(it, today) }
                            } else null,
                            onToggleComplete = if (entry.item.type == ItemType.TASK) {
                                { viewModel.toggleComplete(entry.item) }
                            } else null,
                            onDelete = {
                                deleteWithUndo({ viewModel.delete(entry.item) }, viewModel::undoDelete)
                            },
                            onRename = { viewModel.rename(entry.item, it) },
                            onReschedule = if (entry.item.type == ItemType.TASK) {
                                { viewModel.reschedule(entry.item, it) }
                            } else null,
                            onOpen = { onOpenItem(entry.item) },
                        )
                    }
                }
            }
        }
        }
    }
}

private fun anytimeKey(entry: TodayEntry): String = "any-${entry.item.id}"
