// FIXED VERSION OF GlassMiniPlayer with iOS 26 Fluidity
// Replace the existing GlassMiniPlayer function (lines 913-1006) with this version

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
    modifier: Modifier = Modifier,
    onScroll: Boolean = false
) {
    // State for collapsed mode (circle when playing for 5s, pill when paused/tapped)
    var isCollapsed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val view = androidx.compose.ui.platform.LocalView.current

    // FIX 1: Store the timer job to cancel it when needed (survives recompositions)
    var timerJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // FIX 1: Use rememberCoroutineScope to survive recompositions
    // Auto-collapse after 5 seconds of playing - timer survives UI recompositions
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            // Cancel any existing timer
            timerJob?.cancel()

            // Reset to expanded when playback starts
            isCollapsed = false

            // Start new 5-second timer
            timerJob = scope.launch {
                kotlinx.coroutines.delay(5000)
                // FIX 4: Haptic feedback on shrink
                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                isCollapsed = true
            }
        } else {
            // Cancel timer and expand when paused
            timerJob?.cancel()
            timerJob = null
            isCollapsed = false
        }
    }

    // Shrink on scroll
    LaunchedEffect(onScroll) {
        if (onScroll) {
            timerJob?.cancel()
            isCollapsed = true
        }
    }

    // FIX 2 & 3: iOS 26 morph animation - scale overshoot and subtle rotation
    val morphScale by animateFloatAsState(
        targetValue = if (!isCollapsed) 1.05f else 1.0f,  // Scale to 1.05 then settle to 1.0
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "morph_scale"
    )

    val morphRotation by animateFloatAsState(
        targetValue = if (!isCollapsed) 3f else 0f,  // 3 degrees rotation
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "morph_rotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M)
            // FIX 4: Apply scale and rotation for premium "alive" feel
            .graphicsLayer {
                scaleX = morphScale
                scaleY = morphScale
                rotationZ = morphRotation
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Collapsed Circle Mode - positioned at bottom-left
        // FIX 2: iOS 26 spring physics: dampingRatio = 0.7f (bouncy), stiffness = 400f
        AnimatedVisibility(
            visible = isCollapsed,
            enter = scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
            ) + fadeIn(
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
            ),
            exit = scaleOut(
                targetScale = 0.95f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            ) + fadeOut(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            )
        ) {
            CollapsedMiniPlayerCircle(
                book = book,
                progress = progress,
                isPlaying = isPlaying,
                accentColor = rezonAccentColor,
                isDark = isDark,
                onExpand = {
                    // FIX 4: Haptic feedback on expand
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                    timerJob?.cancel()
                    timerJob = null
                    isCollapsed = false
                }
            )
        }

        // Expanded Pill Mode
        // FIX 2: iOS 26 spring physics: dampingRatio = 0.8f (smooth), stiffness = 300f
        // FIX 3: Added scaleIn/scaleOut for smooth organic morphing
        AnimatedVisibility(
            visible = !isCollapsed,
            enter = expandHorizontally(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                expandFrom = Alignment.Start
            ) + fadeIn(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            ),
            exit = shrinkHorizontally(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                shrinkTowards = Alignment.Start
            ) + fadeOut(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            ) + scaleOut(
                targetScale = 0.95f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            )
        ) {
            ExpandedMiniPlayerPill(
                book = book,
                isPlaying = isPlaying,
                progress = progress,
                playbackSpeed = playbackSpeed,
                isDark = isDark,
                isRezonDark = isRezonDark,
                rezonAccentColor = rezonAccentColor,
                onPlayPause = onPlayPause,
                onSpeedClick = onSpeedClick,
                onClick = onClick
            )
        }
    }
}
