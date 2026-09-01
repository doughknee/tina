package com.tina.app.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteEditorViewModel(
    private val itemId: Long,
    private val repository: ItemRepository,
) : ViewModel() {
    val item = repository.observe(itemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun edit(transform: (Item) -> Item) {
        viewModelScope.launch {
            repository.get(itemId)?.let { repository.update(transform(it)) }
        }
    }

    fun saveTitle(title: String) = edit { it.copy(title = title) }
    fun saveBody(html: String) = edit { it.copy(body = html.ifBlank { null }) }
    fun setColor(color: Long?) = edit { it.copy(color = color) }
    fun togglePin() = edit { it.copy(pinned = !it.pinned) }
}
