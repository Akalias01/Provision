package com.example.rezon.data.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for Google Books API
 */
interface GoogleBooksApi {

    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 5
    ): GoogleBooksResponse
}

// API Response Models
data class GoogleBooksResponse(
    val totalItems: Int,
    val items: List<VolumeItem>?
)

data class VolumeItem(
    val id: String,
    val volumeInfo: VolumeInfo
)

data class VolumeInfo(
    val title: String?,
    val authors: List<String>?,
    val description: String?,
    val imageLinks: ImageLinks?,
    val categories: List<String>?,
    val publisher: String?,
    val publishedDate: String?,
    val pageCount: Int?,
    val averageRating: Double?,
    val seriesInfo: SeriesInfo?
)

data class ImageLinks(
    val thumbnail: String?,
    val smallThumbnail: String?
)

data class SeriesInfo(
    val kind: String?,
    val shortSeriesBookTitle: String?,
    val bookDisplayNumber: String?,
    val volumeSeries: List<VolumeSeries>?
)

data class VolumeSeries(
    val seriesId: String?,
    val seriesBookType: String?,
    val orderNumber: Int?
)
