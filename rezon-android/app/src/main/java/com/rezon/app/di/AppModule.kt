package com.rezon.app.di

import android.content.Context
import androidx.room.Room
import com.rezon.app.data.dao.BookmarkDao
import com.rezon.app.data.database.AppDatabase
import com.rezon.app.data.repository.BookmarkRepository
import com.rezon.app.service.PlaybackController
import com.rezon.app.service.TorrentManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for app-wide dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePlaybackController(
        @ApplicationContext context: Context
    ): PlaybackController {
        return PlaybackController(context)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideBookmarkDao(database: AppDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    @Singleton
    fun provideBookmarkRepository(bookmarkDao: BookmarkDao): BookmarkRepository {
        return BookmarkRepository(bookmarkDao)
    }

    @Provides
    @Singleton
    fun provideTorrentManager(
        @ApplicationContext context: Context
    ): TorrentManager {
        return TorrentManager(context)
    }
}
