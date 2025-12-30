package com.mossglen.reverie.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import android.widget.Toast
import com.mossglen.reverie.R
import com.mossglen.reverie.ui.theme.GlassColors
import com.mossglen.reverie.ui.theme.GlassTypography
import com.mossglen.reverie.ui.theme.glassTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mossglen.reverie.data.Book
import com.mossglen.reverie.data.getSeriesInfo
import com.mossglen.reverie.ui.viewmodel.CoverArtViewModel
import com.mossglen.reverie.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * BookDetailScreen - iOS 26 / Apple Books Quality
 *
 * Design principles:
 * - Large cover art with subtle shadow
 * - Dark play button with accent icon (matches player)
 * - Icons.Rounded throughout (consistent with player)
 * - Minimal space before synopsis
 * - Physics-based spring animations
 * - Swipe to dismiss with scale/corner animation
 */
@Composable
fun BookDetailScreen(
    bookId: String,
    accentColor: Color,
    isReverieDark: Boolean = false,
    onBack: () -> Unit,
    onPlayBook: (Book) -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onSeriesClick: (String) -> Unit = {},
    onSplitBook: (String) -> Unit = {},
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    coverArtViewModel: CoverArtViewModel = hiltViewModel()
) {
    val view = LocalView.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val books by libraryViewModel.books.collectAsState()
    val book = books.find { it.id == bookId }

    val theme = glassTheme(isReverieDark)
    val scope = rememberCoroutineScope()

    // State
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showMarkAsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCoverPicker by remember { mutableStateOf(false) }
    var isFetchingMetadata by remember { mutableStateOf(false) }
    var isSynopsisExpanded by remember { mutableStateOf(true) }  // Start expanded

    // Menu colors
    val menuBg = Color(0xFF1C1C1E)
    val menuTextColor = Color.White
    val destructiveColor = GlassColors.Destructive

    // ═══════════════════════════════════════════════════════════════════════════
    // ENTRY/EXIT ANIMATION - iOS 26 style slide up + scale
    // ═══════════════════════════════════════════════════════════════════════════
    var hasEntered by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    LaunchedEffect(Unit) {
        hasEntered = true
    }

    // Animated exit - slide down before navigating
    val performExit: () -> Unit = {
        if (!isExiting) {
            isExiting = true
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
        }
    }

    // When exit animation completes, actually navigate back
    LaunchedEffect(isExiting) {
        if (isExiting) {
            kotlinx.coroutines.delay(250) // Wait for slide animation
            onBack()
        }
    }

    // Slide up from bottom on entry, slide down on exit
    val entryOffsetY by animateFloatAsState(
        targetValue = when {
            isExiting -> screenHeightPx * 0.3f  // Slide down on exit
            hasEntered -> 0f                     // At position after entry
            else -> screenHeightPx * 0.15f       // Start slightly below
        },
        animationSpec = spring(
            dampingRatio = 0.75f,  // Slightly bouncy
            stiffness = if (isExiting) 400f else 300f
        ),
        label = "entryOffsetY"
    )

    val entryScale by animateFloatAsState(
        targetValue = when {
            isExiting -> 0.92f   // Shrink slightly on exit
            hasEntered -> 1f     // Full size after entry
            else -> 0.95f        // Start slightly scaled
        },
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = if (isExiting) 400f else 300f
        ),
        label = "entryScale"
    )

    val entryAlpha by animateFloatAsState(
        targetValue = when {
            isExiting -> 0f      // Fade out on exit
            hasEntered -> 1f     // Full opacity after entry
            else -> 0f           // Start transparent
        },
        animationSpec = spring(
            dampingRatio = 1f,   // No bounce for alpha
            stiffness = if (isExiting) 500f else 400f
        ),
        label = "entryAlpha"
    )

    val entryCornerRadius by animateFloatAsState(
        targetValue = when {
            isExiting -> 24f     // Round corners on exit
            hasEntered -> 0f     // Sharp corners after entry
            else -> 24f          // Start with rounded corners
        },
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = if (isExiting) 400f else 300f
        ),
        label = "entryCornerRadius"
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // SWIPE TO DISMISS - iOS-style with scale, corner radius, and opacity
    // ═══════════════════════════════════════════════════════════════════════════
    var swipeOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = screenHeightPx * 0.2f

    val animatedSwipeY by animateFloatAsState(
        targetValue = swipeOffsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "swipeOffsetY"
    )

    // Progressive animation as user drags
    val dragProgress = (abs(animatedSwipeY) / dismissThreshold).coerceIn(0f, 1f)
    val dismissScale = 1f - (dragProgress * 0.1f)  // Scale down to 0.9
    val dismissCornerRadius = 24f * dragProgress  // Corners round as you drag
    val dismissAlpha = 1f - (dragProgress * 0.5f)

    // Combined animation values (entry + swipe dismiss)
    val finalOffsetY = entryOffsetY + animatedSwipeY
    val finalScale = (entryScale * dismissScale).coerceIn(0.5f, 1f)
    val finalAlpha = (entryAlpha * dismissAlpha).coerceIn(0f, 1f)
    val finalCornerRadius = (entryCornerRadius + dismissCornerRadius).coerceAtLeast(0f).dp

    BackHandler { performExit() }

    if (book == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.book_not_found),
                style = GlassTypography.Body,
                color = theme.textSecondary
            )
        }
        return
    }

    val progressPercent = if (book.duration > 0) ((book.progress.toFloat() / book.duration) * 100).toInt() else 0
    val seriesInfo = book.getSeriesInfo()

    // Full-screen container with black background for dismiss animation
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main content with entry animation + swipe-to-dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, finalOffsetY.roundToInt()) }
                .graphicsLayer {
                    scaleX = finalScale
                    scaleY = finalScale
                    alpha = finalAlpha
                }
                .clip(RoundedCornerShape(finalCornerRadius))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (swipeOffsetY > dismissThreshold) {
                                // Dismiss with animation
                                performExit()
                            } else {
                                // Snap back with spring
                                swipeOffsetY = 0f
                            }
                        },
                        onDragCancel = { swipeOffsetY = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Only allow downward drag
                            if (dragAmount.y > 0 || swipeOffsetY > 0) {
                                swipeOffsetY = (swipeOffsetY + dragAmount.y).coerceAtLeast(0f)
                            }
                        }
                    )
                }
        ) {
            // ═══════════════════════════════════════════════════════════════════
            // BACKGROUND - Blurred cover art with gradient overlay
            // ═══════════════════════════════════════════════════════════════════
            Box(modifier = Modifier.fillMaxSize()) {
                if (book.coverUrl != null) {
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(80.dp, BlurredEdgeTreatment.Unbounded)
                            .alpha(0.6f),
                        contentScale = ContentScale.Crop
                    )
                }

                // Gradient overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black
                                )
                            )
                        )
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // ═══════════════════════════════════════════════════════════════
                // TOP BAR - Minimal, iOS-style
                // ═══════════════════════════════════════════════════════════════
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dismiss indicator - subtle chevron down
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        performExit()
                    }) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Menu button
                    Box {
                        IconButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showOverflowMenu = true
                        }) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = "More",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier
                                .background(menuBg, RoundedCornerShape(12.dp))
                                .widthIn(min = 200.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_edit), color = menuTextColor) },
                                onClick = { showOverflowMenu = false; showEditDialog = true },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = accentColor) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_cover), color = menuTextColor) },
                                onClick = { showOverflowMenu = false; showCoverPicker = true },
                                leadingIcon = { Icon(Icons.Rounded.Image, null, tint = accentColor) }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isFetchingMetadata) stringResource(R.string.fetching)
                                        else stringResource(R.string.menu_fetch_metadata),
                                        color = menuTextColor
                                    )
                                },
                                onClick = {
                                    if (!isFetchingMetadata) {
                                        showOverflowMenu = false
                                        isFetchingMetadata = true
                                        scope.launch {
                                            try {
                                                libraryViewModel.fetchMetadata(book)
                                                Toast.makeText(context, "Metadata updated", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
                                            } finally { isFetchingMetadata = false }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    if (isFetchingMetadata) CircularProgressIndicator(Modifier.size(20.dp), accentColor, 2.dp)
                                    else Icon(Icons.Rounded.Refresh, null, tint = accentColor)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_mark_as), color = menuTextColor) },
                                onClick = { showOverflowMenu = false; showMarkAsDialog = true },
                                leadingIcon = { Icon(Icons.Rounded.CheckCircle, null, tint = accentColor) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_backup_to_cloud), color = menuTextColor) },
                                onClick = {
                                    showOverflowMenu = false
                                    Toast.makeText(context, context.getString(R.string.toast_cloud_backup_hint), Toast.LENGTH_LONG).show()
                                },
                                leadingIcon = { Icon(Icons.Rounded.CloudUpload, null, tint = accentColor) }
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_delete_book), color = destructiveColor) },
                                onClick = { showOverflowMenu = false; showDeleteDialog = true },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = destructiveColor) }
                            )
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // SCROLLABLE CONTENT
                // ═══════════════════════════════════════════════════════════════
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ───────────────────────────────────────────────────────────
                    // COVER ART - Large (92%) with subtle shadow
                    // ───────────────────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f)
                            .shadow(
                                elevation = 32.dp,
                                shape = RoundedCornerShape(12.dp),
                                spotColor = Color.Black.copy(alpha = 0.5f)
                            )
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
                                    .background(Color(0xFF2C2C2E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Book,
                                    null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ───────────────────────────────────────────────────────────
                    // TITLE
                    // ───────────────────────────────────────────────────────────
                    Text(
                        text = book.title,
                        style = GlassTypography.Title.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    // ───────────────────────────────────────────────────────────
                    // AUTHOR - Clickable (accent color, prominent)
                    // ───────────────────────────────────────────────────────────
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

                    // ───────────────────────────────────────────────────────────
                    // NARRATOR - "Read by" (subtle, under author)
                    // ───────────────────────────────────────────────────────────
                    if (book.narrator.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Read by ${book.narrator}",
                            style = GlassTypography.Caption.copy(fontSize = 13.sp),
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // ───────────────────────────────────────────────────────────
                    // INFO ROW - Genre + Duration + Progress + Series
                    // ───────────────────────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Genre chip (if available)
                        if (book.genre.isNotBlank()) {
                            InfoChip(
                                icon = Icons.Rounded.Category,
                                text = book.genre,
                                accentColor = accentColor
                            )
                            Spacer(Modifier.width(6.dp))
                        }

                        // Duration pill
                        InfoChip(
                            icon = Icons.Rounded.Schedule,
                            text = formatDuration(book.duration),
                            accentColor = accentColor
                        )

                        // Progress (if started)
                        if (progressPercent > 0) {
                            Spacer(Modifier.width(6.dp))
                            InfoChip(
                                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                                text = "$progressPercent%",
                                accentColor = accentColor
                            )
                        }

                        // Series chip
                        if (seriesInfo != null) {
                            Spacer(Modifier.width(6.dp))
                            InfoChip(
                                icon = Icons.Rounded.AutoStories,
                                text = if (seriesInfo.bookNumber != null) {
                                    if (seriesInfo.bookNumber % 1 == 0f) "#${seriesInfo.bookNumber.toInt()}"
                                    else "#${seriesInfo.bookNumber}"
                                } else "Series",
                                accentColor = accentColor,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onSeriesClick(seriesInfo.name)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ───────────────────────────────────────────────────────────
                    // PLAY BUTTON - Matches player style (dark bg, accent icon)
                    // ───────────────────────────────────────────────────────────
                    PlayButton(
                        isResuming = progressPercent > 0,
                        isReverieDark = isReverieDark,
                        accentColor = accentColor,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onPlayBook(book)
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    // ───────────────────────────────────────────────────────────
                    // SYNOPSIS - Primary content, gets here fast!
                    // ───────────────────────────────────────────────────────────
                    if (book.synopsis.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.synopsis),
                                    style = GlassTypography.Label.copy(
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                if (book.synopsis.length > 200) {
                                    Text(
                                        if (isSynopsisExpanded) stringResource(R.string.show_less)
                                        else stringResource(R.string.read_more),
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
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 5,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        // No synopsis - show placeholder
                        Text(
                            text = stringResource(R.string.no_synopsis),
                            style = GlassTypography.Body,
                            color = Color.White.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // ───────────────────────────────────────────────────────────
                    // MISSING METADATA BANNER
                    // ───────────────────────────────────────────────────────────
                    if (book.hasIncompleteMetadata) {
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.1f))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    isFetchingMetadata = true
                                    scope.launch {
                                        try {
                                            libraryViewModel.fetchMetadata(book)
                                            Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) { }
                                        finally { isFetchingMetadata = false }
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.CloudDownload,
                                null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Tap to fetch missing metadata",
                                style = GlassTypography.Caption,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f)
                            )
                            if (isFetchingMetadata) {
                                CircularProgressIndicator(
                                    Modifier.size(16.dp),
                                    accentColor,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(100.dp))
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // DIALOGS
        // ═══════════════════════════════════════════════════════════════════════

        if (showMarkAsDialog) {
            AlertDialog(
                onDismissRequest = { showMarkAsDialog = false },
                containerColor = menuBg,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        stringResource(R.string.menu_mark_as),
                        color = menuTextColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        listOf("Not Started", "In Progress", "Finished").forEach { status ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showMarkAsDialog = false
                                        Toast.makeText(context, "Marked as $status", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(status, color = menuTextColor)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = menuBg,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("Delete Book?", color = menuTextColor, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "This will remove the book from your library.",
                        color = menuTextColor.copy(alpha = 0.7f)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        performExit()
                    }) { Text("Delete", color = destructiveColor) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = menuTextColor.copy(alpha = 0.6f))
                    }
                }
            )
        }

        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                containerColor = menuBg,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("Edit Book", color = menuTextColor, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("Full editor coming soon.", color = menuTextColor.copy(alpha = 0.7f))
                },
                confirmButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("OK", color = accentColor)
                    }
                }
            )
        }

        if (showCoverPicker) {
            CoverArtPickerDialog(
                currentCoverUrl = book.coverUrl,
                bookTitle = book.title,
                bookAuthor = book.author,
                accentColor = accentColor,
                isReverieDark = isReverieDark,
                onDismiss = { showCoverPicker = false },
                onCoverSelected = { newCoverUrl ->
                    coverArtViewModel.updateBookCover(bookId, newCoverUrl)
                    Toast.makeText(context, "Cover updated", Toast.LENGTH_SHORT).show()
                },
                viewModel = coverArtViewModel
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// COMPONENTS
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Play Button - Matches the player's design language exactly
 * Dark background with accent-colored icon and subtle shadow glow
 */
@Composable
private fun PlayButton(
    isResuming: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "playButtonScale"
    )

    // Match player style: dark button, accent icon
    val buttonBg = if (isReverieDark) Color(0xFF1C1C1E) else Color(0xFF2C2C2E)
    val shadowColor = if (isReverieDark) accentColor.copy(alpha = 0.3f) else Color.Black

    Box(
        modifier = Modifier
            .scale(scale)
            .shadow(16.dp, CircleShape, spotColor = shadowColor)
            .size(64.dp)
            .clip(CircleShape)
            .background(buttonBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = if (isResuming) "Resume" else "Play",
            tint = accentColor,
            modifier = Modifier.size(36.dp)
        )
    }
}

/**
 * Info Chip - Subtle glass-style chip for metadata display
 */
@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    accentColor: Color,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        color = Color.White.copy(alpha = 0.08f),
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
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
