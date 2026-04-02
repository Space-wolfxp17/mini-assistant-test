package com.ordis.app.navigation

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object Chat : Routes("chat")
    data object Voice : Routes("voice")
    data object Console : Routes("console")
    data object Versions : Routes("versions")
    data object Settings : Routes("settings")
}
