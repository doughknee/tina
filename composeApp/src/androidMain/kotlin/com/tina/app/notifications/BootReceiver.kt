package com.tina.app.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tina.app.data.ItemRepository
import com.tina.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Alarms are absolute instants that the system forgets on reboot and on app update, and that
 * stop matching the wall clock after a time-zone change. Re-arm everything on each of those,
 * and again when the exact-alarm grant flips so the inexact fallbacks are upgraded.
 */
class BootReceiver : BroadcastReceiver(), KoinComponent {
    private val repository: ItemRepository by inject()
    private val settingsRepository: SettingsRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                repository.rescheduleAllReminders()
                DigestScheduler.sync(context, settingsRepository.settings.first())
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
