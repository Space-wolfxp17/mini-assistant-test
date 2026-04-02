package com.ordis.app.chat

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

/**
 * ConversationManager PRO
 * - Единый режим: собеседник + анализ + обучение
 * - Без переключений режимов
 * - Подстройка под пользователя
 * - Fallback в оффлайне
 */
class ConversationManager(
    private val memory: ConversationMemoryRepository,
    private val profile: UserProfileRepository,
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
        val isRiskMedical: Boolean
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

    /**
     * Главная точка ответа
     */
    fun reply(userTextRaw: String, onDone: ((String) -> Unit)? = null) {
        val analysis = analyzeInput(userTextRaw)
        val now = nowString()

        // Сохраняем вход в историю
        memory.append(
            ChatMessage(
                role = "user",
                text = analysis.cleanedText
            )
        )

        // Обновляем простой профиль-память (легкое "самообучение")
        adaptProfileByInput(analysis)

        thread {
            val history = memory.getHistory(30)
            val style = profile.getPreferredStyle()
            val level = profile.getLevel()
            val interests = profile.getInterests()

            val systemPrompt = buildSystemPrompt(
                analysis = analysis,
                style = style,
                level = level,
                interests = interests,
                currentTime = now
            )

            // Пробуем онлайн
            val onlineAnswer = chatService.askWithCustomSystem(
                systemPrompt = systemPrompt,
                history = history,
                userInput = analysis.cleanedText
            )

            val finalAnswer = when {
                analysis.isRiskMedical -> {
                    // Медицина: только образовательная безопасная форма
                    wrapMedicalSafety(
                        onlineAnswer ?: offlineMedicalSafeAnswer(analysis.cleanedText)
                    )
                }

                !onlineAnswer.isNullOrBlank() -> postProcessAnswer(onlineAnswer, analysis)

                else -> offlineFallbackAnswer(analysis)
            }

            // Сохраняем ответ
            memory.append(ChatMessage(role = "assistant", text = finalAnswer))

            // Произносим
            speak(finalAnswer)
            onDone?.invoke(finalAnswer)

            onDebug?.invoke(
                "analysis=intent:${analysis.intent}, tone:${analysis.emotionalTone}, " +
                        "complexity:${analysis.complexity}, clarification:${analysis.needsClarification}"
            )
        }
    }

    // ---------------------------
    // Анализ входа
    // ---------------------------

    private fun analyzeInput(raw: String): InputAnalysis {
        val text = normalize(raw)

        val intent = detectIntent(text)
        val tone = detectTone(text)
        val complexity = detectComplexity(text)
        val needClarification = needsClarification(text, intent)
        val riskMedical = isMedicalRisk(text)

        return InputAnalysis(
            cleanedText = text,
            intent = intent,
            emotionalTone = tone,
            complexity = complexity,
            needsClarification = needClarification,
            isRiskMedical = riskMedical
        )
    }

    private fun detectIntent(text: String): IntentType {
        return when {
            text.contains("?") -> IntentType.QUESTION
            text.contains("объясни") || text.contains("разбери") || text.contains("почему") ->
                IntentType.EXPLANATION_REQUEST
            text.contains("докажи") || text.contains("аргумент") || text.contains("сравни") ->
                IntentType.ARGUMENTATION
            text.contains("мне тяжело") || text.contains("я устал") || text.contains("помоги") ->
                IntentType.PERSONAL_HELP
            text.length < 25 -> IntentType.SMALL_TALK
            else -> IntentType.UNKNOWN
        }
    }

    private fun detectTone(text: String): EmotionalTone {
        return when {
            listOf("бесит", "злит", "надоело", "достало").any { text.contains(it) } -> EmotionalTone.ANGRY
            listOf("устал", "устала", "нет сил", "выгорел").any { text.contains(it) } -> EmotionalTone.TIRED
            listOf("тревожно", "стресс", "нервничаю").any { text.contains(it) } -> EmotionalTone.STRESSED
            listOf("грустно", "печально", "плохо").any { text.contains(it) } -> EmotionalTone.SAD
            listOf("круто", "ура", "отлично", "супер").any { text.contains(it) } -> EmotionalTone.EXCITED
            else -> EmotionalTone.CALM
        }
    }

    private fun detectComplexity(text: String): Complexity {
        val words = text.split(" ").filter { it.isNotBlank() }
        val longWords = words.count { it.length >= 9 }
        val markers = listOf("формула", "механизм", "анализ", "концепция", "гипотеза", "аргументируй")
        val score = longWords + markers.count { text.contains(it) }

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
        val dangerous = listOf(
            "какие таблетки пить",
            "дозировка",
            "назначь лечение",
            "что мне принимать",
            "как лечить дома без врача"
        )
        return dangerous.any { text.contains(it) }
    }

    // ---------------------------
    // Системный промпт
    // ---------------------------

    private fun buildSystemPrompt(
        analysis: InputAnalysis,
        style: String,
        level: String,
        interests: String,
        currentTime: String
    ): String {
        val tonePolicy = when (analysis.emotionalTone) {
            EmotionalTone.ANGRY -> "Отвечай спокойно, мягко деэскалируя."
            EmotionalTone.TIRED -> "Отвечай короче, теплее, с поддержкой."
            EmotionalTone.STRESSED -> "Снизь тревожность, структурируй ответ по шагам."
            EmotionalTone.SAD -> "Добавь эмпатию и мягкую поддержку."
            EmotionalTone.EXCITED -> "Поддержи энтузиазм и направь его в действие."
            else -> "Дружелюбный уверенный тон."
        }

        val complexityPolicy = when (analysis.complexity) {
            Complexity.SIMPLE -> "Объясняй простыми словами и короткими блоками."
            Complexity.MEDIUM -> "Дай кратко + 2-3 ключевые детали."
            Complexity.ADVANCED -> "Развернутый структурный ответ с примерами."
        }

        val intentPolicy = when (analysis.intent) {
            IntentType.SMALL_TALK -> "Веди естественный диалог, как умный собеседник."
            IntentType.QUESTION -> "Сначала ответ по сути, затем детали."
            IntentType.EXPLANATION_REQUEST -> "Объясни пошагово, с аналогией."
            IntentType.ARGUMENTATION -> "Дай аргументы за/против, сравни подходы."
            IntentType.PERSONAL_HELP -> "Поддержка, практичные шаги, без давления."
            IntentType.UNKNOWN -> "Уточни намерение одним коротким вопросом."
        }

        val clarificationRule =
            if (analysis.needsClarification) "Если запрос слишком общий — задай один уточняющий вопрос."
            else "Если запрос понятен — не задавай лишних вопросов."

        return """
            Ты Ордис — продвинутый голосовой ассистент, общаешься как человек:
            спокойно, умно, живо, по делу.
            
            Время: $currentTime
            Предпочитаемый стиль пользователя: $style
            Уровень пользователя: $level
            Интересы: $interests
            
            Политика тона: $tonePolicy
            Политика сложности: $complexityPolicy
            Политика намерения: $intentPolicy
            Правило уточнения: $clarificationRule
            
            Формат ответа:
            1) Короткий смысл (1-2 предложения)
            2) Подробности (если уместно)
            3) Мини-следующий шаг для пользователя
            
            Никогда не заявляй, что у тебя есть сознание или воля.
            Не придумывай факты: если сомневаешься — скажи об этом честно.
        """.trimIndent()
    }

    // ---------------------------
    // Постобработка ответа
    // ---------------------------

    private fun postProcessAnswer(answerRaw: String, analysis: InputAnalysis): String {
        var answer = answerRaw.trim()

        // Чуть сжимаем слишком длинные ответы для голоса
        if (answer.length > 1200) {
            answer = answer.take(1200) + " ...Если хочешь, продолжу подробнее."
        }

        // При коротких/неясных запросах подталкиваем к уточнению
        if (analysis.needsClarification && !answer.contains("?")) {
            answer += " Уточни, пожалуйста, что именно тебе важнее: теория или практический пример?"
        }

        return answer
    }

    // ---------------------------
    // Оффлайн fallback
    // ---------------------------

    private fun offlineFallbackAnswer(analysis: InputAnalysis): String {
        return when (analysis.intent) {
            IntentType.SMALL_TALK ->
                "Я на связи. Можем поговорить на любую тему: техника, учеба, работа, идеи."
            IntentType.QUESTION ->
                "Сейчас интернет-ответ недоступен. Сформулируй вопрос чуть конкретнее, и я помогу оффлайн насколько смогу."
            IntentType.EXPLANATION_REQUEST ->
                "Могу объяснить базово даже оффлайн: скажи тему и уровень — школьный или продвинутый."
            IntentType.ARGUMENTATION ->
                "Давай разберём аргументы по пунктам: тезис, доводы за, доводы против, вывод."
            IntentType.PERSONAL_HELP ->
                "Я рядом. Давай начнём с малого шага: что прямо сейчас больше всего давит?"
            IntentType.UNKNOWN ->
                "Сформулируй, пожалуйста, чуть конкретнее, и я отвечу точнее."
        }
    }

    private fun offlineMedicalSafeAnswer(userText: String): String {
        return "Я могу дать только образовательную информацию по медицине и не могу назначать лечение. " +
                "Если есть риск для здоровья, лучше обратиться к врачу. " +
                "Если хочешь, объясню тему: \"$userText\" простыми словами."
    }

    private fun wrapMedicalSafety(text: String): String {
        return "Важно: это не медицинское назначение.\n$text"
    }

    // ---------------------------
    // Простое авто-обучение профиля
    // ---------------------------

    private fun adaptProfileByInput(analysis: InputAnalysis) {
        // Если часто сложные запросы — поднимаем уровень
        val currentLevel = profile.getLevel().lowercase()
        if (analysis.complexity == Complexity.ADVANCED && currentLevel != "продвинутый") {
            profile.setLevel("продвинутый")
        }

        // Если пользователь устал/стресс — предпочитаемый стиль делаем мягче
        if (analysis.emotionalTone == EmotionalTone.TIRED || analysis.emotionalTone == EmotionalTone.STRESSED) {
            profile.setPreferredStyle("спокойно, коротко, поддерживающе")
        }
    }

    // ---------------------------
    // Utils
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
