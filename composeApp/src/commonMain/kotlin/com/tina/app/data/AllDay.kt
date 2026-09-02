package com.tina.app.data

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * All-day events are stored as local midnight in the zone they were written in. After a
 * time-zone change that midnight is some other hour of some other day, so the calendar date
 * the user meant is recovered in the old zone and re-anchored to midnight in the new one.
 *
 * ponytail: the alternative was a schema change to epoch-day columns and touching every reader
 * of startAt. Re-anchoring on zone change keeps one storage format and one code path.
 */
fun realignAllDay(millis: Long, from: TimeZone, to: TimeZone): Long {
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(from).date
    return LocalDateTime(date, LocalTime(0, 0)).toInstant(to).toEpochMilliseconds()
}

/**
 * Runs on every launch and on the system's time-zone broadcast. The first run only records the
 * zone; later runs move all-day events when the zone differs from the recorded one.
 */
suspend fun ItemRepository.syncTimeZone(settings: SettingsRepository): Int {
    val current = TimeZone.currentSystemDefault()
    val previousId = settings.lastTimeZoneId()
    var moved = 0
    if (previousId != null && previousId != current.id) {
        val previous = runCatching { TimeZone.of(previousId) }.getOrNull()
        if (previous != null) moved = realignAllDayEvents(previous, current)
    }
    settings.setLastTimeZoneId(current.id)
    return moved
}
