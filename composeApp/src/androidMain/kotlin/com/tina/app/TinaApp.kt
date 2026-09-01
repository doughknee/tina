package com.tina.app

import android.app.Application
import com.tina.app.data.ItemRepository
import com.tina.app.di.androidModule
import com.tina.app.di.initKoin
import com.tina.app.notifications.ensureReminderChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TinaApp : Application(), KoinComponent {
    private val repository: ItemRepository by inject()

    override fun onCreate() {
        super.onCreate()
        initKoin(androidModule) { androidContext(this@TinaApp) }
        ensureReminderChannel(this)
        // Alarms drift across app updates and process death; re-arm on every cold start.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repository.rescheduleAllReminders()
        }
    }
}
