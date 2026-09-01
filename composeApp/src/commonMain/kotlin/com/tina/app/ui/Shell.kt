package com.tina.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import com.tina.app.ui.capture.CaptureSuggestions
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tina.app.LocalSettings
import com.tina.app.agenda.AgendaScreen
import com.tina.app.ask.AskSheet
import com.tina.app.ask.AskViewModel
import com.tina.app.capture.CaptureViewModel
import com.tina.app.data.AiProvider
import com.tina.app.data.Item
import com.tina.app.library.LibraryFilter
import com.tina.app.library.LibraryScreen
import com.tina.app.library.LibraryViewModel
import com.tina.app.notes.NotesViewModel
import com.tina.app.resources.Res
import com.tina.app.resources.tab_agenda
import com.tina.app.resources.tab_ask
import com.tina.app.resources.tab_library
import com.tina.app.ui.capture.CaptureBar
import com.tina.app.ui.capture.SaveBurst
import kotlinx.datetime.number
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// Selected nav item keeps the Filled variant (M3 active-state convention).
enum class TinaTab(val icon: ImageVector, val outlinedIcon: ImageVector, val label: StringResource) {
    AGENDA(Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, Res.string.tab_agenda),
    LIBRARY(Icons.Filled.Inventory2, Icons.Outlined.Inventory2, Res.string.tab_library),
    ASK(Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, Res.string.tab_ask),
}

/**
 * Three destinations: Agenda and Library are pages, Ask is a sheet over whichever is showing.
 * The capture bar sits above the nav bar on all of them, so capture is zero taps from anywhere.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Shell(
    onOpenSettings: () -> Unit,
    onOpenItem: (Item) -> Unit,
    onOpenNote: (Long) -> Unit,
) {
    val settings = LocalSettings.current
    val askEnabled = settings.aiAskEnabled && settings.aiProvider != AiProvider.OFF
    val tabs = if (askEnabled) TinaTab.entries.toList() else listOf(TinaTab.AGENDA, TinaTab.LIBRARY)

    // saved by name so toggling the Ask tab never shifts the selection; LAST relies on
    // rememberSaveable surviving process death, the others pin a start page
    var selectedName by rememberSaveable(settings.openAppTo) { mutableStateOf(TinaTab.AGENDA.name) }
    val selectedTab = tabs.firstOrNull { it.name == selectedName && it != TinaTab.ASK } ?: TinaTab.AGENDA
    // deliberately not saveable: the bar always comes back in capture mode
    var askOpen by remember { mutableStateOf(false) }
    var captureFocused by remember { mutableStateOf(false) }
    var searchFocusNonce by remember { mutableIntStateOf(0) }

    val captureViewModel: CaptureViewModel = koinViewModel()
    val askViewModel: AskViewModel = koinViewModel()
    val libraryViewModel: LibraryViewModel = koinViewModel()
    val notesViewModel: NotesViewModel = koinViewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val captureFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    fun showTab(tab: TinaTab) {
        selectedName = tab.name
        askOpen = false
    }

    fun openLibrary(filter: LibraryFilter? = null, focusSearch: Boolean = false) {
        filter?.let(libraryViewModel::setFilter)
        showTab(TinaTab.LIBRARY)
        if (focusSearch) searchFocusNonce++
    }

    val focusRequested by CaptureFocus.pending.collectAsState()
    LaunchedEffect(focusRequested) {
        if (!focusRequested) return@LaunchedEffect
        askOpen = false
        captureFocus.requestFocus()
        CaptureFocus.clear()
    }

    LaunchedEffect(Unit) {
        KeyBus.events.collect { command ->
            when (command) {
                KeyCommand.FOCUS_CAPTURE -> CaptureFocus.request()
                KeyCommand.SEARCH -> openLibrary(focusSearch = true)
                KeyCommand.NEW_ITEM ->
                    if (selectedName == TinaTab.LIBRARY.name && libraryViewModel.filter.value == LibraryFilter.NOTES) {
                        notesViewModel.createNote(onOpenNote)
                    } else {
                        CaptureFocus.request()
                    }
                else -> Unit
            }
        }
    }

    BackHandler(enabled = askOpen) { askOpen = false }

    NavigationSuiteScaffold(
        // the whole shell (bar and nav included) rides above the keyboard
        modifier = Modifier.imePadding(),
        navigationSuiteItems = {
            tabs.forEach { tab ->
                val selected = if (tab == TinaTab.ASK) askOpen else selectedTab == tab && !askOpen
                item(
                    selected = selected,
                    onClick = { if (tab == TinaTab.ASK) askOpen = true else showTab(tab) },
                    icon = { Icon(if (selected) tab.icon else tab.outlinedIcon, contentDescription = null) },
                    label = { Text(stringResource(tab.label)) },
                )
            }
        },
    ) {
        Scaffold(
            // the screens inside carry their own status-bar inset; adding it here doubled it
            contentWindowInsets = WindowInsets(0.dp),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                CaptureBar(
                    askMode = askOpen,
                    onAskModeChange = { askOpen = it && askEnabled },
                    onAskSend = askViewModel::send,
                    askBusy = askViewModel.sending,
                    snackbarHostState = snackbarHostState,
                    focusRequester = captureFocus,
                    onFocusChanged = { captureFocused = it },
                    viewModel = captureViewModel,
                )
            },
        ) { padding ->
            val motion = rememberAppMotion()
            Box(Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = { motion.lateral(targetState.ordinal > initialState.ordinal) },
                ) { tab ->
                    when (tab) {
                        TinaTab.AGENDA -> AgendaScreen(
                            onOpenSettings = onOpenSettings,
                            onOpenSearch = { openLibrary(focusSearch = true) },
                            onOpenInbox = { openLibrary(LibraryFilter.INBOX) },
                            onOpenItem = onOpenItem,
                            onCaptureForDate = { date ->
                                // a parser-friendly date token keeps capture one flow
                                captureViewModel.prefill("${date.month.number}/${date.day} ")
                                CaptureFocus.request()
                            },
                        )
                        TinaTab.LIBRARY -> LibraryScreen(
                            onOpenSettings = onOpenSettings,
                            onOpenItem = onOpenItem,
                            onOpenNote = onOpenNote,
                            searchFocusNonce = searchFocusNonce,
                            viewModel = libraryViewModel,
                            notesViewModel = notesViewModel,
                        )
                        TinaTab.ASK -> Unit
                    }
                }

                SaveBurst(trigger = captureViewModel.saveCount, modifier = Modifier.align(Alignment.Center))

                // the TRY / RECENT sheet rises while the empty capture field has focus
                val suggestionsOpen = !askOpen && captureFocused && captureViewModel.text.isBlank()
                ShellSheet(
                    visible = suggestionsOpen,
                    onDismiss = { focusManager.clearFocus() },
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    CaptureSuggestions(captureViewModel, onOpenItem)
                }

                // Ask: a sheet over the page, with the bar still visible under it in ask mode
                ShellSheet(
                    visible = askOpen,
                    onDismiss = { askOpen = false },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.72f),
                ) {
                    AskSheet(viewModel = askViewModel, snackbarHostState = snackbarHostState)
                }
            }
        }
    }
}

/**
 * A sheet drawn inside the shell rather than a ModalBottomSheet, so the capture bar stays
 * visible and usable under it. Scrim tap, handle tap, a downward drag on the handle, and
 * system back all dismiss.
 */
@Composable
private fun ShellSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dismissDistance = with(LocalDensity.current) { 80.dp.toPx() }
    var drag by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(visible) { drag = 0f }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier.fillMaxWidth().offset { IntOffset(0, drag.roundToInt()) },
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss,
                            )
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (drag > dismissDistance) onDismiss()
                                        drag = 0f
                                    },
                                    onDragCancel = { drag = 0f },
                                ) { _, dy -> drag = (drag + dy).coerceAtLeast(0f) }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .width(32.dp)
                                .height(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(2.dp),
                                ),
                        )
                    }
                    content()
                }
            }
        }
    }
}
