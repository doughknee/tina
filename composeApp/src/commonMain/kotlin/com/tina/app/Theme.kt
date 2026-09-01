package com.tina.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
expect fun appColorScheme(darkTheme: Boolean): ColorScheme

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = appColorScheme(isSystemInDarkTheme()),
        content = content,
    )
}
