package com.example.rezon8.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import android.widget.Toast
import com.example.rezon8.ui.theme.GlassColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.rezon8.data.Book
import com.example.rezon8.data.MetadataRepository
import com.example.rezon8.data.getSeriesInfo
import com.example.rezon8.ui.viewmodel.CoverArtViewModel
import com.example.rezon8.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun BookDetailScreen(
    bookId: String,
    accentColor: Color,
    isRezonDark: Boolean = false,
    onBack: () -> Unit,
    onPlayBook: (Book) -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onSeriesClick: (String) -> Unit = {},
    onSplitBook: (String) -> Unit = {},
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    coverArtViewModel: CoverArtViewModel = hiltViewModel()
) {
    val view = LocalView.current
    val context = view.context
    val books by libraryViewModel.books.collectAsState()
    val book = books.find { it.id == bookId }

    // Coroutine scope for metadata operations
    val scope = rememberCoroutineScope()

    // Menu state
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showMarkAsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCoverPicker by remember { mutableStateOf(false) }
    var isFetchingMetadata by remember { mutableStateOf(false) }

    // Theme colors - use passed accentColor which is already dynamic
    val menuBg = if (isRezonDark) Color(0xFF0A0A0A) else Color(0xFF2C2C2E)
    val menuTextColor = if (isRezonDark) GlassColors.RezonTextPrimary else Color.White
    val menuIconColor = accentColor
    val destructiveColor = GlassColors.Destructive

    // Swipe to go back
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 150f
    val animatedOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipeOffset"
    )
    val swipeProgress = (swipeOffset / swipeThreshold).coerceIn(0f, 1f)

    BackHandler { onBack() }

    if (book == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Book not found", color = Color.Gray)
        }
        return
    }

    // Calculate progress percentage
    val progressPercent = if (book.duration > 0) {
        ((book.progress.toFloat() / book.duration) * 100).toInt()
    } else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
            .alpha(1f - swipeProgress * 0.3f)
            .background(Color(0xFF0A0A0A))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffset > swipeThreshold) {
                            onBack()
                        } else {
                            swipeOffset = 0f
                        }
                    },
                    onDragCancel = { swipeOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset = (swipeOffset + dragAmount).coerceAtLeast(0f)
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Cover Art with overlaid controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Square cover
                ) {
                    // Cover image
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
                                Icons.Default.Book,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier.size(100.dp)
                            )
                        }
                    }

                    // Top gradient for icons
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                                )
                            )
                    )

                    // Back button - lowered for status bar clearance
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onBack()
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(start = 4.dp, top = 4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    // Three-dot menu - Library management features
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(end = 4.dp, top = 4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showOverflowMenu = true
                            }
                        ) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White
                            )
                        }

                        // Dropdown Menu with library management features
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier
                                .background(menuBg)
                                .widthIn(min = 220.dp)
                        ) {
                            // Edit
                            DropdownMenuItem(
                                text = { Text("Edit", color = menuTextColor) },
                                onClick = {
                                    showOverflowMenu = false
                                    showEditDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Edit, null, tint = menuIconColor)
                                }
                            )

                            // Change Cover
                            DropdownMenuItem(
                                text = { Text("Change Cover", color = menuTextColor) },
                                onClick = {
                                    showOverflowMenu = false
                                    showCoverPicker = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Image, null, tint = menuIconColor)
                                }
                            )

                            // Fetch Metadata
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isFetchingMetadata) "Fetching..." else "Fetch Metadata",
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
                                                Toast.makeText(context, "Failed to fetch metadata", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isFetchingMetadata = false
                                            }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    if (isFetchingMetadata) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = menuIconColor,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Rounded.Refresh, null, tint = menuIconColor)
                                    }
                                },
                                enabled = !isFetchingMetadata
                            )

                            // Add to Series
                            DropdownMenuItem(
                                text = { Text("Add to Series", color = menuTextColor) },
                                onClick = {
                                    showOverflowMenu = false
                                    Toast.makeText(context, "Add to Series coming soon", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.AddCircleOutline, null, tint = menuIconColor)
                                }
                            )

                            // Mark as...
                            DropdownMenuItem(
                                text = { Text("Mark as...", color = menuTextColor) },
                                onClick = {
                                    showOverflowMenu = false
                                    showMarkAsDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.CheckCircleOutline, null, tint = menuIconColor)
                                }
                            )

                            // Divider
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Split Book
                            DropdownMenuItem(
                                text = { Text("Split Book", color = menuTextColor) },
                                onClick = {
                                    showOverflowMenu = false
                                    onSplitBook(book.id)
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.CallSplit, null, tint = menuIconColor)
                                }
                            )

                            // Merge Books
                            DropdownMenuItem(
                                text = { Text("Merge Books", color = menuTextColor) },
                                onClick = {
                                    showOverflowMenu = false
                                    Toast.makeText(context, "Merge Books coming soon", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.CallMerge, null, tint = menuIconColor)
                                }
                            )

                            // Divider
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Share
                            DropdownMenuItem(
                                text = { Text("Share", color = menuTextColor) },
                                onClick = {
                                    showOverflowMenu = false
                                    Toast.makeText(context, "Share coming soon", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Share, null, tint = menuIconColor)
                                }
                            )

                            // Send Files
                            DropdownMenuItem(
                                text = { Text("Send Files", color = menuTextColor) },
                                onClick = {
                                    showOverflowMenu = false
                                    Toast.makeText(context, "Send Files coming soon", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Share, null, tint = menuIconColor)
                                }
                            )

                            // Divider
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Delete Files
                            DropdownMenuItem(
                                text = { Text("Delete Files", color = destructiveColor) },
                                onClick = {
                                    showOverflowMenu = false
                                    Toast.makeText(context, "Delete Files coming soon", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.DeleteSweep, null, tint = destructiveColor)
                                }
                            )

                            // Delete Book
                            DropdownMenuItem(
                                text = { Text("Delete Book", color = destructiveColor) },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Delete, null, tint = destructiveColor)
                                }
                            )
                        }
                    }

                    // Headphone FAB - Play button (dark background, accent icon - matches player)
                    FloatingActionButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onPlayBook(book)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(56.dp),
                        containerColor = Color(0xFF1C1C1E),
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Headphones,
                            contentDescription = "Play",
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Info row: Progress • Year • Duration • Size • Source
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Progress with checkmark
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = if (progressPercent > 0) accentColor else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$progressPercent%",
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    Text(" • ", color = Color.Gray, fontSize = 14.sp)

                    // Year (placeholder)
                    Text(
                        "2024",
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    Text(" • ", color = Color.Gray, fontSize = 14.sp)

                    // Duration
                    Text(
                        formatDuration(book.duration),
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    // Source icon (if from Audible, etc.)
                    if (book.format == "AUDIO") {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Headphones,
                            null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Missing Metadata Banner
                if (book.hasIncompleteMetadata) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3A3A3C))
                            .clickable {
                                isFetchingMetadata = true
                                scope.launch {
                                    try {
                                        libraryViewModel.fetchMetadata(book)
                                        Toast.makeText(context, "Metadata updated", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to fetch metadata", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isFetchingMetadata = false
                                    }
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFA500),
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Incomplete Metadata",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Missing: ${book.missingMetadataFields.joinToString(", ")}",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                        if (isFetchingMetadata) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Fetch Metadata",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Title
                Text(
                    text = book.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(4.dp))

                // Author (clickable, accent color)
                Text(
                    text = book.author,
                    color = accentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable {
                            if (book.author.isNotBlank() && book.author != "Unknown Author") {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onAuthorClick(book.author)
                            }
                        }
                )

                // Series info (if book belongs to a series)
                val seriesInfo = book.getSeriesInfo()
                if (seriesInfo != null) {
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onSeriesClick(seriesInfo.name)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoStories,
                            contentDescription = "Series",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = seriesInfo.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (seriesInfo.bookNumber != null) {
                                val numberText = if (seriesInfo.bookNumber % 1 == 0f) {
                                    "Book ${seriesInfo.bookNumber.toInt()}"
                                } else {
                                    "Book ${seriesInfo.bookNumber}"
                                }
                                Text(
                                    text = numberText,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "View series",
                            tint = Color.LightGray,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(180f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Synopsis
                Text(
                    text = book.synopsis.ifEmpty { "No synopsis available for this book." },
                    color = Color.LightGray,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(140.dp)) // Space for mini player from MainLayoutGlass
            }
        }

        // Mark As Dialog
        if (showMarkAsDialog) {
            AlertDialog(
                onDismissRequest = { showMarkAsDialog = false },
                containerColor = menuBg,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        "Mark as...",
                        color = menuTextColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        listOf(
                            "Not Started" to Icons.Rounded.RadioButtonUnchecked,
                            "In Progress" to Icons.Rounded.PlayCircleOutline,
                            "Finished" to Icons.Rounded.CheckCircle
                        ).forEach { (status, icon) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showMarkAsDialog = false
                                        Toast.makeText(context, "Marked as $status", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(icon, null, tint = menuIconColor)
                                Text(status, color = menuTextColor)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = menuBg,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(
                        Icons.Rounded.Delete,
                        null,
                        tint = destructiveColor,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        "Delete Book?",
                        color = menuTextColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "This will permanently remove the book from your library. This action cannot be undone.",
                        color = menuTextColor.copy(alpha = 0.7f)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        Toast.makeText(context, "Book deleted", Toast.LENGTH_SHORT).show()
                        onBack()
                    }) {
                        Text("Delete", color = destructiveColor, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = menuTextColor.copy(alpha = 0.6f))
                    }
                }
            )
        }

        // Edit Dialog - TODO: Full implementation
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                containerColor = menuBg,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("Edit Book", color = menuTextColor, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Full metadata editor coming soon. This will include cover art search from web and Google Play Books.",
                        color = menuTextColor.copy(alpha = 0.7f)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("OK", color = menuIconColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        // Cover Art Picker Dialog
        if (showCoverPicker) {
            CoverArtPickerDialog(
                currentCoverUrl = book.coverUrl,
                bookTitle = book.title,
                bookAuthor = book.author,
                accentColor = accentColor,
                isRezonDark = isRezonDark,
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

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return if (hours > 0) {
        "${hours} h ${minutes} min"
    } else {
        "${minutes} min"
    }
}

