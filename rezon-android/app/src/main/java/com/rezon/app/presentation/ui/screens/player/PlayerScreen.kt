package com.rezon.app.presentation.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rezon.app.domain.model.Bookmark
import com.rezon.app.domain.model.Chapter
import com.rezon.app.presentation.ui.components.AddBookmarkDialog
import com.rezon.app.presentation.ui.components.PlaybackSpeedDialog
import com.rezon.app.presentation.ui.components.SleepTimerDialog as NewSleepTimerDialog
import com.rezon.app.presentation.ui.theme.*
import com.rezon.app.presentation.viewmodel.PlayerViewModel
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.material3.LinearProgressIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    bookId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit = {},
    onNavigateToBookmarks: (String) -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Chapter sheet and options menu state
    var showChaptersSheet by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var initialSheetTab by remember { mutableIntStateOf(0) } // 0 = chapters, 1 = bookmarks
    val chaptersSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    // Horizontal swipe seek state
    var isHorizontalSeeking by remember { mutableStateOf(false) }
    var seekPreviewPosition by remember { mutableStateOf(0L) }
    var seekStartPosition by remember { mutableStateOf(0L) }
    var horizontalDragStartX by remember { mutableFloatStateOf(0f) }

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
                // Edge swipe gestures for brightness/volume AND horizontal seek
                detectDragGestures(
                    onDragStart = { offset ->
                        when {
                            // Left edge - brightness
                            offset.x < edgeZoneWidth -> showBrightnessIndicator = true
                            // Right edge - volume
                            offset.x > containerSize.width - edgeZoneWidth -> showVolumeIndicator = true
                            // Center area - horizontal seek
                            else -> {
                                isHorizontalSeeking = true
                                seekStartPosition = uiState.currentPosition
                                seekPreviewPosition = uiState.currentPosition
                                horizontalDragStartX = offset.x
                            }
                        }
                    },
                    onDragEnd = {
                        if (isHorizontalSeeking) {
                            // Commit the seek
                            viewModel.seekTo(seekPreviewPosition)
                        }
                        showBrightnessIndicator = false
                        showVolumeIndicator = false
                        isHorizontalSeeking = false
                    },
                    onDragCancel = {
                        showBrightnessIndicator = false
                        showVolumeIndicator = false
                        isHorizontalSeeking = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        when {
                            // Left edge - Brightness control
                            showBrightnessIndicator -> {
                                val changeAmount = -dragAmount.y / containerSize.height * 2
                                brightnessLevel = (brightnessLevel + changeAmount).coerceIn(0f, 1f)
                                viewModel.setBrightness(brightnessLevel)
                            }
                            // Right edge - Volume control
                            showVolumeIndicator -> {
                                val changeAmount = -dragAmount.y / containerSize.height * 2
                                volumeLevel = (volumeLevel + changeAmount).coerceIn(0f, 1f)
                                viewModel.setVolume(volumeLevel)
                            }
                            // Horizontal swipe - Timeline seek
                            isHorizontalSeeking && dragAmount.x.absoluteValue > dragAmount.y.absoluteValue -> {
                                val duration = uiState.duration
                                if (duration > 0) {
                                    // 1 full swipe across screen = 2 minutes of seeking
                                    val seekSensitivity = 120_000L // 2 minutes per screen width
                                    val seekDelta = (dragAmount.x / containerSize.width) * seekSensitivity
                                    seekPreviewPosition = (seekPreviewPosition + seekDelta.toLong())
                                        .coerceIn(0L, duration)
                                }
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
                onChaptersClick = {
                    initialSheetTab = 0
                    showChaptersSheet = true
                },
                onMenuClick = { showOptionsMenu = true },
                showOptionsMenu = showOptionsMenu,
                onDismissMenu = { showOptionsMenu = false },
                onAddBookmark = { showAddBookmarkDialog = true },
                onQuickBookmark = {
                    viewModel.addBookmark("")
                    Toast.makeText(context, "Bookmark added", Toast.LENGTH_SHORT).show()
                },
                onViewBookmarks = {
                    initialSheetTab = 1
                    showChaptersSheet = true
                },
                onEqualizer = onNavigateToEqualizer,
                onSleepTimer = { showSleepTimerDialog = true },
                onPlaybackSpeed = { showSpeedDialog = true },
                onShare = {
                    val book = uiState.book
                    if (book != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Check out this audiobook!")
                            putExtra(Intent.EXTRA_TEXT, "I'm listening to \"${book.title}\" by ${book.author} on REZON - Audiobooks Reimagined!")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                },
                onBookInfo = { /* TODO: Book info dialog */ }
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
                        ambientColor = RezonPurple.copy(alpha = 0.5f),
                        spotColor = RezonPurple.copy(alpha = 0.5f)
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
                if (showPlayPauseRipple) {
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
            val sleepTimerRemaining = uiState.sleepTimerRemaining
            PlaybackControls(
                isPlaying = uiState.isPlaying,
                playbackSpeed = uiState.playbackSpeed,
                sleepTimerActive = sleepTimerRemaining != null && sleepTimerRemaining > 0,
                onPlayPause = { viewModel.togglePlayPause() },
                onSkipBackward = { viewModel.skipBackward() },
                onSkipForward = { viewModel.skipForward() },
                onPreviousChapter = { viewModel.previousChapter() },
                onNextChapter = { viewModel.nextChapter() },
                onSpeedChange = { showSpeedDialog = true },
                onSleepTimer = { showSleepTimerDialog = true }
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

        // Seek Preview Overlay (horizontal swipe)
        SeekPreviewOverlay(
            visible = isHorizontalSeeking,
            currentPosition = uiState.currentPosition,
            seekPosition = seekPreviewPosition,
            duration = uiState.duration,
            modifier = Modifier.align(Alignment.Center)
        )
    }

    // Chapters & Bookmarks Bottom Sheet
    if (showChaptersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChaptersSheet = false },
            sheetState = chaptersSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ChaptersAndBookmarksSheet(
                chapters = uiState.book?.chapters ?: emptyList(),
                bookmarks = uiState.bookmarks,
                currentChapterIndex = uiState.currentChapterIndex,
                initialTab = initialSheetTab,
                onChapterClick = { chapter ->
                    viewModel.seekTo(chapter.startTime)
                    showChaptersSheet = false
                },
                onBookmarkClick = { bookmark ->
                    viewModel.goToBookmark(bookmark)
                    showChaptersSheet = false
                },
                onDeleteBookmark = { bookmark ->
                    viewModel.removeBookmark(bookmark.id)
                    Toast.makeText(context, "Bookmark deleted", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        NewSleepTimerDialog(
            currentTimer = uiState.sleepTimerRemaining,
            onDismiss = { showSleepTimerDialog = false },
            onTimerSelected = { durationMs ->
                if (durationMs == null) {
                    viewModel.cancelSleepTimer()
                } else {
                    viewModel.startSleepTimer(durationMs)
                }
                showSleepTimerDialog = false
            }
        )
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = uiState.playbackSpeed,
            onDismiss = { showSpeedDialog = false },
            onSpeedSelected = { speed ->
                viewModel.setPlaybackSpeed(speed)
            }
        )
    }

    // Add Bookmark Dialog
    if (showAddBookmarkDialog) {
        AddBookmarkDialog(
            currentPosition = uiState.currentPosition,
            onDismiss = { showAddBookmarkDialog = false },
            onSave = { note ->
                viewModel.addBookmark(note)
                showAddBookmarkDialog = false
                Toast.makeText(context, "Bookmark added", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Chapters and Bookmarks bottom sheet with tabs
 */
@Composable
private fun ChaptersAndBookmarksSheet(
    chapters: List<Chapter>,
    bookmarks: List<Bookmark>,
    currentChapterIndex: Int,
    initialTab: Int = 0,
    onChapterClick: (Chapter) -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf("Chapters", "Bookmarks")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            tabs.forEachIndexed { index, title ->
                val count = if (index == 0) chapters.size else bookmarks.size
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = "$title ($count)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = RezonPurple,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> ChaptersTabContent(
                chapters = chapters,
                currentChapterIndex = currentChapterIndex,
                onChapterClick = onChapterClick
            )
            1 -> BookmarksTabContent(
                bookmarks = bookmarks,
                onBookmarkClick = onBookmarkClick,
                onDeleteBookmark = onDeleteBookmark
            )
        }
    }
}

/**
 * Chapters tab content
 */
@Composable
private fun ChaptersTabContent(
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    onChapterClick: (Chapter) -> Unit
) {
    if (chapters.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No chapters available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.height(400.dp)
        ) {
            itemsIndexed(chapters) { index, chapter ->
                ChapterItem(
                    chapter = chapter,
                    isCurrentChapter = index == currentChapterIndex,
                    onClick = { onChapterClick(chapter) }
                )
            }
        }
    }
}

/**
 * Bookmarks tab content
 */
@Composable
private fun BookmarksTabContent(
    bookmarks: List<Bookmark>,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit
) {
    if (bookmarks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = RezonPurple.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No bookmarks yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Use the menu to add a bookmark",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.height(400.dp)
        ) {
            itemsIndexed(bookmarks) { _, bookmark ->
                BookmarkItem(
                    bookmark = bookmark,
                    onClick = { onBookmarkClick(bookmark) },
                    onDelete = { onDeleteBookmark(bookmark) }
                )
            }
        }
    }
}

/**
 * Single bookmark item in the list
 */
@Composable
private fun BookmarkItem(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bookmark icon
        Icon(
            imageVector = Icons.Default.Bookmark,
            contentDescription = null,
            tint = RezonPurple,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Time position
            Text(
                text = bookmark.formatPosition(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Note if available
            bookmark.note?.let { note ->
                if (note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Chapter info
            bookmark.chapterIndex?.let { chapterIdx ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chapter ${chapterIdx + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // Delete button
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete bookmark",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Sleep Timer Dialog
 */
@Composable
private fun SleepTimerDialog(
    currentTimerRemaining: Long?,
    onSetTimer: (Long) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val timerOptions = listOf(
        "5 minutes" to 5 * 60 * 1000L,
        "10 minutes" to 10 * 60 * 1000L,
        "15 minutes" to 15 * 60 * 1000L,
        "30 minutes" to 30 * 60 * 1000L,
        "45 minutes" to 45 * 60 * 1000L,
        "1 hour" to 60 * 60 * 1000L,
        "End of chapter" to -1L // Special case
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            // Custom Z Timer icon
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(48.dp)) {
                    drawCircle(
                        color = RezonPurple,
                        radius = size.minDimension / 2 - 4.dp.toPx(),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                Text(
                    text = "Z",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = RezonPurple
                )
            }
        },
        title = {
            Text(
                text = "Sleep Timer",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (currentTimerRemaining != null && currentTimerRemaining > 0) {
                    // Show current timer
                    Text(
                        text = "Timer active: ${formatTime(currentTimerRemaining)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RezonPurple,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Timer options
                timerOptions.forEach { (label, duration) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetTimer(duration) }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (currentTimerRemaining != null && currentTimerRemaining > 0) {
                TextButton(onClick = onCancelTimer) {
                    Text("Cancel Timer", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Single chapter item in the list
 */
@Composable
private fun ChapterItem(
    chapter: Chapter,
    isCurrentChapter: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isCurrentChapter) RezonPurple.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrentChapter) RezonPurple else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTime(chapter.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isCurrentChapter) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Now Playing",
                tint = RezonPurple,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Seek preview overlay for horizontal swipe
 */
@Composable
private fun SeekPreviewOverlay(
    visible: Boolean,
    currentPosition: Long,
    seekPosition: Long,
    duration: Long,
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
                    Color.Black.copy(alpha = 0.85f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Preview time
            Text(
                text = formatTime(seekPosition),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Time difference
            val diff = seekPosition - currentPosition
            val diffText = if (diff >= 0) "+${formatTime(diff)}" else "-${formatTime(-diff)}"
            val diffColor = if (diff >= 0) RezonPurple else RezonAccentPink
            Text(
                text = diffText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = diffColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar preview
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .background(ProgressTrack, RoundedCornerShape(2.dp))
            ) {
                if (duration > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((seekPosition.toFloat() / duration).coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(ProgressFill, RoundedCornerShape(2.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Total duration
            Text(
                text = "/ ${formatTime(duration)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Format milliseconds to HH:MM:SS or MM:SS
 */
private fun formatTime(ms: Long): String {
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
    onMenuClick: () -> Unit,
    showOptionsMenu: Boolean,
    onDismissMenu: () -> Unit,
    onAddBookmark: () -> Unit,
    onQuickBookmark: () -> Unit,
    onViewBookmarks: () -> Unit,
    onEqualizer: () -> Unit,
    onSleepTimer: () -> Unit,
    onPlaybackSpeed: () -> Unit,
    onShare: () -> Unit,
    onBookInfo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

        Box {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp)
                )
            }

            DropdownMenu(
                expanded = showOptionsMenu,
                onDismissRequest = onDismissMenu
            ) {
                PlayerMenuItem(
                    icon = Icons.Default.BookmarkAdd,
                    text = "Add Bookmark with Note",
                    onClick = {
                        onAddBookmark()
                        onDismissMenu()
                    }
                )
                PlayerMenuItem(
                    icon = Icons.Default.Bookmark,
                    text = "Quick Bookmark",
                    onClick = {
                        onQuickBookmark()
                        onDismissMenu()
                    }
                )
                PlayerMenuItem(
                    icon = Icons.Default.Bookmark,
                    text = "View Bookmarks",
                    onClick = {
                        onViewBookmarks()
                        onDismissMenu()
                    }
                )
                HorizontalDivider()
                PlayerMenuItem(
                    icon = Icons.Default.Equalizer,
                    text = "Equalizer",
                    onClick = {
                        onEqualizer()
                        onDismissMenu()
                    }
                )
                PlayerMenuItem(
                    icon = Icons.Default.Speed,
                    text = "Playback Speed",
                    onClick = {
                        onPlaybackSpeed()
                        onDismissMenu()
                    }
                )
                PlayerMenuItem(
                    icon = Icons.Default.Timer,
                    text = "Sleep Timer",
                    onClick = {
                        onSleepTimer()
                        onDismissMenu()
                    }
                )
                HorizontalDivider()
                PlayerMenuItem(
                    icon = Icons.Default.Share,
                    text = "Share",
                    onClick = {
                        onShare()
                        onDismissMenu()
                    }
                )
                PlayerMenuItem(
                    icon = Icons.Default.Info,
                    text = "Book Info",
                    onClick = {
                        onBookInfo()
                        onDismissMenu()
                    }
                )
            }
        }
    }
}

/**
 * Single menu item in dropdown
 */
@Composable
private fun PlayerMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = text)
            }
        },
        onClick = onClick
    )
}

/**
 * Book progress section showing elapsed/remaining time, chapter indicator, and overall progress bar
 */
@Composable
private fun BookProgressSection(
    currentPosition: Long,
    duration: Long,
    currentChapterIndex: Int,
    totalChapters: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Overall book progress bar
        val progress = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = RezonCyan,
            trackColor = ProgressTrack
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Time and chapter info row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(currentPosition),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = RezonCyan
            )

            Text(
                text = "${currentChapterIndex + 1}/$totalChapters",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "-${formatTime(duration - currentPosition)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
 * Custom Sleep Timer Icon with Z inside a clock
 */
@Composable
private fun SleepTimerIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isActive: Boolean = false
) {
    Box(
        modifier = modifier.size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        // Clock circle
        Canvas(modifier = Modifier.size(28.dp)) {
            // Outer circle
            drawCircle(
                color = if (isActive) RezonPurple else tint,
                radius = size.minDimension / 2 - 2.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        // Z letter in center
        Text(
            text = "Z",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = if (isActive) RezonPurple else tint
        )
    }
}

/**
 * Playback controls row
 */
@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    playbackSpeed: Float,
    sleepTimerActive: Boolean = false,
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
        // Playback speed - Styled pill button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (playbackSpeed != 1.0f) RezonCyan.copy(alpha = 0.15f)
                    else RezonSurfaceVariant
                )
                .border(
                    width = 1.dp,
                    color = if (playbackSpeed != 1.0f) RezonCyan else Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(onClick = onSpeedChange)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${playbackSpeed}x",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (playbackSpeed != 1.0f) RezonCyan else Color.White
            )
        }

        // Skip backward 10s
        IconButton(
            onClick = onSkipBackward,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Replay10,
                contentDescription = "Skip Backward 10s",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Play/Pause button - Large central button
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = RezonCyan.copy(alpha = 0.4f),
                    spotColor = RezonCyan.copy(alpha = 0.4f)
                )
                .background(RezonCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Skip forward 10s (changed from 30s for consistency)
        IconButton(
            onClick = onSkipForward,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Forward30,
                contentDescription = "Skip Forward 10s",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Sleep timer - Styled pill button to match speed
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (sleepTimerActive) RezonCyan.copy(alpha = 0.15f)
                    else RezonSurfaceVariant
                )
                .border(
                    width = 1.dp,
                    color = if (sleepTimerActive) RezonCyan else Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(onClick = onSleepTimer)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            SleepTimerIcon(
                tint = if (sleepTimerActive) RezonCyan else Color.White,
                isActive = sleepTimerActive,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

