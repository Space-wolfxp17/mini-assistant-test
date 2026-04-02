package com.ordis.app.chat

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiChatService(
    private val apiKeyProvider: () -> String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun ask(
        mode: TutorMode,
        profileStyle: String,
        profileLevel: String,
        interests: String,
        history: List<ChatMessage>,
        userInput: String
    ): String? {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank()) return null

        val systemPrompt = buildSystemPrompt(mode, profileStyle, profileLevel, interests)

        val conversation = StringBuilder()
        conversation.appendLine("СИСТЕМА: $systemPrompt")
        history.takeLast(12).forEach {
            conversation.appendLine("${it.role.uppercase()}: ${it.text}")
        }
        conversation.appendLine("USER: $userInput")
        conversation.appendLine("ASSISTANT:")

        val body = JSONObject()
            .put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", conversation.toString())
                ))
            ))

        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val str = resp.body?.string().orEmpty()
                val root = JSONObject(str)
                root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.trim()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildSystemPrompt(
        mode: TutorMode,
        style: String,
        level: String,
        interests: String
    ): String {
        val modeRules = when (mode) {
            TutorMode.GENERAL -> "Ты умный собеседник: анализируй смысл, отвечай естественно."
            TutorMode.PHYSICS -> "Ты эксперт по физике: объясняй с формулами и примерами."
            TutorMode.CHEMISTRY -> "Ты эксперт по химии: уравнения и логика реакций."
            TutorMode.MEDICINE -> "Ты медицинский просветитель: без диагноза и назначения лечения."
            TutorMode.RUSSIAN -> "Ты преподаватель русского языка: правила, примеры, разбор ошибок."
            TutorMode.LITERATURE -> "Ты преподаватель литературы: анализ произведений и контекста."
        }

        return """
            $modeRules
            Стиль ответа: $style.
            Уровень пользователя: $level.
            Интересы пользователя: $interests.
            Отвечай человечно, ясно, по шагам.
            Если вопрос сложный — сначала короткий ответ, потом подробный.
            Если нужно, задавай 1 уточняющий вопрос.
        """.trimIndent()
    }
}
