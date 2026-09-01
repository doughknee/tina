package com.tina.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tina.app.data.ItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Alarms do not survive a reboot; re-arm everything. */
class BootReceiver : BroadcastReceiver(), KoinComponent {
    private val repository: ItemRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                repository.rescheduleAllReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
