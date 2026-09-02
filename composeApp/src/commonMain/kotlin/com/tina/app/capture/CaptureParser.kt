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
private val RE_PRIORITY_WORD = Regex("""\b(urgent|asap)\b:?""", RegexOption.IGNORE_CASE)

private const val WEEKDAYS =
    "monday|tuesday|wednesday|thursday|friday|saturday|sunday|tues|thurs|thur|mon|tue|wed|thu|fri|sat|sun"
private const val MONTHS =
    "january|february|march|april|june|july|august|september|october|november|december|" +
        "sept|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec"
private const val DAY_WORDS = "this|the|tomorrow|today|monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun"

/** Small numbers people write as words; "a"/"an" count as one. */
private val NUMBER_WORDS = mapOf(
    "a" to 1, "an" to 1, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
    "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "fifteen" to 15, "twenty" to 20, "thirty" to 30,
    "forty" to 40, "forty-five" to 45, "sixty" to 60, "ninety" to 90,
)
private const val NUM = """(?:\d{1,4}|a|an|one|two|three|four|five|six|seven|eight|nine|ten|fifteen|twenty|thirty|forty|forty-five|sixty|ninety)"""
private fun numberOf(token: String): Int = token.toIntOrNull() ?: NUMBER_WORDS[token.lowercase()] ?: 1

// ---- recurrence
private val RE_EVERY_N_UNIT = Regex("""\bevery\s+(\d{1,3})\s+(days?|weeks?|months?|years?)\b""", RegexOption.IGNORE_CASE)
private val RE_EVERY_OTHER = Regex("""\bevery\s+other\s+(day|week|month|year)\b""", RegexOption.IGNORE_CASE)
private val RE_EVERY_WEEKDAY_WORD = Regex("""\bevery\s+weekday\b""", RegexOption.IGNORE_CASE)
private val RE_EVERY_WEEKEND = Regex("""\bevery\s+weekend\b""", RegexOption.IGNORE_CASE)
private val RE_EVERY_DAYLIST = Regex(
    """\bevery\s+((?:$WEEKDAYS)(?:\s*(?:,|and|&)\s*(?:$WEEKDAYS))*)\b""",
    RegexOption.IGNORE_CASE,
)
private val RE_EVERY_PART_OF_DAY = Regex("""\bevery\s+(morning|afternoon|evening|night)\b""", RegexOption.IGNORE_CASE)
private val RE_EVERY_ORDINAL = Regex("""\bevery\s+(\d{1,2})(?:st|nd|rd|th)\b""", RegexOption.IGNORE_CASE)
private val RE_EVERY_UNIT = Regex("""\bevery\s+(day|week|month|year)\b""", RegexOption.IGNORE_CASE)
private val RE_FREQ_WORD = Regex("""\b(daily|weekly|monthly|yearly|annually)\b""", RegexOption.IGNORE_CASE)
private val RE_WEEKDAY_TOKEN = Regex("""\b($WEEKDAYS)\b""", RegexOption.IGNORE_CASE)

// ---- duration
private val RE_DURATION_H = Regex(
    """\bfor\s+($NUM)\s*h(?:(?:ou)?rs?)?\b(?:\s*(\d{1,2})\s*m(?:in(?:ute)?s?)?\b)?""",
    RegexOption.IGNORE_CASE,
)
private val RE_DURATION_HALF = Regex("""\bfor\s+half\s+an\s+hour\b""", RegexOption.IGNORE_CASE)
private val RE_DURATION_M = Regex("""\bfor\s+($NUM)\s*m(?:in(?:ute)?s?)?\b""", RegexOption.IGNORE_CASE)

// ---- times
private val RE_TIME_RANGE = Regex(
    """(?:\bat\s+)?\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\s*(?:-|–|to|until)\s*(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b""",
    RegexOption.IGNORE_CASE,
)
private val RE_TIME_COLON = Regex("""(?:\bat\s+)?\b(\d{1,2}):(\d{2})\s*(am|pm)?\b""", RegexOption.IGNORE_CASE)
private val RE_TIME_AMPM = Regex("""(?:\bat\s+)?\b(\d{1,2})\s*(am|pm)\b""", RegexOption.IGNORE_CASE)
private val RE_TIME_AT_BARE = Regex("""\bat\s+(\d{1,2})\b(?!\s*:|\s*(?:am|pm))""", RegexOption.IGNORE_CASE)
private val RE_IN_REL_TIME =
    Regex("""\bin\s+(half\s+an|$NUM)\s*(min(?:ute)?s?|h(?:(?:ou)?rs?)?)\b""", RegexOption.IGNORE_CASE)
private val RE_TONIGHT = Regex("""\btonight\b""", RegexOption.IGNORE_CASE)
private val RE_END_OF_DAY = Regex("""\b(?:by\s+)?(?:end\s+of\s+(?:the\s+)?day|eod)\b""", RegexOption.IGNORE_CASE)
private val RE_FIRST_THING = Regex("""\bfirst\s+thing\b""", RegexOption.IGNORE_CASE)
private val TIME_WORDS = listOf(
    Regex("""\bnoon\b""", RegexOption.IGNORE_CASE) to LocalTime(12, 0),
    Regex("""\bmidnight\b""", RegexOption.IGNORE_CASE) to LocalTime(0, 0),
    // a part-of-day word is a time only after a day word or at the end of the phrase;
    // otherwise it is part of the title ("morning pages", "evening walk")
    Regex("""(?:(?<=\b(?:$DAY_WORDS)\s)morning\b|\bmorning\s*$)""", RegexOption.IGNORE_CASE) to LocalTime(9, 0),
    Regex("""(?:(?<=\b(?:$DAY_WORDS)\s)afternoon\b|\bafternoon\s*$)""", RegexOption.IGNORE_CASE) to LocalTime(14, 0),
    Regex("""(?:(?<=\b(?:$DAY_WORDS)\s)evening\b|\bevening\s*$)""", RegexOption.IGNORE_CASE) to LocalTime(19, 0),
    Regex("""(?:(?<=\b(?:$DAY_WORDS)\s)night\b|\bnight\s*$)""", RegexOption.IGNORE_CASE) to LocalTime(20, 0),
)

// ---- dates
private val RE_TODAY = Regex("""\btoday\b""", RegexOption.IGNORE_CASE)
private val RE_DAY_AFTER_TOMORROW = Regex("""\b(?:the\s+)?day\s+after\s+tomorrow\b""", RegexOption.IGNORE_CASE)
private val RE_TOMORROW = Regex("""\btomorrow\b|\btmrw\b""", RegexOption.IGNORE_CASE)
private val RE_NEXT_WEEK = Regex("""\bnext\s+week\b""", RegexOption.IGNORE_CASE)
private val RE_NEXT_MONTH = Regex("""\bnext\s+month\b""", RegexOption.IGNORE_CASE)
private val RE_WEEKEND = Regex("""\b(this|next)\s+weekend\b""", RegexOption.IGNORE_CASE)
private val RE_END_OF_WEEK = Regex("""\bend\s+of\s+(?:the\s+)?week\b""", RegexOption.IGNORE_CASE)
private val RE_END_OF_MONTH = Regex("""\bend\s+of\s+(?:the\s+)?month\b""", RegexOption.IGNORE_CASE)
private val RE_IN_N = Regex("""\bin\s+($NUM)\s+(day|week|month)s?\b""", RegexOption.IGNORE_CASE)
private val RE_WEEKDAY = Regex("""\b(next\s+|this\s+)?($WEEKDAYS)\b""", RegexOption.IGNORE_CASE)
private val RE_ISO = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")
private val RE_MONTH_DAY = Regex("""\b($MONTHS)\s+(\d{1,2})(?:st|nd|rd|th)?\b(?:,?\s+(\d{4})\b)?""", RegexOption.IGNORE_CASE)
private val RE_DAY_MONTH = Regex("""\b(\d{1,2})(?:st|nd|rd|th)?\s+($MONTHS)\b(?:,?\s+(\d{4})\b)?""", RegexOption.IGNORE_CASE)
private val RE_NUMERIC_MD = Regex("""\b(\d{1,2})/(\d{1,2})(?:/(\d{2}|\d{4}))?\b""")
private val RE_ORDINAL_DAY = Regex("""\b(?:on\s+)?the\s+(\d{1,2})(?:st|nd|rd|th)\b""", RegexOption.IGNORE_CASE)

// a dangling connector after a removed date or time is noise; a leading article is part of a title
private val TRAILING_CONNECTORS = setOf("on", "at", "in", "for", "by", "due", "the", "a", "an", "and", "this", "every", "of")
private val LEADING_CONNECTORS = setOf("on", "at", "in", "for", "by", "due", "and", "this", "every", "of")

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
private fun resolveMonthDay(month: Int, day: Int, today: LocalDate, year: Int? = null): LocalDate? {
    if (month !in 1..12) return null
    if (year != null) return runCatching { LocalDate(year, month, day) }.getOrNull()
    val thisYear = runCatching { LocalDate(today.year, month, day) }.getOrNull() ?: return null
    return if (thisYear < today) {
        runCatching { LocalDate(today.year + 1, month, day) }.getOrNull() ?: thisYear
    } else thisYear
}

/** "the 15th": this month if it has not passed, otherwise next month. */
private fun resolveDayOfMonth(day: Int, today: LocalDate): LocalDate? {
    if (day !in 1..31) return null
    val thisMonth = runCatching { LocalDate(today.year, today.month, day) }.getOrNull()
    if (thisMonth != null && thisMonth >= today) return thisMonth
    val next = today.plus(1, DateTimeUnit.MONTH)
    return runCatching { LocalDate(next.year, next.month, day) }.getOrNull()
}

private fun yearOf(token: String): Int? = token.toIntOrNull()?.let { if (it < 100) 2000 + it else it }

private fun IntRange.intersects(other: IntRange): Boolean = first <= other.last && other.first <= last

private fun cleanTitle(text: String): String {
    val collapsed = text.replace(Regex("""\s+"""), " ").trim().trim(',', ';', '-', ':', ' ')
    val words = collapsed.split(" ").toMutableList()
    while (words.size > 1 && words.last().lowercase() in TRAILING_CONNECTORS) words.removeAt(words.size - 1)
    while (words.size > 1 && words.first().lowercase() in LEADING_CONNECTORS) words.removeAt(0)
    return words.joinToString(" ").trim(',', ';', '-', ':', ' ')
}

private fun clockHour(h: Int, ampm: String): Int? = when {
    ampm.isEmpty() -> h.takeIf { it <= 23 }
    h !in 1..12 -> null
    ampm.equals("am", true) -> if (h == 12) 0 else h
    else -> if (h == 12) 12 else h + 12
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

    var text = RE_TAG.replace(input, " ")
    // "3 p.m." and "5.30pm" are the same clock as "3pm" and "5:30pm"
    text = text.replace(Regex("""\b([ap])\.m\.""", RegexOption.IGNORE_CASE), "$1m")
    text = text.replace(Regex("""\b(\d{1,2})\.(\d{2})\s*(am|pm)\b""", RegexOption.IGNORE_CASE), "$1:$2$3")

    var priority = Priority.NONE
    RE_PRIORITY.find(text)?.let {
        priority = if (it.groupValues[1].length >= 2) Priority.HIGH else Priority.MEDIUM
        text = text.removeRange(it.range)
    }
    RE_PRIORITY_WORD.find(text)?.let {
        priority = Priority.HIGH
        text = text.removeRange(it.range)
    }

    var rrule: String? = null
    var rruleWeekday: DayOfWeek? = null
    var rruleTime: LocalTime? = null
    var rruleDay: LocalDate? = null
    fun consume(m: MatchResult) { text = text.removeRange(m.range) }
    run {
        RE_EVERY_N_UNIT.find(text)?.let { m ->
            val n = m.groupValues[1].toInt()
            val freq = when (m.groupValues[2].lowercase().trimEnd('s')) {
                "day" -> "DAILY"; "week" -> "WEEKLY"; "month" -> "MONTHLY"; else -> "YEARLY"
            }
            rrule = if (n > 1) "FREQ=$freq;INTERVAL=$n" else "FREQ=$freq"
            consume(m); return@run
        }
        RE_EVERY_OTHER.find(text)?.let { m ->
            rrule = when (m.groupValues[1].lowercase()) {
                "day" -> "FREQ=DAILY;INTERVAL=2"
                "week" -> "FREQ=WEEKLY;INTERVAL=2"
                "month" -> "FREQ=MONTHLY;INTERVAL=2"
                else -> "FREQ=YEARLY;INTERVAL=2"
            }
            consume(m); return@run
        }
        RE_EVERY_WEEKDAY_WORD.find(text)?.let { m ->
            rrule = "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
            consume(m); return@run
        }
        RE_EVERY_WEEKEND.find(text)?.let { m ->
            rrule = "FREQ=WEEKLY;BYDAY=SA,SU"
            rruleWeekday = DayOfWeek.SATURDAY
            consume(m); return@run
        }
        RE_EVERY_DAYLIST.find(text)?.let { m ->
            val days = RE_WEEKDAY_TOKEN.findAll(m.groupValues[1]).map { weekdayFrom(it.value) }.distinct()
                .sortedBy { it.isoDayNumber }.toList()
            rruleWeekday = days.minByOrNull { d -> nextOccurrence(today, d, orSame = true) }
            rrule = "FREQ=WEEKLY;BYDAY=" + days.joinToString(",") { byDayCode(it) }
            consume(m); return@run
        }
        RE_EVERY_PART_OF_DAY.find(text)?.let { m ->
            rrule = "FREQ=DAILY"
            rruleTime = when (m.groupValues[1].lowercase()) {
                "morning" -> LocalTime(9, 0); "afternoon" -> LocalTime(14, 0); "evening" -> LocalTime(19, 0); else -> LocalTime(20, 0)
            }
            consume(m); return@run
        }
        RE_EVERY_ORDINAL.find(text)?.let { m ->
            rrule = "FREQ=MONTHLY"
            rruleDay = resolveDayOfMonth(m.groupValues[1].toInt(), today)
            consume(m); return@run
        }
        RE_EVERY_UNIT.find(text)?.let { m ->
            rrule = when (m.groupValues[1].lowercase()) {
                "day" -> "FREQ=DAILY"; "week" -> "FREQ=WEEKLY"; "month" -> "FREQ=MONTHLY"; else -> "FREQ=YEARLY"
            }
            consume(m); return@run
        }
        // "daily", "weekly on friday": a bare frequency word, but not inside a name ("weekly report", "the daily show")
        RE_FREQ_WORD.find(text)?.let { m ->
            val after = text.substring(m.range.last + 1)
            val before = text.substring(0, m.range.first).trimEnd()
            val startsAName = before.endsWith(" the", ignoreCase = true) || before.equals("the", ignoreCase = true)
            val isFreq = !startsAName && (after.isBlank() || after.trimStart().startsWith("on ", ignoreCase = true) ||
                after.trimStart().startsWith("at ", ignoreCase = true) || RE_WEEKDAY_TOKEN.containsMatchIn(after))
            if (isFreq) {
                rrule = when (m.groupValues[1].lowercase()) {
                    "daily" -> "FREQ=DAILY"; "weekly" -> "FREQ=WEEKLY"; "monthly" -> "FREQ=MONTHLY"; else -> "FREQ=YEARLY"
                }
                consume(m)
                // "weekly on friday" pins the day
                if (rrule == "FREQ=WEEKLY") {
                    Regex("""\b(?:on\s+)?($WEEKDAYS)\b""", RegexOption.IGNORE_CASE).find(text)?.let { d ->
                        rruleWeekday = weekdayFrom(d.groupValues[1])
                        rrule = "FREQ=WEEKLY;BYDAY=${byDayCode(rruleWeekday!!)}"
                        consume(d)
                    }
                }
            }
        }
    }

    var durationMinutes: Int? = null
    RE_DURATION_HALF.find(text)?.let { durationMinutes = 30; consume(it) }
    if (durationMinutes == null) {
        val durH = RE_DURATION_H.find(text)
        if (durH != null) {
            val hours = numberOf(durH.groupValues[1])
            val minutes = durH.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: 0
            durationMinutes = hours * 60 + minutes
            consume(durH)
        } else {
            RE_DURATION_M.find(text)?.let {
                durationMinutes = numberOf(it.groupValues[1])
                consume(it)
            }
        }
    }

    var time: LocalTime? = null
    var dateFromTimeWord: LocalDate? = null

    // "2-4pm": a range is a start plus a duration, and one clock, so it is never ambiguous
    RE_TIME_RANGE.find(text)?.let { m ->
        val endAmPm = m.groupValues[6]
        val startAmPm = m.groupValues[3].ifEmpty { endAmPm }
        var startHour = clockHour(m.groupValues[1].toInt(), startAmPm)
        val endHour = clockHour(m.groupValues[4].toInt(), endAmPm)
        val startMin = m.groupValues[2].toIntOrNull() ?: 0
        val endMin = m.groupValues[5].toIntOrNull() ?: 0
        if (startHour != null && endHour != null && startMin <= 59 && endMin <= 59) {
            // "10-6pm" without a start marker: 10 must be am if pm would put it after the end
            if (m.groupValues[3].isEmpty() && startHour * 60 + startMin > endHour * 60 + endMin) startHour -= 12
            if (startHour in 0..23) {
                time = LocalTime(startHour, startMin)
                val span = (endHour * 60 + endMin) - (startHour * 60 + startMin)
                durationMinutes = durationMinutes ?: if (span > 0) span else span + 24 * 60
                consume(m)
            }
        }
    }

    // Collect explicit clock times; more than one distinct time is ambiguous.
    data class TimeHit(val range: IntRange, val time: LocalTime)
    val timeHits = mutableListOf<TimeHit>()
    if (time == null) {
        for (match in RE_TIME_COLON.findAll(text)) {
            val hour = clockHour(match.groupValues[1].toInt(), match.groupValues[3])
            val m = match.groupValues[2].toInt()
            if (hour != null && m <= 59) timeHits += TimeHit(match.range, LocalTime(hour, m))
        }
        for (match in RE_TIME_AMPM.findAll(text)) {
            if (timeHits.any { it.range.intersects(match.range) }) continue
            val hour = clockHour(match.groupValues[1].toInt(), match.groupValues[2]) ?: continue
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
    }

    if (time == null) {
        // "in 30 min" / "in 2 hours" / "in an hour": relative to now, pins both date and time
        RE_IN_REL_TIME.find(text)?.let { match ->
            val amountToken = match.groupValues[1]
            val isHours = match.groupValues[2].startsWith("h", ignoreCase = true)
            val minutes = when {
                amountToken.startsWith("half", ignoreCase = true) -> if (isHours) 30 else 1
                isHours -> numberOf(amountToken) * 60
                else -> numberOf(amountToken)
            }
            val total = now.hour * 60 + now.minute + minutes
            time = LocalTime((total % 1440) / 60, total % 60)
            dateFromTimeWord = now.date.plus(total / 1440, DateTimeUnit.DAY)
            consume(match)
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
                consume(match)
            }
        }
    }
    if (time == null) {
        RE_TONIGHT.find(text)?.let {
            time = LocalTime(20, 0)
            dateFromTimeWord = today
            consume(it)
        }
    }
    if (time == null) {
        RE_END_OF_DAY.find(text)?.let {
            time = LocalTime(17, 0)
            dateFromTimeWord = today
            consume(it)
        }
    }
    if (time == null) {
        RE_FIRST_THING.find(text)?.let {
            time = LocalTime(8, 0)
            consume(it)
        }
    }
    if (time == null) {
        for ((regex, value) in TIME_WORDS) {
            val match = regex.find(text) ?: continue
            time = value
            consume(match)
            break
        }
    }
    if (time == null) time = rruleTime

    // Collect every date token first; more than one distinct resolved date = ambiguous.
    data class DateHit(val range: IntRange, val date: LocalDate?)
    val hits = mutableListOf<DateHit>()
    fun collect(regex: Regex, resolve: (MatchResult) -> LocalDate?) {
        for (m in regex.findAll(text)) {
            if (hits.none { it.range.intersects(m.range) }) hits += DateHit(m.range, resolve(m))
        }
    }
    collect(RE_TODAY) { today }
    collect(RE_DAY_AFTER_TOMORROW) { today.plus(2, DateTimeUnit.DAY) }
    collect(RE_TOMORROW) { today.plus(1, DateTimeUnit.DAY) }
    collect(RE_NEXT_WEEK) { nextOccurrence(today, firstDayOfWeek) }
    collect(RE_NEXT_MONTH) { today.plus(1, DateTimeUnit.MONTH) }
    collect(RE_WEEKEND) { m ->
        val saturday = nextOccurrence(today, DayOfWeek.SATURDAY, orSame = true)
        if (m.groupValues[1].equals("next", true)) saturday.plus(7, DateTimeUnit.DAY) else saturday
    }
    collect(RE_END_OF_WEEK) { today.plus(7 - today.dayOfWeek.isoDayNumber, DateTimeUnit.DAY) }
    collect(RE_END_OF_MONTH) { kotlinx.datetime.YearMonth(today.year, today.month).lastDay }
    collect(RE_IN_N) { m ->
        val n = numberOf(m.groupValues[1])
        when (m.groupValues[2].lowercase()) {
            "day" -> today.plus(n, DateTimeUnit.DAY)
            "week" -> today.plus(n * 7, DateTimeUnit.DAY)
            else -> today.plus(n, DateTimeUnit.MONTH)
        }
    }
    collect(RE_ISO) { m ->
        runCatching { LocalDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }.getOrNull()
    }
    collect(RE_MONTH_DAY) { m -> resolveMonthDay(monthFrom(m.groupValues[1]), m.groupValues[2].toInt(), today, yearOf(m.groupValues[3])) }
    collect(RE_DAY_MONTH) { m -> resolveMonthDay(monthFrom(m.groupValues[2]), m.groupValues[1].toInt(), today, yearOf(m.groupValues[3])) }
    collect(RE_NUMERIC_MD) { m -> resolveMonthDay(m.groupValues[1].toInt(), m.groupValues[2].toInt(), today, yearOf(m.groupValues[3])) }
    collect(RE_WEEKDAY) { m ->
        val base = nextOccurrence(today, weekdayFrom(m.groupValues[2]))
        if (m.groupValues[1].startsWith("next", ignoreCase = true)) base.plus(7, DateTimeUnit.DAY) else base
    }
    collect(RE_ORDINAL_DAY) { m -> resolveDayOfMonth(m.groupValues[1].toInt(), today) }

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
        date = rruleDay ?: rruleWeekday?.let { nextOccurrence(today, it, orSame = true) } ?: today
    }

    // a clock time makes an event; a bare repeat ("every day") is a habit, so a task
    val type = when {
        time != null -> ItemType.EVENT
        else -> ItemType.TASK
    }

    val title = cleanTitle(text)
    if (title.isEmpty()) {
        // Signals with no content: not enough to act on.
        return ParsedCapture(title = input, type = ItemType.INBOX, tags = tags, date = date, time = time, rrule = rrule)
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
