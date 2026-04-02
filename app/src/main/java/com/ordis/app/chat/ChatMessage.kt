package com.ordis.app.chat

data class ChatMessage(
    val role: String, // "user" | "assistant"
    val text: String,
    val ts: Long = System.currentTimeMillis()
)
