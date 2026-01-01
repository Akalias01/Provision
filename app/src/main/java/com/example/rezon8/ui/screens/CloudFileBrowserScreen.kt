package com.mossglen.lithos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mossglen.lithos.data.cloud.CloudFileInfo
import com.mossglen.lithos.data.cloud.CloudSource
import com.mossglen.lithos.ui.theme.*
import com.mossglen.lithos.ui.viewmodel.CloudBrowserEvent
import com.mossglen.lithos.ui.viewmodel.CloudFileBrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudFileBrowserScreen(
    isDark: Boolean,
    isOLED: Boolean = false,
    reverieAccentColor: Color = GlassColors.LithosAccent,
    onNavigateBack: () -> Unit,
    viewModel: CloudFileBrowserViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val event by viewModel.event.collectAsState()
    val theme = glassTheme(isDark, isOLED)
    val focusManager = LocalFocusManager.current

    // Solid colors for consistent appearance - Use Lithos 3-mode system
    val screenBg = LithosUI.background(isDark, isOLED)
    val cardBg = if (isDark) LithosUI.SheetBackground else LithosUI.ElevatedBackgroundLight
    val accentColor = if (isOLED) reverieAccentColor else GlassColors.Interactive
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)

    var searchText by remember { mutableStateOf("") }

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleSignInResult(result)
    }

    // Handle events from ViewModel
    LaunchedEffect(event) {
        when (val currentEvent = event) {
            is CloudBrowserEvent.LaunchGoogleSignIn -> {
                googleSignInLauncher.launch(currentEvent.intent)
                viewModel.clearEvent()
            }
            null -> { /* No event to handle */ }
        }
    }

    // Note: Auto-connect and refresh is handled by ViewModel's init block

    // Handle back navigation
    BackHandler(enabled = state.pathHistory.size > 1 || state.isSearching) {
        if (state.isSearching) {
            searchText = ""
            viewModel.loadFiles()
        } else {
            viewModel.navigateBack()
        }
    }

    // Show snackbar for success/error messages
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 56.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = cardBg,
                    contentColor = textPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cloud Files",
                        style = GlassTypography.Title,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isSearching) {
                            searchText = ""
                            viewModel.loadFiles()
                        } else if (!viewModel.navigateBack()) {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cardBg
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Source Selector Tabs
                SourceTabs(
                    selectedSource = state.selectedSource,
                    isGoogleDriveConnected = state.isGoogleDriveConnected,
                    isDropboxConnected = state.isDropboxConnected,
                    isDark = isDark,
                    isOLED = isOLED,
                    onSourceSelected = { viewModel.selectSource(it) }
                )

                // Search Bar
                SearchBar(
                    query = searchText,
                    onQueryChange = { searchText = it },
                    onSearch = {
                        focusManager.clearFocus()
                        viewModel.search(searchText)
                    },
                    isDark = isDark,
                    isOLED = isOLED
                )

                // Breadcrumb Path
                if (!state.isSearching && state.pathHistory.size > 1) {
                    PathBreadcrumb(
                        pathHistory = state.pathHistory,
                        isDark = isDark,
                        isOLED = isOLED
                    )
                }

                // Connection prompt or file list
                when {
                    state.selectedSource == CloudSource.GOOGLE_DRIVE && !state.isGoogleDriveConnected -> {
                        ConnectionPrompt(
                            source = CloudSource.GOOGLE_DRIVE,
                            isDark = isDark,
                            isOLED = isOLED,
                            onConnect = { viewModel.connectGoogleDrive() }
                        )
                    }
                    state.selectedSource == CloudSource.DROPBOX && !state.isDropboxConnected -> {
                        ConnectionPrompt(
                            source = CloudSource.DROPBOX,
                            isDark = isDark,
                            isOLED = isOLED,
                            onConnect = { viewModel.connectDropbox() }
                        )
                    }
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    }
                    state.files.isEmpty() -> {
                        EmptyState(
                            isSearching = state.isSearching,
                            isDark = isDark
                        )
                    }
                    else -> {
                        FileListWithRefresh(
                            files = state.files,
                            downloadingFile = state.downloadingFile,
                            downloadProgress = state.downloadProgress,
                            isRefreshing = state.isLoading,
                            isDark = isDark,
                            isOLED = isOLED,
                            onRefresh = { viewModel.loadFiles() },
                            onFileClick = { file ->
                                if (file.isFolder) {
                                    viewModel.navigateToFolder(file)
                                } else if (file.isImportable) {
                                    viewModel.downloadAndImport(file)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceTabs(
    selectedSource: CloudSource,
    isGoogleDriveConnected: Boolean,
    isDropboxConnected: Boolean,
    isDark: Boolean,
    isOLED: Boolean = false,
    onSourceSelected: (CloudSource) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M, vertical = GlassSpacing.S),
        horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
    ) {
        SourceTab(
            name = "Google Drive",
            isSelected = selectedSource == CloudSource.GOOGLE_DRIVE,
            isConnected = isGoogleDriveConnected,
            isDark = isDark,
            isOLED = isOLED,
            modifier = Modifier.weight(1f),
            onClick = { onSourceSelected(CloudSource.GOOGLE_DRIVE) }
        )
        SourceTab(
            name = "Dropbox",
            isSelected = selectedSource == CloudSource.DROPBOX,
            isConnected = isDropboxConnected,
            isDark = isDark,
            isOLED = isOLED,
            modifier = Modifier.weight(1f),
            onClick = { onSourceSelected(CloudSource.DROPBOX) }
        )
    }
}

@Composable
private fun SourceTab(
    name: String,
    isSelected: Boolean,
    isConnected: Boolean,
    isDark: Boolean,
    isOLED: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val accentColor = LithosAmber // Use Lithos Amber for accent text/icons
    // Solid card background matching Chapters menu style - Use ReverieUI constants
    val cardBg = if (isDark) LithosUI.SheetBackground else LithosUI.SheetBackgroundLight
    val borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f)
    // Use neutral selection background (not amber)
    val selectionBg = LithosColors.selection(isDark, isOLED)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .clickable(onClick = onClick),
        color = if (isSelected) selectionBg else cardBg,
        shape = RoundedCornerShape(GlassShapes.Medium),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(GlassSpacing.M),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = name,
                style = GlassTypography.Body,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) accentColor else theme.textPrimary
            )
            Spacer(modifier = Modifier.width(GlassSpacing.XS))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) Color(0xFF22C55E) else theme.textSecondary)
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isDark: Boolean,
    isOLED: Boolean = false
) {
    val theme = glassTheme(isDark, isOLED)
    val accentColor = if (isOLED) GlassColors.LithosAccent else GlassColors.Interactive

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M, vertical = GlassSpacing.XS),
        placeholder = {
            Text(
                "Search files...",
                color = theme.textSecondary
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = theme.textSecondary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = theme.textSecondary
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = theme.glassBorder,
            focusedTextColor = theme.textPrimary,
            unfocusedTextColor = theme.textPrimary,
            cursorColor = accentColor
        ),
        shape = RoundedCornerShape(GlassShapes.Medium)
    )
}

@Composable
private fun PathBreadcrumb(
    pathHistory: List<String>,
    isDark: Boolean,
    isOLED: Boolean = false
) {
    val theme = glassTheme(isDark, isOLED)
    val accentColor = if (isOLED) GlassColors.LithosAccent else GlassColors.Interactive

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M, vertical = GlassSpacing.XS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(GlassSpacing.XS))
        Text(
            text = "/ ${pathHistory.drop(1).joinToString(" / ") { it.substringAfterLast("/").take(15) }}",
            style = GlassTypography.Caption,
            color = theme.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ConnectionPrompt(
    source: CloudSource,
    isDark: Boolean,
    isOLED: Boolean = false,
    onConnect: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val accentColor = if (isOLED) GlassColors.LithosAccent else GlassColors.Interactive
    val cardBg = if (isDark) LithosUI.SheetBackground else LithosUI.SheetBackgroundLight
    val sourceName = when (source) {
        CloudSource.GOOGLE_DRIVE -> "Google Drive"
        CloudSource.DROPBOX -> "Dropbox"
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(GlassSpacing.L)
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = theme.textSecondary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(GlassSpacing.M))
            Text(
                text = "Connect to $sourceName",
                style = GlassTypography.Headline,
                fontWeight = FontWeight.SemiBold,
                color = theme.textPrimary
            )
            Spacer(modifier = Modifier.height(GlassSpacing.XS))
            Text(
                text = "Sign in to browse and download your audiobooks and documents",
                style = GlassTypography.Body,
                color = theme.textSecondary
            )
            Spacer(modifier = Modifier.height(GlassSpacing.L))
            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = cardBg,
                    contentColor = accentColor
                ),
                shape = RoundedCornerShape(GlassShapes.Medium)
            ) {
                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = accentColor)
                Spacer(modifier = Modifier.width(GlassSpacing.S))
                Text("Connect $sourceName", color = accentColor)
            }
        }
    }
}

@Composable
private fun EmptyState(
    isSearching: Boolean,
    isDark: Boolean
) {
    val theme = glassTheme(isDark)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (isSearching) Icons.Default.SearchOff else Icons.Default.FolderOpen,
                contentDescription = null,
                tint = theme.textSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(GlassSpacing.M))
            Text(
                text = if (isSearching) "No results found" else "This folder is empty",
                style = GlassTypography.Body,
                color = theme.textSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileListWithRefresh(
    files: List<CloudFileInfo>,
    downloadingFile: String?,
    downloadProgress: Float,
    isRefreshing: Boolean,
    isDark: Boolean,
    isOLED: Boolean = false,
    onRefresh: () -> Unit,
    onFileClick: (CloudFileInfo) -> Unit
) {
    val accentColor = if (isOLED) GlassColors.LithosAccent else GlassColors.Interactive

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(GlassSpacing.M),
            verticalArrangement = Arrangement.spacedBy(GlassSpacing.XS),
            modifier = Modifier.fillMaxSize()
        ) {
            items(files, key = { it.id }) { file ->
                FileItem(
                    file = file,
                    isDownloading = file.name == downloadingFile,
                    downloadProgress = downloadProgress,
                    isDark = isDark,
                    isOLED = isOLED,
                    onClick = { onFileClick(file) }
                )
            }
        }
    }
}

@Composable
private fun FileItem(
    file: CloudFileInfo,
    isDownloading: Boolean,
    downloadProgress: Float,
    isDark: Boolean,
    isOLED: Boolean = false,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val accentColor = if (isOLED) GlassColors.LithosAccent else GlassColors.Interactive
    // Solid card background matching Chapters menu style - Use ReverieUI constants
    val cardBg = if (isDark) LithosUI.SheetBackground else LithosUI.SheetBackgroundLight

    // Check if file is compatible (folder or importable format)
    val isCompatible = file.isFolder || file.isImportable

    // Dim incompatible files to make them visually distinct
    val contentAlpha = if (isCompatible) 1f else 0.4f
    val textColor = theme.textPrimary.copy(alpha = contentAlpha)
    val secondaryTextColor = theme.textSecondary.copy(alpha = contentAlpha)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .clickable(enabled = !isDownloading && isCompatible, onClick = onClick),
        color = cardBg,
        shape = RoundedCornerShape(GlassShapes.Medium)
    ) {
        Row(
            modifier = Modifier.padding(GlassSpacing.M),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(GlassShapes.Small))
                    .background(getFileIconColor(file, isDark, isOLED).copy(alpha = if (isCompatible) 0.15f else 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFileIcon(file),
                    contentDescription = null,
                    tint = getFileIconColor(file, isDark, isOLED).copy(alpha = contentAlpha),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(GlassSpacing.M))

            // File Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = GlassTypography.Body,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!file.isFolder) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = file.formattedSize,
                            style = GlassTypography.Caption,
                            color = secondaryTextColor
                        )
                        if (file.formattedDate.isNotEmpty()) {
                            Text(
                                text = " • ",
                                style = GlassTypography.Caption,
                                color = secondaryTextColor
                            )
                            Text(
                                text = file.formattedDate,
                                style = GlassTypography.Caption,
                                color = secondaryTextColor
                            )
                        }
                        // Show "Unsupported" label for incompatible files
                        if (!isCompatible) {
                            Text(
                                text = " • Unsupported",
                                style = GlassTypography.Caption,
                                color = secondaryTextColor
                            )
                        }
                    }
                }
            }

            // Download indicator or action
            if (isDownloading) {
                CircularProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.size(24.dp),
                    color = accentColor,
                    strokeWidth = 2.dp
                )
            } else if (file.isFolder) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = theme.textSecondary
                )
            } else if (file.isImportable) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Download",
                    tint = accentColor
                )
            } else {
                // Show block icon for unsupported files
                Icon(
                    Icons.Default.Block,
                    contentDescription = "Unsupported format",
                    tint = theme.textSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun getFileIcon(file: CloudFileInfo) = when {
    file.isFolder -> Icons.Default.Folder
    file.isAudioBook -> Icons.Default.Headphones
    file.isDocument -> Icons.Default.Book
    file.isTorrent -> Icons.Default.CloudDownload
    else -> Icons.Default.InsertDriveFile
}

@Composable
private fun getFileIconColor(file: CloudFileInfo, isDark: Boolean, isOLED: Boolean = false): Color {
    val theme = glassTheme(isDark, isOLED)
    val accentColor = if (isOLED) GlassColors.LithosAccent else Color(0xFF60A5FA)
    return when {
        file.isFolder -> accentColor
        file.isAudioBook -> Color(0xFF22C55E) // Green
        file.isDocument -> Color(0xFFF59E0B) // Amber
        file.isTorrent -> Color(0xFFA855F7) // Purple
        else -> theme.textSecondary
    }
}
