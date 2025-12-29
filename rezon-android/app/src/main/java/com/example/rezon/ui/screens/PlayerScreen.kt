package com.example.rezon.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    currentThemeColor: Color,
    onTogglePlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onCycleSpeed: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onChapterClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val book = viewModel.demoBook
    val isPlaying by viewModel.isPlaying.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val currentPos by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    // Status Bar Styling
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val window = (context as Activity).window
        window.statusBarColor = Color.Black.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Background Gradient Layer
        AsyncImage(
            model = book.coverUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.3f),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Black)
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
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text("Now Playing", color = Color.Gray)
                IconButton(onClick = onMoreOptionsClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Large Cover Art with Gestures
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
                    model = book.coverUrl,
                    contentDescription = "Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title & Author
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(book.author, style = MaterialTheme.typography.bodyLarge, color = currentThemeColor)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Seek Bar
            Slider(
                value = if (duration > 0) currentPos.toFloat() / duration else 0f,
                onValueChange = { /* TODO: Implement seekTo based on progress */ },
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
                    Icon(
                        Icons.Default.Replay10,
                        contentDescription = "-10",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play/Pause Button
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
                    Icon(
                        Icons.Default.Forward30,
                        contentDescription = "+30",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Bottom Tools (Speed, Sleep, EQ, Chapters)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BottomTool(Icons.Default.Speed, "${speed}x", currentThemeColor) { onCycleSpeed() }
                BottomTool(Icons.Rounded.Timer, "Sleep", currentThemeColor) { onSleepTimerClick() }
                BottomTool(Icons.Rounded.Equalizer, "EQ", currentThemeColor) { onEqualizerClick() }
                BottomTool(Icons.Default.List, "Chapters", currentThemeColor) { onChapterClick() }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BottomTool(icon: ImageVector, label: String, accentColor: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
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
