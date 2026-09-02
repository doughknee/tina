package com.tina.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

enum class ItemType { INBOX, TASK, EVENT, NOTE }

enum class Priority { NONE, LOW, MEDIUM, HIGH }

/**
 * The one concept in the app. Type-specific fields are nullable columns so an
 * item can change type without losing anything.
 *
 * createdAt/updatedAt/completedAt/startAt/endAt: UTC epoch millis.
 * dueDate: epoch day (wall-clock date, timezone-independent).
 * dueTime: minute of day 0..1439.
 */
@Entity(tableName = "items", indices = [androidx.room.Index(value = ["uuid"], unique = true)])
@kotlinx.serialization.Serializable
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stable identity that survives export, import and (one day) sync; ids are device-local. */
    @androidx.room.ColumnInfo(defaultValue = "") val uuid: String = "",
    val title: String,
    val body: String? = null,
    val type: ItemType = ItemType.INBOX,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<String> = emptyList(),
    val color: Long? = null,
    val pinned: Boolean = false,
    // task
    val dueDate: Int? = null,
    val dueTime: Int? = null,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val priority: Priority = Priority.NONE,
    val reminderOffsetMinutes: Int? = null,
    val sortOrder: Long = 0,
    // event
    val startAt: Long? = null,
    val endAt: Long? = null,
    val allDay: Boolean = false,
    val recurrence: String? = null,
    /** Set when the item is in the Trash; null everywhere else. Purged after the retention window. */
    val deletedAt: Long? = null,
) {
    val dueLocalDate: LocalDate? get() = dueDate?.let { LocalDate.fromEpochDays(it) }
    val dueLocalTime: LocalTime? get() = dueTime?.let { LocalTime(it / 60, it % 60) }
}

class Converters {
    @TypeConverter fun itemTypeToString(value: ItemType): String = value.name
    @TypeConverter fun stringToItemType(value: String): ItemType = ItemType.valueOf(value)

    @TypeConverter fun priorityToString(value: Priority): String = value.name
    @TypeConverter fun stringToPriority(value: String): Priority = Priority.valueOf(value)

    @TypeConverter fun tagsToString(value: List<String>): String = value.joinToString(",")
    @TypeConverter fun stringToTags(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(",")
}
