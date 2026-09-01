package com.tina.app.ai

import com.tina.app.data.Item
import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val TZ = TimeZone.of("America/Chicago")

private fun task(title: String = "call the bank", date: LocalDate? = null) = Item(
    title = title,
    type = ItemType.TASK,
    createdAt = 1L,
    updatedAt = 1L,
    dueDate = date?.toEpochDays()?.toInt(),
)

class ImprovePatchTest {
    @Test fun parsesChangesAndDropsNoOps() {
        val item = task()
        val patch = parseImprovePatch(
            """
            Sure! {"title":"call the bank","type":"EVENT","time":"09:00",
            "rationale":"banks open at 9","questions":[{"id":"q1","question":"Which bank?","options":["Chase","Local"]}]}
            """.trimIndent(),
            item,
        )!!
        assertNull(patch.title) // unchanged title is not a suggestion
        assertEquals(ItemType.EVENT, patch.type)
        assertEquals(LocalTime(9, 0), patch.time)
        assertEquals(1, patch.questions.size)
        assertEquals(listOf("Chase", "Local"), patch.questions[0].options)
    }

    @Test fun garbageIsNull() {
        assertNull(parseImprovePatch("I could not help with that.", task()))
    }

    @Test fun taskBecomesEventWithSchedule() {
        val item = task(date = LocalDate(2026, 9, 7))
        val patch = ImprovePatch(type = ItemType.EVENT, time = LocalTime(9, 0), durationMinutes = 30)
        val out = applyImprovePatch(
            item,
            patch,
            setOf(ImproveField.TYPE, ImproveField.TIME, ImproveField.DURATION),
            TZ,
        )
        assertEquals(ItemType.EVENT, out.type)
        val expectedStart = LocalDateTime(LocalDate(2026, 9, 7), LocalTime(9, 0))
            .toInstant(TZ).toEpochMilliseconds()
        assertEquals(expectedStart, out.startAt)
        assertEquals(expectedStart + 30 * 60_000L, out.endAt)
        assertNull(out.dueDate)
        assertNull(out.dueTime)
    }

    @Test fun eventBecomesTaskKeepingDate() {
        val start = LocalDateTime(LocalDate(2026, 9, 4), LocalTime(19, 30))
            .toInstant(TZ).toEpochMilliseconds()
        val event = Item(
            title = "dinner", type = ItemType.EVENT, createdAt = 1L, updatedAt = 1L,
            startAt = start, endAt = start + 3_600_000L,
        )
        val out = applyImprovePatch(event, ImprovePatch(type = ItemType.TASK), setOf(ImproveField.TYPE), TZ)
        assertEquals(ItemType.TASK, out.type)
        assertEquals(LocalDate(2026, 9, 4).toEpochDays().toInt(), out.dueDate)
        assertEquals(19 * 60 + 30, out.dueTime)
        assertNull(out.startAt)
    }

    @Test fun disabledKeysAreIgnored() {
        val item = task()
        val patch = ImprovePatch(title = "Call the bank", priority = Priority.HIGH)
        val out = applyImprovePatch(item, patch, setOf(ImproveField.PRIORITY), TZ)
        assertEquals("call the bank", out.title)
        assertEquals(Priority.HIGH, out.priority)
    }

    @Test fun emptyPatchDetectsQuestionsSeparately() {
        val patch = ImprovePatch(questions = listOf(ImproveQuestion("q1", "Which bank?")))
        assertTrue(patch.isEmpty)
        assertTrue(patch.questions.isNotEmpty())
    }
}
