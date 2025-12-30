package com.mossglen.reverie.data

import android.util.Log
import com.mossglen.reverie.data.remote.GoogleBooksApi
import com.mossglen.reverie.data.remote.OpenLibraryApi
import com.mossglen.reverie.util.CrashReporter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Metadata Repository - Fetches book metadata from multiple sources.
 *
 * Fallback chain:
 * 1. Google Books API - Fast, good for general books
 * 2. OpenLibrary API - Free, excellent for audiobooks, detailed descriptions
 *
 * OpenLibrary is particularly good for audiobooks as it includes:
 * - Narrator information
 * - Edition details
 * - High-quality cover images
 * - Detailed subjects/categories
 */
@Singleton
class MetadataRepository @Inject constructor(
    private val googleBooksApi: GoogleBooksApi,
    private val openLibraryApi: OpenLibraryApi,
    private val bookDao: BookDao
) {
    companion object {
        private const val TAG = "MetadataRepository"

        /**
         * Clean and normalize title for better search results.
         * Removes common suffixes, file extensions, series numbers, and special characters.
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
                // Remove book numbers and series info (but keep the series name)
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
         * E.g., "Harry Potter and the Sorcerer's Stone" -> "Harry Potter"
         */
        private fun extractSeriesName(title: String): String? {
            // Common patterns for series
            val patterns = listOf(
                // "Series Name and the Book Title"
                Regex("^([^:]+?)\\s+and\\s+the\\s+", RegexOption.IGNORE_CASE),
                // "Series Name: Book Title"
                Regex("^([^:]+?):\\s*"),
                // "Series Name - Book Title"
                Regex("^([^-]+?)\\s*-\\s*")
            )

            patterns.forEach { pattern ->
                val match = pattern.find(title)
                if (match != null && match.groupValues.size > 1) {
                    val seriesName = match.groupValues[1].trim()
                    // Only return if it's reasonably short (likely a series name)
                    if (seriesName.length in 5..50 && seriesName.split(" ").size <= 5) {
                        return seriesName
                    }
                }
            }

            return null
        }

        /**
         * Extract narrator name from title/filename.
         * Supports patterns like "narrated by X", "read by X", "performed by X"
         */
        fun extractNarrator(title: String): String? {
            val patterns = listOf(
                // "Narrated by Stephen Fry"
                Regex("""(?:narrated\s+by|read\s+by|performed\s+by|narrator[:\s]+)\s*(.+?)(?:\s*[\[\(]|\s*$)""", RegexOption.IGNORE_CASE),
                // "Stephen Fry (Narrator)"
                Regex("""([^,\[\(]+?)\s*\((?:narrator|reader|performer)\)""", RegexOption.IGNORE_CASE),
                // "[Narrator: Stephen Fry]"
                Regex("""\[(?:narrator|read(?:er)?)[:\s]+\s*([^\]]+)\]""", RegexOption.IGNORE_CASE)
            )

            for (pattern in patterns) {
                pattern.find(title)?.let { match ->
                    val narrator = match.groupValues.getOrNull(1)?.trim()
                    if (!narrator.isNullOrBlank() && narrator.length in 3..50) {
                        return narrator
                    }
                }
            }

            return null
        }

        /**
         * Extract full series info (name and book number) from title.
         * Returns formatted series string like "Harry Potter #1"
         */
        fun extractSeriesInfo(title: String): String? {
            val cleanedTitle = cleanTitle(title)

            // Pattern 1: "Book Title #1" or "#1.5"
            val hashPattern = """(?:^|[,:\-\s])\s*#\s*(\d+(?:\.\d+)?)\b""".toRegex()
            val hashMatch = hashPattern.find(title)

            // Pattern 2: "Book Title, Book 1" or "Book 2" or "Volume 3"
            val bookPattern = """(?:Book|Vol\.?|Volume|Part)\s*(\d+(?:\.\d+)?)\b""".toRegex(RegexOption.IGNORE_CASE)
            val bookMatch = bookPattern.find(title)

            // Pattern 3: Just trailing number "Series Name 2"
            val trailingPattern = """\s+(\d+(?:\.\d+)?)\s*$""".toRegex()
            val trailingMatch = trailingPattern.find(cleanedTitle)

            val bookNumber = (hashMatch?.groupValues?.getOrNull(1)
                ?: bookMatch?.groupValues?.getOrNull(1)
                ?: trailingMatch?.groupValues?.getOrNull(1))?.toFloatOrNull()

            val seriesName = extractSeriesName(title)

            return if (seriesName != null) {
                if (bookNumber != null) {
                    val numStr = if (bookNumber % 1 == 0f) bookNumber.toInt().toString() else bookNumber.toString()
                    "$seriesName #$numStr"
                } else {
                    seriesName
                }
            } else if (bookNumber != null) {
                // Have book number but no series name - can't determine series
                null
            } else {
                null
            }
        }
    }

    /**
     * Fetch metadata from available sources and save to database.
     * Uses fallback chain: Google Books -> OpenLibrary
     */
    suspend fun fetchAndSaveMetadata(book: Book) {
        try {
            Log.d(TAG, "Fetching metadata for: ${book.title} by ${book.author}")

            // Try Google Books first
            val googleResult = tryGoogleBooks(book)

            // If Google Books didn't provide good results, try OpenLibrary
            val openLibraryResult = if (!googleResult.hasGoodMetadata()) {
                Log.d(TAG, "Google Books incomplete, trying OpenLibrary...")
                tryOpenLibrary(book)
            } else null

            // Merge results (prefer existing data, then Google, then OpenLibrary)
            val updatedBook = mergeMetadata(book, googleResult, openLibraryResult)

            if (updatedBook != book) {
                bookDao.updateBook(updatedBook)
                Log.d(TAG, "Metadata updated for: ${book.title}")
            } else {
                Log.d(TAG, "No new metadata found for: ${book.title}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch metadata for ${book.title}: ${e.message}")
            CrashReporter.logError("Metadata fetch failed for: ${book.title}", e)
            CrashReporter.setCustomKey("metadata_book_title", book.title)
            CrashReporter.setCustomKey("metadata_book_author", book.author)
        }
    }

    /**
     * Fetch metadata from OpenLibrary only (useful for audiobooks).
     */
    suspend fun fetchFromOpenLibrary(book: Book) {
        try {
            Log.d(TAG, "Fetching from OpenLibrary: ${book.title}")
            val result = tryOpenLibrary(book)

            if (result != null) {
                val genreFromSubjects = result.subjects?.split(",")?.firstOrNull()?.trim()
                val updatedBook = book.copy(
                    synopsis = result.description.takeIf { it?.isNotBlank() == true } ?: book.synopsis,
                    genre = genreFromSubjects.takeIf { it?.isNotBlank() == true } ?: book.genre,
                    coverUrl = result.coverUrl.takeIf { it?.isNotBlank() == true } ?: book.coverUrl
                )
                if (updatedBook != book) {
                    bookDao.updateBook(updatedBook)
                    Log.d(TAG, "OpenLibrary metadata saved for: ${book.title}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenLibrary fetch failed: ${e.message}")
            CrashReporter.logError("OpenLibrary fetch failed for: ${book.title}", e)
        }
    }

    // ========================================================================
    // Google Books
    // ========================================================================

    private suspend fun tryGoogleBooks(book: Book): MetadataResult {
        return try {
            val cleanedTitle = cleanTitle(book.title)
            val seriesName = extractSeriesName(book.title)

            Log.d(TAG, "Google Books - Original: ${book.title}, Cleaned: $cleanedTitle, Series: $seriesName")

            // Try multiple search strategies
            val strategies = mutableListOf<String>()

            // Strategy 1: Full cleaned title + author
            if (book.author.isNotBlank()) {
                strategies.add(buildString {
                    append("intitle:\"$cleanedTitle\"")
                    append("+inauthor:\"${book.author}\"")
                })
            }

            // Strategy 2: Series name only (for books like "Harry Potter and the...")
            if (seriesName != null && book.author.isNotBlank()) {
                strategies.add(buildString {
                    append("intitle:\"$seriesName\"")
                    append("+inauthor:\"${book.author}\"")
                })
            }

            // Strategy 3: Just cleaned title (no author)
            strategies.add("intitle:\"$cleanedTitle\"")

            // Strategy 4: General search as fallback
            if (book.author.isNotBlank()) {
                strategies.add("\"$cleanedTitle\" \"${book.author}\"")
            }

            // Try each strategy until we get a good result
            var bestResult: MetadataResult? = null

            for ((index, query) in strategies.withIndex()) {
                try {
                    Log.d(TAG, "Google Books strategy ${index + 1}: $query")
                    val response = googleBooksApi.searchBooks(query, maxResults = 3)
                    val info = response.items?.firstOrNull()?.volumeInfo

                    val result = MetadataResult(
                        source = "GoogleBooks",
                        description = info?.description,
                        subjects = info?.categories?.joinToString(", "),
                        coverUrl = info?.imageLinks?.thumbnail
                            ?.replace("http://", "https://")
                            ?.replace("&edge=curl", "") // Remove edge effects
                            ?.replace("zoom=1", "zoom=2") // Higher quality
                    )

                    // If we found a cover and description, that's good enough
                    if (result.hasGoodMetadata()) {
                        Log.d(TAG, "Google Books found good result with strategy ${index + 1}")
                        return result
                    }

                    // Keep the best partial result
                    if (bestResult == null || result.coverUrl != null) {
                        bestResult = result
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Google Books strategy ${index + 1} failed: ${e.message}")
                }
            }

            bestResult ?: MetadataResult(source = "GoogleBooks")

        } catch (e: Exception) {
            Log.w(TAG, "Google Books failed: ${e.message}")
            // Non-fatal: Log but don't crash report unless it's a network error
            if (e is java.net.UnknownHostException || e is java.net.SocketTimeoutException) {
                CrashReporter.log("Network error fetching Google Books: ${e.javaClass.simpleName}")
            }
            MetadataResult(source = "GoogleBooks")
        }
    }

    // ========================================================================
    // OpenLibrary
    // ========================================================================

    private suspend fun tryOpenLibrary(book: Book): MetadataResult? {
        return try {
            val cleanedTitle = cleanTitle(book.title)
            val seriesName = extractSeriesName(book.title)

            Log.d(TAG, "OpenLibrary - Original: ${book.title}, Cleaned: $cleanedTitle, Series: $seriesName")

            // Try multiple search strategies
            var bestDoc: com.mossglen.reverie.data.remote.OpenLibraryDoc? = null

            // Strategy 1: Cleaned title + author
            if (book.author.isNotBlank()) {
                try {
                    Log.d(TAG, "OpenLibrary strategy 1: title='$cleanedTitle' author='${book.author}'")
                    val response = openLibraryApi.searchByTitleAndAuthor(
                        title = cleanedTitle,
                        author = book.author,
                        limit = 5
                    )
                    bestDoc = response.docs.firstOrNull { it.cover_i != null }
                        ?: response.docs.firstOrNull()
                } catch (e: Exception) {
                    Log.w(TAG, "OpenLibrary strategy 1 failed: ${e.message}")
                }
            }

            // Strategy 2: Series name + author (for series books)
            if (bestDoc == null && seriesName != null && book.author.isNotBlank()) {
                try {
                    Log.d(TAG, "OpenLibrary strategy 2: title='$seriesName' author='${book.author}'")
                    val response = openLibraryApi.searchByTitleAndAuthor(
                        title = seriesName,
                        author = book.author,
                        limit = 5
                    )
                    bestDoc = response.docs.firstOrNull { it.cover_i != null }
                        ?: response.docs.firstOrNull()
                } catch (e: Exception) {
                    Log.w(TAG, "OpenLibrary strategy 2 failed: ${e.message}")
                }
            }

            // Strategy 3: Just cleaned title
            if (bestDoc == null) {
                try {
                    Log.d(TAG, "OpenLibrary strategy 3: title='$cleanedTitle'")
                    val response = openLibraryApi.searchByTitle(title = cleanedTitle, limit = 5)
                    bestDoc = response.docs.firstOrNull { it.cover_i != null }
                        ?: response.docs.firstOrNull()
                } catch (e: Exception) {
                    Log.w(TAG, "OpenLibrary strategy 3 failed: ${e.message}")
                }
            }

            // Strategy 4: General search
            if (bestDoc == null) {
                try {
                    val generalQuery = if (book.author.isNotBlank()) {
                        "$cleanedTitle ${book.author}"
                    } else {
                        cleanedTitle
                    }
                    Log.d(TAG, "OpenLibrary strategy 4: query='$generalQuery'")
                    val response = openLibraryApi.searchBooks(generalQuery, limit = 5)
                    bestDoc = response.docs.firstOrNull { it.cover_i != null }
                        ?: response.docs.firstOrNull()
                } catch (e: Exception) {
                    Log.w(TAG, "OpenLibrary strategy 4 failed: ${e.message}")
                }
            }

            if (bestDoc == null) {
                Log.d(TAG, "OpenLibrary: No results found")
                return null
            }

            Log.d(TAG, "OpenLibrary: Found result '${bestDoc.title}'")

            // Get detailed description from Works API if we have a work key
            val description = bestDoc.getWorkId()?.let { workId ->
                try {
                    val work = openLibraryApi.getWork(workId)
                    work.getDescriptionText()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch work details: ${e.message}")
                    null
                }
            }

            MetadataResult(
                source = "OpenLibrary",
                description = description,
                subjects = bestDoc.getSubjectsString(),
                coverUrl = bestDoc.getCoverUrl("L"),  // Large cover
                publishYear = bestDoc.first_publish_year,
                pageCount = bestDoc.number_of_pages_median
            )
        } catch (e: Exception) {
            Log.w(TAG, "OpenLibrary failed: ${e.message}")
            // Non-fatal: Log but don't crash report unless it's a network error
            if (e is java.net.UnknownHostException || e is java.net.SocketTimeoutException) {
                CrashReporter.log("Network error fetching OpenLibrary: ${e.javaClass.simpleName}")
            }
            null
        }
    }

    // ========================================================================
    // Merge Results
    // ========================================================================

    private fun mergeMetadata(
        book: Book,
        googleResult: MetadataResult?,
        openLibraryResult: MetadataResult?
    ): Book {
        // Priority: Existing book data > Google Books > OpenLibrary
        val synopsis = book.synopsis.takeIf { it.isNotBlank() }
            ?: googleResult?.description?.takeIf { it.isNotBlank() }
            ?: openLibraryResult?.description?.takeIf { it.isNotBlank() }
            ?: book.synopsis

        // Genre from categories/subjects (first one for display)
        val genre = book.genre.takeIf { it.isNotBlank() }
            ?: googleResult?.subjects?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
            ?: openLibraryResult?.subjects?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
            ?: book.genre

        val coverUrl = book.coverUrl?.takeIf { it.isNotBlank() }
            ?: googleResult?.coverUrl?.takeIf { it.isNotBlank() }
            ?: openLibraryResult?.coverUrl?.takeIf { it.isNotBlank() }

        // Extract series info from title if not already set
        val seriesInfo = book.seriesInfo.takeIf { it.isNotBlank() }
            ?: Companion.extractSeriesInfo(book.title)
            ?: book.seriesInfo

        // Extract narrator from title if not already set
        val narrator = book.narrator.takeIf { it.isNotBlank() }
            ?: Companion.extractNarrator(book.title)
            ?: book.narrator

        return book.copy(
            synopsis = synopsis,
            genre = genre,
            coverUrl = coverUrl,
            seriesInfo = seriesInfo,
            narrator = narrator
        )
    }
}

/**
 * Internal class to hold metadata results from any source.
 */
private data class MetadataResult(
    val source: String,
    val description: String? = null,
    val subjects: String? = null,
    val coverUrl: String? = null,
    val publishYear: Int? = null,
    val pageCount: Int? = null
) {
    fun hasGoodMetadata(): Boolean {
        return !description.isNullOrBlank() && !coverUrl.isNullOrBlank()
    }
}
