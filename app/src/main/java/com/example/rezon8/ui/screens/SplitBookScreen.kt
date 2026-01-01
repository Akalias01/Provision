package com.mossglen.lithos.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mossglen.lithos.data.Book
import com.mossglen.lithos.data.Chapter
import com.mossglen.lithos.ui.theme.*
import com.mossglen.lithos.ui.viewmodel.LibraryViewModel
import kotlin.math.roundToInt

// Lithos Amber Design Language Colors
private val LithosAmber = Color(0xFFD48C2C)
private val LithosMoss = Color(0xFF4A5D45)
private val LithosSlate = Color(0xFF1A1D21)
private val LithosGlassBackground = Color(0xD91A1D21) // rgba(26, 29, 33, 0.85)

/**
 * SplitBookScreen - Premium UI for splitting audiobooks into segments.
 *
 * Features:
 * - Interactive timeline with chapter markers
 * - Tap to add split points
 * - Drag to adjust split points
 * - Editable segment titles
 * - Real-time segment duration preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBookScreen(
    bookId: String,
    isDark: Boolean = true,
    isOLED: Boolean = false,
    accentColor: Color = LithosAmber,
    onBack: () -> Unit,
    onSplitComplete: () -> Unit,
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val theme = glassTheme(isDark, isOLED)
    val view = LocalView.current
    val density = LocalDensity.current

    // Get book from library
    val books by libraryViewModel.books.collectAsState()
    val book = books.find { it.id == bookId }

    // Split points state (in milliseconds)
    var splitPoints by remember { mutableStateOf(listOf<Long>()) }

    // Segment titles - automatically generated, can be edited
    var segmentTitles by remember(book?.title, splitPoints.size) {
        mutableStateOf(
            generateSegmentTitles(book?.title ?: "Book", splitPoints.size + 1)
        )
    }

    // UI state
    var showConfirmDialog by remember { mutableStateOf(false) }
    var keepOriginal by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var timelineWidth by remember { mutableIntStateOf(0) }

    BackHandler { onBack() }

    if (book == null) {
        Box(Modifier.fillMaxSize().background(LithosSlate), contentAlignment = Alignment.Center) {
            Text("Book not found", color = theme.textSecondary)
        }
        return
    }

    // Computed segments from split points
    val segments = remember(splitPoints, book.duration) {
        computeSegments(book.duration, splitPoints)
    }

    // Update titles when segments change
    LaunchedEffect(segments.size) {
        if (segmentTitles.size != segments.size) {
            segmentTitles = generateSegmentTitles(book.title, segments.size)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LithosSlate)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        "Split Book",
                        style = GlassTypography.Title,
                        color = theme.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.textPrimary
                        )
                    }
                },
                actions = {
                    // Reset button
                    if (splitPoints.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                splitPoints = emptyList()
                            }
                        ) {
                            Text("Reset", color = LithosAmber)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(GlassSpacing.M),
                verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)
            ) {
                // Book Info Card
                item {
                    BookInfoCard(
                        book = book,
                        theme = theme,
                        accentColor = accentColor
                    )
                }

                // Format Support Banner
                item {
                    val fileExtension = book.filePath
                        .substringAfterLast('.', "")
                        .substringBefore('?')
                        .lowercase()
                    val isSupported = fileExtension in listOf("m4b", "m4a", "mp4", "m4p")

                    FormatSupportBanner(
                        isSupported = isSupported,
                        fileExtension = fileExtension,
                        theme = theme,
                        accentColor = accentColor
                    )
                }

                // Timeline Section
                item {
                    TimelineSection(
                        book = book,
                        splitPoints = splitPoints,
                        theme = theme,
                        accentColor = accentColor,
                        onTimelineWidthChanged = { timelineWidth = it },
                        onAddSplitPoint = { position ->
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            val timeMs = (position * book.duration).toLong()
                            // Don't add if too close to existing points
                            val minDistance = book.duration * 0.02 // 2% minimum distance
                            val isTooClose = splitPoints.any {
                                kotlin.math.abs(it - timeMs) < minDistance
                            }
                            if (!isTooClose && timeMs > 0 && timeMs < book.duration) {
                                splitPoints = (splitPoints + timeMs).sorted()
                            }
                        },
                        onMoveSplitPoint = { index, newPosition ->
                            val timeMs = (newPosition * book.duration).toLong()
                            if (timeMs > 0 && timeMs < book.duration) {
                                splitPoints = splitPoints.toMutableList().apply {
                                    set(index, timeMs)
                                }.sorted()
                            }
                        },
                        onRemoveSplitPoint = { index ->
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            splitPoints = splitPoints.toMutableList().apply {
                                removeAt(index)
                            }
                        }
                    )
                }

                // Segments Preview
                item {
                    Text(
                        "SEGMENTS (${segments.size})",
                        style = GlassTypography.Caption,
                        color = theme.textSecondary,
                        modifier = Modifier.padding(start = GlassSpacing.XS, top = GlassSpacing.S)
                    )
                }

                itemsIndexed(segments) { index, segment ->
                    SegmentCard(
                        index = index,
                        segment = segment,
                        title = segmentTitles.getOrElse(index) { "Part ${index + 1}" },
                        onTitleChange = { newTitle ->
                            segmentTitles = segmentTitles.toMutableList().apply {
                                if (index < size) set(index, newTitle)
                            }
                        },
                        theme = theme,
                        accentColor = accentColor
                    )
                }

                // Add Split Point Button
                item {
                    AddSplitPointButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            // Add a split point at the middle of the largest segment
                            val largestSegment = segments.maxByOrNull { it.second - it.first }
                            if (largestSegment != null) {
                                val midPoint = (largestSegment.first + largestSegment.second) / 2
                                splitPoints = (splitPoints + midPoint).sorted()
                            }
                        },
                        accentColor = accentColor
                    )
                }

                // Spacer for bottom button
                item {
                    Spacer(Modifier.height(80.dp))
                }
            }

            // Bottom Action Button
            AnimatedVisibility(
                visible = splitPoints.isNotEmpty() && !isProcessing,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, LithosSlate)
                            )
                        )
                        .padding(GlassSpacing.M)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            showConfirmDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(GlassShapes.Medium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LithosGlassBackground
                        )
                    ) {
                        Icon(
                            Icons.Rounded.CallSplit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = LithosAmber
                        )
                        Spacer(Modifier.width(GlassSpacing.XS))
                        Text(
                            "Split into ${segments.size} Parts",
                            style = GlassTypography.Label,
                            fontWeight = FontWeight.SemiBold,
                            color = LithosAmber
                        )
                    }
                }
            }
        }

        // Confirmation Dialog
        if (showConfirmDialog) {
            SplitConfirmationDialog(
                segmentCount = segments.size,
                bookTitle = book.title,
                theme = theme,
                accentColor = accentColor,
                onDismiss = { showConfirmDialog = false },
                onConfirm = { keep ->
                    keepOriginal = keep
                    showConfirmDialog = false
                    isProcessing = true

                    // Create split segments data
                    val splitSegments = segments.mapIndexed { index, (start, end) ->
                        Triple(
                            segmentTitles.getOrElse(index) { "${book.title} - Part ${index + 1}" },
                            start,
                            end
                        )
                    }

                    // Trigger split operation via ViewModel
                    libraryViewModel.splitBook(
                        bookId = book.id,
                        segments = splitSegments,
                        keepOriginal = keepOriginal,
                        onComplete = {
                            isProcessing = false
                            onSplitComplete()
                        },
                        onError = { error ->
                            isProcessing = false
                            // Error handling - show toast or snackbar
                        }
                    )
                }
            )
        }

        // Processing Overlay
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)
                ) {
                    CircularProgressIndicator(
                        color = accentColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Splitting audiobook...",
                        style = GlassTypography.Body,
                        color = theme.textPrimary
                    )
                    Text(
                        "This may take a few minutes",
                        style = GlassTypography.Caption,
                        color = theme.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun BookInfoCard(
    book: Book,
    theme: GlassThemeData,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(LithosGlassBackground)
            .padding(GlassSpacing.M),
        horizontalArrangement = Arrangement.spacedBy(GlassSpacing.M)
    ) {
        // Cover thumbnail
        AsyncImage(
            model = book.coverUrl,
            contentDescription = book.title,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(GlassShapes.Small)),
            contentScale = ContentScale.Crop
        )

        // Book info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(GlassSpacing.XXS)
        ) {
            Text(
                book.title,
                style = GlassTypography.Headline,
                color = theme.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                book.author,
                style = GlassTypography.Caption,
                color = theme.textSecondary
            )
            Text(
                formatDuration(book.duration),
                style = GlassTypography.Caption,
                color = LithosAmber
            )
        }
    }
}

@Composable
private fun TimelineSection(
    book: Book,
    splitPoints: List<Long>,
    theme: GlassThemeData,
    accentColor: Color,
    onTimelineWidthChanged: (Int) -> Unit,
    onAddSplitPoint: (Float) -> Unit,
    onMoveSplitPoint: (Int, Float) -> Unit,
    onRemoveSplitPoint: (Int) -> Unit
) {
    var timelineWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(LithosGlassBackground)
            .padding(GlassSpacing.M),
        verticalArrangement = Arrangement.spacedBy(GlassSpacing.S)
    ) {
        Text(
            "TIMELINE",
            style = GlassTypography.Caption,
            color = theme.textSecondary
        )

        // Timeline bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .onGloballyPositioned { coordinates ->
                    timelineWidth = coordinates.size.width
                    onTimelineWidthChanged(timelineWidth)
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val position = offset.x / timelineWidth
                        onAddSplitPoint(position.coerceIn(0f, 1f))
                    }
                }
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LithosSlate)
            )

            // Progress fill (entire duration) - matte finish
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LithosAmber.copy(alpha = 0.4f))
            )

            // Chapter markers
            book.chapters.forEach { chapter ->
                val position = chapter.startMs.toFloat() / book.duration
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (timelineWidth * position).roundToInt(),
                                y = 0
                            )
                        }
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(theme.textSecondary.copy(alpha = 0.3f))
                )
            }

            // Split point markers
            splitPoints.forEachIndexed { index, timeMs ->
                val position = timeMs.toFloat() / book.duration
                SplitPointMarker(
                    position = position,
                    timelineWidth = timelineWidth,
                    accentColor = accentColor,
                    onDrag = { delta ->
                        val newPosition = position + (delta / timelineWidth)
                        onMoveSplitPoint(index, newPosition.coerceIn(0.01f, 0.99f))
                    },
                    onRemove = { onRemoveSplitPoint(index) }
                )
            }
        }

        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "0:00:00",
                style = GlassTypography.Caption,
                color = theme.textSecondary.copy(alpha = 0.6f)
            )
            Text(
                formatDuration(book.duration),
                style = GlassTypography.Caption,
                color = theme.textSecondary.copy(alpha = 0.6f)
            )
        }

        // Instructions
        Text(
            "Tap timeline to add split points. Drag to adjust. Long-press to remove.",
            style = GlassTypography.Caption,
            color = theme.textSecondary.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SplitPointMarker(
    position: Float,
    timelineWidth: Int,
    accentColor: Color,
    onDrag: (Float) -> Unit,
    onRemove: () -> Unit
) {
    val view = LocalView.current

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (timelineWidth * position).roundToInt() - 12.dp.roundToPx(),
                    y = 0
                )
            }
            .size(24.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onRemove()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Vertical line
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(LithosAmber)
        )
        // Handle circle
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(LithosAmber)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun SegmentCard(
    index: Int,
    segment: Pair<Long, Long>,
    title: String,
    onTitleChange: (String) -> Unit,
    theme: GlassThemeData,
    accentColor: Color
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedTitle by remember(title) { mutableStateOf(title) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(LithosGlassBackground)
            .border(
                width = 1.dp,
                color = LithosSlate,
                shape = RoundedCornerShape(GlassShapes.Medium)
            )
            .padding(GlassSpacing.M),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GlassSpacing.M)
    ) {
        // Segment number
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(LithosAmber.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${index + 1}",
                style = GlassTypography.Label,
                color = LithosAmber,
                fontWeight = FontWeight.Bold
            )
        }

        // Segment info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(GlassSpacing.XXS)
        ) {
            if (isEditing) {
                BasicTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    textStyle = GlassTypography.Body.copy(color = theme.textPrimary),
                    cursorBrush = SolidColor(LithosAmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            LithosSlate,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    singleLine = true
                )
            } else {
                Text(
                    title,
                    style = GlassTypography.Body,
                    color = theme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Time range
            Text(
                "${formatDuration(segment.first)} - ${formatDuration(segment.second)}",
                style = GlassTypography.Caption,
                color = theme.textSecondary
            )
        }

        // Duration
        Text(
            formatDuration(segment.second - segment.first),
            style = GlassTypography.Caption,
            color = LithosAmber
        )

        // Edit button
        IconButton(
            onClick = {
                if (isEditing) {
                    onTitleChange(editedTitle)
                }
                isEditing = !isEditing
            }
        ) {
            Icon(
                if (isEditing) Icons.Rounded.Check else Icons.Rounded.Edit,
                contentDescription = if (isEditing) "Save" else "Edit",
                tint = if (isEditing) GlassColors.Success else theme.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AddSplitPointButton(
    onClick: () -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .border(
                width = 1.dp,
                color = LithosAmber.copy(alpha = 0.3f),
                shape = RoundedCornerShape(GlassShapes.Medium)
            )
            .clickable(onClick = onClick)
            .padding(GlassSpacing.M),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = null,
            tint = LithosAmber,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(GlassSpacing.XS))
        Text(
            "Add Split Point",
            style = GlassTypography.Label,
            color = LithosAmber
        )
    }
}

@Composable
private fun SplitConfirmationDialog(
    segmentCount: Int,
    bookTitle: String,
    theme: GlassThemeData,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (keepOriginal: Boolean) -> Unit
) {
    var keepOriginal by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LithosGlassBackground,
        titleContentColor = theme.textPrimary,
        textContentColor = theme.textSecondary,
        title = {
            Text(
                "Split Book?",
                style = GlassTypography.Title
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)
            ) {
                Text(
                    "\"$bookTitle\" will be split into $segmentCount separate audiobooks.",
                    style = GlassTypography.Body
                )

                // Keep original option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { keepOriginal = !keepOriginal }
                        .padding(GlassSpacing.S),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
                ) {
                    Checkbox(
                        checked = keepOriginal,
                        onCheckedChange = { keepOriginal = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = LithosAmber,
                            uncheckedColor = LithosSlate
                        )
                    )
                    Column {
                        Text(
                            "Keep original file",
                            style = GlassTypography.Body,
                            color = theme.textPrimary
                        )
                        Text(
                            "Uncheck to delete after splitting",
                            style = GlassTypography.Caption,
                            color = theme.textSecondary.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(keepOriginal) },
                colors = ButtonDefaults.buttonColors(containerColor = LithosGlassBackground)
            ) {
                Text("Split Book", color = LithosAmber)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = theme.textSecondary)
            }
        }
    )
}

// ============================================================================
// Utility Functions
// ============================================================================

private fun computeSegments(duration: Long, splitPoints: List<Long>): List<Pair<Long, Long>> {
    if (splitPoints.isEmpty()) {
        return listOf(0L to duration)
    }

    val sorted = splitPoints.sorted()
    val segments = mutableListOf<Pair<Long, Long>>()

    // First segment: start to first split point
    segments.add(0L to sorted.first())

    // Middle segments
    for (i in 0 until sorted.size - 1) {
        segments.add(sorted[i] to sorted[i + 1])
    }

    // Last segment: last split point to end
    segments.add(sorted.last() to duration)

    return segments
}

private fun generateSegmentTitles(bookTitle: String, count: Int): List<String> {
    return (1..count).map { "$bookTitle - Part $it" }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

@Composable
private fun FormatSupportBanner(
    isSupported: Boolean,
    fileExtension: String,
    theme: GlassThemeData,
    accentColor: Color
) {
    val backgroundColor = if (isSupported) {
        LithosAmber.copy(alpha = 0.1f)
    } else {
        Color(0xFFFF6B6B).copy(alpha = 0.15f)
    }

    val borderColor = if (isSupported) {
        LithosAmber.copy(alpha = 0.3f)
    } else {
        Color(0xFFFF6B6B).copy(alpha = 0.4f)
    }

    val icon = if (isSupported) {
        Icons.Rounded.CheckCircle
    } else {
        Icons.Rounded.Warning
    }

    val iconTint = if (isSupported) {
        LithosAmber
    } else {
        Color(0xFFFF6B6B)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(GlassShapes.Medium)
            )
            .padding(GlassSpacing.M),
        horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            if (isSupported) {
                Text(
                    "Format Supported",
                    style = GlassTypography.Label,
                    color = LithosAmber,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${fileExtension.uppercase()} files support lossless splitting",
                    style = GlassTypography.Caption,
                    color = theme.textSecondary
                )
            } else {
                Text(
                    "Format Not Supported",
                    style = GlassTypography.Label,
                    color = Color(0xFFFF6B6B),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${fileExtension.uppercase()} files cannot be split. Supported: M4B, M4A, MP4",
                    style = GlassTypography.Caption,
                    color = theme.textSecondary
                )
            }
        }
    }
}
