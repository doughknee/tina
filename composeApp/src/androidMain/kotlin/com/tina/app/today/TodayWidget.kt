package com.tina.app.today

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tina.app.MainActivity
import com.tina.app.R
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import java.util.Date
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.mp.KoinPlatform

private data class WidgetEntry(
    val id: Long,
    val title: String,
    val timeMillis: Long?,
    val isTask: Boolean,
    val completed: Boolean,
    val color: Long? = null,
)

private val ItemIdKey = ActionParameters.Key<Long>("itemId")

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = KoinPlatform.getKoin().get<ItemRepository>()
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        val dayStart = today.atStartOfDayIn(tz).toEpochMilliseconds()
        val dayEnd = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()

        val entries = mutableListOf<WidgetEntry>()
        repository.observeTasksForDay(today, tz).first().forEach { task ->
            entries += WidgetEntry(
                id = task.id,
                title = task.title,
                timeMillis = task.dueLocalTime?.let { LocalDateTime(today, it).toInstant(tz).toEpochMilliseconds() },
                isTask = true,
                completed = task.completed,
            )
        }
        repository.observeEventsIntersecting(dayStart, dayEnd).first().forEach { event ->
            repository.occurrencesOf(event, dayStart, dayEnd, tz).forEach { occurrence ->
                entries += WidgetEntry(
                    id = event.id,
                    title = event.title,
                    timeMillis = if (event.allDay) null else occurrence,
                    isTask = false,
                    completed = false,
                    color = event.color,
                )
            }
        }
        val sorted = entries.sortedWith(compareBy({ it.completed }, { it.timeMillis ?: Long.MAX_VALUE }))

        provideContent {
            GlanceTheme {
                WidgetBody(context, sorted)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetBody(context: Context, entries: List<WidgetEntry>) {
    val timeFormat = DateFormat.getTimeFormat(context)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Text(
                context.getString(R.string.widget_today_title),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            val open = entries.count { !it.completed }
            if (open > 0) {
                Text(
                    open.toString(),
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp),
                )
            }
        }
        // the same second line Plan shows under Today
        Text(
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", java.util.Locale.getDefault())),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            modifier = GlanceModifier.padding(bottom = 8.dp),
        )
        if (entries.isEmpty()) {
            Text(
                context.getString(R.string.widget_today_empty),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
            )
        } else {
            LazyColumn {
                items(entries) { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                    ) {
                        if (entry.isTask) {
                            CheckBox(
                                checked = entry.completed,
                                onCheckedChange = actionRunCallback<ToggleItemAction>(
                                    actionParametersOf(ItemIdKey to entry.id),
                                ),
                            )
                        } else {
                            Spacer(GlanceModifier.width(11.dp))
                            Box(
                                modifier = GlanceModifier
                                    .size(10.dp)
                                    .background(
                                        entry.color?.let { ColorProvider(Color(it)) }
                                            ?: GlanceTheme.colors.primary,
                                    )
                                    .cornerRadius(5.dp),
                            ) {}
                            Spacer(GlanceModifier.width(11.dp))
                        }
                        Column(modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())) {
                            Text(
                                entry.title,
                                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
                                maxLines = 1,
                            )
                            entry.timeMillis?.let { millis ->
                                Text(
                                    timeFormat.format(Date(millis)),
                                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class ToggleItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val itemId = parameters[ItemIdKey] ?: return
        val repository = KoinPlatform.getKoin().get<ItemRepository>()
        repository.get(itemId)?.let { item ->
            if (item.type == ItemType.TASK) {
                if (item.completed) repository.uncomplete(itemId) else repository.complete(itemId)
            }
        }
        TodayWidget().updateAll(context)
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
