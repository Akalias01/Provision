package com.mossglen.lithos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mossglen.lithos.data.Book
import com.mossglen.lithos.data.LibraryRepository
import com.mossglen.lithos.data.MetadataRepository
import com.mossglen.lithos.data.Series
import com.mossglen.lithos.data.SeriesInfo
import com.mossglen.lithos.data.SeriesMetadata
import com.mossglen.lithos.data.groupBySeries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * REVERIE Series ViewModel
 *
 * Manages series grouping, filtering, and manipulation.
 * Provides flows for UI observation and methods for series management.
 */
@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    // Cached series metadata from external sources
    private val _seriesMetadataCache = MutableStateFlow<Map<String, SeriesMetadata>>(emptyMap())
    val seriesMetadataCache: StateFlow<Map<String, SeriesMetadata>> = _seriesMetadataCache

    // Loading state for metadata fetching
    private val _isLoadingMetadata = MutableStateFlow(false)
    val isLoadingMetadata: StateFlow<Boolean> = _isLoadingMetadata

    // Result message for snackbar feedback
    private val _metadataMessage = MutableStateFlow<String?>(null)
    val metadataMessage: StateFlow<String?> = _metadataMessage

    // Excluded books from "Not in Library" section (series name -> set of excluded titles)
    private val _excludedBooks = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val excludedBooks: StateFlow<Map<String, Set<String>>> = _excludedBooks

    fun clearMetadataMessage() {
        _metadataMessage.value = null
    }

    /**
     * Exclude a book from the "Not in Library" section for a series
     */
    fun excludeBookFromSeries(seriesName: String, bookTitle: String) {
        val current = _excludedBooks.value[seriesName] ?: emptySet()
        _excludedBooks.value = _excludedBooks.value + (seriesName to (current + bookTitle.lowercase()))
        _metadataMessage.value = "Removed from series"
    }

    /**
     * Check if a book is excluded from a series
     */
    fun isBookExcluded(seriesName: String, bookTitle: String): Boolean {
        return _excludedBooks.value[seriesName]?.contains(bookTitle.lowercase()) == true
    }

    /**
     * Fetch metadata for a specific book from external sources
     */
    fun fetchBookMetadata(book: com.mossglen.lithos.data.Book) {
        viewModelScope.launch {
            _isLoadingMetadata.value = true
            _metadataMessage.value = "Fetching synopsis..."
            try {
                metadataRepository.fetchAndSaveMetadata(book)
                _metadataMessage.value = "Synopsis updated"
            } catch (e: Exception) {
                _metadataMessage.value = "Failed to fetch synopsis"
            } finally {
                _isLoadingMetadata.value = false
            }
        }
    }

    /**
     * Force refresh series metadata, clearing cache first
     */
    fun refreshSeriesMetadata(seriesName: String, author: String?) {
        viewModelScope.launch {
            // Clear cache for this series
            _seriesMetadataCache.value = _seriesMetadataCache.value - seriesName
            _excludedBooks.value = _excludedBooks.value - seriesName

            _isLoadingMetadata.value = true
            _metadataMessage.value = "Refreshing series info..."

            try {
                var metadata: SeriesMetadata? = null

                if (author != null) {
                    metadata = metadataRepository.fetchSeriesMetadataByAuthor(seriesName, author)
                }

                if (metadata == null) {
                    metadata = metadataRepository.fetchSeriesMetadata(seriesName)
                }

                if (metadata != null) {
                    _seriesMetadataCache.value = _seriesMetadataCache.value + (seriesName to metadata)
                    val coversFound = metadata.books.count { it.coverUrl != null }
                    _metadataMessage.value = "Found ${metadata.totalBooks} books ($coversFound with covers)"
                } else {
                    _metadataMessage.value = "No series info found"
                }
            } catch (e: Exception) {
                _metadataMessage.value = "Failed to refresh"
            } finally {
                _isLoadingMetadata.value = false
            }
        }
    }

    // All books in the library
    private val allBooks = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All series grouped from books
    val allSeries: StateFlow<List<Series>> = allBooks
        .map { books -> books.groupBySeries() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Books without series
    val booksWithoutSeries: StateFlow<List<Book>> = allBooks
        .map { books -> books.filter { it.seriesInfo.isBlank() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Series stats
    val seriesStats: StateFlow<SeriesStats> = allSeries
        .map { series ->
            SeriesStats(
                totalSeries = series.size,
                totalBooksInSeries = series.sumOf { it.bookCount },
                completedSeries = series.count { it.finishedCount == it.bookCount },
                inProgressSeries = series.count { it.inProgressCount > 0 }
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SeriesStats(0, 0, 0, 0)
        )

    /**
     * Get a specific series by name
     */
    fun getSeriesByName(seriesName: String): StateFlow<Series?> {
        return allSeries
            .map { series -> series.find { it.name.equals(seriesName, ignoreCase = true) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    /**
     * Assign a book to a series
     */
    fun assignBookToSeries(bookId: String, seriesName: String, bookNumber: Float?) {
        viewModelScope.launch {
            val seriesInfo = SeriesInfo.format(seriesName, bookNumber)
            repository.updateBook(
                repository.getBookById(bookId)?.copy(seriesInfo = seriesInfo) ?: return@launch
            )
        }
    }

    /**
     * Remove a book from its series
     */
    fun removeBookFromSeries(bookId: String) {
        viewModelScope.launch {
            repository.updateBook(
                repository.getBookById(bookId)?.copy(seriesInfo = "") ?: return@launch
            )
        }
    }

    /**
     * Rename a series (updates all books in that series)
     */
    fun renameSeries(oldName: String, newName: String) {
        viewModelScope.launch {
            // Get all books in the old series
            val series = allSeries.value.find { it.name.equals(oldName, ignoreCase = true) }
                ?: return@launch

            // Update each book with the new series name, preserving book numbers
            series.books.forEach { book ->
                val seriesInfo = SeriesInfo.parse(book.seriesInfo)
                if (seriesInfo != null) {
                    val newSeriesInfo = SeriesInfo.format(newName, seriesInfo.bookNumber)
                    repository.updateBook(book.copy(seriesInfo = newSeriesInfo))
                }
            }
        }
    }

    /**
     * Merge two series into one
     */
    fun mergeSeries(sourceSeriesName: String, targetSeriesName: String) {
        viewModelScope.launch {
            // Get all books from the source series
            val sourceSeries = allSeries.value.find {
                it.name.equals(sourceSeriesName, ignoreCase = true)
            } ?: return@launch

            // Update each book to the target series
            sourceSeries.books.forEach { book ->
                val seriesInfo = SeriesInfo.parse(book.seriesInfo)
                if (seriesInfo != null) {
                    val newSeriesInfo = SeriesInfo.format(targetSeriesName, seriesInfo.bookNumber)
                    repository.updateBook(book.copy(seriesInfo = newSeriesInfo))
                }
            }
        }
    }

    /**
     * Split a series by moving specific books to a new series
     */
    fun splitSeries(bookIds: List<String>, newSeriesName: String) {
        viewModelScope.launch {
            bookIds.forEach { bookId ->
                val book = repository.getBookById(bookId) ?: return@forEach
                val seriesInfo = SeriesInfo.parse(book.seriesInfo)
                if (seriesInfo != null) {
                    val newSeriesInfo = SeriesInfo.format(newSeriesName, seriesInfo.bookNumber)
                    repository.updateBook(book.copy(seriesInfo = newSeriesInfo))
                }
            }
        }
    }

    /**
     * Reorder a book within a series
     */
    fun reorderBookInSeries(bookId: String, newBookNumber: Float) {
        viewModelScope.launch {
            val book = repository.getBookById(bookId) ?: return@launch
            val seriesInfo = SeriesInfo.parse(book.seriesInfo) ?: return@launch
            val newSeriesInfo = SeriesInfo.format(seriesInfo.name, newBookNumber)
            repository.updateBook(book.copy(seriesInfo = newSeriesInfo))
        }
    }

    /**
     * Auto-detect and fix series numbering based on title patterns
     */
    fun autoFixSeriesNumbering(seriesName: String) {
        viewModelScope.launch {
            val series = allSeries.value.find { it.name.equals(seriesName, ignoreCase = true) }
                ?: return@launch

            // Try to extract numbers from book titles
            series.books.forEach { book ->
                val titleNumber = extractNumberFromTitle(book.title)
                if (titleNumber != null) {
                    val newSeriesInfo = SeriesInfo.format(seriesName, titleNumber)
                    repository.updateBook(book.copy(seriesInfo = newSeriesInfo))
                }
            }
        }
    }

    /**
     * Extract book number from title
     * Supports patterns like "Book Title 1", "Book Title: Part 2", etc.
     */
    private fun extractNumberFromTitle(title: String): Float? {
        val patterns = listOf(
            """(?:Book|Part|Volume|Vol\.?)\s*(\d+(?:\.\d+)?)""".toRegex(RegexOption.IGNORE_CASE),
            """\b(\d+(?:\.\d+)?)\s*$""".toRegex(),
            """^(\d+(?:\.\d+)?)\b""".toRegex()
        )

        for (pattern in patterns) {
            pattern.find(title)?.let { match ->
                return match.groupValues[1].toFloatOrNull()
            }
        }

        return null
    }

    /**
     * Mark entire series as finished
     */
    fun markSeriesAsFinished(seriesName: String, finished: Boolean) {
        viewModelScope.launch {
            val series = allSeries.value.find { it.name.equals(seriesName, ignoreCase = true) }
                ?: return@launch

            series.books.forEach { book ->
                val newBook = if (finished) {
                    book.copy(isFinished = true, progress = book.duration)
                } else {
                    book.copy(isFinished = false, progress = 0L)
                }
                repository.updateBook(newBook)
            }
        }
    }

    /**
     * Delete entire series (removes all books in the series)
     */
    fun deleteSeries(seriesName: String) {
        viewModelScope.launch {
            val series = allSeries.value.find { it.name.equals(seriesName, ignoreCase = true) }
                ?: return@launch

            series.books.forEach { book ->
                repository.deleteById(book.id)
            }
        }
    }

    /**
     * Fetch series metadata from external sources (OpenLibrary).
     * Uses both series name and author to find accurate results.
     * Prioritizes author search for more accurate results.
     */
    fun fetchSeriesMetadata(seriesName: String, author: String? = null) {
        viewModelScope.launch {
            // Check cache - allow refresh if cached result seems incomplete
            val cached = _seriesMetadataCache.value[seriesName]
            if (cached != null && cached.totalBooks >= 5 && cached.books.size >= 3) {
                return@launch
            }

            _isLoadingMetadata.value = true
            _metadataMessage.value = "Fetching series info..."

            var metadata: SeriesMetadata? = null

            try {
                // Prioritize author search for more accurate results
                if (author != null) {
                    metadata = metadataRepository.fetchSeriesMetadataByAuthor(seriesName, author)
                }

                // Fall back to series name search if author search failed
                if (metadata == null) {
                    metadata = metadataRepository.fetchSeriesMetadata(seriesName)
                }

                if (metadata != null) {
                    // Only update cache if new result is better than cached
                    if (cached == null || metadata.totalBooks > cached.totalBooks || metadata.books.size > cached.books.size) {
                        _seriesMetadataCache.value = _seriesMetadataCache.value + (seriesName to metadata)
                    }
                    val coversFound = metadata.books.count { it.coverUrl != null }
                    _metadataMessage.value = "Found ${metadata.totalBooks} books ($coversFound with covers)"
                } else {
                    _metadataMessage.value = "No series info found"
                }
            } catch (e: Exception) {
                _metadataMessage.value = "Failed to fetch series info"
            } finally {
                _isLoadingMetadata.value = false
            }
        }
    }

    /**
     * Detect series from a book's title and author.
     * Useful when seriesInfo field is empty but book is clearly part of a series.
     */
    fun detectSeriesFromBook(book: Book): String? {
        val title = book.title

        // Common series patterns in titles
        val patterns = listOf(
            // "Title: Book 1" or "Title - Book 2"
            """^(.+?)(?::|-)?\\s*(?:Book|Volume|Part|#)\\s*\\d+""".toRegex(RegexOption.IGNORE_CASE),
            // "Series Name 1" or "Series Name: The Subtitle"
            """^(.+?)\\s+\\d+(?:\\s*[:-]|$)""".toRegex(),
            // "The Series Name and the Something" (Harry Potter style)
            """^(The\\s+.+?)\\s+and\\s+the\\s+""".toRegex(RegexOption.IGNORE_CASE),
            // "Series Name: Subtitle" where subtitle contains book indicator
            """^(.+?):\\s*.+(?:Book|Volume|Part)\\s*\\d+""".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(title)
            if (match != null && match.groupValues.size > 1) {
                val potentialSeries = match.groupValues[1].trim()
                if (potentialSeries.length in 3..50) {
                    return potentialSeries
                }
            }
        }

        return null
    }

    /**
     * Get cached series metadata for a series name
     */
    fun getSeriesMetadata(seriesName: String): StateFlow<SeriesMetadata?> {
        return _seriesMetadataCache
            .map { it[seriesName] }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }
}

/**
 * Series statistics data class
 */
data class SeriesStats(
    val totalSeries: Int,
    val totalBooksInSeries: Int,
    val completedSeries: Int,
    val inProgressSeries: Int
)
