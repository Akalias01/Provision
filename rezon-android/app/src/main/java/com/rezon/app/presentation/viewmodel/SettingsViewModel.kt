package com.rezon.app.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings UI State
 */
data class SettingsUiState(
    // Downloads
    val downloadOverWifiOnly: Boolean = true,

    // Library
    val scanFoldersOnStartup: Boolean = true,
    val deleteIfMissing: Boolean = false,

    // Player
    val skipBackwardSeconds: Int = 10,
    val skipForwardSeconds: Int = 30,
    val skipAfterPauseSeconds: Int = 5,
    val keepPlaybackServiceActive: Boolean = false,

    // Audio
    val voiceBoostEnabled: Boolean = false,
    val silenceSkippingEnabled: Boolean = false,
    val monoAudioEnabled: Boolean = false,

    // Debug
    val fileLoggingEnabled: Boolean = false
)

/**
 * REZON Settings ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    // TODO: Inject SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        // TODO: Load from DataStore
    }

    // Downloads
    fun setDownloadOverWifiOnly(enabled: Boolean) {
        _uiState.update { it.copy(downloadOverWifiOnly = enabled) }
        saveSettings()
    }

    // Library
    fun setScanFoldersOnStartup(enabled: Boolean) {
        _uiState.update { it.copy(scanFoldersOnStartup = enabled) }
        saveSettings()
    }

    fun setDeleteIfMissing(enabled: Boolean) {
        _uiState.update { it.copy(deleteIfMissing = enabled) }
        saveSettings()
    }

    // Player
    fun setSkipBackward(seconds: Int) {
        _uiState.update { it.copy(skipBackwardSeconds = seconds) }
        saveSettings()
    }

    fun setSkipForward(seconds: Int) {
        _uiState.update { it.copy(skipForwardSeconds = seconds) }
        saveSettings()
    }

    fun setSkipAfterPause(seconds: Int) {
        _uiState.update { it.copy(skipAfterPauseSeconds = seconds) }
        saveSettings()
    }

    fun setKeepPlaybackServiceActive(enabled: Boolean) {
        _uiState.update { it.copy(keepPlaybackServiceActive = enabled) }
        saveSettings()
    }

    // Audio
    fun setVoiceBoost(enabled: Boolean) {
        _uiState.update { it.copy(voiceBoostEnabled = enabled) }
        saveSettings()
    }

    fun setSilenceSkipping(enabled: Boolean) {
        _uiState.update { it.copy(silenceSkippingEnabled = enabled) }
        saveSettings()
    }

    fun setMonoAudio(enabled: Boolean) {
        _uiState.update { it.copy(monoAudioEnabled = enabled) }
        saveSettings()
    }

    // Debug
    fun setFileLogging(enabled: Boolean) {
        _uiState.update { it.copy(fileLoggingEnabled = enabled) }
        saveSettings()
    }

    /**
     * Add books from device via file picker
     */
    fun addBooksFromDevice(uris: List<Uri>) {
        viewModelScope.launch {
            // TODO: Process the selected URIs and add them to the library
            // This would typically:
            // 1. Copy files to app storage or take persistent permissions
            // 2. Extract metadata (title, author, cover)
            // 3. Add entries to the library database
            uris.forEach { uri ->
                // For now, just log the URI
                // In a real implementation, use a BookRepository to add the book
                android.util.Log.d("SettingsViewModel", "Adding book from: $uri")
            }
        }
    }

    private fun saveSettings() {
        viewModelScope.launch {
            // TODO: Save to DataStore
        }
    }
}
