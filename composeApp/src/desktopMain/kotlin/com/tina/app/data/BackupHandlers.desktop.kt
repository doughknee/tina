package com.tina.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
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
    val repository = koinInject<ItemRepository>()
    val backups = koinInject<BackupService>()
    val settingsRepository = koinInject<SettingsRepository>()
    val scope = rememberCoroutineScope()

    return remember {
        BackupHandlers(
            export = {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val dialog = FileDialog(null as Frame?, "", FileDialog.SAVE).apply {
                    file = "tina-backup-$today.json"
                    isVisible = true
                }
                val directory = dialog.directory
                val fileName = dialog.file
                if (directory != null && fileName != null) {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val settings = settingsRepository.settings.first().toBackupSettings()
                            File(directory, fileName).writeText(backups.exportJson(settings))
                        }
                        onExported()
                    }
                }
            },
            restore = {
                val dialog = FileDialog(null as Frame?, "", FileDialog.LOAD).apply { isVisible = true }
                val directory = dialog.directory
                val fileName = dialog.file
                if (directory != null && fileName != null) {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            backups.importJson(File(directory, fileName).readText())
                        }
                        onImported(result)
                    }
                }
            },
        )
    }
}
