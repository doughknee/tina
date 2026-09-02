package com.tina.app

import android.app.KeyguardManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.room.useWriterConnection
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.tina.app.data.SettingsRepository
import com.tina.app.today.TodayWidget
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val settingsRepository: SettingsRepository by inject()
    private val database: com.tina.app.data.AppDatabase by inject()

    /** When the app last went to the background; drives the app-lock grace period. */
    private var backgroundedAt = 0L
    private var locked by mutableStateOf(false)

    private val unlockLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            locked = false
            backgroundedAt = 0L
        } else {
            // refusing to unlock leaves nothing to show
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Android 15+ trims the refresh rate for windows whose redraws look small (a caret, a
        // chip), and a Compose app is one big View, so tina kept getting pinned to 60 Hz mid-use
        // ("frameRateOverride uid=... 60" in dumpsys display). Opt out: the panel picks its rate.
        if (android.os.Build.VERSION.SDK_INT >= 35) window.isFrameRatePowerSavingsBalanced = false
        handleFocusCapture(intent)

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

        setContent {
            if (locked) {
                // opaque cover so nothing is readable behind the system prompt
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
            } else {
                App()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleFocusCapture(intent)
    }

    /**
     * The quick-capture widget and Quick Settings tile exist to get you typing, so they
     * focus the field even when "Keyboard on open" is off.
     */
    private fun handleFocusCapture(intent: android.content.Intent?) {
        intent?.getLongExtra(EXTRA_OPEN_ITEM, -1L)?.takeIf { it > 0 }?.let { com.tina.app.ui.OpenItemRequests.request(it) }
        if (intent?.getBooleanExtra(EXTRA_OPEN_SORT, false) == true) com.tina.app.ui.OpenSortRequests.request()
        if (intent?.getBooleanExtra(EXTRA_FOCUS_CAPTURE, false) == true) {
            com.tina.app.ui.CaptureFocus.request(idea = intent.getBooleanExtra(EXTRA_FOCUS_IDEA, false))
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val settings = settingsRepository.settings.first()
            if (!settings.appLock || backgroundedAt == 0L) return@launch
            val away = System.currentTimeMillis() - backgroundedAt
            if (away < settings.appLockGraceSeconds * 1000L) return@launch
            promptUnlock()
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundedAt = System.currentTimeMillis()
        // keep the home-screen widget in sync with whatever changed in-app
        lifecycleScope.launch { TodayWidget().updateAll(applicationContext) }
        // Android Auto Backup copies the database file as-is; fold the WAL in so a restore is consistent
        lifecycleScope.launch {
            runCatching {
                database.useWriterConnection { transactor ->
                    transactor.usePrepared("PRAGMA wal_checkpoint(TRUNCATE)") { statement -> statement.step() }
                }
            }
        }
    }

    /**
     * Uses the system credential prompt (PIN / pattern / biometric) rather than pulling in
     * androidx.biometric — one fewer dependency for the same result.
     */
    private fun promptUnlock() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard?.isDeviceSecure != true) return
        val intent = keyguard.createConfirmDeviceCredentialIntent(
            getString(R.string.app_name),
            getString(R.string.lock_prompt),
        ) ?: return
        locked = true
        unlockLauncher.launch(intent)
    }

    companion object {
        const val EXTRA_FOCUS_CAPTURE = "com.tina.app.FOCUS_CAPTURE"
        const val EXTRA_FOCUS_IDEA = "com.tina.app.FOCUS_IDEA"
        const val EXTRA_OPEN_ITEM = "com.tina.app.OPEN_ITEM"
        const val EXTRA_OPEN_SORT = "com.tina.app.OPEN_SORT"
    }
}
