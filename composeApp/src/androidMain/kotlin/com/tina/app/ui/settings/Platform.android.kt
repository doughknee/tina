package com.tina.app.ui.settings

actual object Platform {
    actual val isAndroid: Boolean = true
    actual val isDesktop: Boolean = false
    actual val isDevBuild: Boolean = com.tina.app.BuildConfig.BUILD_TYPE == "dev"
}

actual fun appVersionName(): String = com.tina.app.BuildConfig.VERSION_NAME
