package com.mossglen.reverie.di

import com.mossglen.reverie.data.remote.GoogleBooksApi
import com.mossglen.reverie.data.remote.OpenLibraryApi
import com.mossglen.reverie.data.remote.iTunesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ========================================================================
    // Google Books API
    // ========================================================================

    @Provides
    @Singleton
    @Named("GoogleBooks")
    fun provideGoogleBooksRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/books/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGoogleBooksApi(@Named("GoogleBooks") retrofit: Retrofit): GoogleBooksApi {
        return retrofit.create(GoogleBooksApi::class.java)
    }

    // ========================================================================
    // OpenLibrary API - Free book metadata with no API key
    // Great for audiobooks: includes narrator info, edition details
    // ========================================================================

    @Provides
    @Singleton
    @Named("OpenLibrary")
    fun provideOpenLibraryRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://openlibrary.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenLibraryApi(@Named("OpenLibrary") retrofit: Retrofit): OpenLibraryApi {
        return retrofit.create(OpenLibraryApi::class.java)
    }

    // ========================================================================
    // iTunes Search API - Free, no API key
    // Great for audiobook covers with high-quality artwork
    // ========================================================================

    @Provides
    @Singleton
    @Named("iTunes")
    fun provideiTunesRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideiTunesApi(@Named("iTunes") retrofit: Retrofit): iTunesApi {
        return retrofit.create(iTunesApi::class.java)
    }
}
