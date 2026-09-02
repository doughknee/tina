package com.tina.app.search

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.LocalSettings
import com.tina.app.capture.typeLabel
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import com.tina.app.data.SettingsRepository
import com.tina.app.notes.htmlPreview
import com.tina.app.resources.Res
import com.tina.app.resources.search_close
import com.tina.app.resources.search_everything
import com.tina.app.resources.search_no_results
import com.tina.app.resources.search_recent
import com.tina.app.ui.dateLabel
import com.tina.app.ui.timeLabel
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

private const val RECENT_COUNT = 8

class SearchViewModel(
    repository: ItemRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val query = MutableStateFlow("")

    // ponytail: in-memory over the whole table; the LIKE query is gone with the Library
    val results: StateFlow<List<Item>> = combine(repository.observeAll(), query, settingsRepository.settings) { items, q, s ->
        val needle = q.trim()
        val searchable = items.filter { s.searchCompleted || !it.completed }
        if (needle.isEmpty()) return@combine searchable.sortedByDescending { it.updatedAt }.take(RECENT_COUNT)
        searchable.filter { item ->
            (
                item.title.contains(needle, ignoreCase = true) ||
                    item.tags.any { it.contains(needle.removePrefix("#"), ignoreCase = true) } ||
                    (item.body?.let { htmlPreview(it).contains(needle, ignoreCase = true) } == true)
                )
        }.sortedByDescending { it.updatedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }
}

/** Search is a sheet over whatever you were looking at, with its own field. Tap a result to open it. */
@Composable
fun SearchSheet(viewModel: SearchViewModel, onOpenItem: (Item) -> Unit) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val focus = remember { FocusRequester() }
    val use24h = LocalSettings.current.use24h
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        focus.requestFocus()
    }

    Column(Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(56.dp),
        ) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                val placeholder = stringResource(Res.string.search_everything)
                val searchLabel = stringResource(Res.string.search_everything)
                BasicTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp).focusRequester(focus)
                        .onFocusChanged { com.tina.app.ui.KeyBus.textInputActive = it.isFocused }
                        .semantics { contentDescription = searchLabel },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            }
        }

        if (query.isNotBlank() && results.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Outlined.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.search_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
            if (query.isBlank() && results.isNotEmpty()) {
                item("recent") {
                    Text(
                        stringResource(Res.string.search_recent),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
            }
            items(results, key = { it.id }) { item ->
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
                        else -> item.dueLocalDate?.let { dateLabel(it, today) }
                    },
                ).joinToString(" · ")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenItem(item) }
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                        .alpha(if (item.completed) 0.6f else 1f)
                        .animateItem(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        when (item.type) {
                            ItemType.TASK -> Icons.Outlined.TaskAlt
                            ItemType.EVENT -> Icons.Outlined.Event
                            ItemType.NOTE -> Icons.Outlined.Lightbulb
                            ItemType.INBOX -> Icons.Outlined.Inbox
                        },
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                        )
                        Text(
                            supporting,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
