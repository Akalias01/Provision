package com.mossglen.lithos.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mossglen.lithos.data.CoverArtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing cover art operations
 */
@HiltViewModel
class CoverArtViewModel @Inject constructor(
    private val coverArtRepository: CoverArtRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<CoverArtRepository.CoverSearchResult>>(emptyList())
    val searchResults: StateFlow<List<CoverArtRepository.CoverSearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Search for cover art from online sources
     */
    fun searchCoverArt(title: String, author: String? = null) {
        viewModelScope.launch {
            try {
                _isSearching.value = true
                _errorMessage.value = null
                _searchResults.value = emptyList()

                val results = coverArtRepository.searchCoverArt(title, author)

                if (results.isEmpty()) {
                    _errorMessage.value = "No covers found. Try a different search term."
                } else {
                    _searchResults.value = results
                }
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * Download and save cover from URL
     */
    suspend fun downloadAndSaveCover(url: String): String? {
        return try {
            _isDownloading.value = true
            _errorMessage.value = null
            coverArtRepository.downloadAndSaveCover(url)
        } catch (e: Exception) {
            _errorMessage.value = "Download failed: ${e.message}"
            null
        } finally {
            _isDownloading.value = false
        }
    }

    /**
     * Save cover from gallery URI
     */
    suspend fun saveCoverFromGallery(uri: Uri): String? {
        return try {
            _isDownloading.value = true
            _errorMessage.value = null
            coverArtRepository.saveCoverFromGallery(uri)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to save gallery image: ${e.message}"
            null
        } finally {
            _isDownloading.value = false
        }
    }

    /**
     * Update book cover in database
     */
    fun updateBookCover(bookId: String, coverUrl: String?) {
        viewModelScope.launch {
            try {
                coverArtRepository.updateBookCover(bookId, coverUrl)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update cover: ${e.message}"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear search results
     */
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    /**
     * Validate if a URL is a valid image URL
     */
    fun isValidImageUrl(url: String): Boolean {
        return coverArtRepository.isValidImageUrl(url)
    }
}
