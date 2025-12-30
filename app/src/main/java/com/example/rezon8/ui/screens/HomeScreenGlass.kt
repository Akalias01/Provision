package com.mossglen.reverie.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mossglen.reverie.R
import com.mossglen.reverie.data.Book
import com.mossglen.reverie.ui.theme.*
import com.mossglen.reverie.ui.viewmodel.HomeViewModel
import com.mossglen.reverie.ui.viewmodel.PlayerViewModel
import com.mossglen.reverie.ui.viewmodel.LibraryViewModel
import com.mossglen.reverie.ui.components.BookDetailMorphingSheet
import com.mossglen.reverie.haptics.HapticType
import com.mossglen.reverie.haptics.performHaptic

/**
 * REVERIE Glass - Unified Home (10/10 Design)
 *
 * ONE unified experience - no separate "Now" and "Library".
 * The app IS your library. Your current book is the hero at top.
 *
 * Design Principles (Per PROJECT_MANIFEST):
 * - INVISIBLE PERFECTION: No modes to learn, no sections to understand
 * - ANTICIPATORY INTELLIGENCE: Contextual greeting with actionable info
 * - 2-TAP PRINCIPLE: Tap cover = play instantly
 * - PROGRESSIVE DISCLOSURE: Stats glanceable, tap for more
 *
 * Layout:
 * 1. Contextual Greeting with smart subtext
 * 2. Hero Card (current book, tap to play)
 * 3. Ambient Stats Bar (tappable to full stats)
 * 4. Filter Chips (All | In Progress | Finished | Authors | Series)
 * 5. Unified Library Grid (smart sorted)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreenGlass(
    homeViewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    isDark: Boolean = true,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.Interactive,
    scrollToTopTrigger: Int = 0,
    onBookClick: (String) -> Unit = {},
    onPlayBook: (Book) -> Unit = {},
    onSeriesClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onGenreClick: (String) -> Unit = {},
    onStatsClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onScrolling: (isScrollingDown: Boolean) -> Unit = {}  // Smart mini player hide
) {
    val theme = glassTheme(isDark, isReverieDark)
    val listState = rememberLazyListState()
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // Scroll to top when trigger changes
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
        }
    }

    // ===== SMART SCROLL DETECTION FOR MINI PLAYER =====
    var lastScrollOffset by remember { mutableIntStateOf(0) }
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentOffset = listState.firstVisibleItemIndex * 1000 + listState.firstVisibleItemScrollOffset
        val scrollDelta = currentOffset - lastScrollOffset
        if (kotlin.math.abs(scrollDelta) > 10) {
            onScrolling(scrollDelta > 0)  // true = scrolling down (content moving up)
        }
        lastScrollOffset = currentOffset
    }

    // Book preview state (UI3 morphing sheet)
    var bookForPreview by remember { mutableStateOf<Book?>(null) }

    // ══════════════════════════════════════════════════════════════
    // PULL TO REFRESH STATE
    // ══════════════════════════════════════════════════════════════
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    fun onRefresh() {
        scope.launch {
            isRefreshing = true
            view.performHaptic(HapticType.MediumTap)
            delay(1000)
            isRefreshing = false
            view.performHaptic(HapticType.Confirm)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // HOME VIEWMODEL STATE
    // ══════════════════════════════════════════════════════════════
    val mostRecentBook by homeViewModel.mostRecentBook.collectAsState()
    val stats by homeViewModel.stats.collectAsState()
    val booksInProgress by homeViewModel.booksInProgress.collectAsState()
    val finishedBooksCount by homeViewModel.finishedBooksCount.collectAsState()
    val recentlyPlayed by homeViewModel.recentlyPlayed.collectAsState()

    // ══════════════════════════════════════════════════════════════
    // LIBRARY VIEWMODEL STATE (Unified Home)
    // ══════════════════════════════════════════════════════════════
    val allBooks by libraryViewModel.books.collectAsState()

    // Player state
    val currentBook by playerViewModel.currentBook.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val position by playerViewModel.position.collectAsState()

    // Use current playing book if available, otherwise most recent
    val heroBook = currentBook ?: mostRecentBook

    // Recent books for swipeable hero (last 5, excluding current hero)
    val recentBooksForHero = remember(recentlyPlayed, heroBook) {
        val books = if (heroBook != null) {
            listOf(heroBook) + recentlyPlayed.filter { it.id != heroBook.id }.take(4)
        } else {
            recentlyPlayed.take(5)
        }
        books
    }

    // ══════════════════════════════════════════════════════════════
    // UI STATE
    // ══════════════════════════════════════════════════════════════
    var selectedFilter by remember { mutableStateOf(UnifiedFilter.IN_PROGRESS) } // Default to In Progress
    var contentMode by remember { mutableStateOf(ContentMode.LISTENING) }
    var viewMode by remember { mutableStateOf(true) } // true = grid, false = list

    // Filter books based on mode and filter selection
    val modeFilteredBooks = remember(allBooks, contentMode) {
        when (contentMode) {
            ContentMode.LISTENING -> allBooks.filter { it.format == "AUDIO" }
            ContentMode.READING -> allBooks.filter { it.format != "AUDIO" }
        }
    }

    val filteredBooks = remember(modeFilteredBooks, selectedFilter, heroBook) {
        val filtered = when (selectedFilter) {
            UnifiedFilter.ALL -> modeFilteredBooks
            UnifiedFilter.IN_PROGRESS -> modeFilteredBooks.filter { it.progress > 0 && !it.isFinished }
            UnifiedFilter.NOT_STARTED -> modeFilteredBooks.filter { it.progress == 0L && !it.isFinished }
            UnifiedFilter.FINISHED -> modeFilteredBooks.filter { it.isFinished }
        }
        // Exclude hero book from grid (it's shown above)
        // Sort by last played timestamp
        filtered
            .filter { it.id != heroBook?.id }
            .sortedByDescending { it.lastPlayedTimestamp }
    }

    // Get counts for filter chips (based on current mode)
    val inProgressCount = modeFilteredBooks.count { it.progress > 0 && !it.isFinished }
    val notStartedCount = modeFilteredBooks.count { it.progress == 0L && !it.isFinished }
    val finishedCount = modeFilteredBooks.count { it.isFinished }
    val allCount = modeFilteredBooks.size

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { onRefresh() },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(theme.background),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // ══════════════════════════════════════════════════════════════
                // HEADER: Clean Greeting only
                // ══════════════════════════════════════════════════════════════
                item {
                    Text(
                        text = getContextualGreeting(),
                        style = GlassTypography.Display,
                        color = theme.textPrimary,
                        modifier = Modifier
                            .padding(horizontal = GlassSpacing.M)
                            .padding(top = GlassSpacing.XXL)
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.L))
                }

                // ══════════════════════════════════════════════════════════════
                // HERO SECTION - Swipeable Recent Books (Stacked Layout)
                // ══════════════════════════════════════════════════════════════
                if (recentBooksForHero.isNotEmpty()) {
                    item {
                        SwipeableHeroSection(
                            books = recentBooksForHero,
                            currentPlayingBookId = currentBook?.id,
                            isPlaying = isPlaying,
                            position = position,
                            homeViewModel = homeViewModel,
                            isDark = isDark,
                            isReverieDark = isReverieDark,
                            accentColor = accentColor,
                            onPlayBook = { book ->
                                if (currentBook?.id == book.id) {
                                    playerViewModel.togglePlayback()
                                } else {
                                    onPlayBook(book)
                                }
                            },
                            onBookClick = { book -> bookForPreview = book },
                            onSeriesClick = onSeriesClick,
                            onAuthorClick = onAuthorClick
                        )
                        Spacer(modifier = Modifier.height(GlassSpacing.M))
                    }
                }

                // ══════════════════════════════════════════════════════════════
                // AMBIENT STATS BAR (Tappable - Progressive Disclosure)
                // ══════════════════════════════════════════════════════════════
                item {
                    AmbientStatsBar(
                        streakDays = stats.streakDays,
                        weeklyTime = stats.weekTimeFormatted,
                        booksFinished = finishedBooksCount,
                        inProgressCount = booksInProgress.size,
                        isDark = isDark,
                        isReverieDark = isReverieDark,
                        accentColor = accentColor,
                        onClick = {
                            view.performHaptic(HapticType.LightTap)
                            onStatsClick()
                        }
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.M))
                }

                // ══════════════════════════════════════════════════════════════
                // MODE TOGGLE + ICONS ROW (Listening/Reading + View/Add)
                // ══════════════════════════════════════════════════════════════
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = GlassSpacing.M),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Mode toggle
                        ModeToggle(
                            selectedMode = contentMode,
                            onModeSelected = { contentMode = it },
                            isDark = isDark,
                            accentColor = accentColor
                        )

                        // Right: View toggle + Add (larger, better icons)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // View toggle (Grid/List) - larger
                            IconButton(
                                onClick = {
                                    view.performHaptic(HapticType.LightTap)
                                    viewMode = !viewMode
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (viewMode) Icons.Outlined.ViewAgenda else Icons.Default.Apps,
                                    contentDescription = if (viewMode) "Switch to list" else "Switch to grid",
                                    tint = theme.textSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Add button - larger
                            IconButton(
                                onClick = {
                                    view.performHaptic(HapticType.MediumTap)
                                    onAddClick()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddCircleOutline,
                                    contentDescription = "Add book",
                                    tint = theme.textSecondary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(GlassSpacing.S))
                }

                // ══════════════════════════════════════════════════════════════
                // FILTER CHIPS (Horizontal Scroll)
                // ══════════════════════════════════════════════════════════════
                item {
                    FilterChipsRowUpdated(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it },
                        inProgressCount = inProgressCount,
                        notStartedCount = notStartedCount,
                        finishedCount = finishedCount,
                        allCount = allCount,
                        isDark = isDark,
                        accentColor = accentColor
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.M))
                }

                // ══════════════════════════════════════════════════════════════
                // UNIFIED LIBRARY GRID
                // ══════════════════════════════════════════════════════════════
                if (filteredBooks.isEmpty() && recentBooksForHero.isEmpty()) {
                    // Empty state - no books at all
                    item {
                        EmptyLibraryState(
                            isDark = isDark,
                            isReverieDark = isReverieDark,
                            accentColor = accentColor,
                            onAddClick = onAddClick
                        )
                    }
                } else if (filteredBooks.isEmpty()) {
                    // Empty state for this filter
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(GlassSpacing.XL),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (selectedFilter) {
                                    UnifiedFilter.IN_PROGRESS -> "No books in progress"
                                    UnifiedFilter.NOT_STARTED -> "No unstarted books"
                                    UnifiedFilter.FINISHED -> "No finished books yet"
                                    UnifiedFilter.ALL -> "No books found"
                                },
                                style = GlassTypography.Body,
                                color = theme.textSecondary
                            )
                        }
                    }
                } else {
                    // Book grid - 2 columns
                    items(filteredBooks.chunked(2)) { rowBooks ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = GlassSpacing.M),
                            horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
                        ) {
                            rowBooks.forEach { book ->
                                LibraryBookCard(
                                    book = book,
                                    isDark = isDark,
                                    isReverieDark = isReverieDark,
                                    accentColor = accentColor,
                                    onCoverClick = { onPlayBook(book) },
                                    onCardClick = { bookForPreview = book },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill empty space if odd number
                            if (rowBooks.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(GlassSpacing.S))
                    }
                }
            }
        } // End PullToRefreshBox

        // UI3: Morphing book detail sheet
        bookForPreview?.let { book ->
            BookDetailMorphingSheet(
                book = book,
                isVisible = true,
                accentColor = if (isReverieDark) accentColor else theme.interactive,
                isReverieDark = isReverieDark,
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
                }
            )
        }
    }
}

// ============================================================================
// FILTER ENUM
// ============================================================================

enum class UnifiedFilter {
    IN_PROGRESS, NOT_STARTED, FINISHED, ALL
}

enum class ContentMode {
    LISTENING, READING
}

// ============================================================================
// HERO SECTION - Unified Home Design (10/10)
// Large cover with tap-to-play, progress ring, minimal text
// ============================================================================

@Composable
private fun HeroSection(
    book: Book,
    isCurrentlyPlaying: Boolean,
    isPlaying: Boolean,
    progress: Float,
    timeRemaining: String,
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    onPlayClick: () -> Unit,
    onBookClick: () -> Unit,
    onSeriesClick: ((String) -> Unit)? = null,
    onAuthorClick: ((String) -> Unit)? = null
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current

    // Cover art press animation - subtle scale for play feedback
    val coverInteractionSource = remember { MutableInteractionSource() }
    val isCoverPressed by coverInteractionSource.collectIsPressedAsState()
    val coverScale by animateFloatAsState(
        targetValue = if (isCoverPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "coverScale"
    )

    // Play hint alpha - appears on cover press
    val playHintAlpha by animateFloatAsState(
        targetValue = if (isCoverPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "playHint"
    )

    // Glass background matching the nav pill style
    val glassBg = if (isReverieDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.95f)
    } else if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.92f)
    } else {
        Color(0xFFF2F2F7).copy(alpha = 0.95f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M)
    ) {
        // ══════════════════════════════════════════════════════════════
        // "CONTINUE LISTENING" SECTION HEADER - Simple text, NOT a button
        // ══════════════════════════════════════════════════════════════
        Text(
            text = stringResource(R.string.home_continue_listening),
            style = GlassTypography.Caption,
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(GlassSpacing.S))

        // ══════════════════════════════════════════════════════════════
        // HERO CARD - Side-by-side layout with progress RING
        // ══════════════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GlassShapes.Large))
                .background(glassBg)
                .padding(GlassSpacing.M)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // ─────────────────────────────────────────────────────────
                // COVER ART - 160dp (bigger than before), TAP TO PLAY/PAUSE
                // ─────────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            scaleX = coverScale
                            scaleY = coverScale
                            shadowElevation = 16f
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = coverInteractionSource,
                            indication = null
                        ) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            onPlayClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = book.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Play/Pause hint overlay - appears on press
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(playHintAlpha)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(GlassSpacing.M))

                // ─────────────────────────────────────────────────────────
                // TEXT CONTENT - Right side with title, author, progress ring
                // ─────────────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp),  // Match cover height
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top section: Title and Author
                    Column {
                        // Book title - up to 2 lines, tappable for details
                        Text(
                            text = book.title,
                            style = GlassTypography.Body,
                            color = theme.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            lineHeight = 21.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                                onBookClick()
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Author with chevron - tappable to author books
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                                    onAuthorClick?.invoke(book.author)
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "by ${book.author}",
                                style = GlassTypography.Caption,
                                color = if (onAuthorClick != null) accentColor.copy(alpha = 0.9f) else theme.textSecondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (onAuthorClick != null) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "View author's books",
                                    tint = accentColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Bottom section: Progress Ring with time remaining
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Progress Ring - completion-focused, psychologically encouraging
                        ProgressRing(
                            progress = progress,
                            accentColor = accentColor,
                            size = 48.dp,
                            strokeWidth = 4.dp,
                            backgroundColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f),
                            textColor = theme.textPrimary
                        )

                        Spacer(modifier = Modifier.width(GlassSpacing.S))

                        // Time remaining
                        Text(
                            text = timeRemaining,
                            style = GlassTypography.Caption,
                            color = theme.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// QUICK STATS ROW (4 compact cards)
// ============================================================================

@Composable
private fun QuickStatsRow(
    streakDays: Int,
    todayTime: String,
    inProgressCount: Int,
    finishedCount: Int,
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M),
        horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
    ) {
        QuickStatCard(
            icon = Icons.Filled.LocalFireDepartment,
            value = "$streakDays",
            label = stringResource(R.string.home_streak),
            isDark = isDark,
            isReverieDark = isReverieDark,
            accentColor = if (streakDays > 0) accentColor else null,
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            icon = Icons.Outlined.Schedule,
            value = todayTime.ifEmpty { "0m" },
            label = stringResource(R.string.stats_today),
            isDark = isDark,
            isReverieDark = isReverieDark,
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            icon = Icons.Outlined.PlayCircle,
            value = "$inProgressCount",
            label = stringResource(R.string.home_active),
            isDark = isDark,
            isReverieDark = isReverieDark,
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            icon = Icons.Outlined.CheckCircle,
            value = "$finishedCount",
            label = stringResource(R.string.home_done),
            isDark = isDark,
            isReverieDark = isReverieDark,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark, isReverieDark)
    val displayColor = accentColor ?: theme.textSecondary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(
                if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.8f)
                else Color(0xFFF2F2F7).copy(alpha = 0.8f)
            )
            .padding(vertical = GlassSpacing.S, horizontal = GlassSpacing.XS),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = displayColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = GlassTypography.Body,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary,
            maxLines = 1
        )
        Text(
            text = label,
            style = GlassTypography.Caption,
            color = theme.textSecondary,
            maxLines = 1
        )
    }
}

// ============================================================================
// SERIES CARD (for Your Series horizontal scroll)
// ============================================================================

@Composable
private fun SeriesCard(
    seriesName: String,
    books: List<Book>,
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .width(130.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
    ) {
        // Series cover collage (2x2 grid or single cover)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(GlassShapes.Medium))
                .background(theme.glassCard)
        ) {
            if (books.size >= 4) {
                // 2x2 grid of covers
                Column {
                    Row(modifier = Modifier.weight(1f)) {
                        AsyncImage(
                            model = books[0].coverUrl,
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = books[1].coverUrl,
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        AsyncImage(
                            model = books[2].coverUrl,
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = books[3].coverUrl,
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else {
                // Single cover for smaller series
                AsyncImage(
                    model = books.firstOrNull()?.coverUrl,
                    contentDescription = seriesName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Book count badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${books.size} ${stringResource(R.string.stats_books)}",
                    style = GlassTypography.Caption,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(GlassSpacing.XS))

        Text(
            text = seriesName,
            style = GlassTypography.Callout,
            color = theme.textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark, isReverieDark)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.06f else 0.10f),
                        theme.glassCard
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = theme.glassBorder,
                shape = RoundedCornerShape(GlassShapes.Medium)
            )
            .padding(GlassSpacing.M)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(GlassSpacing.S))
            Text(
                text = value,
                style = GlassTypography.Headline,
                color = theme.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = GlassTypography.Caption,
                color = theme.textSecondary
            )
        }
    }
}

// ============================================================================
// RECENT BOOK CARD (Horizontal Scroll)
// ============================================================================

@Composable
private fun RecentBookCard(
    book: Book,
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    onCoverClick: () -> Unit,  // Tap cover = play
    onTextClick: () -> Unit     // Tap text = show half sheet
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current

    // Separate interaction sources for cover and text
    val coverInteractionSource = remember { MutableInteractionSource() }
    val textInteractionSource = remember { MutableInteractionSource() }
    val isCoverPressed by coverInteractionSource.collectIsPressedAsState()
    val isTextPressed by textInteractionSource.collectIsPressedAsState()

    val coverScale by animateFloatAsState(
        targetValue = if (isCoverPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "coverScale"
    )

    val textScale by animateFloatAsState(
        targetValue = if (isTextPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "textScale"
    )

    // Play hint alpha - appears on press
    val playHintAlpha by animateFloatAsState(
        targetValue = if (isCoverPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "playHint"
    )

    Column(
        modifier = Modifier.width(130.dp)
    ) {
        // Cover art - tap to play (1:1 square)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = coverScale
                    scaleY = coverScale
                }
                .clip(RoundedCornerShape(GlassShapes.Medium))
                .clickable(
                    interactionSource = coverInteractionSource,
                    indication = null
                ) {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onCoverClick()
                }
        ) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { shadowElevation = 4f },
                contentScale = ContentScale.Crop
            )

            // Progress indicator at bottom
            if (book.progress > 0 && book.duration > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(book.progressPercent())
                            .fillMaxHeight()
                            .background(accentColor)
                    )
                }
            }

            // Play hint - appears on press (no persistent overlay)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .alpha(playHintAlpha)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(GlassSpacing.XS))

        // Text area - tap to show half sheet
        Column(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = textScale
                    scaleY = textScale
                }
                .clickable(
                    interactionSource = textInteractionSource,
                    indication = null
                ) {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onTextClick()
                }
        ) {
            Text(
                text = book.title,
                style = GlassTypography.Callout,
                color = theme.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
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
    }
}

// ============================================================================
// RECOMMENDATION SECTION
// ============================================================================

@Composable
private fun RecommendationSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    books: List<Book>,
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    onBookClick: (String) -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GlassSpacing.M),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(GlassSpacing.S))
            Column {
                Text(
                    text = title,
                    style = GlassTypography.Title,
                    color = theme.textPrimary
                )
                Text(
                    text = subtitle,
                    style = GlassTypography.Caption,
                    color = theme.textSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(GlassSpacing.M))

        books.forEach { book ->
            RecommendationBookItem(
                book = book,
                isDark = isDark,
                isReverieDark = isReverieDark,
                accentColor = accentColor,
                onClick = { onBookClick(book.id) }
            )
        }
    }
}

@Composable
private fun RecommendationBookItem(
    book: Book,
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M, vertical = GlassSpacing.XS)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(GlassShapes.Small))
            .background(theme.glassCard)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
            .padding(GlassSpacing.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = book.coverUrl,
            contentDescription = book.title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .graphicsLayer { shadowElevation = 2f },
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(GlassSpacing.S))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = GlassTypography.Body,
                color = theme.textPrimary,
                fontWeight = FontWeight.SemiBold,
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
            if (book.seriesInfo.isNotBlank()) {
                Text(
                    text = book.seriesInfo,
                    style = GlassTypography.Caption,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = theme.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================================================
// PREMIUM PLAY BUTTON
// ============================================================================

@Composable
private fun PremiumPlayButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    accentColor: Color,
    isDark: Boolean,
    isReverieDark: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "scale"
    )

    // Muted button style - dark background with accent icon/text
    // Similar to play button in full player - focus stays on cover art
    val buttonBg = if (isReverieDark) {
        Color(0xFF1C1C1E)  // Dark card background
    } else if (isDark) {
        Color(0xFF2C2C2E)  // Slightly lighter for dark mode
    } else {
        Color(0xFF1C1C1E)  // Dark for light mode too (for contrast)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)  // Slightly smaller
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(GlassShapes.Small))
            .background(buttonBg)
            .border(
                width = 0.5.dp,
                color = accentColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(GlassShapes.Small)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,  // Accent color for icon
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(GlassSpacing.S))
            Text(
                text = text,
                style = GlassTypography.Label,
                color = Color.White,  // White text for readability
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ============================================================================
// PROGRESS RING - Circular progress indicator with percentage
// ============================================================================

@Composable
private fun ProgressRing(
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 4.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.15f),
    textColor: Color = Color.White
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progressAnimation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = strokeWidth.toPx()
            val radius = (this.size.minDimension - strokeWidthPx) / 2
            val arcTopLeft = Offset(
                (this.size.width - radius * 2) / 2,
                (this.size.height - radius * 2) / 2
            )
            val arcSize = Size(radius * 2, radius * 2)

            // Background arc
            drawArc(
                color = backgroundColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        // Percentage text in center
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = GlassTypography.Caption,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Returns contextual greeting based on time of day (Anticipatory Intelligence)
 * - "Good morning" (5am-12pm)
 * - "Good afternoon" (12pm-5pm)
 * - "Good evening" (5pm-9pm)
 * - "Good night" (9pm-5am)
 */
@Composable
private fun getContextualGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> stringResource(R.string.home_good_morning)
        in 12..16 -> stringResource(R.string.home_good_afternoon)
        in 17..20 -> stringResource(R.string.home_good_evening)
        else -> stringResource(R.string.home_good_night) // 9pm-5am
    }
}

@Composable
private fun getGreetingString(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..11 -> stringResource(R.string.home_good_morning)
        in 12..16 -> stringResource(R.string.home_good_afternoon)
        else -> stringResource(R.string.home_good_evening)
    }
}

/**
 * Returns contextual subtext based on user state (Anticipatory Intelligence)
 * Prioritizes: time remaining > streak > finished count
 */
@Composable
private fun getContextualSubtext(
    heroBook: Book?,
    timeRemaining: String,
    streakDays: Int,
    finishedCount: Int
): String {
    return when {
        // Has a book with time remaining
        heroBook != null && timeRemaining.isNotEmpty() && timeRemaining != "Almost done" -> {
            timeRemaining
        }
        heroBook != null && timeRemaining == "Almost done" -> {
            "Almost done!"
        }
        // Active streak (7+ days is impressive)
        streakDays >= 7 -> {
            "🔥 $streakDays-day streak! Keep it going"
        }
        streakDays > 0 -> {
            "🔥 $streakDays-day streak"
        }
        // Has finished books
        finishedCount > 0 -> {
            "$finishedCount books finished"
        }
        // New user
        else -> {
            "Start your listening journey"
        }
    }
}

/**
 * Returns contextual subtext WITHOUT time (time is in hero card)
 * Shows streak or finished count only
 */
@Composable
private fun getContextualSubtextNoTime(
    streakDays: Int,
    finishedCount: Int
): String {
    return when {
        // Active streak (7+ days is impressive)
        streakDays >= 7 -> {
            "🔥 $streakDays-day streak! Keep it going"
        }
        streakDays > 0 -> {
            "🔥 $streakDays-day streak"
        }
        // Has finished books
        finishedCount > 0 -> {
            "$finishedCount books finished"
        }
        // New user - empty, don't show anything redundant
        else -> ""
    }
}

// ============================================================================
// SWIPEABLE HERO SECTION - Stacked layout with HorizontalPager
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableHeroSection(
    books: List<Book>,
    currentPlayingBookId: String?,
    isPlaying: Boolean,
    position: Long,
    homeViewModel: HomeViewModel,
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    onPlayBook: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onSeriesClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { books.size })

    // Auto-scroll back to first page after 5 seconds of inactivity
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(pagerState.currentPage) {
        lastInteractionTime = System.currentTimeMillis()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val timeSinceInteraction = System.currentTimeMillis() - lastInteractionTime
            if (timeSinceInteraction > 5000 && pagerState.currentPage != 0) {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M)
    ) {
        // Header: "Continue Listening" or "Now Playing"
        Text(
            text = if (currentPlayingBookId != null) "NOW PLAYING" else "CONTINUE LISTENING",
            style = GlassTypography.Label,
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(GlassSpacing.M))

        // Swipeable pager with beautiful card transition
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 16.dp
        ) { page ->
            val book = books[page]
            val isCurrentBook = book.id == currentPlayingBookId
            val progress = if (isCurrentBook && book.duration > 0) {
                position.toFloat() / book.duration.toFloat()
            } else {
                book.progressPercent()
            }
            val timeRemaining = homeViewModel.formatTimeRemaining(homeViewModel.getTimeRemaining(book))

            // Calculate page offset for beautiful card effect
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val absOffset = kotlin.math.abs(pageOffset)

            // Scale: 1.0 for current page, 0.9 for adjacent pages
            val scale = 1f - (absOffset * 0.1f).coerceIn(0f, 0.1f)
            // Alpha: 1.0 for current page, 0.6 for adjacent pages
            val alpha = 1f - (absOffset * 0.4f).coerceIn(0f, 0.4f)
            // Translation Y: cards slightly rise as they become current
            val translationY = absOffset * 20f

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        this.translationY = translationY
                        // Add subtle shadow for depth
                        shadowElevation = (1f - absOffset).coerceIn(0f, 1f) * 8f
                    }
            ) {
                StackedHeroCard(
                    book = book,
                    progress = progress,
                    timeRemaining = timeRemaining,
                    isPlaying = isCurrentBook && isPlaying,
                    isDark = isDark,
                    isReverieDark = isReverieDark,
                    accentColor = accentColor,
                    onPlayClick = { onPlayBook(book) },
                    onBookClick = { onBookClick(book) },
                    onAuthorClick = { onAuthorClick(book.author) }
                )
            }
        }

        // Page indicator dots (if more than 1 book)
        if (books.size > 1) {
            Spacer(modifier = Modifier.height(GlassSpacing.S))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                books.forEachIndexed { index, _ ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) accentColor
                                else theme.textTertiary.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}

// ============================================================================
// HERO CARD - Cover left with title below, ring right (compact, no wasted space)
// ============================================================================

@Composable
private fun StackedHeroCard(
    book: Book,
    progress: Float,
    timeRemaining: String,
    isPlaying: Boolean,
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    onPlayClick: () -> Unit,
    onBookClick: () -> Unit,
    onAuthorClick: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current

    // Cover press animation
    val coverInteractionSource = remember { MutableInteractionSource() }
    val isCoverPressed by coverInteractionSource.collectIsPressedAsState()
    val coverScale by animateFloatAsState(
        targetValue = if (isCoverPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "coverScale"
    )
    val playHintAlpha by animateFloatAsState(
        targetValue = if (isCoverPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "playHint"
    )

    val glassBg = if (isReverieDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.95f)
    } else if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.92f)
    } else {
        Color(0xFFF2F2F7).copy(alpha = 0.95f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Large))
            .background(glassBg)
            .padding(GlassSpacing.M)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // TOP ROW: Cover on left, Ring on right (with spacing)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT: Cover art - tap to play (LARGER - focal point)
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .graphicsLayer {
                            scaleX = coverScale
                            scaleY = coverScale
                            shadowElevation = 16f
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = coverInteractionSource,
                            indication = null
                        ) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            onPlayClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = book.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Play/Pause hint overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(playHintAlpha)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                // RIGHT: Progress ring + time (with more spacing from cover)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(start = GlassSpacing.L)  // Space from cover
                        .padding(end = GlassSpacing.M)    // Space from edge
                ) {
                    ProgressRing(
                        progress = progress,
                        accentColor = accentColor,
                        size = 80.dp,
                        strokeWidth = 6.dp,
                        backgroundColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f),
                        textColor = theme.textPrimary
                    )

                    Spacer(modifier = Modifier.height(GlassSpacing.XS))

                    Text(
                        text = timeRemaining,
                        style = GlassTypography.Caption,
                        color = theme.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(GlassSpacing.S))

            // BOTTOM: Title and Author (below the cover area)
            Text(
                text = book.title,
                style = GlassTypography.Title,
                color = theme.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                        onBookClick()
                    }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "by ${book.author}",
                style = GlassTypography.Caption,
                color = accentColor.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                    onAuthorClick()
                }
            )
        }
    }
}

// ============================================================================
// MODE TOGGLE - Listening / Reading segment control (subtle)
// ============================================================================

@Composable
private fun ModeToggle(
    selectedMode: ContentMode,
    onModeSelected: (ContentMode) -> Unit,
    isDark: Boolean,
    accentColor: Color
) {
    val view = LocalView.current

    // Compact segment control - no outer padding, just the toggle itself
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(GlassShapes.Small))
            .background(
                if (isDark) Color.White.copy(alpha = 0.06f)
                else Color.Black.copy(alpha = 0.05f)
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ModeToggleItem(
            icon = Icons.Outlined.Headphones,
            label = "Listening",
            isSelected = selectedMode == ContentMode.LISTENING,
            onClick = {
                view.performHaptic(HapticType.LightTap)
                onModeSelected(ContentMode.LISTENING)
            },
            isDark = isDark,
            accentColor = accentColor
        )
        ModeToggleItem(
            icon = Icons.Outlined.MenuBook,
            label = "Reading",
            isSelected = selectedMode == ContentMode.READING,
            onClick = {
                view.performHaptic(HapticType.LightTap)
                onModeSelected(ContentMode.READING)
            },
            isDark = isDark,
            accentColor = accentColor
        )
    }
}

@Composable
private fun ModeToggleItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    accentColor: Color
) {
    val theme = glassTheme(isDark, false)

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent,
        label = "bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else theme.textSecondary,
        label = "content"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(GlassShapes.Small))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),  // Smaller padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(14.dp)  // Smaller icon
        )
        Text(
            text = label,
            style = GlassTypography.Label,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 11.sp  // Smaller text
        )
    }
}

// ============================================================================
// FILTER CHIPS ROW (Updated with new filters)
// ============================================================================

@Composable
private fun FilterChipsRowUpdated(
    selectedFilter: UnifiedFilter,
    onFilterSelected: (UnifiedFilter) -> Unit,
    inProgressCount: Int,
    notStartedCount: Int,
    finishedCount: Int,
    allCount: Int,
    isDark: Boolean,
    accentColor: Color
) {
    val view = LocalView.current
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = GlassSpacing.M),
        horizontalArrangement = Arrangement.spacedBy(GlassSpacing.XS)
    ) {
        FilterChip(
            label = "In Progress",
            count = inProgressCount,
            isSelected = selectedFilter == UnifiedFilter.IN_PROGRESS,
            onClick = {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                onFilterSelected(UnifiedFilter.IN_PROGRESS)
            },
            isDark = isDark,
            accentColor = accentColor
        )
        FilterChip(
            label = "Not Started",
            count = notStartedCount,
            isSelected = selectedFilter == UnifiedFilter.NOT_STARTED,
            onClick = {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                onFilterSelected(UnifiedFilter.NOT_STARTED)
            },
            isDark = isDark,
            accentColor = accentColor
        )
        FilterChip(
            label = "Finished",
            count = finishedCount,
            isSelected = selectedFilter == UnifiedFilter.FINISHED,
            onClick = {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                onFilterSelected(UnifiedFilter.FINISHED)
            },
            isDark = isDark,
            accentColor = accentColor
        )
        FilterChip(
            label = "All",
            count = allCount,
            isSelected = selectedFilter == UnifiedFilter.ALL,
            onClick = {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                onFilterSelected(UnifiedFilter.ALL)
            },
            isDark = isDark,
            accentColor = accentColor
        )
    }
}

// ============================================================================
// AMBIENT STATS BAR - Tappable summary (Progressive Disclosure)
// ============================================================================

@Composable
private fun AmbientStatsBar(
    streakDays: Int,
    weeklyTime: String,
    booksFinished: Int,
    inProgressCount: Int,
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )

    // Build stats text
    val statsText = buildString {
        if (weeklyTime.isNotEmpty() && weeklyTime != "0m") {
            append("This week: $weeklyTime")
        }
        if (inProgressCount > 0) {
            if (isNotEmpty()) append(" • ")
            append("$inProgressCount in progress")
        }
        if (streakDays > 0) {
            if (isNotEmpty()) append(" • ")
            append("🔥 $streakDays")
        }
    }.ifEmpty { "Tap to see your stats" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(GlassShapes.Small))
            .background(
                if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.8f)
                else Color(0xFFF2F2F7).copy(alpha = 0.9f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
            .padding(horizontal = GlassSpacing.M, vertical = GlassSpacing.S)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GlassSpacing.XS)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = "Stats",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = statsText,
                    style = GlassTypography.Caption,
                    color = theme.textSecondary,
                    fontSize = 13.sp
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "View stats",
                tint = theme.textTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ============================================================================
// FILTER CHIP COMPONENT
// ============================================================================

@Composable
private fun FilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    accentColor: Color
) {
    val theme = glassTheme(isDark, false)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> accentColor.copy(alpha = 0.2f)
            isDark -> Color.White.copy(alpha = 0.08f)
            else -> Color.Black.copy(alpha = 0.06f)
        },
        label = "bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else theme.textSecondary,
        label = "text"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(GlassShapes.Small))
            .background(backgroundColor)
            .then(
                if (isSelected) Modifier.border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(GlassShapes.Small)
                ) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (count > 0) "$label ($count)" else label,
            style = GlassTypography.Label,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

// ============================================================================
// LIBRARY BOOK CARD (Grid Item)
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryBookCard(
    book: Book,
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    onCoverClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current
    val coverInteractionSource = remember { MutableInteractionSource() }
    val isCoverPressed by coverInteractionSource.collectIsPressedAsState()

    val coverScale by animateFloatAsState(
        targetValue = if (isCoverPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "coverScale"
    )

    val playHintAlpha by animateFloatAsState(
        targetValue = if (isCoverPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "playHint"
    )

    Column(modifier = modifier) {
        // Cover - tap to play (square for consistent grid)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = coverScale
                    scaleY = coverScale
                }
                .clip(RoundedCornerShape(GlassShapes.Medium))
                .clickable(
                    interactionSource = coverInteractionSource,
                    indication = null
                ) {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onCoverClick()
                }
        ) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Progress bar at bottom
            if (book.progress > 0 && book.duration > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(book.progressPercent())
                            .fillMaxHeight()
                            .background(accentColor)
                    )
                }
            }

            // Play hint on press
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .alpha(playHintAlpha)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(GlassSpacing.XS))

        // Text - tap for details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onCardClick()
                }
        ) {
            Text(
                text = book.title,
                style = GlassTypography.Callout,
                color = theme.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
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
// EMPTY LIBRARY STATE
// ============================================================================

@Composable
private fun EmptyLibraryState(
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    onAddClick: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(GlassSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.LibraryBooks,
            contentDescription = null,
            tint = theme.textTertiary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(GlassSpacing.M))
        Text(
            text = "Your library is empty",
            style = GlassTypography.Title,
            color = theme.textPrimary
        )
        Spacer(modifier = Modifier.height(GlassSpacing.XS))
        Text(
            text = "Add your first audiobook to get started",
            style = GlassTypography.Body,
            color = theme.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(GlassSpacing.L))
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(GlassShapes.Medium))
                .background(GlassColors.ButtonBackground)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onAddClick()
                }
                .padding(horizontal = GlassSpacing.L, vertical = GlassSpacing.M)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Add Audiobook",
                    style = GlassTypography.Label,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
