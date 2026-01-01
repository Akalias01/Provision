package com.mossglen.lithos.data.cloud

import android.content.Context
import android.util.Log
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dropbox Manager - Handles file sync with Dropbox.
 *
 * Features:
 * - Upload/Download backup files
 * - List backups in Dropbox
 * - Delete backups from Dropbox
 * - Uses app-specific folder in Dropbox
 */
@Singleton
class DropboxManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DropboxManager"
        private const val APP_KEY = "YOUR_DROPBOX_APP_KEY" // Replace with actual app key
        private const val FOLDER_PATH = "/Lithos_Backups"
        private const val PREFS_NAME = "dropbox_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }

    sealed class SyncState {
        object Idle : SyncState()
        object Connecting : SyncState()
        data class Uploading(val progress: Float, val fileName: String) : SyncState()
        data class Downloading(val progress: Float, val fileName: String) : SyncState()
        data class Success(val message: String) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var dropboxClient: DbxClientV2? = null

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Get the Dropbox app key for OAuth flow.
     */
    fun getAppKey(): String = APP_KEY

    /**
     * Initialize the Dropbox client with stored credentials.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            _syncState.value = SyncState.Connecting

            val credential = loadCredential()
            if (credential == null) {
                _syncState.value = SyncState.Error("Not signed in to Dropbox")
                return@withContext false
            }

            val config = DbxRequestConfig.newBuilder("Lithos/1.0").build()
            dropboxClient = DbxClientV2(config, credential)

            // Verify connection by getting account info
            dropboxClient?.users()?.currentAccount

            // Ensure backup folder exists
            ensureFolderExists()

            _isConnected.value = true
            _syncState.value = SyncState.Idle
            Log.d(TAG, "Dropbox initialized successfully")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Dropbox", e)
            _syncState.value = SyncState.Error("Failed to connect: ${e.message}")
            _isConnected.value = false
            false
        }
    }

    /**
     * Save OAuth credential after successful authentication.
     */
    fun saveCredential(credential: DbxCredential) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, credential.accessToken)
            putString(KEY_REFRESH_TOKEN, credential.refreshToken)
            putLong(KEY_EXPIRES_AT, credential.expiresAt ?: 0L)
            apply()
        }
    }

    /**
     * Load stored OAuth credential.
     */
    private fun loadCredential(): DbxCredential? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)

        return DbxCredential(
            accessToken,
            expiresAt.takeIf { it > 0 },
            refreshToken,
            APP_KEY,
            null // App secret not needed for PKCE flow
        )
    }

    /**
     * Check if credentials are stored.
     */
    fun hasStoredCredential(): Boolean {
        return prefs.getString(KEY_ACCESS_TOKEN, null) != null
    }

    /**
     * Ensure the backup folder exists in Dropbox.
     */
    private suspend fun ensureFolderExists() = withContext(Dispatchers.IO) {
        try {
            val client = dropboxClient ?: return@withContext

            try {
                client.files().getMetadata(FOLDER_PATH)
                Log.d(TAG, "Backup folder exists")
            } catch (e: Exception) {
                // Folder doesn't exist, create it
                client.files().createFolderV2(FOLDER_PATH)
                Log.d(TAG, "Created backup folder")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure folder exists", e)
        }
    }

    /**
     * Upload a backup file to Dropbox.
     */
    suspend fun uploadBackup(localFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = dropboxClient ?: run {
                _syncState.value = SyncState.Error("Not connected to Dropbox")
                return@withContext false
            }

            _syncState.value = SyncState.Uploading(0f, localFile.name)

            val remotePath = "$FOLDER_PATH/${localFile.name}"

            FileInputStream(localFile).use { inputStream ->
                client.files()
                    .uploadBuilder(remotePath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(inputStream)
            }

            Log.d(TAG, "Uploaded: ${localFile.name}")
            _syncState.value = SyncState.Success("Backup uploaded to Dropbox")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            _syncState.value = SyncState.Error("Upload failed: ${e.message}")
            false
        }
    }

    /**
     * Download a backup file from Dropbox.
     */
    suspend fun downloadBackup(remotePath: String, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = dropboxClient ?: run {
                _syncState.value = SyncState.Error("Not connected to Dropbox")
                return@withContext false
            }

            _syncState.value = SyncState.Downloading(0f, destinationFile.name)

            FileOutputStream(destinationFile).use { outputStream ->
                client.files()
                    .download(remotePath)
                    .download(outputStream)
            }

            Log.d(TAG, "Downloaded: ${destinationFile.name}")
            _syncState.value = SyncState.Success("Backup downloaded from Dropbox")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _syncState.value = SyncState.Error("Download failed: ${e.message}")
            false
        }
    }

    /**
     * List all backups in the app folder.
     */
    suspend fun listBackups(): List<CloudBackupInfo> = withContext(Dispatchers.IO) {
        try {
            val client = dropboxClient ?: return@withContext emptyList()

            val result = client.files().listFolder(FOLDER_PATH)

            result.entries.mapNotNull { entry ->
                if (entry is com.dropbox.core.v2.files.FileMetadata) {
                    CloudBackupInfo(
                        id = entry.pathLower ?: entry.name,
                        name = entry.name,
                        size = entry.size,
                        modifiedTime = entry.serverModified?.time ?: 0L,
                        source = CloudSource.DROPBOX
                    )
                } else null
            }.sortedByDescending { it.modifiedTime }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to list backups", e)
            emptyList()
        }
    }

    /**
     * Delete a backup from Dropbox.
     */
    suspend fun deleteBackup(remotePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = dropboxClient ?: return@withContext false

            client.files().deleteV2(remotePath)
            Log.d(TAG, "Deleted file: $remotePath")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete backup", e)
            false
        }
    }

    /**
     * Reset the sync state.
     */
    fun resetState() {
        _syncState.value = SyncState.Idle
    }

    /**
     * Disconnect from Dropbox and clear credentials.
     */
    fun disconnect() {
        dropboxClient = null
        _isConnected.value = false
        _syncState.value = SyncState.Idle

        // Clear stored credentials
        prefs.edit().clear().apply()
    }

    /**
     * Get account display name.
     */
    suspend fun getAccountName(): String? = withContext(Dispatchers.IO) {
        try {
            dropboxClient?.users()?.currentAccount?.name?.displayName
        } catch (e: Exception) {
            null
        }
    }

    // ========================================================================
    // File Browser Methods
    // ========================================================================

    /**
     * List files in a specific folder (or root if path is empty).
     */
    suspend fun listFiles(folderPath: String = ""): List<CloudFileInfo> = withContext(Dispatchers.IO) {
        try {
            val client = dropboxClient ?: return@withContext emptyList()

            val result = client.files().listFolder(folderPath)

            result.entries.map { entry ->
                when (entry) {
                    is com.dropbox.core.v2.files.FolderMetadata -> {
                        CloudFileInfo(
                            id = entry.id ?: entry.pathLower ?: entry.name,
                            name = entry.name,
                            path = entry.pathLower ?: "/${entry.name}",
                            size = 0L,
                            mimeType = "folder",
                            modifiedTime = 0L,
                            isFolder = true,
                            thumbnailUrl = null,
                            source = CloudSource.DROPBOX
                        )
                    }
                    is com.dropbox.core.v2.files.FileMetadata -> {
                        CloudFileInfo(
                            id = entry.id ?: entry.pathLower ?: entry.name,
                            name = entry.name,
                            path = entry.pathLower ?: "/${entry.name}",
                            size = entry.size,
                            mimeType = getMimeType(entry.name),
                            modifiedTime = entry.serverModified?.time ?: 0L,
                            isFolder = false,
                            thumbnailUrl = null,
                            source = CloudSource.DROPBOX
                        )
                    }
                    else -> {
                        CloudFileInfo(
                            id = entry.name,
                            name = entry.name,
                            path = "/${entry.name}",
                            size = 0L,
                            mimeType = "",
                            modifiedTime = 0L,
                            isFolder = false,
                            thumbnailUrl = null,
                            source = CloudSource.DROPBOX
                        )
                    }
                }
            }.sortedWith(compareBy({ !it.isFolder }, { it.name.lowercase() }))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to list files", e)
            emptyList()
        }
    }

    /**
     * Search for files by name.
     */
    suspend fun searchFiles(query: String): List<CloudFileInfo> = withContext(Dispatchers.IO) {
        try {
            val client = dropboxClient ?: return@withContext emptyList()

            val result = client.files().searchV2(query)

            result.matches.mapNotNull { match ->
                val metadata = match.metadata.metadataValue
                when (metadata) {
                    is com.dropbox.core.v2.files.FileMetadata -> {
                        CloudFileInfo(
                            id = metadata.id ?: metadata.pathLower ?: metadata.name,
                            name = metadata.name,
                            path = metadata.pathLower ?: "/${metadata.name}",
                            size = metadata.size,
                            mimeType = getMimeType(metadata.name),
                            modifiedTime = metadata.serverModified?.time ?: 0L,
                            isFolder = false,
                            thumbnailUrl = null,
                            source = CloudSource.DROPBOX
                        )
                    }
                    is com.dropbox.core.v2.files.FolderMetadata -> {
                        CloudFileInfo(
                            id = metadata.id ?: metadata.pathLower ?: metadata.name,
                            name = metadata.name,
                            path = metadata.pathLower ?: "/${metadata.name}",
                            size = 0L,
                            mimeType = "folder",
                            modifiedTime = 0L,
                            isFolder = true,
                            thumbnailUrl = null,
                            source = CloudSource.DROPBOX
                        )
                    }
                    else -> null
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to search files", e)
            emptyList()
        }
    }

    /**
     * Download any file from Dropbox to a local destination.
     */
    suspend fun downloadFile(remotePath: String, destinationFile: File, onProgress: (Float) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = dropboxClient ?: run {
                _syncState.value = SyncState.Error("Not connected to Dropbox")
                return@withContext false
            }

            _syncState.value = SyncState.Downloading(0f, destinationFile.name)

            FileOutputStream(destinationFile).use { outputStream ->
                client.files()
                    .download(remotePath)
                    .download(outputStream)
            }

            onProgress(1f)
            Log.d(TAG, "Downloaded file: ${destinationFile.name}")
            _syncState.value = SyncState.Success("File downloaded from Dropbox")
            true

        } catch (e: Exception) {
            Log.e(TAG, "File download failed", e)
            _syncState.value = SyncState.Error("Download failed: ${e.message}")
            false
        }
    }

    /**
     * Get file metadata.
     */
    suspend fun getFileInfo(remotePath: String): CloudFileInfo? = withContext(Dispatchers.IO) {
        try {
            val client = dropboxClient ?: return@withContext null

            val metadata = client.files().getMetadata(remotePath)

            when (metadata) {
                is com.dropbox.core.v2.files.FileMetadata -> {
                    CloudFileInfo(
                        id = metadata.id ?: metadata.pathLower ?: metadata.name,
                        name = metadata.name,
                        path = metadata.pathLower ?: remotePath,
                        size = metadata.size,
                        mimeType = getMimeType(metadata.name),
                        modifiedTime = metadata.serverModified?.time ?: 0L,
                        isFolder = false,
                        thumbnailUrl = null,
                        source = CloudSource.DROPBOX
                    )
                }
                is com.dropbox.core.v2.files.FolderMetadata -> {
                    CloudFileInfo(
                        id = metadata.id ?: metadata.pathLower ?: metadata.name,
                        name = metadata.name,
                        path = metadata.pathLower ?: remotePath,
                        size = 0L,
                        mimeType = "folder",
                        modifiedTime = 0L,
                        isFolder = true,
                        thumbnailUrl = null,
                        source = CloudSource.DROPBOX
                    )
                }
                else -> null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file info", e)
            null
        }
    }

    /**
     * Helper to get MIME type from filename.
     */
    private fun getMimeType(filename: String): String {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "mp3" -> "audio/mpeg"
            "m4b", "m4a" -> "audio/mp4"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "opus" -> "audio/opus"
            "aac" -> "audio/aac"
            "epub" -> "application/epub+zip"
            "pdf" -> "application/pdf"
            "torrent" -> "application/x-bittorrent"
            else -> "application/octet-stream"
        }
    }
}
