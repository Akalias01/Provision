package com.example.rezon.di

import android.content.Context
import com.example.rezon.data.AudioServiceHandler
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
    fun provideAudioServiceHandler(@ApplicationContext context: Context): AudioServiceHandler {
        return AudioServiceHandler(context)
    }
}
