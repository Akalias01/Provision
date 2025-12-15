package com.rezon.app.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * State for a single torrent download
 */
data class TorrentDownload(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val magnetUri: String? = null,
    val torrentFile: String? = null,
    val progress: Float = 0f,
    val downloadSpeed: Long = 0L,
    val uploadSpeed: Long = 0L,
    val totalSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val state: TorrentState = TorrentState.QUEUED,
    val peers: Int = 0,
    val seeds: Int = 0
)

/**
 * Torrent download state
 */
enum class TorrentState {
    QUEUED,
    CHECKING,
    DOWNLOADING,
    PAUSED,
    SEEDING,
    FINISHED,
    ERROR
}

/**
 * TorrentManager - Handles torrent downloads
 * Currently uses mock implementation for UI testing
 */
@Singleton
class TorrentManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloads = MutableStateFlow<List<TorrentDownload>>(emptyList())
    val downloads: StateFlow<List<TorrentDownload>> = _downloads.asStateFlow()

    private val _isSessionRunning = MutableStateFlow(false)
    val isSessionRunning: StateFlow<Boolean> = _isSessionRunning.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()

    /**
     * Start the torrent session
     */
    fun startSession() {
        _isSessionRunning.value = true
    }

    /**
     * Stop the torrent session
     */
    fun stopSession() {
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        _isSessionRunning.value = false
    }

    /**
     * Add a magnet link
     */
    fun addMagnet(magnetUri: String) {
        if (!_isSessionRunning.value) {
            startSession()
        }

        val download = TorrentDownload(
            name = extractNameFromMagnet(magnetUri),
            magnetUri = magnetUri,
            totalSize = (100_000_000L..500_000_000L).random(), // Mock size 100-500MB
            state = TorrentState.DOWNLOADING
        )
        _downloads.update { it + download }

        // Start mock download progress
        startMockDownload(download.id)
    }

    /**
     * Add a .torrent file
     */
    fun addTorrentFile(filePath: String) {
        if (!_isSessionRunning.value) {
            startSession()
        }

        val fileName = filePath.substringAfterLast("/").removeSuffix(".torrent")
        val download = TorrentDownload(
            name = fileName,
            torrentFile = filePath,
            totalSize = (100_000_000L..500_000_000L).random(),
            state = TorrentState.DOWNLOADING
        )
        _downloads.update { it + download }

        startMockDownload(download.id)
    }

    /**
     * Pause a download
     */
    fun pauseDownload(downloadId: String) {
        downloadJobs[downloadId]?.cancel()
        downloadJobs.remove(downloadId)
        updateDownloadState(downloadId, TorrentState.PAUSED)
    }

    /**
     * Resume a download
     */
    fun resumeDownload(downloadId: String) {
        updateDownloadState(downloadId, TorrentState.DOWNLOADING)
        startMockDownload(downloadId)
    }

    /**
     * Remove a download
     */
    fun removeDownload(downloadId: String, deleteFiles: Boolean = false) {
        downloadJobs[downloadId]?.cancel()
        downloadJobs.remove(downloadId)
        _downloads.update { downloads ->
            downloads.filter { it.id != downloadId }
        }
    }

    private fun updateDownloadState(downloadId: String, state: TorrentState) {
        _downloads.update { downloads ->
            downloads.map {
                if (it.id == downloadId) it.copy(state = state) else it
            }
        }
    }

    private fun extractNameFromMagnet(magnetUri: String): String {
        // Try to extract display name from magnet link
        val dnParam = magnetUri.substringAfter("dn=", "")
            .substringBefore("&")
            .replace("+", " ")
            .replace("%20", " ")

        return if (dnParam.isNotEmpty()) {
            java.net.URLDecoder.decode(dnParam, "UTF-8")
        } else {
            "Unknown Torrent"
        }
    }

    private fun startMockDownload(downloadId: String) {
        val job = scope.launch {
            while (isActive) {
                val currentDownload = _downloads.value.find { it.id == downloadId } ?: break

                if (currentDownload.state != TorrentState.DOWNLOADING) break
                if (currentDownload.progress >= 1f) {
                    updateDownloadState(downloadId, TorrentState.FINISHED)
                    break
                }

                // Simulate download progress
                val newProgress = (currentDownload.progress + 0.01f).coerceAtMost(1f)
                val speed = (500_000L..2_000_000L).random() // 500KB - 2MB/s

                _downloads.update { downloads ->
                    downloads.map {
                        if (it.id == downloadId) {
                            it.copy(
                                progress = newProgress,
                                downloadSpeed = speed,
                                uploadSpeed = speed / 10,
                                downloadedSize = (newProgress * it.totalSize).toLong(),
                                peers = (5..20).random(),
                                seeds = (2..10).random()
                            )
                        } else it
                    }
                }

                delay(500) // Update every 500ms
            }
        }
        downloadJobs[downloadId] = job
    }

    /**
     * Format bytes to human readable string
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> "%.2f MB".format(bytes / 1_048_576.0)
            bytes >= 1024 -> "%.2f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    /**
     * Format speed to human readable string
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatBytes(bytesPerSecond)}/s"
    }
}
