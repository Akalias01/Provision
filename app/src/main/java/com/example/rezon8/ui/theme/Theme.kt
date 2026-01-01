package com.mossglen.lithos.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * LITHOS AMBER Color Schemes
 *
 * These provide Material 3 compatibility while maintaining the Lithos design language.
 * For the full Lithos design system, use LithosTheme directly.
 */

private val LithosDarkColorScheme = darkColorScheme(
    primary = LithosAmber,
    onPrimary = LithosTextPrimary,
    primaryContainer = LithosAmberDark,
    onPrimaryContainer = LithosTextPrimary,
    secondary = LithosMoss,
    onSecondary = LithosTextPrimary,
    secondaryContainer = LithosMossDark,
    onSecondaryContainer = LithosTextPrimary,
    tertiary = LithosAmberLight,
    background = LithosSlate,
    onBackground = LithosTextPrimary,
    surface = LithosSurfaceDark,
    onSurface = LithosTextPrimary,
    surfaceVariant = LithosSurfaceDarkElevated,
    onSurfaceVariant = LithosTextSecondary,
    error = LithosError,
    onError = LithosTextPrimary
)

private val LithosLightColorScheme = lightColorScheme(
    primary = LithosAmberDark,
    onPrimary = LithosOat,
    primaryContainer = LithosAmber,
    onPrimaryContainer = LithosTextPrimaryLight,
    secondary = LithosMossDark,
    onSecondary = LithosOat,
    secondaryContainer = LithosMoss,
    onSecondaryContainer = LithosTextPrimaryLight,
    tertiary = LithosAmber,
    background = LithosOat,
    onBackground = LithosTextPrimaryLight,
    surface = LithosSurfaceLight,
    onSurface = LithosTextPrimaryLight,
    surfaceVariant = LithosSurfaceLightElevated,
    onSurfaceVariant = LithosTextSecondaryLight,
    error = LithosError,
    onError = LithosOat
)

/**
 * REZON8 Theme - Main app theme using Lithos Amber design system
 *
 * This is a wrapper around LithosTheme for compatibility with existing code
 * that expects a "Rezon8Theme" composable.
 *
 * @param darkTheme Whether to use dark mode (defaults to system setting)
 * @param dynamicColor Whether to use Material You dynamic colors (Android 12+)
 * @param content The composable content to wrap
 */
@Composable
fun Rezon8Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // Disabled to maintain Lithos design consistency
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LithosDarkColorScheme
        else -> LithosLightColorScheme
    }

    // Use LithosTheme for the full design system
    LithosTheme(
        isDark = darkTheme,
        isOLED = false,
        dynamicColor = dynamicColor
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Legacy alias for backwards compatibility
 * Existing code using ReverieTheme will continue to work
 */
@Composable
fun ReverieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    Rezon8Theme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
