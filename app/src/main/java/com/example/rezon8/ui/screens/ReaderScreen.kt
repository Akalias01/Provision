package com.mossglen.reverie.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Html
import android.util.Log
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.mossglen.reverie.data.Book
import com.mossglen.reverie.haptics.HapticType
import com.mossglen.reverie.haptics.performHaptic
import com.mossglen.reverie.reader.EpubReader
import com.mossglen.reverie.ui.theme.GlassBlur
import com.mossglen.reverie.ui.theme.GlassColors
import com.mossglen.reverie.ui.theme.GlassShapes
import com.mossglen.reverie.ui.theme.GlassSpacing
import com.mossglen.reverie.ui.theme.GlassTypography
import com.mossglen.reverie.ui.theme.glassCard
import com.mossglen.reverie.ui.theme.glassEffect
import com.mossglen.reverie.ui.theme.glassTheme
import com.mossglen.reverie.ui.theme.ReaderFont
import com.mossglen.reverie.ui.theme.ReaderFontSizes
import com.mossglen.reverie.ui.theme.ReaderSettings
import com.mossglen.reverie.ui.theme.ReaderTextAlign
import com.mossglen.reverie.ui.theme.ReaderThemeData
import com.mossglen.reverie.ui.theme.ReaderThemes
import com.mossglen.reverie.ui.theme.ReaderThemeType
import com.mossglen.reverie.ui.viewmodel.LibraryViewModel
import com.mossglen.reverie.ui.viewmodel.TtsViewModel
import com.mossglen.reverie.tts.TtsEngineType
import com.mossglen.reverie.tts.VoiceModelManager
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.pager.HorizontalPager as TabPager
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import coil.compose.AsyncImage
import com.mossglen.reverie.R

// ============================================================================
// READER DISPLAY MODE - Scroll or Page flip
// ============================================================================

enum class ReaderDisplayMode(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SCROLL("Scroll", Icons.Default.SwipeVertical),
    PAGE("Page", Icons.AutoMirrored.Filled.MenuBook)
}

// ============================================================================
// PREMIUM E-READER COLORS
// ============================================================================

object ReaderGlassColors {
    // Moss Green - Reading-specific accent (nature, calm, focus)
    val MossGreen = Color(0xFF6B7F5E)                // Primary moss green
    val MossGreenLight = Color(0xFF8FA07E)           // Lighter moss
    val MossGreenVibrant = Color(0xFF7A9E6A)         // Vibrant moss

    // Whisky - Interactive accent (warmth, premium)
    // Lighter golden whisky - closer to Audible but richer
    val Whisky = Color(0xFFE5941F)                   // Primary golden whisky
    val WhiskyLight = Color(0xFFF0A835)              // Lighter whisky
    val WhiskyVibrant = Color(0xFFEA9E28)            // Vibrant whisky
    val WhiskyBorder = Color(0xFFE5941F).copy(alpha = 0.35f)  // Whisky borders/accents

    // Premium glass backgrounds for reading
    val GlassWhite = Color.White.copy(alpha = 0.08f)
    val GlassDark = Color.Black.copy(alpha = 0.5f)
    val GlassBorder = Color.White.copy(alpha = 0.15f)
    val WhiskyGlassBorder = Whisky.copy(alpha = 0.25f)  // Warm glass border

    // Highlight colors for TTS
    val SentenceHighlight = Color(0xFFFFEB3B).copy(alpha = 0.35f)
    val SentenceHighlightDark = MossGreen.copy(alpha = 0.25f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    accentColor: Color,
    isDark: Boolean = true,
    isReverieDark: Boolean = false,
    onBack: () -> Unit,
    onNavigateToBookDetail: (String) -> Unit = {},
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val books by libraryViewModel.books.collectAsState()
    val book = books.find { it.id == bookId }
    val theme = glassTheme(isDark, isReverieDark)

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Swipe to go back state
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 150f
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipeOffset"
    )
    val swipeProgress = (swipeOffset / swipeThreshold).coerceIn(0f, 1f)

    // Handle back button
    BackHandler { onBack() }

    // Determine content type
    val isPdf = book?.format == "PDF"
    val isEpub = book?.format == "EPUB"
    val isText = book?.format == "TEXT"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(animatedSwipeOffset.roundToInt(), 0) }
            .alpha(1f - swipeProgress * 0.3f)
    ) {
        when {
            book == null -> {
                ErrorContent(
                    message = "Book not found",
                    accentColor = accentColor,
                    onBack = onBack
                )
            }
            isPdf -> {
                PdfReaderContent(
                    book = book,
                    accentColor = accentColor,
                    onBack = onBack,
                    onSwipe = { delta -> swipeOffset = (swipeOffset + delta).coerceAtLeast(0f) },
                    onSwipeEnd = {
                        if (swipeOffset > swipeThreshold) onBack()
                        else swipeOffset = 0f
                    },
                    onSwipeCancel = { swipeOffset = 0f }
                )
            }
            isEpub -> {
                EpubReaderContent(
                    book = book,
                    accentColor = accentColor,
                    isDark = isDark,
                    isReverieDark = isReverieDark,
                    onBack = onBack,
                    onNavigateToBookDetail = onNavigateToBookDetail,
                    onSwipe = { delta -> swipeOffset = (swipeOffset + delta).coerceAtLeast(0f) },
                    onSwipeEnd = {
                        if (swipeOffset > swipeThreshold) onBack()
                        else swipeOffset = 0f
                    },
                    onSwipeCancel = { swipeOffset = 0f }
                )
            }
            isText -> {
                TextReaderContent(
                    book = book,
                    accentColor = accentColor,
                    onBack = onBack,
                    onSwipe = { delta -> swipeOffset = (swipeOffset + delta).coerceAtLeast(0f) },
                    onSwipeEnd = {
                        if (swipeOffset > swipeThreshold) onBack()
                        else swipeOffset = 0f
                    },
                    onSwipeCancel = { swipeOffset = 0f }
                )
            }
            else -> {
                ErrorContent(
                    message = "Unsupported format: ${book.format}",
                    accentColor = accentColor,
                    onBack = onBack
                )
            }
        }
    }
}

// ============================================================================
// PREMIUM GLASS E-READER
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpubReaderContent(
    book: com.mossglen.reverie.data.Book,
    accentColor: Color,
    isDark: Boolean = true,
    isReverieDark: Boolean = false,
    onBack: () -> Unit,
    onNavigateToBookDetail: (String) -> Unit = {},
    onSwipe: (Float) -> Unit,
    onSwipeEnd: () -> Unit,
    onSwipeCancel: () -> Unit,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    ttsViewModel: TtsViewModel = hiltViewModel()
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // READER THEME - Defaults to Paper (book-like) instead of dark audiobook theme
    var readerSettings by remember { mutableStateOf(ReaderSettings()) }
    val readerTheme = readerSettings.theme

    // ===== ANIMATED THEME COLORS - Apple-quality smooth transitions =====
    // These animate smoothly when switching between light/dark/sepia themes
    val animatedBackgroundColor by animateColorAsState(
        targetValue = readerTheme.backgroundColor,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "backgroundColorAnim"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = readerTheme.textColor,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "textColorAnim"
    )
    val animatedControlsBackground by animateColorAsState(
        targetValue = readerTheme.controlsBackground,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "controlsBgAnim"
    )
    val animatedControlsText by animateColorAsState(
        targetValue = readerTheme.controlsText,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "controlsTextAnim"
    )

    // TTS State
    val isTtsReady by ttsViewModel.isReady.collectAsState()
    val isTtsSpeaking by ttsViewModel.isSpeaking.collectAsState()
    val currentEngineType by ttsViewModel.currentEngineType.collectAsState()
    val kokoroModelReady by ttsViewModel.kokoroModelReady.collectAsState()
    val voiceSetupState by ttsViewModel.voiceSetupState.collectAsState()
    var showTtsControls by remember { mutableStateOf(false) }  // Voice/engine selection
    var showSpeedSheet by remember { mutableStateOf(false) }    // Speed control (matching audiobook player)
    var showVoiceSheet by remember { mutableStateOf(false) }   // Kokoro voice selection
    var selectedVoiceId by remember { mutableStateOf(0) }       // Current Kokoro speaker ID
    var showKokoroDownloadConfirm by remember { mutableStateOf(false) }
    var showKokoroDownloadProgress by remember { mutableStateOf(false) }
    var pendingKokoroSwitch by remember { mutableStateOf(false) }

    // Auto-show/hide progress dialog based on voiceSetupState
    LaunchedEffect(voiceSetupState) {
        Log.d("ReaderTTS", "voiceSetupState changed: $voiceSetupState")
        when (voiceSetupState) {
            is VoiceModelManager.SetupState.Downloading,
            is VoiceModelManager.SetupState.Installing,
            VoiceModelManager.SetupState.FetchingConfig -> {
                showKokoroDownloadProgress = true
            }
            VoiceModelManager.SetupState.Completed -> {
                // Brief delay to show completion before closing
                delay(500)
                showKokoroDownloadProgress = false
            }
            is VoiceModelManager.SetupState.Error -> {
                // Keep dialog open to show error - user can dismiss manually
                showKokoroDownloadProgress = true
            }
            VoiceModelManager.SetupState.Cancelled -> {
                showKokoroDownloadProgress = false
            }
            VoiceModelManager.SetupState.Idle -> {
                // Don't change
            }
        }
    }
    var ttsSpeed by remember { mutableFloatStateOf(1.0f) }

    // Display mode: SCROLL or PAGE with animation
    var displayMode by remember { mutableStateOf(ReaderDisplayMode.SCROLL) }
    var showDisplayModeMenu by remember { mutableStateOf(false) }

    // Sentence tracking for TTS with highlighting
    var sentences by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentSentenceIndex by remember { mutableIntStateOf(-1) }
    var isTtsPlaying by remember { mutableStateOf(false) }

    // Kokoro model size (approximately 300MB)
    val kokoroModelSizeMb = 300

    // Load saved chapter from book progress (stored as chapter index * 1000000)
    val savedChapter = (book.progress / 1000000).toInt().coerceAtLeast(0)
    var currentChapter by remember { mutableIntStateOf(savedChapter) }
    var totalChapters by remember { mutableIntStateOf(0) }
    var chapterContent by remember { mutableStateOf("") }
    var chapterTitle by remember { mutableStateOf("") }
    var chapterTitles by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showChapterList by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }  // Pill visible by default, tap center for full controls
    var showThemePicker by remember { mutableStateOf(false) }
    var showFontPicker by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }  // Settings sheet from pill (keeps pill visible)
    var showTopBarMenu by remember { mutableStateOf(false) }  // Dropdown menu from 3-dot button

    // ========== NEW: Pill Navigation & Bookmarks State ==========
    var showPillNavigation by remember { mutableStateOf(true) }
    var showChaptersBookmarksDialog by remember { mutableStateOf(false) }
    var showBookmarkNoteDialog by remember { mutableStateOf(false) }
    var bookmarkRippleTrigger by remember { mutableIntStateOf(0) }

    // Reading bookmarks - stored as: chapterIndex * 1000000 + characterPosition
    // For now, we use chapter-based bookmarks since character position tracking is complex
    val readingBookmarks = book.bookmarks
    val readingBookmarkNotes = book.bookmarkNotes

    // Calculate current bookmark position value
    val currentBookmarkPosition: Long = currentChapter.toLong() * 1000000L

    val scrollState = rememberLazyListState()

    // ===== SMART PILL AUTO-HIDE =====
    // Hide when scrolling up (reading), show when:
    // - Scrolling down (looking for controls)
    // - At bottom of content (need navigation)
    // - Idle for 2 seconds (user paused reading)
    var lastScrollOffset by remember { mutableIntStateOf(0) }
    var lastScrollTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isScrollingUp by remember { mutableStateOf(false) }

    // Check if at bottom of scroll content
    val isAtBottom = remember {
        derivedStateOf {
            val lastVisibleItem = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = scrollState.layoutInfo.totalItemsCount
            lastVisibleItem != null && lastVisibleItem.index >= totalItems - 1
        }
    }

    // Scroll direction detection and pill auto-hide
    LaunchedEffect(scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset) {
        val currentOffset = scrollState.firstVisibleItemIndex * 1000 + scrollState.firstVisibleItemScrollOffset
        val scrollDelta = currentOffset - lastScrollOffset
        val currentTime = System.currentTimeMillis()

        // Detect scroll direction (with threshold to avoid jitter)
        if (scrollDelta.absoluteValue > 10) {
            isScrollingUp = scrollDelta > 0  // Scrolling up = reading forward
            lastScrollTime = currentTime

            if (isScrollingUp && !showControls) {
                // Hide pill when reading (scrolling up through content)
                showPillNavigation = false
            } else if (!isScrollingUp) {
                // Show pill when scrolling down (looking for controls)
                showPillNavigation = true
            }
        }

        lastScrollOffset = currentOffset
    }

    // Show pill when at bottom of content
    LaunchedEffect(isAtBottom.value) {
        if (isAtBottom.value && !showControls) {
            showPillNavigation = true
        }
    }

    // Show pill after 2 seconds of idle (stopped scrolling)
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            val timeSinceScroll = System.currentTimeMillis() - lastScrollTime
            if (timeSinceScroll > 2000 && !showPillNavigation && !showControls) {
                showPillNavigation = true
            }
        }
    }

    var epubReader by remember { mutableStateOf<EpubReader?>(null) }
    val context = LocalContext.current

    // Font size from settings
    val fontSize = readerSettings.fontSize

    // Parse sentences when chapter content changes
    LaunchedEffect(chapterContent) {
        if (chapterContent.isNotEmpty()) {
            val doc = Jsoup.parse(chapterContent)
            val text = doc.body().text()
            // Split into sentences (handles ., !, ?, and paragraph breaks)
            // Normalize whitespace (collapse multiple spaces to single) for consistent matching
            sentences = text.split(Regex("(?<=[.!?])\\s+|(?<=\\n)\\s*"))
                .map { it.replace(Regex("\\s+"), " ").trim() }
                .filter { it.isNotEmpty() && it.length > 2 }  // Skip very short fragments
            currentSentenceIndex = -1
            Log.d("ReaderTTS", "Parsed ${sentences.size} sentences from chapter")
        }
    }

    // TTS sentence progression - use snapshotFlow for more reliable detection
    // Track when isTtsSpeaking transitions from true to false
    var wasLastSpeaking by remember { mutableStateOf(false) }

    // Set up pre-buffering for Kokoro TTS - provides next sentence while current plays
    LaunchedEffect(sentences, currentSentenceIndex) {
        ttsViewModel.setNextSentenceProvider {
            val nextIndex = currentSentenceIndex + 1
            if (nextIndex >= 0 && nextIndex < sentences.size) {
                sentences[nextIndex]
            } else {
                null
            }
        }
    }

    // Track generation state to refresh cache provider when generation completes
    val audioGenerationState by ttsViewModel.audioGenerationState.collectAsState()

    // Set up cached audio provider for instant playback from pre-generated audio
    // Re-run when generation completes or sentences change
    LaunchedEffect(book.id, sentences, audioGenerationState) {
        val hasCache = ttsViewModel.hasPreGeneratedAudio(book.id)
        Log.d("ReaderTTS", "Cache check: hasCache=$hasCache, genState=$audioGenerationState")

        if (hasCache) {
            // Calculate front matter offset - sentences skipped during generation
            val frontMatterOffset = com.mossglen.reverie.tts.FrontMatterDetector.findContentStartIndex(sentences)
            Log.d("ReaderTTS", "Setting up cache provider with offset=$frontMatterOffset")

            ttsViewModel.setCachedAudioProvider { text ->
                // Find the sentence index in the full list
                val fullIndex = sentences.indexOf(text)
                if (fullIndex >= frontMatterOffset) {
                    // Adjust for front matter offset
                    val cacheIndex = fullIndex - frontMatterOffset
                    val audio = ttsViewModel.getCachedAudio(book.id, cacheIndex)
                    if (audio != null) {
                        Log.d("ReaderTTS", "Cache HIT: fullIndex=$fullIndex, cacheIndex=$cacheIndex, samples=${audio.size}")
                    } else {
                        Log.d("ReaderTTS", "Cache MISS: fullIndex=$fullIndex, cacheIndex=$cacheIndex")
                    }
                    audio
                } else {
                    Log.d("ReaderTTS", "Front matter: fullIndex=$fullIndex < offset=$frontMatterOffset")
                    null
                }
            }
        } else {
            ttsViewModel.setCachedAudioProvider(null)
        }
    }

    // Clean up providers when leaving
    DisposableEffect(Unit) {
        onDispose {
            ttsViewModel.setNextSentenceProvider(null)
            ttsViewModel.setCachedAudioProvider(null)
        }
    }

    LaunchedEffect(isTtsPlaying) {
        if (!isTtsPlaying) {
            wasLastSpeaking = false
            return@LaunchedEffect
        }

        // Continuously monitor speaking state while TTS is playing
        snapshotFlow { isTtsSpeaking }
            .distinctUntilChanged()
            .collect { speaking ->
                Log.d("ReaderTTS", "Speaking state changed: $speaking, wasLast: $wasLastSpeaking, index: $currentSentenceIndex")

                if (wasLastSpeaking && !speaking && currentSentenceIndex >= 0) {
                    // TTS just finished speaking - advance to next sentence
                    delay(100) // Small delay to ensure TTS is fully done

                    if (currentSentenceIndex < sentences.size - 1) {
                        currentSentenceIndex++
                        Log.d("ReaderTTS", "Advancing to sentence $currentSentenceIndex of ${sentences.size}")
                        ttsViewModel.speak(sentences[currentSentenceIndex])
                    } else {
                        // Finished all sentences
                        Log.d("ReaderTTS", "Finished all sentences")
                        isTtsPlaying = false
                        currentSentenceIndex = -1
                    }
                }
                wasLastSpeaking = speaking
            }
    }

    // Save progress when chapter changes
    LaunchedEffect(currentChapter) {
        // Store chapter as progress (chapter index * 1000000 to avoid conflicts with audio progress)
        val progressValue = currentChapter.toLong() * 1000000L
        libraryViewModel.updateBookProgress(book.id, progressValue)
    }

    // Initialize EPUB reader
    LaunchedEffect(book.filePath) {
        withContext(Dispatchers.IO) {
            try {
                // Convert URI string to File - handles both file:// and content:// URIs
                val uri = Uri.parse(book.filePath)
                val file: File = when (uri.scheme) {
                    "file" -> {
                        // file:// URI - extract the path directly
                        File(uri.path!!)
                    }
                    "content" -> {
                        // content:// URI - copy to temp file for ZipFile access
                        val tempFile = File(context.cacheDir, "epub_${book.id}.epub")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile
                    }
                    else -> {
                        // Try as direct file path (legacy support)
                        File(book.filePath)
                    }
                }

                if (!file.exists()) {
                    errorMessage = "File not found: ${book.filePath}"
                    isLoading = false
                    return@withContext
                }

                val reader = EpubReader(file)
                if (!reader.parse()) {
                    errorMessage = "Failed to parse EPUB file"
                    isLoading = false
                    return@withContext
                }

                epubReader = reader
                totalChapters = reader.getChapterCount()
                chapterTitles = reader.getChapterTitles()

                // Load saved chapter (or first chapter if saved chapter is invalid)
                if (totalChapters > 0) {
                    val chapterToLoad = savedChapter.coerceIn(0, totalChapters - 1)
                    val chapter = reader.getChapter(chapterToLoad)
                    chapterContent = chapter?.content ?: ""
                    chapterTitle = chapter?.title ?: "Chapter ${chapterToLoad + 1}"
                    currentChapter = chapterToLoad
                }

                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to open EPUB: ${e.message}"
                isLoading = false
            }
        }
    }

    // Load chapter when changed
    LaunchedEffect(currentChapter) {
        epubReader?.let { reader ->
            withContext(Dispatchers.IO) {
                val chapter = reader.getChapter(currentChapter)
                chapterContent = chapter?.content ?: ""
                chapterTitle = chapter?.title ?: "Chapter ${currentChapter + 1}"
            }
            // Scroll to top of new chapter
            scrollState.scrollToItem(0)
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            epubReader?.close()
        }
    }

    // ========== PREMIUM GLASS CHAPTER LIST DIALOG ==========
    if (showChapterList) {
        Dialog(
            onDismissRequest = { showChapterList = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showChapterList = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 320.dp, max = 400.dp)
                        .fillMaxWidth(0.9f)
                        .heightIn(max = 500.dp)
                        .glassCard(isDark = readerTheme.isDark)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Consume clicks */ }
                        .padding(GlassSpacing.L)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = ReaderGlassColors.MossGreenVibrant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Chapters",
                                style = GlassTypography.Title,
                                color = readerTheme.controlsText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = {
                                view.performHaptic(HapticType.LightTap)
                                showChapterList = false
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = readerTheme.textSecondaryColor
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Chapter list with glass styling
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chapterTitles.size) { index ->
                            val isCurrentChapter = index == currentChapter
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.97f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                                label = "scale"
                            )

                            Surface(
                                onClick = {
                                    view.performHaptic(HapticType.MediumTap)
                                    currentChapter = index
                                    showChapterList = false
                                },
                                color = if (isCurrentChapter)
                                    ReaderGlassColors.MossGreenVibrant.copy(alpha = 0.15f)
                                else
                                    Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isCurrentChapter)
                                    BorderStroke(1.dp, ReaderGlassColors.MossGreenVibrant.copy(alpha = 0.5f))
                                else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale),
                                interactionSource = interactionSource
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Chapter number badge
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCurrentChapter)
                                                    ReaderGlassColors.MossGreenVibrant
                                                else
                                                    Color.White.copy(alpha = 0.1f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = if (isCurrentChapter) Color.White else readerTheme.textSecondaryColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = chapterTitles[index],
                                        color = if (isCurrentChapter) ReaderGlassColors.MossGreenVibrant else readerTheme.controlsText,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrentChapter) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isCurrentChapter) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = ReaderGlassColors.MossGreenVibrant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ========== KOKORO DOWNLOAD CONFIRMATION DIALOG ==========
    if (showKokoroDownloadConfirm) {
        Dialog(
            onDismissRequest = { showKokoroDownloadConfirm = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showKokoroDownloadConfirm = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 300.dp, max = 380.dp)
                        .fillMaxWidth(0.85f)
                        .glassCard(isDark = true)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Consume clicks */ }
                        .padding(GlassSpacing.L),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)
                ) {
                    // AI Icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        ReaderGlassColors.MossGreenVibrant,
                                        ReaderGlassColors.MossGreen
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Title
                    Text(
                        text = "Download Kokoro AI Voice?",
                        style = GlassTypography.Title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Description
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Kokoro is a premium AI voice that sounds natural and expressive.",
                            style = GlassTypography.Body,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        // Storage size card
                        Surface(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ReaderGlassColors.GlassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = ReaderGlassColors.MossGreenLight,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${kokoroModelSizeMb} MB",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Storage required",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Download button
                        Button(
                            onClick = {
                                view.performHaptic(HapticType.Confirm)
                                showKokoroDownloadConfirm = false
                                ttsViewModel.downloadKokoroModel()
                                if (pendingKokoroSwitch) {
                                    pendingKokoroSwitch = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ReaderGlassColors.MossGreenVibrant
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Download Kokoro",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }

                        // Cancel button
                        TextButton(
                            onClick = {
                                view.performHaptic(HapticType.LightTap)
                                showKokoroDownloadConfirm = false
                                pendingKokoroSwitch = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Use System Voice",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }

    // ========== KOKORO DOWNLOAD PROGRESS DIALOG ==========
    if (showKokoroDownloadProgress) {
        Dialog(
            onDismissRequest = { /* Can't dismiss during download */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1C1C1E),
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val state = voiceSetupState) {
                        VoiceModelManager.SetupState.FetchingConfig -> {
                            CircularProgressIndicator(
                                color = ReaderGlassColors.MossGreenVibrant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Connecting...",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        is VoiceModelManager.SetupState.Downloading -> {
                            CircularProgressIndicator(
                                progress = { state.progress },
                                color = ReaderGlassColors.MossGreenVibrant,
                                modifier = Modifier.size(64.dp),
                                strokeWidth = 6.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Downloading Kokoro",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${(state.progress * 100).toInt()}%",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                            val mbDownloaded = state.bytesDownloaded / (1024 * 1024)
                            val mbTotal = state.totalBytes / (1024 * 1024)
                            Text(
                                "${mbDownloaded}MB / ${mbTotal}MB",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                        is VoiceModelManager.SetupState.Installing -> {
                            CircularProgressIndicator(
                                progress = { state.progress },
                                color = ReaderGlassColors.MossGreenVibrant,
                                modifier = Modifier.size(64.dp),
                                strokeWidth = 6.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Installing...",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        is VoiceModelManager.SetupState.Error -> {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Download Failed",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                state.message,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        VoiceModelManager.SetupState.Completed -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ReaderGlassColors.MossGreenVibrant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Kokoro Ready!",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        else -> {
                            CircularProgressIndicator(
                                color = ReaderGlassColors.MossGreenVibrant,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Different button based on state
                    val isError = voiceSetupState is VoiceModelManager.SetupState.Error
                    val isCompleted = voiceSetupState == VoiceModelManager.SetupState.Completed

                    TextButton(
                        onClick = {
                            if (isError || isCompleted) {
                                ttsViewModel.resetDownloadState()
                            } else {
                                ttsViewModel.cancelDownload()
                            }
                            showKokoroDownloadProgress = false
                        }
                    ) {
                        Text(
                            if (isError) "Dismiss" else if (isCompleted) "Done" else "Cancel",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // ========== PREMIUM READING SPEED SHEET (matches audiobook player style) ==========
    if (showSpeedSheet) {
        ReaderSpeedSheet(
            currentSpeed = ttsSpeed,
            readerTheme = readerTheme,
            onSpeedChanged = { newSpeed ->
                ttsSpeed = newSpeed
                ttsViewModel.setSpeed(newSpeed)
            },
            onDismiss = { showSpeedSheet = false }
        )
    }

    // ========== QUICK SETTINGS SHEET (from Pill) ==========
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = readerTheme.controlsBackground,
            contentColor = readerTheme.controlsText,
            dragHandle = {
                // Whisky accent handle
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ReaderGlassColors.Whisky.copy(alpha = 0.5f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with whisky accent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = ReaderGlassColors.MossGreenVibrant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Reading Settings",
                            color = readerTheme.controlsText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Whisky divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ReaderGlassColors.Whisky.copy(alpha = 0.4f),
                                    ReaderGlassColors.Whisky.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Quick Theme Selection - COMPACT
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Theme",
                        color = readerTheme.textSecondaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReaderThemes.allThemes.forEach { (type, theme) ->
                            val isSelected = readerSettings.themeType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)  // Shorter height
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(theme.backgroundColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) ReaderGlassColors.MossGreenVibrant else theme.dividerColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        view.performHaptic(HapticType.MediumTap)
                                        readerSettings = readerSettings.copy(themeType = type)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    theme.name.take(1),
                                    color = theme.textColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Font Size - Premium slider matching playback speed design
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Font Size",
                            color = readerTheme.textSecondaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${readerSettings.fontSize.toInt()}sp",
                            color = readerTheme.accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Premium slider with +/- buttons (matching audiobook speed control)
                    val chipBg = if (readerTheme.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Minus button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(chipBg)
                                .clickable {
                                    val newSize = (readerSettings.fontSize - 2f).coerceIn(ReaderFontSizes.MIN, ReaderFontSizes.MAX)
                                    readerSettings = readerSettings.copy(fontSize = newSize)
                                    view.performHaptic(HapticType.LightTap)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A", fontSize = 12.sp, color = readerTheme.controlsText)
                        }

                        // Custom thin slider with circle knob
                        val fontProgress = ((readerSettings.fontSize - ReaderFontSizes.MIN) / (ReaderFontSizes.MAX - ReaderFontSizes.MIN)).coerceIn(0f, 1f)
                        val handleSize = 16.dp
                        val trackHeight = 4.dp

                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val progress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        val newSize = ReaderFontSizes.MIN + (progress * (ReaderFontSizes.MAX - ReaderFontSizes.MIN))
                                        readerSettings = readerSettings.copy(fontSize = newSize.coerceIn(ReaderFontSizes.MIN, ReaderFontSizes.MAX))
                                        view.performHaptic(HapticType.LightTap)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        val progress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        val newSize = ReaderFontSizes.MIN + (progress * (ReaderFontSizes.MAX - ReaderFontSizes.MIN))
                                        readerSettings = readerSettings.copy(fontSize = newSize.coerceIn(ReaderFontSizes.MIN, ReaderFontSizes.MAX))
                                    }
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val handleOffset = (maxWidth - handleSize) * fontProgress

                            // Track background
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(trackHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(readerTheme.dividerColor)
                                    .align(Alignment.Center)
                            )

                            // Active track
                            Box(
                                modifier = Modifier
                                    .width(handleOffset + handleSize / 2)
                                    .height(trackHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(ReaderGlassColors.MossGreenVibrant)
                                    .align(Alignment.CenterStart)
                            )

                            // Circle handle
                            Box(
                                modifier = Modifier
                                    .offset(x = handleOffset)
                                    .size(handleSize)
                                    .clip(CircleShape)
                                    .background(ReaderGlassColors.MossGreenVibrant)
                            )
                        }

                        // Plus button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(chipBg)
                                .clickable {
                                    val newSize = (readerSettings.fontSize + 2f).coerceIn(ReaderFontSizes.MIN, ReaderFontSizes.MAX)
                                    readerSettings = readerSettings.copy(fontSize = newSize)
                                    view.performHaptic(HapticType.LightTap)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = readerTheme.controlsText)
                        }
                    }
                }

                // Voice Settings - Opens voice engine selection
                Surface(
                    onClick = {
                        view.performHaptic(HapticType.MediumTap)
                        showSettingsSheet = false
                        showTtsControls = true
                    },
                    color = if (readerTheme.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = ReaderGlassColors.Whisky,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Voice Settings",
                                    color = readerTheme.controlsText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Engine, speed, voice selection",
                                    color = readerTheme.textSecondaryColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = readerTheme.textSecondaryColor
                        )
                    }
                }
            }
        }
    }

    // ========== PREMIUM GLASS THEME PICKER ==========
    if (showThemePicker) {
        Dialog(
            onDismissRequest = { showThemePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showThemePicker = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 300.dp, max = 380.dp)
                        .fillMaxWidth(0.85f)
                        .glassCard(isDark = readerTheme.isDark)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Consume clicks */ }
                        .padding(GlassSpacing.L),
                    verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = ReaderGlassColors.MossGreenVibrant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Reading Theme",
                                style = GlassTypography.Title,
                                color = readerTheme.controlsText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = {
                                view.performHaptic(HapticType.LightTap)
                                showThemePicker = false
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = readerTheme.textSecondaryColor
                            )
                        }
                    }

                    // Theme options
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReaderThemes.allThemes.forEach { (type, theme) ->
                            val isSelected = readerSettings.themeType == type
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.97f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                                label = "scale"
                            )

                            Surface(
                                onClick = {
                                    view.performHaptic(HapticType.MediumTap)
                                    readerSettings = readerSettings.copy(themeType = type)
                                    showThemePicker = false
                                },
                                color = theme.backgroundColor,
                                shape = RoundedCornerShape(16.dp),
                                border = if (isSelected)
                                    BorderStroke(2.dp, theme.glassBorder.copy(alpha = 0.8f))
                                else
                                    BorderStroke(1.dp, theme.glassBorder),
                                shadowElevation = if (isSelected) 8.dp else 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .then(
                                        if (isSelected) Modifier.shadow(
                                            elevation = 4.dp,
                                            shape = RoundedCornerShape(16.dp),
                                            ambientColor = theme.cardGlow,
                                            spotColor = theme.cardGlow
                                        ) else Modifier
                                    ),
                                interactionSource = interactionSource
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Theme preview circle
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(theme.backgroundColor)
                                            .border(
                                                1.dp,
                                                theme.dividerColor,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Aa",
                                            color = theme.textColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = theme.name,
                                            color = theme.textColor,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = when (type) {
                                                ReaderThemeType.PAPER -> "Warm cream, easy on eyes"
                                                ReaderThemeType.SEPIA -> "Classic e-reader feel"
                                                ReaderThemeType.DARK_GRAY -> "Comfortable for low light"
                                                ReaderThemeType.NIGHT -> "OLED black, saves battery"
                                                else -> ""
                                            },
                                            color = theme.textSecondaryColor,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(ReaderGlassColors.MossGreenVibrant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ========== PREMIUM GLASS FONT & DISPLAY SETTINGS ==========
    if (showFontPicker) {
        Dialog(
            onDismissRequest = { showFontPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showFontPicker = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 320.dp, max = 420.dp)
                        .fillMaxWidth(0.9f)
                        .glassCard(isDark = readerTheme.isDark)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Consume clicks */ }
                        .padding(GlassSpacing.L)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TextFields,
                                contentDescription = null,
                                tint = ReaderGlassColors.MossGreenVibrant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Reading Settings",
                                style = GlassTypography.Title,
                                color = readerTheme.controlsText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = {
                                view.performHaptic(HapticType.LightTap)
                                showFontPicker = false
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = readerTheme.textSecondaryColor
                            )
                        }
                    }

                    // ===== DISPLAY MODE SECTION =====
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Display Mode",
                            color = readerTheme.textSecondaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ReaderDisplayMode.entries.forEach { mode ->
                                val isSelected = displayMode == mode
                                Surface(
                                    onClick = {
                                        view.performHaptic(HapticType.MediumTap)
                                        displayMode = mode
                                    },
                                    color = if (isSelected)
                                        ReaderGlassColors.MossGreenVibrant.copy(alpha = 0.15f)
                                    else
                                        Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (isSelected)
                                        BorderStroke(2.dp, ReaderGlassColors.MossGreenVibrant)
                                    else
                                        BorderStroke(1.dp, readerTheme.dividerColor),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            mode.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) ReaderGlassColors.MossGreenVibrant else readerTheme.controlsText,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            mode.displayName,
                                            color = if (isSelected) ReaderGlassColors.MossGreenVibrant else readerTheme.controlsText,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = readerTheme.dividerColor, thickness = 0.5.dp)

                    // ===== FONT FAMILY SECTION =====
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Font Style",
                            color = readerTheme.textSecondaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReaderFont.entries.forEach { font ->
                                val isSelected = readerSettings.fontFamily == font
                                Surface(
                                    onClick = {
                                        view.performHaptic(HapticType.LightTap)
                                        readerSettings = readerSettings.copy(fontFamily = font)
                                    },
                                    color = if (isSelected)
                                        ReaderGlassColors.MossGreenVibrant.copy(alpha = 0.15f)
                                    else
                                        Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) ReaderGlassColors.MossGreenVibrant else readerTheme.dividerColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = font.displayName.take(5),
                                        color = if (isSelected) ReaderGlassColors.MossGreenVibrant else readerTheme.controlsText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                        fontSize = 12.sp,
                                        fontFamily = font.fontFamily,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // ===== FONT SIZE SECTION =====
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Font Size",
                                color = readerTheme.textSecondaryColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Surface(
                                color = ReaderGlassColors.MossGreenVibrant.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "${readerSettings.fontSize.toInt()}sp",
                                    color = ReaderGlassColors.MossGreenVibrant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    view.performHaptic(HapticType.LightTap)
                                    val newSize = (readerSettings.fontSize - ReaderFontSizes.STEP).coerceAtLeast(ReaderFontSizes.MIN)
                                    readerSettings = readerSettings.copy(fontSize = newSize)
                                }
                            ) {
                                Icon(
                                    Icons.Default.TextDecrease,
                                    null,
                                    tint = ReaderGlassColors.MossGreenVibrant
                                )
                            }
                            Slider(
                                value = readerSettings.fontSize,
                                onValueChange = { readerSettings = readerSettings.copy(fontSize = it) },
                                valueRange = ReaderFontSizes.MIN..ReaderFontSizes.MAX,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = ReaderGlassColors.MossGreenVibrant,
                                    activeTrackColor = ReaderGlassColors.MossGreenVibrant,
                                    inactiveTrackColor = readerTheme.dividerColor
                                )
                            )
                            IconButton(
                                onClick = {
                                    view.performHaptic(HapticType.LightTap)
                                    val newSize = (readerSettings.fontSize + ReaderFontSizes.STEP).coerceAtMost(ReaderFontSizes.MAX)
                                    readerSettings = readerSettings.copy(fontSize = newSize)
                                }
                            ) {
                                Icon(
                                    Icons.Default.TextIncrease,
                                    null,
                                    tint = ReaderGlassColors.MossGreenVibrant
                                )
                            }
                        }
                    }

                    // ===== LINE HEIGHT SECTION =====
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Line Spacing",
                                color = readerTheme.textSecondaryColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Surface(
                                color = ReaderGlassColors.MossGreenVibrant.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    String.format("%.1fx", readerSettings.lineHeight),
                                    color = ReaderGlassColors.MossGreenVibrant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Slider(
                            value = readerSettings.lineHeight,
                            onValueChange = { readerSettings = readerSettings.copy(lineHeight = it) },
                            valueRange = 1.2f..2.0f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = ReaderGlassColors.MossGreenVibrant,
                                activeTrackColor = ReaderGlassColors.MossGreenVibrant,
                                inactiveTrackColor = readerTheme.dividerColor
                            )
                        )
                    }
                }
            }
        }
    }

    // ========== PREMIUM GLASS TTS CONTROLS (Slide-up Sheet) ==========
    // Refresh Kokoro model status when opening TTS controls
    LaunchedEffect(showTtsControls) {
        if (showTtsControls) {
            Log.w("ReaderTTS", "TTS controls opened - refreshing Kokoro status")
            ttsViewModel.refreshKokoroStatus()
        }
    }

    // Log Kokoro state changes for debugging
    LaunchedEffect(kokoroModelReady) {
        Log.w("ReaderTTS", "UI kokoroModelReady changed to: $kokoroModelReady")
    }

    if (showTtsControls) {
        // Theme-aware colors for the sheet
        val sheetBg = if (readerTheme.isDark) Color(0xFF1C1C1E) else readerTheme.backgroundColor

        ModalBottomSheet(
            onDismissRequest = { showTtsControls = false },
            containerColor = sheetBg,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(readerTheme.textSecondaryColor.copy(alpha = 0.5f))
                )
            },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = ReaderGlassColors.MossGreenVibrant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Voice Engine",
                            style = GlassTypography.Title,
                            color = readerTheme.textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ===== VOICE ENGINE SELECTION =====
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Voice Engine",
                        color = readerTheme.textSecondaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    // System TTS - Using Button for reliable click handling
                    val isSystemSelected = currentEngineType == TtsEngineType.SYSTEM
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            view.performHaptic(HapticType.MediumTap)
                            android.util.Log.e("ReaderTTS", "System TTS clicked")
                            ttsViewModel.switchEngine(false)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSystemSelected)
                                ReaderGlassColors.MossGreenVibrant.copy(alpha = 0.15f)
                            else
                                Color.Transparent
                        ),
                        border = if (isSystemSelected)
                            BorderStroke(2.dp, ReaderGlassColors.MossGreenVibrant)
                        else
                            BorderStroke(1.dp, readerTheme.dividerColor),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (isSystemSelected) ReaderGlassColors.MossGreenVibrant else readerTheme.controlsText,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "System",
                                color = if (isSystemSelected) ReaderGlassColors.MossGreenVibrant else readerTheme.controlsText,
                                fontSize = 14.sp,
                                fontWeight = if (isSystemSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                "Default voice",
                                color = readerTheme.textSecondaryColor,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Kokoro AI - Only available if book audio is pre-generated
                    val isKokoroSelected = currentEngineType == TtsEngineType.SHERPA
                    val hasBookAudio = remember(book.id, audioGenerationState) { ttsViewModel.hasPreGeneratedAudio(book.id) }
                    val kokoroAvailable = kokoroModelReady && hasBookAudio

                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            view.performHaptic(HapticType.MediumTap)

                            when {
                                !kokoroModelReady -> {
                                    // Need to download model first
                                    Toast.makeText(context, "Download Kokoro model first (see below)", Toast.LENGTH_SHORT).show()
                                }
                                !hasBookAudio -> {
                                    // Need to generate book audio first
                                    Toast.makeText(context, "Generate audio for this book first (see below)", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    // Ready to use Kokoro
                                    ttsViewModel.switchEngine(true)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = when {
                                isKokoroSelected -> ReaderGlassColors.MossGreenVibrant.copy(alpha = 0.15f)
                                !kokoroAvailable -> Color.Gray.copy(alpha = 0.1f)
                                else -> Color.Transparent
                            }
                        ),
                        border = when {
                            isKokoroSelected -> BorderStroke(2.dp, ReaderGlassColors.MossGreenVibrant)
                            !kokoroAvailable -> BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                            else -> BorderStroke(1.dp, readerTheme.dividerColor)
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = when {
                                    isKokoroSelected -> ReaderGlassColors.MossGreenVibrant
                                    !kokoroAvailable -> Color.Gray
                                    else -> readerTheme.controlsText
                                },
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Kokoro AI",
                                    color = when {
                                        isKokoroSelected -> ReaderGlassColors.MossGreenVibrant
                                        !kokoroAvailable -> Color.Gray
                                        else -> readerTheme.controlsText
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = if (isKokoroSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                            Text(
                                when {
                                    !kokoroModelReady -> "Download required"
                                    !hasBookAudio -> "Generate audio first"
                                    else -> "Premium voice"
                                },
                                color = if (kokoroAvailable) readerTheme.textSecondaryColor else Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                    }
                }

                // ===== VOICE SELECTION (for Kokoro) =====
                if (kokoroModelReady) {
                    var showVoicePicker by remember { mutableStateOf(false) }
                    val savedVoiceId by ttsViewModel.savedVoiceId.collectAsState()
                    var selectedVoiceId by remember { mutableStateOf(savedVoiceId) }

                    // Update selected voice when saved value loads
                    LaunchedEffect(savedVoiceId) {
                        selectedVoiceId = savedVoiceId
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Narrator Voice",
                            color = readerTheme.textSecondaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Current voice display with picker trigger
                        Surface(
                            onClick = { showVoicePicker = !showVoicePicker },
                            color = ReaderGlassColors.Whisky.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ReaderGlassColors.WhiskyBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentVoice = com.mossglen.reverie.tts.KokoroVoices.getVoice(selectedVoiceId)
                                    ?: com.mossglen.reverie.tts.KokoroVoices.DEFAULT_VOICE

                                Icon(
                                    if (currentVoice.gender == com.mossglen.reverie.tts.KokoroVoices.Gender.FEMALE)
                                        Icons.Default.Face else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = ReaderGlassColors.Whisky,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        currentVoice.name,
                                        color = readerTheme.textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${if (currentVoice.gender == com.mossglen.reverie.tts.KokoroVoices.Gender.FEMALE) "Female" else "Male"} • ${currentVoice.accent.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                        color = readerTheme.textSecondaryColor,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    if (showVoicePicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select voice",
                                    tint = readerTheme.textSecondaryColor
                                )
                            }
                        }

                        // Voice picker dropdown
                        AnimatedVisibility(visible = showVoicePicker) {
                            Surface(
                                color = readerTheme.controlsBackground,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, readerTheme.dividerColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 250.dp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    var previewingVoiceId by remember { mutableStateOf<Int?>(null) }

                                    com.mossglen.reverie.tts.KokoroVoices.PRIMARY_VOICES.forEach { voice ->
                                        val isSelected = voice.id == selectedVoiceId
                                        val isPreviewing = previewingVoiceId == voice.id

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) ReaderGlassColors.Whisky.copy(alpha = 0.15f)
                                                    else Color.Transparent
                                                )
                                                .clickable {
                                                    selectedVoiceId = voice.id
                                                    ttsViewModel.setVoiceId(voice.id)
                                                    showVoicePicker = false
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Preview button
                                            IconButton(
                                                onClick = {
                                                    if (isPreviewing) {
                                                        // Stop preview
                                                        ttsViewModel.stop()
                                                        previewingVoiceId = null
                                                    } else {
                                                        // Start preview
                                                        previewingVoiceId = voice.id
                                                        ttsViewModel.previewVoice(voice.id) {
                                                            previewingVoiceId = null
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                    contentDescription = if (isPreviewing) "Stop preview" else "Preview voice",
                                                    tint = if (isPreviewing) ReaderGlassColors.MossGreenVibrant else readerTheme.textSecondaryColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Icon(
                                                if (voice.gender == com.mossglen.reverie.tts.KokoroVoices.Gender.FEMALE)
                                                    Icons.Default.Face else Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (isSelected) ReaderGlassColors.Whisky else readerTheme.textSecondaryColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    voice.name,
                                                    color = if (isSelected) ReaderGlassColors.Whisky else readerTheme.textColor,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                                if (voice.description.isNotEmpty()) {
                                                    Text(
                                                        voice.description,
                                                        color = readerTheme.textSecondaryColor,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = ReaderGlassColors.Whisky,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ===== TTS STATUS (Voice/engine selection only - speed is in separate sheet) =====
                if (isTtsPlaying && currentSentenceIndex >= 0) {
                    Surface(
                        color = ReaderGlassColors.MossGreenVibrant.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ReaderGlassColors.MossGreenVibrant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = ReaderGlassColors.MossGreenVibrant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Now reading",
                                    color = readerTheme.textSecondaryColor,
                                    fontSize = 11.sp
                                )
                                Text(
                                    "Sentence ${currentSentenceIndex + 1} of ${sentences.size}",
                                    color = ReaderGlassColors.MossGreenVibrant,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            // Progress
                            CircularProgressIndicator(
                                progress = { (currentSentenceIndex + 1).toFloat() / sentences.size.coerceAtLeast(1) },
                                modifier = Modifier.size(36.dp),
                                color = ReaderGlassColors.MossGreenVibrant,
                                trackColor = readerTheme.dividerColor,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }

                // ===== PRE-GENERATE AUDIO (for Kokoro only) =====
                if (kokoroModelReady && sentences.isNotEmpty()) {
                    // audioGenerationState is already collected at top level (line 490)
                    // Re-check hasPreGenerated when generation state changes
                    val hasPreGenerated = remember(book.id, audioGenerationState) {
                        ttsViewModel.hasPreGeneratedAudio(book.id)
                    }
                    val cacheSize = remember(book.id, hasPreGenerated) {
                        if (hasPreGenerated) ttsViewModel.getPreGeneratedAudioSize(book.id) else 0L
                    }

                    Surface(
                        color = ReaderGlassColors.Whisky.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ReaderGlassColors.WhiskyBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = ReaderGlassColors.Whisky,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Instant Playback",
                                        color = readerTheme.textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        if (hasPreGenerated)
                                            "Audio ready (${(cacheSize / 1024 / 1024)}MB)"
                                        else
                                            "Pre-generate for lag-free reading",
                                        color = readerTheme.textSecondaryColor,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            when (audioGenerationState) {
                                is com.mossglen.reverie.tts.TtsAudioCache.GenerationState.Generating -> {
                                    val genState = audioGenerationState as com.mossglen.reverie.tts.TtsAudioCache.GenerationState.Generating
                                    val progress = genState.currentSentence.toFloat() / genState.totalSentences
                                    Column {
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = ReaderGlassColors.Whisky,
                                            trackColor = readerTheme.dividerColor
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Generating ${genState.currentSentence}/${genState.totalSentences}...",
                                                color = readerTheme.textSecondaryColor,
                                                fontSize = 12.sp
                                            )
                                            TextButton(
                                                onClick = { ttsViewModel.cancelPreGeneration() }
                                            ) {
                                                Text("Cancel", color = ReaderGlassColors.Whisky)
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (hasPreGenerated) {
                                            OutlinedButton(
                                                onClick = {
                                                    view.performHaptic(HapticType.MediumTap)
                                                    ttsViewModel.deletePreGeneratedAudio(book.id)
                                                    Toast.makeText(context, "Audio cache deleted", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, readerTheme.dividerColor)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Delete")
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                view.performHaptic(HapticType.HeavyTap)
                                                ttsViewModel.preGenerateBookAudio(
                                                    bookId = book.id,
                                                    sentences = sentences,
                                                    voiceId = 0,
                                                    onProgress = { /* UI updates via flow */ },
                                                    onComplete = {
                                                        Toast.makeText(context, "Audio ready for instant playback!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = { error ->
                                                        Toast.makeText(context, "Generation failed: $error", Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ReaderGlassColors.Whisky
                                            )
                                        ) {
                                            Icon(
                                                if (hasPreGenerated) Icons.Default.Refresh else Icons.Default.Bolt,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (hasPreGenerated) "Regenerate" else "Generate")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            // ========== FIXED TOP BAR - Always Visible ==========
            Surface(
                color = readerTheme.controlsBackground,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Back button
                    IconButton(
                        onClick = {
                            view.performHaptic(HapticType.LightTap)
                            onBack()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = readerTheme.controlsText
                        )
                    }

                    // Cover art thumbnail
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = readerTheme.dividerColor,
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (!book.coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = book.coverUrl,
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = readerTheme.controlsText.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Book info
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            book.title,
                            color = readerTheme.controlsText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            book.author.ifBlank { "Unknown Author" },
                            color = readerTheme.textSecondaryColor,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Options menu button with dropdown
                    Box {
                        IconButton(
                            onClick = {
                                view.performHaptic(HapticType.LightTap)
                                showTopBarMenu = true
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = readerTheme.controlsText
                            )
                        }

                        DropdownMenu(
                            expanded = showTopBarMenu,
                            onDismissRequest = { showTopBarMenu = false },
                            modifier = Modifier.background(readerTheme.controlsBackground)
                        ) {
                            // Theme
                            DropdownMenuItem(
                                text = { Text("Theme", color = readerTheme.controlsText) },
                                onClick = {
                                    showTopBarMenu = false
                                    showThemePicker = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Palette, null, tint = ReaderGlassColors.MossGreenVibrant)
                                }
                            )
                            // Font & Display
                            DropdownMenuItem(
                                text = { Text("Font & Display", color = readerTheme.controlsText) },
                                onClick = {
                                    showTopBarMenu = false
                                    showFontPicker = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.TextFields, null, tint = readerTheme.controlsText)
                                }
                            )
                            // Chapters
                            DropdownMenuItem(
                                text = { Text("Chapters", color = readerTheme.controlsText) },
                                onClick = {
                                    showTopBarMenu = false
                                    showChapterList = true
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = readerTheme.controlsText)
                                }
                            )
                            // Voice Settings
                            DropdownMenuItem(
                                text = { Text("Voice Settings", color = readerTheme.controlsText) },
                                onClick = {
                                    showTopBarMenu = false
                                    showTtsControls = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.RecordVoiceOver, null, tint = readerTheme.controlsText)
                                }
                            )
                            HorizontalDivider(color = readerTheme.dividerColor)
                            // Book Details (placeholder for now)
                            DropdownMenuItem(
                                text = { Text("Book Details", color = readerTheme.textSecondaryColor) },
                                onClick = {
                                    showTopBarMenu = false
                                    // TODO: Implement book details screen
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, null, tint = readerTheme.textSecondaryColor)
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                // ========== PREMIUM GLASS BOTTOM BAR ==========
                Surface(
                    color = readerTheme.controlsBackground.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(vertical = 12.dp)
                    ) {
                        // TTS Controls Row - Premium Styled
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // TTS Settings button - glass styled
                            val settingsInteraction = remember { MutableInteractionSource() }
                            val settingsPressed by settingsInteraction.collectIsPressedAsState()
                            val settingsScale by animateFloatAsState(
                                targetValue = if (settingsPressed) 0.9f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                                label = "settings"
                            )
                            Surface(
                                onClick = {
                                    view.performHaptic(HapticType.LightTap)
                                    showTtsControls = true
                                },
                                color = Color.White.copy(alpha = 0.08f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(44.dp)
                                    .scale(settingsScale),
                                interactionSource = settingsInteraction
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        Icons.Default.RecordVoiceOver,
                                        "TTS Settings",
                                        tint = readerTheme.textSecondaryColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(20.dp))

                            // TTS Play/Pause button - Premium glass with glow
                            val playInteraction = remember { MutableInteractionSource() }
                            val playPressed by playInteraction.collectIsPressedAsState()
                            val playScale by animateFloatAsState(
                                targetValue = if (playPressed) 0.92f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessHigh
                                ),
                                label = "play"
                            )
                            Surface(
                                onClick = {
                                    view.performHaptic(HapticType.Confirm)
                                    if (isTtsPlaying) {
                                        ttsViewModel.stop()
                                        isTtsPlaying = false
                                        currentSentenceIndex = -1
                                    } else {
                                        if (sentences.isNotEmpty()) {
                                            isTtsPlaying = true
                                            currentSentenceIndex = 0
                                            ttsViewModel.speak(sentences[0])
                                        }
                                    }
                                },
                                color = if (isTtsPlaying)
                                    ReaderGlassColors.MossGreenVibrant
                                else
                                    ReaderGlassColors.MossGreenVibrant.copy(alpha = 0.15f),
                                shape = CircleShape,
                                border = if (!isTtsPlaying)
                                    BorderStroke(2.dp, ReaderGlassColors.MossGreenVibrant.copy(alpha = 0.4f))
                                else null,
                                shadowElevation = if (isTtsPlaying) 8.dp else 0.dp,
                                modifier = Modifier
                                    .size(60.dp)
                                    .scale(playScale),
                                interactionSource = playInteraction
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        if (isTtsPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isTtsPlaying) "Stop" else "Read Aloud",
                                        tint = if (isTtsPlaying) Color.White else ReaderGlassColors.MossGreenVibrant,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(20.dp))

                            // Speed indicator - glass chip (opens speed sheet like audiobook player)
                            Surface(
                                onClick = {
                                    view.performHaptic(HapticType.LightTap)
                                    showSpeedSheet = true
                                },
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${String.format("%.1f", ttsSpeed)}x",
                                    color = readerTheme.textSecondaryColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }

                        // Chapter Navigation Row (only if multiple chapters)
                        if (totalChapters > 1) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous chapter - glass styled
                                Surface(
                                    onClick = {
                                        if (currentChapter > 0) {
                                            view.performHaptic(HapticType.MediumTap)
                                            if (isTtsPlaying) {
                                                ttsViewModel.stop()
                                                isTtsPlaying = false
                                                currentSentenceIndex = -1
                                            }
                                            currentChapter--
                                        }
                                    },
                                    color = if (currentChapter > 0)
                                        ReaderGlassColors.MossGreenVibrant.copy(alpha = 0.15f)
                                    else
                                        Color.Transparent,
                                    shape = CircleShape,
                                    enabled = currentChapter > 0,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            Icons.Default.ChevronLeft,
                                            null,
                                            tint = if (currentChapter > 0) ReaderGlassColors.MossGreenVibrant else readerTheme.textSecondaryColor.copy(alpha = 0.3f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                // Chapter progress - premium styled
                                Column(
                                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Chapter ${currentChapter + 1} of $totalChapters",
                                        color = readerTheme.controlsText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { (currentChapter + 1).toFloat() / totalChapters },
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = ReaderGlassColors.MossGreenVibrant,
                                        trackColor = readerTheme.dividerColor
                                    )
                                }

                                // Next chapter - glass styled
                                Surface(
                                    onClick = {
                                        if (currentChapter < totalChapters - 1) {
                                            view.performHaptic(HapticType.MediumTap)
                                            if (isTtsPlaying) {
                                                ttsViewModel.stop()
                                                isTtsPlaying = false
                                                currentSentenceIndex = -1
                                            }
                                            currentChapter++
                                        }
                                    },
                                    color = if (currentChapter < totalChapters - 1)
                                        ReaderGlassColors.MossGreenVibrant.copy(alpha = 0.15f)
                                    else
                                        Color.Transparent,
                                    shape = CircleShape,
                                    enabled = currentChapter < totalChapters - 1,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            null,
                                            tint = if (currentChapter < totalChapters - 1) ReaderGlassColors.MossGreenVibrant else readerTheme.textSecondaryColor.copy(alpha = 0.3f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = animatedBackgroundColor  // Smooth theme transition
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(animatedBackgroundColor),  // Smooth theme transition
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    // Premium loading state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = ReaderGlassColors.MossGreenVibrant,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Opening book...",
                            color = readerTheme.textSecondaryColor,
                            fontSize = 14.sp
                        )
                    }
                }
                errorMessage != null -> {
                    ErrorContent(
                        message = errorMessage!!,
                        accentColor = ReaderGlassColors.MossGreenVibrant,
                        onBack = onBack
                    )
                }
                else -> {
                    // ========== EPUB CONTENT AREA ==========
                    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
                    var verticalDragOffset by remember { mutableFloatStateOf(0f) }

                    // Gesture modifiers for content (swipe to navigate - tap removed for text selection)
                    val gestureModifier = Modifier
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (verticalDragOffset > 200f) {
                                        onBack()
                                    }
                                    verticalDragOffset = 0f
                                },
                                onDragCancel = { verticalDragOffset = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    if (dragAmount > 0 && scrollState.firstVisibleItemIndex == 0 &&
                                        scrollState.firstVisibleItemScrollOffset == 0
                                    ) {
                                        verticalDragOffset += dragAmount
                                    }
                                }
                            )
                        }

                    // Content based on display mode
                    when (displayMode) {
                        ReaderDisplayMode.SCROLL -> {
                            // ===== SCROLL MODE =====
                            LazyColumn(
                                state = scrollState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                                    .then(gestureModifier)
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (horizontalDragOffset > 100f && currentChapter > 0) {
                                                    currentChapter--
                                                } else if (horizontalDragOffset < -100f && currentChapter < totalChapters - 1) {
                                                    currentChapter++
                                                }
                                                horizontalDragOffset = 0f
                                                onSwipeEnd()
                                            },
                                            onDragCancel = {
                                                horizontalDragOffset = 0f
                                                onSwipeCancel()
                                            },
                                            onHorizontalDrag = { _, dragAmount ->
                                                horizontalDragOffset += dragAmount
                                                if (dragAmount > 0 && currentChapter == 0) {
                                                    onSwipe(dragAmount)
                                                }
                                            }
                                        )
                                    },
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                item {
                                    EpubHtmlContentWithHighlight(
                                        html = chapterContent,
                                        fontSize = fontSize,
                                        lineHeight = readerSettings.lineHeight,
                                        fontFamily = readerSettings.fontFamily.fontFamily,
                                        textColor = readerTheme.textColor,
                                        textSecondaryColor = readerTheme.textSecondaryColor,
                                        accentColor = ReaderGlassColors.MossGreenVibrant,
                                        dividerColor = readerTheme.dividerColor,
                                        highlightedSentenceIndex = if (isTtsPlaying) currentSentenceIndex else -1,
                                        sentences = sentences,
                                        isDark = readerTheme.isDark,
                                        onSentenceTap = { sentenceIndex ->
                                            // Tap-to-select: jump to this sentence and start reading
                                            Log.d("ReaderTTS", "User tapped sentence $sentenceIndex")
                                            view.performHaptic(HapticType.LightTap)
                                            currentSentenceIndex = sentenceIndex
                                            isTtsPlaying = true
                                            ttsViewModel.speak(sentences[sentenceIndex])
                                        }
                                    )
                                }
                            }
                        }

                        ReaderDisplayMode.PAGE -> {
                            // ===== PAGE MODE WITH FLIP ANIMATION =====
                            val pagerState = rememberPagerState(
                                initialPage = currentChapter,
                                pageCount = { totalChapters }
                            )

                            // Sync pager with chapter state
                            LaunchedEffect(currentChapter) {
                                if (pagerState.currentPage != currentChapter) {
                                    pagerState.animateScrollToPage(currentChapter)
                                }
                            }

                            LaunchedEffect(pagerState.currentPage) {
                                if (currentChapter != pagerState.currentPage) {
                                    currentChapter = pagerState.currentPage
                                }
                            }

                            // HorizontalPager for page mode with system gesture exclusion
                            // The systemGestureExclusion prevents system back gesture from
                            // intercepting swipes meant for page turning
                            Box(modifier = Modifier.fillMaxSize()) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .systemGestureExclusion(),  // Prevent system back gesture on edges
                                    pageSpacing = 0.dp,
                                    beyondViewportPageCount = 1,
                                    userScrollEnabled = true  // Ensure user can swipe
                                ) { page ->
                                // Apple-style page animation - subtle, elegant, professional
                                // Key principles: minimal movement, smooth crossfade, natural feel
                                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                                val absoluteOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            // Apple-style: subtle fade only, no flashy 3D effects
                                            // Pages smoothly crossfade during transition
                                            alpha = 1f - (absoluteOffset * 0.3f).coerceIn(0f, 0.3f)
                                        }
                                        .padding(horizontal = 16.dp)
                                        .background(
                                            animatedBackgroundColor,  // Smooth theme transition
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    // Load chapter content for this page
                                    val pageContent = remember(page, epubReader) {
                                        try {
                                            epubReader?.getChapterText(page) ?: ""
                                        } catch (e: Exception) {
                                            ""
                                        }
                                    }

                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        contentPadding = PaddingValues(vertical = 16.dp)
                                    ) {
                                        item {
                                            EpubHtmlContentWithHighlight(
                                                html = pageContent.ifEmpty { chapterContent },
                                                fontSize = fontSize,
                                                lineHeight = readerSettings.lineHeight,
                                                fontFamily = readerSettings.fontFamily.fontFamily,
                                                textColor = readerTheme.textColor,
                                                textSecondaryColor = readerTheme.textSecondaryColor,
                                                accentColor = ReaderGlassColors.MossGreenVibrant,
                                                dividerColor = readerTheme.dividerColor,
                                                highlightedSentenceIndex = if (isTtsPlaying && page == currentChapter) currentSentenceIndex else -1,
                                                sentences = if (page == currentChapter) sentences else emptyList(),
                                                isDark = readerTheme.isDark,
                                                onSentenceTap = if (page == currentChapter) { sentenceIndex ->
                                                    // Tap-to-select: jump to this sentence and start reading
                                                    Log.d("ReaderTTS", "User tapped sentence $sentenceIndex (page mode)")
                                                    view.performHaptic(HapticType.LightTap)
                                                    currentSentenceIndex = sentenceIndex
                                                    isTtsPlaying = true
                                                    ttsViewModel.speak(sentences[sentenceIndex])
                                                } else null
                                            )
                                        }
                                    }

                                    // Page number indicator
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 8.dp)
                                            .background(
                                                readerTheme.controlsBackground.copy(alpha = 0.8f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "${page + 1} / $totalChapters",
                                            color = readerTheme.textSecondaryColor,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                                // ===== PAGE NAVIGATION: SWIPE ONLY =====
                                // REMOVED edge tap zones - they conflicted with text selection
                                // Page turns are handled by HorizontalPager's built-in swipe gesture
                                // Users can swipe left/right anywhere on the page to navigate

                                // NOTE: Text selection is now enabled without interference
                                // Haptic feedback is provided by the pager on page change
                            }
                        }
                    }
                }
            }

            // ========== PILL NAVIGATION - Smart auto-hide based on scroll ==========
            AnimatedVisibility(
                visible = !showControls && showPillNavigation,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ReaderPillNavigation(
                    book = book,
                    currentChapter = currentChapter + 1,
                    totalChapters = totalChapters,
                    readingProgress = if (totalChapters > 0) (currentChapter + 1).toFloat() / totalChapters else 0f,
                    bookmarks = readingBookmarks,
                    bookmarkRippleTrigger = bookmarkRippleTrigger,
                    readerTheme = readerTheme,
                    isTtsPlaying = isTtsPlaying,
                    ttsSpeed = ttsViewModel.settings.collectAsState().value.speed,
                    onSettingsClick = { showSettingsSheet = true },
                    onTtsClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        if (isTtsPlaying) {
                            ttsViewModel.stop()
                            isTtsPlaying = false
                        } else {
                            // Start TTS from current position
                            currentSentenceIndex = 0
                            if (sentences.isNotEmpty()) {
                                isTtsPlaying = true
                                ttsViewModel.speak(sentences[0])
                            }
                        }
                    },
                    onSpeedClick = { showSpeedSheet = true },  // Open dedicated speed sheet
                    onRingTap = { showChaptersBookmarksDialog = true },
                    onRingLongPress = {
                        // Add bookmark with note
                        showBookmarkNoteDialog = true
                    },
                    onBookmarkClick = {
                        // Quick bookmark without note
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        libraryViewModel.addBookmarkWithNote(book.id, currentBookmarkPosition, "")
                        bookmarkRippleTrigger++
                        Toast.makeText(context, "Bookmark added", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // ========== CHAPTERS & BOOKMARKS DIALOG ==========
    if (showChaptersBookmarksDialog) {
        ReaderChaptersBookmarksDialog(
            chapterTitles = chapterTitles,
            currentChapter = currentChapter,
            bookmarks = readingBookmarks,
            bookmarkNotes = readingBookmarkNotes,
            readerTheme = readerTheme,
            onChapterClick = { chapterIndex ->
                currentChapter = chapterIndex
                showChaptersBookmarksDialog = false
            },
            onBookmarkClick = { positionMs ->
                // Extract chapter from bookmark position (chapterIndex * 1000000)
                val chapterFromBookmark = (positionMs / 1000000).toInt()
                currentChapter = chapterFromBookmark.coerceIn(0, totalChapters - 1)
                showChaptersBookmarksDialog = false
            },
            onDeleteBookmark = { positionMs ->
                libraryViewModel.deleteBookmark(book.id, positionMs)
            },
            onEditBookmarkNote = { positionMs, newNote ->
                libraryViewModel.addBookmarkWithNote(book.id, positionMs, newNote)
            },
            onDismiss = { showChaptersBookmarksDialog = false }
        )
    }

    // ========== BOOKMARK NOTE DIALOG ==========
    if (showBookmarkNoteDialog) {
        ReaderBookmarkNoteDialog(
            readerTheme = readerTheme,
            onConfirm = { note ->
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                libraryViewModel.addBookmarkWithNote(book.id, currentBookmarkPosition, note)
                bookmarkRippleTrigger++
                showBookmarkNoteDialog = false
                Toast.makeText(context, "Bookmark added", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showBookmarkNoteDialog = false }
        )
    }
}

// ============================================================================
// READER PILL NAVIGATION - Bottom floating bar with Library, Ring, Bookmark
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderPillNavigation(
    book: Book,
    currentChapter: Int,
    totalChapters: Int,
    readingProgress: Float,
    bookmarks: List<Long>,
    bookmarkRippleTrigger: Int,
    readerTheme: ReaderThemeData,
    isTtsPlaying: Boolean = false,
    ttsSpeed: Float = 1.0f,
    onSettingsClick: () -> Unit = {},
    onTtsClick: () -> Unit = {},
    onSpeedClick: () -> Unit = {},
    onRingTap: () -> Unit,
    onRingLongPress: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    val view = LocalView.current

    // Premium styling with whisky accents like audiobook player
    val pillBg = readerTheme.pillBackground
    val pillBorderColor = readerTheme.pillBorder
    val iconTint = readerTheme.accentColor
    val accentTint = if (readerTheme.isDark) {
        ReaderGlassColors.MossGreenVibrant
    } else {
        readerTheme.accentColor
    }
    val glowColor = readerTheme.cardGlow
    val whiskyAccent = ReaderGlassColors.Whisky
    val dividerColor = if (readerTheme.isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    // Premium floating pill with whisky accent line at top
    // Positioned at bottom with proper spacing from nav bar
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),  // Lower to match audiobook player
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main pill container with shadow - Fully rounded stadium shape
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(30.dp),
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
                .clip(RoundedCornerShape(30.dp))
                .background(pillBg)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            whiskyAccent.copy(alpha = 0.5f),  // Subtle accent at top
                            pillBorderColor.copy(alpha = 0.3f),
                            pillBorderColor
                        )
                    ),
                    shape = RoundedCornerShape(30.dp)
                )
        ) {
            // ===== PILL CONTENT =====
            Row(
                modifier = Modifier
                    .height(58.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ===== 4-ICON PILL: TTS | Ring | Bookmark | Menu =====
                // Clean layout matching audiobook player style

                // TTS Toggle - Clean icon, no border
                PremiumPillIcon(
                    icon = if (isTtsPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isTtsPlaying) "Stop Reading" else "Read Aloud",
                    isActive = isTtsPlaying,
                    defaultColor = iconTint,
                    activeColor = whiskyAccent,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onTtsClick()
                    }
                )

                // ===== SEPARATOR =====
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(dividerColor)
                )

                // ===== CENTER: Navigation Ring =====
                ReaderNavigationRing(
                    progress = readingProgress,
                    currentChapter = currentChapter,
                    totalChapters = totalChapters,
                    bookmarks = bookmarks,
                    bookmarkRippleTrigger = bookmarkRippleTrigger,
                    readerTheme = readerTheme,
                    onTap = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onRingTap()
                    },
                    onLongPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onRingLongPress()
                    }
                )

                // ===== SEPARATOR =====
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(dividerColor)
                )

                // Bookmark button - Outline style
                PremiumPillIcon(
                    icon = Icons.Filled.BookmarkBorder,
                    contentDescription = "Add Bookmark",
                    isActive = false,
                    defaultColor = iconTint,
                    activeColor = whiskyAccent,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onBookmarkClick()
                    }
                )

                // Speed - Quick access to TTS speed
                PremiumPillIcon(
                    icon = Icons.Filled.Speed,
                    contentDescription = "Playback Speed",
                    isActive = ttsSpeed != 1.0f,
                    defaultColor = iconTint,
                    activeColor = whiskyAccent,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onSpeedClick()
                    }
                )
            }
        }
    }
}

// ============================================================================
// PREMIUM PILL ICON - Matching audiobook player quality
// ============================================================================

/**
 * Premium icon button for pill UI with:
 * - Scale animation on press (0.85f spring bounce)
 * - Active/default color states
 * - Optional circle border for play/pause buttons
 * - Matches audiobook player styling (22dp icons)
 */
@Composable
private fun PremiumPillIcon(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    defaultColor: Color,
    activeColor: Color,
    onClick: () -> Unit,
    showBorder: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Spring bounce on press - matches audiobook player
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "pillIconScale"
    )

    // Color: accent when active, softer default
    val iconColor = if (isActive) activeColor else defaultColor

    Box(
        modifier = Modifier
            .scale(scale)
            .size(40.dp)
            .clip(CircleShape)
            .then(
                if (showBorder) Modifier.border(
                    width = 1.5.dp,
                    color = iconColor.copy(alpha = 0.5f),
                    shape = CircleShape
                ) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(22.dp)  // Match audiobook player size
        )
    }
}

// ============================================================================
// READER COMPACT TOP BAR - Always visible with book info
// ============================================================================

@Composable
private fun ReaderCompactTopBar(
    book: Book,
    readerTheme: ReaderThemeData,
    onBackClick: () -> Unit,
    onBookClick: () -> Unit
) {
    val view = LocalView.current
    val whiskyAccent = ReaderGlassColors.Whisky
    val glowColor = readerTheme.cardGlow

    // Premium floating bar with whisky accent at bottom
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Main bar container with shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
        ) {
            Column {
                // ===== BAR CONTENT =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(readerTheme.pillBackground)
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    readerTheme.pillBorder,
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Back button
                    Surface(
                        onClick = {
                            view.performHaptic(HapticType.LightTap)
                            onBackClick()
                        },
                        color = readerTheme.dividerColor.copy(alpha = 0.5f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = readerTheme.controlsText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Book cover thumbnail - tappable
                    Surface(
                        onClick = {
                            view.performHaptic(HapticType.MediumTap)
                            onBookClick()
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (!book.coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = book.coverUrl,
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        readerTheme.accentColor.copy(alpha = 0.2f),
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = readerTheme.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Title and Author - tappable area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                view.performHaptic(HapticType.MediumTap)
                                onBookClick()
                            }
                    ) {
                        Text(
                            book.title,
                            color = readerTheme.controlsText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (book.author.isNotBlank()) {
                            Text(
                                book.author,
                                color = readerTheme.textSecondaryColor,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Chevron indicating tappable
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "View Book Details",
                        tint = readerTheme.textSecondaryColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // ===== WHISKY ACCENT LINE AT BOTTOM =====
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    whiskyAccent.copy(alpha = 0.0f),
                                    whiskyAccent.copy(alpha = 0.6f),
                                    whiskyAccent.copy(alpha = 0.6f),
                                    whiskyAccent.copy(alpha = 0.0f)
                                )
                            )
                        )
                )
            }
        }
    }
}

// ============================================================================
// READER NAVIGATION RING - Progress ring with chapter display
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderNavigationRing(
    progress: Float,
    currentChapter: Int,
    totalChapters: Int,
    bookmarks: List<Long>,
    bookmarkRippleTrigger: Int = 0,
    readerTheme: ReaderThemeData? = null,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animation for press feedback
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "ringScale"
    )

    // Bookmark water ripple animation - 5 waves for smooth, natural effect
    val wave1Scale = remember { Animatable(1f) }
    val wave1Alpha = remember { Animatable(0f) }
    val wave2Scale = remember { Animatable(1f) }
    val wave2Alpha = remember { Animatable(0f) }
    val wave3Scale = remember { Animatable(1f) }
    val wave3Alpha = remember { Animatable(0f) }
    val coreGlow = remember { Animatable(0f) }

    val waterEasing = CubicBezierEasing(0.2f, 0.8f, 0.4f, 1f)

    LaunchedEffect(bookmarkRippleTrigger) {
        if (bookmarkRippleTrigger > 0) {
            // Reset all waves
            listOf(wave1Scale, wave2Scale, wave3Scale).forEach { it.snapTo(1f) }
            listOf(wave1Alpha, wave2Alpha, wave3Alpha).forEach { it.snapTo(0f) }
            coreGlow.snapTo(0f)

            // Core glow - bright flash on the ring
            launch {
                coreGlow.animateTo(0.7f, tween(80, easing = FastOutSlowInEasing))
                delay(150)
                coreGlow.animateTo(0f, tween(400, easing = waterEasing))
            }

            // Wave 1
            launch {
                wave1Alpha.snapTo(0.6f)
                launch { wave1Scale.animateTo(1.4f, tween(600, easing = waterEasing)) }
                wave1Alpha.animateTo(0f, tween(600, easing = waterEasing))
            }

            // Wave 2
            delay(80)
            launch {
                wave2Alpha.snapTo(0.4f)
                launch { wave2Scale.animateTo(1.6f, tween(700, easing = waterEasing)) }
                wave2Alpha.animateTo(0f, tween(700, easing = waterEasing))
            }

            // Wave 3
            delay(80)
            launch {
                wave3Alpha.snapTo(0.3f)
                launch { wave3Scale.animateTo(1.8f, tween(800, easing = waterEasing)) }
                wave3Alpha.animateTo(0f, tween(800, easing = waterEasing))
            }
        }
    }

    // Colors - Theme-aware for visibility in all modes
    val progressColor = ReaderGlassColors.MossGreen
    // Use theme-aware colors for light/sepia mode visibility
    val trackColor = readerTheme?.dividerColor ?: Color.White.copy(alpha = 0.35f)
    // Lighter, softer text color for chapter numbers
    val textColor = (readerTheme?.textSecondaryColor ?: Color.White).copy(alpha = 0.7f)

    // Ring dimensions - Smaller, more refined
    val ringSize = 44.dp  // Reduced from 52dp for better pill proportions
    val strokeWidth = 2.5.dp  // Slightly thinner

    Box(
        modifier = Modifier
            .size(ringSize)
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        // Draw the ring with progress
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val radius = (canvasSize - strokeWidth.toPx()) / 2
            val center = Offset(size.width / 2, size.height / 2)
            val startAngle = -90f // Start from top

            // Arc sizing
            val arcTopLeft = Offset(
                (size.width - canvasSize + strokeWidth.toPx()) / 2,
                (size.height - canvasSize + strokeWidth.toPx()) / 2
            )
            val arcSize = androidx.compose.ui.geometry.Size(
                canvasSize - strokeWidth.toPx(),
                canvasSize - strokeWidth.toPx()
            )

            val gapDegrees = 6f
            val progressSweep = progress * 360f

            // Track (remaining portion)
            if (progress < 0.98f) {
                val trackStartAngle = startAngle + progressSweep + gapDegrees
                val trackSweep = (360f - progressSweep - gapDegrees * 2).coerceAtLeast(0f)
                if (trackSweep > 0f) {
                    drawArc(
                        color = trackColor,
                        startAngle = trackStartAngle,
                        sweepAngle = trackSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Progress arc - Moss Green
            if (progress > 0.02f) {
                drawArc(
                    color = progressColor,
                    startAngle = startAngle,
                    sweepAngle = progressSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }

            // Bookmark ripple effect
            val rippleColor = ReaderGlassColors.MossGreen

            // Core glow
            if (coreGlow.value > 0f) {
                drawCircle(
                    color = rippleColor.copy(alpha = coreGlow.value * 0.4f),
                    radius = radius + 6.dp.toPx(),
                    center = center,
                    style = Stroke(width = 4.dp.toPx())
                )
                drawCircle(
                    color = rippleColor.copy(alpha = coreGlow.value * 0.8f),
                    radius = radius + 2.dp.toPx(),
                    center = center,
                    style = Stroke(width = strokeWidth.toPx() + 1.dp.toPx())
                )
            }

            // Wave 1
            if (wave1Alpha.value > 0f) {
                drawCircle(
                    color = rippleColor.copy(alpha = wave1Alpha.value),
                    radius = radius * wave1Scale.value,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Wave 2
            if (wave2Alpha.value > 0f) {
                drawCircle(
                    color = rippleColor.copy(alpha = wave2Alpha.value),
                    radius = radius * wave2Scale.value,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Wave 3
            if (wave3Alpha.value > 0f) {
                drawCircle(
                    color = rippleColor.copy(alpha = wave3Alpha.value),
                    radius = radius * wave3Scale.value,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // Center content - chapter count (larger, lighter)
        Text(
            text = "$currentChapter/$totalChapters",
            style = TextStyle(
                fontSize = 12.sp,  // Larger for readability
                fontWeight = FontWeight.Normal,  // Lighter weight
                letterSpacing = (-0.2).sp
            ),
            color = textColor
        )
    }
}

// ============================================================================
// READER CHAPTERS & BOOKMARKS DIALOG
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderChaptersBookmarksDialog(
    chapterTitles: List<String>,
    currentChapter: Int,
    bookmarks: List<Long>,
    bookmarkNotes: Map<Long, String>,
    readerTheme: ReaderThemeData,
    onChapterClick: (Int) -> Unit,
    onBookmarkClick: (Long) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onEditBookmarkNote: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = ReaderGlassColors.MossGreen
    val dialogBg = Color(0xFF1C1C1E)
    val glassBg = Color(0xFF2C2C2E)
    val selectionBg = ReaderGlassColors.MossGreen.copy(alpha = 0.15f)

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf("Chapters", "Bookmarks")

    fun formatChapterFromBookmark(positionMs: Long): String {
        val chapterIndex = (positionMs / 1000000).toInt()
        return "Chapter ${chapterIndex + 1}"
    }

    // Swipe-down dismiss state
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 150f

    // Entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val slideOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 400f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f),
        label = "dialogSlide"
    )

    val dialogAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "dialogAlpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .graphicsLayer {
                    alpha = dialogAlpha
                    translationY = slideOffset + dragOffsetY
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                    },
                    onDragStopped = {
                        if (dragOffsetY > dismissThreshold) {
                            onDismiss()
                        } else {
                            dragOffsetY = 0f
                        }
                    }
                )
                .clip(RoundedCornerShape(20.dp))
                .background(dialogBg)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Tab Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(glassBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isSelected) Modifier.background(accentColor)
                                    else Modifier
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    title,
                                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                                )
                                // Badge for bookmarks count
                                if (index == 1 && bookmarks.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) Color.White.copy(alpha = 0.3f)
                                                else accentColor.copy(alpha = 0.3f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${bookmarks.size}",
                                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = if (isSelected) Color.White else accentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pager content
                TabPager(
                    state = pagerState,
                    modifier = Modifier.height(320.dp)
                ) { page ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        when (page) {
                            0 -> {
                                // Chapters tab
                                if (chapterTitles.isNotEmpty()) {
                                    itemsIndexed(chapterTitles) { index, title ->
                                        val isCurrent = index == currentChapter
                                        ReaderChapterRow(
                                            title = title,
                                            chapterNumber = index + 1,
                                            isCurrent = isCurrent,
                                            accentColor = accentColor,
                                            selectionBg = selectionBg,
                                            onClick = { onChapterClick(index) }
                                        )
                                    }
                                } else {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 40.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.MenuBook,
                                                    null,
                                                    tint = Color.White.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    "No chapters found",
                                                    style = TextStyle(fontSize = 15.sp),
                                                    color = Color.White.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // Bookmarks tab
                                if (bookmarks.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 40.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.BookmarkBorder,
                                                    null,
                                                    tint = Color.White.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    "No bookmarks yet",
                                                    style = TextStyle(fontSize = 15.sp),
                                                    color = Color.White.copy(alpha = 0.5f)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "Long-press the ring to add a bookmark",
                                                    style = TextStyle(fontSize = 12.sp),
                                                    color = Color.White.copy(alpha = 0.3f)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    val sortedBookmarks = bookmarks.sortedDescending()
                                    items(sortedBookmarks) { positionMs ->
                                        val note = bookmarkNotes[positionMs]
                                        ReaderBookmarkRow(
                                            chapterLabel = formatChapterFromBookmark(positionMs),
                                            note = note,
                                            accentColor = accentColor,
                                            onClick = { onBookmarkClick(positionMs) },
                                            onDelete = { onDeleteBookmark(positionMs) },
                                            onEditNote = { newNote -> onEditBookmarkNote(positionMs, newNote) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Done button
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text("Done", color = accentColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderChapterRow(
    title: String,
    chapterNumber: Int,
    isCurrent: Boolean,
    accentColor: Color,
    selectionBg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isCurrent) Modifier.background(selectionBg)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Chapter number badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrent) accentColor
                        else Color.White.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$chapterNumber",
                    color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isCurrent) {
            Icon(
                Icons.Default.PlayArrow,
                null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderBookmarkRow(
    chapterLabel: String,
    note: String? = null,
    accentColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onEditNote: (String) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf(note ?: "") }
    val hasNote = !note.isNullOrBlank()

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Bookmark icon with note indicator
                Box {
                    Icon(
                        Icons.Default.Bookmark,
                        null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    if (hasNote) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                    }
                }
                Column {
                    Text(
                        text = chapterLabel,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White
                    )
                    if (hasNote) {
                        Text(
                            text = note!!,
                            style = TextStyle(fontSize = 13.sp),
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Tap to jump - Long press for options",
                            style = TextStyle(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Long-press dropdown menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = Color(0xFF2C2C2E)
        ) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            if (hasNote) Icons.Default.Edit else Icons.Default.NoteAdd,
                            null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            if (hasNote) "Edit Note" else "Add Note",
                            color = Color.White
                        )
                    }
                },
                onClick = {
                    showMenu = false
                    showEditNoteDialog = true
                }
            )
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Delete", color = Color(0xFFFF6B6B))
                    }
                },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }

    // Edit note dialog
    if (showEditNoteDialog) {
        AlertDialog(
            onDismissRequest = { showEditNoteDialog = false },
            title = { Text(if (hasNote) "Edit Note" else "Add Note", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = editingNote,
                    onValueChange = { editingNote = it },
                    placeholder = { Text("Enter a note...", color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = accentColor
                    ),
                    maxLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onEditNote(editingNote)
                    showEditNoteDialog = false
                }) {
                    Text("Save", color = accentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNoteDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF2C2C2E)
        )
    }
}

// ============================================================================
// BOOKMARK NOTE DIALOG - For adding bookmark with note on long-press
// ============================================================================

@Composable
private fun ReaderBookmarkNoteDialog(
    readerTheme: ReaderThemeData,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var noteText by remember { mutableStateOf("") }
    val accentColor = ReaderGlassColors.MossGreen

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.BookmarkAdd,
                    null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Text("Add Bookmark", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Add an optional note for this bookmark:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Enter a note (optional)...", color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = accentColor
                    ),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(noteText) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Add Bookmark", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF2C2C2E)
    )
}

// ============================================================================
// EPUB HTML CONTENT WITH SENTENCE HIGHLIGHTING
// ============================================================================

@Composable
private fun EpubHtmlContentWithHighlight(
    html: String,
    fontSize: Float,
    lineHeight: Float = 1.6f,
    fontFamily: FontFamily = FontFamily.Serif,
    textColor: Color,
    textSecondaryColor: Color,
    accentColor: Color,
    dividerColor: Color,
    highlightedSentenceIndex: Int = -1,
    sentences: List<String> = emptyList(),
    isDark: Boolean = false,
    onSentenceTap: ((Int) -> Unit)? = null  // Callback when user taps to select reading position
) {
    // Parse HTML and render as Compose text
    val doc = remember(html) { Jsoup.parse(html) }

    val elements = remember(html) {
        doc.body().children().toList()
    }

    // Highlight color based on theme
    val highlightColor = if (isDark)
        ReaderGlassColors.SentenceHighlightDark
    else
        ReaderGlassColors.SentenceHighlight

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        elements.forEach { element ->
            when (element.tagName().lowercase()) {
                "h1" -> {
                    Text(
                        text = element.text(),
                        color = textColor,
                        fontSize = (fontSize + 8).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                "h2" -> {
                    Text(
                        text = element.text(),
                        color = textColor,
                        fontSize = (fontSize + 6).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                }
                "h3", "h4", "h5", "h6" -> {
                    Text(
                        text = element.text(),
                        color = textColor,
                        fontSize = (fontSize + 4).sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = fontFamily,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                "p", "div" -> {
                    val text = element.text().trim()
                    if (text.isNotEmpty()) {
                        // Normalize whitespace for matching: collapse multiple spaces to single space
                        fun normalizeText(s: String) = s.replace(Regex("\\s+"), " ").trim()
                        val normalizedText = normalizeText(text)

                        // Check if this paragraph contains the highlighted sentence
                        val highlightedSentence = if (highlightedSentenceIndex >= 0 && highlightedSentenceIndex < sentences.size) {
                            sentences[highlightedSentenceIndex]
                        } else null
                        val normalizedHighlight = highlightedSentence?.let { normalizeText(it) }

                        // Find sentence index for tap-to-select using normalized comparison
                        val paragraphSentenceIndices = remember(normalizedText, sentences) {
                            sentences.mapIndexedNotNull { index, sentence ->
                                val normalizedSentence = normalizeText(sentence)
                                // Check if paragraph contains sentence (normalized)
                                // or if sentence contains start of paragraph (for partial matches)
                                if (normalizedText.contains(normalizedSentence, ignoreCase = true) ||
                                    normalizedSentence.contains(normalizedText.take(30), ignoreCase = true)) {
                                    index
                                } else null
                            }
                        }
                        val firstSentenceIndex = paragraphSentenceIndices.firstOrNull() ?: -1

                        val clickModifier = if (onSentenceTap != null && firstSentenceIndex >= 0) {
                            Modifier
                                .padding(vertical = 4.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onSentenceTap(firstSentenceIndex)
                                }
                        } else {
                            Modifier.padding(vertical = 4.dp)
                        }

                        // Check for highlight using normalized text comparison
                        val hasHighlight = normalizedHighlight != null &&
                            normalizedText.contains(normalizedHighlight, ignoreCase = true)

                        if (hasHighlight && highlightedSentence != null) {
                            // Find the actual position in original text for highlighting
                            // Use case-insensitive search to find the sentence
                            val normalizedLower = normalizedText.lowercase()
                            val sentenceLower = normalizeText(highlightedSentence).lowercase()
                            val startIndexNorm = normalizedLower.indexOf(sentenceLower)

                            if (startIndexNorm >= 0) {
                                // Map normalized position back to original text
                                // For simplicity, find sentence in original text (may have extra spaces)
                                val originalSentence = highlightedSentence.trim()
                                val startIndex = text.indexOf(originalSentence, ignoreCase = true)
                                    .takeIf { it >= 0 } ?: startIndexNorm
                                val sentenceLen = if (startIndex == startIndexNorm) sentenceLower.length else originalSentence.length
                                val endIndex = (startIndex + sentenceLen).coerceAtMost(text.length)

                                Text(
                                    text = buildAnnotatedString {
                                        append(text.substring(0, startIndex))
                                        withStyle(SpanStyle(background = highlightColor)) {
                                            append(text.substring(startIndex, endIndex))
                                        }
                                        if (endIndex < text.length) {
                                            append(text.substring(endIndex))
                                        }
                                    },
                                    color = textColor,
                                    fontSize = fontSize.sp,
                                    lineHeight = (fontSize * lineHeight).sp,
                                    fontFamily = fontFamily,
                                    modifier = clickModifier
                                )
                            } else {
                                // Fallback: highlight entire paragraph if sentence not found precisely
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(background = highlightColor.copy(alpha = 0.5f))) {
                                            append(text)
                                        }
                                    },
                                    color = textColor,
                                    fontSize = fontSize.sp,
                                    lineHeight = (fontSize * lineHeight).sp,
                                    fontFamily = fontFamily,
                                    modifier = clickModifier
                                )
                            }
                        } else {
                            Text(
                                text = text,
                                color = textColor,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * lineHeight).sp,
                                fontFamily = fontFamily,
                                modifier = clickModifier
                            )
                        }
                    }
                }
                "blockquote" -> {
                    Surface(
                        color = dividerColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(2.dp, accentColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = element.text(),
                            color = textSecondaryColor,
                            fontSize = fontSize.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontFamily = fontFamily,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                "ul", "ol" -> {
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        element.select("li").forEachIndexed { index, li ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = if (element.tagName() == "ol") "${index + 1}. " else "• ",
                                    color = accentColor,
                                    fontSize = fontSize.sp,
                                    fontFamily = fontFamily
                                )
                                Text(
                                    text = li.text(),
                                    color = textColor,
                                    fontSize = fontSize.sp,
                                    fontFamily = fontFamily
                                )
                            }
                        }
                    }
                }
                "hr" -> {
                    HorizontalDivider(
                        color = dividerColor,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                else -> {
                    val text = element.text().trim()
                    if (text.isNotEmpty()) {
                        Text(
                            text = text,
                            color = textColor,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * lineHeight).sp,
                            fontFamily = fontFamily
                        )
                    }
                }
            }
        }
    }
}

// ===== PDF READER =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfReaderContent(
    book: com.mossglen.reverie.data.Book,
    accentColor: Color,
    onBack: () -> Unit,
    onSwipe: (Float) -> Unit,
    onSwipeEnd: () -> Unit,
    onSwipeCancel: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Zoom & Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current

    // PDF renderer state
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    // Initialize PDF renderer
    LaunchedEffect(book.filePath) {
        withContext(Dispatchers.IO) {
            try {
                // Convert URI string to File - handles both file:// and content:// URIs
                val uri = Uri.parse(book.filePath)
                val file: File = when (uri.scheme) {
                    "file" -> {
                        // file:// URI - extract the path directly
                        File(uri.path!!)
                    }
                    "content" -> {
                        // content:// URI - copy to temp file for PDF rendering
                        val tempFile = File(context.cacheDir, "pdf_${book.id}.pdf")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile
                    }
                    else -> {
                        // Try as direct file path (legacy support)
                        File(book.filePath)
                    }
                }

                if (!file.exists()) {
                    errorMessage = "File not found: ${book.filePath}"
                    isLoading = false
                    return@withContext
                }

                fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor!!)
                totalPages = pdfRenderer!!.pageCount
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to open PDF: ${e.message}"
                isLoading = false
            }
        }
    }

    // Render current page
    LaunchedEffect(currentPage, pdfRenderer) {
        pdfRenderer?.let { renderer ->
            if (currentPage < renderer.pageCount) {
                withContext(Dispatchers.IO) {
                    try {
                        val page = renderer.openPage(currentPage)
                        val bitmap = Bitmap.createBitmap(
                            page.width * 2,
                            page.height * 2,
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        pageBitmap = bitmap
                    } catch (e: Exception) {
                        errorMessage = "Failed to render page: ${e.message}"
                    }
                }
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            pdfRenderer?.close()
            fileDescriptor?.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            book.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        if (totalPages > 0) {
                            Text(
                                "Page ${currentPage + 1} of $totalPages",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    if (scale != 1f) {
                        IconButton(onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }) {
                            Icon(Icons.Default.ZoomOut, null, tint = accentColor)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
            )
        },
        bottomBar = {
            if (totalPages > 0) {
                Surface(
                    color = Color(0xFF1C1C1E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentPage > 0) {
                                    currentPage--
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            },
                            enabled = currentPage > 0,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (currentPage > 0) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                null,
                                tint = if (currentPage > 0) accentColor else Color.DarkGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Slider(
                                value = currentPage.toFloat(),
                                onValueChange = {
                                    currentPage = it.toInt()
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                },
                                valueRange = 0f..(totalPages - 1).toFloat().coerceAtLeast(0f),
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor,
                                    inactiveTrackColor = Color(0xFF2C2C2E)
                                )
                            )
                        }

                        IconButton(
                            onClick = {
                                if (currentPage < totalPages - 1) {
                                    currentPage++
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            },
                            enabled = currentPage < totalPages - 1,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (currentPage < totalPages - 1) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                null,
                                tint = if (currentPage < totalPages - 1) accentColor else Color.DarkGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF1A1A1A))
                .then(
                    if (scale == 1f) {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = onSwipeEnd,
                                onDragCancel = onSwipeCancel,
                                onHorizontalDrag = { _, dragAmount ->
                                    onSwipe(dragAmount)
                                }
                            )
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = accentColor)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading PDF...", color = Color.Gray)
                    }
                }
                errorMessage != null -> {
                    ErrorContent(
                        message = errorMessage!!,
                        accentColor = accentColor,
                        onBack = onBack
                    )
                }
                pageBitmap != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 4f)
                                    if (scale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1f) {
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        } else {
                                            scale = 2f
                                        }
                                    },
                                    onTap = { offset ->
                                        val width = size.width
                                        if (offset.x < width * 0.3f && currentPage > 0) {
                                            currentPage--
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        } else if (offset.x > width * 0.7f && currentPage < totalPages - 1) {
                                            currentPage++
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = pageBitmap!!.asImageBitmap(),
                            contentDescription = "Page ${currentPage + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                else -> {
                    Text("No content to display", color = Color.Gray)
                }
            }
        }
    }
}

// ===== SHARED COMPONENTS =====

@Composable
private fun ErrorContent(
    message: String,
    accentColor: Color,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            Icons.Default.Error,
            null,
            tint = Color(0xFFFF6B6B),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            message,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = GlassColors.ButtonBackground),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Go Back", color = accentColor)
        }
    }
}

// ===== TEXT READER (TXT, DOCX, DOC) =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextReaderContent(
    book: Book,
    accentColor: Color,
    onBack: () -> Unit,
    onSwipe: (Float) -> Unit,
    onSwipeEnd: () -> Unit,
    onSwipeCancel: () -> Unit
) {
    val context = LocalContext.current
    var textContent by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    // Load text content
    LaunchedEffect(book.filePath) {
        try {
            val uri = Uri.parse(book.filePath)
            val extension = book.filePath.substringAfterLast('.', "").lowercase()

            textContent = when (extension) {
                "txt" -> {
                    // Simple text file
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
                "docx" -> {
                    // DOCX is a ZIP file containing XML
                    extractDocxText(context, uri)
                }
                "doc" -> {
                    // Legacy DOC format - limited support
                    "DOC format requires additional library support.\n\nPlease convert to DOCX or TXT format."
                }
                else -> null
            }

            if (textContent == null) {
                errorMessage = "Could not read file content"
            }
        } catch (e: Exception) {
            errorMessage = "Failed to open file: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                TopAppBar(
                    title = {
                        Text(
                            book.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.8f)
                    )
                )
            }
        },
        containerColor = Color(0xFF1A1A1A)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val screenWidth = size.width
                            val tapX = offset.x / screenWidth
                            when {
                                tapX in 0.35f..0.65f -> showControls = !showControls
                            }
                        }
                    )
                }
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }
                errorMessage != null -> {
                    ErrorContent(
                        message = errorMessage!!,
                        accentColor = accentColor,
                        onBack = onBack
                    )
                }
                textContent != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = textContent!!,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Extract text content from DOCX file.
 * DOCX is a ZIP file containing XML. The main content is in word/document.xml
 */
private fun extractDocxText(context: android.content.Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val zipInputStream = java.util.zip.ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            var documentXml: String? = null

            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    documentXml = zipInputStream.bufferedReader().readText()
                    break
                }
                entry = zipInputStream.nextEntry
            }

            documentXml?.let { xml ->
                // Simple XML text extraction - remove tags and decode entities
                xml.replace(Regex("<w:p[^>]*>"), "\n\n")  // Paragraph breaks
                    .replace(Regex("<w:br[^>]*/>"), "\n")  // Line breaks
                    .replace(Regex("<[^>]+>"), "")         // Remove all tags
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                    .replace(Regex("\n{3,}"), "\n\n")      // Normalize multiple newlines
                    .trim()
            }
        }
    } catch (e: Exception) {
        null
    }
}

// ============================================================================
// PREMIUM READING SPEED SHEET (matches audiobook player style)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSpeedSheet(
    currentSpeed: Float,
    readerTheme: ReaderThemeData,
    onSpeedChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current

    // Theme-aware colors
    val sheetBg = if (readerTheme.isDark) Color(0xFF1C1C1E) else readerTheme.backgroundColor
    val chipBg = if (readerTheme.isDark) Color(0xFF2C2C2E) else readerTheme.textColor.copy(alpha = 0.08f)
    val textPrimary = readerTheme.textColor
    val textSecondary = readerTheme.textSecondaryColor
    val accentColor = ReaderGlassColors.MossGreenVibrant

    // Speed state with slider
    var sliderSpeed by remember { mutableFloatStateOf(currentSpeed) }

    // Preset speeds matching audiobook player
    val presetSpeeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
        dragHandle = {
            // Custom drag handle matching player
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title row: "Reading speed" and current value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Reading Speed",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = textPrimary
                )
                Text(
                    String.format("%.2f×", sliderSpeed),
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Premium custom slider with minus/plus buttons (matching player)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Minus button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(chipBg)
                        .clickable {
                            val newSpeed = ((sliderSpeed - 0.05f) * 20).toInt() / 20f
                            sliderSpeed = newSpeed.coerceIn(0.5f, 3.0f)
                            onSpeedChanged(sliderSpeed)
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Remove,
                        contentDescription = "Decrease speed",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Custom premium slider with draggable scrubber/handle
                val handleSize = 18.dp
                val trackHeight = 4.dp
                val progress = ((sliderSpeed - 0.5f) / 2.5f).coerceIn(0f, 1f)

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val widthPx = size.width.toFloat()
                                    val deltaSpeed = (dragAmount.x / widthPx) * 2.5f
                                    val newSpeed = (sliderSpeed + deltaSpeed).coerceIn(0.5f, 3.0f)
                                    sliderSpeed = (newSpeed * 20).toInt() / 20f
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                },
                                onDragEnd = {
                                    onSpeedChanged(sliderSpeed)
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val widthPx = size.width.toFloat()
                                val tapProgress = (offset.x / widthPx).coerceIn(0f, 1f)
                                val newSpeed = 0.5f + (tapProgress * 2.5f)
                                sliderSpeed = (newSpeed * 20).toInt() / 20f
                                onSpeedChanged(sliderSpeed)
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    val trackWidth = maxWidth
                    val handleOffset = (trackWidth - handleSize) * progress

                    // Track background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(trackHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .align(Alignment.Center)
                    )

                    // Active track (filled portion) - accent colored
                    Box(
                        modifier = Modifier
                            .width(handleOffset + handleSize / 2)
                            .height(trackHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentColor)
                            .align(Alignment.CenterStart)
                    )

                    // Premium scrubber handle
                    Box(
                        modifier = Modifier
                            .offset(x = handleOffset)
                            .size(handleSize)
                            .clip(CircleShape)
                            .background(accentColor)
                            .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                    )
                }

                // Plus button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(chipBg)
                        .clickable {
                            val newSpeed = ((sliderSpeed + 0.05f) * 20).toInt() / 20f
                            sliderSpeed = newSpeed.coerceIn(0.5f, 3.0f)
                            onSpeedChanged(sliderSpeed)
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Increase speed",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preset chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetSpeeds.forEach { speed ->
                    val isSelected = kotlin.math.abs(sliderSpeed - speed) < 0.01f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipBg)
                            .clickable {
                                sliderSpeed = speed
                                onSpeedChanged(speed)
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            String.format("%.2f", speed),
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isSelected) accentColor else textSecondary
                        )
                    }
                }
            }
        }
    }
}
