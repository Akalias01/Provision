package com.example.rezon.data

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.rezon.service.RezonPlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioServiceHandler @Inject constructor(
    private val context: Context
) {
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // State for UI to know if we are connected
    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected = _isServiceConnected.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    // Queue a book if loaded before service is ready
    private var pendingMediaItem: MediaItem? = null

    init {
        setupMediaController()
    }

    private fun setupMediaController() {
        val sessionToken = SessionToken(context, ComponentName(context, RezonPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                _isServiceConnected.value = true

                // Attach Listener
                mediaController?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                    override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                        _playbackSpeed.value = playbackParameters.speed
                    }
                    override fun onEvents(player: Player, events: Player.Events) {
                        _currentPosition.value = player.currentPosition
                        _duration.value = player.duration
                    }
                })

                // If a book was requested while we were connecting, load it now
                pendingMediaItem?.let {
                    mediaController?.setMediaItem(it)
                    mediaController?.prepare()
                    pendingMediaItem = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    fun loadBook(book: Book) {
        val item = MediaItem.fromUri(book.filePath)
        if (mediaController != null) {
            mediaController?.setMediaItem(item)
            mediaController?.prepare()
        } else {
            // Service not ready yet, queue it
            pendingMediaItem = item
        }
    }

    fun play() { mediaController?.play() }
    fun pause() { mediaController?.pause() }
    fun seekTo(position: Long) { mediaController?.seekTo(position) }

    fun setSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
    }

    fun getCurrentPosition(): Long = mediaController?.currentPosition ?: 0L
}
