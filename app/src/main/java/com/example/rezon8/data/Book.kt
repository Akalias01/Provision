package com.mossglen.reverie.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Chapter(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val filePath: String? = null  // Optional: for split-chapter audiobooks where each chapter is a separate file
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromChapterList(chapters: List<Chapter>?): String {
        return gson.toJson(chapters ?: emptyList<Chapter>())
    }

    @TypeConverter
    fun toChapterList(json: String?): List<Chapter> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<Chapter>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromBookmarkList(bookmarks: List<Long>?): String {
        return gson.toJson(bookmarks ?: emptyList<Long>())
    }

    @TypeConverter
    fun toBookmarkList(json: String?): List<Long> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    @TypeConverter
    fun fromBookmarkNotesMap(notes: Map<Long, String>?): String {
        return gson.toJson(notes ?: emptyMap<Long, String>())
    }

    @TypeConverter
    fun toBookmarkNotesMap(json: String?): Map<Long, String> {
        if (json.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<Long, String>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }
}

@Entity(tableName = "books")
@TypeConverters(Converters::class)
data class Book(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val filePath: String,
    val duration: Long,
    val progress: Long = 0,
    val isFinished: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,

    // Advanced Features
    val isKidsApproved: Boolean = false,
    val format: String = "AUDIO",
    val synopsis: String = "",
    val seriesInfo: String = "",
    val genre: String = "",  // Genre/category (e.g., "Fiction", "Thriller", "Science Fiction")
    val narrator: String = "",  // Narrator/reader (e.g., "Stephen Fry")

    // Chapters & Bookmarks
    val chapters: List<Chapter> = emptyList(),
    val bookmarks: List<Long> = emptyList(),
    val bookmarkNotes: Map<Long, String> = emptyMap()
) {
    fun progressPercent(): Float {
        return if (duration > 0) (progress.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    }

    /**
     * Check if metadata is missing or incomplete.
     * Returns true if any of the following are missing:
     * - Author is unknown/empty
     * - Cover image is missing
     * - Synopsis is empty
     */
    val hasIncompleteMetadata: Boolean
        get() = author.isBlank() ||
                author.equals("Unknown Author", ignoreCase = true) ||
                author.equals("Unknown", ignoreCase = true) ||
                coverUrl.isNullOrBlank() ||
                synopsis.isBlank()

    /**
     * Get a list of what metadata is missing.
     */
    val missingMetadataFields: List<String>
        get() = buildList {
            if (author.isBlank() || author.equals("Unknown Author", ignoreCase = true) || author.equals("Unknown", ignoreCase = true)) {
                add("Author")
            }
            if (coverUrl.isNullOrBlank()) {
                add("Cover")
            }
            if (synopsis.isBlank()) {
                add("Synopsis")
            }
        }
}
