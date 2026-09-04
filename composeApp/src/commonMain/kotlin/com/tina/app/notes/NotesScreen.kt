package com.tina.app.notes

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tina.app.data.Item
import com.tina.app.resources.Res
import com.tina.app.resources.delete
import com.tina.app.resources.deleted
import com.tina.app.resources.ideas_empty_hint
import com.tina.app.resources.note_new
import com.tina.app.resources.note_open_tag
import com.tina.app.resources.note_pin
import com.tina.app.resources.notes_all
import com.tina.app.resources.notes_color
import com.tina.app.resources.notes_count
import com.tina.app.resources.notes_count_one
import com.tina.app.resources.notes_deleted
import com.tina.app.resources.notes_empty
import com.tina.app.resources.notes_everything
import com.tina.app.resources.notes_grid
import com.tina.app.resources.notes_label
import com.tina.app.resources.notes_large
import com.tina.app.resources.notes_layout
import com.tina.app.resources.notes_list
import com.tina.app.resources.notes_no_matches
import com.tina.app.resources.notes_pinned
import com.tina.app.resources.notes_selected
import com.tina.app.resources.notes_sort
import com.tina.app.resources.notes_sort_by
import com.tina.app.resources.notes_sort_created
import com.tina.app.resources.notes_sort_edited
import com.tina.app.resources.notes_sort_title
import com.tina.app.resources.search
import com.tina.app.resources.search_close
import com.tina.app.resources.settings
import com.tina.app.resources.tab_notes
import com.tina.app.resources.tag_sheet_title
import com.tina.app.resources.undo
import com.tina.app.ui.ColorSwatchRow
import com.tina.app.ui.ConnectedButtonGroup
import com.tina.app.ui.SectionCardItem
import com.tina.app.ui.rememberUndoWindow
import com.tina.app.ui.showUndo
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/**
 * Ideas = write. A staggered grid split into Pinned and Everything else, a tag rail, long-press
 * selection, a sort/layout sheet, and search results as rows.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun NotesScreen(
    onOpenSettings: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenTag: (String) -> Unit,
    viewModel: NotesViewModel,
) {
    val ui by viewModel.ui.collectAsState()
    val query by viewModel.query.collectAsState()
    val tagFilter by viewModel.tagFilter.collectAsState()
    val selection by viewModel.selection.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val pendingUndo by viewModel.pendingUndo.collectAsState()
    var searching by remember { mutableStateOf(false) }
    var sortSheet by remember { mutableStateOf(false) }
    var colorSheet by remember { mutableStateOf(false) }
    var tagSheet by remember { mutableStateOf(false) }
    val selectionMode = selection.isNotEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val undoWindow = rememberUndoWindow()
    val haptic = LocalHapticFeedback.current
    val deletedText = stringResource(Res.string.deleted)
    val deletedMany = stringResource(Res.string.notes_deleted, pendingUndo.size)
    val undoText = stringResource(Res.string.undo)
    val nowMillis = remember(ui) { Clock.System.now().toEpochMilliseconds() }
    val today = remember(nowMillis) { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    // not while a note or tag page sits above the shell: that back belongs to the page
    BackHandler(enabled = selectionMode && !com.tina.app.ui.KeyBus.pageOpen) { viewModel.clearSelection() }

    LaunchedEffect(pendingUndo) {
        if (pendingUndo.isEmpty()) return@LaunchedEffect
        val message = if (pendingUndo.size == 1) deletedText else deletedMany
        if (snackbarHostState.showUndo(message, undoText, undoWindow)) viewModel.undoDelete() else viewModel.clearPendingUndo()
    }

    // read the selection at tap time: a memoised reference to these would otherwise keep the
    // selectionMode it was created with and open a note instead of selecting it
    val open: (Item) -> Unit = { item ->
        if (viewModel.selection.value.isNotEmpty()) viewModel.toggleSelected(item.id) else onOpenNote(item.id)
    }
    val select: (Item) -> Unit = { item ->
        if (viewModel.selection.value.isEmpty()) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.toggleSelected(item.id)
    }

    Scaffold(
        topBar = {
            AnimatedContent(targetState = selectionMode, label = "notes-bar") { selecting ->
                if (selecting) {
                    TopAppBar(
                        title = { Text(stringResource(Res.string.notes_selected, selection.size), style = MaterialTheme.typography.titleLarge) },
                        navigationIcon = {
                            IconButton(onClick = viewModel::clearSelection) {
                                Icon(Icons.Outlined.Close, stringResource(Res.string.search_close))
                            }
                        },
                        actions = {
                            IconButton(onClick = viewModel::pinSelected) { Icon(Icons.Outlined.PushPin, stringResource(Res.string.note_pin)) }
                            IconButton(onClick = { colorSheet = true }) { Icon(Icons.Outlined.Palette, stringResource(Res.string.notes_color)) }
                            IconButton(onClick = { tagSheet = true }) { Icon(Icons.Outlined.Label, stringResource(Res.string.notes_label)) }
                            IconButton(onClick = viewModel::deleteSelected) { Icon(Icons.Outlined.Delete, stringResource(Res.string.delete)) }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                } else {
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
                            IconButton(onClick = { sortSheet = true }) {
                                Icon(Icons.AutoMirrored.Outlined.Sort, stringResource(Res.string.notes_sort))
                            }
                            IconButton(onClick = onOpenSettings) {
                                Icon(Icons.Outlined.Settings, stringResource(Res.string.settings))
                            }
                        },
                    )
                }
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(onClick = { viewModel.createNote(onOpenNote) }, modifier = Modifier.padding(bottom = 8.dp)) {
                    Icon(Icons.Outlined.Add, stringResource(Res.string.note_new))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!searching && ui.tags.isNotEmpty()) {
                TagRail(
                    tags = ui.tags,
                    selected = tagFilter,
                    overview = ui.overviewTags,
                    total = ui.total,
                    onSelect = viewModel::setTagFilter,
                    onOpenTag = onOpenTag,
                )
            }
            when {
                searching && query.isNotBlank() -> SearchResults(ui.all, query, nowMillis, today, open)
                ui.isEmpty -> EmptyNotes(query = query, filtered = tagFilter != null)
                ui.layout == NotesLayout.LIST -> NotesList(ui, nowMillis, today, selection, selectionMode, open, select)
                else -> NotesGrid(ui, nowMillis, today, selection, selectionMode, open, select, viewModel::toggleChecklistItem)
            }
        }
    }

    if (sortSheet) {
        SortSheet(ui.sort, ui.layout, viewModel::setSort, viewModel::setLayout) { sortSheet = false }
    }
    if (colorSheet) {
        val current = ui.all.filter { it.id in selection }.map { it.color }.distinct().singleOrNull()
        ModalBottomSheet(onDismissRequest = { colorSheet = false }) {
            Text(
                stringResource(Res.string.notes_color),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 40.dp)) {
                ColorSwatchRow(selected = current) {
                    viewModel.colorSelected(it)
                    colorSheet = false
                }
            }
        }
    }
    if (tagSheet) {
        val selected = ui.all.filter { it.id in selection }
        val common = selected.map { it.tags.toSet() }.reduceOrNull { a, b -> a intersect b }.orEmpty()
        TagSheet(
            title = stringResource(Res.string.tag_sheet_title, selected.size),
            tags = allTags,
            checked = common,
            onToggle = viewModel::tagSelected,
            onDismiss = { tagSheet = false },
        )
    }
}

@Composable
private fun TagRail(
    tags: List<TagCount>,
    selected: String?,
    overview: Set<String>,
    total: Int,
    onSelect: (String?) -> Unit,
    onOpenTag: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 4.dp),
    ) {
        item("all") {
            TagChip("${stringResource(Res.string.notes_all)} $total", selected = selected == null, onClick = { onSelect(null) })
        }
        items(tags, key = { it.name }) { tag ->
            TagChip(
                "#${tag.name} ${tag.count}",
                selected = selected == tag.name,
                project = tag.name in overview,
                onClick = { onSelect(tag.name) },
                onLongClick = { onOpenTag(tag.name) },
            )
        }
    }
}

/**
 * A filter chip drawn by hand so one gesture handler owns both the tap (filter) and the hold
 * (open the tag): stock FilterChip keeps its own click and swallows a long press. A tag with an
 * overview note is a project; the 3dp underline says so without a second concept.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    project: Boolean = false,
) {
    val underline = MaterialTheme.colorScheme.primary
    val openLabel = stringResource(Res.string.note_open_tag)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .height(32.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = openLabel)
            .semantics { role = Role.Checkbox; this.selected = selected }
            .drawBehind {
                if (project) {
                    val h = 3.dp.toPx()
                    drawRect(underline, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
                }
            },
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Outlined.Check, null, Modifier.padding(end = 6.dp).size(18.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun SectionHeader(text: String, pinned: Boolean = false, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (pinned) {
            Icon(
                Icons.Filled.PushPin,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp).size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NotesGrid(
    ui: NotesUi,
    nowMillis: Long,
    today: kotlinx.datetime.LocalDate,
    selection: Set<Long>,
    selectionMode: Boolean,
    onOpen: (Item) -> Unit,
    onSelect: (Item) -> Unit,
    onToggleItem: ((Item, Int) -> Unit)? = null,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // adaptive gives three cramped columns on a large phone; two on phones, adaptive on desktop
        val wide = maxWidth > 600.dp
        val columns = when {
            wide -> StaggeredGridCells.Adaptive(minSize = 220.dp)
            ui.layout == NotesLayout.LARGE -> StaggeredGridCells.Fixed(1)
            else -> StaggeredGridCells.Fixed(2)
        }
        LazyVerticalStaggeredGrid(
            columns = columns,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
        ) {
            if (ui.pinned.isNotEmpty()) {
                item(key = "h-pinned", span = StaggeredGridItemSpan.FullLine) {
                    SectionHeader(stringResource(Res.string.notes_pinned), pinned = true, modifier = Modifier.animateItem())
                }
                items(ui.pinned, key = { it.id }) { note ->
                    NoteCard(
                        item = note,
                        nowMillis = nowMillis,
                        today = today,
                        onClick = { onOpen(note) },
                        onLongClick = { onSelect(note) },
                        selected = note.id in selection,
                        selectionMode = selectionMode,
                        showPin = false,
                        onToggleItem = onToggleItem?.let { f -> { i -> f(note, i) } },
                        modifier = Modifier.animateItem(),
                    )
                }
                if (ui.others.isNotEmpty()) {
                    item(key = "h-others", span = StaggeredGridItemSpan.FullLine) {
                        SectionHeader(stringResource(Res.string.notes_everything), modifier = Modifier.animateItem())
                    }
                }
            }
            items(ui.others, key = { it.id }) { note ->
                NoteCard(
                    item = note,
                    nowMillis = nowMillis,
                    today = today,
                    onClick = { onOpen(note) },
                    onLongClick = { onSelect(note) },
                    selected = note.id in selection,
                    selectionMode = selectionMode,
                    onToggleItem = onToggleItem?.let { f -> { i -> f(note, i) } },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun NotesList(
    ui: NotesUi,
    nowMillis: Long,
    today: kotlinx.datetime.LocalDate,
    selection: Set<Long>,
    selectionMode: Boolean,
    onOpen: (Item) -> Unit,
    onSelect: (Item) -> Unit,
) {
    val pinnedText = stringResource(Res.string.notes_pinned)
    val othersText = stringResource(Res.string.notes_everything)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)) {
        fun group(key: String, header: String?, pinned: Boolean, notes: List<Item>) {
            if (notes.isEmpty()) return
            if (header != null) {
                item(key = "h-$key") {
                    SectionHeader(header, pinned, Modifier.padding(horizontal = 20.dp).animateItem())
                }
            }
            itemsIndexed(notes, key = { _, it -> it.id }) { index, note ->
                SectionCardItem(index, notes.size, Modifier.animateItem()) {
                    NoteRow(
                        item = note,
                        nowMillis = nowMillis,
                        today = today,
                        onClick = { onOpen(note) },
                        onLongClick = { onSelect(note) },
                        selected = note.id in selection,
                        selectionMode = selectionMode,
                    )
                }
            }
        }
        group("p", pinnedText, true, ui.pinned)
        // one group needs no header
        group("o", if (ui.pinned.isEmpty()) null else othersText, false, ui.others)
    }
}

@Composable
private fun SearchResults(
    notes: List<Item>,
    query: String,
    nowMillis: Long,
    today: kotlinx.datetime.LocalDate,
    onOpen: (Item) -> Unit,
) {
    if (notes.isEmpty()) {
        EmptyNotes(query = query, filtered = false)
        return
    }
    val highlight = SpanStyle(
        background = MaterialTheme.colorScheme.primaryContainer,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
        item("count") {
            Text(
                if (notes.size == 1) stringResource(Res.string.notes_count_one) else stringResource(Res.string.notes_count, notes.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        itemsIndexed(notes, key = { _, it -> it.id }) { index, note ->
            SectionCardItem(index, notes.size, Modifier.animateItem()) {
                val preview = remember(note) { previewOf(note) }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = false, role = Role.Button, onClick = { onOpen(note) })
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    val headline = preview.title.ifBlank { preview.text }
                    Text(
                        snippet(headline, query, highlight, window = 0),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val body = when (preview.shape) {
                        NoteShape.LIST -> preview.items.joinToString(" · ") { it.text }
                        NoteShape.TITLED -> preview.text
                        NoteShape.SCRAP -> ""
                    }
                    if (body.isNotBlank()) {
                        Text(
                            snippet(body, query, highlight, window = 90),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    NoteMeta(note, nowMillis, today, Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        }
    }
}

/** The text around the first match, the match itself highlighted. [window] 0 keeps the whole text. */
private fun snippet(text: String, query: String, style: SpanStyle, window: Int) = buildAnnotatedString {
    val idx = text.indexOf(query, ignoreCase = true)
    if (idx < 0) {
        append(if (window > 0 && text.length > window * 2) text.take(window * 2) + "…" else text)
        return@buildAnnotatedString
    }
    var start = 0
    var end = text.length
    if (window > 0) {
        start = (idx - window / 3).coerceAtLeast(0)
        // back up to a word boundary so the snippet never opens mid-word
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        end = (idx + query.length + window).coerceAtMost(text.length)
    }
    if (start > 0) append("…")
    append(text.substring(start, idx))
    withStyle(style) { append(text.substring(idx, idx + query.length)) }
    append(text.substring(idx + query.length, end))
    if (end < text.length) append("…")
}

@Composable
private fun EmptyNotes(query: String, filtered: Boolean) {
    Column(
        Modifier.fillMaxSize().padding(bottom = 96.dp),
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
        if (query.isBlank() && !filtered) Text(
            stringResource(Res.string.ideas_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSheet(
    sort: NotesSort,
    layout: NotesLayout,
    onSort: (NotesSort) -> Unit,
    onLayout: (NotesLayout) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(Res.string.notes_sort_by),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        NotesSort.entries.forEach { option ->
            val label = when (option) {
                NotesSort.EDITED -> stringResource(Res.string.notes_sort_edited)
                NotesSort.CREATED -> stringResource(Res.string.notes_sort_created)
                NotesSort.TITLE -> stringResource(Res.string.notes_sort_title)
            }
            val active = option == sort
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .selectable(selected = active, role = Role.RadioButton, onClick = { onSort(option) })
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    if (active) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.size(16.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            stringResource(Res.string.notes_layout),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        val layouts = NotesLayout.entries
        ConnectedButtonGroup(
            count = layouts.size,
            selectedIndex = layouts.indexOf(layout),
            onSelect = { onLayout(layouts[it]) },
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 40.dp),
        ) { index, _ ->
            val (icon, label) = when (layouts[index]) {
                NotesLayout.GRID -> Icons.Outlined.GridView to stringResource(Res.string.notes_grid)
                NotesLayout.LIST -> Icons.AutoMirrored.Outlined.ViewList to stringResource(Res.string.notes_list)
                NotesLayout.LARGE -> Icons.Outlined.ViewAgenda to stringResource(Res.string.notes_large)
            }
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(label, maxLines = 1)
        }
    }
}
