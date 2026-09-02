package com.tina.app.capture

import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/**
 * The parser's spec: real phrasings people type, one line each. When a phrasing fails here,
 * the parser is wrong, not the phrasing. Wednesday 2 Sep 2026, 10:00.
 */
class CaptureCorpusTest {
    private val now = LocalDateTime(2026, 9, 2, 10, 0)

    private data class Want(
        val title: String? = null,
        val type: ItemType? = null,
        val date: LocalDate? = null,
        val time: LocalTime? = null,
        val rrule: String? = null,
        val duration: Int? = null,
        val priority: Priority? = null,
        val noDate: Boolean = false,
        val noTime: Boolean = false,
    )

    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate(y, m, day)
    private fun t(h: Int, m: Int = 0) = LocalTime(h, m)

    private val corpus: List<Pair<String, Want>> = listOf(
        // ---- intervals and day lists
        "water the plants every 3 days" to Want(title = "water the plants", rrule = "FREQ=DAILY;INTERVAL=3", type = ItemType.TASK, date = d(9, 2)),
        "pay the sitter every 2 weeks" to Want(title = "pay the sitter", rrule = "FREQ=WEEKLY;INTERVAL=2"),
        "dentist every 6 months" to Want(title = "dentist", rrule = "FREQ=MONTHLY;INTERVAL=6"),
        "gym every mon and wed 7am" to Want(title = "gym", rrule = "FREQ=WEEKLY;BYDAY=MO,WE", time = t(7), type = ItemType.EVENT, date = d(9, 2)),
        "standup every monday, wednesday and friday" to Want(title = "standup", rrule = "FREQ=WEEKLY;BYDAY=MO,WE,FR", date = d(9, 2)),
        "long run every weekend" to Want(title = "long run", rrule = "FREQ=WEEKLY;BYDAY=SA,SU", date = d(9, 5)),
        "vitamins daily" to Want(title = "vitamins", rrule = "FREQ=DAILY"),
        "review budget weekly" to Want(title = "review budget", rrule = "FREQ=WEEKLY"),
        "invoice clients monthly" to Want(title = "invoice clients", rrule = "FREQ=MONTHLY"),
        "renew domain yearly" to Want(title = "renew domain", rrule = "FREQ=YEARLY"),
        "renew passport annually" to Want(title = "renew passport", rrule = "FREQ=YEARLY"),
        "journal every morning" to Want(title = "journal", rrule = "FREQ=DAILY", time = t(9), type = ItemType.EVENT),
        "stretch every evening" to Want(title = "stretch", rrule = "FREQ=DAILY", time = t(19)),
        "meds every night" to Want(title = "meds", rrule = "FREQ=DAILY", time = t(20)),
        "standup every weekday morning" to Want(title = "standup", rrule = "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", time = t(9)),
        "rent on the 1st every month" to Want(title = "rent", rrule = "FREQ=MONTHLY", date = d(10, 1)),
        "report every 15th" to Want(title = "report", rrule = "FREQ=MONTHLY", date = d(9, 15)),
        "weekly on friday" to Want(rrule = "FREQ=WEEKLY;BYDAY=FR", date = d(9, 4), type = ItemType.INBOX),

        // ---- relative times in words
        "tea in an hour" to Want(title = "tea", time = t(11), date = d(9, 2), type = ItemType.EVENT),
        "call back in half an hour" to Want(title = "call back", time = t(10, 30)),
        "check oven in 90 minutes" to Want(title = "check oven", time = t(11, 30)),
        "leave in two hours" to Want(title = "leave", time = t(12)),
        "follow up in a week" to Want(title = "follow up", date = d(9, 9), noTime = true),
        "renew in a month" to Want(title = "renew", date = d(10, 2)),
        "check in with mia in three days" to Want(title = "check in with mia", date = d(9, 5)),

        // ---- clock time variants
        "call sam 3 p.m." to Want(title = "call sam", time = t(15)),
        "call sam 5.30pm" to Want(title = "call sam", time = t(17, 30)),
        "call sam at 3 PM" to Want(title = "call sam", time = t(15)),
        "workshop 2-4pm" to Want(title = "workshop", time = t(14), duration = 120),
        "workshop 2 to 4pm" to Want(title = "workshop", time = t(14), duration = 120),
        "workshop 9:30-11am" to Want(title = "workshop", time = t(9, 30), duration = 90),
        "overnight shift 10pm-6am" to Want(title = "overnight shift", time = t(22), duration = 480),
        "send invoice by end of day" to Want(title = "send invoice", time = t(17), date = d(9, 2)),
        "send invoice eod" to Want(title = "send invoice", time = t(17), date = d(9, 2)),
        "lunch at 12" to Want(title = "lunch", time = t(12)),
        "movie tomorrow night" to Want(title = "movie", date = d(9, 3), time = t(20)),
        "haircut sat at noon" to Want(title = "haircut", date = d(9, 5), time = t(12)),
        "dentist for an hour 2pm" to Want(title = "dentist", time = t(14), duration = 60),
        "sync for half an hour 3pm" to Want(title = "sync", time = t(15), duration = 30),
        "workshop 1pm for 90 minutes" to Want(title = "workshop", time = t(13), duration = 90),

        // ---- date variants
        "review this tuesday" to Want(title = "review", date = d(9, 8)),
        "review this friday" to Want(title = "review", date = d(9, 4)),
        "taxes on the 15th" to Want(title = "taxes", date = d(9, 15)),
        "rent the 1st" to Want(title = "rent", date = d(10, 1)),
        "dentist 9/15/2026" to Want(title = "dentist", date = d(9, 15)),
        "dentist 9/15/26" to Want(title = "dentist", date = d(9, 15)),
        "dentist 2026-09-15" to Want(title = "dentist", date = d(9, 15)),
        "dentist september 15th, 2027" to Want(title = "dentist", date = d(9, 15, 2027)),
        "hike this weekend" to Want(title = "hike", date = d(9, 5)),
        "hike next weekend" to Want(title = "hike", date = d(9, 12)),
        "pick up parcel day after tomorrow" to Want(title = "pick up parcel", date = d(9, 4)),
        "call the bank first thing tomorrow" to Want(title = "call the bank", date = d(9, 3), time = t(8)),
        "gym tomorrow at 6" to Want(title = "gym", date = d(9, 3), time = t(18)),
        "return library books by friday" to Want(title = "return library books", date = d(9, 4), type = ItemType.TASK),
        "read chapter 4 tonight" to Want(title = "read chapter 4", date = d(9, 2), time = t(20)),

        // ---- priority words
        "fix the login bug asap" to Want(title = "fix the login bug", priority = Priority.HIGH),
        "urgent: call the landlord" to Want(title = "call the landlord", priority = Priority.HIGH),

        // ---- things that must stay untouched
        "chapter 4 notes" to Want(title = "chapter 4 notes", noDate = true, noTime = true),
        "buy 2 tickets" to Want(title = "buy 2 tickets", noDate = true, noTime = true),
        "call unit 5 about the leak" to Want(title = "call unit 5 about the leak", noDate = true, noTime = true),
        "morning pages" to Want(title = "morning pages", noTime = true),
        "the daily show tickets" to Want(title = "the daily show tickets", rrule = null, noDate = true),
        "weekly report draft" to Want(title = "weekly report draft", rrule = null),
        // a leading weekday reads as a date; the title loses it, and that is the accepted trade
        "sunday roast recipe" to Want(title = "roast recipe", date = d(9, 6)),
        "wash car" to Want(title = "wash car", noDate = true, noTime = true, type = ItemType.TASK),
    )

    @Test fun corpus() {
        val failures = mutableListOf<String>()
        for ((input, want) in corpus) {
            val p = parseCapture(input, now)
            fun check(label: String, expected: Any?, actual: Any?) {
                if (expected != actual) failures += "\"$input\": $label expected $expected, got $actual"
            }
            want.title?.let { check("title", it, p.title) }
            want.type?.let { check("type", it, p.type) }
            want.date?.let { check("date", it, p.date) }
            want.time?.let { check("time", it, p.time) }
            if (want.rrule != null || input.contains("weekly report") || input.contains("daily show")) check("rrule", want.rrule, p.rrule)
            want.duration?.let { check("duration", it, p.durationMinutes) }
            want.priority?.let { check("priority", it, p.priority) }
            if (want.noDate) check("date", null, p.date)
            if (want.noTime) check("time", null, p.time)
        }
        assertEquals(emptyList<String>(), failures, "corpus failures:\n" + failures.joinToString("\n"))
    }
}
