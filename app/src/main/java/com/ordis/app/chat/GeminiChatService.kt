package com.ordis.app.chat

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * GeminiChatService PRO
 * - askWithCustomSystem(systemPrompt, history, userInput)
 * - retry/backoff
 * - safe parse ответа
 * - ограничение длины истории в payload
 */
class GeminiChatService(
    private val apiKeyProvider: () -> String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val model = "gemini-1.5-flash"
    private val jsonMedia = "application/json".toMediaType()

    /**
     * Главный метод для ConversationManager
     */
    fun askWithCustomSystem(
        systemPrompt: String,
        history: List<ChatMessage>,
        userInput: String
    ): String? {
        val key = apiKeyProvider().trim()
        if (key.isBlank()) return null

        val payload = buildPayload(systemPrompt, history, userInput)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"

        return executeWithRetry(url, payload, retries = 2)
    }

    /**
     * Совместимость со старым методом (если где-то ещё используется).
     */
    fun ask(
        mode: TutorMode,
        profileStyle: String,
        profileLevel: String,
        interests: String,
        history: List<ChatMessage>,
        userInput: String
    ): String? {
        val systemPrompt = """
            Ты Ордис — умный ассистент.
            Режим: $mode
            Стиль: $profileStyle
            Уровень: $profileLevel
            Интересы: $interests
            Отвечай по-человечески, ясно и полезно.
        """.trimIndent()

        return askWithCustomSystem(
            systemPrompt = systemPrompt,
            history = history,
            userInput = userInput
        )
    }

    private fun executeWithRetry(url: String, payload: JSONObject, retries: Int): String? {
        var attempt = 0
        var lastError: String? = null

        while (attempt <= retries) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .post(payload.toString().toRequestBody(jsonMedia))
                    .build()

                client.newCall(req).execute().use { resp ->
                    val bodyText = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        lastError = "HTTP ${resp.code}: $bodyText"
                    } else {
                        val parsed = parseGeminiText(bodyText)
                        if (!parsed.isNullOrBlank()) return parsed
                        lastError = "Empty parsed text"
                    }
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown exception"
            }

            attempt++
            if (attempt <= retries) {
                val delayMs = 500L * attempt
                try {
                    Thread.sleep(delayMs)
                } catch (_: InterruptedException) {
                }
            }
        }

        // Можно залогировать lastError при желании
        return null
    }

    private fun buildPayload(
        systemPrompt: String,
        history: List<ChatMessage>,
        userInput: String
    ): JSONObject {
        val contents = JSONArray()

        // SYSTEM as first USER block (Gemini API style)
        contents.put(
            JSONObject().put("role", "user").put(
                "parts", JSONArray().put(
                    JSONObject().put("text", "SYSTEM INSTRUCTION:\n$systemPrompt")
                )
            )
        )

        // Добавляем историю (ограничиваем, чтобы не раздувать токены)
        val clipped = history.takeLast(16)
        clipped.forEach { msg ->
            val role = if (msg.role.lowercase() == "assistant") "model" else "user"
            val text = msg.text.take(2000)
            contents.put(
                JSONObject().put("role", role).put(
                    "parts", JSONArray().put(
                        JSONObject().put("text", text)
                    )
                )
            )
        }

        // Текущий ввод
        contents.put(
            JSONObject().put("role", "user").put(
                "parts", JSONArray().put(
                    JSONObject().put("text", userInput.take(4000))
                )
            )
        )

        val generationConfig = JSONObject()
            .put("temperature", 0.7)
            .put("topK", 40)
            .put("topP", 0.95)
            .put("maxOutputTokens", 700)

        return JSONObject()
            .put("contents", contents)
            .put("generationConfig", generationConfig)
    }

    private fun parseGeminiText(raw: String): String? {
        return try {
            val root = JSONObject(raw)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            // Обычно первый кандидат
            val c0 = candidates.optJSONObject(0) ?: return null
            val content = c0.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null

            // Собираем все text parts
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val p = parts.optJSONObject(i) ?: continue
                val t = p.optString("text")
                if (t.isNotBlank()) {
                    if (sb.isNotEmpty()) sb.append("\n")
                    sb.append(t.trim())
                }
            }

            sb.toString().trim().ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }
}
