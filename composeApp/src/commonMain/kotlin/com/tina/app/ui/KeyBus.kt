package com.tina.app.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class KeyCommand { FOCUS_CAPTURE, NEW_ITEM, SEARCH, UP, DOWN, LEFT, RIGHT, CONFIRM, DELETE }

/**
 * Desktop keyboard commands. The window emits, the active screen collects.
 * Android never emits, so this is inert there.
 */
object KeyBus {
    private val _events = MutableSharedFlow<KeyCommand>(extraBufferCapacity = 16)
    val events: SharedFlow<KeyCommand> = _events
    fun emit(command: KeyCommand): Boolean = _events.tryEmit(command)
}

/**
 * "Open capture with the field focused, whatever the setting says" — raised by the
 * quick-capture widget and tile, whose entire purpose is a keyboard-up capture, and by
 * the desktop focus shortcut. State, not an event, so a cold start can't miss it while
 * the UI is still composing; Shell switches to the tab and CaptureScreen clears it.
 */
object CaptureFocus {
    private val _pending = MutableStateFlow(false)
    val pending: StateFlow<Boolean> = _pending
    fun request() { _pending.value = true }
    fun clear() { _pending.value = false }
}
