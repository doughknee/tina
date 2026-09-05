package com.tina.app.data

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.tina.app.notifications.ReminderScheduler
import kotlin.time.Clock

data class ImportResult(val items: Int, val occurrences: Int, val chats: Int, val settings: BackupSettings?)

/**
 * One place that knows what a backup contains. Export carries everything the database holds,
 * trash and habit history included; import is additive, atomic, and keyed on item uuids so a
 * file imported twice changes nothing. Settings are handed back, never applied here: that is
 * the caller's (and the user's) decision.
 */
class BackupService(
    private val database: AppDatabase,
    private val items: ItemDao,
    private val occurrences: OccurrenceDao,
    private val chats: ChatDao,
    private val scheduler: ReminderScheduler,
) {
    suspend fun exportJson(settings: BackupSettings?): String {
        val all = items.getEverything()
        val uuidById = all.associate { it.id to it.uuid }
        val occ = occurrences.all().mapNotNull { o ->
            uuidById[o.itemId]?.let { BackupOccurrence(it, o.epochDay, o.skipped, o.completedAt) }
        }
        val chatList = chats.allChats().map { c ->
            BackupChat(
                title = c.title, model = c.model, reasoning = c.reasoning, createdAt = c.createdAt, updatedAt = c.updatedAt,
                messages = chats.messages(c.id).map { BackupMessage(it.role, it.content, it.createdAt) },
            )
        }
        return encodeBackup(all, Clock.System.now().toEpochMilliseconds(), settings, occ, chatList)
    }

    /** The calendar for other apps: events and dated tasks as iCalendar. Not a backup; nothing imports it. */
    suspend fun exportIcs(): String =
        icsOf(items.getEverything(), kotlinx.datetime.TimeZone.currentSystemDefault(), Clock.System.now().toEpochMilliseconds())

    /** Null when the text is not a backup this build understands. */
    suspend fun importJson(text: String): ImportResult? {
        val backup = decodeBackup(text) ?: return null
        return database.useWriterConnection { transactor ->
            transactor.immediateTransaction { import(backup) }
        }
    }

    private suspend fun import(backup: Backup): ImportResult {
        val existing = items.getEverything()
        val idByUuid = existing.associate { it.uuid to it.id }.toMutableMap()
        // v1 files have no uuids: fall back to the old identity
        val legacyKeys = existing.map { it.title to it.createdAt }.toHashSet()
        var added = 0
        val toArm = mutableListOf<Item>()
        backup.items.forEach { item ->
            val known = (item.uuid.isNotBlank() && item.uuid in idByUuid) ||
                (item.uuid.isBlank() && (item.title to item.createdAt) in legacyKeys)
            if (known) return@forEach
            val stamped = item.copy(id = 0, uuid = item.uuid.ifBlank { newUuid() })
            val id = items.insert(stamped)
            idByUuid[stamped.uuid] = id
            if (stamped.deletedAt == null) toArm += stamped.copy(id = id)
            added++
        }
        var occAdded = 0
        backup.occurrences.forEach { o ->
            val id = idByUuid[o.itemUuid] ?: return@forEach
            occurrences.upsert(OccurrenceCompletion(id, o.epochDay, o.skipped, o.completedAt))
            occAdded++
        }
        val chatKeys = chats.allChats().map { it.title to it.createdAt }.toHashSet()
        var chatsAdded = 0
        backup.chats.forEach { c ->
            if ((c.title to c.createdAt) in chatKeys) return@forEach
            val id = chats.insertChat(ChatEntity(title = c.title, model = c.model, reasoning = c.reasoning, createdAt = c.createdAt, updatedAt = c.updatedAt))
            c.messages.forEach { m -> chats.insertMessage(ChatMessageEntity(chatId = id, role = m.role, content = m.content, createdAt = m.createdAt)) }
            chatsAdded++
        }
        // alarms are not part of the transaction; arm them once the rows are certain
        toArm.forEach(scheduler::schedule)
        return ImportResult(added, occAdded, chatsAdded, backup.settings)
    }
}
