package com.rezon.app.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rezon.app.presentation.ui.screens.library.LibraryScreen
import com.rezon.app.presentation.ui.screens.player.PlayerScreen
import com.rezon.app.presentation.ui.screens.settings.SettingsScreen
import com.rezon.app.presentation.ui.screens.splash.SplashScreen

/**
 * REZON Navigation Routes
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Library : Screen("library")
    data object Player : Screen("player/{bookId}") {
        fun createRoute(bookId: String) = "player/$bookId"
    }
    data object Settings : Screen("settings")
}

/**
 * Main Navigation Host
 * Handles all app navigation with smooth animations
 */
@Composable
fun RezonNavHost(
    navController: NavHostController = rememberNavController(),
    onReady: () -> Unit = {}
) {
    // Signal ready after first composition
    LaunchedEffect(Unit) {
        onReady()
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        }
    ) {
        // Splash Screen with cinematic animation
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLibrary = {
                    navController.navigate(Screen.Library.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Library Screen
        composable(Screen.Library.route) {
            LibraryScreen(
                onNavigateToPlayer = { bookId ->
                    navController.navigate(Screen.Player.createRoute(bookId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Player Screen with gesture controls
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
            PlayerScreen(
                bookId = bookId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
