package com.rezon.app.domain.model

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String? = null,
    val localCoverPath: String? = null,
    val duration: Long = 0L, // Duration in milliseconds
    val currentPosition: Long = 0L, // Current playback position in milliseconds
    val progress: Float = 0f, // Progress 0.0 to 1.0
    val isCompleted: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayed: Long? = null,
    val year: Int? = null,
    val filePath: String? = null,
    val chapters: List<Chapter> = emptyList()
) {
    val formattedDuration: String
        get() {
            val hours = duration / 3600000
            val minutes = (duration % 3600000) / 60000
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }

    val formattedRemainingTime: String
        get() {
            val remaining = duration - currentPosition
            val hours = remaining / 3600000
            val minutes = (remaining % 3600000) / 60000
            return if (hours > 0) "${hours}h ${minutes}m left" else "${minutes}m left"
        }
}

data class Chapter(
    val id: String,
    val title: String,
    val startPosition: Long, // Start position in milliseconds
    val duration: Long // Duration in milliseconds
)
