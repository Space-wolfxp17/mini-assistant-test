package com.ordis.app.chat

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * ConversationMemoryRepository PRO
 * - Краткосрочная история (history)
 * - Долгосрочные факты о пользователе (facts)
 * - Теги тем + счетчики
 */
class ConversationMemoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ordis_conversation", Context.MODE_PRIVATE)

    private val KEY_HISTORY = "history"
    private val KEY_FACTS = "facts"
    private val KEY_TOPIC_STATS = "topic_stats"

    data class MemoryFact(
        val key: String,
        val value: String,
        val confidence: Double,
        val updatedAt: Long = System.currentTimeMillis()
    )

    // ---------------------------
    // История диалога
    // ---------------------------

    fun getHistory(limit: Int = 30): List<ChatMessage> {
        val raw = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<ChatMessage>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list += ChatMessage(
                role = o.optString("role", "user"),
                text = o.optString("text", ""),
                ts = o.optLong("ts", System.currentTimeMillis())
            )
        }
        return list.takeLast(limit)
    }

    fun append(message: ChatMessage, max: Int = 120) {
        val all = getHistory(limit = 500).toMutableList()
        all += message
        val trimmed = all.takeLast(max)
        val arr = JSONArray()
        trimmed.forEach {
            arr.put(
                JSONObject()
                    .put("role", it.role)
                    .put("text", it.text)
                    .put("ts", it.ts)
            )
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()

        // Авто-извлечение тем из user-сообщений
        if (message.role == "user") {
            updateTopicStats(extractTopics(message.text))
        }
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    // ---------------------------
    // Долгосрочные факты
    // ---------------------------

    fun getFacts(): List<MemoryFact> {
        val raw = prefs.getString(KEY_FACTS, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<MemoryFact>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list += MemoryFact(
                key = o.optString("key", ""),
                value = o.optString("value", ""),
                confidence = o.optDouble("confidence", 0.5),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }
        return list
    }

    fun upsertFact(key: String, value: String, confidence: Double) {
        if (key.isBlank() || value.isBlank()) return
        val safeConf = confidence.coerceIn(0.0, 1.0)

        val list = getFacts().toMutableList()
        val idx = list.indexOfFirst { it.key.equals(key, ignoreCase = true) }

        if (idx >= 0) {
            val old = list[idx]
            // мягкое обновление уверенности
            val newConf = ((old.confidence + safeConf) / 2.0).coerceIn(0.0, 1.0)
            list[idx] = old.copy(value = value, confidence = newConf, updatedAt = System.currentTimeMillis())
        } else {
            list += MemoryFact(key = key, value = value, confidence = safeConf)
        }

        saveFacts(list)
    }

    fun getFactValue(key: String): String? {
        return getFacts()
            .filter { it.key.equals(key, ignoreCase = true) }
            .maxByOrNull { it.confidence }?.value
    }

    private fun saveFacts(list: List<MemoryFact>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("key", it.key)
                    .put("value", it.value)
                    .put("confidence", it.confidence)
                    .put("updatedAt", it.updatedAt)
            )
        }
        prefs.edit().putString(KEY_FACTS, arr.toString()).apply()
    }

    // ---------------------------
    // Статистика тем
    // ---------------------------

    fun getTopicStats(): Map<String, Int> {
        val raw = prefs.getString(KEY_TOPIC_STATS, "{}") ?: "{}"
        val obj = JSONObject(raw)
        val map = mutableMapOf<String, Int>()
        obj.keys().forEach { k ->
            map[k] = obj.optInt(k, 0)
        }
        return map
    }

    private fun updateTopicStats(topics: List<String>) {
        if (topics.isEmpty()) return
        val current = getTopicStats().toMutableMap()
        topics.forEach { t ->
            current[t] = (current[t] ?: 0) + 1
        }
        val obj = JSONObject()
        current.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(KEY_TOPIC_STATS, obj.toString()).apply()
    }

    fun topTopics(limit: Int = 5): List<Pair<String, Int>> {
        return getTopicStats().toList()
            .sortedByDescending { it.second }
            .take(limit)
    }

    // ---------------------------
    // Эвристическое извлечение фактов/тем
    // ---------------------------

    /**
     * Вызывай после user-сообщения, чтобы сохранять простые факты:
     * "меня зовут ..."
     * "мне нравится ..."
     * "я изучаю ..."
     */
    fun tryExtractAndStoreFacts(userText: String) {
        val t = userText.lowercase().trim()

        extractName(t)?.let { upsertFact("name", it, 0.85) }
        extractPreference(t)?.let { upsertFact("preference", it, 0.7) }
        extractStudyTopic(t)?.let { upsertFact("study_topic", it, 0.75) }
    }

    private fun extractName(t: String): String? {
        val markers = listOf("меня зовут ", "я ", "мое имя ")
        for (m in markers) {
            if (t.startsWith(m)) {
                val v = t.removePrefix(m).trim().split(" ").take(2).joinToString(" ")
                if (v.length in 2..30) return v
            }
        }
        return null
    }

    private fun extractPreference(t: String): String? {
        val m = "мне нравится "
        return if (t.contains(m)) t.substringAfter(m).take(60).trim() else null
    }

    private fun extractStudyTopic(t: String): String? {
        val m1 = "я изучаю "
        val m2 = "хочу изучать "
        return when {
            t.contains(m1) -> t.substringAfter(m1).take(60).trim()
            t.contains(m2) -> t.substringAfter(m2).take(60).trim()
            else -> null
        }
    }

    private fun extractTopics(text: String): List<String> {
        val t = text.lowercase()
        val topics = mutableListOf<String>()

        if (listOf("физик", "ньютон", "энерг", "сила", "ускорен").any { t.contains(it) }) topics += "physics"
        if (listOf("хим", "реакц", "молекул", "кислот", "щелоч").any { t.contains(it) }) topics += "chemistry"
        if (listOf("медицин", "симптом", "здоров", "болезн", "врач").any { t.contains(it) }) topics += "medicine"
        if (listOf("русск", "орфограф", "пунктуац", "граммат").any { t.contains(it) }) topics += "russian"
        if (listOf("литератур", "роман", "поэз", "автор", "персонаж").any { t.contains(it) }) topics += "literature"
        if (listOf("программ", "код", "android", "kotlin", "api").any { t.contains(it) }) topics += "tech"
        if (listOf("работа", "карьер", "цель", "продуктив").any { t.contains(it) }) topics += "self_growth"

        return topics.distinct()
    }
}
