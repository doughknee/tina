package com.tina.app.capture

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.tina.app.R
import com.tina.app.data.ItemRepository
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.android.ext.android.inject

private const val MAX_SHARED_CHARS = 5_000

/** Invisible share target: shared text becomes an item, no questions asked. */
class ShareActivity : ComponentActivity() {
    private val repository: ItemRepository by inject()
    private val refiner: com.tina.app.ai.CaptureRefiner by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // any app can send anything here: cap it before it reaches the database or a paid model
        val text = (intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?.takeIf { it.isNotBlank() }
            ?: intent?.getStringExtra(Intent.EXTRA_SUBJECT))
            ?.take(MAX_SHARED_CHARS)
        if (text.isNullOrBlank()) {
            finish()
            return
        }
        val tz = TimeZone.currentSystemDefault()
        val parsed = parseCapture(text, Clock.System.now().toLocalDateTime(tz))
        lifecycleScope.launch {
            val id = repository.capture(parsed, tz)
            refiner.refineInBackground(id, text) // survives finish(); refiner owns its scope
            Toast.makeText(this@ShareActivity, R.string.share_captured, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
