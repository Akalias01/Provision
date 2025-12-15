package com.example.rezon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Replay10
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.cyclePlaybackSpeed() }, enabled = isServiceConnected) {
                    Text(text = "${speed}x", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = { showMetadataSheet = true }) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Info")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Cover Art
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray)
                    .pointerInput(isServiceConnected) {
                        if (isServiceConnected) {
                            detectTapGestures(
                                onTap = { viewModel.togglePlayPause() },
                                onDoubleTap = { offset ->
                                    if (offset.x < size.width / 2) viewModel.skipBackward() else viewModel.skipForward()
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

            Spacer(modifier = Modifier.height(32.dp))

            Text(text = book.title, style = MaterialTheme.typography.headlineMedium)
            Text(text = book.author, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)

            Spacer(modifier = Modifier.weight(1f))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.skipBackward() },
                    modifier = Modifier.size(64.dp),
                    enabled = isServiceConnected
                ) {
                    Icon(imageVector = Icons.Default.Replay10, contentDescription = "-10s", modifier = Modifier.size(36.dp))
                }

                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(96.dp),
                    enabled = isServiceConnected
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(80.dp),
                        tint = if (isServiceConnected) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                IconButton(
                    onClick = { viewModel.skipForward() },
                    modifier = Modifier.size(64.dp),
                    enabled = isServiceConnected
                ) {
                    Icon(imageVector = Icons.Default.Forward30, contentDescription = "+30s", modifier = Modifier.size(36.dp))
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
                CircularProgressIndicator()
            }
        }
    }
}
