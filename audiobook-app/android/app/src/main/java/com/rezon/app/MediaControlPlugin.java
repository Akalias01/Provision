package com.rezon.app;

import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Capacitor plugin for controlling media playback and Android Auto integration.
 */
@CapacitorPlugin(name = "MediaControl")
public class MediaControlPlugin extends Plugin {

    private static final String TAG = "MediaControlPlugin";

    /**
     * Update the currently playing media metadata
     */
    @PluginMethod
    public void updateMetadata(PluginCall call) {
        try {
            String title = call.getString("title", "Unknown Title");
            String author = call.getString("author", "Unknown Author");
            String coverUrl = call.getString("coverUrl", "");
            Long durationObj = call.getLong("duration");
            long duration = durationObj != null ? durationObj : 0L;

            Log.d(TAG, "updateMetadata: " + title + " by " + author);

            // Start foreground service for background playback
            Intent serviceIntent = new Intent(getContext(), AudioPlaybackService.class);
            serviceIntent.putExtra("title", title);
            serviceIntent.putExtra("author", author);
            serviceIntent.putExtra("coverUrl", coverUrl);
            serviceIntent.putExtra("duration", duration);
            serviceIntent.putExtra("isPlaying", true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getContext().startForegroundService(serviceIntent);
            } else {
                getContext().startService(serviceIntent);
            }

            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "updateMetadata error: " + e.getMessage());
            JSObject ret = new JSObject();
            ret.put("success", false);
            ret.put("error", e.getMessage());
            call.resolve(ret);
        }
    }

    /**
     * Update playback state
     */
    @PluginMethod
    public void updatePlaybackState(PluginCall call) {
        try {
            Boolean isPlayingObj = call.getBoolean("isPlaying");
            boolean isPlaying = isPlayingObj != null ? isPlayingObj : false;
            Long positionObj = call.getLong("position");
            long position = positionObj != null ? positionObj : 0L;
            Float speedObj = call.getFloat("speed");
            float speed = speedObj != null ? speedObj : 1.0f;

            Log.d(TAG, "updatePlaybackState: playing=" + isPlaying + " position=" + position);

            // Only manage service if there's a meaningful state change
            if (!isPlaying) {
                // Don't immediately stop service, just update state
                try {
                    Intent serviceIntent = new Intent(getContext(), AudioPlaybackService.class);
                    serviceIntent.putExtra("isPlaying", false);
                    serviceIntent.putExtra("position", position);
                    getContext().startService(serviceIntent);
                } catch (Exception ignored) {
                    // Service might not be running, that's ok
                }
            }

            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "updatePlaybackState error: " + e.getMessage());
            JSObject ret = new JSObject();
            ret.put("success", false);
            ret.put("error", e.getMessage());
            call.resolve(ret);
        }
    }

    /**
     * Stop the playback service
     */
    @PluginMethod
    public void stopService(PluginCall call) {
        try {
            Intent serviceIntent = new Intent(getContext(), AudioPlaybackService.class);
            getContext().stopService(serviceIntent);

            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "stopService error: " + e.getMessage());
            JSObject ret = new JSObject();
            ret.put("success", false);
            call.resolve(ret);
        }
    }
}
