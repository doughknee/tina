package com.tina.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Settings
import com.tina.app.data.SettingsRepository
import com.tina.app.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    fun setFirstDayOfWeek(day: DayOfWeek) {
        viewModelScope.launch { repository.setFirstDayOfWeek(day) }
    }

    fun setUse24h(enabled: Boolean) {
        viewModelScope.launch { repository.setUse24h(enabled) }
    }

    fun setDefaultReminderMinutes(minutes: Int) {
        viewModelScope.launch { repository.setDefaultReminderMinutes(minutes) }
    }
}
