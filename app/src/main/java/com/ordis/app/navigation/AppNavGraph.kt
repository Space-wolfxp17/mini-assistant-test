package com.ordis.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ordis.app.data.repo.ConsoleRepository
import com.ordis.app.data.repo.SettingsRepository
import com.ordis.app.data.repo.VersionRepository
import com.ordis.app.ui.screens.ChatScreen
import com.ordis.app.ui.screens.ConsoleScreen
import com.ordis.app.ui.screens.HomeScreen
import com.ordis.app.ui.screens.SettingsScreen
import com.ordis.app.ui.screens.VersionsScreen
import com.ordis.app.ui.screens.VoiceControlScreen

@Composable
fun AppNavGraph(
    settingsRepository: SettingsRepository,
    versionRepository: VersionRepository,
    consoleRepository: ConsoleRepository,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route
    ) {
        composable(Routes.Home.route) {
            HomeScreen(
                onOpenChat = { navController.navigate(Routes.Chat.route) },
                onOpenVoice = { navController.navigate(Routes.Voice.route) },
                onOpenConsole = { navController.navigate(Routes.Console.route) },
                onOpenVersions = { navController.navigate(Routes.Versions.route) },
                onOpenSettings = { navController.navigate(Routes.Settings.route) }
            )
        }

        composable(Routes.Chat.route) {
            ChatScreen(
                onBack = { navController.popBackStack() },
                settingsRepository = settingsRepository,
                consoleRepository = consoleRepository
            )
        }

        composable(Routes.Voice.route) {
            VoiceControlScreen(
                onBack = { navController.popBackStack() },
                consoleRepository = consoleRepository
            )
        }

        composable(Routes.Console.route) {
            ConsoleScreen(
                onBack = { navController.popBackStack() },
                consoleRepository = consoleRepository
            )
        }

        composable(Routes.Versions.route) {
            VersionsScreen(
                onBack = { navController.popBackStack() },
                versionRepository = versionRepository,
                consoleRepository = consoleRepository
            )
        }

        composable(Routes.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                settingsRepository = settingsRepository,
                consoleRepository = consoleRepository
            )
        }
    }
}
