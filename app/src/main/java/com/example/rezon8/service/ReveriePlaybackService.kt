package com.mossglen.reverie.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.mossglen.reverie.MainActivity
import com.mossglen.reverie.R
import com.mossglen.reverie.data.AudioHandler
import com.mossglen.reverie.data.LibraryRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MediaLibraryService for Android Auto, lock screen, and notification support.
 * Uses the shared ExoPlayer singleton from Hilt DI.
 */
@AndroidEntryPoint
class ReveriePlaybackService : MediaLibraryService() {

    companion object {
        private const val TAG = "ReveriePlaybackService"
        private const val NOTIFICATION_CHANNEL_ID = "reverie_playback_channel"
        private const val NOTIFICATION_ID = 1001

        // Custom session commands for skip
        const val ACTION_SKIP_FORWARD = "com.mossglen.reverie.SKIP_FORWARD"
        const val ACTION_SKIP_BACK = "com.mossglen.reverie.SKIP_BACK"

        // Android Auto content style constants
        private const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        private const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        private const val CONTENT_STYLE_LIST_ITEM_HINT_VALUE = 1
        private const val CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2
    }

    @Inject lateinit var player: ExoPlayer
    @Inject lateinit var repository: LibraryRepository
    @Inject lateinit var audioHandler: AudioHandler

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Format duration in milliseconds to human-readable format (e.g., "1:23:45" or "23:45")
     */
    private fun formatDurationMs(ms: Long): String {
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

    /**
     * Convert a file path to a content:// URI using FileProvider.
     * Android Auto requires content:// URIs for artwork.
     */
    private fun getContentUriForCover(filePath: String?): Uri? {
        if (filePath.isNullOrEmpty()) {
            Log.d(TAG, "Cover path is null or empty")
            return null
        }

        // If it's already a content:// or http:// URI, return as-is or null
        if (filePath.startsWith("content://")) {
            Log.d(TAG, "Cover is already a content URI: $filePath")
            return Uri.parse(filePath)
        }
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            // HTTP URLs need to be downloaded first - for now return null
            Log.d(TAG, "Cover is a URL (not local file): $filePath")
            return null
        }

        return try {
            val file = File(filePath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
                Log.d(TAG, "Created content URI for cover: $uri (from $filePath)")
                uri
            } else {
                Log.w(TAG, "Cover file does not exist: $filePath")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get content URI for cover: $filePath", e)
            null
        }
    }

    // Custom commands for skip forward/back
    private val skipForwardCommand = SessionCommand(ACTION_SKIP_FORWARD, Bundle.EMPTY)
    private val skipBackCommand = SessionCommand(ACTION_SKIP_BACK, Bundle.EMPTY)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ReveriePlaybackService onCreate")

        // Create notification channel for Android O+
        createNotificationChannel()

        // Define the intent to open UI when user taps "Now Playing"
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE)

        val callback = object : MediaLibrarySession.Callback {
            // 1. The Root Folder (What the car sees first)
            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                Log.d(TAG, "onGetLibraryRoot called by: ${browser.packageName}")

                // Log all incoming root hints for diagnostics
                params?.extras?.let { extras ->
                    Log.d(TAG, "Root hints received:")
                    for (key in extras.keySet()) {
                        Log.d(TAG, "  $key = ${extras.get(key)}")
                    }
                }

                // Check for Android Auto specific hints
                val isRecent = params?.isRecent ?: false
                val isOffline = params?.isOffline ?: false
                val isSuggested = params?.extras?.getBoolean("android.media.extra.SUGGESTED", false) ?: false

                Log.d(TAG, "Root flags - Recent: $isRecent, Offline: $isOffline, Suggested: $isSuggested")

                // Create Android Auto compatible extras with content style hints
                val extras = Bundle().apply {
                    putBoolean(CONTENT_STYLE_SUPPORTED, true)
                    putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID_ITEM_HINT_VALUE)
                    putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST_ITEM_HINT_VALUE)
                }

                // Create root item with proper metadata
                val rootItem = MediaItem.Builder()
                    .setMediaId("root")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("REVERIE")
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .setExtras(extras)
                            .build()
                    )
                    .build()

                // Create LibraryParams with the content style extras
                val libraryParams = LibraryParams.Builder()
                    .setExtras(extras)
                    .build()

                Log.d(TAG, "Returning root with Android Auto content style hints")
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, libraryParams))
            }

            // 2. The Content (What shows up inside the folder)
            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                Log.d(TAG, "onGetChildren called for parentId: $parentId by ${browser.packageName}")
                return serviceScope.future {
                    when (parentId) {
                        "root" -> {
                            // Return browsable folders for Android Auto launcher customization
                            val folders = mutableListOf(
                                MediaItem.Builder()
                                    .setMediaId("continue_listening")
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle("Continue Listening")
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                            .build()
                                    )
                                    .build(),
                                MediaItem.Builder()
                                    .setMediaId("chapters")
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle("Chapters")
                                            .setSubtitle("Jump to a specific chapter")
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                            .build()
                                    )
                                    .build(),
                                MediaItem.Builder()
                                    .setMediaId("recent_books")
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle("Recent Books")
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                            .build()
                                    )
                                    .build(),
                                MediaItem.Builder()
                                    .setMediaId("library")
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle("Library")
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                            .build()
                                    )
                                    .build()
                            )
                            Log.d(TAG, "Returning ${folders.size} browsable folders for root")
                            LibraryResult.ofItemList(ImmutableList.copyOf(folders), params)
                        }
                        "continue_listening" -> {
                            // Return the most recently played AUDIOBOOK (not ebooks)
                            val recentBook = repository.getMostRecentAudiobookDirect()
                            if (recentBook != null) {
                                // Calculate progress info for rich display
                                val remainingMs = recentBook.duration - recentBook.progress
                                val remainingHours = remainingMs / 3600000
                                val remainingMins = (remainingMs % 3600000) / 60000
                                val progressPercent = if (recentBook.duration > 0) {
                                    ((recentBook.progress.toFloat() / recentBook.duration) * 100).toInt()
                                } else 0
                                val subtitle = if (remainingHours > 0) {
                                    "${remainingHours}h ${remainingMins}m remaining • ${progressPercent}%"
                                } else {
                                    "${remainingMins}m remaining • ${progressPercent}%"
                                }

                                val mediaItem = MediaItem.Builder()
                                    .setMediaId(recentBook.id)
                                    .setUri(Uri.parse(recentBook.filePath))
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(recentBook.title)
                                            .setArtist(recentBook.author)
                                            .setSubtitle(subtitle)
                                            .setAlbumTitle(recentBook.seriesInfo.ifEmpty { recentBook.title })
                                            .setArtworkUri(getContentUriForCover(recentBook.coverUrl))
                                            .setIsBrowsable(false)
                                            .setIsPlayable(true)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                            .setExtras(Bundle().apply {
                                                putLong("duration", recentBook.duration)
                                                putLong("progress", recentBook.progress)
                                                putInt("progressPercent", progressPercent)
                                            })
                                            .build()
                                    )
                                    .build()
                                Log.d(TAG, "Returning 1 recent book: ${recentBook.title} (${progressPercent}%)")
                                LibraryResult.ofItemList(ImmutableList.of(mediaItem), params)
                            } else {
                                Log.d(TAG, "No recent books found")
                                // Return placeholder instead of empty list
                                val placeholder = MediaItem.Builder()
                                    .setMediaId("no_books")
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle("No books in library")
                                            .setSubtitle("Add audiobooks to get started")
                                            .setIsBrowsable(false)
                                            .setIsPlayable(false)
                                            .build()
                                    )
                                    .build()
                                LibraryResult.ofItemList(ImmutableList.of(placeholder), params)
                            }
                        }
                        "recent_books" -> {
                            // Return all books sorted by last played
                            val books = repository.getBooksForAuto()
                            Log.d(TAG, "Returning ${books.size} recent books")
                            LibraryResult.ofItemList(ImmutableList.copyOf(books), params)
                        }
                        "library" -> {
                            // Return all books in library
                            val books = repository.getBooksForAuto()
                            Log.d(TAG, "Returning ${books.size} library books")
                            LibraryResult.ofItemList(ImmutableList.copyOf(books), params)
                        }
                        "chapters" -> {
                            // Return chapters for the currently playing AUDIOBOOK
                            val currentBook = repository.getMostRecentAudiobookDirect()
                            if (currentBook != null && currentBook.chapters.isNotEmpty()) {
                                val chapterItems = currentBook.chapters.mapIndexed { index, chapter ->
                                    MediaItem.Builder()
                                        .setMediaId("chapter_${currentBook.id}_${index}")
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(chapter.title.ifEmpty { "Chapter ${index + 1}" })
                                                .setSubtitle(formatDurationMs(chapter.startMs))
                                                .setArtworkUri(getContentUriForCover(currentBook.coverUrl))
                                                .setIsBrowsable(false)
                                                .setIsPlayable(true)
                                                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                                .setExtras(Bundle().apply {
                                                    putString("bookId", currentBook.id)
                                                    putLong("startMs", chapter.startMs)
                                                })
                                                .build()
                                        )
                                        .build()
                                }
                                Log.d(TAG, "Returning ${chapterItems.size} chapters for ${currentBook.title}")
                                LibraryResult.ofItemList(ImmutableList.copyOf(chapterItems), params)
                            } else {
                                Log.d(TAG, "No chapters found for current book")
                                // Return a single "No chapters" placeholder
                                val noChaptersItem = MediaItem.Builder()
                                    .setMediaId("no_chapters")
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle("No chapters available")
                                            .setSubtitle("This book has no chapter markers")
                                            .setIsBrowsable(false)
                                            .setIsPlayable(false)
                                            .build()
                                    )
                                    .build()
                                LibraryResult.ofItemList(ImmutableList.of(noChaptersItem), params)
                            }
                        }
                        else -> {
                            // Check if it's a chapter selection
                            if (parentId.startsWith("chapter_")) {
                                Log.d(TAG, "Chapter selection: $parentId")
                                LibraryResult.ofItemList(ImmutableList.of(), params)
                            } else {
                                Log.w(TAG, "Unknown parentId: $parentId")
                                LibraryResult.ofItemList(ImmutableList.of(), params)
                            }
                        }
                    }
                }
            }

            // 3. Handle connection requests - add custom skip commands
            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                Log.d(TAG, "Controller connected: ${controller.packageName}")

                // Add custom commands for skip forward/back
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(skipForwardCommand)
                    .add(skipBackCommand)
                    .build()

                return MediaSession.ConnectionResult.accept(sessionCommands, MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
            }

            // Handle custom session commands (skip forward/back)
            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                Log.d(TAG, "Custom command received: ${customCommand.customAction}")
                return when (customCommand.customAction) {
                    ACTION_SKIP_FORWARD -> {
                        audioHandler.skipForward(30)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_SKIP_BACK -> {
                        audioHandler.skipBack(10)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    else -> {
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
                    }
                }
            }

            // 4. Handle media item selection from Android Auto
            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                return serviceScope.future {
                    val resolvedItems = mediaItems.mapNotNull { item ->
                        val mediaId = item.mediaId
                        Log.d("ReveriePlaybackService", "Playing media item: $mediaId")

                        // Check if this is a chapter selection
                        if (mediaId.startsWith("chapter_")) {
                            // Parse chapter_${bookId}_${chapterIndex}
                            val parts = mediaId.removePrefix("chapter_").split("_")
                            if (parts.size >= 2) {
                                val bookId = parts.dropLast(1).joinToString("_") // Handle book IDs with underscores
                                val chapterIndex = parts.last().toIntOrNull() ?: 0
                                val book = repository.getBookById(bookId)
                                if (book != null && chapterIndex < book.chapters.size) {
                                    val chapter = book.chapters[chapterIndex]
                                    Log.d(TAG, "Chapter selected: ${chapter.title} at ${chapter.startMs}ms")

                                    // Load the book, seek to chapter position, and START PLAYBACK
                                    serviceScope.launch(Dispatchers.Main) {
                                        audioHandler.loadBookAndSeek(book, chapter.startMs)
                                        audioHandler.play()
                                    }

                                    // Return a playable media item
                                    MediaItem.Builder()
                                        .setMediaId(book.id)
                                        .setUri(Uri.parse(book.filePath))
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(chapter.title.ifEmpty { "Chapter ${chapterIndex + 1}" })
                                                .setArtist(book.author)
                                                .setAlbumTitle(book.title)
                                                .setArtworkUri(getContentUriForCover(book.coverUrl))
                                                .setIsPlayable(true)
                                                .setIsBrowsable(false)
                                                .build()
                                        )
                                        .build()
                                } else {
                                    Log.w(TAG, "Book or chapter not found: $bookId, index $chapterIndex")
                                    null
                                }
                            } else null
                        } else {
                            // Regular book selection
                            val book = repository.getBookById(mediaId)
                            if (book != null) {
                                Log.d(TAG, "Loading book for Android Auto: ${book.title}")
                                // Use AudioHandler to properly load the book and START PLAYBACK
                                serviceScope.launch(Dispatchers.Main) {
                                    audioHandler.loadBookAndSeek(book)
                                    // Start playback after loading
                                    audioHandler.play()
                                }

                                // Return a playable media item
                                val coverUri = getContentUriForCover(book.coverUrl)
                                Log.d(TAG, "Cover URI for ${book.title}: $coverUri (path: ${book.coverUrl})")

                                MediaItem.Builder()
                                    .setMediaId(book.id)
                                    .setUri(Uri.parse(book.filePath))
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(book.title)
                                            .setArtist(book.author)
                                            .setArtworkUri(coverUri)
                                            .setIsPlayable(true)
                                            .setIsBrowsable(false)
                                            .build()
                                    )
                                    .build()
                            } else {
                                Log.w(TAG, "Book not found for mediaId: $mediaId")
                                null
                            }
                        }
                    }.toMutableList()

                    resolvedItems
                }
            }

            // 5. Handle get item requests
            override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String
            ): ListenableFuture<LibraryResult<MediaItem>> {
                return serviceScope.future {
                    val book = repository.getBookById(mediaId)
                    if (book != null) {
                        val mediaItem = MediaItem.Builder()
                            .setMediaId(book.id)
                            .setUri(Uri.parse(book.filePath))
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(book.title)
                                    .setArtist(book.author)
                                    .setArtworkUri(getContentUriForCover(book.coverUrl))
                                    .setIsPlayable(true)
                                    .setIsBrowsable(false)
                                    .build()
                            )
                            .build()
                        LibraryResult.ofItem(mediaItem, null)
                    } else {
                        LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                    }
                }
            }
        }

        // Create custom layout with skip buttons for notification
        val customLayout = listOf(
            CommandButton.Builder()
                .setDisplayName("Rewind 10s")
                .setIconResId(R.drawable.ic_skip_back)
                .setSessionCommand(skipBackCommand)
                .build(),
            CommandButton.Builder()
                .setDisplayName("Forward 30s")
                .setIconResId(R.drawable.ic_skip_forward)
                .setSessionCommand(skipForwardCommand)
                .build()
        )

        // Create MediaLibrarySession using the shared ExoPlayer
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, callback)
            .setSessionActivity(pendingIntent)
            .setCustomLayout(customLayout)
            .build()

        // CRITICAL: Activate session BEFORE Android Auto tries to connect
        // This ensures the session is discoverable when Auto scans for media apps
        player.playWhenReady = false  // Don't auto-play

        Log.d(TAG, "MediaLibrarySession created with custom skip buttons and activated")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaLibrarySession

    override fun onDestroy() {
        Log.d(TAG, "ReveriePlaybackService onDestroy")
        mediaLibrarySession?.run {
            release()
            mediaLibrarySession = null
        }
        // Don't release the player here - it's managed by AudioHandler/Hilt
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Reverie Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
}
