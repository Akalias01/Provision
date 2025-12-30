/*
 * LEGACY LIBRARY SCREEN - NON-GLASS VERSION
 *
 * This is the original library screen without glass morphism effects.
 * Kept for reference and potential rollback if needed.
 *
 * DEPRECATED: Use LibraryScreenGlass instead for the modern glass UI.
 * This version will be removed in a future release.
 */
package com.mossglen.reverie.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mossglen.reverie.R
import com.mossglen.reverie.data.Book
import com.mossglen.reverie.ui.components.ReverieDialog
import com.mossglen.reverie.ui.viewmodel.DisplayMode
import com.mossglen.reverie.ui.viewmodel.LibraryViewModel
import com.mossglen.reverie.ui.viewmodel.LibraryViewMode
import com.mossglen.reverie.ui.viewmodel.MasterFilter
import com.mossglen.reverie.ui.viewmodel.PlayerViewModel
import com.mossglen.reverie.ui.viewmodel.SortOption
import kotlinx.coroutines.launch

@Deprecated(
    message = "Use LibraryScreenGlass instead for modern glass morphism UI",
    replaceWith = ReplaceWith("LibraryScreenGlass")
)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel,
    accentColor: Color,
    isDarkTheme: Boolean,
    isReverieTheme: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenPlayer: (String) -> Unit,
    onSearchClick: () -> Unit,
    onEditClick: (String) -> Unit = {}
) {
    val libraryData by libraryViewModel.libraryData.collectAsState()
    val currentSort by libraryViewModel.sortOption.collectAsState()
    val currentMasterFilter by libraryViewModel.masterFilter.collectAsState()
    val showHeaders by libraryViewModel.showHeaders.collectAsState()
    val currentBook by playerViewModel.currentBook.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()

    // UI Colors based on theme
    val bgColor = if (isDarkTheme) Color(0xFF0A0A0A) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkTheme) Color(0xFF161618) else Color(0xFFFFFFFF)
    val textColor = if (isDarkTheme) Color.White else Color.Black

    val pagerState = rememberPagerState(pageCount = { 4 }, initialPage = 1) // 4 Tabs
    val scope = rememberCoroutineScope()
    var showSortDialog by remember { mutableStateOf(false) }
    var currentViewMode by remember { mutableStateOf(LibraryViewMode.LIST) }

    Scaffold(
        topBar = {
            // 3-ROW PREMIUM HEADER (combined row 1 & 2)
            Column(modifier = Modifier.background(bgColor)) {
                // ROW 1: Menu + Centered Logo + Sort + Search (all on one row)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, null, tint = textColor)
                    }

                    // Empty center spacer (no branding)
                    Spacer(modifier = Modifier.weight(1f))

                    // Sort & Search on the right
                    IconButton(onClick = { showSortDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, null, tint = textColor)
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Outlined.Search, null, tint = textColor)
                    }
                }

                // ROW 2: Status Tabs with counts - border highlight style
                val titles = listOf("Not started", "In progress", "Finished", "All")
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    edgePadding = 12.dp,
                    divider = {},
                    indicator = {} // No underline indicator
                ) {
                    titles.forEachIndexed { index, title ->
                        val count = if (index < libraryData.size) libraryData[index].size else 0
                        val isSelected = pagerState.currentPage == index
                        Tab(
                            selected = isSelected,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier.background(Color.Transparent)
                                            .border(1.5.dp, accentColor, RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            text = {
                                Text(
                                    "$title ($count)",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) textColor else Color.Gray
                                )
                            }
                        )
                    }
                }

                // ROW 3: Master Switch (Audio vs Read) & View Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // MASTER SWITCH (Audio vs Read)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(surfaceColor)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterIcon(
                            icon = Icons.Rounded.Headphones,
                            label = "Listen",
                            isSelected = currentMasterFilter == MasterFilter.AUDIO,
                            accentColor = accentColor,
                            textColor = textColor
                        ) {
                            libraryViewModel.setMasterFilter(MasterFilter.AUDIO)
                        }
                        FilterIcon(
                            icon = Icons.Outlined.MenuBook,
                            label = "Read",
                            isSelected = currentMasterFilter == MasterFilter.READ,
                            accentColor = accentColor,
                            textColor = textColor
                        ) {
                            libraryViewModel.setMasterFilter(MasterFilter.READ)
                        }
                    }

                    // View Toggle - cycles through LIST -> GRID -> RECENTS
                    IconButton(
                        onClick = {
                            currentViewMode = when (currentViewMode) {
                                LibraryViewMode.LIST -> LibraryViewMode.GRID
                                LibraryViewMode.GRID -> LibraryViewMode.RECENTS
                                LibraryViewMode.RECENTS -> LibraryViewMode.LIST
                                LibraryViewMode.SERIES -> LibraryViewMode.LIST
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(surfaceColor)
                            .size(40.dp)
                    ) {
                        Icon(
                            when (currentViewMode) {
                                LibraryViewMode.LIST -> Icons.AutoMirrored.Filled.ViewList
                                LibraryViewMode.GRID -> Icons.Default.GridView
                                LibraryViewMode.RECENTS -> Icons.Default.History
                                LibraryViewMode.SERIES -> Icons.Default.CollectionsBookmark
                            },
                            contentDescription = when (currentViewMode) {
                                LibraryViewMode.LIST -> "List View"
                                LibraryViewMode.GRID -> "Grid View"
                                LibraryViewMode.RECENTS -> "Recents View"
                                LibraryViewMode.SERIES -> "Series View"
                            },
                            tint = textColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        },
        containerColor = bgColor
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val books = if (page < libraryData.size) libraryData[page] else emptyList()

                if (books.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (currentMasterFilter == MasterFilter.AUDIO)
                                    Icons.Rounded.Headphones
                                else
                                    Icons.AutoMirrored.Outlined.LibraryBooks,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (currentMasterFilter == MasterFilter.AUDIO)
                                    "No audiobooks in this category"
                                else
                                    "No ebooks in this category",
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    // Sort books by recents if in RECENTS mode
                    val displayBooks = if (currentViewMode == LibraryViewMode.RECENTS) {
                        books.sortedByDescending { it.progress } // Sort by most recently read (has progress)
                    } else books

                    when (currentViewMode) {
                        LibraryViewMode.LIST, LibraryViewMode.RECENTS, LibraryViewMode.SERIES -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                // Add "Recents" header in RECENTS mode
                                if (currentViewMode == LibraryViewMode.RECENTS) {
                                    item {
                                        Text(
                                            "Recently Read",
                                            color = textColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }
                                }
                                items(displayBooks) { book ->
                                    BookItem(
                                        book = book,
                                        surfaceColor = surfaceColor,
                                        textColor = textColor,
                                        accentColor = accentColor,
                                        isCurrentlyPlaying = currentBook?.id == book.id && isPlaying,
                                        onCoverClick = { onOpenPlayer(book.id) },
                                        onPlayClick = { playerViewModel.playOrPauseBook(book) },
                                        onDelete = { libraryViewModel.deleteBook(book.id) },
                                        onEditClick = { onEditClick(book.id) },
                                        onMarkAs = { status -> libraryViewModel.markBookStatus(book, status) }
                                    )
                                }
                                item { Spacer(Modifier.height(80.dp)) }
                            }
                        }
                        LibraryViewMode.GRID -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(displayBooks) { book ->
                                    GridBookItem(
                                        book = book,
                                        textColor = textColor,
                                        accentColor = accentColor,
                                        isCurrentlyPlaying = currentBook?.id == book.id && isPlaying,
                                        onClick = { onOpenPlayer(book.id) },
                                        onPlayClick = { playerViewModel.playOrPauseBook(book) }
                                    )
                                }
                                item { Spacer(Modifier.height(80.dp)) }
                            }
                        }
                    }
                }
            }
        }

        // Sort Dialog
        if (showSortDialog) {
            ReverieDialog(title = "Sort Library", onDismiss = { showSortDialog = false }) {
                SortOptionItem("Recents", SortOption.RECENTS, currentSort, accentColor) {
                    libraryViewModel.setSort(SortOption.RECENTS)
                }
                SortOptionItem("Author", SortOption.AUTHOR, currentSort, accentColor) {
                    libraryViewModel.setSort(SortOption.AUTHOR)
                }
                SortOptionItem("Title", SortOption.TITLE, currentSort, accentColor) {
                    libraryViewModel.setSort(SortOption.TITLE)
                }
                SortOptionItem("Series", SortOption.SERIES, currentSort, accentColor) {
                    libraryViewModel.setSort(SortOption.SERIES)
                }
                SortOptionItem("Path", SortOption.PATH, currentSort, accentColor) {
                    libraryViewModel.setSort(SortOption.PATH)
                }

                HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { libraryViewModel.toggleHeaders() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (showHeaders) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        null,
                        tint = if (showHeaders) accentColor else Color.Gray
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Show headers", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun FilterIcon(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) accentColor else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Text(
            label,
            color = if (isSelected) accentColor else Color.Gray,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun SortOptionItem(
    label: String,
    option: SortOption,
    current: SortOption,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
        RadioButton(
            selected = (option == current),
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = color, unselectedColor = Color.Gray)
        )
    }
}

// Grid View Book Item
@Composable
fun GridBookItem(
    book: Book,
    textColor: Color,
    accentColor: Color,
    isCurrentlyPlaying: Boolean = false,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Square cover (no rounded corners)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            if (book.coverUrl != null) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2A2A2C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (book.format == "AUDIO") Icons.Rounded.Headphones else Icons.Outlined.MenuBook,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Progress overlay at bottom
            if (book.progress > 0 && book.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(book.progressPercent())
                            .fillMaxHeight()
                            .background(accentColor)
                    )
                }
            }

            // Playing indicator
            if (isCurrentlyPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Pause,
                        null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Title
        Text(
            text = book.title,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Author
        Text(
            text = book.author,
            color = Color.Gray,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BookItem(
    book: Book,
    surfaceColor: Color,
    textColor: Color,
    accentColor: Color,
    isCurrentlyPlaying: Boolean = false,
    onCoverClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDelete: () -> Unit,
    onEditClick: () -> Unit = {},
    onMarkAs: (String) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showMarkAsMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(surfaceColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover Art (completely square - no rounded corners)
        Box(
            modifier = Modifier
                .size(76.dp)
                .clickable { onCoverClick() }
        ) {
            if (book.coverUrl != null) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2A2A2C))
                ) {
                    Icon(
                        if (book.format == "AUDIO") Icons.Rounded.Headphones else Icons.Outlined.MenuBook,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        // Info Column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                book.title,
                color = textColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                fontSize = 16.sp
            )
            Text(
                book.author,
                color = Color.Gray,
                fontSize = 13.sp,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            // Progress bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF3A3A3C))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(book.progressPercent())
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }
        }

        // Actions Column
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF1C1C1E))
                ) {
                    DropdownMenuItem(
                        text = { Text("Play", color = Color.White) },
                        onClick = { showMenu = false; onPlayClick() },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null, tint = accentColor) }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit", color = Color.White) },
                        onClick = { showMenu = false; onEditClick() },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = accentColor) }
                    )
                    DropdownMenuItem(
                        text = { Text("Mark as...", color = Color.White) },
                        onClick = { showMenu = false; showMarkAsMenu = true },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = accentColor) }
                    )
                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text("Delete from Library", color = Color.White) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Gray) }
                    )
                    DropdownMenuItem(
                        text = { Text("Share", color = Color.White) },
                        onClick = { showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Share, null, tint = Color.Gray) }
                    )
                }

                // Mark As Submenu
                DropdownMenu(
                    expanded = showMarkAsMenu,
                    onDismissRequest = { showMarkAsMenu = false },
                    modifier = Modifier.background(Color(0xFF1C1C1E))
                ) {
                    DropdownMenuItem(
                        text = { Text("Not Started", color = Color.White) },
                        onClick = { showMarkAsMenu = false; onMarkAs("Unfinished") },
                        leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, null, tint = Color.Gray) }
                    )
                    DropdownMenuItem(
                        text = { Text("In Progress", color = Color.White) },
                        onClick = { showMarkAsMenu = false; onMarkAs("In Progress") },
                        leadingIcon = { Icon(Icons.Default.PlayCircle, null, tint = Color(0xFFFFC107)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Finished", color = Color.White) },
                        onClick = { showMarkAsMenu = false; onMarkAs("Finished") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50)) }
                    )
                }
            }
            // Play Icon
            Icon(
                if (isCurrentlyPlaying) Icons.Default.Pause
                else if (book.format == "AUDIO") Icons.Rounded.Headphones
                else Icons.Outlined.MenuBook,
                null,
                tint = if (isCurrentlyPlaying) accentColor else accentColor.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onPlayClick() }
            )
        }
    }
}
