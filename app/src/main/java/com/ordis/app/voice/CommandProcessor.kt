package com.ordis.app.voice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings

class CommandProcessor(
    private val context: Context,
    private val speak: (String) -> Unit
) {
    fun process(rawText: String): Boolean {
        val text = rawText.lowercase().trim()
        val cmd = text
            .removePrefix("привет ордис")
            .removePrefix("ордис")
            .trim()

        if (cmd.isBlank()) {
            speak("Слушаю")
            return false
        }

        // Поиск
        if (cmd.startsWith("найди ")) {
            return searchYandex(cmd.removePrefix("найди").trim())
        }

        // Камера
        if (cmd.contains("открой камеру") || cmd.contains("включи камеру")) {
            return startIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), "Открываю камеру")
        }

        // Видео
        if (cmd.contains("сними на видео") || cmd.contains("запиши видео")) {
            return startIntent(Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA), "Открываю видео камеру")
        }

        // Галерея
        if (cmd.contains("открой галерею")) {
            val i = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
            }
            return startIntent(i, "Открываю галерею")
        }

        // Браузер
        if (cmd.contains("открой браузер")) {
            return openUrl("https://yandex.ru", "Открываю браузер")
        }

        // YouTube
        if (cmd.contains("открой ютуб") || cmd.contains("включи ютуб")) {
            return openPackageOrUrl("com.google.android.youtube", "https://youtube.com", "Открываю ютуб")
        }

        // VK
        if (cmd.contains("включи музыку в вк") || cmd.contains("музыка в вк")) {
            return openPackage("com.vkontakte.android", "Открываю ВК")
        }

        // Настройки
        if (cmd.contains("открой настройки")) {
            return startIntent(Intent(Settings.ACTION_SETTINGS), "Открываю настройки")
        }

        // Wi-Fi settings
        if (cmd.contains("вайфай") || cmd.contains("wi-fi")) {
            return startIntent(Intent(Settings.ACTION_WIFI_SETTINGS), "Открываю настройки Wi-Fi")
        }

        // Будильник
        if (cmd.contains("поставь будильник")) {
            val i = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
            return startIntent(i, "Открываю установку будильника")
        }

        // Таймер
        if (cmd.contains("поставь таймер")) {
            val i = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, 60)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
            return startIntent(i, "Открываю таймер на 1 минуту")
        }

        speak("Команда пока не поддерживается")
        return false
    }

    private fun searchYandex(query: String): Boolean {
        if (query.isBlank()) {
            speak("Что найти?")
            return false
        }
        val url = "https://yandex.ru/search/?text=" + Uri.encode(query)
        val yandex = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.yandex.browser")
        }
        return try {
            context.startActivity(yandex.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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

    private fun openPackageOrUrl(pkg: String, url: String, ok: String): Boolean {
        val i = context.packageManager.getLaunchIntentForPackage(pkg)
        return if (i != null) startIntent(i, ok) else openUrl(url, ok)
    }

    private fun openUrl(url: String, ok: String): Boolean {
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
