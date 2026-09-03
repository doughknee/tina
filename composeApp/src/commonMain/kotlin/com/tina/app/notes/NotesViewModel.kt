package com.tina.app.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import com.tina.app.data.SettingsRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val HTML_TAG = Regex("<[^>]*>")
// only a block boundary is a word boundary; "<b>ginger</b>." must not become "ginger ."
private val BLOCK_TAG = Regex("^</?(p|div|br|li|ul|ol|h[1-6]|blockquote|tr|td|th|pre)\b", RegexOption.IGNORE_CASE)

private val HTML_ENTITY = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);")

fun htmlPreview(html: String): String = HTML_TAG.replace(html) { if (BLOCK_TAG.containsMatchIn(it.value)) " " else "" }
    .let { text ->
        HTML_ENTITY.replace(text) { m ->
            val name = m.groupValues[1]
            when {
                name.startsWith("#x") || name.startsWith("#X") ->
                    name.drop(2).toIntOrNull(16)?.toChar()?.toString() ?: m.value
                name.startsWith("#") -> name.drop(1).toIntOrNull()?.toChar()?.toString() ?: m.value
                else -> when (name) {
                    "amp" -> "&"; "lt" -> "<"; "gt" -> ">"; "quot" -> "\""
                    "apos" -> "'"; "nbsp" -> " "
                    else -> m.value
                }
            }
        }
    }
    .replace(Regex("\\s+"), " ").trim()

enum class NotesSort { EDITED, CREATED, TITLE }
enum class NotesLayout { GRID, LIST, LARGE }

data class TagCount(val name: String, val count: Int)

/** The grid, already filtered, sorted and split, so the screen only draws. */
data class NotesUi(
    val pinned: List<Item> = emptyList(),
    val others: List<Item> = emptyList(),
    /** Every tag on any note, most used first. Empty hides the rail. */
    val tags: List<TagCount> = emptyList(),
    /** Tags that carry a pinned note: a project, shown with an underline. */
    val overviewTags: Set<String> = emptySet(),
    val total: Int = 0,
    val sort: NotesSort = NotesSort.EDITED,
    val layout: NotesLayout = NotesLayout.GRID,
) {
    val all: List<Item> get() = pinned + others
    val isEmpty: Boolean get() = pinned.isEmpty() && others.isEmpty()
}

class NotesViewModel(
    private val repository: ItemRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val query = MutableStateFlow("")
    val tagFilter = MutableStateFlow<String?>(null)
    val selection = MutableStateFlow<Set<Long>>(emptySet())

    /** Deleted from the grid or an editor; the list screen offers one undo for the batch. */
    val pendingUndo = MutableStateFlow<List<Item>>(emptyList())

    val ui: StateFlow<NotesUi> = combine(repository.observeNotes(), query, tagFilter, settings.settings) { notes, q, tag, s ->
        val sort = NotesSort.entries.firstOrNull { it.name == s.notesSort } ?: NotesSort.EDITED
        val layout = NotesLayout.entries.firstOrNull { it.name == s.notesLayout } ?: NotesLayout.GRID
        val tags = notes.flatMap { it.tags }.groupingBy { it }.eachCount()
            .map { (name, count) -> TagCount(name, count) }
            .sortedWith(compareByDescending<TagCount> { it.count }.thenBy { it.name })
        val overview = notes.filter { it.pinned }.flatMap { it.tags }.toSet()
        val visible = notes
            .filter { tag == null || tag in it.tags }
            .filter { q.isBlank() || it.matches(q) }
            .sortedWith(
                when (sort) {
                    NotesSort.EDITED -> compareByDescending { it.updatedAt }
                    NotesSort.CREATED -> compareByDescending { it.createdAt }
                    NotesSort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { previewOf(it).let { p -> p.title.ifBlank { p.text } } }
                },
            )
        NotesUi(
            pinned = visible.filter { it.pinned },
            others = visible.filterNot { it.pinned },
            tags = tags,
            overviewTags = overview,
            total = notes.size,
            sort = sort,
            layout = layout,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUi())

    /** Tags across every item type, for the label sheet. */
    val allTags: StateFlow<List<TagCount>> = repository.observeTagged().map { items ->
        items.flatMap { it.tags }.groupingBy { it }.eachCount()
            .map { (name, count) -> TagCount(name, count) }
            .sortedWith(compareByDescending<TagCount> { it.count }.thenBy { it.name })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun Item.matches(q: String) =
        title.contains(q, ignoreCase = true) || (body?.let { htmlPreview(it).contains(q, ignoreCase = true) } == true)

    fun setQuery(value: String) {
        query.value = value
    }

    fun setTagFilter(tag: String?) {
        tagFilter.value = if (tagFilter.value == tag) null else tag
    }

    fun setSort(sort: NotesSort) {
        viewModelScope.launch { settings.setNotesSort(sort.name) }
    }

    fun setLayout(layout: NotesLayout) {
        viewModelScope.launch { settings.setNotesLayout(layout.name) }
    }

    fun createNote(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val id = repository.insert(
                Item(title = "", type = ItemType.NOTE, createdAt = now, updatedAt = now, sortOrder = now, tags = listOfNotNull(tagFilter.value)),
            )
            onCreated(id)
        }
    }

    // ---- selection

    fun toggleSelected(id: Long) {
        selection.value = if (id in selection.value) selection.value - id else selection.value + id
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    private fun selectedItems(): List<Item> = ui.value.all.filter { it.id in selection.value }

    /** Pins the selection; unpins instead when every selected note is already pinned. */
    fun pinSelected() {
        val items = selectedItems()
        val pin = !items.all { it.pinned }
        viewModelScope.launch { items.forEach { repository.update(it.copy(pinned = pin)) } }
        clearSelection()
    }

    fun colorSelected(color: Long?) {
        val items = selectedItems()
        viewModelScope.launch { items.forEach { repository.update(it.copy(color = color)) } }
        clearSelection()
    }

    /** Adds or removes one tag on every selected note; the sheet stays open so several can be set. */
    fun tagSelected(tag: String, add: Boolean) {
        val items = selectedItems()
        viewModelScope.launch {
            items.forEach {
                val tags = if (add) (it.tags + tag).distinct() else it.tags - tag
                if (tags != it.tags) repository.update(it.copy(tags = tags))
            }
        }
    }

    fun deleteSelected() {
        val items = selectedItems()
        clearSelection()
        pendingUndo.value = items
        viewModelScope.launch { items.forEach { repository.delete(it.id) } }
    }

    fun togglePin(item: Item) {
        viewModelScope.launch { repository.update(item.copy(pinned = !item.pinned)) }
    }

    fun delete(item: Item) {
        pendingUndo.value = listOf(item)
        viewModelScope.launch { repository.delete(item.id) }
    }

    fun undoDelete() {
        val items = pendingUndo.value
        pendingUndo.value = emptyList()
        viewModelScope.launch { items.forEach { repository.restore(it) } }
    }

    fun clearPendingUndo() {
        pendingUndo.value = emptyList()
    }
}
