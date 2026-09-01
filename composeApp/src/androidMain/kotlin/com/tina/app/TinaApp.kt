package com.tina.app

import android.app.Application
import com.tina.app.data.ItemRepository
import com.tina.app.di.androidModule
import com.tina.app.di.initKoin
import com.tina.app.notifications.ensureReminderChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TinaApp : Application(), KoinComponent {
    private val repository: ItemRepository by inject()
    private val settingsRepository: com.tina.app.data.SettingsRepository by inject()

    override fun onCreate() {
        super.onCreate()
        initKoin(androidModule) { androidContext(this@TinaApp) }
        ensureReminderChannel(this)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            // Alarms drift across app updates and process death; re-arm on every cold start.
            repository.rescheduleAllReminders()
            // Trash retention is enforced here rather than by a scheduled job.
            val retention = settingsRepository.settings.first().trashRetention
            repository.purgeExpiredTrash(retention.days)
        }
        // digest alarms follow the settings that describe them
        scope.launch {
            settingsRepository.settings
                .map { listOf(it.dailyAgenda, it.dailyAgendaMinutes, it.overdueNudge, it.overdueNudgeMinutes, it.inboxReminder) }
                .distinctUntilChanged()
                .collect {
                    com.tina.app.notifications.DigestScheduler.sync(
                        this@TinaApp,
                        settingsRepository.settings.first(),
                    )
                }
        }
    }
}
