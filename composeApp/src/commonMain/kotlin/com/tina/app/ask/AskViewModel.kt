package com.tina.app.ask

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tina.app.ai.AiChat
import com.tina.app.ai.ChatMessage
import com.tina.app.ai.ChatRole
import com.tina.app.ai.ReasoningLevel
import com.tina.app.ai.buildAskContext
import com.tina.app.ai.buildAskSystemPrompt
import com.tina.app.data.ItemRepository
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AskViewModel(
    private val repository: ItemRepository,
    private val chat: AiChat,
) : ViewModel() {
    val messages = mutableStateListOf<ChatMessage>()
    var sending by mutableStateOf(false)
        private set
    var lastFailed by mutableStateOf(false)
        private set
    var reasoning by mutableStateOf(ReasoningLevel.BALANCED)
        private set
    private var modelOverride: String? = null

    fun effectiveModel(settingsModel: String): String = modelOverride ?: settingsModel

    fun setModelOverride(model: String) {
        modelOverride = model
    }

    fun setReasoningLevel(level: ReasoningLevel) {
        reasoning = level
    }

    fun send(text: String) {
        messages += ChatMessage(ChatRole.USER, text)
        ask()
    }

    fun retry() = ask()

    fun clear() {
        messages.clear()
        lastFailed = false
    }

    private fun ask() {
        if (sending) return
        sending = true
        lastFailed = false
        viewModelScope.launch {
            val tz = TimeZone.currentSystemDefault()
            val now = Clock.System.now().toLocalDateTime(tz)
            // fresh context per question so answers always reflect current data
            val context = buildAskContext(repository.allItems(), tz)
            val system = buildAskSystemPrompt(context, now, reasoning)
            val reply = chat.chat(system, messages.toList(), modelOverride)
            if (reply == null) {
                lastFailed = true
            } else {
                messages += ChatMessage(ChatRole.ASSISTANT, reply.trim())
            }
            sending = false
        }
    }
}
