package com.tina.app.data

import androidx.compose.runtime.Composable

class BackupHandlers(val export: () -> Unit, val restore: () -> Unit)

/**
 * Platform file-picker plumbing for backup/restore. `onImported` receives the
 * imported item count, or -1 when the chosen file wasn't a tina backup.
 */
@Composable
expect fun rememberBackupHandlers(
    onExported: () -> Unit,
    onImported: (Int) -> Unit,
): BackupHandlers
