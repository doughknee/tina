package com.tina.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.ai.AiCaptureParser
import com.tina.app.ai.ANTHROPIC_DEFAULT_MODEL
import com.tina.app.ai.OLLAMA_DEFAULT_BASE_URL
import com.tina.app.ai.OPENAI_DEFAULT_BASE_URL
import com.tina.app.data.AiProvider
import com.tina.app.data.Settings
import com.tina.app.data.SettingsRepository
import com.tina.app.data.ThemeMode
import kotlin.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val aiParser: AiCaptureParser,
) : ViewModel() {
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

    /** Switching provider seeds sensible defaults so the fields aren't a puzzle. */
    fun setAiProvider(provider: AiProvider) {
        viewModelScope.launch {
            val changed = settings.value.aiProvider != provider
            val (baseUrl, model) = if (!changed) {
                null to null
            } else when (provider) {
                AiProvider.OLLAMA -> OLLAMA_DEFAULT_BASE_URL to ""
                AiProvider.ANTHROPIC -> "" to ANTHROPIC_DEFAULT_MODEL
                AiProvider.OPENAI -> OPENAI_DEFAULT_BASE_URL to ""
                AiProvider.CUSTOM -> "" to null
                AiProvider.OFF -> null to null
            }
            repository.setAiProviderApplyingDefaults(provider, baseUrl, model)
        }
    }

    fun setAiBaseUrl(url: String) {
        viewModelScope.launch { repository.setAiBaseUrl(url) }
    }

    fun setAiModel(model: String) {
        viewModelScope.launch { repository.setAiModel(model) }
    }

    fun setAiApiKey(key: String) {
        viewModelScope.launch { repository.setAiApiKey(key) }
    }

    fun testAi(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val parsed = aiParser.refine("lunch with sam tomorrow at noon", now, settings.value.firstDayOfWeek)
            onResult(parsed != null)
        }
    }
}
