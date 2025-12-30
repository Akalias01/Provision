package com.mossglen.reverie.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.room.Room
import androidx.work.WorkManager
import com.mossglen.reverie.data.*
import com.mossglen.reverie.data.audio.AudioSplitter
import com.mossglen.reverie.data.cloud.CloudSyncRepository
import com.mossglen.reverie.data.cloud.DropboxManager
import com.mossglen.reverie.data.cloud.GoogleDriveManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "reverie.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideBookDao(db: AppDatabase): BookDao = db.bookDao()

    @Provides
    @Singleton
    fun provideLibraryRepository(
        @ApplicationContext context: Context,
        bookDao: BookDao,
        metadataRepository: MetadataRepository,
        torrentManager: dagger.Lazy<TorrentManager>
    ): LibraryRepository {
        return LibraryRepository(context, bookDao, metadataRepository, torrentManager)
    }

    @Provides
    @Singleton
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer {
        // Audio attributes for audiobooks (speech content)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        return ExoPlayer.Builder(context)
            .setSeekForwardIncrementMs(30_000)  // 30 seconds forward
            .setSeekBackIncrementMs(10_000)     // 10 seconds back
            .setAudioAttributes(audioAttributes, false) // false = AudioHandler manages focus
            .setHandleAudioBecomingNoisy(true) // Pause when headphones unplugged
            .build()
    }

    @Provides
    @Singleton
    fun provideTorrentManager(
        @ApplicationContext context: Context,
        libraryRepository: LibraryRepository,
        metadataRepository: MetadataRepository,
        settingsRepository: SettingsRepository
    ): TorrentManager {
        return TorrentManager(context, libraryRepository, metadataRepository, settingsRepository)
    }

    @Provides
    @Singleton
    fun provideAudioEffectManager(
        @ApplicationContext context: Context,
        exoPlayer: ExoPlayer
    ): AudioEffectManager {
        return AudioEffectManager(context, exoPlayer)
    }

    @Provides
    @Singleton
    fun provideDatabaseBackupManager(
        @ApplicationContext context: Context,
        database: AppDatabase
    ): DatabaseBackupManager {
        return DatabaseBackupManager(context, database)
    }

    @Provides
    @Singleton
    fun provideGoogleDriveManager(
        @ApplicationContext context: Context
    ): GoogleDriveManager {
        return GoogleDriveManager(context)
    }

    @Provides
    @Singleton
    fun provideDropboxManager(
        @ApplicationContext context: Context
    ): DropboxManager {
        return DropboxManager(context)
    }

    @Provides
    @Singleton
    fun provideCloudSyncRepository(
        databaseBackupManager: DatabaseBackupManager,
        googleDriveManager: GoogleDriveManager,
        dropboxManager: DropboxManager
    ): CloudSyncRepository {
        return CloudSyncRepository(databaseBackupManager, googleDriveManager, dropboxManager)
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideAudioSplitter(@ApplicationContext context: Context): AudioSplitter {
        return AudioSplitter(context)
    }
}
