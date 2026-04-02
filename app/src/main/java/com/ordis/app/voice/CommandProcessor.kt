package com.ordis.app.voice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.ordis.app.ai.GeminiPlanner
import com.ordis.app.data.repo.SettingsRepository
import kotlin.concurrent.thread

class CommandProcessor(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val speak: (String) -> Unit
) {
    private val geminiPlanner = GeminiPlanner {
        settingsRepository.settings.value.geminiApiKey
    }

    fun process(rawText: String): Boolean {
        val text = rawText.lowercase().trim()
        val cmd = text.removePrefix("привет ордис").removePrefix("ордис").trim()

        if (cmd.isBlank()) {
            speak("Слушаю")
            return false
        }

        // Локальные быстрые правила
        localPlan(cmd)?.let { action ->
            return executeAction(action)
        }

        // Онлайн-планер (Gemini) в фоне
        thread {
            val action = geminiPlanner.plan(cmd)
            if (action == null) {
                speak("Не удалось распознать команду онлайн")
            } else {
                executeAction(action)
            }
        }
        speak("Уточняю команду через интернет")
        return true
    }

    private fun localPlan(cmd: String): String? {
        return when {
            cmd.startsWith("найди ") -> "SEARCH_YANDEX:${cmd.removePrefix("найди").trim()}"
            cmd.contains("открой камеру") || cmd.contains("включи камеру") -> "OPEN_CAMERA"
            cmd.contains("сними на видео") || cmd.contains("запиши видео") -> "OPEN_VIDEO_CAMERA"
            cmd.contains("открой настройки") -> "OPEN_SETTINGS"
            cmd.contains("включи музыку в вк") || cmd.contains("музыка в вк") -> "OPEN_VK"
            cmd.contains("открой ютуб") -> "OPEN_PACKAGE:com.google.android.youtube"
            cmd.contains("открой браузер") -> "OPEN_URL:https://yandex.ru"
            cmd.contains("поставь таймер") -> "SET_TIMER_60"
            cmd.contains("поставь будильник") -> "SET_ALARM"
            else -> null
        }
    }

    private fun executeAction(action: String): Boolean {
        return when {
            action == "OPEN_CAMERA" ->
                startIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), "Открываю камеру")

            action == "OPEN_VIDEO_CAMERA" ->
                startIntent(Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA), "Открываю видео камеру")

            action == "OPEN_SETTINGS" ->
                startIntent(Intent(Settings.ACTION_SETTINGS), "Открываю настройки")

            action == "OPEN_VK" ->
                openPackage("com.vkontakte.android", "Открываю ВК")

            action == "SET_TIMER_60" -> {
                val i = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, 60)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                }
                startIntent(i, "Открываю таймер")
            }

            action == "SET_ALARM" -> {
                val i = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                }
                startIntent(i, "Открываю будильник")
            }

            action.startsWith("SEARCH_YANDEX:") ->
                searchYandex(action.substringAfter("SEARCH_YANDEX:").trim())

            action.startsWith("OPEN_URL:") ->
                openUrl(action.substringAfter("OPEN_URL:").trim(), "Открываю ссылку")

            action.startsWith("OPEN_PACKAGE:") ->
                openPackage(action.substringAfter("OPEN_PACKAGE:").trim(), "Открываю приложение")

            else -> {
                speak("Небезопасное или неизвестное действие отклонено")
                false
            }
        }
    }

    private fun searchYandex(query: String): Boolean {
        if (query.isBlank()) {
            speak("Что найти?")
            return false
        }
        val url = "https://yandex.ru/search/?text=" + Uri.encode(query)
        val yandexIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.yandex.browser")
        }
        return try {
            context.startActivity(yandexIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            speak("Ищу в Яндексе: $query")
            true
        } catch (_: Exception) {
            openUrl(url, "Ищу в браузере: $query")
        }
    }

    private fun openPackage(pkg: String, ok: String): Boolean {
        val i = context.packageManager.getLaunchIntentForPackage(pkg)
        return if (i != null) startIntent(i, ok) else run {
            speak("Приложение не установлено")
            false
        }
    }

    private fun openUrl(url: String, ok: String): Boolean {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            speak("Некорректная ссылка")
            return false
        }
        return startIntent(Intent(Intent.ACTION_VIEW, Uri.parse(url)), ok)
    }

    private fun startIntent(intent: Intent, ok: String): Boolean {
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            speak(ok)
            true
        } catch (_: Exception) {
            speak("Не удалось выполнить")
            false
        }
    }
}
