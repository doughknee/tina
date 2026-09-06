package com.tina.app.ai

import com.tina.app.data.AiProvider
import com.tina.app.pro.Entitlement
import com.tina.app.pro.ProPlan
import kotlin.test.Test
import kotlin.test.assertEquals

class AskProviderTest {
    private val pro = Entitlement.Pro(ProPlan.YEARLY, "token")

    @Test fun proWithNothingConfiguredAsksThroughTheRelay() {
        assertEquals(AiProvider.HOSTED, askProvider(AiProvider.OFF, pro))
    }

    @Test fun freeWithNothingConfiguredStaysOff() {
        assertEquals(AiProvider.OFF, askProvider(AiProvider.OFF, Entitlement.Free))
    }

    @Test fun anExplicitProviderIsRespectedEitherWay() {
        assertEquals(AiProvider.ANTHROPIC, askProvider(AiProvider.ANTHROPIC, pro))
        assertEquals(AiProvider.OLLAMA, askProvider(AiProvider.OLLAMA, Entitlement.Free))
    }
}
