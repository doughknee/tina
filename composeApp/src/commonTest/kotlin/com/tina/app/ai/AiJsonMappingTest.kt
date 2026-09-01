package com.tina.app.ai

import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class AiJsonMappingTest {
    @Test fun happyPath() {
        val json = """
            {"title":"lunch with sam","type":"EVENT","date":"2026-09-03","time":"12:00",
             "durationMinutes":90,"priority":"HIGH","tags":["Work"],"rrule":null,"body":null}
        """.trimIndent()
        val p = aiJsonToParsedCapture(json, "fallback")!!
        assertEquals("lunch with sam", p.title)
        assertEquals(ItemType.EVENT, p.type)
        assertEquals(LocalDate(2026, 9, 3), p.date)
        assertEquals(LocalTime(12, 0), p.time)
        assertEquals(90, p.durationMinutes)
        assertEquals(Priority.HIGH, p.priority)
        assertEquals(listOf("work"), p.tags)
    }

    @Test fun toleratesCodeFencesAndProse() {
        val text = "Here you go:\n```json\n{\"title\":\"x\",\"type\":\"TASK\"}\n```"
        assertEquals(ItemType.TASK, aiJsonToParsedCapture(text, "x")?.type)
    }

    @Test fun invalidDateAndTimeDropSilently() {
        val p = aiJsonToParsedCapture("""{"title":"x","type":"TASK","date":"soon","time":"noonish"}""", "x")!!
        assertNull(p.date)
        assertNull(p.time)
    }

    @Test fun unknownTypeIsRejected() {
        assertNull(aiJsonToParsedCapture("""{"title":"x","type":"REMINDER"}""", "x"))
    }

    @Test fun garbageIsRejected() {
        assertNull(aiJsonToParsedCapture("I could not parse that, sorry!", "x"))
    }

    @Test fun blankTitleFallsBack() {
        assertEquals("raw text", aiJsonToParsedCapture("""{"title":"","type":"TASK"}""", "raw text")?.title)
    }

    // --- merge policy: deterministic parser wins where it found something ---

    @Test fun localDateBeatsAiDate() {
        val local = com.tina.app.capture.ParsedCapture("dinner", ItemType.TASK, date = LocalDate(2026, 9, 11))
        val ai = com.tina.app.capture.ParsedCapture(
            "dinner with mom", ItemType.EVENT, date = LocalDate(2026, 9, 4), time = LocalTime(19, 0),
        )
        val merged = mergeParses(local, ai)
        assertEquals(LocalDate(2026, 9, 11), merged.date)
        assertEquals(LocalTime(19, 0), merged.time)
        assertEquals("dinner with mom", merged.title)
        assertEquals(ItemType.EVENT, merged.type)
    }

    @Test fun aiFillsWhatLocalMissed() {
        val local = com.tina.app.capture.ParsedCapture("thing", ItemType.TASK)
        val ai = com.tina.app.capture.ParsedCapture(
            "thing", ItemType.EVENT, date = LocalDate(2026, 9, 5), time = LocalTime(10, 0),
        )
        val merged = mergeParses(local, ai)
        assertEquals(LocalDate(2026, 9, 5), merged.date)
        assertEquals(LocalTime(10, 0), merged.time)
        assertEquals(ItemType.EVENT, merged.type)
    }

    @Test fun aiNoteStaysNote() {
        val local = com.tina.app.capture.ParsedCapture("x", ItemType.TASK, date = LocalDate(2026, 9, 5))
        val ai = com.tina.app.capture.ParsedCapture("x", ItemType.NOTE, body = "long text")
        assertEquals(ItemType.NOTE, mergeParses(local, ai).type)
    }

    @Test fun localTagsAndPriorityWin() {
        val local = com.tina.app.capture.ParsedCapture(
            "x", ItemType.TASK, priority = Priority.HIGH, tags = listOf("work"),
        )
        val ai = com.tina.app.capture.ParsedCapture(
            "x", ItemType.TASK, priority = Priority.LOW, tags = listOf("invented"),
        )
        val merged = mergeParses(local, ai)
        assertEquals(Priority.HIGH, merged.priority)
        assertEquals(listOf("work"), merged.tags)
    }
}
