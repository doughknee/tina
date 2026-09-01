package com.tina.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.LaunchedEffect
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
import com.tina.app.di.desktopModule
import com.tina.app.di.initKoin
import com.tina.app.notifications.DesktopTray
import com.tina.app.ui.KeyBus
import com.tina.app.ui.KeyCommand

fun main() {
    initKoin(desktopModule)
    application {
        val trayState = rememberTrayState()
        LaunchedEffect(Unit) { DesktopTray.state = trayState }
        Tray(
            state = trayState,
            icon = rememberVectorPainter(Icons.Outlined.Edit),
            tooltip = "tina",
        )

        Window(
            onCloseRequest = ::exitApplication,
            title = "tina",
            state = rememberWindowState(size = DpSize(1200.dp, 800.dp)),
            onKeyEvent = { event ->
                if (event.type != KeyEventType.KeyDown) return@Window false
                when {
                    event.isCtrlPressed && event.key == Key.N -> KeyBus.emit(KeyCommand.FOCUS_CAPTURE)
                    event.isCtrlPressed && event.key == Key.F -> KeyBus.emit(KeyCommand.SEARCH)
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
