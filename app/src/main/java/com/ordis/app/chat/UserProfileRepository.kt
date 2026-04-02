package com.ordis.app.chat

import android.content.Context

class UserProfileRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ordis_user_profile", Context.MODE_PRIVATE)

    fun getPreferredStyle(): String = prefs.getString("style", "дружелюбно, ясно, по делу")!!
    fun setPreferredStyle(value: String) = prefs.edit().putString("style", value).apply()

    fun getLevel(): String = prefs.getString("level", "средний")!!
    fun setLevel(value: String) = prefs.edit().putString("level", value).apply()

    fun getInterests(): String = prefs.getString("interests", "технологии, саморазвитие")!!
    fun setInterests(value: String) = prefs.edit().putString("interests", value).apply()
}
