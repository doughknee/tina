package com.tina.app.ui.settings.subpages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.ItemType
import com.tina.app.data.SettingsRepository
import com.tina.app.data.TrashRetention
import com.tina.app.resources.Res
import com.tina.app.resources.back
import com.tina.app.resources.trash_30
import com.tina.app.resources.trash_7
import com.tina.app.resources.trash_days_left
import com.tina.app.resources.trash_deleted_ago
import com.tina.app.resources.trash_empty
import com.tina.app.resources.trash_empty_all
import com.tina.app.resources.trash_forever
import com.tina.app.resources.trash_keep_for
import com.tina.app.resources.trash_purged
import com.tina.app.resources.trash_restore
import com.tina.app.resources.trash_restored
import com.tina.app.resources.trash_sub
import com.tina.app.resources.trash_swipe_hint
import com.tina.app.resources.trash_title
import com.tina.app.resources.undo
import com.tina.app.ui.relativeAge
import com.tina.app.ui.settings.HoldToConfirm
import com.tina.app.ui.settings.SettingsRowSurface
import kotlin.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

class TrashViewModel(
    private val repository: ItemRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val items = repository.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.tina.app.data.Settings())

    private var lastPurged: Item? = null

    fun restore(item: Item) {
        viewModelScope.launch { repository.restore(item) }
    }

    fun purge(item: Item) {
        lastPurged = item
        viewModelScope.launch { repository.purge(item.id) }
    }

    fun undoPurge() {
        val item = lastPurged ?: return
        lastPurged = null
        viewModelScope.launch { repository.restore(item) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repository.emptyTrash() }
    }

    fun setRetention(value: TrashRetention) {
        viewModelScope.launch { settingsRepository.setTrashRetention(value) }
    }
}

private val RETENTIONS = listOf(TrashRetention.DAYS_7, TrashRetention.DAYS_30, TrashRetention.FOREVER)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(onBack: () -> Unit, viewModel: TrashViewModel = koinViewModel()) {
    val items by viewModel.items.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val now = remember(items) { Clock.System.now().toEpochMilliseconds() }

    val retentionLabels = listOf(
        stringResource(Res.string.trash_7),
        stringResource(Res.string.trash_30),
        stringResource(Res.string.trash_forever),
    )
    val retentionIndex = RETENTIONS.indexOf(settings.trashRetention).coerceAtLeast(0)
    val purgedText = stringResource(Res.string.trash_purged)
    val restoredText = stringResource(Res.string.trash_restored)
    val undoText = stringResource(Res.string.undo)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(Res.string.trash_title), style = MaterialTheme.typography.headlineSmall)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    stringResource(Res.string.trash_sub, retentionLabels[retentionIndex].lowercase()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            item {
                SettingsRowSurface(index = 0, count = 1) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(Res.string.trash_keep_for), style = MaterialTheme.typography.bodyLarge)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            RETENTIONS.forEachIndexed { index, option ->
                                SegmentedButton(
                                    selected = index == retentionIndex,
                                    onClick = { viewModel.setRetention(option) },
                                    shape = SegmentedButtonDefaults.itemShape(index, RETENTIONS.size),
                                ) { Text(retentionLabels[index]) }
                            }
                        }
                    }
                }
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.trash_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
                return@LazyColumn
            }
            itemsIndexed(items, key = { _, it -> it.id }) { index, entry ->
                TrashRow(
                    item = entry,
                    index = index,
                    count = items.size,
                    nowMillis = now,
                    retentionDays = settings.trashRetention.days,
                    onRestore = {
                        viewModel.restore(entry)
                        scope.launch { snackbarHostState.showSnackbar(restoredText) }
                    },
                    onPurge = {
                        viewModel.purge(entry)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                purgedText, undoText, duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) viewModel.undoPurge()
                        }
                    },
                    modifier = Modifier.animateItem(),
                )
            }
            item {
                Column {
                    Text(
                        stringResource(Res.string.trash_swipe_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, bottom = 12.dp),
                    )
                    // emptying is destructive and unrecoverable, so it holds rather than asks
                    HoldToConfirm(
                        label = stringResource(Res.string.trash_empty_all),
                        onConfirm = viewModel::emptyTrash,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashRow(
    item: Item,
    index: Int,
    count: Int,
    nowMillis: Long,
    retentionDays: Int?,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.settledValue) {
        if (dismissState.settledValue != SwipeToDismissBoxValue.Settled) {
            onPurge()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    val deletedAgo = stringResource(Res.string.trash_deleted_ago, relativeAge(nowMillis - (item.deletedAt ?: nowMillis)))
    val daysLeft = retentionDays?.let { days ->
        val elapsed = ((nowMillis - (item.deletedAt ?: nowMillis)) / (24L * 60 * 60 * 1000)).toInt()
        (days - elapsed).coerceAtLeast(0)
    }
    val urgent = daysLeft != null && daysLeft < 5

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
            }
        },
    ) {
        SettingsRowSurface(index = index, count = count) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    when (item.type) {
                        ItemType.TASK -> Icons.Outlined.TaskAlt
                        ItemType.EVENT -> Icons.Outlined.Event
                        ItemType.NOTE -> Icons.Outlined.Description
                        ItemType.INBOX -> Icons.Outlined.Inbox
                    },
                    contentDescription = null,
                    Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(item.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    Text(
                        listOfNotNull(
                            item.type.name.lowercase().replaceFirstChar { it.uppercase() },
                            deletedAgo,
                            daysLeft?.let { stringResource(Res.string.trash_days_left, it) },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (urgent) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onRestore,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) { Text(stringResource(Res.string.trash_restore)) }
            }
        }
    }
}
