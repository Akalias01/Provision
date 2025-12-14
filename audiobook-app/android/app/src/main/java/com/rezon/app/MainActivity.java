package com.rezon.app;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Register custom plugins
        registerPlugin(MediaControlPlugin.class);

        super.onCreate(savedInstanceState);
    }
}
