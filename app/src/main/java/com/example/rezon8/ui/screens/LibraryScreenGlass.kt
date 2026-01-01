package com.mossglen.lithos.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import android.widget.Toast
import com.mossglen.lithos.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mossglen.lithos.data.Book
import com.mossglen.lithos.data.groupBySeries
import com.mossglen.lithos.haptics.HapticType
import com.mossglen.lithos.haptics.performHaptic
import com.mossglen.lithos.ui.components.*
import com.mossglen.lithos.ui.theme.*
import com.mossglen.lithos.ui.theme.LithosAmber
import com.mossglen.lithos.ui.theme.LithosMoss
import com.mossglen.lithos.ui.theme.LithosSlate
import com.mossglen.lithos.ui.theme.LithosGlass
import com.mossglen.lithos.ui.viewmodel.LibraryViewModel
import com.mossglen.lithos.ui.viewmodel.LibraryViewMode
import com.mossglen.lithos.ui.viewmodel.SortOption
import androidx.hilt.navigation.compose.hiltViewModel as hiltViewModelCompose
import com.mossglen.lithos.ui.viewmodel.SeriesViewModel
import com.mossglen.lithos.ui.viewmodel.CoverArtViewModel
import com.mossglen.lithos.ui.viewmodel.TtsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * REVERIE Glass - Library Screen
 *
 * Lithos Amber Design Language:
 * - NO neon/glowing effects - use matte finishes
 * - Progress bars use Amber #D48C2C
 * - Active/selected states use Amber #D48C2C
 * - Filter/tab indicators use Amber #D48C2C
 * - Glass backgrounds: frosted glass rgba(26, 29, 33, 0.85) with blur(20dp)
 * - Play buttons ONLY use Moss #4A5D45
 * - Background: Slate #1A1D21
 *
 * Features:
 * - Pull-to-refresh
 * - Swipeable tabs (HorizontalPager) - 4 tabs: Not Started, In Progress, Finished, All
 * - Master Filter (Audio vs Read)
 * - View Modes (List/Grid/Recents)
 * - Sort Dialog with options
 * - Long-press book menu with Edit, Mark As, Delete, Share
 * - Haptic feedback
 */

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenGlass(
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    seriesViewModel: SeriesViewModel = hiltViewModelCompose(),
    coverArtViewModel: CoverArtViewModel = hiltViewModelCompose(),
    ttsViewModel: TtsViewModel = hiltViewModelCompose(),
    isDark: Boolean = true,
    isOLED: Boolean = false,
    accentColor: Color = LithosAmber,
    scrollToTopTrigger: Int = 0,
    onBookClick: (String) -> Unit,
    onPlayBook: (Book) -> Unit = {},
    onEditBook: (String) -> Unit = {},
    onAddClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onSeriesClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onGenreClick: (String) -> Unit = {},
    onScrollUp: () -> Unit = {},
    onScrollDown: () -> Unit = {}
) {
    val theme = glassTheme(isDark, isOLED)
    val libraryData by libraryViewModel.libraryData.collectAsState()

    // Scroll states for scroll-to-top functionality
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    // Track scroll direction for pill auto-hide
    var lastScrollIndex by remember { mutableStateOf(0) }
    var lastScrollOffset by remember { mutableStateOf(0) }
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset
        val scrollDelta = (currentIndex * 1000 + currentOffset) - (lastScrollIndex * 1000 + lastScrollOffset)

        // Use threshold to avoid jitter
        if (kotlin.math.abs(scrollDelta) > 50) {
            if (scrollDelta > 0) {
                // Scrolling down (browsing) - hide pill
                onScrollUp()
            } else {
                // Scrolling up (looking for controls) - show pill
                onScrollDown()
            }
        }
        lastScrollIndex = currentIndex
        lastScrollOffset = currentOffset
    }

    // Scroll to top when trigger changes
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
            gridState.animateScrollToItem(0)
        }
    }
    val currentSort by libraryViewModel.sortOption.collectAsState()

    // Lithos design: Slate background for audiobook-only library
    val libraryBackground = if (isDark) LithosSlate else Color(0xFFF0F0F4)
    val accentTint = LithosAmber.copy(alpha = 0.03f)  // Amber hint

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
    var bookForCoverPicker by remember { mutableStateOf<Book?>(null) }

    // Half-sheet preview state (UI3)
    var bookForPreview by remember { mutableStateOf<Book?>(null) }

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
            .background(libraryBackground)
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
                    text = stringResource(R.string.nav_browse),  // UI3: "Browse" instead of "Library"
                    style = GlassTypography.Display,
                    color = theme.textPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(GlassSpacing.XS)) {
                    // View Mode Toggle - cycles through LIST -> GRID -> RECENTS
                    GlassIconButton(
                        icon = when (currentViewMode) {
                            LibraryViewMode.LIST -> Icons.AutoMirrored.Filled.ViewList
                            LibraryViewMode.GRID -> Icons.Default.GridView
                            LibraryViewMode.RECENTS -> Icons.Default.History
                            LibraryViewMode.SERIES -> Icons.AutoMirrored.Filled.ViewList
                        },
                        onClick = {
                            view.performHaptic(HapticType.LightTap)
                            libraryViewModel.cycleViewMode()
                        },
                        isDark = isDark,
                        hasBackground = true,
                        size = 40.dp,
                        iconSize = 22.dp
                    )

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

            // Status Tabs with counts - Lithos Amber design: matte finish
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
                            .height(32.dp)  // Compact height
                            .padding(horizontal = 1.dp)
                            .clip(RoundedCornerShape(GlassShapes.Small))
                            .then(
                                if (isSelected) {
                                    // Lithos Amber: subtle amber tint for selected state
                                    Modifier.background(LithosAmber.copy(alpha = 0.15f))
                                } else Modifier
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        text = {
                            Text(
                                text = "$title ($count)",
                                style = GlassTypography.Label.copy(fontSize = 12.sp),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                // Lithos Amber: selected tabs use amber color
                                color = if (isSelected) LithosAmber else theme.textSecondary
                            )
                        }
                    )
                }
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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(accentTint)  // Subtle amber tint
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
                            isLibraryEmpty = totalBooks == 0,
                            onAddClick = onAddClick
                        )
                    } else {
                        when (currentViewMode) {
                            LibraryViewMode.GRID -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2), // Premium 2-per-row layout
                                    state = gridState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = GlassSpacing.M,
                                        end = GlassSpacing.M,
                                        top = GlassSpacing.S,
                                        bottom = GlassSpacing.XXXL
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(GlassSpacing.M),
                                    verticalArrangement = Arrangement.spacedBy(GlassSpacing.L)
                                ) {
                                    items(displayBooks, key = { it.id }) { book ->
                                        BookGridItem(
                                            book = book,
                                            isDark = isDark,
                                            hasAudioReady = ttsViewModel.hasPreGeneratedAudio(book.id),
                                            onCoverClick = {
                                                // Tap cover = play directly
                                                onPlayBook(book)
                                            },
                                            onTextClick = {
                                                // Tap text = show half-sheet preview
                                                bookForPreview = book
                                            },
                                            onLongPress = {
                                                // Long press = show menu (has delete option)
                                                view.performHaptic(HapticType.HeavyTap)
                                                bookForMenu = book
                                            }
                                        )
                                    }
                                }
                            }
                            LibraryViewMode.SERIES -> {
                                // Filter series for audiobooks only
                                val filteredSeries = allSeries.filter { series ->
                                    // Only show audiobooks
                                    val seriesBooks = series.books.filter { it.format == "AUDIO" }

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
                                    // Create filtered series with only audiobooks
                                    val seriesBooks = series.books.filter { it.format == "AUDIO" }
                                    val tabFilteredBooks = when (page) {
                                        0 -> seriesBooks.filter { it.progress == 0L && !it.isFinished }
                                        1 -> seriesBooks.filter { it.progress > 0L && !it.isFinished }
                                        2 -> seriesBooks.filter { it.isFinished }
                                        3 -> seriesBooks
                                        else -> seriesBooks
                                    }
                                    com.mossglen.lithos.data.Series(series.name, tabFilteredBooks)
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
                                            hasAudioReady = ttsViewModel.hasPreGeneratedAudio(book.id),
                                            onClick = {
                                                // UI3: Show half-sheet preview instead of navigating
                                                bookForPreview = book
                                            },
                                            onPlayClick = { onPlayBook(book) },
                                            onLongPress = {
                                                view.performHaptic(HapticType.HeavyTap)
                                                bookForMenu = book
                                            },
                                            onSeriesClick = onSeriesClick,
                                            onAuthorClick = onAuthorClick,
                                            onMarkAs = { status ->
                                                view.performHaptic(HapticType.MediumTap)
                                                libraryViewModel.markBookStatus(book, status)
                                            },
                                            onShare = {
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
                                                    append("\n\n— Shared from Reverie")
                                                }

                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                    putExtra(Intent.EXTRA_SUBJECT, book.title)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share book"))
                                            },
                                            onDelete = { bookToDelete = book },
                                            onEditBook = { onEditBook(book.id) },
                                            onSplitBook = {
                                                // TODO: Implement split book functionality
                                                view.performHaptic(HapticType.MediumTap)
                                            },
                                            onChangeCover = {
                                                view.performHaptic(HapticType.MediumTap)
                                                bookForCoverPicker = book
                                            }
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
                        append("\n\n— Shared from Reverie")
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

        // Delete confirmation dialog with file deletion option
        bookToDelete?.let { book ->
            val isFromTorrent = remember(book.id) { libraryViewModel.isBookFromTorrent(book) }
            var deleteFiles by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { bookToDelete = null },
                containerColor = if (isDark) LithosUI.SheetBackground else LithosUI.SheetBackgroundLight,
                shape = RoundedCornerShape(LithosComponents.Cards.dialogRadius),
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
                    Column {
                        Text(
                            "\"${book.title}\" will be removed from your library.",
                            style = GlassTypography.Body,
                            color = theme.textSecondary
                        )

                        // Show delete files option for torrent-downloaded books
                        if (isFromTorrent) {
                            Spacer(modifier = Modifier.height(GlassSpacing.M))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(GlassShapes.Small))
                                    .background(theme.glassBorder.copy(alpha = 0.3f))
                                    .clickable {
                                        view.performHaptic(HapticType.LightTap)
                                        deleteFiles = !deleteFiles
                                    }
                                    .padding(GlassSpacing.S),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = deleteFiles,
                                    onCheckedChange = {
                                        view.performHaptic(HapticType.LightTap)
                                        deleteFiles = it
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = GlassColors.Destructive,
                                        uncheckedColor = theme.textSecondary,
                                        checkmarkColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(GlassSpacing.S))
                                Column {
                                    Text(
                                        "Also delete downloaded files",
                                        style = GlassTypography.Body,
                                        fontWeight = FontWeight.Medium,
                                        color = theme.textPrimary
                                    )
                                    Text(
                                        "Free up storage space",
                                        style = GlassTypography.Caption,
                                        color = theme.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(GlassSpacing.S))
                        Text(
                            if (deleteFiles) "This will permanently delete the audio files from your device."
                            else "Audio files will remain on your device.",
                            style = GlassTypography.Caption,
                            color = if (deleteFiles) GlassColors.Destructive.copy(alpha = 0.8f) else theme.textSecondary.copy(alpha = 0.7f)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            view.performHaptic(HapticType.HeavyTap)
                            libraryViewModel.deleteBookWithCleanup(book.id, deleteFiles)
                            bookToDelete = null
                        }
                    ) {
                        Text(
                            if (deleteFiles) "Delete All" else stringResource(R.string.dialog_delete),
                            color = GlassColors.Destructive,
                            fontWeight = FontWeight.SemiBold
                        )
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

        // UI3: Morphing book detail sheet
        bookForPreview?.let { book ->
            BookDetailMorphingSheet(
                book = book,
                isVisible = true,
                accentColor = if (isOLED) accentColor else theme.interactive,
                isDark = isDark,
                isOLED = isOLED,
                onDismiss = { bookForPreview = null },
                onPlayBook = {
                    onPlayBook(book)
                    bookForPreview = null
                },
                onAuthorClick = { authorName ->
                    bookForPreview = null
                    onAuthorClick(authorName)
                },
                onSeriesClick = { seriesName ->
                    bookForPreview = null
                    onSeriesClick(seriesName)
                },
                onGenreClick = { genre ->
                    bookForPreview = null
                    onGenreClick(genre)
                },
                // Synopsis scroll -> pill auto-hide
                onScrollDown = onScrollUp,  // Scroll down in sheet = hide pill (same as main list)
                onScrollUp = onScrollDown   // Scroll up in sheet = show pill
            )
        }

        // Cover Art Picker Dialog
        bookForCoverPicker?.let { book ->
            CoverArtPickerDialog(
                currentCoverUrl = book.coverUrl,
                bookTitle = book.title,
                bookAuthor = book.author,
                accentColor = accentColor,
                isOLED = isOLED,
                onDismiss = { bookForCoverPicker = null },
                onCoverSelected = { newCoverUrl ->
                    coverArtViewModel.updateBookCover(book.id, newCoverUrl)
                    Toast.makeText(context, "Cover updated", Toast.LENGTH_SHORT).show()
                },
                viewModel = coverArtViewModel
            )
        }
    }
}

// ============================================================================
// UI3: BOOK PREVIEW SHEET CONTENT
// ============================================================================

@Composable
private fun BookPreviewSheetContent(
    book: Book,
    isDark: Boolean,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M)
            .padding(bottom = 32.dp)
    ) {
        // Cover + Info Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GlassSpacing.M)
        ) {
            // Cover Art
            AsyncImage(
                model = book.coverUrl ?: book.filePath,
                contentDescription = book.title,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(LithosComponents.Cards.cornerRadius))
                    .background(if (isDark) LithosUI.CardBackground else LithosUI.CardBackgroundLight),
                contentScale = ContentScale.Crop
            )

            // Book Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = book.title,
                    style = GlassTypography.Title,
                    color = theme.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = GlassTypography.Body,
                    color = theme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Duration & Progress
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(book.duration),
                        style = GlassTypography.Caption,
                        color = theme.textTertiary
                    )
                    if (book.progress > 0 && !book.isFinished) {
                        Text(
                            text = "•",
                            style = GlassTypography.Caption,
                            color = theme.textTertiary
                        )
                        Text(
                            text = "${(book.progressPercent() * 100).toInt()}%",
                            style = GlassTypography.Caption,
                            color = LithosAmber  // Lithos Amber for progress percentage
                        )
                    }
                    if (book.isFinished) {
                        Text(
                            text = "•",
                            style = GlassTypography.Caption,
                            color = theme.textTertiary
                        )
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = LithosMoss  // Lithos Moss for success/checkmark
                        )
                    }
                }

                // Series info if available
                if (book.seriesInfo.isNotEmpty()) {
                    Text(
                        text = book.seriesInfo,
                        style = GlassTypography.Caption,
                        color = theme.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(GlassSpacing.L))

        // Action Buttons Row - Lithos design
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
        ) {
            // Play Button (Primary) - Lithos Moss for play buttons only
            Button(
                onClick = onPlayClick,
                modifier = Modifier
                    .weight(1f)
                    .height(LithosComponents.Buttons.height),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LithosMoss  // Lithos: Moss for play buttons
                ),
                shape = RoundedCornerShape(LithosComponents.Buttons.cornerRadius)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (book.progress > 0 && !book.isFinished)
                        stringResource(R.string.ui_resume)
                    else
                        stringResource(R.string.player_play),
                    style = GlassTypography.Label
                )
            }

            // Details Button (Secondary)
            OutlinedButton(
                onClick = onDetailsClick,
                modifier = Modifier
                    .weight(1f)
                    .height(LithosComponents.Buttons.height),
                border = BorderStroke(1.dp, theme.textSecondary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(LithosComponents.Buttons.cornerRadius)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = theme.textPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.player_book_details),
                    style = GlassTypography.Label,
                    color = theme.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(GlassSpacing.M))

        // Synopsis Preview (if available)
        if (book.synopsis.isNotEmpty()) {
            Text(
                text = book.synopsis,
                style = GlassTypography.Body,
                color = theme.textSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val hours = ms / 3600000
    val minutes = (ms % 3600000) / 60000
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

// ============================================================================
// BOOK GRID ITEM
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookGridItem(
    book: Book,
    isDark: Boolean,
    hasAudioReady: Boolean = false,  // Pre-generated TTS audio available
    onCoverClick: () -> Unit,  // Tap cover = play
    onTextClick: () -> Unit,   // Tap text = show half sheet
    onLongPress: () -> Unit    // Long press = show menu (with delete option)
) {
    val theme = glassTheme(isDark)
    val coverInteractionSource = remember { MutableInteractionSource() }
    val textInteractionSource = remember { MutableInteractionSource() }
    val isCoverPressed by coverInteractionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    // Show play icon briefly when cover is pressed
    var showPlayHint by remember { mutableStateOf(false) }
    LaunchedEffect(isCoverPressed) {
        showPlayHint = isCoverPressed
    }

    val scale by animateFloatAsState(
        targetValue = if (isCoverPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.scale(scale)
    ) {
        Column {
            // Cover with progress overlay - TAP TO PLAY
            Box(
                modifier = Modifier
                    .aspectRatio(1f) // Square covers for consistency
                    .clip(RoundedCornerShape(GlassShapes.Small))
                    .combinedClickable(
                        interactionSource = coverInteractionSource,
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCoverClick()
                        },
                        onLongClick = onLongPress
                    )
            ) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Progress indicator at bottom - Lithos Amber design
                val progressFloat = if (book.duration > 0) {
                    (book.progress.toFloat() / book.duration.toFloat()).coerceIn(0f, 1f)
                } else 0f
                if (progressFloat > 0f && progressFloat < 1f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFloat)
                                .fillMaxHeight()
                                .background(LithosAmber)  // Lithos Amber for progress
                        )
                    }
                }

                // Play hint that appears when pressed (subtle feedback)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showPlayHint,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Small format indicator at top-right (for non-audio books)
                if (book.format !in listOf("AUDIO", "M4B", "MP3")) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Audio Ready indicator at top-left (for pre-generated TTS)
                // Lithos Amber: matte finish, no glow
                if (hasAudioReady) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(LithosAmber.copy(alpha = 0.9f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Filled.VolumeUp,
                                contentDescription = "Audio Ready",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "AI",
                                style = TextStyle(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(GlassSpacing.S))

            // Text area - TAP TO SHOW HALF SHEET
            Column(
                modifier = Modifier
                    .clickable(
                        interactionSource = textInteractionSource,
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTextClick()
                    }
            ) {
                // Title
                Text(
                    text = book.title,
                    style = GlassTypography.Body,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
}

// ============================================================================
// GLASS FILTER CHIP - Lithos Amber Design
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
                    // Lithos Amber: subtle amber tint for selected, matte finish
                    Modifier.background(LithosAmber.copy(alpha = 0.18f))
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
            // Lithos Amber: selected icons use amber color
            tint = if (isSelected) LithosAmber else theme.textTertiary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = GlassTypography.Label,
            // Lithos Amber: selected text uses amber color
            color = if (isSelected) LithosAmber else theme.textTertiary,
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
    hasAudioReady: Boolean = false,  // Pre-generated TTS audio available
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onLongPress: () -> Unit,
    onSeriesClick: ((String) -> Unit)? = null,
    onAuthorClick: ((String) -> Unit)? = null,
    onMarkAs: ((String) -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onEditBook: (() -> Unit)? = null,
    onSplitBook: (() -> Unit)? = null,
    onChangeCover: (() -> Unit)? = null
) {
    val theme = glassTheme(isDark)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val hasSeries = book.seriesInfo.isNotBlank()
    val hasAuthor = book.author.isNotBlank() && book.author != "Unknown Author"
    val isAudioBook = book.format == "AUDIO" || book.format == "M4B" || book.format == "MP3"
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // Calculate time remaining
    val remainingMs = book.duration - book.progress
    val remainingText = if (remainingMs > 0 && book.duration > 0) {
        val hours = remainingMs / 3600000
        val minutes = (remainingMs % 3600000) / 60000
        if (hours > 0) "${hours}h ${minutes}m left" else "${minutes}m left"
    } else if (book.isFinished) {
        "Finished"
    } else {
        val hours = book.duration / 3600000
        val minutes = (book.duration % 3600000) / 60000
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

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
        // Cover Art - 80dp with audio ready indicator
        Box {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )

            // Audio Ready indicator at top-left
            // Lithos Amber: matte finish, no glow
            if (hasAudioReady) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(LithosAmber.copy(alpha = 0.9f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            Icons.Filled.VolumeUp,
                            contentDescription = "Audio Ready",
                            tint = Color.White,
                            modifier = Modifier.size(8.dp)
                        )
                        Text(
                            text = "AI",
                            style = TextStyle(
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(GlassSpacing.S))

        // Info Column - Title, Author, Progress Bar, Time Left (reordered)
        Column(modifier = Modifier.weight(1f)) {
            // Title
            Text(
                text = book.title,
                style = GlassTypography.Body,
                fontWeight = FontWeight.SemiBold,
                color = theme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Author
            Text(
                text = book.author,
                style = GlassTypography.Caption,
                color = theme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Progress bar - between author and time left
            // Lithos Amber design: thin 3dp stroke, matte finish
            val progressFloat = if (book.duration > 0) book.progress.toFloat() / book.duration else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.15f)
                        else Color.Black.copy(alpha = 0.1f)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFloat.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(LithosAmber)  // Lithos Amber for progress
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Time remaining - at bottom
            Text(
                text = remainingText,
                style = GlassTypography.Caption,
                color = if (book.isFinished) LithosMoss else theme.textSecondary,  // Lithos Moss for finished
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(GlassSpacing.S))

        // Play button - Lithos Moss (play buttons only use Moss)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(LithosMoss)  // Lithos design: Moss for play buttons only
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPlayClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.player_play),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 3-dot menu - slightly bigger, pushed to edge
        Box {
            IconButton(
                onClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    showMenu = true
                },
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = theme.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Complete Dropdown menu with all 9 items
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .background(
                        if (isDark) LithosUI.CardBackground else LithosUI.SheetBackgroundLight,
                        RoundedCornerShape(LithosComponents.Cards.chipRadius)
                    ),
                containerColor = if (isDark) LithosUI.CardBackground else LithosUI.SheetBackgroundLight
            ) {
                // 1. Play - Lithos Moss for play buttons
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.player_play), color = theme.textPrimary) },
                    onClick = {
                        showMenu = false
                        onPlayClick()
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.PlayArrow, null, tint = LithosMoss)
                    }
                )

                // 2. More from Author (if author available)
                if (hasAuthor && onAuthorClick != null) {
                    DropdownMenuItem(
                        text = { Text("More from Author", color = theme.textPrimary) },
                        onClick = {
                            showMenu = false
                            onAuthorClick(book.author)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Person, null, tint = theme.textSecondary)
                        }
                    )
                }

                // 3. More from Series (if series available)
                if (hasSeries && onSeriesClick != null) {
                    DropdownMenuItem(
                        text = { Text("More from Series", color = theme.textPrimary) },
                        onClick = {
                            showMenu = false
                            onSeriesClick(book.seriesInfo)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.AutoStories, null, tint = theme.textSecondary)
                        }
                    )
                }

                HorizontalDivider(color = theme.divider)

                // 4. Edit Metadata
                if (onEditBook != null) {
                    DropdownMenuItem(
                        text = { Text("Edit Metadata", color = theme.textPrimary) },
                        onClick = {
                            showMenu = false
                            onEditBook()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, null, tint = theme.textSecondary)
                        }
                    )
                }

                // 5. Mark as Finished/Unfinished
                if (onMarkAs != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (book.isFinished) "Mark as Unfinished" else "Mark as Finished",
                                color = theme.textPrimary
                            )
                        },
                        onClick = {
                            showMenu = false
                            onMarkAs(if (book.isFinished) "Unfinished" else "Finished")
                        },
                        leadingIcon = {
                            Icon(
                                if (book.isFinished) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle,
                                null,
                                tint = if (book.isFinished) theme.textSecondary else LithosMoss  // Lithos Moss for success
                            )
                        }
                    )
                }

                // 6. Split Book (only for AUDIO format books)
                if (isAudioBook && onSplitBook != null) {
                    DropdownMenuItem(
                        text = { Text("Split Book", color = theme.textPrimary) },
                        onClick = {
                            showMenu = false
                            onSplitBook()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.CallSplit, null, tint = theme.textSecondary)
                        }
                    )
                }

                HorizontalDivider(color = theme.divider)

                // 7. Change Cover
                if (onChangeCover != null) {
                    DropdownMenuItem(
                        text = { Text("Change Cover", color = theme.textPrimary) },
                        onClick = {
                            showMenu = false
                            onChangeCover()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Image, null, tint = theme.textSecondary)
                        }
                    )
                }

                // 8. Share
                if (onShare != null) {
                    DropdownMenuItem(
                        text = { Text("Share", color = theme.textPrimary) },
                        onClick = {
                            showMenu = false
                            onShare()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Share, null, tint = theme.textSecondary)
                        }
                    )
                }

                HorizontalDivider(color = theme.divider)

                // 9. Remove from Library (destructive, at bottom)
                if (onDelete != null) {
                    DropdownMenuItem(
                        text = { Text("Remove from Library", color = GlassColors.Destructive) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, null, tint = GlassColors.Destructive)
                        }
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
        containerColor = if (isDark) LithosUI.SheetBackground else LithosUI.SheetBackgroundLight,
        shape = RoundedCornerShape(LithosComponents.Cards.dialogRadius),
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
                            Icons.AutoMirrored.Filled.ArrowBack,
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
        containerColor = if (isDark) LithosUI.SheetBackground else LithosUI.SheetBackgroundLight,
        shape = RoundedCornerShape(LithosComponents.Cards.dialogRadius),
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

// Lithos Amber design for sort options
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
                    // Lithos Amber: subtle amber tint for selected
                    LithosAmber.copy(alpha = 0.12f)
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
            // Lithos Amber: selected text uses amber color
            color = if (isSelected) LithosAmber else theme.textPrimary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                // Lithos Amber: radio buttons use amber when selected
                selectedColor = LithosAmber,
                unselectedColor = theme.textTertiary
            )
        )
    }
}

// ============================================================================
// BRAND GRADIENT - Lithos Amber (matte, no glow)
// ============================================================================

private val BrandGradient = Brush.verticalGradient(
    colors = listOf(
        LithosAmber,           // Lithos Amber top
        Color(0xFFB57420)      // LithosAmberDark bottom
    )
)

// ============================================================================
// EMPTY STATE
// ============================================================================

@Composable
private fun EmptyLibraryState(
    isDark: Boolean,
    filterName: String,
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
                // Filtered empty state - minimal (library has audiobooks, just not in this category)
                Icon(
                    Icons.Rounded.Headphones,
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
