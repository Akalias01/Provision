package com.rezon.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rezon.app.data.dao.BookmarkDao
import com.rezon.app.data.entity.BookEntity
import com.rezon.app.data.entity.BookmarkEntity

/**
 * Room database for REZON app
 */
@Database(
    entities = [
        BookEntity::class,
        BookmarkEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        const val DATABASE_NAME = "rezon_database"
    }
}
