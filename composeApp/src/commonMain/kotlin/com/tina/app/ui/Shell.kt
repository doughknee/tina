package com.tina.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tina.app.LocalSettings
import com.tina.app.agenda.AgendaScreen
import com.tina.app.ask.AskSheet
import com.tina.app.ask.AskViewModel
import com.tina.app.capture.CaptureViewModel
import com.tina.app.data.AiProvider
import com.tina.app.data.Item
import com.tina.app.inbox.InboxScreen
import com.tina.app.inbox.InboxViewModel
import com.tina.app.notes.NotesScreen
import com.tina.app.notes.NotesViewModel
import com.tina.app.resources.Res
import com.tina.app.resources.draft_discard
import com.tina.app.resources.draft_discard_title
import com.tina.app.resources.draft_keep
import com.tina.app.resources.tab_sort
import com.tina.app.resources.tab_agenda
import com.tina.app.resources.tab_notes
import com.tina.app.resources.undo
import com.tina.app.search.SearchSheet
import com.tina.app.search.SearchViewModel
import com.tina.app.ui.capture.CaptureBar
import com.tina.app.ui.capture.CaptureChips
import com.tina.app.ui.capture.CaptureModeToggle
import com.tina.app.ui.capture.IdeaBody
import com.tina.app.ui.capture.CaptureSuggestions
import com.tina.app.ui.capture.SaveBurst
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.datetime.number
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// Selected nav item keeps the Filled variant (M3 active-state convention).
enum class TinaTab(val icon: ImageVector, val outlinedIcon: ImageVector, val label: StringResource) {
    AGENDA(Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, Res.string.tab_agenda),
    INBOX(Icons.Filled.Inbox, Icons.Outlined.Inbox, Res.string.tab_sort),
    NOTES(Icons.AutoMirrored.Filled.Notes, Icons.AutoMirrored.Outlined.Notes, Res.string.tab_notes),
}

/**
 * The app's three verbs as destinations: plan (Agenda), sort (Inbox), write (Notes).
 * Capture and Ask are modes of the bar pinned above the nav, and Search is a sheet, so none
 * of them takes a slot. The bar sits on every page, so capture is zero taps from anywhere.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Shell(
    onOpenSettings: () -> Unit,
    onOpenItem: (Item) -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenTag: (String) -> Unit,
) {
    val settings = LocalSettings.current
    val askAvailable = settings.aiAskEnabled && settings.aiProvider != AiProvider.OFF

    // saved by name; LAST relies on rememberSaveable surviving process death, the others pin a start page
    var selectedName by rememberSaveable(settings.openAppTo) { mutableStateOf(TinaTab.AGENDA.name) }
    val selectedTab = TinaTab.entries.firstOrNull { it.name == selectedName } ?: TinaTab.AGENDA
    // deliberately not saveable: the bar always comes back in capture mode
    var askOpen by remember { mutableStateOf(false) }
    // the Ask overlay; closing it (back, scrim, drag) leaves the bar in ask mode, so back
    // walks keyboard -> overlay -> page the same way it does for capture
    var askSheetOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var captureFocused by remember { mutableStateOf(false) }
    // opened when the field takes focus, closed only by scrim / handle / back — putting the
    // keyboard away leaves it up, so the starters stay in reach
    var captureSheetOpen by remember { mutableStateOf(false) }

    val captureViewModel: CaptureViewModel = koinViewModel()
    val askViewModel: AskViewModel = koinViewModel()
    val inboxViewModel: InboxViewModel = koinViewModel()
    val notesViewModel: NotesViewModel = koinViewModel()
    val searchViewModel: SearchViewModel = koinViewModel()
    val inboxCount by inboxViewModel.count.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val undoText = stringResource(Res.string.undo)
    val undoWindow = rememberUndoWindow()
    // deletes from the item and event pages land here, so the undo outlives the page
    LaunchedEffect(Unit) {
        PendingUndo.events.collect { request ->
            if (snackbarHostState.showUndo(request.message, undoText, undoWindow)) request.undo()
        }
    }
    val captureFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // the capture sheet rises while the field has focus and stays while there is a draft:
    // starters when empty, parse chips while typing, and it survives the keyboard going away
    val hasDraft = captureViewModel.text.isNotBlank()
    val suggestionsOpen = !askOpen && !searchOpen && (captureSheetOpen || hasDraft)
    val askVisible = askOpen && askSheetOpen
    var discardPrompt by remember { mutableStateOf(false) }

    // closes the overlay only; the pill stays on Ask until it is tapped back to Plan
    fun closeAsk() {
        askSheetOpen = false
        focusManager.clearFocus()
    }

    fun setAskMode(on: Boolean) {
        askOpen = on && askAvailable
        askSheetOpen = askOpen
    }

    fun openSearch() {
        askOpen = false
        askSheetOpen = false
        focusManager.clearFocus()
        searchOpen = true
    }

    fun closeSearch() {
        searchOpen = false
        focusManager.clearFocus()
    }

    // reads the view model at call time: this reference gets memoised across recompositions,
    // so a captured `hasDraft` went stale and drags kept seeing an empty field
    fun dismissCaptureSheet() {
        if (captureViewModel.text.isNotBlank()) {
            discardPrompt = true
        } else {
            captureSheetOpen = false
            focusManager.clearFocus()
        }
    }

    fun showTab(tab: TinaTab) {
        selectedName = tab.name
        askOpen = false
        askSheetOpen = false
        searchOpen = false
        captureSheetOpen = false
        focusManager.clearFocus()
    }
    val sortRequested by OpenSortRequests.pending.collectAsState()
    LaunchedEffect(sortRequested) {
        if (sortRequested) {
            OpenSortRequests.clear()
            showTab(TinaTab.INBOX)
        }
    }

    val focusRequested by CaptureFocus.pending.collectAsState()
    LaunchedEffect(focusRequested) {
        if (!focusRequested) return@LaunchedEffect
        askOpen = false
        askSheetOpen = false
        captureViewModel.switchIdeaMode(CaptureFocus.idea)
        CaptureFocus.prefill?.let(captureViewModel::prefill)
        searchOpen = false
        captureFocus.requestFocus()
        CaptureFocus.clear()
    }

    LaunchedEffect(Unit) {
        KeyBus.events.collect { command ->
            when (command) {
                KeyCommand.FOCUS_CAPTURE -> CaptureFocus.request()
                KeyCommand.SEARCH -> openSearch()
                KeyCommand.NEW_ITEM ->
                    if (selectedName == TinaTab.NOTES.name) notesViewModel.createNote(onOpenNote) else CaptureFocus.request()
                else -> Unit
            }
        }
    }

    BackHandler(enabled = searchOpen) { closeSearch() }
    BackHandler(enabled = askVisible && !captureFocused) { closeAsk() }
    // the keyboard takes the first back itself; the next one, with a draft still up, asks
    BackHandler(enabled = !askOpen && !searchOpen && suggestionsOpen && !captureFocused) { dismissCaptureSheet() }

    if (discardPrompt) {
        AlertDialog(
            onDismissRequest = { discardPrompt = false },
            title = { Text(stringResource(Res.string.draft_discard_title)) },
            text = { Text(captureViewModel.text, maxLines = 3, overflow = TextOverflow.Ellipsis) },
            confirmButton = {
                TextButton(onClick = {
                    discardPrompt = false
                    captureViewModel.discard()
                    captureSheetOpen = false
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
        // the whole shell (bar and nav included) is padded for the keyboard by the platform's
        // own inset animation; imePadding consumes the inset, so the nav bar inside pads nothing
        // extra. A hand-rolled lift (offset by ime minus nav-bar inset) was faster on paper but
        // drifted from the keyboard on device; see DECISIONS "The keyboard lifts the shell".
        modifier = Modifier.imePadding(),
        navigationSuiteItems = {
            TinaTab.entries.forEach { tab ->
                val selected = selectedTab == tab && !askVisible && !searchOpen
                item(
                    selected = selected,
                    onClick = { showTab(tab) },
                    icon = {
                        BadgedBox(
                            badge = {
                                // the nav badge is the "you have things to sort" signal
                                if (tab == TinaTab.INBOX && inboxCount > 0) Badge { Text(inboxCount.toString()) }
                            },
                        ) {
                            Icon(if (selected) tab.icon else tab.outlinedIcon, contentDescription = null)
                        }
                    },
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
                // the search sheet has its own field; two stacked fields would fight for the keyboard
                if (!searchOpen) {
                    CaptureBar(
                        askMode = askOpen,
                        onAskModeChange = ::setAskMode,
                        askAvailable = askAvailable,
                        onAskSend = {
                            askSheetOpen = true
                            askViewModel.send(it)
                        },
                        askBusy = askViewModel.sending,
                        snackbarHostState = snackbarHostState,
                        focusRequester = captureFocus,
                        onFocusChanged = {
                            captureFocused = it
                            if (it) {
                                if (askOpen) askSheetOpen = true else captureSheetOpen = true
                            }
                        },
                        blendWithSheet = suggestionsOpen || askVisible,
                        onOpenNote = onOpenNote,
                        viewModel = captureViewModel,
                    )
                }
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
                            onOpenSearch = ::openSearch,
                            onOpenInbox = { showTab(TinaTab.INBOX) },
                            onOpenItem = onOpenItem,
                            onCaptureForDate = { date ->
                                // a parser-friendly date token keeps capture one flow
                                captureViewModel.prefill("${date.month.number}/${date.day} ")
                                CaptureFocus.request()
                            },
                        )
                        TinaTab.INBOX -> InboxScreen(
                            onOpenSettings = onOpenSettings,
                            onOpenItem = onOpenItem,
                            viewModel = inboxViewModel,
                        )
                        TinaTab.NOTES -> NotesScreen(
                            onOpenSettings = onOpenSettings,
                            onOpenNote = onOpenNote,
                            onOpenTag = onOpenTag,
                            viewModel = notesViewModel,
                        )
                    }
                }

                ShellSheet(
                    visible = suggestionsOpen,
                    onDismiss = ::dismissCaptureSheet,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    // it rose on its own and then rose again with the keyboard: two motions for one tap
                    enter = motion.fadeEnter(),
                ) {
                    Column {
                        AnimatedContent(
                            targetState = captureViewModel.text.isBlank(),
                            transitionSpec = { motion.fadeSwap() },
                            label = "capture-sheet",
                        ) { empty ->
                            when {
                                empty -> CaptureSuggestions(captureViewModel, onOpenItem)
                                captureViewModel.ideaMode ->
                                    IdeaBody(captureViewModel, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
                                else -> CaptureChips(captureViewModel, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            }
                        }
                        // last, right above the field: the sheet is bottom-anchored, so this is the one
                        // spot that stays put however many recents are above it
                        CaptureModeToggle(captureViewModel, Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp))
                    }
                }

                // Ask: a sheet over the page, with the bar still visible under it in ask mode
                ShellSheet(
                    visible = askVisible,
                    onDismiss = ::closeAsk,
                    modifier = Modifier.align(Alignment.BottomCenter).fractionHeight(0.72f),
                ) {
                    AskSheet(viewModel = askViewModel, snackbarHostState = snackbarHostState)
                }

                // Search: a sheet with its own field, over the page you were on
                ShellSheet(
                    visible = searchOpen,
                    onDismiss = ::closeSearch,
                    modifier = Modifier.align(Alignment.BottomCenter).fractionHeight(0.88f),
                ) {
                    SearchSheet(
                        viewModel = searchViewModel,
                        onOpenItem = {
                            closeSearch()
                            onOpenItem(it)
                        },
                    )
                }

                // last, so the celebration draws over whichever sheet is up
                SaveBurst(trigger = captureViewModel.saveCount, modifier = Modifier.align(Alignment.Center))
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
    /** Slide by default; the capture sheet fades, because the keyboard is already lifting it. */
    enter: EnterTransition? = null,
    content: @Composable () -> Unit,
) {
    val dismissDistance = with(LocalDensity.current) { 80.dp.toPx() }
    // the sheet overshoots on its spring; a tail tucked behind the bar keeps the join closed
    val tailPx = with(LocalDensity.current) { SHEET_TAIL.roundToPx() }
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
            enter = enter ?: motion.sheetEnter(),
            exit = motion.sheetExit(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp).offset { IntOffset(0, drag.value.roundToInt() + tailPx) },
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
                    Spacer(Modifier.height(SHEET_TAIL))
                }
            }
        }
    }
}

private val SHEET_TAIL = 32.dp

/** A sheet that fills a fraction of the height above the keyboard (the shell is already padded for it). */
private fun Modifier.fractionHeight(fraction: Float): Modifier = layout { measurable, constraints ->
    val height = (constraints.maxHeight * fraction).toInt().coerceAtLeast(0)
    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}
