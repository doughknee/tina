package com.tina.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.tina.app.data.SettingsRepository
import com.tina.app.today.TodayWidget
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val settingsRepository: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // "Hide in app switcher" — FLAG_SECURE also blanks the recents thumbnail
        lifecycleScope.launch {
            settingsRepository.settings
                .map { it.hideInAppSwitcher }
                .distinctUntilChanged()
                .collect { hide ->
                    if (hide) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
        }

        setContent { App() }
    }

    override fun onStop() {
        super.onStop()
        // keep the home-screen widget in sync with whatever changed in-app
        lifecycleScope.launch { TodayWidget().updateAll(applicationContext) }
    }
}
