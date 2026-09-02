package com.tina.app.notifications

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class QuietHoursTest {
    private val tz = TimeZone.of("America/Chicago")
    private fun at(day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime(2026, 9, day, hour, minute).toInstant(tz).toEpochMilliseconds()

    private val night = QuietHours(startMinutes = 22 * 60, endMinutes = 7 * 60)

    @Test fun insideTheNightWaitsUntilMorning() {
        assertEquals(at(4, 7), deferOutOfQuietHours(at(3, 23, 30), night, tz))
        assertEquals(at(3, 7), deferOutOfQuietHours(at(3, 2), night, tz))
    }

    @Test fun daytimeIsUntouched() {
        assertEquals(at(3, 9), deferOutOfQuietHours(at(3, 9), night, tz))
        assertEquals(at(3, 21, 59), deferOutOfQuietHours(at(3, 21, 59), night, tz))
        assertEquals(at(3, 7), deferOutOfQuietHours(at(3, 7), night, tz))
    }

    @Test fun aDaytimeWindowWorksToo() {
        val lunch = QuietHours(startMinutes = 12 * 60, endMinutes = 13 * 60)
        assertEquals(at(3, 13), deferOutOfQuietHours(at(3, 12, 15), lunch, tz))
        assertEquals(at(3, 14), deferOutOfQuietHours(at(3, 14), lunch, tz))
    }

    @Test fun offMeansNoChange() {
        assertEquals(at(3, 23), deferOutOfQuietHours(at(3, 23), null, tz))
    }
}
