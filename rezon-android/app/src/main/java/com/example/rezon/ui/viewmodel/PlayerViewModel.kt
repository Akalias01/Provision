package com.example.rezon.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rezon.data.AudioServiceHandler
import com.example.rezon.data.Book
import com.example.rezon.data.BookDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val audioHandler: AudioServiceHandler,
    private val bookDao: BookDao
) : ViewModel() {

    // Current Book State
    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook = _currentBook.asStateFlow()

    // Use the demo book as a fallback only if DB is empty/null
    val demoBook = Book(
        id = "demo", title = "No Book Selected", author = "Select from Library",
        coverUrl = null, filePath = "", duration = 1000
    )

    val isPlaying = audioHandler.isPlaying
    val playbackSpeed = audioHandler.playbackSpeed

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(1L)
    val duration = _duration.asStateFlow()

    // Sleep Timer
    private var sleepTimerJob: Job? = null

    init {
        startPositionTracker()
    }

    fun playBook(book: Book) {
        _currentBook.value = book
        audioHandler.loadBook(book)

        // Resume from last saved position
        if (book.progress > 0) {
            audioHandler.seekTo(book.progress)
        }
        audioHandler.play()
    }

    private fun startPositionTracker() {
        viewModelScope.launch {
            while (isActive) {
                if (isPlaying.value) {
                    val pos = audioHandler.getCurrentPosition()
                    _currentPosition.value = pos
                    _duration.value = audioHandler.getDuration()

                    // Save to DB every 5 seconds
                    _currentBook.value?.let { book ->
                        if (pos > 0) {
                            bookDao.updateProgress(book.id, pos, System.currentTimeMillis())
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            audioHandler.pause()
            // Immediate save on pause
            saveCurrentProgress()
        } else {
            audioHandler.play()
        }
    }

    private fun saveCurrentProgress() {
        viewModelScope.launch {
            _currentBook.value?.let { book ->
                bookDao.updateProgress(book.id, audioHandler.getCurrentPosition(), System.currentTimeMillis())
            }
        }
    }

    fun skipForward() {
        val newPos = audioHandler.getCurrentPosition() + 30_000
        audioHandler.seekTo(newPos)
        saveCurrentProgress()
    }

    fun skipBackward() {
        val newPos = (audioHandler.getCurrentPosition() - 10_000).coerceAtLeast(0)
        audioHandler.seekTo(newPos)
        saveCurrentProgress()
    }

    fun cyclePlaybackSpeed() {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        val current = playbackSpeed.value
        val nextIndex = speeds.indexOfFirst { it > current }
        val newSpeed = if (nextIndex != -1) speeds[nextIndex] else speeds[0]
        audioHandler.setSpeed(newSpeed)
    }

    // FEATURE: Sleep Timer
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60 * 1000L)
                if (isPlaying.value) {
                    togglePlayPause()
                }
            }
        }
    }
}
