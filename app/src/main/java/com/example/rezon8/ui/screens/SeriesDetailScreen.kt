package com.mossglen.lithos.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mossglen.lithos.data.Book
import com.mossglen.lithos.data.Series
import com.mossglen.lithos.data.SeriesInfo
import com.mossglen.lithos.data.SeriesBookInfo
import com.mossglen.lithos.data.SeriesMetadata
import com.mossglen.lithos.data.getSeriesInfo
import com.mossglen.lithos.haptics.HapticType
import com.mossglen.lithos.haptics.performHaptic
import com.mossglen.lithos.ui.components.*
import com.mossglen.lithos.ui.theme.*
import com.mossglen.lithos.ui.viewmodel.SeriesViewModel
import java.util.concurrent.TimeUnit

// Lithos Amber Design Language Colors
private val LithosAmber = Color(0xFFD48C2C)
private val LithosMoss = Color(0xFF4A5D45)
private val LithosSlate = Color(0xFF1A1D21)
private val LithosGlassBackground = Color(0xD91A1D21) // rgba(26, 29, 33, 0.85)

/**
 * REVERIE Glass - Series Detail Screen
 *
 * Shows all books in a series with:
 * - Premium cover collage header
 * - Series progress tracking
 * - Books ordered by series number
 * - Quick navigation to books
 * - Series management options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    seriesName: String,
    isDark: Boolean = true,
    isOLED: Boolean = false,
    accentColor: Color = LithosAmber,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onPlayBook: (Book) -> Unit = {},
    onEditSeries: (String) -> Unit = {},
    seriesViewModel: SeriesViewModel = hiltViewModel()
) {
    val theme = glassTheme(isDark, isOLED)
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Get series data
    val series by seriesViewModel.getSeriesByName(seriesName).collectAsState()

    // Get series metadata from external sources
    val seriesMetadata by seriesViewModel.getSeriesMetadata(seriesName).collectAsState()

    // Loading and message states
    val isLoadingMetadata by seriesViewModel.isLoadingMetadata.collectAsState()
    val metadataMessage by seriesViewModel.metadataMessage.collectAsState()
    val excludedBooks by seriesViewModel.excludedBooks.collectAsState()

    // Fetch series metadata when series data is available
    LaunchedEffect(seriesName, series) {
        if (series != null) {
            val author = series!!.books.firstOrNull()?.author
            seriesViewModel.fetchSeriesMetadata(seriesName, author)
        }
    }

    // Show snackbar for metadata messages
    LaunchedEffect(metadataMessage) {
        metadataMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            seriesViewModel.clearMetadataMessage()
        }
    }

    // Dialog states
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var selectedMissingBook by remember { mutableStateOf<SeriesBookInfo?>(null) }
    var bookToRemove by remember { mutableStateOf<SeriesBookInfo?>(null) }
    var selectedOwnedBook by remember { mutableStateOf<Book?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LithosSlate)
    ) {
        // SnackbarHost for metadata feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = LithosGlassBackground,
                contentColor = LithosAmber
            )
        }

        if (series == null) {
            // Loading or series not found
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = theme.textSecondary)
                    Spacer(modifier = Modifier.height(GlassSpacing.M))
                    Text(
                        text = "Loading series...",
                        style = GlassTypography.Body,
                        color = theme.textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = GlassSpacing.XXXL)
            ) {
                // Header
                item {
                    SeriesHeader(
                        series = series!!,
                        isDark = isDark,
                        isOLED = isOLED,
                        accentColor = accentColor,
                        onBackClick = {
                            view.performHaptic(HapticType.LightTap)
                            onBackClick()
                        },
                        onOptionsClick = {
                            view.performHaptic(HapticType.LightTap)
                            showOptionsMenu = true
                        }
                    )
                }

                // Series Stats Card
                item {
                    SeriesStatsCard(
                        series = series!!,
                        isDark = isDark,
                        isOLED = isOLED,
                        accentColor = accentColor
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.M))
                }

                // Series Metadata Card or Loading indicator
                if (isLoadingMetadata) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = GlassSpacing.M)
                                .clip(RoundedCornerShape(GlassShapes.Large))
                                .background(LithosGlassBackground)
                                .padding(GlassSpacing.L),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = LithosAmber,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Fetching series info...",
                                    style = GlassTypography.Body,
                                    color = theme.textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(GlassSpacing.M))
                    }
                } else if (seriesMetadata != null) {
                    item {
                        SeriesMetadataCard(
                            metadata = seriesMetadata!!,
                            ownedCount = series!!.bookCount,
                            isDark = isDark,
                            isOLED = isOLED,
                            accentColor = accentColor
                        )
                        Spacer(modifier = Modifier.height(GlassSpacing.M))
                    }
                }

                // Books Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = GlassSpacing.M, vertical = GlassSpacing.S),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Books",
                            style = GlassTypography.Headline,
                            color = theme.textPrimary
                        )
                        Text(
                            text = if (seriesMetadata != null) {
                                "${series!!.bookCount} of ${seriesMetadata!!.totalBooks}"
                            } else {
                                "${series!!.bookCount} books"
                            },
                            style = GlassTypography.Caption,
                            color = if (seriesMetadata != null && series!!.bookCount < seriesMetadata!!.totalBooks) {
                                accentColor
                            } else {
                                theme.textSecondary
                            }
                        )
                    }
                }

                // Books List
                items(series!!.booksSorted, key = { it.id }) { book ->
                    SeriesBookItem(
                        book = book,
                        isDark = isDark,
                        isOLED = isOLED,
                        accentColor = accentColor,
                        onClick = { onBookClick(book.id) },
                        onCoverClick = {
                            view.performHaptic(HapticType.LightTap)
                            selectedOwnedBook = book
                        },
                        onPlayClick = { onPlayBook(book) }
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.S))
                }

                // Missing Books Section - Show books from series not in library
                if (seriesMetadata != null && seriesMetadata!!.books.isNotEmpty()) {
                    val ownedTitles = series!!.books.map { it.title.lowercase() }.toSet()
                    val excludedTitles = excludedBooks[seriesName] ?: emptySet()
                    val missingBooks = seriesMetadata!!.books.filter { bookInfo ->
                        val titleLower = bookInfo.title.lowercase()
                        // Not owned
                        !ownedTitles.any { owned ->
                            owned.contains(titleLower) || titleLower.contains(owned)
                        } &&
                        // Not excluded by user
                        !excludedTitles.contains(titleLower)
                    }

                    if (missingBooks.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(GlassSpacing.L))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = GlassSpacing.M),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Not in Library",
                                    style = GlassTypography.Headline,
                                    color = theme.textSecondary
                                )
                                Text(
                                    text = "${missingBooks.size} books",
                                    style = GlassTypography.Caption,
                                    color = accentColor
                                )
                            }
                            Spacer(modifier = Modifier.height(GlassSpacing.S))
                        }

                        items(missingBooks, key = { it.title }) { bookInfo ->
                            MissingBookItem(
                                bookInfo = bookInfo,
                                isDark = isDark,
                                isOLED = isOLED,
                                accentColor = accentColor,
                                onClick = {
                                    view.performHaptic(HapticType.LightTap)
                                    selectedMissingBook = bookInfo
                                },
                                onLongPress = {
                                    view.performHaptic(HapticType.HeavyTap)
                                    bookToRemove = bookInfo
                                }
                            )
                            Spacer(modifier = Modifier.height(GlassSpacing.S))
                        }
                    }
                }

                // Related Content Section - Comics, companions, etc.
                if (seriesMetadata != null && seriesMetadata!!.relatedContent.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(GlassSpacing.L))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = GlassSpacing.M),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Related Content",
                                style = GlassTypography.Headline,
                                color = theme.textSecondary
                            )
                            Text(
                                text = "${seriesMetadata!!.relatedContent.size} items",
                                style = GlassTypography.Caption,
                                color = theme.textSecondary.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.height(GlassSpacing.XS))
                        Text(
                            text = "Comics, companions & more set in this world",
                            style = GlassTypography.Caption,
                            color = theme.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = GlassSpacing.M)
                        )
                        Spacer(modifier = Modifier.height(GlassSpacing.S))
                    }

                    items(seriesMetadata!!.relatedContent, key = { "related_${it.title}" }) { bookInfo ->
                        RelatedContentItem(
                            bookInfo = bookInfo,
                            isDark = isDark,
                            isOLED = isOLED,
                            accentColor = accentColor,
                            onClick = {
                                view.performHaptic(HapticType.LightTap)
                                selectedMissingBook = bookInfo
                            }
                        )
                        Spacer(modifier = Modifier.height(GlassSpacing.S))
                    }
                }
            }
        }

        // Options Menu
        if (showOptionsMenu && series != null) {
            SeriesOptionsMenu(
                series = series!!,
                isDark = isDark,
                isOLED = isOLED,
                onDismiss = { showOptionsMenu = false },
                onRefresh = {
                    view.performHaptic(HapticType.LightTap)
                    val author = series!!.books.firstOrNull()?.author
                    seriesViewModel.refreshSeriesMetadata(seriesName, author)
                    showOptionsMenu = false
                },
                onEdit = {
                    showOptionsMenu = false
                    onEditSeries(seriesName)
                },
                onMarkAsFinished = {
                    view.performHaptic(HapticType.Confirm)
                    seriesViewModel.markSeriesAsFinished(seriesName, true)
                    showOptionsMenu = false
                },
                onMarkAsUnread = {
                    view.performHaptic(HapticType.Confirm)
                    seriesViewModel.markSeriesAsFinished(seriesName, false)
                    showOptionsMenu = false
                },
                onDelete = {
                    showOptionsMenu = false
                    showDeleteConfirmation = true
                }
            )
        }

        // Delete Confirmation
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                containerColor = LithosGlassBackground,
                shape = RoundedCornerShape(GlassShapes.Medium),
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = GlassColors.Destructive
                    )
                },
                title = {
                    Text(
                        "Delete Series?",
                        style = GlassTypography.Headline,
                        color = theme.textPrimary
                    )
                },
                text = {
                    Text(
                        "All ${series?.bookCount ?: 0} books in \"$seriesName\" will be permanently removed. This cannot be undone.",
                        style = GlassTypography.Body,
                        color = theme.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            view.performHaptic(HapticType.HeavyTap)
                            seriesViewModel.deleteSeries(seriesName)
                            showDeleteConfirmation = false
                            onBackClick()
                        }
                    ) {
                        Text("Delete", color = GlassColors.Destructive, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel", color = theme.textSecondary)
                    }
                }
            )
        }

        // Missing Book Info Dialog
        if (selectedMissingBook != null) {
            AlertDialog(
                onDismissRequest = { selectedMissingBook = null },
                containerColor = LithosGlassBackground,
                shape = RoundedCornerShape(GlassShapes.Large),
                title = {
                    Text(
                        text = selectedMissingBook!!.title,
                        style = GlassTypography.Headline,
                        color = theme.textPrimary
                    )
                },
                text = {
                    Column {
                        // Cover image
                        if (selectedMissingBook!!.coverUrl != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(GlassShapes.Medium)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = selectedMissingBook!!.coverUrl,
                                    contentDescription = selectedMissingBook!!.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.height(GlassSpacing.M))
                        }

                        // Year
                        if (selectedMissingBook!!.publishYear != null) {
                            Text(
                                text = "First published: ${selectedMissingBook!!.publishYear}",
                                style = GlassTypography.Body,
                                color = theme.textSecondary
                            )
                            Spacer(modifier = Modifier.height(GlassSpacing.S))
                        }

                        // Synopsis
                        if (!selectedMissingBook!!.synopsis.isNullOrBlank()) {
                            Text(
                                text = selectedMissingBook!!.synopsis!!,
                                style = GlassTypography.Body,
                                color = theme.textSecondary
                            )
                        } else {
                            Text(
                                text = "No synopsis available",
                                style = GlassTypography.Caption,
                                color = theme.textSecondary.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(GlassSpacing.M))

                        // Not in library badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Not in your library",
                                style = GlassTypography.Caption,
                                color = accentColor
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedMissingBook = null }) {
                        Text("Close", color = accentColor)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            view.performHaptic(HapticType.HeavyTap)
                            seriesViewModel.excludeBookFromSeries(seriesName, selectedMissingBook!!.title)
                            selectedMissingBook = null
                        }
                    ) {
                        Text("Not in Series", color = theme.textSecondary)
                    }
                }
            )
        }

        // Remove from Series Confirmation Dialog
        if (bookToRemove != null) {
            AlertDialog(
                onDismissRequest = { bookToRemove = null },
                containerColor = LithosGlassBackground,
                shape = RoundedCornerShape(GlassShapes.Medium),
                icon = {
                    Icon(
                        Icons.Default.RemoveCircleOutline,
                        contentDescription = null,
                        tint = accentColor
                    )
                },
                title = {
                    Text(
                        "Remove from Series?",
                        style = GlassTypography.Headline,
                        color = theme.textPrimary
                    )
                },
                text = {
                    Text(
                        "\"${bookToRemove!!.title}\" will be removed from the Not in Library section. This book may not actually be part of the ${seriesName} series.",
                        style = GlassTypography.Body,
                        color = theme.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            view.performHaptic(HapticType.Confirm)
                            seriesViewModel.excludeBookFromSeries(seriesName, bookToRemove!!.title)
                            bookToRemove = null
                        }
                    ) {
                        Text("Remove", color = accentColor, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { bookToRemove = null }) {
                        Text("Cancel", color = theme.textSecondary)
                    }
                }
            )
        }

        // Owned Book Synopsis Dialog
        if (selectedOwnedBook != null) {
            AlertDialog(
                onDismissRequest = { selectedOwnedBook = null },
                containerColor = LithosGlassBackground,
                shape = RoundedCornerShape(GlassShapes.Large),
                title = {
                    Text(
                        text = selectedOwnedBook!!.title,
                        style = GlassTypography.Headline,
                        color = theme.textPrimary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        // Cover image
                        if (!selectedOwnedBook!!.coverUrl.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(GlassShapes.Medium)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = selectedOwnedBook!!.coverUrl,
                                    contentDescription = selectedOwnedBook!!.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.height(GlassSpacing.M))
                        }

                        // Author
                        if (selectedOwnedBook!!.author.isNotBlank() && selectedOwnedBook!!.author != "Unknown Author") {
                            Text(
                                text = "by ${selectedOwnedBook!!.author}",
                                style = GlassTypography.Body,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.height(GlassSpacing.S))
                        }

                        // Series info
                        val seriesInfo = selectedOwnedBook!!.getSeriesInfo()
                        if (seriesInfo != null) {
                            Text(
                                text = "${seriesInfo.name} #${seriesInfo.bookNumber?.toInt() ?: "?"}",
                                style = GlassTypography.Caption,
                                color = theme.textSecondary
                            )
                            Spacer(modifier = Modifier.height(GlassSpacing.S))
                        }

                        // Synopsis/Description
                        if (selectedOwnedBook!!.synopsis.isNotBlank()) {
                            Text(
                                text = selectedOwnedBook!!.synopsis,
                                style = GlassTypography.Body,
                                color = theme.textSecondary
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No synopsis available",
                                    style = GlassTypography.Caption,
                                    color = theme.textSecondary.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(GlassSpacing.S))
                                Text(
                                    text = "Tap 'Fetch' to download from OpenLibrary",
                                    style = GlassTypography.Caption,
                                    color = accentColor.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(GlassSpacing.M))

                        // In library badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GlassColors.Success,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "In your library",
                                style = GlassTypography.Caption,
                                color = GlassColors.Success
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedOwnedBook = null }) {
                        Text("Close", color = accentColor)
                    }
                },
                dismissButton = {
                    if (selectedOwnedBook!!.synopsis.isBlank()) {
                        TextButton(
                            onClick = {
                                view.performHaptic(HapticType.LightTap)
                                seriesViewModel.fetchBookMetadata(selectedOwnedBook!!)
                                selectedOwnedBook = null
                            }
                        ) {
                            Text("Fetch", color = accentColor)
                        }
                    }
                }
            )
        }
    }
}

// ============================================================================
// SERIES HEADER - Cover collage + title
// ============================================================================

@Composable
private fun SeriesHeader(
    series: Series,
    isDark: Boolean,
    isOLED: Boolean = false,
    accentColor: Color = LithosAmber,
    onBackClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M, vertical = GlassSpacing.M)
    ) {
        // Top bar with back and options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBackClick,
                isDark = isDark,
                hasBackground = true
            )

            GlassIconButton(
                icon = Icons.Default.MoreVert,
                onClick = onOptionsClick,
                isDark = isDark,
                hasBackground = true
            )
        }

        Spacer(modifier = Modifier.height(GlassSpacing.L))

        // Cover Collage (2x2 grid if 4+ books, otherwise centered single/row) - square
        SeriesCoverCollage(
            coverUrls = series.coverUrls,
            isDark = isDark,
            isOLED = isOLED,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(1f)
        )

        Spacer(modifier = Modifier.height(GlassSpacing.L))

        // Series Title
        Text(
            text = series.name,
            style = GlassTypography.Display,
            color = theme.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(GlassSpacing.XS))

        // Book count and duration
        val totalHours = TimeUnit.MILLISECONDS.toHours(series.totalDuration)
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(series.totalDuration) % 60

        Text(
            text = "${series.bookCount} books • ${totalHours}h ${totalMinutes}m",
            style = GlassTypography.Body,
            color = theme.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================================================
// SERIES COVER COLLAGE
// ============================================================================

@Composable
private fun SeriesCoverCollage(
    coverUrls: List<String>,
    isDark: Boolean,
    isOLED: Boolean = false,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark, isOLED)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GlassShapes.Large))
            .background(theme.glassBorder),
        contentAlignment = Alignment.Center
    ) {
        when (coverUrls.size) {
            0 -> {
                // No covers - show placeholder
                Icon(
                    Icons.Outlined.MenuBook,
                    contentDescription = null,
                    tint = theme.textTertiary,
                    modifier = Modifier.size(64.dp)
                )
            }
            1 -> {
                // Single cover - centered square
                AsyncImage(
                    model = coverUrls[0],
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(GlassShapes.Medium)),
                    contentScale = ContentScale.Crop
                )
            }
            2, 3 -> {
                // 2-3 covers - horizontal row
                Row(
                    modifier = Modifier.fillMaxSize().padding(GlassSpacing.M),
                    horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S, Alignment.CenterHorizontally)
                ) {
                    coverUrls.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(GlassShapes.Small)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            else -> {
                // 4+ covers - 2x2 grid
                Column(
                    modifier = Modifier.fillMaxSize().padding(GlassSpacing.M),
                    verticalArrangement = Arrangement.spacedBy(GlassSpacing.S)
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
                    ) {
                        AsyncImage(
                            model = coverUrls[0],
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(GlassShapes.Small)),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = coverUrls[1],
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(GlassShapes.Small)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
                    ) {
                        AsyncImage(
                            model = coverUrls.getOrNull(2) ?: coverUrls[0],
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(GlassShapes.Small)),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = coverUrls.getOrNull(3) ?: coverUrls[1],
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(GlassShapes.Small)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// SERIES STATS CARD
// ============================================================================

@Composable
private fun SeriesStatsCard(
    series: Series,
    isDark: Boolean,
    isOLED: Boolean = false,
    accentColor: Color = LithosAmber
) {
    val theme = glassTheme(isDark, isOLED)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M)
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .glassCard(isDark = isDark)
            .padding(GlassSpacing.M)
    ) {
        Column {
            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Series Progress",
                    style = GlassTypography.Body,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textPrimary
                )
                Text(
                    text = "${series.finishedCount} of ${series.bookCount} finished",
                    style = GlassTypography.Caption,
                    color = theme.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(GlassSpacing.S))

            // Progress indicator
            LinearProgressIndicator(
                progress = { series.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = LithosAmber,
                trackColor = LithosSlate
            )

            Spacer(modifier = Modifier.height(GlassSpacing.M))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SeriesStatItem(
                    label = "Not Started",
                    value = (series.bookCount - series.finishedCount - series.inProgressCount).toString(),
                    isDark = isDark,
                    isOLED = isOLED
                )
                GlassDivider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp),
                    isDark = isDark
                )
                SeriesStatItem(
                    label = "In Progress",
                    value = series.inProgressCount.toString(),
                    isDark = isDark,
                    isOLED = isOLED
                )
                GlassDivider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp),
                    isDark = isDark
                )
                SeriesStatItem(
                    label = "Finished",
                    value = series.finishedCount.toString(),
                    isDark = isDark,
                    isOLED = isOLED
                )
            }
        }
    }
}

@Composable
private fun SeriesStatItem(
    label: String,
    value: String,
    isDark: Boolean,
    isOLED: Boolean = false
) {
    val theme = glassTheme(isDark, isOLED)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = GlassTypography.Title,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary
        )
        Text(
            text = label,
            style = GlassTypography.Caption,
            color = theme.textSecondary
        )
    }
}

// ============================================================================
// SERIES METADATA CARD - Shows total books in series from external sources
// ============================================================================

@Composable
private fun SeriesMetadataCard(
    metadata: SeriesMetadata,
    ownedCount: Int,
    isDark: Boolean,
    isOLED: Boolean = false,
    accentColor: Color = LithosAmber
) {
    val theme = glassTheme(isDark, isOLED)
    val missingCount = metadata.totalBooks - ownedCount

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M)
            .clip(RoundedCornerShape(GlassShapes.Large))
            .background(LithosGlassBackground)
            .padding(GlassSpacing.M)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Series Info",
                    style = GlassTypography.Headline,
                    color = theme.textPrimary
                )
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(GlassSpacing.S))

            // Collection status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (missingCount == 0) Icons.Filled.CheckCircle else Icons.Default.CollectionsBookmark,
                    contentDescription = null,
                    tint = if (missingCount == 0) Color(0xFF4CAF50) else accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(GlassSpacing.S))
                Column {
                    Text(
                        text = if (missingCount == 0) {
                            "Complete Collection!"
                        } else {
                            "You have $ownedCount of ${metadata.totalBooks} books"
                        },
                        style = GlassTypography.Body,
                        color = theme.textPrimary
                    )
                    if (missingCount > 0) {
                        Text(
                            text = "$missingCount books missing",
                            style = GlassTypography.Caption,
                            color = accentColor
                        )
                    }
                }
            }

            // Show missing book titles if any
            if (missingCount > 0 && metadata.bookTitles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(GlassSpacing.M))
                Text(
                    text = "Books in this series:",
                    style = GlassTypography.Caption,
                    color = theme.textSecondary
                )
                Spacer(modifier = Modifier.height(GlassSpacing.XS))

                // Show first few book titles
                metadata.bookTitles.take(8).forEach { title ->
                    Text(
                        text = "• $title",
                        style = GlassTypography.Caption,
                        color = theme.textSecondary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (metadata.bookTitles.size > 8) {
                    Text(
                        text = "... and ${metadata.bookTitles.size - 8} more",
                        style = GlassTypography.Caption,
                        color = theme.textSecondary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ============================================================================
// SERIES BOOK ITEM
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeriesBookItem(
    book: Book,
    isDark: Boolean,
    isOLED: Boolean = false,
    accentColor: Color = LithosAmber,
    onClick: () -> Unit,
    onCoverClick: () -> Unit = {},
    onPlayClick: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    val seriesInfo = book.getSeriesInfo()
    val bookNumber = seriesInfo?.bookNumber?.let {
        if (it % 1 == 0f) "Book ${it.toInt()}" else "Book $it"
    } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M)
            .scale(scale)
            .clip(RoundedCornerShape(GlassShapes.Small))
            .glassCard(isDark = isDark)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {}
            )
            .padding(GlassSpacing.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover - tap for synopsis
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable { onCoverClick() }
        ) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Progress overlay
            if (book.isFinished) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(theme.background.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Finished",
                        tint = GlassColors.Success,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (book.progress > 0) {
                val progressFloat = if (book.duration > 0) book.progress.toFloat() / book.duration else 0f
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(LithosSlate)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFloat)
                            .fillMaxHeight()
                            .background(LithosAmber)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(GlassSpacing.S))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            if (bookNumber.isNotEmpty()) {
                Text(
                    text = bookNumber,
                    style = GlassTypography.Caption,
                    color = LithosAmber,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = book.title,
                style = GlassTypography.Body,
                fontWeight = FontWeight.SemiBold,
                color = theme.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (book.author.isNotBlank() && book.author != "Unknown Author") {
                Text(
                    text = book.author,
                    style = GlassTypography.Caption,
                    color = theme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Play button
        GlassIconButton(
            icon = if (book.format in listOf("AUDIO", "M4B", "MP3")) {
                Icons.Outlined.Headphones
            } else {
                Icons.Outlined.MenuBook
            },
            onClick = onPlayClick,
            isDark = isDark,
            hasBackground = false
        )
    }
}



// ============================================================================
// MISSING BOOK ITEM - For books not in library
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MissingBookItem(
    bookInfo: SeriesBookInfo,
    isDark: Boolean,
    isOLED: Boolean = false,
    accentColor: Color = LithosAmber,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    val theme = glassTheme(isDark, isOLED)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M)
            .clip(RoundedCornerShape(GlassShapes.Small))
            .background(
                if (isOLED) Color(0xFF0D0D0D).copy(alpha = 0.6f)
                else if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.6f)
                else Color(0xFFF8F8F8).copy(alpha = 0.6f)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(GlassSpacing.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(theme.divider)
        ) {
            if (bookInfo.coverUrl != null) {
                AsyncImage(
                    model = bookInfo.coverUrl,
                    contentDescription = bookInfo.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = theme.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // "Not Owned" overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Not in library",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(GlassSpacing.S))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookInfo.title,
                style = GlassTypography.Body,
                fontWeight = FontWeight.Medium,
                color = theme.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Not in Library",
                    style = GlassTypography.Caption,
                    color = accentColor.copy(alpha = 0.7f)
                )
                if (bookInfo.publishYear != null) {
                    Text(
                        text = "• ${bookInfo.publishYear}",
                        style = GlassTypography.Caption,
                        color = theme.textSecondary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ============================================================================
// RELATED CONTENT ITEM - For comics, companions, etc.
// ============================================================================

@Composable
private fun RelatedContentItem(
    bookInfo: SeriesBookInfo,
    isDark: Boolean,
    isOLED: Boolean = false,
    accentColor: Color = LithosAmber,
    onClick: () -> Unit = {}
) {
    val theme = glassTheme(isDark, isOLED)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M)
            .clip(RoundedCornerShape(GlassShapes.Small))
            .background(
                if (isOLED) Color(0xFF0D0D0D).copy(alpha = 0.4f)
                else if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.4f)
                else Color(0xFFF8F8F8).copy(alpha = 0.4f)
            )
            .clickable { onClick() }
            .padding(GlassSpacing.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(theme.divider)
        ) {
            if (bookInfo.coverUrl != null) {
                AsyncImage(
                    model = bookInfo.coverUrl,
                    contentDescription = bookInfo.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = theme.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(GlassSpacing.S))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookInfo.title,
                style = GlassTypography.Caption,
                fontWeight = FontWeight.Medium,
                color = theme.textSecondary.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Content type badge
                Text(
                    text = bookInfo.contentType,
                    style = GlassTypography.Caption,
                    color = theme.textSecondary.copy(alpha = 0.5f)
                )
                if (bookInfo.publishYear != null) {
                    Text(
                        text = "• ${bookInfo.publishYear}",
                        style = GlassTypography.Caption,
                        color = theme.textSecondary.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ============================================================================
// SERIES OPTIONS MENU
// ============================================================================

@Composable
private fun SeriesOptionsMenu(
    series: Series,
    isDark: Boolean,
    isOLED: Boolean = false,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onMarkAsFinished: () -> Unit,
    onMarkAsUnread: () -> Unit,
    onDelete: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LithosGlassBackground,
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = series.name,
                style = GlassTypography.Headline,
                color = theme.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                SeriesMenuItem(
                    icon = Icons.Default.Refresh,
                    text = "Refresh Series Info",
                    iconTint = LithosAmber,
                    textColor = theme.textPrimary,
                    onClick = onRefresh
                )
                SeriesMenuItem(
                    icon = Icons.Default.Edit,
                    text = "Edit Series",
                    iconTint = theme.textSecondary,
                    textColor = theme.textPrimary,
                    onClick = onEdit
                )
                SeriesMenuItem(
                    icon = Icons.Default.CheckCircle,
                    text = "Mark All as Finished",
                    iconTint = GlassColors.Success,
                    textColor = theme.textPrimary,
                    onClick = onMarkAsFinished
                )
                SeriesMenuItem(
                    icon = Icons.Default.RadioButtonUnchecked,
                    text = "Mark All as Unread",
                    iconTint = theme.textSecondary,
                    textColor = theme.textPrimary,
                    onClick = onMarkAsUnread
                )
                HorizontalDivider(
                    color = theme.glassBorder,
                    modifier = Modifier.padding(vertical = GlassSpacing.XS)
                )
                SeriesMenuItem(
                    icon = Icons.Default.Delete,
                    text = "Delete Series",
                    iconTint = GlassColors.Destructive,
                    textColor = GlassColors.Destructive,
                    onClick = onDelete
                )
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun SeriesMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    iconTint: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Small))
            .clickable { onClick() }
            .padding(vertical = GlassSpacing.S, horizontal = GlassSpacing.XS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(GlassSpacing.S))
        Text(
            text = text,
            style = GlassTypography.Body,
            color = textColor
        )
    }
}
