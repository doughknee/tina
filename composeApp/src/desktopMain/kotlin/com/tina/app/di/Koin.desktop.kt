package com.tina.app.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.tina.app.db.AppDatabase
import com.tina.app.notifications.Notifier
import com.tina.app.notifications.PlatformNotifier
import java.io.File
import org.koin.dsl.module

val desktopModule = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        val dbFile = File(System.getProperty("user.home"), ".tina/tina.db")
        dbFile.parentFile?.mkdirs()
        Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
    }
    single<Notifier> { PlatformNotifier() }
}
