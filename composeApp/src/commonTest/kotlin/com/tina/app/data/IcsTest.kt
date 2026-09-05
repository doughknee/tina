package com.tina.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class IcsTest {
    private val tz = TimeZone.of("Australia/Sydney")
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDateTime(LocalDate(y, m, d), LocalTime(h, min)).toInstant(tz).toEpochMilliseconds()

    private fun item(vararg edits: Item.() -> Item) = edits.fold(
        Item(id = 7, uuid = "abc", title = "Dentist", type = ItemType.EVENT, createdAt = 0, updatedAt = 0),
    ) { acc, e -> acc.e() }

    @Test
    fun timedEventIsUtcWithAnHourDefault() {
        val ics = icsOf(listOf(item({ copy(startAt = at(2026, 9, 5, 15, 0)) })), tz, 0)
        assertTrue("BEGIN:VEVENT" in ics)
        assertTrue("UID:abc@peggy" in ics)
        assertTrue("DTSTART:20260905T050000Z" in ics)
        assertTrue("DTEND:20260905T060000Z" in ics)
        assertTrue("SUMMARY:Dentist" in ics)
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"))
    }

    @Test
    fun allDayEventIsDatesWithExclusiveEnd() {
        val ics = icsOf(listOf(item({ copy(startAt = at(2026, 9, 5, 0, 0), allDay = true) })), tz, 0)
        assertTrue("DTSTART;VALUE=DATE:20260905" in ics)
        assertTrue("DTEND;VALUE=DATE:20260906" in ics)
    }

    @Test
    fun datedTaskIsATodoAndTrashStaysHome() {
        val task = item({ copy(type = ItemType.TASK, dueDate = LocalDate(2026, 9, 8).toEpochDays().toInt(), priority = Priority.HIGH) })
        val binned = item({ copy(id = 8, uuid = "bin", startAt = at(2026, 9, 5, 9, 0), deletedAt = 1) })
        val ics = icsOf(listOf(task, binned), tz, 0)
        assertTrue("BEGIN:VTODO" in ics)
        assertTrue("DUE;VALUE=DATE:20260908" in ics)
        assertTrue("PRIORITY:1" in ics)
        assertTrue("STATUS:NEEDS-ACTION" in ics)
        assertFalse("bin@peggy" in ics)
    }

    @Test
    fun recurrenceTagsAndTextTravel() {
        val ics = icsOf(
            listOf(item({ copy(startAt = at(2026, 9, 5, 9, 0), recurrence = "FREQ=WEEKLY;BYDAY=MO", tags = listOf("kitchen"), body = "<p>Bring the, notes; ok</p>") })),
            tz, 0,
        )
        assertTrue("RRULE:FREQ=WEEKLY;BYDAY=MO" in ics)
        assertTrue("CATEGORIES:kitchen" in ics)
        assertTrue("DESCRIPTION:Bring the\\, notes\\; ok" in ics)
    }

    @Test
    fun longLinesFoldAt75OctetsAndNeverSplitAMultiByteCharacter() {
        val folded = fold("SUMMARY:" + "é".repeat(60))
        val lines = folded.split("\r\n")
        assertTrue(lines.size > 1)
        lines.forEach { assertTrue(it.encodeToByteArray().size <= 75, "line too long: ${it.length}") }
        lines.drop(1).forEach { assertTrue(it.startsWith(" ")) }
        assertEquals("SUMMARY:" + "é".repeat(60), folded.replace("\r\n ", ""))
        assertEquals("short", fold("short"))
    }

    @Test
    fun utcStamp() {
        assertEquals("19700101T000000Z", utc(0))
    }
}
