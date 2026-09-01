package com.tina.app.capture

import androidx.compose.runtime.Composable

class SpeechCapture(val available: Boolean, val start: () -> Unit)

/** Platform speech-to-text; results are fed through the capture parser like typed text. */
@Composable
expect fun rememberSpeechCapture(onResult: (String) -> Unit): SpeechCapture
