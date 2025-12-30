package com.mossglen.reverie.tts

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * @deprecated Use VoiceModelManager instead.
 *
 * This was a stub implementation that simulated downloads. The real implementation
 * is in VoiceModelManager which:
 * - Fetches remote config from URL
 * - Downloads .tar.bz2 model file with OkHttp
 * - Extracts with Apache Commons Compress
 *
 * VoiceModelManager is now used by TtsRepository and TtsViewModel.
 */
@Deprecated("Use VoiceModelManager instead", ReplaceWith("VoiceModelManager"))
object ModelDownloader {

    private const val TAG = "ModelDownloader"

    // Kokoro directory name (matches SherpaTtsEngine)
    private const val KOKORO_DIR = "kokoro"

    // Approximate model size for progress calculation
    const val ESTIMATED_MODEL_SIZE_BYTES = 80 * 1024 * 1024L // ~80MB

    /**
     * Download state for UI observation.
     */
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(
            val progress: Float,        // 0.0 - 1.0
            val bytesDownloaded: Long,
            val totalBytes: Long
        ) : DownloadState()
        data class Extracting(val progress: Float) : DownloadState()
        object Completed : DownloadState()
        data class Error(val message: String) : DownloadState()
        object Cancelled : DownloadState()
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var isCancelled = false

    /**
     * Download and extract the Kokoro model.
     *
     * @param context Application context
     * @param url URL to the Kokoro model ZIP file
     * @param onComplete Callback when download and extraction complete
     * @param onError Callback when an error occurs
     */
    suspend fun downloadKokoroModel(
        context: Context,
        url: String,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        isCancelled = false
        _downloadState.value = DownloadState.Downloading(0f, 0, ESTIMATED_MODEL_SIZE_BYTES)

        try {
            Log.d(TAG, "Starting Kokoro model download from: $url")

            // Create kokoro directory (external storage survives reinstall)
            val externalDir = context.getExternalFilesDir(null)
            val kokoroDir = if (externalDir != null) {
                File(externalDir, KOKORO_DIR)
            } else {
                File(context.filesDir, KOKORO_DIR)
            }
            if (!kokoroDir.exists()) {
                kokoroDir.mkdirs()
            }

            // Download to temp file
            val tempZipFile = File(context.cacheDir, "kokoro-model.zip")

            // ===== STUB: Replace with actual download implementation =====
            // Example implementation:
            /*
            downloadFile(url, tempZipFile) { progress, downloaded, total ->
                if (isCancelled) throw CancellationException("Download cancelled")
                _downloadState.value = DownloadState.Downloading(progress, downloaded, total)
            }
            */

            // For now, just simulate (remove in production)
            Log.w(TAG, "Download stub - implement actual HTTP download")
            simulateDownload()

            if (isCancelled) {
                _downloadState.value = DownloadState.Cancelled
                tempZipFile.delete()
                return@withContext
            }

            // ===== STUB: Replace with actual extraction implementation =====
            // Example implementation:
            /*
            _downloadState.value = DownloadState.Extracting(0f)
            extractZipToKokoroDir(tempZipFile, kokoroDir) { progress ->
                if (isCancelled) throw CancellationException("Extraction cancelled")
                _downloadState.value = DownloadState.Extracting(progress)
            }

            // Cleanup
            tempZipFile.delete()
            */

            Log.w(TAG, "Extraction stub - implement actual ZIP extraction")
            simulateExtraction()

            if (isCancelled) {
                _downloadState.value = DownloadState.Cancelled
                return@withContext
            }

            _downloadState.value = DownloadState.Completed
            Log.d(TAG, "Kokoro model download and extraction complete")
            withContext(Dispatchers.Main) { onComplete() }

        } catch (e: Exception) {
            val errorMsg = "Download failed: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _downloadState.value = DownloadState.Error(errorMsg)
            withContext(Dispatchers.Main) { onError(errorMsg) }
        }
    }

    /**
     * Download a file from URL with progress reporting.
     */
    private suspend fun downloadFile(
        urlString: String,
        destination: File,
        onProgress: (Float, Long, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        connection.connectTimeout = 30000
        connection.readTimeout = 60000
        connection.requestMethod = "GET"

        try {
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error: ${connection.responseCode} ${connection.responseMessage}")
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: ESTIMATED_MODEL_SIZE_BYTES
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (!isActive || isCancelled) break

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                        onProgress(progress, downloadedBytes, totalBytes)
                    }
                }
            }

            Log.d(TAG, "Download complete: ${downloadedBytes / 1024 / 1024}MB")

        } finally {
            connection.disconnect()
        }
    }

    /**
     * Extract ZIP file to kokoro directory, preserving folder structure.
     */
    private suspend fun extractZipToKokoroDir(
        zipFile: File,
        kokoroDir: File,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Extracting to: ${kokoroDir.absolutePath}")

        val totalSize = zipFile.length()
        var extractedSize = 0L

        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry

            while (entry != null) {
                if (!isActive || isCancelled) break

                // Determine destination path
                val destFile = File(kokoroDir, entry.name)

                // Security check: prevent zip slip vulnerability
                if (!destFile.canonicalPath.startsWith(kokoroDir.canonicalPath)) {
                    Log.w(TAG, "Skipping suspicious entry: ${entry.name}")
                    zis.closeEntry()
                    entry = zis.nextEntry
                    continue
                }

                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    // Ensure parent directories exist
                    destFile.parentFile?.mkdirs()

                    FileOutputStream(destFile).use { output ->
                        zis.copyTo(output)
                    }
                }

                extractedSize += entry.compressedSize
                val progress = (extractedSize.toFloat() / totalSize).coerceIn(0f, 1f)
                onProgress(progress)

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        Log.d(TAG, "Extraction complete")
    }

    /**
     * Simulate download for testing UI (remove in production).
     */
    private suspend fun simulateDownload() = withContext(Dispatchers.IO) {
        repeat(100) { i ->
            if (isCancelled) return@withContext
            kotlinx.coroutines.delay(30)
            val progress = (i + 1) / 100f
            val downloaded = (ESTIMATED_MODEL_SIZE_BYTES * progress).toLong()
            _downloadState.value = DownloadState.Downloading(progress, downloaded, ESTIMATED_MODEL_SIZE_BYTES)
        }
    }

    /**
     * Simulate extraction for testing UI (remove in production).
     */
    private suspend fun simulateExtraction() = withContext(Dispatchers.IO) {
        repeat(50) { i ->
            if (isCancelled) return@withContext
            kotlinx.coroutines.delay(20)
            _downloadState.value = DownloadState.Extracting((i + 1) / 50f)
        }
    }

    /**
     * Cancel an ongoing download.
     */
    fun cancelDownload() {
        Log.d(TAG, "Cancelling download...")
        isCancelled = true
        _downloadState.value = DownloadState.Cancelled
    }

    /**
     * Reset download state.
     */
    fun reset() {
        _downloadState.value = DownloadState.Idle
        isCancelled = false
    }

    /**
     * Check if model is already downloaded.
     */
    fun isModelDownloaded(context: Context): Boolean {
        return SherpaTtsEngine.isModelReady(context)
    }

    /**
     * Get downloaded model size in bytes.
     */
    fun getModelSize(context: Context): Long {
        return SherpaTtsEngine.getModelSize(context)
    }

    /**
     * Delete downloaded model to free up space.
     */
    fun deleteModel(context: Context): Boolean {
        Log.d(TAG, "Deleting Kokoro model...")
        return SherpaTtsEngine.deleteModelFiles(context)
    }
}
