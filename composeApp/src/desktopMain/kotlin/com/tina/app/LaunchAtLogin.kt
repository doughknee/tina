package com.tina.app

import java.io.File

/**
 * Windows start-up entry. Uses the per-user Startup folder rather than the registry:
 * no elevation, and the user can see and remove it in Explorer like any other shortcut.
 * Other desktop platforms are a no-op for now.
 */
object LaunchAtLogin {
    /** True when this process was started by the login entry, so the window starts hidden. */
    val startedHidden: Boolean = System.getProperty("tina.startHidden") == "true"

    private val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows")

    private val startupEntry: File?
        get() {
            val appData = System.getenv("APPDATA") ?: return null
            return File(appData, "Microsoft\\Windows\\Start Menu\\Programs\\Startup\\tina.cmd")
        }

    private fun executablePath(): String? {
        // packaged app-image: <app>/tina.exe sits next to the runtime
        val home = System.getProperty("java.home") ?: return null
        val exe = File(File(home).parentFile, "tina.exe")
        return if (exe.exists()) exe.absolutePath else null
    }

    fun apply(enabled: Boolean) {
        if (!isWindows) return
        val entry = startupEntry ?: return
        runCatching {
            if (!enabled) {
                if (entry.exists()) entry.delete()
                return
            }
            val exe = executablePath() ?: return
            entry.writeText("@echo off\r\nstart \"\" \"$exe\" -Dtina.startHidden=true\r\n")
        }
    }
}
