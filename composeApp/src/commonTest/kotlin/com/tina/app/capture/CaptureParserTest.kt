package com.tina.app.capture

import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

class CaptureParserTest {
    // Wednesday
    private val now = LocalDateTime(2026, 9, 2, 10, 0)

    private fun parse(input: String) = parseCapture(input, now)

    // --- no signals ---

    @Test fun plainTextIsTaskWithNoDate() {
        val p = parse("buy milk")
        assertEquals(ItemType.TASK, p.type)
        assertEquals("buy milk", p.title)
        assertNull(p.date)
        assertNull(p.time)
    }

    @Test fun emptyInputIsInbox() {
        assertEquals(ItemType.INBOX, parse("   ").type)
    }

    // --- dates ---

    @Test fun today() = assertEquals(LocalDate(2026, 9, 2), parse("pay rent today").date)

    @Test fun tomorrow() = assertEquals(LocalDate(2026, 9, 3), parse("call mom tomorrow").date)

    @Test fun tmrw() = assertEquals(LocalDate(2026, 9, 3), parse("call mom tmrw").date)

    @Test fun weekdayFullName() = assertEquals(LocalDate(2026, 9, 4), parse("gym friday").date)

    @Test fun weekdayShortName() = assertEquals(LocalDate(2026, 9, 4), parse("gym fri").date)

    @Test fun weekdayNeverResolvesToToday() =
        assertEquals(LocalDate(2026, 9, 9), parse("review wednesday").date)

    @Test fun nextWeekday() = assertEquals(LocalDate(2026, 9, 11), parse("gym next friday").date)

    @Test fun nextWeek() = assertEquals(LocalDate(2026, 9, 7), parse("plan trip next week").date)

    @Test fun inThreeDays() = assertEquals(LocalDate(2026, 9, 5), parse("follow up in 3 days").date)

    @Test fun inTwoWeeks() = assertEquals(LocalDate(2026, 9, 16), parse("follow up in 2 weeks").date)

    @Test fun inOneMonth() = assertEquals(LocalDate(2026, 10, 2), parse("renew in 1 month").date)

    @Test fun monthDayRollsToNextYear() = assertEquals(LocalDate(2027, 1, 5), parse("taxes jan 5").date)

    @Test fun monthDayThisYear() = assertEquals(LocalDate(2026, 9, 30), parse("report sep 30").date)

    @Test fun monthDayOrdinal() = assertEquals(LocalDate(2026, 12, 25), parse("wrap gifts dec 25th").date)

    @Test fun numericDateRollsToNextYear() = assertEquals(LocalDate(2027, 1, 5), parse("taxes 1/5").date)

    @Test fun numericDateThisYear() = assertEquals(LocalDate(2026, 9, 15), parse("dentist 9/15").date)

    @Test fun invalidMonthDayIgnored() {
        val p = parse("dentist jan 45")
        assertNull(p.date)
        assertEquals(ItemType.TASK, p.type)
        assertTrue(p.title.contains("jan 45"))
    }

    @Test fun dateWithoutTimeIsTask() {
        val p = parse("dentist tomorrow")
        assertEquals(ItemType.TASK, p.type)
        assertEquals("dentist", p.title)
    }

    // --- times ---

    @Test fun timePmShorthand() {
        val p = parse("meet sam 3pm")
        assertEquals(ItemType.EVENT, p.type)
        assertEquals(LocalTime(15, 0), p.time)
    }

    @Test fun timeWithMinutes() = assertEquals(LocalTime(15, 30), parse("meet sam 3:30pm").time)

    @Test fun time24h() = assertEquals(LocalTime(15, 0), parse("meet sam 15:00").time)

    @Test fun timeWithAtConsumed() {
        val p = parse("meet sam at 3pm")
        assertEquals(LocalTime(15, 0), p.time)
        assertEquals("meet sam", p.title)
    }

    @Test fun noon() = assertEquals(LocalTime(12, 0), parse("lunch noon").time)

    @Test fun midnight() = assertEquals(LocalTime(0, 0), parse("deploy at midnight").time)

    @Test fun morningDefault() = assertEquals(LocalTime(9, 0), parse("run tomorrow morning").time)

    @Test fun afternoonDefault() = assertEquals(LocalTime(14, 0), parse("errands tomorrow afternoon").time)

    @Test fun eveningDefault() = assertEquals(LocalTime(19, 0), parse("dinner friday evening").time)

    @Test fun tonightIsTodayEvening() {
        val p = parse("movie tonight")
        assertEquals(LocalDate(2026, 9, 2), p.date)
        assertEquals(LocalTime(20, 0), p.time)
        assertEquals(ItemType.EVENT, p.type)
    }

    @Test fun twelveAmIsMidnight() = assertEquals(LocalTime(0, 0), parse("flight 12am").time)

    @Test fun twelvePmIsNoon() = assertEquals(LocalTime(12, 0), parse("flight 12pm").time)

    @Test fun invalidTimeIgnored() {
        val p = parse("weird 25:00")
        assertNull(p.time)
        assertEquals(ItemType.TASK, p.type)
    }

    @Test fun timeSignalMeansEvent() = assertEquals(ItemType.EVENT, parse("standup 9am").type)

    @Test fun explicitDateBeatsTonightDate() {
        val p = parse("movie tomorrow tonight")
        assertEquals(LocalDate(2026, 9, 3), p.date)
        assertEquals(LocalTime(20, 0), p.time)
    }

    // --- priority ---

    @Test fun singleBangIsMedium() {
        val p = parse("buy milk !")
        assertEquals(Priority.MEDIUM, p.priority)
        assertEquals("buy milk", p.title)
    }

    @Test fun doubleBangIsHigh() = assertEquals(Priority.HIGH, parse("submit report !!").priority)

    @Test fun attachedBangsConsumed() {
        val p = parse("milk!!")
        assertEquals(Priority.HIGH, p.priority)
        assertEquals("milk", p.title)
    }

    // --- tags ---

    @Test fun tagExtracted() {
        val p = parse("email boss #work")
        assertEquals(listOf("work"), p.tags)
        assertEquals("email boss", p.title)
    }

    @Test fun multipleTagsLowercased() =
        assertEquals(listOf("work", "home"), parse("plan stuff #Work #home").tags)

    // --- duration ---

    @Test fun durationHours() = assertEquals(120, parse("deep work 2pm for 2h").durationMinutes)

    @Test fun durationMinutes() = assertEquals(30, parse("sync 3pm for 30m").durationMinutes)

    @Test fun durationHoursAndMinutes() =
        assertEquals(90, parse("workshop 1pm for 1h 30m").durationMinutes)

    // --- recurrence ---

    @Test fun everyDay() {
        val p = parse("water plants every day")
        assertEquals("FREQ=DAILY", p.rrule)
        assertEquals(ItemType.EVENT, p.type)
        assertEquals(LocalDate(2026, 9, 2), p.date)
    }

    @Test fun everyWeek() = assertEquals("FREQ=WEEKLY", parse("trash every week").rrule)

    @Test fun everyMonth() = assertEquals("FREQ=MONTHLY", parse("rent every month").rrule)

    @Test fun everyWeekday() {
        val p = parse("standup every friday")
        assertEquals("FREQ=WEEKLY;BYDAY=FR", p.rrule)
        assertEquals(LocalDate(2026, 9, 4), p.date)
    }

    @Test fun recurrenceWithTime() {
        val p = parse("standup every day 9am")
        assertEquals("FREQ=DAILY", p.rrule)
        assertEquals(LocalTime(9, 0), p.time)
        assertEquals("standup", p.title)
    }

    // --- notes ---

    @Test fun threeSentencesIsNote() {
        val p = parse("Talked to the landlord. He agreed to fix the sink. Follow up next month.")
        assertEquals(ItemType.NOTE, p.type)
        assertEquals("Talked to the landlord", p.title)
        assertTrue(p.body!!.contains("fix the sink"))
    }

    @Test fun longTextIsNote() {
        val text = "a".repeat(101) + " " + "b".repeat(101)
        assertEquals(ItemType.NOTE, parse(text).type)
    }

    @Test fun multiLineIsNote() {
        assertEquals(ItemType.NOTE, parse("shopping\nmilk\neggs").type)
    }

    // --- ambiguity ---

    @Test fun conflictingDatesStayInbox() {
        val p = parse("call sam today or tomorrow")
        assertEquals(ItemType.INBOX, p.type)
        assertEquals("call sam today or tomorrow", p.title)
        assertNull(p.date)
    }

    @Test fun signalsWithoutContentStayInbox() {
        val p = parse("3pm")
        assertEquals(ItemType.INBOX, p.type)
        assertEquals("3pm", p.title)
    }

    // --- title cleanup ---

    @Test fun trailingConnectorStripped() = assertEquals("dinner", parse("dinner on friday").title)

    @Test fun connectorInsideTitleKept() =
        assertEquals("prep for the demo", parse("prep for the demo tomorrow").title)

    // --- newer parser rules ---

    @Test fun nextMonth() = assertEquals(LocalDate(2026, 10, 2), parse("review lease next month").date)

    @Test fun endOfWeek() = assertEquals(LocalDate(2026, 9, 6), parse("report end of week").date)

    @Test fun endOfMonth() = assertEquals(LocalDate(2026, 9, 30), parse("invoice end of the month").date)

    @Test fun dayBeforeMonth() = assertEquals(LocalDate(2027, 1, 5), parse("taxes 5 jan").date)

    @Test fun inThirtyMinutes() {
        val p = parse("tea in 30 min")
        assertEquals(LocalTime(10, 30), p.time)
        assertEquals(LocalDate(2026, 9, 2), p.date)
        assertEquals(ItemType.EVENT, p.type)
    }

    @Test fun inTwoHoursRollsTime() = assertEquals(LocalTime(12, 0), parse("call in 2 hours").time)

    @Test fun relativeTimePastMidnightRollsDate() {
        val p = parseCapture("job in 15 hours", LocalDateTime(2026, 9, 2, 22, 0))
        assertEquals(LocalDate(2026, 9, 3), p.date)
        assertEquals(LocalTime(13, 0), p.time)
    }

    @Test fun bareAtFiveIsEvening() = assertEquals(LocalTime(17, 0), parse("gym at 5").time)

    @Test fun bareAtNineIsMorning() = assertEquals(LocalTime(9, 0), parse("gym at 9").time)

    @Test fun bareAtFifteenIs24h() = assertEquals(LocalTime(15, 0), parse("gym at 15").time)

    @Test fun everyOtherWeek() = assertEquals("FREQ=WEEKLY;INTERVAL=2", parse("cleaning every other week").rrule)

    @Test fun everyWeekdayWord() =
        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", parse("standup every weekday").rrule)

    @Test fun twoTimesConflictStaysInbox() {
        val p = parse("call sam 3pm or 4pm")
        assertEquals(ItemType.INBOX, p.type)
        assertEquals("call sam 3pm or 4pm", p.title)
    }

    @Test fun combinedKitchenSink() {
        val p = parse("lunch with sam tomorrow at noon #work !! for 2h")
        assertEquals(ItemType.EVENT, p.type)
        assertEquals("lunch with sam", p.title)
        assertEquals(LocalDate(2026, 9, 3), p.date)
        assertEquals(LocalTime(12, 0), p.time)
        assertEquals(120, p.durationMinutes)
        assertEquals(Priority.HIGH, p.priority)
        assertEquals(listOf("work"), p.tags)
    }
}
