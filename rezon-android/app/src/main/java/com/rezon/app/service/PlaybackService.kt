package com.rezon.app.service

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.rezon.app.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * REZON Playback Service
 *
 * Media3-based audio playback service for audiobook playback.
 * Features:
 * - Background playback with foreground notification
 * - MediaSession for system integration (lock screen, car mode, etc.)
 * - Audio focus handling
 * - Playback speed control
 * - Sleep timer support
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Configure audio attributes for audiobook playback
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        // Initialize ExoPlayer
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // Handle audio focus
            .setHandleAudioBecomingNoisy(true) // Pause when headphones disconnected
            .setWakeMode(C.WAKE_MODE_LOCAL) // Keep CPU awake during playback
            .build()
            .apply {
                // Add listener for playback events
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_ENDED -> {
                                // Handle book completion
                            }
                            Player.STATE_BUFFERING -> {
                                // Show loading indicator
                            }
                            Player.STATE_READY -> {
                                // Ready to play
                            }
                            Player.STATE_IDLE -> {
                                // Player is idle
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        // Update notification and UI
                    }
                })
            }

        // Create pending intent for notification tap
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create MediaSession
        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player?.playWhenReady == false || player?.mediaItemCount == 0) {
            // Stop the service if playback is paused or no items
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.rezon.app.action.PLAY"
        const val ACTION_PAUSE = "com.rezon.app.action.PAUSE"
        const val ACTION_SKIP_FORWARD = "com.rezon.app.action.SKIP_FORWARD"
        const val ACTION_SKIP_BACKWARD = "com.rezon.app.action.SKIP_BACKWARD"
        const val ACTION_SEEK_TO = "com.rezon.app.action.SEEK_TO"
        const val EXTRA_POSITION = "extra_position"
    }
}
