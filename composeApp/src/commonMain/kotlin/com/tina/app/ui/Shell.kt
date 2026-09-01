package com.tina.app.ui

import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.tina.app.calendar.CalendarScreen
import com.tina.app.capture.CaptureScreen
import com.tina.app.capture.CaptureViewModel
import com.tina.app.data.Item
import com.tina.app.notes.NotesScreen
import com.tina.app.notes.NotesViewModel
import com.tina.app.resources.Res
import com.tina.app.today.TodayScreen
import kotlinx.datetime.number
import org.koin.compose.viewmodel.koinViewModel
import com.tina.app.resources.tab_ask
import com.tina.app.resources.tab_calendar
import com.tina.app.resources.tab_capture
import com.tina.app.resources.tab_notes
import com.tina.app.resources.tab_today
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Selected nav item keeps the Filled variant (M3 active-state convention).
enum class TinaTab(val icon: ImageVector, val outlinedIcon: ImageVector, val label: StringResource) {
    CAPTURE(Icons.Filled.Edit, Icons.Outlined.Edit, Res.string.tab_capture),
    TODAY(Icons.Filled.Today, Icons.Outlined.Today, Res.string.tab_today),
    CALENDAR(Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, Res.string.tab_calendar),
    NOTES(Icons.AutoMirrored.Filled.Notes, Icons.AutoMirrored.Outlined.Notes, Res.string.tab_notes),
    ASK(Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat, Res.string.tab_ask),
}

@Composable
fun Shell(
    onOpenSettings: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenItem: (Item) -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenSearch: () -> Unit,
) {
    val settings = com.tina.app.LocalSettings.current
    val askEnabled = settings.aiAskEnabled &&
        settings.aiProvider != com.tina.app.data.AiProvider.OFF
    val tabs = if (askEnabled) TinaTab.entries.toList() else TinaTab.entries.filter { it != TinaTab.ASK }
    // saved by name, not index, so toggling the Ask tab never shifts the selection.
    // LAST relies on rememberSaveable surviving process death; the others pin a start tab.
    val startTab = when (settings.openAppTo) {
        com.tina.app.data.OpenAppTo.CAPTURE -> TinaTab.CAPTURE.name
        com.tina.app.data.OpenAppTo.TODAY -> TinaTab.TODAY.name
        com.tina.app.data.OpenAppTo.LAST -> TinaTab.CAPTURE.name
    }
    var selectedName by rememberSaveable(settings.openAppTo) { mutableStateOf(startTab) }
    val selectedTab = tabs.firstOrNull { it.name == selectedName } ?: TinaTab.CAPTURE
    val captureViewModel: CaptureViewModel = koinViewModel()
    val notesViewModel: NotesViewModel = koinViewModel()

    LaunchedEffect(Unit) {
        KeyBus.events.collect { command ->
            when (command) {
                KeyCommand.FOCUS_CAPTURE -> selectedName = TinaTab.CAPTURE.name
                KeyCommand.SEARCH -> onOpenSearch()
                KeyCommand.NEW_ITEM ->
                    if (selectedName == TinaTab.NOTES.name) {
                        notesViewModel.createNote(onOpenNote)
                    } else {
                        selectedName = TinaTab.CAPTURE.name
                    }
                else -> Unit
            }
        }
    }

    NavigationSuiteScaffold(
        // the whole shell (nav bar included) rides above the keyboard
        modifier = Modifier.imePadding(),
        navigationSuiteItems = {
            tabs.forEach { tab ->
                item(
                    selected = selectedTab == tab,
                    onClick = { selectedName = tab.name },
                    icon = {
                        Icon(
                            if (selectedTab == tab) tab.icon else tab.outlinedIcon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(tab.label)) },
                )
            }
        },
    ) {
        when (selectedTab) {
            TinaTab.CAPTURE -> CaptureScreen(
                onOpenSettings = onOpenSettings,
                onOpenItem = onOpenItem,
                viewModel = captureViewModel,
            )
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
                    selectedName = TinaTab.CAPTURE.name
                },
            )
            TinaTab.NOTES -> NotesScreen(onOpenSettings = onOpenSettings, onOpenNote = onOpenNote)
            TinaTab.ASK -> com.tina.app.ask.AskScreen()
        }
    }
}
