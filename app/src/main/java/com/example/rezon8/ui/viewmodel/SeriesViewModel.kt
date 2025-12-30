package com.mossglen.reverie.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mossglen.reverie.data.Book
import com.mossglen.reverie.data.LibraryRepository
import com.mossglen.reverie.data.Series
import com.mossglen.reverie.data.SeriesInfo
import com.mossglen.reverie.data.groupBySeries
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
    private val repository: LibraryRepository
) : ViewModel() {

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
