package com.mossglen.lithos.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mossglen.lithos.data.TorrentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TorrentViewModel @Inject constructor(
    private val torrentManager: TorrentManager
) : ViewModel() {

    // Expose active downloads from TorrentManager
    val activeDownloads = torrentManager.activeDownloads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Legacy compatibility
    val downloads = activeDownloads

    // Expose download progress from TorrentManager
    val downloadProgress = torrentManager.currentProgress
        .stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    // Expose downloading state
    val isDownloading = torrentManager.isDownloading
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // Expose errors
    val error = torrentManager.error
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Magnet link input state
    var magnetLinkInput = mutableStateOf("")
        private set

    init {
        // Initialize torrent engine
        torrentManager.initialize()
    }

    fun onMagnetLinkChanged(newLink: String) {
        magnetLinkInput.value = newLink
    }

    fun startDownload() {
        val link = magnetLinkInput.value
        if (link.startsWith("magnet:")) {
            torrentManager.startMagnetDownload(link)
            magnetLinkInput.value = "" // Clear input after starting
        }
    }

    // Alias for clearer API naming
    fun startMagnetDownload() = startDownload()

    fun startDownloadWithLink(magnetUri: String) {
        if (magnetUri.startsWith("magnet:")) {
            torrentManager.startMagnetDownload(magnetUri)
        }
    }

    fun startFileDownload(uri: Uri) {
        torrentManager.startTorrentFileDownload(uri)
    }

    fun cancelDownload(id: String) {
        torrentManager.cancelDownload(id)
    }

    fun pauseDownload(id: String) {
        torrentManager.pauseDownload(id)
    }

    fun resumeDownload(id: String) {
        torrentManager.resumeDownload(id)
    }

    fun pauseAll() {
        torrentManager.pauseAll()
    }

    fun resumeAll() {
        torrentManager.resumeAll()
    }

    /**
     * Clear the current error state.
     */
    fun clearError() {
        torrentManager.clearError()
    }
}
