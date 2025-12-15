package com.rezon.app.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Library : Screen("library")
    object Player : Screen("player/{bookId}") {
        fun createRoute(bookId: String) = "player/$bookId"
    }
    object Settings : Screen("settings")
}
