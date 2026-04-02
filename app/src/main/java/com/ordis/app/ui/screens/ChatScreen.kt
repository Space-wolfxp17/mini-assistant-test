package com.ordis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ordis.app.data.repo.ConsoleRepository
import com.ordis.app.data.repo.SettingsRepository

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    settingsRepository: SettingsRepository,
    consoleRepository: ConsoleRepository
) {
    val settings by settingsRepository.settings.collectAsState()
    val messages = remember { mutableStateListOf<String>() }
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("Назад") }
            Text(
                text = "Чат Ордис",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = " ")
        }

        Text(
            text = if (settings.geminiApiKey.isBlank())
                "API ключ не указан. Сейчас работает локальная заглушка."
            else
                "API ключ подключен. Можно подключать Gemini в следующем шаге.",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Введите сообщение") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (input.isBlank()) return@Button
                    val userMsg = "Вы: $input"
                    val botMsg = "Ордис: Принял команду \"$input\". (MVP-ответ)"
                    messages += userMsg
                    messages += botMsg
                    consoleRepository.info("CHAT", "Запрос: $input")
                    input = ""
                },
                modifier = Modifier.weight(1f)
            ) { Text("Отправить") }

            Button(
                onClick = {
                    messages.clear()
                    consoleRepository.warn("CHAT", "История чата очищена")
                },
                modifier = Modifier.weight(1f)
            ) { Text("Очистить") }
        }
    }
}
