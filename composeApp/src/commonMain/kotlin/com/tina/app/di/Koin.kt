package com.tina.app.di

import com.tina.app.capture.CaptureViewModel
import com.tina.app.data.AppDatabase
import com.tina.app.data.ItemRepository
import com.tina.app.data.SettingsRepository
import com.tina.app.data.buildDatabase
import com.tina.app.ui.SettingsViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    single { buildDatabase(get()) }
    single { get<AppDatabase>().itemDao() }
    single { ItemRepository(get()) }
    single { SettingsRepository(get()) }
    viewModelOf(::CaptureViewModel)
    viewModelOf(::SettingsViewModel)
}

fun initKoin(platformModule: Module, config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(commonModule, platformModule)
    }
}
