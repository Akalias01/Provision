package com.mossglen.lithos.ui.screens

import android.view.HapticFeedbackConstants
import com.mossglen.lithos.util.ShakeDetector
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.mossglen.lithos.R
import com.mossglen.lithos.data.Book
import com.mossglen.lithos.ui.theme.*
import com.mossglen.lithos.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.hilt.navigation.compose.hiltViewModel
import com.mossglen.lithos.data.AudioEffectManager
import com.mossglen.lithos.ui.viewmodel.SettingsViewModel

/**
 * REVERIE Premium Player
 *
 * 2026 Bleeding-Edge Design:
 * - Edge-to-edge immersive album art
 * - Floating glass control pill
 * - Spring physics on all interactions
 * - Visual gesture feedback
 * - Premium micro-animations
 * - Cohesive with iOS Liquid Glass & M3 Expressive
 */

@Composable
fun PlayerScreenGlass(
    playerViewModel: PlayerViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    isDark: Boolean = true,
    isOLED: Boolean = false,
    reverieAccentColor: Color = LithosAmber,
    highlightColor: Color = LithosSlate,
    useBorderHighlight: Boolean = false,
    dynamicColors: Boolean = true,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onListeningStatsClick: () -> Unit = {},
    onCarModeClick: () -> Unit = {},
    onAuthorClick: (authorName: String) -> Unit = {},
    onSeriesClick: (seriesName: String) -> Unit = {}
) {
    // Get AudioEffectManager for Quick EQ
    val audioEffectManager = settingsViewModel.audioEffectManager

    // Theme-aware colors - properly handle Light, Dark, and OLED modes
    val accentColor = LithosAmber  // Amber accent is consistent across all modes
    val textColor = when {
        !isDark -> LithosSlate  // Light mode: dark text
        else -> Color.White     // Dark/OLED: white text
    }
    val secondaryTextColor = when {
        !isDark -> LithosSlate.copy(alpha = 0.7f)  // Light mode
        else -> Color.White.copy(alpha = 0.7f)     // Dark/OLED
    }
    val selectionBg = when {
        !isDark -> Color.Black.copy(alpha = 0.08f)  // Light mode: subtle dark
        else -> Color.White.copy(alpha = 0.1f)      // Dark/OLED: subtle light
    }

    val view = LocalView.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // Player state
    val currentBook by playerViewModel.currentBook.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val position by playerViewModel.position.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsState()
    val sleepTimerMinutes by playerViewModel.sleepTimerMinutes.collectAsState()
    val sleepTimerRemaining by playerViewModel.sleepTimerRemaining.collectAsState()
    val sleepTimerMode by playerViewModel.sleepTimerMode.collectAsState()
    val customSpeedPresets by playerViewModel.customSpeedPresets.collectAsState(initial = emptyList())

    // Shake to extend sleep timer state
    var showShakeExtendedFeedback by remember { mutableStateOf(false) }
    val sleepTimerWarningThreshold = 2 * 60 * 1000L  // 2 minutes
    val isSleepTimerWarning = sleepTimerRemaining > 0 && sleepTimerRemaining < sleepTimerWarningThreshold

    // Shake detector for extending sleep timer
    val shakeDetector = remember {
        ShakeDetector(context) {
            // Only extend if timer is in warning state and in MINUTES mode
            if (isSleepTimerWarning && sleepTimerMode == PlayerViewModel.SleepTimerMode.MINUTES) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                playerViewModel.extendSleepTimer(5)  // Extend by 5 minutes
                showShakeExtendedFeedback = true
            }
        }
    }

    // Start/stop shake detection based on sleep timer warning state
    DisposableEffect(isSleepTimerWarning, sleepTimerMode) {
        if (isSleepTimerWarning && sleepTimerMode == PlayerViewModel.SleepTimerMode.MINUTES) {
            shakeDetector.start()
        } else {
            shakeDetector.stop()
        }
        onDispose {
            shakeDetector.stop()
        }
    }

    // Reset shake extended feedback after 2 seconds
    LaunchedEffect(showShakeExtendedFeedback) {
        if (showShakeExtendedFeedback) {
            delay(2000)
            showShakeExtendedFeedback = false
        }
    }

    // EQ state
    val eqPresetName by audioEffectManager.selectedPresetName.collectAsState()

    // UI state
    var showSpeedPicker by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    var showChaptersDialog by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showBookmarkNoteDialog by remember { mutableStateOf(false) }
    var bookmarkNote by remember { mutableStateOf("") }
    var showAudioSettings by remember { mutableStateOf(false) }
    var showBookDetailsOverlay by remember { mutableStateOf(false) }

    // Ripple animation state for NavigationRing
    var ringRippleDirection by remember { mutableIntStateOf(0) }  // -1 = back, 0 = none, 1 = forward

    // Bookmark ripple animation trigger for NavigationRing
    var bookmarkRippleTrigger by remember { mutableIntStateOf(0) }

    // Auto-reset ripple after animation
    LaunchedEffect(ringRippleDirection) {
        if (ringRippleDirection != 0) {
            delay(400)
            ringRippleDirection = 0
        }
    }

    // Chapter skip animation state - "Invisible Perfection" approach
    // When chapter changes: fill ENTIRE bar to 100% FAST -> snap to 0% -> resume
    var previousChapter by remember { mutableIntStateOf(0) }
    var previousChapterProgress by remember { mutableFloatStateOf(0f) }  // Track progress before chapter change
    var isChapterSkipAnimating by remember { mutableStateOf(false) }
    val chapterProgressAnimatable = remember { Animatable(0f) }

    // Gesture states
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var gestureIndicator by remember { mutableStateOf<GestureType?>(null) }
    val swipeThreshold = 80f

    // Spring animations
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipe"
    )

    // Progress calculations
    val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    val chapters = currentBook?.chapters ?: emptyList()
    val hasRealChapters = chapters.isNotEmpty()
    val totalChapters = if (hasRealChapters) chapters.size else maxOf(1, (duration / (30 * 60 * 1000)).toInt().coerceAtLeast(1))

    val currentChapter = if (hasRealChapters) {
        chapters.indexOfLast { position >= it.startMs }.coerceAtLeast(0) + 1
    } else {
        maxOf(1, ((progress * totalChapters).toInt() + 1).coerceAtMost(totalChapters))
    }

    val chapterDuration = if (hasRealChapters && currentChapter in 1..chapters.size) {
        val ch = chapters[currentChapter - 1]
        ch.endMs - ch.startMs
    } else if (duration > 0 && totalChapters > 0) duration / totalChapters else 1L

    val chapterStartMs = if (hasRealChapters && currentChapter in 1..chapters.size) {
        chapters[currentChapter - 1].startMs
    } else ((currentChapter - 1).toLong() * chapterDuration)

    val chapterPosition = (position - chapterStartMs).coerceAtLeast(0L)
    val chapterProgress = if (chapterDuration > 0) chapterPosition.toFloat() / chapterDuration.toFloat() else 0f

    // Simple chapter skip animation: fill to 100% -> snap to 0% -> done
    val animatedChapterProgress = if (isChapterSkipAnimating) {
        chapterProgressAnimatable.value
    } else {
        chapterProgress
    }

    // Keep animatable in sync when not animating AND track previous progress
    LaunchedEffect(chapterProgress, isChapterSkipAnimating) {
        if (!isChapterSkipAnimating) {
            // Save current progress BEFORE it changes (for animation starting point)
            previousChapterProgress = chapterProgressAnimatable.value
            chapterProgressAnimatable.snapTo(chapterProgress)
        }
    }

    // Detect chapter changes and trigger fluid skip animation
    // Per user feedback: FAST fill ENTIRE bar to 100%, instant reset to 0%, thumb at start
    LaunchedEffect(currentChapter) {
        if (previousChapter != currentChapter && previousChapter > 0) {
            // IMMEDIATELY mark as animating to prevent sync effect from interfering
            isChapterSkipAnimating = true

            // Capture the starting point - where the progress bar WAS before chapter change
            val startPosition = previousChapterProgress.coerceIn(0f, 0.99f)

            // Step 1: SNAP to starting position to ensure we're at the right spot
            chapterProgressAnimatable.snapTo(startPosition)

            // Step 2: Animate FAST to 100% - fill the ENTIRE remaining bar
            // Duration proportional to distance (feels natural) but always fast
            val distance = 1f - startPosition
            val duration = (distance * 200).toInt().coerceIn(80, 200)  // 80-200ms based on distance

            chapterProgressAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = LinearEasing  // Smooth, even fill
                )
            )

            // Step 3: Instant snap to 0% - progress bar completely empty, thumb at start
            chapterProgressAnimatable.snapTo(0f)

            // Step 4: Brief pause so user sees the reset
            delay(60)

            // Done - let normal progress updates resume
            isChapterSkipAnimating = false
        }
        previousChapter = currentChapter
    }

    // Time formatting
    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%d:%02d", minutes, seconds)
    }

    fun formatRemaining(ms: Long): String {
        val remaining = duration - ms
        val hours = remaining / 1000 / 3600
        val minutes = (remaining / 1000 % 3600) / 60
        return if (hours > 0) "-${hours}h ${minutes}m" else "-${minutes}m"
    }

    // Clear gesture indicator after delay
    LaunchedEffect(gestureIndicator) {
        if (gestureIndicator != null) {
            delay(600)
            gestureIndicator = null
        }
    }

    // Background color based on mode - MUST match Lithos theme system
    // Dark mode = Slate (#1A1D21), OLED = True Black (#000000)
    val backgroundColor = when {
        !isDark -> LithosOat     // Light mode: warm paper
        isOLED -> LithosBlack    // OLED: true black
        else -> LithosSlate      // Dark mode: slate
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // LAYER 1: Blurred background artwork (dynamic colors)
        if (dynamicColors) {
            currentBook?.let { book ->
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(when {
                            !isDark -> 40.dp  // Light mode: less blur
                            isOLED -> 60.dp   // OLED: more blur
                            else -> 50.dp     // Dark: medium
                        })
                        .graphicsLayer {
                            alpha = when {
                                !isDark -> 0.3f   // Light mode: subtle
                                isOLED -> 0.35f   // OLED: muted
                                else -> 0.6f      // Dark: vibrant
                            }
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }

        // LAYER 2: Gradient overlay - Top 15% clear, transition 15-35%, theme-based at bottom
        // Uses LithosSlate for dark mode, LithosBlack only for OLED mode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,                // Top - completely clear
                            0.15f to Color.Transparent,               // Still clear at 15%
                            0.25f to backgroundColor.copy(alpha = 0.3f),  // Start darkening
                            0.35f to backgroundColor.copy(alpha = 0.6f),  // Transition complete
                            0.50f to backgroundColor.copy(alpha = 0.85f), // Getting dark
                            0.70f to backgroundColor.copy(alpha = 0.95f), // Very dark
                            1.0f to backgroundColor                       // Theme-appropriate solid at bottom
                        )
                    )
                )
        )

        // LAYER 3: Main content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar - Minimal, sleep timer centered, 3-dot menu on right
            // No back button - swipe down to dismiss per "Invisible Perfection"
            TopPlayerBar(
                onOverflowClick = { showOverflowMenu = true },
                sleepTimerActive = sleepTimerMinutes != null,
                sleepTimerMinutes = sleepTimerMinutes,
                showOverflowMenu = showOverflowMenu,
                onDismissOverflow = { showOverflowMenu = false },
                onShareClick = {
                    currentBook?.let { book ->
                        val progressPercent = if (duration > 0) ((position.toFloat() / duration.toFloat()) * 100).toInt() else 0
                        val timePosition = formatTime(position)
                        val totalTime = formatTime(duration)

                        val shareText = buildString {
                            append("🎧 I'm listening to \"${book.title}\"")
                            if (book.author.isNotBlank() && book.author != "Unknown Author") {
                                append(" by ${book.author}")
                            }
                            append("\n\n")
                            append("📍 $timePosition / $totalTime ($progressPercent% complete)")
                            append("\n\n")
                            append("— Shared from Reverie")
                        }

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            putExtra(Intent.EXTRA_SUBJECT, "Listening to ${book.title}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share what you're listening to"))
                    }
                },
                onBookDetailsClick = {
                    // Show book details overlay
                    showBookDetailsOverlay = true
                },
                onAuthorClick = {
                    currentBook?.let { book ->
                        if (book.author.isNotBlank() && book.author != "Unknown Author") {
                            onAuthorClick(book.author)
                        }
                    }
                },
                onSeriesClick = currentBook?.seriesInfo?.takeIf { it.isNotBlank() }?.let { seriesInfo ->
                    {
                        onSeriesClick(seriesInfo)
                    }
                },
                onBookmarkWithNoteClick = {
                    showBookmarkNoteDialog = true
                },
                onMarkAsFinishedClick = {
                    currentBook?.let { book ->
                        val newIsFinished = !book.isFinished
                        playerViewModel.updateBookFinished(newIsFinished)
                        Toast.makeText(
                            context,
                            if (newIsFinished) "Marked as finished" else "Marked as unfinished",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onPlayerSettingsClick = onSettingsClick,
                onListeningStatsClick = onListeningStatsClick,
                onCarModeClick = onCarModeClick,
                isBookFinished = currentBook?.isFinished == true,
                authorName = currentBook?.author ?: "",
                seriesName = currentBook?.seriesInfo?.takeIf { it.isNotBlank() },
                isDark = isDark,
                isOLED = isOLED,
                reverieAccentColor = reverieAccentColor
            )

            // Album Art - THE HERO (large, fills available space - MAXIMIZED)
            // Tap = Show book details overlay (progressive disclosure)
            // Double-tap = Play/pause or skip (power user gesture)
            // Positioned so top-right corner aligns with top of 3-dot menu circle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)  // Fill available space for maximum cover size
                    .offset(y = (-20).dp)  // Move up slightly so top aligns with 3-dot menu top
                    .padding(horizontal = 12.dp)  // Minimal padding for largest possible cover
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                // Single tap = Show book details overlay
                                showBookDetailsOverlay = true
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            },
                            onDoubleTap = { offset ->
                                val width = size.width
                                when {
                                    offset.x < width / 3 -> {
                                        gestureIndicator = GestureType.SKIP_BACK
                                        playerViewModel.skipBack()
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    }
                                    offset.x > width * 2 / 3 -> {
                                        gestureIndicator = GestureType.SKIP_FORWARD
                                        playerViewModel.skipForward()
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    }
                                    else -> {
                                        gestureIndicator = if (isPlaying) GestureType.PAUSE else GestureType.PLAY
                                        playerViewModel.togglePlayback()
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    // Swipe LEFT = skip BACK (natural reading direction)
                                    swipeOffset < -swipeThreshold -> {
                                        gestureIndicator = GestureType.SKIP_BACK
                                        playerViewModel.skipBackward()
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    }
                                    // Swipe RIGHT = skip FORWARD (natural reading direction)
                                    swipeOffset > swipeThreshold -> {
                                        gestureIndicator = GestureType.SKIP_FORWARD
                                        playerViewModel.skipForward()
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    }
                                }
                                swipeOffset = 0f
                            },
                            onDragCancel = { swipeOffset = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                swipeOffset += dragAmount
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                currentBook?.let { book ->
                    // Album art with premium shadow and animations
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .graphicsLayer {
                                translationX = animatedSwipeOffset * 0.4f
                                rotationZ = animatedSwipeOffset * 0.015f
                                val scale = 1f - (abs(animatedSwipeOffset) / 800f).coerceAtMost(0.03f)
                                scaleX = scale
                                scaleY = scale
                                shadowElevation = 24f
                            }
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = book.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Subtle inner shadow
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                        )
                    }
                }

                // Gesture indicator overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = gestureIndicator != null,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    GestureIndicator(type = gestureIndicator ?: GestureType.PLAY)
                }
            }

            // CONTROLS SECTION - Layout Order:
            // 1. Spacer (generous breathing room from cover)
            // 2. Row: [Book time passed] [Ring] [Book time left]
            // 3. Spacer (small - ring visually connected to bar)
            // 4. Chapter progress bar
            // 5. Row: [Chapter time] [Chapter time left]
            // 6. Playback controls
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                    // Spacing between cover and ring row (equal to ring-to-chapter-bar spacing)
                    Spacer(modifier = Modifier.height(32.dp))

                    // Calculate book times for the ring row
                    val bookElapsedMs = position
                    val bookHoursElapsed = bookElapsedMs / 1000 / 3600
                    val bookMinutesElapsed = (bookElapsedMs / 1000 % 3600) / 60
                    val bookTimePassedText = if (bookHoursElapsed > 0) {
                        "${bookHoursElapsed}h ${bookMinutesElapsed}m"
                    } else {
                        "${bookMinutesElapsed}m"
                    }

                    val bookRemainingMs = duration - position
                    val bookHoursLeft = bookRemainingMs / 1000 / 3600
                    val bookMinutesLeft = (bookRemainingMs / 1000 % 3600) / 60
                    val bookTimeLeftText = if (bookHoursLeft > 0) {
                        "-${bookHoursLeft}h ${bookMinutesLeft}m"
                    } else {
                        "-${bookMinutesLeft}m"
                    }

                    // 1. TIME ROW WITH RING - [Book time passed] [Ring] [Book time left]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LithosComponents.TimeLabels.horizontalPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT: Book time passed (e.g., "1h 24m")
                        Text(
                            text = bookTimePassedText,
                            style = TextStyle(
                                fontSize = LithosComponents.TimeLabels.fontSize.sp,
                                fontWeight = LithosComponents.TimeLabels.fontWeight,
                                letterSpacing = LithosComponents.TimeLabels.letterSpacing.sp
                            ),
                            color = if (!isDark) textColor.copy(alpha = 0.7f) else if (isOLED) secondaryTextColor else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )

                        // CENTER: Navigation Ring with chapter count
                        NavigationRing(
                            progress = progress,
                            chapters = chapters,
                            bookmarks = currentBook?.bookmarks ?: emptyList(),
                            duration = duration,
                            timeRemainingMs = duration - position,
                            currentChapter = currentChapter,
                            totalChapters = totalChapters,
                            rippleDirection = ringRippleDirection,
                            bookmarkRippleTrigger = bookmarkRippleTrigger,
                            isDark = isDark,
                            isOLED = isOLED,
                            reverieAccentColor = reverieAccentColor,
                            onTap = { showChaptersDialog = true }
                        )

                        // RIGHT: Book time remaining (e.g., "-3h 24m")
                        Text(
                            text = bookTimeLeftText,
                            style = TextStyle(
                                fontSize = LithosComponents.TimeLabels.fontSize.sp,
                                fontWeight = LithosComponents.TimeLabels.fontWeight,
                                letterSpacing = LithosComponents.TimeLabels.letterSpacing.sp
                            ),
                            color = if (!isDark) textColor.copy(alpha = 0.7f) else if (isOLED) accentColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }

                    // Spacing between ring row and chapter bar (equal to cover-to-ring spacing)
                    Spacer(modifier = Modifier.height(32.dp))

                    // 2. CHAPTER PROGRESS BAR - Below ring
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LithosComponents.ProgressBar.horizontalPadding)
                    ) {
                        // Premium progress slider - tracks CHAPTER progress for easy chapter navigation
                        var isSeeking by remember { mutableStateOf(false) }
                        var seekPreviewProgress by remember { mutableFloatStateOf(0f) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),  // Compact to bring slider closer to times
                            contentAlignment = Alignment.Center
                        ) {
                            // Time preview overlay (shows when scrubbing) - positioned above slider
                            if (isSeeking) {
                                val previewTimeMs = chapterStartMs + (seekPreviewProgress * chapterDuration).toLong()
                                val scale by animateFloatAsState(
                                    targetValue = if (isSeeking) 1f else 0.8f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    label = "previewScale"
                                )
                                val alpha by animateFloatAsState(
                                    targetValue = if (isSeeking) 1f else 0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    label = "previewAlpha"
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = 0.dp)
                                        .scale(scale)
                                        .alpha(alpha)
                                        .background(
                                            color = if (isOLED)
                                                LithosUI.SheetBackground.copy(alpha = 0.95f)
                                            else
                                                Color.Black.copy(alpha = 0.75f),
                                            shape = RoundedCornerShape(LithosComponents.ProgressBar.previewCornerRadius)
                                        )
                                        .border(
                                            width = LithosComponents.ProgressBar.previewBorderWidth,
                                            color = if (isOLED)
                                                reverieAccentColor.copy(alpha = 0.3f)
                                            else
                                                Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(LithosComponents.ProgressBar.previewCornerRadius)
                                        )
                                        .padding(horizontal = LithosComponents.ProgressBar.previewPadding, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = formatTime(previewTimeMs - chapterStartMs),
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = if (isOLED) reverieAccentColor else Color.White
                                    )
                                }
                            }

                            // Slider at bottom
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                            ) {
                                PremiumSlider(
                                    value = if (isChapterSkipAnimating) animatedChapterProgress else chapterProgress,
                                    isOLED = isOLED,
                                    reverieAccentColor = reverieAccentColor,
                                    isChapterSkipAnimating = isChapterSkipAnimating,
                                    onValueChange = { newChapterProgress ->
                                        if (!isSeeking) {
                                            isSeeking = true
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                        seekPreviewProgress = newChapterProgress
                                        // Seek within current chapter
                                        val newPosition = chapterStartMs + (newChapterProgress * chapterDuration).toLong()
                                        playerViewModel.seekTo(newPosition)
                                    },
                                    onValueChangeFinished = {
                                        isSeeking = false
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    }
                                )
                            }
                        }

                        // 3. CHAPTER BAR TIME ROW - [Chapter time] [Chapter time left]
                        val chapterRemainingMs = (chapterDuration - chapterPosition).coerceAtLeast(0L)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // LEFT: Chapter position (e.g., "2:34")
                            Text(
                                text = formatTime(chapterPosition),
                                style = TextStyle(
                                    fontSize = LithosComponents.TimeLabels.fontSize.sp,
                                    fontWeight = LithosComponents.TimeLabels.fontWeight,
                                    letterSpacing = LithosComponents.TimeLabels.letterSpacing.sp
                                ),
                                color = if (!isDark) textColor.copy(alpha = 0.6f) else if (isOLED) secondaryTextColor else Color.White.copy(alpha = 0.5f)
                            )
                            // RIGHT: Chapter time remaining (e.g., "-42:38")
                            Text(
                                text = "-${formatTime(chapterRemainingMs)}",
                                style = TextStyle(
                                    fontSize = LithosComponents.TimeLabels.fontSize.sp,
                                    fontWeight = LithosComponents.TimeLabels.fontWeight,
                                    letterSpacing = LithosComponents.TimeLabels.letterSpacing.sp
                                ),
                                color = if (!isDark) textColor.copy(alpha = 0.6f) else if (isOLED) secondaryTextColor else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Spacing before controls
                    Spacer(modifier = Modifier.height(20.dp))


                    // Main Controls - Premium floating design
                    MainPlayerControls(
                        isPlaying = isPlaying,
                        isDark = isDark,
                        isOLED = isOLED,
                        reverieAccentColor = reverieAccentColor,
                        onSkipBack = {
                            playerViewModel.skipBackward()
                            ringRippleDirection = -1  // Ripple left
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        onPrevious = {
                            playerViewModel.previousChapter()
                            ringRippleDirection = -1  // Ripple left
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        onPlayPause = {
                            playerViewModel.togglePlayback()
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        onNext = {
                            playerViewModel.nextChapter()
                            ringRippleDirection = 1  // Ripple right
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        onSkipForward = {
                            playerViewModel.skipForward()
                            ringRippleDirection = 1  // Ripple right
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Player Utilities Pill - Glass pill bar with icon-only buttons
                    // Order: Bookmark (spontaneous) | Speed (frequent) | Timer (session) | EQ (rare)
                    PlayerUtilitiesPill(
                        playbackSpeed = playbackSpeed,
                        sleepTimerMinutes = sleepTimerMinutes,
                        sleepTimerRemaining = sleepTimerRemaining,
                        eqPresetName = eqPresetName,
                        isDark = isDark,
                        isOLED = isOLED,
                        reverieAccentColor = reverieAccentColor,
                        onBookmarkClick = {
                            // Quick bookmark - instant with toast feedback + ripple animation
                            playerViewModel.toggleBookmark()
                            bookmarkRippleTrigger++  // Trigger bookmark ripple animation on NavigationRing
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            Toast.makeText(context, context.getString(R.string.player_add_bookmark), Toast.LENGTH_SHORT).show()
                        },
                        onBookmarkLongClick = {
                            // Long press = Bookmark with note dialog
                            showBookmarkNoteDialog = true
                        },
                        onSpeedClick = { showSpeedPicker = true },
                        onTimerClick = { showSleepTimer = true },
                        onEqClick = { showAudioSettings = true }
                    )

                // Bottom spacing - reduced by 50%
                Spacer(modifier = Modifier.height(24.dp))
            } // End of controls section Column
        } // End of main Column

        // Book Details Overlay - Progressive disclosure (tap cover to reveal)
        AnimatedVisibility(
            visible = showBookDetailsOverlay,
            enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            ),
            exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(
                targetOffsetY = { it / 4 },
                animationSpec = tween(150)
            )
        ) {
            // Track vertical swipe for dismiss gesture
            var overlaySwipeOffset by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Move overlay down as user swipes
                        translationY = overlaySwipeOffset.coerceAtLeast(0f)
                        // Fade out slightly as it moves
                        alpha = 1f - (overlaySwipeOffset / 400f).coerceIn(0f, 0.3f)
                    }
                    .background(Color.Black.copy(alpha = 0.85f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showBookDetailsOverlay = false
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                // If swiped down enough, dismiss
                                if (overlaySwipeOffset > 100f) {
                                    showBookDetailsOverlay = false
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                }
                                overlaySwipeOffset = 0f
                            },
                            onDragCancel = { overlaySwipeOffset = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                // Only track downward swipes (positive values)
                                overlaySwipeOffset = (overlaySwipeOffset + dragAmount).coerceAtLeast(0f)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                currentBook?.let { book ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Title - large and prominent
                        Text(
                            text = book.title,
                            style = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 2f), 8f)
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Author - tappable (future: navigate to author's books)
                        Text(
                            text = "by ${book.author}",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.sp
                            ),
                            color = if (isOLED) reverieAccentColor else Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )

                        // Series info if available
                        if (book.seriesInfo.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = book.seriesInfo,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = 0.sp
                                ),
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }

                        // Narrator if available
                        if (book.narrator.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Narrated by ${book.narrator}",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = 0.sp
                                ),
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Chapter info
                        if (hasRealChapters && currentChapter in 1..chapters.size) {
                            val chapterTitle = chapters[currentChapter - 1].title
                            Text(
                                text = "Now Playing: $chapterTitle",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Dismiss hint
                        Text(
                            text = "Tap anywhere to dismiss",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color.White.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Dialogs
        if (showSpeedPicker) {
            PremiumSpeedDialog(
                currentSpeed = playbackSpeed,
                customPresets = customSpeedPresets,
                isOLED = isOLED,
                reverieAccentColor = reverieAccentColor,
                highlightColor = highlightColor,
                onSpeedSelected = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    playerViewModel.setPlaybackSpeed(it)
                    // Stay open - user dismisses by swiping or tapping outside
                },
                onSavePreset = {
                    showSavePresetDialog = true
                },
                onDeletePreset = { name ->
                    playerViewModel.deleteCustomPreset(name)
                },
                onDismiss = { showSpeedPicker = false }
            )
        }

        // Save Preset Dialog
        if (showSavePresetDialog) {
            SavePresetDialog(
                initialName = presetName,
                isOLED = isOLED,
                reverieAccentColor = reverieAccentColor,
                onSave = { name ->
                    playerViewModel.saveCurrentSpeedAsPreset(name)
                    showSavePresetDialog = false
                    presetName = ""
                },
                onDismiss = {
                    showSavePresetDialog = false
                    presetName = ""
                }
            )
        }

        if (showChaptersDialog) {
            ChaptersBookmarksDialog(
                chapters = chapters,
                currentChapter = currentChapter,
                hasRealChapters = hasRealChapters,
                totalChapters = totalChapters,
                chapterDuration = chapterDuration,
                duration = duration,
                bookmarks = currentBook?.bookmarks ?: emptyList(),
                bookmarkNotes = currentBook?.bookmarkNotes ?: emptyMap(),
                isOLED = isOLED,
                reverieAccentColor = reverieAccentColor,
                highlightColor = highlightColor,
                onSeekTo = { ms ->
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    playerViewModel.seekTo(ms)
                    showChaptersDialog = false
                },
                onDeleteBookmark = { positionMs ->
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    playerViewModel.deleteBookmark(positionMs)
                },
                onEditBookmarkNote = { positionMs, note ->
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    playerViewModel.updateBookmarkNote(positionMs, note)
                },
                onDismiss = { showChaptersDialog = false }
            )
        }

        if (showSleepTimer) {
            PremiumSleepDialog(
                sleepTimerMinutes = sleepTimerMinutes,
                isOLED = isOLED,
                reverieAccentColor = reverieAccentColor,
                onTimerSet = { minutes ->
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    playerViewModel.setSleepTimer(minutes)
                    // Stay open - user dismisses by swiping or tapping outside
                },
                onEndOfChapter = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    playerViewModel.setSleepTimerEndOfChapter()
                    // Stay open - user dismisses by swiping or tapping outside
                },
                onCancelTimer = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    playerViewModel.cancelSleepTimer()
                    // Stay open - user dismisses by swiping or tapping outside
                },
                onDismiss = { showSleepTimer = false }
            )
        }

        // Bookmark with note dialog
        if (showBookmarkNoteDialog) {
            BookmarkNoteDialog(
                note = bookmarkNote,
                onNoteChange = { bookmarkNote = it },
                isOLED = isOLED,
                reverieAccentColor = reverieAccentColor,
                onSave = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    playerViewModel.addBookmarkWithNote(bookmarkNote)
                    showBookmarkNoteDialog = false
                    bookmarkNote = ""
                },
                onDismiss = { showBookmarkNoteDialog = false }
            )
        }

        // Audio Settings dialog
        if (showAudioSettings) {
            AudioSettingsDialog(
                audioEffectManager = audioEffectManager,
                isOLED = isOLED,
                reverieAccentColor = reverieAccentColor,
                onDismiss = { showAudioSettings = false }
            )
        }

        // Shake to extend sleep timer feedback
        AnimatedVisibility(
            visible = showShakeExtendedFeedback,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            ),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = reverieAccentColor.copy(alpha = 0.9f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "+5 minutes",
                        style = GlassTypography.Label,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // NOTE: Edit, Mark As, Delete dialogs belong on the Book Detail/Synopsis screen
        // The player only has playback-focused controls
    }
}

// ============================================================================
// GESTURE TYPES
// ============================================================================

private enum class GestureType {
    PLAY, PAUSE, SKIP_FORWARD, SKIP_BACK
}

@Composable
private fun GestureIndicator(type: GestureType) {
    val icon = when (type) {
        GestureType.PLAY -> Icons.Rounded.PlayArrow
        GestureType.PAUSE -> Icons.Rounded.Pause
        GestureType.SKIP_FORWARD -> Icons.Rounded.Forward30
        GestureType.SKIP_BACK -> Icons.Rounded.Replay10
    }

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

// ============================================================================
// TOP BAR
// ============================================================================

@Composable
private fun TopPlayerBar(
    onOverflowClick: () -> Unit,
    sleepTimerActive: Boolean,
    sleepTimerMinutes: Int?,
    showOverflowMenu: Boolean,
    onDismissOverflow: () -> Unit,
    onShareClick: () -> Unit,
    onBookDetailsClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onSeriesClick: (() -> Unit)?,
    onBookmarkWithNoteClick: () -> Unit,
    onMarkAsFinishedClick: () -> Unit,
    onPlayerSettingsClick: () -> Unit,
    onListeningStatsClick: () -> Unit,
    onCarModeClick: () -> Unit,
    isBookFinished: Boolean,
    authorName: String,
    seriesName: String?,
    isDark: Boolean = true,
    isOLED: Boolean = false,
    reverieAccentColor: Color = LithosAmber
) {
    val menuBg = if (!isDark) Color(0xFFF2F2F7) else if (isOLED) LithosUI.DeepBackground else LithosUI.CardBackground
    val menuTextColor = if (!isDark) LithosSlate else if (isOLED) LithosTextPrimary else Color.White
    val menuIconColor = if (!isDark) LithosAmber else if (isOLED) reverieAccentColor else Color.White

    // Minimal header layout - swipe to dismiss, 3-dot on right
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)  // Taller for better touch targets and visibility
            .padding(horizontal = 12.dp)
            .zIndex(10f)  // Ensure it's above cover art
    ) {
        // Center - Sleep timer indicator (only shown when active)
        if (sleepTimerActive && sleepTimerMinutes != null) {
            val timerBg = if (!isDark) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f)
            val timerColor = if (!isDark) LithosSlate.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .background(timerBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Filled.Snooze,
                    null,
                    tint = timerColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "${sleepTimerMinutes}m",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = timerColor
                )
            }
        }

        // Right - 3-dot overflow menu (Audible-style options)
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            TopBarButton(icon = Icons.Rounded.MoreVert, onClick = onOverflowClick, isDark = isDark)

            // Dropdown with Audible-style options
            // NOTE: Speed, Timer, EQ, Bookmark are in utility buttons below - NOT duplicated here
            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = onDismissOverflow,
                modifier = Modifier
                    .background(menuBg)
                    .widthIn(min = 220.dp)
            ) {
                // Book Details
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.player_book_details), color = menuTextColor) },
                    onClick = {
                        onDismissOverflow()
                        onBookDetailsClick()
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Info, null, tint = menuIconColor)
                    }
                )

                // More from Author
                if (authorName.isNotBlank() && authorName != "Unknown Author") {
                    DropdownMenuItem(
                        text = { Text("More from $authorName", color = menuTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            onDismissOverflow()
                            onAuthorClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Person, null, tint = menuIconColor)
                        }
                    )
                }

                // More from Series (only if book has series)
                if (seriesName != null && onSeriesClick != null) {
                    DropdownMenuItem(
                        text = { Text("$seriesName Series", color = menuTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            onDismissOverflow()
                            onSeriesClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.CollectionsBookmark, null, tint = menuIconColor)
                        }
                    )
                }

                // Divider
                HorizontalDivider(
                    color = if (isOLED) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Mark as Finished/Unfinished
                DropdownMenuItem(
                    text = {
                        Text(
                            if (isBookFinished) "Mark as unfinished" else "Mark as finished",
                            color = menuTextColor
                        )
                    },
                    onClick = {
                        onDismissOverflow()
                        onMarkAsFinishedClick()
                    },
                    leadingIcon = {
                        Icon(
                            if (isBookFinished) Icons.Rounded.Replay else Icons.Rounded.CheckCircle,
                            null,
                            tint = menuIconColor
                        )
                    }
                )

                // Add Bookmark with Note
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.player_add_bookmark), color = menuTextColor) },
                    onClick = {
                        onDismissOverflow()
                        onBookmarkWithNoteClick()
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.BookmarkAdd, null, tint = menuIconColor)
                    }
                )

                // Divider before settings section
                HorizontalDivider(
                    color = if (isOLED) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Car Mode - Large buttons for driving
                DropdownMenuItem(
                    text = { Text("Car Mode", color = menuTextColor) },
                    onClick = {
                        onDismissOverflow()
                        onCarModeClick()
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.DirectionsCar, null, tint = menuIconColor)
                    }
                )

                // Player Settings
                DropdownMenuItem(
                    text = { Text("Player settings", color = menuTextColor) },
                    onClick = {
                        onDismissOverflow()
                        onPlayerSettingsClick()
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Settings, null, tint = menuIconColor)
                    }
                )

                // Listening Stats
                DropdownMenuItem(
                    text = { Text("Listening stats", color = menuTextColor) },
                    onClick = {
                        onDismissOverflow()
                        onListeningStatsClick()
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.BarChart, null, tint = menuIconColor)
                    }
                )

                // Share Progress
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_share), color = menuTextColor) },
                    onClick = {
                        onDismissOverflow()
                        onShareClick()
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Share, null, tint = menuIconColor)
                    }
                )
            }
        }
    }
}

@Composable
private fun TopBarButton(
    icon: ImageVector,
    onClick: () -> Unit,
    size: Dp = 40.dp,
    isDark: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) LithosComponents.Pill.pressScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val buttonBg = if (!isDark) Color.Black.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.25f)
    val iconTint = if (!isDark) LithosSlate.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)

    Box(
        modifier = Modifier
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .background(buttonBg)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(if (size > 40.dp) 28.dp else GlassIconSize.Medium)
        )
    }
}

// ============================================================================
// PREMIUM SLIDER
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumSlider(
    value: Float,
    isOLED: Boolean = false,
    reverieAccentColor: Color = LithosAmber,
    isChapterSkipAnimating: Boolean = false,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    // State for tracking if user is actively dragging
    var isDragging by remember { mutableStateOf(false) }
    var previewValue by remember { mutableFloatStateOf(value) }

    val animatedValue by animateFloatAsState(
        targetValue = if (isDragging) previewValue else value,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "slider"
    )

    // Lithos Amber - solid amber color for scrubber/seek bar
    val trackColor = LithosAmber  // Solid Amber #D48C2C
    val thumbColor = LithosAmber  // Matching thumb color
    val inactiveColor = LithosProgressTrack  // 20% white track

    // Create interaction source to detect press state
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Update dragging state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            isDragging = true
        } else if (isDragging) {
            isDragging = false
            onValueChangeFinished()
        }
    }

    Slider(
        value = animatedValue,
        onValueChange = { newValue ->
            previewValue = newValue
            onValueChange(newValue)
        },
        onValueChangeFinished = {
            isDragging = false
            onValueChangeFinished()
        },
        modifier = Modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        enabled = !isChapterSkipAnimating,  // Disable during animation
        colors = SliderDefaults.colors(
            thumbColor = thumbColor,
            activeTrackColor = trackColor,
            inactiveTrackColor = inactiveColor
        ),
        thumb = {
            // Simple thumb that scales up when dragging
            val thumbScale by animateFloatAsState(
                targetValue = if (isDragging) 1.3f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "thumbScale"
            )

            Box(
                modifier = Modifier
                    .scale(thumbScale)
                    .size(16.dp)
                    .shadow(
                        elevation = if (isDragging) 12.dp else 8.dp,
                        shape = CircleShape,
                        spotColor = Color.Black
                    )
                    .background(thumbColor, CircleShape)
            )
        },
        track = { sliderState ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LithosComponents.ProgressBar.height)
                    .clip(RoundedCornerShape(LithosComponents.ProgressBar.cornerRadius))
                    .background(inactiveColor)
            ) {
                // Simple progress track
                Box(
                    modifier = Modifier
                        .fillMaxWidth(sliderState.value)
                        .fillMaxHeight()
                        .background(trackColor)
                )
            }
        }
    )
}

// ============================================================================
// MAIN CONTROLS
// ============================================================================

@Composable
private fun MainPlayerControls(
    isPlaying: Boolean,
    isDark: Boolean = true,
    isOLED: Boolean = false,
    reverieAccentColor: Color = LithosAmber,
    onSkipBack: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSkipForward: () -> Unit
) {
    // Layout: [Prev Chapter] [Skip Back] [PLAY] [Skip Fwd] [Next Chapter]
    // Frequency Proximity: Most used actions closest to center (thumb zone)
    // Premium Rounded icons, balanced sizing hierarchy
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Chapter - OUTSIDE (less frequent)
        ControlButton(
            icon = Icons.Rounded.SkipPrevious,
            onClick = onPrevious,
            size = 44.dp,
            iconSize = 26.dp,
            isDark = isDark
        )

        // Skip back 30s - INSIDE (more frequent, next to play)
        ControlButton(
            icon = Icons.Rounded.Replay30,
            onClick = onSkipBack,
            size = 52.dp,
            iconSize = 30.dp,
            isDark = isDark
        )

        // Play/Pause - Hero button (CENTER)
        PlayPauseButton(
            isPlaying = isPlaying,
            isOLED = isOLED,
            reverieAccentColor = reverieAccentColor,
            onClick = onPlayPause
        )

        // Skip forward 30s - INSIDE (more frequent, next to play)
        ControlButton(
            icon = Icons.Rounded.Forward30,
            onClick = onSkipForward,
            size = 52.dp,
            iconSize = 30.dp,
            isDark = isDark
        )

        // Next Chapter - OUTSIDE (less frequent)
        ControlButton(
            icon = Icons.Rounded.SkipNext,
            onClick = onNext,
            size = 44.dp,
            iconSize = 26.dp,
            isDark = isDark
        )
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    size: Dp,
    iconSize: Dp,
    isDark: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) LithosComponents.Buttons.pressScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) Color.White else LithosSlate,  // Theme-aware
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    isOLED: Boolean = false,
    reverieAccentColor: Color = LithosAmber,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) LithosComponents.Buttons.pressScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    // Lithos Moss button - ONLY for Play/Pause per Lithos design spec
    val buttonColor = LithosMoss  // Moss #4A5D45 for play button ONLY
    val iconColor = LithosTextPrimary  // White icon on Moss background

    Box(
        modifier = Modifier
            .scale(scale)
            .size(80.dp)  // Slightly larger button
            .shadow(12.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.4f))  // Deeper shadow
            .clip(CircleShape)
            .background(buttonColor)
            .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)  // Subtle sharp border
            // Subtle inner shadow effect using gradient overlay for matte/satin feel
            .drawBehind {
                // Draw subtle inner shadow at top for depth
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),  // Top darker
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.4f
                    ),
                    radius = size.minDimension / 2
                )
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(48.dp)  // 2x larger icon
        )
    }
}

// ============================================================================
// NAVIGATION RING - Progress with Chapter Ticks & Bookmark Dots
// ============================================================================

/**
 * Centered progress ring that serves as a navigation portal.
 * - Shows overall book progress as an arc
 * - Chapter markers as ticks around the edge
 * - Bookmark positions as colored dots
 * - Tappable to open chapters/bookmarks sheet
 * - Directional ripple animation on skip
 */
@Composable
private fun NavigationRing(
    progress: Float,
    chapters: List<com.mossglen.lithos.data.Chapter>,
    bookmarks: List<Long>,
    duration: Long,
    timeRemainingMs: Long,
    currentChapter: Int,
    totalChapters: Int,
    rippleDirection: Int = 0, // -1 = left (back), 0 = none, 1 = right (forward)
    bookmarkRippleTrigger: Int = 0, // Increments when bookmark is created
    isDark: Boolean = true,
    isOLED: Boolean = false,
    reverieAccentColor: Color = LithosAmber,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animation for press feedback
    val scale by animateFloatAsState(
        targetValue = if (isPressed) LithosComponents.Buttons.pressScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ringScale"
    )

    // Resonating ripple animation - multiple waves like sound/water
    var rippleTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(rippleDirection) {
        if (rippleDirection != 0) {
            rippleTrigger++
        }
    }

    // Track if we should animate (triggered by skip action)
    var isAnimating by remember { mutableStateOf(false) }
    LaunchedEffect(rippleTrigger) {
        if (rippleTrigger > 0) {
            isAnimating = true
            delay(800) // Duration of full resonance animation
            isAnimating = false
        }
    }

    // =========================================================================
    // BOOKMARK WATER RIPPLE ANIMATION - Fluid, natural water-like effect
    // 5 concentric waves with staggered timing for smooth, elegant appearance
    // Uses cubic easing for natural deceleration like real water ripples
    // Total duration: ~1200ms - satisfying and premium feel
    // =========================================================================

    // 5 waves for smooth, natural water effect
    val wave1Scale = remember { Animatable(1f) }
    val wave1Alpha = remember { Animatable(0f) }
    val wave2Scale = remember { Animatable(1f) }
    val wave2Alpha = remember { Animatable(0f) }
    val wave3Scale = remember { Animatable(1f) }
    val wave3Alpha = remember { Animatable(0f) }
    val wave4Scale = remember { Animatable(1f) }
    val wave4Alpha = remember { Animatable(0f) }
    val wave5Scale = remember { Animatable(1f) }
    val wave5Alpha = remember { Animatable(0f) }

    // Core pulse - subtle highlight (no glow per Lithos design)
    val coreGlow = remember { Animatable(0f) }

    // Natural water ripple easing - starts fast, slows naturally
    val waterEasing = CubicBezierEasing(0.2f, 0.8f, 0.4f, 1f)

    LaunchedEffect(bookmarkRippleTrigger) {
        if (bookmarkRippleTrigger > 0) {
            // Reset all waves
            listOf(wave1Scale, wave2Scale, wave3Scale, wave4Scale, wave5Scale).forEach { it.snapTo(1f) }
            listOf(wave1Alpha, wave2Alpha, wave3Alpha, wave4Alpha, wave5Alpha).forEach { it.snapTo(0f) }
            coreGlow.snapTo(0f)

            // Core pulse - subtle flash (no bright glow per Lithos)
            launch {
                coreGlow.animateTo(0.5f, tween(100, easing = FastOutSlowInEasing))
                delay(100)
                coreGlow.animateTo(0f, tween(300, easing = waterEasing))
            }

            // Wave 1 - Innermost, brightest, thickest
            launch {
                wave1Alpha.snapTo(0.6f)
                launch { wave1Scale.animateTo(1.4f, tween(600, easing = waterEasing)) }
                wave1Alpha.animateTo(0f, tween(600, easing = waterEasing))
            }

            // Wave 2 - Slightly delayed
            delay(80)
            launch {
                wave2Alpha.snapTo(0.5f)
                launch { wave2Scale.animateTo(1.55f, tween(700, easing = waterEasing)) }
                wave2Alpha.animateTo(0f, tween(700, easing = waterEasing))
            }

            // Wave 3 - Middle wave
            delay(80)
            launch {
                wave3Alpha.snapTo(0.4f)
                launch { wave3Scale.animateTo(1.7f, tween(800, easing = waterEasing)) }
                wave3Alpha.animateTo(0f, tween(800, easing = waterEasing))
            }

            // Wave 4 - Outer wave
            delay(80)
            launch {
                wave4Alpha.snapTo(0.3f)
                launch { wave4Scale.animateTo(1.85f, tween(900, easing = waterEasing)) }
                wave4Alpha.animateTo(0f, tween(900, easing = waterEasing))
            }

            // Wave 5 - Outermost, faintest, thinnest
            delay(80)
            launch {
                wave5Alpha.snapTo(0.2f)
                launch { wave5Scale.animateTo(2.0f, tween(1000, easing = waterEasing)) }
                wave5Alpha.animateTo(0f, tween(1000, easing = waterEasing))
            }
        }
    }

    // Colors - Lithos Amber design language (matte, no glow)
    val progressColor = LithosAmber  // Solid Amber for progress ring
    val trackColor = if (!isDark) LithosSlate.copy(alpha = 0.2f) else LithosProgressTrack  // Theme-aware track
    val textColor = if (!isDark) LithosSlate else if (isOLED) LithosTextPrimary else Color.White
    val secondaryTextColor = if (!isDark) LithosSlate.copy(alpha = 0.7f) else if (isOLED) LithosTextSecondary else Color.White.copy(alpha = 0.6f)

    // Ring dimensions - slightly thicker stroke for better visibility
    val ringSize = 52.dp
    val strokeWidth = 4.dp  // Thicker stroke for better visibility (was 2dp)

    Box(
        modifier = modifier
            .size(ringSize)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onTap()
            },
        contentAlignment = Alignment.Center
    ) {
        // Draw the ring with markers
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val radius = (canvasSize - strokeWidth.toPx()) / 2
            val center = Offset(size.width / 2, size.height / 2)
            val startAngle = -90f // Start from top

            // Arc sizing - centered in canvas
            val arcTopLeft = Offset(
                (size.width - canvasSize + strokeWidth.toPx()) / 2,
                (size.height - canvasSize + strokeWidth.toPx()) / 2
            )
            val arcSize = androidx.compose.ui.geometry.Size(
                canvasSize - strokeWidth.toPx(),
                canvasSize - strokeWidth.toPx()
            )

            // Gap between progress and track (in degrees) - creates visual separation
            val gapDegrees = 6f
            val progressSweep = progress * 360f

            // 1. Track (remaining portion only) - starts after progress + gap
            // Only draw if there's remaining progress to show
            if (progress < 0.98f) {
                val trackStartAngle = startAngle + progressSweep + gapDegrees
                val trackSweep = (360f - progressSweep - gapDegrees * 2).coerceAtLeast(0f)
                if (trackSweep > 0f) {
                    drawArc(
                        color = trackColor,
                        startAngle = trackStartAngle,
                        sweepAngle = trackSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 2. Progress arc - with gap before track
            if (progress > 0.02f) {
                drawArc(
                    color = progressColor,
                    startAngle = startAngle,
                    sweepAngle = progressSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. BOOKMARK RIPPLE - Subtle matte ripples (Lithos: no glow effects)
            // Simple expanding ring, matte finish, no bright illumination
            val rippleColor = LithosAmber  // Solid Amber, matte

            // Core pulse - subtle ring highlight (no glow, just brief alpha change)
            if (coreGlow.value > 0f) {
                // Single subtle ring highlight - no outer glow
                drawCircle(
                    color = rippleColor.copy(alpha = coreGlow.value * 0.5f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth.toPx())
                )
            }

            // Wave 1 - Innermost, thin stroke (2dp - Lithos thin strokes)
            if (wave1Alpha.value > 0f) {
                drawCircle(
                    color = rippleColor.copy(alpha = wave1Alpha.value * 0.5f),  // More subtle
                    radius = radius * wave1Scale.value,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Wave 2 - Slightly thinner (1.5dp)
            if (wave2Alpha.value > 0f) {
                drawCircle(
                    color = rippleColor.copy(alpha = wave2Alpha.value * 0.4f),  // More subtle
                    radius = radius * wave2Scale.value,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Wave 3 - Thin stroke (1dp)
            if (wave3Alpha.value > 0f) {
                drawCircle(
                    color = rippleColor.copy(alpha = wave3Alpha.value * 0.3f),  // Subtle
                    radius = radius * wave3Scale.value,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Skip waves 4 & 5 for cleaner matte appearance (less is more)
        }

        // Center content - chapter count in compact format (e.g., "3/12")
        Text(
            text = "$currentChapter/$totalChapters",
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.3).sp
            ),
            color = secondaryTextColor
        )
    }
}

// ============================================================================
// PLAYER UTILITIES PILL
// Per PROJECT_MANIFEST: Glass pill bar with icon-only buttons
// Order: Bookmark (spontaneous) | Speed (frequent) | Timer (session) | EQ (rare)
// Colors: White default, accent when active - matches nav pill pattern
// ============================================================================

@Composable
private fun PlayerUtilitiesPill(
    playbackSpeed: Float,
    sleepTimerMinutes: Int?,
    sleepTimerRemaining: Long = 0L,
    eqPresetName: String = "Flat",
    isDark: Boolean = true,
    isOLED: Boolean = false,
    reverieAccentColor: Color = LithosAmber,
    onBookmarkClick: () -> Unit,
    onBookmarkLongClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onTimerClick: () -> Unit,
    onEqClick: () -> Unit
) {
    val view = LocalView.current

    // Theme-aware frosted glass pill
    val pillBg = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.95f) else Color(0xFFF2F2F7).copy(alpha = 0.95f)
    val pillBorder = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)  // Neutral border
    val dividerColor = if (isDark) LithosDivider else Color.Black.copy(alpha = 0.1f)

    // Icon colors: Theme-aware with Amber active state
    val defaultColor = if (isDark) LithosTextSecondary else LithosSlate.copy(alpha = 0.7f)
    val activeColor = LithosAmber  // Solid Amber when active

    // Determine active states
    val isSpeedActive = playbackSpeed != 1.0f
    val isTimerActive = sleepTimerMinutes != null
    val isEqActive = eqPresetName != "Flat"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LithosComponents.ProgressBar.horizontalPadding)
            .height(LithosComponents.Pill.height - 6.dp)  // Slightly smaller secondary pill
            .clip(RoundedCornerShape(LithosComponents.Pill.cornerRadius - 4.dp))  // Slightly less rounded
            .background(pillBg)
            .border(LithosComponents.Cards.borderWidth, pillBorder, RoundedCornerShape(LithosComponents.Pill.cornerRadius - 4.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. BOOKMARK - Most spontaneous action (quick access)
        // TAP = Quick bookmark | LONG PRESS = Bookmark with note
        BookmarkButton(
            view = view,
            onBookmarkClick = onBookmarkClick,
            onBookmarkLongClick = onBookmarkLongClick,
            defaultColor = defaultColor
        )

        // Divider
        UtilityDivider(dividerColor)

        // 2. SPEED - Frequently adjusted
        // Speed/gauge icon
        UtilityPillIcon(
            icon = Icons.Filled.Speed,
            contentDescription = "Playback Speed",
            isActive = isSpeedActive,
            defaultColor = defaultColor,
            activeColor = activeColor,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onSpeedClick()
            }
        )

        // Divider
        UtilityDivider(dividerColor)

        // 3. TIMER - Sleep timer with clock + Z icon
        SleepTimerPillIcon(
            isActive = isTimerActive,
            defaultColor = defaultColor,
            activeColor = activeColor,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onTimerClick()
            }
        )

        // Divider
        UtilityDivider(dividerColor)

        // 4. EQ - Set once, rarely changed
        // Equalizer bars icon
        UtilityPillIcon(
            icon = Icons.Filled.Equalizer,
            contentDescription = "Equalizer",
            isActive = isEqActive,
            defaultColor = defaultColor,
            activeColor = activeColor,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onEqClick()
            }
        )
    }
}

@Composable
private fun UtilityPillIcon(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    defaultColor: Color,
    activeColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Using unified ReverieComponents for consistent animation values
    val scale by animateFloatAsState(
        targetValue = if (isPressed) LithosComponents.Pill.pressScale else 1f,
        animationSpec = spring(
            dampingRatio = LithosComponents.Pill.springDamping,
            stiffness = LithosComponents.Pill.springStiffness
        ),
        label = "scale"
    )

    // Full brightness default, accent when active (matches sleep timer icon)
    val iconColor = if (isActive) activeColor else defaultColor

    Box(
        modifier = Modifier
            .scale(scale)
            .size(GlassTouchTarget.Standard)  // Unified touch target
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(GlassIconSize.Medium)  // Unified icon size
        )
    }
}

/**
 * Sleep timer icon - Uses Filled.Snooze to match other pill icons (BookmarkBorder, Speed, Equalizer)
 * Using unified ReverieComponents for consistent animation and sizing
 */
@Composable
private fun SleepTimerPillIcon(
    isActive: Boolean,
    defaultColor: Color,
    activeColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) LithosComponents.Pill.pressScale else 1f,
        animationSpec = spring(
            dampingRatio = LithosComponents.Pill.springDamping,
            stiffness = LithosComponents.Pill.springStiffness
        ),
        label = "scale"
    )

    val iconColor = if (isActive) activeColor else defaultColor

    Box(
        modifier = Modifier
            .scale(scale)
            .size(GlassTouchTarget.Standard)  // Unified touch target
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Snooze,
            contentDescription = "Sleep Timer",
            tint = iconColor,
            modifier = Modifier.size(GlassIconSize.Medium)  // Unified icon size
        )
    }
}

@Composable
private fun UtilityDivider(color: Color) {
    Box(
        modifier = Modifier
            .width(LithosComponents.Cards.borderWidth)  // Unified divider width
            .height(20.dp)
            .background(color)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkButton(
    view: android.view.View,
    onBookmarkClick: () -> Unit,
    onBookmarkLongClick: () -> Unit,
    defaultColor: Color
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onBookmarkClick()
                },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onBookmarkLongClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.BookmarkBorder,
            contentDescription = "Add Bookmark (long press for note)",
            modifier = Modifier.size(22.dp),
            tint = defaultColor
        )
    }
}

// ============================================================================
// SPEED PRESET CHIPS
// ============================================================================

@Composable
private fun SpeedPresetChips(
    currentSpeed: Float,
    isOLED: Boolean = false,
    reverieAccentColor: Color = LithosAmber,
    highlightColor: Color = LithosSlate,
    onSpeedSelected: (Float) -> Unit
) {
    val presetSpeeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presetSpeeds.forEach { speed ->
            SpeedChip(
                speed = speed,
                isSelected = abs(currentSpeed - speed) < 0.01f,
                isOLED = isOLED,
                reverieAccentColor = reverieAccentColor,
                highlightColor = highlightColor,
                onClick = { onSpeedSelected(speed) }
            )
        }
    }
}

@Composable
private fun SpeedChip(
    speed: Float,
    isSelected: Boolean,
    isOLED: Boolean = false,
    reverieAccentColor: Color = LithosAmber,
    highlightColor: Color = LithosSlate,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) LithosComponents.Buttons.pressScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    // Theme-appropriate colors
    val activeColor = if (isOLED) reverieAccentColor else Color.White
    val inactiveColor = if (isOLED) LithosTextSecondary else Color.White.copy(alpha = 0.6f)
    val activeBg = if (isOLED) highlightColor else Color.White.copy(alpha = 0.15f)
    val inactiveBg = Color.White.copy(alpha = 0.05f)
    val borderColor = if (isSelected) {
        if (isOLED) reverieAccentColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
    } else {
        Color.White.copy(alpha = 0.1f)
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .height(36.dp)
            .widthIn(min = 52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) activeBg else inactiveBg)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) {
                onClick()
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${speed}x",
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = if (isSelected) activeColor else inactiveColor
        )
    }
}

// ============================================================================
// DIALOGS
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumSpeedDialog(
    currentSpeed: Float,
    customPresets: List<com.mossglen.lithos.data.SettingsRepository.CustomSpeedPreset>,
    isOLED: Boolean = false,
    reverieAccentColor: Color = LithosAmber,
    highlightColor: Color = LithosSlate,
    onSpeedSelected: (Float) -> Unit,
    onSavePreset: () -> Unit,
    onDeletePreset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetBg = LithosUI.SheetBackground
    val accentColor = LithosAmber // Lithos Amber accent
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.6f)
    val chipBg = LithosUI.CardBackground
    val selectedBg = LithosAmber // Lithos Amber selection

    // Speed state with slider
    var sliderSpeed by remember { mutableFloatStateOf(currentSpeed) }

    // Preset speeds matching reference image
    val presetSpeeds = listOf(1.0f, 1.2f, 1.5f, 1.7f, 2.0f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
        dragHandle = {
            // Custom drag handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title row: "Reading speed" and current value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Reading speed",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = textPrimary
                )
                Text(
                    String.format("%.2f×", sliderSpeed),
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = textPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Premium custom slider - scrubber/tab style like EQ
            val view = LocalView.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Minus button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(chipBg)
                        .clickable {
                            val newSpeed = ((sliderSpeed - 0.05f) * 20).toInt() / 20f
                            sliderSpeed = newSpeed.coerceIn(0.5f, 3.0f)
                            onSpeedSelected(sliderSpeed)
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Remove,
                        contentDescription = "Decrease speed",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Custom premium slider with draggable scrubber/handle
                val handleSize = 18.dp  // Smaller, more refined handle
                val trackHeight = 4.dp  // Thinner track
                val progress = ((sliderSpeed - 0.5f) / 2.5f).coerceIn(0f, 1f)
                val sliderAccent = reverieAccentColor  // Use app accent for active track

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val widthPx = size.width.toFloat()
                                    val deltaSpeed = (dragAmount.x / widthPx) * 2.5f // 0.5 to 3.0 range
                                    val newSpeed = (sliderSpeed + deltaSpeed).coerceIn(0.5f, 3.0f)
                                    // Round to 0.05 increments
                                    sliderSpeed = (newSpeed * 20).toInt() / 20f
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                },
                                onDragEnd = {
                                    onSpeedSelected(sliderSpeed)
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                // Tap to seek to position
                                val widthPx = size.width.toFloat()
                                val tapProgress = (offset.x / widthPx).coerceIn(0f, 1f)
                                val newSpeed = 0.5f + (tapProgress * 2.5f)
                                sliderSpeed = (newSpeed * 20).toInt() / 20f
                                onSpeedSelected(sliderSpeed)
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    val trackWidth = maxWidth
                    val handleOffset = (trackWidth - handleSize) * progress

                    // Track background - subtle rounded bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(trackHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .align(Alignment.Center)
                    )

                    // Active track (filled portion) - accent colored
                    Box(
                        modifier = Modifier
                            .width(handleOffset + handleSize / 2)
                            .height(trackHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(sliderAccent)
                            .align(Alignment.CenterStart)
                    )

                    // Premium scrubber handle - matches accent color like track
                    Box(
                        modifier = Modifier
                            .offset(x = handleOffset)
                            .size(handleSize)
                            .clip(CircleShape)
                            .background(sliderAccent)  // Copper accent to match track
                            .border(1.dp, sliderAccent.copy(alpha = 0.5f), CircleShape)
                    )
                }

                // Plus button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(chipBg)
                        .clickable {
                            val newSpeed = ((sliderSpeed + 0.05f) * 20).toInt() / 20f
                            sliderSpeed = newSpeed.coerceIn(0.5f, 3.0f)
                            onSpeedSelected(sliderSpeed)
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Increase speed",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preset chips row - SHORTER boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetSpeeds.forEach { speed ->
                    val isSelected = abs(sliderSpeed - speed) < 0.01f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipBg)  // Same background for all
                            .clickable {
                                sliderSpeed = speed
                                onSpeedSelected(speed)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            String.format("%.1f", speed),
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isSelected) accentColor else textSecondary  // Colored text when selected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavePresetDialog(
    initialName: String,
    isOLED: Boolean = false,
    reverieAccentColor: Color = LithosAmber,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var presetName by remember { mutableStateOf(initialName) }
    val dialogBg = LithosUI.SheetBackground
    val accentColor = if (isOLED) reverieAccentColor else Color.White
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.7f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(dialogBg)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.speed_save_preset),
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.series_name),
                    style = TextStyle(fontSize = 14.sp),
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Study Mode", color = textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = textSecondary,
                        cursorColor = accentColor
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { onDismiss() }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.dialog_cancel),
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                            color = textSecondary
                        )
                    }

                    // Save button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor)
                            .clickable(enabled = presetName.isNotBlank()) {
                                if (presetName.isNotBlank()) {
                                    onSave(presetName)
                                }
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.dialog_save),
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                            color = if (presetName.isNotBlank()) Color.Black else Color.Black.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChaptersBookmarksDialog(
    chapters: List<com.mossglen.lithos.data.Chapter>,
    currentChapter: Int,
    hasRealChapters: Boolean,
    totalChapters: Int,
    chapterDuration: Long,
    duration: Long,
    bookmarks: List<Long>,
    bookmarkNotes: Map<Long, String> = emptyMap(),
    isOLED: Boolean,
    reverieAccentColor: Color = LithosAmber,
    highlightColor: Color = LithosSlate,
    onSeekTo: (Long) -> Unit,
    onDeleteBookmark: (Long) -> Unit = {},
    onEditBookmarkNote: (Long, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    // Use theme accent color - NO BROWN, dark grey selection
    val accentColor = LithosSlate // Dark grey - matches speed/sleep dialogs
    val selectionBg = LithosSlate // Dark grey selection
    val dialogBg = LithosUI.SheetBackground // Consistent solid background
    val glassBg = LithosUI.CardBackground // Consistent chip background
    // Start on Chapters tab (index 0), NOT bookmarks
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf(stringResource(R.string.player_chapters), stringResource(R.string.nav_bookmarks))

    fun formatDuration(ms: Long): String {
        val hours = ms / 1000 / 3600
        val minutes = (ms / 1000 % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun formatTimestamp(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%d:%02d", minutes, seconds)
    }

    // Swipe-down dismiss state and entrance animation
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 150f

    // Entrance animation - slide up from bottom
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val slideOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 400f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dialogSlide"
    )

    val dialogAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "dialogAlpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .graphicsLayer {
                    alpha = dialogAlpha
                    translationY = slideOffset + dragOffsetY
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                    },
                    onDragStopped = {
                        if (dragOffsetY > dismissThreshold) {
                            onDismiss()
                        } else {
                            dragOffsetY = 0f
                        }
                    }
                )
                .clip(RoundedCornerShape(20.dp))
                .background(dialogBg)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Drag handle indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Tab Row - Synced with pager
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(glassBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isSelected) Modifier.background(accentColor)
                                    else Modifier
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    title,
                                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                                )
                                // Badge for bookmarks count
                                if (index == 1 && bookmarks.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) Color.White.copy(alpha = 0.3f)
                                                else accentColor.copy(alpha = 0.3f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${bookmarks.size}",
                                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = if (isSelected) Color.White else accentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Swipeable HorizontalPager for tabs - fixed height to prevent resize during swipe
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.height(320.dp)  // Fixed height prevents resizing
                ) { page ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                    when (page) {
                        0 -> {
                            // Chapters tab
                            if (hasRealChapters) {
                                itemsIndexed(chapters) { index, chapter ->
                                    val chapterNum = index + 1
                                    val isCurrent = chapterNum == currentChapter
                                    ChapterRow(
                                        title = chapter.title,
                                        duration = formatDuration(chapter.endMs - chapter.startMs),
                                        isCurrent = isCurrent,
                                        accentColor = accentColor,
                                        selectionBg = selectionBg,
                                        onClick = { onSeekTo(chapter.startMs) }
                                    )
                                }
                            } else {
                                // No chapters - show empty state (bookmarks are in their own tab)
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Rounded.MenuBook,
                                                null,
                                                tint = Color.White.copy(alpha = 0.3f),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                stringResource(R.string.ui_no_chapters),
                                                style = TextStyle(fontSize = 15.sp),
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                stringResource(R.string.bookmarks_add_hint),
                                                style = TextStyle(fontSize = 12.sp),
                                                color = Color.White.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Bookmarks tab
                            if (bookmarks.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Rounded.BookmarkBorder,
                                                null,
                                                tint = Color.White.copy(alpha = 0.3f),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                stringResource(R.string.bookmarks_none),
                                                style = TextStyle(fontSize = 15.sp),
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                stringResource(R.string.bookmarks_add_hint),
                                                style = TextStyle(fontSize = 12.sp),
                                                color = Color.White.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                            } else {
                                val sortedBookmarks = bookmarks.sortedDescending()
                                items(sortedBookmarks.size) { index ->
                                    val positionMs = sortedBookmarks[index]
                                    val note = bookmarkNotes[positionMs]
                                    BookmarkRow(
                                        timestamp = formatTimestamp(positionMs),
                                        note = note,
                                        accentColor = accentColor,
                                        onClick = { onSeekTo(positionMs) },
                                        onDelete = { onDeleteBookmark(positionMs) },
                                        onEditNote = { newNote -> onEditBookmarkNote(positionMs, newNote) }
                                    )
                                }
                            }
                        }
                    }
                }
                } // End HorizontalPager

                Spacer(modifier = Modifier.height(16.dp))

                // Done button
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(stringResource(R.string.dialog_done), color = accentColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(
    title: String,
    duration: String,
    isCurrent: Boolean,
    accentColor: Color = Color.White,
    selectionBg: Color = LithosSlate,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isCurrent) Modifier.background(selectionBg)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (isCurrent) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = duration,
            style = TextStyle(fontSize = 13.sp),
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkRow(
    timestamp: String,
    note: String? = null,
    accentColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onEditNote: (String) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf(note ?: "") }
    val hasNote = !note.isNullOrBlank()

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Bookmark icon with note indicator
                Box {
                    Icon(
                        Icons.Rounded.Bookmark,
                        null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    // Note indicator badge
                    if (hasNote) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50)) // Green dot for note
                        )
                    }
                }
                Column {
                    Text(
                        text = timestamp,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White
                    )
                    if (hasNote) {
                        Text(
                            text = note!!,
                            style = TextStyle(fontSize = 13.sp),
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Tap to jump • Long press for options",
                            style = TextStyle(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Long-press dropdown menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = LithosUI.CardBackground
        ) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            if (hasNote) Icons.Rounded.Edit else Icons.Rounded.NoteAdd,
                            null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            if (hasNote) "Edit Note" else "Add Note",
                            color = Color.White
                        )
                    }
                },
                onClick = {
                    showMenu = false
                    editingNote = note ?: ""
                    showEditNoteDialog = true
                }
            )
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Delete Bookmark", color = Color(0xFFFF6B6B))
                    }
                },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }

    // Edit note dialog
    if (showEditNoteDialog) {
        AlertDialog(
            onDismissRequest = { showEditNoteDialog = false },
            containerColor = LithosUI.SheetBackground,
            title = {
                Text(
                    if (hasNote) "Edit Note" else "Add Note",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = editingNote,
                    onValueChange = { editingNote = it },
                    placeholder = { Text("Add a note...", color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = accentColor
                    ),
                    minLines = 2,
                    maxLines = 4
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEditNote(editingNote)
                        showEditNoteDialog = false
                    }
                ) {
                    Text("Save", color = accentColor, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNoteDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumSleepDialog(
    sleepTimerMinutes: Int?,
    isOLED: Boolean = false,
    reverieAccentColor: Color = LithosAmber,
    onTimerSet: (Int) -> Unit,
    onEndOfChapter: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetBg = LithosUI.SheetBackground
    val chipBg = LithosUI.CardBackground
    val selectedBg = LithosAmber // Lithos Amber selection
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.6f)

    // Grid of presets matching reference image layout
    val topRow = listOf(45, 60, 90, 120)
    val bottomRow = listOf(5, 10, 15, 30)

    // Track if "None" (no timer) is selected
    val isNoneSelected = sleepTimerMinutes == null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
        dragHandle = {
            // Custom drag handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title row with settings gear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sleep timer",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = textPrimary
                )
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = "Timer settings",
                    tint = textSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { /* Future: open sleep timer settings */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Top row of time presets (45, 60, 90, 120) - COMPACT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                topRow.forEach { minutes ->
                    val isSelected = sleepTimerMinutes == minutes
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipBg)  // Same background for all
                            .clickable { onTimerSet(minutes) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${minutes}m",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isSelected) selectedBg else textSecondary  // Colored text when selected
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom row of time presets (5, 10, 15, 30) - COMPACT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                bottomRow.forEach { minutes ->
                    val isSelected = sleepTimerMinutes == minutes
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipBg)  // Same background for all
                            .clickable { onTimerSet(minutes) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${minutes}m",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isSelected) selectedBg else textSecondary  // Colored text when selected
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row: Chapter end and None - COMPACT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Chapter end option
                val isChapterEndSelected = sleepTimerMinutes == -1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(chipBg)  // Same background for all
                        .clickable { onEndOfChapter() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Chapter end",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = if (isChapterEndSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isChapterEndSelected) selectedBg else textSecondary  // Colored text when selected
                    )
                }

                // None option (cancel timer)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(chipBg)  // Same background for all
                        .clickable { onCancelTimer() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "None",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = if (isNoneSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isNoneSelected) selectedBg else textSecondary  // Colored text when selected
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkNoteDialog(
    note: String,
    onNoteChange: (String) -> Unit,
    isOLED: Boolean,
    reverieAccentColor: Color = LithosAmber,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = if (isOLED) reverieAccentColor else LithosAmber
    val dialogBg = if (isOLED) LithosUI.DeepBackground else LithosUI.SheetBackground

    val textColor = if (isOLED) LithosTextPrimary else Color.White

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(LithosComponents.Cards.cornerRadius),  // Unified
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.BookmarkAdd,
                    null,
                    tint = accentColor,
                    modifier = Modifier.size(GlassIconSize.Medium)  // Unified
                )
                Text(
                    stringResource(R.string.player_add_bookmark),
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = textColor
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.bookmarks_add_hint),
                    style = TextStyle(fontSize = 14.sp),
                    color = textColor.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    placeholder = {
                        Text(
                            stringResource(R.string.bookmarks_add_hint),
                            color = textColor.copy(alpha = 0.4f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.3f),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = accentColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(
                    stringResource(R.string.dialog_save),
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.dialog_cancel),
                    color = textColor.copy(alpha = 0.6f)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioSettingsDialog(
    audioEffectManager: AudioEffectManager,
    isOLED: Boolean,
    reverieAccentColor: Color = LithosAmber,
    onDismiss: () -> Unit
) {
    val accentColor = if (isOLED) reverieAccentColor else LithosAmber
    val dialogBg = if (isOLED) LithosUI.DeepBackground else LithosUI.SheetBackground
    val glassBg = if (isOLED) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.05f)
    val view = LocalView.current

    // Reinitialize effects when dialog opens
    LaunchedEffect(Unit) {
        audioEffectManager.reinitialize()
    }

    // State from AudioEffectManager - REAL audio processing
    val eqEnabled by audioEffectManager.eqEnabled.collectAsState()
    val bands by audioEffectManager.bands.collectAsState()
    val selectedPreset by audioEffectManager.selectedPresetName.collectAsState()
    val bassBoostStrength by audioEffectManager.bassBoostStrength.collectAsState()
    val virtualizerStrength by audioEffectManager.virtualizerStrength.collectAsState()
    val amplifierGain by audioEffectManager.amplifierGain.collectAsState()

    // Convert dB bands (-12 to +12) to UI values (0 to 1)
    val eqBandsUI = bands.map { (it + 12f) / 24f }

    // Local state for UI features not yet in AudioEffectManager
    var skipSilence by remember { mutableStateOf(false) }
    var autoRewind by remember { mutableStateOf(true) }

    val eqFrequencies = listOf("31", "63", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    // Preset configurations - maps to AudioEffectManager presets
    fun applyPreset(preset: String) {
        val presetBands = when (preset) {
            "Flat" -> listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            "Bass" -> listOf(8f, 7f, 5f, 2f, 0f, -1f, -1f, -2f, -2f, -3f)
            "Vocal" -> listOf(-3f, -2f, -1f, 0f, 2f, 5f, 5f, 2f, 0f, -1f)
            "Treble" -> listOf(-3f, -2f, -2f, -1f, 0f, 1f, 4f, 6f, 7f, 8f)
            "Spoken" -> listOf(-4f, -3f, 0f, 2f, 5f, 6f, 4f, 1f, -1f, -2f)
            else -> return
        }
        audioEffectManager.applyPreset(preset, presetBands, 0f)
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    // Use ModalBottomSheet for native swipe-to-dismiss support
    // Force full expansion on open
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = dialogBg,
        dragHandle = {
            // Custom drag handle - swipe down anywhere above/below the EQ bands to dismiss
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // EQ Toggle - with icon inline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.VolumeUp,
                                null,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    stringResource(R.string.settings_equalizer),
                                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                Text(
                                    stringResource(R.string.settings_equalizer_desc),
                                    style = TextStyle(fontSize = 12.sp),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                        Switch(
                        checked = eqEnabled,
                        onCheckedChange = {
                            audioEffectManager.setEqEnabled(it)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }

                // Visual 10-Band EQ (Reverted to stable version)
                AnimatedVisibility(visible = eqEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "10-Band Equalizer",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        // EQ Bars - 10 bands
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            eqFrequencies.forEachIndexed { index, label ->
                                val value = eqBandsUI.getOrElse(index) { 0.5f }
                                val dBValue = bands.getOrElse(index) { 0f }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Value indicator (show actual dB)
                                    Text(
                                        text = "${dBValue.toInt()}",
                                        style = TextStyle(fontSize = 8.sp),
                                        color = accentColor
                                    )

                                    // Vertical slider track
                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .weight(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.1f))
                                            .pointerInput(index) {
                                                detectTapGestures { offset ->
                                                    val newUIValue = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                                                    // Convert UI value (0-1) to dB (-12 to +12)
                                                    val newDbValue = (newUIValue - 0.5f) * 24f
                                                    audioEffectManager.setBandLevel(index, newDbValue)
                                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                }
                                            }
                                            .pointerInput(index) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val currentDb = bands.getOrElse(index) { 0f }
                                                    val deltaDb = -(dragAmount.y / size.height) * 24f
                                                    val newDb = (currentDb + deltaDb).coerceIn(-12f, 12f)
                                                    audioEffectManager.setBandLevel(index, newDb)
                                                }
                                            }
                                    ) {
                                        // Fill from bottom based on value

                                        // Background fill
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(value.coerceIn(0f, 1f))
                                                .align(Alignment.BottomCenter)
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            accentColor,
                                                            accentColor.copy(alpha = 0.4f)
                                                        )
                                                    )
                                                )
                                        )

                                        // Center line (0dB)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .align(Alignment.Center)
                                                .background(Color.White.copy(alpha = 0.4f))
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Frequency label
                                    Text(
                                        text = label,
                                        style = TextStyle(fontSize = 7.sp),
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        // Preset chips - single word labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val presets = listOf("Flat", "Bass", "Vocal", "Treble", "Spoken")
                            presets.forEach { preset ->
                                val isSelected = selectedPreset == preset
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) accentColor
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .clickable { applyPreset(preset) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        preset,
                                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // Reset button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable {
                                    audioEffectManager.reset()
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.eq_reset),
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    stringResource(R.string.eq_reset),
                                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )

                // Volume Boost - Premium Rotary Knob
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.effect_amplifier),
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        color = if (isOLED) LithosTextPrimary else Color.White
                    )

                    // Convert amplifierGain (-12 to +12) to normalized value (0 to 1)
                    val volumeBoostNormalized = ((amplifierGain + 12f) / 24f).coerceIn(0f, 1f)

                    // Premium Circular Knob - Larger for usability
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(glassBg)
                            .border(2.dp, accentColor.copy(alpha = 0.3f), CircleShape)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    // Horizontal or vertical drag changes value
                                    val deltaDb = -(dragAmount.y / 300f) * 24f
                                    val newGain = (amplifierGain + deltaDb).coerceIn(-12f, 12f)
                                    audioEffectManager.setAmplifierGain(newGain)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer ring showing progress
                        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            val sweepAngle = volumeBoostNormalized * 270f
                            drawArc(
                                color = Color.White.copy(alpha = 0.1f),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = accentColor,
                                startAngle = 135f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Center value display
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${if (amplifierGain >= 0) "+" else ""}${amplifierGain.toInt()}",
                                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                            Text(
                                "dB",
                                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                color = if (isOLED) LithosTextSecondary else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Text(
                        "Drag up/down to adjust",
                        style = TextStyle(fontSize = 11.sp),
                        color = if (isOLED) LithosTextTertiary else Color.White.copy(alpha = 0.4f)
                    )
                }

                // Scroll indicator - subtle hint that there's more content
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "More options",
                        style = TextStyle(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.35f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Scroll for more",
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )

                // Audiobook-specific settings
                Text(
                    "Playback Enhancements",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.9f)
                )

                // Skip Silence
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { skipSilence = !skipSilence }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Speed,
                            null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                "Skip Silence",
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                                color = Color.White
                            )
                            Text(
                                "Automatically skip quiet sections",
                                style = TextStyle(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                    Switch(
                        checked = skipSilence,
                        onCheckedChange = { skipSilence = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }

                // Auto-rewind on pause
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { autoRewind = !autoRewind }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Replay,
                            null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                "Auto-Rewind",
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                                color = Color.White
                            )
                            Text(
                                "Rewind 5s when resuming",
                                style = TextStyle(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                    Switch(
                        checked = autoRewind,
                        onCheckedChange = { autoRewind = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
                } // End of scrollable content

                Spacer(modifier = Modifier.height(16.dp))

            // Done button
            Box(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(
                        stringResource(R.string.dialog_done),
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Bottom padding for ModalBottomSheet
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ============================================================================
// EDIT BOOK DIALOG - Metadata & Cover Art Editor
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBookDialog(
    book: com.mossglen.lithos.data.Book,
    isOLED: Boolean,
    reverieAccentColor: Color = LithosAmber,
    onSave: (title: String, author: String, narrator: String, date: String) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = if (isOLED) reverieAccentColor else LithosAmber
    val dialogBg = if (isOLED) LithosUI.DeepBackground else LithosUI.SheetBackground
    val textColor = if (isOLED) LithosTextPrimary else Color.White
    val secondaryText = if (isOLED) LithosTextSecondary else Color.White.copy(alpha = 0.6f)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Editable fields
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author) }
    var narrator by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(dialogBg)
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }
                Text(
                    "Edit",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = textColor
                )
                IconButton(onClick = { onSave(title, author, narrator, date) }) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Save",
                        tint = accentColor
                    )
                }
            }

            // Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("COVER", "TRACKS").forEachIndexed { index, tabName ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            tabName,
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (selectedTab == index) textColor else secondaryText
                        )
                        if (selectedTab == index) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(2.dp)
                                    .background(accentColor, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
            }

            // Content based on selected tab
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedTab == 0) {
                    // COVER TAB
                    // Cover Art Display
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = book.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Cover Action Buttons - Icons only, no labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Download Cover
                        CoverActionIcon(
                            icon = Icons.Rounded.Download,
                            accentColor = accentColor,
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        Toast.makeText(context, "Downloading cover...", Toast.LENGTH_SHORT).show()

                                        val coverUrl = book.coverUrl
                                        if (coverUrl.isNullOrBlank()) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "No cover art available", Toast.LENGTH_SHORT).show()
                                            }
                                            return@launch
                                        }

                                        // Download image in background
                                        val result = withContext(Dispatchers.IO) {
                                            try {
                                                val url = URL(coverUrl)
                                                val connection = url.openConnection()
                                                connection.connect()
                                                val inputStream = connection.getInputStream()

                                                // Create filename with timestamp
                                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                                val sanitizedTitle = book.title.replace(Regex("[^a-zA-Z0-9]"), "_")
                                                val fileName = "Reverie_${sanitizedTitle}_$timestamp.jpg"

                                                // Save to Pictures directory using MediaStore
                                                val contentValues = ContentValues().apply {
                                                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                                                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Reverie")
                                                }

                                                val uri = context.contentResolver.insert(
                                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                    contentValues
                                                )

                                                if (uri != null) {
                                                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                                        inputStream.copyTo(outputStream)
                                                    }
                                                    inputStream.close()
                                                    true
                                                } else {
                                                    false
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                false
                                            }
                                        }

                                        withContext(Dispatchers.Main) {
                                            if (result) {
                                                Toast.makeText(context, "Cover saved to Pictures/Reverie", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Failed to download cover", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )

                        // Web Search
                        CoverActionIcon(
                            icon = Icons.Rounded.ImageSearch,
                            accentColor = accentColor,
                            onClick = {
                                try {
                                    // Create search query for Google Images
                                    val searchQuery = "${book.title} ${book.author} book cover"
                                    val encodedQuery = Uri.encode(searchQuery)
                                    val searchUrl = "https://www.google.com/search?tbm=isch&q=$encodedQuery"

                                    // Open browser with search URL
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        // Google Play Books Search - Book icon
                        CoverActionIcon(
                            icon = Icons.Rounded.LibraryBooks,
                            accentColor = accentColor,
                            onClick = {
                                try {
                                    // Create search query for Google Play Books
                                    val searchQuery = "${book.title} ${book.author}"
                                    val encodedQuery = Uri.encode(searchQuery)
                                    val playBooksUrl = "https://play.google.com/store/search?q=$encodedQuery&c=books"

                                    // Open browser with Play Books search URL
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playBooksUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Unable to open Play Books", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Metadata Fields
                    EditField(
                        label = "Title",
                        value = title,
                        onValueChange = { title = it },
                        accentColor = accentColor,
                        textColor = textColor,
                        secondaryText = secondaryText
                    )

                    EditField(
                        label = "Author",
                        value = author,
                        onValueChange = { author = it },
                        accentColor = accentColor,
                        textColor = textColor,
                        secondaryText = secondaryText
                    )

                    EditField(
                        label = "Narrated By",
                        value = narrator,
                        onValueChange = { narrator = it },
                        accentColor = accentColor,
                        textColor = textColor,
                        secondaryText = secondaryText
                    )

                    EditField(
                        label = "Date",
                        value = date,
                        onValueChange = { date = it },
                        accentColor = accentColor,
                        textColor = textColor,
                        secondaryText = secondaryText
                    )
                } else {
                    // TRACKS TAB
                    Text(
                        "Track management coming soon",
                        style = TextStyle(fontSize = 14.sp),
                        color = secondaryText,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverActionIcon(
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accentColor: Color,
    textColor: Color,
    secondaryText: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            label,
            style = TextStyle(fontSize = 12.sp),
            color = secondaryText
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            ),
            cursorBrush = SolidColor(accentColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(secondaryText.copy(alpha = 0.3f))
        )
    }
}

// ============================================================================
// MARK AS DIALOG
// ============================================================================

@Composable
private fun MarkAsDialog(
    isOLED: Boolean,
    reverieAccentColor: Color = LithosAmber,
    onMarkAs: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = if (isOLED) reverieAccentColor else LithosAmber
    val dialogBg = if (isOLED) LithosUI.DeepBackground else LithosUI.SheetBackground
    val textColor = if (isOLED) LithosTextPrimary else Color.White

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(LithosComponents.Cards.cornerRadius),  // Unified
        title = {
            Text(
                "Mark as...",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = textColor
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    Triple("Not Started", Icons.Rounded.RadioButtonUnchecked, "Reset progress"),
                    Triple("In Progress", Icons.Rounded.PlayCircleOutline, "Currently listening"),
                    Triple("Finished", Icons.Rounded.CheckCircle, "Mark as completed")
                ).forEach { (status, icon, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(LithosComponents.Cards.chipRadius))  // Unified
                            .clickable { onMarkAs(status) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GlassSpacing.M)
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(GlassIconSize.Medium)  // Unified
                        )
                        Column {
                            Text(
                                status,
                                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                                color = textColor
                            )
                            Text(
                                description,
                                style = TextStyle(fontSize = 12.sp),
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

// ============================================================================
// DELETE CONFIRM DIALOG
// ============================================================================

@Composable
private fun DeleteConfirmDialog(
    isOLED: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogBg = if (isOLED) LithosUI.DeepBackground else LithosUI.SheetBackground
    val textColor = if (isOLED) LithosTextPrimary else Color.White
    val destructiveColor = LithosUI.Destructive

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(LithosComponents.Cards.cornerRadius),  // Unified
        icon = {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = null,
                tint = destructiveColor,
                modifier = Modifier.size(GlassIconSize.XLarge)  // Unified
            )
        },
        title = {
            Text(
                "Delete Book?",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                "This will permanently remove the book from your library. This action cannot be undone.",
                style = TextStyle(fontSize = 14.sp),
                color = textColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Delete",
                    color = destructiveColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = textColor.copy(alpha = 0.6f)
                )
            }
        }
    )
}
