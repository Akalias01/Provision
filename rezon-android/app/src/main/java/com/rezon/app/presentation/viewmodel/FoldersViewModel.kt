package com.rezon.app.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rezon.app.presentation.ui.screens.folders.ScanFolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Folders UI State
 */
data class FoldersUiState(
    val folders: List<ScanFolder> = emptyList(),
    val scanningFolderId: String? = null,
    val isLoading: Boolean = false
)

/**
 * REZON Folders ViewModel
 *
 * Manages scan folders:
 * - Add/remove folders
 * - Scan folders for audiobooks
 * - Track scan progress
 */
@HiltViewModel
class FoldersViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoldersUiState())
    val uiState: StateFlow<FoldersUiState> = _uiState.asStateFlow()

    // Supported audio formats
    private val audioExtensions = setOf(
        "mp3", "m4a", "m4b", "aac", "ogg", "opus", "flac", "wav", "wma"
    )

    init {
        loadSavedFolders()
    }

    private fun loadSavedFolders() {
        // Load folders from SharedPreferences or database
        // For now, using in-memory storage
        viewModelScope.launch {
            val persistedUris = context.contentResolver.persistedUriPermissions
            val folders = persistedUris.mapNotNull { permission ->
                try {
                    val documentFile = DocumentFile.fromTreeUri(context, permission.uri)
                    if (documentFile != null && documentFile.exists()) {
                        ScanFolder(
                            id = UUID.randomUUID().toString(),
                            uri = permission.uri,
                            displayName = documentFile.name ?: "Unknown folder",
                            bookCount = 0
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            _uiState.update { it.copy(folders = folders) }
        }
    }

    /**
     * Add a new folder to scan
     */
    fun addFolder(uri: Uri) {
        val documentFile = DocumentFile.fromTreeUri(context, uri) ?: return

        val newFolder = ScanFolder(
            id = UUID.randomUUID().toString(),
            uri = uri,
            displayName = documentFile.name ?: "Unknown folder",
            bookCount = 0
        )

        _uiState.update { state ->
            // Check if folder already exists
            if (state.folders.any { it.uri == uri }) {
                state
            } else {
                state.copy(folders = state.folders + newFolder)
            }
        }

        // Scan the newly added folder
        scanFolder(newFolder.id)
    }

    /**
     * Remove a folder from the list
     */
    fun removeFolder(folderId: String) {
        val folder = _uiState.value.folders.find { it.id == folderId } ?: return

        // Release persistent permission
        try {
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.releasePersistableUriPermission(folder.uri, takeFlags)
        } catch (e: Exception) {
            // Permission might not exist
        }

        _uiState.update { state ->
            state.copy(folders = state.folders.filter { it.id != folderId })
        }
    }

    /**
     * Scan a specific folder for audiobooks
     */
    fun scanFolder(folderId: String) {
        val folder = _uiState.value.folders.find { it.id == folderId } ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(scanningFolderId = folderId) }

            try {
                val bookCount = countAudiobooksInFolder(folder.uri)

                _uiState.update { state ->
                    state.copy(
                        folders = state.folders.map {
                            if (it.id == folderId) {
                                it.copy(
                                    bookCount = bookCount,
                                    lastScanned = System.currentTimeMillis()
                                )
                            } else it
                        },
                        scanningFolderId = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(scanningFolderId = null) }
            }
        }
    }

    /**
     * Scan all folders
     */
    fun scanAllFolders() {
        viewModelScope.launch {
            _uiState.value.folders.forEach { folder ->
                scanFolder(folder.id)
                delay(500) // Small delay between scans
            }
        }
    }

    /**
     * Count audiobooks in a folder
     */
    private fun countAudiobooksInFolder(uri: Uri): Int {
        val documentFile = DocumentFile.fromTreeUri(context, uri) ?: return 0
        return countAudioFilesRecursive(documentFile)
    }

    private fun countAudioFilesRecursive(folder: DocumentFile): Int {
        var count = 0

        folder.listFiles().forEach { file ->
            when {
                file.isDirectory -> {
                    // Count subfolders as potential audiobooks if they contain audio
                    val audioInSubfolder = countAudioFilesRecursive(file)
                    if (audioInSubfolder > 0) {
                        count++ // Count the folder as one audiobook
                    }
                }
                file.isFile -> {
                    val extension = file.name?.substringAfterLast(".", "")?.lowercase()
                    if (extension in audioExtensions) {
                        count++
                    }
                }
            }
        }

        return count
    }
}
