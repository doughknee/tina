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

/** Invisible share target: shared text becomes an item, no questions asked. */
class ShareActivity : ComponentActivity() {
    private val repository: ItemRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?.takeIf { it.isNotBlank() }
            ?: intent?.getStringExtra(Intent.EXTRA_SUBJECT)
        if (text.isNullOrBlank()) {
            finish()
            return
        }
        val tz = TimeZone.currentSystemDefault()
        val parsed = parseCapture(text, Clock.System.now().toLocalDateTime(tz))
        lifecycleScope.launch {
            repository.capture(parsed, tz)
            Toast.makeText(this@ShareActivity, R.string.share_captured, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
