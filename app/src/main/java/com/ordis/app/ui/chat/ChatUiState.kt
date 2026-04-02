package com.ordis.app.ui.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

data class UiChatMessage(
    val id: Long = System.currentTimeMillis(),
    val role: String, // "user" | "assistant" | "system"
    val text: String,
    val ts: Long = System.currentTimeMillis()
)

object ChatUiState {
    val messages = mutableStateListOf<UiChatMessage>()
    val inputText = mutableStateOf("")
    val isThinking = mutableStateOf(false)
    val networkState = mutableStateOf("ready") // ready|online|offline|error

    fun add(role: String, text: String) {
        messages.add(UiChatMessage(role = role, text = text))
        if (messages.size > 300) {
            val removeCount = messages.size - 300
            repeat(removeCount) { messages.removeAt(0) }
        }
    }

    fun clear() {
        messages.clear()
    }
}
