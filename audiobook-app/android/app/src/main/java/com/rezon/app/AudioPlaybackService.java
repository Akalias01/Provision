package com.rezon.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Foreground service for audio playback with media notification controls.
 * Provides playback controls in the notification and lock screen.
 */
public class AudioPlaybackService extends Service {

    private static final String CHANNEL_ID = "rezon_playback_channel";
    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_PLAY = "com.rezon.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.rezon.ACTION_PAUSE";
    public static final String ACTION_PREV = "com.rezon.ACTION_PREV";
    public static final String ACTION_NEXT = "com.rezon.ACTION_NEXT";
    public static final String ACTION_REWIND = "com.rezon.ACTION_REWIND";
    public static final String ACTION_FORWARD = "com.rezon.ACTION_FORWARD";
    public static final String ACTION_STOP = "com.rezon.ACTION_STOP";

    private MediaSessionCompat mediaSession;
    private String currentTitle = "Rezon Audiobooks";
    private String currentAuthor = "Now Playing";
    private String currentCoverUrl = null;
    private Bitmap coverBitmap = null;
    private boolean isPlaying = false;
    private long currentPosition = 0;
    private long duration = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initMediaSession();
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "RezonAudiobookSession");

        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                broadcastAction(ACTION_PLAY);
            }

            @Override
            public void onPause() {
                broadcastAction(ACTION_PAUSE);
            }

            @Override
            public void onSkipToNext() {
                broadcastAction(ACTION_NEXT);
            }

            @Override
            public void onSkipToPrevious() {
                broadcastAction(ACTION_PREV);
            }

            @Override
            public void onFastForward() {
                broadcastAction(ACTION_FORWARD);
            }

            @Override
            public void onRewind() {
                broadcastAction(ACTION_REWIND);
            }

            @Override
            public void onSeekTo(long pos) {
                Intent intent = new Intent(AudioPlaybackService.this, MainActivity.class);
                intent.setAction("com.rezon.SEEK");
                intent.putExtra("position", pos);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            @Override
            public void onStop() {
                stopSelf();
            }
        });

        mediaSession.setActive(true);
    }

    private void broadcastAction(String action) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if (action != null) {
                switch (action) {
                    case ACTION_PLAY:
                    case ACTION_PAUSE:
                    case ACTION_PREV:
                    case ACTION_NEXT:
                    case ACTION_REWIND:
                    case ACTION_FORWARD:
                        broadcastAction(action);
                        return START_STICKY;
                    case ACTION_STOP:
                        stopSelf();
                        return START_NOT_STICKY;
                }
            }

            // Handle metadata update
            String title = intent.getStringExtra("title");
            String author = intent.getStringExtra("author");
            String coverUrl = intent.getStringExtra("coverUrl");
            boolean playing = intent.getBooleanExtra("isPlaying", true);
            long position = intent.getLongExtra("position", 0);
            long dur = intent.getLongExtra("duration", 0);

            if (title != null) currentTitle = title;
            if (author != null) currentAuthor = author;
            if (coverUrl != null && !coverUrl.equals(currentCoverUrl)) {
                currentCoverUrl = coverUrl;
                loadCoverArt(coverUrl);
            }
            isPlaying = playing;
            currentPosition = position;
            duration = dur;

            updateMediaSession();
        }

        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);

        return START_STICKY;
    }

    private void updateMediaSession() {
        // Update metadata
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentAuthor)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentTitle)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);

        if (coverBitmap != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, coverBitmap);
        }

        mediaSession.setMetadata(metadataBuilder.build());

        // Update playback state
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SEEK_TO |
                PlaybackStateCompat.ACTION_FAST_FORWARD |
                PlaybackStateCompat.ACTION_REWIND |
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                currentPosition,
                1.0f
            );

        mediaSession.setPlaybackState(stateBuilder.build());
    }

    private void loadCoverArt(String coverUrl) {
        if (coverUrl == null || coverUrl.isEmpty()) {
            coverBitmap = null;
            return;
        }

        // Load cover art in background
        new Thread(() -> {
            try {
                if (coverUrl.startsWith("data:")) {
                    // Base64 data URL
                    String base64 = coverUrl.substring(coverUrl.indexOf(",") + 1);
                    byte[] decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                    coverBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                } else if (coverUrl.startsWith("http")) {
                    // Remote URL
                    URL url = new URL(coverUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    coverBitmap = BitmapFactory.decodeStream(input);
                    input.close();
                }

                // Update notification with new cover
                if (coverBitmap != null) {
                    updateMediaSession();
                    NotificationManager manager = getSystemService(NotificationManager.class);
                    if (manager != null) {
                        manager.notify(NOTIFICATION_ID, createNotification());
                    }
                }
            } catch (Exception e) {
                coverBitmap = null;
            }
        }).start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Audiobook Playback",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Controls for audiobook playback");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        // Intent to open app
        Intent contentIntent = new Intent(this, MainActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE
        );

        // Media control intents
        PendingIntent prevIntent = PendingIntent.getService(
            this, 0, new Intent(this, AudioPlaybackService.class).setAction(ACTION_PREV),
            PendingIntent.FLAG_IMMUTABLE
        );
        PendingIntent rewindIntent = PendingIntent.getService(
            this, 1, new Intent(this, AudioPlaybackService.class).setAction(ACTION_REWIND),
            PendingIntent.FLAG_IMMUTABLE
        );
        PendingIntent playPauseIntent = PendingIntent.getService(
            this, 2, new Intent(this, AudioPlaybackService.class).setAction(isPlaying ? ACTION_PAUSE : ACTION_PLAY),
            PendingIntent.FLAG_IMMUTABLE
        );
        PendingIntent forwardIntent = PendingIntent.getService(
            this, 3, new Intent(this, AudioPlaybackService.class).setAction(ACTION_FORWARD),
            PendingIntent.FLAG_IMMUTABLE
        );
        PendingIntent nextIntent = PendingIntent.getService(
            this, 4, new Intent(this, AudioPlaybackService.class).setAction(ACTION_NEXT),
            PendingIntent.FLAG_IMMUTABLE
        );
        PendingIntent stopIntent = PendingIntent.getService(
            this, 5, new Intent(this, AudioPlaybackService.class).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText(currentAuthor)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(1, 2, 3)) // Show rewind, play/pause, forward in compact view
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
            .addAction(android.R.drawable.ic_media_rew, "Rewind", rewindIntent)
            .addAction(
                isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                isPlaying ? "Pause" : "Play",
                playPauseIntent
            )
            .addAction(android.R.drawable.ic_media_ff, "Forward", forwardIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent);

        if (coverBitmap != null) {
            builder.setLargeIcon(coverBitmap);
        }

        return builder.build();
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        super.onDestroy();
    }
}
