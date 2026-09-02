package com.tina.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tina.app.data.AppDatabase
import com.tina.app.data.createSettingsStore
import com.tina.app.notifications.AndroidReminderScheduler
import com.tina.app.notifications.Notifier
import com.tina.app.notifications.PlatformNotifier
import com.tina.app.notifications.ReminderScheduler
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        val context: Context = androidContext()
        Room.databaseBuilder<AppDatabase>(
            context = context,
            name = context.getDatabasePath("tina.db").absolutePath,
        )
    }
    single<DataStore<Preferences>> {
        createSettingsStore {
            androidContext().filesDir.resolve("settings.preferences_pb").absolutePath
        }
    }
    single<com.tina.app.data.SecretCipher> { com.tina.app.data.KeystoreSecretCipher() }
    single<Notifier> { PlatformNotifier(androidContext()) }
    single<com.tina.app.pro.ProStore> {
        com.tina.app.pro.PlayProStore(androidContext().applicationContext as android.app.Application, get())
    }
    single<ReminderScheduler> { AndroidReminderScheduler(androidContext()) }
    single<com.tina.app.data.NetworkStatus> { com.tina.app.data.AndroidNetworkStatus(androidContext()) }
    single {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                // local models can take a minute to cold-load before answering
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 180_000
                socketTimeoutMillis = 180_000
            }
        }
    }
}
