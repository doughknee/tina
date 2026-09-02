package com.tina.app.data

import com.tina.app.capture.ParsedCapture
import com.tina.app.notifications.NoopReminderScheduler
import com.tina.app.notifications.ReminderScheduler
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

const val DEFAULT_REMINDER_MINUTES = 10

class ItemRepository(
    private val dao: ItemDao,
    private val scheduler: ReminderScheduler = NoopReminderScheduler,
    private val clock: Clock = Clock.System,
) {
    private fun nowMillis(): Long = clock.now().toEpochMilliseconds()

    fun observe(id: Long): Flow<Item?> = dao.observe(id)
    suspend fun get(id: Long): Item? = dao.get(id)
    suspend fun getAll(): List<Item> = dao.getAll()

    fun observeInbox(): Flow<List<Item>> = dao.observeInbox()

    /** Everything Sort lists, grouped. Today and the stale cutoff are fixed when the flow starts. */
    fun observeDecisions(): Flow<Decisions> {
        val now = clock.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays().toInt()
        val cutoff = now.toEpochMilliseconds() - STALE_AFTER_DAYS * 24L * 60 * 60 * 1000
        return kotlinx.coroutines.flow.combine(
            dao.observeInbox(), dao.observeOverdue(today), dao.observeSnoozed(), dao.observeStale(cutoff),
        ) { new, overdue, snoozed, stale -> Decisions(new, overdue, snoozed, stale) }
    }

    fun observeDecisionCount(): Flow<Int> = observeDecisions().map { it.total }

    suspend fun snooze(id: Long, untilMillis: Long) = dao.setSnoozedUntil(id, untilMillis)
    suspend fun clearSnooze(id: Long) = dao.setSnoozedUntil(id, null)
    fun observeInboxCount(): Flow<Int> = dao.observeInboxCount()
    fun observeRecent(): Flow<List<Item>> = dao.observeRecent()

    /** All items carrying any tag; exact-tag filtering happens in memory (tags are a joined column). */
    fun observeTagged(): Flow<List<Item>> = dao.observeTagged()

    suspend fun allItems(): List<Item> = dao.getAll()

    /**
     * Rename a tag everywhere. If [to] already exists on an item this merges them
     * (distinct collapses the duplicate). Returns the pre-change items for undo.
     */
    suspend fun renameTag(from: String, to: String): List<Item> {
        val target = to.trim().lowercase().removePrefix("#")
        if (target.isEmpty() || target == from) return emptyList()
        val affected = dao.getAll().filter { from in it.tags }
        affected.forEach { item ->
            val tags = item.tags.map { if (it == from) target else it }.distinct()
            dao.update(item.copy(tags = tags, updatedAt = nowMillis()))
        }
        return affected
    }

    /** Strip a tag from every item. Returns the pre-change items for undo. */
    suspend fun removeTag(tag: String): List<Item> {
        val affected = dao.getAll().filter { tag in it.tags }
        affected.forEach { item ->
            dao.update(item.copy(tags = item.tags - tag, updatedAt = nowMillis()))
        }
        return affected
    }

    /** Put a batch of items back exactly as they were (tag edit undo). */
    suspend fun restoreAll(items: List<Item>) = items.forEach { dao.update(it) }

    fun observeAll(): Flow<List<Item>> = dao.observeAll()

    /** Irreversible; only the hold-to-confirm control in Settings calls this. */
    suspend fun deleteEverything() {
        dao.getAll().forEach { scheduler.cancel(it.id) }
        dao.deleteAllOccurrences()
        dao.deleteAll()
    }
    fun observeNotes(): Flow<List<Item>> = dao.observeNotes()

    fun observeTasksForDay(day: LocalDate, tz: TimeZone): Flow<List<Item>> {
        val dayStart = day.atStartOfDayIn(tz).toEpochMilliseconds()
        val dayEnd = day.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()
        return dao.observeTasksForDay(day.toEpochDays().toInt(), dayStart, dayEnd)
    }

    fun observeEventsIntersecting(rangeStartMillis: Long, rangeEndMillis: Long): Flow<List<Item>> =
        dao.observeEventsIntersecting(rangeStartMillis, rangeEndMillis)

    fun observeTasksDueBetween(from: LocalDate, to: LocalDate): Flow<List<Item>> =
        dao.observeTasksDueBetween(from.toEpochDays().toInt(), to.toEpochDays().toInt())

    suspend fun insert(item: Item): Long {
        val stamped = if (item.uuid.isBlank()) item.copy(uuid = newUuid()) else item
        val id = dao.insert(stamped)
        scheduler.schedule(stamped.copy(id = id))
        return id
    }

    suspend fun update(item: Item) {
        val updated = item.copy(updatedAt = nowMillis())
        dao.update(updated)
        scheduler.schedule(updated)
    }

    /** Soft delete — the row moves to Trash rather than disappearing. */
    suspend fun delete(id: Long) {
        dao.softDelete(id, nowMillis())
        scheduler.cancel(id)
    }

    /**
     * Undo path. The row usually still exists (soft-deleted), so clear the flag;
     * only re-insert if it was already purged.
     */
    suspend fun restore(item: Item): Long {
        val existing = dao.getAnyById(item.id)
        return if (existing != null) {
            dao.undelete(item.id, nowMillis())
            scheduler.schedule(existing.copy(deletedAt = null))
            item.id
        } else {
            insert(item.copy(deletedAt = null))
        }
    }

    fun observeTrash(): Flow<List<Item>> = dao.observeTrash()
    fun observeTrashCount(): Flow<Int> = dao.observeTrashCount()

    /** Permanent, single item. */
    suspend fun purge(id: Long) {
        scheduler.cancel(id)
        dao.purgeOccurrences(id)
        dao.purge(id)
    }

    suspend fun emptyTrash() {
        dao.observeTrash().first().forEach { scheduler.cancel(it.id) }
        dao.purgeTrashOccurrences()
        dao.emptyTrash()
    }

    /** Runs at launch; keeps the Trash inside its retention window. */
    suspend fun purgeExpiredTrash(retentionDays: Int?) {
        if (retentionDays == null) return
        val cutoff = nowMillis() - retentionDays * 24L * 60 * 60 * 1000
        dao.purgeOccurrencesOlderThan(cutoff)
        dao.purgeOlderThan(cutoff)
    }

    suspend fun complete(id: Long) {
        dao.complete(id, nowMillis())
        scheduler.cancel(id)
    }

    suspend fun uncomplete(id: Long) {
        dao.uncomplete(id, nowMillis())
        dao.get(id)?.let(scheduler::schedule)
    }

    suspend fun changeType(id: Long, type: ItemType) {
        dao.changeType(id, type, nowMillis())
        dao.get(id)?.let(scheduler::schedule)
    }

    suspend fun reschedule(id: Long, day: LocalDate?) {
        dao.reschedule(id, day?.toEpochDays()?.toInt(), nowMillis())
        dao.get(id)?.let(scheduler::schedule)
    }

    /** Re-arm every pending reminder (boot, app start). */
    suspend fun rescheduleAllReminders() = scheduler.rescheduleAll(dao.getRemindable())

    /** Moves every all-day event's midnight from [from] to [to]; see [realignAllDay]. */
    suspend fun realignAllDayEvents(from: kotlinx.datetime.TimeZone, to: kotlinx.datetime.TimeZone): Int {
        val events = dao.allDayEvents()
        for (item in events) {
            dao.update(
                item.copy(
                    startAt = item.startAt?.let { realignAllDay(it, from, to) },
                    endAt = item.endAt?.let { realignAllDay(it, from, to) },
                ),
            )
        }
        if (events.isNotEmpty()) rescheduleAllReminders()
        return events.size
    }


    fun search(query: String, includeTrashed: Boolean = false): Flow<List<Item>> =
        dao.search(query, includeTrashed)

    suspend fun setSortOrder(id: Long, sortOrder: Long) = dao.setSortOrder(id, sortOrder, nowMillis())

    suspend fun rename(id: Long, title: String) = dao.rename(id, title, nowMillis())

    /** Parse-result in, saved item out. The 2-second path. */
    suspend fun capture(
        parsed: ParsedCapture,
        tz: TimeZone,
        defaultReminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
        undatedToSort: Boolean = true,
    ): Long {
        val now = clock.now().toLocalDateTime(tz)
        return insert(itemFromCapture(parsed, now, tz, defaultReminderMinutes, undatedToSort))
    }

    /** Expand an event's occurrences (recurring or not) within a range. */
    fun occurrencesOf(event: Item, rangeStartMillis: Long, rangeEndMillis: Long, tz: TimeZone): List<Long> {
        val start = event.startAt ?: return emptyList()
        return expandOccurrences(start, event.recurrence, rangeStartMillis, rangeEndMillis, tz)
    }
}

/**
 * Applies capture defaults: events get the next round hour and one hour of length,
 * tasks get no date unless parsed, reminders default on for anything with a time.
 */
fun itemFromCapture(
    parsed: ParsedCapture,
    now: LocalDateTime,
    tz: TimeZone,
    defaultReminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
    /** A task with no date is a decision, not a plan: it lands on Sort rather than today's list. */
    undatedToSort: Boolean = true,
): Item {
    val nowMillis = now.toInstant(tz).toEpochMilliseconds()
    val base = Item(
        title = parsed.title,
        body = parsed.body,
        type = parsed.type,
        createdAt = nowMillis,
        updatedAt = nowMillis,
        tags = parsed.tags,
        priority = parsed.priority,
        sortOrder = nowMillis,
    )
    return when (parsed.type) {
        ItemType.EVENT -> {
            val date = parsed.date ?: now.date
            val allDay = parsed.time == null && parsed.rrule != null
            if (allDay) {
                val start = LocalDateTime(date, LocalTime(0, 0)).toInstant(tz).toEpochMilliseconds()
                base.copy(
                    startAt = start,
                    endAt = LocalDateTime(date.plus(1, DateTimeUnit.DAY), LocalTime(0, 0))
                        .toInstant(tz).toEpochMilliseconds(),
                    allDay = true,
                    recurrence = parsed.rrule,
                )
            } else {
                val startDateTime = when {
                    parsed.time != null -> LocalDateTime(date, parsed.time)
                    date != now.date -> LocalDateTime(date, LocalTime(9, 0))
                    now.hour >= 23 -> LocalDateTime(date.plus(1, DateTimeUnit.DAY), LocalTime(0, 0))
                    else -> LocalDateTime(date, LocalTime(now.hour + 1, 0))
                }
                val start = startDateTime.toInstant(tz).toEpochMilliseconds()
                base.copy(
                    startAt = start,
                    endAt = start + (parsed.durationMinutes ?: 60) * 60_000L,
                    recurrence = parsed.rrule,
                    reminderOffsetMinutes = defaultReminderMinutes,
                )
            }
        }

        ItemType.TASK -> if (undatedToSort && parsed.date == null && parsed.rrule == null) base.copy(type = ItemType.INBOX) else base.copy(
            dueDate = parsed.date?.toEpochDays()?.toInt(),
            dueTime = parsed.time?.let { it.hour * 60 + it.minute },
            recurrence = parsed.rrule,
            reminderOffsetMinutes = if (parsed.time != null) defaultReminderMinutes else null,
        )

        else -> base
    }
}

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
fun newUuid(): String = kotlin.uuid.Uuid.random().toHexString()

/** Days a someday item can sit untouched before Sort asks whether it still matters. */
const val STALE_AFTER_DAYS = 30

data class Decisions(
    val new: List<Item> = emptyList(),
    val overdue: List<Item> = emptyList(),
    val snoozed: List<Item> = emptyList(),
    val stale: List<Item> = emptyList(),
) {
    val total: Int get() = new.size + overdue.size + snoozed.size + stale.size
    val isEmpty: Boolean get() = total == 0
}
