package com.mossglen.reverie.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mossglen.reverie.data.remote.GoogleBooksApi
import com.mossglen.reverie.data.remote.OpenLibraryApi
import com.mossglen.reverie.data.remote.iTunesApi
import com.mossglen.reverie.util.CrashReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cover Art Repository - Manages cover art from multiple sources:
 * 1. Search online sources (Google Books, OpenLibrary)
 * 2. Pick from device gallery
 * 3. Download from URL
 * 4. Save and manage cover art files
 */
@Singleton
class CoverArtRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val googleBooksApi: GoogleBooksApi,
    private val openLibraryApi: OpenLibraryApi,
    private val itunesApi: iTunesApi,
    private val bookDao: BookDao
) {
    companion object {
        private const val TAG = "CoverArtRepository"

        /**
         * Clean and normalize title for better search results.
         */
        private fun cleanTitle(title: String): String {
            return title
                // Remove file extensions
                .replace(Regex("\\.(m4b|mp3|m4a|ogg|opus|flac|wav)$", RegexOption.IGNORE_CASE), "")
                // Remove common audiobook suffixes
                .replace(Regex("\\s*[\\(\\[]?unabridged[\\)\\]]?", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*[\\(\\[]?audiobook[\\)\\]]?", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*[\\(\\[]?audio book[\\)\\]]?", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*[\\(\\[]?narrated by.*[\\)\\]]?", RegexOption.IGNORE_CASE), "")
                // Remove book numbers and series info
                .replace(Regex("\\s*[\\(\\[]?book\\s+\\d+[\\)\\]]?", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*[\\(\\[]?#\\d+[\\)\\]]?"), "")
                .replace(Regex("\\s*[\\(\\[]?volume\\s+\\d+[\\)\\]]?", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*[\\(\\[]?vol\\.?\\s+\\d+[\\)\\]]?", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*[\\(\\[]?part\\s+\\d+[\\)\\]]?", RegexOption.IGNORE_CASE), "")
                // Remove year and edition info
                .replace(Regex("\\s*[\\(\\[]?\\d{4}[\\)\\]]?"), "")
                .replace(Regex("\\s*[\\(\\[]?\\d+(?:st|nd|rd|th)\\s+edition[\\)\\]]?", RegexOption.IGNORE_CASE), "")
                // Remove extra brackets and parentheses content
                .replace(Regex("\\[.*?\\]"), "")
                .replace(Regex("\\(.*?\\)"), "")
                // Clean up special characters and extra spaces
                .replace(Regex("[_\\-]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        /**
         * Extract series name if present in title.
         */
        private fun extractSeriesName(title: String): String? {
            val patterns = listOf(
                Regex("^([^:]+?)\\s+and\\s+the\\s+", RegexOption.IGNORE_CASE),
                Regex("^([^:]+?):\\s*"),
                Regex("^([^-]+?)\\s*-\\s*")
            )

            patterns.forEach { pattern ->
                val match = pattern.find(title)
                if (match != null && match.groupValues.size > 1) {
                    val seriesName = match.groupValues[1].trim()
                    if (seriesName.length in 5..50 && seriesName.split(" ").size <= 5) {
                        return seriesName
                    }
                }
            }

            return null
        }
    }

    /**
     * Search result containing multiple cover art options
     */
    data class CoverSearchResult(
        val title: String,
        val author: String?,
        val coverUrl: String,
        val source: String // "GoogleBooks", "OpenLibrary"
    )

    /**
     * Search for cover art from online sources
     */
    suspend fun searchCoverArt(title: String, author: String? = null): List<CoverSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<CoverSearchResult>()

        try {
            // Search Google Books
            val googleResults = searchGoogleBooks(title, author)
            results.addAll(googleResults)
        } catch (e: Exception) {
            Log.w(TAG, "Google Books search failed: ${e.message}")
            CrashReporter.log("Google Books cover search error: ${e.message}")
        }

        try {
            // Search OpenLibrary
            val openLibraryResults = searchOpenLibrary(title, author)
            results.addAll(openLibraryResults)
        } catch (e: Exception) {
            Log.w(TAG, "OpenLibrary search failed: ${e.message}")
            CrashReporter.log("OpenLibrary cover search error: ${e.message}")
        }

        try {
            // Search iTunes (great for audiobooks)
            val itunesResults = searchItunes(title, author)
            results.addAll(itunesResults)
        } catch (e: Exception) {
            Log.w(TAG, "iTunes search failed: ${e.message}")
            CrashReporter.log("iTunes cover search error: ${e.message}")
        }

        Log.d(TAG, "Cover search completed: ${results.size} results from Google Books, OpenLibrary, and iTunes")

        // Remove duplicates and limit to 15 results
        results.distinctBy { it.coverUrl }.take(15)
    }

    /**
     * Search iTunes for audiobook covers (free, no API key)
     */
    private suspend fun searchItunes(title: String, author: String?): List<CoverSearchResult> {
        val cleanedTitle = cleanTitle(title)
        val seriesName = extractSeriesName(title)

        Log.d(TAG, "iTunes - Original: $title, Cleaned: $cleanedTitle, Series: $seriesName")

        // Try multiple queries for better results
        val queries = mutableListOf<String>()

        // Primary: cleaned title + author
        if (!author.isNullOrBlank()) {
            queries.add("$cleanedTitle $author")
        }

        // Secondary: series name + author (for series books like Harry Potter)
        if (seriesName != null && !author.isNullOrBlank()) {
            queries.add("$seriesName $author")
        }

        // Fallback: just cleaned title
        queries.add(cleanedTitle)

        val allResults = mutableListOf<CoverSearchResult>()

        for ((index, query) in queries.withIndex()) {
            Log.d(TAG, "iTunes search strategy ${index + 1}: $query")

            // Search both audiobooks and ebooks for this query
            val audiobookResults = try {
                itunesApi.searchAudiobooks(query, limit = 5)
            } catch (e: Exception) {
                Log.w(TAG, "iTunes audiobook search failed: ${e.message}")
                null
            }

            val ebookResults = try {
                itunesApi.searchBooks(query, limit = 3)
            } catch (e: Exception) {
                Log.w(TAG, "iTunes ebook search failed: ${e.message}")
                null
            }

            // Collect results from both searches
            audiobookResults?.results?.forEach { item ->
                val coverUrl = item.getHighResArtwork()
                if (coverUrl != null) {
                    allResults.add(CoverSearchResult(
                        title = item.getTitle() ?: title,
                        author = item.artistName ?: author,
                        coverUrl = coverUrl,
                        source = "iTunes"
                    ))
                }
            }

            ebookResults?.results?.forEach { item ->
                val coverUrl = item.getHighResArtwork()
                if (coverUrl != null) {
                    allResults.add(CoverSearchResult(
                        title = item.getTitle() ?: title,
                        author = item.artistName ?: author,
                        coverUrl = coverUrl,
                        source = "iTunes"
                    ))
                }
            }

            // If we found good results, we can stop trying more queries
            if (allResults.size >= 5) break
        }

        Log.d(TAG, "iTunes search completed: ${allResults.size} results found")
        return allResults.distinctBy { it.coverUrl }
    }

    /**
     * Search for additional covers using ISBN lookup
     */
    private suspend fun searchByIsbn(isbn: String): List<CoverSearchResult> {
        val results = mutableListOf<CoverSearchResult>()

        try {
            // OpenLibrary cover by ISBN
            val coverUrl = "https://covers.openlibrary.org/b/isbn/$isbn-L.jpg"
            results.add(CoverSearchResult(
                title = "ISBN: $isbn",
                author = null,
                coverUrl = coverUrl,
                source = "OpenLibrary"
            ))
        } catch (e: Exception) {
            Log.w(TAG, "ISBN search failed: ${e.message}")
        }

        return results
    }

    /**
     * Search Google Books for cover art
     */
    private suspend fun searchGoogleBooks(title: String, author: String?): List<CoverSearchResult> {
        val cleanedTitle = cleanTitle(title)
        val seriesName = extractSeriesName(title)

        Log.d(TAG, "Google Books - Original: $title, Cleaned: $cleanedTitle, Series: $seriesName")

        // Try multiple queries for better results
        val queries = mutableListOf<String>()

        // Primary: cleaned title + author with quotes for exact match
        if (!author.isNullOrBlank()) {
            queries.add("\"$cleanedTitle\" \"$author\"")
        }

        // Secondary: series name + author (for series books like Harry Potter)
        if (seriesName != null && !author.isNullOrBlank()) {
            queries.add("\"$seriesName\" \"$author\"")
        }

        // Tertiary: just cleaned title
        queries.add("\"$cleanedTitle\"")

        // Fallback: simple search without quotes
        if (!author.isNullOrBlank()) {
            queries.add("$cleanedTitle $author")
        }

        val allResults = mutableListOf<CoverSearchResult>()

        for ((index, query) in queries.withIndex()) {
            try {
                Log.d(TAG, "Google Books search strategy ${index + 1}: $query")
                val response = googleBooksApi.searchBooks(query, maxResults = 5)

                response.items?.forEach { item ->
                    val volumeInfo = item.volumeInfo
                    val coverUrl = volumeInfo.imageLinks?.thumbnail
                        ?.replace("http://", "https://")
                        ?.replace("&zoom=1", "&zoom=2") // Higher quality
                        ?.replace("&edge=curl", "") // Remove edge effects

                    if (coverUrl != null) {
                        allResults.add(CoverSearchResult(
                            title = volumeInfo.description ?: title,
                            author = author,
                            coverUrl = coverUrl,
                            source = "GoogleBooks"
                        ))
                    }
                }

                // If we found good results, we can stop trying more queries
                if (allResults.size >= 5) break

            } catch (e: Exception) {
                Log.w(TAG, "Google Books strategy ${index + 1} failed: ${e.message}")
            }
        }

        Log.d(TAG, "Google Books search completed: ${allResults.size} results found")
        return allResults.distinctBy { it.coverUrl }
    }

    /**
     * Search OpenLibrary for cover art
     */
    private suspend fun searchOpenLibrary(title: String, author: String?): List<CoverSearchResult> {
        val cleanedTitle = cleanTitle(title)
        val seriesName = extractSeriesName(title)

        Log.d(TAG, "OpenLibrary - Original: $title, Cleaned: $cleanedTitle, Series: $seriesName")

        // Try multiple search strategies
        val allResults = mutableListOf<CoverSearchResult>()

        // Strategy 1: Cleaned title + author
        if (!author.isNullOrBlank()) {
            try {
                Log.d(TAG, "OpenLibrary strategy 1: title='$cleanedTitle' author='$author'")
                val response = openLibraryApi.searchByTitleAndAuthor(cleanedTitle, author, limit = 5)
                response.docs.forEach { doc ->
                    doc.getCoverUrl("L")?.let { coverUrl ->
                        allResults.add(CoverSearchResult(
                            title = doc.title ?: title,
                            author = doc.getPrimaryAuthor() ?: author,
                            coverUrl = coverUrl,
                            source = "OpenLibrary"
                        ))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "OpenLibrary strategy 1 failed: ${e.message}")
            }
        }

        // Strategy 2: Series name + author (for series books like Harry Potter)
        if (allResults.size < 5 && seriesName != null && !author.isNullOrBlank()) {
            try {
                Log.d(TAG, "OpenLibrary strategy 2: title='$seriesName' author='$author'")
                val response = openLibraryApi.searchByTitleAndAuthor(seriesName, author, limit = 5)
                response.docs.forEach { doc ->
                    doc.getCoverUrl("L")?.let { coverUrl ->
                        allResults.add(CoverSearchResult(
                            title = doc.title ?: title,
                            author = doc.getPrimaryAuthor() ?: author,
                            coverUrl = coverUrl,
                            source = "OpenLibrary"
                        ))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "OpenLibrary strategy 2 failed: ${e.message}")
            }
        }

        // Strategy 3: Just cleaned title
        if (allResults.size < 5) {
            try {
                Log.d(TAG, "OpenLibrary strategy 3: title='$cleanedTitle'")
                val response = openLibraryApi.searchBooks(cleanedTitle, limit = 5)
                response.docs.forEach { doc ->
                    doc.getCoverUrl("L")?.let { coverUrl ->
                        allResults.add(CoverSearchResult(
                            title = doc.title ?: title,
                            author = doc.getPrimaryAuthor() ?: author,
                            coverUrl = coverUrl,
                            source = "OpenLibrary"
                        ))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "OpenLibrary strategy 3 failed: ${e.message}")
            }
        }

        Log.d(TAG, "OpenLibrary search completed: ${allResults.size} results found")
        return allResults.distinctBy { it.coverUrl }
    }

    /**
     * Download and save cover art from URL
     */
    suspend fun downloadAndSaveCover(url: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading cover from: $url")

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doInput = true
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Failed to download cover: ${connection.responseCode}")
                return@withContext null
            }

            // Save to external storage (survives reinstall)
            val coversDir = context.getExternalFilesDir("covers") ?: context.filesDir
            val coverFile = File(coversDir, "cover_${UUID.randomUUID()}.jpg")
            connection.inputStream.use { input ->
                FileOutputStream(coverFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "Cover saved to: ${coverFile.absolutePath}")
            coverFile.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "Failed to download cover: ${e.message}", e)
            CrashReporter.logError("Cover download failed", e)
            null
        }
    }

    /**
     * Copy cover from gallery URI to internal storage
     */
    suspend fun saveCoverFromGallery(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving cover from gallery: $uri")

            val coversDir = context.getExternalFilesDir("covers") ?: context.filesDir
            val coverFile = File(coversDir, "cover_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(coverFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "Gallery cover saved to: ${coverFile.absolutePath}")
            coverFile.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save gallery cover: ${e.message}", e)
            CrashReporter.logError("Gallery cover save failed", e)
            null
        }
    }

    /**
     * Update book cover URL in database
     */
    suspend fun updateBookCover(bookId: String, coverUrl: String?) = withContext(Dispatchers.IO) {
        try {
            val book = bookDao.getBookById(bookId)
            if (book != null) {
                // Delete old cover file if it exists and is a local file
                if (!book.coverUrl.isNullOrBlank() && book.coverUrl.startsWith("/")) {
                    try {
                        File(book.coverUrl).delete()
                    } catch (_: Exception) {}
                }

                // Update with new cover
                bookDao.updateBook(book.copy(coverUrl = coverUrl))
                Log.d(TAG, "Cover updated for book: ${book.title}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update book cover: ${e.message}", e)
            CrashReporter.logError("Cover update failed", e)
        }
    }

    /**
     * Delete a cover file (cleanup)
     */
    suspend fun deleteCoverFile(filePath: String) = withContext(Dispatchers.IO) {
        try {
            if (filePath.startsWith("/") && filePath.contains("cover_")) {
                File(filePath).delete()
                Log.d(TAG, "Deleted cover file: $filePath")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete cover file: ${e.message}")
        }
    }

    /**
     * Validate if a URL is a valid image URL
     */
    fun isValidImageUrl(url: String): Boolean {
        return url.matches(Regex("^https?://.*\\.(jpg|jpeg|png|webp|gif)(\\?.*)?$", RegexOption.IGNORE_CASE))
                || url.contains("covers.openlibrary.org")
                || url.contains("books.google.com/books/content")
    }
}
