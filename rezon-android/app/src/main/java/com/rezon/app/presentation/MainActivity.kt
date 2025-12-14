package com.rezon.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.rezon.app.presentation.navigation.RezonNavHost
import com.rezon.app.presentation.ui.theme.RezonTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * REZON Main Activity
 *
 * Single Activity architecture with Jetpack Compose navigation.
 * Handles edge-to-edge display and splash screen.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Keep splash screen visible while loading
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        setContent {
            RezonTheme(darkTheme = true) { // Default to dark theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RezonNavHost(
                        onReady = {
                            // Hide splash screen when navigation is ready
                            keepSplashScreen = false
                        }
                    )
                }
            }
        }
    }
}
