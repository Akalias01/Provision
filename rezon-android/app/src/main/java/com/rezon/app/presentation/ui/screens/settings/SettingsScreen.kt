package com.rezon.app.presentation.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rezon.app.presentation.ui.theme.ProgressFill
import com.rezon.app.presentation.ui.theme.ProgressTrack
import com.rezon.app.presentation.ui.theme.RezonPurple
import com.rezon.app.presentation.viewmodel.SettingsViewModel

/**
 * REZON Settings Screen
 *
 * Comprehensive settings with categories:
 * - Downloads
 * - Library
 * - Player
 * - Audio
 * - About
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Audio file picker for adding books from device
    val audioFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addBooksFromDevice(uris)
            Toast.makeText(
                context,
                "Added ${uris.size} file(s) to library",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Downloads Section
            item {
                SettingsSection(title = "DOWNLOADS") {
                    SwitchSettingItem(
                        icon = Icons.Default.Download,
                        title = "Download over Wi-Fi only",
                        subtitle = "Don't use mobile data for downloads",
                        checked = uiState.downloadOverWifiOnly,
                        onCheckedChange = { viewModel.setDownloadOverWifiOnly(it) }
                    )
                }
            }

            // Library Section
            item {
                SettingsSection(title = "LIBRARY") {
                    ClickableSettingItem(
                        icon = Icons.Default.AddCircle,
                        title = "Add book from device",
                        subtitle = "Select audiobook files from your device",
                        onClick = {
                            audioFilePicker.launch(arrayOf(
                                "audio/mpeg",
                                "audio/mp4",
                                "audio/x-m4a",
                                "audio/x-m4b",
                                "audio/flac",
                                "audio/ogg",
                                "audio/wav",
                                "application/epub+zip",
                                "application/pdf"
                            ))
                        }
                    )
                    SwitchSettingItem(
                        icon = Icons.Default.Folder,
                        title = "Scan folders on startup",
                        subtitle = "Automatically scan configured folders",
                        checked = uiState.scanFoldersOnStartup,
                        onCheckedChange = { viewModel.setScanFoldersOnStartup(it) }
                    )
                    SwitchSettingItem(
                        icon = Icons.Default.LibraryBooks,
                        title = "Delete if missing",
                        subtitle = "Remove books from library if file is deleted",
                        checked = uiState.deleteIfMissing,
                        onCheckedChange = { viewModel.setDeleteIfMissing(it) }
                    )
                }
            }

            // Player Section
            item {
                SettingsSection(title = "PLAYER") {
                    SliderSettingItem(
                        icon = Icons.Default.Timer,
                        title = "Skip backward",
                        value = uiState.skipBackwardSeconds.toFloat(),
                        valueRange = 5f..60f,
                        steps = 10,
                        valueLabel = "${uiState.skipBackwardSeconds}s",
                        onValueChange = { viewModel.setSkipBackward(it.toInt()) }
                    )
                    SliderSettingItem(
                        icon = Icons.Default.Timer,
                        title = "Skip forward",
                        value = uiState.skipForwardSeconds.toFloat(),
                        valueRange = 5f..60f,
                        steps = 10,
                        valueLabel = "${uiState.skipForwardSeconds}s",
                        onValueChange = { viewModel.setSkipForward(it.toInt()) }
                    )
                    SliderSettingItem(
                        icon = Icons.Default.Restore,
                        title = "Skip after pause",
                        value = uiState.skipAfterPauseSeconds.toFloat(),
                        valueRange = 0f..30f,
                        steps = 5,
                        valueLabel = "${uiState.skipAfterPauseSeconds}s",
                        onValueChange = { viewModel.setSkipAfterPause(it.toInt()) }
                    )
                    SwitchSettingItem(
                        icon = Icons.Default.PlayCircle,
                        title = "Keep playback service active",
                        subtitle = "Faster resume but uses more battery",
                        checked = uiState.keepPlaybackServiceActive,
                        onCheckedChange = { viewModel.setKeepPlaybackServiceActive(it) }
                    )
                }
            }

            // Audio Section
            item {
                SettingsSection(title = "AUDIO") {
                    ClickableSettingItem(
                        icon = Icons.Default.Equalizer,
                        title = "Equalizer",
                        subtitle = "5-band audio equalizer",
                        onClick = onNavigateToEqualizer
                    )
                    SwitchSettingItem(
                        icon = Icons.Default.Headphones,
                        title = "Voice boost",
                        subtitle = "Enhance vocal frequencies (1-4 kHz)",
                        checked = uiState.voiceBoostEnabled,
                        onCheckedChange = { viewModel.setVoiceBoost(it) }
                    )
                    SwitchSettingItem(
                        icon = Icons.Default.Headphones,
                        title = "Silence skipping",
                        subtitle = "Automatically skip silent parts",
                        checked = uiState.silenceSkippingEnabled,
                        onCheckedChange = { viewModel.setSilenceSkipping(it) }
                    )
                    SwitchSettingItem(
                        icon = Icons.Default.Headphones,
                        title = "Mono audio",
                        subtitle = "Mix stereo to mono (for single earbud)",
                        checked = uiState.monoAudioEnabled,
                        onCheckedChange = { viewModel.setMonoAudio(it) }
                    )
                }
            }

            // Debug Section
            item {
                SettingsSection(title = "DEBUGGING") {
                    SwitchSettingItem(
                        icon = Icons.Default.Code,
                        title = "File logging",
                        subtitle = "Save debug logs to file",
                        checked = uiState.fileLoggingEnabled,
                        onCheckedChange = { viewModel.setFileLogging(it) }
                    )
                }
            }

            // About Section
            item {
                SettingsSection(title = "") {
                    ClickableSettingItem(
                        icon = Icons.Default.Restore,
                        title = "Backup & Restore",
                        subtitle = "Export or import app data",
                        onClick = { /* TODO */ }
                    )
                    ClickableSettingItem(
                        icon = Icons.Default.Help,
                        title = "Help",
                        subtitle = "FAQ and troubleshooting",
                        onClick = { /* TODO */ }
                    )
                    ClickableSettingItem(
                        icon = Icons.Default.Info,
                        title = "About",
                        subtitle = "REZON v2.0.1",
                        onClick = { /* TODO */ }
                    )
                }
            }
        }
    }
}

/**
 * Settings section container
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = RezonPurple,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * Switch setting item
 */
@Composable
private fun SwitchSettingItem(
    icon: ImageVector,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RezonPurple,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
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

/**
 * Slider setting item
 */
@Composable
private fun SliderSettingItem(
    icon: ImageVector,
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RezonPurple,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = RezonPurple
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChange(sliderValue) },
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = RezonPurple,
                activeTrackColor = ProgressFill,
                inactiveTrackColor = ProgressTrack
            ),
            modifier = Modifier.padding(start = 42.dp)
        )
    }
}

/**
 * Clickable setting item
 */
@Composable
private fun ClickableSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RezonPurple,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
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
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}
