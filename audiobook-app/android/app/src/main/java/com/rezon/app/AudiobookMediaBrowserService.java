package com.rezon.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * MediaBrowserService for Android Auto compatibility.
 * Provides audiobook browsing and playback controls for car displays.
 */
public class AudiobookMediaBrowserService extends MediaBrowserServiceCompat {

    private static final String TAG = "RezonMediaService";
    private static final String ROOT_ID = "root";
    private static final String AUDIOBOOKS_ID = "audiobooks";
    private static final String RECENT_ID = "recent";

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create MediaSession
        mediaSession = new MediaSessionCompat(this, TAG);
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );

        // Set initial playback state
        stateBuilder = new PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SEEK_TO |
                PlaybackStateCompat.ACTION_FAST_FORWARD |
                PlaybackStateCompat.ACTION_REWIND
            );

        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setCallback(new MediaSessionCallback());
        mediaSession.setActive(true);

        setSessionToken(mediaSession.getSessionToken());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSession != null) {
            mediaSession.release();
        }
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        // Allow all clients to browse
        return new BrowserRoot(ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        if (ROOT_ID.equals(parentId)) {
            // Root menu - show categories
            mediaItems.add(createBrowsableMediaItem(AUDIOBOOKS_ID, "Audiobooks", "Browse your audiobook library"));
            mediaItems.add(createBrowsableMediaItem(RECENT_ID, "Recently Played", "Continue where you left off"));
        } else if (AUDIOBOOKS_ID.equals(parentId)) {
            // Audiobooks will be populated from the web app via JavaScript bridge
            // For now, show a placeholder
            mediaItems.add(createPlayableMediaItem("no_books", "Open Rezon App", "Add audiobooks from the app"));
        } else if (RECENT_ID.equals(parentId)) {
            // Recent items will be populated from the web app
            mediaItems.add(createPlayableMediaItem("no_recent", "No Recent Books", "Start listening to see recent books"));
        }

        result.sendResult(mediaItems);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItem(String id, String title, String subtitle) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
            .setMediaId(id)
            .setTitle(title)
            .setSubtitle(subtitle)
            .build();

        return new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createPlayableMediaItem(String id, String title, String subtitle) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
            .setMediaId(id)
            .setTitle(title)
            .setSubtitle(subtitle)
            .build();

        return new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    /**
     * Media session callbacks for playback control
     */
    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            // Forward to web app via Capacitor bridge
            Intent intent = new Intent(AudiobookMediaBrowserService.this, MainActivity.class);
            intent.setAction("com.rezon.PLAY");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        @Override
        public void onPause() {
            Intent intent = new Intent(AudiobookMediaBrowserService.this, MainActivity.class);
            intent.setAction("com.rezon.PAUSE");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        @Override
        public void onSkipToNext() {
            Intent intent = new Intent(AudiobookMediaBrowserService.this, MainActivity.class);
            intent.setAction("com.rezon.SKIP_NEXT");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        @Override
        public void onSkipToPrevious() {
            Intent intent = new Intent(AudiobookMediaBrowserService.this, MainActivity.class);
            intent.setAction("com.rezon.SKIP_PREV");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        @Override
        public void onSeekTo(long pos) {
            Intent intent = new Intent(AudiobookMediaBrowserService.this, MainActivity.class);
            intent.setAction("com.rezon.SEEK");
            intent.putExtra("position", pos);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        @Override
        public void onFastForward() {
            Intent intent = new Intent(AudiobookMediaBrowserService.this, MainActivity.class);
            intent.setAction("com.rezon.FORWARD");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        @Override
        public void onRewind() {
            Intent intent = new Intent(AudiobookMediaBrowserService.this, MainActivity.class);
            intent.setAction("com.rezon.REWIND");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Intent intent = new Intent(AudiobookMediaBrowserService.this, MainActivity.class);
            intent.setAction("com.rezon.PLAY_MEDIA");
            intent.putExtra("mediaId", mediaId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    /**
     * Update the current playing metadata - called from Capacitor plugin
     */
    public void updateMetadata(String title, String author, String coverUrl, long duration) {
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, author)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);

        if (coverUrl != null && !coverUrl.isEmpty()) {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, coverUrl);
        }

        mediaSession.setMetadata(metadataBuilder.build());
    }

    /**
     * Update playback state - called from Capacitor plugin
     */
    public void updatePlaybackState(boolean isPlaying, long position, float speed) {
        int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;

        mediaSession.setPlaybackState(
            stateBuilder.setState(state, position, speed)
                .build()
        );
    }
}
