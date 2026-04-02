package com.ordis.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsManager(
    context: Context,
    private val onInitResult: (Boolean) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale("ru", "RU")
                tts?.setSpeechRate(1.0f)
                tts?.setPitch(1.0f)
            }
            onInitResult(ready)
        }
    }

    fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ordis_tts")
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
