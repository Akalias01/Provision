package com.mossglen.lithos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books")
    suspend fun getAllBooksDirect(): List<Book>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBook(book: Book)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteById(bookId: String)

    @Query("DELETE FROM books")
    suspend fun clearAll()

    @Query("UPDATE books SET progress = :pos, lastPlayedTimestamp = :ts WHERE id = :id")
    suspend fun updateProgress(id: String, pos: Long, ts: Long)

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: String): Book?

    @Update
    suspend fun updateBook(book: Book)

    @Query("UPDATE books SET bookmarks = :bookmarks WHERE id = :id")
    suspend fun updateBookmarks(id: String, bookmarks: String)

    @Query("UPDATE books SET bookmarkNotes = :bookmarkNotes WHERE id = :id")
    suspend fun updateBookmarkNotes(id: String, bookmarkNotes: String)

    @Query("UPDATE books SET chapters = :chapters WHERE id = :id")
    suspend fun updateChapters(id: String, chapters: String)

    @Query("UPDATE books SET title = :title, author = :author WHERE id = :id")
    suspend fun updateMetadata(id: String, title: String, author: String)

    @Query("UPDATE books SET isFinished = :isFinished, progress = :progress WHERE id = :id")
    suspend fun updateStatus(id: String, isFinished: Boolean, progress: Long)

    @Query("SELECT * FROM books WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 1")
    fun getMostRecentBook(): Flow<Book?>

    @Query("SELECT * FROM books WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 1")
    suspend fun getMostRecentBookDirect(): Book?

    // Series-related queries
    @Query("SELECT * FROM books WHERE seriesInfo != '' ORDER BY seriesInfo, title")
    fun getBooksInSeries(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE seriesInfo != '' ORDER BY seriesInfo, title")
    suspend fun getBooksInSeriesDirect(): List<Book>

    @Query("SELECT DISTINCT seriesInfo FROM books WHERE seriesInfo != '' ORDER BY seriesInfo")
    suspend fun getAllSeriesNames(): List<String>

    @Query("UPDATE books SET seriesInfo = :seriesInfo WHERE id = :bookId")
    suspend fun updateSeriesInfo(bookId: String, seriesInfo: String)

    @Query("UPDATE books SET seriesInfo = :newSeriesInfo WHERE seriesInfo = :oldSeriesInfo")
    suspend fun renameSeriesForAllBooks(oldSeriesInfo: String, newSeriesInfo: String)
}
