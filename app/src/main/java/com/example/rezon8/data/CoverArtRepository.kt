package com.mossglen.lithos.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.mossglen.lithos.data.remote.GoogleBooksApi
import com.mossglen.lithos.data.remote.OpenLibraryApi
import com.mossglen.lithos.data.remote.iTunesApi
import com.mossglen.lithos.util.CrashReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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
        val source: String // "WebSearch", "GoogleBooks", "OpenLibrary"
    )

    // ========================================================================
    // Aspect Ratio Detection
    // ========================================================================

    /**
     * Check if a local cover image is approximately square.
     * Returns true if aspect ratio is between 0.9 and 1.1 (within 10% tolerance)
     */
    suspend fun isSquareCover(coverPath: String?): Boolean = withContext(Dispatchers.IO) {
        if (coverPath.isNullOrBlank()) return@withContext false

        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(coverPath, options)

            val width = options.outWidth
            val height = options.outHeight

            if (width <= 0 || height <= 0) return@withContext false

            val aspectRatio = width.toFloat() / height.toFloat()
            val isSquare = aspectRatio in 0.9f..1.1f

            Log.d(TAG, "Cover aspect ratio: $aspectRatio (${width}x${height}) - isSquare: $isSquare")
            isSquare
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check cover aspect ratio: ${e.message}")
            false
        }
    }

    /**
     * Get aspect ratio of a remote image via partial download.
     * Downloads only the image header to determine dimensions efficiently.
     */
    suspend fun getRemoteImageAspectRatio(url: String): Float? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            // Request only first 64KB to get image header
            connection.setRequestProperty("Range", "bytes=0-65535")

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            connection.inputStream.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            connection.disconnect()

            if (options.outWidth > 0 && options.outHeight > 0) {
                val ratio = options.outWidth.toFloat() / options.outHeight.toFloat()
                Log.d(TAG, "Remote image aspect ratio: $ratio (${options.outWidth}x${options.outHeight})")
                ratio
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get remote image aspect ratio: ${e.message}")
            null
        }
    }

    /**
     * Check if a remote image is approximately square (aspect ratio 0.9-1.1)
     */
    suspend fun isRemoteImageSquare(url: String): Boolean {
        val ratio = getRemoteImageAspectRatio(url) ?: return false
        return ratio in 0.9f..1.1f
    }

    // ========================================================================
    // Web Image Search
    // ========================================================================

    /**
     * Search for book cover images using DuckDuckGo Images.
     * This finds high-quality covers from various sources including Amazon, iTunes CDNs.
     */
    private suspend fun searchWebForCover(title: String, author: String?): List<CoverSearchResult> = withContext(Dispatchers.IO) {
        val cleanedTitle = cleanTitle(title)
        val results = mutableListOf<CoverSearchResult>()

        try {
            // Build search query
            val query = buildString {
                append(cleanedTitle)
                if (!author.isNullOrBlank()) {
                    append(" $author")
                }
                append(" book cover")
            }

            Log.d(TAG, "Web search query: $query")

            // Use DuckDuckGo Images API (no API key required)
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://duckduckgo.com/i.js?q=$encodedQuery&o=json"

            val connection = URL(searchUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val resultsArray = json.optJSONArray("results")

                if (resultsArray != null) {
                    for (i in 0 until minOf(resultsArray.length(), 10)) {
                        val item = resultsArray.getJSONObject(i)
                        val imageUrl = item.optString("image")
                        val thumbnailUrl = item.optString("thumbnail")

                        // Prefer full image URL, fallback to thumbnail
                        val coverUrl = imageUrl.takeIf { it.isNotBlank() }
                            ?: thumbnailUrl.takeIf { it.isNotBlank() }

                        if (coverUrl != null && isValidImageUrl(coverUrl)) {
                            results.add(CoverSearchResult(
                                title = cleanedTitle,
                                author = author,
                                coverUrl = coverUrl,
                                source = "WebSearch"
                            ))
                        }
                    }
                }
            }
            connection.disconnect()

            Log.d(TAG, "Web search completed: ${results.size} results found")
        } catch (e: Exception) {
            Log.w(TAG, "Web search failed: ${e.message}")
            // Non-fatal - fall through to other search methods
        }

        results
    }

    /**
     * Search for cover art from online sources.
     * Priority order: Web Search → Google Books → OpenLibrary
     */
    suspend fun searchCoverArt(title: String, author: String? = null): List<CoverSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<CoverSearchResult>()

        // Priority 1: Web Image Search (finds high-quality covers from various CDNs)
        try {
            val webResults = searchWebForCover(title, author)
            results.addAll(webResults)
            Log.d(TAG, "Web search: ${webResults.size} results")
        } catch (e: Exception) {
            Log.w(TAG, "Web search failed: ${e.message}")
        }

        // Priority 2: Google Books
        try {
            val googleResults = searchGoogleBooks(title, author)
            results.addAll(googleResults)
            Log.d(TAG, "Google Books: ${googleResults.size} results")
        } catch (e: Exception) {
            Log.w(TAG, "Google Books search failed: ${e.message}")
            CrashReporter.log("Google Books cover search error: ${e.message}")
        }

        // Priority 3: OpenLibrary
        try {
            val openLibraryResults = searchOpenLibrary(title, author)
            results.addAll(openLibraryResults)
            Log.d(TAG, "OpenLibrary: ${openLibraryResults.size} results")
        } catch (e: Exception) {
            Log.w(TAG, "OpenLibrary search failed: ${e.message}")
            CrashReporter.log("OpenLibrary cover search error: ${e.message}")
        }

        Log.d(TAG, "Cover search completed: ${results.size} total results")

        // Remove duplicates and limit to 15 results
        results.distinctBy { it.coverUrl }.take(15)
    }

    // ========================================================================
    // Auto-Replace Non-Square Covers
    // ========================================================================

    /**
     * Automatically find and replace a non-square cover with a square one.
     * Returns the new cover path if replacement was successful, null otherwise.
     *
     * @param bookId The book ID in the database
     * @param currentCoverPath Path to the current cover image
     * @param title Book title for searching
     * @param author Book author for searching
     */
    suspend fun autoReplaceNonSquareCover(
        bookId: String,
        currentCoverPath: String?,
        title: String,
        author: String?
    ): String? = withContext(Dispatchers.IO) {
        // Check if current cover is already square
        if (isSquareCover(currentCoverPath)) {
            Log.d(TAG, "Cover is already square, skipping replacement for: $title")
            return@withContext null
        }

        Log.d(TAG, "Cover is non-square, searching for replacement: $title")

        // Search for covers
        val searchResults = searchCoverArt(title, author)

        if (searchResults.isEmpty()) {
            Log.d(TAG, "No cover search results for: $title")
            return@withContext null
        }

        // Find first square cover from results
        for (result in searchResults) {
            try {
                val aspectRatio = getRemoteImageAspectRatio(result.coverUrl)
                if (aspectRatio != null && aspectRatio in 0.9f..1.1f) {
                    Log.d(TAG, "Found square cover from ${result.source} (ratio: $aspectRatio)")

                    // Download and save the square cover locally
                    val newCoverPath = downloadAndSaveCover(result.coverUrl)
                    if (newCoverPath != null) {
                        // Delete old cover if it's a local file
                        if (!currentCoverPath.isNullOrBlank() && currentCoverPath.startsWith("/")) {
                            try {
                                File(currentCoverPath).delete()
                                Log.d(TAG, "Deleted old cover: $currentCoverPath")
                            } catch (_: Exception) {}
                        }

                        // Update database
                        updateBookCover(bookId, newCoverPath)
                        Log.d(TAG, "Cover replaced successfully for: $title")
                        return@withContext newCoverPath
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking cover from ${result.source}: ${e.message}")
            }
        }

        Log.d(TAG, "No square cover found for: $title")
        null
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
     * Re-check all books in the library for non-square covers and replace them.
     * Useful for retroactively fixing covers after this feature was added.
     *
     * @param onProgress Callback for progress updates (current, total)
     * @return Number of covers replaced
     */
    suspend fun recheckAllCovers(
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        val allBooks = bookDao.getAllBooksDirect()
        var replacedCount = 0

        Log.d(TAG, "Starting cover recheck for ${allBooks.size} books")

        allBooks.forEachIndexed { index, book ->
            onProgress?.invoke(index + 1, allBooks.size)

            try {
                val newCover = autoReplaceNonSquareCover(
                    bookId = book.id,
                    currentCoverPath = book.coverUrl,
                    title = book.title,
                    author = book.author
                )
                if (newCover != null) {
                    replacedCount++
                    Log.d(TAG, "Replaced cover for: ${book.title}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to recheck cover for ${book.title}: ${e.message}")
            }
        }

        Log.d(TAG, "Cover recheck complete: $replacedCount covers replaced")
        replacedCount
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
