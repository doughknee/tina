package com.tina.app.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

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
