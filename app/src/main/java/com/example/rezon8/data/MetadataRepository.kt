package com.mossglen.lithos.data

import android.util.Log
import com.mossglen.lithos.data.remote.GoogleBooksApi
import com.mossglen.lithos.data.remote.OpenLibraryApi
import com.mossglen.lithos.util.CrashReporter
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parsed metadata from filename or embedded tags.
 * Contains separated title, author, series, book number, and narrator.
 */
data class ParsedMetadata(
    val title: String,
    val author: String?,
    val series: String?,
    val bookNumber: Float?,
    val narrator: String?
)

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
         * E.g., "Fourth Wing - The Empyrean #1" -> "The Empyrean"
         */
        private fun extractSeriesName(title: String): String? {
            // Pattern 1: "Book Title - Series Name #N" (e.g., "Fourth Wing - The Empyrean #1")
            val dashSeriesPattern = Regex("""^.+?\s*-\s*(.+?)\s*#\d+""", RegexOption.IGNORE_CASE)
            dashSeriesPattern.find(title)?.let { match ->
                val seriesName = match.groupValues[1].trim()
                if (seriesName.length in 3..50) {
                    return seriesName
                }
            }

            // Pattern 2: "Book Title (Series Name #N)" or "Book Title (Series Name, Book N)"
            val parenSeriesPattern = Regex("""^.+?\s*\((.+?)(?:\s*#\d+|,\s*Book\s*\d+)?\)""", RegexOption.IGNORE_CASE)
            parenSeriesPattern.find(title)?.let { match ->
                val seriesName = match.groupValues[1].trim()
                if (seriesName.length in 3..50 && !seriesName.contains("unabridged", ignoreCase = true)) {
                    return seriesName
                }
            }

            // Common patterns for series (legacy patterns)
            val patterns = listOf(
                // "Series Name and the Book Title"
                Regex("^([^:]+?)\\s+and\\s+the\\s+", RegexOption.IGNORE_CASE),
                // "Series Name: Book Title"
                Regex("^([^:]+?):\\s*")
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

        /**
         * Extract book number from text using various patterns.
         */
        fun extractBookNumber(text: String): Float? {
            val patterns = listOf(
                // "#1", "#1.5"
                Regex("""#\s*(\d+(?:\.\d+)?)\b"""),
                // "Book 1", "Book 1.5"
                Regex("""Book\s*(\d+(?:\.\d+)?)\b""", RegexOption.IGNORE_CASE),
                // "Volume 2", "Vol. 2", "Vol 2"
                Regex("""Vol(?:ume)?\.?\s*(\d+(?:\.\d+)?)\b""", RegexOption.IGNORE_CASE),
                // "Part 3"
                Regex("""Part\s*(\d+(?:\.\d+)?)\b""", RegexOption.IGNORE_CASE),
                // "3 of 10" or "3/10"
                Regex("""\b(\d{1,2})\s*(?:of|/)\s*\d+\b""")
            )

            for (pattern in patterns) {
                pattern.find(text)?.groupValues?.getOrNull(1)?.toFloatOrNull()?.let { return it }
            }
            return null
        }

        /**
         * Parse audiobook filename/path to extract title, author, series, and book number.
         * Handles common naming conventions from various sources.
         *
         * Supported patterns:
         * - "Author - Title (Series #1)"
         * - "Author - Series Book 1 - Title"
         * - "Title by Author"
         * - "Author - Title"
         * - Folder structure: Author/Series/01 - Title.m4b
         */
        fun parseFilename(filename: String, folderPath: String? = null): ParsedMetadata {
            val cleanName = filename
                .replace(Regex("\\.(m4b|mp3|m4a|ogg|opus|flac|wav)$", RegexOption.IGNORE_CASE), "")
                .replace("_", " ")
                .trim()

            // Pattern 1: "Author - Title (Series #1)"
            val pattern1 = Regex("""^(.+?)\s*-\s*(.+?)\s*\((.+?)\s*#?(\d+(?:\.\d+)?)\)$""")
            pattern1.find(cleanName)?.let { match ->
                return ParsedMetadata(
                    title = match.groupValues[2].trim(),
                    author = match.groupValues[1].trim(),
                    series = match.groupValues[3].trim(),
                    bookNumber = match.groupValues[4].toFloatOrNull(),
                    narrator = extractNarrator(cleanName)
                )
            }

            // Pattern 2: "Author - Series Book 1 - Title"
            val pattern2 = Regex("""^(.+?)\s*-\s*(.+?)\s+Book\s*(\d+(?:\.\d+)?)\s*-\s*(.+)$""", RegexOption.IGNORE_CASE)
            pattern2.find(cleanName)?.let { match ->
                return ParsedMetadata(
                    title = match.groupValues[4].trim(),
                    author = match.groupValues[1].trim(),
                    series = match.groupValues[2].trim(),
                    bookNumber = match.groupValues[3].toFloatOrNull(),
                    narrator = extractNarrator(cleanName)
                )
            }

            // Pattern 3: "Title by Author"
            val pattern3 = Regex("""^(.+?)\s+by\s+(.+)$""", RegexOption.IGNORE_CASE)
            pattern3.find(cleanName)?.let { match ->
                val potentialTitle = match.groupValues[1].trim()
                val potentialAuthor = match.groupValues[2].trim()
                return ParsedMetadata(
                    title = potentialTitle,
                    author = potentialAuthor,
                    series = extractSeriesName(potentialTitle),
                    bookNumber = extractBookNumber(potentialTitle),
                    narrator = extractNarrator(cleanName)
                )
            }

            // Pattern 4: "Author - Title" (heuristic: if part1 looks like a name, it's author)
            val pattern4 = Regex("""^(.+?)\s*-\s*(.+)$""")
            pattern4.find(cleanName)?.let { match ->
                val part1 = match.groupValues[1].trim()
                val part2 = match.groupValues[2].trim()

                // Heuristic: If part1 looks like a name (2-4 words, no numbers), it's author
                val looksLikeName = part1.split(" ").size in 2..4 && !part1.contains(Regex("\\d"))

                return if (looksLikeName) {
                    ParsedMetadata(
                        title = part2,
                        author = part1,
                        series = extractSeriesName(part2),
                        bookNumber = extractBookNumber(part2),
                        narrator = extractNarrator(cleanName)
                    )
                } else {
                    // part1 might be the title or series
                    ParsedMetadata(
                        title = cleanName,
                        author = null,
                        series = extractSeriesName(cleanName),
                        bookNumber = extractBookNumber(cleanName),
                        narrator = extractNarrator(cleanName)
                    )
                }
            }

            // Pattern 5: Folder structure "Author/Series/Book 01 - Title"
            if (folderPath != null) {
                val parts = folderPath.split(File.separator)
                if (parts.size >= 2) {
                    val potentialAuthor = parts[parts.size - 2]
                    val potentialSeries = parts[parts.size - 1]

                    // Check if potential author looks like a name (2-4 words)
                    if (potentialAuthor.split(" ").size in 2..4) {
                        return ParsedMetadata(
                            title = cleanTitle(cleanName),
                            author = potentialAuthor,
                            series = potentialSeries,
                            bookNumber = extractBookNumber(cleanName),
                            narrator = extractNarrator(cleanName)
                        )
                    }
                }
            }

            // Fallback: Just use filename as title, extract what's possible
            return ParsedMetadata(
                title = cleanTitle(cleanName),
                author = null,
                series = extractSeriesName(cleanName),
                bookNumber = extractBookNumber(cleanName),
                narrator = extractNarrator(cleanName)
            )
        }

        /**
         * Parse embedded audio metadata tags into ParsedMetadata.
         * Uses standard audiobook tag mapping:
         * - TITLE -> Book title
         * - ALBUM_ARTIST -> Author (preferred)
         * - ARTIST -> Author OR Narrator
         * - ALBUM -> Series name
         *
         * @param rawTitle From METADATA_KEY_TITLE
         * @param rawArtist From METADATA_KEY_ARTIST
         * @param rawAlbum From METADATA_KEY_ALBUM
         * @param rawAlbumArtist From METADATA_KEY_ALBUMARTIST
         */
        fun parseEmbeddedMetadata(
            rawTitle: String?,
            rawArtist: String?,
            rawAlbum: String?,
            rawAlbumArtist: String?
        ): ParsedMetadata {
            var title = rawTitle?.trim() ?: ""
            var author = rawAlbumArtist?.trim()?.takeIf { it.isNotBlank() }
            var series = rawAlbum?.trim()?.takeIf { it.isNotBlank() }
            var narrator: String? = null

            // Check if ARTIST is different from ALBUM_ARTIST (likely narrator)
            if (!rawArtist.isNullOrBlank() && !rawAlbumArtist.isNullOrBlank() &&
                rawArtist.trim() != rawAlbumArtist.trim()) {
                narrator = rawArtist.trim()
            } else if (!rawArtist.isNullOrBlank() && rawAlbumArtist.isNullOrBlank()) {
                // ARTIST is the author (ALBUM_ARTIST not set)
                author = rawArtist.trim()
            }

            // Check if title contains series info like "Series Name: Book Title"
            if (title.contains(":") && series == null) {
                val parts = title.split(":", limit = 2)
                if (parts[0].split(" ").size <= 5) {
                    series = parts[0].trim()
                    title = parts[1].trim()
                }
            }

            // Extract book number from title or album
            val bookNumber = extractBookNumber(rawTitle ?: "") ?: extractBookNumber(rawAlbum ?: "")

            // Filter out common non-series album names
            if (series != null) {
                val lowerSeries = series.lowercase()
                if (lowerSeries.contains("audiobook") ||
                    lowerSeries.contains("unabridged") ||
                    lowerSeries == title.lowercase()) {
                    series = null
                }
            }

            return ParsedMetadata(
                title = title.ifBlank { "Unknown" },
                author = author,
                series = series,
                bookNumber = bookNumber,
                narrator = narrator ?: extractNarrator(rawTitle ?: "")
            )
        }

        /**
         * Merge filename-parsed and embedded metadata, preferring embedded when available.
         * Embedded tags are typically user-curated and more accurate.
         */
        fun mergeMetadataSources(
            filenameParsed: ParsedMetadata,
            embeddedParsed: ParsedMetadata
        ): ParsedMetadata {
            return ParsedMetadata(
                // Prefer embedded title if it's meaningful
                title = embeddedParsed.title.takeIf { it.isNotBlank() && it != "Unknown" }
                    ?: filenameParsed.title,
                // Prefer embedded author
                author = embeddedParsed.author ?: filenameParsed.author,
                // Prefer embedded series
                series = embeddedParsed.series ?: filenameParsed.series,
                // Prefer embedded book number
                bookNumber = embeddedParsed.bookNumber ?: filenameParsed.bookNumber,
                // Prefer embedded narrator
                narrator = embeddedParsed.narrator ?: filenameParsed.narrator
            )
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
            var bestDoc: com.mossglen.lithos.data.remote.OpenLibraryDoc? = null

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

    // ========================================================================
    // Series Metadata Fetching
    // ========================================================================

    suspend fun fetchSeriesMetadata(seriesName: String): SeriesMetadata? {
        return try {
            Log.d(TAG, "Fetching series metadata for: $seriesName")

            val allDocs = mutableListOf<com.mossglen.lithos.data.remote.OpenLibraryDoc>()

            // Strategy 1: Exact series name search
            try {
                val response = openLibraryApi.searchBooks(
                    query = "\"$seriesName\"",
                    limit = 100
                )
                allDocs.addAll(response.docs)
                Log.d(TAG, "Strategy 1 found ${response.docs.size} results")
            } catch (e: Exception) {
                Log.w(TAG, "Strategy 1 failed: ${e.message}")
            }

            // Strategy 2: Series name + series keyword
            try {
                val response = openLibraryApi.searchBooks(
                    query = "$seriesName series",
                    limit = 100
                )
                allDocs.addAll(response.docs)
                Log.d(TAG, "Strategy 2 found ${response.docs.size} results")
            } catch (e: Exception) {
                Log.w(TAG, "Strategy 2 failed: ${e.message}")
            }

            // Strategy 3: Subject search
            try {
                val response = openLibraryApi.searchBooks(
                    query = "subject:\"$seriesName\"",
                    limit = 100
                )
                allDocs.addAll(response.docs)
                Log.d(TAG, "Strategy 3 found ${response.docs.size} results")
            } catch (e: Exception) {
                Log.w(TAG, "Strategy 3 failed: ${e.message}")
            }

            if (allDocs.isEmpty()) {
                Log.d(TAG, "No results for series: $seriesName")
                return null
            }

            // Remove duplicates by key
            val uniqueDocs = allDocs.distinctBy { it.key ?: it.title }
            Log.d(TAG, "Total unique docs: ${uniqueDocs.size}")

            val seriesLower = seriesName.lowercase()
            val seriesWords = seriesLower.split(" ").filter { it.length > 2 }

            // More lenient filtering
            val seriesBooks = uniqueDocs.filter { doc ->
                val title = doc.title?.lowercase() ?: ""
                val subjects = doc.subject?.joinToString(" ")?.lowercase() ?: ""

                // Check title - any significant word match
                val inTitle = title.contains(seriesLower) ||
                    seriesWords.any { word -> word.length > 3 && title.contains(word) }

                // Check subjects for series reference
                val inSubjects = subjects.contains(seriesLower) ||
                    seriesWords.any { word -> word.length > 3 && subjects.contains(word) }

                inTitle || inSubjects
            }

            Log.d(TAG, "Filtered to ${seriesBooks.size} matching books")

            if (seriesBooks.isEmpty()) {
                Log.d(TAG, "No matching books for series: $seriesName")
                return null
            }

            // Group by author to get consistent series
            val primaryAuthor = seriesBooks
                .mapNotNull { it.author_name?.firstOrNull() }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }?.key

            Log.d(TAG, "Primary author: $primaryAuthor")

            // Filter to primary author's books only for accurate count
            val authorBooks = if (primaryAuthor != null) {
                seriesBooks.filter { it.author_name?.firstOrNull() == primaryAuthor }
            } else {
                seriesBooks
            }

            Log.d(TAG, "Author books: ${authorBooks.size}")

            // Create SeriesBookInfo for each unique book with covers
            val bookInfoMap = mutableMapOf<String, SeriesBookInfo>()
            authorBooks.forEach { doc ->
                val title = doc.title ?: return@forEach
                if (!bookInfoMap.containsKey(title)) {
                    bookInfoMap[title] = SeriesBookInfo(
                        title = title,
                        coverUrl = doc.getCoverUrl("M"),
                        publishYear = doc.first_publish_year
                    )
                } else if (doc.cover_i != null && bookInfoMap[title]?.coverUrl == null) {
                    // Update with cover if we find one
                    bookInfoMap[title] = bookInfoMap[title]!!.copy(coverUrl = doc.getCoverUrl("M"))
                }
            }

            val uniqueBooks = bookInfoMap.values.toList().sortedBy { it.title }
            val uniqueTitles = uniqueBooks.map { it.title }
            val coverUrl = authorBooks.firstOrNull { it.cover_i != null }?.getCoverUrl("L")

            Log.d(TAG, "Found ${uniqueTitles.size} books in series $seriesName by $primaryAuthor")

            SeriesMetadata(
                name = seriesName,
                totalBooks = uniqueTitles.size,
                author = primaryAuthor,
                coverUrl = coverUrl,
                bookTitles = uniqueTitles,
                books = uniqueBooks
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch series metadata: ${e.message}")
            null
        }
    }

    suspend fun fetchSeriesMetadataByAuthor(seriesName: String, author: String): SeriesMetadata? {
        return try {
            Log.d(TAG, "Fetching series metadata by author: $seriesName by $author")

            val allDocs = mutableListOf<com.mossglen.lithos.data.remote.OpenLibraryDoc>()

            // Strategy 1: Search by author with series name
            try {
                val response = openLibraryApi.searchBooks(
                    query = "author:\"$author\" \"$seriesName\"",
                    limit = 100
                )
                allDocs.addAll(response.docs)
            } catch (e: Exception) {
                Log.w(TAG, "Strategy 1 failed: ${e.message}")
            }

            // Strategy 2: Search series + author
            try {
                val response = openLibraryApi.searchBooks(
                    query = "\"$seriesName\" \"$author\"",
                    limit = 100
                )
                allDocs.addAll(response.docs)
            } catch (e: Exception) {
                Log.w(TAG, "Strategy 2 failed: ${e.message}")
            }

            // Strategy 3: Just author to find all their books, then filter
            try {
                val response = openLibraryApi.searchBooks(
                    query = "author:\"$author\"",
                    limit = 100
                )
                allDocs.addAll(response.docs)
            } catch (e: Exception) {
                Log.w(TAG, "Strategy 3 failed: ${e.message}")
            }

            if (allDocs.isEmpty()) {
                Log.d(TAG, "No results for series by author: $seriesName by $author")
                return null
            }

            // Remove duplicates
            val uniqueDocs = allDocs.distinctBy { it.key ?: it.title }

            processSeriesResults(seriesName, author, uniqueDocs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch series metadata by author: ${e.message}")
            null
        }
    }

    private fun processSeriesResults(
        seriesName: String,
        author: String,
        docs: List<com.mossglen.lithos.data.remote.OpenLibraryDoc>
    ): SeriesMetadata? {
        val seriesLower = seriesName.lowercase()
        val seriesWords = seriesLower.split(" ").filter { it.length > 2 }
        val authorLower = author.lowercase()
        val authorLastName = authorLower.split(" ").lastOrNull() ?: authorLower

        // First pass: Filter to this author's books
        val authorBooks = docs.filter { doc ->
            val docAuthor = doc.author_name?.firstOrNull()?.lowercase() ?: ""
            docAuthor.contains(authorLower) ||
            authorLower.contains(docAuthor) ||
            docAuthor.contains(authorLastName)
        }

        Log.d(TAG, "Found ${authorBooks.size} books by $author")

        // Second pass: Find books that match series in title or subjects
        val seriesBooks = authorBooks.filter { doc ->
            val title = doc.title?.lowercase() ?: ""
            val subjects = doc.subject?.joinToString(" ")?.lowercase() ?: ""

            // Check if title or subjects reference the series
            val inTitle = title.contains(seriesLower) ||
                seriesWords.any { word -> word.length > 3 && title.contains(word) }

            val inSubjects = subjects.contains(seriesLower) ||
                seriesWords.any { word -> word.length > 3 && subjects.contains(word) }

            inTitle || inSubjects
        }

        Log.d(TAG, "Series match: ${seriesBooks.size} books")

        // Check subjects for series tags (e.g., "Red Rising Saga", "Red Rising Trilogy")
        val booksWithSeriesSubject = authorBooks.filter { doc ->
            val subjects = doc.subject?.joinToString(" ")?.lowercase() ?: ""
            subjects.contains(seriesLower) ||
            subjects.contains("$seriesLower saga") ||
            subjects.contains("$seriesLower trilogy") ||
            subjects.contains("$seriesLower series")
        }

        Log.d(TAG, "Books with series in subjects: ${booksWithSeriesSubject.size}")

        // Known series mappings for popular series where OpenLibrary subjects are incomplete
        val knownSeriesBooks = mapOf(
            "red rising" to listOf("red rising", "golden son", "morning star", "iron gold", "dark age", "light bringer", "lightbringer"),
            "empyrean" to listOf("fourth wing", "iron flame", "onyx storm")
        )

        // Check if we have a known series mapping
        val knownBooks = knownSeriesBooks[seriesLower]
        val matchedByKnownList = if (knownBooks != null) {
            authorBooks.filter { doc ->
                val title = doc.title?.lowercase() ?: ""
                knownBooks.any { known -> title.contains(known) }
            }
        } else emptyList()

        Log.d(TAG, "Known series match: ${matchedByKnownList.size} books")

        // Use books that match series in title, subjects, or known list
        val finalBooks = when {
            matchedByKnownList.size >= 3 -> {
                Log.d(TAG, "Using known series list: ${matchedByKnownList.size} books")
                matchedByKnownList
            }
            booksWithSeriesSubject.isNotEmpty() || seriesBooks.isNotEmpty() -> {
                val combined = (booksWithSeriesSubject + seriesBooks).distinctBy { it.key ?: it.title }
                Log.d(TAG, "Using ${combined.size} books matching series criteria")
                combined
            }
            else -> {
                // Fallback: books with title starting with series name
                authorBooks.filter { doc ->
                    val title = doc.title?.lowercase() ?: ""
                    title.startsWith(seriesLower) || seriesWords.first().let { firstWord ->
                        firstWord.length > 3 && title.startsWith(firstWord)
                    }
                }.take(10)
            }
        }.distinctBy { it.title?.lowercase() }

        // Categorize books: main series vs related content
        fun isRelatedContent(title: String): String? {
            val lower = title.lowercase()
            return when {
                lower.contains("graphic novel") || lower.contains("graphic comic") -> "Graphic Novel"
                lower.contains("comic") -> "Comic"
                lower.contains("illustrated edition") || lower.contains("illustrated by") -> "Illustrated Edition"
                lower.contains("coloring book") -> "Coloring Book"
                lower.contains("companion") || lower.contains("handbook") -> "Companion"
                lower.contains("guide to") || lower.contains("encyclopedia") -> "Guide"
                lower.contains("novella") || lower.contains("short stor") -> "Novella"
                else -> null
            }
        }

        fun isExcluded(title: String): Boolean {
            val lower = title.lowercase()
            return lower.contains("box set") ||
                   lower.contains("collection") ||
                   lower.contains("books set") ||
                   lower.contains("serisi")
        }

        // Separate main books from related content
        val (relatedDocs, mainDocs) = finalBooks
            .filter { !isExcluded(it.title ?: "") }
            .partition { isRelatedContent(it.title ?: "") != null }

        Log.d(TAG, "Main books: ${mainDocs.size}, Related content: ${relatedDocs.size}")

        if (mainDocs.isEmpty() && relatedDocs.isEmpty()) {
            Log.d(TAG, "No matching books for series: $seriesName by $author")
            return null
        }

        // Helper to normalize title for deduplication
        fun normalizeTitle(title: String): String {
            return title.lowercase()
                .replace(Regex("""\s*[-:]\s*.*(saga|series|trilogy|#\d+|book\s*\d+).*$""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\s*\(.*\)"""), "")
                .replace(Regex("""[:,]\s*book\s*\d+.*$""", RegexOption.IGNORE_CASE), "")
                .trim()
        }

        // Process main books
        val bookInfoMap = mutableMapOf<String, SeriesBookInfo>()
        mainDocs.forEach { doc ->
            val title = doc.title ?: return@forEach
            val normalizedTitle = normalizeTitle(title)

            if (!bookInfoMap.containsKey(normalizedTitle)) {
                val coverUrl = doc.getCoverUrl("M")
                val cleanTitle = title
                    .replace(Regex("""\s*[-:]\s*${Regex.escape(seriesName)}.*$""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\s*\([^)]*$seriesLower[^)]*\)""", RegexOption.IGNORE_CASE), "")
                    .trim()
                    .ifEmpty { title }

                bookInfoMap[normalizedTitle] = SeriesBookInfo(
                    title = cleanTitle,
                    coverUrl = coverUrl,
                    publishYear = doc.first_publish_year,
                    contentType = "book"
                )
            } else if (doc.cover_i != null && bookInfoMap[normalizedTitle]?.coverUrl == null) {
                bookInfoMap[normalizedTitle] = bookInfoMap[normalizedTitle]!!.copy(coverUrl = doc.getCoverUrl("M"))
            }
        }

        // Process related content
        val relatedInfoMap = mutableMapOf<String, SeriesBookInfo>()
        relatedDocs.forEach { doc ->
            val title = doc.title ?: return@forEach
            val normalizedTitle = normalizeTitle(title)
            val contentType = isRelatedContent(title) ?: "Related"

            if (!relatedInfoMap.containsKey(normalizedTitle)) {
                relatedInfoMap[normalizedTitle] = SeriesBookInfo(
                    title = title,
                    coverUrl = doc.getCoverUrl("M"),
                    publishYear = doc.first_publish_year,
                    contentType = contentType
                )
            } else if (doc.cover_i != null && relatedInfoMap[normalizedTitle]?.coverUrl == null) {
                relatedInfoMap[normalizedTitle] = relatedInfoMap[normalizedTitle]!!.copy(coverUrl = doc.getCoverUrl("M"))
            }
        }

        val uniqueBooks = bookInfoMap.values.toList().sortedBy { it.publishYear ?: 9999 }
        val relatedBooks = relatedInfoMap.values.toList().sortedBy { it.publishYear ?: 9999 }
        val uniqueTitles = uniqueBooks.map { it.title }
        val coverUrl = mainDocs.firstOrNull { it.cover_i != null }?.getCoverUrl("L")
            ?: relatedDocs.firstOrNull { it.cover_i != null }?.getCoverUrl("L")

        Log.d(TAG, "Found ${uniqueTitles.size} main books, ${relatedBooks.size} related in series $seriesName")

        return SeriesMetadata(
            name = seriesName,
            totalBooks = uniqueTitles.size,
            author = author,
            coverUrl = coverUrl,
            bookTitles = uniqueTitles,
            books = uniqueBooks,
            relatedContent = relatedBooks
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
    val pageCount: Int? = null,
    val foundTitle: String? = null
) {
    fun hasGoodMetadata(): Boolean {
        return !description.isNullOrBlank() && !coverUrl.isNullOrBlank()
    }
}
