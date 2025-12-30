package com.mossglen.reverie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mossglen.reverie.BuildConfig

/**
 * Actions that can be triggered from the drawer
 */
enum class DrawerAction {
    OPEN_IMPORT,
    OPEN_TORRENTS,
    OPEN_CLOUD,
    OPEN_THEMES,
    OPEN_TRANSLATE,
    OPEN_STATS
}

@Composable
fun ReverieDrawerContent(
    isDarkTheme: Boolean,
    isReverieTheme: Boolean,
    accentColor: Color,
    onNavigate: (String) -> Unit,
    onAction: (DrawerAction) -> Unit
) {
    val bgColor = if (isDarkTheme) Color(0xFF0A0A0A) else Color(0xFFF5F5F5)
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val subtitleColor = if (isDarkTheme) Color.Gray else Color.DarkGray
    val dividerColor = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFE0E0E0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(top = 48.dp, start = 16.dp, end = 16.dp)
    ) {
        // No branding - will revisit later

        // MENU ITEMS
        ReverieDrawerMenuItem(Icons.Outlined.FolderOpen, "Add Files", "Import content", accentColor, textColor, subtitleColor) {
            onAction(DrawerAction.OPEN_IMPORT)
        }
        ReverieDrawerMenuItem(Icons.Outlined.CloudDownload, "Torrent Downloads", "Magnet downloads", accentColor, textColor, subtitleColor) {
            onAction(DrawerAction.OPEN_TORRENTS)
        }
        ReverieDrawerMenuItem(Icons.Outlined.CloudQueue, "Cloud Storage", "Drive & Dropbox", accentColor, textColor, subtitleColor) {
            onAction(DrawerAction.OPEN_CLOUD)
        }
        ReverieDrawerMenuItem(Icons.AutoMirrored.Outlined.LibraryBooks, "Library", "Your collection", accentColor, textColor, subtitleColor) {
            onNavigate("library")
        }

        HorizontalDivider(
            color = dividerColor,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        ReverieDrawerMenuItem(Icons.Outlined.Palette, "Appearance", "Theme options", accentColor, textColor, subtitleColor) {
            onAction(DrawerAction.OPEN_THEMES)
        }
        ReverieDrawerMenuItem(Icons.Outlined.Settings, "Settings", "Configure the app", accentColor, textColor, subtitleColor) {
            onNavigate("settings")
        }
        ReverieDrawerMenuItem(Icons.Outlined.Translate, "Translate", "Change language", accentColor, textColor, subtitleColor) {
            onAction(DrawerAction.OPEN_TRANSLATE)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Version at bottom
        Text(
            text = "Version ${BuildConfig.VERSION_NAME}",
            color = subtitleColor,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
fun ReverieDrawerMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    accentColor: Color = Color.White,
    textColor: Color = Color.White,
    subtitleColor: Color = Color.Gray,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = accentColor.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                title,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = subtitleColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}
