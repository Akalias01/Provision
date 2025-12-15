package com.example.rezon.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.rezon.ui.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    currentThemeColor: Color,
    onTogglePlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onCycleSpeed: () -> Unit,
    onSleepTimerClick: () -> Unit, // Replaced by internal logic
    onEqualizerClick: () -> Unit,
    onChapterClick: () -> Unit,    // Replaced by internal logic
    onMoreOptionsClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val book by viewModel.currentBook.collectAsState()
    val displayBook = book ?: viewModel.demoBook

    val isPlaying by viewModel.isPlaying.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val currentPos by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    // Sheet States
    var showInfoSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showChaptersSheet by remember { mutableStateOf(false) }

    // Status Bar Styling
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val window = (context as Activity).window
        window.statusBarColor = android.graphics.Color.BLACK
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
    }

    // --- MAIN LAYOUT ---
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Background Gradient Layer
        if (displayBook.coverUrl != null) {
            AsyncImage(
                 model = displayBook.coverUrl,
                 contentDescription = null,
                 modifier = Modifier.fillMaxSize().alpha(0.3f),
                 contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha=0.6f), Color.Black)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text("Now Playing", color = Color.Gray)
                IconButton(onClick = { showInfoSheet = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Cover Art
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                val width = size.width
                                if (offset.x < width / 2) onSkipBackward() else onSkipForward()
                            },
                            onTap = { onTogglePlayPause() }
                        )
                    }
            ) {
                AsyncImage(
                    model = displayBook.coverUrl,
                    contentDescription = "Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title & Author
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(displayBook.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text(displayBook.author, style = MaterialTheme.typography.bodyLarge, color = currentThemeColor, maxLines = 1)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Seek Bar
            Slider(
                value = if (duration > 0) currentPos.toFloat() / duration else 0f,
                onValueChange = { /* Seek logic */ },
                colors = SliderDefaults.colors(
                    thumbColor = currentThemeColor,
                    activeTrackColor = currentThemeColor,
                    inactiveTrackColor = Color.DarkGray
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(currentPos), color = Color.Gray, fontSize = 12.sp)
                Text(formatTime(duration), color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSkipBackward, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.Replay10, contentDescription = "-10", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) Color(0xFFFF2020) else currentThemeColor)
                        .clickable { onTogglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(onClick = onSkipForward, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.Forward30, contentDescription = "+30", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Bottom Tools
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BottomTool(Icons.Rounded.Speed, "${speed}x", currentThemeColor) { onCycleSpeed() }
                BottomTool(Icons.Rounded.Timer, "Sleep", currentThemeColor) { showSleepSheet = true }
                BottomTool(Icons.Rounded.Equalizer, "EQ", currentThemeColor) { onEqualizerClick() }
                BottomTool(Icons.Default.List, "Chapters", currentThemeColor) { showChaptersSheet = true }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- BOTTOM SHEETS ---

        // 1. Info Sheet (Metadata)
        if (showInfoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                containerColor = Color(0xFF161618)
            ) {
                Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                    Text(displayBook.title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(displayBook.author, style = MaterialTheme.typography.titleMedium, color = currentThemeColor)
                    if (displayBook.seriesInfo.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(displayBook.seriesInfo, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (displayBook.synopsis.isNotEmpty()) displayBook.synopsis else "Fetching synopsis...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                    Spacer(Modifier.height(48.dp))
                }
            }
        }

        // 2. Sleep Timer Sheet
        if (showSleepSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSleepSheet = false },
                containerColor = Color(0xFF161618)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Sleep Timer", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    val timers = listOf(15, 30, 45, 60)
                    timers.forEach { min ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSleepTimer(min)
                                    showSleepSheet = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text("$min Minutes", color = Color.White, fontSize = 18.sp)
                        }
                        HorizontalDivider(color = Color.DarkGray)
                    }
                    Spacer(Modifier.height(48.dp))
                }
            }
        }

        // 3. Chapters Sheet (Placeholder)
        if (showChaptersSheet) {
             ModalBottomSheet(
                onDismissRequest = { showChaptersSheet = false },
                containerColor = Color(0xFF161618)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Chapters", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    // Mock chapters for now
                    (1..5).forEach { i ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Chapter $i", color = Color.White)
                            Text("15:00", color = Color.Gray)
                        }
                        HorizontalDivider(color = Color.DarkGray)
                    }
                    Spacer(Modifier.height(48.dp))
                }
             }
        }
    }
}

@Composable
fun BottomTool(icon: ImageVector, label: String, accentColor: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
