package com.tina.app.notifications

interface Notifier {
    fun show(title: String, body: String)
}

expect class PlatformNotifier : Notifier
