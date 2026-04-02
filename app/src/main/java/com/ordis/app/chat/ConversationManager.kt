package com.ordis.app.chat

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

/**
 * ConversationManager v2 PRO
 * - единый интеллект (без режимов)
 * - intent/tone/complexity анализ
 * - short-term + long-term память
 * - топ-темы пользователя
 * - автоадаптация профиля
 * - онлайн ответ через Gemini + оффлайн fallback
 */
class ConversationManager(
    private val memory: ConversationMemoryRepository,
    private val profileRepo: UserProfileRepository,
    private val chatService: GeminiChatService,
    private val speak: (String) -> Unit,
    private val onDebug: ((String) -> Unit)? = null
) {

    data class InputAnalysis(
        val cleanedText: String,
        val intent: IntentType,
        val emotionalTone: EmotionalTone,
        val complexity: Complexity,
        val needsClarification: Boolean,
        val isMedicalRisk: Boolean
    )

    enum class IntentType {
        SMALL_TALK, QUESTION, EXPLANATION_REQUEST, ARGUMENTATION, PERSONAL_HELP, UNKNOWN
    }

    enum class EmotionalTone {
        CALM, TIRED, STRESSED, ANGRY, EXCITED, SAD, NEUTRAL
    }

    enum class Complexity {
        SIMPLE, MEDIUM, ADVANCED
    }

    fun reply(userTextRaw: String, onDone: ((String) -> Unit)? = null) {
        val analysis = analyzeInput(userTextRaw)

        // Сохраняем в историю и пытаемся вытащить факты
        memory.append(ChatMessage(role = "user", text = analysis.cleanedText))
        memory.tryExtractAndStoreFacts(analysis.cleanedText)

        // Автоадаптация профиля
        profileRepo.adaptBySignals(
            userText = analysis.cleanedText,
            isComplex = analysis.complexity == Complexity.ADVANCED,
            isTiredOrStressed = analysis.emotionalTone == EmotionalTone.TIRED || analysis.emotionalTone == EmotionalTone.STRESSED,
            likesExamplesHint = analysis.cleanedText.contains("пример")
        )

        thread {
            val profile = profileRepo.getProfile()
            val history = memory.getHistory(30)
            val facts = memory.getFacts()
                .sortedByDescending { it.confidence }
                .take(8)
            val topTopics = memory.topTopics(5)

            val systemPrompt = buildSystemPrompt(
                analysis = analysis,
                profile = profile,
                facts = facts,
                topTopics = topTopics
            )

            val online = chatService.askWithCustomSystem(
                systemPrompt = systemPrompt,
                history = history,
                userInput = analysis.cleanedText
            )

            val answer = when {
                analysis.isMedicalRisk -> wrapMedicalSafety(
                    online ?: offlineMedicalSafeAnswer(analysis.cleanedText)
                )
                !online.isNullOrBlank() -> postProcess(online, analysis, profile)
                else -> offlineFallback(analysis, profile)
            }

            memory.append(ChatMessage(role = "assistant", text = answer))
            speak(answer)
            onDone?.invoke(answer)

            onDebug?.invoke(
                "intent=${analysis.intent}; tone=${analysis.emotionalTone}; " +
                        "complexity=${analysis.complexity}; clarification=${analysis.needsClarification}; " +
                        "topics=${topTopics.joinToString { it.first }}"
            )
        }
    }

    // ---------------------------
    // ANALYSIS
    // ---------------------------

    private fun analyzeInput(raw: String): InputAnalysis {
        val text = normalize(raw)

        val intent = detectIntent(text)
        val tone = detectTone(text)
        val complexity = detectComplexity(text)
        val needClarify = needsClarification(text, intent)
        val medicalRisk = isMedicalRisk(text)

        return InputAnalysis(
            cleanedText = text,
            intent = intent,
            emotionalTone = tone,
            complexity = complexity,
            needsClarification = needClarify,
            isMedicalRisk = medicalRisk
        )
    }

    private fun detectIntent(text: String): IntentType {
        return when {
            text.length < 12 -> IntentType.SMALL_TALK
            text.contains("?") -> IntentType.QUESTION
            listOf("объясни", "разбери", "почему", "как работает").any { text.contains(it) } ->
                IntentType.EXPLANATION_REQUEST
            listOf("докажи", "аргумент", "сравни", "плюсы и минусы").any { text.contains(it) } ->
                IntentType.ARGUMENTATION
            listOf("помоги", "мне тяжело", "устал", "нет сил").any { text.contains(it) } ->
                IntentType.PERSONAL_HELP
            else -> IntentType.UNKNOWN
        }
    }

    private fun detectTone(text: String): EmotionalTone {
        return when {
            listOf("злит", "бесит", "достало", "ненавижу").any { text.contains(it) } -> EmotionalTone.ANGRY
            listOf("устал", "выгорел", "нет сил").any { text.contains(it) } -> EmotionalTone.TIRED
            listOf("тревожно", "переживаю", "стресс").any { text.contains(it) } -> EmotionalTone.STRESSED
            listOf("грустно", "печально").any { text.contains(it) } -> EmotionalTone.SAD
            listOf("круто", "супер", "ура", "рад").any { text.contains(it) } -> EmotionalTone.EXCITED
            else -> EmotionalTone.CALM
        }
    }

    private fun detectComplexity(text: String): Complexity {
        val words = text.split(" ").filter { it.isNotBlank() }
        val longWords = words.count { it.length >= 9 }
        val hardMarkers = listOf("механизм", "концепция", "доказательство", "корреляция", "формализация")
        val score = longWords + hardMarkers.count { text.contains(it) }

        return when {
            score >= 5 -> Complexity.ADVANCED
            score >= 2 -> Complexity.MEDIUM
            else -> Complexity.SIMPLE
        }
    }

    private fun needsClarification(text: String, intent: IntentType): Boolean {
        if (text.length < 8) return true
        if (intent == IntentType.UNKNOWN && text.split(" ").size <= 3) return true
        return false
    }

    private fun isMedicalRisk(text: String): Boolean {
        val risky = listOf(
            "назначь лечение",
            "какие таблетки пить",
            "дозировка для меня",
            "что принять без врача",
            "как лечиться самому"
        )
        return risky.any { text.contains(it) }
    }

    // ---------------------------
    // PROMPT
    // ---------------------------

    private fun buildSystemPrompt(
        analysis: InputAnalysis,
        profile: UserProfileRepository.UserProfile,
        facts: List<ConversationMemoryRepository.MemoryFact>,
        topTopics: List<Pair<String, Int>>
    ): String {
        val tonePolicy = when (analysis.emotionalTone) {
            EmotionalTone.ANGRY -> "Отвечай спокойно, не спорь, деэскалируй."
            EmotionalTone.TIRED -> "Короткие фразы, минимум воды, мягкая поддержка."
            EmotionalTone.STRESSED -> "Структура: 1) суть 2) шаги 3) что сделать сейчас."
            EmotionalTone.SAD -> "Эмпатия + практичный маленький шаг."
            EmotionalTone.EXCITED -> "Поддержи энергию и направь в действие."
            else -> "Уверенно, дружелюбно, естественно."
        }

        val complexityPolicy = when (analysis.complexity) {
            Complexity.SIMPLE -> "Просто и понятно."
            Complexity.MEDIUM -> "Кратко + детали."
            Complexity.ADVANCED -> "Глубоко и структурно, с терминами и примерами."
        }

        val intentPolicy = when (analysis.intent) {
            IntentType.SMALL_TALK -> "Веди живой диалог как умный собеседник."
            IntentType.QUESTION -> "Сначала короткий ответ, затем пояснение."
            IntentType.EXPLANATION_REQUEST -> "Объясняй пошагово, используй аналогии."
            IntentType.ARGUMENTATION -> "Дай аргументы за/против, затем вывод."
            IntentType.PERSONAL_HELP -> "Поддержка без давления, 1-2 выполнимых шага."
            IntentType.UNKNOWN -> "Если запрос неясен — один уточняющий вопрос."
        }

        val factsBlock = if (facts.isEmpty()) {
            "Долгосрочных фактов пока нет."
        } else {
            facts.joinToString("\n") { "- ${it.key}: ${it.value} (conf=${"%.2f".format(it.confidence)})" }
        }

        val topicsBlock = if (topTopics.isEmpty()) {
            "Топ-тем пока нет."
        } else {
            topTopics.joinToString(", ") { "${it.first}:${it.second}" }
        }

        val examplePolicy = if (profile.wantsExamples) "Добавляй пример, когда уместно." else "Примеры по запросу."
        val stepsPolicy = if (profile.wantsStepByStep) "Если задача сложная — давай шаги." else "Без лишней пошаговости."
        val clarifyPolicy = if (analysis.needsClarification) "Запрос короткий — задай 1 уточнение." else "Не задавай лишних уточнений."

        return """
            Ты Ордис — интеллектуальный ассистент-собеседник.
            Твоя цель: понимать смысл, отвечать как человек, быть полезным, точным и уважительным.
            
            Дата/время: ${nowString()}
            
            Профиль пользователя:
            - Стиль: ${profile.preferredStyle}
            - Уровень: ${profile.level}
            - Интересы: ${profile.interests}
            - Предпочт. длина ответа: ${profile.responseLength}
            - Формат: ${profile.responseFormat}
            - Тон общения: ${profile.emotionalTonePreference}
            
            Память (долгосрочные факты):
            $factsBlock
            
            Частые темы:
            $topicsBlock
            
            Политики ответа:
            - Тон: $tonePolicy
            - Сложность: $complexityPolicy
            - Намерение: $intentPolicy
            - Примеры: $examplePolicy
            - Шаги: $stepsPolicy
            - Уточнение: $clarifyPolicy
            
            Формат ответа:
            1) Смысл в 1-2 предложениях
            2) Полезные детали
            3) Следующий шаг/вопрос
            
            Ограничения:
            - Не заявляй, что у тебя есть сознание/воля.
            - Не придумывай факты, если не уверен.
            - В медицине — только образовательная информация, без назначения лечения.
        """.trimIndent()
    }

    // ---------------------------
    // OUTPUT PROCESSING
    // ---------------------------

    private fun postProcess(
        raw: String,
        analysis: InputAnalysis,
        profile: UserProfileRepository.UserProfile
    ): String {
        var out = raw.trim()

        // Контроль длины по профилю
        out = when (profile.responseLength.lowercase()) {
            "короткая" -> out.take(500)
            "средняя" -> out.take(1000)
            "подробная" -> out.take(1800)
            else -> out.take(1200)
        }

        if (analysis.needsClarification && !out.contains("?")) {
            out += " Уточни, пожалуйста, что важнее: коротко по сути или подробно с примером?"
        }

        return out
    }

    private fun offlineFallback(
        analysis: InputAnalysis,
        profile: UserProfileRepository.UserProfile
    ): String {
        val base = when (analysis.intent) {
            IntentType.SMALL_TALK -> "Я на связи. Можем спокойно обсудить любую тему."
            IntentType.QUESTION -> "Сейчас интернет недоступен, но я могу попробовать ответить базово."
            IntentType.EXPLANATION_REQUEST -> "Давай разберём шаг за шагом даже оффлайн."
            IntentType.ARGUMENTATION -> "Разберём аргументы: тезис, плюсы, минусы, вывод."
            IntentType.PERSONAL_HELP -> "Я рядом. Давай начнём с одного маленького шага."
            IntentType.UNKNOWN -> "Сформулируй чуть конкретнее, и я отвечу точнее."
        }

        val addon = if (profile.wantsExamples) " Если хочешь, добавлю пример." else ""
        return base + addon
    }

    private fun offlineMedicalSafeAnswer(userText: String): String {
        return "Я могу дать только образовательную информацию и не назначаю лечение. " +
                "По теме \"$userText\" могу объяснить общие принципы и когда стоит обратиться к врачу."
    }

    private fun wrapMedicalSafety(text: String): String {
        return "Важно: это не медицинское назначение.\n$text"
    }

    // ---------------------------
    // UTILS
    // ---------------------------

    private fun normalize(s: String): String {
        return s.lowercase()
            .replace("ё", "е")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun nowString(): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru", "RU")).format(Date())
    }
}
