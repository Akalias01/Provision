package com.mossglen.lithos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mossglen.lithos.data.Book
import com.mossglen.lithos.ui.theme.*
import com.mossglen.lithos.ui.viewmodel.LibraryViewModel

// Use LithosUI design system for theme-aware colors

/**
 * Author Books Screen
 *
 * Shows all books by a specific author from the library.
 * Accessible by tapping on an author's name in book details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorBooksScreen(
    authorName: String,
    libraryViewModel: LibraryViewModel,
    isDark: Boolean = true,
    isOLED: Boolean = false,
    accentColor: Color = LithosAmber,
    onBack: () -> Unit,
    onBookClick: (String) -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val allBooks by libraryViewModel.books.collectAsState()

    // Filter books by author (case-insensitive)
    val authorBooks = remember(allBooks, authorName) {
        allBooks.filter { it.author.equals(authorName, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = authorName,
                            style = GlassTypography.Title,
                            color = theme.textPrimary
                        )
                        Text(
                            text = "${authorBooks.size} book${if (authorBooks.size != 1) "s" else ""}",
                            style = GlassTypography.Caption,
                            color = theme.textSecondary
                        )
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
        containerColor = LithosUI.background(isDark, isOLED)
    ) { padding ->
        if (authorBooks.isEmpty()) {
            // Empty state
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
                        text = "No books found by this author",
                        style = GlassTypography.Body,
                        color = theme.textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(GlassSpacing.M),
                verticalArrangement = Arrangement.spacedBy(GlassSpacing.S)
            ) {
                items(authorBooks, key = { it.id }) { book ->
                    AuthorBookItem(
                        book = book,
                        accentColor = accentColor,
                        isDark = isDark,
                        isOLED = isOLED,
                        onClick = { onBookClick(book.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthorBookItem(
    book: Book,
    accentColor: Color,
    isDark: Boolean,
    isOLED: Boolean = false,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)

    // Calculate progress
    val progress = if (book.duration > 0) {
        (book.progress.toFloat() / book.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val progressPercent = (progress * 100).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(if (isDark) LithosUI.CardBackground else LithosUI.CardBackgroundLight)
            .clickable(onClick = onClick)
            .padding(GlassSpacing.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover art
        AsyncImage(
            model = book.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(GlassShapes.Small)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(GlassSpacing.M))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = GlassTypography.Body,
                fontWeight = FontWeight.SemiBold,
                color = theme.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(GlassSpacing.XS))

            // Format & Duration
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (book.format == "AUDIO") Icons.Rounded.Headphones else Icons.Outlined.MenuBook,
                    contentDescription = null,
                    tint = theme.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(GlassSpacing.XS))
                Text(
                    text = formatDuration(book.duration),
                    style = GlassTypography.Caption,
                    color = theme.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(GlassSpacing.S))

            // Progress bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isDark) LithosUI.InactiveTrack else Color(0xFFE0E0E0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(accentColor)
                    )
                }
                Spacer(modifier = Modifier.width(GlassSpacing.S))
                Text(
                    text = "$progressPercent%",
                    style = GlassTypography.Caption,
                    color = if (progressPercent > 0) accentColor else theme.textTertiary
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalMinutes = ms / 1000 / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "0m"
    }
}
