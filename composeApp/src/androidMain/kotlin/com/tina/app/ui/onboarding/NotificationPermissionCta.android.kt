package com.tina.app.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.tina.app.resources.Res
import com.tina.app.resources.onb_notifications_on
import com.tina.app.resources.reminders_allow_notifications
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun NotificationPermissionCta() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    FilledTonalButton(
        enabled = !granted,
        onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
    ) {
        Text(stringResource(if (granted) Res.string.onb_notifications_on else Res.string.reminders_allow_notifications))
    }
}
