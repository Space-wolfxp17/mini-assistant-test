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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.ordis.app.data.repo.ConsoleRepository
import com.ordis.app.data.repo.VersionRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VersionsScreen(
    onBack: () -> Unit,
    versionRepository: VersionRepository,
    consoleRepository: ConsoleRepository
) {
    val versions by versionRepository.versions.collectAsState()
    var versionName by remember { mutableStateOf("") }
    var versionDesc by remember { mutableStateOf("") }

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
                text = "Версии",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(" ")
        }

        Text(
            text = "Откат к любой версии навыков/логики и возврат к последней.",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = versionName,
            onValueChange = { versionName = it },
            label = { Text("Имя версии (например v0.1.1)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = versionDesc,
            onValueChange = { versionDesc = it },
            label = { Text("Описание изменений") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (versionName.isBlank()) return@Button
                    versionRepository.addVersion(versionName, versionDesc.ifBlank { "Без описания" })
                    consoleRepository.info("VERSION", "Создана версия: $versionName")
                    versionName = ""
                    versionDesc = ""
                },
                modifier = Modifier.weight(1f)
            ) { Text("Сохранить версию") }

            Button(
                onClick = {
                    val ok = versionRepository.switchToLatest()
                    if (ok) consoleRepository.warn("VERSION", "Переключено на последнюю версию")
                    else consoleRepository.error("VERSION", "Не удалось переключить на последнюю")
                },
                modifier = Modifier.weight(1f)
            ) { Text("На новую") }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(versions.reversed()) { version ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${version.name} ${if (version.isCurrent) "(ТЕКУЩАЯ)" else ""}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = formatDate(version.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = version.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val ok = versionRepository.rollbackTo(version.id)
                                    if (ok) {
                                        consoleRepository.warn("VERSION", "Откат к ${version.name}")
                                    } else {
                                        consoleRepository.error("VERSION", "Ошибка отката к ${version.name}")
                                    }
                                },
                                enabled = !version.isCurrent
                            ) {
                                Text("Откатиться")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(ts: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(ts))
}
