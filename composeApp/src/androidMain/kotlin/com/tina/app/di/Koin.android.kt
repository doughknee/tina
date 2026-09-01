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
    single<Notifier> { PlatformNotifier(androidContext()) }
    single<ReminderScheduler> { AndroidReminderScheduler(androidContext()) }
    single { HttpClient(OkHttp) }
}
