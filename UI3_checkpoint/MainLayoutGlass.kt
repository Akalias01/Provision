package com.mossglen.reverie.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil.compose.AsyncImage
import com.mossglen.reverie.IncomingTorrentData
import com.mossglen.reverie.IncomingFileData
import com.mossglen.reverie.R
import com.mossglen.reverie.data.Book
import com.mossglen.reverie.data.Chapter
import com.mossglen.reverie.navigation.*
import com.mossglen.reverie.ui.components.*
import com.mossglen.reverie.ui.screens.*
import com.mossglen.reverie.ui.theme.*
import com.mossglen.reverie.ui.viewmodel.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlin.math.max
import kotlin.math.abs

/**
 * REVERIE Glass - Main Layout
 *
 * Type-Safe Navigation with @Serializable routes.
 * Bottom navigation architecture with floating glass design.
 * Aligned with iOS 26, Android 16, One UI 7, OxygenOS 16.
 */

@Composable
fun MainLayoutGlass(
    themeViewModel: ThemeViewModel,
    isDark: Boolean = true,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    torrentViewModel: TorrentViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    incomingTorrentData: IncomingTorrentData? = null,
    onTorrentDataConsumed: () -> Unit = {},
    incomingFileData: IncomingFileData? = null,
    onFileDataConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Theme - use the isDark parameter passed from MainActivity
    val isDarkTheme = isDark
    val themeMode by themeViewModel.themeMode
    val isReverieDark = themeMode == ThemeMode.REVERIE_DARK
    val theme = glassTheme(isDarkTheme, isReverieDark)

    // Haze state for true iOS 26 / Android 16 glass blur effect
    val hazeState = remember { HazeState() }

    // Accent color - uses selected variant for Reverie Dark, blue for standard
    val reverieAccentVariant by themeViewModel.reverieAccentVariant
    val accentColor = if (isReverieDark) {
        themeViewModel.getReverieAccentColor()
    } else {
        GlassColors.Interactive
    }

    // Highlight color for selections - warm slate or subtle copper based on variant
    val highlightColor = if (isReverieDark) {
        themeViewModel.getReverieHighlightColor()
    } else {
        Color.White.copy(alpha = 0.1f)
    }
    val useBorderHighlight = isReverieDark && themeViewModel.useBorderHighlight()

    val books by libraryViewModel.books.collectAsState()
    val dynamicPlayerColors by settingsViewModel.dynamicPlayerColors.collectAsState()
    val mostRecentBook by libraryViewModel.mostRecentBook.collectAsState()

    // Player State
    val currentBook by playerViewModel.currentBook.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val position by playerViewModel.position.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsState()

    // The book to use for Resume/Progress - current playing book takes priority, otherwise most recent
    val resumeBook = currentBook ?: mostRecentBook

    // UI State
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var showAddContentSheet by remember { mutableStateOf(false) }
    var showChapterNavigation by remember { mutableStateOf(false) }
    var swipeDelta by remember { mutableFloatStateOf(0f) }
    var backPressedOnce by remember { mutableStateOf(false) }

    // Mini player dismiss state - remember which book was dismissed
    var dismissedBookId by remember { mutableStateOf<String?>(null) }

    // Scroll-to-top triggers - increment to trigger scroll
    var homeScrollTrigger by remember { mutableIntStateOf(0) }
    var libraryScrollTrigger by remember { mutableIntStateOf(0) }
    var settingsScrollTrigger by remember { mutableIntStateOf(0) }

    // Reset back pressed state after 2 seconds
    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            kotlinx.coroutines.delay(2000)
            backPressedOnce = false
        }
    }

    // Navigation state - Type-Safe
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Track destination changes to collapse mini player on page change
    var shouldCollapseMiniPlayer by remember { mutableStateOf(false) }
    LaunchedEffect(currentDestination?.route) {
        // Collapse mini player when navigating to a new page
        shouldCollapseMiniPlayer = true
        kotlinx.coroutines.delay(100) // Brief pulse
        shouldCollapseMiniPlayer = false
    }

    // Check current route type-safely
    val isOnHome = currentDestination?.hasRoute<Home>() == true
    val isOnLibrary = currentDestination?.hasRoute<Library>() == true
    val isOnSearch = currentDestination?.hasRoute<Search>() == true
    val isOnSettings = currentDestination?.hasRoute<Settings>() == true
    val isOnWelcome = currentDestination?.hasRoute<Welcome>() == true
    val isOnBookDetail = currentDestination?.hasRoute<BookDetail>() == true
    val isOnReader = currentDestination?.hasRoute<Reader>() == true
    val isOnDownloads = currentDestination?.hasRoute<Downloads>() == true

    // Reset dismissed state when book changes or new playback starts
    LaunchedEffect(currentBook?.id) {
        // Clear dismissed state if a different book is played
        val book = currentBook
        if (book != null && book.id != dismissedBookId) {
            dismissedBookId = null
        }
    }

    // Smart reappear: show mini player when full player is closed while audio is playing
    LaunchedEffect(isPlayerExpanded, isPlaying) {
        // When player closes while audio is playing, reset dismissedBookId
        if (!isPlayerExpanded && isPlaying) {
            dismissedBookId = null
        }
    }

    // Calculate mini-player visibility - show on main screens AND book detail, but not if dismissed
    val showMiniPlayer = currentBook?.let { book ->
        !isPlayerExpanded && book.id != dismissedBookId &&
        (isOnHome || isOnLibrary || isOnSearch || isOnSettings || isOnBookDetail)
    } ?: false

    // Handle incoming torrent data
    LaunchedEffect(incomingTorrentData) {
        incomingTorrentData?.let { data ->
            when (data) {
                is IncomingTorrentData.MagnetLink -> {
                    torrentViewModel.startDownloadWithLink(data.uri)
                    navController.navigate(Downloads)
                }
                is IncomingTorrentData.TorrentFile -> {
                    torrentViewModel.startFileDownload(data.uri)
                    navController.navigate(Downloads)
                }
            }
            onTorrentDataConsumed()
        }
    }

    // Handle incoming file data (EPUB, PDF, DOCX, DOC, TXT)
    LaunchedEffect(incomingFileData) {
        incomingFileData?.let { data ->
            when (data) {
                is IncomingFileData.DocumentFile -> {
                    // Import the file to library
                    val book = libraryViewModel.importFileAndReturn(data.uri)
                    if (book != null) {
                        // Navigate to reader for readable formats
                        when (book.format) {
                            "EPUB", "PDF", "TEXT" -> navController.navigate(Reader(book.id))
                            else -> navController.navigate(BookDetail(book.id))
                        }
                    } else {
                        // If import failed, just go to library
                        navController.navigate(Library)
                    }
                }
            }
            onFileDataConsumed()
        }
    }

    // File picker
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            libraryViewModel.importFile(it)
            navController.navigate(Library) {
                popUpTo<Library> { inclusive = true }
            }
        }
    }

    // Torrent file picker
    val torrentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            torrentViewModel.startFileDownload(it)
            navController.navigate(Downloads)
        }
    }

    // Back handler - double-tap to exit on main screen
    BackHandler(enabled = true) {
        when {
            isPlayerExpanded -> isPlayerExpanded = false
            showAddContentSheet -> showAddContentSheet = false
            !isOnHome && !isOnLibrary -> navController.popBackStack()
            else -> {
                if (backPressedOnce) {
                    // Second press within 2 seconds - exit app
                    (context as? Activity)?.finish()
                } else {
                    // First press - show toast and wait for second press
                    backPressedOnce = true
                    android.widget.Toast.makeText(
                        context,
                        "Press back again to exit",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Bottom nav items - Type-Safe routes for standard navigation
    // UI3 5-icon layout: Now, Browse, Resume, Journey, You
    val standardNavItems = listOf(
        GlassNavItem(
            icon = Icons.Outlined.Headphones,
            selectedIcon = Icons.Filled.Headphones,
            label = stringResource(R.string.nav_now),
            route = Home  // Route stays same, just relabeled
        ),
        GlassNavItem(
            icon = Icons.Outlined.Explore,
            selectedIcon = Icons.Filled.Explore,
            label = stringResource(R.string.nav_browse),
            route = Library  // Route stays same, just relabeled
        )
    )

    val settingsNavItem = GlassNavItem(
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings,
        label = stringResource(R.string.nav_settings),
        route = Settings
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        // Main content - Type-Safe NavHost
        // Start with Home if books exist, otherwise Library
        val startRoute = if (books.isNotEmpty()) Home else Library

        // Content scrolls edge-to-edge - pills float on top with full transparency behind them
        // Individual screens should add their own bottom padding/contentPadding for scrollable content

        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)  // Capture content for glass blur
                // No bottom padding - content scrolls underneath floating pills
        ) {
            composable<Welcome> {
                WelcomeScreenGlass(
                    isDark = isDarkTheme,
                    onImportClick = { fileLauncher.launch(arrayOf("audio/*", "application/pdf", "application/epub+zip")) },
                    onTorrentClick = { navController.navigate(Downloads) }
                )
            }

            composable<Home> {
                HomeScreenGlass(
                    isDark = isDarkTheme,
                    isReverieDark = isReverieDark,
                    accentColor = accentColor,
                    scrollToTopTrigger = homeScrollTrigger,
                    onBookClick = { bookId ->
                        val book = books.find { it.id == bookId }
                        if (book != null) {
                            when (book.format) {
                                "PDF", "EPUB", "TEXT" -> navController.navigate(Reader(bookId))
                                else -> navController.navigate(BookDetail(bookId))
                            }
                        }
                    },
                    onPlayBook = { book ->
                        // Route based on format - EPUB/PDF/TEXT go to reader, AUDIO to player
                        when (book.format) {
                            "EPUB", "PDF", "TEXT" -> navController.navigate(Reader(book.id))
                            else -> playerViewModel.checkAndShowResumeDialog(book)
                        }
                    },
                    onSeriesClick = { seriesName ->
                        navController.navigate(SeriesDetail(seriesName))
                    },
                    onAuthorClick = { authorName ->
                        navController.navigate(AuthorBooks(authorName))
                    },
                    onGenreClick = { genre ->
                        navController.navigate(GenreBooks(genre))
                    }
                )
            }

            composable<Library> {
                LibraryScreenGlass(
                    libraryViewModel = libraryViewModel,
                    isDark = isDarkTheme,
                    isReverieDark = isReverieDark,
                    accentColor = accentColor,
                    scrollToTopTrigger = libraryScrollTrigger,
                    onBookClick = { bookId ->
                        val book = books.find { it.id == bookId }
                        if (book != null) {
                            when (book.format) {
                                "PDF", "EPUB", "TEXT" -> navController.navigate(Reader(bookId))
                                else -> navController.navigate(BookDetail(bookId))
                            }
                        }
                    },
                    onPlayBook = { book ->
                        // Route based on format - EPUB/PDF/TEXT go to reader, AUDIO to player
                        when (book.format) {
                            "EPUB", "PDF", "TEXT" -> navController.navigate(Reader(book.id))
                            else -> playerViewModel.checkAndShowResumeDialog(book)
                        }
                    },
                    onEditBook = { bookId ->
                        navController.navigate(EditBook(bookId))
                    },
                    onAddClick = { showAddContentSheet = true },
                    onSeriesClick = { seriesName ->
                        navController.navigate(SeriesDetail(seriesName))
                    },
                    onAuthorClick = { authorName ->
                        navController.navigate(AuthorBooks(authorName))
                    },
                    onGenreClick = { genre ->
                        navController.navigate(GenreBooks(genre))
                    }
                )
            }

            composable<Search> {
                SearchScreenGlass(
                    libraryViewModel = libraryViewModel,
                    isDark = isDarkTheme,
                    onBookClick = { bookId ->
                        val book = books.find { it.id == bookId }
                        if (book != null) {
                            when (book.format) {
                                "PDF", "EPUB", "TEXT" -> navController.navigate(Reader(bookId))
                                else -> navController.navigate(BookDetail(bookId))
                            }
                        }
                    }
                )
            }

            composable<Settings> {
                SettingsScreenGlass(
                    isDark = isDarkTheme,
                    themeViewModel = themeViewModel,
                    scrollToTopTrigger = settingsScrollTrigger,
                    onBack = { navController.popBackStack() },
                    onOpenEqualizer = { navController.navigate(Equalizer) },
                    onNavigateToDownloads = { navController.navigate(Downloads) },
                    onNavigateToCloudFiles = { navController.navigate(CloudSync) },
                    onNavigateToStats = { navController.navigate(ListeningStats) }
                )
            }

            composable<Downloads> {
                DownloadsScreen(
                    isDark = isDarkTheme,
                    isReverieDark = isReverieDark,
                    accentColor = accentColor,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<BookDetail> { backStackEntry ->
                val route: BookDetail = backStackEntry.toRoute()
                BookDetailScreen(
                    bookId = route.bookId,
                    accentColor = accentColor,
                    isReverieDark = isReverieDark,
                    onBack = { navController.popBackStack() },
                    onPlayBook = { book ->
                        // Route based on format - EPUB/PDF/TEXT go to reader, AUDIO to player
                        when (book.format) {
                            "EPUB", "PDF", "TEXT" -> navController.navigate(Reader(book.id))
                            else -> {
                                // Play/pause without expanding - user can tap mini player to expand
                                if (currentBook?.id == book.id) {
                                    // Same book - just toggle playback
                                    playerViewModel.togglePlayback()
                                } else {
                                    // Different book - check for resume dialog
                                    playerViewModel.checkAndShowResumeDialog(book)
                                }
                            }
                        }
                    },
                    onAuthorClick = { authorName ->
                        navController.navigate(AuthorBooks(authorName))
                    },
                    onSplitBook = { bookId ->
                        navController.navigate(SplitBook(bookId))
                    }
                )
            }

            composable<Reader> { backStackEntry ->
                val route: Reader = backStackEntry.toRoute()
                ReaderScreen(
                    bookId = route.bookId,
                    accentColor = accentColor,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<EditBook> { backStackEntry ->
                val route: EditBook = backStackEntry.toRoute()
                EditBookScreen(
                    bookId = route.bookId,
                    accentColor = accentColor,
                    isReverieDark = isReverieDark,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<SplitBook> { backStackEntry ->
                val route: SplitBook = backStackEntry.toRoute()
                SplitBookScreen(
                    bookId = route.bookId,
                    accentColor = accentColor,
                    isReverieDark = isReverieDark,
                    onBack = { navController.popBackStack() },
                    onSplitComplete = {
                        // Navigate back to library after successful split
                        navController.navigate(Library) {
                            popUpTo(Library) { inclusive = true }
                        }
                    }
                )
            }

            composable<CloudSync> {
                CloudFileBrowserScreen(
                    isDark = isDarkTheme,
                    isReverieDark = isReverieDark,
                    reverieAccentColor = accentColor,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<ListeningStats> {
                ListeningStatsScreen(
                    isDark = isDarkTheme,
                    isReverieDark = isReverieDark,
                    accentColor = accentColor,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<AuthorBooks> { backStackEntry ->
                val route: AuthorBooks = backStackEntry.toRoute()
                AuthorBooksScreen(
                    authorName = route.authorName,
                    libraryViewModel = libraryViewModel,
                    isDark = isDarkTheme,
                    isReverieDark = isReverieDark,
                    accentColor = accentColor,
                    onBack = { navController.popBackStack() },
                    onBookClick = { bookId ->
                        val book = books.find { it.id == bookId }
                        if (book != null) {
                            when (book.format) {
                                "PDF", "EPUB", "TEXT" -> navController.navigate(Reader(bookId))
                                else -> navController.navigate(BookDetail(bookId))
                            }
                        }
                    }
                )
            }

            composable<GenreBooks> { backStackEntry ->
                val route: GenreBooks = backStackEntry.toRoute()
                GenreBooksScreen(
                    genre = route.genre,
                    libraryViewModel = libraryViewModel,
                    isDark = isDarkTheme,
                    isReverieDark = isReverieDark,
                    accentColor = accentColor,
                    onBack = { navController.popBackStack() },
                    onBookClick = { bookId ->
                        val book = books.find { it.id == bookId }
                        if (book != null) {
                            when (book.format) {
                                "PDF", "EPUB", "TEXT" -> navController.navigate(Reader(bookId))
                                else -> navController.navigate(BookDetail(bookId))
                            }
                        }
                    }
                )
            }

            composable<Equalizer> {
                EqualizerScreen(
                    isDark = isDarkTheme,
                    isReverieDark = isReverieDark,
                    accentColor = accentColor,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<ChapterList> { backStackEntry ->
                val route: ChapterList = backStackEntry.toRoute()
                ChapterListScreen(
                    bookId = route.bookId,
                    isDark = isDarkTheme,
                    isReverieDark = isReverieDark,
                    accentColor = accentColor,
                    onBack = { navController.popBackStack() },
                    onChapterClick = { chapter ->
                        // After clicking a chapter, navigate back to player
                        navController.popBackStack()
                    }
                )
            }

            composable<Bookmarks> { backStackEntry ->
                val route: Bookmarks = backStackEntry.toRoute()
                BookmarksScreen(
                    bookId = route.bookId,
                    isDark = isDarkTheme,
                    isReverieDark = isReverieDark,
                    accentColor = accentColor,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<SeriesDetail> { backStackEntry ->
                val route: SeriesDetail = backStackEntry.toRoute()
                SeriesDetailScreen(
                    seriesName = route.seriesName,
                    isDark = isDarkTheme,
                    onBackClick = { navController.popBackStack() },
                    onBookClick = { bookId ->
                        val book = books.find { it.id == bookId }
                        if (book != null) {
                            when (book.format) {
                                "PDF", "EPUB", "TEXT" -> navController.navigate(Reader(bookId))
                                else -> navController.navigate(BookDetail(bookId))
                            }
                        }
                    },
                    onPlayBook = { book ->
                        // Route based on format - EPUB/PDF/TEXT go to reader, AUDIO to player
                        when (book.format) {
                            "EPUB", "PDF", "TEXT" -> navController.navigate(Reader(book.id))
                            else -> playerViewModel.checkAndShowResumeDialog(book)
                        }
                    }
                )
            }

            composable<NowPlaying> {
                // NowPlaying route - expand the player
                LaunchedEffect(Unit) {
                    isPlayerExpanded = true
                    navController.popBackStack()
                }
            }
        }

        // Mini Player - position above floating pill bar with comfortable spacing
        // Pill bar height is 64dp (pill content) + navigationBarsPadding + horizontal margin
        val pillBarHeight = 64.dp
        val miniPlayerBottomPadding = if (isOnBookDetail) {
            0.dp // No pill bar, mini player handles navigationBarsPadding itself
        } else {
            pillBarHeight + 24.dp // 24dp spacing above pill bar for better visual separation
        }

        // Calculate chapter progress for mini player (falls back to book progress if no chapters)
        val miniPlayerProgress = currentBook?.let { book ->
            if (book.chapters.isNotEmpty() && duration > 0) {
                // Find current chapter based on position
                val currentChapterIndex = book.chapters.indexOfLast { position >= it.startMs }
                if (currentChapterIndex >= 0 && currentChapterIndex < book.chapters.size) {
                    val chapter = book.chapters[currentChapterIndex]
                    val chapterStart = chapter.startMs
                    val chapterEnd = chapter.endMs
                    val chapterDuration = chapterEnd - chapterStart
                    if (chapterDuration > 0) {
                        ((position - chapterStart).toFloat() / chapterDuration.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                } else {
                    // Fallback to book progress if chapter not found
                    (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                }
            } else {
                // Fallback to book progress if no chapters
                if (duration > 0) position.toFloat() / duration.toFloat() else 0f
            }
        } ?: 0f

        AnimatedVisibility(
            visible = showMiniPlayer,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            // Truly floating mini player - no background wrapper, content scrolls underneath
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // No background - fully transparent, content scrolls underneath
            ) {
                currentBook?.let { book ->
                    GlassMiniPlayer(
                        book = book,
                        isPlaying = isPlaying,
                        progress = miniPlayerProgress,
                        playbackSpeed = playbackSpeed,
                        isDark = isDarkTheme,
                        isReverieDark = isReverieDark,
                        reverieAccentColor = accentColor,
                        hazeState = hazeState,
                        onPlayPause = { playerViewModel.togglePlayback() },
                        onSpeedClick = { isPlayerExpanded = true },
                        onClick = { isPlayerExpanded = true },
                        onDismiss = { dismissedBookId = book.id },
                        onScroll = shouldCollapseMiniPlayer,
                        modifier = Modifier
                            .then(
                                if (isOnBookDetail) Modifier.navigationBarsPadding() else Modifier
                            )
                            .padding(bottom = miniPlayerBottomPadding)
                    )
                }
            }
        }

        // Bottom Navigation - 5-icon layout: Library, Search, Resume, Progress, Settings
        AnimatedVisibility(
            visible = !isPlayerExpanded && !isOnBookDetail && !isOnReader,
            modifier = Modifier
                .align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            GlassBottomBar5Icon(
                standardNavItems = standardNavItems,
                settingsNavItem = settingsNavItem,
                currentDestination = currentDestination,
                resumeBook = resumeBook,
                currentBook = currentBook,
                isPlaying = isPlaying,
                bookProgress = if (resumeBook != null && resumeBook.duration > 0) {
                    (position.toFloat() / resumeBook.duration.toFloat()).coerceIn(0f, 1f)
                } else if (resumeBook != null && resumeBook.progress > 0 && resumeBook.duration > 0) {
                    (resumeBook.progress.toFloat() / resumeBook.duration.toFloat()).coerceIn(0f, 1f)
                } else 0f,
                isReverieDark = isReverieDark,
                reverieAccentColor = accentColor,
                onNavItemSelected = { item ->
                    android.util.Log.d("Navigation", ">>> onNavItemSelected: label=${item.label}, route=${item.route}")
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateBackToSection = { route ->
                    // Navigate back to the section root, clearing sub-routes
                    navController.navigate(route) {
                        popUpTo(route) { inclusive = true }
                    }
                },
                onHomeScrollToTop = { homeScrollTrigger++ },
                onLibraryScrollToTop = { libraryScrollTrigger++ },
                onSettingsScrollToTop = { settingsScrollTrigger++ },
                onResumeClick = {
                    // Smart Resume: Load and play the book, then expand player
                    resumeBook?.let { book ->
                        if (currentBook?.id != book.id) {
                            playerViewModel.loadBook(book)
                        }
                        if (!isPlaying) {
                            playerViewModel.togglePlayback()
                        }
                        isPlayerExpanded = true
                    }
                },
                onProgressClick = {
                    // Open chapter navigation overlay
                    if (resumeBook != null) {
                        showChapterNavigation = true
                    }
                },
                isDark = isDarkTheme
            )
        }

        // Chapter Navigation Bottom Sheet
        if (showChapterNavigation && resumeBook != null) {
            ChapterNavigationSheet(
                book = resumeBook,
                currentPosition = if (currentBook?.id == resumeBook.id) position else resumeBook.progress,
                playbackSpeed = playbackSpeed,
                isReverieDark = isReverieDark,
                accentColor = accentColor,
                isDark = isDarkTheme,
                onChapterSelected = { chapter ->
                    // Load book if not current, then seek to chapter
                    if (currentBook?.id != resumeBook.id) {
                        playerViewModel.loadBook(resumeBook)
                    }
                    playerViewModel.seekTo(chapter.startMs)
                    if (!isPlaying) {
                        playerViewModel.togglePlayback()
                    }
                    showChapterNavigation = false
                    isPlayerExpanded = true
                },
                onBookmarkSelected = { positionMs ->
                    // Load book if not current, then seek to bookmark
                    if (currentBook?.id != resumeBook.id) {
                        playerViewModel.loadBook(resumeBook)
                    }
                    playerViewModel.seekTo(positionMs)
                    if (!isPlaying) {
                        playerViewModel.togglePlayback()
                    }
                    showChapterNavigation = false
                    isPlayerExpanded = true
                },
                onDismiss = { showChapterNavigation = false }
            )
        }

        // Full Player Screen
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
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
                            if (swipeDelta > 100) {
                                isPlayerExpanded = false
                            }
                            swipeDelta = 0f
                        }
                    )
            ) {
                currentBook?.let {
                    PlayerScreenGlass(
                        playerViewModel = playerViewModel,
                        isDark = isDarkTheme,
                        isReverieDark = isReverieDark,
                        reverieAccentColor = accentColor,
                        highlightColor = highlightColor,
                        useBorderHighlight = useBorderHighlight,
                        dynamicColors = dynamicPlayerColors,
                        onBack = { isPlayerExpanded = false }
                    )
                }
            }
        }

        // Add Content Bottom Sheet
        if (showAddContentSheet) {
            GlassBottomSheet(
                onDismiss = { showAddContentSheet = false },
                isDark = isDarkTheme
            ) {
                Text(
                    text = stringResource(R.string.ui_add_content),
                    style = GlassTypography.Title,
                    color = theme.textPrimary,
                    modifier = Modifier.padding(bottom = GlassSpacing.L)
                )

                GlassListItem(
                    title = "Import Files",
                    subtitle = "Audio, PDF, EPUB",
                    leadingIcon = Icons.Outlined.FileOpen,
                    isDark = isDarkTheme,
                    onClick = {
                        showAddContentSheet = false
                        fileLauncher.launch(arrayOf("audio/*", "application/pdf", "application/epub+zip"))
                    }
                )
                GlassDivider(isDark = isDarkTheme, startIndent = 56.dp)

                GlassListItem(
                    title = "Torrent Downloads",
                    subtitle = "Magnet links & .torrent files",
                    leadingIcon = Icons.Outlined.CloudDownload,
                    isDark = isDarkTheme,
                    onClick = {
                        showAddContentSheet = false
                        navController.navigate(Downloads)
                    }
                )
                GlassDivider(isDark = isDarkTheme, startIndent = 56.dp)

                GlassListItem(
                    title = "Cloud Storage",
                    subtitle = "Google Drive, Dropbox",
                    leadingIcon = Icons.Outlined.Cloud,
                    isDark = isDarkTheme,
                    onClick = {
                        showAddContentSheet = false
                        navController.navigate(CloudSync)
                    }
                )

                Spacer(modifier = Modifier.height(GlassSpacing.XXL))
            }
        }
    }

}

// ============================================================================
// TYPE-SAFE NAV ITEM
// ============================================================================

data class GlassNavItem<T : Any>(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val route: T
)

// ============================================================================
// TYPE-SAFE BOTTOM BAR
// ============================================================================

@Composable
fun GlassBottomBarTypeSafe(
    items: List<GlassNavItem<*>>,
    currentDestination: androidx.navigation.NavDestination?,
    onItemSelected: (GlassNavItem<*>) -> Unit,
    isDark: Boolean,
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark, isReverieDark)
    // Use dynamic Reverie accent in Reverie Dark mode
    val accentColor = if (isReverieDark) reverieAccentColor else theme.interactive

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M, vertical = GlassSpacing.XS)
            .clip(RoundedCornerShape(GlassShapes.Large))
            .background(
                if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.95f)
                else Color(0xFFF2F2F7).copy(alpha = 0.95f)
            )
            .padding(horizontal = GlassSpacing.S, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = when (item.route) {
                is Home -> currentDestination?.hasRoute<Home>() == true
                is Library -> currentDestination?.hasRoute<Library>() == true
                is Search -> currentDestination?.hasRoute<Search>() == true
                is Settings -> currentDestination?.hasRoute<Settings>() == true
                else -> false
            }

            GlassNavItemView(
                item = item,
                isSelected = isSelected,
                onClick = { onItemSelected(item) },
                isDark = isDark,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun GlassNavItemView(
    item: GlassNavItem<*>,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    accentColor: Color = GlassColors.Interactive
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val theme = glassTheme(isDark)
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else theme.textSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "nav_color"
    )

    // Premium spring scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 500f
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(GlassShapes.Small))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
            .padding(horizontal = GlassSpacing.M, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.icon,
            contentDescription = item.label,
            tint = animatedColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = item.label,
            style = GlassTypography.Tab,
            color = animatedColor
        )
    }
}

// ============================================================================
// GLASS MINI PLAYER - Premium Pill Design with Collapsible Circle
// ============================================================================

@Composable
fun GlassMiniPlayer(
    book: Book,
    isPlaying: Boolean,
    progress: Float,
    playbackSpeed: Float = 1.0f,
    isDark: Boolean,
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    hazeState: HazeState? = null,  // For glass blur effect
    onPlayPause: () -> Unit,
    onSpeedClick: () -> Unit = {},
    onClick: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    onScroll: Boolean = false
) {
    // State for collapsed mode - key on book.id to reset when book changes
    var isCollapsed by remember(book.id) { mutableStateOf(false) }

    // Robust timer using key-based LaunchedEffect
    var collapseTimerKey by remember { mutableStateOf(0) }

    // Handle scroll - immediate collapse
    LaunchedEffect(onScroll) {
        if (onScroll) isCollapsed = true
    }

    // Handle pause - auto-expand
    LaunchedEffect(isPlaying) {
        if (!isPlaying) isCollapsed = false
    }

    // Collapse timer - 4 seconds, restarts when key changes
    LaunchedEffect(collapseTimerKey) {
        if (isPlaying && !isCollapsed) {
            kotlinx.coroutines.delay(4000)
            isCollapsed = true
        }
    }

    // Trigger timer when expanded and playing
    LaunchedEffect(isCollapsed, isPlaying) {
        if (!isCollapsed && isPlaying) {
            collapseTimerKey++
        }
    }

    // iOS 26 fluid spring animation
    val springSpec = spring<Float>(dampingRatio = 0.8f, stiffness = 200f)

    // Progress from 0 (collapsed/circle) to 1 (expanded/pill)
    val expandProgress by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else 1f,
        animationSpec = springSpec,
        label = "expand_progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M),
        contentAlignment = Alignment.CenterStart
    ) {
        // Expanded Pill - only visible when expanding/expanded
        if (expandProgress > 0.01f) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = expandProgress
                        // Stretch from left: starts small, grows to full
                        scaleX = 0.2f + (expandProgress * 0.8f)
                        scaleY = 0.8f + (expandProgress * 0.2f)
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            ) {
                ExpandedMiniPlayerPill(
                    book = book,
                    isPlaying = isPlaying,
                    progress = progress,
                    playbackSpeed = playbackSpeed,
                    isDark = isDark,
                    isReverieDark = isReverieDark,
                    reverieAccentColor = reverieAccentColor,
                    onPlayPause = onPlayPause,
                    onSpeedClick = onSpeedClick,
                    onClick = { onClick() },
                    onDismiss = onDismiss
                )
            }
        }

        // Circle (mini mode) - only visible when collapsing/collapsed
        if (expandProgress < 0.99f) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = 1f - expandProgress
                        scaleX = 1f - (expandProgress * 0.2f)
                        scaleY = 1f - (expandProgress * 0.2f)
                    }
            ) {
                CollapsedMiniPlayerCircle(
                    book = book,
                    progress = progress,
                    isPlaying = isPlaying,
                    accentColor = reverieAccentColor,
                    isDark = isDark,
                    onExpand = {
                        isCollapsed = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CollapsedMiniPlayerCircle(
    book: Book,
    progress: Float,
    isPlaying: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onExpand: () -> Unit
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "scale"
    )

    // Pulsing glow effect when playing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer glow when playing
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(accentColor.copy(alpha = pulseAlpha * 0.3f))
            )
        }

        // Progress ring
        Canvas(modifier = Modifier.size(64.dp)) {
            val strokeWidth = 3.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            // Background ring
            drawCircle(
                color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f),
                radius = radius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            if (progress > 0f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center - book cover image (slightly larger to fill circle completely, edges cropped)
        AsyncImage(
            model = book.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onExpand()
                },
            contentScale = ContentScale.Crop
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpandedMiniPlayerPill(
    book: Book,
    isPlaying: Boolean,
    progress: Float,
    playbackSpeed: Float,
    isDark: Boolean,
    isReverieDark: Boolean,
    reverieAccentColor: Color,
    onPlayPause: () -> Unit,
    onSpeedClick: () -> Unit,
    onClick: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = androidx.compose.ui.platform.LocalView.current
    val density = LocalDensity.current

    // State for long-press and swipe gestures
    var isLongPressing by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Visual feedback animations
    val scale by animateFloatAsState(
        targetValue = when {
            isLongPressing -> 0.96f
            isDragging -> 0.98f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "mini_player_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = when {
            isLongPressing -> 0.7f
            isDragging -> 0.8f
            else -> 1f
        },
        animationSpec = tween(300),
        label = "mini_player_alpha"
    )

    // Indicator visibility for long-press
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isLongPressing) 1f else 0f,
        animationSpec = tween(200),
        label = "indicator_alpha"
    )

    val interactionSource = remember { MutableInteractionSource() }

    // Monitor long-press state for visual feedback
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    // Start monitoring - wait for long press duration
                    kotlinx.coroutines.delay(500) // Standard long-press duration
                    isLongPressing = true
                }
                is PressInteraction.Release,
                is PressInteraction.Cancel -> {
                    isLongPressing = false
                }
            }
        }
    }

    // Glass effect for mini player - semi-transparent dark with blur effect
    val glassBackground = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.92f)  // Glass effect
    } else {
        Color(0xFFF2F2F7).copy(alpha = 0.92f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                translationY = dragOffsetY
            }
            .clip(RoundedCornerShape(32.dp))
            .background(glassBackground)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        val dragThreshold = with(density) { 100.dp.toPx() }
                        if (dragOffsetY > dragThreshold) {
                            // Dismiss the mini player
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            onDismiss()
                        }
                        // Reset state
                        dragOffsetY = 0f
                        isDragging = false
                    },
                    onDragCancel = {
                        dragOffsetY = 0f
                        isDragging = false
                    },
                    onVerticalDrag = { _, dragAmount ->
                        // Only allow dragging downward
                        dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                    }
                )
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick() },
                onLongClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    onDismiss()
                },
                onLongClickLabel = "Dismiss mini player",
                onClickLabel = "Expand player"
            )
    ) {
        // Progress is shown via the ring around cover art - no bottom bar needed

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp), // Reduced for larger elements
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover art with progress ring - larger for premium feel
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                // Progress ring around cover art
                Canvas(modifier = Modifier.size(56.dp)) {
                    val strokeWidth = 2.5.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2

                    // Background ring
                    drawCircle(
                        color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f),
                        radius = radius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    if (progress > 0f) {
                        drawArc(
                            color = reverieAccentColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress.coerceIn(0f, 1f),
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Cover art - oversized to fill circle completely (no visible edges)
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .graphicsLayer { shadowElevation = 4f },
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Title & Author stacked
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = GlassTypography.Callout,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = GlassTypography.Caption,
                    color = theme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Speed indicator (if not 1x)
            if (playbackSpeed != 1.0f) {
                MiniSpeedButton(
                    speed = playbackSpeed,
                    onClick = onSpeedClick,
                    isDark = isDark,
                    isReverieDark = isReverieDark,
                    reverieAccentColor = reverieAccentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Accent-colored circular play/pause button
            PremiumMiniPlayButton(
                isPlaying = isPlaying,
                onClick = onPlayPause,
                isDark = isDark,
                isReverieDark = isReverieDark,
                reverieAccentColor = reverieAccentColor
            )
        }

        // Long-press indicator - shows down arrow when long-pressing
        if (indicatorAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { this.alpha = indicatorAlpha },
                contentAlignment = Alignment.Center
            ) {
                // Semi-transparent overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f * indicatorAlpha))
                )

                // Down arrow icon
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = stringResource(R.string.content_desc_swipe_dismiss),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun PremiumMiniPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val theme = glassTheme(isDark, isReverieDark)

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 500f
        ),
        label = "scale"
    )

    // Theme-consistent button - light bg in light mode, dark bg in dark mode
    val buttonBackground = if (isDark) {
        Color(0xFF2C2C2E)
    } else {
        Color(0xFFE5E5EA) // Light gray for light mode
    }

    // Theme-aware icon color: contrasts with background
    val iconColor = if (isReverieDark) {
        reverieAccentColor
    } else if (isDark) {
        theme.interactive
    } else {
        Color(0xFF1C1C1E) // Dark icon on light background
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(56.dp) // Match enlarged cover art
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(buttonBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = iconColor,
            modifier = Modifier.size(30.dp) // Larger icon
        )
    }
}

@Composable
private fun MiniSpeedButton(
    speed: Float,
    onClick: () -> Unit,
    isDark: Boolean,
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 500f
        ),
        label = "scale"
    )

    val buttonBg = if (isReverieDark) Color(0xFF1C1C1E).copy(alpha = 0.6f) else Color(0xFF2C2C2E).copy(alpha = 0.6f)
    val textColor = if (isReverieDark) reverieAccentColor else Color.White

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(32.dp)
            .widthIn(min = 42.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(buttonBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${speed}x",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = textColor
        )
    }
}

// ============================================================================
// GLASS BOTTOM SHEET
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismiss: () -> Unit,
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = glassTheme(isDark)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        contentColor = theme.textPrimary,
        shape = RoundedCornerShape(topStart = GlassShapes.Large, topEnd = GlassShapes.Large),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = GlassSpacing.S)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(theme.glassBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GlassSpacing.M, vertical = GlassSpacing.M),
            content = content
        )
    }
}

// ============================================================================
// GLASS DIALOG
// ============================================================================

@Composable
fun GlassDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    isDark: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    val theme = glassTheme(isDark)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = title,
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Text(
                text = message,
                style = GlassTypography.Body,
                color = theme.textSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = if (isDestructive) GlassColors.Destructive else theme.interactive,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = dismissText,
                    color = theme.textSecondary
                )
            }
        }
    )
}

// ============================================================================
// PLACEHOLDER SCREENS
// ============================================================================

@Composable
fun WelcomeScreenGlass(
    isDark: Boolean,
    onImportClick: () -> Unit,
    onTorrentClick: () -> Unit
) {
    val theme = glassTheme(isDark)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(GlassSpacing.L),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = GlassTypography.Display,
                color = theme.textPrimary
            )
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = GlassTypography.Body,
                color = theme.textSecondary
            )

            Spacer(modifier = Modifier.height(GlassSpacing.XXL))

            GlassButton(
                text = stringResource(R.string.ui_import_files),
                onClick = onImportClick,
                isDark = isDark,
                isPrimary = true,
                icon = Icons.Outlined.FileOpen,
                modifier = Modifier.fillMaxWidth()
            )

            GlassButton(
                text = stringResource(R.string.drawer_torrent),
                onClick = onTorrentClick,
                isDark = isDark,
                isPrimary = false,
                icon = Icons.Outlined.CloudDownload,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SearchScreenGlass(
    libraryViewModel: LibraryViewModel,
    isDark: Boolean,
    onBookClick: (String) -> Unit
) {
    val theme = glassTheme(isDark)
    var searchQuery by remember { mutableStateOf("") }
    val books by libraryViewModel.books.collectAsState()

    val filteredBooks = books.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.author.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(horizontal = GlassSpacing.M)
    ) {
        Spacer(modifier = Modifier.height(GlassSpacing.XXL))

        Text(
            text = stringResource(R.string.nav_search),
            style = GlassTypography.Display,
            color = theme.textPrimary
        )

        Spacer(modifier = Modifier.height(GlassSpacing.M))

        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text("Books, authors...", color = theme.textSecondary)
            },
            leadingIcon = {
                Icon(Icons.Outlined.Search, null, tint = theme.textSecondary)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.interactive,
                unfocusedBorderColor = theme.glassBorder,
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                cursorColor = theme.interactive
            ),
            shape = RoundedCornerShape(GlassShapes.Small),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(GlassSpacing.L))

        // Results
        if (searchQuery.isNotEmpty()) {
            filteredBooks.forEach { book ->
                GlassListItem(
                    title = book.title,
                    subtitle = book.author,
                    isDark = isDark,
                    onClick = { onBookClick(book.id) }
                )
                GlassDivider(isDark = isDark)
            }
        }
    }
}

// ============================================================================
// 5-ICON BOTTOM NAV BAR
// ============================================================================

@Composable
fun GlassBottomBar5Icon(
    standardNavItems: List<GlassNavItem<*>>,
    settingsNavItem: GlassNavItem<*>,
    currentDestination: androidx.navigation.NavDestination?,
    resumeBook: Book?,
    currentBook: Book?,
    isPlaying: Boolean,
    bookProgress: Float,
    isReverieDark: Boolean = false,
    reverieAccentColor: Color = GlassColors.ReverieAccent,
    onNavItemSelected: (GlassNavItem<*>) -> Unit,
    onNavigateBackToSection: (route: Any) -> Unit = {},
    onHomeScrollToTop: () -> Unit = {},
    onLibraryScrollToTop: () -> Unit = {},
    onSettingsScrollToTop: () -> Unit = {},
    onResumeClick: () -> Unit,
    onProgressClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark, isReverieDark)
    val accentColor = if (isReverieDark) reverieAccentColor else theme.interactive

    // Helper function to check if on a sub-route of a section
    fun isOnSubRoute(sectionRoute: Any): Boolean {
        return when (sectionRoute) {
            is Home -> {
                // Home section sub-routes: SeriesDetail (when navigated from Home)
                // Note: We can't distinguish where SeriesDetail was navigated from,
                // so we treat it as a sub-route of both Home and Library
                currentDestination?.hasRoute<SeriesDetail>() == true
            }
            is Library -> {
                // Library sub-routes: BookDetail, EditBook, SplitBook, Reader, SeriesDetail, AuthorBooks
                currentDestination?.hasRoute<BookDetail>() == true ||
                currentDestination?.hasRoute<EditBook>() == true ||
                currentDestination?.hasRoute<SplitBook>() == true ||
                currentDestination?.hasRoute<Reader>() == true ||
                currentDestination?.hasRoute<SeriesDetail>() == true ||
                currentDestination?.hasRoute<AuthorBooks>() == true
            }
            is Settings -> {
                // Settings sub-routes: Equalizer, Downloads, CloudSync, ListeningStats
                currentDestination?.hasRoute<Equalizer>() == true ||
                currentDestination?.hasRoute<Downloads>() == true ||
                currentDestination?.hasRoute<CloudSync>() == true ||
                currentDestination?.hasRoute<ListeningStats>() == true
            }
            else -> false
        }
    }

    // Glass effect for the pill itself
    val pillBackground = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.95f)  // Glass effect
    } else {
        Color(0xFFF2F2F7).copy(alpha = 0.95f)
    }

    // Divider color matching full player style
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)

    // Truly floating pill - no background bar, content scrolls underneath
    // The pill itself has glass background via pillBackground
    Box(
        modifier = modifier
            .fillMaxWidth()
            // No background - fully transparent, content scrolls underneath
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = GlassSpacing.M)
                .clip(RoundedCornerShape(GlassShapes.Large))
                .background(pillBackground)
                .padding(horizontal = GlassSpacing.S, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Home nav item - navigate back if on sub-route, scroll to top if on root
        val homeItem = standardNavItems[0]
        val isHomeSelected = currentDestination?.hasRoute<Home>() == true
        val isOnHomeSubRoute = isOnSubRoute(Home)
        GlassNavItemCompact(
            icon = if (isHomeSelected) homeItem.selectedIcon else homeItem.icon,
            label = homeItem.label,
            isSelected = isHomeSelected,
            onClick = {
                when {
                    isOnHomeSubRoute -> onNavigateBackToSection(Home)
                    isHomeSelected -> onHomeScrollToTop()
                    else -> onNavItemSelected(homeItem)
                }
            },
            isDark = isDark,
            accentColor = accentColor
        )

        // Divider
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .height(24.dp)
                .background(dividerColor)
        )

        // Library nav item - navigate back if on sub-route, scroll to top if on root
        val libraryItem = standardNavItems[1]
        val isLibrarySelected = currentDestination?.hasRoute<Library>() == true
        val isOnLibrarySubRoute = isOnSubRoute(Library)
        GlassNavItemCompact(
            icon = if (isLibrarySelected) libraryItem.selectedIcon else libraryItem.icon,
            label = libraryItem.label,
            isSelected = isLibrarySelected,
            onClick = {
                when {
                    isOnLibrarySubRoute -> onNavigateBackToSection(Library)
                    isLibrarySelected -> onLibraryScrollToTop()
                    else -> onNavItemSelected(libraryItem)
                }
            },
            isDark = isDark,
            accentColor = accentColor
        )

        // Divider
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .height(24.dp)
                .background(dividerColor)
        )

        // Smart Resume button
        SmartResumeNavItem(
            hasBook = resumeBook != null,
            isCurrentlyPlaying = currentBook != null && isPlaying,
            onClick = onResumeClick,
            isDark = isDark,
            isReverieDark = isReverieDark,
            accentColor = accentColor
        )

        // Divider
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .height(24.dp)
                .background(dividerColor)
        )

        // Radial Progress button
        RadialProgressNavItem(
            progress = bookProgress,
            hasBook = resumeBook != null,
            onClick = onProgressClick,
            isDark = isDark,
            isReverieDark = isReverieDark,
            accentColor = accentColor
        )

        // Divider
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .height(24.dp)
                .background(dividerColor)
        )

        // Settings nav item - navigate back if on sub-route, scroll to top if on root
        val isSettingsSelected = currentDestination?.hasRoute<Settings>() == true
        val isOnSettingsSubRoute = isOnSubRoute(Settings)
        GlassNavItemCompact(
            icon = if (isSettingsSelected) settingsNavItem.selectedIcon else settingsNavItem.icon,
            label = settingsNavItem.label,
            isSelected = isSettingsSelected,
            onClick = {
                when {
                    isOnSettingsSubRoute -> onNavigateBackToSection(Settings)
                    isSettingsSelected -> onSettingsScrollToTop()
                    else -> onNavItemSelected(settingsNavItem)
                }
            },
            isDark = isDark,
            accentColor = accentColor
        )
        }
    }
}

// Compact nav item for 5-icon layout
@Composable
private fun RowScope.GlassNavItemCompact(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    accentColor: Color = GlassColors.Interactive
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val theme = glassTheme(isDark)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else theme.textSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "nav_color"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(GlassShapes.Small))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = animatedColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = GlassTypography.Tab,
            color = animatedColor,
            maxLines = 1
        )
    }
}

// ============================================================================
// SMART RESUME NAV ITEM
// ============================================================================

@Composable
private fun RowScope.SmartResumeNavItem(
    hasBook: Boolean,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.Interactive
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val theme = glassTheme(isDark, isReverieDark)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Pulsing animation when there's a book to resume but not currently playing
    val shouldPulse = hasBook && !isCurrentlyPlaying
    val infiniteTransition = rememberInfiniteTransition(label = "resume_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Pulse between gray shades when there's a book to resume
    val baseColor = theme.textSecondary
    val animatedColor = if (shouldPulse) {
        baseColor.copy(alpha = pulseAlpha)
    } else {
        baseColor
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(GlassShapes.Small))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (hasBook) {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onClick()
                }
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = stringResource(R.string.content_desc_resume),
            tint = animatedColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.ui_resume),
            style = GlassTypography.Tab,
            color = animatedColor,
            maxLines = 1
        )
    }
}

// ============================================================================
// RADIAL PROGRESS NAV ITEM
// ============================================================================

@Composable
private fun RowScope.RadialProgressNavItem(
    progress: Float,
    hasBook: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.Interactive
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val theme = glassTheme(isDark, isReverieDark)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    // Progress ring always uses accent color
    val ringColor by animateColorAsState(
        targetValue = accentColor,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "ring_color"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(GlassShapes.Small))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (hasBook) {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onClick()
                }
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Radial progress ring
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                val strokeWidth = 2.5.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Background ring
                drawCircle(
                    color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress arc
                if (animatedProgress > 0f) {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Percentage text inside (only show if has progress)
            if (hasBook && progress > 0.01f) {
                Text(
                    text = "${(progress * 100).toInt()}",
                    style = GlassTypography.Tab.copy(fontSize = 7.sp),
                    color = ringColor
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.ui_progress),
            style = GlassTypography.Tab,
            color = theme.textSecondary,
            maxLines = 1
        )
    }
}

// ============================================================================
// CHAPTER NAVIGATION SHEET
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterNavigationSheet(
    book: Book,
    currentPosition: Long,
    playbackSpeed: Float = 1.0f,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.Interactive,
    isDark: Boolean,
    onChapterSelected: (Chapter) -> Unit,
    onBookmarkSelected: (Long) -> Unit = {},
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val chapters = book.chapters
    val bookmarks = book.bookmarks
    val bookmarkNotes = book.bookmarkNotes

    // Tab state: 0 = Chapters, 1 = Bookmarks
    var selectedTab by remember { mutableIntStateOf(0) }

    // Calculate overall book progress
    val bookProgress = if (book.duration > 0) {
        (currentPosition.toFloat() / book.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Calculate time remaining at current speed
    val timeRemainingMs = (book.duration - currentPosition).coerceAtLeast(0)
    val adjustedTimeRemaining = if (playbackSpeed > 0) (timeRemainingMs / playbackSpeed).toLong() else timeRemainingMs

    // Find current chapter
    val currentChapterIndex = chapters.indexOfLast { currentPosition >= it.startMs }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        contentColor = theme.textPrimary,
        shape = RoundedCornerShape(topStart = GlassShapes.Large, topEnd = GlassShapes.Large),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = GlassSpacing.S)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(theme.glassBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GlassSpacing.M)
        ) {
            // Header with book info and large progress ring
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = GlassSpacing.S),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large radial progress
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(64.dp)) {
                        val strokeWidth = 5.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)

                        // Background ring
                        drawCircle(
                            color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Progress arc
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = 360f * bookProgress,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Text(
                        text = "${(bookProgress * 100).toInt()}%",
                        style = GlassTypography.Body,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.width(GlassSpacing.M))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        style = GlassTypography.Headline,
                        color = theme.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Time remaining with speed adjustment
                    val speedText = if (playbackSpeed != 1.0f) " at ${playbackSpeed}x" else ""
                    Text(
                        text = "${formatDuration(adjustedTimeRemaining)} left$speedText",
                        style = GlassTypography.Caption,
                        color = accentColor
                    )
                    if (currentChapterIndex >= 0 && currentChapterIndex < chapters.size) {
                        Text(
                            text = "Ch ${currentChapterIndex + 1}/${chapters.size}: ${chapters[currentChapterIndex].title}",
                            style = GlassTypography.Caption,
                            color = theme.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Tab selector (Chapters / Bookmarks)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = GlassSpacing.S),
                horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
            ) {
                ProgressTabButton(
                    text = stringResource(R.string.player_chapters),
                    count = chapters.size,
                    isSelected = selectedTab == 0,
                    accentColor = accentColor,
                    isDark = isDark,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                ProgressTabButton(
                    text = stringResource(R.string.bookmarks_title),
                    count = bookmarks.size,
                    isSelected = selectedTab == 1,
                    accentColor = accentColor,
                    isDark = isDark,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = theme.glassBorder)

            // Tab content
            when (selectedTab) {
                0 -> {
                    // Chapters tab
                    if (chapters.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.MenuBook,
                                    contentDescription = null,
                                    tint = theme.textTertiary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(GlassSpacing.S))
                                Text(
                                    text = stringResource(R.string.ui_no_chapters),
                                    style = GlassTypography.Body,
                                    color = theme.textSecondary
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            contentPadding = PaddingValues(vertical = GlassSpacing.S)
                        ) {
                            items(chapters.size) { index ->
                                val chapter = chapters[index]
                                val isCurrent = index == currentChapterIndex
                                val isCompleted = currentPosition > chapter.endMs

                                ChapterNavItem(
                                    chapter = chapter,
                                    index = index + 1,
                                    isCurrent = isCurrent,
                                    isCompleted = isCompleted && !isCurrent,
                                    accentColor = accentColor,
                                    isDark = isDark,
                                    isReverieDark = isReverieDark,
                                    onClick = { onChapterSelected(chapter) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Bookmarks tab
                    if (bookmarks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = theme.textTertiary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(GlassSpacing.S))
                                Text(
                                    text = stringResource(R.string.bookmarks_none),
                                    style = GlassTypography.Body,
                                    color = theme.textSecondary
                                )
                                Text(
                                    text = stringResource(R.string.bookmarks_add_while_listening),
                                    style = GlassTypography.Caption,
                                    color = theme.textTertiary
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            contentPadding = PaddingValues(vertical = GlassSpacing.S)
                        ) {
                            items(bookmarks.sorted()) { positionMs ->
                                val note = bookmarkNotes[positionMs]
                                BookmarkNavItem(
                                    positionMs = positionMs,
                                    note = note,
                                    isCurrent = currentPosition >= positionMs - 5000 && currentPosition <= positionMs + 5000,
                                    accentColor = accentColor,
                                    isDark = isDark,
                                    isReverieDark = isReverieDark,
                                    onClick = { onBookmarkSelected(positionMs) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(GlassSpacing.XXL))
        }
    }
}

@Composable
private fun ProgressTabButton(
    text: String,
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GlassShapes.Small))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.15f)
                else if (isDark) Color.White.copy(alpha = 0.05f)
                else Color.Black.copy(alpha = 0.05f)
            )
            .clickable(onClick = onClick)
            .padding(vertical = GlassSpacing.S),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = GlassTypography.Callout,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) accentColor else theme.textSecondary
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "($count)",
                    style = GlassTypography.Caption,
                    color = if (isSelected) accentColor.copy(alpha = 0.7f) else theme.textTertiary
                )
            }
        }
    }
}

@Composable
private fun BookmarkNavItem(
    positionMs: Long,
    note: String?,
    isCurrent: Boolean,
    accentColor: Color,
    isDark: Boolean,
    isReverieDark: Boolean,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val highlightColor = if (isReverieDark) GlassColors.WarmSlate else Color.White.copy(alpha = 0.1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Small))
            .then(
                if (isCurrent) Modifier.background(highlightColor)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = GlassSpacing.S, vertical = GlassSpacing.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bookmark icon
        Icon(
            imageVector = if (isCurrent) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = null,
            tint = if (isCurrent) accentColor else theme.textSecondary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(GlassSpacing.S))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatDuration(positionMs),
                style = GlassTypography.Body,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent) accentColor else theme.textPrimary
            )
            if (!note.isNullOrBlank()) {
                Text(
                    text = note,
                    style = GlassTypography.Caption,
                    color = theme.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isCurrent) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.content_desc_near_position),
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ChapterNavItem(
    chapter: Chapter,
    index: Int,
    isCurrent: Boolean,
    isCompleted: Boolean,
    accentColor: Color,
    isDark: Boolean,
    isReverieDark: Boolean,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val highlightColor = if (isReverieDark) GlassColors.WarmSlate else Color.White.copy(alpha = 0.1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Small))
            .then(
                if (isCurrent) Modifier.background(highlightColor)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = GlassSpacing.S, vertical = GlassSpacing.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chapter number / status indicator
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    when {
                        isCurrent -> accentColor
                        isCompleted -> accentColor.copy(alpha = 0.3f)
                        else -> theme.glassSecondary
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted && !isCurrent) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = stringResource(R.string.content_desc_completed),
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = "$index",
                    style = GlassTypography.Caption,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrent) Color.White else theme.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(GlassSpacing.S))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.title,
                style = GlassTypography.Body,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent) accentColor else theme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val chapterDuration = chapter.endMs - chapter.startMs
            if (chapterDuration > 0) {
                Text(
                    text = formatDuration(chapterDuration),
                    style = GlassTypography.Caption,
                    color = theme.textSecondary
                )
            }
        }

        if (isCurrent) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.content_desc_current),
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

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
