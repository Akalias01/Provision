package com.mossglen.lithos.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Book::class, TorrentDownloadEntity::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun torrentDownloadDao(): TorrentDownloadDao

    companion object {
        /**
         * Migration from version 5 to 6: Add torrent_downloads table
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS torrent_downloads (
                        id TEXT NOT NULL PRIMARY KEY,
                        source TEXT NOT NULL,
                        sourceType TEXT NOT NULL,
                        name TEXT NOT NULL,
                        progress REAL NOT NULL DEFAULT 0,
                        totalSize INTEGER NOT NULL DEFAULT 0,
                        downloadedSize INTEGER NOT NULL DEFAULT 0,
                        state TEXT NOT NULL DEFAULT 'DOWNLOADING',
                        addedAt INTEGER NOT NULL DEFAULT 0,
                        lastActiveAt INTEGER NOT NULL DEFAULT 0,
                        isComplete INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }
    }
}
