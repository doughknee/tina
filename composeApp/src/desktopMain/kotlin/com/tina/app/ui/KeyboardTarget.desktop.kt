package com.tina.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable

@Composable
actual fun keyboardTarget(): WindowInsets = WindowInsets.ime
