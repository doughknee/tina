package com.tina.app.ui.settings.subpages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tina.app.resources.Res
import com.tina.app.resources.back
import com.tina.app.ui.settings.RadioRow
import com.tina.app.ui.settings.SettingsRowSurface
import org.jetbrains.compose.resources.stringResource

/** Every subpage: own top bar, same grouped body, same margins. Never a dialog. */
@Composable
fun SettingsSubpageScaffold(
    title: String,
    onBack: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            content = content,
        )
    }
}

/** Radio subpage: selection applies instantly and pops back. */
@Composable
fun ChoiceSubpage(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    supporting: String? = null,
) {
    SettingsSubpageScaffold(title = title, onBack = onBack) {
        item {
            Column {
                supporting?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, bottom = 12.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    options.forEachIndexed { index, label ->
                        SettingsRowSurface(index = index, count = options.size) {
                            RadioRow(
                                label = label,
                                selected = index == selectedIndex,
                                onSelect = { onSelect(index); onBack() },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Static informational subpage (Widgets, What's new, Licenses, Shortcuts). */
@Composable
fun InfoSubpage(
    title: String,
    onBack: () -> Unit,
    intro: String? = null,
    entries: List<Pair<String, String>> = emptyList(),
) {
    SettingsSubpageScaffold(title = title, onBack = onBack) {
        intro?.let {
            item {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
        if (entries.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    entries.forEachIndexed { index, (head, sub) ->
                        SettingsRowSurface(index = index, count = entries.size) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(head, style = MaterialTheme.typography.bodyLarge)
                                if (sub.isNotBlank()) {
                                    Text(
                                        sub,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
