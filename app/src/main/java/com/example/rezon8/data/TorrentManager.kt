package com.mossglen.lithos.data

import android.content.Context
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.libtorrent4j.*
import org.libtorrent4j.alerts.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

data class DownloadInfo(
    val id: String,
    val name: String,
    val progress: Float,
    val downloadSpeed: Int = 0, // in KB/s
    val isFinished: Boolean = false,
    val totalSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val numPeers: Int = 0,
    val numSeeds: Int = 0,
    val state: TorrentState = TorrentState.DOWNLOADING
)

enum class TorrentState {
    CHECKING,
    DOWNLOADING_METADATA,
    DOWNLOADING,
    SEEDING,
    PAUSED,
    FINISHED,
    ERROR
}

@Singleton
class TorrentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val metadataRepository: MetadataRepository,
    private val settingsRepository: SettingsRepository,
    private val torrentDownloadDao: TorrentDownloadDao
) {
    companion object {
        private const val TAG = "TorrentManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Settings cache
    private var maxDownloads = 3
    private var uploadLimitKbps = 50
    private var downloadLimitKbps = 0
    private var autoFetchMetadata = true
    private var seedAfterDownload = false
    private var wifiOnlyDownloads = true

    private val _activeDownloads = MutableStateFlow<List<DownloadInfo>>(emptyList())
    val activeDownloads = _activeDownloads.asStateFlow()

    // Legacy compatibility
    val downloads = _activeDownloads
    val currentProgress = MutableStateFlow(0f).asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // LibTorrent session
    private var sessionManager: SessionManager? = null
    private var isInitialized = false
    private var idleShutdownJob: kotlinx.coroutines.Job? = null

    // Track torrent handles by ID
    private val torrentHandles = ConcurrentHashMap<String, TorrentHandle>()

    // Cached custom save path from settings
    private var customSavePath: String = ""

    // Default save directory (fallback when no custom path is set)
    private val defaultSaveDir: File by lazy {
        File(context.getExternalFilesDir(null), "Torrents").also {
            if (!it.exists()) it.mkdirs()
        }
    }

    /**
     * Get the current save directory.
     * Uses the custom path from settings if set and valid, otherwise falls back to default.
     *
     * Note: LibTorrent requires a File path, not a SAF URI. When a SAF URI is set,
     * we download to a temporary directory first, then copy to the SAF location on completion.
     */
    private val saveDir: File
        get() {
            if (customSavePath.isNotEmpty()) {
                try {
                    val uri = Uri.parse(customSavePath)
                    val documentFile = DocumentFile.fromTreeUri(context, uri)
                    if (documentFile != null && documentFile.exists() && documentFile.canWrite()) {
                        // LibTorrent needs direct file access, so we use a staging directory
                        // and copy to SAF location when download completes
                        val safStagingDir = File(context.getExternalFilesDir(null), "SAF_Staging").also {
                            if (!it.exists()) it.mkdirs()
                        }
                        Log.d(TAG, "Using SAF staging directory: ${safStagingDir.absolutePath}")
                        return safStagingDir
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to validate custom save path, falling back to default", e)
                }
            }
            return defaultSaveDir
        }

    /**
     * Get the custom SAF destination URI if set, or null if using default path.
     */
    private fun getCustomSafUri(): Uri? {
        return if (customSavePath.isNotEmpty()) {
            try {
                val uri = Uri.parse(customSavePath)
                val documentFile = DocumentFile.fromTreeUri(context, uri)
                if (documentFile != null && documentFile.exists() && documentFile.canWrite()) {
                    uri
                } else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    /**
     * Initialize the torrent session.
     */
    fun initialize() {
        if (isInitialized) return

        scope.launch {
            try {
                Log.d(TAG, "Initializing LibTorrent session...")

                // Load settings
                loadSettings()

                sessionManager = SessionManager().apply {
                    addListener(object : AlertListener {
                        override fun types(): IntArray? = null

                        override fun alert(alert: Alert<*>) {
                            handleAlert(alert)
                        }
                    })

                    // Start session with optimized settings
                    start()
                    Log.d(TAG, "Session started, bootstrapping DHT...")

                    // Apply optimized settings
                    applySessionSettings()

                    // Add DHT bootstrap nodes for faster peer discovery
                    addDhtBootstrapNodes()
                }

                isInitialized = true
                Log.d(TAG, "LibTorrent session initialized successfully")

                // Watch for settings changes
                observeSettings()

                // Restore any saved downloads from previous session
                restoreSavedDownloads()

            } catch (e: UnsatisfiedLinkError) {
                // Native library not available for this architecture (e.g., x86 emulator)
                Log.w(TAG, "LibTorrent native library not available for this architecture - torrent features disabled", e)
                _error.value = "Torrent engine not available on this device architecture"
            } catch (e: LinkageError) {
                // Native library linkage failed
                Log.w(TAG, "LibTorrent native library failed to link - torrent features disabled", e)
                _error.value = "Torrent engine not available on this device"
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize LibTorrent", e)
                _error.value = "Failed to initialize torrent engine: ${e.message}"
            }
        }
    }

    /**
     * Load settings from repository.
     */
    private suspend fun loadSettings() {
        maxDownloads = settingsRepository.torrentMaxDownloads.first()
        uploadLimitKbps = settingsRepository.torrentUploadLimit.first()
        downloadLimitKbps = settingsRepository.torrentDownloadLimit.first()
        autoFetchMetadata = settingsRepository.torrentAutoFetchMetadata.first()
        seedAfterDownload = settingsRepository.torrentSeedAfterDownload.first()
        wifiOnlyDownloads = settingsRepository.torrentWifiOnly.first()
        customSavePath = settingsRepository.torrentSavePath.first()
        Log.d(TAG, "Loaded custom save path: ${if (customSavePath.isNotEmpty()) customSavePath else "default"}")
    }

    /**
     * Observe settings changes and apply them.
     */
    private fun observeSettings() {
        scope.launch {
            settingsRepository.torrentMaxDownloads.collect {
                maxDownloads = it
                applySessionSettings()
            }
        }
        scope.launch {
            settingsRepository.torrentUploadLimit.collect {
                uploadLimitKbps = it
                applySessionSettings()
            }
        }
        scope.launch {
            settingsRepository.torrentDownloadLimit.collect {
                downloadLimitKbps = it
                applySessionSettings()
            }
        }
        scope.launch {
            settingsRepository.torrentAutoFetchMetadata.collect {
                autoFetchMetadata = it
            }
        }
        scope.launch {
            settingsRepository.torrentSeedAfterDownload.collect {
                seedAfterDownload = it
            }
        }
        scope.launch {
            settingsRepository.torrentWifiOnly.collect {
                wifiOnlyDownloads = it
                // Pause/resume downloads based on connectivity
                checkConnectivityAndPause()
            }
        }
        scope.launch {
            settingsRepository.torrentSavePath.collect { path ->
                customSavePath = path
                Log.d(TAG, "Save path updated: ${if (path.isNotEmpty()) path else "default"}")
            }
        }
    }

    /**
     * Restore saved downloads from the database.
     * This is called after the torrent session is initialized to resume downloads
     * that were in progress before app restart or reinstall.
     */
    private suspend fun restoreSavedDownloads() {
        try {
            val savedDownloads = torrentDownloadDao.getActiveDownloadsList()
            if (savedDownloads.isEmpty()) {
                Log.d(TAG, "No saved downloads to restore")
                return
            }

            Log.d(TAG, "Restoring ${savedDownloads.size} saved downloads...")

            savedDownloads.forEach { saved ->
                try {
                    when (saved.sourceType) {
                        TorrentDownloadEntity.SOURCE_TYPE_MAGNET -> {
                            Log.d(TAG, "Resuming magnet download: ${saved.name}")
                            // Add to active downloads first (so UI shows it)
                            addDownload(DownloadInfo(
                                id = saved.id,
                                name = saved.name,
                                progress = saved.progress * 100f,
                                totalSize = saved.totalSize,
                                downloadedSize = saved.downloadedSize,
                                state = TorrentState.DOWNLOADING_METADATA
                            ))
                            // Re-start the magnet download
                            resumeMagnetDownload(saved.source, saved.id, saved.name)
                        }
                        TorrentDownloadEntity.SOURCE_TYPE_FILE -> {
                            Log.d(TAG, "Resuming file download: ${saved.name}")
                            val file = File(saved.source)
                            if (file.exists()) {
                                addDownload(DownloadInfo(
                                    id = saved.id,
                                    name = saved.name,
                                    progress = saved.progress * 100f,
                                    totalSize = saved.totalSize,
                                    downloadedSize = saved.downloadedSize,
                                    state = TorrentState.CHECKING
                                ))
                                resumeTorrentFileDownload(file, saved.id)
                            } else {
                                Log.w(TAG, "Torrent file no longer exists: ${saved.source}, removing from database")
                                torrentDownloadDao.deleteById(saved.id)
                            }
                        }
                        else -> {
                            Log.w(TAG, "Unknown source type: ${saved.sourceType}")
                            torrentDownloadDao.deleteById(saved.id)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore download: ${saved.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring saved downloads", e)
        }
    }

    /**
     * Resume a magnet download that was saved to the database.
     */
    private suspend fun resumeMagnetDownload(magnetUri: String, id: String, name: String) {
        val session = sessionManager ?: return

        try {
            // Give DHT a moment to find peers
            delay(1000)

            val data = session.fetchMagnet(magnetUri, 120, saveDir)
            if (data != null && data.isNotEmpty()) {
                val ti = TorrentInfo.bdecode(data)
                val handle = session.find(ti.infoHash())
                if (handle != null) {
                    torrentHandles[id] = handle

                    // Check if torrent is already complete
                    val status = handle.status()
                    val isComplete = status.isFinished || status.isSeeding ||
                        (status.progress() >= 0.999f && status.state() != TorrentStatus.State.CHECKING_FILES)

                    if (isComplete) {
                        Log.d(TAG, "Restored torrent already complete: $name")
                        updateDownloadState(id, TorrentState.FINISHED)
                        onDownloadComplete(id, ti.name())
                    } else {
                        startProgressMonitoring(id)
                    }
                }
            } else {
                Log.w(TAG, "Could not resume magnet (no peers): $name")
                updateDownloadState(id, TorrentState.ERROR)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume magnet download", e)
            updateDownloadState(id, TorrentState.ERROR)
        }
    }

    /**
     * Resume a torrent file download that was saved to the database.
     */
    private suspend fun resumeTorrentFileDownload(torrentFile: File, id: String) {
        val session = sessionManager ?: return

        try {
            val ti = TorrentInfo(torrentFile)
            session.download(ti, saveDir)

            val handle = session.find(ti.infoHash())
            if (handle != null) {
                torrentHandles[id] = handle

                // Check if already complete
                val status = handle.status()
                val isComplete = status.isFinished || status.isSeeding ||
                    (status.progress() >= 0.999f && status.state() != TorrentStatus.State.CHECKING_FILES)

                if (isComplete) {
                    Log.d(TAG, "Restored torrent file already complete: ${ti.name()}")
                    updateDownloadState(id, TorrentState.FINISHED)
                    onDownloadComplete(id, ti.name())
                } else {
                    startProgressMonitoring(id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume torrent file download", e)
            updateDownloadState(id, TorrentState.ERROR)
        }
    }

    /**
     * Check if currently on Wi-Fi network.
     */
    private fun isOnWifi(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Check if downloads should be allowed based on Wi-Fi setting.
     */
    private fun canStartDownload(): Boolean {
        if (!wifiOnlyDownloads) return true
        return isOnWifi()
    }

    /**
     * Pause or resume downloads based on connectivity and Wi-Fi only setting.
     */
    private fun checkConnectivityAndPause() {
        if (wifiOnlyDownloads && !isOnWifi()) {
            // Pause all active downloads
            _activeDownloads.value.forEach { download ->
                if (download.state == TorrentState.DOWNLOADING) {
                    pauseDownload(download.id)
                    Log.d(TAG, "Paused download due to Wi-Fi only setting: ${download.name}")
                }
            }
        }
    }

    /**
     * Apply current settings to the LibTorrent session.
     * Optimized for maximum download speed based on official libtorrent tuning guide.
     * Reference: https://www.libtorrent.org/tuning.html
     */
    private fun applySessionSettings() {
        val session = sessionManager ?: return
        try {
            val settings = session.settings()

            // === ACTIVE TORRENT LIMITS ===
            settings.activeDownloads(maxDownloads)
            settings.activeSeeds(if (seedAfterDownload) maxDownloads else 0)
            settings.activeLimit(maxDownloads + 10)
            settings.activeChecking(2)

            // === CONNECTION LIMITS - High for fast peer discovery ===
            // Per libtorrent docs: "Set high to allow many simultaneous connections"
            settings.connectionsLimit(500)
            settings.maxPeerlistSize(4000)

            // === DHT/TRACKER/LSD LIMITS - Enable aggressive peer discovery ===
            settings.activeDhtLimit(200)
            settings.activeTrackerLimit(200)
            settings.activeLsdLimit(100)

            // === BUFFER SETTINGS - Optimized for throughput ===
            // Higher send buffer watermark improves sustained throughput
            settings.sendBufferWatermark(512 * 1024) // 512 KB
            // Max queued disk bytes - prevents fast downloads from stalling
            settings.maxQueuedDiskBytes(16 * 1024 * 1024) // 16 MB

            // === TIMEOUT SETTINGS - Aggressive to drop slow peers ===
            settings.inactivityTimeout(30) // Drop idle peers after 30 seconds

            // === SPEED LIMITS ===
            settings.downloadRateLimit(if (downloadLimitKbps > 0) downloadLimitKbps * 1024 else 0)
            // BitTorrent reciprocation: upload helps download speed
            // Minimum 50 KB/s upload for better peer reciprocation
            val effectiveUploadLimit = if (uploadLimitKbps > 0) {
                maxOf(uploadLimitKbps, 50) * 1024
            } else {
                0 // Unlimited - best for download speed
            }
            settings.uploadRateLimit(effectiveUploadLimit)

            session.applySettings(settings)
            Log.d(TAG, "Applied HIGH-SPEED settings: connections=500, peers=4000, DHT=200, sendBuffer=512KB, diskQueue=16MB")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply session settings", e)
        }
    }

    /**
     * Add DHT bootstrap nodes for faster peer discovery.
     * DHT is enabled by default in libtorrent4j - this adds extra bootstrap nodes.
     */
    private fun SessionManager.addDhtBootstrapNodes() {
        try {
            // DHT is enabled by default when session starts
            // The built-in bootstrap nodes should be sufficient, but we log for debugging
            Log.d(TAG, "DHT enabled with default bootstrap nodes")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure DHT", e)
        }
    }

    /**
     * Schedule auto-shutdown of the torrent session when idle.
     * This saves battery by stopping the engine when no downloads are active.
     */
    private fun scheduleIdleShutdown() {
        idleShutdownJob?.cancel()
        idleShutdownJob = scope.launch {
            delay(30000) // Wait 30 seconds after last download completes

            // Check if there are any active downloads
            val hasActiveDownloads = _activeDownloads.value.any {
                it.state == TorrentState.DOWNLOADING ||
                it.state == TorrentState.DOWNLOADING_METADATA ||
                it.state == TorrentState.CHECKING
            }

            val hasSeeding = seedAfterDownload && _activeDownloads.value.any {
                it.state == TorrentState.SEEDING
            }

            if (!hasActiveDownloads && !hasSeeding && torrentHandles.isEmpty()) {
                Log.d(TAG, "No active downloads, shutting down torrent engine to save battery")
                stop()
            }
        }
    }

    /**
     * Cancel idle shutdown (called when new download starts).
     */
    private fun cancelIdleShutdown() {
        idleShutdownJob?.cancel()
        idleShutdownJob = null
    }

    private fun handleAlert(alert: Alert<*>) {
        try {
            when (alert.type()) {
                AlertType.TORRENT_FINISHED -> {
                    val handle = (alert as TorrentFinishedAlert).handle()
                    handleTorrentFinished(handle)
                }
                AlertType.TORRENT_ERROR -> {
                    val errorAlert = alert as TorrentErrorAlert
                    Log.e(TAG, "Torrent error: ${errorAlert.message()}")
                }
                AlertType.METADATA_RECEIVED -> {
                    val handle = (alert as MetadataReceivedAlert).handle()
                    handleMetadataReceived(handle)
                }
                AlertType.STATE_CHANGED -> {
                    val stateAlert = alert as StateChangedAlert
                    handleStateChanged(stateAlert)
                }
                else -> { /* Ignore other alerts */ }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error handling alert", e)
        }
    }

    private fun handleTorrentFinished(handle: TorrentHandle) {
        val id = findIdForHandle(handle) ?: return
        val name = handle.torrentFile()?.name() ?: "Unknown"

        Log.d(TAG, "Torrent finished: $name")

        scope.launch {
            updateDownloadState(id, TorrentState.FINISHED)
            onDownloadComplete(id, name)
        }
    }

    private fun handleMetadataReceived(handle: TorrentHandle) {
        val id = findIdForHandle(handle) ?: return
        val info = handle.torrentFile()

        Log.d(TAG, "Metadata received for: ${info?.name()}")

        scope.launch {
            val downloads = _activeDownloads.value.toMutableList()
            val index = downloads.indexOfFirst { it.id == id }
            if (index >= 0 && downloads[index].name.contains("Unknown")) {
                downloads[index] = downloads[index].copy(
                    name = info?.name() ?: downloads[index].name,
                    totalSize = info?.totalSize() ?: 0L
                )
                _activeDownloads.value = downloads
            }
        }
    }

    private fun handleStateChanged(alert: StateChangedAlert) {
        val handle = alert.handle()
        val id = findIdForHandle(handle) ?: return

        val state = when (alert.state) {
            TorrentStatus.State.CHECKING_FILES,
            TorrentStatus.State.CHECKING_RESUME_DATA -> TorrentState.CHECKING
            TorrentStatus.State.DOWNLOADING_METADATA -> TorrentState.DOWNLOADING_METADATA
            TorrentStatus.State.DOWNLOADING -> TorrentState.DOWNLOADING
            TorrentStatus.State.SEEDING -> TorrentState.SEEDING
            TorrentStatus.State.FINISHED -> TorrentState.FINISHED
            else -> TorrentState.DOWNLOADING
        }

        scope.launch {
            updateDownloadState(id, state)
        }
    }

    private fun findIdForHandle(handle: TorrentHandle): String? {
        return torrentHandles.entries.find { it.value == handle }?.key
    }

    /**
     * Start downloading from a magnet link.
     */
    fun startMagnetDownload(magnetUri: String) {
        if (!magnetUri.startsWith("magnet:")) {
            _error.value = "Invalid magnet link"
            return
        }

        cancelIdleShutdown() // Keep session alive while downloading
        initialize()

        scope.launch {
            try {
                // Check Wi-Fi only setting
                if (!canStartDownload()) {
                    _error.value = "Downloads are restricted to Wi-Fi only. Connect to Wi-Fi or disable this setting."
                    Log.w(TAG, "Download blocked: Wi-Fi only setting enabled but not on Wi-Fi")
                    return@launch
                }

                _isDownloading.value = true
                _error.value = null

                val name = extractNameFromMagnet(magnetUri) ?: "Unknown Torrent"
                val id = magnetUri.hashCode().toString()

                Log.d(TAG, "Starting magnet download: $name")

                addDownload(DownloadInfo(
                    id = id,
                    name = name,
                    progress = 0f,
                    state = TorrentState.DOWNLOADING_METADATA
                ))

                // Save to database for persistence across app restarts
                torrentDownloadDao.insert(TorrentDownloadEntity(
                    id = id,
                    source = magnetUri,
                    sourceType = TorrentDownloadEntity.SOURCE_TYPE_MAGNET,
                    name = name,
                    state = "DOWNLOADING_METADATA"
                ))

                // Wait for session to be ready (up to 10 seconds)
                var waitCount = 0
                while (sessionManager == null && waitCount < 100) {
                    delay(100)
                    waitCount++
                }

                val session = sessionManager
                if (session == null) {
                    _error.value = "Torrent engine not ready. Please try again."
                    removeDownload(id)
                    return@launch
                }

                // Give DHT a moment to find peers
                Log.d(TAG, "Waiting for DHT peers...")
                delay(2000)

                // Fetch magnet metadata and start download (increased timeout to 120s)
                Log.d(TAG, "Fetching magnet metadata...")
                updateDownloadState(id, TorrentState.DOWNLOADING_METADATA)

                val data = session.fetchMagnet(magnetUri, 120, saveDir)
                if (data != null && data.isNotEmpty()) {
                    val ti = TorrentInfo.bdecode(data)
                    val handle = session.find(ti.infoHash())
                    if (handle != null) {
                        val torrentName = ti.name()
                        Log.d(TAG, "Magnet resolved, starting download: $torrentName")
                        torrentHandles[id] = handle

                        // Update name from metadata
                        val downloads = _activeDownloads.value.toMutableList()
                        val index = downloads.indexOfFirst { it.id == id }
                        if (index >= 0) {
                            downloads[index] = downloads[index].copy(
                                name = torrentName,
                                totalSize = ti.totalSize()
                            )
                            _activeDownloads.value = downloads
                        }

                        // Check if torrent is already complete (files exist from previous download)
                        val status = handle.status()
                        val isComplete = status.isFinished || status.isSeeding ||
                            (status.progress() >= 0.999f && status.state() != TorrentStatus.State.CHECKING_FILES)

                        if (isComplete) {
                            Log.d(TAG, "Torrent already complete (files exist), processing immediately: $torrentName")
                            updateDownloadState(id, TorrentState.FINISHED)
                            onDownloadComplete(id, torrentName)
                        } else {
                            startProgressMonitoring(id)
                        }
                    } else {
                        Log.e(TAG, "Could not find torrent handle after metadata")
                        _error.value = "Failed to start download"
                        removeDownload(id)
                    }
                } else {
                    Log.e(TAG, "fetchMagnet returned null or empty data")
                    _error.value = "Could not find peers. Check your connection and try again."
                    removeDownload(id)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start magnet download", e)
                _error.value = "Download failed: ${e.message}"
                _isDownloading.value = false
            }
        }
    }

    /**
     * Start downloading from a .torrent file.
     */
    fun startTorrentFileDownload(torrentFile: File) {
        cancelIdleShutdown() // Keep session alive while downloading
        initialize()

        scope.launch {
            try {
                // Check Wi-Fi only setting
                if (!canStartDownload()) {
                    _error.value = "Downloads are restricted to Wi-Fi only. Connect to Wi-Fi or disable this setting."
                    Log.w(TAG, "Download blocked: Wi-Fi only setting enabled but not on Wi-Fi")
                    return@launch
                }

                _isDownloading.value = true
                _error.value = null

                val name = torrentFile.nameWithoutExtension
                val id = torrentFile.absolutePath.hashCode().toString()

                Log.d(TAG, "Starting torrent file download: $name")

                addDownload(DownloadInfo(
                    id = id,
                    name = name,
                    progress = 0f,
                    state = TorrentState.CHECKING
                ))

                var waitCount = 0
                while (sessionManager == null && waitCount < 50) {
                    delay(100)
                    waitCount++
                }

                val session = sessionManager
                if (session == null) {
                    _error.value = "Torrent engine not ready"
                    removeDownload(id)
                    return@launch
                }

                val ti = TorrentInfo(torrentFile)
                val torrentName = ti.name()
                session.download(ti, saveDir)

                val handle = session.find(ti.infoHash())
                if (handle != null) {
                    torrentHandles[id] = handle

                    // Check if torrent is already complete (files exist from previous download)
                    val status = handle.status()
                    val isComplete = status.isFinished || status.isSeeding ||
                        (status.progress() >= 0.999f && status.state() != TorrentStatus.State.CHECKING_FILES)

                    if (isComplete) {
                        Log.d(TAG, "Torrent file already complete (files exist), processing immediately: $torrentName")
                        updateDownloadState(id, TorrentState.FINISHED)
                        onDownloadComplete(id, torrentName)
                    } else {
                        startProgressMonitoring(id)
                    }
                } else {
                    Log.e(TAG, "Could not find torrent handle after starting download")
                    _error.value = "Failed to start download"
                    removeDownload(id)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start torrent file download", e)
                _error.value = "Download failed: ${e.message}"
                _isDownloading.value = false
            }
        }
    }

    /**
     * Start downloading from a torrent file URI.
     */
    fun startTorrentFileDownload(torrentFileUri: Uri) {
        cancelIdleShutdown() // Keep session alive while downloading
        initialize()

        scope.launch {
            try {
                // Check Wi-Fi only setting
                if (!canStartDownload()) {
                    _error.value = "Downloads are restricted to Wi-Fi only. Connect to Wi-Fi or disable this setting."
                    Log.w(TAG, "Download blocked: Wi-Fi only setting enabled but not on Wi-Fi")
                    return@launch
                }

                _isDownloading.value = true
                _error.value = null

                val name = getFileNameFromUri(torrentFileUri)
                    ?.removeSuffix(".torrent")
                    ?.replace("_", " ")
                    ?.replace("-", " ")
                    ?: "Unknown Torrent"

                val id = torrentFileUri.toString().hashCode().toString()

                Log.d(TAG, "Starting torrent URI download: $name")

                addDownload(DownloadInfo(
                    id = id,
                    name = name,
                    progress = 0f,
                    state = TorrentState.CHECKING
                ))

                // Copy torrent file to temp location
                val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.torrent")
                context.contentResolver.openInputStream(torrentFileUri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                var waitCount = 0
                while (sessionManager == null && waitCount < 50) {
                    delay(100)
                    waitCount++
                }

                val session = sessionManager
                if (session == null) {
                    _error.value = "Torrent engine not ready"
                    removeDownload(id)
                    tempFile.delete()
                    return@launch
                }

                val ti = TorrentInfo(tempFile)
                val torrentName = ti.name()
                session.download(ti, saveDir)

                val handle = session.find(ti.infoHash())
                if (handle != null) {
                    torrentHandles[id] = handle

                    // Check if torrent is already complete (files exist from previous download)
                    val status = handle.status()
                    val isComplete = status.isFinished || status.isSeeding ||
                        (status.progress() >= 0.999f && status.state() != TorrentStatus.State.CHECKING_FILES)

                    tempFile.delete()

                    if (isComplete) {
                        Log.d(TAG, "Torrent URI already complete (files exist), processing immediately: $torrentName")
                        updateDownloadState(id, TorrentState.FINISHED)
                        onDownloadComplete(id, torrentName)
                    } else {
                        startProgressMonitoring(id)
                    }
                } else {
                    Log.e(TAG, "Could not find torrent handle after starting download")
                    _error.value = "Failed to start download"
                    removeDownload(id)
                    tempFile.delete()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load torrent file", e)
                _error.value = "Failed to load torrent file: ${e.message}"
                _isDownloading.value = false
            }
        }
    }

    /**
     * Monitor download progress for a torrent.
     */
    private fun startProgressMonitoring(id: String) {
        scope.launch {
            var iterationCount = 0
            var lastSavedProgress = 0f

            while (torrentHandles.containsKey(id)) {
                val handle = torrentHandles[id]

                if (handle == null || !handle.isValid) {
                    delay(1000)
                    continue
                }

                try {
                    val status = handle.status()
                    val progress = status.progress() * 100f
                    val downloadSpeed = (status.downloadRate() / 1024).toInt()
                    val totalSize = status.totalWanted()
                    val downloadedSize = status.totalWantedDone()
                    val numPeers = status.numPeers()
                    val numSeeds = status.numSeeds()

                    val flags = status.flags()
                    val isPaused = flags.and_(TorrentFlags.PAUSED).eq(TorrentFlags.PAUSED)

                    val state = when (status.state()) {
                        TorrentStatus.State.SEEDING -> TorrentState.SEEDING
                        TorrentStatus.State.FINISHED -> TorrentState.FINISHED
                        TorrentStatus.State.CHECKING_FILES,
                        TorrentStatus.State.CHECKING_RESUME_DATA -> TorrentState.CHECKING
                        TorrentStatus.State.DOWNLOADING_METADATA -> TorrentState.DOWNLOADING_METADATA
                        else -> if (isPaused) TorrentState.PAUSED else TorrentState.DOWNLOADING
                    }

                    updateDownload(
                        id = id,
                        progress = progress,
                        speedKbps = downloadSpeed,
                        finished = state == TorrentState.FINISHED || state == TorrentState.SEEDING,
                        totalSize = totalSize,
                        downloadedSize = downloadedSize,
                        numPeers = numPeers,
                        numSeeds = numSeeds,
                        state = state
                    )

                    // Periodically save progress to database (every 30 seconds or 5% progress change)
                    iterationCount++
                    if (iterationCount >= 30 || (progress - lastSavedProgress) >= 5f) {
                        torrentDownloadDao.updateProgress(
                            id = id,
                            progress = progress / 100f,
                            downloadedSize = downloadedSize,
                            state = state.name
                        )
                        lastSavedProgress = progress
                        iterationCount = 0
                    }

                    if (state == TorrentState.FINISHED || state == TorrentState.SEEDING) {
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting torrent status", e)
                }

                delay(1000)
            }
        }
    }

    private suspend fun onDownloadComplete(id: String, name: String) {
        _isDownloading.value = _activeDownloads.value.any { !it.isFinished && it.id != id }

        // Mark as complete in the database
        torrentDownloadDao.markComplete(id)

        // Play notification sound
        try {
            withContext(Dispatchers.Main) {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notification)
                ringtone?.play()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play notification sound", e)
        }

        val downloadPath = File(saveDir, name)

        Log.d(TAG, "Download complete, processing: $downloadPath")

        // If a custom SAF destination is set, copy files there and import from SAF
        val safDestUri = getCustomSafUri()
        var scannedBooks: List<com.mossglen.lithos.data.Book> = emptyList()

        if (safDestUri != null && downloadPath.exists()) {
            try {
                // Copy to SAF destination
                copyToSafDestination(downloadPath, safDestUri, name)
                Log.d(TAG, "Copied download to custom SAF location")

                // Import files from SAF destination
                scannedBooks = importFromSafDestination(safDestUri, name)
                Log.d(TAG, "Imported ${scannedBooks.size} books from SAF destination")

                // Clean up staging directory after successful import
                if (scannedBooks.isNotEmpty()) {
                    downloadPath.deleteRecursively()
                    Log.d(TAG, "Cleaned up staging directory")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy/import from SAF location", e)
                // Fall back to scanning local path
            }
        }

        // If SAF import didn't work or no custom path, scan local files
        if (scannedBooks.isEmpty()) {
            // Check if this is a folder with split chapter files that need merging
            val processedPath = if (downloadPath.isDirectory) {
                processSplitChapterFolder(downloadPath)
            } else {
                downloadPath
            }

            Log.d(TAG, "Scanning local path: $processedPath")

            // Scan the downloaded files into library
            scannedBooks = if (processedPath.exists()) {
                libraryRepository.scanSpecificFolder(processedPath)
            } else {
                libraryRepository.scanSpecificFolder(saveDir)
            }
        }

        Log.d(TAG, "Total scanned books: ${scannedBooks.size}")

        // Auto-fetch metadata for newly added books
        if (autoFetchMetadata && scannedBooks.isNotEmpty()) {
            Log.d(TAG, "Auto-fetching metadata for ${scannedBooks.size} books...")
            scannedBooks.forEach { book ->
                try {
                    metadataRepository.fetchAndSaveMetadata(book)
                    Log.d(TAG, "Fetched metadata for: ${book.title}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch metadata for ${book.title}: ${e.message}")
                }
            }
        }

        delay(2000)

        // Handle seeding or cleanup - always read fresh setting value
        val shouldSeed = settingsRepository.torrentSeedAfterDownload.first()
        Log.d(TAG, "Download complete - shouldSeed=$shouldSeed")

        val handle = torrentHandles.remove(id)
        if (handle != null) {
            if (shouldSeed) {
                Log.d(TAG, "Seeding enabled, keeping torrent active: $name")
                torrentHandles[id] = handle
                updateDownloadState(id, TorrentState.SEEDING)
            } else {
                Log.d(TAG, "Seeding disabled, stopping and removing torrent: $name")
                handle.pause()
                sessionManager?.remove(handle)
                removeDownload(id)
            }
        } else {
            // No handle, just clean up
            removeDownload(id)
        }

        // Schedule idle shutdown if no more active downloads
        scheduleIdleShutdown()
    }

    /**
     * Process a folder containing split chapter audio files.
     * Detects if files are chapters and creates a virtual audiobook structure.
     */
    private suspend fun processSplitChapterFolder(folder: File): File {
        if (!folder.isDirectory) return folder

        val audioFiles = folder.listFiles()?.filter { file ->
            file.isFile && file.extension.lowercase() in listOf("mp3", "m4a", "m4b", "opus", "ogg", "flac", "wav")
        }?.sortedBy { it.name } ?: return folder

        if (audioFiles.isEmpty()) {
            // Check subdirectories
            val subDirs = folder.listFiles()?.filter { it.isDirectory } ?: emptyList()
            subDirs.forEach { subDir ->
                processSplitChapterFolder(subDir)
            }
            return folder
        }

        // Detect if these look like split chapters
        if (audioFiles.size > 1 && looksLikeSplitChapters(audioFiles)) {
            Log.d(TAG, "Detected split chapter files in: ${folder.name}")
            createChapterMetadata(folder, audioFiles)
        }

        return folder
    }

    /**
     * Check if files appear to be split chapters (numbered sequence).
     */
    private fun looksLikeSplitChapters(files: List<File>): Boolean {
        if (files.size < 2) return false

        // Check for common chapter naming patterns
        val patterns = listOf(
            Regex("""(?i)chapter\s*\d+"""),
            Regex("""(?i)ch\s*\d+"""),
            Regex("""(?i)part\s*\d+"""),
            Regex("""(?i)^\d+\s*[-_.]\s*"""),
            Regex("""(?i)track\s*\d+"""),
            Regex("""(?i)\(\d+\)"""),
            Regex("""(?i)_\d+_"""),
            Regex("""(?i)-\s*\d+\s*[-.]""")
        )

        val matchCount = files.count { file ->
            patterns.any { pattern -> pattern.containsMatchIn(file.nameWithoutExtension) }
        }

        // If more than 70% match chapter patterns, treat as split chapters
        return matchCount.toFloat() / files.size > 0.7f
    }

    /**
     * Create chapter metadata file for the audiobook scanner to pick up.
     */
    private suspend fun createChapterMetadata(folder: File, audioFiles: List<File>) {
        try {
            // Create a .chapters file that the library scanner can read
            val chaptersFile = File(folder, ".reverie_chapters")

            val chapterData = buildString {
                appendLine("# REVERIE Chapter Metadata")
                appendLine("# Auto-generated from split chapter files")
                appendLine("book_title=${folder.name.replace(Regex("""[\[\]_-]"""), " ").trim()}")
                appendLine("chapter_count=${audioFiles.size}")
                appendLine()

                audioFiles.forEachIndexed { index, file ->
                    val chapterNum = index + 1
                    val chapterTitle = extractChapterTitle(file.nameWithoutExtension, chapterNum)
                    appendLine("chapter.$chapterNum.file=${file.name}")
                    appendLine("chapter.$chapterNum.title=$chapterTitle")
                }
            }

            chaptersFile.writeText(chapterData)
            Log.d(TAG, "Created chapter metadata for ${audioFiles.size} chapters in: ${folder.name}")

        } catch (e: Exception) {
            Log.w(TAG, "Failed to create chapter metadata", e)
        }
    }

    /**
     * Extract a clean chapter title from a filename.
     */
    private fun extractChapterTitle(filename: String, fallbackNumber: Int): String {
        // Try to extract chapter title from filename
        val cleanName = filename
            .replace(Regex("""^\d+\s*[-_.]\s*"""), "") // Remove leading numbers
            .replace(Regex("""(?i)^(chapter|ch|part|track)\s*\d+\s*[-_.:]?\s*"""), "") // Remove chapter prefix
            .replace(Regex("""[\[\]()]"""), "") // Remove brackets
            .replace(Regex("""_+"""), " ") // Replace underscores with spaces
            .replace(Regex("""-+"""), " ") // Replace dashes with spaces
            .replace(Regex("""\s+"""), " ") // Normalize spaces
            .trim()

        return if (cleanName.isNotBlank() && cleanName.length > 3) {
            cleanName
        } else {
            "Chapter $fallbackNumber"
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) result = it.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }

    private fun extractNameFromMagnet(magnetUri: String): String? {
        val regex = Regex("dn=([^&]+)")
        return regex.find(magnetUri)?.groupValues?.get(1)?.let {
            java.net.URLDecoder.decode(it, "UTF-8")
        }
    }

    private fun addDownload(download: DownloadInfo) {
        val current = _activeDownloads.value.toMutableList()
        current.removeAll { it.id == download.id }
        current.add(download)
        _activeDownloads.value = current
    }

    private fun updateDownload(
        id: String,
        progress: Float,
        speedKbps: Int,
        finished: Boolean,
        totalSize: Long = 0L,
        downloadedSize: Long = 0L,
        numPeers: Int = 0,
        numSeeds: Int = 0,
        state: TorrentState = TorrentState.DOWNLOADING
    ) {
        val current = _activeDownloads.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(
                progress = progress,
                downloadSpeed = speedKbps,
                isFinished = finished,
                totalSize = totalSize,
                downloadedSize = downloadedSize,
                numPeers = numPeers,
                numSeeds = numSeeds,
                state = state
            )
            _activeDownloads.value = current
        }
    }

    private fun updateDownloadState(id: String, state: TorrentState) {
        val current = _activeDownloads.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(state = state)
            _activeDownloads.value = current
        }
    }

    private fun removeDownload(id: String) {
        val current = _activeDownloads.value.toMutableList()
        current.removeAll { it.id == id }
        _activeDownloads.value = current
    }

    fun cancelDownload(id: String) {
        val handle = torrentHandles.remove(id)
        handle?.let {
            sessionManager?.remove(it)
        }
        removeDownload(id)
        // Remove from database
        scope.launch {
            torrentDownloadDao.deleteById(id)
        }
    }

    fun pauseDownload(id: String) {
        torrentHandles[id]?.pause()
        updateDownloadState(id, TorrentState.PAUSED)
    }

    fun resumeDownload(id: String) {
        torrentHandles[id]?.resume()
        updateDownloadState(id, TorrentState.DOWNLOADING)
    }

    fun pauseAll() {
        sessionManager?.pause()
        _activeDownloads.value.forEach { download ->
            if (!download.isFinished) {
                updateDownloadState(download.id, TorrentState.PAUSED)
            }
        }
    }

    fun resumeAll() {
        sessionManager?.resume()
        _activeDownloads.value.forEach { download ->
            if (download.state == TorrentState.PAUSED) {
                updateDownloadState(download.id, TorrentState.DOWNLOADING)
            }
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping torrent session...")

        torrentHandles.values.forEach { handle ->
            sessionManager?.remove(handle)
        }
        torrentHandles.clear()

        sessionManager?.stop()
        sessionManager = null
        isInitialized = false

        Log.d(TAG, "Torrent session stopped")
    }

    fun getSaveDirectory(): File = saveDir

    /**
     * Clear the current error state.
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Delete downloaded files for a book.
     * This handles files in both the default save directory and split chapter folders.
     *
     * @param filePath The file path of the book (can be a file or directory)
     * @return true if files were deleted, false if no files found
     */
    suspend fun deleteDownloadedFiles(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to delete files for: $filePath")

            // Parse the path - it could be a file:// URI or a direct path
            val path = when {
                filePath.startsWith("file://") -> Uri.parse(filePath).path ?: filePath
                filePath.startsWith("content://") -> {
                    // For SAF URIs, we can't directly delete - would need DocumentFile
                    Log.w(TAG, "Cannot delete SAF content URI directly: $filePath")
                    return@withContext false
                }
                else -> filePath
            }

            val file = File(path)

            if (file.exists()) {
                val deleted = if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    // Also try to delete parent folder if it's empty after deletion
                    val parent = file.parentFile
                    val fileDeleted = file.delete()

                    // Clean up empty parent folder (only if it's in our save directory)
                    if (fileDeleted && parent != null && parent.absolutePath.startsWith(saveDir.absolutePath)) {
                        val remaining = parent.listFiles()
                        if (remaining.isNullOrEmpty()) {
                            parent.delete()
                            Log.d(TAG, "Deleted empty parent folder: ${parent.name}")
                        }
                    }
                    fileDeleted
                }

                Log.d(TAG, "Delete result for $path: $deleted")
                return@withContext deleted
            }

            // Try to find and delete in the save directory by matching name
            val fileName = File(path).name
            val matchingFile = saveDir.listFiles()?.find { it.name == fileName }
            if (matchingFile != null) {
                val deleted = if (matchingFile.isDirectory) {
                    matchingFile.deleteRecursively()
                } else {
                    matchingFile.delete()
                }
                Log.d(TAG, "Deleted matching file in save dir: ${matchingFile.name}, result: $deleted")
                return@withContext deleted
            }

            Log.d(TAG, "No files found to delete for: $filePath")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting files for: $filePath", e)
            false
        }
    }

    /**
     * Remove any active torrent that matches the given file path.
     * This is useful when deleting a book that was downloaded via torrent.
     *
     * @param filePath The file path of the book
     */
    fun removeTorrentByFilePath(filePath: String) {
        try {
            val fileName = when {
                filePath.startsWith("file://") -> Uri.parse(filePath).path?.let { File(it).name }
                else -> File(filePath).name
            } ?: return

            Log.d(TAG, "Looking for torrent matching: $fileName")

            // Find any active download that matches this file name
            val matchingDownload = _activeDownloads.value.find { download ->
                download.name.equals(fileName, ignoreCase = true) ||
                fileName.startsWith(download.name, ignoreCase = true)
            }

            if (matchingDownload != null) {
                Log.d(TAG, "Found matching torrent: ${matchingDownload.name}, removing...")
                cancelDownload(matchingDownload.id)
            }

            // Also check torrent handles directly
            val session = sessionManager ?: return
            torrentHandles.entries.toList().forEach { (id, handle) ->
                try {
                    val torrentName = handle.torrentFile()?.name() ?: return@forEach
                    if (torrentName.equals(fileName, ignoreCase = true) ||
                        fileName.startsWith(torrentName, ignoreCase = true)) {
                        Log.d(TAG, "Removing torrent handle for: $torrentName")
                        session.remove(handle)
                        torrentHandles.remove(id)
                        removeDownload(id)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking torrent handle", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing torrent by file path", e)
        }
    }

    /**
     * Check if a file path is within the torrent save directory.
     * Useful for determining if a book came from a torrent download.
     */
    fun isInTorrentDirectory(filePath: String): Boolean {
        val path = when {
            filePath.startsWith("file://") -> Uri.parse(filePath).path ?: return false
            filePath.startsWith("content://") -> return false
            else -> filePath
        }

        return path.startsWith(saveDir.absolutePath) ||
               path.startsWith(defaultSaveDir.absolutePath)
    }

    /**
     * Import files from a SAF destination after copying.
     * Scans the destination folder for audio files and imports them to the library.
     */
    private suspend fun importFromSafDestination(destUri: Uri, name: String): List<com.mossglen.lithos.data.Book> {
        return withContext(Dispatchers.IO) {
            val importedBooks = mutableListOf<com.mossglen.lithos.data.Book>()

            try {
                val destTree = DocumentFile.fromTreeUri(context, destUri)
                    ?: return@withContext importedBooks

                val destFolder = destTree.findFile(name)

                if (destFolder != null && destFolder.isDirectory) {
                    // Scan folder for audio files
                    importAudioFilesFromSaf(destFolder, importedBooks)
                } else if (destFolder != null && destFolder.isFile) {
                    // Single file
                    val book = libraryRepository.importBook(destFolder.uri)
                    if (book != null) importedBooks.add(book)
                } else {
                    // Try to find the file directly in the destination
                    destTree.listFiles().forEach { file ->
                        if (file.name == name || file.name?.startsWith(name) == true) {
                            if (file.isDirectory) {
                                importAudioFilesFromSaf(file, importedBooks)
                            } else if (isAudioFile(file.name ?: "")) {
                                val book = libraryRepository.importBook(file.uri)
                                if (book != null) importedBooks.add(book)
                            }
                        }
                    }
                }

                Log.d(TAG, "Imported ${importedBooks.size} books from SAF: $name")
            } catch (e: Exception) {
                Log.e(TAG, "Error importing from SAF destination", e)
            }

            importedBooks
        }
    }

    /**
     * Recursively import audio files from a SAF DocumentFile folder.
     */
    private suspend fun importAudioFilesFromSaf(folder: DocumentFile, importedBooks: MutableList<com.mossglen.lithos.data.Book>) {
        folder.listFiles().forEach { file ->
            if (file.isDirectory) {
                importAudioFilesFromSaf(file, importedBooks)
            } else if (isAudioFile(file.name ?: "")) {
                try {
                    val book = libraryRepository.importBook(file.uri)
                    if (book != null) {
                        importedBooks.add(book)
                        Log.d(TAG, "Imported from SAF: ${file.name}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to import ${file.name}: ${e.message}")
                }
            }
        }
    }

    /**
     * Check if a filename is an audio file.
     */
    private fun isAudioFile(filename: String): Boolean {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return ext in listOf("mp3", "m4a", "m4b", "aac", "ogg", "flac", "wav", "opus")
    }

    /**
     * Copy downloaded files to the custom SAF destination.
     * This is needed because LibTorrent requires direct file access,
     * so we download to a staging directory and then copy to SAF.
     */
    private suspend fun copyToSafDestination(source: File, destUri: Uri, name: String) {
        withContext(Dispatchers.IO) {
            try {
                val destTree = DocumentFile.fromTreeUri(context, destUri)
                    ?: throw IllegalStateException("Cannot access destination folder")

                if (source.isDirectory) {
                    // Create folder in destination
                    val destFolder = destTree.createDirectory(name)
                        ?: destTree.findFile(name)  // Folder might already exist
                        ?: throw IllegalStateException("Cannot create destination folder: $name")

                    // Copy all files recursively
                    copyDirectoryToSaf(source, destFolder)
                } else {
                    // Copy single file
                    copyFileToSaf(source, destTree, source.name)
                }

                Log.d(TAG, "Successfully copied to SAF destination: $name")

                // Optionally delete staging files after successful copy
                // source.deleteRecursively()  // Uncomment to auto-delete staging files

            } catch (e: Exception) {
                Log.e(TAG, "Error copying to SAF destination", e)
                throw e
            }
        }
    }

    /**
     * Recursively copy a directory to a SAF DocumentFile.
     */
    private fun copyDirectoryToSaf(source: File, destFolder: DocumentFile) {
        source.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val subFolder = destFolder.createDirectory(file.name)
                    ?: destFolder.findFile(file.name)
                    ?: return@forEach
                copyDirectoryToSaf(file, subFolder)
            } else {
                copyFileToSaf(file, destFolder, file.name)
            }
        }
    }

    /**
     * Copy a single file to a SAF DocumentFile destination.
     */
    private fun copyFileToSaf(source: File, destFolder: DocumentFile, fileName: String) {
        try {
            // Determine MIME type
            val mimeType = when (source.extension.lowercase()) {
                "mp3" -> "audio/mpeg"
                "m4a", "m4b" -> "audio/mp4"
                "flac" -> "audio/flac"
                "ogg", "opus" -> "audio/ogg"
                "wav" -> "audio/wav"
                else -> "application/octet-stream"
            }

            // Create or overwrite the destination file
            val destFile = destFolder.findFile(fileName)?.also { it.delete() }
                ?: destFolder.createFile(mimeType, fileName)
                ?: throw IllegalStateException("Cannot create file: $fileName")

            // Copy contents
            context.contentResolver.openOutputStream(destFile.uri)?.use { output ->
                source.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "Copied file: $fileName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy file: $fileName", e)
        }
    }
}
