package com.example.rezon.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Book::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new metadata columns
                database.execSQL("ALTER TABLE books ADD COLUMN description TEXT")
                database.execSQL("ALTER TABLE books ADD COLUMN series TEXT")
                database.execSQL("ALTER TABLE books ADD COLUMN categories TEXT")
            }
        }
    }
}
