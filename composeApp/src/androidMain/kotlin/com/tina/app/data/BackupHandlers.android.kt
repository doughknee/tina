package com.tina.app.data

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

@Composable
actual fun rememberBackupHandlers(
    onExported: () -> Unit,
    onImported: (ImportResult?) -> Unit,
): BackupHandlers {
    val context = LocalContext.current
    val repository = koinInject<ItemRepository>()
    val backups = koinInject<BackupService>()
    val settingsRepository = koinInject<SettingsRepository>()
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) {
                val settings = settingsRepository.settings.first().toBackupSettings()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(backups.exportJson(settings).encodeToByteArray())
                }
            }
            onExported()
        }
    }

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/calendar"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(backups.exportIcs().encodeToByteArray())
                }
            }
            onExported()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().decodeToString()
                }
                text?.let { backups.importJson(it) }
            }
            onImported(result)
        }
    }

    return remember {
        BackupHandlers(
            export = {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                exportLauncher.launch("peggy-backup-$today.json")
            },
            restore = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
            exportCalendar = {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                calendarLauncher.launch("peggy-calendar-$today.ics")
            },
        )
    }
}
