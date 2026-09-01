package com.tina.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import com.tina.app.ui.rememberUndoWindow
import com.tina.app.ui.showUndo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tina.app.ai.ANTHROPIC_DEFAULT_BASE_URL
import com.tina.app.ai.ANTHROPIC_MODELS
import com.tina.app.data.AiProvider
import com.tina.app.data.AiRefineMode
import com.tina.app.data.ContrastMode
import com.tina.app.data.OpenAppTo
import com.tina.app.data.ReduceMotionMode
import com.tina.app.data.Settings
import com.tina.app.data.ThemeMode
import com.tina.app.data.TrashRetention
import com.tina.app.data.rememberBackupHandlers
import com.tina.app.resources.Res
import com.tina.app.resources.ai_api_key
import com.tina.app.resources.ai_base_url
import com.tina.app.resources.ai_connected
import com.tina.app.resources.ai_failed
import com.tina.app.resources.ai_key_saved
import com.tina.app.resources.ai_model
import com.tina.app.resources.ai_no_key
import com.tina.app.resources.ai_privacy_note
import com.tina.app.resources.ai_provider_anthropic
import com.tina.app.resources.ai_provider_custom
import com.tina.app.resources.ai_provider_off
import com.tina.app.resources.ai_provider_ollama
import com.tina.app.resources.ai_provider_openai
import com.tina.app.resources.ai_test
import com.tina.app.resources.ai_test_fail
import com.tina.app.resources.ai_test_ok
import com.tina.app.resources.ai_untested
import com.tina.app.resources.ai_workspace_id
import com.tina.app.resources.back
import com.tina.app.resources.cancel
import com.tina.app.resources.contrast_high
import com.tina.app.resources.contrast_medium
import com.tina.app.resources.contrast_standard
import com.tina.app.resources.export_data
import com.tina.app.resources.export_done
import com.tina.app.resources.import_data
import com.tina.app.resources.import_done
import com.tina.app.resources.import_failed
import com.tina.app.resources.ok
import com.tina.app.resources.open_to_capture
import com.tina.app.resources.open_to_last
import com.tina.app.resources.open_to_today
import com.tina.app.resources.refine_auto
import com.tina.app.resources.refine_manual
import com.tina.app.resources.refine_suggest
import com.tina.app.resources.reminder_at_time
import com.tina.app.resources.reminder_hour_before
import com.tina.app.resources.reminder_min_before
import com.tina.app.resources.sec_about
import com.tina.app.resources.sec_ai
import com.tina.app.resources.sec_appearance
import com.tina.app.resources.sec_capture
import com.tina.app.resources.sec_data
import com.tina.app.resources.sec_datetime
import com.tina.app.resources.sec_desktop
import com.tina.app.resources.sec_general
import com.tina.app.resources.sec_notifications
import com.tina.app.resources.sec_organisation
import com.tina.app.resources.sec_privacy
import com.tina.app.resources.set_afternoon
import com.tina.app.resources.set_app_lock
import com.tina.app.resources.set_app_lock_sub
import com.tina.app.resources.set_auto_backup
import com.tina.app.resources.set_auto_backup_sub
import com.tina.app.resources.set_clear
import com.tina.app.resources.set_clear_completed
import com.tina.app.resources.set_clear_completed_sub
import com.tina.app.resources.set_cleared_completed
import com.tina.app.resources.set_close_tray
import com.tina.app.resources.set_close_tray_sub
import com.tina.app.resources.set_trash
import com.tina.app.resources.set_trash_sub
import com.tina.app.resources.undo
import com.tina.app.resources.set_contrast
import com.tina.app.resources.set_daily_agenda
import com.tina.app.resources.set_daily_agenda_sub
import com.tina.app.resources.set_day_sections
import com.tina.app.resources.set_day_sections_sub
import com.tina.app.resources.set_delete_all
import com.tina.app.resources.set_delete_all_action
import com.tina.app.resources.set_delete_all_sub
import com.tina.app.resources.set_deleted_all
import com.tina.app.resources.set_diagnostics
import com.tina.app.resources.set_diagnostics_sub
import com.tina.app.resources.set_evening
import com.tina.app.resources.set_export_sub
import com.tina.app.resources.set_haptics
import com.tina.app.resources.set_haptics_sub
import com.tina.app.resources.set_hide_switcher
import com.tina.app.resources.set_hide_switcher_sub
import com.tina.app.resources.set_import_sub
import com.tina.app.resources.set_inbox_reminder
import com.tina.app.resources.set_inbox_reminder_sub
import com.tina.app.resources.set_auto_focus
import com.tina.app.resources.set_auto_focus_sub
import com.tina.app.resources.set_keep_keyboard
import com.tina.app.resources.set_keep_keyboard_sub
import com.tina.app.resources.set_language
import com.tina.app.resources.set_language_sub
import com.tina.app.resources.set_launch_login
import com.tina.app.resources.set_launch_login_sub
import com.tina.app.resources.set_licenses
import com.tina.app.resources.set_morning
import com.tina.app.resources.set_open_app_to
import com.tina.app.resources.set_overdue_nudge
import com.tina.app.resources.set_overdue_nudge_sub
import com.tina.app.resources.set_provider
import com.tina.app.resources.set_provider_sub
import com.tina.app.resources.set_pure_black
import com.tina.app.resources.set_pure_black_sub
import com.tina.app.resources.set_qs_tile
import com.tina.app.resources.set_qs_tile_sub
import com.tina.app.resources.set_quick_shortcut
import com.tina.app.resources.set_quick_shortcut_sub
import com.tina.app.resources.set_reduce_motion
import com.tina.app.resources.set_reduce_motion_system
import com.tina.app.resources.set_search_completed
import com.tina.app.resources.set_search_completed_sub
import com.tina.app.resources.set_shortcuts
import com.tina.app.resources.set_shortcuts_sub
import com.tina.app.resources.set_show_completed
import com.tina.app.resources.set_show_completed_sub
import com.tina.app.resources.set_sound
import com.tina.app.resources.set_sound_sub
import com.tina.app.resources.set_tags
import com.tina.app.resources.set_tags_sub
import com.tina.app.resources.set_theme
import com.tina.app.resources.set_undo_window
import com.tina.app.resources.set_undo_window_sub
import com.tina.app.resources.set_version
import com.tina.app.resources.set_voice_capture
import com.tina.app.resources.set_voice_capture_sub
import com.tina.app.resources.set_whats_new
import com.tina.app.resources.set_widgets
import com.tina.app.resources.set_widgets_sub
import com.tina.app.resources.set_wifi_only
import com.tina.app.resources.set_wifi_only_sub
import com.tina.app.resources.settings
import com.tina.app.resources.settings_24h
import com.tina.app.resources.settings_ai_instructions
import com.tina.app.resources.settings_ask
import com.tina.app.resources.settings_ask_desc
import com.tina.app.resources.settings_dynamic_color
import com.tina.app.resources.settings_dynamic_color_desc
import com.tina.app.resources.settings_first_day
import com.tina.app.resources.settings_refine_mode
import com.tina.app.resources.settings_reminder
import com.tina.app.resources.settings_search
import com.tina.app.resources.settings_search_empty
import com.tina.app.resources.theme_dark
import com.tina.app.resources.theme_light
import com.tina.app.resources.theme_system
import com.tina.app.resources.trash_30
import com.tina.app.resources.trash_7
import com.tina.app.resources.trash_forever
import com.tina.app.resources.weekdays_full
import com.tina.app.ui.SettingsViewModel
import com.tina.app.ui.expandEnter
import com.tina.app.ui.expandExit
import com.tina.app.ui.timeLabel
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Bumped with each release; About reads it. */
const val APP_VERSION = "1.3.0"

private val REMINDER_OPTIONS = listOf(0, 5, 10, 15, 30, 60)
private val FIRST_DAY_OPTIONS = listOf(DayOfWeek.MONDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/** Chevron rows are real destinations; the host maps these to routes. */
enum class SettingsDestination {
    OPEN_APP_TO, UNDO_WINDOW, CONTRAST, WIDGETS, SHORTCUTS, WHATS_NEW, LICENSES, TRASH, TAGS
}

/** Which time a [SettingsRow.TimeRow] edits. */
private enum class TimeTarget { MORNING, AFTERNOON, EVENING, DAILY_AGENDA, OVERDUE_NUDGE }

@Composable
private fun minutesLabel(minutes: Int, use24h: Boolean): String =
    timeLabel(LocalTime(minutes / 60, minutes % 60), use24h)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val undoWindow = rememberUndoWindow()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var highlightedRowId by remember { mutableStateOf<String?>(null) }

    val clearedText = stringResource(Res.string.set_cleared_completed)
    val undoText = stringResource(Res.string.undo)
    val exportDoneText = stringResource(Res.string.export_done)
    val importFailedText = stringResource(Res.string.import_failed)
    val backupHandlers = rememberBackupHandlers(
        onExported = { scope.launch { snackbarHostState.showSnackbar(exportDoneText) } },
        onImported = { count ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (count < 0) importFailedText else getString(Res.string.import_done, count),
                )
            }
        },
    )

    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }

    val actions = rememberPlatformActions()
    val sections = rememberSettingsSections(
        settings = settings,
        actions = actions,
        stats = stats,
        viewModel = viewModel,
        onNavigate = onNavigate,
        onPickTime = { timeTarget = it },
        onClearCompleted = {
            viewModel.clearCompleted()
            scope.launch {
                if (snackbarHostState.showUndo(clearedText, undoText, undoWindow)) viewModel.undoClearCompleted()
            }
        },
        onExport = backupHandlers.export,
        onImport = backupHandlers.restore,
        snackbarHostState = snackbarHostState,
    )

    timeTarget?.let { target ->
        val current = when (target) {
            TimeTarget.MORNING -> settings.morningStartMinutes
            TimeTarget.AFTERNOON -> settings.afternoonStartMinutes
            TimeTarget.EVENING -> settings.eveningStartMinutes
            TimeTarget.DAILY_AGENDA -> settings.dailyAgendaMinutes
            TimeTarget.OVERDUE_NUDGE -> settings.overdueNudgeMinutes
        }
        SettingsTimePicker(
            initialMinutes = current,
            use24h = settings.use24h,
            onDismiss = { timeTarget = null },
            onPicked = { minutes ->
                when (target) {
                    TimeTarget.MORNING -> viewModel.setMorningStart(minutes)
                    TimeTarget.AFTERNOON -> viewModel.setAfternoonStart(minutes)
                    TimeTarget.EVENING -> viewModel.setEveningStart(minutes)
                    TimeTarget.DAILY_AGENDA -> viewModel.setDailyAgendaMinutes(minutes)
                    TimeTarget.OVERDUE_NUDGE -> viewModel.setOverdueNudgeMinutes(minutes)
                }
                timeTarget = null
            },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // TODO(expressive): LargeFlexibleTopAppBar once material3 ships it
            LargeTopAppBar(
                title = { Text(stringResource(Res.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(Res.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { searching = !searching; if (!searching) query = "" },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            if (searching) Icons.Outlined.Close else Icons.Outlined.Search,
                            stringResource(Res.string.settings_search),
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            AnimatedVisibility(visible = searching, enter = expandEnter(), exit = expandExit()) {
                SettingsSearchField(query, onQueryChange = { query = it })
            }
            if (searching && query.isNotBlank()) {
                SettingsSearchResults(
                    sections = sections,
                    query = query,
                    onResult = { section, row ->
                        searching = false
                        query = ""
                        highlightedRowId = row.id
                        scope.launch {
                            val index = sections.filter { it.visible }.indexOf(section)
                            if (index >= 0) listState.animateScrollToItem(index)
                        }
                    },
                )
                return@Column
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                items(sections.filter { it.visible && it.visibleRows.isNotEmpty() }.size) { i ->
                    val section = sections.filter { it.visible && it.visibleRows.isNotEmpty() }[i]
                    SettingsSectionBlock(section = section, highlightedRowId = highlightedRowId)
                }
            }
        }
    }

    // clear the highlight after it has been seen
    LaunchedEffect(highlightedRowId) {
        if (highlightedRowId != null) {
            kotlinx.coroutines.delay(600)
            highlightedRowId = null
        }
    }
}

@Composable
private fun SettingsSearchField(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(56.dp),
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(Res.string.settings_search)) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun SettingsSearchResults(
    sections: List<SettingsSection>,
    query: String,
    onResult: (SettingsSection, SettingsRow) -> Unit,
) {
    val hits = sections.searchable().filter { (_, row) -> row.matches(query) }
    if (hits.isEmpty()) {
        Text(
            stringResource(Res.string.settings_search_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(hits.size) { index ->
            val (section, row) = hits[index]
            SettingsRowSurface(index = index, count = hits.size) {
                ListItem(
                    headlineContent = { HighlightedText(row.title, query) },
                    supportingContent = {
                        Text(
                            listOfNotNull(section.title, row.supporting).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable { onResult(section, row) }
                        .semantics(mergeDescendants = true) {},
                )
            }
        }
    }
}

@Composable
private fun HighlightedText(text: String, query: String) {
    val start = text.indexOf(query, ignoreCase = true)
    if (start < 0) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        return
    }
    val end = start + query.length
    Text(
        buildAnnotatedString {
            append(text.substring(0, start))
            withStyle(
                SpanStyle(
                    background = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) { append(text.substring(start, end)) }
            append(text.substring(end))
        },
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun rememberSettingsSections(
    settings: Settings,
    actions: PlatformActions,
    stats: SettingsViewModel.Stats,
    viewModel: SettingsViewModel,
    onNavigate: (SettingsDestination) -> Unit,
    onPickTime: (TimeTarget) -> Unit,
    onClearCompleted: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    snackbarHostState: SnackbarHostState,
): List<SettingsSection> {
    val weekdayNames = stringArrayResource(Res.array.weekdays_full)
    val use24h = settings.use24h

    val themeLabels = listOf(
        stringResource(Res.string.theme_system),
        stringResource(Res.string.theme_light),
        stringResource(Res.string.theme_dark),
    )
    val reminderLabels = REMINDER_OPTIONS.map { m ->
        when (m) {
            0 -> stringResource(Res.string.reminder_at_time)
            60 -> stringResource(Res.string.reminder_hour_before)
            else -> stringResource(Res.string.reminder_min_before, m)
        }
    }
    val providerLabels = listOf(
        stringResource(Res.string.ai_provider_off),
        stringResource(Res.string.ai_provider_ollama),
        stringResource(Res.string.ai_provider_anthropic),
        stringResource(Res.string.ai_provider_openai),
        stringResource(Res.string.ai_provider_custom),
    )
    val refineLabels = listOf(
        stringResource(Res.string.refine_auto),
        stringResource(Res.string.refine_suggest),
        stringResource(Res.string.refine_manual),
    )
    val openToLabel = when (settings.openAppTo) {
        OpenAppTo.CAPTURE -> stringResource(Res.string.open_to_capture)
        OpenAppTo.TODAY -> stringResource(Res.string.open_to_today)
        OpenAppTo.LAST -> stringResource(Res.string.open_to_last)
    }
    val contrastLabel = when (settings.contrast) {
        ContrastMode.STANDARD -> stringResource(Res.string.contrast_standard)
        ContrastMode.MEDIUM -> stringResource(Res.string.contrast_medium)
        ContrastMode.HIGH -> stringResource(Res.string.contrast_high)
    }
    val retentionLabel = when (settings.trashRetention) {
        TrashRetention.DAYS_7 -> stringResource(Res.string.trash_7)
        TrashRetention.DAYS_30 -> stringResource(Res.string.trash_30)
        TrashRetention.FOREVER -> stringResource(Res.string.trash_forever)
    }

    val general = SettingsSection(
        id = "general",
        title = stringResource(Res.string.sec_general),
        rows = listOf(
            SettingsRow.Navigation(
                id = "openAppTo",
                                title = stringResource(Res.string.set_open_app_to),
                supporting = openToLabel,
                keywords = listOf("start", "launch", "home"),
                onClick = { onNavigate(SettingsDestination.OPEN_APP_TO) },
            ),
            SettingsRow.External(
                id = "language",
                enabled = false,
                title = stringResource(Res.string.set_language),
                supporting = stringResource(Res.string.set_language_sub),
                keywords = listOf("locale", "translate"),
                visible = Platform.isAndroid,
                onClick = { },
            ),
            SettingsRow.Switch(
                id = "haptics",
                title = stringResource(Res.string.set_haptics),
                supporting = stringResource(Res.string.set_haptics_sub),
                keywords = listOf("vibration", "feedback"),
                checked = settings.haptics,
                onCheckedChange = viewModel::setHaptics,
            ),
            SettingsRow.Switch(
                id = "reduceMotion",
                title = stringResource(Res.string.set_reduce_motion),
                supporting = stringResource(Res.string.set_reduce_motion_system),
                keywords = listOf("animation", "accessibility"),
                checked = settings.reduceMotion == ReduceMotionMode.ON,
                onCheckedChange = {
                    viewModel.setReduceMotion(if (it) ReduceMotionMode.ON else ReduceMotionMode.OFF)
                },
            ),
        ),
    )

    val appearance = SettingsSection(
        id = "appearance",
        title = stringResource(Res.string.sec_appearance),
        rows = listOf(
            SettingsRow.ButtonGroupRow(
                id = "theme",
                title = stringResource(Res.string.set_theme),
                keywords = listOf("dark", "light", "night"),
                options = listOf(
                    SettingsRow.ButtonGroupRow.Option(themeLabels[0], Icons.Outlined.BrightnessAuto),
                    SettingsRow.ButtonGroupRow.Option(themeLabels[1], Icons.Outlined.LightMode),
                    SettingsRow.ButtonGroupRow.Option(themeLabels[2], Icons.Outlined.DarkMode),
                ),
                selectedIndex = ThemeMode.entries.indexOf(settings.themeMode),
                onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) },
            ),
            SettingsRow.Switch(
                id = "dynamicColor",
                title = stringResource(Res.string.settings_dynamic_color),
                supporting = stringResource(Res.string.settings_dynamic_color_desc),
                keywords = listOf("material you", "wallpaper"),
                visible = Platform.isAndroid,
                checked = settings.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
            ),
            SettingsRow.Navigation(
                id = "contrast",
                title = stringResource(Res.string.set_contrast),
                supporting = contrastLabel,
                keywords = listOf("accessibility", "legibility"),
                onClick = { onNavigate(SettingsDestination.CONTRAST) },
            ),
            SettingsRow.Switch(
                id = "pureBlack",
                title = stringResource(Res.string.set_pure_black),
                supporting = stringResource(Res.string.set_pure_black_sub),
                keywords = listOf("oled", "amoled", "battery"),
                visible = settings.themeMode == ThemeMode.DARK,
                checked = settings.pureBlack,
                onCheckedChange = viewModel::setPureBlack,
            ),
        ),
    )

    val capture = SettingsSection(
        id = "capture",
        title = stringResource(Res.string.sec_capture),
        rows = listOf(
            SettingsRow.Switch(
                id = "qsTile",
                enabled = false,
                title = stringResource(Res.string.set_qs_tile),
                supporting = stringResource(Res.string.set_qs_tile_sub),
                keywords = listOf("shade", "tile", "quick"),
                visible = Platform.isAndroid,
                checked = settings.quickSettingsTile,
                onCheckedChange = viewModel::setQuickSettingsTile,
            ),
            SettingsRow.Switch(
                id = "autoFocusCapture",
                title = stringResource(Res.string.set_auto_focus),
                supporting = stringResource(Res.string.set_auto_focus_sub),
                keywords = listOf("keyboard", "ime", "focus", "open"),
                checked = settings.autoFocusCapture,
                onCheckedChange = viewModel::setAutoFocusCapture,
            ),
            SettingsRow.Switch(
                id = "keepKeyboard",
                                title = stringResource(Res.string.set_keep_keyboard),
                supporting = stringResource(Res.string.set_keep_keyboard_sub),
                keywords = listOf("keyboard", "ime"),
                checked = settings.keepKeyboardUp,
                onCheckedChange = viewModel::setKeepKeyboardUp,
            ),
            SettingsRow.Switch(
                id = "voiceCapture",
                title = stringResource(Res.string.set_voice_capture),
                supporting = stringResource(Res.string.set_voice_capture_sub),
                keywords = listOf("mic", "dictate", "speech"),
                checked = settings.voiceCapture,
                onCheckedChange = viewModel::setVoiceCapture,
            ),
            SettingsRow.Navigation(
                id = "widgets",
                title = stringResource(Res.string.set_widgets),
                supporting = stringResource(Res.string.set_widgets_sub),
                keywords = listOf("home screen"),
                visible = Platform.isAndroid,
                onClick = { onNavigate(SettingsDestination.WIDGETS) },
            ),
            SettingsRow.Navigation(
                id = "undoWindow",
                                title = stringResource(Res.string.set_undo_window),
                supporting = stringResource(Res.string.set_undo_window_sub, settings.undoWindowSeconds),
                keywords = listOf("snackbar", "undo"),
                onClick = { onNavigate(SettingsDestination.UNDO_WINDOW) },
            ),
        ),
    )

    val dateTime = SettingsSection(
        id = "datetime",
        title = stringResource(Res.string.sec_datetime),
        rows = listOf(
            SettingsRow.ButtonGroupRow(
                id = "firstDay",
                title = stringResource(Res.string.settings_first_day),
                keywords = listOf("week", "monday", "sunday"),
                options = FIRST_DAY_OPTIONS.map {
                    SettingsRow.ButtonGroupRow.Option(weekdayNames[it.isoDayNumber - 1])
                },
                selectedIndex = FIRST_DAY_OPTIONS.indexOf(settings.firstDayOfWeek).coerceAtLeast(0),
                onSelect = { viewModel.setFirstDayOfWeek(FIRST_DAY_OPTIONS[it]) },
            ),
            SettingsRow.Switch(
                id = "use24h",
                title = stringResource(Res.string.settings_24h),
                keywords = listOf("clock", "military"),
                checked = settings.use24h,
                onCheckedChange = viewModel::setUse24h,
            ),
            SettingsRow.ChipRailRow(
                id = "defaultReminder",
                title = stringResource(Res.string.settings_reminder),
                keywords = listOf("remind", "alarm", "notify"),
                options = reminderLabels,
                selectedIndex = REMINDER_OPTIONS.indexOf(settings.defaultReminderMinutes),
                onSelect = { viewModel.setDefaultReminderMinutes(REMINDER_OPTIONS[it]) },
            ),
            SettingsRow.Value(
                id = "daySections",
                                title = stringResource(Res.string.set_day_sections),
                supporting = stringResource(Res.string.set_day_sections_sub),
                keywords = listOf("morning", "afternoon", "evening", "tonight"),
            ),
            SettingsRow.TimeRow(
                id = "morning",
                                title = stringResource(Res.string.set_morning),
                keywords = listOf("morning"),
                timeLabel = minutesLabel(settings.morningStartMinutes, use24h),
                onClick = { onPickTime(TimeTarget.MORNING) },
            ),
            SettingsRow.TimeRow(
                id = "afternoon",
                                title = stringResource(Res.string.set_afternoon),
                keywords = listOf("afternoon"),
                timeLabel = minutesLabel(settings.afternoonStartMinutes, use24h),
                onClick = { onPickTime(TimeTarget.AFTERNOON) },
            ),
            SettingsRow.TimeRow(
                id = "evening",
                                title = stringResource(Res.string.set_evening),
                keywords = listOf("evening", "tonight"),
                timeLabel = minutesLabel(settings.eveningStartMinutes, use24h),
                onClick = { onPickTime(TimeTarget.EVENING) },
            ),
        ),
    )

    val notifications = SettingsSection(
        id = "notifications",
        title = stringResource(Res.string.sec_notifications),
        rows = listOf(
            SettingsRow.Switch(
                id = "dailyAgenda",
                                title = stringResource(Res.string.set_daily_agenda),
                supporting = stringResource(Res.string.set_daily_agenda_sub),
                keywords = listOf("morning", "summary", "digest", "remind"),
                checked = settings.dailyAgenda,
                onCheckedChange = viewModel::setDailyAgenda,
            ),
            SettingsRow.TimeRow(
                id = "dailyAgendaTime",
                                title = stringResource(Res.string.set_daily_agenda),
                keywords = listOf("agenda time"),
                visible = settings.dailyAgenda,
                timeLabel = minutesLabel(settings.dailyAgendaMinutes, use24h),
                onClick = { onPickTime(TimeTarget.DAILY_AGENDA) },
            ),
            SettingsRow.Switch(
                id = "overdueNudge",
                                title = stringResource(Res.string.set_overdue_nudge),
                supporting = stringResource(Res.string.set_overdue_nudge_sub),
                keywords = listOf("overdue", "remind", "late"),
                checked = settings.overdueNudge,
                onCheckedChange = viewModel::setOverdueNudge,
            ),
            SettingsRow.Switch(
                id = "inboxReminder",
                                title = stringResource(Res.string.set_inbox_reminder),
                supporting = stringResource(Res.string.set_inbox_reminder_sub, settings.inboxReminderDays),
                keywords = listOf("inbox", "triage", "remind"),
                checked = settings.inboxReminder,
                onCheckedChange = viewModel::setInboxReminder,
            ),
            SettingsRow.External(
                id = "sound",
                enabled = false,
                title = stringResource(Res.string.set_sound),
                supporting = stringResource(Res.string.set_sound_sub),
                keywords = listOf("vibrate", "channel", "ringtone"),
                visible = Platform.isAndroid,
                onClick = { },
            ),
        ),
    )

    val ai = SettingsSection(
        id = "ai",
        title = stringResource(Res.string.sec_ai),
        rows = listOf(
            SettingsRow.ChipRailRow(
                id = "provider",
                title = stringResource(Res.string.set_provider),
                supporting = stringResource(Res.string.set_provider_sub),
                keywords = listOf("ollama", "claude", "openai", "model", "llm"),
                options = providerLabels,
                selectedIndex = AiProvider.entries.indexOf(settings.aiProvider),
                onSelect = { viewModel.setAiProvider(AiProvider.entries[it]) },
            ),
            SettingsRow.ButtonGroupRow(
                id = "refinement",
                title = stringResource(Res.string.settings_refine_mode),
                keywords = listOf("auto", "suggest", "manual"),
                visible = settings.aiProvider != AiProvider.OFF,
                options = refineLabels.map { SettingsRow.ButtonGroupRow.Option(it) },
                selectedIndex = AiRefineMode.entries.indexOf(settings.aiRefineMode),
                onSelect = { viewModel.setAiRefineMode(AiRefineMode.entries[it]) },
            ),
            SettingsRow.Switch(
                id = "askPage",
                title = stringResource(Res.string.settings_ask),
                supporting = stringResource(Res.string.settings_ask_desc),
                keywords = listOf("chat", "assistant", "ask"),
                visible = settings.aiProvider != AiProvider.OFF,
                checked = settings.aiAskEnabled,
                onCheckedChange = viewModel::setAiAskEnabled,
            ),
            SettingsRow.Switch(
                id = "wifiOnly",
                                title = stringResource(Res.string.set_wifi_only),
                supporting = stringResource(Res.string.set_wifi_only_sub),
                keywords = listOf("data", "cellular", "mobile"),
                visible = settings.aiProvider != AiProvider.OFF && settings.aiProvider != AiProvider.OLLAMA,
                checked = settings.aiWifiOnly,
                onCheckedChange = viewModel::setAiWifiOnly,
            ),
            SettingsRow.Custom(
                id = "aiConfig",
                title = stringResource(Res.string.ai_model),
                keywords = listOf("api key", "base url", "endpoint", "workspace"),
                visible = settings.aiProvider != AiProvider.OFF,
                content = { AiConfigCollapse(settings, viewModel, snackbarHostState) },
            ),
        ),
    )

    val organisation = SettingsSection(
        id = "organisation",
        title = stringResource(Res.string.sec_organisation),
        rows = listOf(
            SettingsRow.Navigation(
                id = "tags",
                title = stringResource(Res.string.set_tags),
                supporting = stringResource(Res.string.set_tags_sub, stats.tags),
                keywords = listOf("label", "hashtag", "rename", "merge"),
                onClick = { onNavigate(SettingsDestination.TAGS) },
            ),
            SettingsRow.Switch(
                id = "showCompleted",
                                title = stringResource(Res.string.set_show_completed),
                supporting = stringResource(Res.string.set_show_completed_sub),
                keywords = listOf("done", "finished"),
                checked = settings.showCompletedInToday,
                onCheckedChange = viewModel::setShowCompletedInToday,
            ),
            SettingsRow.Switch(
                id = "searchCompleted",
                title = stringResource(Res.string.set_search_completed),
                supporting = stringResource(Res.string.set_search_completed_sub),
                keywords = listOf("done", "find"),
                checked = settings.searchCompleted,
                onCheckedChange = viewModel::setSearchCompleted,
            ),
        ),
    )

    val privacy = SettingsSection(
        id = "privacy",
        title = stringResource(Res.string.sec_privacy),
        visible = Platform.isAndroid,
        rows = listOf(
            SettingsRow.Switch(
                id = "appLock",
                                title = stringResource(Res.string.set_app_lock),
                supporting = stringResource(Res.string.set_app_lock_sub, settings.appLockGraceSeconds / 60),
                keywords = listOf("biometric", "fingerprint", "lock", "security"),
                checked = settings.appLock,
                onCheckedChange = viewModel::setAppLock,
            ),
            SettingsRow.Switch(
                id = "hideSwitcher",
                                title = stringResource(Res.string.set_hide_switcher),
                supporting = stringResource(Res.string.set_hide_switcher_sub),
                keywords = listOf("recents", "privacy", "secure"),
                checked = settings.hideInAppSwitcher,
                onCheckedChange = viewModel::setHideInAppSwitcher,
            ),
        ),
    )

    val data = SettingsSection(
        id = "data",
        title = stringResource(Res.string.sec_data),
        rows = listOf(
            SettingsRow.External(
                id = "export",
                title = stringResource(Res.string.export_data),
                supporting = stringResource(Res.string.set_export_sub, stats.items),
                keywords = listOf("backup", "save", "json"),
                onClick = onExport,
            ),
            SettingsRow.External(
                id = "import",
                title = stringResource(Res.string.import_data),
                supporting = stringResource(Res.string.set_import_sub),
                keywords = listOf("backup", "restore"),
                onClick = onImport,
            ),
            SettingsRow.Switch(
                id = "autoBackup",
                                title = stringResource(Res.string.set_auto_backup),
                supporting = stringResource(Res.string.set_auto_backup_sub),
                keywords = listOf("backup", "weekly", "automatic"),
                visible = Platform.isAndroid,
                checked = settings.autoBackup,
                onCheckedChange = viewModel::setAutoBackup,
            ),
            SettingsRow.Navigation(
                id = "trash",
                title = stringResource(Res.string.set_trash),
                supporting = stringResource(Res.string.set_trash_sub, stats.trashed, retentionLabel.lowercase()),
                keywords = listOf("deleted", "bin", "restore", "recover"),
                onClick = { onNavigate(SettingsDestination.TRASH) },
            ),
            SettingsRow.Destructive(
                id = "clearCompleted",
                title = stringResource(Res.string.set_clear_completed),
                supporting = stringResource(Res.string.set_clear_completed_sub, stats.completed),
                keywords = listOf("done", "tidy", "clean"),
                visible = stats.completed > 0,
                actionLabel = stringResource(Res.string.set_clear),
                onAction = onClearCompleted,
            ),
            SettingsRow.Custom(
                id = "deleteAll",
                title = stringResource(Res.string.set_delete_all),
                keywords = listOf("wipe", "reset", "erase"),
                inGroup = false,
                content = { DeleteEverythingCard(viewModel, snackbarHostState) },
            ),
        ),
    )

    val desktop = SettingsSection(
        id = "desktop",
        title = stringResource(Res.string.sec_desktop),
        visible = Platform.isDesktop,
        rows = listOf(
            SettingsRow.Value(
                // a true global hotkey needs a native key hook — a new dependency the brief rules out
                id = "quickShortcut",
                enabled = false,
                title = stringResource(Res.string.set_quick_shortcut),
                supporting = stringResource(Res.string.set_quick_shortcut_sub),
                keywords = listOf("hotkey", "global", "shortcut"),
            ),
            SettingsRow.Switch(
                id = "launchAtLogin",
                                title = stringResource(Res.string.set_launch_login),
                supporting = stringResource(Res.string.set_launch_login_sub),
                keywords = listOf("startup", "boot", "tray"),
                checked = settings.launchAtLogin,
                onCheckedChange = viewModel::setLaunchAtLogin,
            ),
            SettingsRow.Switch(
                id = "closeToTray",
                                title = stringResource(Res.string.set_close_tray),
                supporting = stringResource(Res.string.set_close_tray_sub),
                keywords = listOf("tray", "background"),
                checked = settings.closeToTray,
                onCheckedChange = viewModel::setCloseToTray,
            ),
            SettingsRow.Navigation(
                id = "shortcuts",
                title = stringResource(Res.string.set_shortcuts),
                supporting = stringResource(Res.string.set_shortcuts_sub, 12),
                keywords = listOf("keys", "bindings"),
                onClick = { onNavigate(SettingsDestination.SHORTCUTS) },
            ),
        ),
    )

    val about = SettingsSection(
        id = "about",
        title = stringResource(Res.string.sec_about),
        rows = listOf(
            SettingsRow.Value(
                id = "version",
                title = stringResource(Res.string.set_version),
                supporting = APP_VERSION,
                keywords = listOf("build", "release"),
            ),
            SettingsRow.Navigation(
                id = "whatsNew",
                title = stringResource(Res.string.set_whats_new),
                keywords = listOf("changelog", "release notes"),
                onClick = { onNavigate(SettingsDestination.WHATS_NEW) },
            ),
            SettingsRow.Navigation(
                id = "licenses",
                title = stringResource(Res.string.set_licenses),
                keywords = listOf("open source", "attribution"),
                onClick = { onNavigate(SettingsDestination.LICENSES) },
            ),
            SettingsRow.External(
                id = "diagnostics",
                enabled = false,
                title = stringResource(Res.string.set_diagnostics),
                supporting = stringResource(Res.string.set_diagnostics_sub),
                keywords = listOf("logs", "crash", "debug"),
                onClick = { },
            ),
        ),
    )

    return listOf(
        general, appearance, capture, dateTime, notifications,
        ai, organisation, privacy, data, desktop, about,
    )
}

/** A picker, not a confirmation — the house rule bans dialogs that only ask "are you sure". */
@Composable
private fun SettingsTimePicker(
    initialMinutes: Int,
    use24h: Boolean,
    onDismiss: () -> Unit,
    onPicked: (Int) -> Unit,
) {
    val state = androidx.compose.material3.rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = use24h,
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPicked(state.hour * 60 + state.minute) }) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
        text = { androidx.compose.material3.TimePicker(state = state) },
    )
}

/**
 * Collapsed status line that expands to the credentials. Keeps six fields out of
 * the way once the provider works, which is the normal state.
 */
@Composable
private fun AiConfigCollapse(
    settings: Settings,
    viewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
) {
    var expanded by remember { mutableStateOf(settings.aiApiKey.isBlank() && settings.aiModel.isBlank()) }
    var testing by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<Boolean?>(null) }
    var keyVisible by remember { mutableStateOf(false) }
    var showWorkspace by remember { mutableStateOf(settings.aiWorkspaceId.isNotBlank()) }
    var modelMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    var baseUrl by remember(settings.aiProvider) { mutableStateOf(settings.aiBaseUrl) }
    var model by remember(settings.aiProvider) { mutableStateOf(settings.aiModel) }
    var apiKey by remember(settings.aiProvider) { mutableStateOf(settings.aiApiKey) }
    var workspace by remember(settings.aiProvider) { mutableStateOf(settings.aiWorkspaceId) }
    var instructions by remember(settings.aiProvider) { mutableStateOf(settings.aiInstructions) }

    val okText = stringResource(Res.string.ai_test_ok)
    val failText = stringResource(Res.string.ai_test_fail)
    val isAnthropic = settings.aiProvider == AiProvider.ANTHROPIC
    val statusDot = when (lastResult) {
        true -> MaterialTheme.colorScheme.tertiary
        false -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.primary
    }
    val statusText = listOfNotNull(
        if (settings.aiApiKey.isNotBlank()) stringResource(Res.string.ai_key_saved)
        else stringResource(Res.string.ai_no_key),
        when (lastResult) {
            true -> stringResource(Res.string.ai_connected)
            false -> stringResource(Res.string.ai_failed)
            null -> stringResource(Res.string.ai_untested)
        },
    ).joinToString(" · ")

    Column {
        ListItem(
            headlineContent = {
                Text(
                    ANTHROPIC_MODELS.firstOrNull { it.id == settings.aiModel }?.label
                        ?: settings.aiModel.ifBlank { stringResource(Res.string.ai_model) },
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            supportingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(statusDot, CircleShape))
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            },
            trailingContent = {
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .clickable { expanded = !expanded }
                .semantics(mergeDescendants = true) {},
        )
        AnimatedVisibility(visible = expanded, enter = expandEnter(), exit = expandExit()) {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = if (isAnthropic) ANTHROPIC_DEFAULT_BASE_URL else baseUrl,
                    onValueChange = { baseUrl = it; viewModel.setAiBaseUrl(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.ai_base_url)) },
                    singleLine = true,
                    enabled = !isAnthropic,
                )
                if (isAnthropic) {
                    ExposedDropdownMenuBox(expanded = modelMenu, onExpandedChange = { modelMenu = it }) {
                        OutlinedTextField(
                            value = ANTHROPIC_MODELS.firstOrNull { it.id == model }?.label ?: model,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            label = { Text(stringResource(Res.string.ai_model)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelMenu) },
                            singleLine = true,
                        )
                        ExposedDropdownMenu(
                            expanded = modelMenu,
                            onDismissRequest = { modelMenu = false },
                        ) {
                            ANTHROPIC_MODELS.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        model = option.id
                                        viewModel.setAiModel(option.id)
                                        modelMenu = false
                                    },
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it; viewModel.setAiModel(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.ai_model)) },
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; viewModel.setAiApiKey(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.ai_api_key)) },
                    singleLine = true,
                    visualTransformation = if (keyVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconToggleButton(
                            checked = keyVisible,
                            onCheckedChange = { keyVisible = it },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                if (keyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                )
                if (isAnthropic) {
                    if (showWorkspace) {
                        OutlinedTextField(
                            value = workspace,
                            onValueChange = { workspace = it; viewModel.setAiWorkspaceId(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(Res.string.ai_workspace_id)) },
                            singleLine = true,
                        )
                    } else {
                        TextButton(onClick = { showWorkspace = true }) {
                            Text(stringResource(Res.string.ai_workspace_id))
                        }
                    }
                }
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it; viewModel.setAiInstructions(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.settings_ai_instructions)) },
                    minLines = 2,
                )
                Text(
                    stringResource(Res.string.ai_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        testing = true
                        viewModel.testAi { error ->
                            testing = false
                            lastResult = error == null
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (error == null) okText else "$failText — $error",
                                    duration = androidx.compose.material3.SnackbarDuration.Long,
                                )
                            }
                        }
                    },
                    enabled = !testing,
                ) {
                    if (testing) {
                        // TODO(expressive): LoadingIndicator once material3 ships it
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(Res.string.ai_test))
                    }
                }
            }
        }
    }
}

/**
 * Hold-to-confirm instead of a dialog: a tap can't fire it, releasing early cancels.
 * TalkBack gets an equivalent long-press action rather than a moving target.
 */
@Composable
private fun DeleteEverythingCard(viewModel: SettingsViewModel, snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    val deletedText = stringResource(Res.string.set_deleted_all)
    val holdLabel = stringResource(Res.string.set_delete_all_action)

    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(Res.string.set_delete_all),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                stringResource(Res.string.set_delete_all_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            HoldToConfirm(
                label = holdLabel,
                onConfirm = {
                    viewModel.deleteEverything()
                    scope.launch { snackbarHostState.showSnackbar(deletedText) }
                },
            )
        }
    }
}
