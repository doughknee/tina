package com.tina.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import com.tina.app.ui.capture.CaptureChips
import com.tina.app.ui.capture.CaptureSuggestions
import androidx.compose.animation.togetherWith
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import com.tina.app.resources.draft_discard
import com.tina.app.resources.draft_discard_title
import com.tina.app.resources.draft_keep
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
import com.tina.app.resources.mode_capture
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
    /** A mode, not a page: focuses the capture field over whatever is showing. */
    CAPTURE(Icons.Filled.Edit, Icons.Outlined.Edit, Res.string.mode_capture),
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
    val tabs = if (askEnabled) TinaTab.entries.toList() else listOf(TinaTab.AGENDA, TinaTab.LIBRARY, TinaTab.CAPTURE)

    // saved by name so toggling the Ask tab never shifts the selection; LAST relies on
    // rememberSaveable surviving process death, the others pin a start page
    var selectedName by rememberSaveable(settings.openAppTo) { mutableStateOf(TinaTab.AGENDA.name) }
    val selectedTab = tabs.firstOrNull { it.name == selectedName && it != TinaTab.ASK && it != TinaTab.CAPTURE }
        ?: TinaTab.AGENDA
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

    // the capture sheet rises while the field has focus and stays while there is a draft:
    // starters when empty, parse chips while typing, and it survives the keyboard going away
    val hasDraft = captureViewModel.text.isNotBlank()
    val suggestionsOpen = !askOpen && (captureFocused || hasDraft)
    var discardPrompt by remember { mutableStateOf(false) }

    // closing Ask also drops focus: a focused field in capture mode would raise the capture sheet
    fun closeAsk() {
        askOpen = false
        focusManager.clearFocus()
    }

    // dismissing with a draft asks first; with nothing typed it just folds
    // reads the view model at call time: this reference gets memoised across recompositions,
    // so a captured `hasDraft` went stale and drags kept seeing an empty field
    fun dismissCaptureSheet() {
        if (captureViewModel.text.isNotBlank()) discardPrompt = true else focusManager.clearFocus()
    }

    fun showTab(tab: TinaTab) {
        selectedName = tab.name
        askOpen = false
        focusManager.clearFocus()
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

    BackHandler(enabled = askOpen) { closeAsk() }
    // the keyboard takes the first back itself; the next one, with a draft still up, asks
    BackHandler(enabled = !askOpen && hasDraft && !captureFocused) { discardPrompt = true }

    if (discardPrompt) {
        AlertDialog(
            onDismissRequest = { discardPrompt = false },
            title = { Text(stringResource(Res.string.draft_discard_title)) },
            text = { Text(captureViewModel.text, maxLines = 3, overflow = TextOverflow.Ellipsis) },
            confirmButton = {
                TextButton(onClick = {
                    discardPrompt = false
                    captureViewModel.discard()
                    focusManager.clearFocus()
                }) { Text(stringResource(Res.string.draft_discard)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    discardPrompt = false
                    CaptureFocus.request()
                }) { Text(stringResource(Res.string.draft_keep)) }
            },
        )
    }

    NavigationSuiteScaffold(
        // the whole shell (bar and nav included) rides above the keyboard
        modifier = Modifier.imePadding(),
        navigationSuiteItems = {
            tabs.forEach { tab ->
                val selected = when (tab) {
                    TinaTab.ASK -> askOpen
                    TinaTab.CAPTURE -> suggestionsOpen
                    else -> selectedTab == tab && !askOpen && !suggestionsOpen
                }
                item(
                    selected = selected,
                    onClick = {
                        when (tab) {
                            TinaTab.ASK -> askOpen = true
                            TinaTab.CAPTURE -> CaptureFocus.request()
                            else -> showTab(tab)
                        }
                    },
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
                        TinaTab.ASK, TinaTab.CAPTURE -> Unit
                    }
                }

                SaveBurst(trigger = captureViewModel.saveCount, modifier = Modifier.align(Alignment.Center))

                ShellSheet(
                    visible = suggestionsOpen,
                    onDismiss = ::dismissCaptureSheet,
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    AnimatedContent(
                        targetState = captureViewModel.text.isBlank(),
                        transitionSpec = { motion.fadeSwap() },
                        label = "capture-sheet",
                    ) { empty ->
                        if (empty) {
                            CaptureSuggestions(captureViewModel, onOpenItem)
                        } else {
                            CaptureChips(captureViewModel, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        }
                    }
                }

                // Ask: a sheet over the page, with the bar still visible under it in ask mode
                ShellSheet(
                    visible = askOpen,
                    onDismiss = ::closeAsk,
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
    val scope = rememberCoroutineScope()
    val motion = rememberAppMotion()
    val settle = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    // the dragged offset survives a dismiss so the exit slide continues from where the
    // finger left it; resetting it on close made the sheet jump back up first
    val drag = remember { Animatable(0f) }
    // the drag gesture is set up once, so it must read the *current* dismiss handler and
    // visibility — a stale capture kept calling the pre-draft handler
    val currentDismiss by rememberUpdatedState(onDismiss)
    val currentVisible by rememberUpdatedState(visible)
    LaunchedEffect(visible) { if (visible) drag.snapTo(0f) }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = motion.fadeEnter(), exit = motion.fadeExit()) {
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
            enter = motion.sheetEnter(),
            exit = motion.sheetExit(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier.fillMaxWidth().offset { IntOffset(0, drag.value.roundToInt()) },
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
                                        if (drag.value > dismissDistance) {
                                            currentDismiss()
                                            // a dismiss that only opened a prompt leaves the sheet up: settle it
                                            scope.launch {
                                                withFrameNanos { }
                                                if (currentVisible) drag.animateTo(0f, settle)
                                            }
                                        } else {
                                            scope.launch { drag.animateTo(0f, settle) }
                                        }
                                    },
                                    onDragCancel = { scope.launch { drag.animateTo(0f, settle) } },
                                ) { _, dy ->
                                    scope.launch { drag.snapTo((drag.value + dy).coerceAtLeast(0f)) }
                                }
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
