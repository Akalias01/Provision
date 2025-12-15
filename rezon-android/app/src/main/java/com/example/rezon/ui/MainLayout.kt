package com.example.rezon.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.rezon.ui.components.RezonDrawerContent
import com.example.rezon.ui.screens.LibraryScreen
import com.example.rezon.ui.screens.PlayerScreen
import com.example.rezon.ui.viewmodel.PlayerViewModel
import com.example.rezon.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

@Composable
fun MainLayout(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Player State
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val book = playerViewModel.demoBook

    // Sheet Logic
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    var isPlayerExpanded by remember { mutableStateOf(false) }

    // We toggle this to show the player bar at all
    var showPlayerBar by remember { mutableStateOf(true) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF161618)) {
                RezonDrawerContent(onNavigate = {
                    scope.launch { drawerState.close() }
                    // Handle Navigation
                })
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Main Content Layer
            NavHost(navController = navController, startDestination = "library") {
                composable("library") {
                    LibraryScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onBookClick = { isPlayerExpanded = true }
                    )
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
                            Text(book.author, color = themeViewModel.currentTheme.value.primary, style = MaterialTheme.typography.bodySmall)
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
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopStart).background(themeViewModel.currentTheme.value.primary))
                }
            }

            // 3. Full Player Layer (Animated Overlay)
            AnimatedVisibility(
                visible = isPlayerExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                // Wrap PlayerScreen in a draggable box to swipe down
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                if (delta > 50) isPlayerExpanded = false
                            }
                        )
                ) {
                    PlayerScreen(
                        onBack = { isPlayerExpanded = false }
                    )
                }
            }
        }
    }
}
