package com.tina.app.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.tina.app.notifications.NoopReminderScheduler
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Builds a database exactly as an old release created it (from the exported schema JSON),
 * puts a row in it, then opens it with today's Room configuration. Room validates every
 * table against the entities on open, so a missing or wrong migration fails here instead
 * of on a user's phone.
 */
class MigrationTest {
    private fun schemaStatements(version: Int): List<String> {
        val file = File("schemas/com.tina.app.data.AppDatabase/$version.json")
        val database = Json.parseToJsonElement(file.readText()).jsonObject["database"]!!.jsonObject
        return database["entities"]!!.jsonArray.flatMap { entity ->
            val obj = entity.jsonObject
            val table = obj["tableName"]!!.jsonPrimitive.content
            val create = obj["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table)
            val indices = obj["indices"]?.jsonArray.orEmpty().map {
                it.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table)
            }
            listOf(create) + indices
        }
    }

    private fun createOldDatabase(version: Int): String {
        val file = File.createTempFile("tina-v$version-", ".db").apply { delete() }
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        schemaStatements(version).forEach(connection::execSQL)
        connection.execSQL(
            "INSERT INTO items (title, type, createdAt, updatedAt, tags, pinned, completed, priority, sortOrder, allDay) " +
                "VALUES ('from the past', 'TASK', 1, 1, '', 0, 0, 'NONE', 1, 0)",
        )
        connection.execSQL("PRAGMA user_version = $version")
        connection.close()
        return file.absolutePath
    }

    private fun migrateAndRead(version: Int): List<Item> = runBlocking {
        val path = createOldDatabase(version)
        val db = buildDatabase(Room.databaseBuilder<AppDatabase>(name = path))
        try {
            db.itemDao().getAll()
        } finally {
            db.close()
        }
    }

    @Test fun aVersion5DatabaseMigratesAndKeepsItsRowsAndGainsUuids() {
        val items = migrateAndRead(5)
        assertEquals(1, items.size)
        assertEquals("from the past", items.single().title)
        assertEquals(32, items.single().uuid.length, "backfilled uuid")
    }

    @Test fun aVersion2DatabaseWalksTheWholeChain() {
        val items = migrateAndRead(2)
        assertEquals(1, items.size)
        assertTrue(items.single().uuid.isNotBlank())
    }

    @Test fun aFreshDatabaseAssignsUuidsOnInsert() = runBlocking {
        val file = File.createTempFile("tina-fresh-", ".db").apply { delete() }
        val db = buildDatabase(Room.databaseBuilder<AppDatabase>(name = file.absolutePath))
        try {
            val repository = ItemRepository(db.itemDao(), NoopReminderScheduler)
            val id = repository.insert(Item(title = "new", type = ItemType.INBOX, createdAt = 1, updatedAt = 1))
            assertEquals(32, db.itemDao().get(id)!!.uuid.length)
        } finally {
            db.close()
        }
    }
}
