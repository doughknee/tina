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
}
