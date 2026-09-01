package com.tina.app.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
actual fun rememberPlatformActions(): PlatformActions {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val diagnosticsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            runCatching {
                // last 500 lines for this app only — enough to explain a crash
                val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "500"))
                val log = process.inputStream.bufferedReader().use { it.readText() }
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(log.encodeToByteArray())
                }
            }
        }
    }

    return remember(context) {
        object : PlatformActions {
            override val supportsLanguageSettings = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            override val supportsQuickTile = true
            override val supportsDiagnostics = true

            override fun openLanguageSettings() {
                if (!supportsLanguageSettings) return
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                            .setData(Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }

            override fun openNotificationSettings() {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }

            override fun setQuickTileEnabled(enabled: Boolean) {
                runCatching {
                    context.packageManager.setComponentEnabledSetting(
                        ComponentName(context, "com.tina.app.capture.CaptureTileService"),
                        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP,
                    )
                }
            }

            override fun exportDiagnostics() {
                diagnosticsLauncher.launch("tina-diagnostics.txt")
            }
        }
    }
}
