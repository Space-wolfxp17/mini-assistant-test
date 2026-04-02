package com.ordis.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ordis.app.chat.ConversationMemoryRepository
import com.ordis.app.chat.GeminiChatService
import com.ordis.app.chat.UserProfileRepository
import com.ordis.app.data.repo.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun updateGeminiApiKey(newKey: String) {
    val cur = settings.value
    settings.value = cur.copy(geminiApiKey = newKey)
}
fun SettingsScreen(
    settingsRepository: SettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val memoryRepo = remember { ConversationMemoryRepository(context) }
    val profileRepo = remember { UserProfileRepository(context) }

    // ---------- Settings repository state ----------
    val currentSettings = settingsRepository.settings.value
    var geminiApiKey by remember { mutableStateOf(currentSettings.geminiApiKey) }

    // ---------- Profile state ----------
    var style by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf("") }
    var responseLength by remember { mutableStateOf("") }
    var responseFormat by remember { mutableStateOf("") }
    var emotionalTone by remember { mutableStateOf("") }
    var wantsExamples by remember { mutableStateOf(true) }
    var wantsSteps by remember { mutableStateOf(true) }

    // ---------- Status ----------
    var statusText by remember { mutableStateOf("Готово") }

    LaunchedEffect(Unit) {
        val p = profileRepo.getProfile()
        style = p.preferredStyle
        level = p.level
        interests = p.interests
        responseLength = p.responseLength
        responseFormat = p.responseFormat
        emotionalTone = p.emotionalTonePreference
        wantsExamples = p.wantsExamples
        wantsSteps = p.wantsStepByStep
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Настройки Ordis", style = MaterialTheme.typography.headlineSmall)

        // ---------------- API key block ----------------
        Card(colors = CardDefaults.cardColors()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Gemini API", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = geminiApiKey,
                    onValueChange = { geminiApiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        settingsRepository.updateGeminiApiKey(geminiApiKey.trim())
                        statusText = "API ключ сохранён ✅"
                    }) {
                        Text("Сохранить ключ")
                    }

                    OutlinedButton(onClick = {
                        scope.launch {
                            statusText = "Проверка ключа..."
                            val ok = testGeminiKey(geminiApiKey.trim())
                            statusText = if (ok) {
                                "Ключ рабочий ✅"
                            } else {
                                "Ключ невалидный или сеть недоступна ❌"
                            }
                        }
                    }) {
                        Text("Проверить")
                    }
                }
            }
        }

        // ---------------- Personality block ----------------
        Card {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Личность Ордис", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = style,
                    onValueChange = { style = it },
                    label = { Text("Стиль общения") },
                    placeholder = { Text("дружелюбно, ясно, по делу") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it },
                    label = { Text("Уровень объяснения") },
                    placeholder = { Text("базовый / средний / продвинутый") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = interests,
                    onValueChange = { interests = it },
                    label = { Text("Интересы (через запятую)") },
                    placeholder = { Text("технологии, физика, литература") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = responseLength,
                    onValueChange = { responseLength = it },
                    label = { Text("Длина ответа") },
                    placeholder = { Text("короткая / средняя / подробная") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = responseFormat,
                    onValueChange = { responseFormat = it },
                    label = { Text("Формат ответа") },
                    placeholder = { Text("кратко+подробно+шаг") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = emotionalTone,
                    onValueChange = { emotionalTone = it },
                    label = { Text("Тон ассистента") },
                    placeholder = { Text("спокойный") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Добавлять примеры")
                    Switch(checked = wantsExamples, onCheckedChange = { wantsExamples = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Объяснять по шагам")
                    Switch(checked = wantsSteps, onCheckedChange = { wantsSteps = it })
                }

                Button(onClick = {
                    profileRepo.saveProfile(
                        UserProfileRepository.UserProfile(
                            preferredStyle = style.ifBlank { "дружелюбно, ясно, по делу" },
                            level = level.ifBlank { "средний" },
                            interests = interests.ifBlank { "технологии, саморазвитие" },
                            responseLength = responseLength.ifBlank { "средняя" },
                            responseFormat = responseFormat.ifBlank { "кратко+подробно+шаг" },
                            emotionalTonePreference = emotionalTone.ifBlank { "спокойный" },
                            wantsExamples = wantsExamples,
                            wantsStepByStep = wantsSteps
                        )
                    )
                    statusText = "Профиль Ордис сохранён ✅"
                }) {
                    Text("Сохранить личность")
                }
            }
        }

        // ---------------- Memory block ----------------
        Card {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Память и данные", style = MaterialTheme.typography.titleMedium)

                val topTopics = remember { mutableStateOf(memoryRepo.topTopics(5)) }
                LaunchedEffect(Unit) { topTopics.value = memoryRepo.topTopics(5) }

                Text(
                    "Топ тем: " + if (topTopics.value.isEmpty()) {
                        "пока пусто"
                    } else {
                        topTopics.value.joinToString { "${it.first}:${it.second}" }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        memoryRepo.clearHistory()
                        statusText = "История диалога очищена"
                    }) { Text("Очистить историю") }

                    OutlinedButton(onClick = {
                        scope.launch {
                            val exported = exportAllMemoryToJson(memoryRepo, profileRepo)
                            copyToClipboard(context, "ordis_memory_export", exported)
                            statusText = "Экспорт JSON скопирован в буфер"
                        }
                    }) { Text("Экспорт в JSON") }
                }

                TextButton(onClick = {
                    scope.launch {
                        val example = exportAllMemoryToJson(memoryRepo, profileRepo)
                        statusText = "Пример экспорта: ${example.take(120)}..."
                    }
                }) {
                    Text("Показать превью экспорта")
                }
            }
        }

        Divider()

        Text(
            text = "Статус: $statusText",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ---------------- helpers ----------------

private suspend fun testGeminiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
    if (apiKey.isBlank()) return@withContext false

    return@withContext try {
        val service = GeminiChatService { apiKey }
        val ans = service.askWithCustomSystem(
            systemPrompt = "Ты проверка API. Ответь одним словом: OK",
            history = emptyList(),
            userInput = "test"
        )
        !ans.isNullOrBlank()
    } catch (_: Exception) {
        false
    }
}

private suspend fun exportAllMemoryToJson(
    memoryRepo: ConversationMemoryRepository,
    profileRepo: UserProfileRepository
): String = withContext(Dispatchers.Default) {
    val root = JSONObject()

    // profile
    val p = profileRepo.getProfile()
    val profileJson = JSONObject()
        .put("preferredStyle", p.preferredStyle)
        .put("level", p.level)
        .put("interests", p.interests)
        .put("responseLength", p.responseLength)
        .put("responseFormat", p.responseFormat)
        .put("emotionalTonePreference", p.emotionalTonePreference)
        .put("wantsExamples", p.wantsExamples)
        .put("wantsStepByStep", p.wantsStepByStep)

    // history
    val historyArr = JSONArray()
    memoryRepo.getHistory(500).forEach {
        historyArr.put(
            JSONObject()
                .put("role", it.role)
                .put("text", it.text)
                .put("ts", it.ts)
        )
    }

    // facts
    val factsArr = JSONArray()
    memoryRepo.getFacts().forEach {
        factsArr.put(
            JSONObject()
                .put("key", it.key)
                .put("value", it.value)
                .put("confidence", it.confidence)
                .put("updatedAt", it.updatedAt)
        )
    }

    // topics
    val topicsObj = JSONObject()
    memoryRepo.getTopicStats().forEach { (k, v) -> topicsObj.put(k, v) }

    root.put("profile", profileJson)
    root.put("history", historyArr)
    root.put("facts", factsArr)
    root.put("topics", topicsObj)
    root.put("exportedAt", System.currentTimeMillis())

    root.toString(2)
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}
