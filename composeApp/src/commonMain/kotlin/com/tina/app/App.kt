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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import com.tina.app.notes.NoteEditorScreen
import com.tina.app.ui.LocalSharedTransitionScope
import com.tina.app.ui.Shell
import com.tina.app.ui.rememberAppMotion
import org.koin.compose.koinInject

data object ShellRoute

data object SettingsRoute

data class SettingsSubRoute(val destination: String)

data class DetailRoute(val id: Long)

data class EventEditRoute(val id: Long)

data class NoteRoute(val id: Long)

data class TagRoute(val tag: String)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun App() {
    val settingsRepository = koinInject<SettingsRepository>()
    val settings by settingsRepository.settings.collectAsState(initial = Settings())

    AppTheme(settings) {
        SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSettings provides settings,
            LocalSharedTransitionScope provides this@SharedTransitionLayout,
        ) {
            val backStack = remember { mutableStateListOf<Any>(ShellRoute) }
            fun openItem(item: Item) {
                backStack.add(
                    when (item.type) {
                        ItemType.EVENT -> EventEditRoute(item.id)
                        ItemType.NOTE -> NoteRoute(item.id)
                        else -> DetailRoute(item.id)
                    },
                )
            }
            val motion = rememberAppMotion()
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                transitionSpec = { motion.push() },
                popTransitionSpec = { motion.pop() },
                // nav3 hands us the swipe edge: 1 is the right edge, which mirrors the pop.
                predictivePopTransitionSpec = { edge -> motion.pop(fromRightEdge = edge == 1) },
                entryProvider = entryProvider {
                    entry<ShellRoute> {
                        Shell(
                            onOpenSettings = { backStack.add(SettingsRoute) },
                            onOpenItem = ::openItem,
                            onOpenNote = { id -> backStack.add(NoteRoute(id)) },
                        )
                    }
                    entry<SettingsRoute> {
                        com.tina.app.ui.settings.SettingsScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onNavigate = { backStack.add(SettingsSubRoute(it.name)) },
                        )
                    }
                    entry<SettingsSubRoute> { route ->
                        com.tina.app.ui.settings.SettingsSubpageHost(
                            destination = com.tina.app.ui.settings.SettingsDestination.valueOf(route.destination),
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<DetailRoute> { route ->
                        DetailScreen(
                            itemId = route.id,
                            onBack = { backStack.removeLastOrNull() },
                            onOpenTag = { tag -> backStack.add(TagRoute(tag)) },
                        )
                    }
                    entry<EventEditRoute> { route ->
                        EventEditorScreen(itemId = route.id, onBack = { backStack.removeLastOrNull() })
                    }
                    entry<NoteRoute> { route ->
                        NoteEditorScreen(noteId = route.id, onBack = { backStack.removeLastOrNull() })
                    }
                    entry<TagRoute> { route ->
                        com.tina.app.search.TagScreen(
                            tag = route.tag,
                            onBack = { backStack.removeLastOrNull() },
                            onOpenItem = ::openItem,
                        )
                    }
                },
            )
        }
        }
    }
}
