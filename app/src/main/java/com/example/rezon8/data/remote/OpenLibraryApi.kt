package com.mossglen.lithos.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * OpenLibrary API - Free book metadata with no API key required.
 *
 * Endpoints:
 * - Search: https://openlibrary.org/search.json?q=query
 * - Works: https://openlibrary.org/works/{work_id}.json
 * - Authors: https://openlibrary.org/authors/{author_id}.json
 * - Covers: https://covers.openlibrary.org/b/id/{cover_id}-L.jpg
 *
 * Great for audiobooks as it includes narrator info and edition details.
 */
interface OpenLibraryApi {

    /**
     * Search for books by title, author, or general query.
     * Returns up to 100 results by default.
     */
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("fields") fields: String = "key,title,author_name,first_publish_year,cover_i,subject,isbn,publisher,number_of_pages_median,edition_count"
    ): OpenLibrarySearchResponse

    /**
     * Search specifically by title.
     */
    @GET("search.json")
    suspend fun searchByTitle(
        @Query("title") title: String,
        @Query("limit") limit: Int = 5,
        @Query("fields") fields: String = "key,title,author_name,first_publish_year,cover_i,subject,isbn,publisher,number_of_pages_median"
    ): OpenLibrarySearchResponse

    /**
     * Search by title and author for more accurate results.
     */
    @GET("search.json")
    suspend fun searchByTitleAndAuthor(
        @Query("title") title: String,
        @Query("author") author: String,
        @Query("limit") limit: Int = 3,
        @Query("fields") fields: String = "key,title,author_name,first_publish_year,cover_i,subject,isbn,publisher,number_of_pages_median"
    ): OpenLibrarySearchResponse

    /**
     * Get detailed work information including description.
     * Work key format: /works/OL12345W
     */
    @GET("works/{work_id}.json")
    suspend fun getWork(
        @Path("work_id") workId: String
    ): OpenLibraryWork
}

// ============================================================================
// Response Models
// ============================================================================

data class OpenLibrarySearchResponse(
    val numFound: Int = 0,
    val docs: List<OpenLibraryDoc> = emptyList()
)

data class OpenLibraryDoc(
    val key: String? = null,              // Work key like "/works/OL123W"
    val title: String? = null,
    val author_name: List<String>? = null,
    val first_publish_year: Int? = null,
    val cover_i: Long? = null,            // Cover ID for cover URL
    val subject: List<String>? = null,    // Categories/genres
    val isbn: List<String>? = null,
    val publisher: List<String>? = null,
    val number_of_pages_median: Int? = null,
    val edition_count: Int? = null
) {
    /**
     * Get the work ID from the key (e.g., "OL123W" from "/works/OL123W")
     */
    fun getWorkId(): String? = key?.substringAfterLast("/")

    /**
     * Get cover image URL in specified size.
     * Sizes: S (small), M (medium), L (large)
     */
    fun getCoverUrl(size: String = "L"): String? {
        return cover_i?.let { "https://covers.openlibrary.org/b/id/$it-$size.jpg" }
    }

    /**
     * Get primary author name.
     */
    fun getPrimaryAuthor(): String? = author_name?.firstOrNull()

    /**
     * Get subjects/categories as comma-separated string.
     * Limited to first 5 to avoid overly long strings.
     */
    fun getSubjectsString(): String? {
        return subject?.take(5)?.joinToString(", ")
    }
}

data class OpenLibraryWork(
    val key: String? = null,
    val title: String? = null,
    val description: OpenLibraryDescription? = null,
    val subjects: List<String>? = null,
    val covers: List<Long>? = null,
    val authors: List<OpenLibraryAuthorRef>? = null,
    val first_publish_date: String? = null
) {
    /**
     * Get description text (handles both String and Object formats).
     */
    fun getDescriptionText(): String? {
        return description?.value ?: description?.toString()?.takeIf { it != "null" }
    }

    /**
     * Get primary cover URL.
     */
    fun getCoverUrl(size: String = "L"): String? {
        return covers?.firstOrNull()?.let { "https://covers.openlibrary.org/b/id/$it-$size.jpg" }
    }
}

/**
 * OpenLibrary description can be either a string or an object with "value" field.
 */
data class OpenLibraryDescription(
    val value: String? = null
)

data class OpenLibraryAuthorRef(
    val author: OpenLibraryAuthorKey? = null
)

data class OpenLibraryAuthorKey(
    val key: String? = null  // e.g., "/authors/OL123A"
)
