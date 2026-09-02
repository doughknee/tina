package com.tina.app.data

import androidx.compose.runtime.Composable

class BackupHandlers(val export: () -> Unit, val restore: () -> Unit)

/**
 * Platform file-picker plumbing for backup/restore. `onImported` receives what was
 * imported, or null when the chosen file wasn't a tina backup this build can read.
 */
@Composable
expect fun rememberBackupHandlers(
    onExported: () -> Unit,
    onImported: (ImportResult?) -> Unit,
): BackupHandlers
