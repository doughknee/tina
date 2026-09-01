package com.tina.app.data

import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupSettings(
    val themeMode: String = "SYSTEM",
    val dynamicColor: Boolean = true,
    val firstDayOfWeekIso: Int = 1,
    val use24h: Boolean = false,
    val defaultReminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
    val aiProvider: String = "OFF",
    val aiBaseUrl: String = "",
    val aiModel: String = "",
    val aiApiKey: String = "",
    val aiWorkspaceId: String = "",
    val aiRefineMode: String = "AUTO",
    val aiInstructions: String = "",
    val aiAskEnabled: Boolean = false,
    val aiAskWriteEnabled: Boolean = false,
    val aiWifiOnly: Boolean = false,
    val openAppTo: String = "CAPTURE",
    val haptics: Boolean = true,
    val reduceMotion: String = "SYSTEM",
    val contrast: String = "STANDARD",
    val pureBlack: Boolean = false,
    val quickSettingsTile: Boolean = true,
    val autoFocusCapture: Boolean = false,
    val keepKeyboardUp: Boolean = true,
    val voiceCapture: Boolean = true,
    val undoWindowSeconds: Int = 5,
    val morningStartMinutes: Int = 360,
    val afternoonStartMinutes: Int = 720,
    val eveningStartMinutes: Int = 1080,
    val dailyAgenda: Boolean = false,
    val dailyAgendaMinutes: Int = 450,
    val overdueNudge: Boolean = false,
    val overdueNudgeMinutes: Int = 1080,
    val inboxReminder: Boolean = false,
    val inboxReminderDays: Int = 3,
    val showCompletedInToday: Boolean = true,
    val searchCompleted: Boolean = true,
    val appLock: Boolean = false,
    val appLockGraceSeconds: Int = 60,
    val hideInAppSwitcher: Boolean = false,
    val autoBackup: Boolean = false,
    val trashRetention: String = "DAYS_30",
    val launchAtLogin: Boolean = false,
    val closeToTray: Boolean = true,
)

fun Settings.toBackupSettings(): BackupSettings = BackupSettings(
    themeMode = themeMode.name,
    dynamicColor = dynamicColor,
    firstDayOfWeekIso = firstDayOfWeek.isoDayNumber,
    use24h = use24h,
    defaultReminderMinutes = defaultReminderMinutes,
    aiProvider = aiProvider.name,
    aiBaseUrl = aiBaseUrl,
    aiModel = aiModel,
    aiApiKey = aiApiKey,
    aiWorkspaceId = aiWorkspaceId,
    aiRefineMode = aiRefineMode.name,
    aiInstructions = aiInstructions,
    aiAskEnabled = aiAskEnabled,
    aiAskWriteEnabled = aiAskWriteEnabled,
    aiWifiOnly = aiWifiOnly,
    openAppTo = openAppTo.name,
    haptics = haptics,
    reduceMotion = reduceMotion.name,
    contrast = contrast.name,
    pureBlack = pureBlack,
    quickSettingsTile = quickSettingsTile,
    autoFocusCapture = autoFocusCapture,
    keepKeyboardUp = keepKeyboardUp,
    voiceCapture = voiceCapture,
    undoWindowSeconds = undoWindowSeconds,
    morningStartMinutes = morningStartMinutes,
    afternoonStartMinutes = afternoonStartMinutes,
    eveningStartMinutes = eveningStartMinutes,
    dailyAgenda = dailyAgenda,
    dailyAgendaMinutes = dailyAgendaMinutes,
    overdueNudge = overdueNudge,
    overdueNudgeMinutes = overdueNudgeMinutes,
    inboxReminder = inboxReminder,
    inboxReminderDays = inboxReminderDays,
    showCompletedInToday = showCompletedInToday,
    searchCompleted = searchCompleted,
    appLock = appLock,
    appLockGraceSeconds = appLockGraceSeconds,
    hideInAppSwitcher = hideInAppSwitcher,
    autoBackup = autoBackup,
    trashRetention = trashRetention.name,
    launchAtLogin = launchAtLogin,
    closeToTray = closeToTray,
)

@Serializable
data class Backup(
    val version: Int = 1,
    val exportedAt: Long,
    val items: List<Item>,
    // absent in v1.0 backups; import tolerates both directions
    val settings: BackupSettings? = null,
)

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
}

fun encodeBackup(items: List<Item>, exportedAt: Long, settings: BackupSettings? = null): String =
    json.encodeToString(Backup(exportedAt = exportedAt, items = items, settings = settings))

/** Returns null if the file isn't a tina backup. */
fun decodeBackup(text: String): Backup? = runCatching { json.decodeFromString<Backup>(text) }.getOrNull()
