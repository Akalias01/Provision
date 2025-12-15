package com.rezon.app.presentation.ui.screens.library

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rezon.app.domain.model.Book
import com.rezon.app.presentation.ui.components.*
import com.rezon.app.presentation.ui.theme.*
import com.rezon.app.presentation.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch

/**
 * REZON Library Screen
 *
 * Features:
 * - Integrated top menu with logo
 * - Grid and List view modes
 * - Progress filter tabs with swipe navigation
 * - Modern popup dialogs
 * - Mini player at bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTorrents: () -> Unit = {},
    onNavigateToCloud: () -> Unit = {},
    onNavigateToFolders: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(1) } // Default to "In Progress"
    var isGridView by remember { mutableStateOf(true) }
    var useCompactCovers by remember { mutableStateOf(true) } // Default to smaller covers
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption.RECENT) }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }

    // Dialog states
    var showAddBooksDialog by remember { mutableStateOf(false) }
    var showTorrentDialog by remember { mutableStateOf(false) }
    var showCloudDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var magnetInput by remember { mutableStateOf("") }

    // Theme state
    var currentTheme by remember { mutableStateOf("cyber_cyan") }
    var currentLogoStyle by remember { mutableStateOf(LogoStyle.WAVEFORM) }
    var currentSplashAnimation by remember { mutableStateOf("glitch_cyber") }

    val tabs = listOf("Not Started", "In Progress", "Finished")
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { tabs.size })

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addFilesFromUris(uris)
        }
    }

    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.scanFolderFromUri(it)
        }
    }

    // Sync tab selection with pager
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            selectedTabIndex = page
        }
    }

    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
    }

    // Check if library is empty
    val isLibraryEmpty = uiState.books.isEmpty()

    Scaffold(
        floatingActionButton = {
            if (!isLibraryEmpty) {
                Box(
                    modifier = Modifier.padding(bottom = if (uiState.currentlyPlaying != null) 72.dp else 0.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showAddBooksDialog = true },
                        containerColor = RezonCyan,
                        contentColor = Color.Black,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = uiState.currentlyPlaying != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                uiState.currentlyPlaying?.let { book ->
                    MiniPlayer(
                        book = book,
                        isPlaying = uiState.isPlaying,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onClick = { onNavigateToPlayer(book.id) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top header with logo and menu
            IntegratedHeader(
                logoStyle = currentLogoStyle,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                isSearchActive = isSearchActive,
                onSearchActiveChange = { isSearchActive = it },
                isGridView = isGridView,
                onToggleViewMode = { isGridView = !isGridView },
                showSortMenu = showSortMenu,
                onSortMenuToggle = { showSortMenu = it },
                currentSortOption = sortOption,
                onSortOptionChange = { sortOption = it },
                onAddFiles = { showAddBooksDialog = true },
                onTorrentDownloads = { showTorrentDialog = true },
                onCloudStorage = { showCloudDialog = true },
                onAppearance = { showAppearanceDialog = true }
            )

            if (isLibraryEmpty) {
                // Welcome/Empty state screen
                WelcomeScreen(
                    modifier = Modifier.fillMaxSize(),
                    logoStyle = currentLogoStyle,
                    onImportBooks = { showAddBooksDialog = true },
                    onDownloadFromTorrent = { showTorrentDialog = true }
                )
            } else {
                // Progress filter tabs - compact
                LibraryTabs(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    notStartedCount = uiState.notStartedCount,
                    inProgressCount = uiState.inProgressCount,
                    finishedCount = uiState.finishedCount,
                    onTabSelected = { selectedTabIndex = it }
                )

                // Book content - directly below tabs
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val filteredBooks = when (page) {
                        0 -> uiState.books.filter { it.progress == 0f }
                        1 -> uiState.books.filter { it.progress > 0f && !it.isCompleted }
                        else -> uiState.books.filter { it.isCompleted }
                    }
                    val sortedBooks = filterAndSortBooks(filteredBooks, searchQuery, sortOption)

                    BooksContent(
                        books = sortedBooks,
                        isGridView = isGridView,
                        useCompactCovers = useCompactCovers,
                        onBookClick = { onNavigateToPlayer(it.id) },
                        onDeleteClick = { bookToDelete = it }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showAddBooksDialog) {
        AddBooksDialog(
            onDismiss = { showAddBooksDialog = false },
            onAddFromDevice = {
                showAddBooksDialog = false
                viewModel.addFiles()
            },
            onScanFolder = {
                showAddBooksDialog = false
                folderPickerLauncher.launch(null)
            },
            onImportFromCloud = {
                showAddBooksDialog = false
                showCloudDialog = true
            },
            onSelectFile = {
                showAddBooksDialog = false
                filePickerLauncher.launch(arrayOf("audio/*", "application/epub+zip"))
            }
        )
    }

    if (showTorrentDialog) {
        TorrentDownloadsDialog(
            magnetInput = magnetInput,
            onMagnetInputChange = { magnetInput = it },
            onDismiss = { showTorrentDialog = false },
            onBrowseTorrentFiles = {
                showTorrentDialog = false
                onNavigateToTorrents()
            },
            onDownload = {
                // TODO: Implement torrent download
                showTorrentDialog = false
                magnetInput = ""
            }
        )
    }

    if (showCloudDialog) {
        CloudStorageDialog(
            isGoogleDriveConnected = false,
            isDropboxConnected = false,
            onDismiss = { showCloudDialog = false },
            onConnectGoogleDrive = {
                // Launch Google OAuth
                viewModel.connectGoogleDrive(context)
            },
            onConnectDropbox = {
                // Launch Dropbox OAuth
                viewModel.connectDropbox(context)
            },
            onDisconnectGoogleDrive = {},
            onDisconnectDropbox = {}
        )
    }

    if (showAppearanceDialog) {
        AppearanceDialog(
            currentTheme = currentTheme,
            currentLogoStyle = currentLogoStyle,
            currentSplashAnimation = currentSplashAnimation,
            onDismiss = { showAppearanceDialog = false },
            onThemeSelected = { currentTheme = it },
            onLogoStyleSelected = { currentLogoStyle = it },
            onSplashAnimationSelected = { currentSplashAnimation = it },
            onPreviewSplash = {
                // TODO: Show splash preview
            }
        )
    }

    // Delete confirmation dialog
    bookToDelete?.let { book ->
        DeleteBookDialog(
            bookTitle = book.title,
            onConfirm = {
                viewModel.deleteBook(book.id)
                bookToDelete = null
            },
            onDismiss = { bookToDelete = null }
        )
    }
}

/**
 * Integrated header with logo and menu items
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntegratedHeader(
    logoStyle: LogoStyle,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    isGridView: Boolean,
    onToggleViewMode: () -> Unit,
    showSortMenu: Boolean,
    onSortMenuToggle: (Boolean) -> Unit,
    currentSortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    onAddFiles: () -> Unit,
    onTorrentDownloads: () -> Unit,
    onCloudStorage: () -> Unit,
    onAppearance: () -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RezonBackground)
            .padding(top = statusBarPadding.calculateTopPadding())
    ) {
        // Logo at top center
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            RezonLogo(
                logoSize = LogoSize.MEDIUM,
                style = logoStyle,
                showText = true
            )
        }

        // Menu row with icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add Files
            MenuIconButton(
                icon = Icons.Outlined.FolderOpen,
                label = "Add",
                onClick = onAddFiles
            )

            // Torrent
            MenuIconButton(
                icon = Icons.Default.Download,
                label = "Torrent",
                onClick = onTorrentDownloads
            )

            // Cloud
            MenuIconButton(
                icon = Icons.Default.Cloud,
                label = "Cloud",
                onClick = onCloudStorage
            )

            // Appearance
            MenuIconButton(
                icon = Icons.Default.Palette,
                label = "Theme",
                onClick = onAppearance
            )

            // Search
            MenuIconButton(
                icon = Icons.Default.Search,
                label = "Search",
                onClick = { onSearchActiveChange(true) }
            )

            // Sort
            Box {
                MenuIconButton(
                    icon = Icons.Default.Sort,
                    label = "Sort",
                    onClick = { onSortMenuToggle(true) }
                )
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { onSortMenuToggle(false) }
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.displayName,
                                    fontWeight = if (option == currentSortOption) FontWeight.Bold else FontWeight.Normal,
                                    color = if (option == currentSortOption) RezonCyan else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onSortOptionChange(option)
                                onSortMenuToggle(false)
                            }
                        )
                    }
                }
            }

            // View toggle
            MenuIconButton(
                icon = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                label = "View",
                onClick = onToggleViewMode
            )
        }

        // Search bar (when active)
        AnimatedVisibility(visible = isSearchActive) {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = { onSearchActiveChange(false) },
                active = false,
                onActiveChange = { },
                placeholder = { Text("Search books...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = {
                        onSearchQueryChange("")
                        onSearchActiveChange(false)
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close search")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) { }
        }

        HorizontalDivider(color = RezonSurfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
    }
}

/**
 * Menu icon button with label
 */
@Composable
private fun MenuIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = RezonCyan,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = RezonOnSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

/**
 * Library tabs - compact with centered counts
 */
@Composable
private fun LibraryTabs(
    tabs: List<String>,
    selectedTabIndex: Int,
    notStartedCount: Int,
    inProgressCount: Int,
    finishedCount: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEachIndexed { index, title ->
            val count = when (index) {
                0 -> notStartedCount
                1 -> inProgressCount
                else -> finishedCount
            }
            val isSelected = selectedTabIndex == index

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) RezonCyan else RezonOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "($count)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) RezonCyan else RezonOnSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .background(RezonCyan, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

/**
 * Filter and sort books helper
 */
private fun filterAndSortBooks(
    books: List<Book>,
    searchQuery: String,
    sortOption: SortOption
): List<Book> {
    return books.filter {
        searchQuery.isEmpty() ||
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.author.contains(searchQuery, ignoreCase = true)
    }.let { filtered ->
        when (sortOption) {
            SortOption.RECENT -> filtered.sortedByDescending { it.lastPlayed ?: it.dateAdded }
            SortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortOption.AUTHOR -> filtered.sortedBy { it.author.lowercase() }
            SortOption.PROGRESS -> filtered.sortedByDescending { it.progress }
            SortOption.DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }
        }
    }
}

/**
 * Sort options
 */
enum class SortOption(val displayName: String) {
    RECENT("Recently Played"),
    TITLE("Title"),
    AUTHOR("Author"),
    PROGRESS("Progress"),
    DATE_ADDED("Date Added")
}

/**
 * Welcome screen for empty library
 */
@Composable
private fun WelcomeScreen(
    modifier: Modifier = Modifier,
    logoStyle: LogoStyle,
    onImportBooks: () -> Unit,
    onDownloadFromTorrent: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to REZON",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = RezonCyan
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Audiobooks Reimagined.\nEvery Word Resonates.",
            style = MaterialTheme.typography.bodyLarge,
            color = RezonOnSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Import button
        Button(
            onClick = onImportBooks,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RezonCyan
            )
        ) {
            Icon(
                Icons.Outlined.Upload,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Import Your First Book",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Torrent button
        OutlinedButton(
            onClick = onDownloadFromTorrent,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, RezonSurfaceVariant)
        ) {
            Icon(
                Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Download from Torrent",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Category cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CategoryCard(
                icon = Icons.Outlined.Headphones,
                title = "Audiobooks",
                subtitle = "MP3, M4B, and more",
                modifier = Modifier.weight(1f)
            )
            CategoryCard(
                icon = Icons.Outlined.LibraryBooks,
                title = "E-Books",
                subtitle = "EPUB format",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CategoryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = RezonSurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RezonCyan.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = RezonCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = RezonOnSurfaceVariant
            )
        }
    }
}

/**
 * Books content grid/list
 */
@Composable
private fun BooksContent(
    books: List<Book>,
    isGridView: Boolean,
    useCompactCovers: Boolean,
    onBookClick: (Book) -> Unit,
    onDeleteClick: (Book) -> Unit
) {
    if (books.isEmpty()) {
        EmptyTabState()
    } else if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (useCompactCovers) 3 else 2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(if (useCompactCovers) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (useCompactCovers) 10.dp else 14.dp)
        ) {
            items(books, key = { it.id }) { book ->
                BookGridItem(
                    book = book,
                    isCompact = useCompactCovers,
                    onClick = { onBookClick(book) },
                    onDeleteClick = { onDeleteClick(book) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(books, key = { it.id }) { book ->
                BookListItem(
                    book = book,
                    onClick = { onBookClick(book) },
                    onDeleteClick = { onDeleteClick(book) }
                )
            }
        }
    }
}

/**
 * Book grid item
 */
@Composable
private fun BookGridItem(
    book: Book,
    isCompact: Boolean = false,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = RezonSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
            ) {
                AsyncImage(
                    model = book.coverUrl ?: book.localCoverPath,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                )

                if (book.coverUrl == null && book.localCoverPath == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(RezonSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = RezonCyan.copy(alpha = 0.4f),
                            modifier = Modifier.size(if (isCompact) 32.dp else 48.dp)
                        )
                    }
                }

                // 3-dot menu - more transparent
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                Color.Black.copy(alpha = 0.35f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = RezonAccentRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete", color = RezonAccentRed)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }

                // Progress bar
                if (book.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { book.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = RezonCyan,
                        trackColor = ProgressTrack
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = RezonOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Book list item
 */
@Composable
private fun BookListItem(
    book: Book,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = RezonSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Smaller thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RezonSurface)
            ) {
                AsyncImage(
                    model = book.coverUrl ?: book.localCoverPath,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (book.coverUrl == null && book.localCoverPath == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = RezonCyan.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Progress indicator on cover
                if (book.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { book.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.BottomCenter),
                        color = RezonCyan,
                        trackColor = ProgressTrack
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = RezonOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Duration/year info
                Text(
                    text = "${book.year ?: ""} ${if (book.year != null) "•" else ""} ${book.formattedDuration}",
                    style = MaterialTheme.typography.labelSmall,
                    color = RezonOnSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Menu button
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = RezonOnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = RezonAccentRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete", color = RezonAccentRed)
                            }
                        },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }

            // Headphones icon
            Icon(
                Icons.Default.Headphones,
                contentDescription = null,
                tint = RezonOnSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Empty state for tabs
 */
@Composable
private fun EmptyTabState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                tint = RezonCyan.copy(alpha = 0.2f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No books in this category",
                style = MaterialTheme.typography.bodyLarge,
                color = RezonOnSurfaceVariant
            )
        }
    }
}

/**
 * Delete confirmation dialog
 */
@Composable
private fun DeleteBookDialog(
    bookTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = RezonAccentRed,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Delete Book",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"$bookTitle\"?"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Delete",
                    color = RezonAccentRed,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
