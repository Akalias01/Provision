package com.example.rezon8.ui.screens

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
import com.example.rezon8.R
import com.example.rezon8.data.Book
import com.example.rezon8.ui.theme.*
import com.example.rezon8.ui.viewmodel.PlayerViewModel
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
import com.example.rezon8.data.AudioEffectManager
import com.example.rezon8.ui.viewmodel.SettingsViewModel

/**
 * REZON8 Premium Player
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
    isRezonDark: Boolean = false,
    rezonAccentColor: Color = GlassColors.RezonAccent,
    highlightColor: Color = GlassColors.WarmSlate,
    useBorderHighlight: Boolean = false,
    dynamicColors: Boolean = true,
    onBack: () -> Unit
) {
    // Get AudioEffectManager for Quick EQ
    val audioEffectManager = settingsViewModel.audioEffectManager

    // Rezon Dark uses deep amber accent and muted colors
    val accentColor = if (isRezonDark) rezonAccentColor else Color.White
    // Selection highlight - use warm slate or border style based on preference
    val selectionBg = if (isRezonDark) highlightColor else Color.White.copy(alpha = 0.1f)
    val textColor = if (isRezonDark) GlassColors.RezonTextPrimary else Color.White
    val secondaryTextColor = if (isRezonDark) GlassColors.RezonTextSecondary else Color.White.copy(alpha = 0.7f)

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
    val backgroundColor = if (isRezonDark) Color(0xFF050505) else Color.Black

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
                        .blur(if (isRezonDark) 60.dp else 50.dp)
                        .graphicsLayer {
                            alpha = if (isRezonDark) 0.35f else 0.6f
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
                            append("— Shared from REZON8")
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
                isRezonDark = isRezonDark,
                rezonAccentColor = rezonAccentColor
            )

            // Album Art - THE HERO (large, fills available space)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)  // Fill available space for maximum cover size
                    .padding(horizontal = 32.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
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

            // CONTROLS SECTION - No background needed, gradient overlay handles darkness
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Space between cover art and title
                Spacer(modifier = Modifier.height(16.dp))

                    // Track Info - Compact, centered
                    currentBook?.let { book ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = book.title,
                                style = TextStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp,
                                    shadow = Shadow(Color.Black.copy(alpha = 0.3f), Offset(0f, 2f), 4f)
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = book.author,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.sp
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress Section - Premium slider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        // Chapter info row with book progress indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chapter $currentChapter of $totalChapters",
                                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                color = if (isRezonDark) GlassColors.RezonTextSecondary else Color.White.copy(alpha = 0.6f)
                            )

                            // Circular BOOK progress (overall progress through entire audiobook)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(36.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { progress },  // Book progress, not chapter
                                    modifier = Modifier.fillMaxSize(),
                                    color = if (isRezonDark) accentColor else Color.White,
                                    strokeWidth = 2.5.dp,
                                    trackColor = if (isRezonDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f),
                                    strokeCap = StrokeCap.Round
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",  // Book progress percentage
                                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = if (isRezonDark) GlassColors.RezonTextPrimary else Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Premium progress slider - tracks CHAPTER progress for easy chapter navigation
                        var isSeeking by remember { mutableStateOf(false) }

                        PremiumSlider(
                            value = chapterProgress,  // Chapter progress, not book
                            isRezonDark = isRezonDark,
                            rezonAccentColor = rezonAccentColor,
                            onValueChange = { newChapterProgress ->
                                if (!isSeeking) {
                                    isSeeking = true
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                // Seek within current chapter
                                val newPosition = chapterStartMs + (newChapterProgress * chapterDuration).toLong()
                                playerViewModel.seekTo(newPosition)
                            },
                            onValueChangeFinished = {
                                isSeeking = false
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            }
                        )

                        // Chapter time calculations
                        val chapterRemainingMs = (chapterDuration - chapterPosition).coerceAtLeast(0L)

                        // Book time remaining (Audible-style)
                        val bookRemainingMs = duration - position
                        val bookHoursLeft = bookRemainingMs / 1000 / 3600
                        val bookMinutesLeft = (bookRemainingMs / 1000 % 3600) / 60
                        val bookTimeLeftText = if (bookHoursLeft > 0) {
                            "${bookHoursLeft}h ${bookMinutesLeft}m left in book"
                        } else {
                            "${bookMinutesLeft}m left in book"
                        }

                        // Chapter times on left/right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Chapter elapsed time (left)
                            Text(
                                text = formatTime(chapterPosition),
                                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                color = if (isRezonDark) secondaryTextColor else Color.White.copy(alpha = 0.5f)
                            )
                            // Chapter remaining time (right)
                            Text(
                                text = "-${formatTime(chapterRemainingMs)}",
                                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                color = if (isRezonDark) secondaryTextColor else Color.White.copy(alpha = 0.5f)
                            )
                        }

                        // Book time remaining - centered below
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bookTimeLeftText,
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            color = if (isRezonDark) accentColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))


                    // Main Controls - Premium floating design
                    MainPlayerControls(
                        isPlaying = isPlaying,
                        isRezonDark = isRezonDark,
                        rezonAccentColor = rezonAccentColor,
                        onSkipBack = {
                            playerViewModel.skipBackward()
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        onPrevious = {
                            playerViewModel.skipBack()
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        onPlayPause = {
                            playerViewModel.togglePlayback()
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        onNext = {
                            playerViewModel.skipForward()
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        onSkipForward = {
                            playerViewModel.skipForward()
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bottom Control Pill - Glass floating
                    BottomControlPill(
                        playbackSpeed = playbackSpeed,
                        sleepTimerMinutes = sleepTimerMinutes,
                        sleepTimerRemaining = sleepTimerRemaining,
                        isRezonDark = isRezonDark,
                        rezonAccentColor = rezonAccentColor,
                        highlightColor = highlightColor,
                        onSpeedClick = { showSpeedPicker = true },
                        onSleepClick = { showSleepTimer = true },
                        onChaptersClick = { showChaptersDialog = true },
                        onAudioClick = { showAudioSettings = true },
                        onBookmarkTap = {
                            // Quick bookmark - instant with toast feedback
                            playerViewModel.toggleBookmark()
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

        // Dialogs
        if (showSpeedPicker) {
            PremiumSpeedDialog(
                currentSpeed = playbackSpeed,
                customPresets = customSpeedPresets,
                isRezonDark = isRezonDark,
                rezonAccentColor = rezonAccentColor,
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
                isRezonDark = isRezonDark,
                rezonAccentColor = rezonAccentColor,
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
                isRezonDark = isRezonDark,
                rezonAccentColor = rezonAccentColor,
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
                isRezonDark = isRezonDark,
                rezonAccentColor = rezonAccentColor,
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
                isRezonDark = isRezonDark,
                rezonAccentColor = rezonAccentColor,
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
                isRezonDark = isRezonDark,
                rezonAccentColor = rezonAccentColor,
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
    isRezonDark: Boolean = false,
    rezonAccentColor: Color = GlassColors.RezonAccent
) {
    val menuBg = if (isRezonDark) Color(0xFF0A0A0A) else Color(0xFF2C2C2E)
    val menuTextColor = if (isRezonDark) GlassColors.RezonTextPrimary else Color.White
    val menuIconColor = if (isRezonDark) rezonAccentColor else Color.White
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
                    color = if (isRezonDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.1f),
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
    isRezonDark: Boolean = false,
    rezonAccentColor: Color = GlassColors.RezonAccent,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "slider"
    )

    // Rezon Dark uses dynamic accent color
    val trackColor = if (isRezonDark) rezonAccentColor else Color.White
    val thumbColor = if (isRezonDark) rezonAccentColor else Color.White
    val inactiveColor = if (isRezonDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.2f)

    Slider(
        value = animatedValue,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = thumbColor,
            activeTrackColor = trackColor,
            inactiveTrackColor = inactiveColor
        ),
        thumb = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .shadow(8.dp, CircleShape)
                    .background(thumbColor, CircleShape)
            )
        },
        track = { sliderState ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(inactiveColor)
            ) {
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
    isRezonDark: Boolean = false,
    rezonAccentColor: Color = GlassColors.RezonAccent,
    onSkipBack: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSkipForward: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Skip back 10s
        ControlButton(
            icon = Icons.Rounded.Replay10,
            onClick = onSkipBack,
            size = 48.dp,
            iconSize = 26.dp
        )

        // Previous
        ControlButton(
            icon = Icons.Rounded.SkipPrevious,
            onClick = onPrevious,
            size = 52.dp,
            iconSize = 30.dp
        )

        // Play/Pause - Hero button
        PlayPauseButton(
            isPlaying = isPlaying,
            isRezonDark = isRezonDark,
            rezonAccentColor = rezonAccentColor,
            onClick = onPlayPause
        )

        // Next
        ControlButton(
            icon = Icons.Rounded.SkipNext,
            onClick = onNext,
            size = 52.dp,
            iconSize = 30.dp
        )

        // Skip forward 30s
        ControlButton(
            icon = Icons.Rounded.Forward30,
            onClick = onSkipForward,
            size = 48.dp,
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
    isRezonDark: Boolean = false,
    rezonAccentColor: Color = GlassColors.RezonAccent,
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
    val buttonColor = if (isRezonDark) Color(0xFF1C1C1E) else Color(0xFF2C2C2E)
    val iconColor = if (isRezonDark) rezonAccentColor else Color.White

    Box(
        modifier = Modifier
            .scale(scale)
            .size(80.dp)  // Slightly larger button
            .shadow(20.dp, CircleShape, spotColor = if (isRezonDark) rezonAccentColor.copy(alpha = 0.3f) else Color.Black)
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
// BOTTOM CONTROL PILL
// ============================================================================

@Composable
private fun BottomControlPill(
    playbackSpeed: Float,
    sleepTimerMinutes: Int?,
    sleepTimerRemaining: Long = 0L,
    isRezonDark: Boolean = false,
    rezonAccentColor: Color = GlassColors.RezonAccent,
    highlightColor: Color = GlassColors.WarmSlate,
    onSpeedClick: () -> Unit,
    onSleepClick: () -> Unit,
    onChaptersClick: () -> Unit,
    onAudioClick: () -> Unit,
    onBookmarkTap: () -> Unit,
    onBookmarkLongPress: () -> Unit
) {
    // Use proper accent color from theme
    val accentColor = if (isRezonDark) rezonAccentColor else Color.White
    val pillBg = if (isRezonDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)
    val pillBorder = if (isRezonDark) rezonAccentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f)
    val dividerColor = if (isRezonDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f)

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
            isRezonDark = isRezonDark,
            rezonAccentColor = rezonAccentColor,
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
            isRezonDark = isRezonDark,
            rezonAccentColor = rezonAccentColor,
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
            isRezonDark = isRezonDark,
            rezonAccentColor = rezonAccentColor,
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
            isRezonDark = isRezonDark,
            rezonAccentColor = rezonAccentColor,
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
            isRezonDark = isRezonDark,
            rezonAccentColor = rezonAccentColor,
            highlightColor = highlightColor,
            onClick = onSleepClick,
            isActive = sleepTimerMinutes != null
        )
    }
}

@Composable
private fun BookmarkPillButton(
    accentColor: Color,
    isRezonDark: Boolean = false,
    rezonAccentColor: Color = GlassColors.RezonAccent,
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
    val iconColor = if (isRezonDark) rezonAccentColor else accentColor

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
    isRezonDark: Boolean = false,
    rezonAccentColor: Color = GlassColors.RezonAccent,
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
    val activeColor = if (isRezonDark) rezonAccentColor else Color.White
    val inactiveColor = if (isRezonDark) GlassColors.RezonTextSecondary else Color.White.copy(alpha = 0.7f)
    val activeBg = if (isRezonDark) highlightColor else Color.White.copy(alpha = 0.15f)

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
    isRezonDark: Boolean = false,
    rezonAccentColor: Color = GlassColors.RezonAccent,
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
                isRezonDark = isRezonDark,
                rezonAccentColor = rezonAccentColor,
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
    isRezonDark: Boolean = false,
    rezonAccentColor: Color = GlassColors.RezonAccent,
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
    val activeColor = if (isRezonDark) rezonAccentColor else Color.White
    val inactiveColor = if (isRezonDark) GlassColors.RezonTextSecondary else Color.White.copy(alpha = 0.6f)
    val activeBg = if (isRezonDark) highlightColor else Color.White.copy(alpha = 0.15f)
    val inactiveBg = Color.White.copy(alpha = 0.05f)
    val borderColor = if (isSelected) {
        if (isRezonDark) rezonAccentColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
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
    customPresets: List<com.example.rezon8.data.SettingsRepository.CustomSpeedPreset>,
    isRezonDark: Boolean = false,
    rezonAccentColor: Color = GlassColors.RezonAccent,
    highlightColor: Color = GlassColors.WarmSlate,
    onSpeedSelected: (Float) -> Unit,
    onSavePreset: () -> Unit,
    onDeletePreset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultPresets = com.example.rezon8.data.PlaybackSpeedPresets.defaults
    val allSpeeds = com.example.rezon8.data.PlaybackSpeedPresets.allSpeeds

    val dialogBg = Color(0xFF1C1C1E)
    val accentColor = if (isRezonDark) rezonAccentColor else Color.White
    val selectionBg = if (isRezonDark) highlightColor else Color.White.copy(alpha = 0.15f)
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
    isRezonDark: Boolean = false,
    rezonAccentColor: Color = GlassColors.RezonAccent,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var presetName by remember { mutableStateOf(initialName) }
    val dialogBg = Color(0xFF1C1C1E)
    val accentColor = if (isRezonDark) rezonAccentColor else Color.White
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
    chapters: List<com.example.rezon8.data.Chapter>,
    currentChapter: Int,
    hasRealChapters: Boolean,
    totalChapters: Int,
    chapterDuration: Long,
    duration: Long,
    bookmarks: List<Long>,
    isRezonDark: Boolean,
    rezonAccentColor: Color = GlassColors.RezonAccent,
    highlightColor: Color = GlassColors.WarmSlate,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    // Use theme accent color
    val accentColor = if (isRezonDark) rezonAccentColor else Color(0xFF0A84FF)
    val selectionBg = if (isRezonDark) highlightColor else Color.White.copy(alpha = 0.15f)
    val dialogBg = if (isRezonDark) Color(0xFF0A0A0A) else Color(0xFF1C1C1E)
    val glassBg = if (isRezonDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.08f)
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
    isRezonDark: Boolean = false,
    rezonAccentColor: Color = GlassColors.RezonAccent,
    onTimerSet: (Int) -> Unit,
    onEndOfChapter: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(5, 10, 15, 30, 45, 60)
    val dialogBg = Color(0xFF1C1C1E)
    val accentColor = if (isRezonDark) rezonAccentColor else Color.White

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
    isRezonDark: Boolean,
    rezonAccentColor: Color = GlassColors.RezonAccent,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = if (isRezonDark) rezonAccentColor else Color(0xFF0A84FF)
    val dialogBg = if (isRezonDark) Color(0xFF0A0A0A) else Color(0xFF1C1C1E)

    val textColor = if (isRezonDark) GlassColors.RezonTextPrimary else Color.White

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
    isRezonDark: Boolean,
    rezonAccentColor: Color = GlassColors.RezonAccent,
    onDismiss: () -> Unit
) {
    val accentColor = if (isRezonDark) rezonAccentColor else Color(0xFF0A84FF)
    val dialogBg = if (isRezonDark) Color(0xFF0A0A0A) else Color(0xFF1C1C1E)
    val glassBg = if (isRezonDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.05f)
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
                        color = if (isRezonDark) GlassColors.RezonTextPrimary else Color.White
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
                        color = if (isRezonDark) GlassColors.RezonTextPrimary else Color.White
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
                                color = if (isRezonDark) GlassColors.RezonTextSecondary else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Text(
                        "Drag up/down to adjust",
                        style = TextStyle(fontSize = 11.sp),
                        color = if (isRezonDark) GlassColors.RezonTextTertiary else Color.White.copy(alpha = 0.4f)
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
    book: com.example.rezon8.data.Book,
    isRezonDark: Boolean,
    rezonAccentColor: Color = GlassColors.RezonAccent,
    onSave: (title: String, author: String, narrator: String, date: String) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = if (isRezonDark) rezonAccentColor else Color(0xFF0A84FF)
    val dialogBg = if (isRezonDark) Color(0xFF0A0A0A) else Color(0xFF1C1C1E)
    val textColor = if (isRezonDark) GlassColors.RezonTextPrimary else Color.White
    val secondaryText = if (isRezonDark) GlassColors.RezonTextSecondary else Color.White.copy(alpha = 0.6f)
    val context = LocalContext.current

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
                                // TODO: Download current cover
                                Toast.makeText(context, "Downloading cover...", Toast.LENGTH_SHORT).show()
                            }
                        )

                        // Web Search
                        CoverActionIcon(
                            icon = Icons.Rounded.ImageSearch,
                            accentColor = accentColor,
                            onClick = {
                                // TODO: Open in-app browser for image search
                                Toast.makeText(context, "Opening image search...", Toast.LENGTH_SHORT).show()
                            }
                        )

                        // Google Play Books Search - Book icon
                        CoverActionIcon(
                            icon = Icons.Rounded.LibraryBooks,
                            accentColor = accentColor,
                            onClick = {
                                // TODO: Open in-app browser for Play Books search
                                Toast.makeText(context, "Opening Play Books...", Toast.LENGTH_SHORT).show()
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
    isRezonDark: Boolean,
    rezonAccentColor: Color = GlassColors.RezonAccent,
    onMarkAs: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = if (isRezonDark) rezonAccentColor else Color(0xFF0A84FF)
    val dialogBg = if (isRezonDark) Color(0xFF0A0A0A) else Color(0xFF1C1C1E)
    val textColor = if (isRezonDark) GlassColors.RezonTextPrimary else Color.White

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
    isRezonDark: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogBg = if (isRezonDark) Color(0xFF0A0A0A) else Color(0xFF1C1C1E)
    val textColor = if (isRezonDark) GlassColors.RezonTextPrimary else Color.White
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
