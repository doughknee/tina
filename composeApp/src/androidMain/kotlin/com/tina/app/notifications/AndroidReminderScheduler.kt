package com.tina.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.tina.app.data.Item
import kotlinx.datetime.TimeZone

const val EXTRA_ITEM_ID = "itemId"
const val EXTRA_SNOOZE_MINUTES = "snoozeMinutes"
const val ACTION_FIRE = "com.tina.app.REMINDER_FIRE"
const val ACTION_DONE = "com.tina.app.REMINDER_DONE"
const val ACTION_SNOOZE = "com.tina.app.REMINDER_SNOOZE"

internal fun firePendingIntent(context: Context, itemId: Long): PendingIntent =
    PendingIntent.getBroadcast(
        context,
        itemId.toInt(),
        Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_ITEM_ID, itemId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(item: Item) {
        val at = nextReminderTime(item, System.currentTimeMillis(), TimeZone.currentSystemDefault())
        val pendingIntent = firePendingIntent(context, item.id)
        if (at == null) {
            alarmManager.cancel(pendingIntent)
            return
        }
        scheduleExactAt(at, pendingIntent)
    }

    override fun cancel(itemId: Long) {
        alarmManager.cancel(firePendingIntent(context, itemId))
    }

    internal fun scheduleExactAt(at: Long, pendingIntent: PendingIntent) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
        } else {
            // user has not granted exact alarms yet; inexact beats nothing
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
        }
    }
}
