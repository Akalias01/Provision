package com.rezon.app.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rezon.app.presentation.ui.screens.equalizer.EqualizerScreen
import com.rezon.app.presentation.ui.screens.library.LibraryScreen
import com.rezon.app.presentation.ui.screens.player.PlayerScreen
import com.rezon.app.presentation.ui.screens.settings.SettingsScreen
import com.rezon.app.presentation.ui.screens.torrent.TorrentScreen
import com.rezon.app.presentation.ui.theme.RezonPurple

/**
 * REZON App Navigation
 *
 * Main NavHost containing all app routes with smooth animations.
 * Uses actual screen implementations where available.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Library.route,
        modifier = modifier,
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
        // Library Screen - Main screen with audiobook list
        composable(Route.Library.route) {
            LibraryScreen(
                onNavigateToPlayer = { bookId ->
                    navController.navigate(Route.Player.createRoute(bookId))
                },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings.route)
                }
            )
        }

        // Player Screen - Full audiobook player with gestures
        composable(
            route = Route.Player.route,
            arguments = listOf(
                navArgument(Route.Player.ARG_BOOK_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString(Route.Player.ARG_BOOK_ID) ?: ""
            PlayerScreen(
                bookId = bookId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEqualizer = { navController.navigate(Route.Equalizer.route) },
                onNavigateToBookmarks = { id -> navController.navigate(Route.Bookmarks.createRoute(id)) }
            )
        }

        // Settings Screen - App configuration
        composable(Route.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEqualizer = { navController.navigate(Route.Equalizer.route) }
            )
        }

        // Equalizer Screen - Audio equalizer
        composable(Route.Equalizer.route) {
            EqualizerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Torrents Screen - Download manager with circular progress
        composable(Route.Torrents.route) {
            TorrentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Cloud Screen - Cloud storage integration (placeholder)
        composable(Route.Cloud.route) {
            PlaceholderScreen(
                title = "Cloud Storage",
                subtitle = "Google Drive & Dropbox - Coming Soon",
                icon = Icons.Default.Cloud
            )
        }

        // Folders Screen - Folder management (placeholder)
        composable(Route.Folders.route) {
            PlaceholderScreen(
                title = "Folders to Scan",
                subtitle = "Manage audiobook folders - Coming Soon",
                icon = Icons.Default.Folder
            )
        }
    }
}

/**
 * Placeholder screen for features under development
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String? = null,
    icon: ImageVector
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RezonPurple,
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
