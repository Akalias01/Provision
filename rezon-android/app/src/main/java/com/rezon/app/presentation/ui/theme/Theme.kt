package com.rezon.app.presentation.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * REZON Dark Color Scheme
 * Cyberpunk aesthetic with neon purples and electric blues
 *
 * Background: #0D0D15 (Deep Black/Blue)
 * Primary: #7F00FF (Neon Purple)
 * Secondary: #00E5FF (Cyan/Electric Blue)
 * Surface: #1E1E26 (Dark Gray for cards)
 */
private val RezonDarkColorScheme = darkColorScheme(
    // Primary - Neon Purple
    primary = RezonPurple,
    onPrimary = RezonOnPrimary,
    primaryContainer = RezonPurpleContainer,
    onPrimaryContainer = Color.White,

    // Secondary - Electric Cyan
    secondary = RezonCyan,
    onSecondary = Color.Black,
    secondaryContainer = RezonCyanContainer,
    onSecondaryContainer = RezonCyan,

    // Tertiary - Accent Pink
    tertiary = RezonAccentPink,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4A1942),
    onTertiaryContainer = RezonAccentPink,

    // Background - Deep Black/Blue
    background = RezonBackground,
    onBackground = RezonOnBackground,

    // Surface - Dark Gray for cards
    surface = RezonSurface,
    onSurface = RezonOnSurface,
    surfaceVariant = RezonSurfaceVariant,
    onSurfaceVariant = RezonOnSurfaceVariant,

    // Error
    error = RezonAccentRed,
    onError = Color.White,
    errorContainer = Color(0xFF4A1414),
    onErrorContainer = RezonAccentRed,

    // Outline
    outline = DividerColor,
    outlineVariant = Color(0xFF1E1E26),

    // Inverse
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = RezonBackground,
    inversePrimary = RezonPurpleDark,

    // Scrim
    scrim = Color.Black.copy(alpha = 0.7f),

    // Surface tint
    surfaceTint = RezonPurple
)

/**
 * Extended colors for REZON that aren't in Material 3
 * Access via RezonTheme.colors or LocalRezonColors.current
 */
data class RezonExtendedColors(
    val neonPurple: Color = RezonPurple,
    val neonCyan: Color = RezonCyan,
    val accentPink: Color = RezonAccentPink,
    val success: Color = RezonAccentGreen,
    val warning: Color = RezonAccentOrange,
    val error: Color = RezonAccentRed,
    val neonPurpleGlow: Color = NeonPurpleGlow,
    val neonCyanGlow: Color = NeonCyanGlow,
    val progressTrack: Color = ProgressTrack,
    val progressFill: Color = ProgressFill,
    val progressBuffer: Color = ProgressBuffer,
    val playerGradientStart: Color = PlayerGradientStart,
    val playerGradientEnd: Color = PlayerGradientEnd,
    val drawerBackground: Color = DrawerBackground,
    val drawerItemSelected: Color = DrawerItemSelected,
    val drawerItemHover: Color = DrawerItemHover,
    val divider: Color = DividerColor
)

val LocalRezonColors = staticCompositionLocalOf { RezonExtendedColors() }

/**
 * REZON Theme
 *
 * Always uses dark theme for the cyberpunk aesthetic.
 * Edge-to-edge display with transparent system bars.
 *
 * @param content The composable content to be themed
 */
@Composable
fun RezonTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = RezonDarkColorScheme
    val extendedColors = RezonExtendedColors()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Edge-to-edge display - draw behind system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Transparent system bars for immersive experience
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            // Light icons on dark background (dark theme = false for light icons)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalRezonColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RezonTypography,
            content = content
        )
    }
}

/**
 * Access extended REZON colors from composables
 *
 * Usage: RezonTheme.colors.neonPurple
 */
object RezonTheme {
    val colors: RezonExtendedColors
        @Composable
        get() = LocalRezonColors.current
}
