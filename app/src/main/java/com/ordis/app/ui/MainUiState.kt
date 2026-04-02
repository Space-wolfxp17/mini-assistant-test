package com.ordis.app.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object MainUiState {
    private val _voiceState = MutableStateFlow("Готово")
    val voiceState: StateFlow<String> = _voiceState

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    fun setVoiceState(text: String) { _voiceState.value = text }
    fun setListening(value: Boolean) { _isListening.value = value }
}
