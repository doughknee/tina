package com.tina.app.ui.settings

import com.tina.app.ui.rememberUndoWindow
import com.tina.app.ui.showUndo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tina.app.data.ContrastMode
import com.tina.app.data.OpenAppTo
import com.tina.app.resources.Res
import com.tina.app.resources.contrast_high
import com.tina.app.resources.contrast_medium
import com.tina.app.resources.contrast_standard
import com.tina.app.resources.licenses_title
import com.tina.app.resources.open_to_capture
import com.tina.app.resources.open_to_last
import com.tina.app.resources.set_contrast
import com.tina.app.resources.set_open_app_to
import com.tina.app.resources.set_undo_window
import com.tina.app.resources.shortcuts_title
import com.tina.app.resources.whats_new_title
import com.tina.app.resources.widgets_hint
import com.tina.app.resources.widgets_title
import com.tina.app.ui.SettingsViewModel
import com.tina.app.ui.settings.subpages.ChoiceSubpage
import com.tina.app.ui.settings.subpages.InfoSubpage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val UNDO_OPTIONS = listOf(3, 5, 10)

/** Maps a [SettingsDestination] to its screen; the host owns the back stack. */
@Composable
fun SettingsSubpageHost(
    destination: SettingsDestination,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    when (destination) {
        SettingsDestination.OPEN_APP_TO -> ChoiceSubpage(
            title = stringResource(Res.string.set_open_app_to),
            // CAPTURE and TODAY both open the Agenda now; a persisted TODAY just reads as Agenda
            options = listOf(
                stringResource(Res.string.open_to_capture),
                stringResource(Res.string.open_to_last),
            ),
            selectedIndex = if (settings.openAppTo == OpenAppTo.LAST) 1 else 0,
            onSelect = { viewModel.setOpenAppTo(if (it == 1) OpenAppTo.LAST else OpenAppTo.CAPTURE) },
            onBack = onBack,
        )
        SettingsDestination.UNDO_WINDOW -> ChoiceSubpage(
            title = stringResource(Res.string.set_undo_window),
            options = UNDO_OPTIONS.map { "$it s" },
            selectedIndex = UNDO_OPTIONS.indexOf(settings.undoWindowSeconds).coerceAtLeast(0),
            onSelect = { viewModel.setUndoWindowSeconds(UNDO_OPTIONS[it]) },
            onBack = onBack,
        )
        SettingsDestination.CONTRAST -> ChoiceSubpage(
            title = stringResource(Res.string.set_contrast),
            options = listOf(
                stringResource(Res.string.contrast_standard),
                stringResource(Res.string.contrast_medium),
                stringResource(Res.string.contrast_high),
            ),
            selectedIndex = ContrastMode.entries.indexOf(settings.contrast),
            onSelect = { viewModel.setContrast(ContrastMode.entries[it]) },
            onBack = onBack,
        )
        SettingsDestination.WIDGETS -> InfoSubpage(
            title = stringResource(Res.string.widgets_title),
            onBack = onBack,
            intro = stringResource(Res.string.widgets_hint),
            entries = listOf(
                "Today" to "Your day with tappable checkboxes",
                "Capture" to "1×1 — opens straight into the field",
            ),
        )
        SettingsDestination.SHORTCUTS -> InfoSubpage(
            title = stringResource(Res.string.shortcuts_title),
            onBack = onBack,
            entries = listOf(
                "Ctrl+N" to "Focus capture",
                "N" to "New note in the Notes scope, otherwise focus capture",
                "Ctrl+F" to "Search",
                "Arrows" to "Move the agenda date",
            ),
        )
        SettingsDestination.WHATS_NEW -> InfoSubpage(
            title = stringResource(Res.string.whats_new_title),
            onBack = onBack,
            entries = listOf(
                "1.5 — Plan, Sort, Ideas" to
                    "Pages named for what you do there, Idea mode for notes, one calendar for every " +
                        "zoom level, search as a sheet, and a smoother keyboard.",
                "1.4 — Three places, four zoom levels" to
                    "Agenda, Library and Ask; capture from anywhere; Day / Week / Month / All with " +
                        "repeats rolled up and completed per day.",
                "1.3.1 — Capture, out of your way" to
                    "The keyboard no longer opens with the app, the composer moved to the " +
                        "bottom, and pages slide instead of zooming.",
                "1.3 — Settings, rebuilt" to
                    "Grouped and searchable settings, Trash with restore, tag manager, " +
                        "daily summaries, app lock, auto-backup.",
                "1.2 — Ask your app anything" to
                    "Chat with your data, optional write access, saved conversations, browsable tags.",
                "1.1 — AI parsing" to
                    "Ollama / Claude / OpenAI refinement, AI improve, and the Material 3 redesign.",
                "1.0" to "Capture, Today, Calendar, Notes, reminders, widgets, backup.",
            ),
        )
        SettingsDestination.TAGS -> com.tina.app.ui.settings.subpages.TagManagerScreen(onBack = onBack)
        SettingsDestination.PRO -> com.tina.app.pro.PaywallScreen(onBack = onBack)
        SettingsDestination.TRASH -> com.tina.app.ui.settings.subpages.TrashScreen(onBack = onBack)
        SettingsDestination.LICENSES -> InfoSubpage(
            title = stringResource(Res.string.licenses_title),
            onBack = onBack,
            entries = listOf(
                "Compose Multiplatform" to "Apache-2.0 — JetBrains",
                "AndroidX (Room, DataStore, Glance, Lifecycle)" to "Apache-2.0 — Google",
                "Koin" to "Apache-2.0 — Kotzilla",
                "Ktor" to "Apache-2.0 — JetBrains",
                "kotlinx.datetime / serialization / coroutines" to "Apache-2.0 — JetBrains",
                "Calendar" to "MIT — Kizito Nwose",
                "Compose Rich Editor" to "Apache-2.0 — Mohamed Rejeb",
                "Reorderable" to "Apache-2.0 — Calvin Liang",
            ),
        )
    }
}
