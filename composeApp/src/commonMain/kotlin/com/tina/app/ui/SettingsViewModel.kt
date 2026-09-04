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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val aiParser: AiCaptureParser,
    private val itemRepository: com.tina.app.data.ItemRepository,
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

    fun setAiWorkspaceId(id: String) {
        viewModelScope.launch { repository.setAiWorkspaceId(id) }
    }

    fun setAiRefineMode(mode: com.tina.app.data.AiRefineMode) {
        viewModelScope.launch { repository.setAiRefineMode(mode) }
    }

    fun setAiInstructions(text: String) {
        viewModelScope.launch { repository.setAiInstructions(text) }
    }

    fun setAiAskEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setAiAskEnabled(enabled) }
    }

    fun setAiWifiOnly(v: Boolean) = launchEdit { repository.setAiWifiOnly(v) }
    fun setOpenAppTo(v: com.tina.app.data.OpenAppTo) = launchEdit { repository.setOpenAppTo(v) }
    fun setHaptics(v: Boolean) = launchEdit { repository.setHaptics(v) }
    fun setReduceMotion(v: com.tina.app.data.ReduceMotionMode) = launchEdit { repository.setReduceMotion(v) }
    fun setContrast(v: com.tina.app.data.ContrastMode) = launchEdit { repository.setContrast(v) }
    fun setPureBlack(v: Boolean) = launchEdit { repository.setPureBlack(v) }
    fun setQuickSettingsTile(v: Boolean) = launchEdit { repository.setQuickSettingsTile(v) }
    fun setAutoFocusCapture(v: Boolean) = launchEdit { repository.setAutoFocusCapture(v) }
    fun setKeepKeyboardUp(v: Boolean) = launchEdit { repository.setKeepKeyboardUp(v) }
    fun setVoiceCapture(v: Boolean) = launchEdit { repository.setVoiceCapture(v) }
    fun setUndoWindowSeconds(v: Int) = launchEdit { repository.setUndoWindowSeconds(v) }
    fun setMorningStart(v: Int) = launchEdit { repository.setMorningStart(v) }
    fun setAfternoonStart(v: Int) = launchEdit { repository.setAfternoonStart(v) }
    fun setEveningStart(v: Int) = launchEdit { repository.setEveningStart(v) }
    fun setDailyAgenda(v: Boolean) = launchEdit { repository.setDailyAgenda(v) }
    fun setDailyAgendaMinutes(v: Int) = launchEdit { repository.setDailyAgendaMinutes(v) }
    fun setOverdueNudge(v: Boolean) = launchEdit { repository.setOverdueNudge(v) }
    fun setOverdueNudgeMinutes(v: Int) = launchEdit { repository.setOverdueNudgeMinutes(v) }
    fun setQuietHours(v: Boolean) = launchEdit { repository.setQuietHours(v) }
    fun setUndatedToSort(v: Boolean) = launchEdit { repository.setUndatedToSort(v) }
    fun setQuietStartMinutes(v: Int) = launchEdit { repository.setQuietStartMinutes(v) }
    fun setQuietEndMinutes(v: Int) = launchEdit { repository.setQuietEndMinutes(v) }
    fun setInboxReminder(v: Boolean) = launchEdit { repository.setInboxReminder(v) }
    fun setInboxReminderDays(v: Int) = launchEdit { repository.setInboxReminderDays(v) }
    fun setShowCompletedInToday(v: Boolean) = launchEdit { repository.setShowCompletedInToday(v) }
    fun setSearchCompleted(v: Boolean) = launchEdit { repository.setSearchCompleted(v) }
    fun setAppLock(v: Boolean) = launchEdit { repository.setAppLock(v) }
    fun setAppLockGrace(v: Int) = launchEdit { repository.setAppLockGrace(v) }
    fun setHideInAppSwitcher(v: Boolean) = launchEdit { repository.setHideInAppSwitcher(v) }
    fun setAutoBackup(v: Boolean) = launchEdit { repository.setAutoBackup(v) }
    fun setTrashRetention(v: com.tina.app.data.TrashRetention) = launchEdit { repository.setTrashRetention(v) }
    fun setLaunchAtLogin(v: Boolean) = launchEdit { repository.setLaunchAtLogin(v) }
    fun setCloseToTray(v: Boolean) = launchEdit { repository.setCloseToTray(v) }

    private fun launchEdit(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    /** No undo by design — the hold-to-confirm control is the safeguard. */
    fun deleteEverything() = launchEdit { itemRepository.deleteEverything() }

    /** Counts shown as supporting text on the Data / Organisation rows. */
    data class Stats(val items: Int = 0, val completed: Int = 0, val tags: Int = 0, val trashed: Int = 0)

    val stats: kotlinx.coroutines.flow.StateFlow<Stats> = kotlinx.coroutines.flow.combine(
        itemRepository.observeAll(),
        itemRepository.observeTrashCount(),
    ) { all, trashed ->
        Stats(
            items = all.size,
            completed = all.count { it.completed },
            tags = all.flatMap { it.tags }.distinct().size,
            trashed = trashed,
        )
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), Stats())

    private var clearedBatch: List<com.tina.app.data.Item> = emptyList()

    /** Soft-deletes completed items so they land in Trash; the snackbar can put them back. */
    fun clearCompleted() = launchEdit {
        val done = itemRepository.allItems().filter { it.completed }
        clearedBatch = done
        done.forEach { itemRepository.delete(it.id) }
    }

    fun undoClearCompleted() = launchEdit {
        val batch = clearedBatch
        clearedBatch = emptyList()
        batch.forEach { itemRepository.restore(it) }
    }

    /** Callback receives null on success, otherwise a human-readable failure reason. */
    fun testAi(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            onResult(aiParser.testConnection(now, settings.value.firstDayOfWeek))
        }
    }

    fun applyBackupSettings(settings: com.tina.app.data.BackupSettings) {
        viewModelScope.launch { repository.applyBackup(settings) }
    }

    /** Developer options: the first-run cards show again immediately (App collects the flag). */
    fun resetOnboarding() = viewModelScope.launch { repository.resetOnboarding() }
    fun setDevOptions(enabled: Boolean) = viewModelScope.launch { repository.setDevOptions(enabled) }
}
