package com.rezon.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rezon.app.data.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for bookmark operations
 */
@Dao
interface BookmarkDao {

    /**
     * Get all bookmarks for a specific book, ordered by position
     */
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY position ASC")
    fun getBookmarksForBook(bookId: String): Flow<List<BookmarkEntity>>

    /**
     * Get a specific bookmark by ID
     */
    @Query("SELECT * FROM bookmarks WHERE id = :bookmarkId")
    suspend fun getBookmarkById(bookmarkId: String): BookmarkEntity?

    /**
     * Insert a new bookmark
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    /**
     * Update an existing bookmark (e.g., edit note)
     */
    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    /**
     * Delete a bookmark
     */
    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    /**
     * Delete bookmark by ID
     */
    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmarkById(bookmarkId: String)

    /**
     * Delete all bookmarks for a book
     */
    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteAllBookmarksForBook(bookId: String)

    /**
     * Get bookmark count for a book
     */
    @Query("SELECT COUNT(*) FROM bookmarks WHERE bookId = :bookId")
    suspend fun getBookmarkCount(bookId: String): Int
}
