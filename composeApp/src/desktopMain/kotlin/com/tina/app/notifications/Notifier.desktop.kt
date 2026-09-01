package com.tina.app.notifications

actual class PlatformNotifier : Notifier {
    override fun show(title: String, body: String) = Unit // no-op on desktop
}
