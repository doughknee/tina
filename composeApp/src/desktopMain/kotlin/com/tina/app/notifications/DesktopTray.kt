package com.tina.app.notifications

import androidx.compose.ui.window.TrayState

/** Set by main() once the tray exists; PlatformNotifier routes through it. */
object DesktopTray {
    var state: TrayState? = null
}
