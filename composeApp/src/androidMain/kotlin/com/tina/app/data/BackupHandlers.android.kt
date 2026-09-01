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
    onImported: (Int) -> Unit,
): BackupHandlers {
    val context = LocalContext.current
    val repository = koinInject<ItemRepository>()
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
                    stream.write(repository.exportJson(settings).encodeToByteArray())
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
            val count = withContext(Dispatchers.IO) {
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().decodeToString()
                }
                val backup = text?.let(::decodeBackup)
                if (backup == null) -1 else {
                    backup.settings?.let { settingsRepository.applyBackup(it) }
                    repository.importJson(text)
                }
            }
            onImported(count)
        }
    }

    return remember {
        BackupHandlers(
            export = {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                exportLauncher.launch("tina-backup-$today.json")
            },
            restore = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
        )
    }
}
