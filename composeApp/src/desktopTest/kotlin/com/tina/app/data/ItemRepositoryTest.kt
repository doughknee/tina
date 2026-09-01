package com.tina.app.data

import androidx.room.Room
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class ItemRepositoryTest {
    private lateinit var file: File
    private lateinit var db: AppDatabase
    private lateinit var repo: ItemRepository

    @BeforeTest fun setUp() {
        file = File.createTempFile("tina-test", ".db")
        db = buildDatabase(Room.databaseBuilder<AppDatabase>(file.absolutePath))
        repo = ItemRepository(db.itemDao())
    }

    @AfterTest fun tearDown() {
        db.close()
        file.delete()
    }

    private var clock = 0L

    private fun item(
        title: String = "x",
        type: ItemType = ItemType.INBOX,
        dueDate: Int? = null,
    ) = Item(title = title, type = type, createdAt = ++clock, updatedAt = clock, dueDate = dueDate)

    @Test fun insertAndObserveInbox() = runBlocking {
        repo.insert(item("first"))
        repo.insert(item("second"))
        repo.insert(item("note", type = ItemType.NOTE))
        val inbox = repo.observeInbox().first()
        assertEquals(listOf("second", "first"), inbox.map { it.title })
        assertEquals(2, repo.observeInboxCount().first())
    }

    @Test fun completeAndUncomplete() = runBlocking {
        val id = repo.insert(item(type = ItemType.TASK))
        repo.complete(id)
        var loaded = repo.get(id)!!
        assertTrue(loaded.completed)
        assertNotNull(loaded.completedAt)
        repo.uncomplete(id)
        loaded = repo.get(id)!!
        assertEquals(false, loaded.completed)
        assertNull(loaded.completedAt)
    }

    @Test fun changeTypeKeepsAllFields() = runBlocking {
        val day = LocalDate(2026, 9, 4).toEpochDays().toInt()
        val id = repo.insert(item(type = ItemType.TASK, dueDate = day).copy(tags = listOf("work"), priority = Priority.HIGH))
        repo.changeType(id, ItemType.EVENT)
        val loaded = repo.get(id)!!
        assertEquals(ItemType.EVENT, loaded.type)
        assertEquals(day, loaded.dueDate)
        assertEquals(listOf("work"), loaded.tags)
        assertEquals(Priority.HIGH, loaded.priority)
    }

    @Test fun rescheduleMovesDueDate() = runBlocking {
        val id = repo.insert(item(type = ItemType.TASK, dueDate = 100))
        val newDay = LocalDate(2026, 9, 10)
        repo.reschedule(id, newDay)
        assertEquals(newDay.toEpochDays().toInt(), repo.get(id)!!.dueDate)
    }

    @Test fun overdueTasksAppearInTodayQuery() = runBlocking {
        val today = LocalDate(2026, 9, 2)
        repo.insert(item("overdue", type = ItemType.TASK, dueDate = today.toEpochDays().toInt() - 3))
        repo.insert(item("due today", type = ItemType.TASK, dueDate = today.toEpochDays().toInt()))
        repo.insert(item("future", type = ItemType.TASK, dueDate = today.toEpochDays().toInt() + 1))
        val tasks = repo.observeTasksForDay(today, TimeZone.UTC).first()
        assertEquals(listOf("overdue", "due today"), tasks.map { it.title })
    }

    @Test fun deleteAndRestore() = runBlocking {
        val id = repo.insert(item("keep me"))
        val loaded = repo.get(id)!!
        repo.delete(id)
        assertNull(repo.get(id))
        repo.restore(loaded)
        assertEquals("keep me", repo.get(id)!!.title)
    }
}
