package com.tina.app.notifications

import android.Manifest
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
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val REMINDER_CHANNEL_ID = "reminders"

fun ensureReminderChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
        NotificationChannel(
            REMINDER_CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.reminder_channel_desc) },
    )
}

class ReminderReceiver : BroadcastReceiver(), KoinComponent {
    private val repository: ItemRepository by inject()
    private val scheduler: ReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        if (itemId < 0) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_DONE -> {
                        repository.get(itemId)?.let { item ->
                            if (item.type == ItemType.TASK) repository.complete(itemId)
                        }
                        NotificationManagerCompat.from(context).cancel(itemId.toInt())
                    }
                    ACTION_SNOOZE -> {
                        val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                        NotificationManagerCompat.from(context).cancel(itemId.toInt())
                        (scheduler as AndroidReminderScheduler).scheduleExactAt(
                            System.currentTimeMillis() + minutes * 60_000L,
                            firePendingIntent(context, itemId),
                        )
                    }
                    else -> {
                        repository.get(itemId)?.let { item ->
                            val remindable = item.type == ItemType.TASK || item.type == ItemType.EVENT
                            if (!item.completed && remindable && item.reminderOffsetMinutes != null) {
                                showReminderNotification(context, item)
                            }
                            // recurring events arm their next occurrence
                            if (item.type == ItemType.EVENT && item.recurrence != null) scheduler.schedule(item)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

private fun actionIntent(context: Context, itemId: Long, action: String, requestOffset: Int, snoozeMinutes: Int = 0): PendingIntent =
    PendingIntent.getBroadcast(
        context,
        itemId.toInt() * 4 + requestOffset,
        Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_ITEM_ID, itemId)
            .putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

fun showReminderNotification(context: Context, item: Item) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    ensureReminderChannel(context)
    val contentIntent = PendingIntent.getActivity(
        context,
        item.id.toInt(),
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_capture)
        .setContentTitle(item.title)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .setContentIntent(contentIntent)
    item.body?.let { com.tina.app.notes.htmlPreview(it) }?.takeIf { it.isNotBlank() }?.let {
        builder.setContentText(it.take(120))
    }
    if (item.type == ItemType.TASK) {
        builder.addAction(0, context.getString(R.string.action_done), actionIntent(context, item.id, ACTION_DONE, 1))
    }
    builder.addAction(
        0,
        context.getString(R.string.action_snooze_10),
        actionIntent(context, item.id, ACTION_SNOOZE, 2, snoozeMinutes = 10),
    )
    builder.addAction(
        0,
        context.getString(R.string.action_snooze_60),
        actionIntent(context, item.id, ACTION_SNOOZE, 3, snoozeMinutes = 60),
    )
    try {
        NotificationManagerCompat.from(context).notify(item.id.toInt(), builder.build())
    } catch (_: SecurityException) {
        // permission revoked between check and notify; nothing to do
    }
}
