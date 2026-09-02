package com.tina.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.tina.app.data.Settings
import com.tina.app.data.SettingsRepository
import com.tina.app.di.desktopModule
import com.tina.app.di.initKoin
import com.tina.app.notifications.DesktopTray
import com.tina.app.ui.KeyBus
import com.tina.app.ui.KeyCommand
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext

fun main() {
    initKoin(desktopModule)
    val settingsRepository = GlobalContext.get().get<SettingsRepository>()
    LaunchAtLogin.apply(readLaunchAtLoginSetting(settingsRepository))

    application {
        val settings by settingsRepository.settings.collectAsState(initial = Settings())
        val trayState = rememberTrayState()
        // "Launch at login" starts hidden; otherwise the window opens normally
        var windowVisible by remember { mutableStateOf(!LaunchAtLogin.startedHidden) }

        LaunchedEffect(Unit) { DesktopTray.state = trayState }
        LaunchedEffect(settings.launchAtLogin) { LaunchAtLogin.apply(settings.launchAtLogin) }

        Tray(
            state = trayState,
            icon = rememberVectorPainter(Icons.Outlined.Edit),
            tooltip = "tina",
            onAction = { windowVisible = true },
            menu = {
                Item("Open tina", onClick = { windowVisible = true })
                Item("Quit", onClick = ::exitApplication)
            },
        )

        Window(
            onCloseRequest = {
                // "Close to tray" keeps the app running behind the tray icon
                if (settings.closeToTray) windowVisible = false else exitApplication()
            },
            visible = windowVisible,
            title = "tina",
            state = rememberWindowState(size = DpSize(1200.dp, 800.dp)),
            onKeyEvent = { event ->
                if (event.type != KeyEventType.KeyDown) return@Window false
                val bareKeysAllowed = !KeyBus.textInputActive && !KeyBus.pageOpen
                when {
                    event.isCtrlPressed && event.key == Key.N -> KeyBus.emit(KeyCommand.FOCUS_CAPTURE)
                    event.isCtrlPressed && event.key == Key.F -> KeyBus.emit(KeyCommand.SEARCH)
                    !bareKeysAllowed -> false
                    event.key == Key.N -> KeyBus.emit(KeyCommand.NEW_ITEM)
                    event.key == Key.DirectionUp -> KeyBus.emit(KeyCommand.UP)
                    event.key == Key.DirectionDown -> KeyBus.emit(KeyCommand.DOWN)
                    event.key == Key.DirectionLeft -> KeyBus.emit(KeyCommand.LEFT)
                    event.key == Key.DirectionRight -> KeyBus.emit(KeyCommand.RIGHT)
                    event.key == Key.Enter -> KeyBus.emit(KeyCommand.CONFIRM)
                    event.key == Key.Delete -> KeyBus.emit(KeyCommand.DELETE)
                    else -> false
                }
            },
        ) {
            App()
        }
    }
}

private fun readLaunchAtLoginSetting(repository: SettingsRepository): Boolean =
    runCatching { runBlocking { repository.settings.first().launchAtLogin } }.getOrDefault(false)
