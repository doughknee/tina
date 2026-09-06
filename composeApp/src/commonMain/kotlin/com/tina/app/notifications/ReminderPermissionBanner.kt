package com.tina.app.notifications

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Rationale card asking for notification / exact-alarm access.
 * Renders nothing when everything is already granted (and always on desktop).
 */
@Composable
/** [subject] names what needs to ring ("Dentist at 3 PM") when something on screen does. */
expect fun ReminderPermissionBanner(subject: String?, modifier: Modifier)
