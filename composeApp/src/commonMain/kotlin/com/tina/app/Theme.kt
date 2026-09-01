package com.tina.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.tina.app.data.Settings
import com.tina.app.data.ThemeMode

/** Current user settings, available anywhere in the tree. */
val LocalSettings = staticCompositionLocalOf { Settings() }

@Composable
expect fun appColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme

@Composable
fun AppTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = appColorScheme(dark, settings.dynamicColor),
        content = content,
    )
}
