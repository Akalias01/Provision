package com.rezon.app;

import android.content.Intent;
import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Register custom plugins
        registerPlugin(MediaControlPlugin.class);
        registerPlugin(FilePickerPlugin.class);

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Handle media control intents from notification/Android Auto
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();

            // Forward the action to the WebView via JavaScript
            String jsCode = String.format(
                "window.dispatchEvent(new CustomEvent('mediaControl', { detail: { action: '%s' } }));",
                action
            );

            if (getBridge() != null && getBridge().getWebView() != null) {
                getBridge().getWebView().evaluateJavascript(jsCode, null);
            }
        }
    }
}
