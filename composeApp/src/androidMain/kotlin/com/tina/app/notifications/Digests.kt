package com.tina.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tina.app.MainActivity
import com.tina.app.R
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import com.tina.app.data.Settings
import com.tina.app.data.SettingsRepository
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val DIGEST_CHANNEL_ID = "digests"

const val ACTION_DAILY_AGENDA = "com.tina.app.DIGEST_AGENDA"
const val ACTION_OVERDUE_NUDGE = "com.tina.app.DIGEST_OVERDUE"
const val ACTION_INBOX_REMINDER = "com.tina.app.DIGEST_INBOX"

private const val AGENDA_ID = 900_001
private const val OVERDUE_ID = 900_002
private const val INBOX_ID = 900_003

fun ensureDigestChannel(context: Context) {
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(
            DIGEST_CHANNEL_ID,
            context.getString(R.string.digest_channel_name),
            // digests are summaries, not alarms — quieter than reminders by design
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.digest_channel_desc) },
    )
}

private fun digestIntent(context: Context, action: String, requestCode: Int): PendingIntent =
    PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, DigestReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

/** Next occurrence of [minuteOfDay], today if still ahead, otherwise tomorrow. */
private fun nextDailyTrigger(minuteOfDay: Int, tz: TimeZone): Long {
    val now = Clock.System.now()
    val today = now.toLocalDateTime(tz).date
    val target = LocalDateTime(today, LocalTime(minuteOfDay / 60, minuteOfDay % 60))
    val instant = target.toInstant(tz)
    return if (instant > now) instant.toEpochMilliseconds()
    else LocalDateTime(today.plus(1, DateTimeUnit.DAY), target.time).toInstant(tz).toEpochMilliseconds()
}

/**
 * Digests are daily summaries, so they use inexact alarms — the exact-alarm budget
 * is reserved for item reminders the user actually set.
 */
object DigestScheduler {
    fun sync(context: Context, settings: Settings) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val tz = TimeZone.currentSystemDefault()

        fun apply(enabled: Boolean, minutes: Int, action: String, requestCode: Int) {
            val pending = digestIntent(context, action, requestCode)
            if (!enabled) {
                alarmManager.cancel(pending)
                return
            }
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextDailyTrigger(minutes, tz),
                pending,
            )
        }

        apply(settings.dailyAgenda, settings.dailyAgendaMinutes, ACTION_DAILY_AGENDA, AGENDA_ID)
        apply(settings.overdueNudge, settings.overdueNudgeMinutes, ACTION_OVERDUE_NUDGE, OVERDUE_ID)
        // the inbox check rides the agenda time; it only fires when something is stale
        apply(settings.inboxReminder, settings.dailyAgendaMinutes, ACTION_INBOX_REMINDER, INBOX_ID)
    }
}

class DigestReceiver : BroadcastReceiver(), KoinComponent {
    private val repository: ItemRepository by inject()
    private val settingsRepository: SettingsRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = settingsRepository.settings.first()
                val tz = TimeZone.currentSystemDefault()
                val today = Clock.System.now().toLocalDateTime(tz).date
                val todayEpoch = today.toEpochDays().toInt()
                val items = repository.allItems()

                when (action) {
                    ACTION_DAILY_AGENDA -> {
                        val due = items.filter {
                            !it.completed && it.type == ItemType.TASK && it.dueDate == todayEpoch
                        }
                        val dayStart = today.atStartOfDayIn(tz).toEpochMilliseconds()
                        val dayEnd = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()
                        // today's events only, recurring ones by their occurrence today
                        val events = items.filter { e ->
                            e.type == ItemType.EVENT && e.startAt != null && !e.completed &&
                                com.tina.app.data.expandOccurrences(e.startAt!!, e.recurrence, dayStart, dayEnd, tz).any()
                        }
                        val lines = (due.map { it.title } + events.map { it.title }).take(5)
                        if (lines.isNotEmpty()) {
                            notify(
                                context,
                                AGENDA_ID,
                                context.getString(R.string.digest_agenda_title),
                                lines.joinToString(" · "),
                            )
                        }
                    }
                    ACTION_OVERDUE_NUDGE -> {
                        val overdue = items.filter {
                            !it.completed && it.type == ItemType.TASK &&
                                it.dueDate != null && it.dueDate!! < todayEpoch
                        }
                        if (overdue.isNotEmpty()) {
                            notify(
                                context,
                                OVERDUE_ID,
                                context.getString(R.string.digest_overdue_title, overdue.size),
                                overdue.take(5).joinToString(" · ") { it.title },
                            )
                        }
                    }
                    ACTION_INBOX_REMINDER -> {
                        val cutoff = Clock.System.now().toEpochMilliseconds() -
                            settings.inboxReminderDays * 24L * 60 * 60 * 1000
                        val stale = items.filter { it.type == ItemType.INBOX && it.createdAt < cutoff }
                        if (stale.isNotEmpty()) {
                            notify(
                                context,
                                INBOX_ID,
                                context.getString(R.string.digest_inbox_title, stale.size),
                                context.getString(R.string.digest_inbox_body),
                            )
                        }
                    }
                }
                // daily alarms are one-shot; arm tomorrow's before finishing
                DigestScheduler.sync(context, settings)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun notify(context: Context, id: Int, title: String, body: String, openSort: Boolean = id == OVERDUE_ID) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureDigestChannel(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(MainActivity.EXTRA_OPEN_SORT, openSort),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, DIGEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
