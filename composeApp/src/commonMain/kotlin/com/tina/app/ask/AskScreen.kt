package com.tina.app.ask

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tina.app.LocalSettings
import com.tina.app.ai.ANTHROPIC_MODELS
import com.tina.app.ai.ChatRole
import com.tina.app.ai.ReasoningLevel
import com.tina.app.data.AiProvider
import com.tina.app.resources.Res
import com.tina.app.resources.ask_applied
import com.tina.app.resources.ask_chat_deleted
import com.tina.app.resources.ask_copied
import com.tina.app.resources.ask_apply
import com.tina.app.resources.ask_error
import com.tina.app.resources.ask_error_bad_reply
import com.tina.app.resources.ask_error_insecure
import com.tina.app.resources.ask_error_metered
import com.tina.app.resources.ask_error_no_model
import com.tina.app.resources.ask_error_not_found
import com.tina.app.resources.ask_error_off
import com.tina.app.resources.ask_error_rate_limited
import com.tina.app.resources.ask_error_server
import com.tina.app.resources.ask_error_unauthorized
import com.tina.app.resources.ask_not_now
import com.tina.app.resources.ask_pending
import com.tina.app.resources.ask_pending_deletes
import com.tina.app.resources.ask_hint
import com.tina.app.resources.ask_history
import com.tina.app.resources.ask_history_empty
import com.tina.app.resources.ask_new_chat
import com.tina.app.resources.ask_reason_balanced
import com.tina.app.resources.ask_reason_quick
import com.tina.app.resources.ask_reason_thorough
import com.tina.app.resources.ask_retry
import com.tina.app.resources.ask_sugg_1
import com.tina.app.resources.ask_sugg_2
import com.tina.app.resources.ask_sugg_3
import com.tina.app.resources.ask_sugg_4
import com.tina.app.resources.ask_thinking
import com.tina.app.resources.ask_write_off
import com.tina.app.resources.ask_write_on
import com.tina.app.resources.delete
import com.tina.app.resources.tab_ask
import com.tina.app.resources.undo
import com.tina.app.ui.relativeAge
import com.tina.app.ui.rememberUndoWindow
import com.tina.app.ui.showUndo
import kotlin.time.Clock
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Ask is a sheet over whatever you were looking at, not a page: answers stay in context
 * with the day. The input is the shared capture bar in ask mode, so this is header,
 * mode chips and transcript only.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)
@Composable
fun AskSheet(viewModel: AskViewModel, snackbarHostState: SnackbarHostState) {
    val settings = LocalSettings.current
    val listState = rememberLazyListState()
    var modelMenuOpen by remember { mutableStateOf(false) }
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
        if (viewModel.messages.isNotEmpty()) listState.animateScrollToItem(viewModel.messages.size)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val modelLabel = if (settings.aiProvider == AiProvider.ANTHROPIC) {
                ANTHROPIC_MODELS.firstOrNull { it.id == viewModel.effectiveModel(settings.aiModel) }?.label
                    ?: viewModel.effectiveModel(settings.aiModel)
            } else {
                stringResource(Res.string.tab_ask)
            }
            Box(Modifier.weight(1f)) {
                TextButton(
                    onClick = { modelMenuOpen = true },
                    enabled = settings.aiProvider == AiProvider.ANTHROPIC,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(modelLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                DropdownMenu(expanded = modelMenuOpen, onDismissRequest = { modelMenuOpen = false }) {
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
            IconButton(onClick = { showHistory = true }) {
                Icon(Icons.Outlined.History, stringResource(Res.string.ask_history))
            }
            IconButton(onClick = viewModel::newChat, enabled = viewModel.messages.isNotEmpty()) {
                Icon(Icons.Outlined.AddComment, stringResource(Res.string.ask_new_chat))
            }
        }

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
                label = { Text(stringResource(if (writeOn) Res.string.ask_write_on else Res.string.ask_write_off)) },
                leadingIcon = {
                    Icon(if (writeOn) Icons.Outlined.Edit else Icons.Outlined.Lock, null, Modifier.size(16.dp))
                },
            )
        }

        if (viewModel.messages.isEmpty() && !viewModel.sending) {
            AskEmptyState(onAsk = viewModel::send)
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                            color = if (fromUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
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
                                    if (fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomStart = if (fromUser) 20.dp else 6.dp,
                                        bottomEnd = if (fromUser) 6.dp else 20.dp,
                                    ),
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
                if (viewModel.sending) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LoadingIndicator(Modifier.size(24.dp))
                            Text(
                                stringResource(Res.string.ask_thinking),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (viewModel.pendingActions.isNotEmpty()) {
                    item {
                        val pending = viewModel.pendingActions
                        val deletes = pending.count { it.op == "delete" }
                        androidx.compose.material3.Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    if (deletes > 0) stringResource(Res.string.ask_pending_deletes, pending.size, deletes)
                                    else stringResource(Res.string.ask_pending, pending.size),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    androidx.compose.material3.Button(onClick = viewModel::applyPending) { Text(stringResource(Res.string.ask_apply)) }
                                    OutlinedButton(onClick = viewModel::dismissPending) { Text(stringResource(Res.string.ask_not_now)) }
                                }
                            }
                        }
                    }
                }
                viewModel.lastError?.let { error ->
                    item {
                        Column {
                            Text(
                                stringResource(
                                    when (error) {
                                        com.tina.app.ai.AiError.OFF -> Res.string.ask_error_off
                                        com.tina.app.ai.AiError.NO_MODEL -> Res.string.ask_error_no_model
                                        com.tina.app.ai.AiError.METERED -> Res.string.ask_error_metered
                                        com.tina.app.ai.AiError.INSECURE_ENDPOINT -> Res.string.ask_error_insecure
                                        com.tina.app.ai.AiError.UNAUTHORIZED -> Res.string.ask_error_unauthorized
                                        com.tina.app.ai.AiError.NOT_FOUND -> Res.string.ask_error_not_found
                                        com.tina.app.ai.AiError.RATE_LIMITED -> Res.string.ask_error_rate_limited
                                        com.tina.app.ai.AiError.SERVER -> Res.string.ask_error_server
                                        com.tina.app.ai.AiError.NETWORK -> Res.string.ask_error
                                        com.tina.app.ai.AiError.BAD_REPLY -> Res.string.ask_error_bad_reply
                                    },
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            OutlinedButton(onClick = viewModel::retry) { Text(stringResource(Res.string.ask_retry)) }
                        }
                    }
                }
            }
        }
    }

    if (showHistory) {
        val historyChats = viewModel.history.collectAsState().value
        val nowMillis = remember(historyChats) { Clock.System.now().toEpochMilliseconds() }
        ModalBottomSheet(onDismissRequest = { showHistory = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    stringResource(Res.string.ask_history),
                    style = MaterialTheme.typography.titleMediumEmphasized,
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
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
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
                                Text(entry.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    relativeAge(nowMillis - entry.updatedAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { viewModel.deleteChat(entry.id) }) {
                                Icon(Icons.Outlined.Delete, stringResource(Res.string.delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            SuggestionChip(onClick = { onAsk(suggestion) }, label = { Text(suggestion) })
        }
    }
}
