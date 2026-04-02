package com.ordis.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.ordis.app.chat.ConversationManager
import com.ordis.app.chat.ConversationMemoryRepository
import com.ordis.app.chat.GeminiChatService
import com.ordis.app.chat.UserProfileRepository
import com.ordis.app.core.AppActions
import com.ordis.app.data.repo.SettingsRepository
import com.ordis.app.ui.MainUiState
import com.ordis.app.ui.theme.OrdisTheme
import com.ordis.app.voice.CommandProcessor
import com.ordis.app.voice.VoiceManager
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var voiceManager: VoiceManager
    private lateinit var commandProcessor: CommandProcessor

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var memoryRepository: ConversationMemoryRepository
    private lateinit var profileRepository: UserProfileRepository
    private lateinit var conversationManager: ConversationManager

    private var micGranted = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            micGranted = result[Manifest.permission.RECORD_AUDIO] == true
            MainUiState.setVoiceState(
                if (micGranted) "Микрофон готов" else "Нет доступа к микрофону"
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1) TTS
        tts = TextToSpeech(this, this)

        // 2) Репозитории
        settingsRepository = SettingsRepository()
        memoryRepository = ConversationMemoryRepository(this)
        profileRepository = UserProfileRepository(this)

        // 3) Онлайн чат-сервис (Gemini)
        val chatService = GeminiChatService {
            settingsRepository.settings.value.geminiApiKey
        }

        // 4) Conversation manager (единый диалог)
        conversationManager = ConversationManager(
            memory = memoryRepository,
            profile = profileRepository,
            chatService = chatService,
            speak = { speak(it) }
        )

        // 5) Processor: actions + разговор
        commandProcessor = CommandProcessor(
            context = this,
            speak = { speak(it) },
            conversationManager = conversationManager
        )

        // 6) Voice manager
        voiceManager = VoiceManager(
            context = this,
            onText = { text ->
                // Обрабатываем только если есть обращение
                if (text.lowercase().contains("ордис")) {
                    MainUiState.setVoiceState("Поняла: $text")
                    commandProcessor.process(text)
                }
            },
            onState = { MainUiState.setVoiceState(it) }
        )
        voiceManager.init()

        // 7) Кнопка Start/Stop из UI
        AppActions.onToggleListening = {
            if (!micGranted) {
                askPermissions()
                return@onToggleListening
            }

            val listeningNow = MainUiState.isListening.value
            if (listeningNow) {
                voiceManager.stopLoop()
                MainUiState.setListening(false)
                MainUiState.setVoiceState("Прослушивание остановлено")
            } else {
                voiceManager.startLoop()
                MainUiState.setListening(true)
                MainUiState.setVoiceState("Слушаю...")
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

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ordis_tts")
    }

    private fun askPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    override fun onDestroy() {
        AppActions.onToggleListening = null
        voiceManager.release()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
