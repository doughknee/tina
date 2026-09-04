package com.tina.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

/**
 * The keyboard inset the current animation is heading for: the end value from its first frame.
 * Android's `imeAnimationTarget`; the desktop has no keyboard inset, so its plain `ime` (zero).
 */
@Composable
expect fun keyboardTarget(): WindowInsets
