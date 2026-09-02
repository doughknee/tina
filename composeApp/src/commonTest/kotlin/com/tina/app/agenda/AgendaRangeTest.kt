package com.tina.app.agenda

import com.tina.app.data.Item
import com.tina.app.data.ItemType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val TZ = TimeZone.UTC
private val TODAY = LocalDate(2026, 9, 1)

private fun millis(date: LocalDate, hour: Int = 0, minute: Int = 0) =
    LocalDateTime(date, LocalTime(hour, minute)).toInstant(TZ).toEpochMilliseconds()

private fun task(
    id: Long,
    title: String,
    due: LocalDate? = TODAY,
    time: Int? = null,
    completed: Boolean = false,
    rrule: String? = null,
) = Item(
    id = id,
    title = title,
    type = ItemType.TASK,
    createdAt = 0,
    updatedAt = 0,
    dueDate = due?.toEpochDays()?.toInt(),
    dueTime = time,
    completed = completed,
    recurrence = rrule,
)

private fun event(
    id: Long,
    title: String,
    start: LocalDate = TODAY,
    startHour: Int = 9,
    endDate: LocalDate? = null,
    rrule: String? = null,
    allDay: Boolean = false,
) = Item(
    id = id,
    title = title,
    type = ItemType.EVENT,
    createdAt = 0,
    updatedAt = 0,
    startAt = millis(start, startHour),
    endAt = millis(endDate ?: start, if (endDate != null) 0 else startHour + 1)
        .let { if (endDate != null) millis(endDate.plus(1, DateTimeUnit.DAY)) else it },
    allDay = allDay,
    recurrence = rrule,
)

private fun rows(groups: List<AgendaGroup>) = groups.flatMap { it.rows }

private fun group(groups: List<AgendaGroup>, key: GroupKey) = groups.firstOrNull { it.key == key }

class AgendaRangeTest {

    // ---- series budget boundary (3 expands, 4 rolls up)

    @Test
    fun threeOccurrencesExpandToSingleRows() {
        // a weekly repeat over a 3-week window lands exactly 3 times
        val items = listOf(event(1, "Standup", rrule = "FREQ=WEEKLY;BYDAY=TU"))
        val range = AgendaRange(TODAY, TODAY.plus(20, DateTimeUnit.DAY), Granularity.MONTH)
        val result = rows(buildAgenda(items, range, TODAY, TZ))
        assertEquals(3, result.size)
        assertTrue(result.all { it is AgendaRow.Single })
    }

    @Test
    fun fourOccurrencesRollUpToOneSeriesRow() {
        val items = listOf(event(1, "Standup", rrule = "FREQ=WEEKLY;BYDAY=TU"))
        val range = AgendaRange(TODAY, TODAY.plus(27, DateTimeUnit.DAY), Granularity.MONTH)
        val result = rows(buildAgenda(items, range, TODAY, TZ))
        assertEquals(1, result.size)
        val series = result.single() as AgendaRow.Series
        assertEquals(4, series.occurrencesInRange)
        assertEquals(TODAY, series.nextDue)
    }

    @Test
    fun aDailyRepeatIsOneRowInDayOneInWeekOneInMonthAndOneInAll() {
        val items = listOf(event(1, "Morning pages", rrule = "FREQ=DAILY"))
        assertEquals(1, rows(buildAgenda(items, AgendaRange.day(TODAY), TODAY, TZ)).size)
        assertEquals(1, rows(buildAgenda(items, AgendaRange.week(TODAY), TODAY, TZ)).size)
        assertEquals(1, rows(buildAgenda(items, AgendaRange.month(TODAY), TODAY, TZ)).size)
        assertEquals(1, rows(buildAgenda(items, AgendaRange.All, TODAY, TZ)).size)
    }

    @Test
    fun weekSeriesCarriesASevenDayDoneMask() {
        val items = listOf(event(1, "Morning pages", rrule = "FREQ=DAILY"))
        val done = setOf(
            OccurrenceKey(1, TODAY.toEpochDays().toInt()),
            OccurrenceKey(1, TODAY.plus(1, DateTimeUnit.DAY).toEpochDays().toInt()),
        )
        val result = rows(buildAgenda(items, AgendaRange.week(TODAY), TODAY, TZ, completedOccurrences = done))
        val series = result.single() as AgendaRow.Series
        assertEquals(listOf(true, true, false, false, false, false, false), series.doneMask)
    }

    @Test
    fun onlyTheWeekRangeBuildsADoneMask() {
        val items = listOf(event(1, "Morning pages", rrule = "FREQ=DAILY"))
        val month = rows(buildAgenda(items, AgendaRange.month(TODAY), TODAY, TZ)).single()
        assertNull((month as AgendaRow.Series).doneMask)
    }

    // ---- duplicate normalisation

    @Test
    fun identicalCapturesOnTheSameDayMergeIntoOneRow() {
        val items = listOf(task(1, "Call with Phil"), task(2, "call with phil!"))
        val result = rows(buildAgenda(items, AgendaRange.day(TODAY), TODAY, TZ))
        val duplicate = result.single() as AgendaRow.Duplicate
        assertEquals(2, duplicate.count)
        // both records survive the merge
        assertEquals(1L, duplicate.primary.id)
        assertEquals(listOf(2L), duplicate.others.map { it.id })
    }

    @Test
    fun sameTitleOnDifferentDaysDoesNotMerge() {
        val items = listOf(task(1, "Water plants"), task(2, "Water plants", due = TODAY.plus(1, DateTimeUnit.DAY)))
        val result = rows(buildAgenda(items, AgendaRange.week(TODAY), TODAY, TZ))
        assertEquals(2, result.size)
        assertTrue(result.all { it is AgendaRow.Single })
    }

    @Test
    fun sameTitleDifferentTypeDoesNotMerge() {
        val items = listOf(task(1, "Standup"), event(2, "Standup"))
        val result = rows(buildAgenda(items, AgendaRange.day(TODAY), TODAY, TZ))
        assertEquals(2, result.size)
    }

    @Test
    fun titleNormalisationIgnoresCasePunctuationAndSpacing() {
        assertEquals(normalizeTitle("Call  with Phil!"), normalizeTitle("call with phil"))
        assertEquals(normalizeTitle("Pick-up chair"), normalizeTitle("pickup chair"))
    }

    // ---- span placement

    @Test
    fun aFourDayEventIsExactlyOneRowInEveryRange() {
        val conference = event(1, "Conference", start = TODAY.plus(2, DateTimeUnit.DAY),
            endDate = TODAY.plus(5, DateTimeUnit.DAY), allDay = true)
        listOf(
            AgendaRange.week(TODAY),
            AgendaRange.month(TODAY),
            AgendaRange.All,
        ).forEach { range ->
            val spans = rows(buildAgenda(listOf(conference), range, TODAY, TZ))
                .filterIsInstance<AgendaRow.Span>()
            assertEquals(1, spans.size, "range ${range.granularity}")
            assertEquals(4, spans.single().dayCount)
        }
    }

    @Test
    fun spanReportsItsPositionOnlyInTheDayRange() {
        val conference = event(1, "Conference", start = TODAY,
            endDate = TODAY.plus(3, DateTimeUnit.DAY), allDay = true)
        val secondDay = TODAY.plus(1, DateTimeUnit.DAY)
        val day = rows(buildAgenda(listOf(conference), AgendaRange.day(secondDay), secondDay, TZ))
            .filterIsInstance<AgendaRow.Span>().single()
        assertEquals(2, day.dayIndex)
        assertEquals(4, day.dayCount)

        val week = rows(buildAgenda(listOf(conference), AgendaRange.week(TODAY), TODAY, TZ))
            .filterIsInstance<AgendaRow.Span>().single()
        assertNull(week.dayIndex)
    }

    // ---- positional items

    @Test
    fun overdueAppearsOnceAtTheTopAndNeverInADayGroup() {
        val late = task(1, "Submit expense report", due = TODAY.minus())
        listOf(AgendaRange.day(TODAY), AgendaRange.week(TODAY), AgendaRange.month(TODAY), AgendaRange.All)
            .forEach { range ->
                val groups = buildAgenda(listOf(late), range, TODAY, TZ)
                assertEquals(1, rows(groups).size, "range ${range.granularity}")
                assertEquals(GroupKey.Overdue, groups.first().key)
            }
    }

    @Test
    fun undatedTasksNeverAppearOnPlan() {
        // a someday item lives on Sort until it has a date
        val groups = buildAgenda(listOf(task(1, "Fix the gate", due = null)), AgendaRange.All, TODAY, TZ)
        assertTrue(groups.isEmpty())
    }

    // ---- caps

    @Test
    fun monthDayGroupsCapAtThreeRowsButKeepTheRest() {
        val items = (1..6L).map { task(it, "Task $it", time = 60 * it.toInt()) }
        val groups = buildAgenda(items, AgendaRange.month(TODAY), TODAY, TZ)
        val day = group(groups, GroupKey.Day(TODAY))!!
        assertEquals(6, day.rows.size)
        assertEquals(3, day.hiddenCount)
        assertEquals(3, day.rows.size - day.hiddenCount)
    }

    @Test
    fun horizonBucketsCapAtFive() {
        val items = (1..8L).map { task(it, "Task $it") }
        val groups = buildAgenda(items, AgendaRange.All, TODAY, TZ)
        val todayBucket = group(groups, GroupKey.Horizon(HorizonBucket.TODAY))!!
        assertEquals(8, todayBucket.rows.size)
        assertEquals(3, todayBucket.hiddenCount)
    }

    @Test
    fun weekGroupsAreNotCapped() {
        val items = (1..6L).map { task(it, "Task $it", time = 60 * it.toInt()) }
        val groups = buildAgenda(items, AgendaRange.week(TODAY), TODAY, TZ)
        assertEquals(0, group(groups, GroupKey.Day(TODAY))!!.hiddenCount)
    }

    @Test
    fun emptyDaysAreOmittedRatherThanShownAsEmptyHeaders() {
        val items = listOf(task(1, "Only task", due = TODAY.plus(2, DateTimeUnit.DAY)))
        val groups = buildAgenda(items, AgendaRange.week(TODAY), TODAY, TZ)
        assertEquals(1, groups.size)
        assertEquals(GroupKey.Day(TODAY.plus(2, DateTimeUnit.DAY)), groups.single().key)
    }

    // ---- day sections

    @Test
    fun dayRangeSplitsByTimeOfDay() {
        val items = listOf(
            task(1, "Early", time = 8 * 60),
            task(2, "Midday", time = 13 * 60),
            task(3, "Late", time = 20 * 60),
            task(4, "Whenever", time = null),
        )
        val groups = buildAgenda(items, AgendaRange.day(TODAY), TODAY, TZ)
        assertEquals(
            listOf(
                GroupKey.TimeOfDay(DaySection.MORNING),
                GroupKey.TimeOfDay(DaySection.AFTERNOON),
                GroupKey.TimeOfDay(DaySection.EVENING),
                GroupKey.Anytime,
            ),
            groups.map { it.key },
        )
    }

    @Test
    fun completedItemsAreHiddenUnlessAskedFor() {
        val items = listOf(task(1, "Done thing", completed = true))
        assertTrue(rows(buildAgenda(items, AgendaRange.day(TODAY), TODAY, TZ)).isEmpty())
        val shown = buildAgenda(
            items, AgendaRange.day(TODAY), TODAY, TZ,
            settings = AgendaSettings(showCompleted = true),
        )
        assertEquals(1, rows(shown).size)
    }

    // ---- repeating tasks

    @Test
    fun aRepeatingTaskAnchoredInThePastIsNotOverdue() {
        val habit = task(1, "Water plants", due = TODAY.minus(), rrule = "FREQ=DAILY")
        val groups = buildAgenda(listOf(habit), AgendaRange.day(TODAY), TODAY, TZ)
        assertNull(group(groups, GroupKey.Overdue))
        val row = rows(groups).single() as AgendaRow.Single
        assertEquals(TODAY, row.date)
        assertEquals(false, row.done)
    }

    @Test
    fun aRepeatingTaskOccurrenceCarriesItsDoneState() {
        val habit = task(1, "Water plants", due = TODAY, rrule = "FREQ=DAILY")
        val done = setOf(OccurrenceKey(1, TODAY.toEpochDays().toInt()))
        val shown = rows(buildAgenda(listOf(habit), AgendaRange.day(TODAY), TODAY, TZ,
            settings = AgendaSettings(showCompleted = true), completedOccurrences = done)).single() as AgendaRow.Single
        assertEquals(true, shown.done)
        // hidden entirely when completed rows are hidden
        assertTrue(rows(buildAgenda(listOf(habit), AgendaRange.day(TODAY), TODAY, TZ, completedOccurrences = done)).isEmpty())
    }

    // ---- next occurrence skips done and skipped days

    @Test
    fun nextDueSkipsCompletedAndSkippedOccurrences() {
        val items = listOf(event(1, "Morning pages", rrule = "FREQ=DAILY"))
        val done = setOf(OccurrenceKey(1, TODAY.toEpochDays().toInt()))
        val skipped = setOf(OccurrenceKey(1, TODAY.plus(1, DateTimeUnit.DAY).toEpochDays().toInt()))
        val series = rows(buildAgenda(items, AgendaRange.week(TODAY), TODAY, TZ, completedOccurrences = done, skippedOccurrences = skipped))
            .single() as AgendaRow.Series
        assertEquals(TODAY.plus(2, DateTimeUnit.DAY), series.nextDue)
        assertEquals(7, series.dates.size)
    }

    @Test
    fun nextDueFallsBackToTheLastOccurrenceWhenAllAreDone() {
        val items = listOf(event(1, "Morning pages", rrule = "FREQ=DAILY"))
        val done = (0..6).map { OccurrenceKey(1, TODAY.plus(it, DateTimeUnit.DAY).toEpochDays().toInt()) }.toSet()
        val series = rows(buildAgenda(items, AgendaRange.week(TODAY), TODAY, TZ, completedOccurrences = done))
            .single() as AgendaRow.Series
        assertEquals(TODAY.plus(6, DateTimeUnit.DAY), series.nextDue)
    }
}

private fun LocalDate.minus(): LocalDate = LocalDate.fromEpochDays(this.toEpochDays() - 2)
