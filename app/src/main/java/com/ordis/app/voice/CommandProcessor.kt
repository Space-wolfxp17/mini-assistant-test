package com.ordis.app.voice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings

class CommandProcessor(
    private val context: Context,
    private val speak: (String) -> Unit
) {
    fun process(rawText: String): Boolean {
        val text = rawText.lowercase().trim()
        val cmd = text.removePrefix("привет ордис").removePrefix("ордис").trim()

        if (cmd.startsWith("найди ")) {
            val q = cmd.removePrefix("найди").trim()
            if (q.isBlank()) {
                speak("Что найти?")
                return false
            }
            return openYandexSearch(q)
        }

        if (cmd.contains("открой камеру") || cmd.contains("включи камеру")) {
            return tryStart(
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                "Открываю камеру"
            )
        }

        if (cmd.contains("сними на видео") || cmd.contains("запиши видео")) {
            return tryStart(
                Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                "Открываю камеру для видео"
            )
        }

        if (cmd.contains("музыку в вк") || cmd.contains("включи музыку в вк")) {
            val launch = context.packageManager.getLaunchIntentForPackage("com.vkontakte.android")
            return if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                tryStart(launch, "Открываю ВК. Перейди в Музыку")
            } else {
                speak("ВК не установлен")
                false
            }
        }

        if (cmd.contains("открой настройки")) {
            return tryStart(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                "Открываю настройки"
            )
        }

        speak("Пока не умею это делать")
        return false
    }

    private fun openYandexSearch(query: String): Boolean {
        val url = "https://yandex.ru/search/?text=" + Uri.encode(query)
        val yandexIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.yandex.browser")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(yandexIntent)
            speak("Ищу в Яндексе: $query")
            true
        } catch (_: Exception) {
            tryStart(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                "Ищу в браузере: $query"
            )
        }
    }

    private fun tryStart(intent: Intent, okText: String): Boolean {
        return try {
            context.startActivity(intent)
            speak(okText)
            true
        } catch (_: Exception) {
            speak("Не удалось выполнить команду")
            false
        }
    }
}
