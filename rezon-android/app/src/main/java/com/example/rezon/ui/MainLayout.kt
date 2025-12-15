package com.example.rezon.ui

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.rezon.ui.components.RezonDrawerContent
import com.example.rezon.ui.screens.LibraryScreen
import com.example.rezon.ui.screens.PlayerScreen
import com.example.rezon.ui.screens.SettingsScreen
import com.example.rezon.ui.viewmodel.PlayerViewModel
import com.example.rezon.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MainLayout(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Access current theme from ViewModel
    val currentThemeOption by themeViewModel.currentTheme

    // Player State
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val book = playerViewModel.demoBook

    // Sheet Logic
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var showPlayerBar by remember { mutableStateOf(true) }

    // Side Effect to apply theme colors to the status bar
    val context = LocalContext.current
    LaunchedEffect(currentThemeOption) {
        val window = (context as Activity).window
        window.statusBarColor = Color.Black.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF161618)) {
                RezonDrawerContent(
                    currentTheme = currentThemeOption,
                    onThemeSelect = { theme -> themeViewModel.setTheme(theme) },
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        navController.navigate(route)
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Main Content Layer
            NavHost(navController = navController, startDestination = "library") {
                composable("library") {
                    LibraryScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onBookClick = { isPlayerExpanded = true },
                        currentThemeColor = currentThemeOption.primary
                    )
                }
                composable("player") {
                    // Optional: Player can also be accessed via navigation route
                    PlayerScreen(
                        onBack = { navController.popBackStack() },
                        currentThemeColor = currentThemeOption.primary,
                        onTogglePlayPause = { playerViewModel.togglePlayPause() },
                        onSkipForward = { playerViewModel.skipForward() },
                        onSkipBackward = { playerViewModel.skipBackward() },
                        onCycleSpeed = { playerViewModel.cyclePlaybackSpeed() },
                        onSleepTimerClick = { /* TODO */ },
                        onEqualizerClick = { /* TODO */ },
                        onChapterClick = { /* TODO */ },
                        onMoreOptionsClick = { /* TODO */ }
                    )
                }
                composable("settings") {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
            }

            // 2. Mini Player (Bottom Dock)
            if (showPlayerBar && !isPlayerExpanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0xFF161618))
                        .clickable { isPlayerExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(model = book.coverUrl, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(book.title, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text(book.author, color = currentThemeOption.primary, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { playerViewModel.togglePlayPause() }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Toggle",
                                tint = Color.White
                            )
                        }
                    }
                    // Progress bar line at top
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.TopStart)
                            .background(currentThemeOption.primary)
                    )
                }
            }

            // 3. Full Player Layer (Animated Overlay)
            AnimatedVisibility(
                visible = isPlayerExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                PlayerScreen(
                    onBack = { isPlayerExpanded = false },
                    currentThemeColor = currentThemeOption.primary,
                    onTogglePlayPause = { playerViewModel.togglePlayPause() },
                    onSkipForward = { playerViewModel.skipForward() },
                    onSkipBackward = { playerViewModel.skipBackward() },
                    onCycleSpeed = { playerViewModel.cyclePlaybackSpeed() },
                    onSleepTimerClick = { /* TODO: Implement Sleep Timer UI */ },
                    onEqualizerClick = { /* TODO: Implement Equalizer UI */ },
                    onChapterClick = { /* TODO: Implement Chapter Selection UI */ },
                    onMoreOptionsClick = { /* TODO: Implement More Options */ }
                )
            }
        }
    }
}
