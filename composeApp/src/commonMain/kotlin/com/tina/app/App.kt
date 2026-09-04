package com.tina.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.map
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.zIndex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.tina.app.calendar.EventEditorScreen
import com.tina.app.data.Item
import com.tina.app.data.ItemType
import com.tina.app.data.Settings
import com.tina.app.data.SettingsRepository
import com.tina.app.resources.whats_new_got_it
import com.tina.app.resources.whats_new_in
import com.tina.app.detail.DetailScreen
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import com.tina.app.notes.NoteEditorScreen
import com.tina.app.ui.LocalSharedTransitionScope
import com.tina.app.ui.Shell
import com.tina.app.ui.rememberAppMotion
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data object SettingsRoute

data class SettingsSubRoute(val destination: String)

/** One settings category's page, optionally landing on a row (from search). */
data class SettingsSectionRoute(val sectionId: String, val highlight: String? = null)

data class DetailRoute(val id: Long)

data class EventEditRoute(val id: Long)

data class NoteRoute(val id: Long)

data class TagRoute(val tag: String)

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalComposeUiApi::class)
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
            // Pages sit on top of the shell; the shell itself is never popped out of composition.
            // Re-composing it (calendar, bar, sheets, rows) cost a 120 ms frame on every return.
            val pages = remember { mutableStateListOf<Any>() }
            var pagesVisible by remember { mutableStateOf(false) }
            fun push(route: Any) {
                if (!pagesVisible) pages.clear()
                pages.add(route)
                pagesVisible = true
            }
            fun popLast() {
                if (pages.size > 1) pages.removeLastOrNull() else pagesVisible = false
            }
            fun openItem(item: Item) {
                push(
                    when (item.type) {
                        ItemType.EVENT -> EventEditRoute(item.id)
                        ItemType.NOTE -> NoteRoute(item.id)
                        else -> DetailRoute(item.id)
                    },
                )
            }
            // a reminder tap lands here with an id; open the item the way a row tap would
            val repository = koinInject<com.tina.app.data.ItemRepository>()
            val requestedItem by com.tina.app.ui.OpenItemRequests.pending.collectAsState()
            androidx.compose.runtime.LaunchedEffect(requestedItem) {
                val id = requestedItem ?: return@LaunchedEffect
                com.tina.app.ui.OpenItemRequests.clear()
                repository.get(id)?.let(::openItem)
            }
            androidx.compose.runtime.LaunchedEffect(pagesVisible) { com.tina.app.ui.KeyBus.pageOpen = pagesVisible }
            // assume seen until the store answers, so an existing user never sees the cards flash
            val onboardingSeen by settingsRepository.onboardingSeen.collectAsState(initial = true)
            val appScope = androidx.compose.runtime.rememberCoroutineScope()
            val motion = rememberAppMotion()
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                // shared-element rows need an animated scope; a still AnimatedContent provides one
                AnimatedContent(
                    targetState = Unit,
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    label = "shell",
                ) {
                    CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
                        Shell(
                            onOpenSettings = { push(SettingsRoute) },
                            onOpenItem = ::openItem,
                            onOpenNote = { id -> push(NoteRoute(id)) },
                            onOpenTag = { tag -> push(TagRoute(tag)) },
                        )
                    }
                }
                // after the shell, so it takes back presses first
                BackHandler(enabled = pagesVisible && pages.size == 1) { pagesVisible = false }
                WhatsNewOnUpgrade(settingsRepository, onboardingSeen)
                if (!onboardingSeen) {
                    // above the pages too, so Developer options can show it from inside Settings
                    Box(Modifier.fillMaxSize().zIndex(1f)) {
                        com.tina.app.ui.onboarding.OnboardingScreen(
                            onDone = { appScope.launch { settingsRepository.setOnboardingSeen() } },
                        )
                    }
                }
                AnimatedVisibility(
                    visible = pagesVisible,
                    enter = motion.push().targetContentEnter,
                    exit = motion.pop().initialContentExit,
                ) {
                    // pages are cleared only once the exit has finished and this leaves composition
                    DisposableEffect(Unit) { onDispose { pages.clear() } }
                    NavDisplay(
                        backStack = pages,
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                        onBack = ::popLast,
                        transitionSpec = { motion.push() },
                        popTransitionSpec = { motion.pop() },
                        // one direction for every pop, whichever edge the swipe started on
                        predictivePopTransitionSpec = { _ -> motion.pop() },
                        entryProvider = entryProvider {
                            entry<SettingsRoute> {
                                com.tina.app.ui.settings.SettingsScreen(
                                    onBack = ::popLast,
                                    onNavigate = { push(SettingsSubRoute(it.name)) },
                                    onOpenSection = { id, row -> push(SettingsSectionRoute(id, row)) },
                                )
                            }
                            entry<SettingsSectionRoute> { route ->
                                com.tina.app.ui.settings.SettingsScreen(
                                    onBack = ::popLast,
                                    onNavigate = { push(SettingsSubRoute(it.name)) },
                                    sectionId = route.sectionId,
                                    highlightRowId = route.highlight,
                                )
                            }
                            entry<SettingsSubRoute> { route ->
                                com.tina.app.ui.settings.SettingsSubpageHost(
                                    destination = com.tina.app.ui.settings.SettingsDestination.valueOf(route.destination),
                                    onBack = ::popLast,
                                )
                            }
                            entry<DetailRoute> { route ->
                                DetailScreen(
                                    itemId = route.id,
                                    onBack = ::popLast,
                                    onOpenTag = { tag -> push(TagRoute(tag)) },
                                )
                            }
                            entry<EventEditRoute> { route ->
                                EventEditorScreen(itemId = route.id, onBack = ::popLast)
                            }
                            entry<NoteRoute> { route ->
                                NoteEditorScreen(
                                    noteId = route.id,
                                    onBack = ::popLast,
                                    onOpenNote = { id -> push(NoteRoute(id)) },
                                    onOpenTag = { tag -> push(TagRoute(tag)) },
                                )
                            }
                            entry<TagRoute> { route ->
                                com.tina.app.search.TagScreen(
                                    tag = route.tag,
                                    onBack = ::popLast,
                                    onOpenItem = ::openItem,
                                    onCapture = { prefill ->
                                        // back to the shell, bar focused with the tag already typed
                                        pagesVisible = false
                                        com.tina.app.ui.CaptureFocus.request(prefill = prefill)
                                    },
                                )
                            }
                        },
                    )
                }
            }
        }
        }
    }
}


/**
 * Once per feature release, after an update: the top What's new entry as a sheet. A fresh
 * install records the current release silently, so only upgrades see it. Developer options
 * clear the record to show it again.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun WhatsNewOnUpgrade(settingsRepository: SettingsRepository, onboardingSeen: Boolean) {
    val current = com.tina.app.ui.settings.featureVersion(com.tina.app.ui.settings.appVersionName())
    val seen by settingsRepository.settings.map { it.whatsNewSeen }.collectAsState(initial = null)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val entry = com.tina.app.ui.settings.WHATS_NEW.firstOrNull { it.first == current } ?: return
    val seenVersion = seen ?: return
    // the store has answered: a fresh install (cards still to show) just records this release
    if (!onboardingSeen) {
        androidx.compose.runtime.LaunchedEffect(seenVersion) { if (seenVersion != current) settingsRepository.setWhatsNewSeen(current) }
        return
    }
    if (seenVersion == current) return
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = { scope.launch { settingsRepository.setWhatsNewSeen(current) } }) {
        androidx.compose.foundation.layout.Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            androidx.compose.material3.Text(
                org.jetbrains.compose.resources.stringResource(com.tina.app.resources.Res.string.whats_new_in, entry.first),
                style = MaterialTheme.typography.headlineSmall,
            )
            androidx.compose.material3.Text(
                entry.second,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
            )
            androidx.compose.material3.Button(
                onClick = { scope.launch { settingsRepository.setWhatsNewSeen(current) } },
                modifier = Modifier.align(androidx.compose.ui.Alignment.End),
            ) { androidx.compose.material3.Text(org.jetbrains.compose.resources.stringResource(com.tina.app.resources.Res.string.whats_new_got_it)) }
        }
    }
}
