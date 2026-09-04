package com.tina.app.ui.settings

actual object Platform {
    actual val isAndroid: Boolean = false
    actual val isDesktop: Boolean = true
    actual val isDevBuild: Boolean = System.getProperty("tina.dev") == "true"
}

// jpackage stamps the launcher with the packageVersion; a plain `gradle run` has none
actual fun appVersionName(): String = System.getProperty("jpackage.app-version") ?: "dev"
