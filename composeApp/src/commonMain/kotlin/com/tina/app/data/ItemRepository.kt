package com.tina.app.data

import com.tina.app.capture.ParsedCapture
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
    private val clock: Clock = Clock.System,
) {
    private fun nowMillis(): Long = clock.now().toEpochMilliseconds()

    fun observe(id: Long): Flow<Item?> = dao.observe(id)
    suspend fun get(id: Long): Item? = dao.get(id)
    suspend fun getAll(): List<Item> = dao.getAll()

    fun observeInbox(): Flow<List<Item>> = dao.observeInbox()
    fun observeInboxCount(): Flow<Int> = dao.observeInboxCount()
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

    suspend fun insert(item: Item): Long = dao.insert(item)

    suspend fun update(item: Item) = dao.update(item.copy(updatedAt = nowMillis()))

    suspend fun delete(id: Long) = dao.delete(id)

    /** Restore a previously deleted item (undo); keeps its old id. */
    suspend fun restore(item: Item): Long = dao.insert(item)

    suspend fun complete(id: Long) = dao.complete(id, nowMillis())
    suspend fun uncomplete(id: Long) = dao.uncomplete(id, nowMillis())

    suspend fun changeType(id: Long, type: ItemType) = dao.changeType(id, type, nowMillis())

    suspend fun reschedule(id: Long, day: LocalDate?) =
        dao.reschedule(id, day?.toEpochDays()?.toInt(), nowMillis())

    /** Parse-result in, saved item out. The 2-second path. */
    suspend fun capture(
        parsed: ParsedCapture,
        tz: TimeZone,
        defaultReminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
    ): Long {
        val now = clock.now().toLocalDateTime(tz)
        return dao.insert(itemFromCapture(parsed, now, tz, defaultReminderMinutes))
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
