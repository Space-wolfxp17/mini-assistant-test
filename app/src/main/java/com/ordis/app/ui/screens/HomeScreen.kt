package com.ordis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ordis.app.core.AppActions
import com.ordis.app.ui.MainUiState

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenConsole: () -> Unit,
    onOpenVersion: () -> Unit
) {
    val voiceState by MainUiState.voiceState.collectAsState()
    val isListening by MainUiState.isListening.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "ОРДИС AI-ассистент",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Статус: $voiceState",
            modifier = Modifier.padding(top = 8.dp)
        )

        Button(
            onClick = { AppActions.onToggleListening?.invoke() },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(top = 12.dp)
        ) {
            Text(if (isListening) "Стоп" else "Слушать")
        }

        Button(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(top = 20.dp)
        ) { Text("Настройки") }

        Button(
            onClick = onOpenConsole,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(top = 10.dp)
        ) { Text("Консоль") }

        TextButton(
            onClick = onOpenVersion,
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("Версия") }
    }
}
