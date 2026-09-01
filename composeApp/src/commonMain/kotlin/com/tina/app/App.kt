package com.tina.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.tina.app.capture.CaptureScreen

data object CaptureRoute

@Composable
fun App() {
    AppTheme {
        val backStack = remember { mutableStateListOf<Any>(CaptureRoute) }
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<CaptureRoute> { CaptureScreen() }
            },
        )
    }
}
