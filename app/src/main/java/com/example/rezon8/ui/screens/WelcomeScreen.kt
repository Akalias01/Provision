package com.mossglen.lithos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mossglen.lithos.ui.theme.LithosUI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    accentColor: Color,
    isDarkTheme: Boolean,
    isOLED: Boolean = false,
    onOpenDrawer: () -> Unit,
    onImportClick: () -> Unit,
    onTorrentClick: () -> Unit
) {
    // Use Lithos 3-mode system for consistent theming
    val bgColor = LithosUI.background(isDarkTheme, isOLED)
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val subtitleColor = Color.Gray
    val cardColor = if (isDarkTheme) LithosUI.SheetBackground else LithosUI.ElevatedBackgroundLight
    val secondaryButtonColor = if (isDarkTheme) LithosUI.CardBackground else LithosUI.ElevatedBackgroundLight
    val iconColor = if (isDarkTheme) Color.LightGray else Color.DarkGray

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, "Menu", tint = textColor) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Welcome to Reverie", color = textColor, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("Your audio, reimagined.", color = subtitleColor, textAlign = TextAlign.Center)

            Spacer(Modifier.height(64.dp))

            // Main CTA
            WelcomeButton("Import Your First Book", Icons.Default.FolderOpen, accentColor) { onImportClick() }
            Spacer(Modifier.height(16.dp))
            WelcomeButton("Download from Torrent", Icons.Default.CloudDownload, secondaryButtonColor, textColor) { onTorrentClick() }

            Spacer(Modifier.height(48.dp))

            // Categories - neutral gray icons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CategoryCard("Audiobooks", Icons.Default.Headphones, iconColor, cardColor, textColor, Modifier.weight(1f)) { onImportClick() }
                CategoryCard("EPUB", Icons.Default.ImportContacts, iconColor, cardColor, textColor, Modifier.weight(1f)) { onImportClick() }
            }
            Spacer(Modifier.height(16.dp))
            CategoryCard("PDF / Docs", Icons.Default.Description, iconColor, cardColor, textColor, Modifier.fillMaxWidth()) { onImportClick() }
        }
    }
}

@Composable
fun WelcomeButton(text: String, icon: ImageVector, bg: Color, textColor: Color = Color.Black, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, null, tint = textColor)
        Spacer(Modifier.width(8.dp))
        Text(text, color = textColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CategoryCard(label: String, icon: ImageVector, iconColor: Color, cardColor: Color, textColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(100.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = iconColor)
            Spacer(Modifier.height(8.dp))
            Text(label, color = textColor, fontWeight = FontWeight.Bold)
        }
    }
}
