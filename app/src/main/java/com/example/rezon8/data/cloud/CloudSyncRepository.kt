package com.mossglen.lithos.data.cloud

import android.util.Log
import com.mossglen.lithos.data.DatabaseBackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud Sync Repository - Unified interface for cloud backup operations.
 *
 * Coordinates between:
 * - DatabaseBackupManager (local backups)
 * - GoogleDriveManager (Google Drive sync)
 * - DropboxManager (Dropbox sync)
 */
@Singleton
class CloudSyncRepository @Inject constructor(
    private val databaseBackupManager: DatabaseBackupManager,
    private val googleDriveManager: GoogleDriveManager,
    private val dropboxManager: DropboxManager
) {
    companion object {
        private const val TAG = "CloudSyncRepository"
    }

    sealed class CloudSyncState {
        object Idle : CloudSyncState()
        data class InProgress(val message: String, val source: CloudSource?) : CloudSyncState()
        data class Success(val message: String) : CloudSyncState()
        data class Error(val message: String) : CloudSyncState()
    }

    private val _syncState = MutableStateFlow<CloudSyncState>(CloudSyncState.Idle)
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    // Expose individual manager states
    val googleDriveSyncState = googleDriveManager.syncState
    val dropboxSyncState = dropboxManager.syncState
    val googleDriveConnected = googleDriveManager.isConnected
    val dropboxConnected = dropboxManager.isConnected

    /**
     * Initialize Google Drive connection.
     */
    suspend fun initializeGoogleDrive(): Boolean {
        return googleDriveManager.initialize()
    }

    /**
     * Initialize Dropbox connection.
     */
    suspend fun initializeDropbox(): Boolean {
        return dropboxManager.initialize()
    }

    /**
     * Check if Dropbox has stored credentials.
     */
    fun hasDropboxCredential(): Boolean {
        return dropboxManager.hasStoredCredential()
    }

    /**
     * Get Dropbox app key for OAuth.
     */
    fun getDropboxAppKey(): String {
        return dropboxManager.getAppKey()
    }

    /**
     * Backup to Google Drive.
     * Creates a local backup first, then uploads to Drive.
     */
    suspend fun backupToGoogleDrive(): Boolean {
        _syncState.value = CloudSyncState.InProgress("Creating backup...", CloudSource.GOOGLE_DRIVE)

        // Create local backup
        val backupPath = databaseBackupManager.exportDatabase()
        if (backupPath == null) {
            _syncState.value = CloudSyncState.Error("Failed to create local backup")
            return false
        }

        _syncState.value = CloudSyncState.InProgress("Uploading to Google Drive...", CloudSource.GOOGLE_DRIVE)

        // Upload to Drive
        val backupFile = File(backupPath)
        val result = googleDriveManager.uploadBackup(backupFile)

        if (result) {
            _syncState.value = CloudSyncState.Success("Backup uploaded to Google Drive")
            Log.d(TAG, "Successfully backed up to Google Drive")
        } else {
            _syncState.value = CloudSyncState.Error("Failed to upload to Google Drive")
        }

        return result
    }

    /**
     * Backup to Dropbox.
     * Creates a local backup first, then uploads to Dropbox.
     */
    suspend fun backupToDropbox(): Boolean {
        _syncState.value = CloudSyncState.InProgress("Creating backup...", CloudSource.DROPBOX)

        // Create local backup
        val backupPath = databaseBackupManager.exportDatabase()
        if (backupPath == null) {
            _syncState.value = CloudSyncState.Error("Failed to create local backup")
            return false
        }

        _syncState.value = CloudSyncState.InProgress("Uploading to Dropbox...", CloudSource.DROPBOX)

        // Upload to Dropbox
        val backupFile = File(backupPath)
        val result = dropboxManager.uploadBackup(backupFile)

        if (result) {
            _syncState.value = CloudSyncState.Success("Backup uploaded to Dropbox")
            Log.d(TAG, "Successfully backed up to Dropbox")
        } else {
            _syncState.value = CloudSyncState.Error("Failed to upload to Dropbox")
        }

        return result
    }

    /**
     * Restore from Google Drive.
     */
    suspend fun restoreFromGoogleDrive(backupInfo: CloudBackupInfo): Boolean {
        _syncState.value = CloudSyncState.InProgress("Downloading from Google Drive...", CloudSource.GOOGLE_DRIVE)

        // Download to temp file
        val tempFile = File(databaseBackupManager.getBackupDirectory(), "temp_restore.zip")

        val downloadResult = googleDriveManager.downloadBackup(backupInfo.id, tempFile)
        if (!downloadResult) {
            _syncState.value = CloudSyncState.Error("Failed to download backup")
            tempFile.delete()
            return false
        }

        _syncState.value = CloudSyncState.InProgress("Restoring database...", CloudSource.GOOGLE_DRIVE)

        // Import the backup
        val importResult = databaseBackupManager.importDatabase(tempFile)
        tempFile.delete()

        if (importResult) {
            _syncState.value = CloudSyncState.Success("Database restored. Please restart the app.")
            Log.d(TAG, "Successfully restored from Google Drive")
        } else {
            _syncState.value = CloudSyncState.Error("Failed to restore database")
        }

        return importResult
    }

    /**
     * Restore from Dropbox.
     */
    suspend fun restoreFromDropbox(backupInfo: CloudBackupInfo): Boolean {
        _syncState.value = CloudSyncState.InProgress("Downloading from Dropbox...", CloudSource.DROPBOX)

        // Download to temp file
        val tempFile = File(databaseBackupManager.getBackupDirectory(), "temp_restore.zip")

        val downloadResult = dropboxManager.downloadBackup(backupInfo.id, tempFile)
        if (!downloadResult) {
            _syncState.value = CloudSyncState.Error("Failed to download backup")
            tempFile.delete()
            return false
        }

        _syncState.value = CloudSyncState.InProgress("Restoring database...", CloudSource.DROPBOX)

        // Import the backup
        val importResult = databaseBackupManager.importDatabase(tempFile)
        tempFile.delete()

        if (importResult) {
            _syncState.value = CloudSyncState.Success("Database restored. Please restart the app.")
            Log.d(TAG, "Successfully restored from Dropbox")
        } else {
            _syncState.value = CloudSyncState.Error("Failed to restore database")
        }

        return importResult
    }

    /**
     * List all cloud backups from both services.
     */
    suspend fun listAllCloudBackups(): List<CloudBackupInfo> {
        val driveBackups = if (googleDriveManager.isConnected.value) {
            googleDriveManager.listBackups()
        } else emptyList()

        val dropboxBackups = if (dropboxManager.isConnected.value) {
            dropboxManager.listBackups()
        } else emptyList()

        return (driveBackups + dropboxBackups).sortedByDescending { it.modifiedTime }
    }

    /**
     * List Google Drive backups.
     */
    suspend fun listGoogleDriveBackups(): List<CloudBackupInfo> {
        return if (googleDriveManager.isConnected.value) {
            googleDriveManager.listBackups()
        } else emptyList()
    }

    /**
     * List Dropbox backups.
     */
    suspend fun listDropboxBackups(): List<CloudBackupInfo> {
        return if (dropboxManager.isConnected.value) {
            dropboxManager.listBackups()
        } else emptyList()
    }

    /**
     * Delete a cloud backup.
     */
    suspend fun deleteCloudBackup(backupInfo: CloudBackupInfo): Boolean {
        return when (backupInfo.source) {
            CloudSource.GOOGLE_DRIVE -> googleDriveManager.deleteBackup(backupInfo.id)
            CloudSource.DROPBOX -> dropboxManager.deleteBackup(backupInfo.id)
        }
    }

    /**
     * Disconnect from Google Drive.
     */
    fun disconnectGoogleDrive() {
        googleDriveManager.disconnect()
    }

    /**
     * Disconnect from Dropbox.
     */
    fun disconnectDropbox() {
        dropboxManager.disconnect()
    }

    /**
     * Get Dropbox account name.
     */
    suspend fun getDropboxAccountName(): String? {
        return dropboxManager.getAccountName()
    }

    /**
     * Reset sync state.
     */
    fun resetState() {
        _syncState.value = CloudSyncState.Idle
        googleDriveManager.resetState()
        dropboxManager.resetState()
    }
}
