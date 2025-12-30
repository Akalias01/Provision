package com.mossglen.reverie.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mossglen.reverie.data.Chapter
import com.mossglen.reverie.ui.theme.*
import com.mossglen.reverie.ui.viewmodel.LibraryViewModel
import com.mossglen.reverie.ui.viewmodel.PlayerViewModel

/**
 * Chapter List Screen
 *
 * Shows all chapters for a specific book with playback controls.
 * Displays chapter progress and allows quick navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListScreen(
    bookId: String,
    isDark: Boolean = true,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.ReverieAccent,
    onBack: () -> Unit,
    onChapterClick: (Chapter) -> Unit = {},
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current

    val books by libraryViewModel.books.collectAsState()
    val book = books.find { it.id == bookId }

    val currentBook by playerViewModel.currentBook.collectAsState()
    val position by playerViewModel.position.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()

    // Determine current position - use player position if this book is playing
    val currentPosition = if (currentBook?.id == bookId) position else book?.progress ?: 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Chapters",
                            style = GlassTypography.Title,
                            color = theme.textPrimary
                        )
                        book?.let {
                            Text(
                                text = "${it.chapters.size} chapter${if (it.chapters.size != 1) "s" else ""}",
                                style = GlassTypography.Caption,
                                color = theme.textSecondary
                            )
                        }
                    }
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = theme.background
    ) { padding ->
        if (book == null) {
            // Book not found
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = theme.textTertiary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.M))
                    Text(
                        text = "Book not found",
                        style = GlassTypography.Body,
                        color = theme.textSecondary
                    )
                }
            }
        } else if (book.chapters.isEmpty()) {
            // No chapters
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = theme.textTertiary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.M))
                    Text(
                        text = "No chapters available",
                        style = GlassTypography.Body,
                        color = theme.textSecondary
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.S))
                    Text(
                        text = "This book doesn't have chapter information",
                        style = GlassTypography.Caption,
                        color = theme.textTertiary
                    )
                }
            }
        } else {
            // Chapter list
            val listState = rememberLazyListState()

            // Auto-scroll to current chapter on first composition
            LaunchedEffect(book.chapters, currentPosition) {
                val currentChapterIndex = book.chapters.indexOfLast { currentPosition >= it.startMs }
                if (currentChapterIndex >= 0) {
                    // Scroll to current chapter (add 1 for header item)
                    listState.animateScrollToItem(currentChapterIndex + 1)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(GlassSpacing.M),
                verticalArrangement = Arrangement.spacedBy(GlassSpacing.S)
            ) {
                // Book header with overall progress
                item {
                    BookProgressHeader(
                        book = book,
                        currentPosition = currentPosition,
                        accentColor = accentColor,
                        isDark = isDark,
                        isReverieDark = isReverieDark
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.M))
                }

                // Chapter items
                itemsIndexed(book.chapters, key = { index, _ -> index }) { index, chapter ->
                    val isCurrent = currentPosition >= chapter.startMs && currentPosition < chapter.endMs
                    val isCompleted = currentPosition > chapter.endMs

                    ChapterItem(
                        chapter = chapter,
                        index = index + 1,
                        isCurrent = isCurrent,
                        isCompleted = isCompleted && !isCurrent,
                        accentColor = accentColor,
                        isDark = isDark,
                        isReverieDark = isReverieDark,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            // Load book if not current, then seek to chapter
                            if (currentBook?.id != bookId) {
                                playerViewModel.loadBook(book)
                            }
                            playerViewModel.seekTo(chapter.startMs)
                            if (!isPlaying) {
                                playerViewModel.togglePlayback()
                            }
                            onChapterClick(chapter)
                        }
                    )
                }
            }
        }
    }
}

// ============================================================================
// BOOK PROGRESS HEADER
// ============================================================================

@Composable
private fun BookProgressHeader(
    book: com.mossglen.reverie.data.Book,
    currentPosition: Long,
    accentColor: Color,
    isDark: Boolean,
    isReverieDark: Boolean
) {
    val theme = glassTheme(isDark, isReverieDark)
    val progress = if (book.duration > 0) {
        (currentPosition.toFloat() / book.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(theme.glassPrimary)
            .padding(GlassSpacing.L)
    ) {
        Text(
            text = book.title,
            style = GlassTypography.Headline,
            fontWeight = FontWeight.SemiBold,
            color = theme.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(GlassSpacing.XS))

        Text(
            text = book.author,
            style = GlassTypography.Body,
            color = theme.textSecondary
        )

        Spacer(modifier = Modifier.height(GlassSpacing.M))

        // Progress bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(currentPosition),
                    style = GlassTypography.Caption,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(progress * 100).toInt()}% Complete",
                    style = GlassTypography.Caption,
                    color = theme.textSecondary
                )
                Text(
                    text = formatDuration(book.duration),
                    style = GlassTypography.Caption,
                    color = theme.textTertiary
                )
            }

            Spacer(modifier = Modifier.height(GlassSpacing.S))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(theme.glassSecondary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }
        }
    }
}

// ============================================================================
// CHAPTER ITEM
// ============================================================================

@Composable
private fun ChapterItem(
    chapter: Chapter,
    index: Int,
    isCurrent: Boolean,
    isCompleted: Boolean,
    accentColor: Color,
    isDark: Boolean,
    isReverieDark: Boolean,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val highlightColor = if (isReverieDark) GlassColors.WarmSlate else Color.White.copy(alpha = 0.1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(if (isCurrent) highlightColor else theme.glassPrimary)
            .clickable(onClick = onClick)
            .padding(GlassSpacing.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chapter number / status indicator
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCurrent -> accentColor
                        isCompleted -> accentColor.copy(alpha = 0.3f)
                        else -> theme.glassSecondary
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted && !isCurrent) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            } else if (isCurrent) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Playing",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = "$index",
                    style = GlassTypography.Body,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(GlassSpacing.M))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.title,
                style = GlassTypography.Body,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent) accentColor else theme.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(GlassSpacing.XS))

            val chapterDuration = chapter.endMs - chapter.startMs
            Text(
                text = formatDuration(chapterDuration),
                style = GlassTypography.Caption,
                color = theme.textSecondary
            )
        }

        if (!isCurrent && !isCompleted) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = "Play",
                tint = theme.textTertiary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================================================
// UTILITIES
// ============================================================================

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
