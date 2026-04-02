package com.ordis.app.chat

import android.content.Context
import org.json.JSONObject

/**
 * UserProfileRepository PRO
 * Персональный профиль общения:
 * - стиль ответа
 * - желаемая длина ответа
 * - уровень сложности
 * - тон
 * - предпочтительный формат (шаги/кратко/пример)
 */
class UserProfileRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ordis_user_profile", Context.MODE_PRIVATE)

    private val KEY_PROFILE = "profile_json"

    data class UserProfile(
        val preferredStyle: String = "дружелюбно, ясно, по делу",
        val level: String = "средний",
        val interests: String = "технологии, саморазвитие",
        val responseLength: String = "средняя", // короткая|средняя|подробная
        val responseFormat: String = "кратко+подробно+шаг",
        val emotionalTonePreference: String = "спокойный",
        val wantsExamples: Boolean = true,
        val wantsStepByStep: Boolean = true
    )

    fun getProfile(): UserProfile {
        val raw = prefs.getString(KEY_PROFILE, null) ?: return UserProfile()
        return try {
            val o = JSONObject(raw)
            UserProfile(
                preferredStyle = o.optString("preferredStyle", "дружелюбно, ясно, по делу"),
                level = o.optString("level", "средний"),
                interests = o.optString("interests", "технологии, саморазвитие"),
                responseLength = o.optString("responseLength", "средняя"),
                responseFormat = o.optString("responseFormat", "кратко+подробно+шаг"),
                emotionalTonePreference = o.optString("emotionalTonePreference", "спокойный"),
                wantsExamples = o.optBoolean("wantsExamples", true),
                wantsStepByStep = o.optBoolean("wantsStepByStep", true)
            )
        } catch (_: Exception) {
            UserProfile()
        }
    }

    fun saveProfile(profile: UserProfile) {
        val o = JSONObject()
            .put("preferredStyle", profile.preferredStyle)
            .put("level", profile.level)
            .put("interests", profile.interests)
            .put("responseLength", profile.responseLength)
            .put("responseFormat", profile.responseFormat)
            .put("emotionalTonePreference", profile.emotionalTonePreference)
            .put("wantsExamples", profile.wantsExamples)
            .put("wantsStepByStep", profile.wantsStepByStep)

        prefs.edit().putString(KEY_PROFILE, o.toString()).apply()
    }

    fun getPreferredStyle() = getProfile().preferredStyle
    fun getLevel() = getProfile().level
    fun getInterests() = getProfile().interests

    fun setPreferredStyle(value: String) = saveProfile(getProfile().copy(preferredStyle = value))
    fun setLevel(value: String) = saveProfile(getProfile().copy(level = value))
    fun setInterests(value: String) = saveProfile(getProfile().copy(interests = value))
    fun setResponseLength(value: String) = saveProfile(getProfile().copy(responseLength = value))
    fun setResponseFormat(value: String) = saveProfile(getProfile().copy(responseFormat = value))
    fun setEmotionalTonePreference(value: String) = saveProfile(getProfile().copy(emotionalTonePreference = value))
    fun setWantsExamples(value: Boolean) = saveProfile(getProfile().copy(wantsExamples = value))
    fun setWantsStepByStep(value: Boolean) = saveProfile(getProfile().copy(wantsStepByStep = value))

    /**
     * Легкое автообучение под пользователя (эвристики).
     * Вызывается ConversationManager после анализа входа.
     */
    fun adaptBySignals(
        userText: String,
        isComplex: Boolean,
        isTiredOrStressed: Boolean,
        likesExamplesHint: Boolean
    ) {
        val p = getProfile()

        var level = p.level
        if (isComplex && p.level != "продвинутый") level = "продвинутый"

        var style = p.preferredStyle
        var tone = p.emotionalTonePreference
        if (isTiredOrStressed) {
            style = "кратко, мягко, с поддержкой"
            tone = "спокойный"
        }

        var wantsExamples = p.wantsExamples
        if (likesExamplesHint) wantsExamples = true

        // Небольшой захват интересов
        val t = userText.lowercase()
        val inferredInterests = buildList {
            if (listOf("физ", "ньютон", "энерг", "механи").any { t.contains(it) }) add("физика")
            if (listOf("хим", "реакц", "кислот", "молек").any { t.contains(it) }) add("химия")
            if (listOf("android", "kotlin", "код", "api").any { t.contains(it) }) add("программирование")
            if (listOf("русск", "граммат", "литератур").any { t.contains(it) }) add("язык и литература")
        }.distinct()

        val mergedInterests = mergeInterests(p.interests, inferredInterests)

        saveProfile(
            p.copy(
                preferredStyle = style,
                level = level,
                emotionalTonePreference = tone,
                wantsExamples = wantsExamples,
                interests = mergedInterests
            )
        )
    }

    private fun mergeInterests(base: String, inferred: List<String>): String {
        if (inferred.isEmpty()) return base
        val set = base.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()

        inferred.forEach { set.add(it) }
        return set.joinToString(", ")
    }
}
