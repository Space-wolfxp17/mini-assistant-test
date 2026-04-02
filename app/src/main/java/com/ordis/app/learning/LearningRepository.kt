package com.ordis.app.learning

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LearnedCommand(
    val phrase: String,
    val action: String,   // e.g. "OPEN_CAMERA", "OPEN_URL:https://..."
    val successCount: Int = 0,
    val failCount: Int = 0
)

class LearningRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ordis_learning", Context.MODE_PRIVATE)

    private val KEY_COMMANDS = "commands"

    fun getAll(): List<LearnedCommand> {
        val raw = prefs.getString(KEY_COMMANDS, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val result = mutableListOf<LearnedCommand>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result += LearnedCommand(
                phrase = o.optString("phrase"),
                action = o.optString("action"),
                successCount = o.optInt("success", 0),
                failCount = o.optInt("fail", 0)
            )
        }
        return result
    }

    fun saveAll(list: List<LearnedCommand>) {
        val arr = JSONArray()
        list.forEach {
            val o = JSONObject()
            o.put("phrase", it.phrase)
            o.put("action", it.action)
            o.put("success", it.successCount)
            o.put("fail", it.failCount)
            arr.put(o)
        }
        prefs.edit().putString(KEY_COMMANDS, arr.toString()).apply()
    }

    fun remember(phrase: String, action: String) {
        val normalized = phrase.trim().lowercase()
        val list = getAll().toMutableList()
        val idx = list.indexOfFirst { it.phrase == normalized }
        if (idx >= 0) {
            list[idx] = list[idx].copy(action = action)
        } else {
            list += LearnedCommand(phrase = normalized, action = action)
        }
        saveAll(list)
    }

    fun findBestActionByPhrase(phrase: String): String? {
        val normalized = phrase.trim().lowercase()
        val candidates = getAll().filter { normalized.contains(it.phrase) || it.phrase.contains(normalized) }
        return candidates.maxByOrNull { it.successCount - it.failCount }?.action
    }

    fun markSuccess(phrase: String) = mark(phrase, success = true)
    fun markFail(phrase: String) = mark(phrase, success = false)

    private fun mark(phrase: String, success: Boolean) {
        val normalized = phrase.trim().lowercase()
        val list = getAll().toMutableList()
        val idx = list.indexOfFirst { it.phrase == normalized }
        if (idx >= 0) {
            val c = list[idx]
            list[idx] = if (success) c.copy(successCount = c.successCount + 1)
            else c.copy(failCount = c.failCount + 1)
            saveAll(list)
        }
    }
}
