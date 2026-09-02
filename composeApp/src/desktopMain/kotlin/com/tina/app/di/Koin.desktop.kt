package com.tina.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tina.app.data.AppDatabase
import com.tina.app.data.createSettingsStore
import com.tina.app.notifications.NoopReminderScheduler
import com.tina.app.notifications.Notifier
import com.tina.app.notifications.PlatformNotifier
import com.tina.app.notifications.ReminderScheduler
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import java.io.File
import org.koin.dsl.module

val desktopModule = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        val dbFile = File(System.getProperty("user.home"), ".tina/tina.db")
        dbFile.parentFile?.mkdirs()
        Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
    }
    single<DataStore<Preferences>> {
        createSettingsStore {
            File(System.getProperty("user.home"), ".tina/settings.preferences_pb")
                .apply { parentFile?.mkdirs() }
                .absolutePath
        }
    }
    single<com.tina.app.data.SecretCipher> { com.tina.app.data.PlainSecretCipher }
    single<Notifier> { PlatformNotifier() }
    single<ReminderScheduler> { NoopReminderScheduler }
    single<com.tina.app.data.NetworkStatus> { com.tina.app.data.DesktopNetworkStatus() }
    single {
        HttpClient(CIO) {
            install(HttpTimeout) {
                // local models can take a minute to cold-load before answering
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 180_000
                socketTimeoutMillis = 180_000
            }
        }
    }
}
