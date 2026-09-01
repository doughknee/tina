package com.tina.app.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.SearchOff
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.LocalSettings
import com.tina.app.capture.typeLabel
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import com.tina.app.resources.Res
import com.tina.app.resources.back
import com.tina.app.resources.deleted
import com.tina.app.resources.search_everything
import com.tina.app.resources.search_no_results
import com.tina.app.resources.search_tags
import com.tina.app.resources.undo
import com.tina.app.ui.ItemRow
import com.tina.app.ui.dateLabel
import com.tina.app.ui.timeLabel
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

class SearchViewModel(
    private val repository: ItemRepository,
    settingsRepository: com.tina.app.data.SettingsRepository,
) : ViewModel() {
    val query = MutableStateFlow("")
    private var lastDeleted: Item? = null

    private val includeTrashed = settingsRepository.settings
        .map { it.searchCompleted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    @OptIn(ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<Item>> = kotlinx.coroutines.flow.combine(query, includeTrashed) { q, t -> q to t }
        .flatMapLatest { (q, includeTrash) ->
            if (q.isBlank()) flowOf(emptyList()) else repository.search(q.trim(), includeTrash)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every distinct tag in use, for the browse row shown while the query is empty. */
    val tags: StateFlow<List<String>> = repository.observeTagged()
        .map { list -> list.flatMap { it.tags }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

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
fun SearchScreen(
    onBack: () -> Unit,
    onOpenItem: (Item) -> Unit,
    onOpenTag: (String) -> Unit = {},
    viewModel: SearchViewModel = koinViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedText = stringResource(Res.string.deleted)
    val undoText = stringResource(Res.string.undo)
    val use24h = LocalSettings.current.use24h
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        placeholder = { Text(stringResource(Res.string.search_everything)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(Res.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (query.isBlank()) {
            val tags by viewModel.tags.collectAsState()
            if (tags.isNotEmpty()) {
                Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                    Text(
                        stringResource(Res.string.search_tags),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    )
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        tags.forEach { tag ->
                            androidx.compose.material3.AssistChip(
                                onClick = { onOpenTag(tag) },
                                label = { Text("#$tag") },
                            )
                        }
                    }
                }
            }
            return@Scaffold
        }
        if (query.isNotBlank() && results.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Outlined.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.search_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(results, key = { it.id }) { item ->
                Column(Modifier.animateItem()) {
                    Text(
                        typeLabel(item.type),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    )
                    ItemRow(
                        item = item,
                        today = today,
                        timeText = when {
                            item.type == ItemType.TASK -> item.dueLocalDate?.let { dateLabel(it, today) }
                            else -> item.dueLocalTime?.let { timeLabel(it, use24h) }
                        },
                        onToggleComplete = if (item.type == ItemType.TASK) {
                            { viewModel.toggleComplete(item) }
                        } else null,
                        onDelete = {
                            viewModel.delete(item)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    deletedText, undoText, duration = SnackbarDuration.Short,
                                )
                                if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
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
