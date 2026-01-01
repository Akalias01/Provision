package com.mossglen.lithos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mossglen.lithos.ui.theme.GlassColors
import com.mossglen.lithos.ui.theme.glassTheme
import com.mossglen.lithos.ui.theme.LithosAmber
import com.mossglen.lithos.ui.viewmodel.CoverArtViewModel
import com.mossglen.lithos.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookScreen(
    bookId: String,
    accentColor: Color = LithosAmber,
    isDark: Boolean = true,
    isOLED: Boolean = false,
    onBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    coverArtViewModel: CoverArtViewModel = hiltViewModel()
) {
    val theme = glassTheme(isDark, isOLED)

    // Get book directly from VM
    val book = viewModel.books.collectAsState().value.find { it.id == bookId }

    if (book == null) {
        Box(Modifier.fillMaxSize().background(theme.background), contentAlignment = Alignment.Center) {
            Text("Book not found", color = theme.textSecondary)
        }
        return
    }

    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author) }
    var series by remember { mutableStateOf(book.seriesInfo) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Cover, 1 = Tracks
    var showCoverPicker by remember { mutableStateOf(false) }
    var isFetchingMetadata by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit", color = theme.textPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.updateBookMetadata(bookId, title, author, series)
                        onBack()
                    }) {
                        Icon(Icons.Default.Check, null, tint = accentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.background)
            )
        },
        containerColor = theme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = theme.background,
                contentColor = theme.textPrimary,
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = accentColor
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("COVER", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("TRACKS", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // Content
            Column(modifier = Modifier.padding(16.dp)) {
                if (selectedTab == 0) {
                    // COVER TAB
                    // Cover Image Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.glassCard)
                            .clickable { showCoverPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (book.coverUrl != null) {
                            AsyncImage(
                                model = book.coverUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Overlay to indicate it's clickable
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0f))
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Image,
                                    null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Tap to change cover", color = Color.Gray, fontSize = 12.sp)
                            }
                        }

                        // Change cover button overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Change cover",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Title Field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title", color = theme.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = theme.textSecondary,
                            cursorColor = accentColor,
                            focusedLabelColor = accentColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    // Author Field
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("Author", color = theme.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = theme.textSecondary,
                            cursorColor = accentColor,
                            focusedLabelColor = accentColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    // Series Field
                    OutlinedTextField(
                        value = series,
                        onValueChange = { series = it },
                        label = { Text("Series", color = theme.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = theme.textSecondary,
                            cursorColor = accentColor,
                            focusedLabelColor = accentColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(Modifier.height(24.dp))

                    // Format Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.glassCard)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Format", color = theme.textSecondary, fontSize = 12.sp)
                            Text(book.format, color = theme.textPrimary, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Duration", color = theme.textSecondary, fontSize = 12.sp)
                            Text(formatDuration(book.duration), color = theme.textPrimary, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Fetch Metadata Button
                    Button(
                        onClick = {
                            scope.launch {
                                isFetchingMetadata = true
                                try {
                                    viewModel.fetchMetadata(book)
                                    // Refresh the book data after fetch
                                    val updatedBook = viewModel.books.value.find { it.id == bookId }
                                    if (updatedBook != null) {
                                        if (updatedBook.synopsis.isNotBlank() && book.synopsis.isBlank()) {
                                            // Synopsis was updated
                                        }
                                        if (!updatedBook.coverUrl.isNullOrBlank() && book.coverUrl.isNullOrBlank()) {
                                            // Cover was updated
                                        }
                                    }
                                } finally {
                                    isFetchingMetadata = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isFetchingMetadata,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.glassCard,
                            contentColor = accentColor,
                            disabledContainerColor = theme.glassCard,
                            disabledContentColor = theme.textSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isFetchingMetadata) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Fetching...")
                        } else {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Fetch Metadata Online")
                        }
                    }

                } else {
                    // TRACKS TAB
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Track Editor", color = theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Track editing coming soon.\nThis will allow you to split and name chapters.",
                                color = theme.textSecondary,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Cover Art Picker Dialog
    if (showCoverPicker) {
        CoverArtPickerDialog(
            currentCoverUrl = book.coverUrl,
            bookTitle = book.title,
            bookAuthor = book.author,
            accentColor = accentColor,
            isOLED = isOLED,
            onDismiss = { showCoverPicker = false },
            onCoverSelected = { newCoverUrl ->
                coverArtViewModel.updateBookCover(bookId, newCoverUrl)
            },
            viewModel = coverArtViewModel
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
