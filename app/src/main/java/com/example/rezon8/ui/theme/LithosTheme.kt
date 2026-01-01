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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * LITHOS AMBER Design System - Theme Provider
 *
 * Design Philosophy:
 * - NO neon/glowing effects
 * - Matte/satin finishes
 * - Natural materials: Stone (Slate), Fossilized Resin (Amber), Forest (Moss)
 * - Progress rings use THIN strokes (2-3px)
 * - Glass effects use frosted blur (20dp) not shiny gradients
 *
 * Theme Modes:
 * - Standard Dark (Slate)
 * - Reader Light (Oat)
 * - OLED Night (Black)
 */

// ============================================================================
// LITHOS THEME DATA CLASS
// ============================================================================

@Immutable
data class LithosThemeData(
    val isDark: Boolean,
    val isOLED: Boolean = false,

    // Backgrounds
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,

    // Glass (frosted blur at 20dp)
    val glass: Color,
    val glassBorder: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,

    // Accents
    val amber: Color,           // Primary accent for most UI
    val amberLight: Color,
    val amberDark: Color,
    val moss: Color,            // ONLY for Play/Pause button
    val mossLight: Color,
    val mossDark: Color,

    // Semantic
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,

    // Divider
    val divider: Color,

    // Progress (thin 2-3px strokes)
    val progressTrack: Color,
    val progressFill: Color     // Uses amber by default
)

// ============================================================================
// PRE-DEFINED LITHOS THEMES
// ============================================================================

/**
 * Standard Dark Theme - Slate background
 * Default for audiobook playback and general navigation
 */
val LithosDarkTheme = LithosThemeData(
    isDark = true,
    isOLED = false,
    background = LithosSlate,
    surface = LithosSurfaceDark,
    surfaceElevated = LithosSurfaceDarkElevated,
    glass = LithosGlass,
    glassBorder = LithosGlassBorder,
    textPrimary = LithosTextPrimary,
    textSecondary = LithosTextSecondary,
    textTertiary = LithosTextTertiary,
    amber = LithosAmber,
    amberLight = LithosAmberLight,
    amberDark = LithosAmberDark,
    moss = LithosMoss,
    mossLight = LithosMossLight,
    mossDark = LithosMossDark,
    success = LithosSuccess,
    warning = LithosWarning,
    error = LithosError,
    info = LithosInfo,
    divider = LithosDivider,
    progressTrack = LithosProgressTrack,
    progressFill = LithosAmber
)

/**
 * Reader Light Theme - Oat background
 * For comfortable reading in well-lit environments
 */
val LithosLightTheme = LithosThemeData(
    isDark = false,
    isOLED = false,
    background = LithosOat,
    surface = LithosSurfaceLight,
    surfaceElevated = LithosSurfaceLightElevated,
    glass = LithosGlassLight,
    glassBorder = LithosGlassBorderLight,
    textPrimary = LithosTextPrimaryLight,
    textSecondary = LithosTextSecondaryLight,
    textTertiary = LithosTextTertiaryLight,
    amber = LithosAmberDark,        // Darker amber for light backgrounds
    amberLight = LithosAmber,
    amberDark = LithosAmberDark,
    moss = LithosMossDark,          // Darker moss for light backgrounds
    mossLight = LithosMoss,
    mossDark = LithosMossDark,
    success = LithosMossDark,
    warning = LithosAmberDark,
    error = LithosError,
    info = LithosInfo,
    divider = LithosDividerLight,
    progressTrack = LithosProgressTrackLight,
    progressFill = LithosAmberDark
)

/**
 * OLED Night Theme - True black background
 * Maximum contrast, battery saving on OLED screens
 */
val LithosOLEDTheme = LithosThemeData(
    isDark = true,
    isOLED = true,
    background = LithosBlack,
    surface = Color(0xFF0A0A0A),
    surfaceElevated = Color(0xFF141414),
    glass = Color(0xD9000000),      // 85% black
    glassBorder = Color(0x1AFFFFFF),
    textPrimary = LithosTextPrimary,
    textSecondary = LithosTextSecondary,
    textTertiary = LithosTextTertiary,
    amber = LithosAmber,
    amberLight = LithosAmberLight,
    amberDark = LithosAmberDark,
    moss = LithosMoss,
    mossLight = LithosMossLight,
    mossDark = LithosMossDark,
    success = LithosSuccess,
    warning = LithosWarning,
    error = LithosError,
    info = LithosInfo,
    divider = Color(0x0DFFFFFF),    // 5% white for OLED
    progressTrack = Color(0x26FFFFFF),
    progressFill = LithosAmber
)

// ============================================================================
// COMPOSITION LOCAL
// ============================================================================

val LocalLithosTheme = staticCompositionLocalOf { LithosDarkTheme }

// ============================================================================
// LITHOS THEME COMPOSABLE
// ============================================================================

/**
 * Lithos Amber Theme Provider
 *
 * @param isDark Whether to use dark mode
 * @param isOLED Whether to use true black OLED mode (only applies when isDark = true)
 * @param dynamicColor Whether to use Material You dynamic colors (Android 12+)
 * @param content The composable content to wrap
 */
@Composable
fun LithosTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    isOLED: Boolean = false,
    dynamicColor: Boolean = false,  // Disabled by default for Lithos design consistency
    content: @Composable () -> Unit
) {
    // Select Lithos theme
    val lithosTheme = when {
        isOLED && isDark -> LithosOLEDTheme
        isDark -> LithosDarkTheme
        else -> LithosLightTheme
    }

    // Material 3 color scheme for compatibility
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColorScheme(
            primary = LithosAmber,
            onPrimary = LithosTextPrimary,
            primaryContainer = LithosAmberDark,
            onPrimaryContainer = LithosTextPrimary,
            secondary = LithosMoss,
            onSecondary = LithosTextPrimary,
            secondaryContainer = LithosMossDark,
            onSecondaryContainer = LithosTextPrimary,
            tertiary = LithosAmberLight,
            background = lithosTheme.background,
            onBackground = LithosTextPrimary,
            surface = lithosTheme.surface,
            onSurface = LithosTextPrimary,
            surfaceVariant = lithosTheme.surfaceElevated,
            onSurfaceVariant = LithosTextSecondary,
            error = LithosError,
            onError = LithosTextPrimary
        )
        else -> lightColorScheme(
            primary = LithosAmberDark,
            onPrimary = LithosOat,
            primaryContainer = LithosAmber,
            onPrimaryContainer = LithosTextPrimaryLight,
            secondary = LithosMossDark,
            onSecondary = LithosOat,
            secondaryContainer = LithosMoss,
            onSecondaryContainer = LithosTextPrimaryLight,
            tertiary = LithosAmber,
            background = lithosTheme.background,
            onBackground = LithosTextPrimaryLight,
            surface = lithosTheme.surface,
            onSurface = LithosTextPrimaryLight,
            surfaceVariant = lithosTheme.surfaceElevated,
            onSurfaceVariant = LithosTextSecondaryLight,
            error = LithosError,
            onError = LithosOat
        )
    }

    CompositionLocalProvider(
        LocalLithosTheme provides lithosTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// ============================================================================
// THEME ACCESSOR
// ============================================================================

/**
 * Access the current Lithos theme from any composable
 *
 * Usage:
 * val theme = LithosTheme.current
 * Box(modifier = Modifier.background(theme.background))
 */
object LithosTheme {
    val current: LithosThemeData
        @Composable
        get() = LocalLithosTheme.current
}

// ============================================================================
// LITHOS TYPOGRAPHY
// ============================================================================

object LithosTypography {
    // Display - Major headers
    val Display = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 41.sp
    )

    // Title - Screen titles, section headers
    val Title = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
        lineHeight = 29.sp
    )

    // Headline - Card titles, list items
    val Headline = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        lineHeight = 22.sp
    )

    // Body - Descriptions, content
    val Body = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 21.sp
    )

    // Callout - Secondary information
    val Callout = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )

    // Caption - Timestamps, metadata
    val Caption = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )

    // Label - Buttons, tabs
    val Label = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )

    // Tab - Bottom navigation
    val Tab = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 12.sp
    )
}

// ============================================================================
// LITHOS SHAPES
// ============================================================================

object LithosShapes {
    val Small: Dp = 12.dp       // Buttons, chips
    val Medium: Dp = 20.dp      // Cards, dialogs
    val Large: Dp = 28.dp       // Bottom sheets, large cards
    val ExtraLarge: Dp = 36.dp  // Full-screen modals
}

// ============================================================================
// LITHOS SPACING
// ============================================================================

object LithosSpacing {
    val XXS: Dp = 4.dp
    val XS: Dp = 8.dp
    val S: Dp = 12.dp
    val M: Dp = 16.dp
    val L: Dp = 24.dp
    val XL: Dp = 32.dp
    val XXL: Dp = 48.dp
    val XXXL: Dp = 64.dp
}

// ============================================================================
// LITHOS BLUR VALUES - For frosted glass effects
// ============================================================================

object LithosBlur {
    val Light: Dp = 8.dp        // Subtle blur
    val Medium: Dp = 16.dp      // Standard glass
    val Standard: Dp = 20.dp    // Default frosted glass (design spec)
    val Heavy: Dp = 24.dp       // Prominent glass
    val Ultra: Dp = 32.dp       // Full backdrop blur
}

// ============================================================================
// LITHOS PROGRESS RING SPECS
// ============================================================================

object LithosProgressRing {
    // Stroke widths - THIN per design spec
    val StrokeThin: Dp = 2.dp
    val StrokeStandard: Dp = 3.dp
    val StrokeMedium: Dp = 4.dp  // Maximum recommended

    // Ring sizes
    val SizeSmall: Dp = 32.dp
    val SizeMedium: Dp = 48.dp
    val SizeLarge: Dp = 64.dp
    val SizeXLarge: Dp = 88.dp

    // Track alpha (background ring)
    const val TrackAlpha: Float = 0.20f
}

// ============================================================================
// LITHOS MOTION - Physics-based animations
// ============================================================================

object LithosMotion {
    // Spring presets
    const val DampingRatio = 0.8f
    const val Stiffness = 300f

    const val DampingQuick = 0.75f
    const val StiffnessQuick = 500f

    const val DampingGentle = 0.85f
    const val StiffnessGentle = 150f

    // Durations for tween fallbacks
    const val DurationInstant = 50
    const val DurationFast = 150
    const val DurationMedium = 250
    const val DurationSlow = 350

    // Press scale feedback
    const val ScalePressed = 0.95f
    const val ScalePressedLight = 0.98f
}

// ============================================================================
// LITHOS ICON SIZES
// ============================================================================

object LithosIconSize {
    val Small: Dp = 20.dp
    val Medium: Dp = 24.dp
    val Large: Dp = 28.dp
    val XLarge: Dp = 32.dp
    val XXLarge: Dp = 64.dp     // Play button
}

// ============================================================================
// LITHOS TOUCH TARGETS - Accessibility compliant
// ============================================================================

object LithosTouchTarget {
    val Minimum: Dp = 44.dp     // Apple HIG minimum
    val Standard: Dp = 48.dp    // Material standard
    val Large: Dp = 56.dp       // Primary actions
}
