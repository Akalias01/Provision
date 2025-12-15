package com.rezon.app.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rezon.app.data.repository.BookmarkRepository
import com.rezon.app.domain.model.Book
import com.rezon.app.domain.model.Bookmark
import com.rezon.app.domain.model.BookFormat
import com.rezon.app.domain.model.Chapter
import com.rezon.app.service.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Player UI State
 */
data class PlayerUiState(
    val book: Book? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val currentChapterIndex: Int = 0,
    val currentChapter: Chapter? = null,
    val sleepTimerRemaining: Long? = null,
    val isSilenceSkippingEnabled: Boolean = false,
    val isVoiceBoostEnabled: Boolean = false,
    val volume: Float = 0.7f,
    val brightness: Float = 0.5f,
    val isLoading: Boolean = true,
    val isBuffering: Boolean = false,
    val error: String? = null,
    val bookmarks: List<Bookmark> = emptyList()
)

/**
 * REZON Player ViewModel
 *
 * Manages playback state with Media3 integration.
 * Implements Smart Resume feature:
 * - Pause < 5 mins: Resume instantly
 * - Pause > 5 mins: Auto-rewind 10s
 * - Pause > 1 hr: Auto-rewind 30s
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Current book ID for bookmark operations
    private var currentBookId: String? = null

    // Timestamp of last pause for Smart Resume
    private var lastPauseTime: Long? = null

    // Skip amounts (configurable)
    private var skipBackwardMs = 10_000L // 10 seconds
    private var skipForwardMs = 30_000L  // 30 seconds

    // Playback speeds
    private val playbackSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    // Position update job
    private var positionUpdateJob: Job? = null

    // Sleep timer job
    private var sleepTimerJob: Job? = null

    // Bookmark collection job
    private var bookmarkCollectionJob: Job? = null

    init {
        // Connect to playback service
        playbackController.connect()

        // Observe playback state
        viewModelScope.launch {
            playbackController.playbackState.collect { playbackState ->
                _uiState.update {
                    it.copy(
                        isPlaying = playbackState.isPlaying,
                        currentPosition = playbackState.currentPosition,
                        duration = if (playbackState.duration > 0) playbackState.duration else it.duration,
                        playbackSpeed = playbackState.playbackSpeed,
                        isBuffering = playbackState.isBuffering
                    )
                }

                // Start/stop position updates based on playback state
                if (playbackState.isPlaying) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }
        }
    }

    private fun startPositionUpdates() {
        if (positionUpdateJob?.isActive == true) return

        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                val position = playbackController.getCurrentPosition()
                _uiState.update { it.copy(currentPosition = position) }
                updateCurrentChapter(position)
                delay(500) // Update every 500ms
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private fun updateCurrentChapter(position: Long) {
        val chapters = _uiState.value.book?.chapters ?: return
        val chapterIndex = chapters.indexOfLast { position >= it.startTime }
        if (chapterIndex >= 0 && chapterIndex != _uiState.value.currentChapterIndex) {
            _uiState.update {
                it.copy(
                    currentChapterIndex = chapterIndex,
                    currentChapter = chapters[chapterIndex]
                )
            }
        }
    }

    /**
     * Load book data and prepare for playback
     */
    fun loadBook(bookId: String) {
        currentBookId = bookId

        // Start collecting bookmarks for this book
        startBookmarkCollection(bookId)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // TODO: Load from repository - using mock data for now
            val mockBook = Book(
                id = bookId,
                title = "Sample Audiobook",
                author = "Sample Author",
                filePath = "/storage/emulated/0/Audiobooks/sample.mp3",
                format = BookFormat.AUDIO_MP3,
                duration = 3600000L, // 1 hour
                chapters = listOf(
                    Chapter(
                        title = "Chapter 1: The Beginning",
                        index = 0,
                        startTime = 0L,
                        endTime = 1200000L
                    ),
                    Chapter(
                        title = "Chapter 2: The Journey",
                        index = 1,
                        startTime = 1200000L,
                        endTime = 2400000L
                    ),
                    Chapter(
                        title = "Chapter 3: The End",
                        index = 2,
                        startTime = 2400000L,
                        endTime = 3600000L
                    )
                )
            )

            _uiState.update {
                it.copy(
                    book = mockBook,
                    duration = mockBook.duration,
                    currentChapter = mockBook.chapters.firstOrNull(),
                    isLoading = false
                )
            }

            // Start playback
            playbackController.playBook(
                bookId = mockBook.id,
                filePath = mockBook.filePath,
                title = mockBook.title,
                author = mockBook.author,
                startPosition = mockBook.currentPosition
            )
        }
    }

    /**
     * Start collecting bookmarks from database
     */
    private fun startBookmarkCollection(bookId: String) {
        bookmarkCollectionJob?.cancel()
        bookmarkCollectionJob = viewModelScope.launch {
            bookmarkRepository.getBookmarksForBook(bookId).collect { bookmarks ->
                _uiState.update { it.copy(bookmarks = bookmarks) }
            }
        }
    }

    /**
     * Toggle play/pause with Smart Resume
     */
    fun togglePlayPause() {
        val currentState = _uiState.value

        if (currentState.isPlaying) {
            // Pausing - record the time
            lastPauseTime = System.currentTimeMillis()
            playbackController.pause()
        } else {
            // Resuming - apply Smart Resume logic
            lastPauseTime?.let { pauseTime ->
                val pauseDuration = System.currentTimeMillis() - pauseTime
                val rewindAmount = when {
                    pauseDuration > 3600_000 -> 30_000L  // > 1 hour: rewind 30s
                    pauseDuration > 300_000 -> 10_000L   // > 5 mins: rewind 10s
                    else -> 0L                           // < 5 mins: no rewind
                }

                if (rewindAmount > 0) {
                    val newPosition = (currentState.currentPosition - rewindAmount).coerceAtLeast(0)
                    playbackController.seekTo(newPosition)
                }
            }
            playbackController.play()
        }
    }

    /**
     * Skip backward (default 10s)
     */
    fun skipBackward() {
        playbackController.skipBackward(skipBackwardMs)
    }

    /**
     * Skip forward (default 30s)
     */
    fun skipForward() {
        playbackController.skipForward(skipForwardMs)
    }

    /**
     * Seek to specific position
     */
    fun seekTo(positionMs: Long) {
        playbackController.seekTo(positionMs)
        _uiState.update { it.copy(currentPosition = positionMs) }
    }

    /**
     * Go to previous chapter
     */
    fun previousChapter() {
        val state = _uiState.value
        val newIndex = (state.currentChapterIndex - 1).coerceAtLeast(0)
        val chapter = state.book?.chapters?.getOrNull(newIndex)
        chapter?.let {
            playbackController.seekTo(it.startTime)
            _uiState.update { s ->
                s.copy(
                    currentChapterIndex = newIndex,
                    currentChapter = chapter,
                    currentPosition = chapter.startTime
                )
            }
        }
    }

    /**
     * Go to next chapter
     */
    fun nextChapter() {
        val state = _uiState.value
        val totalChapters = state.book?.chapters?.size ?: 0
        val newIndex = (state.currentChapterIndex + 1).coerceAtMost(totalChapters - 1)
        val chapter = state.book?.chapters?.getOrNull(newIndex)
        chapter?.let {
            playbackController.seekTo(it.startTime)
            _uiState.update { s ->
                s.copy(
                    currentChapterIndex = newIndex,
                    currentChapter = chapter,
                    currentPosition = chapter.startTime
                )
            }
        }
    }

    /**
     * Cycle through playback speeds
     */
    fun cyclePlaybackSpeed() {
        val currentIndex = playbackSpeeds.indexOf(_uiState.value.playbackSpeed)
        val nextIndex = (currentIndex + 1) % playbackSpeeds.size
        val newSpeed = playbackSpeeds[nextIndex]
        playbackController.setPlaybackSpeed(newSpeed)
        _uiState.update { it.copy(playbackSpeed = newSpeed) }
    }

    /**
     * Set specific playback speed
     */
    fun setPlaybackSpeed(speed: Float) {
        playbackController.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    /**
     * Set volume (0.0 to 1.0)
     */
    fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        playbackController.setVolume(clampedVolume)
        _uiState.update { it.copy(volume = clampedVolume) }
    }

    /**
     * Set brightness (0.0 to 1.0)
     */
    fun setBrightness(brightness: Float) {
        _uiState.update { it.copy(brightness = brightness.coerceIn(0f, 1f)) }
    }

    /**
     * Toggle silence skipping
     */
    fun toggleSilenceSkipping() {
        _uiState.update { it.copy(isSilenceSkippingEnabled = !it.isSilenceSkippingEnabled) }
        // TODO: Configure via audio processor
    }

    /**
     * Toggle voice boost EQ
     */
    fun toggleVoiceBoost() {
        _uiState.update { it.copy(isVoiceBoostEnabled = !it.isVoiceBoostEnabled) }
        // TODO: Configure via equalizer
    }

    /**
     * Start sleep timer
     */
    fun startSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        _uiState.update { it.copy(sleepTimerRemaining = durationMs) }

        sleepTimerJob = viewModelScope.launch {
            var remaining = durationMs
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining -= 1000
                _uiState.update { it.copy(sleepTimerRemaining = remaining) }
            }
            if (remaining <= 0) {
                playbackController.pause()
                _uiState.update { it.copy(sleepTimerRemaining = null) }
            }
        }
    }

    /**
     * Cancel sleep timer
     */
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _uiState.update { it.copy(sleepTimerRemaining = null) }
    }

    /**
     * Add bookmark at current position
     */
    fun addBookmark(note: String? = null) {
        val bookId = currentBookId ?: return
        val currentPosition = _uiState.value.currentPosition
        val chapterIndex = _uiState.value.currentChapterIndex
        val newBookmark = Bookmark(
            position = currentPosition,
            note = note,
            chapterIndex = chapterIndex
        )
        viewModelScope.launch {
            bookmarkRepository.addBookmark(bookId, newBookmark)
        }
    }

    /**
     * Update bookmark note
     */
    fun updateBookmarkNote(bookmarkId: String, newNote: String) {
        val bookId = currentBookId ?: return
        val bookmark = _uiState.value.bookmarks.find { it.id == bookmarkId } ?: return
        val updatedBookmark = bookmark.copy(note = newNote)
        viewModelScope.launch {
            bookmarkRepository.updateBookmark(bookId, updatedBookmark)
        }
    }

    /**
     * Remove bookmark
     */
    fun removeBookmark(bookmarkId: String) {
        viewModelScope.launch {
            bookmarkRepository.deleteBookmark(bookmarkId)
        }
    }

    /**
     * Go to bookmark position
     */
    fun goToBookmark(bookmark: Bookmark) {
        seekTo(bookmark.position)
    }

    /**
     * Update skip amounts from settings
     */
    fun updateSkipAmounts(backwardMs: Long, forwardMs: Long) {
        skipBackwardMs = backwardMs
        skipForwardMs = forwardMs
    }

    override fun onCleared() {
        super.onCleared()
        stopPositionUpdates()
        sleepTimerJob?.cancel()
        bookmarkCollectionJob?.cancel()
        playbackController.disconnect()
    }
}

/**
 * Extension to set screen brightness
 */
fun Activity.setScreenBrightness(brightness: Float) {
    val params = window.attributes
    params.screenBrightness = brightness.coerceIn(0f, 1f)
    window.attributes = params
}
