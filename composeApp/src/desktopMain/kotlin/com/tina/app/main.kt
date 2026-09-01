package com.tina.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.tina.app.di.desktopModule
import com.tina.app.di.initKoin

fun main() {
    initKoin(desktopModule)
    application {
        Window(onCloseRequest = ::exitApplication, title = "tina") {
            App()
        }
    }
}
