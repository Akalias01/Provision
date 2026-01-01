package com.mossglen.lithos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Entity for persisting torrent downloads across app restarts/reinstalls.
 * Stores the magnet URI or torrent file path so downloads can be resumed.
 */
@Entity(tableName = "torrent_downloads")
data class TorrentDownloadEntity(
    @PrimaryKey
    val id: String,

    /** The magnet URI or torrent file path */
    val source: String,

    /** Type of source: MAGNET or FILE */
    val sourceType: String,

    /** Display name of the torrent */
    val name: String,

    /** Last known progress (0.0 - 1.0) */
    val progress: Float = 0f,

    /** Total size in bytes */
    val totalSize: Long = 0L,

    /** Downloaded size in bytes */
    val downloadedSize: Long = 0L,

    /** Current state: DOWNLOADING, PAUSED, SEEDING, FINISHED, ERROR */
    val state: String = "DOWNLOADING",

    /** Timestamp when download was added */
    val addedAt: Long = System.currentTimeMillis(),

    /** Timestamp of last activity */
    val lastActiveAt: Long = System.currentTimeMillis(),

    /** Whether download is complete */
    val isComplete: Boolean = false
) {
    companion object {
        const val SOURCE_TYPE_MAGNET = "MAGNET"
        const val SOURCE_TYPE_FILE = "FILE"
    }
}

@Dao
interface TorrentDownloadDao {
    @Query("SELECT * FROM torrent_downloads ORDER BY addedAt DESC")
    fun getAllDownloads(): Flow<List<TorrentDownloadEntity>>

    @Query("SELECT * FROM torrent_downloads WHERE isComplete = 0 ORDER BY addedAt DESC")
    fun getActiveDownloads(): Flow<List<TorrentDownloadEntity>>

    @Query("SELECT * FROM torrent_downloads WHERE id = :id")
    suspend fun getDownload(id: String): TorrentDownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: TorrentDownloadEntity)

    @Update
    suspend fun update(download: TorrentDownloadEntity)

    @Delete
    suspend fun delete(download: TorrentDownloadEntity)

    @Query("DELETE FROM torrent_downloads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE torrent_downloads SET progress = :progress, downloadedSize = :downloadedSize, state = :state, lastActiveAt = :lastActiveAt WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, downloadedSize: Long, state: String, lastActiveAt: Long = System.currentTimeMillis())

    @Query("UPDATE torrent_downloads SET isComplete = 1, state = 'FINISHED' WHERE id = :id")
    suspend fun markComplete(id: String)

    @Query("SELECT * FROM torrent_downloads WHERE isComplete = 0")
    suspend fun getActiveDownloadsList(): List<TorrentDownloadEntity>
}
