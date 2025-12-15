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

    // Current book being played
    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook = _currentBook.asStateFlow()

    // Demo book for when no book is selected
    val demoBook = Book(
        id = "demo",
        title = "The Martian",
        author = "Andy Weir",
        coverUrl = "https://upload.wikimedia.org/wikipedia/en/2/21/The_Martian_%28Weir_novel%29.jpg",
        filePath = "asset:///demo_audio.mp3",
        duration = 0L
    )

    val isServiceConnected = audioHandler.isServiceConnected
    val isPlaying = audioHandler.isPlaying
    val playbackSpeed = audioHandler.playbackSpeed
    val currentPosition = audioHandler.currentPosition
    val duration = audioHandler.duration

    private var lastPauseTime: Long = 0L
    private var progressSaveJob: Job? = null

    fun playBook(book: Book) {
        _currentBook.value = book
        audioHandler.loadBook(book)

        // Seek to saved progress if available
        if (book.progress > 0) {
            audioHandler.seekTo(book.progress)
        }

        audioHandler.play()
        startProgressSaving()
    }

    private fun startProgressSaving() {
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(5000) // Save every 5 seconds
                saveCurrentProgress()
            }
        }
    }

    private fun saveCurrentProgress() {
        val book = _currentBook.value ?: return
        val position = audioHandler.getCurrentPosition()
        if (position > 0) {
            viewModelScope.launch {
                bookDao.updateProgress(
                    id = book.id,
                    pos = position,
                    ts = System.currentTimeMillis()
                )
            }
        }
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            audioHandler.pause()
            lastPauseTime = System.currentTimeMillis()
            saveCurrentProgress() // Save progress when pausing
            progressSaveJob?.cancel()
        } else {
            val now = System.currentTimeMillis()
            val pauseDuration = now - lastPauseTime
            val currentPos = audioHandler.getCurrentPosition()

            // Smart Resume Logic
            var seekPos = currentPos
            if (pauseDuration > 60 * 60 * 1000) {
                seekPos = (currentPos - 30_000).coerceAtLeast(0)
            } else if (pauseDuration > 5 * 60 * 1000) {
                seekPos = (currentPos - 10_000).coerceAtLeast(0)
            }

            if (seekPos != currentPos) {
                audioHandler.seekTo(seekPos)
            }
            audioHandler.play()
            startProgressSaving()
        }
    }

    fun skipForward() {
        if (!isServiceConnected.value) return
        val newPos = audioHandler.getCurrentPosition() + 30_000
        audioHandler.seekTo(newPos)
    }

    fun skipBackward() {
        if (!isServiceConnected.value) return
        val newPos = (audioHandler.getCurrentPosition() - 10_000).coerceAtLeast(0)
        audioHandler.seekTo(newPos)
    }

    fun cyclePlaybackSpeed() {
        if (!isServiceConnected.value) return
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        val current = playbackSpeed.value
        val nextIndex = speeds.indexOfFirst { it > current }
        val newSpeed = if (nextIndex != -1) speeds[nextIndex] else speeds[0]
        audioHandler.setSpeed(newSpeed)
    }

    override fun onCleared() {
        super.onCleared()
        saveCurrentProgress() // Save progress when ViewModel is destroyed
        progressSaveJob?.cancel()
    }
}
