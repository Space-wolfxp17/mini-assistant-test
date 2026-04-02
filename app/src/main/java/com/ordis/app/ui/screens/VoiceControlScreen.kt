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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ordis.app.data.repo.ConsoleRepository

@Composable
fun VoiceControlScreen(
    onBack: () -> Unit,
    consoleRepository: ConsoleRepository
) {
    var command by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ожидание команды") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("Назад") }
            Text(
                "Голосовое управление",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(" ")
        }

        Text(
            text = "MVP: кнопки-эмуляторы речи. Реальные STT/TTS подключим далее.",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            label = { Text("Команда (текстом или распознанная речь)") },
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
                    status = "Слушаю... (заглушка)"
                    consoleRepository.info("VOICE", "Запуск прослушивания")
                },
                modifier = Modifier.weight(1f)
            ) { Text("Слушать") }

            Button(
                onClick = {
                    if (command.isBlank()) return@Button
                    status = "Выполнено: $command"
                    consoleRepository.info("VOICE", "Команда: $command")
                },
                modifier = Modifier.weight(1f)
            ) { Text("Выполнить") }
        }

        Text(
            text = "Статус: $status",
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}
