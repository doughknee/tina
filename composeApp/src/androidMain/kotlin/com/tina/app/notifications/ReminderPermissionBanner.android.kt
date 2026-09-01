package com.tina.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tina.app.resources.Res
import com.tina.app.resources.reminders_allow_exact
import com.tina.app.resources.reminders_allow_notifications
import com.tina.app.resources.reminders_banner_body
import com.tina.app.resources.reminders_banner_title
import org.jetbrains.compose.resources.stringResource

private fun hasNotificationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun canExactAlarm(context: Context): Boolean =
    context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

@Composable
actual fun ReminderPermissionBanner(modifier: Modifier) {
    val context = LocalContext.current
    var notificationsGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    var exactGranted by remember { mutableStateOf(canExactAlarm(context)) }

    // Re-check when coming back from the system settings screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsGranted = hasNotificationPermission(context)
                exactGranted = canExactAlarm(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (notificationsGranted && exactGranted) return

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsGranted = granted }

    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(Res.string.reminders_banner_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(Res.string.reminders_banner_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row {
                if (!notificationsGranted) {
                    TextButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text(stringResource(Res.string.reminders_allow_notifications))
                    }
                }
                if (!exactGranted) {
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }) {
                        Text(stringResource(Res.string.reminders_allow_exact))
                    }
                }
            }
        }
    }
}
