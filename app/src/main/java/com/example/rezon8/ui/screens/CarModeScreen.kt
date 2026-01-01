package com.mossglen.lithos.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mossglen.lithos.haptics.HapticType
import com.mossglen.lithos.haptics.performHaptic
import com.mossglen.lithos.ui.theme.*
import com.mossglen.lithos.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * REVERIE Car Mode - Premium Driving-Safe Interface
 *
 * Redesigned for:
 * - HUGE touch targets (100dp+) for safe driving
 * - Clean, minimal visual design
 * - High contrast OLED-optimized
 * - Proper back gesture handling
 * - Works in landscape and portrait
 *
 * Gestures:
 * - Swipe anywhere: Skip back/forward
 * - Back gesture (2x): Exit car mode
 */
@Composable
fun CarModeScreen(
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onExitCarMode: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Player state
    val currentBook by playerViewModel.currentBook.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val position by playerViewModel.position.collectAsState()
    val duration by playerViewModel.duration.collectAsState()

    // Back press handling - double-tap to exit
    var backPressedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(2000)
            backPressedOnce = false
        }
    }

    BackHandler {
        if (backPressedOnce) {
            onExitCarMode()
        } else {
            backPressedOnce = true
            view.performHaptic(HapticType.LightTap)
            Toast.makeText(context, "Swipe again to exit Car Mode", Toast.LENGTH_SHORT).show()
        }
    }

    // Gesture state
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 120f
    var showSkipIndicator by remember { mutableStateOf<Int?>(null) }

    // Clear skip indicator after delay
    LaunchedEffect(showSkipIndicator) {
        if (showSkipIndicator != null) {
            delay(500)
            showSkipIndicator = null
        }
    }

    // Progress
    val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f

    // Time formatting
    fun formatTimeShort(ms: Long): String {
        val totalMinutes = ms / 1000 / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            swipeOffset < -swipeThreshold -> {
                                view.performHaptic(HapticType.MediumTap)
                                playerViewModel.skipBack()
                                showSkipIndicator = -1
                            }
                            swipeOffset > swipeThreshold -> {
                                view.performHaptic(HapticType.MediumTap)
                                playerViewModel.skipForward()
                                showSkipIndicator = 1
                            }
                        }
                        swipeOffset = 0f
                    },
                    onDragCancel = { swipeOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset += dragAmount
                    }
                )
            }
    ) {
        // Subtle background artwork
        currentBook?.let { book ->
            AsyncImage(
                model = book.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.15f },
                contentScale = ContentScale.Crop
            )
        }

        // Dark vignette overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.95f)
                        ),
                        radius = 1200f
                    )
                )
        )

        // Exit button (top-right) - Larger touch target
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .clickable {
                    view.performHaptic(HapticType.LightTap)
                    onExitCarMode()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Exit Car Mode",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(28.dp)
            )
        }

        // "CAR MODE" label (top-left)
        Text(
            text = "CAR MODE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 2.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .statusBarsPadding()
        )

        // Main content
        if (isLandscape) {
            CarModeLandscape(
                currentBook = currentBook,
                isPlaying = isPlaying,
                progress = progress,
                timeRemaining = duration - position,
                formatTime = ::formatTimeShort,
                onPlayPause = {
                    view.performHaptic(HapticType.MediumTap)
                    playerViewModel.togglePlayback()
                },
                onSkipBack = {
                    view.performHaptic(HapticType.MediumTap)
                    playerViewModel.skipBack()
                    showSkipIndicator = -1
                },
                onSkipForward = {
                    view.performHaptic(HapticType.MediumTap)
                    playerViewModel.skipForward()
                    showSkipIndicator = 1
                }
            )
        } else {
            CarModePortrait(
                currentBook = currentBook,
                isPlaying = isPlaying,
                progress = progress,
                timeRemaining = duration - position,
                formatTime = ::formatTimeShort,
                onPlayPause = {
                    view.performHaptic(HapticType.MediumTap)
                    playerViewModel.togglePlayback()
                },
                onSkipBack = {
                    view.performHaptic(HapticType.MediumTap)
                    playerViewModel.skipBack()
                    showSkipIndicator = -1
                },
                onSkipForward = {
                    view.performHaptic(HapticType.MediumTap)
                    playerViewModel.skipForward()
                    showSkipIndicator = 1
                }
            )
        }

        // Skip indicator overlay (left or right side)
        showSkipIndicator?.let { direction ->
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = if (direction < 0) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 48.dp)
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay30,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = if (direction > 0) -1f else 1f
                            }
                    )
                }
            }
        }

        // Active swipe indicator
        if (abs(swipeOffset) > 40) {
            val isBack = swipeOffset < 0
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (isBack) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .graphicsLayer {
                            alpha = (abs(swipeOffset) / swipeThreshold).coerceAtMost(1f) * 0.8f
                            scaleX = 0.8f + (abs(swipeOffset) / swipeThreshold).coerceAtMost(1f) * 0.2f
                            scaleY = 0.8f + (abs(swipeOffset) / swipeThreshold).coerceAtMost(1f) * 0.2f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay30,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer {
                                scaleX = if (!isBack) -1f else 1f
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun CarModePortrait(
    currentBook: com.mossglen.lithos.data.Book?,
    isPlaying: Boolean,
    progress: Float,
    timeRemaining: Long,
    formatTime: (Long) -> String,
    onPlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Cover art with progress ring
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Progress ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 6.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2

                // Background ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )

                // Progress arc
                drawArc(
                    color = GlassColors.LithosAccent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Cover art
            currentBook?.let { book ->
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } ?: Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Book title
        Text(
            text = currentBook?.title ?: "No Book",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Time remaining
        Text(
            text = "${formatTime(timeRemaining)} left",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Controls - Three big buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Skip back
            CarButton(
                icon = Icons.Filled.Replay30,
                size = 88.dp,
                iconSize = 48.dp,
                backgroundColor = Color.White.copy(alpha = 0.1f),
                onClick = onSkipBack
            )

            // Play/Pause - HERO
            CarButton(
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                size = 120.dp,
                iconSize = 64.dp,
                backgroundColor = GlassColors.LithosAccent,
                onClick = onPlayPause
            )

            // Skip forward - Mirror the Replay icon for visual consistency
            CarButton(
                icon = Icons.Filled.Replay30,
                size = 88.dp,
                iconSize = 48.dp,
                backgroundColor = Color.White.copy(alpha = 0.1f),
                onClick = onSkipForward,
                mirrorHorizontally = true
            )
        }
    }
}

@Composable
private fun CarModeLandscape(
    currentBook: com.mossglen.lithos.data.Book?,
    isPlaying: Boolean,
    progress: Float,
    timeRemaining: Long,
    formatTime: (Long) -> String,
    onPlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Cover with progress
        Box(
            modifier = Modifier
                .weight(0.4f)
                .aspectRatio(1f, matchHeightConstraintsFirst = true),
            contentAlignment = Alignment.Center
        ) {
            // Progress ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 6.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2

                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )

                drawArc(
                    color = GlassColors.LithosAccent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Cover
            currentBook?.let { book ->
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier
                        .fillMaxSize(0.8f)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } ?: Box(
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Right: Info + Controls
        Column(
            modifier = Modifier.weight(0.6f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = currentBook?.title ?: "No Book",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Time remaining
            Text(
                text = "${formatTime(timeRemaining)} left",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarButton(
                    icon = Icons.Filled.Replay30,
                    size = 72.dp,
                    iconSize = 40.dp,
                    backgroundColor = Color.White.copy(alpha = 0.1f),
                    onClick = onSkipBack
                )

                CarButton(
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    size = 100.dp,
                    iconSize = 56.dp,
                    backgroundColor = GlassColors.LithosAccent,
                    onClick = onPlayPause
                )

                CarButton(
                    icon = Icons.Filled.Replay30,
                    size = 72.dp,
                    iconSize = 40.dp,
                    backgroundColor = Color.White.copy(alpha = 0.1f),
                    onClick = onSkipForward,
                    mirrorHorizontally = true
                )
            }
        }
    }
}

@Composable
private fun CarButton(
    icon: ImageVector,
    size: Dp,
    iconSize: Dp,
    backgroundColor: Color,
    onClick: () -> Unit,
    mirrorHorizontally: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = if (mirrorHorizontally) -1f else 1f
                }
        )
    }
}
