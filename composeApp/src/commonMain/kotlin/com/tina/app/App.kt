package com.tina.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.tina.app.calendar.EventEditorScreen
import com.tina.app.data.Item
import com.tina.app.data.ItemType
import com.tina.app.data.Settings
import com.tina.app.data.SettingsRepository
import com.tina.app.detail.DetailScreen
import com.tina.app.inbox.InboxScreen
import com.tina.app.ui.SettingsScreen
import com.tina.app.ui.Shell
import org.koin.compose.koinInject

data object ShellRoute

data object SettingsRoute

data object InboxRoute

data class DetailRoute(val id: Long)

data class EventEditRoute(val id: Long)

@Composable
fun App() {
    val settingsRepository = koinInject<SettingsRepository>()
    val settings by settingsRepository.settings.collectAsState(initial = Settings())

    AppTheme(settings) {
        CompositionLocalProvider(LocalSettings provides settings) {
            val backStack = remember { mutableStateListOf<Any>(ShellRoute) }
            fun openItem(item: Item) {
                backStack.add(
                    if (item.type == ItemType.EVENT) EventEditRoute(item.id) else DetailRoute(item.id),
                )
            }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<ShellRoute> {
                        Shell(
                            onOpenSettings = { backStack.add(SettingsRoute) },
                            onOpenInbox = { backStack.add(InboxRoute) },
                            onOpenItem = ::openItem,
                        )
                    }
                    entry<SettingsRoute> {
                        SettingsScreen(onBack = { backStack.removeLastOrNull() })
                    }
                    entry<InboxRoute> {
                        InboxScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onOpenItem = ::openItem,
                        )
                    }
                    entry<DetailRoute> { route ->
                        DetailScreen(itemId = route.id, onBack = { backStack.removeLastOrNull() })
                    }
                    entry<EventEditRoute> { route ->
                        EventEditorScreen(itemId = route.id, onBack = { backStack.removeLastOrNull() })
                    }
                },
            )
        }
    }
}
