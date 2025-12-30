package com.mossglen.reverie.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.mossglen.reverie.data.Book
import com.mossglen.reverie.data.LibraryRepository
import com.mossglen.reverie.data.MetadataRepository
import com.mossglen.reverie.worker.AudioSplitWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption { RECENTS, AUTHOR, TITLE, SERIES, PATH }
enum class MasterFilter { AUDIO, READ } // The "Master Switch"
enum class LibraryViewMode { LIST, GRID, RECENTS, SERIES }

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val metadataRepository: MetadataRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _rawBooks = repository.allBooks

    // Legacy accessor for MainLayout start destination check
    val books: StateFlow<List<Book>> = _rawBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Most recently played book for Smart Resume nav button
    val mostRecentBook: StateFlow<Book?> = repository.mostRecentBook
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI States
    private val _sortOption = MutableStateFlow(SortOption.RECENTS)
    val sortOption = _sortOption.asStateFlow()

    // Master Filter - Default to Audiobooks
    private val _masterFilter = MutableStateFlow(MasterFilter.AUDIO)
    val masterFilter = _masterFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _showHeaders = MutableStateFlow(false)
    val showHeaders = _showHeaders.asStateFlow()

    // View mode - persists across screen navigations
    private val _viewMode = MutableStateFlow(LibraryViewMode.GRID)
    val viewMode = _viewMode.asStateFlow()

    // The Grid/Matrix Logic - Returns list of lists for each tab
    val libraryData: StateFlow<List<List<Book>>> = combine(
        _rawBooks,
        _sortOption,
        _masterFilter,
        _searchQuery
    ) { books, sort, filter, query ->
        Log.d("LibraryViewModel", "=== LIBRARY DATA UPDATE ===")
        Log.d("LibraryViewModel", "Raw books count: ${books.size}")
        books.forEach { book ->
            Log.d("LibraryViewModel", "  Book: ${book.title}, format=${book.format}, id=${book.id.take(8)}")
        }
        // 1. Master Filter (Audio vs Read/Text)
        val typeFiltered = when (filter) {
            MasterFilter.AUDIO -> books.filter { it.format == "AUDIO" }
            MasterFilter.READ -> books.filter {
                it.format == "TEXT" || it.format == "DOCUMENT" ||
                it.format == "PDF" || it.format == "EPUB"
            }
        }
        Log.d("LibraryViewModel", "After filter ($filter): ${typeFiltered.size} books")

        // 2. Search Filter
        val searchFiltered = if (query.isBlank()) typeFiltered else typeFiltered.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.author.contains(query, ignoreCase = true)
        }

        // 3. Sort
        val sorted = when (sort) {
            SortOption.RECENTS -> searchFiltered.sortedByDescending { it.lastPlayedTimestamp }
            SortOption.AUTHOR -> searchFiltered.sortedBy { it.author.lowercase() }
            SortOption.TITLE -> searchFiltered.sortedBy { it.title.lowercase() }
            SortOption.SERIES -> searchFiltered.sortedBy { it.seriesInfo.lowercase() }
            SortOption.PATH -> searchFiltered.sortedBy { it.filePath }
        }

        // 4. Split into Tabs: [Not Started, In Progress, Finished, All]
        listOf(
            sorted.filter { it.progress == 0L && !it.isFinished }, // 0: Not Started
            sorted.filter { it.progress > 0L && !it.isFinished },  // 1: In Progress
            sorted.filter { it.isFinished },                       // 2: Finished
            sorted                                                 // 3: All
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        listOf(emptyList(), emptyList(), emptyList(), emptyList())
    )

    fun importFile(uri: Uri) {
        Log.d("LibraryViewModel", ">>> importFile called with URI: $uri")
        viewModelScope.launch {
            val book = repository.importBook(uri)
            Log.d("LibraryViewModel", ">>> importFile result: ${book?.title ?: "NULL"}")
        }
    }

    /**
     * Import a file and return the resulting Book (for handling external file intents)
     */
    suspend fun importFileAndReturn(uri: Uri): Book? {
        return repository.importBook(uri)
    }

    fun setSort(option: SortOption) {
        _sortOption.value = option
    }

    fun setMasterFilter(filter: MasterFilter) {
        _masterFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleHeaders() {
        _showHeaders.value = !_showHeaders.value
    }

    fun setViewMode(mode: LibraryViewMode) {
        _viewMode.value = mode
    }

    fun cycleViewMode() {
        // Cycle through LIST -> GRID -> RECENTS (SERIES removed - integrated into list/recents)
        _viewMode.value = when (_viewMode.value) {
            LibraryViewMode.LIST -> LibraryViewMode.GRID
            LibraryViewMode.GRID -> LibraryViewMode.RECENTS
            LibraryViewMode.RECENTS -> LibraryViewMode.LIST
            LibraryViewMode.SERIES -> LibraryViewMode.LIST // Fallback if somehow in SERIES
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch { repository.deleteById(bookId) }
    }

    /**
     * Delete a book with full cleanup options.
     *
     * @param bookId The ID of the book to delete
     * @param deleteFiles If true, also delete the audio files from device storage
     */
    fun deleteBookWithCleanup(bookId: String, deleteFiles: Boolean) {
        viewModelScope.launch {
            val result = repository.deleteBookWithCleanup(bookId, deleteFiles)
            Log.d("LibraryViewModel", "deleteBookWithCleanup result: $result, deleteFiles=$deleteFiles")
        }
    }

    /**
     * Check if a book's files are stored in the torrent download directory.
     * This helps determine if we should offer the "delete files" option in the UI.
     */
    fun isBookFromTorrent(book: Book): Boolean {
        return repository.isBookFromTorrent(book)
    }

    /**
     * Get a book by ID for checking properties before deletion.
     */
    suspend fun getBookById(bookId: String): Book? {
        return repository.getBookById(bookId)
    }

    fun clearLibrary() {
        viewModelScope.launch { repository.clearAll() }
    }

    fun updateBookMetadata(id: String, title: String, author: String, series: String = "") {
        viewModelScope.launch {
            repository.updateMetadata(id, title, author, series)
        }
    }

    /**
     * Update book progress (used for EPUB chapter tracking)
     */
    fun updateBookProgress(bookId: String, progress: Long) {
        viewModelScope.launch {
            repository.updateProgress(bookId, progress)
        }
    }

    fun markBookStatus(book: Book, status: String) {
        viewModelScope.launch {
            when (status) {
                "Finished" -> repository.updateStatus(book.id, true, book.duration)
                "Unfinished" -> repository.updateStatus(book.id, false, 0L)
                "In Progress" -> repository.updateStatus(book.id, false, book.progress)
            }
        }
    }

    /**
     * Fetch metadata from online sources (Google Books, OpenLibrary) and update the book.
     */
    suspend fun fetchMetadata(book: Book) {
        metadataRepository.fetchAndSaveMetadata(book)
    }

    /**
     * Update bookmarks for a book (used by BookmarksScreen)
     */
    fun updateBookmarks(bookId: String, bookmarks: List<Long>) {
        viewModelScope.launch {
            val book = repository.getBookById(bookId)
            if (book != null) {
                repository.updateBook(book.copy(bookmarks = bookmarks))
            }
        }
    }

    /**
     * Add a bookmark with a note
     */
    fun addBookmarkWithNote(bookId: String, positionMs: Long, note: String) {
        viewModelScope.launch {
            repository.addBookmarkWithNote(bookId, positionMs, note)
        }
    }

    /**
     * Update the note for an existing bookmark
     */
    fun updateBookmarkNote(bookId: String, positionMs: Long, note: String) {
        viewModelScope.launch {
            val book = repository.getBookById(bookId)
            if (book != null) {
                val updatedNotes = book.bookmarkNotes.toMutableMap()
                if (note.isNotBlank()) {
                    updatedNotes[positionMs] = note
                } else {
                    updatedNotes.remove(positionMs)
                }
                repository.updateBook(book.copy(bookmarkNotes = updatedNotes))
            }
        }
    }

    /**
     * Delete a bookmark and its note
     */
    fun deleteBookmark(bookId: String, positionMs: Long) {
        viewModelScope.launch {
            val book = repository.getBookById(bookId)
            if (book != null) {
                val updatedBookmarks = book.bookmarks.toMutableList()
                updatedBookmarks.remove(positionMs)

                val updatedNotes = book.bookmarkNotes.toMutableMap()
                updatedNotes.remove(positionMs)

                repository.updateBook(book.copy(
                    bookmarks = updatedBookmarks,
                    bookmarkNotes = updatedNotes
                ))
            }
        }
    }

    /**
     * Re-extract chapters for a specific book.
     * Useful for books imported before chapter detection was implemented.
     */
    fun extractChaptersForBook(bookId: String, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.extractChaptersForBook(bookId)
            if (success) {
                val book = repository.getBookById(bookId)
                onComplete(book?.chapters?.size ?: 0)
            } else {
                onComplete(0)
            }
        }
    }

    /**
     * Re-extract chapters for all books that don't have them.
     * This is useful for migrating existing libraries.
     */
    fun extractChaptersForAllBooks(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.extractChaptersForAllBooks()
            onComplete(count)
        }
    }

    /**
     * Split a book into multiple segments using background processing.
     *
     * @param bookId The ID of the book to split
     * @param segments List of Triple(title, startMs, endMs) for each segment
     * @param keepOriginal Whether to keep the original book after splitting
     * @param onComplete Callback when split is successful
     * @param onError Callback when split fails with error message
     */
    fun splitBook(
        bookId: String,
        segments: List<Triple<String, Long, Long>>,
        keepOriginal: Boolean,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("LibraryViewModel", "Starting split for book: $bookId")
                Log.d("LibraryViewModel", "Segments: ${segments.size}, keepOriginal: $keepOriginal")

                // Create and enqueue the work request
                val workRequest = AudioSplitWorker.createWorkRequest(
                    bookId = bookId,
                    segments = segments,
                    keepOriginal = keepOriginal
                )

                workManager.enqueue(workRequest)

                // Observe the work status
                workManager.getWorkInfoByIdFlow(workRequest.id)
                    .collect { workInfo ->
                        when (workInfo?.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                Log.d("LibraryViewModel", "Split work succeeded")
                                val newBookIds = workInfo.outputData.getStringArray(AudioSplitWorker.KEY_NEW_BOOK_IDS)
                                Log.d("LibraryViewModel", "Created ${newBookIds?.size ?: 0} new books")
                                onComplete()
                            }
                            WorkInfo.State.FAILED -> {
                                val errorMessage = workInfo.outputData.getString(AudioSplitWorker.KEY_ERROR_MESSAGE)
                                    ?: "Unknown error during split"
                                Log.e("LibraryViewModel", "Split work failed: $errorMessage")
                                onError(errorMessage)
                            }
                            WorkInfo.State.CANCELLED -> {
                                Log.w("LibraryViewModel", "Split work cancelled")
                                onError("Split operation was cancelled")
                            }
                            else -> {
                                // Still running or enqueued
                                val progress = workInfo?.progress
                                val current = progress?.getInt(AudioSplitWorker.PROGRESS_CURRENT, 0) ?: 0
                                val total = progress?.getInt(AudioSplitWorker.PROGRESS_TOTAL, 0) ?: 0
                                if (total > 0) {
                                    Log.d("LibraryViewModel", "Split progress: $current/$total")
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Failed to start split work", e)
                onError(e.message ?: "Failed to start split operation")
            }
        }
    }
}
