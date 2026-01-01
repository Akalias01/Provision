package com.mossglen.lithos.service

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.FileProvider
import androidx.media.MediaBrowserServiceCompat
import com.mossglen.lithos.data.AudioHandler
import com.mossglen.lithos.data.LibraryRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Legacy MediaBrowserServiceCompat for older Android Auto versions.
 * Provides the same browsing structure as LithosPlaybackService.
 *
 * Features:
 * - Continue Listening
 * - Chapters
 * - Recent Books
 * - Library
 * - By Author
 * - By Series
 */
@AndroidEntryPoint
class LithosLegacyBrowserService : MediaBrowserServiceCompat() {

    companion object {
        private const val TAG = "LithosLegacyBrowserService"
        private const val MEDIA_ROOT_ID = "root"

        // Media ID prefixes (match LithosPlaybackService)
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

    @Inject lateinit var repository: LibraryRepository
    @Inject lateinit var audioHandler: AudioHandler

    private var mediaSession: MediaSessionCompat? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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

    /**
     * Format duration in milliseconds to human-readable format.
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

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LithosLegacyBrowserService onCreate")

        mediaSession = MediaSessionCompat(this, TAG).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    Log.d(TAG, "onPlayFromMediaId: $mediaId")
                    if (mediaId == null) return

                    serviceScope.launch {
                        try {
                            // Handle chapter selection
                            if (mediaId.startsWith(PREFIX_CHAPTER)) {
                                val parts = mediaId.removePrefix(PREFIX_CHAPTER).split("_")
                                if (parts.size >= 2) {
                                    val bookId = parts.dropLast(1).joinToString("_")
                                    val chapterIndex = parts.last().toIntOrNull() ?: 0
                                    val book = repository.getBookById(bookId)
                                    if (book != null && chapterIndex < book.chapters.size) {
                                        val chapter = book.chapters[chapterIndex]
                                        Log.d(TAG, "Playing chapter: ${chapter.title}")
                                        audioHandler.loadBookAndSeek(book, chapter.startMs)
                                        audioHandler.play()
                                        return@launch
                                    }
                                }
                            }

                            // Regular book selection
                            val book = repository.getBookById(mediaId)
                            if (book != null) {
                                Log.d(TAG, "Playing book: ${book.title}")
                                audioHandler.playBook(book)
                            } else {
                                Log.w(TAG, "Book not found for mediaId: $mediaId")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error playing book: $mediaId", e)
                        }
                    }
                }

                override fun onPlay() {
                    Log.d(TAG, "onPlay")
                    audioHandler.play()
                }

                override fun onPause() {
                    Log.d(TAG, "onPause")
                    audioHandler.pause()
                }

                override fun onSkipToNext() {
                    Log.d(TAG, "onSkipToNext (30s forward)")
                    audioHandler.skipForward(30)
                }

                override fun onSkipToPrevious() {
                    Log.d(TAG, "onSkipToPrevious (10s back)")
                    audioHandler.skipBack(10)
                }

                override fun onSeekTo(pos: Long) {
                    Log.d(TAG, "onSeekTo: $pos")
                    audioHandler.seekTo(pos)
                }

                override fun onStop() {
                    Log.d(TAG, "onStop")
                    audioHandler.pause()
                }

                override fun onSetPlaybackSpeed(speed: Float) {
                    Log.d(TAG, "onSetPlaybackSpeed: $speed")
                    audioHandler.setPlaybackSpeed(speed)
                }
            })

            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                        PlaybackStateCompat.ACTION_SET_PLAYBACK_SPEED
                    )
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                    .build()
            )

            isActive = true
        }

        sessionToken = mediaSession?.sessionToken
        Log.d(TAG, "Legacy MediaSession created")
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        Log.d(TAG, "onGetRoot called by: $clientPackageName")

        val extras = Bundle().apply {
            putBoolean(CONTENT_STYLE_SUPPORTED, true)
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID_ITEM_HINT_VALUE)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST_ITEM_HINT_VALUE)
        }

        return BrowserRoot(MEDIA_ROOT_ID, extras)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        Log.d(TAG, "onLoadChildren for: $parentId")
        result.detach()

        serviceScope.launch {
            val items = when {
                parentId == MEDIA_ROOT_ID -> getRootItems()
                parentId == "continue_listening" -> getContinueListeningItems()
                parentId == "chapters" -> getChapterItems()
                parentId == "recent_books" -> getRecentBooksItems()
                parentId == "library" -> getLibraryItems()
                parentId == "by_author" -> getAuthorListItems()
                parentId == "by_series" -> getSeriesListItems()
                parentId.startsWith(PREFIX_AUTHOR) -> getBooksByAuthor(parentId.removePrefix(PREFIX_AUTHOR))
                parentId.startsWith(PREFIX_SERIES) -> getBooksBySeries(parentId.removePrefix(PREFIX_SERIES))
                else -> {
                    Log.w(TAG, "Unknown parentId: $parentId")
                    mutableListOf()
                }
            }

            Log.d(TAG, "Returning ${items.size} items for $parentId")
            result.sendResult(items)
        }
    }

    // ===== Content Building Methods =====

    private fun getRootItems(): MutableList<MediaBrowserCompat.MediaItem> {
        return mutableListOf(
            createBrowsableItem("continue_listening", "Continue Listening", "Pick up where you left off"),
            createBrowsableItem("chapters", "Chapters", "Jump to a chapter"),
            createBrowsableItem("recent_books", "Recent Books", "Recently played"),
            createBrowsableItem("library", "Library", "All your audiobooks"),
            createBrowsableItem("by_author", "By Author", "Browse by author"),
            createBrowsableItem("by_series", "By Series", "Browse by series")
        )
    }

    private suspend fun getContinueListeningItems(): MutableList<MediaBrowserCompat.MediaItem> {
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

            mutableListOf(
                MediaBrowserCompat.MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId(recentBook.id)
                        .setTitle(recentBook.title)
                        .setSubtitle(subtitle)
                        .setDescription(recentBook.author)
                        .setIconUri(getContentUriForCover(recentBook.coverUrl))
                        .build(),
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                )
            )
        } else {
            mutableListOf(
                MediaBrowserCompat.MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId("no_books")
                        .setTitle("No audiobooks")
                        .setSubtitle("Add audiobooks to get started")
                        .build(),
                    0 // Not playable
                )
            )
        }
    }

    private suspend fun getChapterItems(): MutableList<MediaBrowserCompat.MediaItem> {
        val currentBook = repository.getMostRecentAudiobookDirect()
        return if (currentBook != null && currentBook.chapters.isNotEmpty()) {
            currentBook.chapters.mapIndexed { index, chapter ->
                MediaBrowserCompat.MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId("${PREFIX_CHAPTER}${currentBook.id}_${index}")
                        .setTitle(chapter.title.ifEmpty { "Chapter ${index + 1}" })
                        .setSubtitle(formatDurationMs(chapter.startMs))
                        .setIconUri(getContentUriForCover(currentBook.coverUrl))
                        .build(),
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                )
            }.toMutableList()
        } else {
            mutableListOf(
                MediaBrowserCompat.MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId("no_chapters")
                        .setTitle("No chapters")
                        .setSubtitle("This book has no chapter markers")
                        .build(),
                    0
                )
            )
        }
    }

    private suspend fun getRecentBooksItems(): MutableList<MediaBrowserCompat.MediaItem> {
        val books = repository.getAllBooksDirect().filter { it.format == "AUDIO" }
            .sortedByDescending { it.lastPlayedTimestamp }
        return books.map { book ->
            val progressPercent = if (book.duration > 0) ((book.progress.toFloat() / book.duration) * 100).toInt() else 0
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(book.id)
                    .setTitle(book.title)
                    .setSubtitle(if (book.progress > 0) "$progressPercent% complete" else book.author)
                    .setDescription(book.author)
                    .setIconUri(getContentUriForCover(book.coverUrl))
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }.toMutableList()
    }

    private suspend fun getLibraryItems(): MutableList<MediaBrowserCompat.MediaItem> {
        val books = repository.getAllBooksDirect().filter { it.format == "AUDIO" }
            .sortedBy { it.title }
        return books.map { book ->
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(book.id)
                    .setTitle(book.title)
                    .setSubtitle(book.author)
                    .setIconUri(getContentUriForCover(book.coverUrl))
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }.toMutableList()
    }

    private suspend fun getAuthorListItems(): MutableList<MediaBrowserCompat.MediaItem> {
        val authors = repository.getUniqueAuthorsForAuto()
        return if (authors.isNotEmpty()) {
            authors.map { author ->
                createBrowsableItem("${PREFIX_AUTHOR}$author", author, "Tap to see books")
            }.toMutableList()
        } else {
            mutableListOf(
                MediaBrowserCompat.MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId("no_authors")
                        .setTitle("No authors found")
                        .setSubtitle("Your books don't have author info")
                        .build(),
                    0
                )
            )
        }
    }

    private suspend fun getSeriesListItems(): MutableList<MediaBrowserCompat.MediaItem> {
        val series = repository.getUniqueSeriesForAuto()
        return if (series.isNotEmpty()) {
            series.map { seriesName ->
                createBrowsableItem("${PREFIX_SERIES}$seriesName", seriesName, "Tap to see books")
            }.toMutableList()
        } else {
            mutableListOf(
                MediaBrowserCompat.MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId("no_series")
                        .setTitle("No series found")
                        .setSubtitle("Your books don't have series info")
                        .build(),
                    0
                )
            )
        }
    }

    private suspend fun getBooksByAuthor(author: String): MutableList<MediaBrowserCompat.MediaItem> {
        val books = repository.getAllBooksDirect()
            .filter { it.format == "AUDIO" && it.author.equals(author, ignoreCase = true) }
            .sortedByDescending { it.lastPlayedTimestamp }
        return books.map { book ->
            val progressPercent = if (book.duration > 0) ((book.progress.toFloat() / book.duration) * 100).toInt() else 0
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(book.id)
                    .setTitle(book.title)
                    .setSubtitle(if (book.progress > 0) "$progressPercent% complete" else "Not started")
                    .setIconUri(getContentUriForCover(book.coverUrl))
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }.toMutableList()
    }

    private suspend fun getBooksBySeries(series: String): MutableList<MediaBrowserCompat.MediaItem> {
        val books = repository.getAllBooksDirect()
            .filter { it.format == "AUDIO" && it.seriesInfo.equals(series, ignoreCase = true) }
            .sortedBy { it.title }
        return books.map { book ->
            val progressPercent = if (book.duration > 0) ((book.progress.toFloat() / book.duration) * 100).toInt() else 0
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(book.id)
                    .setTitle(book.title)
                    .setSubtitle(if (book.progress > 0) "$progressPercent% complete" else "Not started")
                    .setIconUri(getContentUriForCover(book.coverUrl))
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }.toMutableList()
    }

    // ===== Helper Methods =====

    private fun createBrowsableItem(mediaId: String, title: String, subtitle: String): MediaBrowserCompat.MediaItem {
        return MediaBrowserCompat.MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .setTitle(title)
                .setSubtitle(subtitle)
                .build(),
            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        )
    }

    override fun onDestroy() {
        Log.d(TAG, "LithosLegacyBrowserService onDestroy")
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.action == Intent.ACTION_MEDIA_BUTTON) {
                Log.d(TAG, "Media button intent received")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
}
