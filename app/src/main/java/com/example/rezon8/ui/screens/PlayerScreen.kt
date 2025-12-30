/*
 * LEGACY PLAYER SCREEN - NON-GLASS VERSION
 *
 * This is the original player screen without glass morphism effects.
 * Kept for reference and potential rollback if needed.
 *
 * DEPRECATED: Use PlayerScreenGlass instead for the modern glass UI.
 * This version will be removed in a future release.
 */
package com.mossglen.reverie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mossglen.reverie.ui.components.ReverieDialog
import com.mossglen.reverie.ui.viewmodel.DisplayMode
import com.mossglen.reverie.ui.viewmodel.PlayerViewModel

@Deprecated(
    message = "Use PlayerScreenGlass instead for modern glass morphism UI",
    replaceWith = ReplaceWith("PlayerScreenGlass")
)
@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel,
    accentColor: Color,
    displayMode: DisplayMode,
    onBack: () -> Unit
) {
    val book by playerViewModel.currentBook.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val pos by playerViewModel.position.collectAsState()
    val dur by playerViewModel.duration.collectAsState()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsState()
    val sleepTimerMinutes by playerViewModel.sleepTimerMinutes.collectAsState()

    // Dialog States
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showChaptersDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (book == null) return
    val currentBook = book!!

    // Calculate progress values
    val totalProgress = if (dur > 0) pos.toFloat() / dur.toFloat() else 0f

    // Use real chapters if available, otherwise simulate
    val chapters = currentBook.chapters
    val hasRealChapters = chapters.isNotEmpty()
    val totalChapters = if (hasRealChapters) chapters.size else if (dur > 0) maxOf(1, (dur / (30 * 60 * 1000)).toInt()) else 1

    val currentChapter = if (hasRealChapters) {
        chapters.indexOfLast { pos >= it.startMs }.coerceAtLeast(0) + 1
    } else if (dur > 0) {
        maxOf(1, ((totalProgress * totalChapters).toInt() + 1).coerceAtMost(totalChapters))
    } else 1

    val chapterDuration = if (hasRealChapters && currentChapter > 0 && currentChapter <= chapters.size) {
        val ch = chapters[currentChapter - 1]
        ch.endMs - ch.startMs
    } else if (dur > 0 && totalChapters > 0) dur / totalChapters else 1L

    val chapterStartMs = if (hasRealChapters && currentChapter > 0 && currentChapter <= chapters.size) {
        chapters[currentChapter - 1].startMs
    } else ((currentChapter - 1).toLong() * chapterDuration)

    val chapterPosition = (pos - chapterStartMs).coerceAtLeast(0L)
    val chapterProgress = if (chapterDuration > 0) chapterPosition.toFloat() / chapterDuration.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        // === TOP BAR ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Square thumbnail (no rounded corners)
            AsyncImage(
                model = currentBook.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF2C2C2E)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(10.dp))

            // Chapter + Author
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format("%03d", currentChapter),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentBook.author,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Chapter list icon
            IconButton(onClick = { showChaptersDialog = true }) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = "Chapters",
                    tint = Color.White
                )
            }

            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.White)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF2C2C2E))
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Bookmark", color = Color.White) },
                        onClick = {
                            playerViewModel.toggleBookmark()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.BookmarkAdd, null, tint = Color.White) }
                    )
                    DropdownMenuItem(
                        text = { Text("Book Details", color = Color.White) },
                        onClick = { showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Info, null, tint = Color.White) }
                    )
                }
            }
        }

        // === EDGE-TO-EDGE SQUARE COVER with double-tap gestures ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Square
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val width = size.width
                            when {
                                offset.x < width / 3 -> playerViewModel.skipBack() // Left third
                                offset.x > width * 2 / 3 -> playerViewModel.skipForward() // Right third
                                else -> playerViewModel.togglePlayback() // Center
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (currentBook.coverUrl != null) {
                AsyncImage(
                    model = currentBook.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(), // No clip, no rounded corners
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
                        Icons.Default.Headphones,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(100.dp)
                    )
                }
            }
        }

        // === BOOK PROGRESS ROW ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTimeCompact(pos), color = Color.White, fontSize = 13.sp)
            Text("$currentChapter / $totalChapters", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("-${formatTimeCompact(dur - pos)}", color = Color.White, fontSize = 13.sp)
        }

        // === CHAPTER TITLE ===
        Text(
            text = if (hasRealChapters && currentChapter > 0 && currentChapter <= chapters.size)
                chapters[currentChapter - 1].title
            else String.format("%03d", currentChapter),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(8.dp))

        // === CHAPTER PROGRESS SLIDER with circular indicator ===
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Circular progress indicator for chapter - neutral white
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { chapterProgress },
                        modifier = Modifier.size(48.dp),
                        color = Color.White,
                        strokeWidth = 4.dp,
                        trackColor = Color(0xFF3C3C3E),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        "${(chapterProgress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Slider - neutral white
            Slider(
                value = totalProgress,
                onValueChange = { playerViewModel.seekTo((it * dur).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0xFF3C3C3E)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(chapterPosition), color = Color.Gray, fontSize = 12.sp)
                Text("-${formatTime(chapterDuration - chapterPosition)}", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.weight(1f))

        // === CONTROL ROW ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed - no highlight when normal
            ControlButton(
                text = "${playbackSpeed}x",
                onClick = { showSpeedDialog = true }
            )

            // Skip back
            IconButton(
                onClick = { playerViewModel.skipBack() },
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    Icons.Rounded.Replay10,
                    contentDescription = "Rewind",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Play/Pause - round button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable { playerViewModel.togglePlayback() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Skip forward
            IconButton(
                onClick = { playerViewModel.skipForward() },
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    Icons.Rounded.Forward10,
                    contentDescription = "Forward",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Sleep timer - clock with Z (Snooze icon), show time if active
            ControlButton(
                icon = Icons.Default.Snooze,
                text = if (sleepTimerMinutes != null) "${sleepTimerMinutes}m" else null,
                showActiveState = sleepTimerMinutes != null,
                onClick = { showSleepDialog = true }
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    // === DIALOGS ===

    // Speed Dialog - neutral styling
    if (showSpeedDialog) {
        ReverieDialog("Playback Speed", { showSpeedDialog = false }) {
            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f).forEach { speed ->
                val isSelected = playbackSpeed == speed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            playerViewModel.setPlaybackSpeed(speed)
                            showSpeedDialog = false
                        }
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${speed}x" + if (speed == 1.0f) " (Normal)" else "",
                        color = if (isSelected) Color.White else Color.Gray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    // Sleep Timer Dialog
    if (showSleepDialog) {
        ReverieDialog("Sleep Timer", { showSleepDialog = false }) {
            if (sleepTimerMinutes != null) {
                Button(
                    onClick = {
                        playerViewModel.cancelSleepTimer()
                        showSleepDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel Timer (${sleepTimerMinutes}m left)", color = Color.White)
                }
                Spacer(Modifier.height(12.dp))
            }

            listOf(15, 30, 45, 60, 90).forEach { minutes ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            playerViewModel.setSleepTimer(minutes)
                            showSleepDialog = false
                        }
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Snooze, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("$minutes Minutes", color = Color.White)
                }
            }
        }
    }

    // Chapters & Bookmarks Dialog - neutral styling
    if (showChaptersDialog) {
        ReverieDialog("Chapters & Bookmarks", { showChaptersDialog = false }) {
            // Tab for chapters vs bookmarks
            var selectedTab by remember { mutableIntStateOf(0) }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Chapters", color = if (selectedTab == 0) Color.White else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Bookmarks", color = if (selectedTab == 1) Color.White else Color.Gray) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Chapters list
                    if (hasRealChapters) {
                        chapters.forEachIndexed { index, chapter ->
                            val chapterNum = index + 1
                            val isCurrentCh = chapterNum == currentChapter
                            ChapterItem(
                                title = chapter.title,
                                duration = formatTimeCompact(chapter.endMs - chapter.startMs),
                                isCurrent = isCurrentCh,
                                onClick = {
                                    playerViewModel.seekTo(chapter.startMs)
                                    showChaptersDialog = false
                                }
                            )
                        }
                    } else {
                        (1..minOf(totalChapters, 30)).forEach { chapterNum ->
                            val isCurrentCh = chapterNum == currentChapter
                            ChapterItem(
                                title = "Chapter ${String.format("%03d", chapterNum)}",
                                duration = formatTimeCompact(chapterDuration),
                                isCurrent = isCurrentCh,
                                onClick = {
                                    val startPos = ((chapterNum - 1).toFloat() / totalChapters) * dur
                                    playerViewModel.seekTo(startPos.toLong())
                                    showChaptersDialog = false
                                }
                            )
                        }
                    }
                }
                1 -> {
                    // Bookmarks list
                    val bookmarks = currentBook.bookmarks
                    if (bookmarks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.BookmarkBorder, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("No bookmarks yet", color = Color.Gray)
                                Text("Add one from the menu", color = Color.DarkGray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        bookmarks.forEachIndexed { index, positionMs ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerViewModel.seekTo(positionMs)
                                        showChaptersDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bookmark, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Bookmark ${index + 1}", color = Color.White, fontWeight = FontWeight.Medium)
                                    Text(formatTimeCompact(positionMs), color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterItem(
    title: String,
    duration: String,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (isCurrent) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                title,
                color = if (isCurrent) Color.White else Color.Gray,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(duration, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun ControlButton(
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    showActiveState: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0xFF2C2C2E))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (showActiveState && text != null) {
            // Show timer remaining
            Text(
                text = text,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        } else if (text != null) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        } else if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun formatTimeCompact(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) String.format("%d h %d min", hours, minutes) else String.format("%d min", minutes)
}
