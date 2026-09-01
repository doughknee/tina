package com.tina.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class RecurrenceTest {
    private val tz = TimeZone.UTC

    private fun ms(year: Int, month: Int, day: Int, hour: Int = 9, minute: Int = 0): Long =
        LocalDateTime(year, month, day, hour, minute).toInstant(tz).toEpochMilliseconds()

    private fun expand(start: Long, rrule: String?, from: Long, to: Long): List<Long> =
        expandOccurrences(start, rrule, from, to, tz)

    @Test fun noRuleSingleOccurrenceInRange() {
        val start = ms(2026, 1, 3)
        assertEquals(listOf(start), expand(start, null, ms(2026, 1, 1), ms(2026, 1, 8)))
    }

    @Test fun noRuleOutOfRange() {
        assertEquals(emptyList(), expand(ms(2026, 2, 1), null, ms(2026, 1, 1), ms(2026, 1, 8)))
    }

    @Test fun dailyBasic() {
        val out = expand(ms(2026, 1, 1), "FREQ=DAILY", ms(2026, 1, 1, 0), ms(2026, 1, 8, 0))
        assertEquals((1..7).map { ms(2026, 1, it) }, out)
    }

    @Test fun dailyInterval2() {
        val out = expand(ms(2026, 1, 1), "FREQ=DAILY;INTERVAL=2", ms(2026, 1, 1, 0), ms(2026, 1, 8, 0))
        assertEquals(listOf(ms(2026, 1, 1), ms(2026, 1, 3), ms(2026, 1, 5), ms(2026, 1, 7)), out)
    }

    @Test fun dailyWindowExcludesEarlierOccurrences() {
        val out = expand(ms(2026, 1, 1), "FREQ=DAILY", ms(2026, 1, 5, 0), ms(2026, 1, 8, 0))
        assertEquals(listOf(ms(2026, 1, 5), ms(2026, 1, 6), ms(2026, 1, 7)), out)
    }

    @Test fun weeklySimple() {
        val out = expand(ms(2026, 1, 1), "FREQ=WEEKLY", ms(2026, 1, 1, 0), ms(2026, 1, 29, 0))
        assertEquals(listOf(ms(2026, 1, 1), ms(2026, 1, 8), ms(2026, 1, 15), ms(2026, 1, 22)), out)
    }

    @Test fun weeklyByDay() {
        // 2026-01-05 is a Monday
        val out = expand(
            ms(2026, 1, 5), "FREQ=WEEKLY;BYDAY=MO,WE",
            ms(2026, 1, 5, 0), ms(2026, 1, 19, 0),
        )
        assertEquals(listOf(ms(2026, 1, 5), ms(2026, 1, 7), ms(2026, 1, 12), ms(2026, 1, 14)), out)
    }

    @Test fun weeklyByDayInterval2() {
        val out = expand(
            ms(2026, 1, 5), "FREQ=WEEKLY;BYDAY=MO,WE;INTERVAL=2",
            ms(2026, 1, 5, 0), ms(2026, 1, 26, 0),
        )
        assertEquals(listOf(ms(2026, 1, 5), ms(2026, 1, 7), ms(2026, 1, 19), ms(2026, 1, 21)), out)
    }

    @Test fun monthlyOn31stSkipsShortMonths() {
        val out = expand(ms(2026, 1, 31), "FREQ=MONTHLY", ms(2026, 1, 1, 0), ms(2027, 1, 1, 0))
        assertEquals(
            listOf(ms(2026, 1, 31), ms(2026, 3, 31), ms(2026, 5, 31), ms(2026, 7, 31), ms(2026, 8, 31), ms(2026, 10, 31), ms(2026, 12, 31)),
            out,
        )
    }

    @Test fun monthlyInterval3() {
        val out = expand(ms(2026, 1, 15), "FREQ=MONTHLY;INTERVAL=3", ms(2026, 1, 1, 0), ms(2027, 1, 1, 0))
        assertEquals(listOf(ms(2026, 1, 15), ms(2026, 4, 15), ms(2026, 7, 15), ms(2026, 10, 15)), out)
    }

    @Test fun yearly() {
        val out = expand(ms(2026, 3, 10), "FREQ=YEARLY", ms(2026, 1, 1, 0), ms(2029, 1, 1, 0))
        assertEquals(listOf(ms(2026, 3, 10), ms(2027, 3, 10), ms(2028, 3, 10)), out)
    }

    @Test fun yearlyFeb29OnlyOnLeapYears() {
        val out = expand(ms(2024, 2, 29), "FREQ=YEARLY", ms(2024, 1, 1, 0), ms(2029, 1, 1, 0))
        assertEquals(listOf(ms(2024, 2, 29), ms(2028, 2, 29)), out)
    }

    @Test fun countLimits() {
        val out = expand(ms(2026, 1, 1), "FREQ=DAILY;COUNT=3", ms(2026, 1, 1, 0), ms(2026, 1, 20, 0))
        assertEquals(3, out.size)
    }

    @Test fun countIsAnchoredAtEventStartNotRange() {
        // occurrences 1..3 fall before the window, so nothing lands inside it
        val out = expand(ms(2026, 1, 1), "FREQ=DAILY;COUNT=3", ms(2026, 1, 10, 0), ms(2026, 1, 20, 0))
        assertEquals(emptyList(), out)
    }

    @Test fun untilLimits() {
        val out = expand(ms(2026, 1, 1), "FREQ=DAILY;UNTIL=20260105", ms(2026, 1, 1, 0), ms(2026, 1, 20, 0))
        assertEquals(5, out.size)
    }

    @Test fun untilWithTimeComponent() {
        val out = expand(ms(2026, 1, 1), "FREQ=DAILY;UNTIL=20260105T235959Z", ms(2026, 1, 1, 0), ms(2026, 1, 20, 0))
        assertEquals(5, out.size)
    }

    @Test fun rrulePrefixAccepted() {
        val out = expand(ms(2026, 1, 1), "RRULE:FREQ=DAILY;COUNT=2", ms(2026, 1, 1, 0), ms(2026, 1, 20, 0))
        assertEquals(2, out.size)
    }

    @Test fun bogusRuleFallsBackToSingleOccurrence() {
        val start = ms(2026, 1, 3)
        assertEquals(listOf(start), expand(start, "FREQ=BOGUS", ms(2026, 1, 1), ms(2026, 1, 8)))
    }

    @Test fun parseRruleFields() {
        val rule = parseRrule("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,FR;COUNT=10;UNTIL=20261231T000000Z")!!
        assertEquals(RecurrenceRule.Freq.WEEKLY, rule.freq)
        assertEquals(2, rule.interval)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), rule.byDay)
        assertEquals(10, rule.count)
        assertEquals(LocalDate(2026, 12, 31), rule.until)
    }

    @Test fun parseRruleRejectsGarbage() {
        assertNull(parseRrule("not an rrule"))
    }
}
