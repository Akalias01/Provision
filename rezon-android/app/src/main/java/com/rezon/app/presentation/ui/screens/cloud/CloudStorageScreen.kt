package com.rezon.app.presentation.ui.screens.cloud

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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rezon.app.presentation.ui.theme.PlayerGradientEnd
import com.rezon.app.presentation.ui.theme.PlayerGradientStart
import com.rezon.app.presentation.ui.theme.RezonCyan
import com.rezon.app.presentation.ui.theme.RezonPurple
import com.rezon.app.presentation.viewmodel.CloudStorageViewModel

/**
 * Cloud provider type
 */
enum class CloudProvider {
    GOOGLE_DRIVE,
    DROPBOX
}

/**
 * Cloud account data
 */
data class CloudAccount(
    val id: String,
    val provider: CloudProvider,
    val email: String,
    val isConnected: Boolean,
    val isSyncing: Boolean = false,
    val lastSynced: Long? = null,
    val folderPath: String? = null
)

/**
 * REZON Cloud Storage Screen
 *
 * Manages cloud storage integrations:
 * - Connect to Google Drive
 * - Connect to Dropbox
 * - Sync audiobooks from cloud folders
 * - Manage connected accounts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudStorageScreen(
    onNavigateBack: () -> Unit,
    viewModel: CloudStorageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = RezonPurple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Cloud Storage",
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
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header info
                item {
                    Text(
                        text = "Connect your cloud storage to access audiobooks from anywhere",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Cloud providers
                item {
                    Text(
                        text = "CLOUD SERVICES",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = RezonPurple,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Google Drive
                item {
                    CloudProviderCard(
                        provider = CloudProvider.GOOGLE_DRIVE,
                        providerName = "Google Drive",
                        isConnected = uiState.googleDriveConnected,
                        isSyncing = uiState.googleDriveSyncing,
                        accountEmail = uiState.googleDriveEmail,
                        onConnect = { viewModel.connectGoogleDrive() },
                        onDisconnect = { viewModel.disconnectGoogleDrive() },
                        onSync = { viewModel.syncGoogleDrive() }
                    )
                }

                // Dropbox
                item {
                    CloudProviderCard(
                        provider = CloudProvider.DROPBOX,
                        providerName = "Dropbox",
                        isConnected = uiState.dropboxConnected,
                        isSyncing = uiState.dropboxSyncing,
                        accountEmail = uiState.dropboxEmail,
                        onConnect = { viewModel.connectDropbox() },
                        onDisconnect = { viewModel.disconnectDropbox() },
                        onSync = { viewModel.syncDropbox() }
                    )
                }

                // Sync Settings
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SYNC SETTINGS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = RezonPurple,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column {
                            // Auto-sync toggle
                            SyncSettingItem(
                                title = "Auto-sync on startup",
                                subtitle = "Automatically check for new files when app starts",
                                checked = uiState.autoSyncEnabled,
                                onCheckedChange = { viewModel.setAutoSync(it) }
                            )

                            // Wi-Fi only toggle
                            SyncSettingItem(
                                title = "Sync over Wi-Fi only",
                                subtitle = "Don't use mobile data for syncing",
                                checked = uiState.wifiOnlySync,
                                onCheckedChange = { viewModel.setWifiOnlySync(it) }
                            )

                            // Download files toggle
                            SyncSettingItem(
                                title = "Auto-download new files",
                                subtitle = "Automatically download newly synced audiobooks",
                                checked = uiState.autoDownloadEnabled,
                                onCheckedChange = { viewModel.setAutoDownload(it) }
                            )
                        }
                    }
                }

                // Supported formats info
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = RezonPurple.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Supported Formats",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = RezonPurple
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "MP3, M4A, M4B, AAC, OGG, OPUS, FLAC, WAV, WMA, EPUB, PDF",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cloud provider card
 */
@Composable
private fun CloudProviderCard(
    provider: CloudProvider,
    providerName: String,
    isConnected: Boolean,
    isSyncing: Boolean,
    accountEmail: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit
) {
    val iconColor = when (provider) {
        CloudProvider.GOOGLE_DRIVE -> Color(0xFF4285F4) // Google blue
        CloudProvider.DROPBOX -> Color(0xFF0061FF) // Dropbox blue
    }

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
            // Provider icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Provider info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = providerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isConnected && accountEmail != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = RezonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = accountEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isSyncing) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Syncing...",
                            style = MaterialTheme.typography.labelSmall,
                            color = RezonPurple,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = "Not connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action buttons
            if (isConnected) {
                // Sync button
                IconButton(
                    onClick = onSync,
                    enabled = !isSyncing,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(RezonPurple.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        tint = if (isSyncing)
                            MaterialTheme.colorScheme.outline
                        else
                            RezonPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Disconnect button
                IconButton(
                    onClick = onDisconnect,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Disconnect",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                // Connect button
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = iconColor
                    ),
                    modifier = Modifier.clickable(onClick = onConnect)
                ) {
                    Text(
                        text = "Connect",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Sync setting toggle item
 */
@Composable
private fun SyncSettingItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RezonPurple
            )
        )
    }
}
