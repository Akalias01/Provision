package com.rezon.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rezon.app.service.TorrentDownload
import com.rezon.app.service.TorrentManager
import com.rezon.app.service.TorrentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Torrent Screen
 */
data class TorrentUiState(
    val downloads: List<TorrentDownload> = emptyList(),
    val isSessionRunning: Boolean = false,
    val showAddDialog: Boolean = false,
    val magnetInput: String = "",
    val error: String? = null
)

/**
 * TorrentViewModel - Manages torrent download UI state
 */
@HiltViewModel
class TorrentViewModel @Inject constructor(
    private val torrentManager: TorrentManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TorrentUiState())
    val uiState: StateFlow<TorrentUiState> = _uiState.asStateFlow()

    init {
        // Start session and observe downloads
        torrentManager.startSession()

        viewModelScope.launch {
            torrentManager.downloads.collect { downloads ->
                _uiState.update { it.copy(downloads = downloads) }
            }
        }

        viewModelScope.launch {
            torrentManager.isSessionRunning.collect { isRunning ->
                _uiState.update { it.copy(isSessionRunning = isRunning) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, magnetInput = "") }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false, magnetInput = "") }
    }

    fun updateMagnetInput(input: String) {
        _uiState.update { it.copy(magnetInput = input) }
    }

    fun addMagnet() {
        val magnet = _uiState.value.magnetInput.trim()
        if (magnet.isNotEmpty()) {
            torrentManager.addMagnet(magnet)
            hideAddDialog()
        }
    }

    fun addTorrentFile(filePath: String) {
        torrentManager.addTorrentFile(filePath)
    }

    fun pauseDownload(downloadId: String) {
        torrentManager.pauseDownload(downloadId)
    }

    fun resumeDownload(downloadId: String) {
        torrentManager.resumeDownload(downloadId)
    }

    fun removeDownload(downloadId: String, deleteFiles: Boolean = false) {
        torrentManager.removeDownload(downloadId, deleteFiles)
    }

    fun formatBytes(bytes: Long): String = torrentManager.formatBytes(bytes)

    fun formatSpeed(bytesPerSecond: Long): String = torrentManager.formatSpeed(bytesPerSecond)

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        // Don't stop session here - let it run in background
    }
}
