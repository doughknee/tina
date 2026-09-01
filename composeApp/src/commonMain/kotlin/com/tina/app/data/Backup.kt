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
