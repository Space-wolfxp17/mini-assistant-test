package com.ordis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ordis.app.data.model.VoiceGender
import com.ordis.app.data.repo.ConsoleRepository
import com.ordis.app.data.repo.SettingsRepository

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsRepository: SettingsRepository,
    consoleRepository: ConsoleRepository
) {
    val settings by settingsRepository.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("Назад") }
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(" ")
        }

        OutlinedTextField(
            value = settings.geminiApiKey,
            onValueChange = { settingsRepository.updateGeminiKey(it) },
            label = { Text("Gemini API Key") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        SettingSwitch("Фоновый режим", settings.backgroundModeEnabled) {
            settingsRepository.setBackgroundMode(it)
        }
        SettingSwitch("Доступ к микрофону", settings.allowMicrophone) {
            settingsRepository.setMicAccess(it)
        }
        SettingSwitch("Доступ к камере", settings.allowCamera) {
            settingsRepository.setCameraAccess(it)
        }
        SettingSwitch("Доступ к контактам", settings.allowContacts) {
            settingsRepository.setContactsAccess(it)
        }
        SettingSwitch("Доступ к галерее", settings.allowGallery) {
            settingsRepository.setGalleryAccess(it)
        }
        SettingSwitch("Онлайн режим", settings.onlineMode) {
            settingsRepository.setOnlineMode(it)
        }
        SettingSwitch("Оффлайн режим", settings.offlineMode) {
            settingsRepository.setOfflineMode(it)
        }
        SettingSwitch("Подтверждение кнопкой", settings.requireConfirmButton) {
            settingsRepository.setRequireConfirmButton(it)
        }

        Text(
            text = "Голос",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { settingsRepository.setVoiceGender(VoiceGender.FEMALE) },
                modifier = Modifier.weight(1f)
            ) { Text(if (settings.voiceGender == VoiceGender.FEMALE) "Женский ✓" else "Женский") }

            Button(
                onClick = { settingsRepository.setVoiceGender(VoiceGender.MALE) },
                modifier = Modifier.weight(1f)
            ) { Text(if (settings.voiceGender == VoiceGender.MALE) "Мужской ✓" else "Мужской") }
        }

        Button(
            onClick = {
                consoleRepository.info("SETTINGS", "Настройки сохранены (в памяти MVP)")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
        ) {
            Text("Сохранить")
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
