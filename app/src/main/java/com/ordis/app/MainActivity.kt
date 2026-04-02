package com.ordis.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.ordis.app.core.AppActions
import com.ordis.app.data.repo.SettingsRepository
import com.ordis.app.ui.MainUiState
import com.ordis.app.ui.theme.OrdisTheme
import com.ordis.app.voice.CommandProcessor
import com.ordis.app.voice.VoiceManager
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var voiceManager: VoiceManager

    private var micGranted = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            micGranted = result[Manifest.permission.RECORD_AUDIO] == true
            if (micGranted) {
                MainUiState.setVoiceState("Микрофон готов")
            } else {
                MainUiState.setVoiceState("Нет доступа к микрофону")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tts = TextToSpeech(this, this)

        // Один инстанс репозитория для Activity
        settingsRepository = SettingsRepository()

        // ВАЖНО: новый конструктор с settingsRepository
        commandProcessor = CommandProcessor(
            context = this,
            settingsRepository = settingsRepository,
            speak = { speak(it) }
        )

        voiceManager = VoiceManager(
            context = this,
            onText = { text ->
                // активация по имени
                if (text.lowercase().contains("ордис")) {
                    commandProcessor.process(text)
                }
            },
            onState = { MainUiState.setVoiceState(it) }
        )
        voiceManager.init()

        AppActions.onToggleListening = {
            if (!micGranted) {
                askPermissions()
                return@onToggleListening
            }
            val listening = MainUiState.isListening.value
            if (listening) {
                voiceManager.stopLoop()
                MainUiState.setListening(false)
                MainUiState.setVoiceState("Остановлено")
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
            speak("Привет, я Ордис. Готова к командам.")
        }
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

    override fun onDestroy() {
        AppActions.onToggleListening = null
        voiceManager.release()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
