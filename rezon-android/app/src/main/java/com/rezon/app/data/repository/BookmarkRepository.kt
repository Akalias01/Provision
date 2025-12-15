package com.rezon.app.data.repository

import com.rezon.app.data.dao.BookmarkDao
import com.rezon.app.data.entity.BookmarkEntity
import com.rezon.app.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for bookmark operations
 */
@Singleton
class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao
) {

    /**
     * Get all bookmarks for a book as Flow (reactive updates)
     */
    fun getBookmarksForBook(bookId: String): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksForBook(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Add a new bookmark
     */
    suspend fun addBookmark(bookId: String, bookmark: Bookmark) {
        val entity = BookmarkEntity.fromDomain(bookmark, bookId)
        bookmarkDao.insertBookmark(entity)
    }

    /**
     * Update bookmark (e.g., edit note)
     */
    suspend fun updateBookmark(bookId: String, bookmark: Bookmark) {
        val entity = BookmarkEntity.fromDomain(bookmark, bookId)
        bookmarkDao.updateBookmark(entity)
    }

    /**
     * Delete a bookmark by ID
     */
    suspend fun deleteBookmark(bookmarkId: String) {
        bookmarkDao.deleteBookmarkById(bookmarkId)
    }

    /**
     * Delete all bookmarks for a book
     */
    suspend fun deleteAllBookmarks(bookId: String) {
        bookmarkDao.deleteAllBookmarksForBook(bookId)
    }

    /**
     * Get bookmark count for a book
     */
    suspend fun getBookmarkCount(bookId: String): Int {
        return bookmarkDao.getBookmarkCount(bookId)
    }
}
