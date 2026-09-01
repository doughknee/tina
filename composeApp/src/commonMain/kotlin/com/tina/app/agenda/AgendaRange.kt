package com.tina.app.agenda

import com.tina.app.data.Item
import com.tina.app.data.ItemType
import com.tina.app.data.RecurrenceRule
import com.tina.app.data.expandOccurrences
import com.tina.app.data.parseRrule
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** How far the agenda zooms out. A fifth level would be data here, not a new screen. */
enum class Granularity { DAY, WEEK, MONTH, ALL }

/** Null bounds mean unbounded, which only the ALL range uses. */
data class AgendaRange(val start: LocalDate?, val end: LocalDate?, val granularity: Granularity) {
    companion object {
        fun day(date: LocalDate) = AgendaRange(date, date, Granularity.DAY)

        /** Seven days from the selected date, so the title reads "Sep 1–7" (see range-1-week). */
        fun week(date: LocalDate) =
            AgendaRange(date, date.plus(6, DateTimeUnit.DAY), Granularity.WEEK)

        fun month(date: LocalDate): AgendaRange {
            val first = LocalDate(date.year, date.month, 1)
            return AgendaRange(
                first,
                first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY),
                Granularity.MONTH,
            )
        }

        val All = AgendaRange(null, null, Granularity.ALL)
    }
}

enum class DaySection { MORNING, AFTERNOON, EVENING }

enum class HorizonBucket { TODAY, THIS_WEEK, LATER, SOMEDAY }

sealed interface GroupKey {
    /** Every range shows overdue once, at the top. */
    data object Overdue : GroupKey

    /** Rolled-up repeats. WEEK and MONTH only; ALL files series into their horizon bucket. */
    data object Series : GroupKey

    /** DAY range only. */
    data class TimeOfDay(val section: DaySection) : GroupKey

    /** WEEK and MONTH ranges. */
    data class Day(val date: LocalDate) : GroupKey

    /** ALL range only. */
    data class Horizon(val bucket: HorizonBucket) : GroupKey

    /** Undated tasks, once, at the end. */
    data object Anytime : GroupKey
}

sealed interface AgendaRow {
    val item: Item

    data class Single(override val item: Item, val time: LocalTime? = null) : AgendaRow

    /**
     * One row standing in for many occurrences. [doneMask] is per-day and only filled for the
     * WEEK range, where the dot strip needs it; elsewhere it is null.
     */
    data class Series(
        override val item: Item,
        val ruleLabel: String,
        val occurrencesInRange: Int,
        val nextDue: LocalDate,
        val doneMask: List<Boolean>?,
    ) : AgendaRow

    /** Same title, type and date captured more than once. Both records survive. */
    data class Duplicate(val primary: Item, val others: List<Item>) : AgendaRow {
        override val item: Item get() = primary
        val count: Int get() = others.size + 1
    }

    /** A multi-day event: one row per range, never one per day touched. */
    data class Span(
        override val item: Item,
        val first: LocalDate,
        val last: LocalDate,
        /** 1-based position within the span, for the DAY range's "Day 2 of 4"; null elsewhere. */
        val dayIndex: Int?,
        val dayCount: Int,
    ) : AgendaRow
}

/**
 * [rows] always holds everything; [hiddenCount] is how many of them fall past the range's cap.
 * Collapsed views render `rows.dropLast(hiddenCount)`, so expanding a group costs no rebuild.
 */
data class AgendaGroup(
    val key: GroupKey,
    val label: String,
    val rows: List<AgendaRow>,
    val hiddenCount: Int = 0,
)

/** Day-section boundaries and completion visibility, mirrored from Settings. */
data class AgendaSettings(
    val afternoonStartMinutes: Int = 12 * 60,
    val eveningStartMinutes: Int = 18 * 60,
    val showCompleted: Boolean = false,
)

/** Identifies one occurrence of a repeating item, for per-day completion. */
data class OccurrenceKey(val itemId: Long, val epochDay: Int)

/** Expand to individual rows at or below this many occurrences; roll up above it. */
const val SERIES_BUDGET = 3

private const val MONTH_DAY_CAP = 3
private const val HORIZON_BUCKET_CAP = 5

/** How far ahead the ALL range looks when expanding repeats and bucketing. */
private const val ALL_HORIZON_DAYS = 365
private const val SOMEDAY_AFTER_DAYS = 90

/**
 * The one agenda query. Every range returns the same row and group types, so all four views
 * share a renderer and widgets, notifications and Ask can read the same shapes rather than
 * re-deriving them — the de-duplication rules must live here, not in a screen.
 *
 * [ruleLabel] converts an RRULE to display text; it is injected because the localized version
 * is a @Composable, and [AgendaGroup.label] carries a plain-text fallback for the non-UI callers.
 */
fun buildAgenda(
    items: List<Item>,
    range: AgendaRange,
    today: LocalDate,
    tz: TimeZone,
    settings: AgendaSettings = AgendaSettings(),
    completedOccurrences: Set<OccurrenceKey> = emptySet(),
    ruleLabel: (String) -> String = ::defaultRuleLabel,
): List<AgendaGroup> {
    val rangeStart = range.start ?: today
    val rangeEnd = range.end ?: today.plus(ALL_HORIZON_DAYS, DateTimeUnit.DAY)
    val startMillis = rangeStart.atStartOfDayIn(tz).toEpochMilliseconds()
    val endMillis = rangeEnd.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()

    val candidates = items.filter {
        it.deletedAt == null &&
            (it.type == ItemType.TASK || it.type == ItemType.EVENT) &&
            (settings.showCompleted || !it.completed)
    }

    // Rule 4: overdue is positional — it appears here and never again in a day group.
    val overdue = candidates.filter {
        it.type == ItemType.TASK && !it.completed && it.dueDate != null &&
            it.dueDate!! < today.toEpochDays().toInt()
    }
    val overdueIds = overdue.map { it.id }.toSet()
    val rest = candidates.filter { it.id !in overdueIds }

    val placements = mutableListOf<Placement>()
    val seriesRows = mutableListOf<AgendaRow.Series>()
    val anytime = mutableListOf<Item>()

    for (item in rest) {
        val anchor = recurrenceAnchorMillis(item, tz)
        if (item.recurrence != null && anchor != null) {
            val dates = expandOccurrences(anchor, item.recurrence, startMillis, endMillis, tz)
                .map { Instant.fromEpochMilliseconds(it).toLocalDateTime(tz).date }
            if (dates.isEmpty()) continue
            // Rule 1: a handful of occurrences reads better as real rows than as a summary.
            if (dates.size <= SERIES_BUDGET) {
                dates.forEach { placements += Placement(item, it, timeOf(item, tz)) }
            } else {
                seriesRows += AgendaRow.Series(
                    item = item,
                    ruleLabel = item.recurrence?.let(ruleLabel).orEmpty(),
                    occurrencesInRange = dates.size,
                    nextDue = dates.firstOrNull { it >= today } ?: dates.first(),
                    doneMask = if (range.granularity == Granularity.WEEK) {
                        weekMask(item, dates, rangeStart, completedOccurrences)
                    } else {
                        null
                    },
                )
            }
            continue
        }

        val span = spanOf(item, tz)
        when {
            // Rule 3: a multi-day event is one row across the range, not one per day.
            span != null && span.first != span.second -> {
                if (span.second >= rangeStart && span.first <= rangeEnd) {
                    placements += Placement(
                        item,
                        span.first.coerceAtLeast(rangeStart),
                        null,
                        span = span,
                    )
                }
            }
            span != null -> {
                if (span.first in rangeStart..rangeEnd) {
                    placements += Placement(item, span.first, timeOf(item, tz))
                }
            }
            item.dueLocalDate != null -> {
                val date = item.dueLocalDate!!
                if (date in rangeStart..rangeEnd) placements += Placement(item, date, item.dueLocalTime)
            }
            // Rule 4: undated tasks live in one Anytime group at the end.
            else -> anytime += item
        }
    }

    return when (range.granularity) {
        Granularity.DAY -> dayGroups(overdue, placements, seriesRows, anytime, rangeStart, settings)
        Granularity.WEEK, Granularity.MONTH ->
            spanGroups(overdue, placements, seriesRows, anytime, range, ruleLabel)
        Granularity.ALL -> horizonGroups(overdue, placements, seriesRows, anytime, today)
    }
}

private data class Placement(
    val item: Item,
    val date: LocalDate,
    val time: LocalTime?,
    val span: Pair<LocalDate, LocalDate>? = null,
)

/** A built row plus the date it files under — rows themselves don't all carry one. */
private data class DatedRow(val date: LocalDate, val row: AgendaRow)

// ---------------------------------------------------------------- range shapes

private fun dayGroups(
    overdue: List<Item>,
    placements: List<Placement>,
    series: List<AgendaRow.Series>,
    anytime: List<Item>,
    date: LocalDate,
    settings: AgendaSettings,
): List<AgendaGroup> {
    val rows = mergeDuplicates(placements, dayIndexAgainst = date).map { it.row }
    val timed = rows.filter { minutesOf(it) >= 0 }
    val untimed = rows.filter { minutesOf(it) < 0 }

    fun section(from: Int, until: Int) =
        timed.filter { minutesOf(it) in from until until }.sortedBy { minutesOf(it) }

    return buildList {
        addGroup(GroupKey.Overdue, "Overdue", overdue.map { AgendaRow.Single(it, it.dueLocalTime) })
        // series here are ordinary rows: a daily repeat on this date is just one occurrence
        addGroup(
            GroupKey.TimeOfDay(DaySection.MORNING),
            "Morning",
            section(0, settings.afternoonStartMinutes),
        )
        addGroup(
            GroupKey.TimeOfDay(DaySection.AFTERNOON),
            "Afternoon",
            section(settings.afternoonStartMinutes, settings.eveningStartMinutes),
        )
        addGroup(
            GroupKey.TimeOfDay(DaySection.EVENING),
            "Evening",
            section(settings.eveningStartMinutes, 24 * 60 + 1),
        )
        addGroup(
            GroupKey.Anytime,
            "Anytime",
            untimed + series + anytime.sortedBy { it.sortOrder }.map { AgendaRow.Single(it) },
        )
    }
}

private fun spanGroups(
    overdue: List<Item>,
    placements: List<Placement>,
    series: List<AgendaRow.Series>,
    anytime: List<Item>,
    range: AgendaRange,
    ruleLabel: (String) -> String,
): List<AgendaGroup> {
    val week = range.granularity == Granularity.WEEK
    val cap = if (week) Int.MAX_VALUE else MONTH_DAY_CAP
    val byDate = mergeDuplicates(placements, dayIndexAgainst = null).groupBy { it.date }

    return buildList {
        addGroup(GroupKey.Overdue, "Overdue", overdue.map { AgendaRow.Single(it, it.dueLocalTime) })
        addGroup(
            GroupKey.Series,
            if (week) "Repeating this week" else "Series",
            series.sortedBy { it.nextDue },
        )
        byDate.entries.sortedBy { it.key }.forEach { (date, dated) ->
            addGroup(
                GroupKey.Day(date),
                dayLabel(date),
                dated.map { it.row }.sortedBy { minutesOf(it) },
                cap,
            )
        }
        addGroup(
            GroupKey.Anytime,
            "Anytime",
            anytime.sortedBy { it.sortOrder }.map { AgendaRow.Single(it) },
            cap,
        )
    }
}

private fun horizonGroups(
    overdue: List<Item>,
    placements: List<Placement>,
    series: List<AgendaRow.Series>,
    anytime: List<Item>,
    today: LocalDate,
): List<AgendaGroup> {
    val endOfWeek = today.plus(7 - today.dayOfWeek.isoDayNumber, DateTimeUnit.DAY)
    val somedayFrom = today.plus(SOMEDAY_AFTER_DAYS, DateTimeUnit.DAY)

    fun bucketOf(date: LocalDate): HorizonBucket = when {
        date <= today -> HorizonBucket.TODAY
        date <= endOfWeek -> HorizonBucket.THIS_WEEK
        date < somedayFrom -> HorizonBucket.LATER
        else -> HorizonBucket.SOMEDAY
    }

    // ALL has no Series group: a rolled-up repeat files under whenever it next comes due.
    val dated = mergeDuplicates(placements, dayIndexAgainst = null) +
        series.map { DatedRow(it.nextDue, it) }
    val byBucket = dated.groupBy { bucketOf(it.date) }

    return buildList {
        addGroup(GroupKey.Overdue, "Overdue", overdue.map { AgendaRow.Single(it, it.dueLocalTime) })
        HorizonBucket.entries.forEach { bucket ->
            addGroup(
                GroupKey.Horizon(bucket),
                horizonLabel(bucket),
                byBucket[bucket].orEmpty().sortedBy { it.date }.map { it.row },
                HORIZON_BUCKET_CAP,
            )
        }
        addGroup(
            GroupKey.Anytime,
            "Anytime",
            anytime.sortedBy { it.sortOrder }.map { AgendaRow.Single(it) },
            HORIZON_BUCKET_CAP,
        )
    }
}

/** Empty groups are omitted entirely — never an empty header. Rule 5 attaches the cap. */
private fun MutableList<AgendaGroup>.addGroup(
    key: GroupKey,
    label: String,
    rows: List<AgendaRow>,
    cap: Int = Int.MAX_VALUE,
) {
    if (rows.isEmpty()) return
    add(AgendaGroup(key, label, rows, (rows.size - cap).coerceAtLeast(0)))
}

// ---------------------------------------------------------------- rules

/**
 * Rule 2. Same normalised title, type and date collapses to one row carrying the others,
 * so a double-capture or an import collision reads as "×2" instead of two identical lines.
 */
private fun mergeDuplicates(
    placements: List<Placement>,
    dayIndexAgainst: LocalDate?,
): List<DatedRow> {
    val out = mutableListOf<DatedRow>()
    val consumed = mutableSetOf<Int>()
    placements.forEachIndexed { index, placement ->
        if (index in consumed) return@forEachIndexed
        val twins = placements.withIndex().filter { (other, candidate) ->
            other > index && other !in consumed &&
                candidate.span == null && placement.span == null &&
                candidate.date == placement.date &&
                candidate.item.type == placement.item.type &&
                normalizeTitle(candidate.item.title) == normalizeTitle(placement.item.title)
        }
        twins.forEach { consumed += it.index }
        val row = when {
            placement.span != null -> {
                val (first, last) = placement.span
                AgendaRow.Span(
                    item = placement.item,
                    first = first,
                    last = last,
                    dayIndex = dayIndexAgainst?.let { first.daysUntilInclusive(it) },
                    dayCount = first.daysUntilInclusive(last),
                )
            }
            twins.isEmpty() -> AgendaRow.Single(placement.item, placement.time)
            else -> AgendaRow.Duplicate(placement.item, twins.map { it.value.item })
        }
        out += DatedRow(placement.date, row)
    }
    return out
}

/** Lowercase, drop punctuation, collapse whitespace. */
fun normalizeTitle(title: String): String =
    title.lowercase().filter { it.isLetterOrDigit() || it.isWhitespace() }.split(" ")
        .filter { it.isNotBlank() }.joinToString(" ")

private fun weekMask(
    item: Item,
    dates: List<LocalDate>,
    weekStart: LocalDate,
    completed: Set<OccurrenceKey>,
): List<Boolean> = (0..6).map { offset ->
    val date = weekStart.plus(offset, DateTimeUnit.DAY)
    date in dates && OccurrenceKey(item.id, date.toEpochDays().toInt()) in completed
}

// ---------------------------------------------------------------- helpers

/** Events anchor on their start; repeating tasks (once the schema carries them) on their due. */
private fun recurrenceAnchorMillis(item: Item, tz: TimeZone): Long? = when {
    item.startAt != null -> item.startAt
    item.dueDate != null -> item.dueLocalDate!!.atStartOfDayIn(tz).toEpochMilliseconds() +
        (item.dueTime ?: 0) * 60_000L
    else -> null
}

/** First and last local date an event covers, or null if it is not an event with a start. */
private fun spanOf(item: Item, tz: TimeZone): Pair<LocalDate, LocalDate>? {
    val start = item.startAt ?: return null
    val first = Instant.fromEpochMilliseconds(start).toLocalDateTime(tz).date
    val endMillis = item.endAt ?: return first to first
    // an all-day event ends at midnight of the following day; that day is not covered
    val rawLast = Instant.fromEpochMilliseconds(endMillis - 1).toLocalDateTime(tz).date
    return first to maxOf(first, rawLast)
}

private fun timeOf(item: Item, tz: TimeZone): LocalTime? = when {
    item.type == ItemType.EVENT && item.allDay -> null
    item.startAt != null -> Instant.fromEpochMilliseconds(item.startAt!!).toLocalDateTime(tz).time
    else -> item.dueLocalTime
}

private fun minutesOf(row: AgendaRow): Int = when (row) {
    is AgendaRow.Single -> row.time?.let { it.hour * 60 + it.minute } ?: -1
    else -> -1
}

private fun LocalDate.daysUntilInclusive(other: LocalDate): Int =
    (this.toEpochDays() - other.toEpochDays()).let { if (it < 0) -it else it }.toInt() + 1

private fun dayLabel(date: LocalDate): String = "${date.month.name.take(3).lowercase()
    .replaceFirstChar { it.uppercase() }} ${date.day}"

private fun horizonLabel(bucket: HorizonBucket) = when (bucket) {
    HorizonBucket.TODAY -> "Today"
    HorizonBucket.THIS_WEEK -> "This week"
    HorizonBucket.LATER -> "Later"
    HorizonBucket.SOMEDAY -> "Someday"
}

/** Plain-English fallback for non-UI callers; the screens pass the localized formatter. */
fun defaultRuleLabel(rrule: String): String {
    val rule = parseRrule(rrule) ?: return "Repeats"
    val day = rule.byDay.singleOrNull()
    if (day != null) {
        return "Every ${day.name.lowercase().replaceFirstChar { it.uppercase() }}"
    }
    if (rule.byDay.size == 5) return "Weekdays"
    return when (rule.freq) {
        RecurrenceRule.Freq.DAILY -> "Daily"
        RecurrenceRule.Freq.WEEKLY -> "Weekly"
        RecurrenceRule.Freq.MONTHLY -> "Monthly"
        RecurrenceRule.Freq.YEARLY -> "Yearly"
    }
}
