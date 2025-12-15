package com.rezon.app.service

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Playback state data class
 */
data class PlaybackUiState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isBuffering: Boolean = false,
    val currentMediaId: String? = null
)

/**
 * REZON Playback Controller
 *
 * Singleton controller that manages communication with PlaybackService via MediaController.
 * Provides reactive state flows for UI updates and methods for playback control.
 */
@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /**
     * Connect to the PlaybackService
     */
    fun connect() {
        if (controllerFuture != null) return

        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            _isConnected.value = true
            setupPlayerListener()
        }, MoreExecutors.directExecutor())
    }

    /**
     * Disconnect from the PlaybackService
     */
    fun disconnect() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controllerFuture = null
        mediaController = null
        _isConnected.value = false
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _playbackState.value = _playbackState.value.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING
                )
                updatePlaybackState()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                updatePlaybackState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _playbackState.value = _playbackState.value.copy(
                    currentMediaId = mediaItem?.mediaId
                )
                updatePlaybackState()
            }

            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                _playbackState.value = _playbackState.value.copy(
                    playbackSpeed = playbackParameters.speed
                )
            }
        })
    }

    private fun updatePlaybackState() {
        mediaController?.let { controller ->
            _playbackState.value = _playbackState.value.copy(
                isPlaying = controller.isPlaying,
                currentPosition = controller.currentPosition.coerceAtLeast(0),
                duration = controller.duration.coerceAtLeast(0),
                currentMediaId = controller.currentMediaItem?.mediaId
            )
        }
    }

    /**
     * Get current playback position (for real-time updates)
     */
    fun getCurrentPosition(): Long {
        return mediaController?.currentPosition ?: 0L
    }

    /**
     * Play an audiobook file
     */
    fun playBook(
        bookId: String,
        filePath: String,
        title: String,
        author: String,
        coverUri: Uri? = null,
        startPosition: Long = 0L
    ) {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(author)
            .setArtworkUri(coverUri)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(bookId)
            .setUri(filePath)
            .setMediaMetadata(mediaMetadata)
            .build()

        mediaController?.apply {
            setMediaItem(mediaItem)
            seekTo(startPosition)
            prepare()
            play()
        }
    }

    /**
     * Play/Pause toggle
     */
    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    /**
     * Play
     */
    fun play() {
        mediaController?.play()
    }

    /**
     * Pause
     */
    fun pause() {
        mediaController?.pause()
    }

    /**
     * Seek to specific position
     */
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs.coerceAtLeast(0))
    }

    /**
     * Skip backward by specified milliseconds
     */
    fun skipBackward(ms: Long = 10_000L) {
        mediaController?.let { controller ->
            val newPosition = (controller.currentPosition - ms).coerceAtLeast(0)
            controller.seekTo(newPosition)
        }
    }

    /**
     * Skip forward by specified milliseconds
     */
    fun skipForward(ms: Long = 30_000L) {
        mediaController?.let { controller ->
            val newPosition = (controller.currentPosition + ms).coerceAtMost(controller.duration)
            controller.seekTo(newPosition)
        }
    }

    /**
     * Set playback speed
     */
    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed.coerceIn(0.5f, 3.0f))
    }

    /**
     * Set volume (0.0 to 1.0)
     */
    fun setVolume(volume: Float) {
        mediaController?.volume = volume.coerceIn(0f, 1f)
    }

    /**
     * Stop playback and release resources
     */
    fun stop() {
        mediaController?.stop()
    }
}
