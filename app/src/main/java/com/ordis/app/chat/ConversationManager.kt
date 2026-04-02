package com.ordis.app.chat

import kotlin.concurrent.thread

class ConversationManager(
    private val memory: ConversationMemoryRepository,
    private val profile: UserProfileRepository,
    private val chatService: GeminiChatService,
    private val speak: (String) -> Unit
) {

    fun setMode(mode: TutorMode) {
        memory.setMode(mode)
        speak("Режим: ${mode.name.lowercase()}")
    }

    fun clearHistory() {
        memory.clear()
        speak("История очищена")
    }

    fun reply(userText: String, onDone: ((String) -> Unit)? = null) {
        memory.append(ChatMessage("user", userText))

        val mode = memory.getMode()
        val history = memory.getHistory(20)

        thread {
            val answer = chatService.ask(
                mode = mode,
                profileStyle = profile.getPreferredStyle(),
                profileLevel = profile.getLevel(),
                interests = profile.getInterests(),
                history = history,
                userInput = userText
            ) ?: "Я сейчас офлайн или ключ API не настроен. Повтори позже."

            memory.append(ChatMessage("assistant", answer))
            speak(answer)
            onDone?.invoke(answer)
        }
    }
}
