package com.tina.app.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val HTML_TAG = Regex("<[^>]*>")

fun htmlPreview(html: String): String = HTML_TAG.replace(html, " ").replace(Regex("\\s+"), " ").trim()

class NotesViewModel(private val repository: ItemRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val gridMode = MutableStateFlow(true)

    /** Set when a note was deleted from its editor; the list screen offers undo. */
    val pendingUndo = MutableStateFlow<Item?>(null)

    val notes: StateFlow<List<Item>> = combine(repository.observeNotes(), query) { notes, q ->
        if (q.isBlank()) notes
        else notes.filter {
            it.title.contains(q, ignoreCase = true) ||
                (it.body?.let { body -> htmlPreview(body).contains(q, ignoreCase = true) } == true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    fun toggleGrid() {
        gridMode.value = !gridMode.value
    }

    fun createNote(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val id = repository.insert(
                Item(title = "", type = ItemType.NOTE, createdAt = now, updatedAt = now, sortOrder = now),
            )
            onCreated(id)
        }
    }

    fun togglePin(item: Item) {
        viewModelScope.launch { repository.update(item.copy(pinned = !item.pinned)) }
    }

    fun delete(item: Item) {
        pendingUndo.value = item
        viewModelScope.launch { repository.delete(item.id) }
    }

    fun undoDelete() {
        val item = pendingUndo.value ?: return
        pendingUndo.value = null
        viewModelScope.launch { repository.restore(item) }
    }

    fun clearPendingUndo() {
        pendingUndo.value = null
    }
}
