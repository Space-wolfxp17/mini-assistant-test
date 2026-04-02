package com.ordis.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ordis.app.data.repo.SettingsRepository
import com.ordis.app.ui.chat.ChatScreen
import com.ordis.app.ui.screens.HomeScreen
import com.ordis.app.ui.screens.SettingsScreen
import com.ordis.app.core.AppActions

object Routes {
    const val HOME = "home"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            // ВАЖНО:
            // Используем максимально "безопасный" вызов HomeScreen:
            // если у тебя другая сигнатура HomeScreen — замени только этот блок.
            HomeScreen(
                onOpenChat = { navController.navigate(Routes.CHAT) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.CHAT) {
            ChatScreen(
                onSend = { text ->
                    AppActions.onChatSend?.invoke(text)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                settingsRepository = settingsRepository
            )
        }
    }
}
