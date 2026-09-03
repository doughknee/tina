package com.tina.app.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import kotlin.time.Clock
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
            // opening a note re-emits its content; an unchanged save must not bump updatedAt
            repository.get(itemId)?.let { current -> transform(current).takeIf { it != current }?.let { repository.update(it) } }
        }
    }

    fun saveTitle(title: String) = edit { it.copy(title = title) }
    fun saveBody(html: String) = edit { it.copy(body = html.ifBlank { null }) }
    fun setColor(color: Long?) = edit { it.copy(color = color) }
    fun togglePin() = edit { it.copy(pinned = !it.pinned) }
    fun setTag(tag: String, add: Boolean) = edit {
        it.copy(tags = if (add) (it.tags + tag).distinct() else it.tags - tag)
    }

    /** A pinned note carrying a tag is that tag's overview; pinning is the promotion. */
    fun setOverview(tag: String) = edit { it.copy(pinned = true, tags = (it.tags + tag).distinct()) }

    fun duplicate(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val source = repository.get(itemId) ?: return@launch
            val now = Clock.System.now().toEpochMilliseconds()
            onCreated(repository.insert(source.copy(id = 0, uuid = "", createdAt = now, updatedAt = now, sortOrder = now, pinned = false)))
        }
    }

    /** The note becomes an undated task: it lands on Sort, where a date is one tap away. */
    fun convertToTask(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.changeType(itemId, ItemType.TASK)
            onDone()
        }
    }
}
