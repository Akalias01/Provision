package com.mossglen.lithos.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AutoBookmarkManager
 *
 * Handles the auto-bookmark feature which creates a bookmark automatically
 * when playback is paused for more than 30 seconds.
 */
@Singleton
class AutoBookmarkManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository
) {
    companion object {
        private const val TAG = "AutoBookmarkManager"
        private const val PAUSE_THRESHOLD_MS = 30_000L // 30 seconds
        private const val DUPLICATE_THRESHOLD_MS = 5_000L // 5 seconds - don't create if bookmark exists within 5 seconds
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pauseTimerJob: Job? = null
    private var lastPausePosition: Long? = null
    private var currentBookId: String? = null

    /**
     * Called when playback is paused.
     * Starts a timer that will create a bookmark after 30 seconds if still paused.
     */
    fun onPause(bookId: String, positionMs: Long) {
        scope.launch {
            // Check if feature is enabled
            val isEnabled = settingsRepository.autoBookmarkEnabled.first()
            if (!isEnabled) {
                Log.d(TAG, "Auto-bookmark disabled, skipping")
                return@launch
            }

            // Cancel any existing timer
            pauseTimerJob?.cancel()

            // Store current state
            currentBookId = bookId
            lastPausePosition = positionMs

            Log.d(TAG, "Pause detected at ${formatTimestamp(positionMs)}, starting 30-second timer")

            // Start countdown timer
            pauseTimerJob = scope.launch {
                delay(PAUSE_THRESHOLD_MS)

                if (isActive) {
                    // Still paused after 30 seconds - create the bookmark
                    createAutoBookmark(bookId, positionMs)
                }
            }
        }
    }

    /**
     * Called when playback is resumed.
     * Cancels the auto-bookmark timer.
     */
    fun onResume() {
        Log.d(TAG, "Resume detected, cancelling auto-bookmark timer")
        pauseTimerJob?.cancel()
        pauseTimerJob = null
        lastPausePosition = null
        currentBookId = null
    }

    /**
     * Creates an auto-bookmark with a timestamp label.
     */
    private suspend fun createAutoBookmark(bookId: String, positionMs: Long) {
        try {
            // Get the current book
            val book = libraryRepository.getBookById(bookId)
            if (book == null) {
                Log.w(TAG, "Book not found: $bookId")
                return
            }

            // Check if a bookmark already exists at this position (within 5 seconds)
            val existingBookmark = book.bookmarks.find {
                kotlin.math.abs(it - positionMs) < DUPLICATE_THRESHOLD_MS
            }

            if (existingBookmark != null) {
                Log.d(TAG, "Bookmark already exists near position ${formatTimestamp(positionMs)}, skipping")
                return
            }

            // Create the bookmark with a descriptive note
            val timestamp = formatTimestamp(positionMs)
            val note = "Auto-bookmark at $timestamp"

            Log.d(TAG, "Creating auto-bookmark at $timestamp for book: ${book.title}")
            libraryRepository.addBookmarkWithNote(bookId, positionMs, note)

            Log.d(TAG, "Auto-bookmark created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create auto-bookmark", e)
        }
    }

    /**
     * Formats milliseconds into a human-readable timestamp (H:MM:SS or M:SS)
     */
    private fun formatTimestamp(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    /**
     * Clean up resources when no longer needed
     */
    fun cleanup() {
        pauseTimerJob?.cancel()
        pauseTimerJob = null
        lastPausePosition = null
        currentBookId = null
    }
}
