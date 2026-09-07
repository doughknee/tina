package com.tina.app.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class WhatsNewTest {
    private val changelog = """
        # Changelog

        ## Unreleased

        ### Shell
        - The nav is **Plan · Ask · Ideas**.

        ## v1.9.4 (internal testing, 2026-09-05)

        - Streaming keeps the end in view.

        ### Peggy Pro
        - App icons.

        ## v1.8.3 (internal testing, 2026-09-04)

        - Old news.
    """.trimIndent()

    @Test
    fun currentFeatureReleaseAndUnreleasedOnly() {
        assertEquals(
            listOf(
                "Unreleased · Shell" to "• The nav is Plan · Ask · Ideas.",
                "1.9.4" to "• Streaming keeps the end in view.",
                "1.9.4 · Peggy Pro" to "• App icons.",
            ),
            whatsNewEntries(changelog, "1.9.4-dev"),
        )
    }

    @Test
    fun otherFeatureReleaseIsNotShown() {
        assertEquals(listOf("Unreleased · Shell" to "• The nav is Plan · Ask · Ideas."), whatsNewEntries(changelog, "2.0.0"))
    }
}
