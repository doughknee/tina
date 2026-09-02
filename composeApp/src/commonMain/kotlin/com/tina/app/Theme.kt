package com.tina.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.tina.app.data.ContrastMode
import com.tina.app.data.ReduceMotionMode
import com.tina.app.data.Settings
import com.tina.app.data.ThemeMode
import com.tina.app.ui.LocalReduceMotion

/** Current user settings, available anywhere in the tree. */
val LocalSettings = staticCompositionLocalOf { Settings() }

@Composable
expect fun appColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme

/** True when the platform itself asks for reduced motion. */
@Composable
expect fun systemPrefersReducedMotion(): Boolean

/**
 * Contrast pushes on/surface pairs apart. Real M3 contrast tone sets aren't exposed
 * by the multiplatform scheme builders, so this shifts the same roles in the same
 * direction rather than leaving the setting decorative.
 */
private fun ColorScheme.withContrast(mode: ContrastMode, dark: Boolean): ColorScheme {
    if (mode == ContrastMode.STANDARD) return this
    val amount = if (mode == ContrastMode.MEDIUM) 0.12f else 0.28f
    // push foregrounds away from their backgrounds
    val fg = if (dark) Color.White else Color.Black
    return copy(
        onSurface = lerp(onSurface, fg, amount),
        onSurfaceVariant = lerp(onSurfaceVariant, fg, amount),
        onBackground = lerp(onBackground, fg, amount),
        outline = lerp(outline, fg, amount * 0.6f),
        outlineVariant = lerp(outlineVariant, fg, amount * 0.6f),
    )
}

/** OLED black: only the page-level surfaces go true black, not the raised ones. */
private fun ColorScheme.pureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = surfaceContainerLow.darkenTowardsBlack(),
    surfaceContainer = surfaceContainer.darkenTowardsBlack(),
)

private fun Color.darkenTowardsBlack(): Color = lerp(this, Color.Black, 0.5f)

@Composable
fun AppTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = appColorScheme(dark, settings.dynamicColor)
        .withContrast(settings.contrast, dark)
        .let { if (dark && settings.pureBlack) it.pureBlack() else it }

    val reduceMotion = when (settings.reduceMotion) {
        ReduceMotionMode.ON -> true
        ReduceMotionMode.OFF -> false
        ReduceMotionMode.SYSTEM -> systemPrefersReducedMotion()
    }

    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        // expressive: the spring MotionScheme every AppMotion transition reads, plus the
        // expressive component defaults now that material3 1.12 ships them
        MaterialExpressiveTheme(colorScheme = scheme, content = content)
    }
}
