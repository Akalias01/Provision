package com.mossglen.lithos.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mossglen.lithos.data.GoogleSignInManager
import com.mossglen.lithos.data.LibraryRepository
import com.mossglen.lithos.data.TorrentManager
import com.mossglen.lithos.data.cloud.CloudFileInfo
import com.mossglen.lithos.data.cloud.CloudSource
import com.mossglen.lithos.data.cloud.DropboxManager
import com.mossglen.lithos.data.cloud.GoogleDriveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Sealed class representing events that require UI interaction (like launching sign-in)
 */
sealed class CloudBrowserEvent {
    data class LaunchGoogleSignIn(val intent: Intent) : CloudBrowserEvent()
}

data class CloudBrowserState(
    val isLoading: Boolean = false,
    val files: List<CloudFileInfo> = emptyList(),
    val currentPath: String = "",
    val pathHistory: List<String> = listOf(""),
    val selectedSource: CloudSource = CloudSource.GOOGLE_DRIVE,
    val isGoogleDriveConnected: Boolean = false,
    val isDropboxConnected: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val error: String? = null,
    val downloadProgress: Float = 0f,
    val downloadingFile: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class CloudFileBrowserViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val googleDriveManager: GoogleDriveManager,
    private val googleSignInManager: GoogleSignInManager,
    private val dropboxManager: DropboxManager,
    private val libraryRepository: LibraryRepository,
    private val torrentManager: TorrentManager
) : ViewModel() {

    companion object {
        private const val TAG = "CloudFileBrowserVM"
    }

    private val _state = MutableStateFlow(CloudBrowserState())
    val state: StateFlow<CloudBrowserState> = _state.asStateFlow()

    // Event flow for UI actions that need to be handled by the screen
    private val _event = MutableStateFlow<CloudBrowserEvent?>(null)
    val event: StateFlow<CloudBrowserEvent?> = _event.asStateFlow()

    init {
        autoConnectAndRefresh()
    }

    /**
     * Auto-connect to cloud services if user was previously signed in,
     * then refresh files automatically.
     */
    private fun autoConnectAndRefresh() {
        viewModelScope.launch {
            // First update state with current connection status
            _state.value = _state.value.copy(
                isGoogleDriveConnected = googleDriveManager.isConnected.value,
                isDropboxConnected = dropboxManager.isConnected.value
            )

            // Try to auto-reconnect Google Drive if user is signed in
            val isSignedIn = googleSignInManager.isSignedIn.value
            val hasDrivePermission = googleSignInManager.hasDrivePermission()

            if (isSignedIn && hasDrivePermission && !googleDriveManager.isConnected.value) {
                Log.d(TAG, "User is signed in, auto-connecting to Google Drive...")
                _state.value = _state.value.copy(isLoading = true)
                val success = googleDriveManager.initialize()
                _state.value = _state.value.copy(
                    isLoading = false,
                    isGoogleDriveConnected = success
                )
                if (success && _state.value.selectedSource == CloudSource.GOOGLE_DRIVE) {
                    loadFiles()
                }
            } else if (googleDriveManager.isConnected.value && _state.value.selectedSource == CloudSource.GOOGLE_DRIVE) {
                // Already connected, just refresh files
                loadFiles()
            }

            // Similarly for Dropbox
            if (dropboxManager.isConnected.value && _state.value.selectedSource == CloudSource.DROPBOX) {
                loadFiles()
            }
        }
    }

    fun selectSource(source: CloudSource) {
        _state.value = _state.value.copy(
            selectedSource = source,
            currentPath = if (source == CloudSource.GOOGLE_DRIVE) "" else "",
            pathHistory = listOf(""),
            files = emptyList(),
            searchQuery = "",
            isSearching = false
        )
        loadFiles()
    }

    fun connectGoogleDrive() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Check if user is already signed in with Google
            val isSignedIn = googleSignInManager.isSignedIn.value
            val hasDrivePermission = googleSignInManager.hasDrivePermission()

            if (isSignedIn && hasDrivePermission) {
                // User is signed in with proper permissions, initialize Drive
                initializeGoogleDrive()
            } else {
                // Need to sign in first - emit event to trigger sign-in UI
                Log.d(TAG, "User not signed in or missing Drive permissions, launching sign-in")
                _state.value = _state.value.copy(isLoading = false)
                val signInIntent = googleSignInManager.getSignInIntent()
                _event.value = CloudBrowserEvent.LaunchGoogleSignIn(signInIntent)
            }
        }
    }

    /**
     * Handle the result from Google Sign-In activity
     */
    fun handleGoogleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            Log.d(TAG, "Handling sign-in result, resultCode: ${result.resultCode}")

            val signInResult = googleSignInManager.handleSignInResult(result)

            signInResult.fold(
                onSuccess = { user ->
                    Log.d(TAG, "Google Sign-In successful for ${user.email}")
                    Log.d(TAG, "Account object is ${if (user.account != null) "present" else "NULL"}")

                    // Check if we have the account object needed for Drive
                    if (user.account?.account == null) {
                        Log.e(TAG, "Sign-in succeeded but account.account is null - OAuth may not be configured correctly")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Drive access not granted. Please try again."
                        )
                        // Sign out to force fresh sign-in next time
                        googleSignInManager.signOut()
                        return@launch
                    }

                    // Now initialize Google Drive
                    initializeGoogleDrive()
                },
                onFailure = { exception ->
                    Log.e(TAG, "Google Sign-In failed", exception)
                    val errorCode = if (exception is com.google.android.gms.common.api.ApiException) {
                        "Error code: ${exception.statusCode}"
                    } else {
                        ""
                    }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Sign-in failed: ${exception.message ?: "Unknown error"} $errorCode"
                    )
                }
            )
        }
    }

    /**
     * Initialize Google Drive after successful sign-in
     */
    private suspend fun initializeGoogleDrive() {
        val success = googleDriveManager.initialize()

        if (success) {
            _state.value = _state.value.copy(
                isLoading = false,
                isGoogleDriveConnected = true,
                error = null
            )
            if (_state.value.selectedSource == CloudSource.GOOGLE_DRIVE) {
                loadFiles()
            }
        } else {
            // Get specific error from Drive manager
            val driveState = googleDriveManager.syncState.value
            val errorMessage = when (driveState) {
                is GoogleDriveManager.SyncState.Error -> driveState.message
                else -> "Failed to connect to Google Drive"
            }

            // If it's an auth issue, sign out to force fresh login next time
            if (errorMessage.contains("sign in again", ignoreCase = true) ||
                errorMessage.contains("Authentication", ignoreCase = true)) {
                Log.d(TAG, "Auth issue detected, signing out to force fresh login")
                googleSignInManager.signOut()
            }

            _state.value = _state.value.copy(
                isLoading = false,
                isGoogleDriveConnected = false,
                error = errorMessage
            )
        }
    }

    /**
     * Clear the current event after it's been handled
     */
    fun clearEvent() {
        _event.value = null
    }

    fun connectDropbox() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val success = dropboxManager.initialize()
            _state.value = _state.value.copy(
                isLoading = false,
                isDropboxConnected = success,
                error = if (!success) "Failed to connect to Dropbox" else null
            )
            if (success && _state.value.selectedSource == CloudSource.DROPBOX) {
                loadFiles()
            }
        }
    }

    fun loadFiles(path: String? = null) {
        viewModelScope.launch {
            val targetPath = path ?: _state.value.currentPath
            _state.value = _state.value.copy(isLoading = true, error = null, isSearching = false, searchQuery = "")

            try {
                val files = when (_state.value.selectedSource) {
                    CloudSource.GOOGLE_DRIVE -> {
                        val folderId = targetPath.ifEmpty { null }
                        googleDriveManager.listFiles(folderId)
                    }
                    CloudSource.DROPBOX -> {
                        dropboxManager.listFiles(targetPath)
                    }
                }

                _state.value = _state.value.copy(
                    isLoading = false,
                    files = files,
                    currentPath = targetPath
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load files", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load files: ${e.message}"
                )
            }
        }
    }

    fun navigateToFolder(file: CloudFileInfo) {
        if (!file.isFolder) return

        val newPath = when (_state.value.selectedSource) {
            CloudSource.GOOGLE_DRIVE -> file.id
            CloudSource.DROPBOX -> file.path
        }

        val newHistory = _state.value.pathHistory + newPath
        _state.value = _state.value.copy(pathHistory = newHistory)
        loadFiles(newPath)
    }

    fun navigateBack(): Boolean {
        val history = _state.value.pathHistory
        if (history.size <= 1) return false

        val newHistory = history.dropLast(1)
        val previousPath = newHistory.last()
        _state.value = _state.value.copy(pathHistory = newHistory)
        loadFiles(previousPath)
        return true
    }

    fun search(query: String) {
        if (query.isBlank()) {
            loadFiles()
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, isSearching = true, searchQuery = query)

            try {
                val files = when (_state.value.selectedSource) {
                    CloudSource.GOOGLE_DRIVE -> googleDriveManager.searchFiles(query)
                    CloudSource.DROPBOX -> dropboxManager.searchFiles(query)
                }

                _state.value = _state.value.copy(
                    isLoading = false,
                    files = files
                )
            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Search failed: ${e.message}"
                )
            }
        }
    }

    fun downloadAndImport(file: CloudFileInfo) {
        if (file.isFolder) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                downloadingFile = file.name,
                downloadProgress = 0f,
                error = null
            )

            try {
                // Create download directory
                val downloadDir = File(context.getExternalFilesDir(null), "CloudDownloads")
                if (!downloadDir.exists()) downloadDir.mkdirs()

                val destinationFile = File(downloadDir, file.name)

                val success = when (_state.value.selectedSource) {
                    CloudSource.GOOGLE_DRIVE -> {
                        googleDriveManager.downloadFile(file.id, destinationFile) { progress ->
                            _state.value = _state.value.copy(downloadProgress = progress)
                        }
                    }
                    CloudSource.DROPBOX -> {
                        dropboxManager.downloadFile(file.path, destinationFile) { progress ->
                            _state.value = _state.value.copy(downloadProgress = progress)
                        }
                    }
                }

                if (success) {
                    // Handle the file based on type
                    when {
                        file.isTorrent -> {
                            // Add to torrent manager
                            val uri = Uri.fromFile(destinationFile)
                            torrentManager.startTorrentFileDownload(uri)
                            _state.value = _state.value.copy(
                                downloadingFile = null,
                                successMessage = "Torrent added: ${file.name}"
                            )
                        }
                        file.isAudioBook || file.isDocument -> {
                            // Import to library
                            val uri = Uri.fromFile(destinationFile)
                            libraryRepository.importBook(uri)
                            _state.value = _state.value.copy(
                                downloadingFile = null,
                                successMessage = "Imported to library: ${file.name}"
                            )
                        }
                        else -> {
                            _state.value = _state.value.copy(
                                downloadingFile = null,
                                successMessage = "Downloaded: ${file.name}"
                            )
                        }
                    }
                } else {
                    _state.value = _state.value.copy(
                        downloadingFile = null,
                        error = "Download failed"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Download and import failed", e)
                _state.value = _state.value.copy(
                    downloadingFile = null,
                    error = "Failed: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearSuccess() {
        _state.value = _state.value.copy(successMessage = null)
    }
}
