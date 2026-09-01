package com.tina.app.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AskActionsTest {
    @Test fun extractsTrailingBlockAndStripsIt() {
        val reply = """
            Done — marked "Read 20 pages" complete and moved the dentist call to Friday.
            {"actions":[{"op":"complete","id":4},{"op":"reschedule","id":5,"date":"2026-09-04","time":"15:00"}]}
        """.trimIndent()
        val (text, actions) = extractAskActions(reply)
        assertEquals(2, actions.size)
        assertEquals("complete", actions[0].op)
        assertEquals(4L, actions[0].id)
        assertEquals("2026-09-04", actions[1].date)
        assertTrue("actions" !in text)
        assertTrue(text.startsWith("Done"))
    }

    @Test fun toleratesCodeFences() {
        val reply = "Sure.\n```json\n{\"actions\":[{\"op\":\"delete\",\"id\":9}]}\n```"
        val (text, actions) = extractAskActions(reply)
        assertEquals(listOf("delete"), actions.map { it.op })
        assertEquals("Sure.", text)
    }

    @Test fun plainAnswerHasNoActions() {
        val (text, actions) = extractAskActions("You have three tasks overdue.")
        assertTrue(actions.isEmpty())
        assertEquals("You have three tasks overdue.", text)
    }

    @Test fun bracesInsideStringsDoNotBreakMatching() {
        val reply = """Renamed. {"actions":[{"op":"rename","id":2,"title":"fix {gate} latch"}]}"""
        val (_, actions) = extractAskActions(reply)
        assertEquals("fix {gate} latch", actions[0].title)
    }

    @Test fun malformedBlockIsIgnored() {
        val reply = """Hmm {"actions":[{"op":}"""
        val (text, actions) = extractAskActions(reply)
        assertTrue(actions.isEmpty())
        assertEquals(reply, text)
    }
}
