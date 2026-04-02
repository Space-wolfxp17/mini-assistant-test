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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ordis.app.data.model.LogLevel
import com.ordis.app.data.repo.ConsoleRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConsoleScreen(
    onBack: () -> Unit,
    consoleRepository: ConsoleRepository
) {
    val logs by consoleRepository.logs.collectAsState()
    var patchText by remember { mutableStateOf("") }

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
                text = "Консоль",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(" ")
        }

        Text(
            text = "Логи ошибок и окно для патча/кода (MVP: валидация-заглушка).",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = patchText,
            onValueChange = { patchText = it },
            label = { Text("Вставьте код/патч") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            minLines = 5
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (patchText.isBlank()) {
                        consoleRepository.warn("PATCH", "Пустой патч")
                    } else {
                        val ok = patchText.contains("function", ignoreCase = true) ||
                                patchText.contains("{") ||
                                patchText.contains("class")
                        if (ok) {
                            consoleRepository.info("PATCH", "Патч прошел базовую проверку и применен (MVP)")
                        } else {
                            consoleRepository.error("PATCH", "Ошибка в патче: не распознан формат кода")
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Исправить")
            }

            Button(
                onClick = {
                    consoleRepository.clear()
                    consoleRepository.info("CONSOLE", "Логи очищены")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Очистить логи")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(logs.reversed()) { log ->
                val color = when (log.level) {
                    LogLevel.INFO -> MaterialTheme.colorScheme.primary
                    LogLevel.WARN -> MaterialTheme.colorScheme.secondary
                    LogLevel.ERROR -> MaterialTheme.colorScheme.error
                }
                Text(
                    text = "[${formatTime(log.timestamp)}] ${log.level} / ${log.tag}: ${log.message}",
                    color = color,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun formatTime(ts: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(ts))
}
