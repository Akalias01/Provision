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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Settings
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
import com.rezon.app.presentation.ui.theme.RezonPurple

/**
 * REZON App Navigation
 *
 * Main NavHost containing all app routes with smooth animations.
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
        // Library Screen
        composable(Route.Library.route) {
            PlaceholderScreen(
                title = "Library",
                icon = Icons.Default.Headphones
            )
        }

        // Player Screen
        composable(
            route = Route.Player.route,
            arguments = listOf(
                navArgument(Route.Player.ARG_BOOK_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString(Route.Player.ARG_BOOK_ID) ?: ""
            PlaceholderScreen(
                title = "Now Playing",
                subtitle = "Book ID: $bookId",
                icon = Icons.Default.Headphones
            )
        }

        // Torrents Screen
        composable(Route.Torrents.route) {
            PlaceholderScreen(
                title = "Downloads",
                subtitle = "Torrent Manager",
                icon = Icons.Default.Download
            )
        }

        // Cloud Screen
        composable(Route.Cloud.route) {
            PlaceholderScreen(
                title = "Cloud Storage",
                subtitle = "Google Drive & Dropbox",
                icon = Icons.Default.Cloud
            )
        }

        // Folders Screen
        composable(Route.Folders.route) {
            PlaceholderScreen(
                title = "Folders to Scan",
                subtitle = "Manage audiobook folders",
                icon = Icons.Default.Folder
            )
        }

        // Settings Screen
        composable(Route.Settings.route) {
            PlaceholderScreen(
                title = "Settings",
                subtitle = "App preferences",
                icon = Icons.Default.Settings
            )
        }
    }
}

/**
 * Placeholder screen for testing navigation
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
