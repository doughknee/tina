package com.tina.app.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class StreamingTest {
    @Test
    fun textDeltaIsTheOnlyLineThatYieldsText() {
        assertEquals("Hel", sseTextDelta("""data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hel"}}"""))
        assertNull(sseTextDelta("event: content_block_delta"))
        assertNull(sseTextDelta(""))
        assertNull(sseTextDelta("""data: {"type":"ping"}"""))
        assertNull(sseTextDelta("""data: {"type":"message_start","message":{}}"""))
        assertNull(sseTextDelta("""data: {"type":"content_block_delta","index":0,"delta":{"type":"input_json_delta","partial_json":"{"}}"""))
        assertNull(sseTextDelta("data: [DONE]"))
    }

    @Test
    fun streamedErrorThrows() {
        val e = assertFailsWith<AiException> {
            sseTextDelta("""data: {"type":"error","error":{"type":"overloaded_error","message":"Overloaded"}}""")
        }
        assertEquals(AiError.SERVER, e.error)
        assertEquals("Overloaded", e.message)
    }

    @Test
    fun previewHidesTheActionsBlockOnceItStarts() {
        assertEquals("Sure, moving it.", streamPreview("Sure, moving it."))
        assertEquals("Sure, moving it.", streamPreview("Sure, moving it.\n{\"actions\""))
        assertEquals("Sure, moving it.", streamPreview("Sure, moving it.\n{\"actions\": [{\"op\": \"upd"))
        // a brace in prose before the block is not the block
        assertEquals("Use {braces}.", streamPreview("Use {braces}.\n{\"actions\": ["))
    }
}
