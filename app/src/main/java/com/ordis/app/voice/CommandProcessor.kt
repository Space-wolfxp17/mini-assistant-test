package com.ordis.app.voice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import com.ordis.app.learning.LearningRepository
import com.ordis.app.learning.PlannerService

class CommandProcessor(
    private val context: Context,
    private val speak: (String) -> Unit
) {
    private val learning = LearningRepository(context)
    private val planner = PlannerService()

    fun process(rawText: String): Boolean {
        val text = rawText.lowercase().trim()
        val cmd = text.removePrefix("привет ордис").removePrefix("ордис").trim()

        if (cmd.isBlank()) {
            speak("Слушаю")
            return false
        }

        // 1) Команда самообучения вручную:
        // "запомни команду открыть кинопоиск действие OPEN_URL:https://www.kinopoisk.ru"
        if (cmd.startsWith("запомни команду")) {
            return teachCommand(cmd)
        }

        // 2) Сначала проверяем обученные команды
        val learnedAction = learning.findBestActionByPhrase(cmd)
        if (learnedAction != null) {
            val ok = executeAction(learnedAction)
            if (ok) learning.markSuccess(cmd) else learning.markFail(cmd)
            return ok
        }

        // 3) Базовые правила + онлайн-планер (пока заглушка)
        val planned = planner.plan(cmd)
        if (planned != null) {
            val ok = executeAction(planned)
            if (ok) {
                learning.remember(cmd, planned) // автообучение на успешной интерпретации
                learning.markSuccess(cmd)
            } else {
                learning.markFail(cmd)
            }
            return ok
        }

        speak("Пока не знаю такую команду. Скажи: запомни команду ... действие ...")
        return false
    }

    private fun teachCommand(cmd: String): Boolean {
        val phrasePart = cmd.substringAfter("запомни команду", "").trim()
        if (!phrasePart.contains("действие")) {
            speak("Формат: запомни команду [фраза] действие [тип]")
            return false
        }
        val phrase = phrasePart.substringBefore("действие").trim()
        val action = phrasePart.substringAfter("действие").trim()
        if (phrase.isBlank() || action.isBlank()) {
            speak("Не хватает фразы или действия")
            return false
        }
        learning.remember(phrase, action)
        speak("Запомнила")
        return true
    }

    private fun executeAction(action: String): Boolean {
        return when {
            action == "OPEN_CAMERA" -> tryStart(
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                "Открываю камеру"
            )
            action == "OPEN_VIDEO_CAMERA" -> tryStart(
                Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                "Открываю видео камеру"
            )
            action == "OPEN_SETTINGS" -> tryStart(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                "Открываю настройки"
            )
            action == "OPEN_VK" -> {
                val launch = context.packageManager.getLaunchIntentForPackage("com.vkontakte.android")
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    tryStart(launch, "Открываю ВК")
                } else {
                    speak("ВК не установлен")
                    false
                }
            }
            action.startsWith("SEARCH_YANDEX:") -> {
                val q = action.substringAfter("SEARCH_YANDEX:").trim()
                openYandexSearch(q)
            }
            action.startsWith("OPEN_URL:") -> {
                val url = action.substringAfter("OPEN_URL:").trim()
                tryStart(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "Открываю")
            }
            action.startsWith("OPEN_PACKAGE:") -> {
                val pkg = action.substringAfter("OPEN_PACKAGE:").trim()
                val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    tryStart(launch, "Открываю приложение")
                } else {
                    speak("Приложение не найдено")
                    false
                }
            }
            else -> {
                speak("Неизвестный тип действия")
                false
            }
        }
    }

    private fun openYandexSearch(query: String): Boolean {
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
            speak("Не удалось выполнить")
            false
        }
    }
}
