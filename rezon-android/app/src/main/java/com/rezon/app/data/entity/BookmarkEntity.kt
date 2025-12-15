package com.rezon.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rezon.app.domain.model.Bookmark

/**
 * Room entity for bookmarks with foreign key relationship to books
 */
@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["bookId"])]
)
data class BookmarkEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val position: Long,
    val note: String?,
    val createdAt: Long,
    val chapterIndex: Int?
) {
    /**
     * Convert entity to domain model
     */
    fun toDomain(): Bookmark = Bookmark(
        id = id,
        position = position,
        note = note,
        createdAt = createdAt,
        chapterIndex = chapterIndex
    )

    companion object {
        /**
         * Create entity from domain model
         */
        fun fromDomain(bookmark: Bookmark, bookId: String): BookmarkEntity = BookmarkEntity(
            id = bookmark.id,
            bookId = bookId,
            position = bookmark.position,
            note = bookmark.note,
            createdAt = bookmark.createdAt,
            chapterIndex = bookmark.chapterIndex
        )
    }
}
