package com.tina.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tina.app.LocalSettings
import com.tina.app.resources.Res
import com.tina.app.resources.dismiss
import com.tina.app.resources.first_need_day_body
import com.tina.app.resources.first_need_day_title
import com.tina.app.resources.first_plan_at
import com.tina.app.resources.first_plan_body
import com.tina.app.resources.first_plan_body_date
import com.tina.app.resources.first_plan_on
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * The one callout after the first capture, on the page it landed on (design/first-run
 * LandedPlan / LandedSort): tonal card, a bold line saying where it went, one line saying why,
 * and a close. Shows while [FirstCapture.callout] matches [landing]; the close clears it.
 */
@Composable
fun FirstCaptureCallout(landing: FirstCapture.Landing, today: LocalDate, modifier: Modifier = Modifier) {
    val use24h = LocalSettings.current.use24h
    val (title, body) = when (landing) {
        is FirstCapture.Landing.Plan -> {
            val day = dateLabel(landing.date, today)
            val time = landing.time
            if (time == null) stringResource(Res.string.first_plan_on, day) to stringResource(Res.string.first_plan_body_date)
            else stringResource(Res.string.first_plan_at, day, timeLabel(time, use24h)) to stringResource(Res.string.first_plan_body)
        }
        else -> stringResource(Res.string.first_need_day_title) to stringResource(Res.string.first_need_day_body)
    }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            IconButton(onClick = FirstCapture::dismissCallout) {
                Icon(Icons.Outlined.Close, stringResource(Res.string.dismiss), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
