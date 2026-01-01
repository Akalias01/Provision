package com.mossglen.lithos.data.remote

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
    val description: String?,
    val categories: List<String>?,
    val imageLinks: ImageLinks?
)
data class ImageLinks(val thumbnail: String?, val smallThumbnail: String?)
