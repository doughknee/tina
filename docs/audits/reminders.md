# Audit: reminders, notifications, background work

Read-only audit performed 2026-09-02 against v1.6.0 (`aaf28e5`). Paths relative to `composeApp/src/`.

## 1. End-to-end scheduling

**API: AlarmManager only.** WorkManager is used solely for auto-backup (`androidMain/.../data/AutoBackup.kt`). One alarm slot per item.

Path: repo mutation → `ItemRepository` → `ReminderScheduler.schedule(item)` (`commonMain/.../data/ItemRepository.kt:88,95,112,140,145,150,154`) → `AndroidReminderScheduler.schedule` (`androidMain/.../notifications/AndroidReminderScheduler.kt:29`) → `nextReminderTime` (`commonMain/.../notifications/Reminders.kt:28`) → `scheduleExactAt` (`AndroidReminderScheduler.kt:43`):
- `canScheduleExactAlarms()` true → `setExactAndAllowWhileIdle(RTC_WAKEUP, …)`
- false → `setAndAllowWhileIdle(RTC_WAKEUP, …)`
- `at == null` → `alarmManager.cancel(pendingIntent)`

PendingIntent: `getBroadcast`, requestCode `itemId.toInt()`, action `ACTION_FIRE`. Fires → `ReminderReceiver.onReceive`, `goAsync()` + ad-hoc IO scope, re-reads the item, posts a notification if `!completed && (TASK||EVENT) && reminderOffsetMinutes != null`.

**Trigger time** (`Reminders.kt:28-47`): TASK = `dueLocalDate + dueLocalTime - offset`, must be `> now`; EVENT = first occurrence from `expandOccurrences(start, recurrence, now+offset, now+2y, tz)` minus offset. INBOX/NOTE → null.

**Boot**: `BootReceiver` (BOOT_COMPLETED only) calls `repository.rescheduleAllReminders()` → `dao.getRemindable()`. Digest alarms are re-armed only as a side effect of the process starting (`TinaApp.onCreate` settings collector → `DigestScheduler.sync`).

**App update (MY_PACKAGE_REPLACED)**: not handled. Reminders due between the update and the next launch are lost.

**Time-zone / DST / TIME_SET**: not handled. Alarms are absolute instants computed at scheduling time, so a task due "09:00" fires at the old instant after travel or a DST shift.

**Item lifecycle**: insert/update/delete/restore/complete/uncomplete/changeType/reschedule all (re)schedule or cancel correctly. `purge`, `emptyTrash`, `purgeExpiredTrash`, `deleteEverything` (`ItemRepository.kt:71,123,125,128-131`) do **not** cancel alarms.

**Recurring items after a fire**: only EVENTs re-arm (`ReminderReceiver.kt:71`). Recurring TASKs get exactly one reminder ever; nothing advances `dueDate` on completion.

**Desktop**: `NoopReminderScheduler` — no reminders at all, no UI saying so.

## 2. Permissions

- Manifest declares `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`. `USE_EXACT_ALARM` is **not** declared.
- `targetSdk 36`: `SCHEDULE_EXACT_ALARM` is denied by default, so every fresh install runs on inexact alarms until the user finds the banner on the agenda screen and opens special access.
- The banner (`ReminderPermissionBanner.android.kt`) requests `POST_NOTIFICATIONS` and deep-links to `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`; it re-checks on resume but **never reschedules** — alarms armed before the grant stay inexact until the next cold start.
- Not used: `setAlarmClock()` (Doze-exempt, no permission needed), battery-optimisation prompt, full-screen intent.

## 3. Digests

`DigestScheduler.sync` (`Digests.kt:79-102`) arms three inexact daily alarms (agenda, overdue nudge, inbox reminder). Called from `TinaApp` on settings changes and re-armed after each delivery.

- **Agenda**: today's tasks plus **every event with a non-null `startAt` regardless of date** (`Digests.kt:124`); `take(5)` then pushes real items out.
- **Overdue**: correct, but its time (`overdueNudgeMinutes`) is unreachable in Settings — `TimeTarget.OVERDUE_NUDGE` exists but no row calls it.
- **Inbox**: fires at the agenda time, even when the agenda toggle is off; up to three ungrouped notifications land at once.
- **Quiet hours / DND**: nonexistent.
- Missed day: an inexact alarm that slips past midnight skips that day's digest.

## 4. Bugs and gaps, ranked

**P0**
1. Recurring tasks never re-remind (`Reminders.kt:31-38`, `ReminderReceiver.kt:71`).
2. Exact alarms off by default on the shipped target SDK (`AndroidManifest.xml:8`, `AndroidReminderScheduler.kt:48`).
3. Granting the permission does not upgrade existing alarms (`ReminderPermissionBanner.android.kt:57-60`).
4. No time-zone / DST reschedule (no `TIMEZONE_CHANGED` receiver).
5. App update loses reminders until next launch (no `MY_PACKAGE_REPLACED` receiver).
6. Tapping a reminder does not open the item (`ReminderReceiver.kt:100-105`, `Digests.kt:178-183`).

**P1**
7. Daily agenda lists the wrong events (`Digests.kt:124` has no date bound).
8. Overdue-nudge time unreachable in the UI (`SettingsScreen.kt:354,366` dead branch).
9. "Sound & vibration" row is a dead no-op (`SettingsScreen.kt:895-903`); channels set no sound/vibration.
10. Missing alarm cancel on hard-delete paths (`ItemRepository.kt:71,123,125,128-131`).
11. Recurring-event re-arm is off-by-a-millisecond fragile (`Reminders.kt:40-43`): an on-time delivery returns the current occurrence and cancels the series.
12. Cold-start/receiver race between `TinaApp.rescheduleAllReminders()` and the receiver's own re-arm.
13. No notification grouping or summary.
14. Completing from a notification leaves the widget stale (`ReminderReceiver.kt:50-55` never calls `TodayWidget().updateAll`).

**P2**
15. `ReminderReceiver.kt:59` hard-casts the scheduler.
16. Snooze and the recurring re-arm share one PendingIntent per item.
17. Reminder body has no `BigTextStyle`; digests do.
18. No `setDeleteIntent`; swiping a reminder away loses it.
19. Notification id collisions possible (item ids vs 900001-900003; `title.hashCode()` in `PlatformNotifier`).
20. All-day recurring events never remind (`ItemRepository.kt:225-233` omits the offset).
21. `Recurrence.kt:72` `MAX_OCCURRENCES = 3700` counts from series start; old daily series stop yielding occurrences.
22. `AndroidReminderScheduler.kt:30` uses `System.currentTimeMillis()` rather than the injectable clock.
23. Ad-hoc `CoroutineScope(SupervisorJob() + Dispatchers.IO)` in every receiver; `goAsync()` budget.
24. Notification small icon is the "+" capture glyph.
25. Desktop is a silent no-op.
26. No battery-optimisation prompt, no `setAlarmClock`, no full-screen intent.

## 5. Tests

Exists: `commonTest/.../notifications/NextReminderTimeTest.kt` (7 tests, all UTC). Untested: every repository→scheduler interaction, the Android scheduler branch, the receiver (fire/DONE/SNOOZE, recurring re-arm), boot, `nextDailyTrigger`, digest content selection, DST/timezone behaviour, notification construction, the permission banner.

Cheapest high-value fixes, in order: recurring-task reminders, `USE_EXACT_ALARM`/`setAlarmClock`, a single receiver for `MY_PACKAGE_REPLACED` + `TIMEZONE_CHANGED` + exact-alarm-state-changed, item id in the content intent with a deep-link branch in `MainActivity`, and a date filter on `Digests.kt:124`.
