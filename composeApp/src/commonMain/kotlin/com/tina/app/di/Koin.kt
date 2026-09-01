package com.tina.app.di

import com.tina.app.data.AppDatabase
import com.tina.app.data.ItemRepository
import com.tina.app.data.buildDatabase
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    single { buildDatabase(get()) }
    single { get<AppDatabase>().itemDao() }
    single { ItemRepository(get()) }
}

fun initKoin(platformModule: Module, config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(commonModule, platformModule)
    }
}
