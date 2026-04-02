package com.ordis.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ordis.app.data.repo.ConsoleRepository
import com.ordis.app.data.repo.SettingsRepository
import com.ordis.app.data.repo.VersionRepository
import com.ordis.app.navigation.AppNavGraph

@Composable
fun OrdisApp() {
    val settingsRepo = remember { SettingsRepository() }
    val versionRepo = remember { VersionRepository() }
    val consoleRepo = remember { ConsoleRepository() }

    AppNavGraph(
        settingsRepository = settingsRepo,
        versionRepository = versionRepo,
        consoleRepository = consoleRepo
    )
}
