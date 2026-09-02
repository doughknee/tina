package com.tina.app.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val AUTO_BACKUP_WORK = "tina-auto-backup"
private const val KEEP_BACKUPS = 4

/** Where silent backups live: app-private storage, so no storage permission is involved. */
fun autoBackupDir(context: Context): File =
    File(context.filesDir, "backups").apply { mkdirs() }

/**
 * Weekly silent export. Deliberately app-private and capped at [KEEP_BACKUPS] — this is a
 * safety net against accidents, not a substitute for the user's own exported file.
 */
class AutoBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {
    private val backups: BackupService by inject()
    private val settingsRepository: SettingsRepository by inject()

    override suspend fun doWork(): Result {
        return runCatching {
            val settings = settingsRepository.settings.first()
            if (!settings.autoBackup) return Result.success()

            val today = kotlin.time.Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            val dir = autoBackupDir(applicationContext)
            // temp file then rename: a crash mid-write must never leave a truncated backup in the set
            val target = File(dir, "peggy-auto-$today.json")
            val temp = File(dir, "tina-auto-$today.json.tmp")
            temp.writeText(backups.exportJson(settings.toBackupSettings()))
            if (!temp.renameTo(target)) { target.delete(); temp.renameTo(target) }

            dir.listFiles()
                ?.filter { it.extension == "json" }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(KEEP_BACKUPS)
                ?.forEach { it.delete() }

            Result.success()
        }.getOrElse { Result.retry() }
    }
}

object AutoBackupScheduler {
    fun sync(context: Context, enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        if (!enabled) {
            manager.cancelUniqueWork(AUTO_BACKUP_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build(),
            )
            .build()
        manager.enqueueUniquePeriodicWork(
            AUTO_BACKUP_WORK,
            // UPDATE so a changed period or constraint reaches installs that already enqueued the job
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
