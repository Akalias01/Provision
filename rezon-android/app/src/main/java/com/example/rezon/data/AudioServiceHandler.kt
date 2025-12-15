package com.example.rezon.data

import android.content.ComponentName
import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
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

    // Audio Effects
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    // State
    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected = _isServiceConnected.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    // EQ State (Exposed to UI)
    private val _eqEnabled = MutableStateFlow(false)
    val eqEnabled = _eqEnabled.asStateFlow()

    // 5 Bands: 60Hz, 230Hz, 910Hz, 3kHz, 14kHz (Approximations based on device)
    private val _bandLevels = MutableStateFlow(listOf(0, 0, 0, 0, 0)) // Values -15 to +15
    val bandLevels = _bandLevels.asStateFlow()

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

                // Listen for Session ID to attach EQ
                mediaController?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                    override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                        _playbackSpeed.value = playbackParameters.speed
                    }
                    override fun onEvents(player: Player, events: Player.Events) {
                        _currentPosition.value = player.currentPosition
                    }
                    // Crucial: We need the Audio Session ID to attach effects
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        if (audioSessionId != 0) {
                            initAudioEffects(audioSessionId)
                        }
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

    private fun initAudioEffects(sessionId: Int) {
        try {
            equalizer = Equalizer(0, sessionId)
            loudnessEnhancer = LoudnessEnhancer(sessionId)
            equalizer?.enabled = _eqEnabled.value
            loudnessEnhancer?.enabled = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        _eqEnabled.value = enabled
        equalizer?.enabled = enabled
    }

    fun setBandLevel(index: Int, level: Int) {
        // level is slider value (-15 to 15), converted to millibels for Android (-1500 to 1500)
        // Note: Android EQ bands might not match exactly 5, this is a simplified mapping
        equalizer?.let { eq ->
            if (index < eq.numberOfBands) {
                val range = eq.bandLevelRange // usually [-1500, 1500]
                val safeLevel = (level * 100).coerceIn(range[0].toInt(), range[1].toInt())
                eq.setBandLevel(index.toShort(), safeLevel.toShort())

                val currentList = _bandLevels.value.toMutableList()
                currentList[index] = level
                _bandLevels.value = currentList
            }
        }
    }

    // Amplifier (Simulated using LoudnessEnhancer)
    fun setAmplifierLevel(gain: Int) {
        // Gain 0 to 1000 (mB)
        loudnessEnhancer?.setTargetGain(gain)
    }

    fun loadBook(book: Book) {
        val item = MediaItem.fromUri(book.filePath)
        if (mediaController != null) {
            mediaController?.setMediaItem(item)
            mediaController?.prepare()
        } else {
            pendingMediaItem = item
        }
    }

    fun play() { mediaController?.play() }
    fun pause() { mediaController?.pause() }
    fun seekTo(position: Long) { mediaController?.seekTo(position) }
    fun setSpeed(speed: Float) { mediaController?.setPlaybackSpeed(speed) }
    fun getCurrentPosition(): Long = mediaController?.currentPosition ?: 0L
    fun getDuration(): Long = mediaController?.duration ?: 0L
}
