package com.tina.app.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tina.app.data.Item
import com.tina.app.resources.Res
import com.tina.app.resources.deleted
import com.tina.app.resources.note_new
import com.tina.app.resources.note_pin
import com.tina.app.resources.note_unpin
import com.tina.app.resources.note_untitled
import com.tina.app.resources.ideas_empty_hint
import com.tina.app.resources.notes_empty
import com.tina.app.resources.notes_no_matches
import com.tina.app.resources.search
import com.tina.app.resources.search_close
import com.tina.app.resources.settings
import com.tina.app.resources.tab_notes
import com.tina.app.resources.undo
import com.tina.app.ui.rememberUndoWindow
import com.tina.app.ui.showUndo
import org.jetbrains.compose.resources.stringResource

/** Notes = write. The staggered grid, with an inline filter in the title and a FAB for a new note. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onOpenSettings: () -> Unit,
    onOpenNote: (Long) -> Unit,
    viewModel: NotesViewModel,
) {
    val notes by viewModel.notes.collectAsState()
    val query by viewModel.query.collectAsState()
    val pendingUndo by viewModel.pendingUndo.collectAsState()
    var searching by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val undoWindow = rememberUndoWindow()
    val deletedText = stringResource(Res.string.deleted)
    val undoText = stringResource(Res.string.undo)

    LaunchedEffect(pendingUndo) {
        if (pendingUndo == null) return@LaunchedEffect
        if (snackbarHostState.showUndo(deletedText, undoText, undoWindow)) viewModel.undoDelete() else viewModel.clearPendingUndo()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searching) {
                        TextField(
                            value = query,
                            onValueChange = viewModel::setQuery,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(Res.string.search)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        )
                    } else {
                        Text(stringResource(Res.string.tab_notes), style = MaterialTheme.typography.titleLargeEmphasized)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (searching) viewModel.setQuery("")
                        searching = !searching
                    }) {
                        Icon(
                            if (searching) Icons.Outlined.Close else Icons.Outlined.Search,
                            stringResource(if (searching) Res.string.search_close else Res.string.search),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(Res.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.createNote(onOpenNote) }) {
                Icon(Icons.Outlined.Add, stringResource(Res.string.note_new))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (notes.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding),
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
                    if (query.isBlank()) stringResource(Res.string.notes_empty) else stringResource(Res.string.notes_no_matches, query),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                )
                if (query.isBlank()) Text(
                    stringResource(Res.string.ideas_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp),
                )
            }
            return@Scaffold
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(minSize = 140.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
        ) {
            items(notes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onClick = { onOpenNote(note.id) },
                    onTogglePin = { viewModel.togglePin(note) },
                    modifier = Modifier.animateItem(),
                )
            }
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
                    style = MaterialTheme.typography.titleMediumEmphasized,
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
