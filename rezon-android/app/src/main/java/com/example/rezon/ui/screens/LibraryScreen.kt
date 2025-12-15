package com.example.rezon.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.*
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
import com.example.rezon.data.Book
import com.example.rezon.ui.theme.RezonSurface
import com.example.rezon.ui.viewmodel.LibraryViewModel
import com.example.rezon.ui.viewmodel.PlayerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LibraryScreen(
    onOpenDrawer: () -> Unit,
    onBookClick: () -> Unit,
    currentThemeColor: Color,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val books by libraryViewModel.allBooks.collectAsState(initial = emptyList())

    // Permission Handling
    val permissionState = rememberPermissionState(
        permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
    )

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            libraryViewModel.scanLibrary()
        } else {
            permissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("REZON", color = currentThemeColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { libraryViewModel.scanLibrary() }) {
                        Icon(Icons.Default.Search, contentDescription = "Scan", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(count = books.size.toString(), label = "Total", color = Color(0xFF00E5FF), modifier = Modifier.weight(1f))
                StatCard(count = books.count { !it.isFinished }.toString(), label = "Audio", color = Color(0xFF00E5FF), modifier = Modifier.weight(1f))
                StatCard(count = "0", label = "EPUB", color = Color(0xFFD500F9), modifier = Modifier.weight(1f))
                StatCard(count = "0", label = "PDF", color = Color(0xFFFF9100), modifier = Modifier.weight(1f))
            }

            if (books.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No books found. Tap Search to scan.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(books) { book ->
                        BookListItem(
                            book = book,
                            accentColor = currentThemeColor,
                            onClick = {
                                playerViewModel.playBook(book)
                                onBookClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(count: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(RezonSurface, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(count, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun BookListItem(book: Book, accentColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() }
            .background(RezonSurface, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover
        if (book.coverUrl != null) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Headphones, contentDescription = null, tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                book.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            Text(
                book.author,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { book.progressPercent() },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp),
                    color = accentColor,
                    trackColor = Color.DarkGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("${(book.progressPercent() * 100).toInt()}%", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}
