package com.example.rezon8.ui.screens

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
import com.example.rezon8.R
import com.example.rezon8.data.Book
import com.example.rezon8.ui.theme.*
import com.example.rezon8.ui.viewmodel.HomeViewModel
import com.example.rezon8.ui.viewmodel.PlayerViewModel

/**
 * REZON8 Glass - Home Screen (Dashboard)
 *
 * Features:
 * - Hero Section: Currently playing/last played book with large cover art
 * - Quick Stats Cards: Listening time, books in progress, finished, streak
 * - Recent Activity: Horizontal scroll of recently played books
 * - Recommendations: Continue series, unfinished books
 * - Glass UI design with premium animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenGlass(
    homeViewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    isDark: Boolean = true,
    isRezonDark: Boolean = false,
    accentColor: Color = GlassColors.Interactive,
    scrollToTopTrigger: Int = 0,
    onBookClick: (String) -> Unit = {},
    onPlayBook: (Book) -> Unit = {},
    onSeriesClick: (String) -> Unit = {}
) {
    val theme = glassTheme(isDark, isRezonDark)
    val listState = rememberLazyListState()

    // Scroll to top when trigger changes
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
        }
    }

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

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GlassSpacing.M)
            ) {
                Spacer(modifier = Modifier.height(GlassSpacing.XXL))
                Text(
                    text = stringResource(R.string.nav_home),
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
                    isRezonDark = isRezonDark,
                    accentColor = accentColor,
                    onPlayClick = {
                        if (currentBook?.id == heroBook.id) {
                            playerViewModel.togglePlayback()
                        } else {
                            onPlayBook(heroBook)
                        }
                    },
                    onBookClick = { onBookClick(heroBook.id) }
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
                isRezonDark = isRezonDark,
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
                                isRezonDark = isRezonDark,
                                accentColor = accentColor,
                                onClick = { onBookClick(book.id) },
                                onPlayClick = { onPlayBook(book) }
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
                                isRezonDark = isRezonDark,
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
                    isRezonDark = isRezonDark,
                    accentColor = accentColor,
                    onBookClick = onBookClick
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
                    isRezonDark = isRezonDark,
                    accentColor = accentColor,
                    onBookClick = onBookClick
                )
                Spacer(modifier = Modifier.height(GlassSpacing.L))
            }
        }
    }
}

// ============================================================================
// HERO SECTION - Currently Playing / Last Played Book
// ============================================================================

@Composable
private fun HeroSection(
    book: Book,
    isCurrentlyPlaying: Boolean,
    isPlaying: Boolean,
    progress: Float,
    timeRemaining: String,
    isDark: Boolean,
    isRezonDark: Boolean,
    accentColor: Color,
    onPlayClick: () -> Unit,
    onBookClick: () -> Unit
) {
    val theme = glassTheme(isDark, isRezonDark)
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M)
    ) {
        // Glass card with gradient border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GlassShapes.Large))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.08f else 0.12f),
                            theme.glassCard
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            theme.glassBorder.copy(alpha = 1.5f),
                            theme.glassBorder
                        )
                    ),
                    shape = RoundedCornerShape(GlassShapes.Large)
                )
                .clickable { onBookClick() }
                .padding(GlassSpacing.M)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Large cover art (1:1 square)
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = book.title,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(GlassShapes.Medium))
                            .graphicsLayer { shadowElevation = 8f },
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(GlassSpacing.M))

                    // Book info
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isCurrentlyPlaying) stringResource(R.string.player_now_playing) else stringResource(R.string.home_last_played),
                                style = GlassTypography.Caption,
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = book.title,
                                style = GlassTypography.Headline,
                                color = theme.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = book.author,
                                style = GlassTypography.Callout,
                                color = theme.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Progress and time remaining
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = GlassTypography.Caption,
                                    color = theme.textSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = timeRemaining,
                                    style = GlassTypography.Caption,
                                    color = theme.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(accentColor, accentColor.copy(alpha = 0.7f))
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(GlassSpacing.M))

                // Continue Listening Button
                PremiumPlayButton(
                    text = if (isCurrentlyPlaying && isPlaying) stringResource(R.string.player_pause) else stringResource(R.string.home_continue_listening),
                    icon = if (isCurrentlyPlaying && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        onPlayClick()
                    },
                    accentColor = accentColor,
                    isDark = isDark,
                    isRezonDark = isRezonDark
                )
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
    isRezonDark: Boolean,
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
            isRezonDark = isRezonDark,
            accentColor = if (streakDays > 0) accentColor else null,
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            icon = Icons.Outlined.Schedule,
            value = todayTime.ifEmpty { "0m" },
            label = stringResource(R.string.stats_today),
            isDark = isDark,
            isRezonDark = isRezonDark,
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            icon = Icons.Outlined.PlayCircle,
            value = "$inProgressCount",
            label = stringResource(R.string.home_active),
            isDark = isDark,
            isRezonDark = isRezonDark,
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            icon = Icons.Outlined.CheckCircle,
            value = "$finishedCount",
            label = stringResource(R.string.home_done),
            isDark = isDark,
            isRezonDark = isRezonDark,
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
    isRezonDark: Boolean,
    accentColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark, isRezonDark)
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
    isRezonDark: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isRezonDark)
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
    isRezonDark: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark, isRezonDark)

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
    isRezonDark: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val theme = glassTheme(isDark, isRezonDark)
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
        // Cover art with play button overlay (1:1 square)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(GlassShapes.Medium))
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

            // Play button overlay
            IconButton(
                onClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onPlayClick()
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.player_play),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(GlassSpacing.XS))

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
    isRezonDark: Boolean,
    accentColor: Color,
    onBookClick: (String) -> Unit
) {
    val theme = glassTheme(isDark, isRezonDark)

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
                isRezonDark = isRezonDark,
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
    isRezonDark: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isRezonDark)
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
    isRezonDark: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(GlassShapes.Small))
            .background(accentColor)
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
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(GlassSpacing.S))
            Text(
                text = text,
                style = GlassTypography.Label,
                color = Color.White,
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
