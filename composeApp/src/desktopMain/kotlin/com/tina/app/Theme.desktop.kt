package com.tina.app

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun appColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme =
    if (darkTheme) darkColorScheme() else lightColorScheme()

/** Desktop exposes no reduced-motion signal; the explicit setting is the only source. */
@Composable
actual fun systemPrefersReducedMotion(): Boolean = false
