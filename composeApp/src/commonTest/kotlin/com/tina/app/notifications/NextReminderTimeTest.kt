package com.tina.app.notifications

import com.tina.app.data.Item
import com.tina.app.data.ItemType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class NextReminderTimeTest {
    private val tz = TimeZone.UTC

    private fun ms(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long =
        LocalDateTime(year, month, day, hour, minute).toInstant(tz).toEpochMilliseconds()

    private val now = ms(2026, 9, 2, 10, 0)

    private fun task(dueDate: LocalDate?, dueMinute: Int?, offset: Int? = 10, completed: Boolean = false) = Item(
        title = "t", type = ItemType.TASK, createdAt = 0, updatedAt = 0,
        dueDate = dueDate?.toEpochDays()?.toInt(), dueTime = dueMinute,
        reminderOffsetMinutes = offset, completed = completed,
    )

    @Test fun taskReminderIsDueTimeMinusOffset() {
        val item = task(LocalDate(2026, 9, 2), 15 * 60)
        assertEquals(ms(2026, 9, 2, 14, 50), nextReminderTime(item, now, tz))
    }

    @Test fun pastTaskReminderIsNull() {
        assertNull(nextReminderTime(task(LocalDate(2026, 9, 1), 9 * 60), now, tz))
    }

    @Test fun completedTaskIsNull() {
        assertNull(nextReminderTime(task(LocalDate(2026, 9, 2), 15 * 60, completed = true), now, tz))
    }

    @Test fun taskWithoutTimeIsNull() {
        assertNull(nextReminderTime(task(LocalDate(2026, 9, 2), null), now, tz))
    }

    @Test fun noOffsetIsNull() {
        assertNull(nextReminderTime(task(LocalDate(2026, 9, 2), 15 * 60, offset = null), now, tz))
    }

    @Test fun oneShotEvent() {
        val event = Item(
            title = "e", type = ItemType.EVENT, createdAt = 0, updatedAt = 0,
            startAt = ms(2026, 9, 2, 12, 0), endAt = ms(2026, 9, 2, 13, 0),
            reminderOffsetMinutes = 10,
        )
        assertEquals(ms(2026, 9, 2, 11, 50), nextReminderTime(event, now, tz))
    }

    @Test fun recurringEventUsesNextOccurrence() {
        val event = Item(
            title = "e", type = ItemType.EVENT, createdAt = 0, updatedAt = 0,
            startAt = ms(2026, 9, 1, 9, 0), endAt = ms(2026, 9, 1, 10, 0),
            recurrence = "FREQ=DAILY", reminderOffsetMinutes = 10,
        )
        // now = Sep 2 10:00; today's 9:00 already passed, so tomorrow 8:50
        assertEquals(ms(2026, 9, 3, 8, 50), nextReminderTime(event, now, tz))
    }
}
