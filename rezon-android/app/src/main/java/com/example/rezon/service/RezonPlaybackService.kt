package com.example.rezon.service

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RezonPlaybackService : MediaLibraryService() {

    @Inject
    lateinit var player: ExoPlayer

    private var mediaLibrarySession: MediaLibrarySession? = null

    companion object {
        const val CUSTOM_COMMAND_SKIP_30 = "rezon.action.SKIP_30"
    }

    override fun onCreate() {
        super.onCreate()

        val skip30Command = SessionCommand(CUSTOM_COMMAND_SKIP_30, Bundle.EMPTY)

        val callback = object : MediaLibrarySession.Callback {
            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                // Return the root folder for Android Auto
                val rootItem = MediaItem.Builder()
                    .setMediaId("root")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setTitle("Audiobooks")
                            .build()
                    )
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                // In a real app, query your Room DB here.
                // For now, we return a dummy list so Auto shows something.
                if (parentId == "root") {
                    val item1 = MediaItem.Builder()
                        .setMediaId("asset:///demo_audio.mp3")
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setIsBrowsable(false)
                                .setIsPlayable(true)
                                .setTitle("The Martian")
                                .setArtist("Andy Weir")
                                .build()
                        ).build()

                    return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(item1), params))
                }
                return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))
            }

            // Allow connections from Auto
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                    .buildUpon()
                    .add(skip30Command)
                    .build()
                return MediaSession.ConnectionResult.accept(
                    sessionCommands,
                    MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                )
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                if (customCommand.customAction == CUSTOM_COMMAND_SKIP_30) {
                    player.seekTo(player.currentPosition + 30_000)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }
        }

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, callback).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }
}
