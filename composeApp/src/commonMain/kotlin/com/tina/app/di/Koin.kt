package com.tina.app.di

import com.tina.app.db.AppDatabase
import com.tina.app.db.buildDatabase
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    single { buildDatabase(get()) }
    single { get<AppDatabase>().noteDao() }
}

fun initKoin(platformModule: Module, config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(commonModule, platformModule)
    }
}
