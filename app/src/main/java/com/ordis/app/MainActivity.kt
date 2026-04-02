package com.ordis.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.ordis.app.ui.theme.OrdisTheme
import com.ordis.app.voice.CommandProcessor
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var commandProcessor: CommandProcessor

    private val handler = Handler(Looper.getMainLooper())

    // Состояния слушания
    @Volatile
    private var isListening = false

    @Volatile
    private var keepListening = false

    private var lastStartAt = 0L
    private val minRestartDelayMs = 1200L // анти-дребезг

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val micGranted = result[Manifest.permission.RECORD_AUDIO] == true
            if (micGranted) {
                startVoiceLoop()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tts = TextToSpeech(this, this)
        commandProcessor = CommandProcessor(this) { speak(it) }
        setupSpeechRecognizer()
        askInitialPermissions()

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

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onResults(results: Bundle?) {
                isListening = false

                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                if (text.isNotBlank()) {
                    // Реагируем только на обращения с "ордис"
                    if (text.lowercase().contains("ордис")) {
                        commandProcessor.process(text)
                    }
                }

                scheduleRestart()
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onError(error: Int) {
                isListening = false
                // Не перезапускаем мгновенно, чтобы не мигал микрофон
                scheduleRestart()
            }
        })
    }

    private fun startVoiceLoop() {
        keepListening = true
        safeStartListening()
    }

    private fun stopVoiceLoop() {
        keepListening = false
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        isListening = false
    }

    private fun scheduleRestart() {
        if (!keepListening) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ safeStartListening() }, minRestartDelayMs)
    }

    private fun safeStartListening() {
        if (!keepListening) return
        if (speechRecognizer == null) return
        if (isListening) return

        val now = System.currentTimeMillis()
        if (now - lastStartAt < minRestartDelayMs) return
        lastStartAt = now

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (_: Exception) {
            isListening = false
            scheduleRestart()
        }
    }

    private fun askInitialPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    override fun onPause() {
        super.onPause()
        // Чтобы в фоне не дёргалось
        stopVoiceLoop()
    }

    override fun onResume() {
        super.onResume()
        // Возвращаем прослушивание только в активном экране
        startVoiceLoop()
    }

    override fun onDestroy() {
        stopVoiceLoop()
        speechRecognizer?.destroy()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
