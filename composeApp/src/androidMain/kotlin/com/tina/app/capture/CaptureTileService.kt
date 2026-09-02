package com.tina.app.capture

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.tina.app.MainActivity

/** Quick Settings tile: straight into the bar. [IdeaTileService] is the same tile in Idea mode. */
open class CaptureTileService : TileService() {
    protected open val idea: Boolean = false

    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_FOCUS_CAPTURE, true)
            .putExtra(MainActivity.EXTRA_FOCUS_IDEA, idea)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

class IdeaTileService : CaptureTileService() {
    override val idea: Boolean = true
}
