package com.tina.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPlatformActions(): PlatformActions = remember {
    object : PlatformActions {
        // the desktop build has no per-app locale screen, tile or notification channels
        override val supportsLanguageSettings = false
        override val supportsQuickTile = false
        override val supportsDiagnostics = false

        override fun openLanguageSettings() = Unit
        override fun openNotificationSettings() = Unit
        override fun setQuickTileEnabled(enabled: Boolean) = Unit
        override fun exportDiagnostics() = Unit
    }
}
