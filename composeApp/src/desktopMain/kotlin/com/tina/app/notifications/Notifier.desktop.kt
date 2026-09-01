package com.tina.app.notifications

import androidx.compose.ui.window.Notification

actual class PlatformNotifier : Notifier {
    override fun show(title: String, body: String) {
        DesktopTray.state?.sendNotification(Notification(title, body, Notification.Type.Info))
    }
}
