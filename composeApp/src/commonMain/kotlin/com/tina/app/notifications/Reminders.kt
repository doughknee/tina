package com.tina.app.notifications

import com.tina.app.data.Item
import com.tina.app.data.ItemType
import com.tina.app.data.expandOccurrences
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.plus

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

/** Minutes after local midnight; a window that ends before it starts wraps past midnight. */
data class QuietHours(val startMinutes: Int, val endMinutes: Int)

/**
 * A reminder due inside quiet hours rings when they end instead. Digests are not touched:
 * their times are chosen by the user directly.
 */
fun deferOutOfQuietHours(atMillis: Long, quiet: QuietHours?, tz: TimeZone): Long {
    if (quiet == null || quiet.startMinutes == quiet.endMinutes) return atMillis
    val local = kotlinx.datetime.Instant.fromEpochMilliseconds(atMillis).toLocalDateTime(tz)
    val minute = local.hour * 60 + local.minute
    val wraps = quiet.endMinutes < quiet.startMinutes
    val inside = if (wraps) minute >= quiet.startMinutes || minute < quiet.endMinutes
    else minute >= quiet.startMinutes && minute < quiet.endMinutes
    if (!inside) return atMillis
    // the end lands tomorrow when the window wraps and we are already past its start
    val endDate = if (wraps && minute >= quiet.startMinutes) local.date.plus(1, kotlinx.datetime.DateTimeUnit.DAY) else local.date
    return LocalDateTime(endDate, kotlinx.datetime.LocalTime(quiet.endMinutes / 60, quiet.endMinutes % 60)).toInstant(tz).toEpochMilliseconds()
}

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
