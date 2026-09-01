package com.tina.app.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Sell
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
import com.tina.app.ui.rememberUndoWindow
import com.tina.app.ui.showUndo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.LocalSettings
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import com.tina.app.resources.Res
import com.tina.app.resources.back
import com.tina.app.resources.deleted
import com.tina.app.resources.tag_empty
import com.tina.app.resources.undo
import com.tina.app.ui.ItemRow
import com.tina.app.ui.SectionCardItem
import com.tina.app.ui.dateLabel
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

class TagViewModel(
    tag: String,
    private val repository: ItemRepository,
) : ViewModel() {
    val items = repository.observeTagged()
        .map { list -> list.filter { tag in it.tags }.sortedBy { it.completed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagScreen(
    tag: String,
    onBack: () -> Unit,
    onOpenItem: (Item) -> Unit,
    viewModel: TagViewModel = koinViewModel(key = "tag-$tag") { parametersOf(tag) },
) {
    val items by viewModel.items.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val undoWindow = rememberUndoWindow()
    val scope = rememberCoroutineScope()
    val deletedText = stringResource(Res.string.deleted)
    val undoText = stringResource(Res.string.undo)
    val use24h = LocalSettings.current.use24h
    val tz = TimeZone.currentSystemDefault()
    val today = remember { Clock.System.now().toLocalDateTime(tz).date }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("#$tag") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(Res.string.back))
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
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
                SectionCardItem(index, items.size, Modifier.animateItem()) {
                    ItemRow(
                        item = item,
                        today = today,
                        timeText = when (item.type) {
                            ItemType.TASK -> item.dueLocalDate?.let { dateLabel(it, today) }
                            ItemType.EVENT -> item.startAt?.let { millis ->
                                val start = Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz)
                                if (item.allDay) dateLabel(start.date, today)
                                else "${dateLabel(start.date, today)} ${timeLabel(start.time, use24h)}"
                            }
                            else -> null
                        },
                        onToggleComplete = if (item.type == ItemType.TASK) {
                            { viewModel.toggleComplete(item) }
                        } else null,
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
    }
}
