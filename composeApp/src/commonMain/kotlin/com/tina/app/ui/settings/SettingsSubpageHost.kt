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
import com.tina.app.resources.open_to_ideas
import com.tina.app.resources.open_to_last
import com.tina.app.resources.open_to_sort
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
            // CAPTURE and TODAY are the old names for Plan; a saved one still reads as Plan
            options = listOf(
                stringResource(Res.string.open_to_capture),
                stringResource(Res.string.open_to_sort),
                stringResource(Res.string.open_to_ideas),
                stringResource(Res.string.open_to_last),
            ),
            selectedIndex = when (settings.openAppTo) {
                OpenAppTo.SORT -> 1
                OpenAppTo.IDEAS -> 2
                OpenAppTo.LAST -> 3
                else -> 0
            },
            onSelect = { viewModel.setOpenAppTo(listOf(OpenAppTo.CAPTURE, OpenAppTo.SORT, OpenAppTo.IDEAS, OpenAppTo.LAST)[it]) },
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
                "Today" to "Your day with checkboxes, a capture button, and rows that open the item. Refreshes itself every half hour.",
                "Capture" to "A pill that opens straight into the field",
            ),
        )
        SettingsDestination.SHORTCUTS -> InfoSubpage(
            title = stringResource(Res.string.shortcuts_title),
            onBack = onBack,
            entries = listOf(
                "Ctrl+N" to "Focus capture",
                "N" to "New idea on Ideas, otherwise focus capture",
                "Ctrl+F" to "Search",
                "Arrows" to "Move the Plan date",
                "Enter" to "Confirm the selected row",
                "Delete" to "Delete the selected row",
            ),
        )
        SettingsDestination.WHATS_NEW -> InfoSubpage(
            title = stringResource(Res.string.whats_new_title),
            onBack = onBack,
            entries = com.tina.app.ui.settings.WHATS_NEW.map { (version, text) -> version to text },
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
