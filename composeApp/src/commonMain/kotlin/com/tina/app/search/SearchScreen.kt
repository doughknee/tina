package com.tina.app.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.LocalSettings
import com.tina.app.ai.ChatRole
import com.tina.app.ai.HostedQuota
import com.tina.app.ai.askProvider
import com.tina.app.ai.fetchHostedQuota
import com.tina.app.ask.AskViewModel
import com.tina.app.ask.aiErrorText
import com.tina.app.capture.typeLabel
import com.tina.app.data.AiProvider
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import com.tina.app.data.SettingsRepository
import com.tina.app.notes.htmlPreview
import com.tina.app.pro.Entitlement
import com.tina.app.pro.rememberEntitlement
import com.tina.app.resources.Res
import com.tina.app.resources.ask_about
import com.tina.app.resources.ask_about_sub
import com.tina.app.resources.ask_answer
import com.tina.app.resources.ask_ex_1
import com.tina.app.resources.ask_ex_2
import com.tina.app.resources.ask_ex_3
import com.tina.app.resources.ask_follow_up
import com.tina.app.resources.ask_matches
import com.tina.app.resources.ask_offer_body
import com.tina.app.resources.ask_offer_cta
import com.tina.app.resources.ask_offer_note
import com.tina.app.resources.ask_offer_title
import com.tina.app.resources.ask_subtitle_free
import com.tina.app.resources.ask_subtitle_pro
import com.tina.app.resources.ask_thinking
import com.tina.app.resources.ask_try
import com.tina.app.resources.pro_tag
import com.tina.app.resources.pro_title
import com.tina.app.resources.search_close
import com.tina.app.resources.search_no_results
import com.tina.app.resources.search_placeholder
import com.tina.app.resources.search_recent
import com.tina.app.resources.search_tags
import com.tina.app.resources.settings
import com.tina.app.resources.tab_ask
import com.tina.app.ui.dateLabel
import com.tina.app.ui.timeLabel
import io.ktor.client.HttpClient
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val RECENT_COUNT = 8

class SearchViewModel(
    repository: ItemRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val query = MutableStateFlow("")

    // ponytail: in-memory over the whole table; the LIKE query is gone with the Library
    val results: StateFlow<List<Item>> = combine(repository.observeAll(), query, settingsRepository.settings) { items, q, s ->
        val needle = q.trim()
        val searchable = items.filter { s.searchCompleted || !it.completed }
        if (needle.isEmpty()) return@combine searchable.sortedByDescending { it.updatedAt }.take(RECENT_COUNT)
        searchable.filter { item ->
            (
                item.title.contains(needle, ignoreCase = true) ||
                    item.tags.any { it.contains(needle.removePrefix("#"), ignoreCase = true) } ||
                    (item.body?.let { htmlPreview(it).contains(needle, ignoreCase = true) } == true)
                )
        }.sortedByDescending { it.updatedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every tag in use, for the chip row under the field. */
    val tags: StateFlow<List<String>> = repository.observeTagged()
        .map { items -> items.flatMap { it.tags }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }
}

/**
 * The Ask tab: one field, two jobs. Searching your own data is free and instant (Matches as
 * you type); asking is Peggy Pro. A free user sees three example questions and the offer on
 * the page itself, and every ask-shaped tap opens the Pro page. A Pro user sees asks left in
 * the subtitle, the Answer streams under the Matches on submit, and Follow up pushes the
 * conversation. [focusKey] changes when the search shortcut fires, so the field takes focus again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenSettings: () -> Unit,
    onOpenItem: (Item) -> Unit,
    onOpenTag: (String) -> Unit,
    onOpenAskChat: () -> Unit,
    onOpenPaywall: () -> Unit,
    focusKey: Int,
    viewModel: SearchViewModel,
    askViewModel: AskViewModel = koinViewModel(),
    http: HttpClient = koinInject(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val focus = remember { FocusRequester() }
    val settings = LocalSettings.current
    val entitlement by rememberEntitlement()
    val isPro = entitlement is Entitlement.Pro
    // Pro, or an own key: either way the field answers. Otherwise every ask lands on the offer.
    val canAsk = askProvider(settings.aiProvider, entitlement) != AiProvider.OFF
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    // the question this page submitted; the Answer shows while the field still holds it
    var asked by remember { mutableStateOf<String?>(null) }
    val answerVisible = canAsk && asked != null && asked == query.trim()

    // the relay's own count; refetched after each answer so the subtitle keeps up
    var quota by remember { mutableStateOf<HostedQuota?>(null) }
    LaunchedEffect(entitlement, askViewModel.sending) {
        if (!askViewModel.sending) quota = fetchHostedQuota(http, entitlement)
    }

    fun ask(question: String) {
        val q = question.trim()
        if (q.isEmpty()) return
        if (!canAsk) {
            onOpenPaywall()
            return
        }
        viewModel.setQuery(q)
        asked = q
        askViewModel.send(q)
    }

    LaunchedEffect(focusKey) {
        withFrameNanos { }
        focus.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(Res.string.tab_ask), style = MaterialTheme.typography.titleLargeEmphasized)
                        Text(
                            when {
                                isPro && quota != null -> stringResource(Res.string.ask_subtitle_pro, (quota!!.askLimit - quota!!.askUsed).coerceAtLeast(0))
                                isPro -> stringResource(Res.string.pro_title)
                                else -> stringResource(Res.string.ask_subtitle_free)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(Res.string.settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(56.dp),
            ) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    val placeholder = stringResource(Res.string.search_placeholder)
                    BasicTextField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp).focusRequester(focus)
                            .onFocusChanged { com.tina.app.ui.KeyBus.textInputActive = it.isFocused }
                            .semantics { contentDescription = placeholder },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = if (canAsk) ImeAction.Send else ImeAction.Search),
                        keyboardActions = KeyboardActions(onSend = { ask(query) }, onSearch = { ask(query) }),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                inner()
                            }
                        },
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Outlined.Close, stringResource(Res.string.search_close))
                        }
                    }
                }
            }

            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                if (query.isBlank()) {
                    if (tags.isNotEmpty()) {
                        item("tags") {
                            SectionLabel(stringResource(Res.string.search_tags))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(tags, key = { it }) { tag ->
                                    SuggestionChip(onClick = { onOpenTag(tag) }, label = { Text("#$tag") })
                                }
                            }
                        }
                    }
                    item("try") {
                        SectionLabel(stringResource(Res.string.ask_try))
                        listOf(Res.string.ask_ex_1, Res.string.ask_ex_2, Res.string.ask_ex_3).forEach { res ->
                            val example = stringResource(res)
                            Row(
                                Modifier.fillMaxWidth().clickable { ask(example) }.padding(horizontal = 24.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(example, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (!isPro) item("offer") { OfferCard(onOpenPaywall) }
                    if (results.isNotEmpty()) item("recent") { SectionLabel(stringResource(Res.string.search_recent)) }
                } else {
                    item("matches") {
                        SectionLabel(stringResource(Res.string.ask_matches))
                        if (results.isEmpty()) {
                            Text(
                                stringResource(Res.string.search_no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                items(results, key = { it.id }) { item ->
                    ResultRow(item, today, settings.use24h, Modifier.animateItem()) { onOpenItem(item) }
                }
                if (query.isNotBlank()) {
                    if (answerVisible) {
                        item("answer") { AnswerBlock(askViewModel, onOpenAskChat) }
                    } else {
                        item("ask") {
                            Row(
                                Modifier.fillMaxWidth().clickable { ask(query) }.padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(Res.string.ask_about, query.trim()), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        stringResource(Res.string.ask_about_sub),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (!canAsk) ProTag()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun ProTag() {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
        Text(
            stringResource(Res.string.pro_tag),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** The one Pro advertisement in the app (DECISIONS.md → Shell). Prices are the locked ones. */
@Composable
private fun OfferCard(onOpenPaywall: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(20.dp))
                Text(stringResource(Res.string.ask_offer_title), style = MaterialTheme.typography.titleMediumEmphasized)
            }
            Text(stringResource(Res.string.ask_offer_body), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onOpenPaywall, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(stringResource(Res.string.ask_offer_cta))
            }
            Text(
                stringResource(Res.string.ask_offer_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

/** The reply to the question in the field: streamed while it arrives, then the finished text. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnswerBlock(askViewModel: AskViewModel, onOpenAskChat: () -> Unit) {
    val lastAnswer = askViewModel.messages.lastOrNull { it.role == ChatRole.ASSISTANT }?.content
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        SectionLabel(stringResource(Res.string.ask_answer))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val error = askViewModel.lastError
                when {
                    askViewModel.sending && askViewModel.streamingReply.isNotEmpty() ->
                        Text(askViewModel.streamingReply, style = MaterialTheme.typography.bodyLarge)
                    askViewModel.sending -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LoadingIndicator(Modifier.size(24.dp))
                        Text(stringResource(Res.string.ask_thinking), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    error != null -> Text(aiErrorText(error), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    lastAnswer != null -> Text(lastAnswer, style = MaterialTheme.typography.bodyLarge)
                }
                if (!askViewModel.sending) {
                    TextButton(onClick = onOpenAskChat, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(Res.string.ask_follow_up))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.padding(start = 6.dp).size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(item: Item, today: kotlinx.datetime.LocalDate, use24h: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val supporting = listOfNotNull(
        typeLabel(item.type),
        when (item.type) {
            ItemType.NOTE -> item.body?.let { htmlPreview(it) }?.takeIf { it.isNotBlank() }
            ItemType.EVENT -> item.startAt?.let { start ->
                val local = Instant.fromEpochMilliseconds(start).toLocalDateTime(TimeZone.currentSystemDefault())
                listOfNotNull(
                    dateLabel(local.date, today),
                    if (item.allDay) null else timeLabel(local.time, use24h),
                ).joinToString(" ")
            }
            else -> item.dueLocalDate?.let { dateLabel(it, today) }
        },
    ).joinToString(" · ")
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .alpha(if (item.completed) 0.6f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            when (item.type) {
                ItemType.TASK -> Icons.Outlined.TaskAlt
                ItemType.EVENT -> Icons.Outlined.Event
                ItemType.NOTE -> Icons.Outlined.Lightbulb
                ItemType.INBOX -> Icons.Outlined.Inbox
            },
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (item.completed) TextDecoration.LineThrough else null,
            )
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
