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
import com.tina.app.calendar.CalendarScreen
import com.tina.app.capture.CaptureScreen
import com.tina.app.capture.CaptureViewModel
import com.tina.app.data.Item
import com.tina.app.notes.NotesScreen
import com.tina.app.resources.Res
import com.tina.app.today.TodayScreen
import kotlinx.datetime.number
import org.koin.compose.viewmodel.koinViewModel
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
fun Shell(
    onOpenSettings: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenItem: (Item) -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenSearch: () -> Unit,
) {
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val selectedTab = TinaTab.entries[selectedIndex]
    val captureViewModel: CaptureViewModel = koinViewModel()

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
            TinaTab.CAPTURE -> CaptureScreen(onOpenSettings = onOpenSettings, viewModel = captureViewModel)
            TinaTab.TODAY -> TodayScreen(
                onOpenSettings = onOpenSettings,
                onOpenInbox = onOpenInbox,
                onOpenItem = onOpenItem,
                onOpenSearch = onOpenSearch,
            )
            TinaTab.CALENDAR -> CalendarScreen(
                onOpenSettings = onOpenSettings,
                onOpenItem = onOpenItem,
                onCaptureForDate = { date ->
                    // Prefill with a parser-friendly date token so capture stays one flow.
                    captureViewModel.prefill("${date.month.number}/${date.day} ")
                    selectedIndex = 0
                },
            )
            TinaTab.NOTES -> NotesScreen(onOpenSettings = onOpenSettings, onOpenNote = onOpenNote)
        }
    }
}
