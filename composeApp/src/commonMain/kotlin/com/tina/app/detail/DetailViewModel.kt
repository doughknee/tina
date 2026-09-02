package com.tina.app.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class DetailViewModel(
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

    fun setTitle(title: String) = edit { it.copy(title = title) }
    fun setBody(body: String) = edit { it.copy(body = body.ifBlank { null }) }
    fun setType(type: ItemType) = edit { it.copy(type = type) }
    fun setPriority(priority: Priority) = edit { it.copy(priority = priority) }
    fun setDate(date: LocalDate?) = edit { it.copy(dueDate = date?.toEpochDays()?.toInt()) }
    fun setTime(time: LocalTime?) = edit { it.copy(dueTime = time?.let { t -> t.hour * 60 + t.minute }) }
    fun setReminder(minutes: Int?) = edit { it.copy(reminderOffsetMinutes = minutes) }
    /** Rule change on a series: the item is the series, so this edits every future occurrence. */
    fun setRrule(rrule: String?) = edit { it.copy(recurrence = rrule) }
    fun setTags(raw: String) = edit { item ->
        item.copy(tags = raw.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() })
    }

    fun toggleCompleted() {
        viewModelScope.launch {
            repository.get(itemId)?.let {
                if (it.completed) repository.uncomplete(itemId) else repository.complete(itemId)
            }
        }
    }

    fun applyImprovement(updated: Item) {
        viewModelScope.launch { repository.update(updated) }
    }

    fun delete(deletedMessage: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val item = repository.get(itemId)
            repository.delete(itemId)
            if (item != null) com.tina.app.ui.PendingUndo.request(deletedMessage) { repository.restore(item) }
            onDeleted()
        }
    }
}
