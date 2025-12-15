package com.example.rezon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rezon.ui.MainLayout
import com.example.rezon.ui.screens.SplashScreen
import com.example.rezon.ui.theme.RezonBlack
import com.example.rezon.ui.theme.RezonSurface
import com.example.rezon.ui.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val currentThemeOption by themeViewModel.currentTheme

            // Apply the selected theme to the MaterialTheme
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = currentThemeOption.primary,
                    secondary = Color.Gray,
                    background = RezonBlack,
                    surface = RezonSurface,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onAnimationFinished = { showSplash = false })
                } else {
                    MainLayout(themeViewModel = themeViewModel)
                }
            }
        }
    }
}
