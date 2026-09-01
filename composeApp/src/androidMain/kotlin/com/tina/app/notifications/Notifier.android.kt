package com.tina.app.notifications

import android.content.Context

actual class PlatformNotifier(private val context: Context) : Notifier {
    override fun show(title: String, body: String) {
        // TODO: post a real notification once the app has something to say
    }
}
