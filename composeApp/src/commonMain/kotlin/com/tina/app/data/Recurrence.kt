package com.tina.app.data

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Supported RFC 5545 RRULE subset: FREQ (DAILY/WEEKLY/MONTHLY/YEARLY),
 * INTERVAL, BYDAY (weekly only), COUNT, UNTIL. Unknown parts are ignored.
 */
data class RecurrenceRule(
    val freq: Freq,
    val interval: Int = 1,
    val byDay: Set<DayOfWeek> = emptySet(),
    val count: Int? = null,
    val until: LocalDate? = null,
) {
    enum class Freq { DAILY, WEEKLY, MONTHLY, YEARLY }
}

private val BY_DAY_CODES = mapOf(
    "MO" to DayOfWeek.MONDAY, "TU" to DayOfWeek.TUESDAY, "WE" to DayOfWeek.WEDNESDAY,
    "TH" to DayOfWeek.THURSDAY, "FR" to DayOfWeek.FRIDAY, "SA" to DayOfWeek.SATURDAY,
    "SU" to DayOfWeek.SUNDAY,
)

fun byDayCode(day: DayOfWeek): String = BY_DAY_CODES.entries.first { it.value == day }.key

fun parseRrule(value: String): RecurrenceRule? {
    val body = value.removePrefix("RRULE:").trim()
    if (body.isEmpty()) return null
    var freq: RecurrenceRule.Freq? = null
    var interval = 1
    var count: Int? = null
    var until: LocalDate? = null
    var byDay = emptySet<DayOfWeek>()
    for (part in body.split(";")) {
        val kv = part.split("=", limit = 2)
        if (kv.size != 2) return null
        val v = kv[1]
        when (kv[0].uppercase()) {
            "FREQ" -> freq = RecurrenceRule.Freq.entries.firstOrNull { it.name == v.uppercase() } ?: return null
            "INTERVAL" -> interval = (v.toIntOrNull() ?: 1).coerceAtLeast(1)
            "COUNT" -> count = v.toIntOrNull()
            "UNTIL" -> until = parseUntilDate(v)
            "BYDAY" -> byDay = v.split(",").mapNotNull { BY_DAY_CODES[it.trim().uppercase()] }.toSet()
            // anything else: ignored subset
        }
    }
    return freq?.let { RecurrenceRule(it, interval, byDay, count, until) }
}

/** Accepts YYYYMMDD or YYYYMMDDTHHMMSS(Z); only the date part is used. */
private fun parseUntilDate(value: String): LocalDate? {
    val digits = value.take(8)
    if (digits.length != 8 || digits.any { !it.isDigit() }) return null
    return runCatching {
        LocalDate(digits.take(4).toInt(), digits.substring(4, 6).toInt(), digits.substring(6, 8).toInt())
    }.getOrNull()
}

// ponytail: hard cap ~10y of daily occurrences; raise if someone plans further ahead
private const val MAX_OCCURRENCES = 3700
private const val MAX_SCAN_STEPS = 40_000

/**
 * Occurrence start times (epoch millis) of an event within [rangeStartMillis, rangeEndMillis).
 * A null/unparseable rule yields just the event's own start if it is in range.
 */
fun expandOccurrences(
    startMillis: Long,
    rrule: String?,
    rangeStartMillis: Long,
    rangeEndMillis: Long,
    tz: TimeZone,
): List<Long> {
    val rule = rrule?.let { parseRrule(it) }
        ?: return if (startMillis in rangeStartMillis until rangeEndMillis) listOf(startMillis) else emptyList()

    val startLocal = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(tz)
    val startDate = startLocal.date
    val timeOfDay = startLocal.time

    val candidates: Sequence<LocalDate> = when (rule.freq) {
        RecurrenceRule.Freq.DAILY ->
            generateSequence(startDate) { it.plus(rule.interval, DateTimeUnit.DAY) }

        RecurrenceRule.Freq.WEEKLY ->
            if (rule.byDay.isEmpty()) {
                generateSequence(startDate) { it.plus(rule.interval * 7, DateTimeUnit.DAY) }
            } else sequence {
                // ponytail: day-by-day scan; fine at personal scale
                val anchorWeekStart = startDate.minus(startDate.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
                var d = startDate
                var steps = 0
                while (steps++ < MAX_SCAN_STEPS) {
                    val weekStart = d.minus(d.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
                    val weeks = anchorWeekStart.daysUntil(weekStart) / 7
                    if (d.dayOfWeek in rule.byDay && weeks % rule.interval == 0) yield(d)
                    d = d.plus(1, DateTimeUnit.DAY)
                }
            }

        RecurrenceRule.Freq.MONTHLY -> sequence {
            // same day-of-month; months without it (e.g. the 31st) are skipped, not clamped
            val dayOfMonth = startDate.day
            val firstOfStartMonth = LocalDate(startDate.year, startDate.month, 1)
            var i = 0
            while (i < MAX_SCAN_STEPS) {
                val anchor = firstOfStartMonth.plus(i * rule.interval, DateTimeUnit.MONTH)
                runCatching { LocalDate(anchor.year, anchor.month, dayOfMonth) }.getOrNull()?.let { yield(it) }
                i++
            }
        }

        RecurrenceRule.Freq.YEARLY -> sequence {
            // Feb 29 only recurs on leap years
            var i = 0
            while (i < MAX_SCAN_STEPS) {
                runCatching {
                    LocalDate(startDate.year + i * rule.interval, startDate.month, startDate.day)
                }.getOrNull()?.let { yield(it) }
                i++
            }
        }
    }

    val out = mutableListOf<Long>()
    var produced = 0
    for (date in candidates) {
        if (rule.until != null && date > rule.until) break
        produced++
        if (produced > MAX_OCCURRENCES) break
        if (rule.count != null && produced > rule.count) break
        val ms = LocalDateTime(date, timeOfDay).toInstant(tz).toEpochMilliseconds()
        if (ms >= rangeEndMillis) break
        if (ms >= rangeStartMillis) out += ms
    }
    return out
}
