package com.rezon.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rezon.app.domain.model.Book
import com.rezon.app.domain.model.BookFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Library UI State
 */
data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val currentlyPlaying: Book? = null,
    val isPlaying: Boolean = false,
    val notStartedCount: Int = 0,
    val inProgressCount: Int = 0,
    val finishedCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * REZON Library ViewModel
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    // TODO: Inject BookRepository
    // TODO: Inject AudioServiceController
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadBooks()
    }

    /**
     * Load books from repository
     */
    private fun loadBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // TODO: Load from repository
            // Mock data for now
            val mockBooks = listOf(
                Book(
                    id = "1",
                    title = "The Martian",
                    author = "Andy Weir",
                    filePath = "/storage/audiobooks/the-martian.m4b",
                    format = BookFormat.AUDIO_M4B,
                    duration = 3600000L * 10,
                    currentPosition = 3600000L * 3,
                    narrator = "R.C. Bray"
                ),
                Book(
                    id = "2",
                    title = "Project Hail Mary",
                    author = "Andy Weir",
                    filePath = "/storage/audiobooks/hail-mary.m4b",
                    format = BookFormat.AUDIO_M4B,
                    duration = 3600000L * 16,
                    currentPosition = 0L,
                    narrator = "Ray Porter"
                ),
                Book(
                    id = "3",
                    title = "Dune",
                    author = "Frank Herbert",
                    filePath = "/storage/audiobooks/dune.mp3",
                    format = BookFormat.AUDIO_MP3,
                    duration = 3600000L * 21,
                    currentPosition = 3600000L * 21,
                    isCompleted = true,
                    narrator = "Scott Brick"
                )
            )

            val notStarted = mockBooks.count { it.progress == 0f }
            val inProgress = mockBooks.count { it.progress > 0f && !it.isCompleted }
            val finished = mockBooks.count { it.isCompleted }

            _uiState.update {
                it.copy(
                    books = mockBooks,
                    notStartedCount = notStarted,
                    inProgressCount = inProgress,
                    finishedCount = finished,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Scan folder for audiobooks
     */
    fun scanFolder() {
        // TODO: Open folder picker and scan
    }

    /**
     * Add files via file picker
     */
    fun addFiles() {
        // TODO: Open file picker
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
        // TODO: Control via audio service
    }

    /**
     * Delete a book from the library
     */
    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            // Remove book from the list
            val updatedBooks = _uiState.value.books.filter { it.id != bookId }

            val notStarted = updatedBooks.count { it.progress == 0f }
            val inProgress = updatedBooks.count { it.progress > 0f && !it.isCompleted }
            val finished = updatedBooks.count { it.isCompleted }

            _uiState.update {
                it.copy(
                    books = updatedBooks,
                    notStartedCount = notStarted,
                    inProgressCount = inProgress,
                    finishedCount = finished,
                    // Clear currently playing if it was the deleted book
                    currentlyPlaying = if (it.currentlyPlaying?.id == bookId) null else it.currentlyPlaying
                )
            }

            // TODO: Delete from repository
        }
    }
}
