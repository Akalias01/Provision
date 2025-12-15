package com.example.rezon.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.rezon.data.AudioServiceHandler
import com.example.rezon.data.Book
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val audioHandler: AudioServiceHandler
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

    fun playBook(book: Book) {
        _currentBook.value = book
        audioHandler.loadBook(book)
        audioHandler.play()
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            audioHandler.pause()
            lastPauseTime = System.currentTimeMillis()
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
}
