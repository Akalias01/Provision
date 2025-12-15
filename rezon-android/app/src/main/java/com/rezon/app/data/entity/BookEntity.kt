package com.rezon.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rezon.app.domain.model.Book
import com.rezon.app.domain.model.BookFormat

/**
 * Room entity for audiobooks
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val narrator: String?,
    val description: String?,
    val coverUrl: String?,
    val localCoverPath: String?,
    val filePath: String,
    val format: String,
    val duration: Long,
    val currentPosition: Long,
    val currentChapterIndex: Int,
    val series: String?,
    val seriesIndex: Int?,
    val year: Int?,
    val dateAdded: Long,
    val lastPlayed: Long?,
    val isCompleted: Boolean
) {
    /**
     * Convert entity to domain model (without chapters and bookmarks - load separately)
     */
    fun toDomain(): Book = Book(
        id = id,
        title = title,
        author = author,
        narrator = narrator,
        description = description,
        coverUrl = coverUrl,
        localCoverPath = localCoverPath,
        filePath = filePath,
        format = BookFormat.valueOf(format),
        duration = duration,
        currentPosition = currentPosition,
        currentChapterIndex = currentChapterIndex,
        series = series,
        seriesIndex = seriesIndex,
        year = year,
        dateAdded = dateAdded,
        lastPlayed = lastPlayed,
        isCompleted = isCompleted
    )

    companion object {
        /**
         * Create entity from domain model
         */
        fun fromDomain(book: Book): BookEntity = BookEntity(
            id = book.id,
            title = book.title,
            author = book.author,
            narrator = book.narrator,
            description = book.description,
            coverUrl = book.coverUrl,
            localCoverPath = book.localCoverPath,
            filePath = book.filePath,
            format = book.format.name,
            duration = book.duration,
            currentPosition = book.currentPosition,
            currentChapterIndex = book.currentChapterIndex,
            series = book.series,
            seriesIndex = book.seriesIndex,
            year = book.year,
            dateAdded = book.dateAdded,
            lastPlayed = book.lastPlayed,
            isCompleted = book.isCompleted
        )
    }
}
