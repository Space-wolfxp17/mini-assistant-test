package com.ordis.app

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.ordis.app.chat.ConversationManager
import com.ordis.app.chat.ConversationMemoryRepository
import com.ordis.app.chat.GeminiChatService
import com.ordis.app.chat.UserProfileRepository
import com.ordis.app.core.AppActions
import com.ordis.app.data.repo.ConsoleRepository
import com.ordis.app.data.repo.SettingsRepository
import com.ordis.app.ui.MainUiState
import com.ordis.app.ui.chat.ChatUiState
import com.ordis.app.ui.theme.OrdisTheme
import com.ordis.app.voice.CommandProcessor
import com.ordis.app.voice.VoiceManager
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var voiceManager: VoiceManager
    private lateinit var commandProcessor: CommandProcessor

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var consoleRepository: ConsoleRepository
    private lateinit var memoryRepository: ConversationMemoryRepository
    private lateinit var profileRepository: UserProfileRepository
    private lateinit var conversationManager: ConversationManager

    private var micGranted = false
    private var autoListen = false // можно переключить при желании

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            micGranted = result[Manifest.permission.RECORD_AUDIO] == true
            val camGranted = result[Manifest.permission.CAMERA] == true

            MainUiState.setVoiceState(
                if (micGranted) "Микрофон готов" else "Нет доступа к микрофону"
            )

            consoleRepository.info("PERMISSIONS", "mic=$micGranted camera=$camGranted")

            if (micGranted && autoListen) {
                startListening()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Репозитории
        settingsRepository = SettingsRepository()
        consoleRepository = ConsoleRepository()
        memoryRepository = ConversationMemoryRepository(this)
        profileRepository = UserProfileRepository(this)

        // TTS
        tts = TextToSpeech(this, this)

        // Chat service
        val chatService = GeminiChatService {
            settingsRepository.settings.value.geminiApiKey
        }

        // Conversation manager
        conversationManager = ConversationManager(
            memory = memoryRepository,
            profileRepo = profileRepository,
            chatService = chatService,
            speak = { speak(it) },
            onDebug = { dbg -> consoleRepository.info("CONV_DEBUG", dbg) }
        )

        // Command processor
        commandProcessor = CommandProcessor(
            context = this,
            speak = { speak(it) },
            conversationManager = conversationManager
        )

        // Voice manager
        voiceManager = VoiceManager(
            context = this,
            onText = { text ->
                consoleRepository.info("ASR_TEXT", text)
                if (text.lowercase().contains("ордис")) {
                    MainUiState.setVoiceState("Поняла: $text")
                    commandProcessor.process(text)
                }
            },
            onState = { state ->
                MainUiState.setVoiceState(state)
                consoleRepository.info("VOICE_STATE", state)
            }
        )
        voiceManager.init()

        // Глобальные действия для UI
        AppActions.onToggleListening = toggle@{
            if (!micGranted) {
             askPermissions()
             return@toggle
            }
            if (MainUiState.isListening.value) stopListening() else startListening()
        }

        AppActions.onExportMemory = {
            exportMemoryToDownloads(this)
        }

        AppActions.onClearMemory = {
            memoryRepository.clearHistory()
            ChatUiState.clear()
            Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show()
            consoleRepository.info("MEMORY", "history/chat cleared")
        }

        // Хук для текстового ChatScreen
        AppActions.onChatSend = chat@{ userText ->
            val clean = userText.trim()
            if (clean.isBlank()) return@chat

            ChatUiState.isThinking.value = true
            ChatUiState.networkState.value = "ready"

            // Сообщение пользователя обычно уже добавляет ChatScreen
            // Но если вызвано не из ChatScreen — подстрахуемся:
            if (ChatUiState.messages.lastOrNull()?.text != clean ||
                ChatUiState.messages.lastOrNull()?.role != "user"
            ) {
                ChatUiState.add("user", clean)
            }

            conversationManager.reply(clean) { answer ->
                runOnUiThread {
                    ChatUiState.isThinking.value = false
                    ChatUiState.add("assistant", answer)
                    ChatUiState.networkState.value =
                        if (answer.contains("оффлайн", ignoreCase = true)) "offline" else "online"
                }
            }
        }

        askPermissions()

        setContent {
            OrdisTheme {
                OrdisApp()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("ru", "RU")
            speak("Привет, я Ордис. Готова общаться.")
        }
    }

    private fun startListening() {
        voiceManager.startLoop()
        MainUiState.setListening(true)
        MainUiState.setVoiceState("Слушаю...")
        consoleRepository.info("VOICE", "startListening")
    }

    private fun stopListening() {
        voiceManager.stopLoop()
        MainUiState.setListening(false)
        MainUiState.setVoiceState("Остановлено")
        consoleRepository.info("VOICE", "stopListening")
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ordis_tts")
    }

    private fun askPermissions() {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(list.toTypedArray())
    }

    private fun exportMemoryToDownloads(context: Context) {
        try {
            val history = memoryRepository.getHistory(300)
            val facts = memoryRepository.getFacts()
            val topics = memoryRepository.topTopics(30)
            val profile = profileRepository.getProfile()

            val json = buildString {
                append("{\n")
                append("  \"profile\": {\n")
                append("    \"preferredStyle\": \"${escape(profile.preferredStyle)}\",\n")
                append("    \"level\": \"${escape(profile.level)}\",\n")
                append("    \"interests\": \"${escape(profile.interests)}\",\n")
                append("    \"responseLength\": \"${escape(profile.responseLength)}\",\n")
                append("    \"responseFormat\": \"${escape(profile.responseFormat)}\",\n")
                append("    \"emotionalTonePreference\": \"${escape(profile.emotionalTonePreference)}\",\n")
                append("    \"wantsExamples\": ${profile.wantsExamples},\n")
                append("    \"wantsStepByStep\": ${profile.wantsStepByStep}\n")
                append("  },\n")

                append("  \"facts\": [\n")
                facts.forEachIndexed { i, f ->
                    append("    {\"key\":\"${escape(f.key)}\",\"value\":\"${escape(f.value)}\",\"confidence\":${f.confidence},\"updatedAt\":${f.updatedAt}}")
                    if (i < facts.lastIndex) append(",")
                    append("\n")
                }
                append("  ],\n")

                append("  \"topics\": [\n")
                topics.forEachIndexed { i, t ->
                    append("    {\"topic\":\"${escape(t.first)}\",\"count\":${t.second}}")
                    if (i < topics.lastIndex) append(",")
                    append("\n")
                }
                append("  ],\n")

                append("  \"history\": [\n")
                history.forEachIndexed { i, h ->
                    append("    {\"role\":\"${escape(h.role)}\",\"text\":\"${escape(h.text)}\",\"ts\":${h.ts}}")
                    if (i < history.lastIndex) append(",")
                    append("\n")
                }
                append("  ]\n")
                append("}\n")
            }

            val fileName = "ordis_memory_${System.currentTimeMillis()}.json"
            context.openFileOutput(fileName, MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }

            Toast.makeText(context, "Экспортировано: /data/data/.../$fileName", Toast.LENGTH_LONG).show()
            consoleRepository.info("MEMORY_EXPORT", "ok: $fileName")
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
            consoleRepository.error("MEMORY_EXPORT", "fail: ${e.message}")
        }
    }

    private fun escape(s: String): String {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }

    override fun onDestroy() {
        AppActions.onToggleListening = null
        AppActions.onExportMemory = null
        AppActions.onClearMemory = null
        AppActions.onChatSend = null
        voiceManager.release()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
