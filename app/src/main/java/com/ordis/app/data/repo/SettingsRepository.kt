package com.ordis.app.data.repo

import com.ordis.app.data.model.UserSettings
import com.ordis.app.data.model.VoiceGender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SettingsRepository {

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings

    fun updateGeminiKey(key: String) {
        _settings.update { it.copy(geminiApiKey = key.trim()) }
    }

    fun setBackgroundMode(enabled: Boolean) {
        _settings.update { it.copy(backgroundModeEnabled = enabled) }
    }

    fun setMicAccess(enabled: Boolean) {
        _settings.update { it.copy(allowMicrophone = enabled) }
    }

    fun setCameraAccess(enabled: Boolean) {
        _settings.update { it.copy(allowCamera = enabled) }
    }

    fun setContactsAccess(enabled: Boolean) {
        _settings.update { it.copy(allowContacts = enabled) }
    }

    fun setGalleryAccess(enabled: Boolean) {
        _settings.update { it.copy(allowGallery = enabled) }
    }

    fun setVoiceGender(gender: VoiceGender) {
        _settings.update { it.copy(voiceGender = gender) }
    }

    fun setOnlineMode(enabled: Boolean) {
        _settings.update { it.copy(onlineMode = enabled) }
    }

    fun setOfflineMode(enabled: Boolean) {
        _settings.update { it.copy(offlineMode = enabled) }
    }

    fun setRequireConfirmButton(enabled: Boolean) {
        _settings.update { it.copy(requireConfirmButton = enabled) }
    }
}
