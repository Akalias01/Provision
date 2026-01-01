package com.mossglen.lithos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mossglen.lithos.data.Series
import com.mossglen.lithos.haptics.HapticType
import com.mossglen.lithos.haptics.performHaptic
import com.mossglen.lithos.ui.components.GlassIconButton
import com.mossglen.lithos.ui.theme.*
import com.mossglen.lithos.ui.theme.glassCard
import java.util.concurrent.TimeUnit

/**
 * REVERIE Glass - Series Library View
 *
 * Display series in a premium card-based layout with:
 * - Expandable series cards
 * - Cover collage thumbnails
 * - Progress tracking
 * - Quick actions
 */

// ============================================================================
// SERIES GRID VIEW - For compact display
// ============================================================================

@Composable
fun SeriesGridView(
    series: List<Series>,
    isDark: Boolean,
    onSeriesClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    if (series.isEmpty()) {
        EmptySeriesState(isDark = isDark)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier,
            contentPadding = PaddingValues(
                start = GlassSpacing.M,
                end = GlassSpacing.M,
                top = GlassSpacing.S,
                bottom = GlassSpacing.XXXL
            ),
            horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S),
            verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)
        ) {
            items(series, key = { it.name }) { seriesItem ->
                SeriesGridCard(
                    series = seriesItem,
                    isDark = isDark,
                    onClick = {
                        view.performHaptic(HapticType.LightTap)
                        onSeriesClick(seriesItem.name)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeriesGridCard(
    series: Series,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {}
            )
    ) {
        // Cover collage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(GlassShapes.Small))
                .background(
                    if (isDark) Color.White.copy(alpha = 0.05f)
                    else Color.Black.copy(alpha = 0.03f)
                )
        ) {
            when (series.coverUrls.size) {
                0 -> {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = theme.textTertiary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    )
                }
                1 -> {
                    AsyncImage(
                        model = series.coverUrls[0],
                        contentDescription = series.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    // 2x2 grid of covers
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            AsyncImage(
                                model = series.coverUrls[0],
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                            if (series.coverUrls.size > 1) {
                                AsyncImage(
                                    model = series.coverUrls[1],
                                    contentDescription = null,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        if (series.coverUrls.size > 2) {
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                AsyncImage(
                                    model = series.coverUrls.getOrNull(2) ?: series.coverUrls[0],
                                    contentDescription = null,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    contentScale = ContentScale.Crop
                                )
                                AsyncImage(
                                    model = series.coverUrls.getOrNull(3) ?: series.coverUrls[1],
                                    contentDescription = null,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // Book count badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(GlassSpacing.XS)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${series.bookCount}",
                    style = GlassTypography.Caption,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Progress indicator
            if (series.progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(series.progress)
                            .fillMaxHeight()
                            .background(GlassColors.LithosAccent)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(GlassSpacing.XS))

        // Series name
        Text(
            text = series.name,
            style = GlassTypography.Body,
            fontWeight = FontWeight.SemiBold,
            color = theme.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Progress text
        val progressText = when {
            series.finishedCount == series.bookCount -> "Complete"
            series.finishedCount > 0 -> "${series.finishedCount}/${series.bookCount} finished"
            series.inProgressCount > 0 -> "In progress"
            else -> "Not started"
        }

        Text(
            text = progressText,
            style = GlassTypography.Caption,
            color = theme.textSecondary
        )
    }
}

// ============================================================================
// SERIES LIST VIEW - For detailed expandable cards
// ============================================================================

@Composable
fun SeriesListView(
    series: List<Series>,
    isDark: Boolean,
    onSeriesClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    if (series.isEmpty()) {
        EmptySeriesState(isDark = isDark)
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(
                start = GlassSpacing.M,
                end = GlassSpacing.M,
                top = GlassSpacing.S,
                bottom = GlassSpacing.XXXL
            ),
            verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)
        ) {
            items(series, key = { it.name }) { seriesItem ->
                var isExpanded by remember { mutableStateOf(false) }

                SeriesListCard(
                    series = seriesItem,
                    isDark = isDark,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        view.performHaptic(HapticType.LightTap)
                        isExpanded = !isExpanded
                    },
                    onClick = {
                        view.performHaptic(HapticType.LightTap)
                        onSeriesClick(seriesItem.name)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeriesListCard(
    series: Series,
    isDark: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark)
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

    val expandIconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .glassCard(isDark = isDark, cornerRadius = GlassShapes.Medium)
    ) {
        // Header - always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = {}
                )
                .padding(GlassSpacing.M),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover thumbnail (single or mini collage)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(GlassShapes.Small))
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.05f)
                        else Color.Black.copy(alpha = 0.03f)
                    )
            ) {
                if (series.coverUrls.isEmpty()) {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = theme.textTertiary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                    )
                } else {
                    AsyncImage(
                        model = series.coverUrls[0],
                        contentDescription = series.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Book count badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${series.bookCount}",
                        style = GlassTypography.Caption.copy(fontSize = 11.sp),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(GlassSpacing.M))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = series.name,
                    style = GlassTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(GlassSpacing.XXS))

                // Progress text
                val progressText = when {
                    series.finishedCount == series.bookCount -> "Complete • ${series.bookCount} books"
                    series.finishedCount > 0 -> "${series.finishedCount} of ${series.bookCount} finished"
                    series.inProgressCount > 0 -> "${series.inProgressCount} in progress • ${series.bookCount} books"
                    else -> "${series.bookCount} books"
                }

                Text(
                    text = progressText,
                    style = GlassTypography.Caption,
                    color = theme.textSecondary
                )

                Spacer(modifier = Modifier.height(GlassSpacing.XS))

                // Progress bar
                LinearProgressIndicator(
                    progress = { series.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = GlassColors.LithosAccent,
                    trackColor = if (isDark) Color.White.copy(alpha = 0.1f)
                    else Color.Black.copy(alpha = 0.08f)
                )
            }

            Spacer(modifier = Modifier.width(GlassSpacing.S))

            // Expand/collapse button
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = theme.textSecondary,
                    modifier = Modifier.rotate(expandIconRotation)
                )
            }
        }

        // Expanded content - additional details
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GlassSpacing.M)
                    .padding(bottom = GlassSpacing.M)
            ) {
                HorizontalDivider(
                    color = theme.glassBorder,
                    modifier = Modifier.padding(bottom = GlassSpacing.M)
                )

                // Duration info
                val totalHours = TimeUnit.MILLISECONDS.toHours(series.totalDuration)
                val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(series.totalDuration) % 60

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoItem(
                        label = "Total Duration",
                        value = "${totalHours}h ${totalMinutes}m",
                        isDark = isDark
                    )
                    InfoItem(
                        label = "Books",
                        value = "${series.bookCount}",
                        isDark = isDark
                    )
                    InfoItem(
                        label = "Finished",
                        value = "${series.finishedCount}",
                        isDark = isDark
                    )
                }

                // Next unread book preview
                series.getNextUnreadBook()?.let { nextBook ->
                    Spacer(modifier = Modifier.height(GlassSpacing.M))
                    HorizontalDivider(
                        color = theme.glassBorder,
                        modifier = Modifier.padding(bottom = GlassSpacing.S)
                    )
                    Text(
                        text = "Next: ${nextBook.title}",
                        style = GlassTypography.Caption,
                        color = GlassColors.LithosAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String, isDark: Boolean) {
    val theme = glassTheme(isDark)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = GlassTypography.Body,
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
// EMPTY STATE
// ============================================================================

@Composable
private fun EmptySeriesState(isDark: Boolean) {
    val theme = glassTheme(isDark)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(GlassSpacing.XL)
        ) {
            Icon(
                Icons.Default.AutoStories,
                contentDescription = null,
                tint = theme.textTertiary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(GlassSpacing.M))

            Text(
                text = "No Series Found",
                style = GlassTypography.Title,
                color = theme.textPrimary
            )

            Spacer(modifier = Modifier.height(GlassSpacing.XS))

            Text(
                text = "Books with series information will appear here",
                style = GlassTypography.Body,
                color = theme.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
