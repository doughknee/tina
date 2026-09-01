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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.tina.app.resources.Res
import com.tina.app.resources.back
import com.tina.app.resources.date_today
import com.tina.app.resources.date_tomorrow
import com.tina.app.resources.deleted
import com.tina.app.resources.inbox
import com.tina.app.resources.inbox_captured
import com.tina.app.resources.inbox_empty
import com.tina.app.resources.inbox_empty_sub
import com.tina.app.resources.sorted
import com.tina.app.resources.triage_make_event
import com.tina.app.resources.triage_make_note
import com.tina.app.resources.triage_someday
import com.tina.app.resources.triage_this_week
import com.tina.app.resources.undo
import com.tina.app.ui.ItemRow
import com.tina.app.ui.SectionCardItem
import com.tina.app.ui.relativeAge
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onBack: () -> Unit,
    onOpenItem: (com.tina.app.data.Item) -> Unit,
    viewModel: InboxViewModel = koinViewModel(),
) {
    val items by viewModel.items.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sortedText = stringResource(Res.string.sorted)
    val deletedText = stringResource(Res.string.deleted)
    val undoText = stringResource(Res.string.undo)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    fun withUndo(message: String, action: () -> Unit, undo: () -> Unit) {
        action()
        scope.launch {
            val result = snackbarHostState.showSnackbar(message, undoText, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) undo()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.inbox)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(Res.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
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
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.inbox_empty),
                    style = MaterialTheme.typography.titleMedium,
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

        val nowMillis = remember(items) { Clock.System.now().toEpochMilliseconds() }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.id }) { _, item ->
                SectionCardItem(0, 1, Modifier.animateItem()) {
                    ItemRow(
                        item = item,
                        today = today,
                        leading = false,
                        timeText = stringResource(
                            Res.string.inbox_captured,
                            relativeAge(nowMillis - item.createdAt),
                        ),
                        onDelete = {
                            withUndo(deletedText, { viewModel.deleteWithSnapshot(item) }, viewModel::undoDelete)
                        },
                        onRename = { viewModel.rename(item, it) },
                        onOpen = { onOpenItem(item) },
                        extraContent = {
                            LazyRow(
                                Modifier.padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                item {
                                    TriageChip(stringResource(Res.string.date_today)) {
                                        withUndo(sortedText, { viewModel.triage(item, TriageAction.TODAY) }, viewModel::undoTriage)
                                    }
                                }
                                item {
                                    TriageChip(stringResource(Res.string.date_tomorrow)) {
                                        withUndo(sortedText, { viewModel.triage(item, TriageAction.TOMORROW) }, viewModel::undoTriage)
                                    }
                                }
                                item {
                                    TriageChip(stringResource(Res.string.triage_this_week)) {
                                        withUndo(sortedText, { viewModel.triage(item, TriageAction.THIS_WEEK) }, viewModel::undoTriage)
                                    }
                                }
                                item {
                                    TriageChip(stringResource(Res.string.triage_someday)) {
                                        withUndo(sortedText, { viewModel.triage(item, TriageAction.SOMEDAY) }, viewModel::undoTriage)
                                    }
                                }
                                item {
                                    TriageChip(stringResource(Res.string.triage_make_event)) {
                                        withUndo(sortedText, { viewModel.triage(item, TriageAction.MAKE_EVENT) }, viewModel::undoTriage)
                                    }
                                }
                                item {
                                    TriageChip(stringResource(Res.string.triage_make_note)) {
                                        withUndo(sortedText, { viewModel.triage(item, TriageAction.MAKE_NOTE) }, viewModel::undoTriage)
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

@Composable
private fun TriageChip(label: String, onClick: () -> Unit) {
    SuggestionChip(onClick = onClick, label = { Text(label, style = MaterialTheme.typography.labelMedium) })
}
