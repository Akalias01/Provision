package com.mossglen.lithos.ui.screens

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
// Pager imports removed - now using HeroCardStack
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
import com.mossglen.lithos.data.Book
import com.mossglen.lithos.haptics.HapticType
import com.mossglen.lithos.ui.components.HeroCardStack
import com.mossglen.lithos.haptics.performHaptic
import com.mossglen.lithos.ui.theme.*
import com.mossglen.lithos.ui.viewmodel.HomeViewModel
import com.mossglen.lithos.ui.viewmodel.ListeningStatsViewModel
import com.mossglen.lithos.ui.viewmodel.PlayerViewModel
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
    isOLED: Boolean = false,
    accentColor: Color = GlassColors.Interactive,
    onPlayBook: (Book) -> Unit = {},
    onBookClick: (String) -> Unit = {},
    onRecentBookClick: (Book) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onSeriesClick: (String) -> Unit = {},
    onStatsClick: () -> Unit = {},
    onScrollUp: () -> Unit = {},
    onScrollDown: () -> Unit = {}
) {
    val theme = glassTheme(isDark, isOLED)
    val view = LocalView.current

    // State
    val mostRecentBook by homeViewModel.mostRecentBook.collectAsState()
    val recentlyPlayed by homeViewModel.recentlyPlayed.collectAsState()
    val currentBook by playerViewModel.currentBook.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val position by playerViewModel.position.collectAsState()
    val duration by playerViewModel.duration.collectAsState()

    // Sleep Timer State - for interactive ring integration
    val sleepTimerMinutes by playerViewModel.sleepTimerMinutes.collectAsState()
    val sleepTimerRemaining by playerViewModel.sleepTimerRemaining.collectAsState()
    val sleepTimerActive = sleepTimerMinutes != null && sleepTimerRemaining > 0

    // Stats for Quick Stats Bar
    val currentStreak by statsViewModel.currentStreak.collectAsState()
    val todayListeningTime by statsViewModel.todayListeningTime.collectAsState()
    val booksInProgress by statsViewModel.booksInProgress.collectAsState()
    val dailyGoalMinutes by statsViewModel.dailyGoalMinutes.collectAsState()

    // Goal dialog state
    var showGoalDialog by remember { mutableStateOf(false) }

    // Base hero books from state (max 4 most recent)
    val baseHeroBooks = remember(recentlyPlayed, currentBook, mostRecentBook) {
        val books = mutableListOf<Book>()
        currentBook?.let { books.add(it) }
        mostRecentBook?.let { if (it.id != currentBook?.id) books.add(it) }
        recentlyPlayed.filter { book ->
            book.id != currentBook?.id && book.id != mostRecentBook?.id
        }.take(2).forEach { books.add(it) }
        books.take(4)
    }

    // Use base hero books directly (carousel doesn't reorder)
    val heroBooks = baseHeroBooks

    // Track current card index for dot indicators
    var currentCardIndex by remember { mutableStateOf(0) }

    // Track swipe progress for background crossfade (0.0 to 1.0)
    var swipeProgress by remember { mutableStateOf(0f) }

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
            // Get current and adjacent books for background crossfade
            val currentHeroBook = heroBooks.getOrNull(currentCardIndex) ?: heroBooks.firstOrNull() ?: return
            val nextHeroBook = heroBooks.getOrNull(currentCardIndex + 1)
            val prevHeroBook = heroBooks.getOrNull(currentCardIndex - 1)

            // Ambient background blur - shows current book with crossfade during swipes
            Box(modifier = Modifier.fillMaxSize()) {
                // Current book cover (always visible, fades slightly during transition)
                AsyncImage(
                    model = currentHeroBook.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.3f * (1f - swipeProgress * 0.5f) }
                        .blur(50.dp),
                    contentScale = ContentScale.Crop
                )

                // Next/Prev book cover (fades in during swipe)
                val transitionBook = nextHeroBook ?: prevHeroBook
                if (transitionBook != null && swipeProgress > 0.1f) {
                    AsyncImage(
                        model = transitionBook.coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 0.3f * swipeProgress }
                            .blur(50.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

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

            // Track scroll direction for pill auto-hide
            var lastScrollValue by remember { mutableStateOf(0) }
            LaunchedEffect(scrollState.value) {
                val scrollDelta = scrollState.value - lastScrollValue
                // Use threshold to avoid jitter
                if (abs(scrollDelta) > 10) {
                    if (scrollDelta > 0) {
                        // Scrolling down (reading forward) - hide pill
                        onScrollUp()
                    } else {
                        // Scrolling up (looking for controls) - show pill
                        onScrollDown()
                    }
                }
                lastScrollValue = scrollState.value
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(scrollState)
            ) {
                // ══════════════════════════════════════════════════════════════
                // HEADER - Greeting (Settings accessed via Profile tab)
                // ══════════════════════════════════════════════════════════════
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = greeting,
                        style = GlassTypography.Display.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp
                        ),
                        color = theme.textPrimary
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

                Spacer(modifier = Modifier.height(24.dp))

                // ══════════════════════════════════════════════════════════════
                // DEPTH CAROUSEL - Premium flanking card effect
                // Center card at full scale, side cards tucked behind
                // Auto-returns to first card after 5 seconds of inactivity
                // ══════════════════════════════════════════════════════════════
                HeroCardStack(
                    items = heroBooks,
                    onPageChanged = { index ->
                        currentCardIndex = index
                    },
                    onSwipeProgress = { progress ->
                        swipeProgress = progress
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) { book, centeredness ->
                    HeroCard(
                        book = book,
                        isPlaying = isPlaying && currentBook?.id == book.id,
                        position = if (currentBook?.id == book.id) position else book.progress,
                        duration = if (currentBook?.id == book.id && duration > 0) duration else book.duration,
                        accentColor = accentColor,
                        isDark = isDark,
                        isOLED = isOLED,
                        centeredness = centeredness, // Smooth 0.0-1.0 for shadow interpolation
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

                // Dot indicators showing current position
                if (heroBooks.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(heroBooks.size) { index ->
                            val isSelected = index == currentCardIndex
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) Color.White.copy(alpha = 0.1f)
                                        else Color.Black.copy(alpha = 0.08f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Fill dot when selected
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ══════════════════════════════════════════════════════════════
                // INNOVATIVE LISTENING GOAL SECTION
                // Features:
                // - Goal Achievement: Pulsing glow + trophy when goal met
                // - Sleep Timer: Amber overlay, warning pulse, tap to extend
                // - Active Listening: Breathing animation while playing
                // - Streak Fire: Animated flame for hot streaks
                // ══════════════════════════════════════════════════════════════
                val goalProgress = if (dailyGoalMinutes > 0) {
                    (todayListeningTime.toFloat() / dailyGoalMinutes.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val goalMet = todayListeningTime >= dailyGoalMinutes
                val sleepTimerWarning = sleepTimerActive && sleepTimerRemaining < 2 * 60 * 1000L

                // Animation: Goal achievement glow pulse
                val infiniteTransition = rememberInfiniteTransition(label = "goal")
                val goalGlow by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "goalGlow"
                )

                // Animation: Sleep timer warning pulse
                val sleepWarningAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "sleepWarning"
                )

                // Animation: Breathing while playing
                val breathingScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.03f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "breathing"
                )

                // Animation: Streak fire flicker
                val fireFlicker by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(300),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "fire"
                )

                // Track goal achievement for haptic feedback (only once per session)
                var hasTriggeredGoalHaptic by remember { mutableStateOf(false) }
                LaunchedEffect(goalMet) {
                    if (goalMet && !hasTriggeredGoalHaptic) {
                        view.performHaptic(HapticType.Confirm)
                        hasTriggeredGoalHaptic = true
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.06f)
                            else Color.Black.copy(alpha = 0.04f)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            view.performHaptic(HapticType.LightTap)
                            onStatsClick()
                        }
                        .padding(16.dp)
                ) {
                    // Main Row: Ring + Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: INNOVATIVE Goal Ring (64dp)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .graphicsLayer {
                                    // Breathing animation while playing
                                    if (isPlaying) {
                                        scaleX = breathingScale
                                        scaleY = breathingScale
                                    }
                                    // Goal glow scale when achieved
                                    if (goalMet) {
                                        scaleX = goalGlow * 0.15f + 0.85f
                                        scaleY = goalGlow * 0.15f + 0.85f
                                    }
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    view.performHaptic(HapticType.LightTap)
                                    // If sleep timer active, tap to extend (+5 min)
                                    if (sleepTimerActive) {
                                        playerViewModel.extendSleepTimer(5)
                                        view.performHaptic(HapticType.Confirm)
                                    } else {
                                        showGoalDialog = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 6.dp.toPx()
                                val radius = (size.minDimension - strokeWidth) / 2

                                // Background circle
                                drawCircle(
                                    color = if (isDark) Color.White.copy(alpha = 0.12f)
                                    else Color.Black.copy(alpha = 0.1f),
                                    radius = radius,
                                    style = Stroke(width = strokeWidth)
                                )

                                // Sleep timer overlay (amber) when active
                                if (sleepTimerActive) {
                                    val sleepProgress = if (sleepTimerMinutes != null && sleepTimerMinutes!! > 0) {
                                        (sleepTimerRemaining.toFloat() / (sleepTimerMinutes!! * 60 * 1000L)).coerceIn(0f, 1f)
                                    } else 1f

                                    val sleepColor = if (sleepTimerWarning) {
                                        Color(0xFFFF9800).copy(alpha = sleepWarningAlpha)
                                    } else {
                                        Color(0xFFFFB74D).copy(alpha = 0.6f)
                                    }

                                    drawArc(
                                        color = sleepColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * sleepProgress,
                                        useCenter = false,
                                        topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                                        size = androidx.compose.ui.geometry.Size(
                                            size.width - strokeWidth,
                                            size.height - strokeWidth
                                        ),
                                        style = Stroke(width = strokeWidth + 2.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }

                                // Progress arc (goal)
                                val progressColor = when {
                                    goalMet -> Color(0xFF4CAF50)
                                    else -> accentColor
                                }

                                // Goal glow effect when achieved
                                if (goalMet) {
                                    drawCircle(
                                        color = Color(0xFF4CAF50).copy(alpha = (goalGlow - 0.8f) * 0.5f),
                                        radius = radius + 4.dp.toPx(),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }

                                drawArc(
                                    color = progressColor,
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

                            // Center: Time or Sleep Timer or Trophy
                            if (sleepTimerActive) {
                                // Show sleep timer remaining
                                val mins = (sleepTimerRemaining / 60000).toInt()
                                val secs = ((sleepTimerRemaining % 60000) / 1000).toInt()
                                Text(
                                    text = if (mins > 0) "${mins}m" else "${secs}s",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sleepTimerWarning) Color(0xFFFF9800) else Color(0xFFFFB74D)
                                )
                            } else if (goalMet) {
                                // Trophy icon for goal achieved
                                Icon(
                                    Icons.Filled.EmojiEvents,
                                    contentDescription = "Goal achieved!",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer {
                                            scaleX = goalGlow * 0.2f + 0.8f
                                            scaleY = goalGlow * 0.2f + 0.8f
                                        }
                                )
                            } else {
                                Text(
                                    text = formatMinutes(todayListeningTime),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textPrimary
                                )
                            }
                        }

                        // Center: Goal info + Sleep Timer hint
                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (sleepTimerActive) {
                                Text(
                                    text = if (sleepTimerWarning) "⏰ Tap ring +5m" else "Sleep timer on",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (sleepTimerWarning) Color(0xFFFF9800) else Color(0xFFFFB74D)
                                )
                                Text(
                                    text = "Tap ring to add 5 minutes",
                                    fontSize = 12.sp,
                                    color = theme.textSecondary
                                )
                            } else {
                                Text(
                                    text = if (goalMet) "Goal reached!" else "${dailyGoalMinutes - todayListeningTime}m to go",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (goalMet) Color(0xFF4CAF50) else theme.textPrimary
                                )
                                Text(
                                    text = "Daily goal: ${dailyGoalMinutes}m",
                                    fontSize = 12.sp,
                                    color = theme.textSecondary
                                )
                            }
                        }

                        // Right: Streak with animated fire
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6B35),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer {
                                            // Flicker animation when streak >= 3
                                            if (currentStreak >= 3) {
                                                scaleX = fireFlicker
                                                scaleY = fireFlicker
                                            }
                                        }
                                )
                                Text(
                                    text = "$currentStreak",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6B35)
                                )
                            }
                            Text(
                                text = if (currentStreak >= 7) "🔥 streak" else "streak",
                                fontSize = 11.sp,
                                color = theme.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Row: Mini stats (Active, Done, Journey link)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Active books
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.PlayCircleOutline,
                                    contentDescription = null,
                                    tint = theme.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "$booksInProgress active",
                                    fontSize = 12.sp,
                                    color = theme.textSecondary
                                )
                            }
                            // Currently playing indicator
                            if (isPlaying) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                            .graphicsLayer {
                                                alpha = breathingScale
                                            }
                                    )
                                    Text(
                                        text = "playing",
                                        fontSize = 12.sp,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }

                        // View Journey link
                        Text(
                            text = "Journey →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
            // EMPTY STATE - Elegant, inviting
            // ══════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header - greeting only (settings accessible from Profile)
                Text(
                    text = greeting,
                    style = GlassTypography.Display.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(top = 8.dp)
                )

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
    isOLED: Boolean,
    centeredness: Float, // 0.0 = off-center, 1.0 = fully centered
    onPlayClick: () -> Unit,
    onTitleClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onSeriesClick: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)

    // Direct interpolation - no animation needed since centeredness is already smooth
    // Shadow scales from 8f (off-center) to 24f (centered)
    val cardShadow = 8f + (16f * centeredness)
    val coverShadow = 4f + (12f * centeredness)

    // Calculate progress
    val progress = if (duration > 0) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Time remaining
    val remainingMs = (duration - position).coerceAtLeast(0L)
    val timeRemaining = formatTimeRemaining(remainingMs)

    // Parse series info for "Book X of Y"
    val seriesDisplay = parseSeriesInfo(book.seriesInfo)

    // Card container with smooth shadow (interpolated from centeredness)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                shadowElevation = cardShadow
                shape = RoundedCornerShape(28.dp)
                clip = false
            }
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (isOLED) Color(0xFF0A0A0A) // Near black but not pure
                else if (isDark) Color(0xFF1C1C1E) // Original dark color
                else Color(0xFFF8F8F8)
            )
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cover art - 240dp for visual impact
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        shadowElevation = coverShadow
                        shape = RoundedCornerShape(20.dp)
                        clip = true
                    }
            ) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPlayClick
                        ),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title - Tappable → Series (if exists) or Book Detail
            Text(
                text = book.title,
                style = GlassTypography.Display.copy(
                    fontSize = 18.sp,
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

            Spacer(modifier = Modifier.height(4.dp))

            // Author - Tappable → Author Books
            Text(
                text = book.author,
                style = GlassTypography.Body.copy(
                    fontSize = 14.sp,
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
                    style = GlassTypography.Caption.copy(fontSize = 12.sp),
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
                    style = GlassTypography.Body.copy(fontSize = 13.sp),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(accentColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress percentage and time remaining
            Row(
                modifier = Modifier.fillMaxWidth(0.75f),
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
