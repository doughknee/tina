package com.tina.app.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tina.app.LocalSettings
import com.tina.app.capture.typeLabel
import com.tina.app.data.Item
import com.tina.app.data.ItemType
import com.tina.app.notes.NotesViewModel
import com.tina.app.notes.htmlPreview
import com.tina.app.resources.Res
import com.tina.app.resources.date_today
import com.tina.app.resources.date_tomorrow
import com.tina.app.resources.deleted
import com.tina.app.resources.filter_all
import com.tina.app.resources.filter_done
import com.tina.app.resources.filter_events
import com.tina.app.resources.filter_inbox
import com.tina.app.resources.filter_notes
import com.tina.app.resources.filter_tasks
import com.tina.app.resources.inbox_captured
import com.tina.app.resources.library_empty
import com.tina.app.resources.library_everything
import com.tina.app.resources.library_triage
import com.tina.app.resources.note_new
import com.tina.app.resources.note_pin
import com.tina.app.resources.note_unpin
import com.tina.app.resources.note_untitled
import com.tina.app.resources.search_close
import com.tina.app.resources.search_everything
import com.tina.app.resources.settings
import com.tina.app.resources.sorted
import com.tina.app.resources.triage_make_event
import com.tina.app.resources.triage_make_note
import com.tina.app.resources.triage_someday
import com.tina.app.resources.triage_this_week
import com.tina.app.resources.undo
import com.tina.app.ui.ItemRow
import com.tina.app.ui.SectionCardItem
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Library = Notes + Inbox + Search + Done. The search field is the header, the filter rail
 * scopes the one list, and the Notes scope is the only view-mode switch (the staggered grid).
 */
@Composable
fun LibraryScreen(
    onOpenSettings: () -> Unit,
    onOpenItem: (Item) -> Unit,
    onOpenNote: (Long) -> Unit,
    searchFocusNonce: Int,
    viewModel: LibraryViewModel,
    notesViewModel: NotesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val query by viewModel.query.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val pendingNoteUndo by notesViewModel.pendingUndo.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val undoWindow = rememberUndoWindow()
    val scope = rememberCoroutineScope()
    val searchFocus = remember { FocusRequester() }
    val deletedText = stringResource(Res.string.deleted)
    val sortedText = stringResource(Res.string.sorted)
    val undoText = stringResource(Res.string.undo)
    val use24h = LocalSettings.current.use24h
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val nowMillis = remember(state) { Clock.System.now().toEpochMilliseconds() }

    LaunchedEffect(searchFocusNonce) {
        if (searchFocusNonce > 0) searchFocus.requestFocus()
    }
    LaunchedEffect(pendingNoteUndo) {
        if (pendingNoteUndo == null) return@LaunchedEffect
        if (snackbarHostState.showUndo(deletedText, undoText, undoWindow)) notesViewModel.undoDelete() else notesViewModel.clearPendingUndo()
    }

    fun withUndo(message: String, action: () -> Unit, undo: () -> Unit) {
        action()
        scope.launch {
            if (snackbarHostState.showUndo(message, undoText, undoWindow)) undo()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (filter == LibraryFilter.NOTES) {
                FloatingActionButton(onClick = { notesViewModel.createNote(onOpenNote) }) {
                    Icon(Icons.Outlined.Add, stringResource(Res.string.note_new))
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // the search field is the header
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(56.dp),
            ) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    val placeholder = stringResource(Res.string.search_everything)
                    BasicTextField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp).focusRequester(searchFocus),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {}),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
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
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Outlined.Close, stringResource(Res.string.search_close))
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(Res.string.settings))
                    }
                }
            }

            // the filter rail never wraps; the bleeding right edge is the affordance
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                items(LibraryFilter.entries.size) { index ->
                    val option = LibraryFilter.entries[index]
                    val selected = option == filter
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setFilter(option) },
                        label = { Text(filterLabel(option)) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }
                        } else null,
                        trailingIcon = if (option == LibraryFilter.INBOX && state.inboxCount > 0) {
                            { Badge { Text(state.inboxCount.toString()) } }
                        } else null,
                    )
                }
            }

            if (filter == LibraryFilter.NOTES) {
                NotesGrid(state.rows, onOpenNote, viewModel::togglePin)
                return@Column
            }

            if (state.triage.isEmpty() && state.rows.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        if (query.isBlank()) Icons.Outlined.Inbox else Icons.Outlined.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(Res.string.library_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                return@Column
            }

            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                if (state.triage.isNotEmpty()) {
                    item(key = "triage-header") { SectionHeader(stringResource(Res.string.library_triage)) }
                    itemsIndexed(state.triage, key = { _, it -> "triage-${it.id}" }) { _, item ->
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
                                    LazyRow(
                                        Modifier.padding(bottom = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        val chips = listOf(
                                            TriageAction.TODAY to Res.string.date_today,
                                            TriageAction.TOMORROW to Res.string.date_tomorrow,
                                            TriageAction.THIS_WEEK to Res.string.triage_this_week,
                                            TriageAction.SOMEDAY to Res.string.triage_someday,
                                            TriageAction.MAKE_EVENT to Res.string.triage_make_event,
                                            TriageAction.MAKE_NOTE to Res.string.triage_make_note,
                                        )
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
                if (state.rows.isNotEmpty()) {
                    if (state.triage.isNotEmpty()) {
                        item(key = "rows-header") { SectionHeader(stringResource(Res.string.library_everything)) }
                    }
                    itemsIndexed(state.rows, key = { _, it -> it.id }) { index, item ->
                        SectionCardItem(index, state.rows.size, Modifier.animateItem()) {
                            val supporting = listOfNotNull(
                                typeLabel(item.type),
                                when (item.type) {
                                    ItemType.NOTE -> item.body?.let { htmlPreview(it) }?.takeIf { it.isNotBlank() }
                                    ItemType.EVENT -> item.startAt?.let { start ->
                                        val local = Instant.fromEpochMilliseconds(start).toLocalDateTime(TimeZone.currentSystemDefault())
                                        listOfNotNull(
                                            dateLabel(local.date, today),
                                            if (item.allDay) null else timeLabel(local.time, use24h),
                                        ).joinToString(" ")
                                    }
                                    else -> when {
                                        item.completed && item.completedAt != null ->
                                            relativeAge(nowMillis - item.completedAt!!).let { "done $it" }
                                        else -> item.dueLocalDate?.let { dateLabel(it, today) }
                                    }
                                },
                            ).joinToString(" · ")
                            ItemRow(
                                item = item,
                                today = today,
                                timeText = supporting,
                                leadingIcon = when (item.type) {
                                    ItemType.EVENT -> Icons.Outlined.Event
                                    ItemType.NOTE -> Icons.Outlined.Lightbulb
                                    ItemType.INBOX -> Icons.Outlined.Inbox
                                    ItemType.TASK -> null
                                },
                                onToggleComplete = if (item.type == ItemType.TASK) {
                                    { viewModel.toggleComplete(item) }
                                } else null,
                                onDelete = { withUndo(deletedText, { viewModel.delete(item) }, viewModel::undoDelete) },
                                onRename = { viewModel.rename(item, it) },
                                onOpen = { onOpenItem(item) },
                                modifier = Modifier.alpha(if (item.completed) 0.6f else 1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun filterLabel(filter: LibraryFilter): String = stringResource(
    when (filter) {
        LibraryFilter.ALL -> Res.string.filter_all
        LibraryFilter.INBOX -> Res.string.filter_inbox
        LibraryFilter.TASKS -> Res.string.filter_tasks
        LibraryFilter.EVENTS -> Res.string.filter_events
        LibraryFilter.NOTES -> Res.string.filter_notes
        LibraryFilter.DONE -> Res.string.filter_done
    },
)

@Composable
private fun SectionHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun NotesGrid(notes: List<Item>, onOpenNote: (Long) -> Unit, onTogglePin: (Item) -> Unit) {
    if (notes.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Outlined.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.library_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        return
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        items(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                onClick = { onOpenNote(note.id) },
                onTogglePin = { onTogglePin(note) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun NoteCard(
    note: Item,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = note.color?.let { Color(it).copy(alpha = 0.18f) }
                ?: MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Box {
            Column(Modifier.padding(16.dp)) {
                Text(
                    note.title.ifBlank { stringResource(Res.string.note_untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(end = 28.dp),
                )
                note.body?.let { body ->
                    val preview = remember(body) { htmlPreview(body) }
                    if (preview.isNotBlank()) {
                        Text(
                            preview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .clickable(onClick = onTogglePin)
                    .padding(12.dp),
            ) {
                Icon(
                    Icons.Outlined.PushPin,
                    stringResource(if (note.pinned) Res.string.note_unpin else Res.string.note_pin),
                    modifier = Modifier.size(16.dp),
                    tint = if (note.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
