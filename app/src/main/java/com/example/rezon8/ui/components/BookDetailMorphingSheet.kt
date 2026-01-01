package com.mossglen.lithos.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mossglen.lithos.data.Book
import com.mossglen.lithos.data.getSeriesInfo
import com.mossglen.lithos.ui.theme.GlassColors
import com.mossglen.lithos.ui.theme.GlassTypography
import com.mossglen.lithos.ui.theme.glassTheme
import com.mossglen.lithos.ui.theme.LithosSlate
import com.mossglen.lithos.ui.theme.LithosBlack
import com.mossglen.lithos.ui.theme.LithosOat
import com.mossglen.lithos.ui.theme.LithosSurfaceDark
import com.mossglen.lithos.ui.theme.LithosSurfaceDarkElevated
import com.mossglen.lithos.ui.theme.LithosSurfaceLight
import com.mossglen.lithos.ui.theme.LithosSurfaceLightElevated
import com.mossglen.lithos.ui.theme.LithosError
import com.mossglen.lithos.ui.theme.LithosAmber
import kotlin.math.roundToInt
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.layout.layout

/**
 * Modifier that animates both alpha AND height based on visibility fraction.
 * This prevents layout jumps by smoothly collapsing/expanding content.
 * - visibility = 1f: fully visible, full height
 * - visibility = 0f: invisible and takes 0 height in layout
 */
private fun Modifier.animatedVisibility(visibility: Float): Modifier = this
    .graphicsLayer { alpha = visibility }
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val height = (placeable.height * visibility).roundToInt()
        layout(placeable.width, height) {
            // Place at top, content will be clipped by reduced height
            placeable.place(0, 0)
        }
    }

/**
 * BookDetailMorphingSheet - Redesigned per Interaction Design Laws
 *
 * HALF-SHEET LAYOUT (Following Primary Action Prominence):
 * ┌──────────────────────────────────────────────────────┐
 * │            [═══ drag handle ═══]                     │
 * │                                                      │
 * │  ┌──────────┐   Book Title Here                      │
 * │  │  COVER   │   Author Name                          │
 * │  │          │   8h 42m · 45% complete                │
 * │  │   [▶]    │   ← Play button ON cover               │
 * │  └──────────┘                                        │
 * │                                                      │
 * │  Synopsis preview that spans the FULL WIDTH...       │
 * │                                                      │
 * │  ┌────────────────────────────────────────────────┐  │
 * │  │     ▶  CONTINUE FROM 3:47:22                   │  │
 * │  └────────────────────────────────────────────────┘  │
 * └──────────────────────────────────────────────────────┘
 *
 * KEY PRINCIPLES:
 * - Play button is UNMISSABLE (on cover + large resume bar)
 * - Synopsis spans FULL WIDTH (not cramped beside cover)
 * - 2-tap maximum to play (tap book → tap play)
 * - Consistent behavior everywhere (Home + Library)
 */
@Composable
fun BookDetailMorphingSheet(
    book: Book,
    isVisible: Boolean,
    accentColor: Color,
    isDark: Boolean = true,
    isOLED: Boolean = false,
    onDismiss: () -> Unit,
    onPlayBook: () -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onSeriesClick: (String) -> Unit = {},
    onGenreClick: (String) -> Unit = {},
    onScrollDown: () -> Unit = {},  // Scroll down = hide pill
    onScrollUp: () -> Unit = {}     // Scroll up = show pill
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val theme = glassTheme(isDark, isOLED)

    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    val halfSheetHeight = screenHeight * 0.52f  // 52% for half sheet - more room for synopsis + resume bar
    val fullSheetHeight = screenHeight * 0.95f  // 95% for full sheet

    // Sheet offset - 0 = hidden, halfSheetHeight = half, fullSheetHeight = full
    var targetOffset by remember { mutableFloatStateOf(0f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Track scroll direction for pill auto-hide
    var lastScrollValue by remember { mutableStateOf(0) }
    LaunchedEffect(scrollState.value) {
        val scrollDelta = scrollState.value - lastScrollValue
        // Use threshold to avoid jitter
        if (kotlin.math.abs(scrollDelta) > 30) {
            if (scrollDelta > 0) {
                // Scrolling down (reading synopsis) - hide pill
                onScrollDown()
            } else {
                // Scrolling up (looking for controls) - show pill
                onScrollUp()
            }
        }
        lastScrollValue = scrollState.value
    }

    // Exit animation state for smooth play transition
    var isExiting by remember { mutableStateOf(false) }
    val exitAlpha by animateFloatAsState(
        targetValue = if (isExiting) 0f else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        ),
        label = "exitAlpha"
    )
    val exitScale by animateFloatAsState(
        targetValue = if (isExiting) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        ),
        label = "exitScale"
    )

    // Nested scroll connection to handle swipe-down from anywhere when at top
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && scrollState.value == 0) {
                    val consumed = available.y.coerceAtMost(targetOffset + dragOffset)
                    dragOffset -= consumed
                    return Offset(0f, consumed)
                }
                if (available.y < 0 && targetOffset < fullSheetHeight) {
                    val consumed = available.y.coerceAtLeast(-(fullSheetHeight - targetOffset - dragOffset))
                    dragOffset -= consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0 && scrollState.value == 0) {
                    dragOffset -= available.y
                    return available
                }
                return Offset.Zero
            }
        }
    }

    // Animate to target with spring physics
    val animatedOffset by animateFloatAsState(
        targetValue = targetOffset + dragOffset,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "sheetOffset"
    )

    // Calculate expansion progress using derivedStateOf to minimize recomposition
    // This only triggers recomposition when the RESULT changes significantly
    val expansionProgress by remember {
        derivedStateOf {
            ((animatedOffset - halfSheetHeight) / (fullSheetHeight - halfSheetHeight))
                .coerceIn(0f, 1f)
        }
    }

    // State for full-sheet features
    var isSynopsisExpanded by remember { mutableStateOf(true) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Show/hide animation
    LaunchedEffect(isVisible) {
        targetOffset = if (isVisible) halfSheetHeight else 0f
        dragOffset = 0f
        isExiting = false // Reset exit state when becoming visible
    }

    // Handle exit animation completion - call callbacks after animation finishes
    LaunchedEffect(isExiting) {
        if (isExiting) {
            // Wait for animation to complete (~300ms)
            kotlinx.coroutines.delay(300)
            onPlayBook()
            onDismiss()
        }
    }

    // Snap after finger lift (backup handler for when onDragEnd doesn't trigger properly)
    // Uses a small delay to ensure gesture detection completes first
    LaunchedEffect(isDragging, dragOffset) {
        if (!isDragging && dragOffset != 0f) {
            // Small delay to let onDragEnd handle it first
            kotlinx.coroutines.delay(50)
            // Only snap if dragOffset is still non-zero (onDragEnd didn't handle it)
            if (dragOffset != 0f) {
                val currentHeight = targetOffset + dragOffset
                targetOffset = when {
                    currentHeight < halfSheetHeight * 0.5f -> {
                        onDismiss()
                        0f
                    }
                    currentHeight < (halfSheetHeight + fullSheetHeight) / 2 -> halfSheetHeight
                    else -> fullSheetHeight
                }
                dragOffset = 0f
            }
        }
    }

    // Don't render if not visible
    if (animatedOffset < 10f && !isVisible) return

    // Scrim (darkens background as sheet expands)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // Apply exit animation alpha to scrim as well
                alpha = exitAlpha
            }
            .background(Color.Black.copy(alpha = (animatedOffset / fullSheetHeight) * 0.6f))
            .clickable(enabled = isVisible) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onDismiss()
            }
    )

    // Sheet - unified morphing component (no navigation, just morphs)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, (screenHeight - animatedOffset).roundToInt()) }
            .graphicsLayer {
                // Apply exit animation transforms
                alpha = exitAlpha
                scaleX = exitScale
                scaleY = exitScale
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        when {
                            !isDark -> LithosOat
                            isOLED -> LithosBlack
                            else -> LithosSlate
                        },
                        when {
                            !isDark -> LithosOat.copy(alpha = 0.95f)
                            isOLED -> LithosBlack
                            else -> LithosSlate.copy(alpha = 0.95f)
                        }
                    )
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        // Single haptic on snap - no haptics during drag
                        view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                        val currentHeight = targetOffset + dragOffset
                        targetOffset = when {
                            currentHeight < halfSheetHeight * 0.5f -> {
                                onDismiss()
                                0f
                            }
                            currentHeight < (halfSheetHeight + fullSheetHeight) / 2 -> halfSheetHeight
                            else -> fullSheetHeight
                        }
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                    onVerticalDrag = { _, delta ->
                        dragOffset = (dragOffset - delta).coerceIn(
                            -targetOffset,
                            fullSheetHeight - targetOffset
                        )
                    }
                )
            }
    ) {
        // Blurred background - Only show in dark mode for premium effect
        // In light mode, the solid background is cleaner
        if (book.coverUrl != null && isDark) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .graphicsLayer {
                        // Calculate INSIDE lambda to defer state read
                        alpha = ((expansionProgress - 0.3f) * 1.4f).coerceIn(0f, 1f)
                    },
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black
                            )
                        )
                    )
                    .graphicsLayer {
                        // Calculate INSIDE lambda
                        alpha = expansionProgress
                    }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        isDragging = true
                        waitForUpOrCancellation()
                        isDragging = false
                    }
                }
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(scrollState, enabled = targetOffset >= fullSheetHeight * 0.9f)
                .padding(horizontal = 20.dp)
        ) {
            // Top bar with drag handle and menu (menu fades in as sheet expands)
            // Tapping the drag handle area toggles between half-sheet and full-sheet
            // Using pointerInput instead of clickable to not interfere with drag gestures
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                // Toggle between half-sheet and full-sheet
                                targetOffset = if (targetOffset >= fullSheetHeight * 0.9f) halfSheetHeight else fullSheetHeight
                                dragOffset = 0f
                            }
                        )
                    }
            ) {
                // Drag handle (always visible, fades out when fully expanded)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer { alpha = 1f - expansionProgress }
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.2f))
                )

                // 3-dot menu - ALWAYS in layout, uses alpha to fade in (no conditional composition)
                // Positioned at far right edge with negative padding to extend to edge
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 12.dp) // Push further right toward edge
                        .graphicsLayer {
                            // Calculate alpha inside lambda to defer state read
                            val menuAlpha = ((expansionProgress - 0.3f) * 1.4f).coerceIn(0f, 1f)
                            alpha = menuAlpha
                        }
                ) {
                    IconButton(
                        onClick = {
                            // Only respond when visible enough
                            if (expansionProgress > 0.4f) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showOverflowMenu = true
                            }
                        }
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "More options",
                            tint = theme.textSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier
                            .background(
                                if (isDark) LithosSurfaceDark else LithosSurfaceLight,
                                RoundedCornerShape(12.dp)
                            )
                            .widthIn(min = 180.dp),
                        containerColor = if (isDark) LithosSurfaceDark else LithosSurfaceLight
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Details", color = theme.textPrimary) },
                            onClick = { showOverflowMenu = false },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = accentColor) }
                        )
                        DropdownMenuItem(
                            text = { Text("Change Cover", color = theme.textPrimary) },
                            onClick = { showOverflowMenu = false },
                            leadingIcon = { Icon(Icons.Rounded.Image, null, tint = accentColor) }
                        )
                        DropdownMenuItem(
                            text = { Text("Fetch Metadata", color = theme.textPrimary) },
                            onClick = { showOverflowMenu = false },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, null, tint = accentColor) }
                        )
                        HorizontalDivider(color = theme.divider)
                        DropdownMenuItem(
                            text = { Text("Delete Book", color = LithosError) },
                            onClick = { showOverflowMenu = false },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = LithosError) }
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // MORPHING CONTENT - Pure GPU animation, NO layout changes
            //
            // CRITICAL: All calculations that depend on expansionProgress must be
            // done INSIDE graphicsLayer lambdas to defer state reads and avoid
            // recomposition on every animation frame.
            // ═══════════════════════════════════════════════════════════════

            // FIXED constants - these never change, safe to calculate once
            val baseCoverSize = 110.dp
            val expandedCoverSize = 280.dp
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
            val paddingPx = with(density) { 20.dp.toPx() }
            val baseCoverSizePx = with(density) { baseCoverSize.toPx() }
            val expandedCoverSizePx = with(density) { expandedCoverSize.toPx() }
            val availableWidth = screenWidthPx - paddingPx * 2
            val scaleRatio = expandedCoverSizePx / baseCoverSizePx
            // Negative value moves cover UP when expanded for tighter spacing and more synopsis view
            val coverTranslationYMax = with(density) { (-12).dp.toPx() }

            // ═══════════════════════════════════════════════════════════════
            // HALF-SHEET ROW - Cover + Info side by side
            // Uses Box overlay so both are at same vertical position
            // Tapping this area (except play button) expands to full details
            // Using pointerInput to not interfere with drag gestures
            // ═══════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(baseCoverSize) // Fixed height for the row
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                // Only expand if in half-sheet mode
                                if (expansionProgress < 0.5f) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    targetOffset = fullSheetHeight
                                    dragOffset = 0f
                                }
                            }
                        )
                    }
            ) {
                // COVER - scales and moves to center when expanded
                Box(
                    modifier = Modifier
                        .size(baseCoverSize)
                        .graphicsLayer {
                            val progress = expansionProgress
                            val scale = 1f + (progress * (scaleRatio - 1f))
                            val scaledWidth = baseCoverSizePx * scale

                            scaleX = scale
                            scaleY = scale
                            translationX = ((availableWidth - scaledWidth) / 2) * progress
                            translationY = progress * coverTranslationYMax
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                        .shadow(16.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    if (book.coverUrl != null) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isDark) LithosSurfaceDarkElevated else LithosSurfaceLightElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Book, null, tint = theme.textSecondary, modifier = Modifier.size(40.dp))
                        }
                    }

                    // Play/Read button overlay - Only show for AUDIO books (ebooks have no cover button)
                    val isEbook = book.format in listOf("EPUB", "PDF", "TEXT", "DOCUMENT")
                    if (!isEbook) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .graphicsLayer {
                                    alpha = (1f - expansionProgress * 1.5f).coerceIn(0f, 1f)
                                }
                        ) {
                            CoverPlayButton(
                                accentColor = accentColor,
                                onClick = {
                                    if (expansionProgress < 0.5f) {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        // Trigger exit animation instead of immediate callback
                                        isExiting = true
                                    }
                                }
                            )
                        }
                    }
                }

                // HALF-SHEET INFO - positioned beside cover, fades out
                Column(
                    modifier = Modifier
                        .padding(start = baseCoverSize + 16.dp)
                        .align(Alignment.CenterStart)
                        .graphicsLayer {
                            alpha = (1f - expansionProgress * 1.5f).coerceIn(0f, 1f)
                        },
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = book.title,
                        style = GlassTypography.Title,
                        color = theme.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = book.author,
                        style = GlassTypography.Body,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            if (book.author.isNotBlank() && book.author != "Unknown Author" && expansionProgress < 0.5f) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onAuthorClick(book.author)
                            }
                        }
                    )

                    // Only show narrator for audiobooks
                    val isAudioFormat = book.format !in listOf("EPUB", "PDF", "TEXT", "DOCUMENT")
                    if (book.narrator.isNotBlank() && isAudioFormat) {
                        Text(
                            text = "Read by ${book.narrator}",
                            style = GlassTypography.Caption,
                            color = theme.textSecondary,
                            maxLines = 1
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatDuration(book.duration),
                            style = GlassTypography.Caption,
                            color = theme.textSecondary
                        )
                        if (book.progress > 0 && book.duration > 0) {
                            val pct = ((book.progress.toFloat() / book.duration) * 100).toInt()
                            Text("· $pct%", style = GlassTypography.Caption, color = accentColor.copy(alpha = 0.8f))
                        }
                    }

                    if (book.genre.isNotBlank()) {
                        Text(
                            text = book.genre,
                            style = GlassTypography.Caption,
                            color = accentColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            modifier = Modifier.clickable {
                                if (expansionProgress < 0.5f) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onGenreClick(book.genre)
                                }
                            }
                        )
                    }

                    val seriesInfo = book.getSeriesInfo()
                    if (seriesInfo != null) {
                        Text(
                            text = "${seriesInfo.name}${seriesInfo.bookNumber?.let { " #${it.toInt()}" } ?: ""}",
                            style = GlassTypography.Caption,
                            color = theme.textSecondary,
                            maxLines = 1,
                            modifier = Modifier.clickable {
                                if (expansionProgress < 0.5f) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onSeriesClick(seriesInfo.name)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════════════════
            // FULL-WIDTH SYNOPSIS (Half-sheet mode) - Below cover row
            // Calculation INSIDE lambda to defer state reads
            // ═══════════════════════════════════════════════════════════════
            if (book.synopsis.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = (1f - expansionProgress * 1.5f).coerceIn(0f, 1f)
                        }
                ) {
                    Text(
                        text = book.synopsis,
                        style = GlassTypography.Body.copy(lineHeight = 22.sp),
                        color = theme.textSecondary,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // PROGRESS INFO BAR (Half-sheet mode)
            // Calculation INSIDE lambda to defer state reads
            // ═══════════════════════════════════════════════════════════════
            if (book.progress > 0 && book.duration > 0) {
                val progressFraction = (book.progress.toFloat() / book.duration).coerceIn(0f, 1f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = (1f - expansionProgress * 1.5f).coerceIn(0f, 1f)
                        }
                ) {
                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .fillMaxHeight()
                                .background(accentColor)
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    val remainingMs = book.duration - book.progress
                    Text(
                        text = "${formatDuration(remainingMs)} remaining",
                        style = GlassTypography.Caption,
                        color = theme.textSecondary
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // FULL-SHEET CONTENT - Centered layout that fades in
            // All calculations INSIDE lambda to defer state reads
            // ═══════════════════════════════════════════════════════════════
            //
            // Layout math (updated for new Box layout):
            // - Cover row Box: 110dp
            // - Spacer: 16dp
            // - Synopsis and progress bar sections (variable, but fade out when expanded)
            // - When expanded, cover visually takes 280dp, content should be just below
            // - The half-sheet content sections (synopsis, progress) take ~100dp of layout space
            // - We use translationY to pull full-sheet content UP over the invisible half-sheet content
            val coverGrowthPx = with(density) { (expandedCoverSize - baseCoverSize).toPx() }
            val halfSheetContentHeightPx = with(density) { 120.dp.toPx() } // Approximate height of synopsis + progress

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val progress = expansionProgress
                        alpha = ((progress - 0.4f) * 2f).coerceIn(0f, 1f)
                        // Move down for cover growth, pull up more to position title closer to cover
                        translationY = (coverGrowthPx - halfSheetContentHeightPx) * progress
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Reduced top spacer to move title closer to cover
                Spacer(Modifier.height(4.dp))

                    // Title
                    Text(
                        text = book.title,
                        style = GlassTypography.Title.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp
                        ),
                        color = theme.textPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    // Author
                    Text(
                        text = book.author,
                        style = GlassTypography.Body.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = accentColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable {
                            if (book.author.isNotBlank() && book.author != "Unknown Author") {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onAuthorClick(book.author)
                            }
                        }
                    )

                    // Narrator - only for audiobooks
                    val isAudioBook = book.format !in listOf("EPUB", "PDF", "TEXT", "DOCUMENT")
                    if (book.narrator.isNotBlank() && isAudioBook) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Read by ${book.narrator}",
                            style = GlassTypography.Caption.copy(fontSize = 13.sp),
                            color = theme.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Info chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoChip(Icons.Rounded.Schedule, formatDuration(book.duration), accentColor, isDark)

                        val progressPercent = if (book.duration > 0)
                            ((book.progress.toFloat() / book.duration) * 100).toInt() else 0
                        if (progressPercent > 0) {
                            Spacer(Modifier.width(6.dp))
                            InfoChip(Icons.Rounded.TrendingUp, "$progressPercent%", accentColor, isDark)
                        }

                        // Genre chip (clickable)
                        if (book.genre.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            InfoChip(
                                icon = Icons.Rounded.Category,
                                text = book.genre,
                                accentColor = accentColor,
                                isDark = isDark,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onGenreClick(book.genre)
                                }
                            )
                        }

                        val seriesInfo = book.getSeriesInfo()
                        if (seriesInfo != null) {
                            Spacer(Modifier.width(6.dp))
                            InfoChip(
                                icon = Icons.Rounded.AutoStories,
                                text = if (seriesInfo.bookNumber != null) "#${seriesInfo.bookNumber.toInt()}" else "Series",
                                accentColor = accentColor,
                                isDark = isDark,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onSeriesClick(seriesInfo.name)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Play/Read button (full sheet mode) - positioned ABOVE synopsis
                    // Show book icon for ebooks, play icon for audiobooks
                    val isEbookFormat = book.format in listOf("EPUB", "PDF", "TEXT", "DOCUMENT")
                    ActionButton(
                        isResuming = book.progress > 0,
                        isEbook = isEbookFormat,
                        isDark = isDark,
                        isOLED = isOLED,
                        accentColor = accentColor,
                        size = 64.dp,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            // Trigger exit animation instead of immediate callback
                            isExiting = true
                        }
                    )

                    // Synopsis (Full sheet - below play button)
                    if (book.synopsis.isNotBlank()) {
                        Spacer(Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Synopsis",
                                style = GlassTypography.Label.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp
                                ),
                                color = theme.textSecondary
                            )

                            // Show Less / Read More toggle (only if synopsis is long)
                            if (book.synopsis.length > 200) {
                                Text(
                                    text = if (isSynopsisExpanded) "Show Less" else "Read More",
                                    style = GlassTypography.Caption,
                                    color = accentColor,
                                    modifier = Modifier.clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        isSynopsisExpanded = !isSynopsisExpanded
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = book.synopsis,
                            style = GlassTypography.Body.copy(lineHeight = 24.sp),
                            color = theme.textPrimary.copy(alpha = 0.85f),
                            maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(100.dp))
                }
        }
    }
}

/**
 * Play button that overlays on the cover art - CENTERED with TRANSLUCENT background
 * Matches the Recent Activity cards in Now view for consistency
 * Provides immediate access to play without finding a separate button
 */
@Composable
private fun CoverPlayButton(
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(LithosSlate.copy(alpha = 0.85f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = "Play",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Large resume bar - PRIMARY ACTION following Interaction Design Laws
 *
 * - Full width, large touch target (minimum 56dp height)
 * - Visually dominant - can't miss it
 * - Shows progress context ("Continue from X:XX")
 * - Includes progress bar visualization
 */
@Composable
private fun ResumeBar(
    book: Book,
    accentColor: Color,
    isOLED: Boolean,
    alpha: Float,
    onClick: () -> Unit
) {
    val hasProgress = book.progress > 0
    val progressFraction = if (book.duration > 0)
        (book.progress.toFloat() / book.duration).coerceIn(0f, 1f)
    else 0f
    val isEbook = book.format in listOf("EPUB", "PDF", "TEXT", "DOCUMENT")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isOLED) LithosSurfaceDark else LithosSurfaceDarkElevated
            )
            .clickable(onClick = onClick)
    ) {
        // Progress bar background
        if (hasProgress) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFraction)
                    .height(60.dp)
                    .background(accentColor.copy(alpha = 0.15f))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                // Use book icon for ebooks, play for audiobooks
                imageVector = if (isEbook) Icons.Rounded.MenuBook else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = when {
                    hasProgress && isEbook -> "Continue Reading"
                    hasProgress -> "Continue from ${formatDurationFull(book.progress)}"
                    isEbook -> "Start Reading"
                    else -> "Start Listening"
                },
                style = GlassTypography.Body.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = Color.White
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    accentColor: Color,
    isDark: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick)
            else Modifier
        ),
        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = accentColor.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                style = GlassTypography.Caption,
                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ActionButton(
    isResuming: Boolean,
    isEbook: Boolean,
    isDark: Boolean,
    isOLED: Boolean,
    accentColor: Color,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val buttonBg = when {
        !isDark -> LithosSurfaceLightElevated
        isOLED -> LithosSurfaceDark
        else -> LithosSurfaceDarkElevated
    }

    Box(
        modifier = Modifier
            .shadow(16.dp, CircleShape, spotColor = accentColor.copy(alpha = 0.3f))
            .size(size)
            .clip(CircleShape)
            .background(buttonBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            // Use book icon for ebooks, play icon for audiobooks
            imageVector = if (isEbook) Icons.Rounded.MenuBook else Icons.Rounded.PlayArrow,
            contentDescription = when {
                isEbook && isResuming -> "Continue Reading"
                isEbook -> "Start Reading"
                isResuming -> "Resume"
                else -> "Play"
            },
            tint = accentColor,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatDurationFull(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
