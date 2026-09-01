package com.tina.app.ui.settings.subpages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.resources.Res
import com.tina.app.resources.back
import com.tina.app.resources.delete
import com.tina.app.resources.tags_count
import com.tina.app.resources.tags_empty
import com.tina.app.resources.tags_merged
import com.tina.app.resources.tags_removed
import com.tina.app.resources.tags_renamed
import com.tina.app.resources.tags_title
import com.tina.app.resources.undo
import com.tina.app.ui.rememberUndoWindow
import com.tina.app.ui.settings.SettingsRowSurface
import com.tina.app.ui.showUndo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

data class TagEntry(val name: String, val count: Int)

class TagManagerViewModel(private val repository: ItemRepository) : ViewModel() {
    val tags = repository.observeTagged()
        .map { items ->
            items.flatMap { it.tags }
                .groupingBy { it }.eachCount()
                .map { (name, count) -> TagEntry(name, count) }
                .sortedBy { it.name }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var snapshot: List<Item> = emptyList()

    /** True when the new name already exists — i.e. this was a merge, not a rename. */
    fun rename(from: String, to: String, onDone: (merged: Boolean) -> Unit) {
        val merged = tags.value.any { it.name == to.trim().lowercase().removePrefix("#") && it.name != from }
        viewModelScope.launch {
            snapshot = repository.renameTag(from, to)
            if (snapshot.isNotEmpty()) onDone(merged)
        }
    }

    fun remove(tag: String, onDone: () -> Unit) {
        viewModelScope.launch {
            snapshot = repository.removeTag(tag)
            if (snapshot.isNotEmpty()) onDone()
        }
    }

    fun undo() {
        val batch = snapshot
        snapshot = emptyList()
        viewModelScope.launch { repository.restoreAll(batch) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagerScreen(onBack: () -> Unit, viewModel: TagManagerViewModel = koinViewModel()) {
    val tags by viewModel.tags.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val undoWindow = rememberUndoWindow()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<String?>(null) }

    val renamedText = stringResource(Res.string.tags_renamed)
    val mergedText = stringResource(Res.string.tags_merged)
    val removedText = stringResource(Res.string.tags_removed)
    val undoText = stringResource(Res.string.undo)

    fun announce(message: String) {
        scope.launch { if (snackbarHostState.showUndo(message, undoText, undoWindow)) viewModel.undo() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.tags_title), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (tags.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(
                    stringResource(Res.string.tags_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(tags, key = { _, it -> it.name }) { index, tag ->
                SettingsRowSurface(index = index, count = tags.size, modifier = Modifier.animateItem()) {
                    if (editing == tag.name) {
                        // inline rename, matching the app's tap-to-edit pattern — never a dialog
                        var text by remember(tag.name) { mutableStateOf(tag.name) }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                editing = null
                                viewModel.rename(tag.name, text) { merged ->
                                    announce(if (merged) mergedText else renamedText)
                                }
                            }),
                        )
                    } else {
                        ListItem(
                            headlineContent = { Text("#${tag.name}", style = MaterialTheme.typography.bodyLarge) },
                            supportingContent = {
                                Text(
                                    stringResource(Res.string.tags_count, tag.count),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    viewModel.remove(tag.name) { announce(removedText) }
                                }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        stringResource(Res.string.delete),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .clickable { editing = tag.name }
                                .semantics(mergeDescendants = true) {},
                        )
                    }
                }
            }
        }
    }
}
