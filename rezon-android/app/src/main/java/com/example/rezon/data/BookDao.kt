package com.example.rezon.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastPlayedTimestamp DESC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE isFinished = 0 ORDER BY lastPlayedTimestamp DESC")
    fun getInProgressBooks(): Flow<List<Book>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBook(book: Book)

    @Update
    suspend fun updateBook(book: Book)

    @Query("UPDATE books SET progress = :pos, lastPlayedTimestamp = :ts WHERE id = :id")
    suspend fun updateProgress(id: String, pos: Long, ts: Long)
}
