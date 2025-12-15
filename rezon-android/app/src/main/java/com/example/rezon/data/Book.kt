package com.example.rezon.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val filePath: String,
    val duration: Long,
    val progress: Long = 0,
    val lastPlayedTimestamp: Long = 0,
    val format: String = "AUDIO",
    val isFinished: Boolean = false,
    // New Fields for Metadata
    val synopsis: String = "",
    val seriesInfo: String = ""
) {
    fun progressPercent(): Float {
        if (duration == 0L) return 0f
        return progress.toFloat() / duration.toFloat()
    }
}
