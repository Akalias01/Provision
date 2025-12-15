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

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    init {
        setupMediaController()
    }

    private fun setupMediaController() {
        val sessionToken = SessionToken(context, ComponentName(context, RezonPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
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
        }, MoreExecutors.directExecutor())
    }

    fun loadBook(book: Book) {
        val mediaItem = MediaItem.fromUri(book.filePath)
        mediaController?.setMediaItem(mediaItem)
        mediaController?.prepare()
    }

    fun play() { mediaController?.play() }
    fun pause() { mediaController?.pause() }
    fun seekTo(position: Long) { mediaController?.seekTo(position) }

    fun setSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
    }

    fun getCurrentPosition(): Long = mediaController?.currentPosition ?: 0L
}
