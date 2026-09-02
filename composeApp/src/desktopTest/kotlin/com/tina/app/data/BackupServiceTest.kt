package com.tina.app.data

import androidx.room.Room
import com.tina.app.notifications.NoopReminderScheduler
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

/** Real Room on a temp file: export carries everything, import is idempotent and keyed on uuids. */
class BackupServiceTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ItemRepository
    private lateinit var occurrences: OccurrenceRepository
    private lateinit var backups: BackupService

    @BeforeTest fun open() {
        val file = File.createTempFile("tina-backup-", ".db").apply { delete() }
        db = buildDatabase(Room.databaseBuilder<AppDatabase>(name = file.absolutePath))
        repo = ItemRepository(db.itemDao(), NoopReminderScheduler)
        occurrences = OccurrenceRepository(db.occurrenceDao())
        backups = BackupService(db, db.itemDao(), db.occurrenceDao(), db.chatDao(), NoopReminderScheduler)
    }

    @AfterTest fun close() = db.close()

    private suspend fun seed(): Pair<Long, Long> {
        val habit = repo.insert(
            Item(title = "water plants", type = ItemType.TASK, createdAt = 10, updatedAt = 10, recurrence = "FREQ=DAILY", dueDate = 20698),
        )
        occurrences.complete(habit, LocalDate(2026, 9, 1))
        occurrences.skip(habit, LocalDate(2026, 9, 2))
        val trashed = repo.insert(Item(title = "old note", type = ItemType.NOTE, createdAt = 11, updatedAt = 11))
        repo.delete(trashed)
        val chat = db.chatDao().insertChat(ChatEntity(title = "plans", model = null, reasoning = "BALANCED", createdAt = 12, updatedAt = 12))
        db.chatDao().insertMessage(ChatMessageEntity(chatId = chat, role = "user", content = "hi", createdAt = 12))
        return habit to trashed
    }

    @Test fun exportCarriesTrashOccurrencesAndChats() = runBlocking {
        seed()
        val backup = decodeBackup(backups.exportJson(null))
        assertNotNull(backup)
        assertEquals(BACKUP_VERSION, backup.version)
        assertEquals(2, backup.items.size, "trashed items are in the file")
        assertEquals(2, backup.occurrences.size)
        assertEquals(1, backup.chats.size)
        assertEquals("hi", backup.chats.single().messages.single().content)
    }

    @Test fun importIntoAnEmptyDatabaseRestoresEverything() = runBlocking {
        seed()
        val json = backups.exportJson(null)
        repo.deleteEverything()
        db.chatDao().allChats().forEach { db.chatDao().deleteChat(it.id) }
        val result = backups.importJson(json)
        assertNotNull(result)
        assertEquals(2, result.items)
        assertEquals(2, result.occurrences)
        assertEquals(1, result.chats)
        assertEquals(1, db.itemDao().getAll().size, "the trashed note stays in the trash")
        assertEquals(1, db.itemDao().observeTrashCount().first())
        val habit = db.itemDao().getAll().single()
        assertTrue(occurrences.isHandled(habit.id, LocalDate(2026, 9, 1)))
        assertTrue(occurrences.isHandled(habit.id, LocalDate(2026, 9, 2)))
    }

    @Test fun importingTheSameFileTwiceChangesNothing() = runBlocking {
        seed()
        val json = backups.exportJson(null)
        val first = backups.importJson(json)!!
        val second = backups.importJson(json)!!
        assertEquals(0, first.items, "everything already present")
        assertEquals(0, second.items)
        assertEquals(2, db.itemDao().getEverything().size)
    }

    @Test fun aLegacyFileWithoutUuidsStillImportsOnce() = runBlocking {
        val legacy = """{"version":1,"exportedAt":5,"items":[{"title":"from v1","type":"TASK","createdAt":7,"updatedAt":7}]}"""
        assertEquals(1, backups.importJson(legacy)!!.items)
        assertEquals(0, backups.importJson(legacy)!!.items)
        assertTrue(db.itemDao().getAll().single().uuid.isNotBlank())
    }

    @Test fun aFileFromTheFutureIsRefused() = runBlocking {
        assertNull(backups.importJson("""{"version":99,"exportedAt":1,"items":[]}"""))
    }

    @Test fun settingsComeBackAsAnOfferNotASideEffect() = runBlocking {
        val json = backups.exportJson(Settings(themeMode = ThemeMode.DARK).toBackupSettings())
        val result = backups.importJson(json)!!
        assertEquals("DARK", result.settings?.themeMode)
    }
}
