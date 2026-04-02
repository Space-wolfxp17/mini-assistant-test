package com.ordis.app.learning

class PlannerService {
    /**
     * MVP без сети: заглушка.
     * Позже сюда подключишь Gemini/OpenAI API:
     * вход: "ордис найди что такое интернет"
     * выход: "SEARCH_YANDEX:что такое интернет"
     */
    fun plan(command: String): String? {
        val c = command.lowercase().trim()
        return when {
            c.contains("найди ") -> "SEARCH_YANDEX:${c.substringAfter("найди").trim()}"
            c.contains("камер") && c.contains("видео") -> "OPEN_VIDEO_CAMERA"
            c.contains("камер") -> "OPEN_CAMERA"
            c.contains("музык") && c.contains("вк") -> "OPEN_VK"
            c.contains("настройк") -> "OPEN_SETTINGS"
            else -> null
        }
    }
}
