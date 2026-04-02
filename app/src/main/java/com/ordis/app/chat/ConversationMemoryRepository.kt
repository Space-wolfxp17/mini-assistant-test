package com.ordis.app.chat

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ConversationMemoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ordis_conversation", Context.MODE_PRIVATE)

    private val KEY_HISTORY = "history"
    private val KEY_MODE = "mode"

    fun getMode(): TutorMode {
        return try {
            TutorMode.valueOf(prefs.getString(KEY_MODE, TutorMode.GENERAL.name)!!)
        } catch (_: Exception) {
            TutorMode.GENERAL
        }
    }

    fun setMode(mode: TutorMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun getHistory(limit: Int = 20): List<ChatMessage> {
        val raw = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<ChatMessage>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += ChatMessage(
                role = o.optString("role"),
                text = o.optString("text"),
                ts = o.optLong("ts", System.currentTimeMillis())
            )
        }
        return list.takeLast(limit)
    }

    fun append(message: ChatMessage, max: Int = 60) {
        val list = getHistory(200).toMutableList()
        list += message
        val trimmed = list.takeLast(max)
        val arr = JSONArray()
        trimmed.forEach {
            arr.put(JSONObject().apply {
                put("role", it.role)
                put("text", it.text)
                put("ts", it.ts)
            })
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
