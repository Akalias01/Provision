package com.mossglen.reverie.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mossglen.reverie.data.AudioHandler
import com.mossglen.reverie.data.Book
import com.mossglen.reverie.data.BookDao
import com.mossglen.reverie.data.LibraryRepository
import com.mossglen.reverie.data.ListeningStatsRepository
import com.mossglen.reverie.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val audioHandler: AudioHandler,
    private val bookDao: BookDao,
    private val libraryRepository: LibraryRepository,
    private val listeningStatsRepository: ListeningStatsRepository,
    private val settingsRepository: SettingsRepository,
    private val autoBookmarkManager: com.mossglen.reverie.data.AutoBookmarkManager
) : ViewModel() {

    // Audio state from handler
    val isPlaying = audioHandler.isPlaying
    val position = audioHandler.currentPosition
    val progress = audioHandler.currentPosition // Alias for PlayerScreen
    val duration = audioHandler.duration

    // Current book state
    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook = _currentBook.asStateFlow()

    // Resume confirmation dialog state
    private val _showResumeDialog = MutableStateFlow<Book?>(null)
    val showResumeDialog = _showResumeDialog.asStateFlow()

    // Playback speed
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    // Custom speed presets
    val customSpeedPresets = settingsRepository.customSpeedPresets

    // Sleep timer state
    enum class SleepTimerMode {
        MINUTES,
        END_OF_CHAPTER
    }

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow<Long>(0L)
    val sleepTimerRemaining = _sleepTimerRemaining.asStateFlow()

    private val _sleepTimerEndTime = MutableStateFlow<Long?>(null)
    val sleepTimerEndTime = _sleepTimerEndTime.asStateFlow()

    private val _sleepTimerMode = MutableStateFlow<SleepTimerMode?>(null)
    val sleepTimerMode = _sleepTimerMode.asStateFlow()

    private var progressSaverJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var statsTrackerJob: Job? = null
    private var sessionStarted: Boolean = false

    init {
        // Progress update loop for UI
        viewModelScope.launch {
            while (isActive) {
                if (isPlaying.value) {
                    audioHandler.updateProgress()
                }
                delay(500)
            }
        }

        // Load saved playback speed
        viewModelScope.launch {
            val savedSpeed = settingsRepository.playbackSpeed.firstOrNull() ?: 1.0f
            _playbackSpeed.value = savedSpeed
            audioHandler.setPlaybackSpeed(savedSpeed)
        }
    }

    /**
     * Load a book into the player without auto-playing.
     * Used when clicking cover art to open full player.
     */
    fun loadBook(book: Book) {
        // Reset session tracking for new book
        if (_currentBook.value?.id != book.id) {
            sessionStarted = false
        }
        _currentBook.value = book
        if (book.format == "AUDIO") {
            audioHandler.loadBookAndSeek(book)
            startProgressSaver()
        }
    }

    /**
     * Toggle play/pause for the currently loaded book.
     */
    fun togglePlayback() {
        if (_currentBook.value != null) {
            // Check if we're about to start playing (before toggling)
            val wasNotPlaying = !isPlaying.value

            // Toggle playback immediately for instant UI response
            audioHandler.togglePlayPause()

            // Handle auto-bookmark based on play/pause state
            if (wasNotPlaying) {
                // About to start playing - notify resume
                autoBookmarkManager.onResume()
            } else {
                // About to pause - notify pause with current position
                _currentBook.value?.let { book ->
                    val currentPos = audioHandler.getCurrentPosition()
                    autoBookmarkManager.onPause(book.id, currentPos)
                }
            }

            // Track session start asynchronously (non-blocking) after playback starts
            if (wasNotPlaying && !sessionStarted) {
                sessionStarted = true
                viewModelScope.launch {
                    listeningStatsRepository.startSession()
                }
            }
        }
    }

    /**
     * Play or pause a specific book.
     * If it's a new book, load and play it.
     * If it's the current book, toggle playback.
     * Used for headphone icon in library list.
     */
    fun playOrPauseBook(book: Book) {
        if (_currentBook.value?.id != book.id) {
            loadBook(book)
            audioHandler.play()
            autoBookmarkManager.onResume()
        } else {
            togglePlayback()
        }
    }

    /**
     * Legacy method for compatibility - loads and shows player.
     */
    fun loadAndShowPlayer(book: Book) {
        loadBook(book)
    }

    /**
     * Legacy method for mini-player toggle button.
     */
    fun toggleMiniPlayer() {
        togglePlayback()
    }

    /**
     * Play a book, automatically resuming from saved position.
     * No dialog needed - audiobooks should just resume where you left off.
     */
    fun checkAndShowResumeDialog(book: Book) {
        // Auto-resume: just load and play from saved position
        resumeFromPosition(book)
    }

    /**
     * Resume book from saved position.
     */
    fun resumeFromPosition(book: Book) {
        _showResumeDialog.value = null
        // Load and play in proper sequence
        loadBookAndPlay(book)
    }

    /**
     * Load a book and immediately start playing.
     * Ensures the book is properly loaded before calling play().
     */
    private fun loadBookAndPlay(book: Book) {
        // Reset session tracking for new book
        if (_currentBook.value?.id != book.id) {
            sessionStarted = false
        }
        _currentBook.value = book
        if (book.format == "AUDIO") {
            // Load book - audioHandler.loadBookAndSeek handles seeking to saved position
            audioHandler.loadBookAndSeek(book)
            startProgressSaver()
            // Small delay to ensure MediaSession is ready, then play
            viewModelScope.launch {
                kotlinx.coroutines.delay(150)
                audioHandler.play()
            }
        }
    }

    /**
     * Start book from beginning (reset progress to 0).
     */
    fun startFromBeginning(book: Book) {
        _showResumeDialog.value = null
        // Reset progress to 0 in database
        viewModelScope.launch {
            bookDao.updateProgress(book.id, 0L, System.currentTimeMillis())
            // Load updated book with progress = 0
            val updatedBook = bookDao.getBookById(book.id) ?: book.copy(progress = 0)
            loadBook(updatedBook)
            audioHandler.play()
        }
    }

    /**
     * Dismiss resume dialog without action.
     */
    fun dismissResumeDialog() {
        _showResumeDialog.value = null
    }

    // Playback controls
    fun play() {
        audioHandler.play()
        // Notify auto-bookmark manager that playback has resumed
        autoBookmarkManager.onResume()
    }

    fun pause() {
        audioHandler.pause()
        // Notify auto-bookmark manager that playback has paused
        _currentBook.value?.let { book ->
            val currentPos = audioHandler.getCurrentPosition()
            autoBookmarkManager.onPause(book.id, currentPos)
        }
    }

    fun skipForward() = audioHandler.skipForward()
    fun skipBack() = audioHandler.skipBack()
    fun skipBackward() = audioHandler.skipBack() // Alias

    fun seekTo(position: Long) = audioHandler.seekTo(position)
    fun seekToFloat(position: Float) = audioHandler.seekTo(position.toLong())

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        audioHandler.setPlaybackSpeed(speed)
        // Save speed to settings for persistence
        viewModelScope.launch {
            settingsRepository.setPlaybackSpeed(speed)
        }
    }

    /**
     * Save current playback speed as a custom preset
     */
    fun saveCurrentSpeedAsPreset(name: String) {
        viewModelScope.launch {
            settingsRepository.saveCustomSpeedPreset(name, _playbackSpeed.value)
        }
    }

    /**
     * Delete a custom speed preset
     */
    fun deleteCustomPreset(name: String) {
        viewModelScope.launch {
            settingsRepository.deleteCustomSpeedPreset(name)
        }
    }

    private fun startProgressSaver() {
        progressSaverJob?.cancel()
        progressSaverJob = viewModelScope.launch {
            while (isActive) {
                if (isPlaying.value) {
                    _currentBook.value?.let { book ->
                        val currentPos = audioHandler.getCurrentPosition()
                        bookDao.updateProgress(book.id, currentPos, System.currentTimeMillis())
                    }
                }
                delay(5000)
            }
        }

        // Start stats tracking (separate job for 30-second intervals)
        statsTrackerJob?.cancel()
        statsTrackerJob = viewModelScope.launch {
            while (isActive) {
                if (isPlaying.value) {
                    // Track listening time every 30 seconds
                    listeningStatsRepository.addListeningTime(30_000L)
                }
                delay(30_000L)
            }
        }
    }

    // Sleep Timer Functions
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes
        _sleepTimerMode.value = SleepTimerMode.MINUTES

        val durationMs = minutes * 60 * 1000L
        _sleepTimerEndTime.value = System.currentTimeMillis() + durationMs
        _sleepTimerRemaining.value = durationMs

        sleepTimerJob = viewModelScope.launch {
            while (isActive) {
                val endTime = _sleepTimerEndTime.value ?: break
                val remaining = endTime - System.currentTimeMillis()

                if (remaining <= 0) {
                    pause()
                    _sleepTimerMinutes.value = null
                    _sleepTimerRemaining.value = 0L
                    _sleepTimerEndTime.value = null
                    _sleepTimerMode.value = null
                    break
                }

                _sleepTimerRemaining.value = remaining
                delay(1000)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = null
        _sleepTimerRemaining.value = 0L
        _sleepTimerEndTime.value = null
        _sleepTimerMode.value = null
    }

    /**
     * Set sleep timer to end at the end of current chapter.
     * Uses special value -1 to indicate "end of chapter" mode.
     */
    fun setSleepTimerEndOfChapter() {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = -1  // Special value for "end of chapter"
        _sleepTimerMode.value = SleepTimerMode.END_OF_CHAPTER

        sleepTimerJob = viewModelScope.launch {
            val book = _currentBook.value ?: return@launch
            val chapters = book.chapters
            if (chapters.isEmpty()) {
                // No chapters - just set 15 min timer as fallback
                setSleepTimer(15)
                return@launch
            }

            // Find current chapter and its end time
            while (isActive) {
                val currentPos = audioHandler.getCurrentPosition()
                val currentChapter = chapters.indexOfLast { currentPos >= it.startMs }

                if (currentChapter >= 0 && currentChapter < chapters.size) {
                    val chapterEndMs = chapters[currentChapter].endMs
                    val remaining = chapterEndMs - currentPos

                    if (remaining > 0) {
                        _sleepTimerRemaining.value = remaining
                        _sleepTimerEndTime.value = System.currentTimeMillis() + remaining

                        // Check every second if we've reached the end of chapter
                        delay(1000)

                        // Re-check position after delay
                        val newPos = audioHandler.getCurrentPosition()
                        if (newPos >= chapterEndMs) {
                            pause()
                            _sleepTimerMinutes.value = null
                            _sleepTimerRemaining.value = 0L
                            _sleepTimerEndTime.value = null
                            _sleepTimerMode.value = null
                            break
                        }
                    } else {
                        // Already at or past chapter end
                        pause()
                        _sleepTimerMinutes.value = null
                        _sleepTimerRemaining.value = 0L
                        _sleepTimerEndTime.value = null
                        _sleepTimerMode.value = null
                        break
                    }
                } else {
                    // No valid chapter found
                    break
                }
            }
        }
    }

    // Bookmark Functions
    fun toggleBookmark() {
        viewModelScope.launch {
            _currentBook.value?.let { book ->
                val currentPos = audioHandler.getCurrentPosition()
                libraryRepository.toggleBookmark(book.id, currentPos)
                // Refresh book to get updated bookmarks
                val updatedBook = bookDao.getBookById(book.id)
                if (updatedBook != null) {
                    _currentBook.value = updatedBook
                }
            }
        }
    }

    /**
     * Add a bookmark with an optional note.
     * This adds the bookmark at the current position and stores the note.
     */
    fun addBookmarkWithNote(note: String) {
        viewModelScope.launch {
            _currentBook.value?.let { book ->
                val currentPos = audioHandler.getCurrentPosition()
                libraryRepository.addBookmarkWithNote(book.id, currentPos, note)
                // Refresh book to get updated bookmarks
                val updatedBook = bookDao.getBookById(book.id)
                if (updatedBook != null) {
                    _currentBook.value = updatedBook
                }
            }
        }
    }

    fun seekToBookmark(positionMs: Long) {
        seekTo(positionMs)
    }

    /**
     * Delete a bookmark at the specified position.
     */
    fun deleteBookmark(positionMs: Long) {
        viewModelScope.launch {
            _currentBook.value?.let { book ->
                libraryRepository.deleteBookmark(book.id, positionMs)
                // Refresh book to get updated bookmarks
                bookDao.getBookById(book.id)?.let { updatedBook ->
                    _currentBook.value = updatedBook
                }
            }
        }
    }

    /**
     * Update or add a note to an existing bookmark.
     */
    fun updateBookmarkNote(positionMs: Long, note: String) {
        viewModelScope.launch {
            _currentBook.value?.let { book ->
                libraryRepository.updateBookmarkNote(book.id, positionMs, note)
                // Refresh book to get updated notes
                bookDao.getBookById(book.id)?.let { updatedBook ->
                    _currentBook.value = updatedBook
                }
            }
        }
    }

    // Chapter Functions
    fun seekToChapter(startMs: Long) {
        seekTo(startMs)
    }

    /**
     * Skip to the next chapter.
     * If there are no real chapters, does nothing.
     */
    fun nextChapter() {
        val book = _currentBook.value ?: return
        val chapters = book.chapters
        if (chapters.isEmpty()) return

        val currentPos = audioHandler.getCurrentPosition()
        val currentChapterIndex = chapters.indexOfLast { currentPos >= it.startMs }

        // Move to next chapter if available
        if (currentChapterIndex < chapters.size - 1) {
            seekTo(chapters[currentChapterIndex + 1].startMs)
        }
    }

    /**
     * Skip to the previous chapter.
     * If we're more than 3 seconds into the current chapter, restart it.
     * Otherwise, go to the actual previous chapter.
     * If there are no real chapters, does nothing.
     */
    fun previousChapter() {
        val book = _currentBook.value ?: return
        val chapters = book.chapters
        if (chapters.isEmpty()) return

        val currentPos = audioHandler.getCurrentPosition()
        val currentChapterIndex = chapters.indexOfLast { currentPos >= it.startMs }

        if (currentChapterIndex >= 0) {
            val chapterStart = chapters[currentChapterIndex].startMs
            val positionInChapter = currentPos - chapterStart

            // If more than 3 seconds into chapter, restart current chapter
            if (positionInChapter > 3000) {
                seekTo(chapterStart)
            } else if (currentChapterIndex > 0) {
                // Otherwise go to previous chapter
                seekTo(chapters[currentChapterIndex - 1].startMs)
            } else {
                // Already at first chapter, just restart it
                seekTo(chapterStart)
            }
        }
    }

    /**
     * Update the current book's finished status (mark as finished/unfinished)
     */
    fun updateBookFinished(isFinished: Boolean) {
        viewModelScope.launch {
            _currentBook.value?.let { book ->
                val progress = if (isFinished) book.duration else book.progress
                bookDao.updateStatus(book.id, isFinished, progress)
                // Update local state
                _currentBook.value = book.copy(isFinished = isFinished, progress = progress)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressSaverJob?.cancel()
        sleepTimerJob?.cancel()
        statsTrackerJob?.cancel()
        autoBookmarkManager.cleanup()
        // Save final progress before clearing
        viewModelScope.launch {
            _currentBook.value?.let { book ->
                val currentPos = audioHandler.getCurrentPosition()
                bookDao.updateProgress(book.id, currentPos, System.currentTimeMillis())
            }
        }
    }

}
