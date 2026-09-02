package com.tina.app.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.unit.dp
import com.tina.app.data.Item
import com.tina.app.resources.Res
import com.tina.app.resources.date_today
import com.tina.app.resources.date_tomorrow
import com.tina.app.resources.deleted
import com.tina.app.resources.tab_sort
import com.tina.app.resources.inbox_captured
import com.tina.app.resources.inbox_empty
import com.tina.app.resources.inbox_empty_sub
import com.tina.app.resources.settings
import com.tina.app.resources.sorted
import com.tina.app.resources.triage_make_event
import com.tina.app.resources.triage_make_note
import com.tina.app.resources.triage_someday
import com.tina.app.resources.triage_this_week
import com.tina.app.resources.undo
import com.tina.app.ui.ItemRow
import com.tina.app.ui.SectionCardItem
import com.tina.app.ui.relativeAge
import com.tina.app.ui.rememberUndoWindow
import com.tina.app.ui.showUndo
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/** Inbox = sort. Each untriaged capture is a card with one-tap triage chips; sorting animates it out. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onOpenSettings: () -> Unit,
    onOpenItem: (Item) -> Unit,
    viewModel: InboxViewModel,
) {
    val items by viewModel.items.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val undoWindow = rememberUndoWindow()
    val scope = rememberCoroutineScope()
    val sortedText = stringResource(Res.string.sorted)
    val deletedText = stringResource(Res.string.deleted)
    val undoText = stringResource(Res.string.undo)
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val nowMillis = remember(items) { Clock.System.now().toEpochMilliseconds() }

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
        if (items.isEmpty()) {
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
                    stringResource(Res.string.inbox_empty),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    stringResource(Res.string.inbox_empty_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.id }) { _, item ->
                SectionCardItem(0, 1, Modifier.padding(bottom = 12.dp).animateItem()) {
                    ItemRow(
                        item = item,
                        today = today,
                        leading = false,
                        timeText = stringResource(Res.string.inbox_captured, relativeAge(nowMillis - item.createdAt)),
                        onDelete = { withUndo(deletedText, { viewModel.delete(item) }, viewModel::undoDelete) },
                        onRename = { viewModel.rename(item, it) },
                        onOpen = { onOpenItem(item) },
                        extraContent = {
                            val chips = listOf(
                                TriageAction.TODAY to Res.string.date_today,
                                TriageAction.TOMORROW to Res.string.date_tomorrow,
                                TriageAction.THIS_WEEK to Res.string.triage_this_week,
                                TriageAction.SOMEDAY to Res.string.triage_someday,
                                TriageAction.MAKE_EVENT to Res.string.triage_make_event,
                                TriageAction.MAKE_NOTE to Res.string.triage_make_note,
                            )
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
                            }
                        },
                    )
                }
            }
        }
    }
}
