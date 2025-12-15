package com.example.rezon.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Keep ChapterMarker for chapter navigation (will be stored separately or embedded later)
data class ChapterMarker(
    val title: String,
    val startMs: Long
)

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: String, // Usually the file path
    val title: String,
    val author: String,
    val coverUrl: String?, // Can be null if no cover found
    val filePath: String,
    val duration: Long,
    val progress: Long = 0,
    val lastPlayedTimestamp: Long = 0,
    val format: String = "AUDIO", // AUDIO, EPUB, PDF
    val isFinished: Boolean = false
) {
    // Helper for UI progress calculation
    fun progressPercent(): Float {
        if (duration == 0L) return 0f
        return progress.toFloat() / duration.toFloat()
    }
}
