package com.ordis.app.data.model

data class UserSettings(
    val geminiApiKey: String = "",
    val backgroundModeEnabled: Boolean = true,
    val allowMicrophone: Boolean = true,
    val allowCamera: Boolean = false,
    val allowContacts: Boolean = false,
    val allowGallery: Boolean = false,
    val voiceGender: VoiceGender = VoiceGender.FEMALE,
    val onlineMode: Boolean = true,
    val offlineMode: Boolean = true,
    val requireConfirmButton: Boolean = true
)

enum class VoiceGender {
    MALE, FEMALE
}
