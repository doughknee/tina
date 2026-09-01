package com.tina.app.notifications

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Rationale card asking for notification / exact-alarm access.
 * Renders nothing when everything is already granted (and always on desktop).
 */
@Composable
expect fun ReminderPermissionBanner(modifier: Modifier)
