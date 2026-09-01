package com.tina.app.capture

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
// the Intent-taking overload lives in the appwidget artifact, not glance.action
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tina.app.MainActivity
import com.tina.app.R

/**
 * Widgets cannot host a real text input (RemoteViews limitation), so this renders
 * as a search-bar-shaped button that drops you straight into Capture with the
 * keyboard up — same as Keep's and Google's own quick-capture widgets.
 */
class CaptureWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        GlanceTheme {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(GlanceTheme.colors.widgetBackground)
                    .cornerRadius(28.dp)
                    .clickable(
                        actionStartActivity(
                            android.content.Intent(context, MainActivity::class.java)
                                .putExtra(MainActivity.EXTRA_FOCUS_CAPTURE, true),
                        ),
                    )
                    .padding(start = 20.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_capture),
                    contentDescription = null,
                    modifier = GlanceModifier.size(22.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                )
                Spacer(GlanceModifier.width(12.dp))
                Text(
                    text = androidx.glance.LocalContext.current.getString(R.string.widget_hint),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 16.sp),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Box(
                    modifier = GlanceModifier
                        .size(36.dp)
                        .background(GlanceTheme.colors.primary)
                        .cornerRadius(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_capture),
                        contentDescription = null,
                        modifier = GlanceModifier.size(18.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                    )
                }
            }
        }
    }
}

class CaptureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CaptureWidget()
}
