package com.tina.app.notifications

import com.tina.app.data.Item
import com.tina.app.data.ItemType
import com.tina.app.data.expandOccurrences
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** Platform alarm scheduling; the Android actual uses AlarmManager, desktop is a no-op for now. */
interface ReminderScheduler {
    fun schedule(item: Item)
    fun cancel(itemId: Long)
    fun rescheduleAll(items: List<Item>) = items.forEach(::schedule)
}

object NoopReminderScheduler : ReminderScheduler {
    override fun schedule(item: Item) = Unit
    override fun cancel(itemId: Long) = Unit
}

private const val TWO_YEARS_MILLIS = 2 * 366L * 24 * 60 * 60 * 1000

/** First occurrence strictly after [afterMillis]: the one that just fired must not be picked again. */
private fun nextOccurrence(start: Long, recurrence: String, afterMillis: Long, tz: TimeZone): Long? =
    expandOccurrences(start, recurrence, afterMillis + 1, afterMillis + TWO_YEARS_MILLIS, tz).firstOrNull()

/**
 * When this item should next ring, in epoch millis — or null if it never should.
 * Tasks need a due date and time; events use their next (possibly recurring) occurrence.
 */
fun nextReminderTime(item: Item, nowMillis: Long, tz: TimeZone): Long? {
    val offsetMillis = (item.reminderOffsetMinutes ?: return null) * 60_000L
    if (item.completed) return null
    return when (item.type) {
        ItemType.TASK -> {
            val date = item.dueLocalDate ?: return null
            val time = item.dueLocalTime ?: return null
            val start = LocalDateTime(date, time).toInstant(tz).toEpochMilliseconds()
            // a repeating task rings for every occurrence, exactly like a repeating event
            val next = if (item.recurrence == null) start else nextOccurrence(start, item.recurrence, nowMillis + offsetMillis, tz)
            next?.minus(offsetMillis)?.takeIf { it > nowMillis }
        }
        ItemType.EVENT -> {
            val start = item.startAt ?: return null
            val next = if (item.recurrence == null) start else nextOccurrence(start, item.recurrence, nowMillis + offsetMillis, tz)
            next?.minus(offsetMillis)?.takeIf { it > nowMillis }
        }
        else -> null
    }
}
