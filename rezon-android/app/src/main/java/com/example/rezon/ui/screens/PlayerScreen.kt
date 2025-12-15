package com.example.rezon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.rezon.ui.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit = {},
    currentThemeColor: Color = MaterialTheme.colorScheme.primary,
    onTogglePlayPause: () -> Unit = {},
    onSkipForward: () -> Unit = {},
    onSkipBackward: () -> Unit = {},
    onCycleSpeed: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onChapterClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val book = viewModel.demoBook
    val isPlaying by viewModel.isPlaying.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val isServiceConnected by viewModel.isServiceConnected.collectAsState()

    var showMetadataSheet by remember { mutableStateOf(false) }

    if (showMetadataSheet) {
        ModalBottomSheet(onDismissRequest = { showMetadataSheet = false }) {
            Column(Modifier.padding(16.dp)) {
                Text(text = "Synopsis", style = MaterialTheme.typography.titleMedium)
                Text(text = book.synopsis, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Text(text = "Series Info", style = MaterialTheme.typography.titleMedium)
                Text(text = book.seriesInfo, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row with Back, Speed, and More Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = Color.White
                    )
                }

                TextButton(onClick = { onCycleSpeed(); viewModel.cyclePlaybackSpeed() }, enabled = isServiceConnected) {
                    Text(text = "${speed}x", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = currentThemeColor)
                }

                IconButton(onClick = { showMetadataSheet = true }) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cover Art with Gesture Detection
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray)
                    .pointerInput(isServiceConnected) {
                        if (isServiceConnected) {
                            detectTapGestures(
                                onTap = {
                                    onTogglePlayPause()
                                    viewModel.togglePlayPause()
                                },
                                onDoubleTap = { offset ->
                                    if (offset.x < size.width / 2) {
                                        onSkipBackward()
                                        viewModel.skipBackward()
                                    } else {
                                        onSkipForward()
                                        viewModel.skipForward()
                                    }
                                }
                            )
                        }
                    }
            ) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = "Cover Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Book Info
            Text(text = book.title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(text = book.author, style = MaterialTheme.typography.bodyLarge, color = currentThemeColor)

            Spacer(modifier = Modifier.height(16.dp))

            // Chapter Info (clickable)
            TextButton(onClick = onChapterClick) {
                Text(
                    text = "Chapter 1 of ${book.chapterMarkers.size}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = onSleepTimerClick) {
                    Icon(Icons.Default.Timer, contentDescription = "Sleep Timer", tint = Color.Gray)
                }
                IconButton(onClick = onEqualizerClick) {
                    Icon(Icons.Default.Equalizer, contentDescription = "Equalizer", tint = Color.Gray)
                }
                IconButton(onClick = onMoreOptionsClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSkipBackward(); viewModel.skipBackward() },
                    modifier = Modifier.size(64.dp),
                    enabled = isServiceConnected
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "-10s",
                        modifier = Modifier.size(36.dp),
                        tint = if (isServiceConnected) Color.White else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = { onTogglePlayPause(); viewModel.togglePlayPause() },
                    modifier = Modifier.size(96.dp),
                    enabled = isServiceConnected
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(80.dp),
                        tint = if (isServiceConnected) currentThemeColor else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = { onSkipForward(); viewModel.skipForward() },
                    modifier = Modifier.size(64.dp),
                    enabled = isServiceConnected
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward30,
                        contentDescription = "+30s",
                        modifier = Modifier.size(36.dp),
                        tint = if (isServiceConnected) Color.White else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Loading Overlay
        if (!isServiceConnected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = currentThemeColor)
            }
        }
    }
}
