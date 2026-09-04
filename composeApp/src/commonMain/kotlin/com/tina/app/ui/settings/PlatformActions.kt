package com.tina.app.ui.settings

import androidx.compose.runtime.Composable

/**
 * Settings rows that leave the app or touch OS state. Implemented per platform;
 * rows whose action is unsupported are hidden rather than shown doing nothing.
 */
interface PlatformActions {
    /** Per-app language screen (Android 13+). */
    fun openLanguageSettings()

    /** System notification-channel settings for the app. */
    fun openNotificationSettings()

    /** Enable/disable the quick-settings tile component. */
    fun setQuickTileEnabled(enabled: Boolean)

    /** Writes recent logs to a file the user picks. */
    fun exportDiagnostics()

    /** The system share sheet with plain text (a note as Markdown). */
    fun share(title: String, text: String)

    val supportsLanguageSettings: Boolean
    val supportsQuickTile: Boolean
    val supportsDiagnostics: Boolean
    val supportsShare: Boolean
}

@Composable
expect fun rememberPlatformActions(): PlatformActions
