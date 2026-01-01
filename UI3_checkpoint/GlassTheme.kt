package com.mossglen.lithos.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reverie Glass Design System
 *
 * Aligned with:
 * - iOS 26 Liquid Glass
 * - Android 16 Material 3 Expressive
 * - One UI 7
 * - OxygenOS 16
 *
 * Standard: #1 in category, Top 5 overall
 */

// ============================================================================
// COLORS - Monochrome base, content provides color
// ============================================================================

object GlassColors {
    // Backgrounds
    val Background = Color(0xFF000000)          // True black
    val BackgroundLight = Color(0xFFFFFFFF)     // True white

    // Glass Surfaces (Dark Mode)
    val GlassPrimary = Color(0xFFFFFFFF).copy(alpha = 0.10f)    // 10% white
    val GlassSecondary = Color(0xFFFFFFFF).copy(alpha = 0.06f)  // 6% white
    val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.15f)     // 15% white
    val GlassCard = Color(0xFFFFFFFF).copy(alpha = 0.08f)       // 8% white
    val GlassElevated = Color(0xFFFFFFFF).copy(alpha = 0.12f)   // 12% white

    // Glass Surfaces (Light Mode) - More visible glass effect
    val GlassPrimaryLight = Color(0xFF000000).copy(alpha = 0.08f)
    val GlassSecondaryLight = Color(0xFF000000).copy(alpha = 0.05f)
    val GlassBorderLight = Color(0xFF000000).copy(alpha = 0.12f)
    val GlassCardLight = Color(0xFF000000).copy(alpha = 0.06f)
    val GlassElevatedLight = Color(0xFF000000).copy(alpha = 0.10f)

    // Text
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFEBEBF5).copy(alpha = 0.60f)   // iOS secondary
    val TextTertiary = Color(0xFFEBEBF5).copy(alpha = 0.40f)
    val TextPrimaryLight = Color(0xFF000000)
    val TextSecondaryLight = Color(0xFF3C3C43).copy(alpha = 0.60f)

    // Interactive (Single accent - iOS Blue)
    val Interactive = Color(0xFF0A84FF)         // iOS system blue
    val InteractivePressed = Color(0xFF0A84FF).copy(alpha = 0.80f)

    // Reverie Dark - Copper is THE brand color
    val ReverieAccent = Color(0xFFB87333)         // Copper - THE Reverie brand color
    val ReverieAccentPressed = Color(0xFFB87333).copy(alpha = 0.80f)
    val ReverieAccentGlow = Color(0xFFB87333).copy(alpha = 0.25f)
    val ReverieAccentSubtle = Color(0xFFB87333).copy(alpha = 0.15f)

    // Highlight colors for selections
    val WarmSlate = Color(0xFF3D3633)           // Warm slate for Copper/Slate mode
    val SubtleCopper = Color(0xFFB87333).copy(alpha = 0.06f)  // Very subtle for border mode

    // Legacy aliases for compatibility
    val ReverieCopper = ReverieAccent
    val ReverieCopperPressed = ReverieAccentPressed
    val ReverieCopperGlow = ReverieAccentGlow
    val ReverieOrange = ReverieAccent
    val ReverieOrangePressed = ReverieAccentPressed
    val ReverieOrangeGlow = ReverieAccentGlow

    // Reverie Dark subdued text/icon colors - softer on eyes
    val ReverieTextPrimary = Color(0xFFAAAAAA)    // Soft gray-white
    val ReverieTextSecondary = Color(0xFF777777)  // Medium gray
    val ReverieTextTertiary = Color(0xFF505050)   // Dark gray

    // Semantic
    val Success = Color(0xFF30D158)             // iOS green
    val Warning = Color(0xFFFF9F0A)             // iOS orange
    val Destructive = Color(0xFFFF453A)         // iOS red

    // Divider
    val Divider = Color(0xFFFFFFFF).copy(alpha = 0.08f)
    val DividerLight = Color(0xFF000000).copy(alpha = 0.06f)

    // ============================================================================
    // READING MODE - Warm amber/sepia for eye comfort
    // Designed for extended reading sessions, reduces eye strain
    // ============================================================================

    // Reading Mode Background - Warm cream/parchment
    val ReadingBackground = Color(0xFF1A1410)           // Warm dark brown (dark mode)
    val ReadingBackgroundLight = Color(0xFFFAF4E8)      // Warm cream/parchment (light mode)

    // Reading Mode Surfaces
    val ReadingGlassPrimary = Color(0xFFD4A574).copy(alpha = 0.08f)   // Warm amber glass
    val ReadingGlassSecondary = Color(0xFFD4A574).copy(alpha = 0.05f)
    val ReadingGlassBorder = Color(0xFFD4A574).copy(alpha = 0.15f)
    val ReadingGlassCard = Color(0xFFD4A574).copy(alpha = 0.06f)

    // Reading Mode Text - Sepia tones, easy on eyes
    val ReadingTextPrimary = Color(0xFFE8DCC8)          // Warm off-white
    val ReadingTextSecondary = Color(0xFFB8A890)        // Muted tan
    val ReadingTextTertiary = Color(0xFF8A7A68)         // Warm gray-brown

    // Reading Mode Light - For daytime reading
    val ReadingTextPrimaryLight = Color(0xFF3D3228)     // Dark sepia brown
    val ReadingTextSecondaryLight = Color(0xFF5C4F42)   // Medium sepia
    val ReadingTextTertiaryLight = Color(0xFF7A6B5C)    // Light sepia

    // Reading Mode Accent - Warm amber
    val ReadingAccent = Color(0xFFD4A574)               // Warm amber
    val ReadingAccentPressed = Color(0xFFD4A574).copy(alpha = 0.80f)
    val ReadingAccentGlow = Color(0xFFD4A574).copy(alpha = 0.25f)

    // Reading Mode Divider
    val ReadingDivider = Color(0xFFD4A574).copy(alpha = 0.08f)
    val ReadingDividerLight = Color(0xFF3D3228).copy(alpha = 0.10f)

    // Get Reverie accent - always Copper (the brand color)
    fun getReverieAccent(variant: String): Color = ReverieAccent
}

// ============================================================================
// TYPOGRAPHY - Bold titles, clear hierarchy
// ============================================================================

object GlassTypography {
    // Display - For major headers (Library, Settings)
    val Display = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 41.sp
    )

    // Title - For screen titles, section headers
    val Title = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
        lineHeight = 29.sp
    )

    // Headline - For card titles, list items
    val Headline = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        lineHeight = 22.sp
    )

    // Body - For descriptions, content
    val Body = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 21.sp
    )

    // Callout - For secondary information
    val Callout = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )

    // Caption - For timestamps, metadata
    val Caption = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )

    // Label - For buttons, tabs
    val Label = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )

    // Tab - For bottom navigation
    val Tab = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 12.sp
    )
}

// ============================================================================
// SHAPES - Rounded, pill-like
// ============================================================================

object GlassShapes {
    val Small: Dp = 12.dp       // Buttons, chips
    val Medium: Dp = 20.dp      // Cards, dialogs
    val Large: Dp = 28.dp       // Bottom sheets, large cards
    val ExtraLarge: Dp = 36.dp  // Full-screen modals

    // For pill shapes, use height / 2
}

// ============================================================================
// SPACING - Consistent rhythm
// ============================================================================

object GlassSpacing {
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
// BLUR - Gaussian blur values for glass effect
// ============================================================================

object GlassBlur {
    val Light: Dp = 8.dp        // Subtle blur
    val Medium: Dp = 16.dp      // Standard glass
    val Heavy: Dp = 24.dp       // Prominent glass (bottom bar)
    val Ultra: Dp = 32.dp       // Full backdrop blur
}

// ============================================================================
// ELEVATION - Blur-based depth, not shadows
// ============================================================================

object GlassElevation {
    val Level0: Dp = 0.dp       // On surface
    val Level1: Dp = 1.dp       // Cards
    val Level2: Dp = 2.dp       // Floating elements
    val Level3: Dp = 4.dp       // Dialogs
    val Level4: Dp = 8.dp       // Bottom sheets
}

// ============================================================================
// MOTION - Physics-based spring animations
// ============================================================================

object GlassMotion {
    // Spring configurations
    const val DampingRatio = 0.8f
    const val Stiffness = 300f
    const val StiffnessLow = 200f
    const val StiffnessMedium = 400f

    // Durations
    const val DurationFast = 150
    const val DurationMedium = 250
    const val DurationSlow = 350
    const val DurationGesture = 350

    // Easing - Use spring physics instead
}

// ============================================================================
// ICON SIZES - Consistent sizing
// ============================================================================

object GlassIconSize {
    val Small: Dp = 20.dp       // Inline icons
    val Medium: Dp = 24.dp      // Standard icons
    val Large: Dp = 28.dp       // Emphasized icons
    val XLarge: Dp = 32.dp      // Navigation icons
    val XXLarge: Dp = 64.dp     // Play button
}

// ============================================================================
// TOUCH TARGETS - Accessibility compliant (44dp minimum)
// ============================================================================

object GlassTouchTarget {
    val Minimum: Dp = 44.dp     // Apple HIG minimum
    val Standard: Dp = 48.dp    // Material standard
    val Large: Dp = 56.dp       // Primary actions
}

// ============================================================================
// GLASS THEME DATA CLASS
// ============================================================================

@Immutable
data class GlassThemeData(
    val isDark: Boolean,
    val background: Color,
    val glassPrimary: Color,
    val glassSecondary: Color,
    val glassBorder: Color,
    val glassCard: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val interactive: Color
)

val DarkGlassTheme = GlassThemeData(
    isDark = true,
    background = GlassColors.Background,
    glassPrimary = GlassColors.GlassPrimary,
    glassSecondary = GlassColors.GlassSecondary,
    glassBorder = GlassColors.GlassBorder,
    glassCard = GlassColors.GlassCard,
    textPrimary = GlassColors.TextPrimary,
    textSecondary = GlassColors.TextSecondary,
    textTertiary = GlassColors.TextTertiary,
    divider = GlassColors.Divider,
    interactive = GlassColors.Interactive
)

val LightGlassTheme = GlassThemeData(
    isDark = false,
    background = GlassColors.BackgroundLight,
    glassPrimary = GlassColors.GlassPrimaryLight,
    glassSecondary = GlassColors.GlassSecondaryLight,
    glassBorder = GlassColors.GlassBorderLight,
    glassCard = GlassColors.GlassCardLight,
    textPrimary = GlassColors.TextPrimaryLight,
    textSecondary = GlassColors.TextSecondaryLight,
    textTertiary = GlassColors.TextSecondaryLight.copy(alpha = 0.40f),
    divider = GlassColors.DividerLight,
    interactive = GlassColors.Interactive
)

// Reverie Dark - Premium night mode with cognac/bronze accents
// Evokes leather-bound books, premium audio equipment, sophisticated libraries
val ReverieDarkGlassTheme = GlassThemeData(
    isDark = true,
    background = Color(0xFF050505),              // Deep black background
    glassPrimary = Color(0xFFFFFFFF).copy(alpha = 0.05f),  // Very subtle glass
    glassSecondary = Color(0xFFFFFFFF).copy(alpha = 0.03f),
    glassBorder = GlassColors.ReverieAccent.copy(alpha = 0.18f),  // Warm bronze glow
    glassCard = Color(0xFFFFFFFF).copy(alpha = 0.04f),
    textPrimary = GlassColors.ReverieTextPrimary,  // Soft gray-white
    textSecondary = GlassColors.ReverieTextSecondary,  // Medium gray
    textTertiary = GlassColors.ReverieTextTertiary,  // Dark gray
    divider = Color(0xFFFFFFFF).copy(alpha = 0.04f),  // Very subtle dividers
    interactive = GlassColors.ReverieAccent        // Premium bronze accent
)

// Reading Mode Dark - Warm amber/sepia for extended reading sessions
// Reduces blue light exposure, minimizes eye strain during night reading
val ReadingDarkGlassTheme = GlassThemeData(
    isDark = true,
    background = GlassColors.ReadingBackground,
    glassPrimary = GlassColors.ReadingGlassPrimary,
    glassSecondary = GlassColors.ReadingGlassSecondary,
    glassBorder = GlassColors.ReadingGlassBorder,
    glassCard = GlassColors.ReadingGlassCard,
    textPrimary = GlassColors.ReadingTextPrimary,
    textSecondary = GlassColors.ReadingTextSecondary,
    textTertiary = GlassColors.ReadingTextTertiary,
    divider = GlassColors.ReadingDivider,
    interactive = GlassColors.ReadingAccent
)

// Reading Mode Light - Warm parchment for daytime reading
// Soft cream background with sepia text, easy on eyes in bright environments
val ReadingLightGlassTheme = GlassThemeData(
    isDark = false,
    background = GlassColors.ReadingBackgroundLight,
    glassPrimary = Color(0xFF3D3228).copy(alpha = 0.06f),
    glassSecondary = Color(0xFF3D3228).copy(alpha = 0.04f),
    glassBorder = Color(0xFF3D3228).copy(alpha = 0.12f),
    glassCard = Color(0xFF3D3228).copy(alpha = 0.05f),
    textPrimary = GlassColors.ReadingTextPrimaryLight,
    textSecondary = GlassColors.ReadingTextSecondaryLight,
    textTertiary = GlassColors.ReadingTextTertiaryLight,
    divider = GlassColors.ReadingDividerLight,
    interactive = Color(0xFFB8864A)  // Darker amber for light mode
)

val LocalGlassTheme = staticCompositionLocalOf { DarkGlassTheme }

@Composable
fun glassTheme(
    isDark: Boolean = true,
    isReverieDark: Boolean = false,
    isReadingMode: Boolean = false
): GlassThemeData {
    return when {
        isReadingMode && isDark -> ReadingDarkGlassTheme
        isReadingMode && !isDark -> ReadingLightGlassTheme
        isReverieDark -> ReverieDarkGlassTheme
        isDark -> DarkGlassTheme
        else -> LightGlassTheme
    }
}
