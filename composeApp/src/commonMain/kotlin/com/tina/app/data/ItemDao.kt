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

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: Long): Item?

    @Query("SELECT * FROM items WHERE id = :id")
    fun observe(id: Long): Flow<Item?>

    @Query("SELECT * FROM items ORDER BY createdAt")
    suspend fun getAll(): List<Item>

    @Query("SELECT * FROM items WHERE type = 'INBOX' ORDER BY createdAt DESC")
    fun observeInbox(): Flow<List<Item>>

    @Query("SELECT COUNT(*) FROM items WHERE type = 'INBOX'")
    fun observeInboxCount(): Flow<Int>

    /** Open tasks due on or before the given day (overdue rolls forward), plus tasks completed today. */
    @Query(
        """SELECT * FROM items WHERE type = 'TASK' AND (
             (completed = 0 AND dueDate IS NOT NULL AND dueDate <= :epochDay)
             OR (completed = 1 AND completedAt BETWEEN :dayStartMillis AND :dayEndMillis)
           ) ORDER BY dueDate, dueTime IS NULL, dueTime, sortOrder"""
    )
    fun observeTasksForDay(epochDay: Int, dayStartMillis: Long, dayEndMillis: Long): Flow<List<Item>>

    /** Events possibly intersecting [rangeStart, rangeEnd); recurring ones need expansion by the caller. */
    @Query(
        """SELECT * FROM items WHERE type = 'EVENT' AND startAt IS NOT NULL
           AND startAt < :rangeEndMillis
           AND (recurrence IS NOT NULL OR COALESCE(endAt, startAt) > :rangeStartMillis)
           ORDER BY startAt"""
    )
    fun observeEventsIntersecting(rangeStartMillis: Long, rangeEndMillis: Long): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE type = 'TASK' AND completed = 0 AND dueDate BETWEEN :fromEpochDay AND :toEpochDay")
    fun observeTasksDueBetween(fromEpochDay: Int, toEpochDay: Int): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE type = 'NOTE' ORDER BY pinned DESC, updatedAt DESC")
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

    @Query("SELECT * FROM items WHERE reminderOffsetMinutes IS NOT NULL AND completed = 0")
    suspend fun getRemindable(): List<Item>

    @Query(
        """SELECT * FROM items WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%'
           ORDER BY updatedAt DESC LIMIT 100"""
    )
    fun search(query: String): Flow<List<Item>>
}
