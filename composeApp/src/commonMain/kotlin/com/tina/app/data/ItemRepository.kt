package com.tina.app.data

import com.tina.app.capture.ParsedCapture
import com.tina.app.notifications.NoopReminderScheduler
import com.tina.app.notifications.ReminderScheduler
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
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
    fun observeInboxCount(): Flow<Int> = dao.observeInboxCount()
    fun observeRecent(): Flow<List<Item>> = dao.observeRecent()

    /** All items carrying any tag; exact-tag filtering happens in memory (tags are a joined column). */
    fun observeTagged(): Flow<List<Item>> = dao.observeTagged()
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
        val id = dao.insert(item)
        scheduler.schedule(item.copy(id = id))
        return id
    }

    suspend fun update(item: Item) {
        val updated = item.copy(updatedAt = nowMillis())
        dao.update(updated)
        scheduler.schedule(updated)
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
        scheduler.cancel(id)
    }

    /** Restore a previously deleted item (undo); keeps its old id. */
    suspend fun restore(item: Item): Long = insert(item)

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

    suspend fun exportJson(settings: BackupSettings? = null): String =
        encodeBackup(dao.getAll(), nowMillis(), settings)

    /**
     * Additive import: items get fresh ids; exact (title, createdAt) duplicates
     * of existing rows are skipped. Returns how many were imported.
     */
    suspend fun importJson(text: String): Int {
        val backup = decodeBackup(text) ?: return 0
        val existing = dao.getAll().map { it.title to it.createdAt }.toHashSet()
        var imported = 0
        backup.items.forEach { item ->
            if ((item.title to item.createdAt) !in existing) {
                insert(item.copy(id = 0))
                imported++
            }
        }
        return imported
    }

    fun search(query: String): Flow<List<Item>> = dao.search(query)

    suspend fun setSortOrder(id: Long, sortOrder: Long) = dao.setSortOrder(id, sortOrder)

    suspend fun rename(id: Long, title: String) = dao.rename(id, title, nowMillis())

    /** Parse-result in, saved item out. The 2-second path. */
    suspend fun capture(
        parsed: ParsedCapture,
        tz: TimeZone,
        defaultReminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
    ): Long {
        val now = clock.now().toLocalDateTime(tz)
        return insert(itemFromCapture(parsed, now, tz, defaultReminderMinutes))
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

        ItemType.TASK -> base.copy(
            dueDate = parsed.date?.toEpochDays()?.toInt(),
            dueTime = parsed.time?.let { it.hour * 60 + it.minute },
            reminderOffsetMinutes = if (parsed.time != null) defaultReminderMinutes else null,
        )

        else -> base
    }
}
