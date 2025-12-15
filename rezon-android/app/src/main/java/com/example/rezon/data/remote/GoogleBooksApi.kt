package com.example.rezon.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApi {
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 1
    ): BookResponse
}

data class BookResponse(val items: List<BookItem>?)
data class BookItem(val volumeInfo: VolumeInfo)
data class VolumeInfo(
    val title: String,
    val authors: List<String>?,
    val description: String?, // The Synopsis
    val subtitle: String?,    // Often contains Series info
    val categories: List<String>?,
    val averageRating: Float?
)
