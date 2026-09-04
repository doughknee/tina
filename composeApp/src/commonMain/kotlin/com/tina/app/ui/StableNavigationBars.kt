package com.tina.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

/**
 * The navigation-bar inset that does not change while the keyboard animates. Android's
 * `navigationBarsIgnoringVisibility`; the desktop has no bar and no such API.
 */
@Composable
expect fun stableNavigationBars(): WindowInsets
