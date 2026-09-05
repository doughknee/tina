package com.tina.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import okio.Path.Companion.toPath

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** CAPTURE and TODAY are the old names for Plan; both stay readable from saved settings. */
enum class OpenAppTo { CAPTURE, TODAY, LAST, SORT, IDEAS }

/** SYSTEM follows the platform accessibility flag; ON/OFF override it. */
enum class ReduceMotionMode { SYSTEM, ON, OFF }

enum class ContrastMode { STANDARD, MEDIUM, HIGH }

enum class TrashRetention(val days: Int?) { DAYS_7(7), DAYS_30(30), FOREVER(null) }

enum class AiProvider { OFF, OLLAMA, ANTHROPIC, OPENAI, CUSTOM, HOSTED }

/** AUTO applies refinements silently, SUGGEST computes but waits for the user, MANUAL only runs on demand. */
enum class AiRefineMode { AUTO, SUGGEST, MANUAL }

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val use24h: Boolean = false,
    val defaultReminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
    val aiProvider: AiProvider = AiProvider.OFF,
    val aiBaseUrl: String = "",
    val aiModel: String = "",
    val aiApiKey: String = "",
    val aiWorkspaceId: String = "",
    val aiRefineMode: AiRefineMode = AiRefineMode.AUTO,
    val aiInstructions: String = "",
    val aiAskEnabled: Boolean = false,
    val aiAskWriteEnabled: Boolean = false,
    val aiWifiOnly: Boolean = false,
    // General
    val openAppTo: OpenAppTo = OpenAppTo.CAPTURE,
    /** Agenda zoom level (a [com.tina.app.agenda.Granularity] name); view state, so not in backups. */
    val agendaRange: String = "DAY",
    /** Agenda calendar header (an [com.tina.app.agenda.AgendaCalendar] name): the same on every range. */
    val agendaCalendar: String = "WEEK",
    /** Ideas grid: NotesSort / NotesLayout names. */
    val notesSort: String = "EDITED",
    val notesLayout: String = "GRID",
    /** Developer options unlocked (seven taps on Version); the dev build never needs it. */
    val devOptions: Boolean = false,
    /** major.minor of the last release whose what's-new sheet was shown; blank until the first upgrade check. */
    val whatsNewSeen: String = "",
    val haptics: Boolean = true,
    val reduceMotion: ReduceMotionMode = ReduceMotionMode.SYSTEM,
    // Appearance
    val contrast: ContrastMode = ContrastMode.STANDARD,
    val pureBlack: Boolean = false,
    // Capture
    val quickSettingsTile: Boolean = true,
    // off by default: opening the app shouldn't cover half the screen with a keyboard.
    // The capture widget and tile still focus the field, since that's their whole job.
    val autoFocusCapture: Boolean = false,
    val undatedToSort: Boolean = true,
    val keepKeyboardUp: Boolean = true,
    val voiceCapture: Boolean = true,
    val undoWindowSeconds: Int = 5,
    // Day sections: minutes from midnight, drive Today's Morning/Afternoon/Evening split
    val morningStartMinutes: Int = 6 * 60,
    val afternoonStartMinutes: Int = 12 * 60,
    val eveningStartMinutes: Int = 18 * 60,
    // Notifications
    val dailyAgenda: Boolean = false,
    val dailyAgendaMinutes: Int = 7 * 60 + 30,
    val overdueNudge: Boolean = false,
    val overdueNudgeMinutes: Int = 18 * 60,
    val quietHours: Boolean = false,
    val quietStartMinutes: Int = 22 * 60,
    val quietEndMinutes: Int = 7 * 60,
    val inboxReminder: Boolean = false,
    val inboxReminderDays: Int = 3,
    /** When the Sort reminder checks, on its own clock rather than the agenda's. */
    val inboxReminderMinutes: Int = 9 * 60,
    // Organisation
    val showCompletedInToday: Boolean = true,
    val searchCompleted: Boolean = true,
    // Privacy
    val appLock: Boolean = false,
    val appLockGraceSeconds: Int = 60,
    val hideInAppSwitcher: Boolean = false,
    // Data
    val autoBackup: Boolean = false,
    /** Epoch millis of the last successful silent backup; 0 until one has run. */
    val lastAutoBackupAt: Long = 0,
    val trashRetention: TrashRetention = TrashRetention.DAYS_30,
    // Desktop
    val launchAtLogin: Boolean = false,
    val closeToTray: Boolean = true,
)

fun createSettingsStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })

private val KEY_THEME = stringPreferencesKey("themeMode")
private val KEY_DYNAMIC = booleanPreferencesKey("dynamicColor")
private val KEY_FIRST_DAY = intPreferencesKey("firstDayOfWeek")
private val KEY_24H = booleanPreferencesKey("use24h")
private val KEY_REMINDER = intPreferencesKey("defaultReminderMinutes")
private val KEY_AI_PROVIDER = stringPreferencesKey("aiProvider")
private val KEY_AI_BASE_URL = stringPreferencesKey("aiBaseUrl")
private val KEY_AI_MODEL = stringPreferencesKey("aiModel")
private val KEY_AI_API_KEY = stringPreferencesKey("aiApiKey")
private val KEY_AI_WORKSPACE_ID = stringPreferencesKey("aiWorkspaceId")
private val KEY_AI_REFINE_MODE = stringPreferencesKey("aiRefineMode")
private val KEY_AI_INSTRUCTIONS = stringPreferencesKey("aiInstructions")
private val KEY_AI_ASK_ENABLED = booleanPreferencesKey("aiAskEnabled")
private val KEY_AI_ASK_WRITE = booleanPreferencesKey("aiAskWriteEnabled")
private val KEY_AI_WIFI_ONLY = booleanPreferencesKey("aiWifiOnly")
private val KEY_OPEN_APP_TO = stringPreferencesKey("openAppTo")
private val KEY_AGENDA_RANGE = stringPreferencesKey("agendaRange")
private val KEY_AGENDA_CALENDAR = stringPreferencesKey("agendaCalendar")
private val KEY_NOTES_SORT = stringPreferencesKey("notesSort")
private val KEY_NOTES_LAYOUT = stringPreferencesKey("notesLayout")
private val KEY_DEV_OPTIONS = booleanPreferencesKey("devOptions")
private val KEY_WHATS_NEW_SEEN = stringPreferencesKey("whatsNewSeen")
private val KEY_HAPTICS = booleanPreferencesKey("haptics")
private val KEY_REDUCE_MOTION = stringPreferencesKey("reduceMotion")
private val KEY_CONTRAST = stringPreferencesKey("contrast")
private val KEY_PURE_BLACK = booleanPreferencesKey("pureBlack")
private val KEY_QS_TILE = booleanPreferencesKey("quickSettingsTile")
private val KEY_AUTO_FOCUS_CAPTURE = booleanPreferencesKey("autoFocusCapture")
private val KEY_KEEP_KEYBOARD = booleanPreferencesKey("keepKeyboardUp")
private val KEY_VOICE_CAPTURE = booleanPreferencesKey("voiceCapture")
private val KEY_UNDO_SECONDS = intPreferencesKey("undoWindowSeconds")
private val KEY_MORNING = intPreferencesKey("morningStartMinutes")
private val KEY_AFTERNOON = intPreferencesKey("afternoonStartMinutes")
private val KEY_EVENING = intPreferencesKey("eveningStartMinutes")
private val KEY_DAILY_AGENDA = booleanPreferencesKey("dailyAgenda")
private val KEY_DAILY_AGENDA_MIN = intPreferencesKey("dailyAgendaMinutes")
private val KEY_OVERDUE_NUDGE = booleanPreferencesKey("overdueNudge")
private val KEY_OVERDUE_NUDGE_MIN = intPreferencesKey("overdueNudgeMinutes")
private val KEY_QUIET = booleanPreferencesKey("quietHours")
private val KEY_QUIET_START = intPreferencesKey("quietStartMinutes")
private val KEY_QUIET_END = intPreferencesKey("quietEndMinutes")
private val KEY_INBOX_REMINDER = booleanPreferencesKey("inboxReminder")
private val KEY_INBOX_REMINDER_DAYS = intPreferencesKey("inboxReminderDays")
private val KEY_INBOX_REMINDER_MIN = intPreferencesKey("inboxReminderMinutes")
private val KEY_LAST_AUTO_BACKUP = androidx.datastore.preferences.core.longPreferencesKey("lastAutoBackupAt")
private val KEY_SHOW_COMPLETED = booleanPreferencesKey("showCompletedInToday")
private val KEY_SEARCH_COMPLETED = booleanPreferencesKey("searchCompleted")
private val KEY_APP_LOCK = booleanPreferencesKey("appLock")
private val KEY_APP_LOCK_GRACE = intPreferencesKey("appLockGraceSeconds")
private val KEY_HIDE_SWITCHER = booleanPreferencesKey("hideInAppSwitcher")
private val KEY_AUTO_BACKUP = booleanPreferencesKey("autoBackup")
private val KEY_TRASH_RETENTION = stringPreferencesKey("trashRetention")
private val KEY_LAUNCH_AT_LOGIN = booleanPreferencesKey("launchAtLogin")
private val KEY_CLOSE_TO_TRAY = booleanPreferencesKey("closeToTray")
private val KEY_ONBOARDING_SEEN = booleanPreferencesKey("onboardingSeen")
private val KEY_UNDATED_TO_SORT = booleanPreferencesKey("undatedToSort")
private val KEY_LAST_TIME_ZONE = androidx.datastore.preferences.core.stringPreferencesKey("lastTimeZone")

class SettingsRepository(
    private val store: DataStore<Preferences>,
    private val cipher: SecretCipher = PlainSecretCipher,
) {
    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            themeMode = p[KEY_THEME]?.let { value -> ThemeMode.entries.firstOrNull { it.name == value } }
                ?: ThemeMode.SYSTEM,
            dynamicColor = p[KEY_DYNAMIC] ?: true,
            firstDayOfWeek = p[KEY_FIRST_DAY]?.let { DayOfWeek(it) } ?: DayOfWeek.MONDAY,
            use24h = p[KEY_24H] ?: false,
            defaultReminderMinutes = p[KEY_REMINDER] ?: DEFAULT_REMINDER_MINUTES,
            aiProvider = p[KEY_AI_PROVIDER]?.let { value ->
                AiProvider.entries.firstOrNull { it.name == value }
            } ?: AiProvider.OFF,
            aiBaseUrl = p[KEY_AI_BASE_URL] ?: "",
            aiModel = p[KEY_AI_MODEL] ?: "",
            aiApiKey = cipher.decrypt(p[KEY_AI_API_KEY] ?: ""),
            aiWorkspaceId = p[KEY_AI_WORKSPACE_ID] ?: "",
            aiRefineMode = p[KEY_AI_REFINE_MODE]?.let { value ->
                AiRefineMode.entries.firstOrNull { it.name == value }
            } ?: AiRefineMode.AUTO,
            aiInstructions = p[KEY_AI_INSTRUCTIONS] ?: "",
            aiAskEnabled = p[KEY_AI_ASK_ENABLED] ?: false,
            aiAskWriteEnabled = p[KEY_AI_ASK_WRITE] ?: false,
            aiWifiOnly = p[KEY_AI_WIFI_ONLY] ?: false,
            agendaRange = p[KEY_AGENDA_RANGE] ?: "DAY",
            notesSort = p[KEY_NOTES_SORT] ?: "EDITED",
            notesLayout = p[KEY_NOTES_LAYOUT] ?: "GRID",
            devOptions = p[KEY_DEV_OPTIONS] ?: false,
            whatsNewSeen = p[KEY_WHATS_NEW_SEEN] ?: "",
            agendaCalendar = p[KEY_AGENDA_CALENDAR] ?: "WEEK",
            openAppTo = p[KEY_OPEN_APP_TO]?.let { v -> OpenAppTo.entries.firstOrNull { it.name == v } }
                ?: OpenAppTo.CAPTURE,
            haptics = p[KEY_HAPTICS] ?: true,
            reduceMotion = p[KEY_REDUCE_MOTION]?.let { v -> ReduceMotionMode.entries.firstOrNull { it.name == v } }
                ?: ReduceMotionMode.SYSTEM,
            contrast = p[KEY_CONTRAST]?.let { v -> ContrastMode.entries.firstOrNull { it.name == v } }
                ?: ContrastMode.STANDARD,
            pureBlack = p[KEY_PURE_BLACK] ?: false,
            quickSettingsTile = p[KEY_QS_TILE] ?: true,
            autoFocusCapture = p[KEY_AUTO_FOCUS_CAPTURE] ?: false,
            keepKeyboardUp = p[KEY_KEEP_KEYBOARD] ?: true,
            voiceCapture = p[KEY_VOICE_CAPTURE] ?: true,
            undoWindowSeconds = p[KEY_UNDO_SECONDS] ?: 5,
            morningStartMinutes = p[KEY_MORNING] ?: (6 * 60),
            afternoonStartMinutes = p[KEY_AFTERNOON] ?: (12 * 60),
            eveningStartMinutes = p[KEY_EVENING] ?: (18 * 60),
            dailyAgenda = p[KEY_DAILY_AGENDA] ?: false,
            dailyAgendaMinutes = p[KEY_DAILY_AGENDA_MIN] ?: (7 * 60 + 30),
            overdueNudge = p[KEY_OVERDUE_NUDGE] ?: false,
            overdueNudgeMinutes = p[KEY_OVERDUE_NUDGE_MIN] ?: (18 * 60),
            quietHours = p[KEY_QUIET] ?: false,
            undatedToSort = p[KEY_UNDATED_TO_SORT] ?: true,
            quietStartMinutes = p[KEY_QUIET_START] ?: (22 * 60),
            quietEndMinutes = p[KEY_QUIET_END] ?: (7 * 60),
            inboxReminder = p[KEY_INBOX_REMINDER] ?: false,
            inboxReminderDays = p[KEY_INBOX_REMINDER_DAYS] ?: 3,
            inboxReminderMinutes = p[KEY_INBOX_REMINDER_MIN] ?: 9 * 60,
            lastAutoBackupAt = p[KEY_LAST_AUTO_BACKUP] ?: 0L,
            showCompletedInToday = p[KEY_SHOW_COMPLETED] ?: true,
            searchCompleted = p[KEY_SEARCH_COMPLETED] ?: true,
            appLock = p[KEY_APP_LOCK] ?: false,
            appLockGraceSeconds = p[KEY_APP_LOCK_GRACE] ?: 60,
            hideInAppSwitcher = p[KEY_HIDE_SWITCHER] ?: false,
            autoBackup = p[KEY_AUTO_BACKUP] ?: false,
            trashRetention = p[KEY_TRASH_RETENTION]?.let { v -> TrashRetention.entries.firstOrNull { it.name == v } }
                ?: TrashRetention.DAYS_30,
            launchAtLogin = p[KEY_LAUNCH_AT_LOGIN] ?: false,
            closeToTray = p[KEY_CLOSE_TO_TRAY] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[KEY_THEME] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = store.edit { it[KEY_DYNAMIC] = enabled }
    suspend fun setFirstDayOfWeek(day: DayOfWeek) = store.edit { it[KEY_FIRST_DAY] = day.isoDayNumber }
    suspend fun setUse24h(enabled: Boolean) = store.edit { it[KEY_24H] = enabled }
    suspend fun setDefaultReminderMinutes(minutes: Int) = store.edit { it[KEY_REMINDER] = minutes }

    suspend fun setAiProvider(provider: AiProvider) = store.edit { it[KEY_AI_PROVIDER] = provider.name }

    /** Atomic provider switch so the defaults arrive in the same emission as the provider. */
    suspend fun setAiProviderApplyingDefaults(provider: AiProvider, baseUrl: String?, model: String?) =
        store.edit { p ->
            p[KEY_AI_PROVIDER] = provider.name
            baseUrl?.let { p[KEY_AI_BASE_URL] = it }
            model?.let { p[KEY_AI_MODEL] = it }
        }
    suspend fun setAiBaseUrl(url: String) = store.edit { it[KEY_AI_BASE_URL] = url.trim() }
    suspend fun setAiModel(model: String) = store.edit { it[KEY_AI_MODEL] = model.trim() }
    suspend fun setAiApiKey(key: String) = store.edit { it[KEY_AI_API_KEY] = cipher.encrypt(key.trim()) }
    suspend fun setAiWorkspaceId(id: String) = store.edit { it[KEY_AI_WORKSPACE_ID] = id.trim() }
    suspend fun setAiRefineMode(mode: AiRefineMode) = store.edit { it[KEY_AI_REFINE_MODE] = mode.name }
    suspend fun setAiInstructions(text: String) = store.edit { it[KEY_AI_INSTRUCTIONS] = text }
    suspend fun setAiAskEnabled(enabled: Boolean) = store.edit { it[KEY_AI_ASK_ENABLED] = enabled }
    suspend fun setAiAskWriteEnabled(enabled: Boolean) = store.edit { it[KEY_AI_ASK_WRITE] = enabled }
    suspend fun setAiWifiOnly(enabled: Boolean) = store.edit { it[KEY_AI_WIFI_ONLY] = enabled }
    suspend fun setOpenAppTo(value: OpenAppTo) = store.edit { it[KEY_OPEN_APP_TO] = value.name }
    suspend fun setAgendaRange(value: String) = store.edit { it[KEY_AGENDA_RANGE] = value }
    suspend fun setAgendaCalendar(value: String) = store.edit { it[KEY_AGENDA_CALENDAR] = value }
    suspend fun setNotesSort(value: String) = store.edit { it[KEY_NOTES_SORT] = value }
    suspend fun setNotesLayout(value: String) = store.edit { it[KEY_NOTES_LAYOUT] = value }
    suspend fun setDevOptions(enabled: Boolean) = store.edit { it[KEY_DEV_OPTIONS] = enabled }
    suspend fun setWhatsNewSeen(version: String) = store.edit { it[KEY_WHATS_NEW_SEEN] = version }
    suspend fun setHaptics(enabled: Boolean) = store.edit { it[KEY_HAPTICS] = enabled }
    suspend fun setReduceMotion(mode: ReduceMotionMode) = store.edit { it[KEY_REDUCE_MOTION] = mode.name }
    suspend fun setContrast(mode: ContrastMode) = store.edit { it[KEY_CONTRAST] = mode.name }
    suspend fun setPureBlack(enabled: Boolean) = store.edit { it[KEY_PURE_BLACK] = enabled }
    suspend fun setQuickSettingsTile(enabled: Boolean) = store.edit { it[KEY_QS_TILE] = enabled }
    suspend fun setAutoFocusCapture(enabled: Boolean) = store.edit { it[KEY_AUTO_FOCUS_CAPTURE] = enabled }
    suspend fun setKeepKeyboardUp(enabled: Boolean) = store.edit { it[KEY_KEEP_KEYBOARD] = enabled }
    suspend fun setVoiceCapture(enabled: Boolean) = store.edit { it[KEY_VOICE_CAPTURE] = enabled }
    suspend fun setUndoWindowSeconds(seconds: Int) = store.edit { it[KEY_UNDO_SECONDS] = seconds }
    suspend fun setMorningStart(minutes: Int) = store.edit { it[KEY_MORNING] = minutes }
    suspend fun setAfternoonStart(minutes: Int) = store.edit { it[KEY_AFTERNOON] = minutes }
    suspend fun setEveningStart(minutes: Int) = store.edit { it[KEY_EVENING] = minutes }
    suspend fun setDailyAgenda(enabled: Boolean) = store.edit { it[KEY_DAILY_AGENDA] = enabled }
    suspend fun setDailyAgendaMinutes(minutes: Int) = store.edit { it[KEY_DAILY_AGENDA_MIN] = minutes }
    suspend fun setOverdueNudge(enabled: Boolean) = store.edit { it[KEY_OVERDUE_NUDGE] = enabled }
    suspend fun setOverdueNudgeMinutes(minutes: Int) = store.edit { it[KEY_OVERDUE_NUDGE_MIN] = minutes }
    suspend fun setQuietHours(enabled: Boolean) = store.edit { it[KEY_QUIET] = enabled }
    suspend fun setUndatedToSort(enabled: Boolean) = store.edit { it[KEY_UNDATED_TO_SORT] = enabled }
    suspend fun setQuietStartMinutes(minutes: Int) = store.edit { it[KEY_QUIET_START] = minutes }
    suspend fun setQuietEndMinutes(minutes: Int) = store.edit { it[KEY_QUIET_END] = minutes }
    suspend fun setInboxReminder(enabled: Boolean) = store.edit { it[KEY_INBOX_REMINDER] = enabled }
    suspend fun setInboxReminderDays(days: Int) = store.edit { it[KEY_INBOX_REMINDER_DAYS] = days }
    suspend fun setInboxReminderMinutes(minutes: Int) = store.edit { it[KEY_INBOX_REMINDER_MIN] = minutes }
    suspend fun setLastAutoBackupAt(millis: Long) = store.edit { it[KEY_LAST_AUTO_BACKUP] = millis }
    suspend fun setShowCompletedInToday(enabled: Boolean) = store.edit { it[KEY_SHOW_COMPLETED] = enabled }
    suspend fun setSearchCompleted(enabled: Boolean) = store.edit { it[KEY_SEARCH_COMPLETED] = enabled }
    suspend fun setAppLock(enabled: Boolean) = store.edit { it[KEY_APP_LOCK] = enabled }
    suspend fun setAppLockGrace(seconds: Int) = store.edit { it[KEY_APP_LOCK_GRACE] = seconds }
    suspend fun setHideInAppSwitcher(enabled: Boolean) = store.edit { it[KEY_HIDE_SWITCHER] = enabled }
    suspend fun setAutoBackup(enabled: Boolean) = store.edit { it[KEY_AUTO_BACKUP] = enabled }
    suspend fun setTrashRetention(value: TrashRetention) = store.edit { it[KEY_TRASH_RETENTION] = value.name }
    suspend fun setLaunchAtLogin(enabled: Boolean) = store.edit { it[KEY_LAUNCH_AT_LOGIN] = enabled }
    suspend fun setCloseToTray(enabled: Boolean) = store.edit { it[KEY_CLOSE_TO_TRAY] = enabled }

    /** True once the first-run cards were seen or skipped. Not part of [settings]: it is not a preference. */
    val onboardingSeen: Flow<Boolean> = store.data.map { it[KEY_ONBOARDING_SEEN] ?: false }
    suspend fun setOnboardingSeen() = store.edit { it[KEY_ONBOARDING_SEEN] = true }
    suspend fun resetOnboarding() = store.edit { it[KEY_ONBOARDING_SEEN] = false }

    /** The zone all-day events were last anchored in; see [syncTimeZone]. */
    suspend fun lastTimeZoneId(): String? = store.data.first()[KEY_LAST_TIME_ZONE]
    suspend fun setLastTimeZoneId(id: String) = store.edit { it[KEY_LAST_TIME_ZONE] = id }

    /** One atomic write restoring a backed-up settings block. */
    suspend fun applyBackup(s: BackupSettings) = store.edit { p ->
        p[KEY_THEME] = s.themeMode
        p[KEY_DYNAMIC] = s.dynamicColor
        p[KEY_FIRST_DAY] = s.firstDayOfWeekIso
        p[KEY_24H] = s.use24h
        p[KEY_REMINDER] = s.defaultReminderMinutes
        p[KEY_AI_PROVIDER] = s.aiProvider
        p[KEY_AI_BASE_URL] = s.aiBaseUrl
        p[KEY_AI_MODEL] = s.aiModel
        // the API key is deliberately not part of a backup; it stays where it was
        p[KEY_AI_WORKSPACE_ID] = s.aiWorkspaceId
        p[KEY_AI_REFINE_MODE] = s.aiRefineMode
        p[KEY_AI_INSTRUCTIONS] = s.aiInstructions
        p[KEY_AI_ASK_ENABLED] = s.aiAskEnabled
        p[KEY_AI_ASK_WRITE] = s.aiAskWriteEnabled
        p[KEY_AI_WIFI_ONLY] = s.aiWifiOnly
        p[KEY_OPEN_APP_TO] = s.openAppTo
        p[KEY_HAPTICS] = s.haptics
        p[KEY_REDUCE_MOTION] = s.reduceMotion
        p[KEY_CONTRAST] = s.contrast
        p[KEY_PURE_BLACK] = s.pureBlack
        p[KEY_QS_TILE] = s.quickSettingsTile
        p[KEY_AUTO_FOCUS_CAPTURE] = s.autoFocusCapture
        p[KEY_KEEP_KEYBOARD] = s.keepKeyboardUp
        p[KEY_VOICE_CAPTURE] = s.voiceCapture
        p[KEY_UNDO_SECONDS] = s.undoWindowSeconds
        p[KEY_MORNING] = s.morningStartMinutes
        p[KEY_AFTERNOON] = s.afternoonStartMinutes
        p[KEY_EVENING] = s.eveningStartMinutes
        p[KEY_DAILY_AGENDA] = s.dailyAgenda
        p[KEY_DAILY_AGENDA_MIN] = s.dailyAgendaMinutes
        p[KEY_OVERDUE_NUDGE] = s.overdueNudge
        p[KEY_OVERDUE_NUDGE_MIN] = s.overdueNudgeMinutes
        p[KEY_INBOX_REMINDER] = s.inboxReminder
        p[KEY_INBOX_REMINDER_DAYS] = s.inboxReminderDays
        p[KEY_SHOW_COMPLETED] = s.showCompletedInToday
        p[KEY_SEARCH_COMPLETED] = s.searchCompleted
        p[KEY_APP_LOCK] = s.appLock
        p[KEY_APP_LOCK_GRACE] = s.appLockGraceSeconds
        p[KEY_HIDE_SWITCHER] = s.hideInAppSwitcher
        p[KEY_AUTO_BACKUP] = s.autoBackup
        p[KEY_TRASH_RETENTION] = s.trashRetention
        p[KEY_LAUNCH_AT_LOGIN] = s.launchAtLogin
        p[KEY_CLOSE_TO_TRAY] = s.closeToTray
    }
}
