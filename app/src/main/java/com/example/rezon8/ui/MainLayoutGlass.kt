package com.mossglen.lithos.ui

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
import com.mossglen.lithos.IncomingTorrentData
import com.mossglen.lithos.IncomingFileData
import com.mossglen.lithos.R
import com.mossglen.lithos.data.Book
import com.mossglen.lithos.data.Chapter
import com.mossglen.lithos.navigation.*
import com.mossglen.lithos.ui.components.*
import com.mossglen.lithos.ui.screens.*
import com.mossglen.lithos.ui.theme.*
import com.mossglen.lithos.ui.viewmodel.*
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
    val isOLED = themeMode == ThemeMode.LITHOS_DARK
    val theme = glassTheme(isDarkTheme, isOLED)

    // Haze state for true iOS 26 / Android 16 glass blur effect
    val hazeState = remember { HazeState() }

    // Accent color - uses selected variant for Reverie Dark, blue for standard
    val lithosAccentVariant by themeViewModel.lithosAccentVariant
    val accentColor = if (isOLED) {
        themeViewModel.getLithosAccentColor()
    } else {
        GlassColors.Interactive
    }

    // Highlight color for selections - warm slate or subtle copper based on variant
    val highlightColor = if (isOLED) {
        themeViewModel.getLithosHighlightColor()
    } else {
        Color.White.copy(alpha = 0.1f)
    }
    val useBorderHighlight = isOLED && themeViewModel.useBorderHighlight()

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

    // Track when user navigated away from the player (to return on back)
    var navigatedFromPlayer by remember { mutableStateOf(false) }

    // Mini player dismiss state - remember which book was dismissed
    var dismissedBookId by remember { mutableStateOf<String?>(null) }

    // Scroll-to-top triggers - increment to trigger scroll
    var homeScrollTrigger by remember { mutableIntStateOf(0) }
    var libraryScrollTrigger by remember { mutableIntStateOf(0) }
    var settingsScrollTrigger by remember { mutableIntStateOf(0) }

    // Pill auto-hide on scroll (like reader)
    var showPill by remember { mutableStateOf(true) }
    var lastPillScrollTime by remember { mutableStateOf(0L) }

    // Show pill after 2 seconds of scroll inactivity
    LaunchedEffect(showPill) {
        if (!showPill) {
            while (true) {
                kotlinx.coroutines.delay(500)
                val timeSinceScroll = System.currentTimeMillis() - lastPillScrollTime
                if (timeSinceScroll > 2000) {
                    showPill = true
                    break
                }
            }
        }
    }

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
    val isOnNow = currentDestination?.hasRoute<Now>() == true
    val isOnLibrary = currentDestination?.hasRoute<Library>() == true
    val isOnJourney = currentDestination?.hasRoute<Journey>() == true
    val isOnSearch = currentDestination?.hasRoute<Search>() == true
    val isOnSettings = currentDestination?.hasRoute<Settings>() == true
    val isOnWelcome = currentDestination?.hasRoute<Welcome>() == true
    val isOnBookDetail = currentDestination?.hasRoute<BookDetail>() == true
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
        (isOnHome || isOnNow || isOnLibrary || isOnJourney || isOnSearch || isOnSettings || isOnBookDetail)
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
                        // Navigate to book detail
                        navController.navigate(BookDetail(book.id))
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
    // Per PROJECT_MANIFEST: [Now] [Browse] [Resume] [Search] [Profile]
    // Journey removed from main nav (accessible from Now page and Profile)
    val standardNavItems = listOf(
        GlassNavItem(
            icon = Icons.Outlined.Headphones,
            selectedIcon = Icons.Filled.Headphones,
            label = stringResource(R.string.nav_now),
            route = Now  // Immersive Now screen - the listening sanctuary
        ),
        GlassNavItem(
            icon = Icons.Outlined.LibraryBooks,
            selectedIcon = Icons.Filled.LibraryBooks,
            label = stringResource(R.string.nav_browse),
            route = Library  // Pure browsing experience
        ),
        GlassNavItem(
            icon = Icons.Outlined.Search,
            selectedIcon = Icons.Filled.Search,
            label = stringResource(R.string.nav_search),
            route = Search  // Universal search
        ),
        GlassNavItem(
            icon = Icons.Outlined.Person,
            selectedIcon = Icons.Filled.Person,
            label = stringResource(R.string.nav_profile),
            route = Profile  // Settings, stats, achievements
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
        // Start with Now if books exist, otherwise Library
        val startRoute = if (books.isNotEmpty()) Now else Library

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
                    isOLED = isOLED,
                    accentColor = accentColor,
                    scrollToTopTrigger = homeScrollTrigger,
                    onBookClick = { bookId ->
                        navController.navigate(BookDetail(bookId))
                    },
                    onPlayBook = { book ->
                        playerViewModel.checkAndShowResumeDialog(book)
                        isPlayerExpanded = true  // Open full player directly
                    },
                    onSeriesClick = { seriesName ->
                        navController.navigate(SeriesDetail(seriesName))
                    },
                    onAuthorClick = { authorName ->
                        navController.navigate(AuthorBooks(authorName))
                    },
                    onGenreClick = { genre ->
                        navController.navigate(GenreBooks(genre))
                    },
                    onScrolling = { isScrollingDown ->
                        // Smart mini player: collapse when scrolling down, expand when scrolling up
                        shouldCollapseMiniPlayer = isScrollingDown
                    }
                )
            }

            // Now - Immersive listening sanctuary (separated from Library)
            composable<Now> {
                NowScreenGlass(
                    isDark = isDarkTheme,
                    isOLED = isOLED,
                    accentColor = accentColor,
                    onPlayBook = { book ->
                        playerViewModel.checkAndShowResumeDialog(book)
                        isPlayerExpanded = true  // Open full player directly
                    },
                    onBookClick = { bookId ->
                        navController.navigate(BookDetail(bookId))
                    },
                    onRecentBookClick = { book ->
                        playerViewModel.checkAndShowResumeDialog(book)
                        isPlayerExpanded = true  // Open full player directly
                    },
                    onSettingsClick = {
                        navController.navigate(Settings)
                    },
                    onAuthorClick = { authorName ->
                        navController.navigate(AuthorBooks(authorName))
                    },
                    onSeriesClick = { seriesName ->
                        navController.navigate(SeriesDetail(seriesName))
                    },
                    onStatsClick = {
                        navController.navigate(Journey)
                    },
                    onScrollUp = {
                        showPill = false
                        lastPillScrollTime = System.currentTimeMillis()
                    },
                    onScrollDown = {
                        showPill = true
                    }
                )
            }

            composable<Journey> {
                JourneyScreenGlass(
                    isDark = isDarkTheme,
                    isOLED = isOLED,
                    accentColor = accentColor,
                    onSettingsClick = {
                        navController.navigate(Settings)
                    },
                    onStatsClick = {
                        navController.navigate(ListeningStats)
                    },
                    onAchievementsClick = {
                        navController.navigate(ListeningStats)  // Same screen shows achievements
                    }
                )
            }

            composable<Library> {
                LibraryScreenGlass(
                    libraryViewModel = libraryViewModel,
                    isDark = isDarkTheme,
                    isOLED = isOLED,
                    accentColor = accentColor,
                    scrollToTopTrigger = libraryScrollTrigger,
                    onBookClick = { bookId ->
                        navController.navigate(BookDetail(bookId))
                    },
                    onPlayBook = { book ->
                        playerViewModel.checkAndShowResumeDialog(book)
                        isPlayerExpanded = true  // Open full player directly
                    },
                    onEditBook = { bookId ->
                        navController.navigate(EditBook(bookId))
                    },
                    onAddClick = { showAddContentSheet = true },
                    onSearchClick = { navController.navigate(Search) },
                    onSeriesClick = { seriesName ->
                        navController.navigate(SeriesDetail(seriesName))
                    },
                    onAuthorClick = { authorName ->
                        navController.navigate(AuthorBooks(authorName))
                    },
                    onGenreClick = { genre ->
                        navController.navigate(GenreBooks(genre))
                    },
                    onScrollUp = {
                        showPill = false
                        lastPillScrollTime = System.currentTimeMillis()
                    },
                    onScrollDown = {
                        showPill = true
                    }
                )
            }

            composable<Search> {
                SearchScreenGlass(
                    libraryViewModel = libraryViewModel,
                    isDark = isDarkTheme,
                    onBookClick = { bookId ->
                        navController.navigate(BookDetail(bookId))
                    },
                    onSettingsClick = {
                        navController.navigate(Settings) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSection = { section ->
                        when (section) {
                            "equalizer" -> navController.navigate(Equalizer)
                            "downloads" -> navController.navigate(Downloads)
                            "cloudSync" -> navController.navigate(CloudSync)
                            "stats" -> navController.navigate(ListeningStats)
                            "sleepTimer", "speed", "import", "theme", "about" -> {
                                // Navigate to settings for these
                                navController.navigate(Settings)
                            }
                        }
                    }
                )
            }

            // Profile - Stats, settings, achievements hub
            composable<Profile> {
                ProfileScreenGlass(
                    isDark = isDarkTheme,
                    isOLED = isOLED,
                    accentColor = accentColor,
                    onSettingsClick = { navController.navigate(Settings) },
                    onStatsClick = { navController.navigate(ListeningStats) },
                    onJourneyClick = { navController.navigate(Journey) },
                    onAchievementsClick = { navController.navigate(ListeningStats) },
                    onDownloadsClick = { navController.navigate(Downloads) },
                    onCloudClick = { navController.navigate(CloudSync) },
                    onEqualizerClick = { navController.navigate(Equalizer) }
                )
            }

            composable<Settings> {
                SettingsScreenGlass(
                    isDark = isDarkTheme,
                    themeViewModel = themeViewModel,
                    scrollToTopTrigger = settingsScrollTrigger,
                    onBack = {
                        navController.popBackStack()
                        if (navigatedFromPlayer) {
                            navigatedFromPlayer = false
                            isPlayerExpanded = true
                        }
                    },
                    onOpenEqualizer = { navController.navigate(Equalizer) },
                    onNavigateToDownloads = { navController.navigate(Downloads) },
                    onNavigateToCloudFiles = { navController.navigate(CloudSync) },
                    onNavigateToStats = { navController.navigate(ListeningStats) }
                )
            }

            composable<Downloads> {
                DownloadsScreen(
                    isDark = isDarkTheme,
                    isOLED = isOLED,
                    accentColor = accentColor,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<BookDetail> { backStackEntry ->
                val route: BookDetail = backStackEntry.toRoute()
                BookDetailScreen(
                    bookId = route.bookId,
                    accentColor = accentColor,
                    isDark = isDarkTheme,
                    isOLED = isOLED,
                    onBack = { navController.popBackStack() },
                    onPlayBook = { book ->
                        // Play/pause without expanding - user can tap mini player to expand
                        if (currentBook?.id == book.id) {
                            // Same book - just toggle playback
                            playerViewModel.togglePlayback()
                        } else {
                            // Different book - check for resume dialog
                            playerViewModel.checkAndShowResumeDialog(book)
                            isPlayerExpanded = true  // Open full player directly
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

            composable<EditBook> { backStackEntry ->
                val route: EditBook = backStackEntry.toRoute()
                EditBookScreen(
                    bookId = route.bookId,
                    accentColor = accentColor,
                    isOLED = isOLED,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<SplitBook> { backStackEntry ->
                val route: SplitBook = backStackEntry.toRoute()
                SplitBookScreen(
                    bookId = route.bookId,
                    accentColor = accentColor,
                    isOLED = isOLED,
                    onBack = { navController.popBackStack() },
                    onSplitComplete = {
                        // Navigate back to library after successful split
                        navController.navigate(Library) {
                            popUpTo(Library) { inclusive = true }
                        }
                    }
                )
            }

            composable<CarMode> {
                CarModeScreen(
                    onExitCarMode = {
                        navController.popBackStack()
                        if (navigatedFromPlayer) {
                            navigatedFromPlayer = false
                            isPlayerExpanded = true
                        }
                    }
                )
            }

            composable<CloudSync> {
                CloudFileBrowserScreen(
                    isDark = isDarkTheme,
                    isOLED = isOLED,
                    reverieAccentColor = accentColor,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<ListeningStats> {
                ListeningStatsScreen(
                    isDark = isDarkTheme,
                    isOLED = isOLED,
                    accentColor = accentColor,
                    onBack = {
                        navController.popBackStack()
                        if (navigatedFromPlayer) {
                            navigatedFromPlayer = false
                            isPlayerExpanded = true
                        }
                    }
                )
            }

            composable<AuthorBooks> { backStackEntry ->
                val route: AuthorBooks = backStackEntry.toRoute()
                AuthorBooksScreen(
                    authorName = route.authorName,
                    libraryViewModel = libraryViewModel,
                    isDark = isDarkTheme,
                    isOLED = isOLED,
                    accentColor = accentColor,
                    onBack = {
                        navController.popBackStack()
                        if (navigatedFromPlayer) {
                            navigatedFromPlayer = false
                            isPlayerExpanded = true
                        }
                    },
                    onBookClick = { bookId ->
                        navController.navigate(BookDetail(bookId))
                    }
                )
            }

            composable<GenreBooks> { backStackEntry ->
                val route: GenreBooks = backStackEntry.toRoute()
                GenreBooksScreen(
                    genre = route.genre,
                    libraryViewModel = libraryViewModel,
                    isDark = isDarkTheme,
                    isOLED = isOLED,
                    accentColor = accentColor,
                    onBack = { navController.popBackStack() },
                    onBookClick = { bookId ->
                        navController.navigate(BookDetail(bookId))
                    }
                )
            }

            composable<Equalizer> {
                EqualizerScreen(
                    isDark = isDarkTheme,
                    isOLED = isOLED,
                    accentColor = accentColor,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<ChapterList> { backStackEntry ->
                val route: ChapterList = backStackEntry.toRoute()
                ChapterListScreen(
                    bookId = route.bookId,
                    isDark = isDarkTheme,
                    isOLED = isOLED,
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
                    isOLED = isOLED,
                    accentColor = accentColor,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<SeriesDetail> { backStackEntry ->
                val route: SeriesDetail = backStackEntry.toRoute()
                SeriesDetailScreen(
                    seriesName = route.seriesName,
                    isDark = isDarkTheme,
                    onBackClick = {
                        navController.popBackStack()
                        if (navigatedFromPlayer) {
                            navigatedFromPlayer = false
                            isPlayerExpanded = true
                        }
                    },
                    onBookClick = { bookId ->
                        navController.navigate(BookDetail(bookId))
                    },
                    onPlayBook = { book ->
                        playerViewModel.checkAndShowResumeDialog(book)
                        isPlayerExpanded = true  // Open full player directly
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
        // Must account for actual navigation bar height which varies by device
        val navBarInsets = WindowInsets.navigationBars.asPaddingValues()
        val navBarHeight = navBarInsets.calculateBottomPadding()
        val pillBarHeight = 64.dp
        val miniPlayerBottomPadding = if (isOnBookDetail) {
            0.dp // No pill bar, mini player handles navigationBarsPadding itself
        } else {
            // Pill bar height + nav bar insets + spacing above pill
            pillBarHeight + navBarHeight + 16.dp
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
                        isOLED = isOLED,
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
        // Auto-hides on scroll up (like reader), reappears after 2s inactivity or scroll down
        AnimatedVisibility(
            visible = !isPlayerExpanded && !isOnBookDetail && showPill,
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
                isOLED = isOLED,
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
                    // Navigate back to the section root by navigating with popUpTo
                    // This clears the sub-routes and returns to the main section
                    android.util.Log.d("Navigation", ">>> onNavigateBackToSection: $route")
                    navController.navigate(route) {
                        // Pop up to the start destination, removing all sub-routes
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onHomeScrollToTop = { homeScrollTrigger++ },
                onLibraryScrollToTop = { libraryScrollTrigger++ },
                onSettingsScrollToTop = { settingsScrollTrigger++ },
                onResumeClick = {
                    // Resume audiobook playback
                    resumeBook?.let { book ->
                        // Load and expand player
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
                onSearchClick = {
                    navController.navigate(Search) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
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
                isOLED = isOLED,
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
                        isOLED = isOLED,
                        reverieAccentColor = accentColor,
                        highlightColor = highlightColor,
                        useBorderHighlight = useBorderHighlight,
                        dynamicColors = dynamicPlayerColors,
                        onBack = { isPlayerExpanded = false },
                        onSettingsClick = {
                            navigatedFromPlayer = true
                            isPlayerExpanded = false
                            navController.navigate(Settings)
                        },
                        onListeningStatsClick = {
                            navigatedFromPlayer = true
                            isPlayerExpanded = false
                            navController.navigate(ListeningStats)
                        },
                        onCarModeClick = {
                            navigatedFromPlayer = true
                            isPlayerExpanded = false
                            navController.navigate(CarMode)
                        },
                        onAuthorClick = { authorName ->
                            navigatedFromPlayer = true
                            isPlayerExpanded = false
                            navController.navigate(AuthorBooks(authorName))
                        },
                        onSeriesClick = { seriesName ->
                            navigatedFromPlayer = true
                            isPlayerExpanded = false
                            navController.navigate(SeriesDetail(seriesName))
                        }
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
    isOLED: Boolean = false,
    reverieAccentColor: Color = GlassColors.LithosAccent,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark, isOLED)
    // Use dynamic Reverie accent in Reverie Dark mode
    val accentColor = if (isOLED) reverieAccentColor else theme.interactive

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
    isOLED: Boolean = false,
    reverieAccentColor: Color = GlassColors.LithosAccent,
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
                    isOLED = isOLED,
                    reverieAccentColor = reverieAccentColor,
                    onPlayPause = onPlayPause,
                    onSpeedClick = onSpeedClick,
                    onClick = { onClick() },
                    onDismiss = onDismiss,
                    onCollapse = { isCollapsed = true }  // Tap cover to shrink
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
                    },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollapsedMiniPlayerCircle(
    book: Book,
    progress: Float,
    isPlaying: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit = {}
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
        // Tap to expand, long-press to dismiss
        AsyncImage(
            model = book.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        onExpand()
                    },
                    onLongClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        onDismiss()
                    }
                ),
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
    isOLED: Boolean,
    reverieAccentColor: Color,
    onPlayPause: () -> Unit,
    onSpeedClick: () -> Unit,
    onClick: () -> Unit,
    onDismiss: () -> Unit = {},
    onCollapse: () -> Unit = {}  // Tap cover to shrink
) {
    val theme = glassTheme(isDark, isOLED)
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

    // Subtle pill border - matches eReader pill style for premium feel
    val pillBorderColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)  // Subtle neutral border
    } else {
        Color.Black.copy(alpha = 0.08f)
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
            .border(
                width = 0.5.dp,
                color = pillBorderColor,
                shape = RoundedCornerShape(32.dp)
            )
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

                // Cover art - tap to shrink, oversized to fill circle completely
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = "Tap to shrink",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .graphicsLayer { shadowElevation = 4f }
                        .clickable {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            onCollapse()
                        },
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
                    isOLED = isOLED,
                    reverieAccentColor = reverieAccentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Accent-colored circular play/pause button
            PremiumMiniPlayButton(
                isPlaying = isPlaying,
                onClick = onPlayPause,
                isDark = isDark,
                isOLED = isOLED,
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
    isOLED: Boolean = false,
    reverieAccentColor: Color = GlassColors.LithosAccent
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val theme = glassTheme(isDark, isOLED)

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
    val iconColor = if (isOLED) {
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
    isOLED: Boolean = false,
    reverieAccentColor: Color = GlassColors.LithosAccent
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

    val buttonBg = if (isOLED) Color(0xFF1C1C1E).copy(alpha = 0.6f) else Color(0xFF2C2C2E).copy(alpha = 0.6f)
    val textColor = if (isOLED) reverieAccentColor else Color.White

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
    onBookClick: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    onNavigateToSection: (String) -> Unit = {}
) {
    val theme = glassTheme(isDark)
    var searchQuery by remember { mutableStateOf("") }
    val books by libraryViewModel.books.collectAsState()
    val isSearching = searchQuery.isNotEmpty()

    // All searchable features: (icon, title, subtitle, keywords, action)
    val searchableFeatures: List<Pair<Pair<androidx.compose.ui.graphics.vector.ImageVector, String>, Triple<String, List<String>, String>>> = remember {
        listOf(
            (Icons.Outlined.Settings to "Settings") to Triple("App preferences", listOf("settings", "preferences", "config"), "settings"),
            (Icons.Outlined.Tune to "Equalizer") to Triple("Audio EQ and presets", listOf("equalizer", "eq", "audio", "bass", "treble"), "equalizer"),
            (Icons.Outlined.Timer to "Sleep Timer") to Triple("Auto-pause playback", listOf("sleep", "timer", "bedtime", "pause"), "sleepTimer"),
            (Icons.Outlined.Download to "Downloads") to Triple("Torrent downloads", listOf("download", "torrent", "magnet"), "downloads"),
            (Icons.Outlined.Cloud to "Cloud Sync") to Triple("Google Drive & Dropbox", listOf("cloud", "sync", "drive", "dropbox", "backup"), "cloudSync"),
            (Icons.Outlined.Speed to "Playback Speed") to Triple("Adjust audio speed", listOf("speed", "playback", "fast", "slow"), "speed"),
            (Icons.Outlined.FolderOpen to "Import Files") to Triple("Add books from device", listOf("import", "file", "folder", "add", "scan"), "import"),
            (Icons.Outlined.Palette to "Theme") to Triple("Dark mode and colors", listOf("theme", "dark", "light", "color", "mode"), "theme"),
            (Icons.Outlined.Info to "About") to Triple("App info and version", listOf("about", "version", "info"), "about"),
            (Icons.Outlined.BarChart to "Listening Stats") to Triple("Your progress", listOf("stats", "statistics", "progress", "journey", "goal"), "stats")
        )
    }

    // Filter books based on query - requires at least 2 characters
    val filteredBooks = if (searchQuery.length >= 2) {
        books.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.author.contains(searchQuery, ignoreCase = true)
        }
    } else emptyList()

    // Filter features based on query - prefix matching like iOS/Android
    // Requires at least 2 characters and matches from start of words
    val matchedFeatures = if (isSearching && searchQuery.length >= 2) {
        val query = searchQuery.lowercase().trim()
        searchableFeatures.filter { (iconTitle, subtitleKeywordsAction) ->
            val (_, title) = iconTitle
            val (_, keywords, _) = subtitleKeywordsAction
            // Match if any keyword STARTS with the query
            keywords.any { it.startsWith(query) } ||
            // Or if title starts with query
            title.lowercase().startsWith(query) ||
            // Or if any word in title starts with query
            title.split(" ").any { it.lowercase().startsWith(query) }
        }
    } else emptyList()

    val hasResults = filteredBooks.isNotEmpty() || matchedFeatures.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        // Results area (scrollable) - full screen
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // Proper status bar handling
                .padding(horizontal = GlassSpacing.M),
            contentPadding = PaddingValues(
                // Generous top padding when showing search results to prevent cutoff
                top = if (isSearching) 72.dp else 24.dp,
                bottom = 200.dp // Space for search bar + nav bar (increased)
            )
        ) {
            // Header and hints - fades out when searching (not sliding)
            item(key = "header_content") {
                AnimatedVisibility(
                    visible = !isSearching,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.nav_search),
                                style = GlassTypography.Display,
                                color = theme.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Find books, authors, and settings",
                                style = GlassTypography.Caption,
                                color = theme.textSecondary
                            )
                        }

                        // Hints label
                        Text(
                            text = "Try searching for...",
                            style = GlassTypography.Caption.copy(fontWeight = FontWeight.Medium),
                            color = theme.textSecondary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                        )

                        // Suggestion chips
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Books", "Authors", "Settings", "Equalizer", "Downloads", "Theme").forEach { hint ->
                                SuggestionChip(
                                    onClick = { searchQuery = hint.lowercase() },
                                    label = { Text(hint, style = GlassTypography.Caption) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                                        labelColor = theme.textPrimary
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }
            }

            // Search results
            if (isSearching) {
                // Feature results
                if (matchedFeatures.isNotEmpty()) {
                    item(key = "features_header") {
                        Text(
                            text = "Features",
                            style = GlassTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                            color = theme.textSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(matchedFeatures, key = { it.first.second }) { (iconTitle, subtitleKeywordsAction) ->
                        val (icon, title) = iconTitle
                        val (subtitle, _, action) = subtitleKeywordsAction
                        GlassListItem(
                            title = title,
                            subtitle = subtitle,
                            leadingIcon = icon,
                            isDark = isDark,
                            onClick = {
                                when (action) {
                                    "settings" -> onSettingsClick()
                                    else -> onNavigateToSection(action)
                                }
                            }
                        )
                    }
                }

                // Book results
                if (filteredBooks.isNotEmpty()) {
                    item(key = "books_header") {
                        Text(
                            text = "Books (${filteredBooks.size})",
                            style = GlassTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                            color = theme.textSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(filteredBooks.take(20), key = { it.id }) { book ->
                        GlassListItem(
                            title = book.title,
                            subtitle = book.author,
                            isDark = isDark,
                            onClick = { onBookClick(book.id) }
                        )
                    }
                }

                // No results
                if (!hasResults) {
                    item(key = "no_results") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SearchOff,
                                contentDescription = null,
                                tint = theme.textSecondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No results for \"$searchQuery\"",
                                style = GlassTypography.Body,
                                color = theme.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // Search field at bottom (modern mobile UX)
        // Must account for nav bar height which varies by device
        val navBarInsets = WindowInsets.navigationBars.asPaddingValues()
        val navBarHeight = navBarInsets.calculateBottomPadding()
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            theme.background.copy(alpha = 0.95f),
                            theme.background
                        ),
                        startY = 0f,
                        endY = 80f
                    )
                )
                .padding(horizontal = GlassSpacing.M)
                .padding(bottom = 80.dp + navBarHeight, top = 12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search...", color = theme.textSecondary, style = GlassTypography.Body)
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, null, tint = theme.textSecondary, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.Clear, "Clear", tint = theme.textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.interactive,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary,
                    cursorColor = theme.interactive,
                    focusedContainerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                    unfocusedContainerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                textStyle = GlassTypography.Body
            )
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
    isOLED: Boolean = false,
    reverieAccentColor: Color = GlassColors.LithosAccent,
    onNavItemSelected: (GlassNavItem<*>) -> Unit,
    onNavigateBackToSection: (route: Any) -> Unit = {},
    onHomeScrollToTop: () -> Unit = {},
    onLibraryScrollToTop: () -> Unit = {},
    onSettingsScrollToTop: () -> Unit = {},
    onResumeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark, isOLED)
    val accentColor = if (isOLED) reverieAccentColor else theme.interactive

    // Helper function to check if on a sub-route of a section
    fun isOnSubRoute(sectionRoute: Any): Boolean {
        return when (sectionRoute) {
            is Now -> {
                // Now section sub-routes: SeriesDetail, AuthorBooks (when navigated from Now)
                currentDestination?.hasRoute<SeriesDetail>() == true ||
                currentDestination?.hasRoute<AuthorBooks>() == true ||
                currentDestination?.hasRoute<BookDetail>() == true
            }
            is Journey -> {
                // Journey section sub-routes: ListeningStats details
                currentDestination?.hasRoute<ListeningStats>() == true
            }
            is Library -> {
                // Library sub-routes: BookDetail, EditBook, SplitBook, SeriesDetail, AuthorBooks
                currentDestination?.hasRoute<BookDetail>() == true ||
                currentDestination?.hasRoute<EditBook>() == true ||
                currentDestination?.hasRoute<SplitBook>() == true ||
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

    // Subtle pill border - matches eReader pill style for premium feel
    val pillBorderColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)  // Subtle neutral border
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    // Divider color matching full player style
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)

    // Truly floating pill - no background bar, content scrolls underneath
    // The pill itself has glass background via pillBackground with subtle border
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
                .border(
                    width = 0.5.dp,
                    color = pillBorderColor,
                    shape = RoundedCornerShape(GlassShapes.Large)
                )
                .padding(horizontal = GlassSpacing.S, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Now nav item - The listening sanctuary
        // ALWAYS navigate to Now when clicked, regardless of current location
        // This ensures sub-pages (Journey, etc.) properly return to Now
        val nowItem = standardNavItems[0]
        val isNowSelected = currentDestination?.hasRoute<Now>() == true
        GlassNavItemCompact(
            icon = if (isNowSelected) nowItem.selectedIcon else nowItem.icon,
            label = nowItem.label,
            isSelected = isNowSelected,
            onClick = {
                // Always navigate first to ensure we get to Now
                onNavItemSelected(nowItem)
                // Then scroll to top if we were already there
                if (isNowSelected) {
                    onHomeScrollToTop()
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

        // Browse/Library nav item - Pure browsing experience
        // ALWAYS navigate to Library when clicked, regardless of current location
        // This ensures sub-pages (BookDetail, SeriesDetail, etc.) properly return to Library
        val libraryItem = standardNavItems[1]
        val isLibrarySelected = currentDestination?.hasRoute<Library>() == true
        GlassNavItemCompact(
            icon = if (isLibrarySelected) libraryItem.selectedIcon else libraryItem.icon,
            label = libraryItem.label,
            isSelected = isLibrarySelected,
            onClick = {
                // Always navigate first to ensure we get to Library
                onNavItemSelected(libraryItem)
                // Then scroll to top if we were already there
                if (isLibrarySelected) {
                    onLibraryScrollToTop()
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

        // Smart Resume button (CENTER per manifest)
        SmartResumeNavItem(
            hasBook = resumeBook != null,
            isCurrentlyPlaying = currentBook != null && isPlaying,
            onClick = onResumeClick,
            isDark = isDark,
            isOLED = isOLED,
            accentColor = accentColor
        )

        // Divider
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .height(24.dp)
                .background(dividerColor)
        )

        // Search nav item (per manifest: universal search)
        val searchItem = standardNavItems[2]
        val isSearchSelected = currentDestination?.hasRoute<Search>() == true
        GlassNavItemCompact(
            icon = if (isSearchSelected) searchItem.selectedIcon else searchItem.icon,
            label = searchItem.label,
            isSelected = isSearchSelected,
            onClick = {
                onSearchClick()
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

        // Profile nav item - Settings, stats, achievements
        val profileItem = standardNavItems[3]
        val isProfileSelected = currentDestination?.hasRoute<Profile>() == true
        GlassNavItemCompact(
            icon = if (isProfileSelected) profileItem.selectedIcon else profileItem.icon,
            label = profileItem.label,
            isSelected = isProfileSelected,
            onClick = {
                onNavItemSelected(profileItem)
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
    isOLED: Boolean = false,
    accentColor: Color = GlassColors.Interactive
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val theme = glassTheme(isDark, isOLED)
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
    isOLED: Boolean = false,
    accentColor: Color = GlassColors.Interactive
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val theme = glassTheme(isDark, isOLED)
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
    isOLED: Boolean = false,
    accentColor: Color = GlassColors.Interactive,
    isDark: Boolean,
    onChapterSelected: (Chapter) -> Unit,
    onBookmarkSelected: (Long) -> Unit = {},
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
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
                                    isOLED = isOLED,
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
                                    isOLED = isOLED,
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
                if (isSelected) GlassColors.SelectionGlass
                else if (isDark) Color.White.copy(alpha = 0.05f)
                else Color.Black.copy(alpha = 0.05f)
            )
            .then(
                if (isSelected) Modifier.border(
                    width = 1.dp,
                    color = GlassColors.SelectionBorder,
                    shape = RoundedCornerShape(GlassShapes.Small)
                ) else Modifier
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
    isOLED: Boolean,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val highlightColor = if (isOLED) GlassColors.WarmSlate else Color.White.copy(alpha = 0.1f)

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
    isOLED: Boolean,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val highlightColor = if (isOLED) GlassColors.WarmSlate else Color.White.copy(alpha = 0.1f)

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
