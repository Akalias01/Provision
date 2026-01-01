package com.mossglen.lithos.ui.screens

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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.util.lerp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
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
import com.mossglen.lithos.R
import com.mossglen.lithos.data.Book
import com.mossglen.lithos.ui.theme.*
import com.mossglen.lithos.ui.theme.LithosUI
import com.mossglen.lithos.ui.theme.LithosComponents
import com.mossglen.lithos.ui.theme.GlassIconSize
import com.mossglen.lithos.ui.viewmodel.HomeViewModel
import com.mossglen.lithos.ui.viewmodel.PlayerViewModel
import com.mossglen.lithos.ui.viewmodel.LibraryViewModel
import com.mossglen.lithos.ui.components.BookDetailMorphingSheet
import com.mossglen.lithos.haptics.HapticType
import com.mossglen.lithos.haptics.performHaptic

/**
 * LITHOS AMBER Design Language - Home Screen
 *
 * ONE unified experience - no separate "Now" and "Library".
 * The app IS your library. Your current book is the hero at top.
 *
 * Lithos Design Principles:
 * - MATTE/SATIN FINISH: No glow effects, no gradients
 * - FROSTED GLASS: rgba(26, 29, 33, 0.85) with 20dp blur
 * - AMBER ACCENT (#D48C2C): Progress indicators, active/selected states
 * - MOSS (#4A5D45): Primary action buttons (play)
 * - THIN STROKES: 2-3dp for progress rings
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
    isOLED: Boolean = false,
    accentColor: Color = LithosAmber,  // Lithos Amber for active/selected states
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
    val theme = glassTheme(isDark, isOLED)
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
                            isOLED = isOLED,
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
                        isOLED = isOLED,
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
                                    modifier = Modifier.size(GlassIconSize.Medium)
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
                            isOLED = isOLED,
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
                                    isOLED = isOLED,
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
    isOLED: Boolean,
    accentColor: Color,
    onPlayClick: () -> Unit,
    onBookClick: () -> Unit,
    onSeriesClick: ((String) -> Unit)? = null,
    onAuthorClick: ((String) -> Unit)? = null
) {
    val theme = glassTheme(isDark, isOLED)
    val view = LocalView.current

    // Cover art press animation - Apple-quality spring physics
    // Uses DampingRatioMediumBouncy with StiffnessLow for premium feel
    val coverInteractionSource = remember { MutableInteractionSource() }
    val isCoverPressed by coverInteractionSource.collectIsPressedAsState()
    val coverScale by animateFloatAsState(
        targetValue = if (isCoverPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,  // 0.75 - natural with subtle overshoot
            stiffness = Spring.StiffnessLow  // 200 - smooth, not snappy
        ),
        label = "coverScale"
    )

    // Play hint alpha - smooth fade with no bounce
    val playHintAlpha by animateFloatAsState(
        targetValue = if (isCoverPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,  // Clean fade
            stiffness = Spring.StiffnessMedium  // Responsive
        ),
        label = "playHint"
    )

    // Lithos frosted glass background - matte/satin finish, no glow
    val glassBg = if (isDark) {
        LithosColors.FrostedGlass  // rgba(26, 29, 33, 0.85)
    } else {
        LithosColors.FrostedGlassLight
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M)
    ) {
        // ══════════════════════════════════════════════════════════════
        // "CONTINUE LISTENING" SECTION HEADER - Lithos Amber
        // ══════════════════════════════════════════════════════════════
        Text(
            text = stringResource(R.string.home_continue_listening),
            style = GlassTypography.Caption,
            color = LithosColors.Amber,  // Lithos Amber
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
                        .clip(RoundedCornerShape(LithosComponents.Buttons.cornerRadius))
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

                        // Author with chevron - tappable to author books - Lithos Amber
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
                                color = if (onAuthorClick != null) LithosColors.Amber.copy(alpha = 0.9f) else theme.textSecondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (onAuthorClick != null) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "View author's books",
                                    tint = LithosColors.Amber.copy(alpha = 0.6f),  // Lithos Amber
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
                        // Lithos Progress Ring - thin stroke (2.5dp), Amber accent
                        ProgressRing(
                            progress = progress,
                            accentColor = LithosColors.Amber,  // Lithos Amber for progress
                            size = 48.dp,
                            strokeWidth = 2.5.dp,  // Lithos thin stroke
                            backgroundColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
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
// SERIES CARD (for Your Series horizontal scroll)
// ============================================================================

@Composable
private fun SeriesCard(
    seriesName: String,
    books: List<Book>,
    isDark: Boolean,
    isOLED: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
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
    isOLED: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark, isOLED)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GlassShapes.Medium))
            // Lithos: Matte/satin finish - no gradient, solid frosted glass
            .background(
                if (isDark) LithosColors.FrostedGlass
                else LithosColors.FrostedGlassLight
            )
            .border(
                width = 0.5.dp,
                color = LithosColors.BorderMatte,  // Lithos matte border
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
                tint = LithosColors.Amber,  // Lithos Amber accent
                modifier = Modifier.size(GlassIconSize.Medium)
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
    isOLED: Boolean,
    accentColor: Color,
    onCoverClick: () -> Unit,  // Tap cover = play
    onTextClick: () -> Unit     // Tap text = show half sheet
) {
    val theme = glassTheme(isDark, isOLED)
    val view = LocalView.current

    // Separate interaction sources for cover and text
    val coverInteractionSource = remember { MutableInteractionSource() }
    val textInteractionSource = remember { MutableInteractionSource() }
    val isCoverPressed by coverInteractionSource.collectIsPressedAsState()
    val isTextPressed by textInteractionSource.collectIsPressedAsState()

    val coverScale by animateFloatAsState(
        targetValue = if (isCoverPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "coverScale"
    )

    val textScale by animateFloatAsState(
        targetValue = if (isTextPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "textScale"
    )

    // Play hint alpha - appears on press
    val playHintAlpha by animateFloatAsState(
        targetValue = if (isCoverPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
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

            // Lithos progress indicator - thin bar with Amber accent
            if (book.progress > 0 && book.duration > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)  // Lithos thin stroke
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(book.progressPercent())
                            .fillMaxHeight()
                            .background(LithosColors.Amber)  // Lithos Amber progress
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
    isOLED: Boolean,
    accentColor: Color,
    onBookClick: (String) -> Unit
) {
    val theme = glassTheme(isDark, isOLED)

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
                tint = LithosColors.Amber,  // Lithos Amber accent
                modifier = Modifier.size(GlassIconSize.Medium)
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
                isOLED = isOLED,
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
    isOLED: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
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
                .clip(RoundedCornerShape(LithosComponents.Cards.chipRadius))
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
                    color = LithosColors.Amber,  // Lithos Amber for series
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = theme.textTertiary,
            modifier = Modifier.size(GlassIconSize.Small)
        )
    }
}

// ============================================================================
// PREMIUM PLAY BUTTON - Uses Lithos Moss for primary action
// ============================================================================

@Composable
private fun PremiumPlayButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    accentColor: Color,
    isDark: Boolean,
    isOLED: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // Lithos: Moss background for primary play action, matte finish
    val buttonBg = LithosColors.Moss

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(LithosComponents.Buttons.height)  // Standardized button height
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(LithosComponents.Buttons.cornerRadius))
            .background(buttonBg)
            // Lithos: No glow border, just subtle matte border
            .border(
                width = LithosComponents.Cards.borderWidth,
                color = LithosColors.Moss.copy(alpha = 0.5f),
                shape = RoundedCornerShape(LithosComponents.Buttons.cornerRadius)
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
                tint = LithosColors.Oat,  // Lithos Oat for icon on Moss
                modifier = Modifier.size(GlassIconSize.Medium)
            )
            Spacer(modifier = Modifier.width(GlassSpacing.S))
            Text(
                text = text,
                style = GlassTypography.Label,
                color = LithosColors.Oat,  // Lithos Oat text for readability
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ============================================================================
// PROGRESS RING - Lithos thin stroke circular progress indicator
// ============================================================================

@Composable
private fun ProgressRing(
    progress: Float,
    accentColor: Color = LithosColors.Amber,  // Default to Lithos Amber
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 2.5.dp,  // Lithos thin stroke
    backgroundColor: Color = Color.White.copy(alpha = 0.12f),
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
        // Lithos: No emoji for matte feel
        streakDays >= 7 -> {
            "$streakDays-day streak! Keep it going"
        }
        streakDays > 0 -> {
            "$streakDays-day streak"
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
 * Lithos: No emoji for matte feel
 */
@Composable
private fun getContextualSubtextNoTime(
    streakDays: Int,
    finishedCount: Int
): String {
    return when {
        // Lithos: No emoji for matte feel
        streakDays >= 7 -> {
            "$streakDays-day streak! Keep it going"
        }
        streakDays > 0 -> {
            "$streakDays-day streak"
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
// PAGER OFFSET HELPERS - Premium animation calculations
// ============================================================================

/**
 * Calculate offset for a page relative to current scroll position.
 * Returns negative for pages to the right, positive for pages to the left.
 * Range: approximately -1 to +1 for adjacent pages.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun androidx.compose.foundation.pager.PagerState.offsetForPage(page: Int): Float {
    return (currentPage - page) + currentPageOffsetFraction
}

/**
 * Get offset clamped to positive values (pages scrolling out to the left).
 */
@OptIn(ExperimentalFoundationApi::class)
private fun androidx.compose.foundation.pager.PagerState.startOffsetForPage(page: Int): Float {
    return offsetForPage(page).coerceAtLeast(0f)
}

/**
 * Get offset clamped to negative values (pages scrolling in from the right).
 */
@OptIn(ExperimentalFoundationApi::class)
private fun androidx.compose.foundation.pager.PagerState.endOffsetForPage(page: Int): Float {
    return offsetForPage(page).coerceAtMost(0f)
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
    isOLED: Boolean,
    accentColor: Color,
    onPlayBook: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onSeriesClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { books.size })

    // Track last user interaction for auto-return
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Update interaction time when user drags
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling: Boolean ->
                if (isScrolling) {
                    lastInteractionTime = System.currentTimeMillis()
                }
            }
    }

    // Apple-quality auto-return: scroll back to first card after 4 seconds
    // Uses spring animation for smooth, natural feel
    LaunchedEffect(Unit) {
        while (true) {
            delay(500) // Check twice per second for responsive feel

            val currentPage = pagerState.currentPage
            val isSettled = !pagerState.isScrollInProgress
            val timeSinceInteraction = System.currentTimeMillis() - lastInteractionTime

            if (currentPage > 0 && isSettled && timeSinceInteraction > 4000) {
                // Smoothly scroll back with spring animation
                pagerState.animateScrollToPage(
                    page = 0,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,  // Subtle bounce
                        stiffness = Spring.StiffnessLow  // Smooth, not snappy
                    )
                )
                // Reset timer after auto-scroll
                lastInteractionTime = System.currentTimeMillis()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M)
    ) {
        // Header: "Continue Listening" or "Now Playing" - Lithos Amber
        Text(
            text = if (currentPlayingBookId != null) "NOW PLAYING" else "CONTINUE LISTENING",
            style = GlassTypography.Label,
            color = LithosColors.Amber,  // Lithos Amber for active state
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

            // ============================================================
            // PREMIUM APPLE-QUALITY CARD TRANSITION
            // Uses lerp for smooth interpolation, 3D depth effects
            // ============================================================
            val pageOffset = pagerState.offsetForPage(page)
            val absOffset = kotlin.math.abs(pageOffset).coerceIn(0f, 1f)

            // Scale: lerp from 1.0 (centered) to 0.88 (off-screen)
            // Creates depth perception - centered card is largest
            val scale = lerp(
                start = 0.88f,
                stop = 1f,
                fraction = 1f - absOffset
            )

            // Alpha: subtle fade for off-center cards
            val alpha = lerp(
                start = 0.5f,
                stop = 1f,
                fraction = 1f - absOffset
            )

            // Rotation Y: 3D carousel effect - cards rotate away from center
            // Negative offset (right side) rotates left, positive rotates right
            val rotationY = lerp(
                start = 0f,
                stop = if (pageOffset < 0) -10f else 10f,
                fraction = absOffset.coerceIn(0f, 1f)
            )

            // Shadow elevation: centered card has more shadow (depth effect)
            val shadowElevation = lerp(
                start = 4f,
                stop = 20f,
                fraction = 1f - absOffset
            )

            // Slight vertical offset for stacking illusion
            val translationY = lerp(
                start = 0f,
                stop = 12f,
                fraction = absOffset
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        this.rotationY = rotationY
                        this.translationY = translationY
                        cameraDistance = 12f * density  // Smooth 3D perspective
                        this.shadowElevation = shadowElevation
                        // Transform from center for natural rotation
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
            ) {
                StackedHeroCard(
                    book = book,
                    progress = progress,
                    timeRemaining = timeRemaining,
                    isPlaying = isCurrentBook && isPlaying,
                    isDark = isDark,
                    isOLED = isOLED,
                    accentColor = accentColor,
                    onPlayClick = { onPlayBook(book) },
                    onBookClick = { onBookClick(book) },
                    onAuthorClick = { onAuthorClick(book.author) },
                    onSeriesClick = {
                        // Extract series name from seriesInfo (e.g., "Book 3 of Mercenary" -> "Mercenary")
                        val seriesName = book.seriesInfo.substringAfter("of ").trim()
                            .ifBlank { book.seriesInfo }
                        onSeriesClick(seriesName)
                    }
                )
            }
        }

        // Lithos page indicator dots - Amber for selected
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
                                if (isSelected) LithosColors.Amber  // Lithos Amber for selected
                                else theme.textTertiary.copy(alpha = 0.35f)
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
    isOLED: Boolean,
    accentColor: Color,
    onPlayClick: () -> Unit,
    onBookClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onSeriesClick: () -> Unit = {}
) {
    val theme = glassTheme(isDark, isOLED)
    val view = LocalView.current

    // Cover press animation
    val coverInteractionSource = remember { MutableInteractionSource() }
    val isCoverPressed by coverInteractionSource.collectIsPressedAsState()
    val coverScale by animateFloatAsState(
        targetValue = if (isCoverPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "coverScale"
    )
    val playHintAlpha by animateFloatAsState(
        targetValue = if (isCoverPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "playHint"
    )

    // Lithos frosted glass background - matte/satin finish
    val glassBg = if (isDark) {
        LithosColors.FrostedGlass  // rgba(26, 29, 33, 0.85)
    } else {
        LithosColors.FrostedGlassLight
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
                        .clip(RoundedCornerShape(LithosComponents.Buttons.cornerRadius))
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
                    // Lithos Progress Ring - thin stroke (3dp), Amber accent
                    ProgressRing(
                        progress = progress,
                        accentColor = LithosColors.Amber,  // Lithos Amber
                        size = 80.dp,
                        strokeWidth = 3.dp,  // Lithos thin stroke
                        backgroundColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
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

            // BOTTOM: Title, Author, and Series Info (below the cover area)

            // Title - taps to series if available, otherwise book detail
            val hasSeries = book.seriesInfo.isNotBlank()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                        if (hasSeries) {
                            onSeriesClick()
                        } else {
                            onBookClick()
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = book.title,
                    style = GlassTypography.Title,
                    color = theme.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (hasSeries) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Go to series",
                        tint = theme.textTertiary,
                        modifier = Modifier.size(GlassIconSize.Small)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Author - always navigates to author page - Lithos Amber
            Row(
                modifier = Modifier
                    .clickable {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                        onAuthorClick()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "by ${book.author}",
                    style = GlassTypography.Caption,
                    color = LithosColors.Amber.copy(alpha = 0.9f),  // Lithos Amber
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Go to author",
                    tint = LithosColors.Amber.copy(alpha = 0.6f),  // Lithos Amber
                    modifier = Modifier.size(16.dp)
                )
            }

            // Series info chip - Lithos Amber tappable
            if (hasSeries) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(LithosComponents.Cards.chipRadius))
                        .background(LithosColors.Amber.copy(alpha = 0.12f))  // Lithos Amber bg
                        .clickable {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                            onSeriesClick()
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LibraryBooks,
                        contentDescription = null,
                        tint = LithosColors.Amber,  // Lithos Amber
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = book.seriesInfo,
                        style = GlassTypography.Caption,
                        color = LithosColors.Amber,  // Lithos Amber
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
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

    // Lithos: Use Amber for selected state
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) LithosColors.Amber.copy(alpha = 0.15f) else Color.Transparent,
        label = "bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) LithosColors.Amber else theme.textSecondary,
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
// AMBIENT STATS BAR - Lithos frosted glass with Amber accent
// ============================================================================

@Composable
private fun AmbientStatsBar(
    streakDays: Int,
    weeklyTime: String,
    booksFinished: Int,
    inProgressCount: Int,
    isDark: Boolean,
    isOLED: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
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
            append("$streakDays day streak")  // Lithos: No emoji for matte feel
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
            // Lithos frosted glass background
            .background(
                if (isDark) LithosColors.FrostedGlass
                else LithosColors.FrostedGlassLight
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
                    tint = LithosColors.Amber,  // Lithos Amber accent
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // Lithos: Use Amber for selected state, frosted glass for unselected
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> LithosColors.Amber.copy(alpha = 0.18f)  // Lithos Amber
            isDark -> Color.White.copy(alpha = 0.06f)
            else -> Color.Black.copy(alpha = 0.05f)
        },
        label = "bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) LithosColors.Amber else theme.textSecondary,  // Lithos Amber
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
                    color = LithosColors.Amber.copy(alpha = 0.45f),  // Lithos Amber border
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
    isOLED: Boolean,
    accentColor: Color,
    onCoverClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark, isOLED)
    val view = LocalView.current
    val coverInteractionSource = remember { MutableInteractionSource() }
    val isCoverPressed by coverInteractionSource.collectIsPressedAsState()

    val coverScale by animateFloatAsState(
        targetValue = if (isCoverPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "coverScale"
    )

    val playHintAlpha by animateFloatAsState(
        targetValue = if (isCoverPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
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

            // Lithos progress bar - thin stroke with Amber
            if (book.progress > 0 && book.duration > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)  // Lithos thin stroke
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.35f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(book.progressPercent())
                            .fillMaxHeight()
                            .background(LithosColors.Amber)  // Lithos Amber progress
                    )
                }
            }

            // Play hint on press - Moss background for play action
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .alpha(playHintAlpha)
                    .background(LithosColors.Moss.copy(alpha = 0.9f), CircleShape),  // Lithos Moss
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = LithosColors.Oat,  // Lithos Oat for contrast
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
// EMPTY LIBRARY STATE - Lithos styling
// ============================================================================

@Composable
private fun EmptyLibraryState(
    isDark: Boolean,
    isOLED: Boolean,
    accentColor: Color,
    onAddClick: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
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
        // Lithos: Moss button for primary action
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(GlassShapes.Medium))
                .background(LithosColors.Moss)  // Lithos Moss for action
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
                    tint = LithosColors.Oat,  // Lithos Oat for contrast
                    modifier = Modifier.size(GlassIconSize.Small)
                )
                Text(
                    text = "Add Audiobook",
                    style = GlassTypography.Label,
                    color = LithosColors.Oat,  // Lithos Oat for contrast
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
