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
 * LITHOS AMBER Glass Design System
 *
 * Design Philosophy:
 * - NO neon/glowing effects
 * - Matte/satin finishes
 * - Natural materials: Stone (Slate), Fossilized Resin (Amber), Forest (Moss)
 * - Progress rings use THIN strokes (2-3px)
 * - Glass effects use frosted blur (20dp) not shiny gradients
 */

// ============================================================================
// COLORS - Lithos Amber palette with natural materials theme
// ============================================================================

object GlassColors {
    // Backgrounds - Stone/Slate inspired
    val Background = LithosSlate                   // Standard dark
    val BackgroundLight = LithosOat                // Reader light
    val BackgroundOLED = LithosBlack               // OLED night

    // Glass Surfaces (Dark Mode) - Frosted, not shiny
    val GlassPrimary = Color(0xFFFFFFFF).copy(alpha = 0.10f)
    val GlassSecondary = Color(0xFFFFFFFF).copy(alpha = 0.06f)
    val GlassBorder = LithosGlassBorder
    val GlassCard = Color(0xFFFFFFFF).copy(alpha = 0.08f)
    val GlassElevated = Color(0xFFFFFFFF).copy(alpha = 0.12f)

    // Glass Surfaces (Light Mode)
    val GlassPrimaryLight = Color(0xFF000000).copy(alpha = 0.08f)
    val GlassSecondaryLight = Color(0xFF000000).copy(alpha = 0.05f)
    val GlassBorderLight = LithosGlassBorderLight
    val GlassCardLight = Color(0xFF000000).copy(alpha = 0.06f)
    val GlassElevatedLight = Color(0xFF000000).copy(alpha = 0.10f)

    // Text - Lithos text colors
    val TextPrimary = LithosTextPrimary
    val TextSecondary = LithosTextSecondary
    val TextTertiary = LithosTextTertiary
    val TextPrimaryLight = LithosTextPrimaryLight
    val TextSecondaryLight = LithosTextSecondaryLight

    // Interactive - Lithos Amber (NOT iOS blue)
    val Interactive = LithosAmber
    val InteractivePressed = LithosAmber.copy(alpha = 0.80f)

    // Lithos Amber accent for TEXT and ICONS
    val LithosAccent = LithosAmber
    val LithosAccentPressed = LithosAmber.copy(alpha = 0.80f)
    val LithosAccentSubtle = LithosAmber.copy(alpha = 0.15f)

    // Legacy aliases - map old names to Lithos (no duplicates)
    val LithosAccentMain = LithosAmber
    val LithosAccentGlow = LithosAmber.copy(alpha = 0.20f)  // Reduced - no neon glow
    val LithosAccentSubtleMain = LithosAmber.copy(alpha = 0.15f)

    // Selection highlights - Slate based, neutral
    val SelectionGlass = Color(0xFF2D3339)
    val SelectionBorder = Color(0xFF3D444D).copy(alpha = 0.60f)
    val SelectionGlow = Color(0xFF3D444D).copy(alpha = 0.15f)  // Reduced

    // Background colors for selections
    val WarmSlate = Color(0xFF3D444D)
    val SubtleSlate = Color(0xFF2D3339).copy(alpha = 0.50f)

    // Button backgrounds - Slate based
    val ButtonBackground = Color(0xFF2D3339)
    val ButtonBackgroundLight = Color(0xFF3D444D).copy(alpha = 0.25f)
    val ButtonBackgroundPressed = Color(0xFF3D444D)

    // Legacy aliases for compatibility
    val LithosCopper = LithosAmber
    val LithosCopperPressed = LithosAmber.copy(alpha = 0.80f)
    val LithosCopperGlow = LithosAmber.copy(alpha = 0.15f)
    val LithosOrange = LithosAmber
    val LithosOrangePressed = LithosAmber.copy(alpha = 0.80f)
    val LithosOrangeGlow = LithosAmber.copy(alpha = 0.15f)
    val SubtleCopper = SubtleSlate

    // Lithos subdued text/icon colors
    val LithosPremiumTextPrimary = Color(0xFFAAAAAA)
    val LithosPremiumTextSecondary = Color(0xFF777777)
    val LithosPremiumTextTertiary = Color(0xFF505050)

    // Semantic - Natural colors, no neon
    val Success = LithosSuccess      // Moss-based
    val Warning = LithosWarning      // Amber
    val Destructive = LithosError    // Muted terracotta

    // Divider
    val Divider = LithosDivider
    val DividerLight = LithosDividerLight

    // ============================================================================
    // READING MODE - Warm tones for eye comfort
    // ============================================================================

    val ReadingBackground = Color(0xFF1A1410)
    val ReadingBackgroundLight = LithosOat

    // Reading Mode Surfaces - Moss Green
    val ReadingGlassPrimary = LithosMoss.copy(alpha = 0.08f)
    val ReadingGlassSecondary = LithosMoss.copy(alpha = 0.05f)
    val ReadingGlassBorder = LithosMoss.copy(alpha = 0.15f)
    val ReadingGlassCard = LithosMoss.copy(alpha = 0.06f)

    // Reading Mode Text
    val ReadingTextPrimary = Color(0xFFE8DCC8)
    val ReadingTextSecondary = Color(0xFFB8A890)
    val ReadingTextTertiary = Color(0xFF8A7A68)

    // Reading Mode Light
    val ReadingTextPrimaryLight = Color(0xFF3D3228)
    val ReadingTextSecondaryLight = Color(0xFF5C4F42)
    val ReadingTextTertiaryLight = Color(0xFF7A6B5C)

    // Reading Mode Accent - Moss
    val ReadingAccent = LithosMoss
    val ReadingAccentPressed = LithosMoss.copy(alpha = 0.80f)

    // Reading Mode Divider
    val ReadingDivider = LithosMoss.copy(alpha = 0.08f)
    val ReadingDividerLight = Color(0xFF3D3228).copy(alpha = 0.10f)

    // Get Lithos accent
    fun getLithosAccent(variant: String): Color = LithosAmber
}

// ============================================================================
// TYPOGRAPHY - Bold titles, clear hierarchy
// ============================================================================

object GlassTypography {
    val Display = LithosTypography.Display
    val Title = LithosTypography.Title
    val Headline = LithosTypography.Headline
    val Body = LithosTypography.Body
    val Callout = LithosTypography.Callout
    val Caption = LithosTypography.Caption
    val Label = LithosTypography.Label
    val Tab = LithosTypography.Tab
}

// ============================================================================
// SHAPES - Rounded, pill-like
// ============================================================================

object GlassShapes {
    val Small: Dp = LithosShapes.Small
    val Medium: Dp = LithosShapes.Medium
    val Large: Dp = LithosShapes.Large
    val ExtraLarge: Dp = LithosShapes.ExtraLarge
}

// ============================================================================
// UNIFIED COMPONENT STYLES - Lithos design language
// ============================================================================

object LithosComponents {

    // ========== CONTROL PILL - Floating bottom control bar ==========
    object Pill {
        val height: Dp = 58.dp
        val cornerRadius: Dp = 30.dp
        val horizontalPadding: Dp = 16.dp
        val bottomPadding: Dp = 8.dp
        val shadowElevation: Dp = 8.dp           // Reduced - matte finish
        val iconSize: Dp = 22.dp
        val touchTargetSize: Dp = 40.dp
        val dividerWidth: Dp = 1.dp
        val dividerHeight: Dp = 24.dp
        val borderWidth: Dp = 1.dp

        // Colors (dark mode) - Matte finishes
        val backgroundAlpha: Float = 0.92f
        val borderAlpha: Float = 0.20f           // Reduced - no shine
        val accentBorderAlpha: Float = 0.40f     // Reduced
        val dividerAlpha: Float = 0.10f

        // Animation
        val pressScale: Float = 0.85f
        val springDamping: Float = 0.5f
        val springStiffness: Float = 500f
    }

    // ========== NAVIGATION RING - THIN strokes per spec ==========
    object NavigationRing {
        val outerRadius: Dp = 44.dp
        val strokeWidth: Dp = 3.dp               // THIN per Lithos spec
        val innerTextSize: Float = 11f
        val progressTrackAlpha: Float = 0.20f    // Lithos track alpha
        val glowAlpha: Float = 0.10f             // Reduced - no neon glow
        val bookmarkDotSize: Dp = 4.dp
    }

    // ========== PROGRESS BAR - THIN per Lithos spec ==========
    object ProgressBar {
        val height: Dp = 3.dp                    // THIN
        val cornerRadius: Dp = 2.dp
        val thumbSize: Dp = 14.dp
        val thumbSizeActive: Dp = 18.dp
        val trackAlpha: Float = 0.20f            // Lithos track alpha
        val horizontalPadding: Dp = 24.dp

        // Preview tooltip
        val previewPadding: Dp = 16.dp
        val previewCornerRadius: Dp = 12.dp
        val previewBorderWidth: Dp = 1.dp
    }

    // ========== CARDS & SHEETS ==========
    object Cards {
        val cornerRadius: Dp = 20.dp
        val dialogRadius: Dp = 24.dp
        val sheetRadius: Dp = 20.dp
        val chipRadius: Dp = 12.dp
        val buttonRadius: Dp = 12.dp

        // Background colors - Lithos surfaces
        val sheetBackground = LithosSurfaceDark
        val cardBackground = LithosSurfaceDarkElevated
        val chipBackground = LithosSurfaceDarkElevated

        // Borders
        val borderWidth: Dp = 0.5.dp
        val borderAlpha: Float = 0.10f           // Reduced

        // Padding
        val contentPadding: Dp = 16.dp
        val headerPadding: Dp = 20.dp
    }

    // ========== BUTTONS ==========
    object Buttons {
        val height: Dp = 48.dp
        val minWidth: Dp = 120.dp
        val cornerRadius: Dp = 12.dp
        val iconButtonSize: Dp = 44.dp
        val playButtonSize: Dp = 64.dp

        // Press animation
        val pressScale: Float = 0.95f
        val pressAlpha: Float = 0.80f

        // Colors
        val primaryAlpha: Float = 1.0f
        val secondaryAlpha: Float = 0.60f
        val disabledAlpha: Float = 0.38f
    }

    // ========== ALBUM ART / COVER ==========
    object Cover {
        val cornerRadius: Dp = 24.dp
        val shadowElevation: Dp = 16.dp          // Reduced - matte
        val horizontalPadding: Dp = 12.dp

        // Animation
        val maxRotation: Float = 0.015f
        val maxScale: Float = 0.03f
    }

    // ========== TIME LABELS ==========
    object TimeLabels {
        val fontSize: Float = 13f
        val fontWeight: FontWeight = FontWeight.Medium
        val letterSpacing: Float = 0.5f
        val horizontalPadding: Dp = 24.dp
    }

    // ========== CHAPTER/SECTION LIST ==========
    object ChapterList {
        val itemHeight: Dp = 56.dp
        val itemPadding: Dp = 16.dp
        val progressBarHeight: Dp = 2.dp         // THIN
        val progressBarRadius: Dp = 1.dp
        val dividerAlpha: Float = 0.08f
    }
}

// ============================================================================
// LITHOS ACCENTS - Natural color system
// ============================================================================

object LithosAccents {
    // Primary accent - Amber (fossilized resin)
    val Primary = LithosAmber
    val PrimaryLight = LithosAmberLight
    val PrimaryVibrant = LithosAmber

    // Reading accent - Moss (forest)
    val Reading = LithosMoss
    val ReadingLight = LithosMossLight
    val ReadingVibrant = LithosMoss

    // Interactive states - matte, no glow
    fun pressed(color: Color): Color = color.copy(alpha = 0.80f)
    fun glow(color: Color): Color = color.copy(alpha = 0.15f)  // Reduced
    fun subtle(color: Color): Color = color.copy(alpha = 0.12f)
    fun border(color: Color): Color = color.copy(alpha = 0.25f)
}

// ============================================================================
// LITHOS UI COLORS - Consistent backgrounds
// ============================================================================

object LithosUI {
    // ========== SURFACES - Dark Mode ==========
    val SheetBackground = LithosSurfaceDark
    val CardBackground = LithosSurfaceDarkElevated
    val ElevatedBackground = Color(0xFF3C3C3E)
    val DeepBackground = Color(0xFF0A0A0A)
    val DimBackground = LithosSlate

    // ========== SURFACES - Light Mode ==========
    val SheetBackgroundLight = LithosSurfaceLight
    val CardBackgroundLight = LithosSurfaceLightElevated
    val ElevatedBackgroundLight = LithosOat

    // ========== CHIPS & TAGS ==========
    val ChipBackground = LithosSurfaceDarkElevated
    val ChipBackgroundLight = LithosSurfaceLightElevated
    val ChipBorder = Color(0xFF3C3C3E).copy(alpha = 0.5f)

    // ========== SEMANTIC COLORS ==========
    val Destructive = LithosError
    val DestructiveDark = LithosError
    val Success = LithosSuccess
    val SuccessAlt = LithosSuccess
    val Warning = LithosWarning

    // ========== INTERACTIVE ==========
    val InactiveTrack = LithosSurfaceDarkElevated
    val ActiveTrack = LithosAmber

    // ========== GLASS EFFECTS - Frosted blur at 20dp ==========
    val GlassOverlay95 = LithosGlass
    val GlassOverlay92 = Color(0xFF1A1D21).copy(alpha = 0.92f)
    val GlassOverlay80 = Color(0xFF1A1D21).copy(alpha = 0.80f)
    val GlassOverlayLight = LithosGlassLight

    // ========== HELPER FUNCTIONS ==========
    fun surface(isDark: Boolean) = if (isDark) SheetBackground else SheetBackgroundLight
    fun card(isDark: Boolean) = if (isDark) CardBackground else CardBackgroundLight
    fun chip(isDark: Boolean) = if (isDark) ChipBackground else ChipBackgroundLight
    fun overlay(isDark: Boolean, alpha: Float = 0.85f) =
        if (isDark) LithosSlate.copy(alpha = alpha)
        else LithosOat.copy(alpha = alpha)

    /**
     * Get the correct background based on Lithos 3-mode system:
     * - Light mode: Oat (#F2F0E9)
     * - Dark mode: Slate (#1A1D21)
     * - OLED mode: True Black (#000000) or DeepBackground (#0A0A0A)
     */
    fun background(isDark: Boolean, isOLED: Boolean = false): Color = when {
        isOLED -> LithosBlack
        isDark -> LithosSlate
        else -> LithosOat
    }

    /**
     * Get the correct sheet/dialog background based on Lithos 3-mode system
     */
    fun sheetBackground(isDark: Boolean, isOLED: Boolean = false): Color = when {
        isOLED -> DeepBackground
        isDark -> SheetBackground
        else -> SheetBackgroundLight
    }
}

// ============================================================================
// SPACING - Consistent rhythm
// ============================================================================

object GlassSpacing {
    val XXS: Dp = LithosSpacing.XXS
    val XS: Dp = LithosSpacing.XS
    val S: Dp = LithosSpacing.S
    val M: Dp = LithosSpacing.M
    val L: Dp = LithosSpacing.L
    val XL: Dp = LithosSpacing.XL
    val XXL: Dp = LithosSpacing.XXL
    val XXXL: Dp = LithosSpacing.XXXL
}

// ============================================================================
// BLUR - Frosted glass values (20dp standard per Lithos spec)
// ============================================================================

object GlassBlur {
    val Light: Dp = LithosBlur.Light
    val Medium: Dp = LithosBlur.Medium
    val Standard: Dp = LithosBlur.Standard       // 20dp - Lithos default
    val Heavy: Dp = LithosBlur.Heavy
    val Ultra: Dp = LithosBlur.Ultra
}

// ============================================================================
// ELEVATION - Subtle depth, not shiny
// ============================================================================

object GlassElevation {
    val Level0: Dp = 0.dp
    val Level1: Dp = 1.dp
    val Level2: Dp = 2.dp
    val Level3: Dp = 4.dp
    val Level4: Dp = 8.dp
}

// ============================================================================
// MOTION - Physics-based animations
// ============================================================================

object GlassMotion {
    const val DampingRatio = LithosMotion.DampingRatio
    const val Stiffness = LithosMotion.Stiffness

    const val DampingQuick = LithosMotion.DampingQuick
    const val StiffnessQuick = LithosMotion.StiffnessQuick

    const val DampingGentle = LithosMotion.DampingGentle
    const val StiffnessGentle = LithosMotion.StiffnessGentle

    const val DampingBouncy = 0.5f
    const val StiffnessBouncy = 350f

    const val DampingCardStack = 0.7f
    const val StiffnessCardStack = 200f

    const val DampingPress = 0.6f
    const val StiffnessPress = 400f

    const val StiffnessLow = 200f
    const val StiffnessMedium = 400f

    const val DurationInstant = LithosMotion.DurationInstant
    const val DurationFast = LithosMotion.DurationFast
    const val DurationMedium = LithosMotion.DurationMedium
    const val DurationSlow = LithosMotion.DurationSlow
    const val DurationGesture = 350
    const val DurationPage = 400

    const val ScalePressed = LithosMotion.ScalePressed
    const val ScalePressedLight = LithosMotion.ScalePressedLight
    const val ScalePressedHeavy = 0.90f
    const val ScaleHover = 1.02f

    const val StaggerDelay = 50
    const val ChainDelay = 100
}

// ============================================================================
// ICON SIZES
// ============================================================================

object GlassIconSize {
    val Small: Dp = LithosIconSize.Small
    val Medium: Dp = LithosIconSize.Medium
    val Large: Dp = LithosIconSize.Large
    val XLarge: Dp = LithosIconSize.XLarge
    val XXLarge: Dp = LithosIconSize.XXLarge
}

// ============================================================================
// TOUCH TARGETS - Accessibility compliant
// ============================================================================

object GlassTouchTarget {
    val Minimum: Dp = LithosTouchTarget.Minimum
    val Standard: Dp = LithosTouchTarget.Standard
    val Large: Dp = LithosTouchTarget.Large
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
    background = LithosSlate,
    glassPrimary = GlassColors.GlassPrimary,
    glassSecondary = GlassColors.GlassSecondary,
    glassBorder = LithosGlassBorder,
    glassCard = GlassColors.GlassCard,
    textPrimary = LithosTextPrimary,
    textSecondary = LithosTextSecondary,
    textTertiary = LithosTextTertiary,
    divider = LithosDivider,
    interactive = LithosAmber
)

val LightGlassTheme = GlassThemeData(
    isDark = false,
    background = LithosOat,
    glassPrimary = GlassColors.GlassPrimaryLight,
    glassSecondary = GlassColors.GlassSecondaryLight,
    glassBorder = LithosGlassBorderLight,
    glassCard = GlassColors.GlassCardLight,
    textPrimary = LithosTextPrimaryLight,
    textSecondary = LithosTextSecondaryLight,
    textTertiary = LithosTextSecondaryLight.copy(alpha = 0.40f),
    divider = LithosDividerLight,
    interactive = LithosAmberDark
)

// Lithos OLED - True black for OLED screens with maximum contrast
val LithosOLEDGlassTheme = GlassThemeData(
    isDark = true,
    background = LithosBlack,  // True black #000000
    glassPrimary = Color(0xFFFFFFFF).copy(alpha = 0.05f),
    glassSecondary = Color(0xFFFFFFFF).copy(alpha = 0.03f),
    glassBorder = Color.White.copy(alpha = 0.12f),
    glassCard = Color(0xFFFFFFFF).copy(alpha = 0.04f),
    textPrimary = LithosTextPrimary,
    textSecondary = LithosTextSecondary,
    textTertiary = LithosTextTertiary,
    divider = Color(0xFFFFFFFF).copy(alpha = 0.04f),
    interactive = LithosAmber
)

// Legacy alias for backward compatibility
val LithosDarkGlassTheme = LithosOLEDGlassTheme

// Reading Mode Dark
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
    interactive = LithosMoss
)

// Reading Mode Light
val ReadingLightGlassTheme = GlassThemeData(
    isDark = false,
    background = LithosOat,
    glassPrimary = Color(0xFF3D3228).copy(alpha = 0.06f),
    glassSecondary = Color(0xFF3D3228).copy(alpha = 0.04f),
    glassBorder = Color(0xFF3D3228).copy(alpha = 0.12f),
    glassCard = Color(0xFF3D3228).copy(alpha = 0.05f),
    textPrimary = GlassColors.ReadingTextPrimaryLight,
    textSecondary = GlassColors.ReadingTextSecondaryLight,
    textTertiary = GlassColors.ReadingTextTertiaryLight,
    divider = GlassColors.ReadingDividerLight,
    interactive = LithosMossDark
)

val LocalGlassTheme = staticCompositionLocalOf { DarkGlassTheme }

/**
 * Get the appropriate GlassTheme based on mode settings
 *
 * LITHOS 3-Mode System:
 * - Lithos Light: Warm oat/cream background (#F2F0E9)
 * - Lithos Standard: Deep slate background (#1A1D21)
 * - Lithos Black: True OLED black (#000000)
 *
 * All modes use:
 * - Amber (#D48C2C) for accents, progress, highlights
 * - Moss (#4A5D45) ONLY for play/pause button
 *
 * @param isDark Whether to use dark mode (Standard or OLED)
 * @param isOLED Whether to use true black OLED mode (only applies when isDark=true)
 * @param isReadingMode Whether reading mode is active (uses moss/sepia tones)
 */
@Composable
fun glassTheme(
    isDark: Boolean = true,
    isOLED: Boolean = false,
    isReadingMode: Boolean = false
): GlassThemeData {
    return when {
        isReadingMode && isDark -> ReadingDarkGlassTheme
        isReadingMode && !isDark -> ReadingLightGlassTheme
        isOLED && isDark -> LithosOLEDGlassTheme  // True black for OLED
        isDark -> DarkGlassTheme                   // Slate for standard dark
        else -> LightGlassTheme                    // Oat for light mode
    }
}

// Legacy compatibility - old parameter name
@Composable
fun glassTheme(
    isDark: Boolean = true,
    isLithosDark: Boolean = false,
    isReadingMode: Boolean = false,
    @Suppress("UNUSED_PARAMETER") legacyMode: Boolean = false
): GlassThemeData = glassTheme(isDark, isLithosDark, isReadingMode)

// ============================================================================
// LEGACY COMPATIBILITY ALIASES
// Map old Reverie names to new Lithos names for backward compatibility
// ============================================================================
val ReverieComponents = LithosComponents
val ReverieUI = LithosUI
val ReverieAccents = LithosAccents
// Note: GlassColors.ReverieAccent already defined as alias to LithosAmber
