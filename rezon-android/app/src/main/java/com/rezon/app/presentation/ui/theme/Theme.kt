package com.rezon.app.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * REZON Theme
 * Cyberpunk/Futuristic aesthetic with neon purples and deep blues
 */

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Color.White,

    secondary = SecondaryDark,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = Color.White,

    tertiary = TertiaryDark,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = Color.White,

    background = BackgroundDark,
    onBackground = OnBackgroundDark,

    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariant,

    error = Error,
    onError = Color.White,

    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),

    scrim = Color.Black.copy(alpha = 0.6f)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = PrimaryContainer,

    secondary = SecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = SecondaryContainer,

    tertiary = TertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = TertiaryContainer,

    background = BackgroundLight,
    onBackground = OnBackgroundLight,

    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF64748B),

    error = Error,
    onError = Color.White,

    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),

    scrim = Color.Black.copy(alpha = 0.4f)
)

@Composable
fun RezonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // REZON always uses custom theme, not dynamic colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Edge-to-edge: draw behind system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Make status bar transparent
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            // Set status bar icons color based on theme
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RezonTypography,
        content = content
    )
}

/**
 * Extended colors not in Material 3 scheme
 * Access via LocalRezonColors.current
 */
data class RezonColors(
    val accentPink: Color = AccentPink,
    val success: Color = Success,
    val warning: Color = Warning,
    val neonGlow: Color = NeonGlow,
    val cyanGlow: Color = CyanGlow,
    val playerGradientStart: Color = PlayerGradientStart,
    val playerGradientEnd: Color = PlayerGradientEnd,
    val progressTrack: Color = ProgressTrack,
    val progressFill: Color = ProgressFill
)

val LocalRezonColors = androidx.compose.runtime.staticCompositionLocalOf { RezonColors() }
