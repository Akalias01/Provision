package com.rezon.app.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
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
    val error: String? = null,
    val isGoogleDriveConnected: Boolean = false,
    val isDropboxConnected: Boolean = false
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
     * Scan folder for audiobooks from URI
     */
    fun scanFolderFromUri(uri: Uri) {
        viewModelScope.launch {
            // TODO: Implement actual folder scanning
            // For now, just log that we received the URI
            android.util.Log.d("LibraryViewModel", "Scanning folder: $uri")
            // TODO: Use DocumentFile to iterate through the folder
            // and add audio files to the library
        }
    }

    /**
     * Add files from selected URIs
     */
    fun addFilesFromUris(uris: List<Uri>) {
        viewModelScope.launch {
            // TODO: Implement actual file import
            android.util.Log.d("LibraryViewModel", "Adding ${uris.size} files")
            // TODO: Parse audio metadata and add to library
        }
    }

    /**
     * Scan folder for audiobooks (legacy)
     */
    fun scanFolder() {
        // TODO: Open folder picker and scan
    }

    /**
     * Add files via file picker (legacy)
     */
    fun addFiles() {
        // TODO: Open file picker
    }

    /**
     * Connect to Google Drive
     */
    fun connectGoogleDrive(context: Context) {
        // Launch Google OAuth flow
        // Using Google Sign-In API
        val googleSignInIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=YOUR_CLIENT_ID.apps.googleusercontent.com" +
                "&redirect_uri=com.rezon.app:/oauth2callback" +
                "&response_type=code" +
                "&scope=https://www.googleapis.com/auth/drive.readonly")
        }
        try {
            context.startActivity(googleSignInIntent)
        } catch (e: Exception) {
            android.util.Log.e("LibraryViewModel", "Failed to launch Google OAuth: ${e.message}")
        }
    }

    /**
     * Connect to Dropbox
     */
    fun connectDropbox(context: Context) {
        // Launch Dropbox OAuth flow
        val dropboxAuthIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.dropbox.com/oauth2/authorize?" +
                "client_id=YOUR_DROPBOX_APP_KEY" +
                "&redirect_uri=com.rezon.app://dropbox-auth" +
                "&response_type=token")
        }
        try {
            context.startActivity(dropboxAuthIntent)
        } catch (e: Exception) {
            android.util.Log.e("LibraryViewModel", "Failed to launch Dropbox OAuth: ${e.message}")
        }
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
