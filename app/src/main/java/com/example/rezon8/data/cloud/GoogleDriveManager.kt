package com.mossglen.reverie.data.cloud

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive Manager - Handles file sync with Google Drive.
 *
 * Features:
 * - Upload/Download backup files
 * - List backups in Drive
 * - Delete backups from Drive
 * - Uses app-specific folder in Drive
 */
@Singleton
class GoogleDriveManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GoogleDriveManager"
        private const val APP_NAME = "REVERIE"
        private const val FOLDER_NAME = "REVERIE_Backups"
        private const val MIME_TYPE_FOLDER = "application/vnd.google-apps.folder"
        private const val MIME_TYPE_ZIP = "application/zip"
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

    private var driveService: Drive? = null
    private var appFolderId: String? = null

    /**
     * Initialize the Drive service with the signed-in account.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            _syncState.value = SyncState.Connecting

            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) {
                Log.e(TAG, "No signed-in Google account found")
                _syncState.value = SyncState.Error("Not signed in to Google")
                return@withContext false
            }

            // Check if we have the Android account object
            val androidAccount = account.account
            if (androidAccount == null) {
                Log.e(TAG, "Google account exists but Android Account object is null - need re-authentication")
                _syncState.value = SyncState.Error("Please sign in again to grant Drive access")
                return@withContext false
            }

            Log.d(TAG, "Initializing Drive for account: ${account.email}")

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE, DriveScopes.DRIVE_APPDATA)
            ).apply {
                selectedAccount = androidAccount
            }

            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName(APP_NAME).build()

            // Verify connection by getting or creating app folder
            appFolderId = getOrCreateAppFolder()

            if (appFolderId == null) {
                Log.e(TAG, "Failed to create/access app folder in Drive")
                _syncState.value = SyncState.Error("Could not access Google Drive folder")
                _isConnected.value = false
                return@withContext false
            }

            _isConnected.value = true
            _syncState.value = SyncState.Idle
            Log.d(TAG, "Google Drive initialized successfully with folder: $appFolderId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Google Drive", e)
            val errorMessage = when {
                e.message?.contains("NetworkError") == true -> "Network error - check your connection"
                e.message?.contains("401") == true -> "Authentication expired - please sign in again"
                e.message?.contains("403") == true -> "Access denied - please grant Drive permissions"
                else -> "Failed to connect: ${e.localizedMessage ?: e.message}"
            }
            _syncState.value = SyncState.Error(errorMessage)
            _isConnected.value = false
            false
        }
    }

    /**
     * Get or create the app's backup folder in Drive.
     */
    private suspend fun getOrCreateAppFolder(): String? = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext null

            // Search for existing folder
            val query = "name = '$FOLDER_NAME' and mimeType = '$MIME_TYPE_FOLDER' and trashed = false"
            val result = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            if (result.files.isNotEmpty()) {
                Log.d(TAG, "Found existing folder: ${result.files[0].id}")
                return@withContext result.files[0].id
            }

            // Create new folder
            val folderMetadata = DriveFile().apply {
                name = FOLDER_NAME
                mimeType = MIME_TYPE_FOLDER
            }

            val folder = drive.files().create(folderMetadata)
                .setFields("id")
                .execute()

            Log.d(TAG, "Created new folder: ${folder.id}")
            folder.id

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get/create app folder", e)
            null
        }
    }

    /**
     * Upload a backup file to Google Drive.
     */
    suspend fun uploadBackup(localFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: run {
                _syncState.value = SyncState.Error("Not connected to Google Drive")
                return@withContext false
            }

            val folderId = appFolderId ?: run {
                _syncState.value = SyncState.Error("App folder not found")
                return@withContext false
            }

            _syncState.value = SyncState.Uploading(0f, localFile.name)

            // Check if file already exists
            val existingFileId = findFileByName(localFile.name)

            val fileMetadata = DriveFile().apply {
                name = localFile.name
                parents = listOf(folderId)
            }

            val mediaContent = FileContent(MIME_TYPE_ZIP, localFile)

            val uploadedFile = if (existingFileId != null) {
                // Update existing file
                drive.files().update(existingFileId, fileMetadata, mediaContent)
                    .setFields("id, name, size, modifiedTime")
                    .execute()
            } else {
                // Create new file
                drive.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, size, modifiedTime")
                    .execute()
            }

            Log.d(TAG, "Uploaded: ${uploadedFile.name} (${uploadedFile.id})")
            _syncState.value = SyncState.Success("Backup uploaded to Google Drive")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            _syncState.value = SyncState.Error("Upload failed: ${e.message}")
            false
        }
    }

    /**
     * Download a backup file from Google Drive.
     */
    suspend fun downloadBackup(fileId: String, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: run {
                _syncState.value = SyncState.Error("Not connected to Google Drive")
                return@withContext false
            }

            _syncState.value = SyncState.Downloading(0f, destinationFile.name)

            FileOutputStream(destinationFile).use { outputStream ->
                drive.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream)
            }

            Log.d(TAG, "Downloaded: ${destinationFile.name}")
            _syncState.value = SyncState.Success("Backup downloaded from Google Drive")
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
            val drive = driveService ?: return@withContext emptyList()
            val folderId = appFolderId ?: return@withContext emptyList()

            val query = "'$folderId' in parents and trashed = false"
            val result = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, size, modifiedTime)")
                .setOrderBy("modifiedTime desc")
                .execute()

            result.files.map { file ->
                CloudBackupInfo(
                    id = file.id,
                    name = file.name,
                    size = file.getSize() ?: 0L,
                    modifiedTime = file.modifiedTime?.value ?: 0L,
                    source = CloudSource.GOOGLE_DRIVE
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to list backups", e)
            emptyList()
        }
    }

    /**
     * Delete a backup from Google Drive.
     */
    suspend fun deleteBackup(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext false

            drive.files().delete(fileId).execute()
            Log.d(TAG, "Deleted file: $fileId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete backup", e)
            false
        }
    }

    /**
     * Find a file by name in the app folder.
     */
    private suspend fun findFileByName(fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext null
            val folderId = appFolderId ?: return@withContext null

            val query = "name = '$fileName' and '$folderId' in parents and trashed = false"
            val result = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id)")
                .execute()

            result.files.firstOrNull()?.id

        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reset the sync state.
     */
    fun resetState() {
        _syncState.value = SyncState.Idle
    }

    /**
     * Upload a single file (like an audiobook or cover) to Google Drive.
     * Creates a REVERIE_Books folder for audiobook backups.
     */
    suspend fun uploadFile(localFile: File, folderName: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: run {
                _syncState.value = SyncState.Error("Not connected to Google Drive")
                return@withContext false
            }

            // Get or create the books backup folder
            val targetFolderId = if (folderName != null) {
                getOrCreateSubFolder(folderName)
            } else {
                getOrCreateBooksFolder()
            } ?: run {
                _syncState.value = SyncState.Error("Could not create backup folder")
                return@withContext false
            }

            _syncState.value = SyncState.Uploading(0f, localFile.name)

            // Determine MIME type
            val mimeType = when (localFile.extension.lowercase()) {
                "mp3" -> "audio/mpeg"
                "m4a", "m4b" -> "audio/mp4"
                "flac" -> "audio/flac"
                "ogg", "opus" -> "audio/ogg"
                "wav" -> "audio/wav"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "zip" -> "application/zip"
                else -> "application/octet-stream"
            }

            val fileMetadata = DriveFile().apply {
                name = localFile.name
                parents = listOf(targetFolderId)
            }

            val mediaContent = FileContent(mimeType, localFile)

            val uploadedFile = drive.files().create(fileMetadata, mediaContent)
                .setFields("id, name, size")
                .execute()

            Log.d(TAG, "Uploaded file: ${uploadedFile.name} (${uploadedFile.id})")
            _syncState.value = SyncState.Success("Uploaded: ${localFile.name}")
            true

        } catch (e: Exception) {
            Log.e(TAG, "File upload failed", e)
            _syncState.value = SyncState.Error("Upload failed: ${e.message}")
            false
        }
    }

    /**
     * Get or create the books backup folder in Drive.
     */
    private suspend fun getOrCreateBooksFolder(): String? = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext null

            val folderName = "REVERIE_Books"

            // Search for existing folder
            val query = "name = '$folderName' and mimeType = '$MIME_TYPE_FOLDER' and trashed = false"
            val result = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            if (result.files.isNotEmpty()) {
                return@withContext result.files[0].id
            }

            // Create new folder
            val folderMetadata = DriveFile().apply {
                name = folderName
                mimeType = MIME_TYPE_FOLDER
            }

            val folder = drive.files().create(folderMetadata)
                .setFields("id")
                .execute()

            Log.d(TAG, "Created books folder: ${folder.id}")
            folder.id

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get/create books folder", e)
            null
        }
    }

    /**
     * Get or create a subfolder within the books folder.
     */
    private suspend fun getOrCreateSubFolder(subFolderName: String): String? = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext null

            val parentFolderId = getOrCreateBooksFolder() ?: return@withContext null

            // Search for existing subfolder
            val query = "name = '$subFolderName' and '$parentFolderId' in parents and mimeType = '$MIME_TYPE_FOLDER' and trashed = false"
            val result = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            if (result.files.isNotEmpty()) {
                return@withContext result.files[0].id
            }

            // Create new subfolder
            val folderMetadata = DriveFile().apply {
                name = subFolderName
                mimeType = MIME_TYPE_FOLDER
                parents = listOf(parentFolderId)
            }

            val folder = drive.files().create(folderMetadata)
                .setFields("id")
                .execute()

            Log.d(TAG, "Created subfolder: ${folder.id}")
            folder.id

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get/create subfolder", e)
            null
        }
    }

    /**
     * Disconnect from Google Drive.
     */
    fun disconnect() {
        driveService = null
        appFolderId = null
        _isConnected.value = false
        _syncState.value = SyncState.Idle
    }

    // ========================================================================
    // File Browser Methods
    // ========================================================================

    /**
     * List files in a specific folder (or root if folderId is null).
     */
    suspend fun listFiles(folderId: String? = null): List<CloudFileInfo> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext emptyList()

            val parentId = folderId ?: "root"
            val query = "'$parentId' in parents and trashed = false"

            val result = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, size, mimeType, modifiedTime, thumbnailLink)")
                .setOrderBy("folder,name")
                .setPageSize(100)
                .execute()

            result.files.map { file ->
                val isFolder = file.mimeType == MIME_TYPE_FOLDER
                CloudFileInfo(
                    id = file.id,
                    name = file.name,
                    path = file.id, // For Google Drive, we use ID as path
                    size = file.getSize() ?: 0L,
                    mimeType = file.mimeType ?: "",
                    modifiedTime = file.modifiedTime?.value ?: 0L,
                    isFolder = isFolder,
                    thumbnailUrl = file.thumbnailLink,
                    source = CloudSource.GOOGLE_DRIVE
                )
            }

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
            val drive = driveService ?: return@withContext emptyList()

            val searchQuery = "name contains '$query' and trashed = false"

            val result = drive.files().list()
                .setQ(searchQuery)
                .setSpaces("drive")
                .setFields("files(id, name, size, mimeType, modifiedTime, thumbnailLink)")
                .setOrderBy("modifiedTime desc")
                .setPageSize(50)
                .execute()

            result.files.map { file ->
                val isFolder = file.mimeType == MIME_TYPE_FOLDER
                CloudFileInfo(
                    id = file.id,
                    name = file.name,
                    path = file.id,
                    size = file.getSize() ?: 0L,
                    mimeType = file.mimeType ?: "",
                    modifiedTime = file.modifiedTime?.value ?: 0L,
                    isFolder = isFolder,
                    thumbnailUrl = file.thumbnailLink,
                    source = CloudSource.GOOGLE_DRIVE
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to search files", e)
            emptyList()
        }
    }

    /**
     * Download any file from Google Drive to a local destination.
     */
    suspend fun downloadFile(fileId: String, destinationFile: File, onProgress: (Float) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: run {
                _syncState.value = SyncState.Error("Not connected to Google Drive")
                return@withContext false
            }

            _syncState.value = SyncState.Downloading(0f, destinationFile.name)

            // Get file size for progress tracking
            val fileMetadata = drive.files().get(fileId).setFields("size").execute()
            val totalSize = fileMetadata.getSize() ?: 0L

            FileOutputStream(destinationFile).use { outputStream ->
                drive.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream)
            }

            onProgress(1f)
            Log.d(TAG, "Downloaded file: ${destinationFile.name}")
            _syncState.value = SyncState.Success("File downloaded from Google Drive")
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
    suspend fun getFileInfo(fileId: String): CloudFileInfo? = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext null

            val file = drive.files().get(fileId)
                .setFields("id, name, size, mimeType, modifiedTime, thumbnailLink")
                .execute()

            val isFolder = file.mimeType == MIME_TYPE_FOLDER
            CloudFileInfo(
                id = file.id,
                name = file.name,
                path = file.id,
                size = file.getSize() ?: 0L,
                mimeType = file.mimeType ?: "",
                modifiedTime = file.modifiedTime?.value ?: 0L,
                isFolder = isFolder,
                thumbnailUrl = file.thumbnailLink,
                source = CloudSource.GOOGLE_DRIVE
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file info", e)
            null
        }
    }
}

enum class CloudSource {
    GOOGLE_DRIVE,
    DROPBOX
}

data class CloudBackupInfo(
    val id: String,
    val name: String,
    val size: Long,
    val modifiedTime: Long,
    val source: CloudSource
) {
    val formattedSize: String
        get() = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / 1024 / 1024} MB"
        }

    val formattedDate: String
        get() = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.US)
            .format(java.util.Date(modifiedTime))
}

/**
 * Represents a file or folder in cloud storage for browsing.
 */
data class CloudFileInfo(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String,
    val modifiedTime: Long,
    val isFolder: Boolean,
    val thumbnailUrl: String? = null,
    val source: CloudSource
) {
    val formattedSize: String
        get() = when {
            isFolder -> ""
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / 1024 / 1024} MB"
            else -> String.format("%.2f GB", size / 1024.0 / 1024.0 / 1024.0)
        }

    val formattedDate: String
        get() = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
            .format(java.util.Date(modifiedTime))

    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()

    val isAudioBook: Boolean
        get() = extension in listOf("mp3", "m4b", "m4a", "flac", "ogg", "wav", "opus", "aac")

    val isDocument: Boolean
        get() = extension in listOf("epub", "pdf", "mobi", "azw", "azw3")

    val isTorrent: Boolean
        get() = extension == "torrent"

    val isImportable: Boolean
        get() = isAudioBook || isDocument || isTorrent

    val fileIcon: String
        get() = when {
            isFolder -> "folder"
            isAudioBook -> "audiobook"
            isDocument -> "document"
            isTorrent -> "torrent"
            else -> "file"
        }
}
