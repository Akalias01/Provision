package com.rezon.app.presentation.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rezon.app.presentation.navigation.AppNavigation
import com.rezon.app.presentation.navigation.Route
import com.rezon.app.presentation.ui.theme.DrawerBackground
import com.rezon.app.presentation.ui.theme.DrawerItemSelected
import com.rezon.app.presentation.ui.theme.DividerColor
import com.rezon.app.presentation.ui.theme.RezonCyan
import com.rezon.app.presentation.ui.theme.RezonOnSurfaceVariant
import com.rezon.app.presentation.ui.theme.RezonPurple
import kotlinx.coroutines.launch

/**
 * REZON Main Layout
 *
 * App shell with Sirin-style ModalNavigationDrawer.
 * Contains:
 * - Drawer with REZON branding and navigation items
 * - TopAppBar with current screen title
 * - Navigation content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    navController: NavHostController = rememberNavController()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Get current route for title
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = Route.fromRoute(navBackStackEntry?.destination?.route)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            RezonDrawerContent(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch {
                        drawerState.close()
                        navController.navigate(route.route) {
                            popUpTo(Route.Library.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onScanFolder = {
                    scope.launch { drawerState.close() }
                    // Open document tree picker to select folder
                    Toast.makeText(context, "Select a folder containing audiobooks", Toast.LENGTH_SHORT).show()
                    // Note: Actual folder picker requires Activity result handling
                    // This shows intent - full implementation needs ActivityResultLauncher
                },
                onRecommend = {
                    scope.launch { drawerState.close() }
                    // Share the app
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "REZON Audiobook Player")
                        putExtra(Intent.EXTRA_TEXT, "Check out REZON - A beautiful audiobook player for Android!\n\nhttps://play.google.com/store/apps/details?id=com.rezon.app")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share REZON"))
                },
                onTranslate = {
                    scope.launch { drawerState.close() }
                    // Open language settings
                    try {
                        val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Language settings not available", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            topBar = {
                RezonTopAppBar(
                    title = currentRoute.title,
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            AppNavigation(
                navController = navController,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

/**
 * REZON Drawer Content
 * Sirin-style navigation drawer with gradient header
 */
@Composable
private fun RezonDrawerContent(
    currentRoute: Route,
    onNavigate: (Route) -> Unit,
    onScanFolder: () -> Unit,
    onRecommend: () -> Unit,
    onTranslate: () -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    ModalDrawerSheet(
        drawerContainerColor = DrawerBackground,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = statusBarPadding.calculateTopPadding())
                .padding(bottom = navBarPadding.calculateBottomPadding())
        ) {
            // ==================== DRAWER HEADER ====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                RezonPurple.copy(alpha = 0.3f),
                                RezonCyan.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo "R"
                    Text(
                        text = "R",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 48.sp
                        ),
                        color = RezonPurple
                    )
                    // "EZON" text
                    Text(
                        text = "EZON",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            letterSpacing = 2.sp
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==================== DRAWER ITEMS ====================
            // Scan Folder
            DrawerItem(
                icon = Icons.Default.FolderOpen,
                label = "Scan folder",
                selected = false,
                onClick = onScanFolder
            )

            // Folders to scan
            DrawerItem(
                icon = Icons.Default.Folder,
                label = "Folders to scan",
                selected = currentRoute == Route.Folders,
                onClick = { onNavigate(Route.Folders) }
            )

            // Download torrent
            DrawerItem(
                icon = Icons.Default.Download,
                label = "Download torrent",
                selected = currentRoute == Route.Torrents,
                onClick = { onNavigate(Route.Torrents) }
            )

            // Cloud Storage
            DrawerItem(
                icon = Icons.Default.Cloud,
                label = "Cloud Storage",
                selected = currentRoute == Route.Cloud,
                onClick = { onNavigate(Route.Cloud) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                color = DividerColor
            )

            // Settings
            DrawerItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                selected = currentRoute == Route.Settings,
                onClick = { onNavigate(Route.Settings) }
            )

            // Recommend
            DrawerItem(
                icon = Icons.Default.Share,
                label = "Recommend",
                selected = false,
                onClick = onRecommend
            )

            // Translate
            DrawerItem(
                icon = Icons.Default.Language,
                label = "Translate",
                selected = false,
                onClick = onTranslate
            )

            Spacer(modifier = Modifier.weight(1f))

            // ==================== FOOTER ====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "REZON v2.0.1",
                    style = MaterialTheme.typography.bodySmall,
                    color = RezonOnSurfaceVariant
                )
            }
        }
    }
}

/**
 * Single drawer item
 */
@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        selected -> DrawerItemSelected
        else -> Color.Transparent
    }
    val contentColor = when {
        selected -> RezonPurple
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = contentColor
        )
    }
}

/**
 * REZON Top App Bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RezonTopAppBar(
    title: String,
    onMenuClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}
