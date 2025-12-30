package com.mossglen.reverie.ui.screens

import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mossglen.reverie.data.DownloadInfo
import com.mossglen.reverie.data.TorrentState
import com.mossglen.reverie.ui.theme.*
import com.mossglen.reverie.ui.viewmodel.SettingsViewModel
import com.mossglen.reverie.ui.viewmodel.TorrentViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    isDark: Boolean = true,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.ReverieAccent,
    onBack: () -> Unit,
    torrentViewModel: TorrentViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val view = LocalView.current
    val theme = glassTheme(isDark, isReverieDark)
    val downloads by torrentViewModel.activeDownloads.collectAsState()
    val error by torrentViewModel.error.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    // Settings state
    val wifiOnly by settingsViewModel.torrentWifiOnly.collectAsState()
    val maxDownloads by settingsViewModel.torrentMaxDownloads.collectAsState()
    val autoFetchMetadata by settingsViewModel.torrentAutoFetchMetadata.collectAsState()
    val seedAfterDownload by settingsViewModel.torrentSeedAfterDownload.collectAsState()

    // UI state
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var magnetLink by remember { mutableStateOf("") }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            torrentViewModel.clearError()
        }
    }

    // File picker for .torrent files
    val torrentFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            torrentViewModel.startFileDownload(it)
        }
    }

    // Swipe to go back state
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 150f
    val animatedOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipeOffset"
    )
    val swipeProgress = (swipeOffset / swipeThreshold).coerceIn(0f, 1f)

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
            .alpha(1f - swipeProgress * 0.3f)
            .background(theme.background)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffset > swipeThreshold) {
                            onBack()
                        } else {
                            swipeOffset = 0f
                        }
                    },
                    onDragCancel = { swipeOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset = (swipeOffset + dragAmount).coerceAtLeast(0f)
                    }
                )
            }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Torrent Downloads",
                            style = GlassTypography.Title,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = theme.textPrimary
                            )
                        }
                    },
                    actions = {
                        // Settings button
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = theme.textSecondary
                            )
                        }
                        // Add button
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Torrent",
                                tint = theme.interactive
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = theme.glassCard
                    )
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = theme.glassCard,
                        contentColor = theme.textPrimary,
                        actionColor = theme.interactive
                    )
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                if (downloads.isEmpty()) {
                    // Empty State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(GlassSpacing.L)
                        ) {
                            Icon(
                                Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                tint = theme.textSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(GlassSpacing.M))
                            Text(
                                "No Downloads",
                                style = GlassTypography.Headline,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.textPrimary
                            )
                            Spacer(modifier = Modifier.height(GlassSpacing.XS))
                            Text(
                                "Add a magnet link or torrent file to start",
                                style = GlassTypography.Body,
                                color = theme.textSecondary
                            )
                            Spacer(modifier = Modifier.height(GlassSpacing.L))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = theme.glassCard,
                                    contentColor = theme.interactive
                                ),
                                shape = RoundedCornerShape(GlassShapes.Medium)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = theme.interactive)
                                Spacer(modifier = Modifier.width(GlassSpacing.S))
                                Text("ADD TORRENT", fontWeight = FontWeight.SemiBold, color = theme.interactive)
                            }
                        }
                    }
                } else {
                    // Downloads List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(GlassSpacing.M),
                        verticalArrangement = Arrangement.spacedBy(GlassSpacing.S)
                    ) {
                        items(
                            items = downloads,
                            key = { it.id }
                        ) { downloadInfo ->
                            DownloadItem(
                                downloadInfo = downloadInfo,
                                isDark = isDark,
                                isReverieDark = isReverieDark,
                                onPause = { torrentViewModel.pauseDownload(downloadInfo.id) },
                                onResume = { torrentViewModel.resumeDownload(downloadInfo.id) },
                                onCancel = { torrentViewModel.cancelDownload(downloadInfo.id) }
                            )
                        }
                    }
                }
            }
        }

        // Add Torrent Dialog
        if (showAddDialog) {
            AddTorrentDialog(
                magnetLink = magnetLink,
                onMagnetLinkChange = { magnetLink = it },
                isDark = isDark,
                isReverieDark = isReverieDark,
                onAddMagnet = {
                    if (magnetLink.startsWith("magnet:")) {
                        torrentViewModel.startDownloadWithLink(magnetLink)
                        magnetLink = ""
                        showAddDialog = false
                    }
                },
                onAddFile = {
                    torrentFilePicker.launch("application/x-bittorrent")
                    showAddDialog = false
                },
                onPasteFromClipboard = {
                    clipboardManager.getText()?.text?.let { text ->
                        if (text.startsWith("magnet:")) {
                            magnetLink = text
                        }
                    }
                },
                onDismiss = {
                    showAddDialog = false
                    magnetLink = ""
                }
            )
        }

        // Settings Bottom Sheet
        if (showSettingsSheet) {
            // Solid dialog background - matches player dialogs throughout app
            val sheetBg = Color(0xFF1C1C1E)
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = sheetBg,
                shape = RoundedCornerShape(topStart = GlassShapes.Large, topEnd = GlassShapes.Large)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(GlassSpacing.M)
                ) {
                    Text(
                        "Download Settings",
                        style = GlassTypography.Headline,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary,
                        modifier = Modifier.padding(bottom = GlassSpacing.M)
                    )

                    // Wi-Fi Only Toggle
                    SettingsToggleRow(
                        title = "Wi-Fi Only",
                        subtitle = "Only download on Wi-Fi networks",
                        isEnabled = wifiOnly,
                        isDark = isDark,
                        isReverieDark = isReverieDark,
                        onToggle = { settingsViewModel.setTorrentWifiOnly(it) }
                    )

                    HorizontalDivider(
                        color = theme.glassBorder,
                        modifier = Modifier.padding(vertical = GlassSpacing.S)
                    )

                    // Max Downloads
                    SettingsValueRow(
                        title = "Max Concurrent Downloads",
                        value = "$maxDownloads",
                        isDark = isDark,
                        isReverieDark = isReverieDark,
                        options = listOf(1, 2, 3, 4, 5),
                        onValueChange = { settingsViewModel.setTorrentMaxDownloads(it) }
                    )

                    HorizontalDivider(
                        color = theme.glassBorder,
                        modifier = Modifier.padding(vertical = GlassSpacing.S)
                    )

                    // Auto-Fetch Metadata Toggle
                    SettingsToggleRow(
                        title = "Auto-Fetch Metadata",
                        subtitle = "Get cover art and synopsis after download",
                        isEnabled = autoFetchMetadata,
                        isDark = isDark,
                        isReverieDark = isReverieDark,
                        onToggle = { settingsViewModel.setTorrentAutoFetchMetadata(it) }
                    )

                    HorizontalDivider(
                        color = theme.glassBorder,
                        modifier = Modifier.padding(vertical = GlassSpacing.S)
                    )

                    // Seed After Download Toggle
                    SettingsToggleRow(
                        title = "Seed After Download",
                        subtitle = "Share with others after download completes (uses battery)",
                        isEnabled = seedAfterDownload,
                        isDark = isDark,
                        isReverieDark = isReverieDark,
                        onToggle = { settingsViewModel.setTorrentSeedAfterDownload(it) }
                    )

                    Spacer(modifier = Modifier.height(GlassSpacing.L))
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    downloadInfo: DownloadInfo,
    isDark: Boolean,
    isReverieDark: Boolean = false,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    var visible by remember { mutableStateOf(true) }

    // Auto-hide completed downloads after 5 seconds
    LaunchedEffect(downloadInfo.isFinished) {
        if (downloadInfo.isFinished) {
            delay(5000)
            visible = false
        }
    }

    val stateColor = when (downloadInfo.state) {
        TorrentState.DOWNLOADING -> theme.interactive
        TorrentState.SEEDING -> Color(0xFF22C55E)
        TorrentState.FINISHED -> Color(0xFF22C55E)
        TorrentState.PAUSED -> Color(0xFFF59E0B)
        TorrentState.ERROR -> Color(0xFFEF4444)
        else -> theme.textSecondary
    }

    val isPaused = downloadInfo.state == TorrentState.PAUSED
    val isActive = downloadInfo.state == TorrentState.DOWNLOADING ||
                   downloadInfo.state == TorrentState.DOWNLOADING_METADATA

    // Format speed nicely (KB/s or MB/s) - single line, no wrapping
    val formattedSpeed = remember(downloadInfo.downloadSpeed) {
        when {
            downloadInfo.downloadSpeed >= 1024 -> String.format("%.1f MB/s", downloadInfo.downloadSpeed / 1024f)
            downloadInfo.downloadSpeed > 0 -> "${downloadInfo.downloadSpeed} KB/s"
            else -> ""
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = theme.glassCard,
            shape = RoundedCornerShape(GlassShapes.Medium)
        ) {
            Row(
                modifier = Modifier.padding(GlassSpacing.M),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress Circle - Premium element
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { downloadInfo.progress / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = stateColor,
                        strokeWidth = 5.dp,
                        trackColor = theme.glassBorder.copy(alpha = 0.3f)
                    )
                    Text(
                        "${downloadInfo.progress.toInt()}%",
                        style = GlassTypography.Body,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(GlassSpacing.M))

                // Info Column - Fixed layout
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Title - single line with ellipsis
                    Text(
                        text = downloadInfo.name.ifEmpty { "Fetching metadata..." },
                        style = GlassTypography.Body,
                        fontWeight = FontWeight.Medium,
                        color = theme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status row - clean single line
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // State indicator dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(stateColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        // Status text - fixed width to prevent wrapping
                        Text(
                            text = when (downloadInfo.state) {
                                TorrentState.CHECKING -> "Checking"
                                TorrentState.DOWNLOADING_METADATA -> "Getting info"
                                TorrentState.DOWNLOADING -> formattedSpeed
                                TorrentState.SEEDING -> "Seeding"
                                TorrentState.PAUSED -> "Paused"
                                TorrentState.FINISHED -> "Complete"
                                TorrentState.ERROR -> "Error"
                            },
                            style = GlassTypography.Caption,
                            fontWeight = if (downloadInfo.state == TorrentState.DOWNLOADING) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (downloadInfo.state == TorrentState.DOWNLOADING) stateColor else theme.textSecondary,
                            maxLines = 1
                        )

                        // Peers - only show if we have peers
                        if (downloadInfo.numPeers > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${downloadInfo.numPeers} peers",
                                style = GlassTypography.Caption,
                                color = theme.textSecondary.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Action buttons - compact
                if (!downloadInfo.isFinished) {
                    if (isPaused) {
                        IconButton(onClick = onResume) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = theme.interactive
                            )
                        }
                    } else if (isActive) {
                        IconButton(onClick = onPause) {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = theme.textSecondary
                            )
                        }
                    }
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = theme.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTorrentDialog(
    magnetLink: String,
    onMagnetLinkChange: (String) -> Unit,
    isDark: Boolean,
    isReverieDark: Boolean = false,
    onAddMagnet: () -> Unit,
    onAddFile: () -> Unit,
    onPasteFromClipboard: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val focusManager = LocalFocusManager.current
    // Solid dialog background - matches player dialogs throughout app
    val dialogBg = Color(0xFF1C1C1E)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(GlassShapes.Large),
        title = {
            Text(
                "Add Torrent",
                style = GlassTypography.Headline,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary
            )
        },
        text = {
            Column {
                // Magnet Link Input
                OutlinedTextField(
                    value = magnetLink,
                    onValueChange = onMagnetLinkChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Paste magnet link...", color = theme.textSecondary)
                    },
                    trailingIcon = {
                        IconButton(onClick = onPasteFromClipboard) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = theme.interactive
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        if (magnetLink.startsWith("magnet:")) {
                            onAddMagnet()
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.interactive,
                        unfocusedBorderColor = theme.glassBorder,
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                        cursorColor = theme.interactive
                    ),
                    shape = RoundedCornerShape(GlassShapes.Small)
                )

                Spacer(modifier = Modifier.height(GlassSpacing.M))

                // Or divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = theme.glassBorder
                    )
                    Text(
                        "  or  ",
                        style = GlassTypography.Caption,
                        color = theme.textSecondary
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = theme.glassBorder
                    )
                }

                Spacer(modifier = Modifier.height(GlassSpacing.M))

                // Add from file button
                OutlinedButton(
                    onClick = onAddFile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GlassShapes.Small),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = theme.interactive
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = SolidColor(theme.interactive.copy(alpha = 0.5f))
                    )
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(GlassSpacing.S))
                    Text("Select .torrent File")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onAddMagnet,
                enabled = magnetLink.startsWith("magnet:")
            ) {
                Text(
                    "Add",
                    color = if (magnetLink.startsWith("magnet:")) theme.interactive else theme.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = theme.textSecondary)
            }
        }
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    isDark: Boolean,
    isReverieDark: Boolean = false,
    onToggle: (Boolean) -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GlassSpacing.XS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = GlassTypography.Body,
                color = theme.textPrimary
            )
            Text(
                text = subtitle,
                style = GlassTypography.Caption,
                color = theme.textSecondary
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = theme.interactive,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = theme.glassBorder
            )
        )
    }
}

@Composable
private fun SettingsValueRow(
    title: String,
    value: String,
    isDark: Boolean,
    isReverieDark: Boolean = false,
    options: List<Int>,
    onValueChange: (Int) -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = GlassSpacing.XS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = GlassTypography.Body,
            color = theme.textPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = GlassTypography.Body,
                color = theme.interactive
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = theme.textSecondary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1C1C1E))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text("$option", color = theme.textPrimary) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
