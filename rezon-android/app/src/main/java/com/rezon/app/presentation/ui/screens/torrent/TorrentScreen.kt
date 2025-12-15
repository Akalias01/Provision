package com.rezon.app.presentation.ui.screens.torrent

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rezon.app.presentation.ui.theme.PlayerGradientEnd
import com.rezon.app.presentation.ui.theme.PlayerGradientStart
import com.rezon.app.presentation.ui.theme.RezonCyan
import com.rezon.app.presentation.ui.theme.RezonPurple
import com.rezon.app.presentation.viewmodel.TorrentViewModel
import com.rezon.app.service.TorrentDownload
import com.rezon.app.service.TorrentState

/**
 * REZON Torrent Download Screen
 *
 * Features:
 * - Circular progress indicators for each download
 * - Add magnet link dialog
 * - Pause/Resume/Delete controls
 * - Download speed display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentScreen(
    onNavigateBack: () -> Unit,
    viewModel: TorrentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = RezonPurple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Downloads",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = RezonPurple,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Torrent",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PlayerGradientStart, PlayerGradientEnd)
                    )
                )
                .padding(paddingValues)
        ) {
            if (uiState.downloads.isEmpty()) {
                // Empty state
                EmptyDownloadsState()
            } else {
                // Downloads list
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.downloads, key = { it.id }) { download ->
                        DownloadItem(
                            download = download,
                            onPause = { viewModel.pauseDownload(download.id) },
                            onResume = { viewModel.resumeDownload(download.id) },
                            onRemove = { viewModel.removeDownload(download.id, deleteFiles = false) },
                            formatBytes = viewModel::formatBytes,
                            formatSpeed = viewModel::formatSpeed
                        )
                    }
                }
            }
        }

        // Add magnet dialog
        if (uiState.showAddDialog) {
            AddMagnetDialog(
                magnetInput = uiState.magnetInput,
                onMagnetChange = { viewModel.updateMagnetInput(it) },
                onConfirm = { viewModel.addMagnet() },
                onDismiss = { viewModel.hideAddDialog() }
            )
        }
    }
}

/**
 * Single download item with circular progress
 */
@Composable
private fun DownloadItem(
    download: TorrentDownload,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRemove: () -> Unit,
    formatBytes: (Long) -> String,
    formatSpeed: (Long) -> String
) {
    val progressAnimated by animateFloatAsState(
        targetValue = download.progress,
        animationSpec = tween(300),
        label = "progress"
    )

    val progressColor by animateColorAsState(
        targetValue = when (download.state) {
            TorrentState.FINISHED -> RezonCyan
            TorrentState.PAUSED -> MaterialTheme.colorScheme.outline
            TorrentState.ERROR -> MaterialTheme.colorScheme.error
            else -> RezonPurple
        },
        label = "progressColor"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Progress Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(72.dp)
            ) {
                // Background circle
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(72.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )

                // Progress circle
                CircularProgressIndicator(
                    progress = { progressAnimated },
                    modifier = Modifier.size(72.dp),
                    color = progressColor,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )

                // Percentage or icon in center
                if (download.state == TorrentState.FINISHED) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Finished",
                        tint = RezonCyan,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Text(
                        text = "${(download.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Download info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Size info
                Text(
                    text = "${formatBytes(download.downloadedSize)} / ${formatBytes(download.totalSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Speed and peers (only while downloading)
                if (download.state == TorrentState.DOWNLOADING) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        Text(
                            text = "↓ ${formatSpeed(download.downloadSpeed)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RezonCyan,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${download.peers} peers • ${download.seeds} seeds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // State indicator
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (download.state) {
                        TorrentState.QUEUED -> "Queued"
                        TorrentState.CHECKING -> "Checking..."
                        TorrentState.DOWNLOADING -> "Downloading"
                        TorrentState.PAUSED -> "Paused"
                        TorrentState.SEEDING -> "Seeding"
                        TorrentState.FINISHED -> "Complete"
                        TorrentState.ERROR -> "Error"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = progressColor,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pause/Resume button
                if (download.state == TorrentState.DOWNLOADING || download.state == TorrentState.PAUSED) {
                    IconButton(
                        onClick = {
                            if (download.state == TorrentState.PAUSED) onResume() else onPause()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(RezonPurple.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (download.state == TorrentState.PAUSED)
                                Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (download.state == TorrentState.PAUSED)
                                "Resume" else "Pause",
                            tint = RezonPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Remove button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Empty state when no downloads
 */
@Composable
private fun EmptyDownloadsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = RezonPurple.copy(alpha = 0.3f),
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No Downloads",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap + to add a magnet link or torrent file",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Dialog to add magnet link
 */
@Composable
private fun AddMagnetDialog(
    magnetInput: String,
    onMagnetChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Magnet Link",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = magnetInput,
                onValueChange = onMagnetChange,
                label = { Text("Magnet URI") },
                placeholder = { Text("magnet:?xt=urn:btih:...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RezonPurple,
                    cursorColor = RezonPurple
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = magnetInput.trim().isNotEmpty()
            ) {
                Text("Add", color = RezonPurple, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
