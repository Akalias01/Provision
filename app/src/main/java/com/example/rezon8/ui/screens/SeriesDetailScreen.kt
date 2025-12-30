package com.mossglen.reverie.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.mossglen.reverie.data.Book
import com.mossglen.reverie.data.Series
import com.mossglen.reverie.data.SeriesInfo
import com.mossglen.reverie.data.getSeriesInfo
import com.mossglen.reverie.haptics.HapticType
import com.mossglen.reverie.haptics.performHaptic
import com.mossglen.reverie.ui.components.*
import com.mossglen.reverie.ui.theme.*
import com.mossglen.reverie.ui.viewmodel.SeriesViewModel
import java.util.concurrent.TimeUnit

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
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.ReverieAccent,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onPlayBook: (Book) -> Unit = {},
    onEditSeries: (String) -> Unit = {},
    seriesViewModel: SeriesViewModel = hiltViewModel()
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    // Get series data
    val series by seriesViewModel.getSeriesByName(seriesName).collectAsState()

    // Dialog states
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
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
                        isReverieDark = isReverieDark,
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
                        isReverieDark = isReverieDark,
                        accentColor = accentColor
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.M))
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
                            text = "Books in Series",
                            style = GlassTypography.Headline,
                            color = theme.textPrimary
                        )
                        Text(
                            text = "${series!!.bookCount} books",
                            style = GlassTypography.Caption,
                            color = theme.textSecondary
                        )
                    }
                }

                // Books List
                items(series!!.booksSorted, key = { it.id }) { book ->
                    SeriesBookItem(
                        book = book,
                        isDark = isDark,
                        isReverieDark = isReverieDark,
                        accentColor = accentColor,
                        onClick = { onBookClick(book.id) },
                        onPlayClick = { onPlayBook(book) }
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.S))
                }
            }
        }

        // Options Menu
        if (showOptionsMenu && series != null) {
            SeriesOptionsMenu(
                series = series!!,
                isDark = isDark,
                isReverieDark = isReverieDark,
                onDismiss = { showOptionsMenu = false },
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
                containerColor = theme.background,
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
    }
}

// ============================================================================
// SERIES HEADER - Cover collage + title
// ============================================================================

@Composable
private fun SeriesHeader(
    series: Series,
    isDark: Boolean,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.ReverieAccent,
    onBackClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)

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
            isReverieDark = isReverieDark,
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
    isReverieDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark, isReverieDark)

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
                // Single cover - centered
                AsyncImage(
                    model = coverUrls[0],
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .aspectRatio(0.67f)
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
                                .aspectRatio(0.67f)
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
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.ReverieAccent
) {
    val theme = glassTheme(isDark, isReverieDark)

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
                color = accentColor,
                trackColor = theme.glassBorder
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
                    isReverieDark = isReverieDark
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
                    isReverieDark = isReverieDark
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
                    isReverieDark = isReverieDark
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
    isReverieDark: Boolean = false
) {
    val theme = glassTheme(isDark, isReverieDark)

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
// SERIES BOOK ITEM
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeriesBookItem(
    book: Book,
    isDark: Boolean,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.ReverieAccent,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
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
        // Cover
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(4.dp))
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
                        .background(theme.glassBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFloat)
                            .fillMaxHeight()
                            .background(accentColor)
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
                    color = accentColor,
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
// SERIES OPTIONS MENU
// ============================================================================

@Composable
private fun SeriesOptionsMenu(
    series: Series,
    isDark: Boolean,
    isReverieDark: Boolean = false,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onMarkAsFinished: () -> Unit,
    onMarkAsUnread: () -> Unit,
    onDelete: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.background,
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
