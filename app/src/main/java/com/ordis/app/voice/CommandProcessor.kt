package com.ordis.app.voice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.ordis.app.chat.ConversationManager

class CommandProcessor(
    private val context: Context,
    private val speak: (String) -> Unit,
    private val conversationManager: ConversationManager
) {

    /**
     * Возвращает true, если запрос обработан.
     * Логика:
     * 1) Если это action-команда (камера/поиск/настройки/музыка и т.п.) — выполнить.
     * 2) Иначе отправить в ConversationManager как обычный диалог.
     */
    fun process(rawText: String): Boolean {
        val text = normalize(rawText)

        // Нужна активация "ордис" в начале/содержании
        if (!text.contains("ордис")) return false

        // Убираем обращение
        val cmd = text
            .replace("привет ордис", "")
            .replace("ордис", "")
            .trim()

        if (cmd.isBlank()) {
            speak("Да, я слушаю")
            return true
        }

        // --- Action-команды (без режимов) ---
        parseAction(cmd)?.let { action ->
            return executeAction(action)
        }

        // --- Обычный разговор / обучение / рассуждение ---
        // Всё, что не action, уходит в диалоговый движок
        conversationManager.reply(cmd)
        return true
    }

    private fun parseAction(cmd: String): String? {
        val c = cmd.lowercase()

        return when {
            // Поиск
            c.startsWith("найди ") -> "SEARCH_YANDEX:${c.removePrefix("найди").trim()}"
            c.startsWith("поиск ") -> "SEARCH_YANDEX:${c.removePrefix("поиск").trim()}"

            // Камера / видео
            c.contains("открой камеру") || c.contains("включи камеру") -> "OPEN_CAMERA"
            c.contains("сними на видео") || c.contains("запиши видео") || c.contains("включи видео камеру") -> "OPEN_VIDEO_CAMERA"

            // Галерея
            c.contains("открой галерею") || c.contains("покажи фото") -> "OPEN_GALLERY"

            // Настройки
            c.contains("открой настройки") || c.contains("зайди в настройки") -> "OPEN_SETTINGS"
            c.contains("вайфай") || c.contains("wi-fi") -> "OPEN_WIFI_SETTINGS"
            c.contains("блютуз") || c.contains("bluetooth") -> "OPEN_BT_SETTINGS"

            // Музыка / приложения
            c.contains("музыка в вк") || c.contains("включи вк") -> "OPEN_PACKAGE:com.vkontakte.android"
            c.contains("открой ютуб") || c.contains("включи ютуб") -> "OPEN_PACKAGE:com.google.android.youtube"
            c.contains("открой браузер") -> "OPEN_URL:https://yandex.ru"

            // Будильник / таймер
            c.contains("поставь будильник") -> "SET_ALARM"
            c.contains("поставь таймер") -> "SET_TIMER_60"

            else -> null
        }
    }

    private fun executeAction(action: String): Boolean {
        return when {
            action == "OPEN_CAMERA" ->
                startIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), "Открываю камеру")

            action == "OPEN_VIDEO_CAMERA" ->
                startIntent(Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA), "Открываю видео камеру")

            action == "OPEN_GALLERY" -> {
                val i = Intent(Intent.ACTION_VIEW).apply { type = "image/*" }
                startIntent(i, "Открываю галерею")
            }

            action == "OPEN_SETTINGS" ->
                startIntent(Intent(Settings.ACTION_SETTINGS), "Открываю настройки")

            action == "OPEN_WIFI_SETTINGS" ->
                startIntent(Intent(Settings.ACTION_WIFI_SETTINGS), "Открываю Wi-Fi настройки")

            action == "OPEN_BT_SETTINGS" ->
                startIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Открываю Bluetooth настройки")

            action == "SET_ALARM" -> {
                val i = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                }
                startIntent(i, "Открываю будильник")
            }

            action == "SET_TIMER_60" -> {
                val i = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, 60)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                }
                startIntent(i, "Открываю таймер на минуту")
            }

            action.startsWith("SEARCH_YANDEX:") ->
                searchYandex(action.substringAfter("SEARCH_YANDEX:").trim())

            action.startsWith("OPEN_URL:") ->
                openUrl(action.substringAfter("OPEN_URL:").trim(), "Открываю ссылку")

            action.startsWith("OPEN_PACKAGE:") ->
                openPackage(action.substringAfter("OPEN_PACKAGE:").trim(), "Открываю приложение")

            else -> {
                speak("Команда не поддерживается")
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
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(yandexIntent)
            speak("Ищу в Яндексе: $query")
            true
        } catch (_: Exception) {
            openUrl(url, "Ищу в браузере: $query")
        }
    }

    private fun openUrl(url: String, okText: String): Boolean {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            speak("Некорректная ссылка")
            return false
        }
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startIntent(i, okText)
    }

    private fun openPackage(pkg: String, okText: String): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
        return if (launch != null) {
            startIntent(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), okText)
        } else {
            speak("Приложение не установлено")
            false
        }
    }

    private fun startIntent(intent: Intent, okText: String): Boolean {
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            speak(okText)
            true
        } catch (_: Exception) {
            speak("Не удалось выполнить команду")
            false
        }
    }

    private fun normalize(s: String): String {
        return s.lowercase()
            .replace("ё", "е")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
