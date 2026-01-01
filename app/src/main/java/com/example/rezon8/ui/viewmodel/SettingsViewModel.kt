package com.mossglen.lithos.ui.viewmodel

import android.app.LocaleManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mossglen.lithos.data.AudioEffectManager
import com.mossglen.lithos.data.CoverArtRepository
import com.mossglen.lithos.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    val audioEffectManager: AudioEffectManager,
    private val libraryRepository: com.mossglen.lithos.data.LibraryRepository,
    private val coverArtRepository: CoverArtRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Download Settings
    val wifiOnly = repository.wifiOnly
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch { repository.setWifiOnly(enabled) }
    }

    // Library Settings
    val scanOnStartup = repository.scanOnStartup
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun setScanOnStartup(enabled: Boolean) {
        viewModelScope.launch { repository.setScanOnStartup(enabled) }
    }

    val deleteIfMissing = repository.deleteIfMissing
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setDeleteIfMissing(enabled: Boolean) {
        viewModelScope.launch { repository.setDeleteIfMissing(enabled) }
    }

    // Audio Settings
    val audioCodec = repository.audioCodec
        .stateIn(viewModelScope, SharingStarted.Lazily, "Android")

    fun setAudioCodec(codec: String) {
        viewModelScope.launch { repository.setAudioCodec(codec) }
    }

    val audioOutput = repository.audioOutput
        .stateIn(viewModelScope, SharingStarted.Lazily, "OpenSL ES")

    fun setAudioOutput(output: String) {
        viewModelScope.launch { repository.setAudioOutput(output) }
    }

    // Player Settings
    val skipBackward = repository.skipBackward
        .stateIn(viewModelScope, SharingStarted.Lazily, 10)

    fun setSkipBackward(seconds: Int) {
        viewModelScope.launch { repository.setSkipBackward(seconds) }
    }

    val skipForward = repository.skipForward
        .stateIn(viewModelScope, SharingStarted.Lazily, 30)

    fun setSkipForward(seconds: Int) {
        viewModelScope.launch { repository.setSkipForward(seconds) }
    }

    val keepServiceActive = repository.keepServiceActive
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setKeepServiceActive(enabled: Boolean) {
        viewModelScope.launch { repository.setKeepServiceActive(enabled) }
    }

    val showLockScreenCover = repository.showLockScreenCover
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun setShowLockScreenCover(enabled: Boolean) {
        viewModelScope.launch { repository.setShowLockScreenCover(enabled) }
    }

    val sleepTimerMinutes = repository.sleepTimerMinutes
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun setSleepTimerMinutes(minutes: Int) {
        viewModelScope.launch { repository.setSleepTimerMinutes(minutes) }
    }

    // Smart Auto-Rewind (rewinds based on pause duration)
    val smartAutoRewindEnabled = repository.smartAutoRewindEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun setSmartAutoRewindEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSmartAutoRewindEnabled(enabled) }
    }

    // Debug Settings
    val fileLogging = repository.fileLogging
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setFileLogging(enabled: Boolean) {
        viewModelScope.launch { repository.setFileLogging(enabled) }
    }

    // Appearance Settings
    val dynamicPlayerColors = repository.dynamicPlayerColors
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun setDynamicPlayerColors(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicPlayerColors(enabled) }
    }

    // ========================================================================
    // Torrent Settings
    // ========================================================================

    val torrentEnabled = repository.torrentEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun setTorrentEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setTorrentEnabled(enabled) }
    }

    val torrentWifiOnly = repository.torrentWifiOnly
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun setTorrentWifiOnly(enabled: Boolean) {
        viewModelScope.launch { repository.setTorrentWifiOnly(enabled) }
    }

    val torrentMaxDownloads = repository.torrentMaxDownloads
        .stateIn(viewModelScope, SharingStarted.Lazily, 3)

    fun setTorrentMaxDownloads(max: Int) {
        viewModelScope.launch { repository.setTorrentMaxDownloads(max) }
    }

    val torrentUploadLimit = repository.torrentUploadLimit
        .stateIn(viewModelScope, SharingStarted.Lazily, 50)

    fun setTorrentUploadLimit(kbps: Int) {
        viewModelScope.launch { repository.setTorrentUploadLimit(kbps) }
    }

    val torrentDownloadLimit = repository.torrentDownloadLimit
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun setTorrentDownloadLimit(kbps: Int) {
        viewModelScope.launch { repository.setTorrentDownloadLimit(kbps) }
    }

    val torrentAutoFetchMetadata = repository.torrentAutoFetchMetadata
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun setTorrentAutoFetchMetadata(enabled: Boolean) {
        viewModelScope.launch { repository.setTorrentAutoFetchMetadata(enabled) }
    }

    val torrentSeedAfterDownload = repository.torrentSeedAfterDownload
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setTorrentSeedAfterDownload(enabled: Boolean) {
        viewModelScope.launch { repository.setTorrentSeedAfterDownload(enabled) }
    }

    val torrentSavePath = repository.torrentSavePath
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun setTorrentSavePath(path: String) {
        viewModelScope.launch { repository.setTorrentSavePath(path) }
    }

    // ========================================================================
    // Kids Mode Settings
    // ========================================================================

    val kidsModeEnabled = repository.kidsModeEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setKidsModeEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setKidsModeEnabled(enabled) }
    }

    val kidsModePin = repository.kidsModePin
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun setKidsModePin(pin: String) {
        viewModelScope.launch { repository.setKidsModePin(pin) }
    }

    /**
     * Verify Kids Mode PIN
     */
    suspend fun verifyKidsModePin(enteredPin: String): Boolean {
        return repository.verifyKidsModePin(enteredPin)
    }

    // ========================================================================
    // Library Folders
    // ========================================================================

    val libraryFolders = repository.libraryFolders
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addLibraryFolder(folderUri: String) {
        viewModelScope.launch { repository.addLibraryFolder(folderUri) }
    }

    fun removeLibraryFolder(folderUri: String) {
        viewModelScope.launch { repository.removeLibraryFolder(folderUri) }
    }

    companion object {
        private const val TAG = "SettingsViewModel"

        // Supported audio file extensions
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "m4b", "m4a", "flac", "ogg", "wav", "opus", "aac"
        )

        // Supported document extensions
        private val DOCUMENT_EXTENSIONS = setOf(
            "epub", "pdf"
        )

        // All supported extensions combined
        private val ALL_SUPPORTED_EXTENSIONS = AUDIO_EXTENSIONS + DOCUMENT_EXTENSIONS
    }

    /**
     * Scan all library folders for audiobooks and documents.
     * Uses DocumentFile to recursively scan SAF URIs from OpenDocumentTree.
     *
     * @return The number of books successfully imported
     */
    suspend fun scanLibraryFolders(): Int = withContext(Dispatchers.IO) {
        val folders = repository.getLibraryFolders()
        Log.d(TAG, "=== LIBRARY SCAN START ===")
        Log.d(TAG, "Number of library folders configured: ${folders.size}")

        if (folders.isEmpty()) {
            Log.w(TAG, "No library folders configured - nothing to scan")
            return@withContext 0
        }

        // Log all persisted URI permissions for debugging
        val persistedUris = context.contentResolver.persistedUriPermissions
        Log.d(TAG, "Persisted URI permissions count: ${persistedUris.size}")
        persistedUris.forEachIndexed { index, permission ->
            Log.d(TAG, "  Persisted[$index]: uri=${permission.uri}, read=${permission.isReadPermission}, write=${permission.isWritePermission}")
        }

        var totalImported = 0

        for (folderUriString in folders) {
            Log.d(TAG, "--- Processing folder: $folderUriString ---")
            try {
                val folderUri = Uri.parse(folderUriString)
                Log.d(TAG, "Parsed URI: $folderUri")
                Log.d(TAG, "URI scheme: ${folderUri.scheme}, authority: ${folderUri.authority}")

                // Verify we have permission to access this folder
                val hasPermission = hasUriPermission(folderUri)
                Log.d(TAG, "hasUriPermission result: $hasPermission")

                if (!hasPermission) {
                    Log.w(TAG, "NO PERMISSION for folder: $folderUriString")
                    Log.w(TAG, "Attempting to continue anyway with DocumentFile...")
                }

                val documentFile = DocumentFile.fromTreeUri(context, folderUri)
                Log.d(TAG, "DocumentFile created: ${documentFile != null}")

                if (documentFile == null) {
                    Log.e(TAG, "DocumentFile.fromTreeUri returned null for: $folderUriString")
                    continue
                }

                Log.d(TAG, "DocumentFile.exists(): ${documentFile.exists()}")
                Log.d(TAG, "DocumentFile.canRead(): ${documentFile.canRead()}")
                Log.d(TAG, "DocumentFile.isDirectory(): ${documentFile.isDirectory}")
                Log.d(TAG, "DocumentFile.name: ${documentFile.name}")
                Log.d(TAG, "DocumentFile.uri: ${documentFile.uri}")

                if (!documentFile.exists()) {
                    Log.w(TAG, "Folder does not exist: $folderUriString")
                    continue
                }

                if (!documentFile.canRead()) {
                    Log.w(TAG, "Cannot read folder (permission issue?): $folderUriString")
                    continue
                }

                Log.d(TAG, "Starting recursive scan of folder: ${documentFile.name}")
                val importedCount = scanDocumentFileRecursively(documentFile)
                totalImported += importedCount
                Log.d(TAG, "Imported $importedCount books from: ${documentFile.name}")

            } catch (e: Exception) {
                Log.e(TAG, "Error scanning folder: $folderUriString", e)
                e.printStackTrace()
                // Continue to next folder on error
            }
        }

        Log.d(TAG, "=== LIBRARY SCAN COMPLETE ===")
        Log.d(TAG, "Total books imported: $totalImported")
        totalImported
    }

    /**
     * Check if we have persistable permission for a URI.
     * Compares URIs using string representation to handle encoding differences.
     */
    private fun hasUriPermission(uri: Uri): Boolean {
        val persistedUris = context.contentResolver.persistedUriPermissions
        val uriString = uri.toString()

        Log.d(TAG, "hasUriPermission: Checking URI: $uriString")
        Log.d(TAG, "hasUriPermission: Number of persisted permissions: ${persistedUris.size}")

        for (permission in persistedUris) {
            val permissionUriString = permission.uri.toString()
            Log.d(TAG, "hasUriPermission: Comparing with: $permissionUriString")

            // Check exact match
            if (permissionUriString == uriString) {
                Log.d(TAG, "hasUriPermission: EXACT MATCH found!")
                return true
            }

            // Check if the stored URI starts with the permission URI (child folder case)
            // or if permission URI starts with stored URI (parent folder case)
            if (uriString.startsWith(permissionUriString) || permissionUriString.startsWith(uriString)) {
                if (permission.isReadPermission) {
                    Log.d(TAG, "hasUriPermission: PREFIX MATCH found with read permission!")
                    return true
                }
            }

            // Check if URIs match when decoded (handles encoding differences)
            try {
                val decodedUri = java.net.URLDecoder.decode(uriString, "UTF-8")
                val decodedPermissionUri = java.net.URLDecoder.decode(permissionUriString, "UTF-8")
                if (decodedUri == decodedPermissionUri && permission.isReadPermission) {
                    Log.d(TAG, "hasUriPermission: DECODED MATCH found!")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "hasUriPermission: Failed to decode URIs for comparison", e)
            }
        }

        Log.w(TAG, "hasUriPermission: NO MATCH found for URI: $uriString")
        return false
    }

    /**
     * Recursively scan a DocumentFile directory for supported files.
     * Imports each found file using LibraryRepository.
     *
     * @param directory The DocumentFile directory to scan
     * @return Number of books imported from this directory and its subdirectories
     */
    private suspend fun scanDocumentFileRecursively(directory: DocumentFile): Int {
        var importedCount = 0

        Log.d(TAG, "scanDocumentFileRecursively: Entering directory: ${directory.name}")
        Log.d(TAG, "scanDocumentFileRecursively: Directory URI: ${directory.uri}")

        val files = try {
            val result = directory.listFiles()
            Log.d(TAG, "scanDocumentFileRecursively: listFiles() returned ${result.size} items")
            result
        } catch (e: Exception) {
            Log.e(TAG, "scanDocumentFileRecursively: FAILED to list files in: ${directory.name}", e)
            e.printStackTrace()
            return 0
        }

        if (files.isEmpty()) {
            Log.w(TAG, "scanDocumentFileRecursively: Directory is EMPTY: ${directory.name}")
            return 0
        }

        for (file in files) {
            try {
                val fileName = file.name ?: "unknown"
                Log.d(TAG, "scanDocumentFileRecursively: Processing item: $fileName (isDirectory=${file.isDirectory}, isFile=${file.isFile})")

                when {
                    file.isDirectory -> {
                        // Recursively scan subdirectories
                        Log.d(TAG, "scanDocumentFileRecursively: Recursing into subdirectory: $fileName")
                        importedCount += scanDocumentFileRecursively(file)
                    }
                    file.isFile -> {
                        // Check if this is a supported file type
                        val extension = fileName.substringAfterLast('.', "").lowercase()
                        Log.d(TAG, "scanDocumentFileRecursively: File extension: '$extension'")

                        if (extension in ALL_SUPPORTED_EXTENSIONS) {
                            val fileUri = file.uri
                            Log.d(TAG, "scanDocumentFileRecursively: SUPPORTED FILE FOUND: $fileName")
                            Log.d(TAG, "scanDocumentFileRecursively: File URI: $fileUri")

                            // Import the file using LibraryRepository
                            val book = try {
                                Log.d(TAG, "scanDocumentFileRecursively: Calling libraryRepository.importBook()...")
                                val result = libraryRepository.importBook(fileUri)
                                Log.d(TAG, "scanDocumentFileRecursively: importBook() returned: ${result?.title ?: "null"}")
                                result
                            } catch (e: Exception) {
                                Log.e(TAG, "scanDocumentFileRecursively: FAILED to import: $fileName", e)
                                e.printStackTrace()
                                null
                            }

                            if (book != null) {
                                importedCount++
                                Log.d(TAG, "scanDocumentFileRecursively: SUCCESS - Imported: ${book.title} (total: $importedCount)")
                            } else {
                                Log.w(TAG, "scanDocumentFileRecursively: importBook returned NULL for: $fileName")
                            }
                        } else {
                            Log.d(TAG, "scanDocumentFileRecursively: Skipping unsupported file type: $fileName (extension: $extension)")
                        }
                    }
                    else -> {
                        Log.d(TAG, "scanDocumentFileRecursively: Skipping unknown item type: $fileName")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "scanDocumentFileRecursively: Error processing file: ${file.name}", e)
                e.printStackTrace()
                // Continue to next file on error
            }
        }

        Log.d(TAG, "scanDocumentFileRecursively: Exiting directory: ${directory.name}, imported: $importedCount")
        return importedCount
    }

    // ========================================================================
    // Language Settings
    // ========================================================================

    val appLanguage = repository.appLanguage
        .stateIn(viewModelScope, SharingStarted.Lazily, "System")

    fun setAppLanguage(language: String) {
        viewModelScope.launch {
            repository.setAppLanguage(language)

            // Get the locale tag
            val localeTag = when (language) {
                "System" -> ""
                "English" -> "en"
                "Spanish" -> "es"
                "French" -> "fr"
                "German" -> "de"
                "Italian" -> "it"
                "Portuguese" -> "pt"
                "Japanese" -> "ja"
                "Korean" -> "ko"
                "Chinese" -> "zh"
                "Russian" -> "ru"
                "Arabic" -> "ar"
                else -> ""
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ (API 33+): Use native LocaleManager
                    val localeManager = context.getSystemService(LocaleManager::class.java)
                    if (localeTag.isEmpty()) {
                        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
                    } else {
                        localeManager.applicationLocales = LocaleList.forLanguageTags(localeTag)
                    }
                    android.util.Log.d("SettingsViewModel", "Set locale via LocaleManager: $localeTag")
                } else {
                    // Android 12 and below: Use AppCompatDelegate
                    val localeList = if (localeTag.isEmpty()) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(localeTag)
                    }
                    AppCompatDelegate.setApplicationLocales(localeList)
                    android.util.Log.d("SettingsViewModel", "Set locale via AppCompatDelegate: $localeTag")
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to set app locale: $localeTag", e)
            }
        }
    }

    // ========================================================================
    // Cover Art Settings
    // ========================================================================

    /**
     * Re-check all books for non-square cover art and replace with square versions.
     *
     * @param onProgress Callback for progress updates (current, total)
     * @return Number of covers replaced
     */
    suspend fun recheckAllCovers(
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting cover art recheck...")
        val result = coverArtRepository.recheckAllCovers(onProgress)
        Log.d(TAG, "Cover art recheck complete: $result covers replaced")
        result
    }
}
