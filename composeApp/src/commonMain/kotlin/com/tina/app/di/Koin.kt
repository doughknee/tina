package com.tina.app.di

import com.tina.app.ai.AiCaptureParser
import com.tina.app.ai.CaptureRefiner
import com.tina.app.calendar.CalendarViewModel
import com.tina.app.calendar.EventEditorViewModel
import com.tina.app.capture.CaptureViewModel
import com.tina.app.data.AppDatabase
import com.tina.app.data.ItemRepository
import com.tina.app.data.SettingsRepository
import com.tina.app.data.buildDatabase
import com.tina.app.detail.DetailViewModel
import com.tina.app.inbox.InboxViewModel
import com.tina.app.notes.NoteEditorViewModel
import com.tina.app.notes.NotesViewModel
import com.tina.app.search.SearchViewModel
import com.tina.app.today.TodayViewModel
import com.tina.app.ui.SettingsViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    single { buildDatabase(get()) }
    single { get<AppDatabase>().itemDao() }
    single { ItemRepository(get(), get()) }
    single { SettingsRepository(get()) }
    single { AiCaptureParser(get(), get()) }
    single { com.tina.app.ai.AiImprover(get(), get()) }
    single { CaptureRefiner(get(), get(), get(), get()) }
    viewModelOf(::CaptureViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::TodayViewModel)
    viewModelOf(::InboxViewModel)
    viewModelOf(::CalendarViewModel)
    viewModel { (id: Long) -> DetailViewModel(id, get()) }
    viewModel { (id: Long) -> EventEditorViewModel(id, get()) }
    viewModelOf(::NotesViewModel)
    viewModelOf(::SearchViewModel)
    viewModel { (id: Long) -> NoteEditorViewModel(id, get()) }
}

fun initKoin(platformModule: Module, config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(commonModule, platformModule)
    }
}
