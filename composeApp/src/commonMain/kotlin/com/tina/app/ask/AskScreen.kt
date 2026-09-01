package com.tina.app.ask

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteSweep
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tina.app.LocalSettings
import com.tina.app.data.AiProvider
import com.tina.app.resources.Res
import com.tina.app.resources.ask_clear
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
import com.tina.app.resources.ask_thinking
import com.tina.app.resources.capture_save
import com.tina.app.resources.tab_ask
import com.tina.app.ai.ANTHROPIC_MODELS
import com.tina.app.ai.ChatRole
import com.tina.app.ai.ReasoningLevel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskScreen(viewModel: AskViewModel = koinViewModel()) {
    val settings = LocalSettings.current
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var modelMenuOpen by remember { mutableStateOf(false) }

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
                    IconButton(onClick = viewModel::clear, enabled = viewModel.messages.isNotEmpty()) {
                        Icon(Icons.Outlined.DeleteSweep, stringResource(Res.string.ask_clear))
                    }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
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
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
