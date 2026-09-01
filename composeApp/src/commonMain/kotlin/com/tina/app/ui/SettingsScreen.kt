package com.tina.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.tina.app.data.ThemeMode
import com.tina.app.data.rememberBackupHandlers
import com.tina.app.resources.Res
import com.tina.app.resources.back
import com.tina.app.resources.export_data
import com.tina.app.resources.export_done
import com.tina.app.resources.import_data
import com.tina.app.resources.import_done
import com.tina.app.resources.import_failed
import com.tina.app.resources.settings_data
import com.tina.app.resources.reminder_at_time
import com.tina.app.resources.reminder_hour_before
import com.tina.app.resources.reminder_min_before
import com.tina.app.resources.settings
import com.tina.app.resources.settings_24h
import com.tina.app.resources.settings_dynamic_color
import com.tina.app.resources.settings_dynamic_color_desc
import com.tina.app.resources.settings_first_day
import com.tina.app.resources.settings_reminder
import com.tina.app.resources.settings_theme
import com.tina.app.resources.theme_dark
import com.tina.app.resources.theme_light
import com.tina.app.resources.theme_system
import com.tina.app.resources.weekdays_full
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val REMINDER_OPTIONS = listOf(0, 5, 10, 15, 30, 60)
private val FIRST_DAY_OPTIONS = listOf(DayOfWeek.MONDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.settings.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val weekdayNames = stringArrayResource(Res.array.weekdays_full)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.settings_theme), style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        ) {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> stringResource(Res.string.theme_system)
                                    ThemeMode.LIGHT -> stringResource(Res.string.theme_light)
                                    ThemeMode.DARK -> stringResource(Res.string.theme_dark)
                                },
                            )
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.settings_dynamic_color), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(Res.string.settings_dynamic_color_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.dynamicColor, onCheckedChange = viewModel::setDynamicColor)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.settings_first_day), style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    FIRST_DAY_OPTIONS.forEachIndexed { index, day ->
                        SegmentedButton(
                            selected = settings.firstDayOfWeek == day,
                            onClick = { viewModel.setFirstDayOfWeek(day) },
                            shape = SegmentedButtonDefaults.itemShape(index, FIRST_DAY_OPTIONS.size),
                        ) {
                            Text(weekdayNames[day.isoDayNumber - 1])
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.settings_24h),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = settings.use24h, onCheckedChange = viewModel::setUse24h)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.settings_reminder), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    REMINDER_OPTIONS.forEach { minutes ->
                        FilterChip(
                            selected = settings.defaultReminderMinutes == minutes,
                            onClick = { viewModel.setDefaultReminderMinutes(minutes) },
                            label = {
                                Text(
                                    when (minutes) {
                                        0 -> stringResource(Res.string.reminder_at_time)
                                        60 -> stringResource(Res.string.reminder_hour_before)
                                        else -> stringResource(Res.string.reminder_min_before, minutes)
                                    },
                                )
                            },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.settings_data), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = backupHandlers.export) {
                        Text(stringResource(Res.string.export_data))
                    }
                    OutlinedButton(onClick = backupHandlers.restore) {
                        Text(stringResource(Res.string.import_data))
                    }
                }
            }
        }
    }
}
