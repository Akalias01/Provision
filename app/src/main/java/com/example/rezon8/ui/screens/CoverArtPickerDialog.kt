package com.mossglen.reverie.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mossglen.reverie.data.CoverArtRepository
import com.mossglen.reverie.ui.theme.GlassBlur
import com.mossglen.reverie.ui.theme.GlassColors
import com.mossglen.reverie.ui.theme.GlassShapes
import com.mossglen.reverie.ui.theme.GlassSpacing
import com.mossglen.reverie.ui.theme.GlassTypography
import com.mossglen.reverie.ui.viewmodel.CoverArtViewModel
import kotlinx.coroutines.launch

/**
 * Cover Art Picker Dialog with Glass UI Design System
 *
 * Features:
 * - Search cover art from Google Books and OpenLibrary
 * - Pick from device gallery
 * - Enter a URL manually
 * - Preview current cover
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverArtPickerDialog(
    currentCoverUrl: String?,
    bookTitle: String,
    bookAuthor: String,
    accentColor: Color = GlassColors.ReverieAccent,
    isReverieDark: Boolean = false,
    onDismiss: () -> Unit,
    onCoverSelected: (String) -> Unit,
    viewModel: CoverArtViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("$bookTitle $bookAuthor".trim()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Search, 1 = Gallery

    val scope = rememberCoroutineScope()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Theme colors
    val background = if (isReverieDark) Color(0xFF0A0A0A) else Color(0xFF1C1C1E)
    val glassSurface = if (isReverieDark) GlassColors.GlassPrimary else GlassColors.GlassSecondary
    val textPrimary = if (isReverieDark) GlassColors.ReverieTextPrimary else GlassColors.TextPrimary
    val textSecondary = if (isReverieDark) GlassColors.ReverieTextSecondary else GlassColors.TextSecondary

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                // Save gallery image to internal storage
                val localPath = viewModel.saveCoverFromGallery(it)
                if (localPath != null) {
                    onCoverSelected(localPath)
                    onDismiss()
                }
            }
        }
    }

    // Auto-search when dialog opens
    LaunchedEffect(Unit) {
        if (searchQuery.isNotBlank()) {
            viewModel.searchCoverArt(searchQuery, null)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(GlassSpacing.M),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(GlassShapes.Large),
                color = background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(GlassSpacing.M)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Change Cover Art",
                            style = GlassTypography.Title,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(GlassSpacing.M))

                    // Current Cover Preview
                    if (currentCoverUrl != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(GlassShapes.Medium))
                                .background(glassSurface)
                                .padding(GlassSpacing.S),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = currentCoverUrl,
                                contentDescription = "Current cover",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(GlassShapes.Small))
                                    .background(Color(0xFF2C2C2E)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(GlassSpacing.S))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Current Cover",
                                    style = GlassTypography.Callout,
                                    color = textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = bookTitle,
                                    style = GlassTypography.Caption,
                                    color = textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(GlassSpacing.M))
                    }

                    // Tab Row - Search + Gallery (URL removed - web search covers all sources)
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = accentColor,
                        indicator = { tabPositions ->
                            if (tabPositions.isNotEmpty()) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = accentColor,
                                    height = 3.dp
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    "Search",
                                    style = GlassTypography.Label,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    "Gallery",
                                    style = GlassTypography.Label,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(GlassSpacing.M))

                    // Tab Content
                    when (selectedTab) {
                        // SEARCH TAB
                        0 -> {
                            // Search Field
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Search by title or author", style = GlassTypography.Callout) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = textSecondary.copy(alpha = 0.5f),
                                    cursorColor = accentColor,
                                    focusedLabelColor = accentColor,
                                    unfocusedLabelColor = textSecondary
                                ),
                                shape = RoundedCornerShape(GlassShapes.Small),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (searchQuery.isNotBlank() && !isSearching) {
                                                // Use full search query - let API handle parsing
                                                viewModel.searchCoverArt(searchQuery, null)
                                            }
                                        },
                                        enabled = !isSearching
                                    ) {
                                        if (isSearching) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = accentColor,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = "Search",
                                                tint = accentColor
                                            )
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(GlassSpacing.M))

                            // Search Results Grid
                            if (searchResults.isNotEmpty()) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(GlassSpacing.XS),
                                    verticalArrangement = Arrangement.spacedBy(GlassSpacing.XS)
                                ) {
                                    items(searchResults) { result ->
                                        CoverArtGridItem(
                                            coverUrl = result.coverUrl,
                                            source = result.source,
                                            accentColor = accentColor,
                                            onClick = {
                                                scope.launch {
                                                    // Download and save the cover
                                                    val localPath = viewModel.downloadAndSaveCover(result.coverUrl)
                                                    if (localPath != null) {
                                                        onCoverSelected(localPath)
                                                        onDismiss()
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            } else if (isSearching) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            color = accentColor,
                                            strokeWidth = 3.dp
                                        )
                                        Spacer(modifier = Modifier.height(GlassSpacing.M))
                                        Text(
                                            "Searching for covers...",
                                            style = GlassTypography.Body,
                                            color = textSecondary
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(GlassSpacing.XL)
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = textSecondary,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(GlassSpacing.M))
                                        Text(
                                            text = "Search for cover art",
                                            style = GlassTypography.Headline,
                                            color = textPrimary,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(GlassSpacing.XS))
                                        Text(
                                            text = "Searching Google Books, OpenLibrary, and iTunes",
                                            style = GlassTypography.Body,
                                            color = textSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // GALLERY TAB
                        1 -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(GlassSpacing.XL)
                                ) {
                                    Icon(
                                        Icons.Default.Photo,
                                        contentDescription = null,
                                        tint = textSecondary,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(GlassSpacing.M))
                                    Text(
                                        text = "Pick from Gallery",
                                        style = GlassTypography.Headline,
                                        color = textPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(GlassSpacing.XS))
                                    Text(
                                        text = "Choose a cover image from your device's photo gallery",
                                        style = GlassTypography.Body,
                                        color = textSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(GlassSpacing.L))
                                    Button(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GlassColors.ButtonBackground,
                                            contentColor = accentColor
                                        ),
                                        shape = RoundedCornerShape(GlassShapes.Small),
                                        modifier = Modifier.fillMaxWidth(0.7f)
                                    ) {
                                        Icon(Icons.Default.Photo, contentDescription = null, tint = accentColor)
                                        Spacer(modifier = Modifier.width(GlassSpacing.XS))
                                        Text("Open Gallery", style = GlassTypography.Label, color = accentColor)
                                    }
                                }
                            }
                        }
                    }

                    // Error Message
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = GlassSpacing.M)
                                .clip(RoundedCornerShape(GlassShapes.Small))
                                .background(GlassColors.Destructive.copy(alpha = 0.2f))
                                .padding(GlassSpacing.S),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = GlassColors.Destructive,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(GlassSpacing.XS))
                            Text(
                                text = errorMessage ?: "",
                                style = GlassTypography.Callout,
                                color = GlassColors.Destructive
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Grid item for displaying a cover art search result
 */
@Composable
private fun CoverArtGridItem(
    coverUrl: String,
    source: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(0.67f) // Standard book cover ratio
            .clip(RoundedCornerShape(GlassShapes.Small))
            .border(
                width = 1.dp,
                color = GlassColors.GlassBorder,
                shape = RoundedCornerShape(GlassShapes.Small)
            )
            .clickable(onClick = onClick)
            .background(Color(0xFF2C2C2E))
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = "Cover from $source",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Source badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = source,
                style = GlassTypography.Caption,
                color = Color.White,
                fontSize = 9.sp
            )
        }
    }
}
