package com.tina.app.ask

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.tina.app.LocalSettings
import com.tina.app.data.AiProvider
import com.tina.app.resources.Res
import com.tina.app.resources.ask_error
import com.tina.app.resources.ask_hint
import com.tina.app.resources.ask_placeholder
import com.tina.app.resources.ask_reason_balanced
import com.tina.app.resources.ask_reason_quick
import com.tina.app.resources.ask_reason_thorough
import com.tina.app.resources.ask_retry
import com.tina.app.resources.ask_sugg_1
import com.tina.app.resources.ask_sugg_2
import com.tina.app.resources.ask_sugg_3
import com.tina.app.resources.ask_sugg_4
import com.tina.app.resources.ask_applied
import com.tina.app.resources.ask_chat_deleted
import com.tina.app.resources.ask_copied
import com.tina.app.resources.ask_history
import com.tina.app.resources.ask_history_empty
import com.tina.app.resources.ask_new_chat
import com.tina.app.resources.ask_thinking
import com.tina.app.resources.delete
import com.tina.app.resources.ask_write_off
import com.tina.app.resources.ask_write_on
import com.tina.app.resources.capture_save
import com.tina.app.resources.tab_ask
import com.tina.app.resources.undo
import com.tina.app.ai.ANTHROPIC_MODELS
import com.tina.app.ai.ChatRole
import com.tina.app.ai.ReasoningLevel
import com.tina.app.ui.relativeAge
import kotlin.time.Clock
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)
@Composable
fun AskScreen(viewModel: AskViewModel = koinViewModel()) {
    val settings = LocalSettings.current
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var modelMenuOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val undoWindow = rememberUndoWindow()
    val appliedText = stringResource(Res.string.ask_applied)
    val undoText = stringResource(Res.string.undo)

    var showHistory by remember { mutableStateOf(false) }
    val chatDeletedText = stringResource(Res.string.ask_chat_deleted)
    val copiedText = stringResource(Res.string.ask_copied)
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel.appliedNonce) {
        if (viewModel.appliedNonce == 0) return@LaunchedEffect
        if (snackbarHostState.showUndo("$appliedText: ${viewModel.appliedCount}", undoText, undoWindow)) viewModel.undoLastBatch()
    }

    LaunchedEffect(viewModel.chatDeletedNonce) {
        if (viewModel.chatDeletedNonce == 0) return@LaunchedEffect
        if (snackbarHostState.showUndo(chatDeletedText, undoText, undoWindow)) viewModel.undoDeleteChat()
    }

    LaunchedEffect(viewModel.messages.size, viewModel.sending) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size)
        }
    }

    fun sendNow(text: String = input) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || viewModel.sending) return
        input = ""
        viewModel.send(trimmed)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.tab_ask)) },
                actions = {
                    if (settings.aiProvider == AiProvider.ANTHROPIC) {
                        Box {
                            TextButton(onClick = { modelMenuOpen = true }) {
                                Text(
                                    ANTHROPIC_MODELS.firstOrNull { it.id == viewModel.effectiveModel(settings.aiModel) }
                                        ?.label ?: viewModel.effectiveModel(settings.aiModel),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            DropdownMenu(
                                expanded = modelMenuOpen,
                                onDismissRequest = { modelMenuOpen = false },
                            ) {
                                ANTHROPIC_MODELS.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            viewModel.setModelOverride(option.id)
                                            modelMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Outlined.History, stringResource(Res.string.ask_history))
                    }
                    IconButton(
                        onClick = viewModel::newChat,
                        enabled = viewModel.messages.isNotEmpty(),
                    ) {
                        Icon(
                            Icons.Outlined.AddComment,
                            stringResource(Res.string.ask_new_chat),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(Res.string.ask_placeholder)) },
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    maxLines = 4,
                )
                FilledIconButton(
                    onClick = { sendNow() },
                    modifier = Modifier.padding(start = 8.dp),
                    enabled = !viewModel.sending && input.isNotBlank(),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Send, stringResource(Res.string.capture_save))
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReasoningLevel.entries.forEach { level ->
                    FilterChip(
                        selected = viewModel.reasoning == level,
                        onClick = { viewModel.setReasoningLevel(level) },
                        label = {
                            Text(
                                stringResource(
                                    when (level) {
                                        ReasoningLevel.QUICK -> Res.string.ask_reason_quick
                                        ReasoningLevel.BALANCED -> Res.string.ask_reason_balanced
                                        ReasoningLevel.THOROUGH -> Res.string.ask_reason_thorough
                                    },
                                ),
                            )
                        },
                    )
                }
                val writeOn = settings.aiAskWriteEnabled
                FilterChip(
                    selected = writeOn,
                    onClick = { viewModel.setWriteEnabled(!writeOn) },
                    label = {
                        Text(
                            stringResource(
                                if (writeOn) Res.string.ask_write_on else Res.string.ask_write_off,
                            ),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (writeOn) Icons.Outlined.Edit else Icons.Outlined.Lock,
                            contentDescription = null,
                            Modifier.size(16.dp),
                        )
                    },
                )
            }
            if (viewModel.messages.isEmpty() && !viewModel.sending) {
                AskEmptyState(onAsk = ::sendNow)
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp, vertical = 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(viewModel.messages) { message ->
                        val fromUser = message.role == ChatRole.USER
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
                        ) {
                            Text(
                                message.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (fromUser) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier
                                    .widthIn(max = 320.dp)
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            clipboard.setText(AnnotatedString(message.content))
                                            scope.launch { snackbarHostState.showSnackbar(copiedText) }
                                        },
                                    )
                                    .background(
                                        if (fromUser) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerLow
                                        },
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (fromUser) 16.dp else 4.dp,
                                            bottomEnd = if (fromUser) 4.dp else 16.dp,
                                        ),
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                        }
                    }
                    if (viewModel.sending) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(Modifier.size(18.dp))
                                Text(
                                    stringResource(Res.string.ask_thinking),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (viewModel.lastFailed) {
                        item {
                            Column {
                                Text(
                                    stringResource(Res.string.ask_error),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                OutlinedButton(onClick = viewModel::retry) {
                                    Text(stringResource(Res.string.ask_retry))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHistory) {
        val historyChats = viewModel.history.collectAsState().value
        val nowMillis = remember(historyChats) { Clock.System.now().toEpochMilliseconds() }
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showHistory = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    stringResource(Res.string.ask_history),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (historyChats.isEmpty()) {
                    Text(
                        stringResource(Res.string.ask_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp),
                    )
                }
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    items(historyChats) { entry ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(onClick = {
                                    viewModel.openChat(entry.id)
                                    showHistory = false
                                })
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                Text(
                                    relativeAge(nowMillis - entry.updatedAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { viewModel.deleteChat(entry.id) }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    stringResource(Res.string.delete),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AskEmptyState(onAsk: (String) -> Unit) {
    val suggestions = listOf(
        stringResource(Res.string.ask_sugg_1),
        stringResource(Res.string.ask_sugg_2),
        stringResource(Res.string.ask_sugg_3),
        stringResource(Res.string.ask_sugg_4),
    )
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(Res.string.ask_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
        )
        suggestions.forEach { suggestion ->
            SuggestionChip(
                onClick = { onAsk(suggestion) },
                label = { Text(suggestion) },
            )
        }
    }
}
