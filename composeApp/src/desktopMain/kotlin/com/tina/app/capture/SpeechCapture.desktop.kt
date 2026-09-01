package com.tina.app.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberSpeechCapture(onResult: (String) -> Unit): SpeechCapture =
    remember { SpeechCapture(available = false, start = {}) }
