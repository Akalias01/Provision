package com.mossglen.lithos.data

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Database Backup Manager - Handles export and import of the app's Room database.
 *
 * Features:
 * - Export database to a .zip backup file
 * - Import database from a backup file
 * - Backup includes all database files (main db, wal, shm)
 * - Progress tracking via StateFlow
 */
@Singleton
class DatabaseBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    companion object {
        private const val TAG = "DatabaseBackupManager"
        private const val DATABASE_NAME = "reverie.db"
        private const val BACKUP_PREFIX = "reverie_backup_"
        private const val BACKUP_EXTENSION = ".zip"
    }

    sealed class BackupState {
        object Idle : BackupState()
        data class InProgress(val message: String) : BackupState()
        data class Success(val message: String, val filePath: String? = null) : BackupState()
        data class Error(val message: String) : BackupState()
    }

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    /**
     * Get the backup directory.
     */
    fun getBackupDirectory(): File {
        return File(context.getExternalFilesDir(null), "Backups").also {
            if (!it.exists()) it.mkdirs()
        }
    }

    /**
     * Export the database to a backup file.
     *
     * @return The path to the created backup file, or null if failed
     */
    suspend fun exportDatabase(): String? = withContext(Dispatchers.IO) {
        try {
            _backupState.value = BackupState.InProgress("Preparing backup...")

            // Close database connections
            database.close()

            // Get database files
            val dbPath = context.getDatabasePath(DATABASE_NAME)
            val dbDir = dbPath.parentFile ?: return@withContext null

            val dbFiles = listOf(
                File(dbDir, DATABASE_NAME),
                File(dbDir, "$DATABASE_NAME-wal"),
                File(dbDir, "$DATABASE_NAME-shm")
            ).filter { it.exists() }

            if (dbFiles.isEmpty()) {
                _backupState.value = BackupState.Error("No database files found")
                return@withContext null
            }

            // Create backup file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(getBackupDirectory(), "$BACKUP_PREFIX$timestamp$BACKUP_EXTENSION")

            _backupState.value = BackupState.InProgress("Creating backup...")

            // Create zip archive
            ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
                dbFiles.forEach { file ->
                    _backupState.value = BackupState.InProgress("Backing up ${file.name}...")

                    FileInputStream(file).use { input ->
                        val entry = ZipEntry(file.name)
                        zipOut.putNextEntry(entry)
                        input.copyTo(zipOut)
                        zipOut.closeEntry()
                    }
                }
            }

            Log.d(TAG, "Database exported to: ${backupFile.absolutePath}")
            _backupState.value = BackupState.Success(
                "Backup created successfully",
                backupFile.absolutePath
            )

            backupFile.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "Failed to export database", e)
            _backupState.value = BackupState.Error("Backup failed: ${e.message}")
            null
        }
    }

    /**
     * Export database to a specific URI (for SAF - Storage Access Framework).
     *
     * @param destinationUri The URI to write the backup to
     * @return true if successful
     */
    suspend fun exportToUri(destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            _backupState.value = BackupState.InProgress("Preparing backup...")

            // Close database connections
            database.close()

            // Get database files
            val dbPath = context.getDatabasePath(DATABASE_NAME)
            val dbDir = dbPath.parentFile ?: return@withContext false

            val dbFiles = listOf(
                File(dbDir, DATABASE_NAME),
                File(dbDir, "$DATABASE_NAME-wal"),
                File(dbDir, "$DATABASE_NAME-shm")
            ).filter { it.exists() }

            if (dbFiles.isEmpty()) {
                _backupState.value = BackupState.Error("No database files found")
                return@withContext false
            }

            _backupState.value = BackupState.InProgress("Creating backup...")

            // Write to the URI
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    dbFiles.forEach { file ->
                        _backupState.value = BackupState.InProgress("Backing up ${file.name}...")

                        FileInputStream(file).use { input ->
                            val entry = ZipEntry(file.name)
                            zipOut.putNextEntry(entry)
                            input.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    }
                }
            }

            Log.d(TAG, "Database exported to URI")
            _backupState.value = BackupState.Success("Backup created successfully")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to export database to URI", e)
            _backupState.value = BackupState.Error("Backup failed: ${e.message}")
            false
        }
    }

    /**
     * Import database from a backup file.
     *
     * @param backupFile The backup file to restore
     * @return true if successful
     */
    suspend fun importDatabase(backupFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) {
                _backupState.value = BackupState.Error("Backup file not found")
                return@withContext false
            }

            _backupState.value = BackupState.InProgress("Preparing restore...")

            // Close database connections
            database.close()

            // Get database directory
            val dbPath = context.getDatabasePath(DATABASE_NAME)
            val dbDir = dbPath.parentFile ?: return@withContext false

            // Delete existing database files
            _backupState.value = BackupState.InProgress("Clearing existing database...")
            listOf(
                File(dbDir, DATABASE_NAME),
                File(dbDir, "$DATABASE_NAME-wal"),
                File(dbDir, "$DATABASE_NAME-shm")
            ).forEach { it.delete() }

            // Extract backup
            _backupState.value = BackupState.InProgress("Restoring database...")

            ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val destFile = File(dbDir, entry.name)

                    // Security check
                    if (!destFile.canonicalPath.startsWith(dbDir.canonicalPath)) {
                        Log.w(TAG, "Skipping suspicious entry: ${entry.name}")
                        entry = zipIn.nextEntry
                        continue
                    }

                    FileOutputStream(destFile).use { output ->
                        zipIn.copyTo(output)
                    }

                    Log.d(TAG, "Restored: ${entry.name}")
                    entry = zipIn.nextEntry
                }
            }

            Log.d(TAG, "Database restored from: ${backupFile.absolutePath}")
            _backupState.value = BackupState.Success("Database restored successfully. Please restart the app.")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to import database", e)
            _backupState.value = BackupState.Error("Restore failed: ${e.message}")
            false
        }
    }

    /**
     * Import database from a URI (for SAF - Storage Access Framework).
     *
     * @param sourceUri The URI to read the backup from
     * @return true if successful
     */
    suspend fun importFromUri(sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            _backupState.value = BackupState.InProgress("Preparing restore...")

            // Close database connections
            database.close()

            // Get database directory
            val dbPath = context.getDatabasePath(DATABASE_NAME)
            val dbDir = dbPath.parentFile ?: return@withContext false

            // Delete existing database files
            _backupState.value = BackupState.InProgress("Clearing existing database...")
            listOf(
                File(dbDir, DATABASE_NAME),
                File(dbDir, "$DATABASE_NAME-wal"),
                File(dbDir, "$DATABASE_NAME-shm")
            ).forEach { it.delete() }

            // Extract backup
            _backupState.value = BackupState.InProgress("Restoring database...")

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val destFile = File(dbDir, entry.name)

                        // Security check
                        if (!destFile.canonicalPath.startsWith(dbDir.canonicalPath)) {
                            Log.w(TAG, "Skipping suspicious entry: ${entry.name}")
                            entry = zipIn.nextEntry
                            continue
                        }

                        FileOutputStream(destFile).use { output ->
                            zipIn.copyTo(output)
                        }

                        Log.d(TAG, "Restored: ${entry.name}")
                        entry = zipIn.nextEntry
                    }
                }
            }

            Log.d(TAG, "Database restored from URI")
            _backupState.value = BackupState.Success("Database restored successfully. Please restart the app.")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to import database from URI", e)
            _backupState.value = BackupState.Error("Restore failed: ${e.message}")
            false
        }
    }

    /**
     * Get list of available backup files.
     */
    fun getAvailableBackups(): List<BackupInfo> {
        val backupDir = getBackupDirectory()
        return backupDir.listFiles { file ->
            file.isFile && file.name.startsWith(BACKUP_PREFIX) && file.name.endsWith(BACKUP_EXTENSION)
        }?.map { file ->
            BackupInfo(
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                date = Date(file.lastModified())
            )
        }?.sortedByDescending { it.date } ?: emptyList()
    }

    /**
     * Delete a backup file.
     */
    fun deleteBackup(backupPath: String): Boolean {
        return try {
            File(backupPath).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete backup", e)
            false
        }
    }

    /**
     * Reset the backup state.
     */
    fun resetState() {
        _backupState.value = BackupState.Idle
    }

    data class BackupInfo(
        val name: String,
        val path: String,
        val size: Long,
        val date: Date
    ) {
        val formattedSize: String
            get() = when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                else -> "${size / 1024 / 1024} MB"
            }

        val formattedDate: String
            get() = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(date)
    }
}
