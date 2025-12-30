package com.mossglen.reverie.data

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.mossglen.reverie.util.CrashReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayer: ExoPlayer,
    private val audioEffectManager: AudioEffectManager,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "AudioHandler"
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private var currentBook: Book? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Audio Focus Management
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var hasAudioFocus = false
    private var wasPlayingBeforeFocusLoss = false
    private var focusRequest: AudioFocusRequest? = null

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // We regained focus - resume if we were playing before
                Log.d(TAG, "Audio focus gained")
                hasAudioFocus = true
                // Restore full volume immediately
                exoPlayer.volume = 1.0f
                if (wasPlayingBeforeFocusLoss) {
                    exoPlayer.play()
                    wasPlayingBeforeFocusLoss = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss - another app took over (e.g., music player)
                Log.d(TAG, "Audio focus lost permanently")
                hasAudioFocus = false
                wasPlayingBeforeFocusLoss = exoPlayer.isPlaying
                exoPlayer.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss - phone call, navigation, etc.
                Log.d(TAG, "Audio focus lost temporarily")
                hasAudioFocus = false
                wasPlayingBeforeFocusLoss = exoPlayer.isPlaying
                exoPlayer.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Can duck - notification, brief sound
                Log.d(TAG, "Audio focus ducking")
                exoPlayer.volume = 0.3f
            }
        }
    }

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = exoPlayer.duration.coerceAtLeast(0L)
                    // EQ is initialized once on app start - no need to reinitialize
                    // Reinitializing here causes audio glitches
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
    }

    /**
     * Request audio focus before playing. This will pause other audio apps.
     */
    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Reuse existing focus request if available to avoid recreating objects
            if (focusRequest == null) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH) // Audiobooks are speech
                    .build()

                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .build()
            }

            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        // Accept both GRANTED and DELAYED as success
        // DELAYED means we'll get focus soon via callback
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ||
                        result == AudioManager.AUDIOFOCUS_REQUEST_DELAYED

        Log.d(TAG, "Audio focus request result: $result, hasAudioFocus: $hasAudioFocus")
        return hasAudioFocus
    }

    /**
     * Abandon audio focus when stopping playback
     */
    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
        hasAudioFocus = false
        Log.d(TAG, "Audio focus abandoned")
    }

    fun playBook(book: Book) {
        currentBook = book

        // Ensure volume is at normal level
        exoPlayer.volume = 1.0f

        // Request audio focus first - this pauses other audio apps
        if (!requestAudioFocus()) {
            Log.w(TAG, "Could not gain audio focus for playBook")
        }

        try {
            CrashReporter.log("Playing book: ${book.title} (${book.format})")
            CrashReporter.setCustomKey("current_book_id", book.id)
            CrashReporter.setCustomKey("current_book_format", book.format)

            // Check if this is a split-chapter audiobook
            if (isSplitChapterBook(book)) {
                loadSplitChapterBook(book, book.progress)
            } else {
                val mediaItem = createMediaItemWithMetadata(book)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                // Seek to saved progress
                if (book.progress > 0) {
                    exoPlayer.seekTo(book.progress)
                }
            }
            // Load artwork for notification/lock screen
            loadArtworkAsync(book)
        } catch (e: Exception) {
            CrashReporter.logError("Failed to play book: ${book.title}", e)
            e.printStackTrace()
        }
    }

    fun loadBookAndSeek(book: Book) {
        loadBookAndSeek(book, book.progress)
    }

    fun loadBookAndSeek(book: Book, seekPosition: Long) {
        currentBook = book
        try {
            CrashReporter.log("Loading book and seeking: ${book.title} to ${seekPosition}ms")

            // Check if this is a split-chapter audiobook
            if (isSplitChapterBook(book)) {
                loadSplitChapterBook(book, seekPosition)
            } else {
                val mediaItem = createMediaItemWithMetadata(book)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.seekTo(seekPosition)
            }
            // Load artwork for notification/lock screen
            loadArtworkAsync(book)
        } catch (e: Exception) {
            CrashReporter.logError("Failed to load book: ${book.title}", e)
            e.printStackTrace()
        }
    }

    /**
     * Check if this book has split chapter files (each chapter is a separate audio file)
     */
    private fun isSplitChapterBook(book: Book): Boolean {
        return book.chapters.isNotEmpty() && book.chapters.any { !it.filePath.isNullOrBlank() }
    }

    /**
     * Load a split-chapter audiobook as a playlist
     */
    private fun loadSplitChapterBook(book: Book, seekToPosition: Long) {
        val chaptersWithFiles = book.chapters.filter { !it.filePath.isNullOrBlank() }

        if (chaptersWithFiles.isEmpty()) {
            CrashReporter.log("No chapter files found, falling back to single file")
            val mediaItem = createMediaItemWithMetadata(book)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            if (seekToPosition > 0) exoPlayer.seekTo(seekToPosition)
            return
        }

        CrashReporter.log("Loading split-chapter book with ${chaptersWithFiles.size} chapter files")

        // Create media items for each chapter file
        val mediaItems = chaptersWithFiles.map { chapter ->
            val metadata = MediaMetadata.Builder()
                .setTitle(chapter.title)
                .setArtist(book.author)
                .setAlbumTitle(book.title)
                .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
                .build()

            MediaItem.Builder()
                .setUri(Uri.parse("file://${chapter.filePath}"))
                .setMediaMetadata(metadata)
                .build()
        }

        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()

        // Find which chapter and position to seek to
        if (seekToPosition > 0) {
            var chapterIndex = 0
            var positionInChapter = seekToPosition

            for ((index, chapter) in chaptersWithFiles.withIndex()) {
                val chapterDuration = chapter.endMs - chapter.startMs
                if (positionInChapter <= chapterDuration) {
                    chapterIndex = index
                    break
                }
                positionInChapter -= chapterDuration
            }

            exoPlayer.seekTo(chapterIndex, positionInChapter.coerceAtLeast(0))
            CrashReporter.log("Seeking to chapter $chapterIndex at position $positionInChapter")
        }
    }

    private fun createMediaItemWithMetadata(book: Book): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(book.title)
            .setArtist(book.author)
            .setAlbumTitle(book.seriesInfo.ifEmpty { book.title })
            .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK)
            .build()

        return MediaItem.Builder()
            .setUri(Uri.parse(book.filePath))
            .setMediaMetadata(metadata)
            .build()
    }

    private fun loadArtworkAsync(book: Book) {
        if (book.coverUrl.isNullOrEmpty()) return

        scope.launch(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(book.coverUrl)
                    .allowHardware(false)
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap()
                    // Update media item with artwork
                    val updatedMetadata = MediaMetadata.Builder()
                        .setTitle(book.title)
                        .setArtist(book.author)
                        .setAlbumTitle(book.seriesInfo.ifEmpty { book.title })
                        .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK)
                        .setArtworkData(bitmapToByteArray(bitmap), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        .build()

                    val updatedItem = MediaItem.Builder()
                        .setUri(Uri.parse(book.filePath))
                        .setMediaMetadata(updatedMetadata)
                        .build()

                    scope.launch(Dispatchers.Main) {
                        val pos = exoPlayer.currentPosition
                        exoPlayer.setMediaItem(updatedItem)
                        exoPlayer.prepare()
                        exoPlayer.seekTo(pos)
                    }
                }
            } catch (e: Exception) {
                CrashReporter.logError("Failed to load artwork for: ${book.title}", e)
                e.printStackTrace()
            }
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    fun play() {
        // Request audio focus before playing - this pauses other audio apps
        if (!hasAudioFocus) {
            if (!requestAudioFocus()) {
                Log.w(TAG, "Could not gain audio focus, attempting playback anyway")
                // Don't return - try to play anyway
                // The system will handle audio mixing if needed
            }
        }

        // Ensure volume is set to normal before playing
        exoPlayer.volume = 1.0f
        exoPlayer.play()
        Log.d(TAG, "play() called, isPlaying: ${exoPlayer.isPlaying}")
    }

    fun pause() {
        exoPlayer.pause()
        // Note: We don't abandon focus on pause - only on stop/release
        // This allows quick resume without other apps jumping in
    }

    fun stop() {
        exoPlayer.stop()
        abandonAudioFocus()
        // Reset volume to default
        exoPlayer.volume = 1.0f
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(position: Long) {
        val book = currentBook
        if (book != null && isSplitChapterBook(book)) {
            // For split-chapter books, find the right chapter and seek within it
            val chaptersWithFiles = book.chapters.filter { !it.filePath.isNullOrBlank() }
            var chapterIndex = 0
            var positionInChapter = position

            for ((index, chapter) in chaptersWithFiles.withIndex()) {
                val chapterDuration = chapter.endMs - chapter.startMs
                if (positionInChapter <= chapterDuration) {
                    chapterIndex = index
                    break
                }
                positionInChapter -= chapterDuration
            }

            exoPlayer.seekTo(chapterIndex, positionInChapter.coerceAtLeast(0))
        } else {
            exoPlayer.seekTo(position)
        }
        _currentPosition.value = position
    }

    fun skipForward(seconds: Int = 30) {
        val newPos = (getOverallPosition() + seconds * 1000L).coerceAtMost(getOverallDuration())
        seekTo(newPos)
    }

    fun skipBack(seconds: Int = 30) {
        val newPos = (getOverallPosition() - seconds * 1000L).coerceAtLeast(0L)
        seekTo(newPos)
    }

    fun updateProgress() {
        _currentPosition.value = getOverallPosition()
        _duration.value = getOverallDuration()
    }

    fun getCurrentProgress(): Long = getOverallPosition()

    fun getCurrentPosition(): Long = getOverallPosition()

    /**
     * Get overall position across all chapters for split-chapter books
     */
    private fun getOverallPosition(): Long {
        val book = currentBook
        if (book != null && isSplitChapterBook(book)) {
            val chaptersWithFiles = book.chapters.filter { !it.filePath.isNullOrBlank() }
            val currentMediaIndex = exoPlayer.currentMediaItemIndex
            val positionInCurrentChapter = exoPlayer.currentPosition.coerceAtLeast(0L)

            // Sum up durations of all previous chapters
            var overallPosition = 0L
            for (i in 0 until currentMediaIndex) {
                if (i < chaptersWithFiles.size) {
                    overallPosition += chaptersWithFiles[i].endMs - chaptersWithFiles[i].startMs
                }
            }
            return overallPosition + positionInCurrentChapter
        }
        return exoPlayer.currentPosition.coerceAtLeast(0L)
    }

    /**
     * Get overall duration for split-chapter books
     */
    private fun getOverallDuration(): Long {
        val book = currentBook
        if (book != null && isSplitChapterBook(book)) {
            return book.duration // Use the pre-calculated total duration
        }
        return exoPlayer.duration.coerceAtLeast(0L)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    fun release() {
        abandonAudioFocus()
        exoPlayer.release()
    }
}
