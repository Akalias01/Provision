package com.rezon.app.presentation.viewmodel

import android.app.Activity
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rezon.app.domain.model.Book
import com.rezon.app.domain.model.Chapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val error: String? = null
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
    // TODO: Inject PlaybackRepository
    // TODO: Inject AudioServiceController
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Timestamp of last pause for Smart Resume
    private var lastPauseTime: Long? = null

    // Skip amounts (configurable)
    private var skipBackwardMs = 10_000L // 10 seconds
    private var skipForwardMs = 30_000L  // 30 seconds

    // Playback speeds
    private val playbackSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    /**
     * Load book data and prepare for playback
     */
    fun loadBook(bookId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // TODO: Load from repository
            // For now, create mock data
            val mockBook = Book(
                id = bookId,
                title = "Sample Audiobook",
                author = "Sample Author",
                filePath = "",
                format = com.rezon.app.domain.model.BookFormat.AUDIO_MP3,
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
            // TODO: Pause via audio service
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
                    _uiState.update { it.copy(currentPosition = newPosition) }
                    // TODO: Seek via audio service
                }
            }
            // TODO: Play via audio service
        }

        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    /**
     * Skip backward (default 10s)
     */
    fun skipBackward() {
        _uiState.update {
            val newPosition = (it.currentPosition - skipBackwardMs).coerceAtLeast(0)
            it.copy(currentPosition = newPosition)
        }
        // TODO: Seek via audio service
    }

    /**
     * Skip forward (default 30s)
     */
    fun skipForward() {
        _uiState.update {
            val newPosition = (it.currentPosition + skipForwardMs).coerceAtMost(it.duration)
            it.copy(currentPosition = newPosition)
        }
        // TODO: Seek via audio service
    }

    /**
     * Seek to specific position
     */
    fun seekTo(positionMs: Long) {
        _uiState.update {
            it.copy(currentPosition = positionMs.coerceIn(0, it.duration))
        }
        // TODO: Seek via audio service
    }

    /**
     * Go to previous chapter
     */
    fun previousChapter() {
        _uiState.update { state ->
            val newIndex = (state.currentChapterIndex - 1).coerceAtLeast(0)
            val chapter = state.book?.chapters?.getOrNull(newIndex)
            state.copy(
                currentChapterIndex = newIndex,
                currentChapter = chapter,
                currentPosition = chapter?.startTime ?: state.currentPosition
            )
        }
        // TODO: Seek via audio service
    }

    /**
     * Go to next chapter
     */
    fun nextChapter() {
        _uiState.update { state ->
            val totalChapters = state.book?.chapters?.size ?: 0
            val newIndex = (state.currentChapterIndex + 1).coerceAtMost(totalChapters - 1)
            val chapter = state.book?.chapters?.getOrNull(newIndex)
            state.copy(
                currentChapterIndex = newIndex,
                currentChapter = chapter,
                currentPosition = chapter?.startTime ?: state.currentPosition
            )
        }
        // TODO: Seek via audio service
    }

    /**
     * Cycle through playback speeds
     */
    fun cyclePlaybackSpeed() {
        _uiState.update { state ->
            val currentIndex = playbackSpeeds.indexOf(state.playbackSpeed)
            val nextIndex = (currentIndex + 1) % playbackSpeeds.size
            state.copy(playbackSpeed = playbackSpeeds[nextIndex])
        }
        // TODO: Set speed via audio service
    }

    /**
     * Set specific playback speed
     */
    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
        // TODO: Set speed via audio service
    }

    /**
     * Set volume (0.0 to 1.0)
     */
    fun setVolume(volume: Float) {
        _uiState.update { it.copy(volume = volume.coerceIn(0f, 1f)) }
        // TODO: Set volume via audio service
    }

    /**
     * Set brightness (0.0 to 1.0)
     * Note: This requires the Activity context
     */
    fun setBrightness(brightness: Float) {
        _uiState.update { it.copy(brightness = brightness.coerceIn(0f, 1f)) }
        // Brightness is set in the composable via LocalContext
    }

    /**
     * Toggle silence skipping
     */
    fun toggleSilenceSkipping() {
        _uiState.update { it.copy(isSilenceSkippingEnabled = !it.isSilenceSkippingEnabled) }
        // TODO: Configure via audio service
    }

    /**
     * Toggle voice boost EQ
     */
    fun toggleVoiceBoost() {
        _uiState.update { it.copy(isVoiceBoostEnabled = !it.isVoiceBoostEnabled) }
        // TODO: Configure via audio service
    }

    /**
     * Start sleep timer
     */
    fun startSleepTimer(durationMs: Long) {
        _uiState.update { it.copy(sleepTimerRemaining = durationMs) }
        // TODO: Start countdown
    }

    /**
     * Cancel sleep timer
     */
    fun cancelSleepTimer() {
        _uiState.update { it.copy(sleepTimerRemaining = null) }
    }

    /**
     * Update skip amounts from settings
     */
    fun updateSkipAmounts(backwardMs: Long, forwardMs: Long) {
        skipBackwardMs = backwardMs
        skipForwardMs = forwardMs
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
