package com.mossglen.lithos.service

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
import com.mossglen.lithos.MainActivity
import com.mossglen.lithos.R
import com.mossglen.lithos.data.AudioHandler
import com.mossglen.lithos.data.LibraryRepository
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
 * MediaLibraryService for Android Auto with premium audiobook features.
 *
 * Features:
 * - Continue Listening (most recent audiobook)
 * - Chapters (chapter navigation)
 * - Recent Books (recently played)
 * - Library (all audiobooks)
 * - By Author (browse by author)
 * - By Series (browse by series)
 * - Speed Control (1.0x, 1.25x, 1.5x, 2.0x)
 * - Sleep Timer (15min, 30min, 60min, cancel)
 * - Custom skip buttons (10s back, 30s forward)
 */
@AndroidEntryPoint
class LithosPlaybackService : MediaLibraryService() {

    companion object {
        private const val TAG = "LithosPlaybackService"
        private const val NOTIFICATION_CHANNEL_ID = "lithos_playback_channel"
        private const val NOTIFICATION_ID = 1001

        // Custom session commands
        const val ACTION_SKIP_FORWARD = "com.mossglen.lithos.SKIP_FORWARD"
        const val ACTION_SKIP_BACK = "com.mossglen.lithos.SKIP_BACK"
        const val ACTION_SPEED_1X = "com.mossglen.lithos.SPEED_1X"
        const val ACTION_SPEED_125X = "com.mossglen.lithos.SPEED_125X"
        const val ACTION_SPEED_15X = "com.mossglen.lithos.SPEED_15X"
        const val ACTION_SPEED_2X = "com.mossglen.lithos.SPEED_2X"
        const val ACTION_SLEEP_15 = "com.mossglen.lithos.SLEEP_15"
        const val ACTION_SLEEP_30 = "com.mossglen.lithos.SLEEP_30"
        const val ACTION_SLEEP_60 = "com.mossglen.lithos.SLEEP_60"
        const val ACTION_SLEEP_CANCEL = "com.mossglen.lithos.SLEEP_CANCEL"
        const val ACTION_NEXT_CHAPTER = "com.mossglen.lithos.NEXT_CHAPTER"
        const val ACTION_PREV_CHAPTER = "com.mossglen.lithos.PREV_CHAPTER"

        // Media ID prefixes
        private const val PREFIX_AUTHOR = "author_"
        private const val PREFIX_SERIES = "series_"
        private const val PREFIX_CHAPTER = "chapter_"

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

    // Custom commands
    private val skipForwardCommand = SessionCommand(ACTION_SKIP_FORWARD, Bundle.EMPTY)
    private val skipBackCommand = SessionCommand(ACTION_SKIP_BACK, Bundle.EMPTY)
    private val speed1xCommand = SessionCommand(ACTION_SPEED_1X, Bundle.EMPTY)
    private val speed125xCommand = SessionCommand(ACTION_SPEED_125X, Bundle.EMPTY)
    private val speed15xCommand = SessionCommand(ACTION_SPEED_15X, Bundle.EMPTY)
    private val speed2xCommand = SessionCommand(ACTION_SPEED_2X, Bundle.EMPTY)
    private val sleep15Command = SessionCommand(ACTION_SLEEP_15, Bundle.EMPTY)
    private val sleep30Command = SessionCommand(ACTION_SLEEP_30, Bundle.EMPTY)
    private val sleep60Command = SessionCommand(ACTION_SLEEP_60, Bundle.EMPTY)
    private val sleepCancelCommand = SessionCommand(ACTION_SLEEP_CANCEL, Bundle.EMPTY)
    private val nextChapterCommand = SessionCommand(ACTION_NEXT_CHAPTER, Bundle.EMPTY)
    private val prevChapterCommand = SessionCommand(ACTION_PREV_CHAPTER, Bundle.EMPTY)

    // Sleep timer state
    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    private var sleepTimerEndTime: Long? = null

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
     */
    private fun getContentUriForCover(filePath: String?): Uri? {
        if (filePath.isNullOrEmpty()) return null
        if (filePath.startsWith("content://")) return Uri.parse(filePath)
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) return null

        return try {
            val file = File(filePath)
            if (file.exists()) {
                FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get content URI for cover: $filePath", e)
            null
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LithosPlaybackService onCreate")
        createNotificationChannel()

        // Add listener to handle seeking to stored positions after media loads
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null && reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    val seekPosition = mediaItem.mediaMetadata.extras?.getLong("seekPosition", 0L) ?: 0L
                    if (seekPosition > 0) {
                        Log.d(TAG, "Seeking to stored position: ${seekPosition}ms")
                        player.seekTo(seekPosition)
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    // Check if we need to seek after the player is ready
                    val currentItem = player.currentMediaItem
                    val seekPosition = currentItem?.mediaMetadata?.extras?.getLong("seekPosition", 0L) ?: 0L
                    if (seekPosition > 0 && player.currentPosition < 1000) {
                        Log.d(TAG, "Player ready, seeking to: ${seekPosition}ms")
                        player.seekTo(seekPosition)
                    }
                }
            }
        })

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE)

        val callback = object : MediaLibrarySession.Callback {

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                Log.d(TAG, "onGetLibraryRoot called by: ${browser.packageName}")

                val extras = Bundle().apply {
                    putBoolean(CONTENT_STYLE_SUPPORTED, true)
                    putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID_ITEM_HINT_VALUE)
                    putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST_ITEM_HINT_VALUE)
                }

                val rootItem = MediaItem.Builder()
                    .setMediaId("root")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("LITHOS")
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .setExtras(extras)
                            .build()
                    )
                    .build()

                val libraryParams = LibraryParams.Builder().setExtras(extras).build()
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, libraryParams))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                Log.d(TAG, "onGetChildren for: $parentId")
                return serviceScope.future {
                    when {
                        parentId == "root" -> getRootItems()
                        parentId == "continue_listening" -> getContinueListeningItems()
                        parentId == "chapters" -> getChapterItems()
                        parentId == "recent_books" -> getRecentBooksItems()
                        parentId == "library" -> getLibraryItems()
                        parentId == "by_author" -> getAuthorListItems()
                        parentId == "by_series" -> getSeriesListItems()
                        parentId == "speed_control" -> getSpeedControlItems()
                        parentId == "sleep_timer" -> getSleepTimerItems()
                        parentId.startsWith(PREFIX_AUTHOR) -> getBooksByAuthor(parentId.removePrefix(PREFIX_AUTHOR))
                        parentId.startsWith(PREFIX_SERIES) -> getBooksBySeries(parentId.removePrefix(PREFIX_SERIES))
                        parentId.startsWith(PREFIX_CHAPTER) -> LibraryResult.ofItemList(ImmutableList.of(), params)
                        else -> {
                            Log.w(TAG, "Unknown parentId: $parentId")
                            LibraryResult.ofItemList(ImmutableList.of(), params)
                        }
                    }
                }
            }

            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                Log.d(TAG, "Controller connected: ${controller.packageName}")

                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(skipForwardCommand)
                    .add(skipBackCommand)
                    .add(speed1xCommand)
                    .add(speed125xCommand)
                    .add(speed15xCommand)
                    .add(speed2xCommand)
                    .add(sleep15Command)
                    .add(sleep30Command)
                    .add(sleep60Command)
                    .add(sleepCancelCommand)
                    .add(nextChapterCommand)
                    .add(prevChapterCommand)
                    .build()

                return MediaSession.ConnectionResult.accept(sessionCommands, MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                Log.d(TAG, "Custom command: ${customCommand.customAction}")
                return when (customCommand.customAction) {
                    ACTION_SKIP_FORWARD -> {
                        audioHandler.skipForward(30)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_SKIP_BACK -> {
                        audioHandler.skipBack(10)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_SPEED_1X -> {
                        audioHandler.setPlaybackSpeed(1.0f)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_SPEED_125X -> {
                        audioHandler.setPlaybackSpeed(1.25f)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_SPEED_15X -> {
                        audioHandler.setPlaybackSpeed(1.5f)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_SPEED_2X -> {
                        audioHandler.setPlaybackSpeed(2.0f)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_SLEEP_15 -> {
                        startSleepTimer(15)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_SLEEP_30 -> {
                        startSleepTimer(30)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_SLEEP_60 -> {
                        startSleepTimer(60)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_SLEEP_CANCEL -> {
                        cancelSleepTimer()
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_NEXT_CHAPTER -> {
                        serviceScope.launch { skipToNextChapter() }
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_PREV_CHAPTER -> {
                        serviceScope.launch { skipToPreviousChapter() }
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    else -> Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
                }
            }

            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                return serviceScope.future {
                    mediaItems.mapNotNull { item ->
                        resolveMediaItem(item.mediaId)
                    }.toMutableList()
                }
            }

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

        // Custom layout with skip buttons
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

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, callback)
            .setSessionActivity(pendingIntent)
            .setCustomLayout(customLayout)
            .build()

        player.playWhenReady = false
        Log.d(TAG, "MediaLibrarySession created with premium features")
    }

    // ===== Content Building Methods =====

    private suspend fun getRootItems(): LibraryResult<ImmutableList<MediaItem>> {
        val folders = mutableListOf(
            createBrowsableItem("continue_listening", "Continue Listening", "Pick up where you left off"),
            createBrowsableItem("chapters", "Chapters", "Jump to a chapter"),
            createBrowsableItem("recent_books", "Recent Books", "Recently played"),
            createBrowsableItem("library", "Library", "All your audiobooks"),
            createBrowsableItem("by_author", "By Author", "Browse by author"),
            createBrowsableItem("by_series", "By Series", "Browse by series"),
            createBrowsableItem("speed_control", "Speed", "Adjust playback speed"),
            createBrowsableItem("sleep_timer", "Sleep Timer", "Set a sleep timer")
        )
        Log.d(TAG, "Returning ${folders.size} root items")
        return LibraryResult.ofItemList(ImmutableList.copyOf(folders), null)
    }

    private suspend fun getContinueListeningItems(): LibraryResult<ImmutableList<MediaItem>> {
        val recentBook = repository.getMostRecentAudiobookDirect()
        return if (recentBook != null) {
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
                        .build()
                )
                .build()
            LibraryResult.ofItemList(ImmutableList.of(mediaItem), null)
        } else {
            val placeholder = createPlaceholderItem("no_books", "No audiobooks", "Add audiobooks to get started")
            LibraryResult.ofItemList(ImmutableList.of(placeholder), null)
        }
    }

    private suspend fun getChapterItems(): LibraryResult<ImmutableList<MediaItem>> {
        val currentBook = repository.getMostRecentAudiobookDirect()
        return if (currentBook != null && currentBook.chapters.isNotEmpty()) {
            val chapterItems = currentBook.chapters.mapIndexed { index, chapter ->
                MediaItem.Builder()
                    .setMediaId("${PREFIX_CHAPTER}${currentBook.id}_${index}")
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
            LibraryResult.ofItemList(ImmutableList.copyOf(chapterItems), null)
        } else {
            val placeholder = createPlaceholderItem("no_chapters", "No chapters", "This book has no chapter markers")
            LibraryResult.ofItemList(ImmutableList.of(placeholder), null)
        }
    }

    private suspend fun getRecentBooksItems(): LibraryResult<ImmutableList<MediaItem>> {
        val books = repository.getBooksForAuto()
        Log.d(TAG, "Returning ${books.size} recent books")
        return LibraryResult.ofItemList(ImmutableList.copyOf(books), null)
    }

    private suspend fun getLibraryItems(): LibraryResult<ImmutableList<MediaItem>> {
        val books = repository.getBooksForAuto()
        Log.d(TAG, "Returning ${books.size} library books")
        return LibraryResult.ofItemList(ImmutableList.copyOf(books), null)
    }

    private suspend fun getAuthorListItems(): LibraryResult<ImmutableList<MediaItem>> {
        val authors = repository.getUniqueAuthorsForAuto()
        val items = authors.map { author ->
            createBrowsableItem("${PREFIX_AUTHOR}$author", author, "Tap to see books")
        }
        Log.d(TAG, "Returning ${items.size} authors")
        return LibraryResult.ofItemList(ImmutableList.copyOf(items), null)
    }

    private suspend fun getSeriesListItems(): LibraryResult<ImmutableList<MediaItem>> {
        val series = repository.getUniqueSeriesForAuto()
        return if (series.isNotEmpty()) {
            val items = series.map { seriesName ->
                createBrowsableItem("${PREFIX_SERIES}$seriesName", seriesName, "Tap to see books in series")
            }
            Log.d(TAG, "Returning ${items.size} series")
            LibraryResult.ofItemList(ImmutableList.copyOf(items), null)
        } else {
            val placeholder = createPlaceholderItem("no_series", "No series found", "Your books don't have series info")
            LibraryResult.ofItemList(ImmutableList.of(placeholder), null)
        }
    }

    private suspend fun getBooksByAuthor(author: String): LibraryResult<ImmutableList<MediaItem>> {
        val books = repository.getAudiobooksByAuthorForAuto(author)
        Log.d(TAG, "Returning ${books.size} books by $author")
        return LibraryResult.ofItemList(ImmutableList.copyOf(books), null)
    }

    private suspend fun getBooksBySeries(series: String): LibraryResult<ImmutableList<MediaItem>> {
        val books = repository.getAudiobooksBySeriesForAuto(series)
        Log.d(TAG, "Returning ${books.size} books in series $series")
        return LibraryResult.ofItemList(ImmutableList.copyOf(books), null)
    }

    private fun getSpeedControlItems(): LibraryResult<ImmutableList<MediaItem>> {
        val currentSpeed = player.playbackParameters.speed
        val items = listOf(
            createActionItem("speed_1x", "1.0x Normal", if (currentSpeed == 1.0f) "Currently selected" else "Tap to select", speed1xCommand),
            createActionItem("speed_125x", "1.25x", if (currentSpeed == 1.25f) "Currently selected" else "Tap to select", speed125xCommand),
            createActionItem("speed_15x", "1.5x", if (currentSpeed == 1.5f) "Currently selected" else "Tap to select", speed15xCommand),
            createActionItem("speed_2x", "2.0x Fast", if (currentSpeed == 2.0f) "Currently selected" else "Tap to select", speed2xCommand)
        )
        return LibraryResult.ofItemList(ImmutableList.copyOf(items), null)
    }

    private fun getSleepTimerItems(): LibraryResult<ImmutableList<MediaItem>> {
        val timerActive = sleepTimerEndTime != null
        val items = mutableListOf(
            createActionItem("sleep_15", "15 minutes", "Stop playback after 15 min", sleep15Command),
            createActionItem("sleep_30", "30 minutes", "Stop playback after 30 min", sleep30Command),
            createActionItem("sleep_60", "60 minutes", "Stop playback after 60 min", sleep60Command)
        )
        if (timerActive) {
            val remaining = ((sleepTimerEndTime ?: 0) - System.currentTimeMillis()) / 60000
            items.add(0, createActionItem("sleep_cancel", "Cancel Timer", "${remaining}m remaining - tap to cancel", sleepCancelCommand))
        }
        return LibraryResult.ofItemList(ImmutableList.copyOf(items), null)
    }

    // ===== Helper Methods =====

    private fun createBrowsableItem(mediaId: String, title: String, subtitle: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()
            )
            .build()
    }

    private fun createPlaceholderItem(mediaId: String, title: String, subtitle: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(false)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    private fun createActionItem(mediaId: String, title: String, subtitle: String, command: SessionCommand): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(false)
                    .setIsPlayable(true) // Playable to trigger selection
                    .setExtras(Bundle().apply {
                        putString("customCommand", command.customAction)
                    })
                    .build()
            )
            .build()
    }

    private suspend fun resolveMediaItem(mediaId: String): MediaItem? {
        Log.d(TAG, "Resolving media item: $mediaId")

        // Handle speed control actions
        when (mediaId) {
            "speed_1x" -> { audioHandler.setPlaybackSpeed(1.0f); return null }
            "speed_125x" -> { audioHandler.setPlaybackSpeed(1.25f); return null }
            "speed_15x" -> { audioHandler.setPlaybackSpeed(1.5f); return null }
            "speed_2x" -> { audioHandler.setPlaybackSpeed(2.0f); return null }
            "sleep_15" -> { startSleepTimer(15); return null }
            "sleep_30" -> { startSleepTimer(30); return null }
            "sleep_60" -> { startSleepTimer(60); return null }
            "sleep_cancel" -> { cancelSleepTimer(); return null }
        }

        // Handle chapter selection
        if (mediaId.startsWith(PREFIX_CHAPTER)) {
            val parts = mediaId.removePrefix(PREFIX_CHAPTER).split("_")
            if (parts.size >= 2) {
                val bookId = parts.dropLast(1).joinToString("_")
                val chapterIndex = parts.last().toIntOrNull() ?: 0
                val book = repository.getBookById(bookId)
                if (book != null && chapterIndex < book.chapters.size) {
                    val chapter = book.chapters[chapterIndex]
                    Log.d(TAG, "Preparing chapter: ${chapter.title} at ${chapter.startMs}ms")
                    // Return MediaItem with request metadata for seeking
                    // MediaLibrarySession will use this to set up playback
                    return MediaItem.Builder()
                        .setMediaId("${book.id}#${chapter.startMs}")
                        .setUri(Uri.parse(book.filePath))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(chapter.title.ifEmpty { "Chapter ${chapterIndex + 1}" })
                                .setArtist(book.author)
                                .setAlbumTitle(book.title)
                                .setArtworkUri(getContentUriForCover(book.coverUrl))
                                .setIsPlayable(true)
                                .setIsBrowsable(false)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                .setExtras(Bundle().apply {
                                    putLong("seekPosition", chapter.startMs)
                                    putString("bookId", book.id)
                                })
                                .build()
                        )
                        .build()
                }
            }
            return null
        }

        // Regular book selection - return MediaItem for MediaLibrarySession to play
        val book = repository.getBookById(mediaId)
        return if (book != null) {
            Log.d(TAG, "Preparing book: ${book.title}")
            // Create MediaItem with proper URI and metadata for the shared ExoPlayer
            // The player will load and play this directly
            MediaItem.Builder()
                .setMediaId(book.id)
                .setUri(Uri.parse(book.filePath))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(book.title)
                        .setArtist(book.author)
                        .setAlbumTitle(book.seriesInfo.ifEmpty { book.title })
                        .setArtworkUri(getContentUriForCover(book.coverUrl))
                        .setIsPlayable(true)
                        .setIsBrowsable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .setExtras(Bundle().apply {
                            putLong("seekPosition", book.progress)
                            putString("bookId", book.id)
                        })
                        .build()
                )
                .build()
        } else {
            Log.w(TAG, "Book not found: $mediaId")
            null
        }
    }

    // ===== Sleep Timer =====

    private fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerEndTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        Log.d(TAG, "Sleep timer set for $minutes minutes")

        sleepTimerJob = serviceScope.launch {
            kotlinx.coroutines.delay(minutes * 60 * 1000L)
            Log.d(TAG, "Sleep timer expired, pausing playback")
            audioHandler.pause()
            sleepTimerEndTime = null
        }
    }

    private fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepTimerEndTime = null
        Log.d(TAG, "Sleep timer cancelled")
    }

    // ===== Chapter Navigation =====

    private suspend fun skipToNextChapter() {
        val book = repository.getMostRecentAudiobookDirect() ?: return
        val currentPosition = audioHandler.getCurrentPosition()
        val nextChapter = book.chapters.firstOrNull { it.startMs > currentPosition + 1000 }
        if (nextChapter != null) {
            Log.d(TAG, "Skipping to next chapter: ${nextChapter.title}")
            audioHandler.seekTo(nextChapter.startMs)
        }
    }

    private suspend fun skipToPreviousChapter() {
        val book = repository.getMostRecentAudiobookDirect() ?: return
        val currentPosition = audioHandler.getCurrentPosition()
        val prevChapter = book.chapters.lastOrNull { it.startMs < currentPosition - 3000 }
        if (prevChapter != null) {
            Log.d(TAG, "Skipping to previous chapter: ${prevChapter.title}")
            audioHandler.seekTo(prevChapter.startMs)
        } else {
            audioHandler.seekTo(0)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaLibrarySession

    override fun onDestroy() {
        Log.d(TAG, "LithosPlaybackService onDestroy")
        sleepTimerJob?.cancel()
        mediaLibrarySession?.run {
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Lithos Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
