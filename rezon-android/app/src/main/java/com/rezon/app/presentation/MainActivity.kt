package com.rezon.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.rezon.app.presentation.ui.MainLayout
import com.rezon.app.presentation.ui.theme.RezonTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * REZON Main Activity
 *
 * Single Activity architecture with Jetpack Compose.
 * Features:
 * - Edge-to-edge display
 * - Native Android splash screen
 * - Dark theme by default
 * - Sirin-style navigation drawer
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Track splash screen visibility
    private var keepSplashScreen by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Keep splash screen visible while app initializes
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        setContent {
            RezonTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainLayout()
                }
            }

            // Hide splash screen after first composition
            keepSplashScreen = false
        }
    }
}
