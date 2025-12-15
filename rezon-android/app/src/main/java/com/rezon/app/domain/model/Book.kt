package com.rezon.app.domain.model

import java.util.UUID

/**
 * Domain model for an audiobook
 */
data class Book(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String,
    val narrator: String? = null,
    val description: String? = null,
    val coverUrl: String? = null,
    val localCoverPath: String? = null,
    val filePath: String,
    val format: BookFormat,
    val duration: Long = 0L, // Total duration in milliseconds
    val currentPosition: Long = 0L, // Current playback position
    val currentChapterIndex: Int = 0,
    val chapters: List<Chapter> = emptyList(),
    val series: String? = null,
    val seriesIndex: Int? = null,
    val year: Int? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayed: Long? = null,
    val isCompleted: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList()
) {
    val progress: Float
        get() = if (duration > 0) (currentPosition.toFloat() / duration) else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val remainingTime: Long
        get() = duration - currentPosition

    val currentChapter: Chapter?
        get() = chapters.getOrNull(currentChapterIndex)

    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "< 1m"
            }
        }

    val formattedRemainingTime: String
        get() {
            val totalSeconds = remainingTime / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m left"
                minutes > 0 -> "${minutes}m left"
                else -> "< 1m left"
            }
        }
}

/**
 * Book format types
 */
enum class BookFormat {
    AUDIO_MP3,
    AUDIO_M4B,
    AUDIO_M4A,
    AUDIO_FLAC,
    AUDIO_OGG,
    EPUB,
    PDF,
    DOCX,
    DOC
}

/**
 * Chapter within a book
 */
data class Chapter(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val index: Int,
    val startTime: Long, // Milliseconds from start
    val endTime: Long,
    val duration: Long = endTime - startTime
) {
    fun containsPosition(position: Long): Boolean =
        position in startTime until endTime
}

/**
 * Bookmark with optional note
 */
data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val position: Long, // Milliseconds
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val chapterIndex: Int? = null
) {
    fun formatPosition(): String {
        val totalSeconds = position / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}

/**
 * Playback state
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val currentBook: Book? = null,
    val currentChapter: Chapter? = null,
    val sleepTimerRemaining: Long? = null,
    val isSilenceSkippingEnabled: Boolean = false,
    val isVoiceBoostEnabled: Boolean = false
)
