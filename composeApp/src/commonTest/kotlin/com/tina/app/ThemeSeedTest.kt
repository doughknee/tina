package com.tina.app

import com.tina.app.data.ThemeSeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ThemeSeedTest {
    @Test
    fun everySeedGrowsItsOwnScheme() {
        val lightPrimaries = ThemeSeed.entries.map { seedColorScheme(it, dark = false).primary }
        assertEquals(lightPrimaries.size, lightPrimaries.toSet().size, "two seeds gave the same primary")
        ThemeSeed.entries.forEach { seed ->
            assertNotEquals(seedColorScheme(seed, dark = false).surface, seedColorScheme(seed, dark = true).surface)
        }
    }

    @Test
    fun theBrandSeedIsTheLauncherBlue() {
        assertEquals(BrandSeed.value, androidx.compose.ui.graphics.Color(ThemeSeed.PEGGY.argb).value)
        assertTrue(ThemeSeed.entries.first() == ThemeSeed.PEGGY, "the free seed comes first in the rail")
    }
}
