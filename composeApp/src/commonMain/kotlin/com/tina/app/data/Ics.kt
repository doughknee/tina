package com.tina.app.data

import com.tina.app.notes.htmlPreview
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * The calendar as an iCalendar file (RFC 5545): every event as a VEVENT, every dated task as
 * a VTODO. Timed events are written in UTC; all-day ones as dates, recovered in [tz], which is
 * the zone their midnight was written in. Recurrence is stored as an RRULE already, so it
 * goes out as it is. Trash stays home.
 */
fun icsOf(items: List<Item>, tz: TimeZone, now: Long): String {
    val out = StringBuilder()
    fun line(name: String, value: String) = out.append(fold("$name:$value")).append("\r\n")
    line("BEGIN", "VCALENDAR")
    line("VERSION", "2.0")
    line("PRODID", "-//Peggy//Peggy//EN")
    line("CALSCALE", "GREGORIAN")
    val stamp = utc(now)
    for (item in items) {
        if (item.deletedAt != null) continue
        when {
            item.type == ItemType.EVENT && item.startAt != null -> {
                line("BEGIN", "VEVENT")
                line("UID", uid(item))
                line("DTSTAMP", stamp)
                if (item.allDay) {
                    val start = Instant.fromEpochMilliseconds(item.startAt).toLocalDateTime(tz).date
                    val end = item.endAt?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(tz).date } ?: start
                    line("DTSTART;VALUE=DATE", date(start))
                    // DTEND is exclusive for dates: a one-day event ends the next morning
                    line("DTEND;VALUE=DATE", date(end.plusDays(1)))
                } else {
                    line("DTSTART", utc(item.startAt))
                    line("DTEND", utc(item.endAt ?: (item.startAt + 60 * 60 * 1000L)))
                }
                common(item, ::line)
                line("END", "VEVENT")
            }
            item.type == ItemType.TASK && item.dueDate != null -> {
                line("BEGIN", "VTODO")
                line("UID", uid(item))
                line("DTSTAMP", stamp)
                line("DUE;VALUE=DATE", date(item.dueLocalDate!!))
                line("STATUS", if (item.completed) "COMPLETED" else "NEEDS-ACTION")
                item.completedAt?.let { line("COMPLETED", utc(it)) }
                when (item.priority) {
                    Priority.HIGH -> line("PRIORITY", "1")
                    Priority.MEDIUM -> line("PRIORITY", "5")
                    Priority.LOW -> line("PRIORITY", "9")
                    Priority.NONE -> Unit
                }
                common(item, ::line)
                line("END", "VTODO")
            }
        }
    }
    line("END", "VCALENDAR")
    return out.toString()
}

private fun common(item: Item, line: (String, String) -> Unit) {
    line("SUMMARY", escape(item.title))
    item.body?.let { htmlPreview(it).trim() }?.takeIf { it.isNotEmpty() }?.let { line("DESCRIPTION", escape(it)) }
    if (item.tags.isNotEmpty()) line("CATEGORIES", item.tags.joinToString(",") { escape(it) })
    item.recurrence?.takeIf { it.isNotBlank() }?.let { line("RRULE", it.removePrefix("RRULE:")) }
    line("CREATED", utc(item.createdAt))
    line("LAST-MODIFIED", utc(item.updatedAt))
}

private fun uid(item: Item) = (item.uuid.ifBlank { "item-${item.id}" }) + "@peggy"

private fun LocalDate.plusDays(n: Int) = LocalDate.fromEpochDays(toEpochDays() + n)

private fun date(d: LocalDate): String =
    d.year.toString().padStart(4, '0') + d.monthNumber.toString().padStart(2, '0') + d.day.toString().padStart(2, '0')

/** 20260905T141500Z */
internal fun utc(millis: Long): String {
    val t = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC)
    return date(t.date) + "T" +
        t.hour.toString().padStart(2, '0') + t.minute.toString().padStart(2, '0') + t.second.toString().padStart(2, '0') + "Z"
}

/** Backslash, semicolon and comma carry meaning in a TEXT value; newlines become \n. */
internal fun escape(text: String): String =
    text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\n").replace("\n", "\\n")

/** Lines longer than 75 octets continue on the next line after a single space (RFC 5545 §3.1). */
internal fun fold(line: String): String {
    val bytes = line.encodeToByteArray()
    if (bytes.size <= 75) return line
    val sb = StringBuilder()
    var start = 0
    var first = true
    while (start < bytes.size) {
        val limit = if (first) 75 else 74
        var end = minOf(start + limit, bytes.size)
        // never split a multi-byte character: back up to a UTF-8 boundary
        while (end < bytes.size && (bytes[end].toInt() and 0xC0) == 0x80) end--
        if (!first) sb.append("\r\n ")
        sb.append(bytes.decodeToString(start, end))
        start = end
        first = false
    }
    return sb.toString()
}
