package com.tina.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.tina.app.capture.CaptureScreen
import com.tina.app.resources.Res
import com.tina.app.resources.tab_calendar
import com.tina.app.resources.tab_capture
import com.tina.app.resources.tab_notes
import com.tina.app.resources.tab_today
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class TinaTab(val icon: ImageVector, val label: StringResource) {
    CAPTURE(Icons.Filled.Edit, Res.string.tab_capture),
    TODAY(Icons.Filled.Today, Res.string.tab_today),
    CALENDAR(Icons.Filled.CalendarMonth, Res.string.tab_calendar),
    NOTES(Icons.AutoMirrored.Filled.Notes, Res.string.tab_notes),
}

@Composable
fun Shell(onOpenSettings: () -> Unit) {
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val selectedTab = TinaTab.entries[selectedIndex]

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TinaTab.entries.forEachIndexed { index, tab ->
                item(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.label)) },
                )
            }
        },
    ) {
        when (selectedTab) {
            TinaTab.CAPTURE -> CaptureScreen(onOpenSettings = onOpenSettings)
            TinaTab.TODAY -> PlaceholderTab(Res.string.tab_today, onOpenSettings)
            TinaTab.CALENDAR -> PlaceholderTab(Res.string.tab_calendar, onOpenSettings)
            TinaTab.NOTES -> PlaceholderTab(Res.string.tab_notes, onOpenSettings)
        }
    }
}
