package com.rezon.app.service

import android.content.Context
import android.os.Environment
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
import org.libtorrent4j.AlertListener
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.alerts.AddTorrentAlert
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.alerts.TorrentFinishedAlert
import java.io.File
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
 * TorrentManager - Handles torrent downloads using libtorrent4j
 */
@Singleton
class TorrentManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var sessionManager: SessionManager? = null
    private val torrentHandles = mutableMapOf<String, TorrentHandle>()

    private val _downloads = MutableStateFlow<List<TorrentDownload>>(emptyList())
    val downloads: StateFlow<List<TorrentDownload>> = _downloads.asStateFlow()

    private val _isSessionRunning = MutableStateFlow(false)
    val isSessionRunning: StateFlow<Boolean> = _isSessionRunning.asStateFlow()

    private var updateJob: Job? = null

    // Download directory
    private val downloadDir: File by lazy {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "REZON/Audiobooks"
        ).apply { mkdirs() }
    }

    /**
     * Start the torrent session
     */
    fun startSession() {
        if (sessionManager != null) return

        scope.launch {
            try {
                sessionManager = SessionManager().apply {
                    start()
                    addListener(createAlertListener())
                }
                _isSessionRunning.value = true
                startProgressUpdates()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Stop the torrent session
     */
    fun stopSession() {
        updateJob?.cancel()
        sessionManager?.stop()
        sessionManager = null
        _isSessionRunning.value = false
        torrentHandles.clear()
    }

    /**
     * Add a magnet link
     */
    fun addMagnet(magnetUri: String) {
        val session = sessionManager ?: run {
            startSession()
            scope.launch {
                delay(1000) // Wait for session to start
                addMagnetInternal(magnetUri)
            }
            return
        }
        addMagnetInternal(magnetUri)
    }

    private fun addMagnetInternal(magnetUri: String) {
        scope.launch {
            try {
                val session = sessionManager ?: return@launch
                session.download(magnetUri, downloadDir)

                // Create pending download entry
                val download = TorrentDownload(
                    name = "Fetching metadata...",
                    magnetUri = magnetUri,
                    state = TorrentState.CHECKING
                )
                _downloads.update { it + download }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Add a .torrent file
     */
    fun addTorrentFile(filePath: String) {
        scope.launch {
            try {
                val session = sessionManager ?: run {
                    startSession()
                    delay(1000)
                    sessionManager
                } ?: return@launch

                val torrentInfo = TorrentInfo(File(filePath))
                session.download(torrentInfo, downloadDir)

                val download = TorrentDownload(
                    name = torrentInfo.name(),
                    torrentFile = filePath,
                    totalSize = torrentInfo.totalSize(),
                    state = TorrentState.QUEUED
                )
                _downloads.update { it + download }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Pause a download
     */
    fun pauseDownload(downloadId: String) {
        torrentHandles[downloadId]?.pause()
        updateDownloadState(downloadId, TorrentState.PAUSED)
    }

    /**
     * Resume a download
     */
    fun resumeDownload(downloadId: String) {
        torrentHandles[downloadId]?.resume()
        updateDownloadState(downloadId, TorrentState.DOWNLOADING)
    }

    /**
     * Remove a download
     */
    fun removeDownload(downloadId: String, deleteFiles: Boolean = false) {
        scope.launch {
            torrentHandles[downloadId]?.let { handle ->
                sessionManager?.remove(handle)
                if (deleteFiles) {
                    // Delete downloaded files
                    val savePath = handle.savePath()
                    File(savePath).deleteRecursively()
                }
            }
            torrentHandles.remove(downloadId)
            _downloads.update { downloads ->
                downloads.filter { it.id != downloadId }
            }
        }
    }

    private fun updateDownloadState(downloadId: String, state: TorrentState) {
        _downloads.update { downloads ->
            downloads.map {
                if (it.id == downloadId) it.copy(state = state) else it
            }
        }
    }

    private fun createAlertListener(): AlertListener {
        return object : AlertListener {
            override fun types(): IntArray = intArrayOf(
                AlertType.ADD_TORRENT.swig(),
                AlertType.TORRENT_FINISHED.swig(),
                AlertType.STATE_CHANGED.swig()
            )

            override fun alert(alert: Alert<*>) {
                when (alert) {
                    is AddTorrentAlert -> {
                        val handle = alert.handle()
                        val name = handle.name()
                        val id = handle.infoHash().toString()

                        torrentHandles[id] = handle

                        _downloads.update { downloads ->
                            val existingIndex = downloads.indexOfFirst {
                                it.name == "Fetching metadata..." || it.name == name
                            }
                            if (existingIndex >= 0) {
                                downloads.toMutableList().apply {
                                    this[existingIndex] = this[existingIndex].copy(
                                        id = id,
                                        name = name,
                                        totalSize = handle.torrentFile()?.totalSize() ?: 0L,
                                        state = TorrentState.DOWNLOADING
                                    )
                                }
                            } else {
                                downloads + TorrentDownload(
                                    id = id,
                                    name = name,
                                    totalSize = handle.torrentFile()?.totalSize() ?: 0L,
                                    state = TorrentState.DOWNLOADING
                                )
                            }
                        }
                    }

                    is TorrentFinishedAlert -> {
                        val id = alert.handle().infoHash().toString()
                        updateDownloadState(id, TorrentState.FINISHED)
                    }
                }
            }
        }
    }

    private fun startProgressUpdates() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                updateAllProgress()
                delay(1000) // Update every second
            }
        }
    }

    private fun updateAllProgress() {
        _downloads.update { downloads ->
            downloads.map { download ->
                val handle = torrentHandles[download.id]
                if (handle != null && download.state == TorrentState.DOWNLOADING) {
                    val status = handle.status()
                    download.copy(
                        progress = status.progress(),
                        downloadSpeed = status.downloadRate().toLong(),
                        uploadSpeed = status.uploadRate().toLong(),
                        downloadedSize = (status.progress() * download.totalSize).toLong(),
                        peers = status.numPeers(),
                        seeds = status.numSeeds()
                    )
                } else {
                    download
                }
            }
        }
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
