package com.tina.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.tina.app.data.ThemeSeed

/**
 * The launcher icon is one of eight activity-aliases on MainActivity, one per [ThemeSeed],
 * declared in the manifest as `.Icon<Seed>`. Exactly one is enabled at a time; the launcher
 * re-reads the enabled set and swaps the icon in place. DONT_KILL_APP keeps the switch from
 * closing the app under the user, which the framework would otherwise do.
 */
fun applyAppIcon(context: Context, seed: ThemeSeed) {
    val pm = context.packageManager
    ThemeSeed.entries.forEach { candidate ->
        val component = ComponentName(context, "com.tina.app.Icon${candidate.name.lowercase().replaceFirstChar { it.uppercase() }}")
        val wanted = if (candidate == seed) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        if (pm.getComponentEnabledSetting(component) != wanted) {
            pm.setComponentEnabledSetting(component, wanted, PackageManager.DONT_KILL_APP)
        }
    }
}
