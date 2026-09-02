package com.tina.app.data

import com.tina.app.capture.ParsedCapture
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class UndatedCaptureTest {
    private val now = LocalDateTime(LocalDate(2026, 9, 3), LocalTime(10, 0))
    private val tz = TimeZone.of("America/Chicago")

    @Test fun anUndatedTaskIsADecision() {
        val item = itemFromCapture(ParsedCapture(title = "Renew passport", type = ItemType.TASK), now, tz)
        assertEquals(ItemType.INBOX, item.type)
    }

    @Test fun theSettingKeepsItOnTodaysList() {
        val item = itemFromCapture(ParsedCapture(title = "Renew passport", type = ItemType.TASK), now, tz, undatedToSort = false)
        assertEquals(ItemType.TASK, item.type)
        assertEquals(null, item.dueDate)
    }

    @Test fun datedAndRepeatingTasksAreNotTouched() {
        val dated = itemFromCapture(ParsedCapture(title = "x", type = ItemType.TASK, date = LocalDate(2026, 9, 4)), now, tz)
        assertEquals(ItemType.TASK, dated.type)
        val habit = itemFromCapture(ParsedCapture(title = "x", type = ItemType.TASK, rrule = "FREQ=DAILY"), now, tz)
        assertEquals(ItemType.TASK, habit.type)
    }
}
