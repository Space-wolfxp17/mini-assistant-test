package com.ordis.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    onSend: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val messages = ChatUiState.messages
    val input by remember { ChatUiState.inputText }
    val isThinking by remember { ChatUiState.isThinking }
    val networkState by remember { ChatUiState.networkState }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .navigationBarsPadding()
    ) {
        Header(
            networkState = networkState,
            isThinking = isThinking,
            onClear = { ChatUiState.clear() }
        )

        Divider()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(
                    role = msg.role,
                    text = msg.text,
                    ts = msg.ts
                )
            }

            if (isThinking) {
                item {
                    ThinkingBubble()
                }
            }
        }

        Divider()

        InputBar(
            text = input,
            onTextChange = { ChatUiState.inputText.value = it },
            onSend = {
                val t = ChatUiState.inputText.value.trim()
                if (t.isNotBlank()) {
                    ChatUiState.add("user", t)
                    ChatUiState.inputText.value = ""
                    onSend(t)
                }
            }
        )
    }
}

@Composable
private fun Header(
    networkState: String,
    isThinking: Boolean,
    onClear: () -> Unit
) {
    val stateText = when (networkState) {
        "online" -> "Онлайн"
        "offline" -> "Оффлайн"
        "error" -> "Ошибка сети"
        else -> "Готово"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Диалог с Ордис", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (isThinking) "Ордис думает..." else stateText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        TextButton(onClick = onClear) {
            Icon(Icons.Default.Clear, contentDescription = null)
            Text("Очистить", modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun MessageBubble(
    role: String,
    text: String,
    ts: Long
) {
    val isUser = role == "user"
    val bg = when (role) {
        "assistant" -> MaterialTheme.colorScheme.surfaceVariant
        "system" -> Color(0xFFE8F5E9)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bg),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = if (isUser) "Вы" else if (role == "assistant") "Ордис" else "Система",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = formatTime(ts),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(" Думаю...", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Напиши сообщение...") },
            maxLines = 4
        )
        IconButton(onClick = onSend) {
            Icon(Icons.Default.Send, contentDescription = "Отправить")
        }
    }
}

private fun formatTime(ts: Long): String {
    return SimpleDateFormat("HH:mm", Locale("ru", "RU")).format(Date(ts))
}
