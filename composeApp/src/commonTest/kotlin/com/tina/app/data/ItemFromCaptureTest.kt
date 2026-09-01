package com.tina.app.data

import com.tina.app.capture.ParsedCapture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class ItemFromCaptureTest {
    private val tz = TimeZone.UTC
    private val now = LocalDateTime(2026, 9, 2, 10, 23)

    private fun ms(dt: LocalDateTime): Long = dt.toInstant(tz).toEpochMilliseconds()

    @Test fun eventWithNoTimeDefaultsToNextRoundHour() {
        val item = itemFromCapture(ParsedCapture("x", ItemType.EVENT), now, tz)
        assertEquals(ms(LocalDateTime(2026, 9, 2, 11, 0)), item.startAt)
        assertEquals(ms(LocalDateTime(2026, 9, 2, 12, 0)), item.endAt)
    }

    @Test fun eventOnFutureDateWithNoTimeStartsAtNine() {
        val item = itemFromCapture(
            ParsedCapture("x", ItemType.EVENT, date = LocalDate(2026, 9, 4)), now, tz,
        )
        assertEquals(ms(LocalDateTime(2026, 9, 4, 9, 0)), item.startAt)
    }

    @Test fun eventUsesParsedTimeAndDuration() {
        val item = itemFromCapture(
            ParsedCapture("x", ItemType.EVENT, date = LocalDate(2026, 9, 3), time = LocalTime(15, 0), durationMinutes = 90),
            now, tz,
        )
        assertEquals(ms(LocalDateTime(2026, 9, 3, 15, 0)), item.startAt)
        assertEquals(ms(LocalDateTime(2026, 9, 3, 16, 30)), item.endAt)
    }

    @Test fun timedEventGetsDefaultReminder() {
        val item = itemFromCapture(ParsedCapture("x", ItemType.EVENT, time = LocalTime(15, 0)), now, tz)
        assertEquals(DEFAULT_REMINDER_MINUTES, item.reminderOffsetMinutes)
    }

    @Test fun recurringEventWithoutTimeIsAllDay() {
        val item = itemFromCapture(
            ParsedCapture("x", ItemType.EVENT, date = LocalDate(2026, 9, 2), rrule = "FREQ=DAILY"), now, tz,
        )
        assertTrue(item.allDay)
        assertEquals(ms(LocalDateTime(2026, 9, 2, 0, 0)), item.startAt)
        assertEquals("FREQ=DAILY", item.recurrence)
        assertNull(item.reminderOffsetMinutes)
    }

    @Test fun taskKeepsNoDateByDefault() {
        val item = itemFromCapture(ParsedCapture("x", ItemType.TASK), now, tz)
        assertNull(item.dueDate)
        assertNull(item.reminderOffsetMinutes)
    }

    @Test fun taskWithDateStoresEpochDay() {
        val date = LocalDate(2026, 9, 4)
        val item = itemFromCapture(ParsedCapture("x", ItemType.TASK, date = date), now, tz)
        assertEquals(date.toEpochDays().toInt(), item.dueDate)
        assertEquals(date, item.dueLocalDate)
    }

    @Test fun taskWithTimeGetsReminder() {
        val item = itemFromCapture(
            ParsedCapture("x", ItemType.TASK, date = LocalDate(2026, 9, 4), time = LocalTime(8, 30)), now, tz,
        )
        assertEquals(8 * 60 + 30, item.dueTime)
        assertEquals(LocalTime(8, 30), item.dueLocalTime)
        assertEquals(DEFAULT_REMINDER_MINUTES, item.reminderOffsetMinutes)
    }

    @Test fun lateNightEventRollsToMidnight() {
        val lateNow = LocalDateTime(2026, 9, 2, 23, 30)
        val item = itemFromCapture(ParsedCapture("x", ItemType.EVENT), lateNow, tz)
        assertEquals(ms(LocalDateTime(2026, 9, 3, 0, 0)), item.startAt)
    }
}
