package com.tina.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tina.app.data.AppDatabase
import com.tina.app.notifications.Notifier
import com.tina.app.notifications.PlatformNotifier
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
    single<Notifier> { PlatformNotifier(androidContext()) }
}
