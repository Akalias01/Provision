package com.example.rezon.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.rezon.ui.theme.RezonThemeOption

@Composable
fun RezonDrawerContent(
    currentTheme: RezonThemeOption,
    onThemeSelect: (RezonThemeOption) -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header with Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(currentTheme.primary, shape = MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Text("R", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("REZON", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = currentTheme.primary)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Explore Section
        Text("Explore", color = Color.Gray, modifier = Modifier.padding(start = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        DrawerItem(Icons.Default.LibraryBooks, "Library", currentTheme.primary) { onNavigate("library") }
        DrawerItem(Icons.Default.CloudDownload, "Torrent Downloads", currentTheme.primary) { /* TODO */ }
        DrawerItem(Icons.Default.Cloud, "Cloud Storage", currentTheme.primary) { /* TODO */ }

        HorizontalDivider(
            color = Color.Gray.copy(alpha = 0.2f),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Customize Section
        Text("Customize", color = Color.Gray, modifier = Modifier.padding(start = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        // Theme Selection
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Theme", color = Color.White, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RezonThemeOption.entries.forEach { themeOption ->
                    ThemeSelectorChip(
                        label = themeOption.nameStr,
                        color = themeOption.primary,
                        isSelected = themeOption == currentTheme,
                        onClick = {
                            onThemeSelect(themeOption)
                            // Update Status Bar Color dynamically
                            val window = (context as Activity).window
                            window.statusBarColor = Color.Black.toArgb()
                            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Settings at bottom
        DrawerItem(Icons.Default.Settings, "Settings", currentTheme.primary) { onNavigate("settings") }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, accentColor: Color, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, color = Color.White) },
        icon = { Icon(icon, contentDescription = null, tint = accentColor) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 4.dp),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            selectedContainerColor = accentColor.copy(alpha = 0.1f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectorChip(label: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp) },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            containerColor = Color.DarkGray.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.small
    )
}
