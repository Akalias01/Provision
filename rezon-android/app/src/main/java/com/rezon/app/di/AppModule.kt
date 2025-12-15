package com.rezon.app.di

import android.content.Context
import com.rezon.app.service.PlaybackController
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
}
