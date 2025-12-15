package com.example.rezon.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Book::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new metadata columns (old schema)
                database.execSQL("ALTER TABLE books ADD COLUMN description TEXT")
                database.execSQL("ALTER TABLE books ADD COLUMN series TEXT")
                database.execSQL("ALTER TABLE books ADD COLUMN categories TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new metadata fields (synopsis, seriesInfo)
                database.execSQL("ALTER TABLE books ADD COLUMN synopsis TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE books ADD COLUMN seriesInfo TEXT NOT NULL DEFAULT ''")
                // Remove old columns by recreating table
                database.execSQL("""
                    CREATE TABLE books_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        author TEXT NOT NULL,
                        coverUrl TEXT,
                        filePath TEXT NOT NULL,
                        duration INTEGER NOT NULL,
                        progress INTEGER NOT NULL DEFAULT 0,
                        lastPlayedTimestamp INTEGER NOT NULL DEFAULT 0,
                        format TEXT NOT NULL DEFAULT 'AUDIO',
                        isFinished INTEGER NOT NULL DEFAULT 0,
                        synopsis TEXT NOT NULL DEFAULT '',
                        seriesInfo TEXT NOT NULL DEFAULT ''
                    )
                """)
                database.execSQL("""
                    INSERT INTO books_new (id, title, author, coverUrl, filePath, duration, progress, lastPlayedTimestamp, format, isFinished, synopsis, seriesInfo)
                    SELECT id, title, author, coverUrl, filePath, duration, progress, lastPlayedTimestamp, format, isFinished, COALESCE(description, ''), COALESCE(series, '')
                    FROM books
                """)
                database.execSQL("DROP TABLE books")
                database.execSQL("ALTER TABLE books_new RENAME TO books")
            }
        }
    }
}
