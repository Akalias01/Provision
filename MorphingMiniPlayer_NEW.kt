// ============================================================================
// GLASS MINI PLAYER - iOS 26 FLUID MORPHING ANIMATION
// ============================================================================
// This is the NEW implementation with:
// - Single morphing composable (no AnimatedVisibility)
// - True shape morphing: Circle → Pill
// - Spring physics with overshoot
// - Rotation flourish
// - Content crossfade with stagger
// - Haptic feedback
// ============================================================================

@Composable
fun GlassMiniPlayer(
    book: Book,
    isPlaying: Boolean,
    progress: Float,
    playbackSpeed: Float = 1.0f,
    isDark: Boolean,
    isRezonDark: Boolean = false,
    rezonAccentColor: Color = GlassColors.RezonAccent,
    onPlayPause: () -> Unit,
    onSpeedClick: () -> Unit = {},
    onClick: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    onScroll: Boolean = false
) {
    // State for collapsed mode (circle when playing for 3s, pill when paused/tapped)
    var isCollapsed by remember { mutableStateOf(false) }
    var lastCollapseState by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var shrinkJob by remember { mutableStateOf<Job?>(null) }
    var timerResetKey by remember { mutableIntStateOf(0) }

    // Auto-collapse after 3 seconds of playing, with proper cancellation
    DisposableEffect(isPlaying, onScroll, timerResetKey) {
        shrinkJob?.cancel()

        if (onScroll) {
            // Immediately collapse on scroll
            isCollapsed = true
        } else if (isPlaying && !isCollapsed) {
            // Reset to expanded when playback starts and start 3-second timer
            isCollapsed = false
            shrinkJob = scope.launch {
                kotlinx.coroutines.delay(3000) // 3 seconds
                isCollapsed = true
            }
        } else if (!isPlaying) {
            // Auto-expand when paused
            isCollapsed = false
        }

        onDispose {
            shrinkJob?.cancel()
        }
    }

    // Reset timer on user interaction with expanded pill
    val onPillClick = {
        if (!isCollapsed) {
            timerResetKey++
        }
        onClick()
    }

    // Track state changes for haptics - iOS 26 style
    val view = androidx.compose.ui.platform.LocalView.current
    LaunchedEffect(isCollapsed) {
        if (lastCollapseState != isCollapsed) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            lastCollapseState = isCollapsed
        }
    }

    // ============================================================================
    // iOS 26 MORPHING ANIMATION SYSTEM
    // ============================================================================

    // Spring physics configurations
    val shrinkSpring = spring<Dp>(
        dampingRatio = 0.65f,  // Underdamped for bounce
        stiffness = 300f,
        visibilityThreshold = 0.1.dp
    )
    val expandSpring = spring<Dp>(
        dampingRatio = 0.75f,  // Smooth settle
        stiffness = 250f,
        visibilityThreshold = 0.1.dp
    )

    // Morphing dimensions: Circle (56dp x 56dp) → Pill (fillMaxWidth x 64dp)
    val density = LocalDensity.current
    val maxWidth = with(density) { 400.dp }  // Large value for fillMaxWidth effect

    val targetWidth = if (isCollapsed) 56.dp else maxWidth
    val targetHeight = if (isCollapsed) 56.dp else 64.dp
    val targetCornerRadius = if (isCollapsed) 28.dp else 32.dp

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = if (isCollapsed) shrinkSpring else expandSpring,
        label = "width"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = if (isCollapsed) shrinkSpring else expandSpring,
        label = "height"
    )
    val animatedCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = if (isCollapsed) shrinkSpring else expandSpring,
        label = "cornerRadius"
    )

    // Scale overshoot effect for "breathing" animation
    val transitionScale = remember { Animatable(1f) }
    LaunchedEffect(isCollapsed) {
        if (isCollapsed) {
            // Shrinking: 1.0 → 0.95 → 1.0 (circle breathes in)
            transitionScale.snapTo(1.0f)
            transitionScale.animateTo(
                0.95f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
            )
            transitionScale.animateTo(
                1.0f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
            )
        } else {
            // Expanding: 0.9 → 1.03 → 1.0 (pill pops out)
            transitionScale.snapTo(0.9f)
            transitionScale.animateTo(
                1.03f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 350f)
            )
            transitionScale.animateTo(
                1.0f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 250f)
            )
        }
    }

    // Rotation flourish (5° subtle rotation for organic feel)
    val rotationAnimation = remember { Animatable(0f) }
    LaunchedEffect(isCollapsed) {
        if (isCollapsed) {
            // Shrink: rotate -5° then spring back
            rotationAnimation.animateTo(
                -5f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
            )
            rotationAnimation.animateTo(
                0f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 250f)
            )
        } else {
            // Expand: rotate +5° then spring back
            rotationAnimation.animateTo(
                5f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
            )
            rotationAnimation.animateTo(
                0f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 250f)
            )
        }
    }

    // Content crossfade with 100ms stagger
    val contentDelay = 100 // ms
    val circleContentAlpha by animateFloatAsState(
        targetValue = if (isCollapsed) 1f else 0f,
        animationSpec = tween(
            durationMillis = 250,
            delayMillis = if (isCollapsed) contentDelay else 0
        ),
        label = "circleAlpha"
    )
    val pillContentAlpha by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else 1f,
        animationSpec = tween(
            durationMillis = 250,
            delayMillis = if (!isCollapsed) contentDelay else 0
        ),
        label = "pillAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M),
        contentAlignment = Alignment.CenterStart
    ) {
        // Single morphing composable - TRUE iOS 26 fluidity
        MorphingMiniPlayer(
            book = book,
            isPlaying = isPlaying,
            progress = progress,
            playbackSpeed = playbackSpeed,
            isDark = isDark,
            isRezonDark = isRezonDark,
            rezonAccentColor = rezonAccentColor,
            isCollapsed = isCollapsed,
            width = animatedWidth,
            height = animatedHeight,
            cornerRadius = animatedCornerRadius,
            scale = transitionScale.value,
            rotation = rotationAnimation.value,
            circleContentAlpha = circleContentAlpha,
            pillContentAlpha = pillContentAlpha,
            onPlayPause = onPlayPause,
            onSpeedClick = onSpeedClick,
            onClick = onPillClick,
            onDismiss = onDismiss,
            onExpand = {
                isCollapsed = false
                timerResetKey++ // Reset timer when expanding from collapsed state
            }
        )
    }
}

// ============================================================================
// MORPHING MINI PLAYER - Single composable that morphs shape
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MorphingMiniPlayer(
    book: Book,
    isPlaying: Boolean,
    progress: Float,
    playbackSpeed: Float,
    isDark: Boolean,
    isRezonDark: Boolean,
    rezonAccentColor: Color,
    isCollapsed: Boolean,
    width: Dp,
    height: Dp,
    cornerRadius: Dp,
    scale: Float,
    rotation: Float,
    circleContentAlpha: Float,
    pillContentAlpha: Float,
    onPlayPause: () -> Unit,
    onSpeedClick: () -> Unit,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onExpand: () -> Unit
) {
    val theme = glassTheme(isDark, isRezonDark)
    val view = androidx.compose.ui.platform.LocalView.current
    val density = LocalDensity.current

    // Pill interaction states
    var isLongPressing by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Press scale for circle
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && isCollapsed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "pressScale"
    )

    // Pill interaction feedback
    val pillScale by animateFloatAsState(
        targetValue = when {
            isLongPressing -> 0.96f
            isDragging -> 0.98f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "pillScale"
    )

    val pillAlpha by animateFloatAsState(
        targetValue = when {
            isLongPressing -> 0.7f
            isDragging -> 0.8f
            else -> 1f
        },
        animationSpec = tween(300),
        label = "pillAlpha"
    )

    // Long-press indicator
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isLongPressing) 1f else 0f,
        animationSpec = tween(200),
        label = "indicatorAlpha"
    )

    val pillInteractionSource = remember { MutableInteractionSource() }

    // Monitor long-press state
    LaunchedEffect(pillInteractionSource) {
        pillInteractionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    kotlinx.coroutines.delay(500)
                    isLongPressing = true
                }
                is PressInteraction.Release,
                is PressInteraction.Cancel -> {
                    isLongPressing = false
                }
            }
        }
    }

    // Pulsing glow when playing (circle only)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Glass styling
    val glassBackground = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.88f)
    } else {
        Color(0xFFF2F2F7).copy(alpha = 0.88f)
    }

    val glassBorder = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }

    val glassGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.08f else 0.4f),
            glassBackground
        ),
        startY = 0f,
        endY = with(density) { height.toPx() }
    )

    Box(
        modifier = Modifier
            .width(if (isCollapsed) width else width.coerceAtMost(400.dp))
            .height(height)
            .graphicsLayer {
                scaleX = scale * (if (isCollapsed) pressScale else pillScale)
                scaleY = scale * (if (isCollapsed) pressScale else pillScale)
                rotationZ = rotation
                this.alpha = if (isCollapsed) 1f else pillAlpha
                translationY = if (!isCollapsed) dragOffsetY else 0f
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (isCollapsed) Color.Transparent else glassGradient)
            .then(
                if (!isCollapsed) {
                    Modifier.border(
                        width = 0.5.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                glassBorder.copy(alpha = glassBorder.alpha * 1.5f),
                                glassBorder
                            )
                        ),
                        shape = RoundedCornerShape(cornerRadius)
                    )
                } else Modifier
            )
            .then(
                if (!isCollapsed) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isDragging = true
                            },
                            onDragEnd = {
                                val dragThreshold = with(density) { 100.dp.toPx() }
                                if (dragOffsetY > dragThreshold) {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                    onDismiss()
                                }
                                dragOffsetY = 0f
                                isDragging = false
                            },
                            onDragCancel = {
                                dragOffsetY = 0f
                                isDragging = false
                            },
                            onVerticalDrag = { _, dragAmount ->
                                dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                            }
                        )
                    }
                } else Modifier
            )
            .then(
                if (!isCollapsed) {
                    Modifier.combinedClickable(
                        interactionSource = pillInteractionSource,
                        indication = null,
                        onClick = { onClick() },
                        onLongClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            onDismiss()
                        },
                        onLongClickLabel = "Dismiss mini player",
                        onClickLabel = "Expand player"
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Circle content (cover art with progress ring)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = circleContentAlpha },
            contentAlignment = Alignment.Center
        ) {
            if (circleContentAlpha > 0.01f) {
                // Outer glow when playing
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(rezonAccentColor.copy(alpha = pulseAlpha * 0.3f * circleContentAlpha))
                    )
                }

                // Progress ring
                Canvas(modifier = Modifier.size(56.dp)) {
                    val strokeWidth = 3.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2

                    // Background ring
                    drawCircle(
                        color = (if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f))
                            .copy(alpha = (if (isDark) 0.15f else 0.1f) * circleContentAlpha),
                        radius = radius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    if (progress > 0f) {
                        drawArc(
                            color = rezonAccentColor.copy(alpha = circleContentAlpha),
                            startAngle = -90f,
                            sweepAngle = 360f * progress.coerceIn(0f, 1f),
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Cover art
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            onExpand()
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Pill content (full player controls)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = pillContentAlpha }
        ) {
            if (pillContentAlpha > 0.01f) {
                // Progress bar at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White.copy(alpha = 0.1f * pillContentAlpha))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        rezonAccentColor.copy(alpha = pillContentAlpha),
                                        rezonAccentColor.copy(alpha = 0.7f * pillContentAlpha)
                                    )
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cover art with progress ring
                    Box(
                        modifier = Modifier.size(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Progress ring
                        Canvas(modifier = Modifier.size(52.dp)) {
                            val strokeWidth = 2.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2

                            drawCircle(
                                color = (if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f))
                                    .copy(alpha = (if (isDark) 0.15f else 0.1f) * pillContentAlpha),
                                radius = radius,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            if (progress > 0f) {
                                drawArc(
                                    color = rezonAccentColor.copy(alpha = pillContentAlpha),
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                                    useCenter = false,
                                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }

                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .graphicsLayer {
                                    shadowElevation = 4f
                                    alpha = pillContentAlpha
                                },
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & Author
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = GlassTypography.Callout,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.textPrimary.copy(alpha = pillContentAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = book.author,
                            style = GlassTypography.Caption,
                            color = theme.textSecondary.copy(alpha = pillContentAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Speed indicator
                    if (playbackSpeed != 1.0f) {
                        MiniSpeedButton(
                            speed = playbackSpeed,
                            onClick = onSpeedClick,
                            isDark = isDark,
                            isRezonDark = isRezonDark,
                            rezonAccentColor = rezonAccentColor,
                            modifier = Modifier.graphicsLayer { alpha = pillContentAlpha }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Play/pause button
                    PremiumMiniPlayButton(
                        isPlaying = isPlaying,
                        onClick = onPlayPause,
                        isDark = isDark,
                        isRezonDark = isRezonDark,
                        rezonAccentColor = rezonAccentColor,
                        modifier = Modifier.graphicsLayer { alpha = pillContentAlpha }
                    )
                }

                // Long-press indicator
                if (indicatorAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = indicatorAlpha },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f * indicatorAlpha))
                        )

                        Icon(
                            imageVector = Icons.Filled.ArrowDownward,
                            contentDescription = "Swipe down to dismiss",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
