package com.tina.app.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import com.tina.app.LocalSettings
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Shows an undo snackbar for exactly [seconds]. Material's Short/Long durations are
 * fixed, so the Undo window setting is honoured by holding an Indefinite snackbar
 * and timing it out instead.
 *
 * Returns true when the user tapped Undo.
 */
suspend fun SnackbarHostState.showUndo(
    message: String,
    actionLabel: String,
    seconds: Int,
): Boolean = withTimeoutOrNull(seconds * 1000L) {
    showSnackbar(message, actionLabel, duration = SnackbarDuration.Indefinite) ==
        SnackbarResult.ActionPerformed
} ?: false

/** The user's configured undo window; read in composition, used inside coroutines. */
@Composable
fun rememberUndoWindow(): Int = LocalSettings.current.undoWindowSeconds
