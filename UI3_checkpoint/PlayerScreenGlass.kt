package com.mossglen.reverie.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.mossglen.reverie.R
import com.mossglen.reverie.data.Book
import com.mossglen.reverie.ui.theme.*
import com.mossglen.reverie.ui.viewmodel.PlayerViewModel
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
import com.mossglen.reverie.data.AudioEffectManager
import com.mossglen.reverie.ui.viewmodel.SettingsViewModel

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
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    highlightColor: Color = GlassColors.WarmSlate,
    useBorderHighlight: Boolean = false,
    dynamicColors: Boolean = true,
    onBack: () -> Unit
) {
    // Get AudioEffectManager for Quick EQ
    val audioEffectManager = settingsViewModel.audioEffectManager

    // Reverie Dark uses deep amber accent and muted colors
    val accentColor = if (isReverieDark) reverieAccentColor else Color.White
    // Selection highlight - use warm slate or border style based on preference
    val selectionBg = if (isReverieDark) highlightColor else Color.White.copy(alpha = 0.1f)
    val textColor = if (isReverieDark) GlassColors.ReverieTextPrimary else Color.White
    val secondaryTextColor = if (isReverieDark) GlassColors.ReverieTextSecondary else Color.White.copy(alpha = 0.7f)

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
    val customSpeedPresets by playerViewModel.customSpeedPresets.collectAsState(initial = emptyList())

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

    // Chapter skip animation state - Flash Transition approach
    // Phase 1: Fill (0-200ms) - progress smoothly fills to 100%
    // Phase 2: Flash (200-250ms) - bright pulse at end of bar
    // Phase 3: Crossfade (250-350ms) - old fades out, new fades in (NO backwards animation)
    var previousChapter by remember { mutableIntStateOf(0) }
    var isChapterSkipAnimating by remember { mutableStateOf(false) }
    var chapterSkipPhase by remember { mutableIntStateOf(0) } // 0=none, 1=fill, 2=flash, 3=crossfade
    var chapterSkipFlashAlpha by remember { mutableFloatStateOf(0f) } // For flash effect
    var crossfadeAlpha by remember { mutableFloatStateOf(1f) } // Alpha for crossfade (1=visible, 0=hidden)
    var displayedProgress by remember { mutableFloatStateOf(0f) } // The actual displayed progress value

    // Gesture states
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var gestureIndicator by remember { mutableStateOf<GestureType?>(null) }
    val swipeThreshold = 80f

    // Spring animations
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
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

    // Animated fill progress for Phase 1 (smooth fill to 100%)
    val animatedFillProgress by animateFloatAsState(
        targetValue = when {
            chapterSkipPhase == 1 -> 1f  // Fill to 100%
            else -> chapterProgress  // Normal progress
        },
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "fillProgress",
        finishedListener = { value ->
            if (chapterSkipPhase == 1 && value >= 0.99f) {
                // Fill complete, move to flash phase
                chapterSkipPhase = 2
            }
        }
    )

    // Animated flash alpha for Phase 2
    val animatedFlashAlpha by animateFloatAsState(
        targetValue = chapterSkipFlashAlpha,
        animationSpec = tween(
            durationMillis = 50,
            easing = LinearEasing
        ),
        label = "flashAlpha",
        finishedListener = { value ->
            if (chapterSkipPhase == 2) {
                if (value >= 0.9f) {
                    // Flash peaked, now fade out
                    chapterSkipFlashAlpha = 0f
                } else if (value <= 0.1f && chapterSkipFlashAlpha <= 0.1f) {
                    // Flash complete, move to crossfade phase
                    chapterSkipPhase = 3
                    crossfadeAlpha = 0f  // Start crossfade
                }
            }
        }
    )

    // Animated crossfade alpha for Phase 3
    val animatedCrossfadeAlpha by animateFloatAsState(
        targetValue = crossfadeAlpha,
        animationSpec = tween(
            durationMillis = 100,
            easing = FastOutSlowInEasing
        ),
        label = "crossfadeAlpha",
        finishedListener = { value ->
            if (chapterSkipPhase == 3 && value <= 0.01f) {
                // Crossfade complete - instantly snap to new position and fade back in
                displayedProgress = chapterProgress
                crossfadeAlpha = 1f
                chapterSkipPhase = 4  // Final fade-in phase
            } else if (chapterSkipPhase == 4 && value >= 0.99f) {
                // Animation complete
                isChapterSkipAnimating = false
                chapterSkipPhase = 0
            }
        }
    )

    // Calculate the displayed progress value based on animation phase
    val animatedChapterProgress = when (chapterSkipPhase) {
        1 -> animatedFillProgress  // Phase 1: Filling to 100%
        2 -> 1f  // Phase 2: Hold at 100% during flash
        3 -> 1f  // Phase 3: Still at 100% while fading out
        4 -> chapterProgress  // Phase 4: Show new chapter progress while fading in
        else -> chapterProgress  // Normal: just show current progress
    }

    // Track alpha for crossfade effect (used in slider)
    val trackAlpha = when (chapterSkipPhase) {
        3 -> animatedCrossfadeAlpha  // Fading out
        4 -> animatedCrossfadeAlpha  // Fading in
        else -> 1f
    }

    // Flash glow intensity
    val flashGlowIntensity = if (chapterSkipPhase == 2) animatedFlashAlpha else 0f

    // Trigger flash when entering phase 2
    LaunchedEffect(chapterSkipPhase) {
        if (chapterSkipPhase == 2) {
            chapterSkipFlashAlpha = 1f  // Trigger flash
        }
    }

    // Detect chapter changes and trigger skip animation
    LaunchedEffect(currentChapter) {
        if (previousChapter != currentChapter && previousChapter > 0) {
            // Chapter changed - trigger flash transition animation
            isChapterSkipAnimating = true
            chapterSkipPhase = 1  // Start with fill phase
            chapterSkipFlashAlpha = 0f
            crossfadeAlpha = 1f
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

    // Background color based on mode
    val backgroundColor = if (isReverieDark) Color(0xFF050505) else Color.Black

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
                        .blur(if (isReverieDark) 60.dp else 50.dp)
                        .graphicsLayer {
                            alpha = if (isReverieDark) 0.35f else 0.6f
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }

        // LAYER 2: Gradient overlay - Top 15% clear, transition 15-35%, dark 35-100%
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,                // Top - completely clear
                            0.15f to Color.Transparent,               // Still clear at 15%
                            0.25f to Color.Black.copy(alpha = 0.3f),  // Start darkening
                            0.35f to Color.Black.copy(alpha = 0.6f),  // Transition complete
                            0.50f to Color.Black.copy(alpha = 0.85f), // Getting dark
                            0.70f to Color.Black.copy(alpha = 0.95f), // Very dark
                            1.0f to Color.Black                       // Solid black at bottom
                        )
                    )
                )
        )

        // LAYER 3: Main content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar - Minimal, floating with playback-focused overflow menu
            TopPlayerBar(
                onBack = onBack,
                onOverflowClick = { showOverflowMenu = true },
                onBookmark = {
                    playerViewModel.toggleBookmark()
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    Toast.makeText(view.context, context.getString(R.string.bookmarks_add_hint), Toast.LENGTH_SHORT).show()
                },
                sleepTimerActive = sleepTimerMinutes != null,
                sleepTimerMinutes = sleepTimerMinutes,
                showOverflowMenu = showOverflowMenu,
                onDismissOverflow = { showOverflowMenu = false },
                onShareClick = {
                    showOverflowMenu = false
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
                onAudioSettingsClick = {
                    showOverflowMenu = false
                    showAudioSettings = true
                },
                isReverieDark = isReverieDark,
                reverieAccentColor = reverieAccentColor
            )

            // Album Art - THE HERO (large, fills available space - MAXIMIZED)
            // Tap = Show book details overlay (progressive disclosure)
            // Double-tap = Play/pause or skip (power user gesture)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)  // Fill available space for maximum cover size
                    .padding(horizontal = 20.dp)  // Reduced padding for larger cover
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
                                    swipeOffset < -swipeThreshold -> {
                                        gestureIndicator = GestureType.SKIP_FORWARD
                                        playerViewModel.skipForward()
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    }
                                    swipeOffset > swipeThreshold -> {
                                        gestureIndicator = GestureType.SKIP_BACK
                                        playerViewModel.skipBackward()
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
                    enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    GestureIndicator(type = gestureIndicator ?: GestureType.PLAY)
                }
            }

            // CONTROLS SECTION - New Layout Order:
            // 1. Spacer (generous breathing room from cover)
            // 2. Navigation Ring (shows chapter position "3/12")
            // 3. Chapter progress bar
            // 4. Time row (chapter time left | book time right)
            // 5. Playback controls
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                    // Generous spacing between cover and ring
                    Spacer(modifier = Modifier.height(24.dp))

                    // 1. NAVIGATION RING - Above progress bar for visual hierarchy
                    // Ring shows overall position, progress bar shows detailed chapter position
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                            isReverieDark = isReverieDark,
                            reverieAccentColor = reverieAccentColor,
                            onTap = { showChaptersDialog = true }
                        )
                    }

                    // Tight spacing - ring visually connected to progress bar
                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. CHAPTER PROGRESS BAR - Below ring
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        // Premium progress slider - tracks CHAPTER progress for easy chapter navigation
                        var isSeeking by remember { mutableStateOf(false) }
                        var seekPreviewProgress by remember { mutableFloatStateOf(0f) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Time preview overlay (shows when scrubbing) - positioned above slider
                            if (isSeeking) {
                                val previewTimeMs = chapterStartMs + (seekPreviewProgress * chapterDuration).toLong()
                                val scale by animateFloatAsState(
                                    targetValue = if (isSeeking) 1f else 0.8f,
                                    animationSpec = spring(dampingRatio = 0.8f),
                                    label = "previewScale"
                                )
                                val alpha by animateFloatAsState(
                                    targetValue = if (isSeeking) 1f else 0f,
                                    animationSpec = spring(dampingRatio = 0.8f),
                                    label = "previewAlpha"
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = 0.dp)
                                        .scale(scale)
                                        .alpha(alpha)
                                        .background(
                                            color = if (isReverieDark)
                                                Color(0xFF1C1C1E).copy(alpha = 0.95f)
                                            else
                                                Color.Black.copy(alpha = 0.75f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isReverieDark)
                                                reverieAccentColor.copy(alpha = 0.3f)
                                            else
                                                Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = formatTime(previewTimeMs - chapterStartMs),
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = if (isReverieDark) reverieAccentColor else Color.White
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
                                    isReverieDark = isReverieDark,
                                    reverieAccentColor = reverieAccentColor,
                                    isChapterSkipAnimating = isChapterSkipAnimating,
                                    trackAlpha = trackAlpha,
                                    flashGlowIntensity = flashGlowIntensity,
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

                        // 3. TIME ROW - Split layout: chapter time (left) | book time remaining (right)
                        val chapterRemainingMs = (chapterDuration - chapterPosition).coerceAtLeast(0L)
                        val bookRemainingMs = duration - position
                        val bookHoursLeft = bookRemainingMs / 1000 / 3600
                        val bookMinutesLeft = (bookRemainingMs / 1000 % 3600) / 60
                        val bookTimeLeftText = if (bookHoursLeft > 0) {
                            "-${bookHoursLeft}h ${bookMinutesLeft}m left"
                        } else {
                            "-${bookMinutesLeft}m left"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // LEFT: Chapter time (current / total) e.g., "2:34 / 45:12"
                            Text(
                                text = "${formatTime(chapterPosition)} / ${formatTime(chapterDuration)}",
                                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                color = if (isReverieDark) secondaryTextColor else Color.White.copy(alpha = 0.5f)
                            )
                            // RIGHT: Book time remaining e.g., "-3h 24m left"
                            Text(
                                text = bookTimeLeftText,
                                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                color = if (isReverieDark) accentColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Spacing before controls
                    Spacer(modifier = Modifier.height(20.dp))


                    // Main Controls - Premium floating design
                    MainPlayerControls(
                        isPlaying = isPlaying,
                        isReverieDark = isReverieDark,
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

                    // Bottom Control Pill - Glass floating
                    BottomControlPill(
                        playbackSpeed = playbackSpeed,
                        sleepTimerMinutes = sleepTimerMinutes,
                        sleepTimerRemaining = sleepTimerRemaining,
                        isReverieDark = isReverieDark,
                        reverieAccentColor = reverieAccentColor,
                        highlightColor = highlightColor,
                        onSpeedClick = { showSpeedPicker = true },
                        onSleepClick = { showSleepTimer = true },
                        onChaptersClick = { showChaptersDialog = true },
                        onAudioClick = { showAudioSettings = true },
                        onBookmarkTap = {
                            // Quick bookmark - instant with toast feedback + ripple animation
                            playerViewModel.toggleBookmark()
                            bookmarkRippleTrigger++  // Trigger bookmark glow animation on NavigationRing
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            Toast.makeText(context, context.getString(R.string.player_add_bookmark), Toast.LENGTH_SHORT).show()
                        },
                        onBookmarkLongPress = {
                            // Long press - show note dialog
                            bookmarkNote = ""
                            showBookmarkNoteDialog = true
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
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
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ),
            exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(
                targetOffsetY = { it / 4 },
                animationSpec = tween(150)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showBookDetailsOverlay = false
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
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
                            color = if (isReverieDark) reverieAccentColor else Color.White.copy(alpha = 0.85f),
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
                isReverieDark = isReverieDark,
                reverieAccentColor = reverieAccentColor,
                highlightColor = highlightColor,
                onSpeedSelected = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    playerViewModel.setPlaybackSpeed(it)
                    showSpeedPicker = false
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
                isReverieDark = isReverieDark,
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
                isReverieDark = isReverieDark,
                reverieAccentColor = reverieAccentColor,
                highlightColor = highlightColor,
                onSeekTo = { ms ->
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    playerViewModel.seekTo(ms)
                    showChaptersDialog = false
                },
                onDismiss = { showChaptersDialog = false }
            )
        }

        if (showSleepTimer) {
            PremiumSleepDialog(
                sleepTimerMinutes = sleepTimerMinutes,
                isReverieDark = isReverieDark,
                reverieAccentColor = reverieAccentColor,
                onTimerSet = { minutes ->
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    playerViewModel.setSleepTimer(minutes)
                    showSleepTimer = false
                },
                onEndOfChapter = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    playerViewModel.setSleepTimerEndOfChapter()
                    showSleepTimer = false
                },
                onCancelTimer = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    playerViewModel.cancelSleepTimer()
                    showSleepTimer = false
                },
                onDismiss = { showSleepTimer = false }
            )
        }

        // Bookmark with note dialog
        if (showBookmarkNoteDialog) {
            BookmarkNoteDialog(
                note = bookmarkNote,
                onNoteChange = { bookmarkNote = it },
                isReverieDark = isReverieDark,
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
                isReverieDark = isReverieDark,
                reverieAccentColor = reverieAccentColor,
                onDismiss = { showAudioSettings = false }
            )
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
    onBack: () -> Unit,
    onOverflowClick: () -> Unit,
    onBookmark: () -> Unit,
    sleepTimerActive: Boolean,
    sleepTimerMinutes: Int?,
    showOverflowMenu: Boolean,
    onDismissOverflow: () -> Unit,
    onShareClick: () -> Unit,
    onAudioSettingsClick: () -> Unit,
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent
) {
    val menuBg = if (isReverieDark) Color(0xFF0A0A0A) else Color(0xFF2C2C2E)
    val menuTextColor = if (isReverieDark) GlassColors.ReverieTextPrimary else Color.White
    val menuIconColor = if (isReverieDark) reverieAccentColor else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),  // Reduced vertical padding
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        TopBarButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            onClick = onBack,
            size = 44.dp
        )

        // Center - Sleep timer indicator
        if (sleepTimerActive && sleepTimerMinutes != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Rounded.Bedtime,
                    null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "${sleepTimerMinutes}m",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(44.dp))
        }

        // Right - Overflow menu with proper anchoring
        Box {
            TopBarButton(icon = Icons.Rounded.MoreVert, onClick = onOverflowClick)

            // Dropdown anchored to the overflow button - Playback focused only
            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = onDismissOverflow,
                modifier = Modifier
                    .background(menuBg)
                    .widthIn(min = 200.dp)
            ) {
                // Bookmark
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.player_add_bookmark), color = menuTextColor) },
                    onClick = {
                        onBookmark()
                        onDismissOverflow()
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.BookmarkAdd, null, tint = menuIconColor)
                    }
                )

                // Audio Settings
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings_equalizer), color = menuTextColor) },
                    onClick = onAudioSettingsClick,
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = menuIconColor)
                    }
                )

                // Divider
                HorizontalDivider(
                    color = if (isReverieDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Share Progress
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_share), color = menuTextColor) },
                    onClick = onShareClick,
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
    size: Dp = 40.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
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
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(if (size > 40.dp) 28.dp else 22.dp)
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
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    isChapterSkipAnimating: Boolean = false,
    trackAlpha: Float = 1f,  // For crossfade effect
    flashGlowIntensity: Float = 0f,  // For flash effect at chapter completion
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    // State for tracking if user is actively dragging
    var isDragging by remember { mutableStateOf(false) }
    var previewValue by remember { mutableFloatStateOf(value) }

    val animatedValue by animateFloatAsState(
        targetValue = if (isDragging) previewValue else value,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "slider"
    )

    // Reverie Dark uses dynamic accent color
    val baseTrackColor = if (isReverieDark) reverieAccentColor else Color.White
    val thumbColor = if (isReverieDark) reverieAccentColor else Color.White
    val inactiveColor = if (isReverieDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.2f)

    // Enhanced track color during chapter skip animation with flash effect
    val trackColor = when {
        flashGlowIntensity > 0f -> {
            // Flash phase - bright pulse
            baseTrackColor.copy(alpha = 0.8f + flashGlowIntensity * 0.2f)
        }
        isChapterSkipAnimating -> {
            // Fill/crossfade phases - apply trackAlpha for crossfade
            baseTrackColor.copy(alpha = trackAlpha)
        }
        else -> baseTrackColor
    }

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
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                // Apply crossfade alpha to entire slider during transition
                alpha = if (isChapterSkipAnimating) trackAlpha.coerceIn(0.3f, 1f) else 1f
            },
        interactionSource = interactionSource,
        enabled = !isChapterSkipAnimating,  // Disable during animation
        colors = SliderDefaults.colors(
            thumbColor = thumbColor.copy(alpha = trackAlpha),
            activeTrackColor = trackColor,
            inactiveTrackColor = inactiveColor
        ),
        thumb = {
            // Animated thumb that scales up when dragging or during chapter skip
            val thumbScale by animateFloatAsState(
                targetValue = when {
                    flashGlowIntensity > 0f -> 1.4f  // Pop during flash
                    isChapterSkipAnimating -> 1.2f  // Slightly enlarged during animation
                    isDragging -> 1.3f
                    else -> 1f
                },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "thumbScale"
            )

            // Glow effect during flash phase
            val glowSize by animateFloatAsState(
                targetValue = when {
                    flashGlowIntensity > 0f -> 32f  // Larger glow during flash
                    isChapterSkipAnimating -> 20f
                    else -> 0f
                },
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
                label = "glowSize"
            )

            Box(contentAlignment = Alignment.Center) {
                // Outer glow during chapter skip - enhanced during flash
                if ((isChapterSkipAnimating || flashGlowIntensity > 0f) && glowSize > 0f) {
                    val glowIntensity = if (flashGlowIntensity > 0f) flashGlowIntensity else 0.5f
                    Box(
                        modifier = Modifier
                            .size(glowSize.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        thumbColor.copy(alpha = glowIntensity * 0.8f),
                                        thumbColor.copy(alpha = glowIntensity * 0.4f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )
                }

                // Main thumb with crossfade alpha
                Box(
                    modifier = Modifier
                        .scale(thumbScale)
                        .size(16.dp)
                        .graphicsLayer {
                            alpha = if (isChapterSkipAnimating) trackAlpha.coerceIn(0.4f, 1f) else 1f
                        }
                        .shadow(
                            elevation = when {
                                flashGlowIntensity > 0f -> 20.dp  // Extra glow during flash
                                isChapterSkipAnimating -> 16.dp
                                isDragging -> 12.dp
                                else -> 8.dp
                            },
                            shape = CircleShape,
                            spotColor = if (isChapterSkipAnimating || flashGlowIntensity > 0f) thumbColor else Color.Black
                        )
                        .background(thumbColor, CircleShape)
                )
            }
        },
        track = { sliderState ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(inactiveColor)
            ) {
                // Flash glow layer - bright pulse during flash phase
                if (flashGlowIntensity > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(sliderState.value)
                            .fillMaxHeight()
                            .graphicsLayer {
                                shadowElevation = (16.dp * flashGlowIntensity).toPx()
                            }
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        baseTrackColor.copy(alpha = flashGlowIntensity * 0.8f),
                                        baseTrackColor.copy(alpha = flashGlowIntensity),
                                        Color.White.copy(alpha = flashGlowIntensity)  // Bright flash at end
                                    )
                                )
                            )
                    )
                }

                // Main track with crossfade alpha
                Box(
                    modifier = Modifier
                        .fillMaxWidth(sliderState.value)
                        .fillMaxHeight()
                        .graphicsLayer {
                            alpha = if (isChapterSkipAnimating) trackAlpha else 1f
                        }
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
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    onSkipBack: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSkipForward: () -> Unit
) {
    // Layout: [Prev Chapter] [Skip Back] [PLAY] [Skip Fwd] [Next Chapter]
    // Frequency Proximity: Most used actions closest to center (thumb zone)
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
            iconSize = 26.dp
        )

        // Skip back 30s - INSIDE (more frequent, next to play)
        ControlButton(
            icon = Icons.Rounded.Replay30,
            onClick = onSkipBack,
            size = 52.dp,
            iconSize = 30.dp
        )

        // Play/Pause - Hero button (CENTER)
        PlayPauseButton(
            isPlaying = isPlaying,
            isReverieDark = isReverieDark,
            reverieAccentColor = reverieAccentColor,
            onClick = onPlayPause
        )

        // Skip forward 30s - INSIDE (more frequent, next to play)
        ControlButton(
            icon = Icons.Rounded.Forward30,
            onClick = onSkipForward,
            size = 52.dp,
            iconSize = 30.dp
        )

        // Next Chapter - OUTSIDE (less frequent)
        ControlButton(
            icon = Icons.Rounded.SkipNext,
            onClick = onNext,
            size = 44.dp,
            iconSize = 26.dp
        )
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    size: Dp,
    iconSize: Dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
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
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "scale"
    )

    // Dark button with accent-colored icon for premium feel
    val buttonColor = if (isReverieDark) Color(0xFF1C1C1E) else Color(0xFF2C2C2E)
    val iconColor = if (isReverieDark) reverieAccentColor else Color.White

    Box(
        modifier = Modifier
            .scale(scale)
            .size(80.dp)  // Slightly larger button
            .shadow(20.dp, CircleShape, spotColor = if (isReverieDark) reverieAccentColor.copy(alpha = 0.3f) else Color.Black)
            .clip(CircleShape)
            .background(buttonColor)
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
    chapters: List<com.mossglen.reverie.data.Chapter>,
    bookmarks: List<Long>,
    duration: Long,
    timeRemainingMs: Long,
    currentChapter: Int,
    totalChapters: Int,
    rippleDirection: Int = 0, // -1 = left (back), 0 = none, 1 = right (forward)
    bookmarkRippleTrigger: Int = 0, // Increments when bookmark is created
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animation for press feedback
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
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
    // BOOKMARK WATER DROPLET ANIMATION
    // Beautiful one-directional animation like a pebble dropped in still water
    // Phase 1: GLOW (0-200ms) - Ring illuminates from within
    // Phase 2: RIPPLE (200-800ms) - Single wave expands outward, fades as it travels
    // Phase 3: SETTLE (800-1000ms) - Glow fades smoothly to 0
    // =========================================================================

    // Glow alpha - the inner illumination (LED warming up effect)
    val glowAlpha = remember { Animatable(0f) }

    // Ripple scale - expands from ring edge outward (1.0 = ring edge, 1.5 = 50% beyond)
    val rippleScale = remember { Animatable(1f) }

    // Ripple alpha - fades as ripple travels outward
    val rippleAlpha = remember { Animatable(0f) }

    LaunchedEffect(bookmarkRippleTrigger) {
        if (bookmarkRippleTrigger > 0) {
            // Reset all values to start state
            glowAlpha.snapTo(0f)
            rippleScale.snapTo(1f)
            rippleAlpha.snapTo(0f)

            // Phase 1: GLOW (0-200ms) - Ring illuminates from within
            // Like an LED warming up
            launch {
                glowAlpha.animateTo(
                    targetValue = 0.6f,
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
            }

            // Phase 2: RIPPLE (200-800ms) - Single wave expands OUTWARD
            // NO RETURN - this is critical, never animate back inward
            delay(200)
            launch {
                // Ripple appears
                rippleAlpha.snapTo(0.4f)
                // Ripple expands and fades simultaneously
                launch {
                    rippleScale.animateTo(
                        targetValue = 1.5f,
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = LinearOutSlowInEasing
                        )
                    )
                }
                rippleAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = LinearOutSlowInEasing
                    )
                )
            }

            // Phase 3: SETTLE (800-1000ms) - Glow fades smoothly
            delay(600)
            glowAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 200,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    // Wave 1 - first ripple (fastest start)
    val wave1Progress by animateFloatAsState(
        targetValue = if (isAnimating) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "wave1"
    )

    // Wave 2 - second ripple (slight delay via different easing)
    val wave2Progress by animateFloatAsState(
        targetValue = if (isAnimating) 1f else 0f,
        animationSpec = tween(durationMillis = 700, delayMillis = 100, easing = LinearOutSlowInEasing),
        label = "wave2"
    )

    // Wave 3 - third ripple (most delay for resonance trail)
    val wave3Progress by animateFloatAsState(
        targetValue = if (isAnimating) 1f else 0f,
        animationSpec = tween(durationMillis = 750, delayMillis = 200, easing = LinearOutSlowInEasing),
        label = "wave3"
    )

    // Colors
    val progressColor = if (isReverieDark) reverieAccentColor else Color.White
    val trackColor = if (isReverieDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.15f)
    val chapterTickColor = if (isReverieDark) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.5f)
    val bookmarkColor = if (isReverieDark) reverieAccentColor else Color(0xFFFFD700) // Gold for bookmarks
    val textColor = if (isReverieDark) GlassColors.ReverieTextPrimary else Color.White
    val secondaryTextColor = if (isReverieDark) GlassColors.ReverieTextSecondary else Color.White.copy(alpha = 0.6f)

    // Ring dimensions - compact, unobtrusive
    val ringSize = 48.dp
    val strokeWidth = 2.5.dp
    val tickLength = 4.dp
    val bookmarkDotSize = 4.dp

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

            // 1. Track (background circle)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(
                    (size.width - canvasSize + strokeWidth.toPx()) / 2,
                    (size.height - canvasSize + strokeWidth.toPx()) / 2
                ),
                size = androidx.compose.ui.geometry.Size(
                    canvasSize - strokeWidth.toPx(),
                    canvasSize - strokeWidth.toPx()
                ),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // 2. Progress arc
            drawArc(
                color = progressColor,
                startAngle = startAngle,
                sweepAngle = progress * 360f,
                useCenter = false,
                topLeft = Offset(
                    (size.width - canvasSize + strokeWidth.toPx()) / 2,
                    (size.height - canvasSize + strokeWidth.toPx()) / 2
                ),
                size = androidx.compose.ui.geometry.Size(
                    canvasSize - strokeWidth.toPx(),
                    canvasSize - strokeWidth.toPx()
                ),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // 3. Chapter ticks (if we have real chapters)
            if (chapters.isNotEmpty() && duration > 0) {
                chapters.forEach { chapter ->
                    val chapterProgress = chapter.startMs.toFloat() / duration.toFloat()
                    val angle = startAngle + (chapterProgress * 360f)
                    val angleRad = Math.toRadians(angle.toDouble())

                    val innerRadius = radius - tickLength.toPx() / 2
                    val outerRadius = radius + tickLength.toPx() / 2

                    val startX = center.x + (innerRadius * kotlin.math.cos(angleRad)).toFloat()
                    val startY = center.y + (innerRadius * kotlin.math.sin(angleRad)).toFloat()
                    val endX = center.x + (outerRadius * kotlin.math.cos(angleRad)).toFloat()
                    val endY = center.y + (outerRadius * kotlin.math.sin(angleRad)).toFloat()

                    drawLine(
                        color = chapterTickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 4. Bookmark dots
            if (duration > 0) {
                bookmarks.forEach { bookmarkMs ->
                    val bookmarkProgress = bookmarkMs.toFloat() / duration.toFloat()
                    val angle = startAngle + (bookmarkProgress * 360f)
                    val angleRad = Math.toRadians(angle.toDouble())

                    val dotX = center.x + (radius * kotlin.math.cos(angleRad)).toFloat()
                    val dotY = center.y + (radius * kotlin.math.sin(angleRad)).toFloat()

                    // Draw bookmark dot
                    drawCircle(
                        color = bookmarkColor,
                        radius = bookmarkDotSize.toPx() / 2,
                        center = Offset(dotX, dotY)
                    )
                }
            }

            // 5. Resonating ripple effect - subtle, muted waves
            val rippleColor = Color.White.copy(alpha = 0.15f) // Muted white, not accent

            // Wave 1 - innermost, fastest
            if (wave1Progress > 0f) {
                val wave1Radius = radius * (1f + wave1Progress * 0.5f)
                val wave1Alpha = (1f - wave1Progress) * 0.2f

                drawCircle(
                    color = rippleColor.copy(alpha = wave1Alpha),
                    radius = wave1Radius,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }

            // Wave 2 - middle wave
            if (wave2Progress > 0f) {
                val wave2Radius = radius * (1f + wave2Progress * 0.7f)
                val wave2Alpha = (1f - wave2Progress) * 0.15f

                drawCircle(
                    color = rippleColor.copy(alpha = wave2Alpha),
                    radius = wave2Radius,
                    center = center,
                    style = Stroke(width = 1f)
                )
            }

            // Wave 3 - outermost, slowest fade for resonance trail
            if (wave3Progress > 0f) {
                val wave3Radius = radius * (1f + wave3Progress * 0.9f)
                val wave3Alpha = (1f - wave3Progress) * 0.1f

                drawCircle(
                    color = rippleColor.copy(alpha = wave3Alpha),
                    radius = wave3Radius,
                    center = center,
                    style = Stroke(width = 0.5f)
                )
            }

            // =========================================================================
            // 6. BOOKMARK WATER DROPLET EFFECT - Beautiful one-directional animation
            // Like dropping a pebble in still water - fluid, never bounces back
            // =========================================================================
            val bookmarkGlowColor = bookmarkColor
            val currentGlowAlpha = glowAlpha.value
            val currentRippleScale = rippleScale.value
            val currentRippleAlpha = rippleAlpha.value

            // Phase 1 & 3: GLOW - Soft illumination behind the ring
            // Drawn as a soft filled circle that appears to glow from within
            if (currentGlowAlpha > 0f) {
                // Outer soft glow halo
                drawCircle(
                    color = bookmarkGlowColor.copy(alpha = currentGlowAlpha * 0.15f),
                    radius = radius * 1.3f,
                    center = center
                )
                // Inner brighter glow
                drawCircle(
                    color = bookmarkGlowColor.copy(alpha = currentGlowAlpha * 0.25f),
                    radius = radius * 1.1f,
                    center = center
                )
                // Core glow right at ring edge
                drawCircle(
                    color = bookmarkGlowColor.copy(alpha = currentGlowAlpha * 0.4f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 4f)
                )
            }

            // Phase 2: RIPPLE - Single expanding ring that fades as it travels
            // This is the "water droplet" effect - expands outward only, never returns
            if (currentRippleAlpha > 0f) {
                val expandedRadius = radius * currentRippleScale
                drawCircle(
                    color = bookmarkGlowColor.copy(alpha = currentRippleAlpha),
                    radius = expandedRadius,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
            }
        }

        // Center content - chapter count in compact format (e.g., "3/12")
        Text(
            text = "$currentChapter/$totalChapters",
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.2).sp
            ),
            color = secondaryTextColor
        )
    }
}

// ============================================================================
// BOTTOM CONTROL PILL
// ============================================================================

@Composable
private fun BottomControlPill(
    playbackSpeed: Float,
    sleepTimerMinutes: Int?,
    sleepTimerRemaining: Long = 0L,
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    highlightColor: Color = GlassColors.WarmSlate,
    onSpeedClick: () -> Unit,
    onSleepClick: () -> Unit,
    onChaptersClick: () -> Unit,
    onAudioClick: () -> Unit,
    onBookmarkTap: () -> Unit,
    onBookmarkLongPress: () -> Unit
) {
    // Use proper accent color from theme
    val accentColor = if (isReverieDark) reverieAccentColor else Color.White
    val pillBg = if (isReverieDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)
    val pillBorder = if (isReverieDark) reverieAccentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f)
    val dividerColor = if (isReverieDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(pillBg)
            .border(0.5.dp, pillBorder, RoundedCornerShape(26.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Bookmark - tap for quick, long-press for note (most used action)
        BookmarkPillButton(
            accentColor = accentColor,
            isReverieDark = isReverieDark,
            reverieAccentColor = reverieAccentColor,
            onTap = onBookmarkTap,
            onLongPress = onBookmarkLongPress
        )

        // Divider
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .height(20.dp)
                .background(dividerColor)
        )

        // 2. Speed (frequently changed)
        PillButton(
            text = "${playbackSpeed}x",
            isReverieDark = isReverieDark,
            reverieAccentColor = reverieAccentColor,
            highlightColor = highlightColor,
            onClick = onSpeedClick
        )

        // Divider
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .height(20.dp)
                .background(dividerColor)
        )

        // 3. Chapters (navigation - pairs with speed)
        PillButton(
            icon = Icons.AutoMirrored.Filled.List,
            isReverieDark = isReverieDark,
            reverieAccentColor = reverieAccentColor,
            highlightColor = highlightColor,
            onClick = onChaptersClick
        )

        // Divider
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .height(20.dp)
                .background(dividerColor)
        )

        // 4. Audio/EQ (settings category)
        PillButton(
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            isReverieDark = isReverieDark,
            reverieAccentColor = reverieAccentColor,
            highlightColor = highlightColor,
            onClick = onAudioClick
        )

        // Divider
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .height(20.dp)
                .background(dividerColor)
        )

        // 5. Sleep Timer (set once, at the end)
        PillButton(
            icon = Icons.Rounded.Bedtime,
            text = when {
                sleepTimerMinutes == null -> null
                sleepTimerMinutes == -1 -> "Ch" // End of chapter mode
                sleepTimerRemaining > 0 -> {
                    // Format remaining time
                    val totalSeconds = (sleepTimerRemaining / 1000).toInt()
                    val minutes = totalSeconds / 60
                    val seconds = totalSeconds % 60
                    if (minutes > 0) "${minutes}m" else "${seconds}s"
                }
                else -> "${sleepTimerMinutes}m"
            },
            isReverieDark = isReverieDark,
            reverieAccentColor = reverieAccentColor,
            highlightColor = highlightColor,
            onClick = onSleepClick,
            isActive = sleepTimerMinutes != null
        )
    }
}

@Composable
private fun BookmarkPillButton(
    accentColor: Color,
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "scale"
    )

    // Use theme-appropriate icon color
    val iconColor = if (isReverieDark) reverieAccentColor else accentColor

    Box(
        modifier = Modifier
            .scale(scale)
            .height(40.dp)
            .widthIn(min = 48.dp)
            .clip(RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.BookmarkAdd,
            contentDescription = stringResource(R.string.player_add_bookmark),
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun PillButton(
    icon: ImageVector? = null,
    text: String? = null,
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    highlightColor: Color = GlassColors.WarmSlate,
    onClick: () -> Unit,
    isActive: Boolean = false
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "scale"
    )

    // Theme-appropriate colors
    val activeColor = if (isReverieDark) reverieAccentColor else Color.White
    val inactiveColor = if (isReverieDark) GlassColors.ReverieTextSecondary else Color.White.copy(alpha = 0.7f)
    val activeBg = if (isReverieDark) highlightColor else Color.White.copy(alpha = 0.15f)

    Box(
        modifier = Modifier
            .scale(scale)
            .height(40.dp)
            .widthIn(min = 48.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isActive) Modifier.background(activeBg)
                else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) activeColor else inactiveColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (text != null) {
                Text(
                    text = text,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = if (isActive) activeColor else inactiveColor
                )
            }
        }
    }
}

// ============================================================================
// SPEED PRESET CHIPS
// ============================================================================

@Composable
private fun SpeedPresetChips(
    currentSpeed: Float,
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    highlightColor: Color = GlassColors.WarmSlate,
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
                isReverieDark = isReverieDark,
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
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    highlightColor: Color = GlassColors.WarmSlate,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "scale"
    )

    // Theme-appropriate colors
    val activeColor = if (isReverieDark) reverieAccentColor else Color.White
    val inactiveColor = if (isReverieDark) GlassColors.ReverieTextSecondary else Color.White.copy(alpha = 0.6f)
    val activeBg = if (isReverieDark) highlightColor else Color.White.copy(alpha = 0.15f)
    val inactiveBg = Color.White.copy(alpha = 0.05f)
    val borderColor = if (isSelected) {
        if (isReverieDark) reverieAccentColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
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

@Composable
private fun PremiumSpeedDialog(
    currentSpeed: Float,
    customPresets: List<com.mossglen.reverie.data.SettingsRepository.CustomSpeedPreset>,
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    highlightColor: Color = GlassColors.WarmSlate,
    onSpeedSelected: (Float) -> Unit,
    onSavePreset: () -> Unit,
    onDeletePreset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultPresets = com.mossglen.reverie.data.PlaybackSpeedPresets.defaults
    val allSpeeds = com.mossglen.reverie.data.PlaybackSpeedPresets.allSpeeds

    val dialogBg = Color(0xFF1C1C1E)
    val accentColor = if (isReverieDark) reverieAccentColor else Color.White
    val selectionBg = if (isReverieDark) highlightColor else Color.White.copy(alpha = 0.15f)
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.7f)

    // Swipe-down dismiss state
    var offsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 150f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .offset { IntOffset(0, offsetY.toInt()) }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        offsetY = (offsetY + delta).coerceAtLeast(0f)
                    },
                    onDragStopped = {
                        if (offsetY > dismissThreshold) {
                            onDismiss()
                        } else {
                            offsetY = 0f
                        }
                    }
                )
                .clip(RoundedCornerShape(20.dp))
                .background(dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.player_speed),
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Default Presets Section
                if (defaultPresets.isNotEmpty()) {
                    Text(
                        "Presets",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        defaultPresets.forEach { preset ->
                            val isSelected = preset.speed == currentSpeed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        if (isSelected) Modifier.background(selectionBg)
                                        else Modifier
                                    )
                                    .clickable { onSpeedSelected(preset.speed) }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        preset.name,
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) accentColor else textPrimary
                                    )
                                    Text(
                                        "${preset.speed}x",
                                        style = TextStyle(fontSize = 12.sp),
                                        color = textSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        null,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Custom Presets Section
                if (customPresets.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Custom Presets",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        customPresets.forEach { preset ->
                            val isSelected = preset.speed == currentSpeed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        if (isSelected) Modifier.background(selectionBg)
                                        else Modifier
                                    )
                                    .clickable { onSpeedSelected(preset.speed) }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        preset.name,
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) accentColor else textPrimary
                                    )
                                    Text(
                                        "${preset.speed}x",
                                        style = TextStyle(fontSize = 12.sp),
                                        color = textSecondary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            null,
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Delete",
                                        tint = textSecondary,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { onDeletePreset(preset.name) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // All Speeds Section
                Text(
                    "All Speeds",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    allSpeeds.forEach { speed ->
                        val isSelected = speed == currentSpeed
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (isSelected) Modifier.background(selectionBg)
                                    else Modifier
                                )
                                .clickable { onSpeedSelected(speed) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${speed}x",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (isSelected) accentColor else textSecondary
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Current as Preset Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(selectionBg)
                        .clickable { onSavePreset() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.speed_save_preset),
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.speed_save_preset),
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SavePresetDialog(
    initialName: String,
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var presetName by remember { mutableStateOf(initialName) }
    val dialogBg = Color(0xFF1C1C1E)
    val accentColor = if (isReverieDark) reverieAccentColor else Color.White
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
    chapters: List<com.mossglen.reverie.data.Chapter>,
    currentChapter: Int,
    hasRealChapters: Boolean,
    totalChapters: Int,
    chapterDuration: Long,
    duration: Long,
    bookmarks: List<Long>,
    isReverieDark: Boolean,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    highlightColor: Color = GlassColors.WarmSlate,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    // Use theme accent color
    val accentColor = if (isReverieDark) reverieAccentColor else Color(0xFF0A84FF)
    val selectionBg = if (isReverieDark) highlightColor else Color.White.copy(alpha = 0.15f)
    val dialogBg = if (isReverieDark) Color(0xFF0A0A0A) else Color(0xFF1C1C1E)
    val glassBg = if (isReverieDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.08f)
    val pagerState = rememberPagerState(pageCount = { 2 })
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
            dampingRatio = 0.85f,
            stiffness = 350f
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
                            } else if (bookmarks.isNotEmpty()) {
                                // No real chapters - show bookmarks instead
                                item {
                                    Text(
                                        "No chapters found. Showing bookmarks:",
                                        style = TextStyle(fontSize = 13.sp),
                                        color = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                val sortedBookmarks = bookmarks.sorted()
                                items(sortedBookmarks.size) { index ->
                                    val positionMs = sortedBookmarks[index]
                                    BookmarkRow(
                                        timestamp = formatTimestamp(positionMs),
                                        accentColor = accentColor,
                                        onClick = { onSeekTo(positionMs) }
                                    )
                                }
                            } else {
                                // No chapters and no bookmarks
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
                                    BookmarkRow(
                                        timestamp = formatTimestamp(positionMs),
                                        accentColor = accentColor,
                                        onClick = { onSeekTo(positionMs) }
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
    selectionBg: Color = GlassColors.WarmSlate,
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

@Composable
private fun BookmarkRow(
    timestamp: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Rounded.Bookmark,
                null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = timestamp,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White
                )
                Text(
                    text = "Tap to jump to this position",
                    style = TextStyle(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
        Icon(
            Icons.Rounded.ChevronRight,
            null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PremiumSleepDialog(
    sleepTimerMinutes: Int?,
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    onTimerSet: (Int) -> Unit,
    onEndOfChapter: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(5, 10, 15, 30, 45, 60)
    val dialogBg = Color(0xFF1C1C1E)
    val accentColor = if (isReverieDark) reverieAccentColor else Color.White

    // Custom time state
    var showCustomInput by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf("") }

    // Swipe-down dismiss state
    var offsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 150f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .offset { IntOffset(0, offsetY.toInt()) }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        offsetY = (offsetY + delta).coerceAtLeast(0f)
                    },
                    onDragStopped = {
                        if (offsetY > dismissThreshold) {
                            onDismiss()
                        } else {
                            offsetY = 0f
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

                // Title
                Text(
                    stringResource(R.string.player_sleep_timer),
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (sleepTimerMinutes != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE53935).copy(alpha = 0.2f))
                                .clickable { onCancelTimer() }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (sleepTimerMinutes == -1) {
                                    "Cancel Timer (End of Chapter)"
                                } else {
                                    "Cancel Timer (${sleepTimerMinutes}m left)"
                                },
                                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFE53935)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // End of chapter option (special)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.1f))
                            .clickable { onEndOfChapter() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MenuBook,
                            null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            stringResource(R.string.sleep_end_of_chapter),
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                            color = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    options.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onTimerSet(minutes) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Bedtime,
                                null,
                                tint = accentColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                stringResource(R.string.sleep_minutes, minutes),
                                style = TextStyle(fontSize = 15.sp),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Custom time option
                    if (showCustomInput) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.1f))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            BasicTextField(
                                value = customMinutes,
                                onValueChange = { customMinutes = it.filter { char -> char.isDigit() } },
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    color = Color.White
                                ),
                                cursorBrush = SolidColor(accentColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp),
                                decorationBox = { innerTextField ->
                                    if (customMinutes.isEmpty()) {
                                        Text(
                                            "Enter minutes...",
                                            style = TextStyle(fontSize = 15.sp),
                                            color = Color.White.copy(alpha = 0.4f)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    stringResource(R.string.dialog_cancel),
                                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable {
                                        showCustomInput = false
                                        customMinutes = ""
                                    }
                                )
                                Text(
                                    stringResource(R.string.dialog_confirm),
                                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                    color = accentColor,
                                    modifier = Modifier.clickable {
                                        val minutes = customMinutes.toIntOrNull()
                                        if (minutes != null && minutes > 0) {
                                            onTimerSet(minutes)
                                            showCustomInput = false
                                            customMinutes = ""
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showCustomInput = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                null,
                                tint = accentColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                stringResource(R.string.sleep_custom),
                                style = TextStyle(fontSize = 15.sp),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkNoteDialog(
    note: String,
    onNoteChange: (String) -> Unit,
    isReverieDark: Boolean,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = if (isReverieDark) reverieAccentColor else Color(0xFF0A84FF)
    val dialogBg = if (isReverieDark) Color(0xFF0A0A0A) else Color(0xFF1C1C1E)

    val textColor = if (isReverieDark) GlassColors.ReverieTextPrimary else Color.White

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.BookmarkAdd,
                    null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
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

@Composable
private fun AudioSettingsDialog(
    audioEffectManager: AudioEffectManager,
    isReverieDark: Boolean,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    onDismiss: () -> Unit
) {
    val accentColor = if (isReverieDark) reverieAccentColor else Color(0xFF0A84FF)
    val dialogBg = if (isReverieDark) Color(0xFF0A0A0A) else Color(0xFF1C1C1E)
    val glassBg = if (isReverieDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.05f)
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

    // Swipe-down dismiss state
    var offsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 150f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .offset { IntOffset(0, offsetY.toInt()) }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        offsetY = (offsetY + delta).coerceAtLeast(0f)
                    },
                    onDragStopped = {
                        if (offsetY > dismissThreshold) {
                            onDismiss()
                        } else {
                            offsetY = 0f
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

                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.VolumeUp,
                        null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        stringResource(R.string.settings_equalizer),
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        color = if (isReverieDark) GlassColors.ReverieTextPrimary else Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // EQ Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.settings_equalizer),
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                            color = Color.White
                        )
                        Text(
                            stringResource(R.string.settings_equalizer_desc),
                            style = TextStyle(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.5f)
                        )
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

                // Visual 10-Band EQ
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

                        // Preset chips with Reset
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val presets = listOf(
                                "Flat" to R.string.eq_flat,
                                "Bass" to R.string.eq_bass_boost,
                                "Vocal" to R.string.eq_narrator,
                                "Treble" to R.string.eq_treble_boost,
                                "Spoken" to R.string.eq_speech_clarity
                            )
                            presets.forEach { (preset, stringResId) ->
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
                                        stringResource(stringResId),
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
                        color = if (isReverieDark) GlassColors.ReverieTextPrimary else Color.White
                    )

                    // Convert amplifierGain (-12 to +12) to normalized value (0 to 1)
                    val volumeBoostNormalized = ((amplifierGain + 12f) / 24f).coerceIn(0f, 1f)

                    // Premium Circular Knob
                    Box(
                        modifier = Modifier
                            .size(100.dp)
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
                                color = if (isReverieDark) GlassColors.ReverieTextSecondary else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Text(
                        "Drag up/down to adjust",
                        style = TextStyle(fontSize = 11.sp),
                        color = if (isReverieDark) GlassColors.ReverieTextTertiary else Color.White.copy(alpha = 0.4f)
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
            }
        }
    }
}

// ============================================================================
// EDIT BOOK DIALOG - Metadata & Cover Art Editor
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBookDialog(
    book: com.mossglen.reverie.data.Book,
    isReverieDark: Boolean,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    onSave: (title: String, author: String, narrator: String, date: String) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = if (isReverieDark) reverieAccentColor else Color(0xFF0A84FF)
    val dialogBg = if (isReverieDark) Color(0xFF0A0A0A) else Color(0xFF1C1C1E)
    val textColor = if (isReverieDark) GlassColors.ReverieTextPrimary else Color.White
    val secondaryText = if (isReverieDark) GlassColors.ReverieTextSecondary else Color.White.copy(alpha = 0.6f)
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
    isReverieDark: Boolean,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    onMarkAs: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = if (isReverieDark) reverieAccentColor else Color(0xFF0A84FF)
    val dialogBg = if (isReverieDark) Color(0xFF0A0A0A) else Color(0xFF1C1C1E)
    val textColor = if (isReverieDark) GlassColors.ReverieTextPrimary else Color.White

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(20.dp),
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
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onMarkAs(status) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
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
    isReverieDark: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogBg = if (isReverieDark) Color(0xFF0A0A0A) else Color(0xFF1C1C1E)
    val textColor = if (isReverieDark) GlassColors.ReverieTextPrimary else Color.White
    val destructiveColor = GlassColors.Destructive

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = null,
                tint = destructiveColor,
                modifier = Modifier.size(32.dp)
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
