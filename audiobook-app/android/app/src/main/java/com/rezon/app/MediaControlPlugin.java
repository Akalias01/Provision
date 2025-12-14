package com.rezon.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

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

    private AudiobookMediaBrowserService mediaBrowserService;
    private boolean isServiceBound = false;

    @Override
    public void load() {
        // Bind to the MediaBrowserService
        Intent intent = new Intent(getContext(), AudiobookMediaBrowserService.class);
        getContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Service is bound
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            mediaBrowserService = null;
        }
    };

    /**
     * Update the currently playing media metadata
     */
    @PluginMethod
    public void updateMetadata(PluginCall call) {
        String title = call.getString("title", "Unknown Title");
        String author = call.getString("author", "Unknown Author");
        String coverUrl = call.getString("coverUrl", "");
        long duration = call.getLong("duration", 0L);

        // Start foreground service for background playback
        Intent serviceIntent = new Intent(getContext(), AudioPlaybackService.class);
        serviceIntent.putExtra("title", title);
        serviceIntent.putExtra("author", author);
        getContext().startForegroundService(serviceIntent);

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }

    /**
     * Update playback state
     */
    @PluginMethod
    public void updatePlaybackState(PluginCall call) {
        boolean isPlaying = call.getBoolean("isPlaying", false);
        long position = call.getLong("position", 0L);
        float speed = call.getFloat("speed", 1.0f);

        if (!isPlaying) {
            // Stop foreground service when paused
            Intent serviceIntent = new Intent(getContext(), AudioPlaybackService.class);
            getContext().stopService(serviceIntent);
        }

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }

    /**
     * Stop the playback service
     */
    @PluginMethod
    public void stopService(PluginCall call) {
        Intent serviceIntent = new Intent(getContext(), AudioPlaybackService.class);
        getContext().stopService(serviceIntent);

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }

    @Override
    protected void handleOnDestroy() {
        if (isServiceBound) {
            getContext().unbindService(serviceConnection);
            isServiceBound = false;
        }
        super.handleOnDestroy();
    }
}
