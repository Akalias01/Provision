package com.example.rezon.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rezon.data.AudioServiceHandler
import com.example.rezon.data.Book
import com.example.rezon.data.ChapterMarker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val audioHandler: AudioServiceHandler
) : ViewModel() {

    // Demo Data
    val demoBook = Book(
        id = "1",
        title = "The Martian",
        author = "Andy Weir",
        coverUrl = "https://upload.wikimedia.org/wikipedia/en/2/21/The_Martian_%28Weir_novel%29.jpg",
        filePath = "asset:///demo_audio.mp3",
        synopsis = "Six days ago, astronaut Mark Watney became one of the first people to walk on Mars. Now, he's sure he'll be the first person to die there.",
        seriesInfo = "The Martian: Standalone Novel",
        chapterMarkers = listOf(
            ChapterMarker("Chapter 1", 0),
            ChapterMarker("Chapter 2", 300000),
            ChapterMarker("Chapter 3", 600000)
        )
    )

    val isServiceConnected = audioHandler.isServiceConnected
    val isPlaying = audioHandler.isPlaying
    val playbackSpeed = audioHandler.playbackSpeed

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private var lastPauseTime: Long = 0L

    init {
        // This will now queue correctly if service isn't ready
        audioHandler.loadBook(demoBook)
        startPositionTracker()
    }

    private fun startPositionTracker() {
        viewModelScope.launch {
            while (isActive) {
                // Only poll if connected
                if (isServiceConnected.value) {
                    _currentPosition.value = audioHandler.getCurrentPosition()
                }
                delay(1000)
            }
        }
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
