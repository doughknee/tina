package com.tina.app.ai

import kotlin.test.Test
import kotlin.test.assertEquals

class AiErrorTest {
    @Test fun statusCodesMapToWhatTheUserCanFix() {
        assertEquals(AiError.UNAUTHORIZED, aiErrorFor(401))
        assertEquals(AiError.UNAUTHORIZED, aiErrorFor(403))
        assertEquals(AiError.NOT_FOUND, aiErrorFor(404))
        assertEquals(AiError.RATE_LIMITED, aiErrorFor(429))
        assertEquals(AiError.SERVER, aiErrorFor(503))
        assertEquals(AiError.BAD_REPLY, aiErrorFor(400))
    }
}
