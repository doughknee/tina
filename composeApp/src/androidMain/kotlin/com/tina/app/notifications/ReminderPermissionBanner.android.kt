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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tina.app.data.SettingsRepository
import com.tina.app.resources.Res
import com.tina.app.resources.reminders_allow_exact
import com.tina.app.resources.reminders_allow_notifications
import com.tina.app.resources.reminders_banner_body
import com.tina.app.resources.reminders_banner_for
import com.tina.app.resources.reminders_banner_title
import com.tina.app.resources.reminders_not_now
import com.tina.app.resources.reminders_off
import com.tina.app.resources.reminders_turn_on
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private fun hasNotificationPermission(context: Context): Boolean =
    android.os.Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun canExactAlarm(context: Context): Boolean =
    context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

@Composable
actual fun ReminderPermissionBanner(subject: String?, modifier: Modifier) {
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

    val settings = koinInject<SettingsRepository>()
    val scope = rememberCoroutineScope()
    // true until read, so the card never flashes for someone who already put it away
    val dismissed by settings.remindersDismissed.collectAsState(initial = true)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsGranted = granted }
    fun askExact() = context.startActivity(
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
    )
    fun askNext() = if (!notificationsGranted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else askExact()

    if (dismissed) {
        // Put away: one quiet line instead of a card above every day, gone once granted.
        Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(Res.string.reminders_off),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = ::askNext) { Text(stringResource(Res.string.reminders_turn_on)) }
            }
        }
        return
    }

    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(Res.string.reminders_banner_title), style = MaterialTheme.typography.titleMedium)
            Text(
                // named after the thing that needs it when there is one on screen
                if (subject != null) stringResource(Res.string.reminders_banner_for, subject)
                else stringResource(Res.string.reminders_banner_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = ::askNext) {
                    Text(stringResource(if (!notificationsGranted) Res.string.reminders_allow_notifications else Res.string.reminders_allow_exact))
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = { scope.launch { settings.setRemindersDismissed(true) } }) {
                    Text(stringResource(Res.string.reminders_not_now))
                }
            }
        }
    }
}
