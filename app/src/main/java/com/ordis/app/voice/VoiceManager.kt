package com.ordis.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceManager(
    private val context: Context,
    private val onText: (String) -> Unit,
    private val onState: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var keepListening = false
    @Volatile private var isListening = false

    private var lastStartAt = 0L
    private val restartDelay = 1000L

    fun init() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onState("Распознавание недоступно")
            return
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                onState("Слушаю...")
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                onState("Обрабатываю...")
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (text.isNotBlank()) onText(text)
                scheduleRestart()
            }

            override fun onError(error: Int) {
                isListening = false
                onState("Ошибка распознавания: $error")
                scheduleRestart()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startLoop() {
        keepListening = true
        safeStart()
    }

    fun stopLoop() {
        keepListening = false
        try { recognizer?.stopListening() } catch (_: Exception) {}
        isListening = false
        onState("Остановлено")
    }

    private fun scheduleRestart() {
        if (!keepListening) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ safeStart() }, restartDelay)
    }

    private fun safeStart() {
        if (!keepListening || recognizer == null || isListening) return
        val now = System.currentTimeMillis()
        if (now - lastStartAt < restartDelay) return
        lastStartAt = now

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            scheduleRestart()
        }
    }

    fun release() {
        stopLoop()
        recognizer?.destroy()
        recognizer = null
    }
}
