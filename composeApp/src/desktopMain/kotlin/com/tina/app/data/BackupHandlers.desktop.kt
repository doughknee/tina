package com.tina.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
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
    val repository = koinInject<ItemRepository>()
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
                            File(directory, fileName).writeText(repository.exportJson())
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
                        val count = withContext(Dispatchers.IO) {
                            val text = File(directory, fileName).readText()
                            if (decodeBackup(text) == null) -1 else repository.importJson(text)
                        }
                        onImported(count)
                    }
                }
            },
        )
    }
}
