package com.tina.app.ai

import androidx.compose.runtime.Composable

/**
 * Requests whatever network permissions the platform gates AI providers behind
 * (Android 17: INTERNET is user-sensitive, LAN needs ACCESS_LOCAL_NETWORK).
 * No-op where nothing is needed.
 */
@Composable
expect fun RequestAiNetworkPermissions(enabled: Boolean)
