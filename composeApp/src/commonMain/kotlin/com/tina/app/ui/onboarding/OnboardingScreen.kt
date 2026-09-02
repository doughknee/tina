package com.tina.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tina.app.resources.Res
import com.tina.app.resources.onb_capture_body
import com.tina.app.resources.onb_capture_example
import com.tina.app.resources.onb_capture_title
import com.tina.app.resources.onb_next
import com.tina.app.resources.onb_reminders_body
import com.tina.app.resources.onb_reminders_title
import com.tina.app.resources.onb_skip
import com.tina.app.resources.onb_sort_body
import com.tina.app.resources.onb_sort_title
import com.tina.app.resources.onb_start
import com.tina.app.resources.onb_widget_hint
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private class Card(val icon: ImageVector, val title: StringResource, val body: StringResource)

private val CARDS = listOf(
    Card(Icons.Outlined.Bolt, Res.string.onb_capture_title, Res.string.onb_capture_body),
    Card(Icons.Outlined.Inbox, Res.string.onb_sort_title, Res.string.onb_sort_body),
    Card(Icons.Outlined.NotificationsActive, Res.string.onb_reminders_title, Res.string.onb_reminders_body),
)

/**
 * Three cards on first launch: capture, sort, reminders. Skippable in one tap from any card;
 * the last card carries the notification permission so the first reminder can actually ring.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { CARDS.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == CARDS.lastIndex
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) { Text(stringResource(Res.string.onb_skip)) }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { index ->
            val card = CARDS[index]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(card.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(card.title),
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(card.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 480.dp),
                )
                when (index) {
                    0 -> {
                        Spacer(Modifier.height(24.dp))
                        // the pitch in one line: what you type, and what it becomes
                        Text(
                            stringResource(Res.string.onb_capture_example),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                    }
                    2 -> {
                        Spacer(Modifier.height(24.dp))
                        NotificationPermissionCta()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(Res.string.onb_widget_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(CARDS.size) { i ->
                    Box(
                        Modifier.size(8.dp).background(
                            if (i == pager.currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            }) {
                Text(stringResource(if (last) Res.string.onb_start else Res.string.onb_next))
            }
        }
    }
}

/** "Allow notifications" where the platform has a runtime permission; nothing elsewhere. */
@Composable
expect fun NotificationPermissionCta()
