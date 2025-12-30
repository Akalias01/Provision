package com.mossglen.reverie.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mossglen.reverie.ui.theme.*
import com.mossglen.reverie.ui.viewmodel.LibraryViewModel
import com.mossglen.reverie.ui.viewmodel.PlayerViewModel

/**
 * Bookmarks Screen
 *
 * Shows all bookmarks for a specific book.
 * Allows quick navigation to saved positions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    bookId: String,
    isDark: Boolean = true,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.ReverieAccent,
    onBack: () -> Unit,
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

    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkToDelete by remember { mutableStateOf<Long?>(null) }
    var bookmarkToEdit by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Bookmarks",
                            style = GlassTypography.Title,
                            color = theme.textPrimary
                        )
                        book?.let {
                            Text(
                                text = "${it.bookmarks.size} bookmark${if (it.bookmarks.size != 1) "s" else ""}",
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
                actions = {
                    // Add bookmark button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showAddBookmarkDialog = true
                        }
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Add Bookmark",
                            tint = accentColor
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
                        Icons.Outlined.BookmarkBorder,
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
        } else if (book.bookmarks.isEmpty()) {
            // No bookmarks
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = GlassSpacing.XL)
                ) {
                    Icon(
                        Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = theme.textTertiary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.M))
                    Text(
                        text = "No bookmarks yet",
                        style = GlassTypography.Body,
                        color = theme.textSecondary
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.S))
                    Text(
                        text = "Tap the + button to add a bookmark at your current position",
                        style = GlassTypography.Caption,
                        color = theme.textTertiary
                    )
                }
            }
        } else {
            // Bookmark list - sorted by position
            val sortedBookmarks = book.bookmarks.sorted()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(GlassSpacing.M),
                verticalArrangement = Arrangement.spacedBy(GlassSpacing.S)
            ) {
                itemsIndexed(sortedBookmarks, key = { index, bookmark -> bookmark }) { index, bookmarkPosition ->
                    BookmarkItem(
                        bookmarkPosition = bookmarkPosition,
                        bookDuration = book.duration,
                        note = book.bookmarkNotes[bookmarkPosition],
                        index = index + 1,
                        accentColor = accentColor,
                        isDark = isDark,
                        isReverieDark = isReverieDark,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            // Load book if not current, then seek to bookmark
                            if (currentBook?.id != bookId) {
                                playerViewModel.loadBook(book)
                            }
                            playerViewModel.seekTo(bookmarkPosition)
                            if (!isPlaying) {
                                playerViewModel.togglePlayback()
                            }
                        },
                        onDelete = {
                            bookmarkToDelete = bookmarkPosition
                        },
                        onEditNote = {
                            bookmarkToEdit = bookmarkPosition
                        }
                    )
                }
            }
        }
    }

    // Add Bookmark Dialog
    if (showAddBookmarkDialog) {
        AddBookmarkDialog(
            currentPosition = if (currentBook?.id == bookId) position else book?.progress ?: 0L,
            isDark = isDark,
            isReverieDark = isReverieDark,
            accentColor = accentColor,
            onConfirm = { bookmarkPosition, note ->
                book?.let {
                    libraryViewModel.addBookmarkWithNote(it.id, bookmarkPosition, note)
                }
                showAddBookmarkDialog = false
            },
            onDismiss = { showAddBookmarkDialog = false }
        )
    }

    // Delete Confirmation Dialog
    bookmarkToDelete?.let { bookmarkPos ->
        DeleteBookmarkDialog(
            isDark = isDark,
            isReverieDark = isReverieDark,
            accentColor = accentColor,
            onConfirm = {
                book?.let {
                    libraryViewModel.deleteBookmark(it.id, bookmarkPos)
                }
                bookmarkToDelete = null
            },
            onDismiss = { bookmarkToDelete = null }
        )
    }

    // Edit Note Dialog
    bookmarkToEdit?.let { bookmarkPos ->
        book?.let {
            EditNoteDialog(
                currentNote = it.bookmarkNotes[bookmarkPos] ?: "",
                isDark = isDark,
                isReverieDark = isReverieDark,
                accentColor = accentColor,
                onConfirm = { newNote ->
                    libraryViewModel.updateBookmarkNote(it.id, bookmarkPos, newNote)
                    bookmarkToEdit = null
                },
                onDismiss = { bookmarkToEdit = null }
            )
        }
    }
}

// ============================================================================
// BOOKMARK ITEM
// ============================================================================

@Composable
private fun BookmarkItem(
    bookmarkPosition: Long,
    bookDuration: Long,
    note: String?,
    index: Int,
    accentColor: Color,
    isDark: Boolean,
    isReverieDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEditNote: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current

    val progress = if (bookDuration > 0) {
        (bookmarkPosition.toFloat() / bookDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(theme.glassSecondary)
            .clickable(onClick = onClick)
            .padding(GlassSpacing.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bookmark number
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Bookmark,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(GlassSpacing.M))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bookmark $index",
                    style = GlassTypography.Body,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textPrimary
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = GlassTypography.Caption,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatDuration(bookmarkPosition),
                style = GlassTypography.Caption,
                color = theme.textSecondary
            )

            // Display note if present
            if (!note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(GlassSpacing.S))
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
                ) {
                    Icon(
                        Icons.Rounded.Notes,
                        contentDescription = null,
                        tint = theme.textTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = note,
                        style = GlassTypography.Caption,
                        color = theme.textTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(GlassSpacing.S))

            // Mini progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
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

        Spacer(modifier = Modifier.width(GlassSpacing.S))

        // Action buttons column
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Edit note button
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onEditNote()
                }
            ) {
                Icon(
                    if (note.isNullOrBlank()) Icons.Rounded.AddComment else Icons.Rounded.Edit,
                    contentDescription = if (note.isNullOrBlank()) "Add Note" else "Edit Note",
                    tint = theme.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete button
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onDelete()
                }
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = GlassColors.Destructive,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ============================================================================
// ADD BOOKMARK DIALOG
// ============================================================================

@Composable
private fun AddBookmarkDialog(
    currentPosition: Long,
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    onConfirm: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    var customPosition by remember { mutableStateOf(currentPosition) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.glassSecondary,
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = "Add Bookmark",
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Create a bookmark at your current position",
                    style = GlassTypography.Body,
                    color = theme.textSecondary
                )

                Spacer(modifier = Modifier.height(GlassSpacing.M))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GlassShapes.Small))
                        .background(theme.glassSecondary)
                        .padding(GlassSpacing.M),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Position",
                        style = GlassTypography.Caption,
                        color = theme.textSecondary
                    )
                    Text(
                        text = formatDuration(currentPosition),
                        style = GlassTypography.Body,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.height(GlassSpacing.M))

                // Note input field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = {
                        Text(
                            "Note (optional)",
                            style = GlassTypography.Caption
                        )
                    },
                    placeholder = {
                        Text(
                            "Add a note to this bookmark...",
                            style = GlassTypography.Caption
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GlassShapes.Small),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = theme.textTertiary.copy(alpha = 0.3f),
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = theme.textSecondary,
                        cursorColor = accentColor
                    ),
                    textStyle = GlassTypography.Body,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(currentPosition, note) }
            ) {
                Text(
                    "Add",
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = theme.textSecondary
                )
            }
        }
    )
}

// ============================================================================
// DELETE BOOKMARK DIALOG
// ============================================================================

@Composable
private fun DeleteBookmarkDialog(
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.glassSecondary,
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = "Delete Bookmark",
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete this bookmark? This action cannot be undone.",
                style = GlassTypography.Body,
                color = theme.textSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Delete",
                    color = GlassColors.Destructive,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = theme.textSecondary
                )
            }
        }
    )
}

// ============================================================================
// EDIT NOTE DIALOG
// ============================================================================

@Composable
private fun EditNoteDialog(
    currentNote: String,
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    var note by remember { mutableStateOf(currentNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.glassSecondary,
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = if (currentNote.isBlank()) "Add Note" else "Edit Note",
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = if (currentNote.isBlank())
                        "Add a note to this bookmark to help you remember what's important at this moment."
                    else
                        "Edit your bookmark note.",
                    style = GlassTypography.Body,
                    color = theme.textSecondary
                )

                Spacer(modifier = Modifier.height(GlassSpacing.M))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = {
                        Text(
                            "Note",
                            style = GlassTypography.Caption
                        )
                    },
                    placeholder = {
                        Text(
                            "Enter your note here...",
                            style = GlassTypography.Caption
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GlassShapes.Small),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = theme.textTertiary.copy(alpha = 0.3f),
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = theme.textSecondary,
                        cursorColor = accentColor
                    ),
                    textStyle = GlassTypography.Body,
                    maxLines = 5,
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(note) }
            ) {
                Text(
                    "Save",
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = theme.textSecondary
                )
            }
        }
    )
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
