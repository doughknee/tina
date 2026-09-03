package com.tina.app.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.LocalSettings
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import com.tina.app.notes.NoteCard
import com.tina.app.notes.htmlPreview
import com.tina.app.resources.Res
import com.tina.app.resources.back
import com.tina.app.resources.deleted
import com.tina.app.resources.tag_add_to
import com.tina.app.resources.tag_all
import com.tina.app.resources.tag_coming_up
import com.tina.app.resources.tag_counts
import com.tina.app.resources.tag_done_count
import com.tina.app.resources.tag_empty
import com.tina.app.resources.tag_events
import com.tina.app.resources.tag_ideas
import com.tina.app.resources.tag_open_tasks
import com.tina.app.resources.tag_overview
import com.tina.app.resources.tag_tasks
import com.tina.app.resources.undo
import com.tina.app.ui.ItemRow
import com.tina.app.ui.SectionCardItem
import com.tina.app.ui.dateLabel
import com.tina.app.ui.rememberUndoWindow
import com.tina.app.ui.showUndo
import com.tina.app.ui.timeLabel
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Everything carrying one tag, split by type. The overview is the pinned note that carries it. */
data class TagUi(
    val overview: Item? = null,
    val notes: List<Item> = emptyList(),
    val openTasks: List<Item> = emptyList(),
    val doneTasks: Int = 0,
    val events: List<Item> = emptyList(),
) {
    val isEmpty get() = overview == null && notes.isEmpty() && openTasks.isEmpty() && doneTasks == 0 && events.isEmpty()
}

class TagViewModel(
    tag: String,
    private val repository: ItemRepository,
) : ViewModel() {
    val ui = repository.observeTagged()
        .map { list ->
            val items = list.filter { tag in it.tags }
            val notes = items.filter { it.type == ItemType.NOTE }
            val overview = notes.firstOrNull { it.pinned }
            val tasks = items.filter { it.type == ItemType.TASK || it.type == ItemType.INBOX }
            TagUi(
                overview = overview,
                notes = (notes - listOfNotNull(overview)).sortedWith(compareByDescending<Item> { it.pinned }.thenByDescending { it.updatedAt }),
                openTasks = tasks.filterNot { it.completed }.sortedWith(compareBy(nullsLast()) { it.dueDate }),
                doneTasks = tasks.count { it.completed },
                events = items.filter { it.type == ItemType.EVENT }.sortedBy { it.startAt },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagUi())

    private var lastDeleted: Item? = null

    fun toggleComplete(item: Item) {
        viewModelScope.launch {
            if (item.completed) repository.uncomplete(item.id) else repository.complete(item.id)
        }
    }

    fun delete(item: Item) {
        lastDeleted = item
        viewModelScope.launch { repository.delete(item.id) }
    }

    fun undoDelete() {
        val item = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { repository.restore(item) }
    }

    fun rename(item: Item, title: String) {
        viewModelScope.launch { repository.rename(item.id, title) }
    }
}

private enum class TagFilter { ALL, IDEAS, TASKS, EVENTS }

/**
 * A tag is the project: no second hierarchy, just one route that collects notes, tasks and
 * events together. Capturing from here pre-fills the tag, the only implicit organisation.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TagScreen(
    tag: String,
    onBack: () -> Unit,
    onOpenItem: (Item) -> Unit,
    onCapture: (String) -> Unit,
    viewModel: TagViewModel = koinViewModel(key = "tag-$tag") { parametersOf(tag) },
) {
    val ui by viewModel.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val undoWindow = rememberUndoWindow()
    val scope = rememberCoroutineScope()
    val deletedText = stringResource(Res.string.deleted)
    val undoText = stringResource(Res.string.undo)
    val use24h = LocalSettings.current.use24h
    val tz = TimeZone.currentSystemDefault()
    val nowMillis = remember(ui) { Clock.System.now().toEpochMilliseconds() }
    val today = remember(nowMillis) { Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(tz).date }
    var filter by remember { mutableStateOf(TagFilter.ALL) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val ideasCount = ui.notes.size + (if (ui.overview != null) 1 else 0)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("#$tag") },
                subtitle = {
                    Text(
                        stringResource(Res.string.tag_counts, ideasCount, ui.openTasks.size + ui.doneTasks, ui.events.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(Res.string.back))
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            // one tap to capture into this tag: the bar reads like the capture field, prefilled
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Surface(
                        onClick = { onCapture("#$tag ") },
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lightbulb, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                stringResource(Res.string.tag_add_to, tag),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (ui.isEmpty) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Outlined.Sell,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.tag_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            return@Scaffold
        }

        val showIdeas = filter == TagFilter.ALL || filter == TagFilter.IDEAS
        val showTasks = filter == TagFilter.ALL || filter == TagFilter.TASKS
        val showEvents = filter == TagFilter.ALL || filter == TagFilter.EVENTS
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            ui.overview?.let { overview ->
                item("overview") {
                    Surface(
                        onClick = { onOpenItem(overview) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).animateItem(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PushPin, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    stringResource(Res.string.tag_overview).uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                            if (overview.title.isNotBlank()) {
                                Text(
                                    overview.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                            overview.body?.let { htmlPreview(it) }?.takeIf { it.isNotBlank() }?.let { body ->
                                Text(
                                    body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 6,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
            item("rail") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    item {
                        FilterChip(selected = filter == TagFilter.ALL, onClick = { filter = TagFilter.ALL }, label = { Text(stringResource(Res.string.tag_all)) })
                    }
                    item {
                        FilterChip(selected = filter == TagFilter.IDEAS, onClick = { filter = TagFilter.IDEAS }, label = { Text("${stringResource(Res.string.tag_ideas)} $ideasCount") })
                    }
                    item {
                        FilterChip(selected = filter == TagFilter.TASKS, onClick = { filter = TagFilter.TASKS }, label = { Text("${stringResource(Res.string.tag_tasks)} ${ui.openTasks.size + ui.doneTasks}") })
                    }
                    item {
                        FilterChip(selected = filter == TagFilter.EVENTS, onClick = { filter = TagFilter.EVENTS }, label = { Text("${stringResource(Res.string.tag_events)} ${ui.events.size}") })
                    }
                }
            }

            if (showTasks && (ui.openTasks.isNotEmpty() || ui.doneTasks > 0)) {
                item("h-tasks") { Header(stringResource(Res.string.tag_open_tasks)) }
                val rows = ui.openTasks.size + if (ui.doneTasks > 0) 1 else 0
                itemsIndexed(ui.openTasks, key = { _, it -> "t${it.id}" }) { index, item ->
                    SectionCardItem(index, rows, Modifier.animateItem()) {
                        ItemRow(
                            item = item,
                            today = today,
                            timeText = item.dueLocalDate?.let { dateLabel(it, today) },
                            onToggleComplete = { viewModel.toggleComplete(item) },
                            onDelete = {
                                viewModel.delete(item)
                                scope.launch {
                                    if (snackbarHostState.showUndo(deletedText, undoText, undoWindow)) viewModel.undoDelete()
                                }
                            },
                            onRename = { viewModel.rename(item, it) },
                            onOpen = { onOpenItem(item) },
                        )
                    }
                }
                if (ui.doneTasks > 0) {
                    item("done") {
                        SectionCardItem(rows - 1, rows) {
                            Text(
                                stringResource(Res.string.tag_done_count, ui.doneTasks),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }

            if (showEvents && ui.events.isNotEmpty()) {
                item("h-events") { Header(stringResource(Res.string.tag_coming_up)) }
                itemsIndexed(ui.events, key = { _, it -> "e${it.id}" }) { index, item ->
                    SectionCardItem(index, ui.events.size, Modifier.animateItem()) {
                        ItemRow(
                            item = item,
                            today = today,
                            timeText = item.startAt?.let { millis ->
                                val start = Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz)
                                if (item.allDay) dateLabel(start.date, today)
                                else "${dateLabel(start.date, today)} ${timeLabel(start.time, use24h)}"
                            },
                            onDelete = {
                                viewModel.delete(item)
                                scope.launch {
                                    if (snackbarHostState.showUndo(deletedText, undoText, undoWindow)) viewModel.undoDelete()
                                }
                            },
                            onRename = { viewModel.rename(item, it) },
                            onOpen = { onOpenItem(item) },
                        )
                    }
                }
            }

            if (showIdeas && ui.notes.isNotEmpty()) {
                item("h-ideas") { Header(stringResource(Res.string.tag_ideas)) }
                // two columns by hand: a grid cannot nest inside the column
                val pairs = ui.notes.chunked(2)
                itemsIndexed(pairs, key = { _, pair -> "n${pair.first().id}" }) { _, pair ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        pair.forEach { note ->
                            NoteCard(
                                item = note,
                                nowMillis = nowMillis,
                                today = today,
                                onClick = { onOpenItem(note) },
                                onLongClick = { onOpenItem(note) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
    )
}
