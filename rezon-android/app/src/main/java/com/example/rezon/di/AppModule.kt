package com.example.rezon.di

import android.content.Context
import androidx.room.Room
import com.example.rezon.data.AppDatabase
import com.example.rezon.data.AudioServiceHandler
import com.example.rezon.data.BookDao
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
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "rezon_db"
        ).build()
    }

    @Provides
    fun provideBookDao(db: AppDatabase): BookDao = db.bookDao()

    @Provides
    @Singleton
    fun provideAudioServiceHandler(@ApplicationContext context: Context): AudioServiceHandler {
        return AudioServiceHandler(context)
    }
}
