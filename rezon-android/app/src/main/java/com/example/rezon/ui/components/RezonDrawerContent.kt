package com.example.rezon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rezon.ui.theme.RezonThemeOption
import com.example.rezon.ui.viewmodel.ThemeViewModel

@Composable
fun RezonDrawerContent(
    onNavigate: (String) -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val currentTheme by themeViewModel.currentTheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161618))
            .padding(16.dp)
    ) {
        // Header
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "REZON",
            color = currentTheme.primary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
        Text(
            text = "Audiobooks Reimagined",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = Color(0xFF2A2A2C))
        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Items
        DrawerMenuItem(
            icon = Icons.Default.Home,
            label = "Library",
            selected = true,
            accentColor = currentTheme.primary,
            onClick = { onNavigate("library") }
        )
        DrawerMenuItem(
            icon = Icons.Default.Search,
            label = "Discover",
            accentColor = currentTheme.primary,
            onClick = { onNavigate("discover") }
        )
        DrawerMenuItem(
            icon = Icons.Default.Bookmark,
            label = "Bookmarks",
            accentColor = currentTheme.primary,
            onClick = { onNavigate("bookmarks") }
        )
        DrawerMenuItem(
            icon = Icons.Default.History,
            label = "History",
            accentColor = currentTheme.primary,
            onClick = { onNavigate("history") }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFF2A2A2C))
        Spacer(modifier = Modifier.height(16.dp))

        // Theme Section
        Text(
            text = "THEME",
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RezonThemeOption.entries.forEach { theme ->
                ThemeColorButton(
                    color = theme.primary,
                    selected = currentTheme == theme,
                    onClick = { themeViewModel.setTheme(theme) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Footer
        HorizontalDivider(color = Color(0xFF2A2A2C))
        Spacer(modifier = Modifier.height(16.dp))
        DrawerMenuItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            accentColor = currentTheme.primary,
            onClick = { onNavigate("settings") }
        )
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) accentColor.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) accentColor else Color.Gray
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ThemeColorButton(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
