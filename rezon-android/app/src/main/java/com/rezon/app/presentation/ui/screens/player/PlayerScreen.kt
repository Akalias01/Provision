package com.rezon.app.presentation.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rezon.app.presentation.ui.theme.AccentPink
import com.rezon.app.presentation.ui.theme.NeonGlow
import com.rezon.app.presentation.ui.theme.PlayerGradientEnd
import com.rezon.app.presentation.ui.theme.PlayerGradientStart
import com.rezon.app.presentation.ui.theme.PrimaryDark
import com.rezon.app.presentation.ui.theme.ProgressFill
import com.rezon.app.presentation.ui.theme.ProgressTrack
import com.rezon.app.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * REZON Player Screen
 *
 * Full-featured audiobook player with gesture controls:
 * - Double tap center: Play/Pause with ripple
 * - Double tap left: Skip backward (-10s)
 * - Double tap right: Skip forward (+30s)
 * - Left edge vertical swipe: Brightness
 * - Right edge vertical swipe: Volume
 * - Horizontal swipe: Timeline seek
 */
@Composable
fun PlayerScreen(
    bookId: String,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val scope = rememberCoroutineScope()

    // Gesture state
    var showSkipOverlay by remember { mutableStateOf<SkipOverlay?>(null) }
    var showPlayPauseRipple by remember { mutableStateOf(false) }
    var brightnessLevel by remember { mutableFloatStateOf(0.5f) }
    var volumeLevel by remember { mutableFloatStateOf(0.7f) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Animation for play/pause ripple
    val rippleScale = remember { Animatable(0f) }

    // Edge zone width for brightness/volume gestures (40dp from each edge)
    val edgeZoneWidth = with(density) { 40.dp.toPx() }

    // Load book data
    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

    // System bar insets
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PlayerGradientStart, PlayerGradientEnd)
                )
            )
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                // Gesture detection
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val width = containerSize.width.toFloat()
                        val leftZone = width * 0.33f
                        val rightZone = width * 0.67f

                        when {
                            // Double tap left zone - Skip backward
                            offset.x < leftZone -> {
                                viewModel.skipBackward()
                                showSkipOverlay = SkipOverlay.BACKWARD
                                scope.launch {
                                    delay(800)
                                    showSkipOverlay = null
                                }
                            }
                            // Double tap right zone - Skip forward
                            offset.x > rightZone -> {
                                viewModel.skipForward()
                                showSkipOverlay = SkipOverlay.FORWARD
                                scope.launch {
                                    delay(800)
                                    showSkipOverlay = null
                                }
                            }
                            // Double tap center - Play/Pause
                            else -> {
                                viewModel.togglePlayPause()
                                showPlayPauseRipple = true
                                scope.launch {
                                    rippleScale.snapTo(0f)
                                    rippleScale.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                    delay(300)
                                    showPlayPauseRipple = false
                                }
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                // Edge swipe gestures for brightness/volume
                detectDragGestures(
                    onDragStart = { offset ->
                        when {
                            offset.x < edgeZoneWidth -> showBrightnessIndicator = true
                            offset.x > containerSize.width - edgeZoneWidth -> showVolumeIndicator = true
                        }
                    },
                    onDragEnd = {
                        showBrightnessIndicator = false
                        showVolumeIndicator = false
                    },
                    onDragCancel = {
                        showBrightnessIndicator = false
                        showVolumeIndicator = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val changeAmount = -dragAmount.y / containerSize.height * 2

                        when {
                            // Left edge - Brightness control
                            showBrightnessIndicator -> {
                                brightnessLevel = (brightnessLevel + changeAmount).coerceIn(0f, 1f)
                                viewModel.setBrightness(brightnessLevel)
                            }
                            // Right edge - Volume control
                            showVolumeIndicator -> {
                                volumeLevel = (volumeLevel + changeAmount).coerceIn(0f, 1f)
                                viewModel.setVolume(volumeLevel)
                            }
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding.calculateTopPadding())
                .padding(bottom = navBarPadding.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with back button and menu
            PlayerHeader(
                title = uiState.book?.title ?: "Unknown",
                author = uiState.book?.author ?: "Unknown Author",
                onBackClick = onNavigateBack,
                onChaptersClick = { /* TODO */ },
                onMenuClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Album Art with neon glow
            Box(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .aspectRatio(1f)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = PrimaryDark.copy(alpha = 0.5f),
                        spotColor = PrimaryDark.copy(alpha = 0.5f)
                    )
            ) {
                AsyncImage(
                    model = uiState.book?.coverUrl ?: uiState.book?.localCoverPath,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // Play/Pause ripple overlay
                AnimatedVisibility(
                    visible = showPlayPauseRipple,
                    enter = scaleIn(initialScale = 0.5f) + fadeIn(),
                    exit = scaleOut(targetScale = 1.5f) + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Book Progress Bar
            BookProgressSection(
                currentPosition = uiState.currentPosition,
                duration = uiState.duration,
                currentChapterIndex = uiState.currentChapterIndex,
                totalChapters = uiState.book?.chapters?.size ?: 0
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chapter name
            Text(
                text = uiState.currentChapter?.title ?: "Chapter ${uiState.currentChapterIndex + 1}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chapter Progress Slider
            ChapterProgressSlider(
                currentPosition = uiState.currentPosition,
                chapterStart = uiState.currentChapter?.startTime ?: 0L,
                chapterEnd = uiState.currentChapter?.endTime ?: uiState.duration,
                onSeek = { position -> viewModel.seekTo(position) }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Playback Controls
            PlaybackControls(
                isPlaying = uiState.isPlaying,
                playbackSpeed = uiState.playbackSpeed,
                onPlayPause = { viewModel.togglePlayPause() },
                onSkipBackward = { viewModel.skipBackward() },
                onSkipForward = { viewModel.skipForward() },
                onPreviousChapter = { viewModel.previousChapter() },
                onNextChapter = { viewModel.nextChapter() },
                onSpeedChange = { viewModel.cyclePlaybackSpeed() },
                onSleepTimer = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Skip Overlay Animations
        SkipOverlayDisplay(
            skipOverlay = showSkipOverlay,
            modifier = Modifier.align(Alignment.Center)
        )

        // Brightness Indicator (left side)
        VerticalIndicator(
            visible = showBrightnessIndicator,
            value = brightnessLevel,
            label = "Brightness",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        )

        // Volume Indicator (right side)
        VerticalIndicator(
            visible = showVolumeIndicator,
            value = volumeLevel,
            label = "Volume",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )
    }
}

/**
 * Skip overlay types
 */
enum class SkipOverlay {
    FORWARD, BACKWARD
}

/**
 * Skip overlay animation display
 */
@Composable
private fun SkipOverlayDisplay(
    skipOverlay: SkipOverlay?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = skipOverlay != null,
        enter = scaleIn(initialScale = 0.8f) + fadeIn(),
        exit = scaleOut(targetScale = 1.2f) + fadeOut(),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(50)
                )
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            when (skipOverlay) {
                SkipOverlay.BACKWARD -> {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "-10s",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                SkipOverlay.FORWARD -> {
                    Text(
                        text = "+30s",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Forward30,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                null -> {}
            }
        }
    }
}

/**
 * Vertical indicator for brightness/volume
 */
@Composable
private fun VerticalIndicator(
    visible: Boolean,
    value: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(150.dp)
                    .background(ProgressTrack, RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((150 * value).dp)
                        .align(Alignment.BottomCenter)
                        .background(ProgressFill, RoundedCornerShape(4.dp))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

/**
 * Player header with title, author, and navigation
 */
@Composable
private fun PlayerHeader(
    title: String,
    author: String,
    onBackClick: () -> Unit,
    onChaptersClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onChaptersClick) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "Chapters",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }

        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Book progress section showing elapsed/remaining time and chapter indicator
 */
@Composable
private fun BookProgressSection(
    currentPosition: Long,
    duration: Long,
    currentChapterIndex: Int,
    totalChapters: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTime(currentPosition),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Chapter ${currentChapterIndex + 1}/$totalChapters",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryDark
        )

        Text(
            text = "-${formatTime(duration - currentPosition)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Chapter progress slider
 */
@Composable
private fun ChapterProgressSlider(
    currentPosition: Long,
    chapterStart: Long,
    chapterEnd: Long,
    onSeek: (Long) -> Unit
) {
    val chapterDuration = chapterEnd - chapterStart
    val chapterProgress = if (chapterDuration > 0) {
        ((currentPosition - chapterStart).toFloat() / chapterDuration).coerceIn(0f, 1f)
    } else 0f

    var sliderPosition by remember { mutableFloatStateOf(chapterProgress) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(chapterProgress, isDragging) {
        if (!isDragging) {
            sliderPosition = chapterProgress
        }
    }

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Slider(
            value = sliderPosition,
            onValueChange = { value ->
                isDragging = true
                sliderPosition = value
            },
            onValueChangeFinished = {
                isDragging = false
                val newPosition = chapterStart + (sliderPosition * chapterDuration).toLong()
                onSeek(newPosition)
            },
            colors = SliderDefaults.colors(
                thumbColor = ProgressFill,
                activeTrackColor = ProgressFill,
                inactiveTrackColor = ProgressTrack
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Chapter time indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition - chapterStart),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(chapterDuration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Playback controls row
 */
@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onSpeedChange: () -> Unit,
    onSleepTimer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Playback speed
        IconButton(onClick = onSpeedChange) {
            Text(
                text = "${playbackSpeed}x",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Skip backward 10s
        IconButton(
            onClick = onSkipBackward,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Replay10,
                contentDescription = "Skip Backward",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(36.dp)
            )
        }

        // Play/Pause button
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = PrimaryDark.copy(alpha = 0.5f),
                    spotColor = PrimaryDark.copy(alpha = 0.5f)
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(PrimaryDark, AccentPink)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Skip forward 30s
        IconButton(
            onClick = onSkipForward,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Forward30,
                contentDescription = "Skip Forward",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(36.dp)
            )
        }

        // Sleep timer
        IconButton(onClick = onSleepTimer) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Sleep Timer",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Format milliseconds to time string (HH:MM:SS or MM:SS)
 */
private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
