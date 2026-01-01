package com.mossglen.lithos.service

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.FileProvider
import androidx.media.MediaBrowserServiceCompat
import coil.ImageLoader
import com.mossglen.lithos.MainActivity
import com.mossglen.lithos.R
import com.mossglen.lithos.data.AudioHandler
import com.mossglen.lithos.data.Book
import com.mossglen.lithos.data.LibraryRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * LithosMediaService - Production-grade Android Auto integration.
 *
 * Features:
 * - Full MediaBrowserServiceCompat for Android Auto
 * - Dynamic content hierarchy (Now Playing, Library, Favorites, Search)
 * - Album art with proper bitmap resizing (max 512x512)
 * - Animated equalizer bars ("Spotify visualizer")
 * - Voice search support
 * - Audio focus management
 * - State observation and synchronization
 *
 * Architecture:
 * - Bridges to AudioHandler singleton for playback control
 * - Observes playback state and updates MediaSession accordingly
 * - Uses Coil for efficient image loading and caching
 */
@AndroidEntryPoint
class LithosMediaService : MediaBrowserServiceCompat() {

    companion object {
        private const val TAG = "LithosAutoDebug"

        // Allowed client packages for security
        private val ALLOWED_PACKAGES = setOf(
            "com.google.android.projection.gearhead",     // Android Auto
            "com.google.android.googlequicksearchbox",    // Google Assistant
            "com.google.android.carassistant",            // Car Assistant
            "com.android.bluetooth",                       // Bluetooth audio
            "com.mossglen.lithos"                         // Self (for debugging)
        )

        // Root and browse IDs
        private const val MEDIA_ROOT_ID = "lithos_root"
        private const val BROWSABLE_NOW_PLAYING = "browsable_now_playing"
        private const val BROWSABLE_LIBRARY = "browsable_library"
        private const val BROWSABLE_FAVORITES = "browsable_favorites"
        private const val BROWSABLE_SEARCH = "browsable_search"
        private const val BROWSABLE_BY_AUTHOR = "browsable_by_author"
        private const val BROWSABLE_BY_SERIES = "browsable_by_series"
        private const val BROWSABLE_RECENT = "browsable_recent"

        // Media ID prefixes
        private const val PREFIX_BOOK = "book_"
        private const val PREFIX_CHAPTER = "chapter_"
        private const val PREFIX_AUTHOR = "author_"
        private const val PREFIX_SERIES = "series_"

        // Content style hints for Android Auto
        private const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        private const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        private const val CONTENT_STYLE_LIST = 1
        private const val CONTENT_STYLE_GRID = 2

        // Extras for animated equalizer bars
        private const val EXTRA_DURATION = "android.media.metadata.DURATION"

        // Maximum album art size to prevent TransactionTooLargeException
        private const val MAX_ART_SIZE = 512
    }

    @Inject lateinit var repository: LibraryRepository
    @Inject lateinit var audioHandler: AudioHandler

    private var mediaSession: MediaSessionCompat? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

    // Image loader for album art
    private lateinit var imageLoader: ImageLoader

    // Audio focus
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var wasPlayingBeforeFocusLoss = false

    // Current state tracking
    private var currentPlayingMediaId: String? = null
    private var currentAlbumArt: Bitmap? = null

    // ========================================================================
    // LIFECYCLE
    // ========================================================================

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "⚡ LithosMediaService onCreate - Initializing Android Auto integration")

        imageLoader = ImageLoader.Builder(this)
            .crossfade(false)
            .build()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        initializeMediaSession()
        observePlaybackState()

        Log.d(TAG, "⚡ LithosMediaService ready for Android Auto connections")
    }

    private fun initializeMediaSession() {
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSessionCompat(this, TAG).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or
                MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS
            )

            setSessionActivity(sessionActivityPendingIntent)
            setCallback(MediaSessionCallback())

            // Set initial playback state
            setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_NONE, 0L))

            isActive = true
        }

        // Critical: Set session token immediately
        sessionToken = mediaSession?.sessionToken
        Log.d(TAG, "MediaSession initialized, token set")
    }

    private fun observePlaybackState() {
        // Observe playing state
        serviceScope.launch {
            audioHandler.isPlaying.collectLatest { isPlaying ->
                updatePlaybackState(isPlaying)
            }
        }

        // Observe position for scrubbing
        serviceScope.launch {
            audioHandler.currentPosition.collectLatest { position ->
                val isPlaying = audioHandler.isPlaying.value
                val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                mediaSession?.setPlaybackState(buildPlaybackState(state, position))
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "⚡ LithosMediaService onDestroy - Cleaning up")
        abandonAudioFocus()
        mediaSession?.release()
        mediaSession = null
        serviceScope.cancel()
        currentAlbumArt?.recycle()
        Log.d(TAG, "⚡ LithosMediaService destroyed")
        super.onDestroy()
    }

    // ========================================================================
    // MEDIA BROWSER SERVICE - CONTENT HIERARCHY
    // ========================================================================

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        Log.d(TAG, "⚡ onGetRoot() from: $clientPackageName (uid: $clientUid)")

        // Security: Only allow known clients (Android Auto, Google Assistant, etc.)
        if (clientPackageName !in ALLOWED_PACKAGES) {
            Log.w(TAG, "⚡ REJECTED connection from unauthorized package: $clientPackageName")
            return null // Reject unknown clients
        }

        Log.d(TAG, "⚡ ACCEPTED connection from: $clientPackageName")

        val extras = Bundle().apply {
            putBoolean(CONTENT_STYLE_SUPPORTED, true)
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
        }

        return BrowserRoot(MEDIA_ROOT_ID, extras)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        Log.d(TAG, "onLoadChildren: $parentId")
        result.detach()

        serviceScope.launch {
            val items = when (parentId) {
                MEDIA_ROOT_ID -> buildRootMenu()
                BROWSABLE_NOW_PLAYING -> buildNowPlayingItems()
                BROWSABLE_LIBRARY -> buildLibraryItems()
                BROWSABLE_FAVORITES -> buildFavoritesItems()
                BROWSABLE_RECENT -> buildRecentItems()
                BROWSABLE_BY_AUTHOR -> buildAuthorList()
                BROWSABLE_BY_SERIES -> buildSeriesList()
                else -> handleDynamicParent(parentId)
            }

            Log.d(TAG, "Returning ${items.size} items for $parentId")
            result.sendResult(items)
        }
    }

    private suspend fun buildRootMenu(): MutableList<MediaBrowserCompat.MediaItem> {
        val items = mutableListOf<MediaBrowserCompat.MediaItem>()

        // Only show "Now Playing" if there's an active book
        val currentBook = audioHandler.getCurrentBook()
        if (currentBook != null) {
            items.add(createBrowsableItem(
                mediaId = BROWSABLE_NOW_PLAYING,
                title = "Now Playing",
                subtitle = currentBook.title,
                iconResId = R.drawable.ic_play_circle
            ))
        }

        // Main menu items with resource URIs for sharp icons
        items.add(createBrowsableItem(
            mediaId = BROWSABLE_RECENT,
            title = "Continue Listening",
            subtitle = "Pick up where you left off",
            iconResId = R.drawable.ic_history
        ))

        items.add(createBrowsableItem(
            mediaId = BROWSABLE_LIBRARY,
            title = "Library",
            subtitle = "All your audiobooks",
            iconResId = R.drawable.ic_library
        ))

        items.add(createBrowsableItem(
            mediaId = BROWSABLE_BY_AUTHOR,
            title = "By Author",
            subtitle = "Browse by author",
            iconResId = R.drawable.ic_person
        ))

        items.add(createBrowsableItem(
            mediaId = BROWSABLE_BY_SERIES,
            title = "By Series",
            subtitle = "Browse by series",
            iconResId = R.drawable.ic_collections
        ))

        items.add(createBrowsableItem(
            mediaId = BROWSABLE_FAVORITES,
            title = "Favorites",
            subtitle = "Your favorite books",
            iconResId = R.drawable.ic_favorite
        ))

        return items
    }

    private suspend fun buildNowPlayingItems(): MutableList<MediaBrowserCompat.MediaItem> {
        val currentBook = audioHandler.getCurrentBook() ?: return mutableListOf()

        return if (currentBook.chapters.isNotEmpty()) {
            currentBook.chapters.mapIndexed { index, chapter ->
                val chapterId = "${PREFIX_CHAPTER}${currentBook.id}_$index"
                val durationMs = if (index < currentBook.chapters.size - 1) {
                    currentBook.chapters[index + 1].startMs - chapter.startMs
                } else {
                    currentBook.duration - chapter.startMs
                }

                createPlayableItem(
                    mediaId = chapterId,
                    title = chapter.title.ifEmpty { "Chapter ${index + 1}" },
                    subtitle = formatDuration(chapter.startMs),
                    iconUri = getContentUri(currentBook.coverUrl),
                    durationMs = durationMs
                )
            }.toMutableList()
        } else {
            mutableListOf(createPlayableItem(
                mediaId = currentBook.id,
                title = currentBook.title,
                subtitle = currentBook.author,
                iconUri = getContentUri(currentBook.coverUrl),
                durationMs = currentBook.duration
            ))
        }
    }

    private suspend fun buildLibraryItems(): MutableList<MediaBrowserCompat.MediaItem> {
        val books = repository.getAllBooksDirect()
            .filter { it.format == "AUDIO" }
            .sortedBy { it.title.lowercase() }

        return books.map { book ->
            createPlayableItem(
                mediaId = book.id,
                title = book.title,
                subtitle = book.author,
                iconUri = getContentUri(book.coverUrl),
                durationMs = book.duration
            )
        }.toMutableList()
    }

    private suspend fun buildFavoritesItems(): MutableList<MediaBrowserCompat.MediaItem> {
        // Note: Favorites feature requires adding isFavorite field to Book entity
        // For now, show most-played books as "favorites"
        val books = repository.getAllBooksDirect()
            .filter { it.format == "AUDIO" && it.progress > 0 }
            .sortedByDescending { it.lastPlayedTimestamp }
            .take(5)

        return if (books.isNotEmpty()) {
            books.map { book ->
                val progressPercent = if (book.duration > 0) {
                    ((book.progress.toFloat() / book.duration) * 100).toInt()
                } else 0
                createPlayableItem(
                    mediaId = book.id,
                    title = book.title,
                    subtitle = "${book.author} • $progressPercent%",
                    iconUri = getContentUri(book.coverUrl),
                    durationMs = book.duration
                )
            }.toMutableList()
        } else {
            mutableListOf(createEmptyItem("No favorites yet", "Start listening to build your favorites"))
        }
    }

    private suspend fun buildRecentItems(): MutableList<MediaBrowserCompat.MediaItem> {
        val books = repository.getAllBooksDirect()
            .filter { it.format == "AUDIO" && it.progress > 0 }
            .sortedByDescending { it.lastPlayedTimestamp }
            .take(10)

        return if (books.isNotEmpty()) {
            books.map { book ->
                val progressPercent = if (book.duration > 0) {
                    ((book.progress.toFloat() / book.duration) * 100).toInt()
                } else 0
                val remainingMs = book.duration - book.progress
                val subtitle = "${book.author} • $progressPercent% • ${formatDuration(remainingMs)} left"

                createPlayableItem(
                    mediaId = book.id,
                    title = book.title,
                    subtitle = subtitle,
                    iconUri = getContentUri(book.coverUrl),
                    durationMs = book.duration
                )
            }.toMutableList()
        } else {
            mutableListOf(createEmptyItem("No recent books", "Start listening to see books here"))
        }
    }

    private suspend fun buildAuthorList(): MutableList<MediaBrowserCompat.MediaItem> {
        val authors = repository.getUniqueAuthorsForAuto()
        return authors.map { author ->
            createBrowsableItem(
                mediaId = "${PREFIX_AUTHOR}$author",
                title = author,
                subtitle = "Tap to see books",
                iconResId = R.drawable.ic_person
            )
        }.toMutableList()
    }

    private suspend fun buildSeriesList(): MutableList<MediaBrowserCompat.MediaItem> {
        val series = repository.getUniqueSeriesForAuto()
        return if (series.isNotEmpty()) {
            series.map { seriesName ->
                createBrowsableItem(
                    mediaId = "${PREFIX_SERIES}$seriesName",
                    title = seriesName,
                    subtitle = "Tap to see books",
                    iconResId = R.drawable.ic_collections
                )
            }.toMutableList()
        } else {
            mutableListOf(createEmptyItem("No series found", "Books with series info will appear here"))
        }
    }

    private suspend fun handleDynamicParent(parentId: String): MutableList<MediaBrowserCompat.MediaItem> {
        return when {
            parentId.startsWith(PREFIX_AUTHOR) -> {
                val author = parentId.removePrefix(PREFIX_AUTHOR)
                buildBooksByAuthor(author)
            }
            parentId.startsWith(PREFIX_SERIES) -> {
                val series = parentId.removePrefix(PREFIX_SERIES)
                buildBooksBySeries(series)
            }
            parentId.startsWith(PREFIX_BOOK) -> {
                val bookId = parentId.removePrefix(PREFIX_BOOK)
                buildChaptersForBook(bookId)
            }
            else -> {
                // Try to load as book ID for chapters
                buildChaptersForBook(parentId)
            }
        }
    }

    private suspend fun buildBooksByAuthor(author: String): MutableList<MediaBrowserCompat.MediaItem> {
        val books = repository.getAllBooksDirect()
            .filter { it.format == "AUDIO" && it.author.equals(author, ignoreCase = true) }
            .sortedByDescending { it.lastPlayedTimestamp }

        return books.map { book ->
            val progressPercent = if (book.duration > 0 && book.progress > 0) {
                " • ${((book.progress.toFloat() / book.duration) * 100).toInt()}%"
            } else ""

            createPlayableItem(
                mediaId = book.id,
                title = book.title,
                subtitle = formatDuration(book.duration) + progressPercent,
                iconUri = getContentUri(book.coverUrl),
                durationMs = book.duration
            )
        }.toMutableList()
    }

    private suspend fun buildBooksBySeries(series: String): MutableList<MediaBrowserCompat.MediaItem> {
        val books = repository.getAllBooksDirect()
            .filter { it.format == "AUDIO" && it.seriesInfo.equals(series, ignoreCase = true) }
            .sortedBy { it.title }

        return books.map { book ->
            val progressPercent = if (book.duration > 0 && book.progress > 0) {
                " • ${((book.progress.toFloat() / book.duration) * 100).toInt()}%"
            } else ""

            createPlayableItem(
                mediaId = book.id,
                title = book.title,
                subtitle = book.author + progressPercent,
                iconUri = getContentUri(book.coverUrl),
                durationMs = book.duration
            )
        }.toMutableList()
    }

    private suspend fun buildChaptersForBook(bookId: String): MutableList<MediaBrowserCompat.MediaItem> {
        val book = repository.getBookById(bookId) ?: return mutableListOf()

        return if (book.chapters.isNotEmpty()) {
            book.chapters.mapIndexed { index, chapter ->
                val durationMs = if (index < book.chapters.size - 1) {
                    book.chapters[index + 1].startMs - chapter.startMs
                } else {
                    book.duration - chapter.startMs
                }

                createPlayableItem(
                    mediaId = "${PREFIX_CHAPTER}${book.id}_$index",
                    title = chapter.title.ifEmpty { "Chapter ${index + 1}" },
                    subtitle = formatDuration(durationMs),
                    iconUri = getContentUri(book.coverUrl),
                    durationMs = durationMs
                )
            }.toMutableList()
        } else {
            mutableListOf(createEmptyItem("No chapters", "This book has no chapter markers"))
        }
    }

    // ========================================================================
    // VOICE SEARCH SUPPORT
    // ========================================================================

    override fun onSearch(
        query: String,
        extras: Bundle?,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        Log.d(TAG, "onSearch: $query")
        result.detach()

        serviceScope.launch {
            val searchResults = searchBooks(query)
            result.sendResult(searchResults)

            // Auto-play if exact match found
            if (searchResults.size == 1 && searchResults[0].isPlayable) {
                val mediaId = searchResults[0].mediaId
                if (mediaId != null) {
                    playFromMediaId(mediaId)
                }
            }
        }
    }

    private suspend fun searchBooks(query: String): MutableList<MediaBrowserCompat.MediaItem> {
        val queryLower = query.lowercase().trim()
        val books = repository.getAllBooksDirect()
            .filter { it.format == "AUDIO" }
            .filter { book ->
                book.title.lowercase().contains(queryLower) ||
                book.author.lowercase().contains(queryLower) ||
                book.seriesInfo.lowercase().contains(queryLower)
            }
            .sortedBy { book ->
                // Prioritize exact title matches
                when {
                    book.title.lowercase() == queryLower -> 0
                    book.title.lowercase().startsWith(queryLower) -> 1
                    else -> 2
                }
            }
            .take(10)

        return books.map { book ->
            createPlayableItem(
                mediaId = book.id,
                title = book.title,
                subtitle = book.author,
                iconUri = getContentUri(book.coverUrl),
                durationMs = book.duration
            )
        }.toMutableList()
    }

    // ========================================================================
    // MEDIA SESSION CALLBACK
    // ========================================================================

    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            Log.d(TAG, "⚡ onPlayFromMediaId() received from Auto: $mediaId")
            if (mediaId == null) {
                Log.w(TAG, "⚡ onPlayFromMediaId() - mediaId is null, ignoring")
                return
            }

            if (requestAudioFocus()) {
                Log.d(TAG, "⚡ Audio focus granted, starting playback for: $mediaId")
                playFromMediaId(mediaId)
            } else {
                Log.w(TAG, "⚡ Audio focus denied, cannot play: $mediaId")
            }
        }

        override fun onPlay() {
            Log.d(TAG, "⚡ onPlay() received from Auto")
            if (requestAudioFocus()) {
                Log.d(TAG, "⚡ Audio focus granted, resuming playback")
                audioHandler.play()
            } else {
                Log.w(TAG, "⚡ Audio focus denied, cannot resume")
            }
        }

        override fun onPause() {
            Log.d(TAG, "⚡ onPause() received from Auto")
            audioHandler.pause()
        }

        override fun onStop() {
            Log.d(TAG, "⚡ onStop() received from Auto")
            audioHandler.pause()
            abandonAudioFocus()
        }

        override fun onSkipToNext() {
            Log.d(TAG, "⚡ onSkipToNext() received from Auto (30s forward)")
            audioHandler.skipForward(30)
        }

        override fun onSkipToPrevious() {
            Log.d(TAG, "⚡ onSkipToPrevious() received from Auto (10s back)")
            audioHandler.skipBack(10)
        }

        override fun onSeekTo(pos: Long) {
            Log.d(TAG, "⚡ onSeekTo() received from Auto: ${pos}ms")
            audioHandler.seekTo(pos)
        }

        override fun onSetPlaybackSpeed(speed: Float) {
            Log.d(TAG, "⚡ onSetPlaybackSpeed() received from Auto: ${speed}x")
            audioHandler.setPlaybackSpeed(speed)
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            Log.d(TAG, "⚡ onPlayFromSearch() received from Auto: \"$query\"")
            if (query.isNullOrBlank()) {
                Log.w(TAG, "⚡ onPlayFromSearch() - query is blank, ignoring")
                return
            }

            serviceScope.launch {
                val results = searchBooks(query)
                Log.d(TAG, "⚡ Search found ${results.size} results for: \"$query\"")
                if (results.isNotEmpty()) {
                    val firstResult = results[0]
                    if (firstResult.isPlayable && firstResult.mediaId != null) {
                        Log.d(TAG, "⚡ Auto-playing first result: ${firstResult.description.title}")
                        playFromMediaId(firstResult.mediaId!!)
                    }
                }
            }
        }
    }

    private fun playFromMediaId(mediaId: String) {
        // IMMEDIATELY set buffering state so the car UI shows we're working
        Log.d(TAG, "⚡ Setting STATE_BUFFERING for: $mediaId")
        mediaSession?.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_BUFFERING, 0L))
        currentPlayingMediaId = mediaId

        // Do all heavy work on IO thread to prevent UI stutter
        serviceScope.launch(Dispatchers.IO) {
            try {
                when {
                    mediaId.startsWith(PREFIX_CHAPTER) -> {
                        val parts = mediaId.removePrefix(PREFIX_CHAPTER).split("_")
                        if (parts.size >= 2) {
                            val bookId = parts.dropLast(1).joinToString("_")
                            val chapterIndex = parts.last().toIntOrNull() ?: 0
                            val book = repository.getBookById(bookId)
                            if (book != null && chapterIndex < book.chapters.size) {
                                val chapter = book.chapters[chapterIndex]
                                Log.d(TAG, "⚡ Playing chapter: ${chapter.title}")

                                // Load metadata and art first
                                updateMetadataAsync(book, mediaId)

                                // Then start playback on Main thread
                                withContext(Dispatchers.Main) {
                                    audioHandler.loadBookAndSeek(book, chapter.startMs)
                                    audioHandler.play()
                                }
                            }
                        }
                    }
                    else -> {
                        val book = repository.getBookById(mediaId)
                        if (book != null) {
                            Log.d(TAG, "⚡ Playing book: ${book.title}")

                            // Load metadata and art first
                            updateMetadataAsync(book, mediaId)

                            // Then start playback on Main thread
                            withContext(Dispatchers.Main) {
                                audioHandler.playBook(book)
                            }
                        } else {
                            Log.w(TAG, "⚡ Book not found: $mediaId")
                            withContext(Dispatchers.Main) {
                                mediaSession?.setPlaybackState(
                                    buildPlaybackState(PlaybackStateCompat.STATE_ERROR, 0L)
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "⚡ Error playing media: $mediaId", e)
                withContext(Dispatchers.Main) {
                    mediaSession?.setPlaybackState(
                        buildPlaybackState(PlaybackStateCompat.STATE_ERROR, 0L)
                    )
                }
            }
        }
    }

    // ========================================================================
    // METADATA & ALBUM ART
    // ========================================================================

    /**
     * Update metadata asynchronously with robust album art loading.
     * Called from IO thread - handles all bitmap loading and metadata setting.
     */
    private suspend fun updateMetadataAsync(book: Book, mediaId: String) {
        Log.d(TAG, "⚡ updateMetadataAsync: Loading art for ${book.title}")

        // Load and resize album art on IO thread
        val albumArt = resolveCoverArt(book.coverUrl)

        if (albumArt != null) {
            Log.d(TAG, "⚡ Album art loaded: ${albumArt.width}x${albumArt.height}")
        } else {
            Log.w(TAG, "⚡ Album art is NULL for: ${book.coverUrl}")
        }

        // Build metadata
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId) // Critical for equalizer bars
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, book.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, book.author)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, book.seriesInfo.ifEmpty { book.title })
            .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, book.author)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, book.duration)
            .apply {
                // Set bitmap if we have it
                if (albumArt != null) {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                    putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, albumArt)
                    Log.d(TAG, "⚡ Bitmap attached to metadata")
                }

                // Also add URI for displays that prefer URIs
                getContentUri(book.coverUrl)?.let { uri ->
                    putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, uri.toString())
                    putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, uri.toString())
                    Log.d(TAG, "⚡ Art URI attached: $uri")
                }
            }
            .build()

        // Set metadata on Main thread
        withContext(Dispatchers.Main) {
            mediaSession?.setMetadata(metadata)
            Log.d(TAG, "⚡ Metadata set on MediaSession")
        }

        // Store for recycling later
        currentAlbumArt?.recycle()
        currentAlbumArt = albumArt
    }

    /**
     * Robust cover art loader using BitmapFactory.
     * Handles file://, content://, and raw file paths.
     * Scales to 512x512 max to prevent TransactionTooLargeException.
     */
    private fun resolveCoverArt(coverPath: String?): Bitmap? {
        if (coverPath.isNullOrEmpty()) {
            Log.d(TAG, "⚡ resolveCoverArt: No cover path provided")
            return null
        }

        return try {
            Log.d(TAG, "⚡ resolveCoverArt: Loading from $coverPath")

            val uri = when {
                coverPath.startsWith("content://") -> Uri.parse(coverPath)
                coverPath.startsWith("file://") -> Uri.parse(coverPath)
                coverPath.startsWith("/") -> Uri.fromFile(File(coverPath))
                else -> Uri.fromFile(File(coverPath))
            }

            resolveCoverArtFromUri(uri)
        } catch (e: Exception) {
            Log.e(TAG, "⚡ resolveCoverArt failed: ${e.message}", e)
            null
        }
    }

    /**
     * Load and scale bitmap from URI using BitmapFactory.
     * Uses two-pass decode: first to get dimensions, then to load scaled.
     */
    private fun resolveCoverArtFromUri(uri: Uri): Bitmap? {
        return try {
            val pfd = contentResolver.openFileDescriptor(uri, "r")
            if (pfd == null) {
                Log.w(TAG, "⚡ Could not open file descriptor for: $uri")
                return null
            }

            pfd.use { descriptor ->
                val fd = descriptor.fileDescriptor

                // First pass: get dimensions only
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFileDescriptor(fd, null, boundsOptions)

                if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                    Log.w(TAG, "⚡ Invalid image dimensions: ${boundsOptions.outWidth}x${boundsOptions.outHeight}")
                    return null
                }

                Log.d(TAG, "⚡ Original size: ${boundsOptions.outWidth}x${boundsOptions.outHeight}")

                // Calculate scale to fit 512x512
                val targetSize = MAX_ART_SIZE
                var scale = 1
                while (boundsOptions.outWidth / scale / 2 >= targetSize &&
                       boundsOptions.outHeight / scale / 2 >= targetSize) {
                    scale *= 2
                }

                Log.d(TAG, "⚡ Using sample size: $scale")

                // Second pass: decode with scaling
                // Need to re-open file descriptor for second decode
                val pfd2 = contentResolver.openFileDescriptor(uri, "r")
                pfd2?.use { desc2 ->
                    val loadOptions = BitmapFactory.Options().apply {
                        inSampleSize = scale
                        inPreferredConfig = Bitmap.Config.RGB_565  // Smaller memory footprint
                    }
                    val bitmap = BitmapFactory.decodeFileDescriptor(desc2.fileDescriptor, null, loadOptions)

                    if (bitmap != null) {
                        Log.d(TAG, "⚡ Loaded bitmap: ${bitmap.width}x${bitmap.height}")
                    }
                    bitmap
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚡ resolveCoverArtFromUri failed: ${e.message}", e)
            null
        }
    }

    // ========================================================================
    // PLAYBACK STATE
    // ========================================================================

    private fun updatePlaybackState(isPlaying: Boolean) {
        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        val position = audioHandler.currentPosition.value
        mediaSession?.setPlaybackState(buildPlaybackState(state, position))
    }

    private fun buildPlaybackState(state: Int, position: Long): PlaybackStateCompat {
        val speed = if (state == PlaybackStateCompat.STATE_PLAYING) 1.0f else 0f

        return PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_SET_PLAYBACK_SPEED
            )
            .setState(state, position, speed)
            .apply {
                // Add current media ID for equalizer bars
                currentPlayingMediaId?.let { mediaId ->
                    setActiveQueueItemId(mediaId.hashCode().toLong())
                }
            }
            .build()
    }

    // ========================================================================
    // AUDIO FOCUS
    // ========================================================================

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(audioFocusListener, handler)
            .build()

        val result = audioManager.requestAudioFocus(audioFocusRequest!!)
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

        Log.d(TAG, "Audio focus request: ${if (hasAudioFocus) "granted" else "denied"}")
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { request ->
            audioManager.abandonAudioFocusRequest(request)
        }
        hasAudioFocus = false
    }

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus gained")
                hasAudioFocus = true
                if (wasPlayingBeforeFocusLoss) {
                    audioHandler.play()
                    wasPlayingBeforeFocusLoss = false
                }
                audioHandler.setVolume(1.0f)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Audio focus lost permanently")
                hasAudioFocus = false
                wasPlayingBeforeFocusLoss = audioHandler.isPlaying.value
                audioHandler.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus lost transiently")
                wasPlayingBeforeFocusLoss = audioHandler.isPlaying.value
                audioHandler.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus: ducking")
                audioHandler.setVolume(0.3f)
            }
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private fun createBrowsableItem(
        mediaId: String,
        title: String,
        subtitle: String,
        iconResId: Int? = null
    ): MediaBrowserCompat.MediaItem {
        val builder = MediaDescriptionCompat.Builder()
            .setMediaId(mediaId)
            .setTitle(title)
            .setSubtitle(subtitle)

        iconResId?.let { resId ->
            builder.setIconUri(getResourceUri(resId))
        }

        return MediaBrowserCompat.MediaItem(
            builder.build(),
            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        )
    }

    private fun createPlayableItem(
        mediaId: String,
        title: String,
        subtitle: String,
        iconUri: Uri?,
        durationMs: Long
    ): MediaBrowserCompat.MediaItem {
        // Add duration to extras for animated equalizer bars
        val extras = Bundle().apply {
            putLong(EXTRA_DURATION, durationMs)
        }

        val description = MediaDescriptionCompat.Builder()
            .setMediaId(mediaId)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setIconUri(iconUri)
            .setExtras(extras)
            .build()

        return MediaBrowserCompat.MediaItem(
            description,
            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        )
    }

    private fun createEmptyItem(title: String, subtitle: String): MediaBrowserCompat.MediaItem {
        return MediaBrowserCompat.MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId("empty_${System.currentTimeMillis()}")
                .setTitle(title)
                .setSubtitle(subtitle)
                .build(),
            0 // Not playable or browsable
        )
    }

    private fun getResourceUri(resId: Int): Uri {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(packageName)
            .path(resId.toString())
            .build()
    }

    private fun getContentUri(filePath: String?): Uri? {
        if (filePath.isNullOrEmpty()) return null
        if (filePath.startsWith("content://")) return Uri.parse(filePath)
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) return null

        return try {
            val file = File(filePath)
            if (file.exists()) {
                FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get content URI: $filePath", e)
            null
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
}
