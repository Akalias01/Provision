package com.mossglen.reverie.data

/**
 * REVERIE Series Data Models
 *
 * Handles series grouping, parsing, and management for audiobooks/ebooks.
 * Supports various series naming formats commonly found in metadata.
 */

/**
 * Represents a parsed series with name and book number
 */
data class SeriesInfo(
    val name: String,
    val bookNumber: Float? = null
) {
    companion object {
        /**
         * Parse series information from the seriesInfo field in Book entity.
         * Supports formats like:
         * - "The Expanse #1"
         * - "Harry Potter, Book 1"
         * - "Mistborn: Book 2"
         * - "The Dark Tower 3"
         * - "Foundation Series - 4"
         * - "1.5" (short story/novella number)
         */
        fun parse(seriesInfo: String?): SeriesInfo? {
            if (seriesInfo.isNullOrBlank()) return null

            val trimmed = seriesInfo.trim()

            // Pattern 1: "Series Name #1" or "Series Name #1.5"
            val hashPattern = """^(.+?)\s*#\s*(\d+(?:\.\d+)?)$""".toRegex()
            hashPattern.find(trimmed)?.let { match ->
                val name = match.groupValues[1].trim()
                val number = match.groupValues[2].toFloatOrNull()
                return SeriesInfo(name, number)
            }

            // Pattern 2: "Series Name, Book 1" or "Series Name: Book 2"
            val bookPattern = """^(.+?)[,:]\s*(?:Book|book|Vol|vol|Volume|volume)\s*(\d+(?:\.\d+)?)$""".toRegex()
            bookPattern.find(trimmed)?.let { match ->
                val name = match.groupValues[1].trim()
                val number = match.groupValues[2].toFloatOrNull()
                return SeriesInfo(name, number)
            }

            // Pattern 3: "Series Name 1" or "Series Name 2.5"
            val numberPattern = """^(.+?)\s+(\d+(?:\.\d+)?)$""".toRegex()
            numberPattern.find(trimmed)?.let { match ->
                val name = match.groupValues[1].trim()
                val number = match.groupValues[2].toFloatOrNull()
                return SeriesInfo(name, number)
            }

            // Pattern 4: "Series Name - 3"
            val dashPattern = """^(.+?)\s*-\s*(\d+(?:\.\d+)?)$""".toRegex()
            dashPattern.find(trimmed)?.let { match ->
                val name = match.groupValues[1].trim()
                val number = match.groupValues[2].toFloatOrNull()
                return SeriesInfo(name, number)
            }

            // If no number pattern matches, treat entire string as series name
            return SeriesInfo(trimmed, null)
        }

        /**
         * Format series info for display
         */
        fun format(name: String, bookNumber: Float?): String {
            return if (bookNumber != null) {
                val numberStr = if (bookNumber % 1 == 0f) {
                    bookNumber.toInt().toString()
                } else {
                    bookNumber.toString()
                }
                "$name #$numberStr"
            } else {
                name
            }
        }
    }

    /**
     * Format for display
     */
    fun format(): String = format(name, bookNumber)
}

/**
 * Represents a grouped series with all books
 */
data class Series(
    val name: String,
    val books: List<Book>
) {
    /**
     * Total duration of all books in the series
     */
    val totalDuration: Long
        get() = books.sumOf { it.duration }

    /**
     * Number of books in the series
     */
    val bookCount: Int
        get() = books.size

    /**
     * Number of finished books
     */
    val finishedCount: Int
        get() = books.count { it.isFinished }

    /**
     * Number of books in progress
     */
    val inProgressCount: Int
        get() = books.count { it.progress > 0 && !it.isFinished }

    /**
     * Overall series progress (0.0 to 1.0)
     * Based on number of finished books
     */
    val progress: Float
        get() = if (bookCount > 0) finishedCount.toFloat() / bookCount else 0f

    /**
     * Books sorted by their series number (if available)
     */
    val booksSorted: List<Book>
        get() = books.sortedBy { book ->
            SeriesInfo.parse(book.seriesInfo)?.bookNumber ?: Float.MAX_VALUE
        }

    /**
     * First book cover URL for series thumbnail
     */
    val coverUrl: String?
        get() = booksSorted.firstOrNull()?.coverUrl

    /**
     * All cover URLs for creating a collage (up to 4)
     */
    val coverUrls: List<String>
        get() = booksSorted.take(4).mapNotNull { it.coverUrl }

    /**
     * Get the next unread book in the series
     */
    fun getNextUnreadBook(): Book? {
        return booksSorted.firstOrNull { !it.isFinished }
    }

    /**
     * Get the currently reading book (if any)
     */
    fun getCurrentlyReadingBook(): Book? {
        return booksSorted.firstOrNull { it.progress > 0 && !it.isFinished }
    }
}

/**
 * Extension function to group books by series
 */
fun List<Book>.groupBySeries(): List<Series> {
    // Filter books that have series info
    val booksWithSeries = this.filter { it.seriesInfo.isNotBlank() }

    // Group by series name (parsed)
    val groupedBySeries = booksWithSeries
        .mapNotNull { book ->
            val seriesInfo = SeriesInfo.parse(book.seriesInfo)
            if (seriesInfo != null) book to seriesInfo.name else null
        }
        .groupBy({ it.second }, { it.first })

    // Convert to Series objects and sort by series name
    return groupedBySeries.map { (name, books) ->
        Series(name, books)
    }.sortedBy { it.name.lowercase() }
}

/**
 * Extension function to get all books not in any series
 */
fun List<Book>.getBooksWithoutSeries(): List<Book> {
    return this.filter { it.seriesInfo.isBlank() }
}

/**
 * Extension function to check if a book belongs to a series
 */
fun Book.belongsToSeries(): Boolean {
    return seriesInfo.isNotBlank() && SeriesInfo.parse(seriesInfo) != null
}

/**
 * Extension function to get parsed series info for a book
 */
fun Book.getSeriesInfo(): SeriesInfo? {
    return SeriesInfo.parse(seriesInfo)
}
