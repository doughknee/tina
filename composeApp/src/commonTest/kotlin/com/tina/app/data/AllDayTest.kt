package com.tina.app.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class AllDayTest {
    private fun midnight(date: LocalDate, zone: TimeZone) =
        LocalDateTime(date, LocalTime(0, 0)).toInstant(zone).toEpochMilliseconds()

    @Test fun keepsTheCalendarDateAcrossZones() {
        val date = LocalDate(2026, 9, 3)
        val chicago = TimeZone.of("America/Chicago")
        val berlin = TimeZone.of("Europe/Berlin")
        val stored = midnight(date, chicago)
        assertEquals(midnight(date, berlin), realignAllDay(stored, chicago, berlin))
        // and back again is the identity
        assertEquals(stored, realignAllDay(realignAllDay(stored, chicago, berlin), berlin, chicago))
    }

    @Test fun sameZoneIsANoOp() {
        val tokyo = TimeZone.of("Asia/Tokyo")
        val stored = midnight(LocalDate(2026, 1, 1), tokyo)
        assertEquals(stored, realignAllDay(stored, tokyo, tokyo))
    }
}
