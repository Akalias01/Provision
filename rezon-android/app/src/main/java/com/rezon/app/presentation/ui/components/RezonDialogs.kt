package com.rezon.app.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rezon.app.presentation.ui.theme.*

/**
 * REZON Modal Dialogs
 * Modern popup dialogs matching the reference design
 */

// ==================== ADD BOOKS DIALOG ====================

@Composable
fun AddBooksDialog(
    onDismiss: () -> Unit,
    onAddFromDevice: () -> Unit,
    onScanFolder: () -> Unit,
    onImportFromCloud: () -> Unit,
    onSelectFile: () -> Unit = onAddFromDevice
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = RezonSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Books",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = RezonOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Select File Button
                Button(
                    onClick = onSelectFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RezonCyan
                    )
                ) {
                    Icon(
                        Icons.Outlined.FileOpen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Select File",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Browse and select individual audiobook files",
                    style = MaterialTheme.typography.bodySmall,
                    color = RezonOnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scan Folder Button
                Button(
                    onClick = onScanFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RezonCyan
                    )
                ) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Scan Folder for Compatible Content",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Scan an entire folder to find audiobooks, ebooks, and PDFs",
                    style = MaterialTheme.typography.bodySmall,
                    color = RezonOnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Import from Cloud Storage
                Button(
                    onClick = onImportFromCloud,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RezonPurple
                    )
                ) {
                    Icon(
                        Icons.Outlined.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Import from Cloud Storage",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Connect Google Drive or Dropbox to import files",
                    style = MaterialTheme.typography.bodySmall,
                    color = RezonOnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Format type cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FormatCard(
                        icon = Icons.Outlined.Headphones,
                        title = "Audio",
                        subtitle = "MP3, M4B,\nAAC, OGG",
                        modifier = Modifier.weight(1f)
                    )
                    FormatCard(
                        icon = Icons.Outlined.MenuBook,
                        title = "EPUB",
                        subtitle = "EPUB format",
                        modifier = Modifier.weight(1f)
                    )
                    FormatCard(
                        icon = Icons.Outlined.Description,
                        title = "PDF/DOC",
                        subtitle = "PDF, DOC,\nDOCX",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = RezonSurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = RezonCyan
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = RezonOnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

// ==================== TORRENT DOWNLOADS DIALOG ====================

@Composable
fun TorrentDownloadsDialog(
    magnetInput: String,
    onMagnetInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onBrowseTorrentFiles: () -> Unit,
    onDownload: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = RezonSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Torrent Downloads",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = RezonOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Browse for Torrent Files section - centered
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large centered + button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(RezonCyan)
                            .clickable { onBrowseTorrentFiles() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Torrent",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Browse for Torrent Files",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Select .torrent files from your device",
                        style = MaterialTheme.typography.bodySmall,
                        color = RezonOnSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = RezonSurfaceVariant)

                Spacer(modifier = Modifier.height(24.dp))

                // Enter Magnet Link section
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Link,
                        contentDescription = null,
                        tint = RezonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Enter Magnet Link",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Paste a magnet link to download audiobooks or ebooks. Only download content you have the right to access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RezonOnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = magnetInput,
                    onValueChange = onMagnetInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "magnet:?xt=urn:btih:...",
                            color = RezonOnSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RezonCyan,
                        unfocusedBorderColor = RezonSurfaceVariant,
                        cursorColor = RezonCyan,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, RezonSurfaceVariant)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RezonCyan
                        ),
                        enabled = magnetInput.isNotBlank()
                    ) {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download", color = Color.Black)
                    }
                }
            }
        }
    }
}

// ==================== CLOUD STORAGE DIALOG ====================

@Composable
fun CloudStorageDialog(
    isGoogleDriveConnected: Boolean,
    isDropboxConnected: Boolean,
    onDismiss: () -> Unit,
    onConnectGoogleDrive: () -> Unit,
    onConnectDropbox: () -> Unit,
    onDisconnectGoogleDrive: () -> Unit,
    onDisconnectDropbox: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = RezonSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cloud Storage",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = RezonOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Connect your cloud storage to import audiobooks directly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RezonOnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Google Drive Card
                CloudProviderCard(
                    icon = Icons.Default.Cloud,
                    iconTint = Color(0xFF4285F4),
                    name = "Google Drive",
                    isConnected = isGoogleDriveConnected,
                    onConnect = onConnectGoogleDrive,
                    onDisconnect = onDisconnectGoogleDrive
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Dropbox Card
                CloudProviderCard(
                    icon = Icons.Default.Cloud,
                    iconTint = Color(0xFF0061FF),
                    name = "Dropbox",
                    isConnected = isDropboxConnected,
                    onConnect = onConnectDropbox,
                    onDisconnect = onDisconnectDropbox
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Footer text
                Text(
                    text = "Your files remain in the cloud. We only download what you select.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RezonOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CloudProviderCard(
    icon: ImageVector,
    iconTint: Color,
    name: String,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = RezonSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Name and status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = if (isConnected) "Connected" else "Not connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConnected) RezonAccentGreen else RezonOnSurfaceVariant
                )
            }

            // Connect/Disconnect button
            Button(
                onClick = if (isConnected) onDisconnect else onConnect,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) RezonAccentRed else RezonCyan
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isConnected) "Disconnect" else "Connect",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isConnected) Color.White else Color.Black
                )
            }
        }
    }
}

// ==================== PLAYBACK SPEED DIALOG ====================

@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onSpeedSelected: (Float) -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = RezonSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = RezonOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Speed options - vertical scrollable list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 280.dp)
                ) {
                    items(speeds) { speed ->
                        SpeedChip(
                            speed = speed,
                            isSelected = currentSpeed == speed,
                            onClick = {
                                onSpeedSelected(speed)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedChip(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) RezonCyan else RezonSurfaceVariant,
        border = if (isSelected) null else BorderStroke(1.dp, RezonSurfaceLight)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatSpeed(speed),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.Black else Color.White
            )
        }
    }
}

private fun formatSpeed(speed: Float): String {
    return if (speed == speed.toLong().toFloat()) {
        "${speed.toInt()}x"
    } else {
        "${speed}x"
    }
}

// ==================== ADD BOOKMARK DIALOG ====================

@Composable
fun AddBookmarkDialog(
    currentPosition: Long,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = RezonSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Bookmark",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = RezonOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Position display
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = RezonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Position: ${formatTime(currentPosition)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RezonOnSurface
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Note input
                Text(
                    text = "Add a note (optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = {
                        Text(
                            "What's happening at this moment?",
                            color = RezonOnSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RezonCyan,
                        unfocusedBorderColor = RezonSurfaceVariant,
                        cursorColor = RezonCyan,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, RezonSurfaceVariant)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(note) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RezonCyan
                        )
                    ) {
                        Icon(
                            Icons.Outlined.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save", color = Color.Black)
                    }
                }
            }
        }
    }
}

// ==================== SLEEP TIMER DIALOG ====================

@Composable
fun SleepTimerDialog(
    currentTimer: Long?,
    onDismiss: () -> Unit,
    onTimerSelected: (Long?) -> Unit
) {
    val timerOptions = listOf(
        5L to "5 minutes",
        10L to "10 minutes",
        15L to "15 minutes",
        30L to "30 minutes",
        45L to "45 minutes",
        60L to "1 hour",
        90L to "1.5 hours",
        120L to "2 hours"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = RezonSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SleepTimerIcon(
                            modifier = Modifier.size(22.dp),
                            tint = RezonCyan,
                            isActive = currentTimer != null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sleep Timer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = RezonOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (currentTimer != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Active: ${currentTimer / 60000} min left",
                        style = MaterialTheme.typography.bodySmall,
                        color = RezonCyan
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Timer options - scrollable list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 260.dp)
                ) {
                    items(timerOptions) { (minutes, label) ->
                        TimerOptionItem(
                            label = label,
                            isSelected = currentTimer == minutes * 60 * 1000,
                            onClick = {
                                onTimerSelected(minutes * 60 * 1000)
                                onDismiss()
                            }
                        )
                    }

                    // End of chapter option
                    item {
                        TimerOptionItem(
                            label = "End of chapter",
                            isSelected = false,
                            onClick = {
                                onTimerSelected(-1) // Special value for end of chapter
                                onDismiss()
                            }
                        )
                    }

                    // Cancel timer option
                    if (currentTimer != null) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = {
                                    onTimerSelected(null)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = RezonAccentRed
                                ),
                                border = BorderStroke(1.dp, RezonAccentRed),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) RezonCyan else RezonSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Color.Black else Color.White,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ==================== SLEEP TIMER ICON ====================

@Composable
fun SleepTimerIcon(
    modifier: Modifier = Modifier,
    tint: Color = RezonOnSurfaceVariant,
    isActive: Boolean = false
) {
    val color = if (isActive) RezonCyan else tint

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw clock circle
            drawCircle(
                color = color,
                radius = size.minDimension / 2 - 2.dp.toPx(),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            // Draw clock tick at top
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(size.width / 2, 4.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(size.width / 2, 7.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }
        Text(
            text = "Z",
            fontWeight = FontWeight.Black,
            fontSize = (modifier.toString().filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toInt()?.div(2) ?: 12).sp,
            color = color
        )
    }
}

// ==================== UTILITY FUNCTIONS ====================

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
