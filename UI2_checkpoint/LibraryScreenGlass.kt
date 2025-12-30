package com.example.rezon8.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import android.content.Intent
import com.example.rezon8.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.rezon8.data.Book
import com.example.rezon8.data.groupBySeries
import com.example.rezon8.haptics.HapticType
import com.example.rezon8.haptics.performHaptic
import com.example.rezon8.ui.components.*
import com.example.rezon8.ui.theme.*
import com.example.rezon8.ui.viewmodel.LibraryViewModel
import com.example.rezon8.ui.viewmodel.LibraryViewMode
import com.example.rezon8.ui.viewmodel.MasterFilter
import com.example.rezon8.ui.viewmodel.SortOption
import androidx.hilt.navigation.compose.hiltViewModel as hiltViewModelCompose
import com.example.rezon8.ui.viewmodel.SeriesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * REZON8 Glass - Library Screen
 *
 * Modern library with:
 * - Pull-to-refresh
 * - Swipeable tabs (HorizontalPager) - 4 tabs: Not Started, In Progress, Finished, All
 * - Master Filter (Audio vs Read)
 * - View Modes (List/Grid/Recents)
 * - Sort Dialog with options
 * - Long-press book menu with Edit, Mark As, Delete, Share
 * - Haptic feedback
 * - Glass visual effects
 */

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenGlass(
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    seriesViewModel: SeriesViewModel = hiltViewModelCompose(),
    isDark: Boolean = true,
    scrollToTopTrigger: Int = 0,
    onBookClick: (String) -> Unit,
    onPlayBook: (Book) -> Unit = {},
    onEditBook: (String) -> Unit = {},
    onAddClick: () -> Unit,
    onSeriesClick: (String) -> Unit = {}
) {
    val theme = glassTheme(isDark)
    val libraryData by libraryViewModel.libraryData.collectAsState()

    // Scroll states for scroll-to-top functionality
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    // Scroll to top when trigger changes
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
            gridState.animateScrollToItem(0)
        }
    }
    val currentSort by libraryViewModel.sortOption.collectAsState()
    val currentMasterFilter by libraryViewModel.masterFilter.collectAsState()
    val showHeaders by libraryViewModel.showHeaders.collectAsState()
    val allSeries by seriesViewModel.allSeries.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val context = LocalContext.current

    // Pull to refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // 4 Tabs: Not Started, In Progress, Finished, All
    val tabTitles = listOf(
        stringResource(R.string.library_not_started),
        stringResource(R.string.library_in_progress),
        stringResource(R.string.library_finished),
        stringResource(R.string.library_all)
    )
    val pagerState = rememberPagerState(pageCount = { 4 }, initialPage = 1) // Start on In Progress

    // View mode state - from ViewModel for persistence
    val currentViewMode by libraryViewModel.viewMode.collectAsState()

    // Dialog states
    var showSortDialog by remember { mutableStateOf(false) }
    var bookForMenu by remember { mutableStateOf<Book?>(null) }
    var showMarkAsMenu by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }

    // Handle refresh
    fun onRefresh() {
        scope.launch {
            isRefreshing = true
            view.performHaptic(HapticType.MediumTap)
            delay(1000)
            isRefreshing = false
            view.performHaptic(HapticType.Confirm)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Row: Library Title + Sort + Add
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = GlassSpacing.M,
                        end = GlassSpacing.M,
                        top = GlassSpacing.XXL,
                        bottom = GlassSpacing.S
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.nav_library),
                    style = GlassTypography.Display,
                    color = theme.textPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(GlassSpacing.XS)) {
                    // Sort button
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Filled.Sort,
                        onClick = {
                            view.performHaptic(HapticType.LightTap)
                            showSortDialog = true
                        },
                        isDark = isDark,
                        hasBackground = true,
                        size = 40.dp,
                        iconSize = 22.dp
                    )

                    // Add button
                    GlassIconButton(
                        icon = Icons.Default.Add,
                        onClick = {
                            view.performHaptic(HapticType.MediumTap)
                            onAddClick()
                        },
                        isDark = isDark,
                        hasBackground = true,
                        size = 40.dp,
                        iconSize = 24.dp
                    )
                }
            }

            // Status Tabs with counts - compact, glass styling
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GlassSpacing.XS),
                containerColor = Color.Transparent,
                edgePadding = 0.dp,
                divider = {},
                indicator = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val count = if (index < libraryData.size) libraryData[index].size else 0
                    val isSelected = pagerState.currentPage == index

                    Tab(
                        selected = isSelected,
                        onClick = {
                            scope.launch {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 1.dp, vertical = 1.dp)
                            .clip(RoundedCornerShape(GlassShapes.Small))
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        if (isDark) Color.White.copy(alpha = 0.12f)
                                        else Color.Black.copy(alpha = 0.08f)
                                    )
                                } else Modifier
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        text = {
                            Text(
                                text = "$title ($count)",
                                style = GlassTypography.Label,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) theme.textPrimary else theme.textSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Master Filter (Audio vs Read) + View Mode Toggle - compact row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(horizontal = GlassSpacing.M),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Master Filter Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(GlassShapes.Small))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.06f)
                            else Color.Black.copy(alpha = 0.05f)
                        )
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    GlassFilterChip(
                        icon = Icons.Rounded.Headphones,
                        label = stringResource(R.string.library_listen),
                        isSelected = currentMasterFilter == MasterFilter.AUDIO,
                        isDark = isDark,
                        onClick = {
                            view.performHaptic(HapticType.LightTap)
                            libraryViewModel.setMasterFilter(MasterFilter.AUDIO)
                        }
                    )
                    GlassFilterChip(
                        icon = Icons.Outlined.MenuBook,
                        label = stringResource(R.string.library_read),
                        isSelected = currentMasterFilter == MasterFilter.READ,
                        isDark = isDark,
                        onClick = {
                            view.performHaptic(HapticType.LightTap)
                            libraryViewModel.setMasterFilter(MasterFilter.READ)
                        }
                    )
                }

                // View Mode Toggle - cycles through LIST -> GRID -> RECENTS
                GlassIconButton(
                    icon = when (currentViewMode) {
                        LibraryViewMode.LIST -> Icons.AutoMirrored.Filled.ViewList
                        LibraryViewMode.GRID -> Icons.Default.GridView
                        LibraryViewMode.RECENTS -> Icons.Default.History
                        LibraryViewMode.SERIES -> Icons.AutoMirrored.Filled.ViewList // Fallback
                    },
                    onClick = {
                        view.performHaptic(HapticType.LightTap)
                        libraryViewModel.cycleViewMode()
                    },
                    isDark = isDark,
                    hasBackground = true,
                    size = 36.dp,
                    iconSize = 20.dp
                )
            }

            Spacer(modifier = Modifier.height(GlassSpacing.S))

            // Pull-to-refresh wrapper with HorizontalPager
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { onRefresh() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val pageBooks = if (page < libraryData.size) libraryData[page] else emptyList()

                    // Sort by recents if in RECENTS view mode
                    val displayBooks = if (currentViewMode == LibraryViewMode.RECENTS) {
                        pageBooks.sortedByDescending { it.progress }
                    } else pageBooks

                    // Check if library is completely empty (no books in any tab)
                    val totalBooks = libraryData.sumOf { it.size }

                    if (displayBooks.isEmpty()) {
                        EmptyLibraryState(
                            isDark = isDark,
                            filterName = tabTitles[page],
                            masterFilter = currentMasterFilter,
                            isLibraryEmpty = totalBooks == 0,
                            onAddClick = onAddClick
                        )
                    } else {
                        when (currentViewMode) {
                            LibraryViewMode.GRID -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    state = gridState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = GlassSpacing.M,
                                        end = GlassSpacing.M,
                                        top = GlassSpacing.S,
                                        bottom = GlassSpacing.XXXL
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S),
                                    verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)
                                ) {
                                    items(displayBooks, key = { it.id }) { book ->
                                        SwipeableBookGridItem(
                                            book = book,
                                            isDark = isDark,
                                            onClick = { onBookClick(book.id) },
                                            onLongPress = {
                                                view.performHaptic(HapticType.HeavyTap)
                                                bookForMenu = book
                                            },
                                            onSwipeToDelete = {
                                                view.performHaptic(HapticType.HeavyTap)
                                                bookToDelete = book
                                            }
                                        )
                                    }
                                }
                            }
                            LibraryViewMode.SERIES -> {
                                // Filter series by master filter and current tab
                                val filteredSeries = allSeries.filter { series ->
                                    // Apply master filter
                                    val seriesBooks = when (currentMasterFilter) {
                                        MasterFilter.AUDIO -> series.books.filter { it.format == "AUDIO" }
                                        MasterFilter.READ -> series.books.filter {
                                            it.format == "TEXT" || it.format == "DOCUMENT" ||
                                            it.format == "PDF" || it.format == "EPUB"
                                        }
                                    }

                                    // Apply tab filter (same as books)
                                    val tabFilteredBooks = when (page) {
                                        0 -> seriesBooks.filter { it.progress == 0L && !it.isFinished } // Not Started
                                        1 -> seriesBooks.filter { it.progress > 0L && !it.isFinished }  // In Progress
                                        2 -> seriesBooks.filter { it.isFinished }                        // Finished
                                        3 -> seriesBooks                                                 // All
                                        else -> seriesBooks
                                    }

                                    tabFilteredBooks.isNotEmpty()
                                }.map { series ->
                                    // Create filtered series with only relevant books
                                    val seriesBooks = when (currentMasterFilter) {
                                        MasterFilter.AUDIO -> series.books.filter { it.format == "AUDIO" }
                                        MasterFilter.READ -> series.books.filter {
                                            it.format == "TEXT" || it.format == "DOCUMENT" ||
                                            it.format == "PDF" || it.format == "EPUB"
                                        }
                                    }
                                    val tabFilteredBooks = when (page) {
                                        0 -> seriesBooks.filter { it.progress == 0L && !it.isFinished }
                                        1 -> seriesBooks.filter { it.progress > 0L && !it.isFinished }
                                        2 -> seriesBooks.filter { it.isFinished }
                                        3 -> seriesBooks
                                        else -> seriesBooks
                                    }
                                    com.example.rezon8.data.Series(series.name, tabFilteredBooks)
                                }

                                SeriesListView(
                                    series = filteredSeries,
                                    isDark = isDark,
                                    onSeriesClick = onSeriesClick,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            LibraryViewMode.LIST, LibraryViewMode.RECENTS -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = GlassSpacing.M,
                                        end = GlassSpacing.M,
                                        top = GlassSpacing.S,
                                        bottom = GlassSpacing.XXXL
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(GlassSpacing.S)
                                ) {
                                    // Show "Recently Read" header in RECENTS mode
                                    if (currentViewMode == LibraryViewMode.RECENTS) {
                                        item {
                                            Text(
                                                text = stringResource(R.string.library_recents_view),
                                                style = GlassTypography.Headline,
                                                color = theme.textPrimary,
                                                modifier = Modifier.padding(bottom = GlassSpacing.XS)
                                            )
                                        }
                                    }
                                    items(displayBooks, key = { it.id }) { book ->
                                        BookListItem(
                                            book = book,
                                            isDark = isDark,
                                            onClick = { onBookClick(book.id) },
                                            onPlayClick = { onPlayBook(book) },
                                            onLongPress = {
                                                view.performHaptic(HapticType.HeavyTap)
                                                bookForMenu = book
                                            },
                                            onSeriesClick = onSeriesClick
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Book Menu Dialog
        bookForMenu?.let { book ->
            BookMenuDialog(
                book = book,
                isDark = isDark,
                onDismiss = { bookForMenu = null },
                onPlay = {
                    bookForMenu = null
                    onPlayBook(book)
                },
                onEdit = {
                    bookForMenu = null
                    onEditBook(book.id)
                },
                onMarkAs = { status ->
                    view.performHaptic(HapticType.MediumTap)
                    libraryViewModel.markBookStatus(book, status)
                    bookForMenu = null
                },
                onDelete = {
                    bookForMenu = null
                    bookToDelete = book
                },
                onShare = {
                    bookForMenu = null
                    val progressPercent = if (book.duration > 0) {
                        ((book.progress.toFloat() / book.duration.toFloat()) * 100).toInt()
                    } else 0

                    val shareText = buildString {
                        append("📚 Check out \"${book.title}\"")
                        if (book.author.isNotBlank() && book.author != "Unknown Author") {
                            append(" by ${book.author}")
                        }
                        if (progressPercent > 0) {
                            append("\n\n📍 I'm $progressPercent% through this book")
                        }
                        append("\n\n— Shared from REZON8")
                    }

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        putExtra(Intent.EXTRA_SUBJECT, book.title)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share book"))
                }
            )
        }

        // Delete confirmation dialog
        bookToDelete?.let { book ->
            AlertDialog(
                onDismissRequest = { bookToDelete = null },
                containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
                shape = RoundedCornerShape(GlassShapes.Medium),
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = GlassColors.Destructive
                    )
                },
                title = {
                    Text(
                        stringResource(R.string.detail_delete_book) + "?",
                        style = GlassTypography.Headline,
                        color = theme.textPrimary
                    )
                },
                text = {
                    Text(
                        "\"${book.title}\" will be removed from your library. This cannot be undone.",
                        style = GlassTypography.Body,
                        color = theme.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            view.performHaptic(HapticType.HeavyTap)
                            libraryViewModel.deleteBook(book.id)
                            bookToDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.dialog_delete), color = GlassColors.Destructive, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { bookToDelete = null }) {
                        Text(stringResource(R.string.dialog_cancel), color = theme.textSecondary)
                    }
                }
            )
        }

        // Sort Dialog
        if (showSortDialog) {
            SortDialog(
                currentSort = currentSort,
                showHeaders = showHeaders,
                isDark = isDark,
                onDismiss = { showSortDialog = false },
                onSortSelected = { option ->
                    view.performHaptic(HapticType.LightTap)
                    libraryViewModel.setSort(option)
                },
                onToggleHeaders = {
                    view.performHaptic(HapticType.LightTap)
                    libraryViewModel.toggleHeaders()
                }
            )
        }
    }
}

// ============================================================================
// SWIPEABLE BOOK GRID ITEM
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableBookGridItem(
    book: Book,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeToDelete: () -> Unit
) {
    val theme = glassTheme(isDark)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    // Swipe state
    var offsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = -100f

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    // Only allow left swipe (negative)
                    val newOffset = offsetX + delta
                    if (newOffset <= 0) {
                        offsetX = newOffset.coerceIn(-150f, 0f)
                    }
                },
                onDragStopped = {
                    if (offsetX < swipeThreshold) {
                        onSwipeToDelete()
                    }
                    offsetX = 0f
                }
            )
    ) {
        // Delete indicator behind
        if (offsetX < -20f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        GlassColors.Destructive.copy(alpha = (-offsetX / 150f).coerceIn(0f, 1f)),
                        RoundedCornerShape(GlassShapes.Small)
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.content_desc_delete),
                    tint = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                    onLongClick = onLongPress
                )
        ) {
            // Cover with progress overlay
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(GlassShapes.Small))
            ) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Progress indicator at bottom
                val progressFloat = book.progress.toFloat()
                if (progressFloat > 0f && progressFloat < 1f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFloat)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                }

                // Format badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(GlassSpacing.XXS)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (book.format in listOf("AUDIO", "M4B", "MP3")) {
                            Icons.Outlined.Headphones
                        } else {
                            Icons.Outlined.MenuBook
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(GlassSpacing.XS))

            // Title
            Text(
                text = book.title,
                style = GlassTypography.Caption,
                fontWeight = FontWeight.Medium,
                color = theme.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = GlassTypography.Caption.lineHeight
            )

            // Author
            Text(
                text = book.author,
                style = GlassTypography.Caption,
                color = theme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// GLASS FILTER CHIP
// ============================================================================

@Composable
private fun GlassFilterChip(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(GlassShapes.Small))
            .then(
                if (isSelected) {
                    Modifier.background(
                        if (isDark) Color.White.copy(alpha = 0.15f)
                        else Color.Black.copy(alpha = 0.10f)
                    )
                } else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) theme.textPrimary else theme.textTertiary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = GlassTypography.Label,
            color = if (isSelected) theme.textPrimary else theme.textTertiary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ============================================================================
// BOOK LIST ITEM (for List/Recents view)
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookListItem(
    book: Book,
    isDark: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onLongPress: () -> Unit,
    onSeriesClick: ((String) -> Unit)? = null
) {
    val theme = glassTheme(isDark)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val hasSeries = book.seriesInfo.isNotBlank()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(GlassShapes.Small))
            .glassCard(isDark = isDark, cornerRadius = GlassShapes.Small)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(GlassSpacing.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover Art
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Progress bar at bottom
            val progressFloat = if (book.duration > 0) book.progress.toFloat() / book.duration else 0f
            if (progressFloat > 0f && progressFloat < 1f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFloat)
                            .fillMaxHeight()
                            .background(Color.White)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(GlassSpacing.S))

        // Info Column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = GlassTypography.Body,
                fontWeight = FontWeight.SemiBold,
                color = theme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = book.author,
                style = GlassTypography.Caption,
                color = theme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Play button and optional series arrow (stacked vertically)
        val view = LocalView.current
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 4.dp)
        ) {
            // Raised headphones/book icon
            Spacer(modifier = Modifier.height(if (hasSeries) 4.dp else 0.dp))

            GlassIconButton(
                icon = if (book.format in listOf("AUDIO", "M4B", "MP3")) {
                    Icons.Rounded.Headphones
                } else {
                    Icons.Outlined.MenuBook
                },
                onClick = onPlayClick,
                isDark = isDark,
                hasBackground = false,
                size = 36.dp,
                iconSize = 22.dp
            )

            // Series arrow - only show if book has series info
            if (hasSeries && onSeriesClick != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                            onSeriesClick(book.seriesInfo)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.content_desc_go_to_series),
                        tint = theme.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// BOOK MENU DIALOG
// ============================================================================

@Composable
private fun BookMenuDialog(
    book: Book,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onMarkAs: (String) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val theme = glassTheme(isDark)
    var showMarkAsSubmenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = book.title,
                style = GlassTypography.Headline,
                color = theme.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                if (!showMarkAsSubmenu) {
                    // Main menu
                    BookMenuItem(
                        icon = Icons.Default.PlayArrow,
                        text = stringResource(R.string.menu_play),
                        iconTint = Color.White,
                        textColor = theme.textPrimary,
                        onClick = onPlay
                    )
                    BookMenuItem(
                        icon = Icons.Default.Edit,
                        text = stringResource(R.string.menu_edit),
                        iconTint = Color.White,
                        textColor = theme.textPrimary,
                        onClick = onEdit
                    )
                    BookMenuItem(
                        icon = Icons.Default.CheckCircle,
                        text = stringResource(R.string.menu_mark_as),
                        iconTint = Color.White,
                        textColor = theme.textPrimary,
                        onClick = { showMarkAsSubmenu = true }
                    )
                    HorizontalDivider(
                        color = theme.glassBorder,
                        modifier = Modifier.padding(vertical = GlassSpacing.XS)
                    )
                    BookMenuItem(
                        icon = Icons.Default.Delete,
                        text = stringResource(R.string.menu_delete),
                        iconTint = theme.textTertiary,
                        textColor = theme.textPrimary,
                        onClick = onDelete
                    )
                    BookMenuItem(
                        icon = Icons.Default.Share,
                        text = stringResource(R.string.menu_share),
                        iconTint = theme.textTertiary,
                        textColor = theme.textPrimary,
                        onClick = onShare
                    )
                } else {
                    // Mark As submenu
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMarkAsSubmenu = false }
                            .padding(bottom = GlassSpacing.S),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back),
                            tint = theme.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(GlassSpacing.XS))
                        Text(
                            text = stringResource(R.string.menu_mark_as),
                            style = GlassTypography.Label,
                            color = theme.textSecondary
                        )
                    }
                    BookMenuItem(
                        icon = Icons.Default.RadioButtonUnchecked,
                        text = stringResource(R.string.library_not_started),
                        iconTint = theme.textTertiary,
                        textColor = theme.textPrimary,
                        onClick = { onMarkAs("Unfinished") }
                    )
                    BookMenuItem(
                        icon = Icons.Default.PlayCircle,
                        text = stringResource(R.string.library_in_progress),
                        iconTint = Color(0xFFFFC107),
                        textColor = theme.textPrimary,
                        onClick = { onMarkAs("In Progress") }
                    )
                    BookMenuItem(
                        icon = Icons.Default.CheckCircle,
                        text = stringResource(R.string.library_finished),
                        iconTint = Color(0xFF4CAF50),
                        textColor = theme.textPrimary,
                        onClick = { onMarkAs("Finished") }
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun BookMenuItem(
    icon: ImageVector,
    text: String,
    iconTint: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Small))
            .clickable { onClick() }
            .padding(vertical = GlassSpacing.S, horizontal = GlassSpacing.XS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(GlassSpacing.S))
        Text(
            text = text,
            style = GlassTypography.Body,
            color = textColor
        )
    }
}

// ============================================================================
// SORT DIALOG
// ============================================================================

@Composable
private fun SortDialog(
    currentSort: SortOption,
    showHeaders: Boolean,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onToggleHeaders: () -> Unit
) {
    val theme = glassTheme(isDark)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = stringResource(R.string.library_sort),
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Column {
                SortOptionItem(stringResource(R.string.library_recents), SortOption.RECENTS, currentSort, theme, onSortSelected)
                SortOptionItem(stringResource(R.string.library_author), SortOption.AUTHOR, currentSort, theme, onSortSelected)
                SortOptionItem(stringResource(R.string.library_title), SortOption.TITLE, currentSort, theme, onSortSelected)
                SortOptionItem(stringResource(R.string.library_series), SortOption.SERIES, currentSort, theme, onSortSelected)
                SortOptionItem(stringResource(R.string.library_path), SortOption.PATH, currentSort, theme, onSortSelected)

                HorizontalDivider(
                    color = theme.glassBorder,
                    modifier = Modifier.padding(vertical = GlassSpacing.S)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GlassShapes.Small))
                        .clickable { onToggleHeaders() }
                        .padding(vertical = GlassSpacing.S),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (showHeaders) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (showHeaders) theme.textPrimary else theme.textTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(GlassSpacing.S))
                        Text(
                            text = stringResource(R.string.library_show_headers),
                            style = GlassTypography.Body,
                            color = theme.textPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_done), color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun SortOptionItem(
    label: String,
    option: SortOption,
    currentSort: SortOption,
    theme: GlassThemeData,
    onClick: (SortOption) -> Unit
) {
    val isSelected = option == currentSort

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Small))
            .then(
                if (isSelected) Modifier.background(
                    if (theme.isDark) Color.White.copy(alpha = 0.1f)
                    else Color.Black.copy(alpha = 0.08f)
                )
                else Modifier
            )
            .clickable { onClick(option) }
            .padding(vertical = GlassSpacing.S, horizontal = GlassSpacing.XS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = GlassTypography.Body,
            color = if (isSelected) theme.textPrimary else theme.textPrimary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = theme.textPrimary,
                unselectedColor = theme.textTertiary
            )
        )
    }
}

// ============================================================================
// BRAND GRADIENT
// ============================================================================

private val BrandGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFF8A50),  // Warm orange top
        Color(0xFFE91E63)   // Magenta pink bottom
    )
)

// ============================================================================
// EMPTY STATE
// ============================================================================

@Composable
private fun EmptyLibraryState(
    isDark: Boolean,
    filterName: String,
    masterFilter: MasterFilter,
    isLibraryEmpty: Boolean,
    onAddClick: () -> Unit
) {
    val theme = glassTheme(isDark)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = GlassSpacing.XL)
        ) {
            if (isLibraryEmpty) {
                // Premium logo - show on ANY tab when library is empty
                Image(
                    painter = painterResource(id = R.drawable.splash_logo),
                    contentDescription = stringResource(R.string.ui_rezon8),
                    modifier = Modifier.size(70.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(GlassSpacing.XL))

                Text(
                    text = stringResource(R.string.library_empty_add_first),
                    style = GlassTypography.Title,
                    color = theme.textPrimary
                )

                Spacer(modifier = Modifier.height(GlassSpacing.XS))

                Text(
                    text = stringResource(R.string.library_empty_import_hint),
                    style = GlassTypography.Body,
                    color = theme.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(GlassSpacing.XL))

                // Premium add button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(GlassShapes.Small))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.1f)
                            else Color.Black.copy(alpha = 0.08f)
                        )
                        .clickable { onAddClick() }
                        .padding(horizontal = 32.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = theme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.library_empty_browse),
                            style = GlassTypography.Body,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.textPrimary
                        )
                    }
                }
            } else {
                // Filtered empty state - minimal (library has books, just not in this category)
                Icon(
                    if (masterFilter == MasterFilter.AUDIO) Icons.Rounded.Headphones else Icons.Outlined.MenuBook,
                    contentDescription = null,
                    tint = theme.textTertiary,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(GlassSpacing.M))

                Text(
                    text = stringResource(R.string.library_empty_no_items, filterName),
                    style = GlassTypography.Body,
                    color = theme.textSecondary
                )
            }
        }
    }
}
