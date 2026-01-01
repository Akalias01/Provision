package com.mossglen.lithos.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * iTunes Search API - Free, no API key required
 * Great for audiobook covers as it has high-quality artwork
 */
interface iTunesApi {

    @GET("search")
    suspend fun searchAudiobooks(
        @Query("term") term: String,
        @Query("media") media: String = "audiobook",
        @Query("limit") limit: Int = 10
    ): iTunesSearchResponse

    @GET("search")
    suspend fun searchBooks(
        @Query("term") term: String,
        @Query("media") media: String = "ebook",
        @Query("limit") limit: Int = 10
    ): iTunesSearchResponse
}

data class iTunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<iTunesResult> = emptyList()
)

data class iTunesResult(
    val artistName: String? = null,
    val collectionName: String? = null,
    val trackName: String? = null,
    val artworkUrl100: String? = null,
    val artworkUrl60: String? = null
) {
    /**
     * Get high-resolution artwork URL (600x600)
     */
    fun getHighResArtwork(): String? {
        return artworkUrl100?.replace("100x100", "600x600")
    }

    /**
     * Get the best available title
     */
    fun getTitle(): String? = collectionName ?: trackName
}
