package com.tina.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    /** Soft delete: the row stays until purged, so Trash can restore it. */
    @Query("UPDATE items SET deletedAt = :at WHERE id = :id")
    suspend fun softDelete(id: Long, at: Long)

    @Query("UPDATE items SET deletedAt = NULL, updatedAt = :at WHERE id = :id")
    suspend fun undelete(id: Long, at: Long)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun purge(id: Long)

    @Query("SELECT * FROM items WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<Item>>

    @Query("SELECT COUNT(*) FROM items WHERE deletedAt IS NOT NULL")
    fun observeTrashCount(): Flow<Int>

    @Query("DELETE FROM items WHERE deletedAt IS NOT NULL AND deletedAt < :cutoffMillis")
    suspend fun purgeOlderThan(cutoffMillis: Long)

    @Query("DELETE FROM items WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash()

    /** Trashed items are invisible to app logic — reminders, AI actions and detail all use this. */
    @Query("SELECT * FROM items WHERE id = :id AND deletedAt IS NULL")
    suspend fun get(id: Long): Item?

    /** Includes trashed rows; only restore/purge paths need this. */
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getAnyById(id: Long): Item?

    @Query("SELECT * FROM items WHERE id = :id AND deletedAt IS NULL")
    fun observe(id: Long): Flow<Item?>

    @Query("SELECT * FROM items WHERE deletedAt IS NULL ORDER BY createdAt")
    suspend fun getAll(): List<Item>

    @Query("SELECT * FROM items WHERE tags != '' AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeTagged(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE deletedAt IS NULL ORDER BY createdAt")
    fun observeAll(): Flow<List<Item>>

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Query("SELECT * FROM items WHERE type = 'INBOX' AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeInbox(): Flow<List<Item>>

    @Query("SELECT COUNT(*) FROM items WHERE type = 'INBOX' AND deletedAt IS NULL")
    fun observeInboxCount(): Flow<Int>

    @Query("SELECT * FROM items WHERE deletedAt IS NULL ORDER BY createdAt DESC LIMIT 3")
    fun observeRecent(): Flow<List<Item>>

    /** Open tasks due on or before the given day (overdue rolls forward), plus tasks completed today. */
    @Query(
        """SELECT * FROM items WHERE type = 'TASK' AND deletedAt IS NULL AND (
             (completed = 0 AND dueDate IS NOT NULL AND dueDate <= :epochDay)
             OR (completed = 1 AND completedAt BETWEEN :dayStartMillis AND :dayEndMillis)
           ) ORDER BY dueDate, dueTime IS NULL, dueTime, sortOrder"""
    )
    fun observeTasksForDay(epochDay: Int, dayStartMillis: Long, dayEndMillis: Long): Flow<List<Item>>

    /** Events possibly intersecting [rangeStart, rangeEnd); recurring ones need expansion by the caller. */
    @Query(
        """SELECT * FROM items WHERE type = 'EVENT' AND deletedAt IS NULL AND startAt IS NOT NULL
           AND startAt < :rangeEndMillis
           AND (recurrence IS NOT NULL OR COALESCE(endAt, startAt) > :rangeStartMillis)
           ORDER BY startAt"""
    )
    fun observeEventsIntersecting(rangeStartMillis: Long, rangeEndMillis: Long): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE type = 'TASK' AND deletedAt IS NULL AND completed = 0 AND dueDate BETWEEN :fromEpochDay AND :toEpochDay")
    fun observeTasksDueBetween(fromEpochDay: Int, toEpochDay: Int): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE type = 'NOTE' AND deletedAt IS NULL ORDER BY pinned DESC, updatedAt DESC")
    fun observeNotes(): Flow<List<Item>>

    @Query("UPDATE items SET completed = 1, completedAt = :at, updatedAt = :at WHERE id = :id")
    suspend fun complete(id: Long, at: Long)

    @Query("UPDATE items SET completed = 0, completedAt = NULL, updatedAt = :at WHERE id = :id")
    suspend fun uncomplete(id: Long, at: Long)

    @Query("UPDATE items SET type = :type, updatedAt = :at WHERE id = :id")
    suspend fun changeType(id: Long, type: ItemType, at: Long)

    @Query("UPDATE items SET dueDate = :epochDay, updatedAt = :at WHERE id = :id")
    suspend fun reschedule(id: Long, epochDay: Int?, at: Long)

    @Query("UPDATE items SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Long)

    @Query("UPDATE items SET title = :title, updatedAt = :at WHERE id = :id")
    suspend fun rename(id: Long, title: String, at: Long)

    @Query("SELECT * FROM items WHERE reminderOffsetMinutes IS NOT NULL AND completed = 0 AND deletedAt IS NULL")
    suspend fun getRemindable(): List<Item>

    @Query(
        """SELECT * FROM items
           WHERE (title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%')
             AND (:includeTrashed OR deletedAt IS NULL)
           ORDER BY updatedAt DESC LIMIT 100"""
    )
    fun search(query: String, includeTrashed: Boolean): Flow<List<Item>>
}
