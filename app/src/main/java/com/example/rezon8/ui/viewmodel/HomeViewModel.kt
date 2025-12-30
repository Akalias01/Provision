package com.mossglen.reverie.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mossglen.reverie.data.Book
import com.mossglen.reverie.data.LibraryRepository
import com.mossglen.reverie.data.ListeningStatsRepository
import com.mossglen.reverie.data.ListeningStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HomeViewModel - Manages the Home Screen dashboard data
 *
 * Provides:
 * - Currently playing / last played book
 * - Quick stats (listening time, books in progress, finished, streak)
 * - Recent activity (recently played books)
 * - Recommendations (continue series, unfinished books)
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val statsRepository: ListeningStatsRepository
) : ViewModel() {

    // All books from library
    private val _allBooks = libraryRepository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Most recent book (last played)
    val mostRecentBook: StateFlow<Book?> = libraryRepository.mostRecentBook
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Listening stats
    val stats: StateFlow<ListeningStats> = flow {
        emit(statsRepository.getStats())
        // Collect updates
        statsRepository.totalTimeMs.collect {
            emit(statsRepository.getStats())
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ListeningStats(0, 0, 0, 0, 0, 0, 0)
    )

    // Books in progress (have progress > 0, not finished)
    val booksInProgress: StateFlow<List<Book>> = _allBooks.map { books ->
        books.filter { it.progress > 0 && !it.isFinished && it.format == "AUDIO" }
            .sortedByDescending { it.lastPlayedTimestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Finished books count
    val finishedBooksCount: StateFlow<Int> = _allBooks.map { books ->
        books.count { it.isFinished }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Recently played books (last 10)
    val recentlyPlayed: StateFlow<List<Book>> = _allBooks.map { books ->
        books.filter { it.lastPlayedTimestamp > 0 && it.format == "AUDIO" }
            .sortedByDescending { it.lastPlayedTimestamp }
            .take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unfinished books (not started or in progress, sorted by date added)
    val unfinishedBooks: StateFlow<List<Book>> = _allBooks.map { books ->
        books.filter { !it.isFinished && it.format == "AUDIO" }
            .sortedByDescending { it.lastPlayedTimestamp }
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Series books - books that have series info and aren't finished
    val continueSeries: StateFlow<List<Book>> = _allBooks.map { books ->
        books.filter {
            it.seriesInfo.isNotBlank() &&
            !it.isFinished &&
            it.format == "AUDIO"
        }
        .sortedBy { it.seriesInfo }
        .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Distinct series list - grouped by series name with book count
    // Returns list of Pair<seriesName, List<Book>>
    val distinctSeries: StateFlow<List<Pair<String, List<Book>>>> = _allBooks.map { books ->
        books.filter { it.seriesInfo.isNotBlank() && it.format == "AUDIO" }
            .groupBy { it.seriesInfo }
            .map { (seriesName, seriesBooks) -> seriesName to seriesBooks.sortedBy { it.title } }
            .sortedByDescending { (_, seriesBooks) ->
                seriesBooks.maxOfOrNull { it.lastPlayedTimestamp } ?: 0L
            }
            .take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Calculate time remaining for a book in milliseconds
     */
    fun getTimeRemaining(book: Book): Long {
        return (book.duration - book.progress).coerceAtLeast(0)
    }

    /**
     * Format milliseconds to human-readable time remaining
     */
    fun formatTimeRemaining(ms: Long): String {
        val totalMinutes = ms / 1000 / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m left"
            minutes > 0 -> "${minutes}m left"
            else -> "Almost done"
        }
    }
}
