package com.mossglen.reverie.tts

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Voice Model Manager - Handles remote config fetching, model downloading, and installation.
 *
 * Flow:
 * 1. fetchConfig() - GET JSON from remote URL, parse to get download_url
 * 2. downloadModel() - Download .tar.bz2 file from extracted URL
 * 3. installModel() - Extract .tar.bz2 to context.filesDir/kokoro
 *
 * Usage:
 * ```kotlin
 * val manager = VoiceModelManager(context)
 * manager.startVoiceSetup(configUrl)
 * ```
 */
class VoiceModelManager(private val context: Context) {

    companion object {
        private const val TAG = "VoiceModelManager"

        // Hardcoded remote configuration URL
        const val CONFIG_URL = "https://gist.githubusercontent.com/Akalias01/d4bbc29888d7b40747462537308a3f6a/raw/dbffe96e2d9db14e812abc541194d1fbf9c90359/voice_config.json"

        // Fallback direct download URL (in case config URL is stale)
        private const val FALLBACK_MODEL_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2"

        // Kokoro model directory
        private const val KOKORO_DIR = "kokoro"

        // Required model files
        private const val MODEL_FILE = "model.onnx"
        private const val TOKENS_FILE = "tokens.txt"
        private const val ESPEAK_DATA_DIR = "espeak-ng-data"

        // Timeout settings
        private const val CONNECT_TIMEOUT_SECONDS = 30L
        private const val READ_TIMEOUT_SECONDS = 120L
    }

    /**
     * Get the Kokoro model directory.
     * Uses external app storage which survives app reinstalls.
     */
    private fun getKokoroDir(): File {
        val externalDir = context.getExternalFilesDir(null)
        return if (externalDir != null) {
            File(externalDir, KOKORO_DIR)
        } else {
            Log.w(TAG, "External storage not available, using internal")
            File(context.filesDir, KOKORO_DIR)
        }
    }

    // OkHttp client for network requests
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    // Gson for JSON parsing
    private val gson = Gson()

    // ===== State Management =====

    sealed class SetupState {
        object Idle : SetupState()
        object FetchingConfig : SetupState()
        data class Downloading(
            val progress: Float,
            val bytesDownloaded: Long,
            val totalBytes: Long
        ) : SetupState()
        data class Installing(val progress: Float) : SetupState()
        object Completed : SetupState()
        data class Error(val message: String) : SetupState()
        object Cancelled : SetupState()
    }

    private val _setupState = MutableStateFlow<SetupState>(SetupState.Idle)
    val setupState: StateFlow<SetupState> = _setupState.asStateFlow()

    private var isCancelled = false

    // ===== Public API =====

    /**
     * Start the complete voice setup process.
     * Chains: Fetch Config -> Download Model -> Install Model
     *
     * @param configUrl URL to the JSON configuration file
     * @param onComplete Callback when setup completes successfully
     * @param onError Callback when an error occurs
     */
    suspend fun startVoiceSetup(
        configUrl: String,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        isCancelled = false
        Log.d(TAG, "Starting voice setup from: $configUrl")

        try {
            // Step 1: Fetch remote configuration
            _setupState.value = SetupState.FetchingConfig

            // Try to get download URL from config, fallback if it fails
            val downloadUrl = try {
                val config = fetchConfig(configUrl)
                Log.d(TAG, "Config fetched - Version: ${config.version}, URL: ${config.downloadUrl}")
                config.downloadUrl
            } catch (e: Exception) {
                Log.w(TAG, "Config fetch failed, using fallback URL: ${e.message}")
                FALLBACK_MODEL_URL
            }

            if (isCancelled) {
                _setupState.value = SetupState.Cancelled
                return@withContext
            }

            // Step 2: Download the model (try primary URL, fallback if 404)
            val downloadFile = File(context.cacheDir, "kokoro-model.tar.bz2")
            try {
                downloadModel(downloadUrl, downloadFile)
            } catch (e: Exception) {
                if (e.message?.contains("404") == true && downloadUrl != FALLBACK_MODEL_URL) {
                    Log.w(TAG, "Primary URL returned 404, trying fallback URL")
                    downloadModel(FALLBACK_MODEL_URL, downloadFile)
                } else {
                    throw e
                }
            }

            if (isCancelled) {
                _setupState.value = SetupState.Cancelled
                downloadFile.delete()
                return@withContext
            }

            // Step 3: Install (extract) the model
            val kokoroDir = getKokoroDir()
            installModel(downloadFile, kokoroDir)

            // Cleanup downloaded archive
            downloadFile.delete()

            if (isCancelled) {
                _setupState.value = SetupState.Cancelled
                return@withContext
            }

            // Verify installation
            if (isModelInstalled()) {
                _setupState.value = SetupState.Completed
                Log.d(TAG, "Voice setup completed successfully!")
                withContext(Dispatchers.Main) { onComplete() }
            } else {
                throw Exception("Model installation verification failed")
            }

        } catch (e: Exception) {
            val errorMsg = "Voice setup failed: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _setupState.value = SetupState.Error(errorMsg)
            withContext(Dispatchers.Main) { onError(errorMsg) }
        }
    }

    /**
     * Cancel the ongoing setup process.
     */
    fun cancel() {
        Log.d(TAG, "Cancelling voice setup...")
        isCancelled = true
    }

    /**
     * Reset the setup state.
     */
    fun reset() {
        _setupState.value = SetupState.Idle
        isCancelled = false
    }

    /**
     * Check if model is already installed.
     */
    fun isModelInstalled(): Boolean {
        val kokoroDir = getKokoroDir()
        if (!kokoroDir.exists()) return false

        val modelFile = File(kokoroDir, MODEL_FILE)
        val tokensFile = File(kokoroDir, TOKENS_FILE)
        val espeakDir = File(kokoroDir, ESPEAK_DATA_DIR)

        return modelFile.exists() && tokensFile.exists() && espeakDir.exists()
    }

    /**
     * Get installed model size in bytes.
     */
    fun getInstalledModelSize(): Long {
        val kokoroDir = getKokoroDir()
        return if (kokoroDir.exists()) {
            kokoroDir.walkTopDown().sumOf { it.length() }
        } else 0L
    }

    /**
     * Delete installed model.
     */
    fun deleteInstalledModel(): Boolean {
        val kokoroDir = getKokoroDir()
        return if (kokoroDir.exists()) {
            kokoroDir.deleteRecursively()
        } else true
    }

    // ===== Step A: Fetch Configuration =====

    /**
     * Fetch the remote JSON configuration.
     *
     * @param configUrl URL to the JSON config file
     * @return Parsed VoiceConfig object
     * @throws Exception if fetch or parse fails
     */
    private suspend fun fetchConfig(configUrl: String): VoiceConfig = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching config from: $configUrl")

        val request = Request.Builder()
            .url(configUrl)
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Config fetch failed: HTTP ${response.code}")
        }

        val jsonBody = response.body?.string()
            ?: throw Exception("Empty config response")

        Log.d(TAG, "Config JSON: $jsonBody")

        val config = gson.fromJson(jsonBody, VoiceConfig::class.java)
            ?: throw Exception("Failed to parse config JSON")

        if (config.downloadUrl.isBlank()) {
            throw Exception("Config missing download_url")
        }

        config
    }

    // ===== Step B: Download Model =====

    /**
     * Download the model file from the given URL.
     *
     * @param url Download URL for the .tar.bz2 model archive
     * @param destination Local file to save the download
     */
    private suspend fun downloadModel(
        url: String,
        destination: File
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Downloading model from: $url")

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Download failed: HTTP ${response.code}")
        }

        val body = response.body ?: throw Exception("Empty download response")
        val totalBytes = body.contentLength().takeIf { it > 0 } ?: (80 * 1024 * 1024L)

        var downloadedBytes = 0L

        body.byteStream().use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (!isActive || isCancelled) {
                        Log.d(TAG, "Download cancelled")
                        return@withContext
                    }

                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val progress = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                    _setupState.value = SetupState.Downloading(progress, downloadedBytes, totalBytes)
                }
            }
        }

        Log.d(TAG, "Download complete: ${downloadedBytes / 1024 / 1024}MB")
    }

    // ===== Step C: Install (Extract) Model =====

    /**
     * Extract the .tar.bz2 model archive to the kokoro directory.
     *
     * @param archiveFile The downloaded .tar.bz2 file
     * @param destinationDir The directory to extract files to (filesDir/kokoro)
     */
    private suspend fun installModel(
        archiveFile: File,
        destinationDir: File
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Installing model to: ${destinationDir.absolutePath}")

        _setupState.value = SetupState.Installing(0f)

        // Ensure destination exists
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        val archiveSize = archiveFile.length()
        var extractedBytes = 0L

        // Open .tar.bz2 stream
        FileInputStream(archiveFile).use { fileIn ->
            BufferedInputStream(fileIn).use { bufferedIn ->
                BZip2CompressorInputStream(bufferedIn).use { bz2In ->
                    TarArchiveInputStream(bz2In).use { tarIn ->

                        var entry = tarIn.nextTarEntry
                        var rootDirStripped = false
                        var rootDirName = ""

                        while (entry != null) {
                            if (!isActive || isCancelled) {
                                Log.d(TAG, "Installation cancelled")
                                return@withContext
                            }

                            // Handle directory structure - archives have a root folder like "kokoro-multi-lang-v1_0/"
                            var entryName = entry.name
                            Log.d(TAG, "Processing tar entry: $entryName")

                            // Detect and strip root directory (first entry is usually the root dir)
                            if (!rootDirStripped && entry.isDirectory && !entryName.contains("/")) {
                                rootDirName = entryName.trimEnd('/')
                                rootDirStripped = true
                                Log.d(TAG, "Detected root directory: $rootDirName")
                                entry = tarIn.nextTarEntry
                                continue
                            }

                            // Strip the root directory prefix if present
                            if (rootDirName.isNotEmpty() && entryName.startsWith("$rootDirName/")) {
                                entryName = entryName.removePrefix("$rootDirName/")
                            } else if (entryName.contains("/")) {
                                // Fallback: strip first directory segment
                                val parts = entryName.split("/", limit = 2)
                                if (parts.size > 1) {
                                    entryName = parts[1]
                                }
                            }

                            // Skip empty names
                            if (entryName.isBlank()) {
                                entry = tarIn.nextTarEntry
                                continue
                            }

                            val destFile = File(destinationDir, entryName)
                            Log.d(TAG, "Extracting: $entryName -> ${destFile.absolutePath}")

                            // Security check: prevent path traversal
                            if (!destFile.canonicalPath.startsWith(destinationDir.canonicalPath)) {
                                Log.w(TAG, "Skipping suspicious entry: ${entry.name}")
                                entry = tarIn.nextTarEntry
                                continue
                            }

                            if (entry.isDirectory) {
                                destFile.mkdirs()
                            } else {
                                // Ensure parent directories exist
                                destFile.parentFile?.mkdirs()

                                FileOutputStream(destFile).use { output ->
                                    tarIn.copyTo(output)
                                }
                            }

                            extractedBytes += entry.size
                            val progress = (extractedBytes.toFloat() / archiveSize).coerceIn(0f, 1f)
                            _setupState.value = SetupState.Installing(progress)

                            entry = tarIn.nextTarEntry
                        }
                    }
                }
            }
        }

        Log.d(TAG, "Installation complete")

        // Log installed files for verification
        destinationDir.walkTopDown().forEach { file ->
            Log.d(TAG, "Installed: ${file.relativeTo(destinationDir)}")
        }
    }
}
