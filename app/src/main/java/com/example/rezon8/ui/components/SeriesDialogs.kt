package com.mossglen.reverie.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mossglen.reverie.data.Book
import com.mossglen.reverie.data.Series
import com.mossglen.reverie.data.SeriesInfo
import com.mossglen.reverie.data.getSeriesInfo
import com.mossglen.reverie.haptics.HapticType
import com.mossglen.reverie.haptics.performHaptic
import com.mossglen.reverie.ui.theme.*
import kotlin.math.roundToInt

/**
 * REVERIE Glass - Series Management Dialogs
 *
 * Dialogs for managing series:
 * - Assign book to series
 * - Edit series name and book order
 * - Merge series
 * - Split series
 */

// ============================================================================
// ASSIGN BOOK TO SERIES DIALOG
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignBookToSeriesDialog(
    book: Book,
    existingSeries: List<String>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onAssign: (seriesName: String, bookNumber: Float?) -> Unit
) {
    val theme = glassTheme(isDark)
    val view = LocalView.current

    var selectedSeriesName by remember { mutableStateOf(book.getSeriesInfo()?.name ?: "") }
    var bookNumberText by remember {
        val currentNumber = book.getSeriesInfo()?.bookNumber
        mutableStateOf(currentNumber?.toString() ?: "")
    }
    var isCreatingNew by remember { mutableStateOf(false) }

    // Swipe-to-dismiss state
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 120f

    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "dialogOffset"
    )

    val dismissProgress = (dragOffsetY / dismissThreshold).coerceIn(0f, 1f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 300.dp, max = 400.dp)
                .fillMaxWidth(0.9f)
                .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
                .scale(1f - dismissProgress * 0.05f)
                .alpha(1f - dismissProgress * 0.3f)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffsetY > dismissThreshold) {
                                onDismiss()
                            } else {
                                dragOffsetY = 0f
                            }
                        },
                        onDragCancel = { dragOffsetY = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                        }
                    )
                }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            DragHandle(
                modifier = Modifier.padding(bottom = 8.dp),
                color = Color.Gray.copy(alpha = 0.4f + dismissProgress * 0.3f)
            )

            // Title
            Text(
                text = "Assign to Series",
                style = GlassTypography.Headline,
                color = theme.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Content
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)
            ) {
                // Book title
                Text(
                    text = book.title,
                    style = GlassTypography.Body,
                    color = theme.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                HorizontalDivider(color = theme.glassBorder)

                // Choose existing or create new
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
                ) {
                    FilterChip(
                        selected = !isCreatingNew,
                        onClick = { isCreatingNew = false },
                        label = { Text("Existing") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.glassPrimary,
                            selectedLabelColor = theme.textPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isCreatingNew,
                        onClick = { isCreatingNew = true },
                        label = { Text("New Series") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.glassPrimary,
                            selectedLabelColor = theme.textPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isCreatingNew) {
                    // New series name input
                    OutlinedTextField(
                        value = selectedSeriesName,
                        onValueChange = { selectedSeriesName = it },
                        label = { Text("Series Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.interactive,
                            unfocusedBorderColor = theme.glassBorder
                        ),
                        singleLine = true
                    )
                } else {
                    // Existing series dropdown
                    if (existingSeries.isEmpty()) {
                        Text(
                            text = "No existing series found",
                            style = GlassTypography.Caption,
                            color = theme.textSecondary
                        )
                    } else {
                        var expanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedSeriesName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Series") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = theme.textPrimary,
                                    unfocusedTextColor = theme.textPrimary,
                                    focusedBorderColor = theme.interactive,
                                    unfocusedBorderColor = theme.glassBorder
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(
                                    if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
                                )
                            ) {
                                existingSeries.forEach { seriesName ->
                                    DropdownMenuItem(
                                        text = { Text(seriesName, color = theme.textPrimary) },
                                        onClick = {
                                            selectedSeriesName = seriesName
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Book number input
                OutlinedTextField(
                    value = bookNumberText,
                    onValueChange = { bookNumberText = it },
                    label = { Text("Book Number (optional)") },
                    placeholder = { Text("e.g., 1 or 1.5") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                        focusedBorderColor = theme.interactive,
                        unfocusedBorderColor = theme.glassBorder
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = theme.textSecondary)
                }
                TextButton(
                    onClick = {
                        if (selectedSeriesName.isNotBlank()) {
                            view.performHaptic(HapticType.Confirm)
                            val bookNumber = bookNumberText.toFloatOrNull()
                            onAssign(selectedSeriesName.trim(), bookNumber)
                        }
                    },
                    enabled = selectedSeriesName.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Assign",
                        color = if (selectedSeriesName.isNotBlank()) theme.interactive else theme.textTertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ============================================================================
// EDIT SERIES DIALOG
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSeriesDialog(
    series: Series,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit,
    onReorderBook: (bookId: String, newNumber: Float) -> Unit
) {
    val theme = glassTheme(isDark)
    val view = LocalView.current

    var newSeriesName by remember { mutableStateOf(series.name) }
    var showReorderDialog by remember { mutableStateOf(false) }
    var bookToReorder by remember { mutableStateOf<Book?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = "Edit Series",
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)
            ) {
                // Series name input
                OutlinedTextField(
                    value = newSeriesName,
                    onValueChange = { newSeriesName = it },
                    label = { Text("Series Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                        focusedBorderColor = theme.interactive,
                        unfocusedBorderColor = theme.glassBorder
                    ),
                    singleLine = true
                )

                HorizontalDivider(color = theme.glassBorder)

                // Books list with reorder option
                Text(
                    text = "Books (${series.bookCount})",
                    style = GlassTypography.Body,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textPrimary
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(GlassSpacing.XS)
                ) {
                    items(series.booksSorted) { book ->
                        val seriesInfo = book.getSeriesInfo()
                        val bookNumber = seriesInfo?.bookNumber

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(GlassShapes.Small))
                                .clickable {
                                    view.performHaptic(HapticType.LightTap)
                                    bookToReorder = book
                                    showReorderDialog = true
                                }
                                .padding(GlassSpacing.S),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (bookNumber != null) {
                                    Text(
                                        text = "#${if (bookNumber % 1 == 0f) bookNumber.toInt() else bookNumber}",
                                        style = GlassTypography.Caption,
                                        color = GlassColors.ReverieAccent,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.width(40.dp)
                                    )
                                }
                                Text(
                                    text = book.title,
                                    style = GlassTypography.Caption,
                                    color = theme.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit order",
                                tint = theme.textTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newSeriesName.isNotBlank() && newSeriesName != series.name) {
                        view.performHaptic(HapticType.Confirm)
                        onRename(newSeriesName.trim())
                    }
                    onDismiss()
                }
            ) {
                Text("Save", color = theme.interactive, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = theme.textSecondary)
            }
        }
    )

    // Reorder book dialog
    bookToReorder?.let { book ->
        if (showReorderDialog) {
            ReorderBookDialog(
                book = book,
                isDark = isDark,
                onDismiss = {
                    showReorderDialog = false
                    bookToReorder = null
                },
                onReorder = { newNumber ->
                    view.performHaptic(HapticType.Confirm)
                    onReorderBook(book.id, newNumber)
                    showReorderDialog = false
                    bookToReorder = null
                }
            )
        }
    }
}

// ============================================================================
// REORDER BOOK DIALOG
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReorderBookDialog(
    book: Book,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onReorder: (Float) -> Unit
) {
    val theme = glassTheme(isDark)
    val currentNumber = book.getSeriesInfo()?.bookNumber
    var newNumberText by remember { mutableStateOf(currentNumber?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = "Reorder Book",
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)) {
                Text(
                    text = book.title,
                    style = GlassTypography.Body,
                    color = theme.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                OutlinedTextField(
                    value = newNumberText,
                    onValueChange = { newNumberText = it },
                    label = { Text("Book Number") },
                    placeholder = { Text("e.g., 1 or 1.5") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                        focusedBorderColor = theme.interactive,
                        unfocusedBorderColor = theme.glassBorder
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    newNumberText.toFloatOrNull()?.let { number ->
                        onReorder(number)
                    }
                },
                enabled = newNumberText.toFloatOrNull() != null
            ) {
                Text(
                    "Save",
                    color = if (newNumberText.toFloatOrNull() != null) theme.interactive else theme.textTertiary,
                    fontWeight = FontWeight.SemiBold
                )
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
// MERGE SERIES DIALOG
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeSeriesDialog(
    sourceSeries: Series,
    allSeries: List<Series>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onMerge: (targetSeriesName: String) -> Unit
) {
    val theme = glassTheme(isDark)
    val view = LocalView.current

    var selectedTargetSeries by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val availableTargetSeries = allSeries.filter { it.name != sourceSeries.name }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = "Merge Series",
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)) {
                Text(
                    text = "Merge \"${sourceSeries.name}\" (${sourceSeries.bookCount} books) into:",
                    style = GlassTypography.Body,
                    color = theme.textSecondary
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedTargetSeries,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Series") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.interactive,
                            unfocusedBorderColor = theme.glassBorder
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(
                            if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
                        )
                    ) {
                        availableTargetSeries.forEach { series ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${series.name} (${series.bookCount} books)",
                                        color = theme.textPrimary
                                    )
                                },
                                onClick = {
                                    selectedTargetSeries = series.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedTargetSeries.isNotBlank()) {
                    Text(
                        text = "All books from \"${sourceSeries.name}\" will be moved to \"$selectedTargetSeries\"",
                        style = GlassTypography.Caption,
                        color = GlassColors.Warning
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    view.performHaptic(HapticType.Confirm)
                    onMerge(selectedTargetSeries)
                },
                enabled = selectedTargetSeries.isNotBlank()
            ) {
                Text(
                    "Merge",
                    color = if (selectedTargetSeries.isNotBlank()) theme.interactive else theme.textTertiary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = theme.textSecondary)
            }
        }
    )
}
