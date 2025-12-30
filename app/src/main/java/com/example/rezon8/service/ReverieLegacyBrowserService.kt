package com.mossglen.reverie.service

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
import com.mossglen.reverie.data.AudioHandler
import com.mossglen.reverie.data.LibraryRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Legacy MediaBrowserServiceCompat for older Android Auto versions.
 * Some car systems still use the old MediaBrowser API and won't discover
 * apps that only implement MediaLibraryService.
 */
@AndroidEntryPoint
class ReverieLegacyBrowserService : MediaBrowserServiceCompat() {

    companion object {
        private const val TAG = "ReverieLegacyBrowserService"
        private const val MEDIA_ROOT_ID = "root"

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
     * Android Auto requires content:// URIs for artwork.
     */
    private fun getContentUriForCover(filePath: String?): Uri? {
        if (filePath.isNullOrEmpty()) return null
        return try {
            val file = File(filePath)
            if (file.exists()) {
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get content URI for cover: $filePath", e)
            null
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ReverieLegacyBrowserService onCreate - providing backward compatibility")

        // Create a MediaSessionCompat for the legacy service
        mediaSession = MediaSessionCompat(this, TAG).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            // Set playback callback to handle Android Auto commands
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    Log.d(TAG, "onPlayFromMediaId: $mediaId")
                    if (mediaId == null) return

                    serviceScope.launch {
                        try {
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
                    Log.d(TAG, "onSkipToNext")
                    audioHandler.skipForward()
                }

                override fun onSkipToPrevious() {
                    Log.d(TAG, "onSkipToPrevious")
                    audioHandler.skipBack()
                }

                override fun onSeekTo(pos: Long) {
                    Log.d(TAG, "onSeekTo: $pos")
                    audioHandler.seekTo(pos)
                }

                override fun onStop() {
                    Log.d(TAG, "onStop")
                    audioHandler.pause()
                }
            })

            // Set initial playback state
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
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                    )
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                    .build()
            )

            isActive = true
        }

        sessionToken = mediaSession?.sessionToken
        Log.d(TAG, "Legacy MediaSession created with playback callback")
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        Log.d(TAG, "onGetRoot called by: $clientPackageName (uid: $clientUid)")

        // Log all incoming root hints for diagnostics
        rootHints?.let { hints ->
            Log.d(TAG, "Root hints received:")
            for (key in hints.keySet()) {
                Log.d(TAG, "  $key = ${hints.get(key)}")
            }
        }

        // Check for Android Auto specific hints
        val isRecent = rootHints?.getBoolean(BrowserRoot.EXTRA_RECENT) ?: false
        val isOffline = rootHints?.getBoolean(BrowserRoot.EXTRA_OFFLINE) ?: false
        val isSuggested = rootHints?.getBoolean("android.media.extra.SUGGESTED") ?: false

        Log.d(TAG, "Root flags - Recent: $isRecent, Offline: $isOffline, Suggested: $isSuggested")

        // Create extras with content style hints
        val extras = Bundle().apply {
            putBoolean(CONTENT_STYLE_SUPPORTED, true)
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID_ITEM_HINT_VALUE)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST_ITEM_HINT_VALUE)
        }

        Log.d(TAG, "Returning BrowserRoot with Android Auto content style hints")
        return BrowserRoot(MEDIA_ROOT_ID, extras)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        Log.d(TAG, "onLoadChildren called for parentId: $parentId")

        // Detach result to perform async operations
        result.detach()

        serviceScope.launch {
            val items = when (parentId) {
                MEDIA_ROOT_ID -> {
                    // Return browsable folders
                    mutableListOf(
                        MediaBrowserCompat.MediaItem(
                            MediaDescriptionCompat.Builder()
                                .setMediaId("continue_listening")
                                .setTitle("Continue Listening")
                                .build(),
                            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                        ),
                        MediaBrowserCompat.MediaItem(
                            MediaDescriptionCompat.Builder()
                                .setMediaId("recent_books")
                                .setTitle("Recent Books")
                                .build(),
                            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                        ),
                        MediaBrowserCompat.MediaItem(
                            MediaDescriptionCompat.Builder()
                                .setMediaId("library")
                                .setTitle("Library")
                                .build(),
                            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                        )
                    )
                }
                "continue_listening" -> {
                    // Return the most recently played book
                    val recentBook = repository.getMostRecentBookDirect()
                    if (recentBook != null) {
                        val coverUri = getContentUriForCover(recentBook.coverUrl)
                        mutableListOf(
                            MediaBrowserCompat.MediaItem(
                                MediaDescriptionCompat.Builder()
                                    .setMediaId(recentBook.id)
                                    .setTitle(recentBook.title)
                                    .setSubtitle(recentBook.author)
                                    .setIconUri(coverUri)
                                    .build(),
                                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                            )
                        )
                    } else {
                        mutableListOf()
                    }
                }
                "recent_books", "library" -> {
                    // Return all books with cover art
                    val allBooks = repository.getAllBooksDirect()
                    allBooks.map { book ->
                        val coverUri = getContentUriForCover(book.coverUrl)
                        MediaBrowserCompat.MediaItem(
                            MediaDescriptionCompat.Builder()
                                .setMediaId(book.id)
                                .setTitle(book.title)
                                .setSubtitle(book.author)
                                .setIconUri(coverUri)
                                .build(),
                            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                        )
                    }.toMutableList()
                }
                else -> {
                    Log.w(TAG, "Unknown parentId: $parentId")
                    mutableListOf()
                }
            }

            Log.d(TAG, "Returning ${items.size} items for parentId: $parentId")
            result.sendResult(items)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "ReverieLegacyBrowserService onDestroy")
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle MEDIA_BUTTON intents
        intent?.let {
            if (it.action == Intent.ACTION_MEDIA_BUTTON) {
                Log.d(TAG, "Media button intent received")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
}
