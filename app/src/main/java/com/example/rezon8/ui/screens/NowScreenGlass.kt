package com.mossglen.reverie.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mossglen.reverie.data.Book
import com.mossglen.reverie.haptics.HapticType
import com.mossglen.reverie.haptics.performHaptic
import com.mossglen.reverie.ui.theme.*
import com.mossglen.reverie.ui.viewmodel.HomeViewModel
import com.mossglen.reverie.ui.viewmodel.ListeningStatsViewModel
import com.mossglen.reverie.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.abs

/**
 * REVERIE Glass - Immersive Now Screen
 *
 * This is the SANCTUARY. A calm, focused space for the current listening experience.
 * No browsing, no library grid - just your book and the moment.
 *
 * Design Philosophy (Per PROJECT_MANIFEST):
 * - INVISIBLE PERFECTION: The cover IS the experience
 * - PREMIUM BY DEFAULT: Generous whitespace, atmospheric design
 * - ANTICIPATORY INTELLIGENCE: Contextual greeting, time-aware, series info
 * - SENSORY HARMONY: Subtle ambient effects, haptic feedback
 *
 * TOUCH ZONES (Per Manifest):
 * - Cover Art: Play/Pause
 * - Title: Series (if exists) OR Book Detail
 * - Author: Author Books Screen
 * - Series Info: Series Detail Screen
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NowScreenGlass(
    homeViewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    statsViewModel: ListeningStatsViewModel = hiltViewModel(),
    isDark: Boolean = true,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.Interactive,
    onPlayBook: (Book) -> Unit = {},
    onBookClick: (String) -> Unit = {},
    onRecentBookClick: (Book) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onSeriesClick: (String) -> Unit = {},
    onStatsClick: () -> Unit = {}
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current

    // State
    val mostRecentBook by homeViewModel.mostRecentBook.collectAsState()
    val recentlyPlayed by homeViewModel.recentlyPlayed.collectAsState()
    val currentBook by playerViewModel.currentBook.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val position by playerViewModel.position.collectAsState()
    val duration by playerViewModel.duration.collectAsState()

    // Stats for Quick Stats Bar
    val currentStreak by statsViewModel.currentStreak.collectAsState()
    val todayListeningTime by statsViewModel.todayListeningTime.collectAsState()
    val booksInProgress by statsViewModel.booksInProgress.collectAsState()
    val dailyGoalMinutes by statsViewModel.dailyGoalMinutes.collectAsState()

    // Goal dialog state
    var showGoalDialog by remember { mutableStateOf(false) }

    // Hero books for swipeable pager (last 5)
    val heroBooks = remember(recentlyPlayed, currentBook, mostRecentBook) {
        val books = mutableListOf<Book>()
        currentBook?.let { books.add(it) }
        mostRecentBook?.let { if (it.id != currentBook?.id) books.add(it) }
        recentlyPlayed.filter { book ->
            book.id != currentBook?.id && book.id != mostRecentBook?.id
        }.take(3).forEach { books.add(it) }
        books.take(5)
    }

    // Pager state
    val pagerState = rememberPagerState(initialPage = 0) { heroBooks.size.coerceAtLeast(1) }

    // Auto-scroll back to first page after 5 seconds of inactivity
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 0 && heroBooks.isNotEmpty()) {
            delay(5000)
            pagerState.animateScrollToPage(0)
        }
    }

    // Recent books for "Up Next" section (exclude current hero)
    val upNextBooks = remember(recentlyPlayed, heroBooks) {
        val heroIds = heroBooks.map { it.id }.toSet()
        recentlyPlayed.filter { it.id !in heroIds }.take(5)
    }

    // Contextual greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 5 -> "Late night listening"
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            hour < 21 -> "Good evening"
            else -> "Night owl mode"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        if (heroBooks.isNotEmpty()) {
            val currentHeroBook = heroBooks.getOrNull(pagerState.currentPage) ?: heroBooks.first()

            // Ambient background blur - cover art as atmosphere
            AsyncImage(
                model = currentHeroBook.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.3f }
                    .blur(50.dp),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                theme.background.copy(alpha = 0.4f),
                                theme.background.copy(alpha = 0.85f),
                                theme.background
                            )
                        )
                    )
            )

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(scrollState)
            ) {
                // ══════════════════════════════════════════════════════════════
                // HEADER ROW - Greeting + Settings Gear
                // ══════════════════════════════════════════════════════════════
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = greeting,
                            style = GlassTypography.Display.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Continue reading",
                            style = GlassTypography.Body.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = theme.textSecondary
                        )
                    }

                    // Settings gear
                    IconButton(
                        onClick = {
                            view.performHaptic(HapticType.LightTap)
                            onSettingsClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = theme.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ══════════════════════════════════════════════════════════════
                // SWIPEABLE HERO CARDS
                // ══════════════════════════════════════════════════════════════
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    pageSpacing = 16.dp,
                    beyondViewportPageCount = 1,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        snapPositionalThreshold = 0.35f // Snap when 35% past the page
                    )
                ) { page ->
                    val book = heroBooks.getOrNull(page) ?: return@HorizontalPager

                    // Calculate scale and alpha for parallax effect
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val scale = 1f - (abs(pageOffset) * 0.1f).coerceIn(0f, 0.1f)
                    val alpha = 1f - (abs(pageOffset) * 0.3f).coerceIn(0f, 0.3f)

                    HeroCard(
                        book = book,
                        isPlaying = isPlaying && currentBook?.id == book.id,
                        position = if (currentBook?.id == book.id) position else book.progress,
                        duration = if (currentBook?.id == book.id && duration > 0) duration else book.duration,
                        accentColor = accentColor,
                        isDark = isDark,
                        isReverieDark = isReverieDark,
                        scale = scale,
                        alpha = alpha,
                        onPlayClick = {
                            view.performHaptic(HapticType.MediumTap)
                            onPlayBook(book)
                        },
                        onTitleClick = {
                            view.performHaptic(HapticType.LightTap)
                            if (book.seriesInfo.isNotBlank()) {
                                onSeriesClick(book.seriesInfo)
                            } else {
                                onBookClick(book.id)
                            }
                        },
                        onAuthorClick = {
                            view.performHaptic(HapticType.LightTap)
                            onAuthorClick(book.author)
                        },
                        onSeriesClick = {
                            view.performHaptic(HapticType.LightTap)
                            onSeriesClick(book.seriesInfo)
                        }
                    )
                }

                // Page indicators
                if (heroBooks.size > 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(heroBooks.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (isSelected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) accentColor
                                        else theme.textSecondary.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ══════════════════════════════════════════════════════════════
                // LISTENING GOAL SECTION - Apple-inspired design
                // ══════════════════════════════════════════════════════════════
                val goalProgress = if (dailyGoalMinutes > 0) {
                    (todayListeningTime.toFloat() / dailyGoalMinutes.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val goalMet = todayListeningTime >= dailyGoalMinutes

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.05f)
                            else Color.Black.copy(alpha = 0.05f)
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Goal Ring with Today's Listening
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                view.performHaptic(HapticType.LightTap)
                                showGoalDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Background ring
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 8.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2

                            // Background circle
                            drawCircle(
                                color = if (isDark) Color.White.copy(alpha = 0.1f)
                                else Color.Black.copy(alpha = 0.1f),
                                radius = radius,
                                style = Stroke(width = strokeWidth)
                            )

                            // Progress arc
                            drawArc(
                                color = if (goalMet) Color(0xFF4CAF50) else accentColor,
                                startAngle = -90f,
                                sweepAngle = 360f * goalProgress,
                                useCenter = false,
                                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width - strokeWidth,
                                    size.height - strokeWidth
                                ),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        // Center content
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formatMinutes(todayListeningTime),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textPrimary
                            )
                            Text(
                                text = "today",
                                fontSize = 11.sp,
                                color = theme.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Goal status text
                    Text(
                        text = if (goalMet) "🎉 Goal reached!" else "${dailyGoalMinutes - todayListeningTime}m to go",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (goalMet) Color(0xFF4CAF50) else theme.textSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MiniStat(
                            icon = Icons.Filled.LocalFireDepartment,
                            value = "$currentStreak",
                            label = "Streak",
                            color = Color(0xFFFF6B35),
                            isDark = isDark
                        )
                        MiniStat(
                            icon = Icons.Outlined.Timer,
                            value = formatMinutes(todayListeningTime),
                            label = "Today",
                            color = accentColor,
                            isDark = isDark
                        )
                        MiniStat(
                            icon = Icons.Outlined.PlayCircleOutline,
                            value = "$booksInProgress",
                            label = "Active",
                            color = accentColor,
                            isDark = isDark
                        )
                        MiniStat(
                            icon = Icons.Outlined.CheckCircleOutline,
                            value = "0",
                            label = "Done",
                            color = accentColor,
                            isDark = isDark
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Engaging copy + Journey link
                    Text(
                        text = "Listen daily. Watch your progress soar.",
                        fontSize = 12.sp,
                        color = theme.textSecondary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // View full stats button
                    Text(
                        text = "View Journey →",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            view.performHaptic(HapticType.LightTap)
                            onStatsClick()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ══════════════════════════════════════════════════════════════
                // UP NEXT - Subtle horizontal scroll of recent books
                // ══════════════════════════════════════════════════════════════
                if (upNextBooks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp)) // More space from stats

                    Text(
                        text = "Up Next",
                        style = GlassTypography.Body.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = theme.textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2-column grid layout matching Library
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        upNextBooks.chunked(2).forEach { rowBooks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                rowBooks.forEach { book ->
                                    UpNextBookItem(
                                        book = book,
                                        isDark = isDark,
                                        onClick = {
                                            view.performHaptic(HapticType.LightTap)
                                            onRecentBookClick(book)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Fill empty space if odd number of books
                                if (rowBooks.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(120.dp)) // Space for nav bar
            }
        } else {
            // ══════════════════════════════════════════════════════════════
            // EMPTY STATE - Elegant, inviting (with Settings access)
            // ══════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header row with settings - always visible
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = greeting,
                        style = GlassTypography.Display.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp
                        ),
                        color = Color.White
                    )

                    // Settings gear - always accessible
                    IconButton(
                        onClick = {
                            view.performHaptic(HapticType.LightTap)
                            onSettingsClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = theme.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Centered content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Your next story awaits",
                        style = GlassTypography.Body.copy(
                            fontSize = 18.sp
                        ),
                        color = theme.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Add a book to your library to begin",
                        style = GlassTypography.Caption,
                        color = theme.textSecondary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Hero Card - The main book display
 * Per manifest: Cover 140dp, touch zones for navigation
 */
@Composable
private fun HeroCard(
    book: Book,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    accentColor: Color,
    isDark: Boolean,
    isReverieDark: Boolean,
    scale: Float,
    alpha: Float,
    onPlayClick: () -> Unit,
    onTitleClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onSeriesClick: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)

    // Calculate progress
    val progress = if (duration > 0) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Time remaining
    val remainingMs = (duration - position).coerceAtLeast(0L)
    val timeRemaining = formatTimeRemaining(remainingMs)

    // Parse series info for "Book X of Y"
    val seriesDisplay = parseSeriesInfo(book.seriesInfo)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cover art - 200dp for visual impact, tappable for play/pause
        // Per manifest: Cover art is king - no play overlay, tap anywhere plays
        AsyncImage(
            model = book.coverUrl,
            contentDescription = book.title,
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPlayClick
                ),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title - Tappable → Series (if exists) or Book Detail
        Text(
            text = book.title,
            style = GlassTypography.Display.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = theme.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTitleClick
                )
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Author - Tappable → Author Books
        Text(
            text = book.author,
            style = GlassTypography.Body.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onAuthorClick
            )
        )

        // Narrator - if available
        if (book.narrator.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Narrated by ${book.narrator}",
                style = GlassTypography.Caption.copy(
                    fontSize = 12.sp
                ),
                color = theme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        // Series info - "Book X of Y" if available
        if (seriesDisplay.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = seriesDisplay,
                style = GlassTypography.Body.copy(
                    fontSize = 14.sp
                ),
                color = theme.textSecondary,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSeriesClick
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress percentage and time remaining
        Row(
            modifier = Modifier.fillMaxWidth(0.7f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = GlassTypography.Caption.copy(fontSize = 12.sp),
                color = theme.textSecondary
            )
            Text(
                text = timeRemaining,
                style = GlassTypography.Caption.copy(fontSize = 12.sp),
                color = theme.textSecondary
            )
        }
    }
}

@Composable
private fun QuickStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    isDark: Boolean
) {
    val theme = glassTheme(isDark)

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = value,
                style = GlassTypography.Body.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = theme.textPrimary
            )
            Text(
                text = label,
                style = GlassTypography.Caption.copy(
                    fontSize = 10.sp
                ),
                color = theme.textSecondary
            )
        }
    }
}

@Composable
private fun MiniStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    isDark: Boolean
) {
    val theme = glassTheme(isDark)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = theme.textSecondary
        )
    }
}

@Composable
private fun UpNextBookItem(
    book: Book,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark)

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cover art fills available width with aspect ratio
        AsyncImage(
            model = book.coverUrl,
            contentDescription = book.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = book.title,
            style = GlassTypography.Body.copy(
                fontSize = 14.sp
            ),
            color = theme.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatTimeRemaining(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m left"
        minutes > 0 -> "${minutes}m left"
        else -> "Almost done"
    }
}

private fun formatMinutes(ms: Long): String {
    val minutes = (ms / 60000).toInt()
    val hours = minutes / 60
    return if (hours > 0) "${hours}h ${minutes % 60}m" else "${minutes}m"
}

private fun parseSeriesInfo(seriesInfo: String): String {
    if (seriesInfo.isBlank()) return ""

    // Try to extract "Book X" or "#X" pattern
    val bookPattern = Regex("""(?:Book\s*)?#?\s*(\d+(?:\.\d+)?)\s*(?:of\s*(\d+))?""", RegexOption.IGNORE_CASE)
    val match = bookPattern.find(seriesInfo)

    return if (match != null) {
        val bookNum = match.groupValues[1]
        val totalBooks = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
        if (totalBooks != null) {
            "Book $bookNum of $totalBooks"
        } else {
            "Book $bookNum in series"
        }
    } else {
        seriesInfo
    }
}
