package com.mossglen.lithos

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.mossglen.lithos.service.LithosPlaybackService
import com.mossglen.lithos.ui.MainLayoutGlass
import com.mossglen.lithos.ui.screens.SplashScreen
import com.mossglen.lithos.ui.viewmodel.ThemeViewModel
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable true edge-to-edge display with fully transparent bars
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Start the MediaLibraryService for Android Auto and media controls
        startMediaService()

        // Extract incoming intent data (magnet link, .torrent file, or document)
        val incomingData = handleIntent(intent)
        val incomingFile = handleDocumentIntent(intent)

        // Skip splash if resuming from notification or if app was already running
        val skipSplash = savedInstanceState != null || isResumeFromNotification(intent)

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val appTheme by themeViewModel.appTheme

            // Determine if dark mode based on ThemeMode setting and system preference
            val isSystemDark = isSystemInDarkTheme()
            val isDark = themeViewModel.shouldUseDarkMode(isSystemDark)

            var showSplash by remember { mutableStateOf(!skipSplash) }

            // Track incoming data to pass to MainLayout after splash
            var pendingTorrentData by remember { mutableStateOf(incomingData) }
            var pendingFileData by remember { mutableStateOf(incomingFile) }

            val colorScheme = if (isDark) {
                darkColorScheme(
                    primary = appTheme.accentColors[0],
                    background = Color(0xFF050505),
                    surface = Color(0xFF121212),
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            } else {
                lightColorScheme(
                    primary = appTheme.accentColors[0],
                    background = Color(0xFFF5F5F5),
                    surface = Color.White,
                    onBackground = Color.Black,
                    onSurface = Color.Black
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
                if (showSplash) {
                    SplashScreen { showSplash = false }
                } else {
                    MainLayoutGlass(
                        themeViewModel = themeViewModel,
                        isDark = isDark,
                        incomingTorrentData = pendingTorrentData,
                        onTorrentDataConsumed = { pendingTorrentData = null },
                        incomingFileData = pendingFileData,
                        onFileDataConsumed = { pendingFileData = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle new intent when app is already running
        setIntent(intent)
    }

    private fun handleIntent(intent: Intent?): IncomingTorrentData? {
        if (intent == null) return null

        return when {
            // Handle magnet: scheme
            intent.action == Intent.ACTION_VIEW && intent.data?.scheme == "magnet" -> {
                IncomingTorrentData.MagnetLink(intent.data.toString())
            }
            // Handle .torrent file
            intent.action == Intent.ACTION_VIEW && intent.data != null -> {
                val mimeType = intent.type ?: contentResolver.getType(intent.data!!)
                if (mimeType == "application/x-bittorrent") {
                    IncomingTorrentData.TorrentFile(intent.data!!)
                } else null
            }
            else -> null
        }
    }

    /**
     * Handle incoming document files (EPUB, PDF, DOCX, DOC, TXT)
     */
    private fun handleDocumentIntent(intent: Intent?): IncomingFileData? {
        if (intent == null || intent.action != Intent.ACTION_VIEW) return null

        val uri = intent.data ?: return null
        val mimeType = intent.type ?: contentResolver.getType(uri) ?: return null

        // Check if it's a supported document type
        val supportedMimeTypes = listOf(
            "application/epub+zip",
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "text/plain"
        )

        // Also check file extension for EPUB files that might have wrong mime type
        val isEpubByExtension = uri.toString().lowercase().endsWith(".epub")

        return if (mimeType in supportedMimeTypes || isEpubByExtension) {
            val effectiveMimeType = if (isEpubByExtension && mimeType !in supportedMimeTypes) {
                "application/epub+zip"
            } else {
                mimeType
            }
            IncomingFileData.DocumentFile(uri, effectiveMimeType)
        } else null
    }

    /**
     * Check if app is being resumed from media notification click
     */
    private fun isResumeFromNotification(intent: Intent?): Boolean {
        if (intent == null) return false
        // Check if launched from notification (has FLAG_ACTIVITY_BROUGHT_TO_FRONT)
        // or if it's a resume action (not a fresh launch from launcher)
        val isFromNotification = intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0
        val isNotFromLauncher = intent.action != Intent.ACTION_MAIN ||
                !intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        return isFromNotification || (intent.action == Intent.ACTION_MAIN && isNotFromLauncher)
    }

    /**
     * Start the MediaLibraryService and connect a MediaController.
     * This is required for Android Auto to detect the app.
     */
    private fun startMediaService() {
        val sessionToken = SessionToken(this, ComponentName(this, LithosPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    override fun onDestroy() {
        mediaController?.release()
        mediaController = null
        super.onDestroy()
    }
}

sealed class IncomingTorrentData {
    data class MagnetLink(val uri: String) : IncomingTorrentData()
    data class TorrentFile(val uri: Uri) : IncomingTorrentData()
}

/**
 * Incoming file data for EPUB, PDF, DOCX, DOC, TXT files
 */
sealed class IncomingFileData {
    data class DocumentFile(val uri: Uri, val mimeType: String) : IncomingFileData()
}
