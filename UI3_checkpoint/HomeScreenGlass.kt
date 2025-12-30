package com.mossglen.reverie.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.mossglen.reverie.ui.components.BookDetailMorphingSheet

/**
 * REVERIE Glass - Now Screen (UI3)
 *
 * The "Now" screen is your personal listening command center.
 * Opens directly to YOUR book - zero taps to resume.
 *
 * Features:
 * - Hero Section: Your current book with large cover art and instant play
 * - Contextual Messages: Smart suggestions based on time of day
 * - Quick Stats Cards: Your listening journey at a glance
 * - Recent Activity: Quick access to other books
 * - Glass UI design with premium animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenGlass(
    homeViewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    isDark: Boolean = true,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.Interactive,
    scrollToTopTrigger: Int = 0,
    onBookClick: (String) -> Unit = {},
    onPlayBook: (Book) -> Unit = {},
    onSeriesClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onGenreClick: (String) -> Unit = {}
) {
    val theme = glassTheme(isDark, isReverieDark)
    val listState = rememberLazyListState()

    // Scroll to top when trigger changes
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
        }
    }

    // Book preview state (UI3 morphing sheet)
    var bookForPreview by remember { mutableStateOf<Book?>(null) }

    // ViewModel state
    val mostRecentBook by homeViewModel.mostRecentBook.collectAsState()
    val stats by homeViewModel.stats.collectAsState()
    val booksInProgress by homeViewModel.booksInProgress.collectAsState()
    val finishedBooksCount by homeViewModel.finishedBooksCount.collectAsState()
    val recentlyPlayed by homeViewModel.recentlyPlayed.collectAsState()
    val unfinishedBooks by homeViewModel.unfinishedBooks.collectAsState()
    val continueSeries by homeViewModel.continueSeries.collectAsState()
    val distinctSeries by homeViewModel.distinctSeries.collectAsState()

    // Player state
    val currentBook by playerViewModel.currentBook.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val position by playerViewModel.position.collectAsState()

    // Use current playing book if available, otherwise most recent
    val heroBook = currentBook ?: mostRecentBook

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
        // Header - UI3 "Now" screen
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GlassSpacing.M)
            ) {
                Spacer(modifier = Modifier.height(GlassSpacing.XXL))
                Text(
                    text = stringResource(R.string.nav_now),
                    style = GlassTypography.Display,
                    color = theme.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getGreetingString(),
                    style = GlassTypography.Body,
                    color = theme.textSecondary
                )
                Spacer(modifier = Modifier.height(GlassSpacing.L))
            }
        }

        // Hero Section - Currently Playing / Last Played
        if (heroBook != null) {
            item {
                HeroSection(
                    book = heroBook,
                    isCurrentlyPlaying = currentBook != null,
                    isPlaying = isPlaying,
                    progress = if (currentBook?.id == heroBook.id) {
                        if (heroBook.duration > 0) position.toFloat() / heroBook.duration.toFloat() else 0f
                    } else {
                        heroBook.progressPercent()
                    },
                    timeRemaining = homeViewModel.formatTimeRemaining(
                        homeViewModel.getTimeRemaining(heroBook)
                    ),
                    isDark = isDark,
                    isReverieDark = isReverieDark,
                    accentColor = accentColor,
                    onPlayClick = {
                        if (currentBook?.id == heroBook.id) {
                            playerViewModel.togglePlayback()
                        } else {
                            onPlayBook(heroBook)
                        }
                    },
                    onBookClick = { bookForPreview = heroBook },
                    onSeriesClick = { seriesName -> onSeriesClick(seriesName) },
                    onAuthorClick = { authorName -> onAuthorClick(authorName) }
                )
                Spacer(modifier = Modifier.height(GlassSpacing.L))
            }
        }

        // Quick Stats Row (4 compact cards)
        item {
            QuickStatsRow(
                streakDays = stats.streakDays,
                todayTime = stats.todayTimeFormatted,
                inProgressCount = booksInProgress.size,
                finishedCount = finishedBooksCount,
                isDark = isDark,
                isReverieDark = isReverieDark,
                accentColor = accentColor
            )
            Spacer(modifier = Modifier.height(GlassSpacing.L))
        }

        // Recent Activity Section
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.home_recent_activity),
                        style = GlassTypography.Title,
                        color = theme.textPrimary,
                        modifier = Modifier.padding(horizontal = GlassSpacing.M)
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.M))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S),
                        contentPadding = PaddingValues(horizontal = GlassSpacing.M)
                    ) {
                        items(recentlyPlayed) { book ->
                            RecentBookCard(
                                book = book,
                                isDark = isDark,
                                isReverieDark = isReverieDark,
                                accentColor = accentColor,
                                onCoverClick = { onPlayBook(book) },      // Tap cover = play
                                onTextClick = { bookForPreview = book }   // Tap text = half sheet
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(GlassSpacing.L))
            }
        }

        // Your Series Section
        if (distinctSeries.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.home_your_series),
                        style = GlassTypography.Title,
                        color = theme.textPrimary,
                        modifier = Modifier.padding(horizontal = GlassSpacing.M)
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.M))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S),
                        contentPadding = PaddingValues(horizontal = GlassSpacing.M)
                    ) {
                        items(distinctSeries) { (seriesName, books) ->
                            SeriesCard(
                                seriesName = seriesName,
                                books = books,
                                isDark = isDark,
                                isReverieDark = isReverieDark,
                                accentColor = accentColor,
                                onClick = { onSeriesClick(seriesName) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(GlassSpacing.L))
            }
        }

        // Continue Series Section
        if (continueSeries.isNotEmpty()) {
            item {
                RecommendationSection(
                    title = stringResource(R.string.home_continue_series),
                    subtitle = stringResource(R.string.home_continue_series_subtitle),
                    icon = Icons.Outlined.AutoStories,
                    books = continueSeries,
                    isDark = isDark,
                    isReverieDark = isReverieDark,
                    accentColor = accentColor,
                    onBookClick = { bookId ->
                        continueSeries.find { it.id == bookId }?.let { bookForPreview = it }
                    }
                )
                Spacer(modifier = Modifier.height(GlassSpacing.L))
            }
        }

        // Unfinished Books Section
        if (unfinishedBooks.isNotEmpty()) {
            item {
                RecommendationSection(
                    title = stringResource(R.string.home_unfinished_books),
                    subtitle = stringResource(R.string.home_unfinished_books_subtitle),
                    icon = Icons.Outlined.BookmarkBorder,
                    books = unfinishedBooks,
                    isDark = isDark,
                    isReverieDark = isReverieDark,
                    accentColor = accentColor,
                    onBookClick = { bookId ->
                        unfinishedBooks.find { it.id == bookId }?.let { bookForPreview = it }
                    }
                )
                Spacer(modifier = Modifier.height(GlassSpacing.L))
            }
        }
        }

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
// HERO SECTION - Premium Side-by-Side Layout (Cover LEFT, Text RIGHT)
// Designed to exceed Apple's Now Playing quality
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

    // Button press animation
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "buttonScale"
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

    // Parse series info for navigation
    val seriesInfo = book.seriesInfo.takeIf { it.isNotBlank() }
    val hasSeriesNavigation = seriesInfo != null && onSeriesClick != null

    // Derive current chapter from progress and chapters list
    val currentChapterInfo = remember(book.chapters, book.progress) {
        if (book.chapters.isNotEmpty()) {
            val currentChapter = book.chapters.indexOfFirst { chapter ->
                book.progress >= chapter.startMs && book.progress < chapter.endMs
            }.takeIf { it >= 0 }
            if (currentChapter != null) {
                val chapter = book.chapters[currentChapter]
                "Chapter ${currentChapter + 1}: ${chapter.title}"
            } else {
                "Chapter ${book.chapters.size} of ${book.chapters.size}"
            }
        } else null
    }

    // Chapter context or series info (prefer chapter, then series)
    val chapterContext = currentChapterInfo ?: seriesInfo

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M)
    ) {
        // Glass card - SIDE-BY-SIDE LAYOUT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GlassShapes.Large))
                .background(glassBg)
                .padding(GlassSpacing.M)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // ══════════════════════════════════════════════════════════════
                // MAIN CONTENT ROW: Cover LEFT (140dp) | Text RIGHT
                // ══════════════════════════════════════════════════════════════
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // ─────────────────────────────────────────────────────────
                    // COVER ART - 140dp, prominent with shadow, TAP TO PLAY/PAUSE
                    // ─────────────────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .size(140.dp)
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
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(GlassSpacing.M))

                    // ─────────────────────────────────────────────────────────
                    // TEXT CONTENT - Right side, fills remaining space
                    // ─────────────────────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp),  // Match cover height
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            // Status label - contextual, personal
                            Text(
                                text = if (isCurrentlyPlaying)
                                    stringResource(R.string.player_now_playing)
                                else
                                    "Continue where you left off",
                                style = GlassTypography.Caption,
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Book title - full, up to 2 lines, tappable
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
                                    if (hasSeriesNavigation) {
                                        onSeriesClick?.invoke(seriesInfo!!)
                                    } else {
                                        onBookClick()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(4.dp))

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
                                    text = book.author,
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
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Chapter context or series info
                            if (chapterContext != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = chapterContext,
                                    style = GlassTypography.Caption,
                                    color = theme.textSecondary.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Bottom section: Progress + Time
                        Column {
                            // Progress bar - full width below text
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(accentColor)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Time remaining
                            Text(
                                text = timeRemaining,
                                style = GlassTypography.Caption,
                                color = theme.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(GlassSpacing.M))

                // ══════════════════════════════════════════════════════════════
                // PREMIUM "CONTINUE LISTENING" BUTTON - Full width with play icon
                // ══════════════════════════════════════════════════════════════
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor)
                        .clickable(
                            interactionSource = buttonInteractionSource,
                            indication = null
                        ) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            onPlayClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPlaying) "PAUSE" else "CONTINUE LISTENING",
                            style = GlassTypography.Label,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
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
// HELPER FUNCTIONS
// ============================================================================

@Composable
private fun getGreetingString(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..11 -> stringResource(R.string.home_good_morning)
        in 12..16 -> stringResource(R.string.home_good_afternoon)
        else -> stringResource(R.string.home_good_evening)
    }
}
