package com.tina.app.capture

import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import com.tina.app.data.byDayCode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

data class ParsedCapture(
    val title: String,
    val type: ItemType,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val durationMinutes: Int? = null,
    val priority: Priority = Priority.NONE,
    val tags: List<String> = emptyList(),
    val rrule: String? = null,
    val body: String? = null,
)

private val RE_TAG = Regex("""#([\p{L}\d_-]+)""")
private val RE_PRIORITY = Regex("""(!{1,2})(?=\s|$)""")
private val RE_SENTENCE_SPLIT = Regex("""[.!?]+(?=\s|$)""")

private const val WEEKDAYS =
    "monday|tuesday|wednesday|thursday|friday|saturday|sunday|tues|thurs|thur|mon|tue|wed|thu|fri|sat|sun"
private const val MONTHS =
    "january|february|march|april|june|july|august|september|october|november|december|" +
        "sept|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec"

private val RE_EVERY_OTHER = Regex("""\bevery\s+other\s+(day|week|month|year)\b""", RegexOption.IGNORE_CASE)
private val RE_EVERY_WEEKDAY_WORD = Regex("""\bevery\s+weekday\b""", RegexOption.IGNORE_CASE)
private val RE_EVERY_WEEKDAY = Regex("""\bevery\s+($WEEKDAYS)\b""", RegexOption.IGNORE_CASE)
private val RE_EVERY_UNIT = Regex("""\bevery\s+(day|week|month|year)\b""", RegexOption.IGNORE_CASE)
private val RE_DURATION_H = Regex(
    """\bfor\s+(\d{1,3})\s*h(?:(?:ou)?rs?)?\b(?:\s*(\d{1,2})\s*m(?:in(?:ute)?s?)?\b)?""",
    RegexOption.IGNORE_CASE,
)
private val RE_DURATION_M = Regex("""\bfor\s+(\d{1,4})\s*m(?:in(?:ute)?s?)?\b""", RegexOption.IGNORE_CASE)
private val RE_TIME_COLON = Regex("""(?:\bat\s+)?\b(\d{1,2}):(\d{2})\s*(am|pm)?\b""", RegexOption.IGNORE_CASE)
private val RE_TIME_AMPM = Regex("""(?:\bat\s+)?\b(\d{1,2})\s*(am|pm)\b""", RegexOption.IGNORE_CASE)
private val RE_TIME_AT_BARE = Regex("""\bat\s+(\d{1,2})\b(?!\s*:|\s*(?:am|pm))""", RegexOption.IGNORE_CASE)
private val RE_IN_REL_TIME =
    Regex("""\bin\s+(\d{1,3})\s*(min(?:ute)?s?|h(?:(?:ou)?rs?)?)\b""", RegexOption.IGNORE_CASE)
private val RE_TONIGHT = Regex("""\btonight\b""", RegexOption.IGNORE_CASE)
private val TIME_WORDS = listOf(
    Regex("""\bnoon\b""", RegexOption.IGNORE_CASE) to LocalTime(12, 0),
    Regex("""\bmidnight\b""", RegexOption.IGNORE_CASE) to LocalTime(0, 0),
    // a part-of-day word is a time only after a day word or at the end of the phrase;
    // otherwise it is part of the title ("morning pages", "evening walk")
    Regex("""(?:(?<=\b(?:this|the|tomorrow|today|monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)\s)morning\b|\bmorning\s*$)""", RegexOption.IGNORE_CASE) to LocalTime(9, 0),
    Regex("""(?:(?<=\b(?:this|the|tomorrow|today|monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)\s)afternoon\b|\bafternoon\s*$)""", RegexOption.IGNORE_CASE) to LocalTime(14, 0),
    Regex("""(?:(?<=\b(?:this|the|tomorrow|today|monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)\s)evening\b|\bevening\s*$)""", RegexOption.IGNORE_CASE) to LocalTime(19, 0),
)
private val RE_TODAY = Regex("""\btoday\b""", RegexOption.IGNORE_CASE)
private val RE_TOMORROW = Regex("""\btomorrow\b|\btmrw\b""", RegexOption.IGNORE_CASE)
private val RE_NEXT_WEEK = Regex("""\bnext\s+week\b""", RegexOption.IGNORE_CASE)
private val RE_NEXT_MONTH = Regex("""\bnext\s+month\b""", RegexOption.IGNORE_CASE)
private val RE_END_OF_WEEK = Regex("""\bend\s+of\s+(?:the\s+)?week\b""", RegexOption.IGNORE_CASE)
private val RE_END_OF_MONTH = Regex("""\bend\s+of\s+(?:the\s+)?month\b""", RegexOption.IGNORE_CASE)
private val RE_IN_N = Regex("""\bin\s+(\d{1,3})\s+(day|week|month)s?\b""", RegexOption.IGNORE_CASE)
private val RE_WEEKDAY = Regex("""\b(next\s+)?($WEEKDAYS)\b""", RegexOption.IGNORE_CASE)
private val RE_MONTH_DAY = Regex("""\b($MONTHS)\s+(\d{1,2})(?:st|nd|rd|th)?\b""", RegexOption.IGNORE_CASE)
private val RE_DAY_MONTH = Regex("""\b(\d{1,2})(?:st|nd|rd|th)?\s+($MONTHS)\b""", RegexOption.IGNORE_CASE)
private val RE_NUMERIC_MD = Regex("""\b(\d{1,2})/(\d{1,2})\b""")

private val CONNECTORS = setOf("on", "at", "in", "for", "by", "due", "the", "a", "an", "and")

private fun weekdayFrom(token: String): DayOfWeek = when (token.take(3).lowercase()) {
    "mon" -> DayOfWeek.MONDAY
    "tue" -> DayOfWeek.TUESDAY
    "wed" -> DayOfWeek.WEDNESDAY
    "thu" -> DayOfWeek.THURSDAY
    "fri" -> DayOfWeek.FRIDAY
    "sat" -> DayOfWeek.SATURDAY
    else -> DayOfWeek.SUNDAY
}

private fun monthFrom(token: String): Int = when (token.take(3).lowercase()) {
    "jan" -> 1; "feb" -> 2; "mar" -> 3; "apr" -> 4; "may" -> 5; "jun" -> 6
    "jul" -> 7; "aug" -> 8; "sep" -> 9; "oct" -> 10; "nov" -> 11; else -> 12
}

/** Next occurrence of [dow] after [from] (1..7 days out), or [from] itself when [orSame]. */
private fun nextOccurrence(from: LocalDate, dow: DayOfWeek, orSame: Boolean = false): LocalDate {
    var delta = (dow.isoDayNumber - from.dayOfWeek.isoDayNumber + 7) % 7
    if (delta == 0 && !orSame) delta = 7
    return from.plus(delta, DateTimeUnit.DAY)
}

/** "jan 5" / "1/5": this year, or next year if already past. Invalid dates return null. */
private fun resolveMonthDay(month: Int, day: Int, today: LocalDate): LocalDate? {
    if (month !in 1..12) return null
    val thisYear = runCatching { LocalDate(today.year, month, day) }.getOrNull() ?: return null
    return if (thisYear < today) {
        runCatching { LocalDate(today.year + 1, month, day) }.getOrNull() ?: thisYear
    } else thisYear
}

private fun IntRange.intersects(other: IntRange): Boolean = first <= other.last && other.first <= last

private fun cleanTitle(text: String): String {
    val collapsed = text.replace(Regex("""\s+"""), " ").trim().trim(',', ';', '-', ' ')
    val words = collapsed.split(" ").toMutableList()
    while (words.size > 1 && words.last().lowercase() in CONNECTORS) words.removeAt(words.size - 1)
    while (words.size > 1 && words.first().lowercase() in CONNECTORS) words.removeAt(0)
    return words.joinToString(" ").trim(',', ';', '-', ' ')
}

/**
 * Deterministic natural-language capture parser. [now] is injected for testability;
 * [firstDayOfWeek] anchors "next week".
 */
fun parseCapture(
    raw: String,
    now: LocalDateTime,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
): ParsedCapture {
    val input = raw.trim()
    if (input.isEmpty()) return ParsedCapture(title = "", type = ItemType.INBOX)
    val today = now.date

    val tags = RE_TAG.findAll(input).map { it.groupValues[1].lowercase() }.distinct().toList()

    // Prose beats embedded signals: long text is a note.
    val sentences = input.split(RE_SENTENCE_SPLIT).map { it.trim() }.filter { it.isNotEmpty() }
    val nonBlankLines = input.lines().filter { it.isNotBlank() }
    if (sentences.size >= 3 || nonBlankLines.size >= 3 || input.length > 200) {
        val title = (sentences.firstOrNull()?.lineSequence()?.first() ?: input).take(80).trim()
        return ParsedCapture(title = title, type = ItemType.NOTE, tags = tags, body = input)
    }

    var text = RE_TAG.replace(input, " ")

    var priority = Priority.NONE
    RE_PRIORITY.find(text)?.let {
        priority = if (it.groupValues[1].length >= 2) Priority.HIGH else Priority.MEDIUM
        text = text.removeRange(it.range)
    }

    var rrule: String? = null
    var rruleWeekday: DayOfWeek? = null
    val everyOther = RE_EVERY_OTHER.find(text)
    val everyWeekdayWord = if (everyOther == null) RE_EVERY_WEEKDAY_WORD.find(text) else null
    val everyWeekday = if (everyOther == null && everyWeekdayWord == null) RE_EVERY_WEEKDAY.find(text) else null
    when {
        everyOther != null -> {
            rrule = when (everyOther.groupValues[1].lowercase()) {
                "day" -> "FREQ=DAILY;INTERVAL=2"
                "week" -> "FREQ=WEEKLY;INTERVAL=2"
                "month" -> "FREQ=MONTHLY;INTERVAL=2"
                else -> "FREQ=YEARLY;INTERVAL=2"
            }
            text = text.removeRange(everyOther.range)
        }
        everyWeekdayWord != null -> {
            rrule = "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
            text = text.removeRange(everyWeekdayWord.range)
        }
        everyWeekday != null -> {
            rruleWeekday = weekdayFrom(everyWeekday.groupValues[1])
            rrule = "FREQ=WEEKLY;BYDAY=${byDayCode(rruleWeekday)}"
            text = text.removeRange(everyWeekday.range)
        }
        else -> {
            RE_EVERY_UNIT.find(text)?.let {
                rrule = when (it.groupValues[1].lowercase()) {
                    "day" -> "FREQ=DAILY"
                    "week" -> "FREQ=WEEKLY"
                    "month" -> "FREQ=MONTHLY"
                    else -> "FREQ=YEARLY"
                }
                text = text.removeRange(it.range)
            }
        }
    }

    var durationMinutes: Int? = null
    val durH = RE_DURATION_H.find(text)
    if (durH != null) {
        val hours = durH.groupValues[1].toInt()
        val minutes = durH.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: 0
        durationMinutes = hours * 60 + minutes
        text = text.removeRange(durH.range)
    } else {
        RE_DURATION_M.find(text)?.let {
            durationMinutes = it.groupValues[1].toInt()
            text = text.removeRange(it.range)
        }
    }

    var time: LocalTime? = null
    var dateFromTimeWord: LocalDate? = null

    // Collect explicit clock times first; more than one distinct time is ambiguous.
    data class TimeHit(val range: IntRange, val time: LocalTime)
    val timeHits = mutableListOf<TimeHit>()
    for (match in RE_TIME_COLON.findAll(text)) {
        val h = match.groupValues[1].toInt()
        val m = match.groupValues[2].toInt()
        val ampm = match.groupValues[3].lowercase()
        val hour = when {
            ampm.isEmpty() -> h.takeIf { it <= 23 }
            h !in 1..12 -> null
            ampm == "am" -> if (h == 12) 0 else h
            else -> if (h == 12) 12 else h + 12
        }
        if (hour != null && m <= 59) timeHits += TimeHit(match.range, LocalTime(hour, m))
    }
    for (match in RE_TIME_AMPM.findAll(text)) {
        if (timeHits.any { it.range.intersects(match.range) }) continue
        val h = match.groupValues[1].toInt()
        if (h !in 1..12) continue
        val hour = if (match.groupValues[2].equals("am", true)) {
            if (h == 12) 0 else h
        } else {
            if (h == 12) 12 else h + 12
        }
        timeHits += TimeHit(match.range, LocalTime(hour, 0))
    }
    if (timeHits.map { it.time }.distinct().size > 1) {
        // "3pm or 4pm": don't guess, let the human decide.
        return ParsedCapture(title = input, type = ItemType.INBOX, tags = tags)
    }
    timeHits.firstOrNull()?.let {
        time = it.time
        text = text.removeRange(it.range)
    }

    if (time == null) {
        // "in 30 min" / "in 2 hours": relative to now, pins both date and time
        RE_IN_REL_TIME.find(text)?.let { match ->
            val amount = match.groupValues[1].toInt()
            val minutes = if (match.groupValues[2].startsWith("h", ignoreCase = true)) amount * 60 else amount
            val total = now.hour * 60 + now.minute + minutes
            time = LocalTime((total % 1440) / 60, total % 60)
            dateFromTimeWord = now.date.plus(total / 1440, DateTimeUnit.DAY)
            text = text.removeRange(match.range)
        }
    }
    if (time == null) {
        // bare "at 5": 1-7 reads as evening, 8-11 as morning
        RE_TIME_AT_BARE.find(text)?.let { match ->
            val h = match.groupValues[1].toInt()
            val hour = when {
                h == 0 -> 0
                h in 1..7 -> h + 12
                h in 8..12 -> if (h == 12) 12 else h
                h in 13..23 -> h
                else -> null
            }
            if (hour != null) {
                time = LocalTime(hour, 0)
                text = text.removeRange(match.range)
            }
        }
    }
    if (time == null) {
        RE_TONIGHT.find(text)?.let {
            time = LocalTime(20, 0)
            dateFromTimeWord = today
            text = text.removeRange(it.range)
        }
    }
    if (time == null) {
        for ((regex, value) in TIME_WORDS) {
            val match = regex.find(text) ?: continue
            time = value
            text = text.removeRange(match.range)
            break
        }
    }

    // Collect every date token first; more than one distinct resolved date = ambiguous.
    data class DateHit(val range: IntRange, val date: LocalDate?)
    val hits = mutableListOf<DateHit>()
    fun collect(regex: Regex, resolve: (MatchResult) -> LocalDate?) {
        for (m in regex.findAll(text)) {
            if (hits.none { it.range.intersects(m.range) }) hits += DateHit(m.range, resolve(m))
        }
    }
    collect(RE_TODAY) { today }
    collect(RE_TOMORROW) { today.plus(1, DateTimeUnit.DAY) }
    collect(RE_NEXT_WEEK) { nextOccurrence(today, firstDayOfWeek) }
    collect(RE_NEXT_MONTH) { today.plus(1, DateTimeUnit.MONTH) }
    collect(RE_END_OF_WEEK) { today.plus(7 - today.dayOfWeek.isoDayNumber, DateTimeUnit.DAY) }
    collect(RE_END_OF_MONTH) { kotlinx.datetime.YearMonth(today.year, today.month).lastDay }
    collect(RE_IN_N) { m ->
        val n = m.groupValues[1].toInt()
        when (m.groupValues[2].lowercase()) {
            "day" -> today.plus(n, DateTimeUnit.DAY)
            "week" -> today.plus(n * 7, DateTimeUnit.DAY)
            else -> today.plus(n, DateTimeUnit.MONTH)
        }
    }
    collect(RE_WEEKDAY) { m ->
        val base = nextOccurrence(today, weekdayFrom(m.groupValues[2]))
        if (m.groupValues[1].isNotEmpty()) base.plus(7, DateTimeUnit.DAY) else base
    }
    collect(RE_MONTH_DAY) { m -> resolveMonthDay(monthFrom(m.groupValues[1]), m.groupValues[2].toInt(), today) }
    collect(RE_DAY_MONTH) { m -> resolveMonthDay(monthFrom(m.groupValues[2]), m.groupValues[1].toInt(), today) }
    collect(RE_NUMERIC_MD) { m -> resolveMonthDay(m.groupValues[1].toInt(), m.groupValues[2].toInt(), today) }

    val resolved = hits.filter { it.date != null }
    val distinctDates = resolved.mapNotNull { it.date }.distinct()
    if (distinctDates.size > 1) {
        // Conflicting dates: keep the original text, let the human sort it out.
        return ParsedCapture(title = input, type = ItemType.INBOX, tags = tags)
    }
    var date: LocalDate? = distinctDates.firstOrNull()
    if (date != null) {
        for (hit in resolved.sortedByDescending { it.range.first }) {
            text = text.removeRange(hit.range)
        }
    }
    if (date == null) date = dateFromTimeWord
    if (rrule != null && date == null) {
        date = rruleWeekday?.let { nextOccurrence(today, it, orSame = true) } ?: today
    }

    // a clock time makes an event; a bare repeat ("every day") is a habit, so a task
    val type = when {
        time != null -> ItemType.EVENT
        else -> ItemType.TASK
    }

    val title = cleanTitle(text)
    if (title.isEmpty()) {
        // Signals with no content: not enough to act on.
        return ParsedCapture(title = input, type = ItemType.INBOX, tags = tags)
    }

    return ParsedCapture(
        title = title,
        type = type,
        date = date,
        time = time,
        durationMinutes = durationMinutes,
        priority = priority,
        tags = tags,
        rrule = rrule,
    )
}
