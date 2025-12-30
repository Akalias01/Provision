/*
 * LEGACY MAIN LAYOUT - NON-GLASS VERSION
 *
 * This is the original main layout without glass morphism effects.
 * Kept for reference and potential rollback if needed.
 *
 * DEPRECATED: Use MainLayoutGlass instead for the modern glass UI.
 * This version will be removed in a future release.
 */
package com.mossglen.reverie.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.mossglen.reverie.data.Book
import com.mossglen.reverie.ui.theme.GlassColors
import com.mossglen.reverie.ui.components.DialogItem
import com.mossglen.reverie.ui.components.DrawerAction
import com.mossglen.reverie.ui.components.ReverieDialog
import com.mossglen.reverie.ui.components.ReverieDrawerContent
import com.mossglen.reverie.ui.screens.BookDetailScreen
import com.mossglen.reverie.ui.screens.DownloadsScreen
import com.mossglen.reverie.ui.screens.EditBookScreen
import com.mossglen.reverie.ui.screens.LibraryScreen
import com.mossglen.reverie.ui.screens.PlayerScreen
import com.mossglen.reverie.ui.screens.ReaderScreen
import com.mossglen.reverie.ui.screens.SettingsScreen
import com.mossglen.reverie.ui.screens.WelcomeScreen
import com.mossglen.reverie.ui.viewmodel.AppTheme
import com.mossglen.reverie.ui.viewmodel.DisplayMode
import com.mossglen.reverie.ui.viewmodel.LibraryViewModel
import com.mossglen.reverie.ui.viewmodel.PlayerViewModel
import com.mossglen.reverie.ui.viewmodel.ThemeViewModel
import com.mossglen.reverie.ui.viewmodel.TorrentViewModel
import com.mossglen.reverie.IncomingTorrentData
import kotlinx.coroutines.launch

@Deprecated(
    message = "Use MainLayoutGlass instead for modern glass morphism UI",
    replaceWith = ReplaceWith("MainLayoutGlass")
)
@Composable
fun MainLayout(
    themeViewModel: ThemeViewModel,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    torrentViewModel: TorrentViewModel = hiltViewModel(),
    incomingTorrentData: IncomingTorrentData? = null,
    onTorrentDataConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Handle incoming torrent data from external intents
    LaunchedEffect(incomingTorrentData) {
        incomingTorrentData?.let { data ->
            when (data) {
                is IncomingTorrentData.MagnetLink -> {
                    torrentViewModel.startDownloadWithLink(data.uri)
                    navController.navigate("downloads")
                }
                is IncomingTorrentData.TorrentFile -> {
                    torrentViewModel.startFileDownload(data.uri)
                    navController.navigate("downloads")
                }
            }
            onTorrentDataConsumed()
        }
    }

    // Theme & State
    val appTheme by themeViewModel.appTheme
    val accentColor = themeViewModel.getAccentColor()
    val isDarkTheme = themeViewModel.isDarkTheme()
    val isReverieTheme = themeViewModel.isReverieTheme()
    val books by libraryViewModel.books.collectAsState()

    // Player State
    val currentBook by playerViewModel.currentBook.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val position by playerViewModel.position.collectAsState()
    val duration by playerViewModel.duration.collectAsState()

    // Calculate current chapter for mini player
    val totalChapters = if (duration > 0) maxOf(1, (duration / (30 * 60 * 1000)).toInt()) else 1
    val totalProgress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    val currentChapter = maxOf(1, ((totalProgress * totalChapters).toInt() + 1).coerceAtMost(totalChapters))

    var showImportDialog by remember { mutableStateOf(false) }
    var showCloudDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTorrentDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }

    // FIX: Track current route for context-aware mini-player
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Mini-player shows when book is loaded, player not expanded, on library or book_detail screen
    val showMiniPlayer = currentBook != null && !isPlayerExpanded &&
        (currentRoute == "library" || currentRoute?.startsWith("book_detail") == true)

    // Torrent input state
    var magnetLink by remember { mutableStateOf("") }

    // Swipe tracking for player collapse
    var swipeDelta by remember { mutableFloatStateOf(0f) }

    // Snackbar for toast notifications
    val snackbarHostState = remember { SnackbarHostState() }

    // Exit dialog state
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Back handler for intelligent navigation
    BackHandler(enabled = true) {
        when {
            isPlayerExpanded -> isPlayerExpanded = false
            currentRoute != "library" && currentRoute != "welcome" -> navController.popBackStack()
            else -> showExitDialog = true
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            libraryViewModel.importFile(it)
            navController.navigate("library") { popUpTo("welcome") { inclusive = true } }
        }
    }

    // Torrent file launcher
    val torrentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            torrentViewModel.startFileDownload(it)
            showTorrentDialog = false
            navController.navigate("downloads") // Navigate to downloads
            scope.launch {
                snackbarHostState.showSnackbar("Download started")
            }
        }
    }

    // Determine Background Color based on Theme
    val appBackground = if (isDarkTheme) Color(0xFF0A0A0A) else Color(0xFFF5F5F5)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = appBackground) {
                ReverieDrawerContent(
                    isDarkTheme = isDarkTheme,
                    isReverieTheme = isReverieTheme,
                    accentColor = accentColor,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        navController.navigate(route)
                    },
                    onAction = { action ->
                        scope.launch { drawerState.close() }
                        when (action) {
                            DrawerAction.OPEN_IMPORT -> showImportDialog = true
                            DrawerAction.OPEN_CLOUD -> showCloudDialog = true
                            DrawerAction.OPEN_THEMES -> showThemeDialog = true
                            // FIX: Navigate directly to downloads screen
                            DrawerAction.OPEN_TORRENTS -> navController.navigate("downloads")
                            DrawerAction.OPEN_STATS -> showStatsDialog = true
                            DrawerAction.OPEN_TRANSLATE -> navController.navigate("settings")
                        }
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Snackbar Host for toast notifications
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            NavHost(navController = navController, startDestination = if (books.isNotEmpty()) "library" else "welcome") {
                composable("welcome") {
                    WelcomeScreen(
                        accentColor = accentColor,
                        isDarkTheme = isDarkTheme,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onImportClick = { showImportDialog = true },
                        onTorrentClick = { showTorrentDialog = true }
                    )
                }
                composable("library") {
                    LibraryScreen(
                        libraryViewModel = libraryViewModel,
                        playerViewModel = playerViewModel,
                        accentColor = accentColor,
                        isDarkTheme = isDarkTheme,
                        isReverieTheme = isReverieTheme,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenPlayer = { bookId ->
                            val book = books.find { it.id == bookId }
                            if (book != null) {
                                // Route PDF/EPUB to ReaderScreen, audio to BookDetailScreen
                                when (book.format) {
                                    "PDF", "EPUB" -> navController.navigate("reader/$bookId")
                                    else -> navController.navigate("book_detail/$bookId")
                                }
                            }
                        },
                        onSearchClick = { showSearchDialog = true },
                        onEditClick = { bookId -> navController.navigate("edit_book/$bookId") }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        accentColor = accentColor,
                        isDarkTheme = isDarkTheme,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("downloads") {
                    DownloadsScreen(
                        isDark = isDarkTheme,
                        isReverieDark = false,  // Legacy layout doesn't support Reverie Dark
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("edit_book/{bookId}") { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getString("bookId")
                    if (bookId != null) {
                        EditBookScreen(
                            bookId = bookId,
                            accentColor = accentColor,
                            isReverieDark = false, // MainLayout doesn't support Reverie Dark
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                composable("reader/{bookId}") { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getString("bookId")
                    if (bookId != null) {
                        ReaderScreen(
                            bookId = bookId,
                            accentColor = accentColor,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                composable("book_detail/{bookId}") { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getString("bookId")
                    if (bookId != null) {
                        BookDetailScreen(
                            bookId = bookId,
                            accentColor = accentColor,
                            onBack = { navController.popBackStack() },
                            onPlayBook = { book ->
                                playerViewModel.loadBook(book)
                                playerViewModel.togglePlayback() // Start playback immediately
                            }
                        )
                    }
                }
            }

            // MINI PLAYER UI with fluid spring animation
            AnimatedVisibility(
                visible = showMiniPlayer,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(250)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(200, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(150))
            ) {
                currentBook?.let { book ->
                    MiniPlayer(
                        book = book,
                        isPlaying = isPlaying,
                        accentColor = accentColor,
                        currentChapter = currentChapter,
                        onToggle = { playerViewModel.togglePlayback() },
                        onClick = { isPlayerExpanded = true }
                    )
                }
            }

            // FULL PLAYER SCREEN with smooth slide + swipe gesture
            AnimatedVisibility(
                visible = isPlayerExpanded,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(250, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                swipeDelta += delta
                            },
                            onDragStopped = {
                                // Smooth dismiss with lower threshold
                                if (swipeDelta > 80) {
                                    isPlayerExpanded = false
                                }
                                swipeDelta = 0f
                            }
                        )
                ) {
                    currentBook?.let {
                        PlayerScreen(
                            playerViewModel = playerViewModel,
                            accentColor = accentColor,
                            displayMode = themeViewModel.displayMode,
                            onBack = { isPlayerExpanded = false }
                        )
                    }
                }
            }
        }

        // --- DIALOGS ---

        // SEARCH DIALOG
        if (showSearchDialog) {
            ReverieDialog(title = "Search Library", onDismiss = { showSearchDialog = false }) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search books...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = accentColor
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = Color.Gray)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.height(16.dp))

                // Search results
                val filteredBooks = books.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.author.contains(searchQuery, ignoreCase = true)
                }

                if (searchQuery.isNotEmpty() && filteredBooks.isNotEmpty()) {
                    filteredBooks.take(5).forEach { book ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSearchDialog = false
                                    searchQuery = ""
                                    // Route PDF/EPUB to ReaderScreen, audio to PlayerScreen
                                    when (book.format) {
                                        "PDF", "EPUB" -> navController.navigate("reader/${book.id}")
                                        else -> {
                                            playerViewModel.loadBook(book)
                                            isPlayerExpanded = true
                                        }
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = book.coverUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(book.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(book.author, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                } else if (searchQuery.isNotEmpty()) {
                    Text("No results found", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }

        // LIBRARY STATS DIALOG
        if (showStatsDialog) {
            ReverieDialog(title = "Library", onDismiss = { showStatsDialog = false }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCardCompact("Total", "${books.size}", Color(0xFFFDD835), Modifier.weight(1f)) {
                        showStatsDialog = false
                        navController.navigate("library")
                    }
                    StatCardCompact("Audio", "${books.count { it.format == "AUDIO" }}", Color(0xFF00E5FF), Modifier.weight(1f)) {
                        showStatsDialog = false
                        navController.navigate("library")
                    }
                    StatCardCompact("EPUB", "${books.count { it.format == "EPUB" }}", Color(0xFFD500F9), Modifier.weight(1f)) {
                        showStatsDialog = false
                        navController.navigate("library")
                    }
                    StatCardCompact("PDF", "${books.count { it.format == "PDF" }}", Color(0xFFFF9100), Modifier.weight(1f)) {
                        showStatsDialog = false
                        navController.navigate("library")
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Recent Row
                if (books.isNotEmpty()) {
                    val recent = books.first()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2C2C2E))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(recent.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(recent.format, color = Color.Gray, fontSize = 10.sp)
                        }
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Remove",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { libraryViewModel.deleteBook(recent.id) }
                        )
                    }
                } else {
                    Text("No recent books", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        showStatsDialog = false
                        navController.navigate("library")
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassColors.ButtonBackground),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Open Full Library", color = accentColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        // IMPORT DIALOG
        if (showImportDialog) {
            ReverieDialog(title = "Add Content", onDismiss = { showImportDialog = false }) {
                DialogItem(Icons.Default.FileOpen, "Import File", accentColor) {
                    showImportDialog = false
                    launcher.launch(arrayOf("audio/*", "application/pdf", "application/epub+zip"))
                }
                DialogItem(Icons.Default.Folder, "Scan Folder", accentColor) {
                    showImportDialog = false
                    // TODO: Trigger Folder Picker
                }
                DialogItem(Icons.Default.Cloud, "Cloud Storage", accentColor) {
                    showImportDialog = false
                    showCloudDialog = true
                }
            }
        }

        // CLOUD DIALOG
        if (showCloudDialog) {
            ReverieDialog(title = "Cloud Storage", onDismiss = { showCloudDialog = false }) {
                Text("Connect cloud storage to import directly.", color = Color.Gray, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                CloudCard("Google Drive", Color(0xFF34A853))
                Spacer(Modifier.height(12.dp))
                CloudCard("Dropbox", Color(0xFF0061FF))
            }
        }

        // TORRENT DIALOG (USES VIEWMODEL)
        if (showTorrentDialog) {
            val magnetInput by torrentViewModel.magnetLinkInput

            ReverieDialog(title = "Add Torrent", onDismiss = { showTorrentDialog = false }) {
                // 1. Browse Section
                Text("Browse for Torrent Files", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        torrentLauncher.launch(arrayOf("application/x-bittorrent"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, null, tint = accentColor)
                    Spacer(Modifier.width(8.dp))
                    Text("Select .torrent Files", color = Color.White)
                }

                Spacer(Modifier.height(24.dp))

                // 2. Magnet Link Section
                Text("Enter Magnet Link", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Paste a magnet link to download.", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = magnetInput,
                    onValueChange = { torrentViewModel.onMagnetLinkChanged(it) },
                    placeholder = { Text("magnet:?xt=urn:btih...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = accentColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(24.dp))

                // 3. Action Buttons
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showTorrentDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            torrentViewModel.startDownload()
                            showTorrentDialog = false
                            navController.navigate("downloads") // Navigate to downloads
                            scope.launch {
                                snackbarHostState.showSnackbar("Download started")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassColors.ButtonBackground),
                        enabled = magnetInput.startsWith("magnet:"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, null, tint = accentColor)
                        Spacer(Modifier.width(4.dp))
                        Text("Download", color = accentColor)
                    }
                }
            }
        }

        // THEME DIALOG - Minimal premium style with accent preview
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                containerColor = Color(0xFF1A1A1A),
                title = null,
                text = {
                    Column {
                        Text(
                            "APPEARANCE",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        AppTheme.entries.forEach { theme ->
                            val isSelected = appTheme == theme
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { themeViewModel.setTheme(theme) }
                                    .padding(vertical = 14.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Accent color circle
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.horizontalGradient(theme.accentColors)
                                        )
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = theme.displayName,
                                    color = if (isSelected) theme.accentColors[0] else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                    fontSize = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = theme.accentColors[0],
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (theme != AppTheme.entries.last()) {
                                HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 0.5.dp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Done", color = Color.Gray)
                    }
                }
            )
        }

        // EXIT DIALOG
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                containerColor = Color(0xFF1C1C1E),
                title = {
                    Text("Exit Reverie?", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("Are you sure you want to exit the app?", color = Color.Gray)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            (context as? Activity)?.finish()
                        }
                    ) {
                        Text("Exit", color = Color(0xFFFF6B6B))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Cancel", color = accentColor)
                    }
                }
            )
        }
    }
}

// MINI PLAYER COMPONENT
@Composable
fun MiniPlayer(
    book: Book,
    isPlaying: Boolean,
    accentColor: Color,
    currentChapter: Int = 1,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0xFF1C1C1E))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail (completely square - no rounded corners)
        AsyncImage(
            model = book.coverUrl,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(14.dp))

        // Chapter number + Author
        Column(Modifier.weight(1f)) {
            Text(
                text = String.format("%03d", currentChapter),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = book.author,
                color = Color.Gray,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Play/Pause button - prominent accent colored
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accentColor)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// COMPONENT HELPERS

@Composable
fun StatCardCompact(label: String, count: String, dotColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2C2C2E))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.height(8.dp))
        Text(count, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun CloudCard(name: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).background(color, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Cloud, null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Not connected", color = Color.Gray, fontSize = 12.sp)
                }
            }
            Button(
                onClick = { /* TODO: Connect */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBCAAA4).copy(alpha = 0.2f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text("Connect", color = Color(0xFFFFB74D))
            }
        }
    }
}
