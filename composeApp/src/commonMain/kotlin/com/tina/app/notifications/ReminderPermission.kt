package com.tina.app.notifications

import androidx.compose.runtime.Composable

/**
 * Null when reminders can already ring (or the platform never asks); otherwise the next step
 * that gets them ringing — the notification prompt, then the exact-alarm settings page.
 * Asked for once, in a snackbar, the first time a timed item is saved.
 */
@Composable
expect fun rememberReminderPermissionRequest(): (() -> Unit)?
