package com.tina.app.ai

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private val NETWORK_PERMISSIONS = arrayOf(
    "android.permission.INTERNET",
    "android.permission.ACCESS_LOCAL_NETWORK",
)

@Composable
actual fun RequestAiNetworkPermissions(enabled: Boolean) {
    if (!enabled) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* results surface through the test button's diagnostics */ }
    LaunchedEffect(Unit) {
        val missing = NETWORK_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) launcher.launch(missing.toTypedArray())
    }
}
