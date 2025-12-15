package com.rezon.app.presentation.ui.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rezon.app.domain.model.Book
import com.rezon.app.presentation.ui.components.MiniPlayer
import com.rezon.app.presentation.ui.theme.ProgressFill
import com.rezon.app.presentation.ui.theme.ProgressTrack
import com.rezon.app.presentation.ui.theme.RezonCyan
import com.rezon.app.presentation.ui.theme.RezonPurple
import com.rezon.app.presentation.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch

/**
 * REZON Library Screen
 *
 * Features:
 * - Hierarchical folder navigation
 * - Grid and List view modes
 * - Progress filter tabs (Not Started, In Progress, Finished)
 * - Series grouping
 * - Mini player at bottom
 * - Search functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(1) } // Default to "In Progress"
    var isGridView by remember { mutableStateOf(true) }
    var useCompactCovers by remember { mutableStateOf(false) } // Smaller cover option
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption.RECENT) }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showAllBooks by remember { mutableStateOf(false) } // View All toggle

    val tabs = listOf("Not Started", "In Progress", "Finished")

    // Pager state for swipe navigation
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { tabs.size })

    // Sync tab selection with pager
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            selectedTabIndex = page
        }
    }

    // Sync pager with tab clicks
    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onNavigateToSettings = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                },
                onScanFolder = { viewModel.scanFolder() },
                onAddTorrent = { /* TODO */ },
                onCloudStorage = { /* TODO */ }
            )
        }
    ) {
        Scaffold(
            topBar = {
                LibraryTopBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    isSearchActive = isSearchActive,
                    onSearchActiveChange = { isSearchActive = it },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSettingsClick = onNavigateToSettings,
                    isGridView = isGridView,
                    onToggleViewMode = { isGridView = !isGridView },
                    showSortMenu = showSortMenu,
                    onSortMenuToggle = { showSortMenu = it },
                    currentSortOption = sortOption,
                    onSortOptionChange = { sortOption = it }
                )
            },
            floatingActionButton = {
                Box(
                    modifier = Modifier.padding(bottom = if (uiState.currentlyPlaying != null) 72.dp else 0.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = RezonPurple,
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // FAB Options Menu
                    DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AudioFile, null, Modifier.size(20.dp), tint = RezonPurple)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Add from device")
                                }
                            },
                            onClick = {
                                showFabMenu = false
                                viewModel.addFiles()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, null, Modifier.size(20.dp), tint = RezonPurple)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Scan folder")
                                }
                            },
                            onClick = {
                                showFabMenu = false
                                viewModel.scanFolder()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Link, null, Modifier.size(20.dp), tint = RezonPurple)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Add magnet/torrent")
                                }
                            },
                            onClick = {
                                showFabMenu = false
                                // TODO: Navigate to torrent screen
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Cloud, null, Modifier.size(20.dp), tint = RezonPurple)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Cloud storage")
                                }
                            },
                            onClick = {
                                showFabMenu = false
                                // TODO: Navigate to cloud storage
                            }
                        )
                    }
                }
            },
            bottomBar = {
                // Mini Player
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
                // Progress filter tabs with View All option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabRow(
                        selectedTabIndex = if (showAllBooks) -1 else selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        divider = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val count = when (index) {
                                0 -> uiState.notStartedCount
                                1 -> uiState.inProgressCount
                                else -> uiState.finishedCount
                            }
                            Tab(
                                selected = selectedTabIndex == index && !showAllBooks,
                                onClick = {
                                    showAllBooks = false
                                    selectedTabIndex = index
                                },
                                text = {
                                    Text(
                                        text = "$title ($count)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selectedTabIndex == index && !showAllBooks) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selectedContentColor = RezonPurple,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // View All button
                    IconButton(
                        onClick = { showAllBooks = !showAllBooks }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = "View All",
                            tint = if (showAllBooks) RezonPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Swipeable content with HorizontalPager
                if (showAllBooks) {
                    // Show all books
                    val allBooks = uiState.books.filter {
                        searchQuery.isEmpty() ||
                        it.title.contains(searchQuery, ignoreCase = true) ||
                        it.author.contains(searchQuery, ignoreCase = true)
                    }.let { books ->
                        when (sortOption) {
                            SortOption.RECENT -> books.sortedByDescending { it.lastPlayed ?: it.dateAdded }
                            SortOption.TITLE -> books.sortedBy { it.title.lowercase() }
                            SortOption.AUTHOR -> books.sortedBy { it.author.lowercase() }
                            SortOption.PROGRESS -> books.sortedByDescending { it.progress }
                            SortOption.DATE_ADDED -> books.sortedByDescending { it.dateAdded }
                        }
                    }

                    BooksContent(
                        books = allBooks,
                        isGridView = isGridView,
                        useCompactCovers = useCompactCovers,
                        onBookClick = { onNavigateToPlayer(it.id) },
                        onDeleteClick = { bookToDelete = it }
                    )
                } else {
                    // Swipeable pager for tabs
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val filteredBooks = when (page) {
                            0 -> uiState.books.filter { it.progress == 0f }
                            1 -> uiState.books.filter { it.progress > 0f && !it.isCompleted }
                            else -> uiState.books.filter { it.isCompleted }
                        }.filter {
                            searchQuery.isEmpty() ||
                            it.title.contains(searchQuery, ignoreCase = true) ||
                            it.author.contains(searchQuery, ignoreCase = true)
                        }.let { books ->
                            when (sortOption) {
                                SortOption.RECENT -> books.sortedByDescending { it.lastPlayed ?: it.dateAdded }
                                SortOption.TITLE -> books.sortedBy { it.title.lowercase() }
                                SortOption.AUTHOR -> books.sortedBy { it.author.lowercase() }
                                SortOption.PROGRESS -> books.sortedByDescending { it.progress }
                                SortOption.DATE_ADDED -> books.sortedByDescending { it.dateAdded }
                            }
                        }

                        BooksContent(
                            books = filteredBooks,
                            isGridView = isGridView,
                            useCompactCovers = useCompactCovers,
                            onBookClick = { onNavigateToPlayer(it.id) },
                            onDeleteClick = { bookToDelete = it }
                        )
                    }
                }
            }
        }
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
 * Sort options for the library
 */
enum class SortOption(val displayName: String) {
    RECENT("Recently Played"),
    TITLE("Title"),
    AUTHOR("Author"),
    PROGRESS("Progress"),
    DATE_ADDED("Date Added")
}

/**
 * Library top bar with search
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isGridView: Boolean,
    onToggleViewMode: () -> Unit,
    showSortMenu: Boolean,
    onSortMenuToggle: (Boolean) -> Unit,
    currentSortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = statusBarPadding.calculateTopPadding())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu button
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.size(28.dp)
                )
            }

            // Modern Logo
            RezonLogo(modifier = Modifier.weight(1f))

            // Search button
            IconButton(onClick = { onSearchActiveChange(true) }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(28.dp)
                )
            }

            // Sort button with dropdown
            Box {
                IconButton(onClick = { onSortMenuToggle(true) }) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        modifier = Modifier.size(28.dp)
                    )
                }

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
                                    color = if (option == currentSortOption) RezonPurple else MaterialTheme.colorScheme.onSurface
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

            // View mode toggle
            IconButton(onClick = onToggleViewMode) {
                Icon(
                    imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "Toggle View",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Search bar
        AnimatedVisibility(visible = isSearchActive) {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = { onSearchActiveChange(false) },
                active = false,
                onActiveChange = { },
                placeholder = { Text("Search books...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) { }
        }
    }
}

/**
 * Navigation drawer content
 */
@Composable
private fun DrawerContent(
    onNavigateToSettings: () -> Unit,
    onScanFolder: () -> Unit,
    onAddTorrent: () -> Unit,
    onCloudStorage: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Logo in drawer
        RezonLogo(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            size = LogoSize.LARGE
        )

        Spacer(modifier = Modifier.height(16.dp))

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(26.dp)) },
            label = { Text("Scan Folder", style = MaterialTheme.typography.bodyLarge) },
            selected = false,
            onClick = onScanFolder,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(26.dp)) },
            label = { Text("Add Torrent/Magnet", style = MaterialTheme.typography.bodyLarge) },
            selected = false,
            onClick = onAddTorrent,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
            },
            label = { Text("Cloud Storage", style = MaterialTheme.typography.bodyLarge) },
            selected = false,
            onClick = onCloudStorage,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(26.dp)) },
            label = { Text("Settings", style = MaterialTheme.typography.bodyLarge) },
            selected = false,
            onClick = onNavigateToSettings,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Reusable content for displaying books in grid or list
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
        EmptyState()
    } else if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (useCompactCovers) 3 else 2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(if (useCompactCovers) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (useCompactCovers) 12.dp else 16.dp)
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
 * Book item in grid view with 3-dot menu
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
        shape = RoundedCornerShape(if (isCompact) 12.dp else 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Cover art with menu button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isCompact) 0.75f else 1f)
            ) {
                AsyncImage(
                    model = book.coverUrl ?: book.localCoverPath,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = if (isCompact) 12.dp else 16.dp, topEnd = if (isCompact) 12.dp else 16.dp))
                )

                // Headphones icon if no cover
                if (book.coverUrl == null && book.localCoverPath == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = RezonPurple.copy(alpha = 0.5f),
                            modifier = Modifier.size(if (isCompact) 40.dp else 64.dp)
                        )
                    }
                }

                // 3-dot menu button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
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
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }

                // Progress overlay
                if (book.progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        LinearProgressIndicator(
                            progress = { book.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = RezonPurple,
                            trackColor = ProgressTrack
                        )
                    }
                }
            }

            // Book info
            Column(
                modifier = Modifier.padding(if (isCompact) 8.dp else 12.dp)
            ) {
                Text(
                    text = book.title,
                    style = if (isCompact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (isCompact) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = book.author,
                    style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Book item in list view
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
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
                            tint = RezonPurple.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { book.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = RezonPurple,
                    trackColor = ProgressTrack
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${book.progressPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // More button with dropdown
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        modifier = Modifier.size(24.dp)
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
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Delete",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
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
                tint = MaterialTheme.colorScheme.error,
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
                text = "Are you sure you want to delete \"$bookTitle\"? This will remove the book from your library but won't delete the file from your device."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Delete",
                    color = MaterialTheme.colorScheme.error,
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

/**
 * Empty state when no books
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                tint = RezonPurple.copy(alpha = 0.3f),
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No audiobooks yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add files or scan a folder to get started",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Logo size enum
 */
enum class LogoSize {
    SMALL, MEDIUM, LARGE
}

/**
 * Modern REZON Logo - High-quality tech company style
 * Inspired by Instagram/TikTok quality branding
 */
@Composable
private fun RezonLogo(
    modifier: Modifier = Modifier,
    logoSize: LogoSize = LogoSize.MEDIUM
) {
    val iconSize = when (logoSize) {
        LogoSize.SMALL -> 24.dp
        LogoSize.MEDIUM -> 32.dp
        LogoSize.LARGE -> 44.dp
    }

    val textSize = when (logoSize) {
        LogoSize.SMALL -> 20.sp
        LogoSize.MEDIUM -> 26.sp
        LogoSize.LARGE -> 34.sp
    }

    val letterSpacing = when (logoSize) {
        LogoSize.SMALL -> 1.sp
        LogoSize.MEDIUM -> 2.sp
        LogoSize.LARGE -> 3.sp
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo icon - Modern audio wave in gradient circle
        Box(
            modifier = Modifier
                .size(iconSize)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(iconSize / 4),
                    ambientColor = RezonPurple.copy(alpha = 0.3f),
                    spotColor = RezonPurple.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(iconSize / 4))
                .background(
                    Brush.linearGradient(
                        colors = listOf(RezonPurple, RezonCyan),
                        start = Offset(0f, 0f),
                        end = Offset(iconSize.value, iconSize.value)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Audio wave bars
            Canvas(
                modifier = Modifier
                    .size(iconSize * 0.6f)
            ) {
                val barWidth = size.width / 9
                val maxHeight = size.height * 0.8f
                val spacing = barWidth * 1.5f
                val startX = (size.width - (5 * barWidth + 4 * (spacing - barWidth))) / 2

                // Draw 5 bars with different heights
                val heights = listOf(0.4f, 0.7f, 1f, 0.7f, 0.4f)
                heights.forEachIndexed { index, heightFactor ->
                    val barHeight = maxHeight * heightFactor
                    val x = startX + index * spacing
                    val y = (size.height - barHeight) / 2

                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(if (logoSize == LogoSize.SMALL) 6.dp else 10.dp))

        // Logo text with gradient
        Text(
            text = "REZON",
            fontSize = textSize,
            fontWeight = FontWeight.Black,
            letterSpacing = letterSpacing,
            style = MaterialTheme.typography.headlineMedium.copy(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White,
                        Color.White.copy(alpha = 0.85f)
                    )
                )
            )
        )
    }
}
