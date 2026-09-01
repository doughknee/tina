package com.tina.app.ui

import androidx.compose.runtime.Composable
import com.tina.app.data.parseRrule
import com.tina.app.resources.Res
import com.tina.app.resources.date_today
import com.tina.app.resources.date_tomorrow
import com.tina.app.resources.duration_hours
import com.tina.app.resources.duration_hours_minutes
import com.tina.app.resources.duration_min
import com.tina.app.resources.every_day
import com.tina.app.resources.every_month
import com.tina.app.resources.every_week
import com.tina.app.resources.every_weekday
import com.tina.app.resources.every_year
import com.tina.app.resources.months_short
import com.tina.app.resources.repeats
import com.tina.app.resources.time_am
import com.tina.app.resources.time_day_ago
import com.tina.app.resources.time_hr_ago
import com.tina.app.resources.time_just_now
import com.tina.app.resources.time_min_ago
import com.tina.app.resources.time_pm
import com.tina.app.resources.weekdays_full
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun dateLabel(date: LocalDate, today: LocalDate): String {
    val days = today.daysUntil(date)
    return when {
        days == 0 -> stringResource(Res.string.date_today)
        days == 1 -> stringResource(Res.string.date_tomorrow)
        days in 2..6 -> stringArrayResource(Res.array.weekdays_full)[date.dayOfWeek.isoDayNumber - 1]
        else -> {
            val month = stringArrayResource(Res.array.months_short)[date.month.number - 1]
            if (date.year == today.year) "$month ${date.day}" else "$month ${date.day}, ${date.year}"
        }
    }
}

@Composable
fun relativeAge(ageMillis: Long): String {
    val minutes = ageMillis / 60_000
    return when {
        minutes < 1 -> stringResource(Res.string.time_just_now)
        minutes < 60 -> stringResource(Res.string.time_min_ago, minutes.toString())
        minutes < 60 * 24 -> stringResource(Res.string.time_hr_ago, (minutes / 60).toString())
        else -> stringResource(Res.string.time_day_ago, (minutes / (60 * 24)).toString())
    }
}

@Composable
fun timeLabel(time: LocalTime, use24h: Boolean = false): String {
    if (use24h) {
        val mm = time.minute.toString().padStart(2, '0')
        return "${time.hour}:$mm"
    }
    val amPm = if (time.hour < 12) stringResource(Res.string.time_am) else stringResource(Res.string.time_pm)
    val hour12 = when {
        time.hour == 0 -> 12
        time.hour > 12 -> time.hour - 12
        else -> time.hour
    }
    return if (time.minute == 0) "$hour12 $amPm"
    else "$hour12:${time.minute.toString().padStart(2, '0')} $amPm"
}

@Composable
fun durationLabel(minutes: Int): String = when {
    minutes < 60 -> stringResource(Res.string.duration_min, minutes)
    minutes % 60 == 0 -> stringResource(Res.string.duration_hours, minutes / 60)
    else -> stringResource(Res.string.duration_hours_minutes, minutes / 60, minutes % 60)
}

@Composable
fun recurrenceLabel(rrule: String): String {
    val rule = parseRrule(rrule) ?: return stringResource(Res.string.repeats)
    val weekday = rule.byDay.firstOrNull()
    return when {
        weekday != null && rule.byDay.size == 1 ->
            stringResource(Res.string.every_weekday, stringArrayResource(Res.array.weekdays_full)[weekday.isoDayNumber - 1])
        else -> when (rule.freq) {
            com.tina.app.data.RecurrenceRule.Freq.DAILY -> stringResource(Res.string.every_day)
            com.tina.app.data.RecurrenceRule.Freq.WEEKLY -> stringResource(Res.string.every_week)
            com.tina.app.data.RecurrenceRule.Freq.MONTHLY -> stringResource(Res.string.every_month)
            com.tina.app.data.RecurrenceRule.Freq.YEARLY -> stringResource(Res.string.every_year)
        }
    }
}
