package com.mossglen.lithos.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * LITHOS AMBER Design System - Color Palette
 *
 * Design Philosophy:
 * - NO neon/glowing effects
 * - Matte/satin finishes
 * - Natural materials: Stone (Slate), Fossilized Resin (Amber), Forest (Moss)
 * - Progress rings use THIN strokes (2-3px)
 * - Glass effects use frosted blur (20dp) not shiny gradients
 */

// ============================================================================
// LITHOS AMBER PRIMARY PALETTE
// ============================================================================

/**
 * Primary Accent - Amber
 * Inspired by fossilized resin, warm honey tones
 * Use for: Primary actions, accents, highlights, interactive elements
 */
val LithosAmber = Color(0xFFD48C2C)
val LithosAmberLight = Color(0xFFE6A84D)
val LithosAmberDark = Color(0xFFB57420)

/**
 * Primary Action - Moss
 * ONLY for Play/Pause button - represents nature, calm, forest
 * Use for: Play/Pause button ONLY
 * Darkened for better contrast (was #4A5D45)
 */
val LithosMoss = Color(0xFF3D4F39)           // Darker moss for better contrast
val LithosMossLight = Color(0xFF4A5D45)      // Previous main now light variant
val LithosMossDark = Color(0xFF2F3D2C)       // Deep forest

// ============================================================================
// LITHOS BACKGROUNDS
// ============================================================================

/**
 * Slate - Standard Dark background
 * Inspired by natural stone, neutral and grounding
 */
val LithosSlate = Color(0xFF1A1D21)

/**
 * Oat - Reader Light background
 * Warm, natural paper-like tone for comfortable reading
 */
val LithosOat = Color(0xFFF2F0E9)

/**
 * Black - OLED Night mode
 * True black for maximum contrast and battery saving
 */
val LithosBlack = Color(0xFF000000)

// ============================================================================
// LITHOS GLASS EFFECTS
// ============================================================================

/**
 * Glass surface with frosted blur effect
 * 85% opacity slate - use with 20dp blur
 */
val LithosGlass = Color(0xD91A1D21)  // rgba(26, 29, 33, 0.85)

/**
 * Glass border - subtle 10% white edge
 * Creates depth without shiny gradients
 */
val LithosGlassBorder = Color(0x1AFFFFFF)  // 10% white

/**
 * Glass light mode variant
 */
val LithosGlassLight = Color(0xD9F2F0E9)  // rgba(242, 240, 233, 0.85)
val LithosGlassBorderLight = Color(0x1A000000)  // 10% black

// ============================================================================
// LITHOS TEXT COLORS
// ============================================================================

// Dark mode text (on Slate/Black backgrounds)
val LithosTextPrimary = Color(0xFFFFFFFF)
val LithosTextSecondary = Color(0xB3FFFFFF)  // 70% white
val LithosTextTertiary = Color(0x80FFFFFF)   // 50% white

// Light mode text (on Oat background)
val LithosTextPrimaryLight = Color(0xFF1A1D21)
val LithosTextSecondaryLight = Color(0xB31A1D21)  // 70% slate
val LithosTextTertiaryLight = Color(0x801A1D21)   // 50% slate

// ============================================================================
// LITHOS SEMANTIC COLORS
// ============================================================================

val LithosSuccess = Color(0xFF4A5D45)       // Moss-based success (natural, not neon)
val LithosWarning = Color(0xFFD48C2C)       // Amber warning
val LithosError = Color(0xFFB54D4D)         // Muted terracotta red (natural, not neon)
val LithosInfo = Color(0xFF4D6B8C)          // Muted slate blue

// ============================================================================
// LITHOS SURFACE COLORS
// ============================================================================

val LithosSurfaceDark = Color(0xFF22262B)         // Elevated surface (dark)
val LithosSurfaceDarkElevated = Color(0xFF2A2F35) // Higher elevation (dark)
val LithosSurfaceLight = Color(0xFFE8E6DF)        // Elevated surface (light)
val LithosSurfaceLightElevated = Color(0xFFDEDCD5) // Higher elevation (light)

// ============================================================================
// LITHOS DIVIDERS
// ============================================================================

val LithosDivider = Color(0x1AFFFFFF)      // 10% white for dark mode
val LithosDividerLight = Color(0x1A1A1D21) // 10% slate for light mode

// ============================================================================
// LITHOS SELECTION & HIGHLIGHT COLORS
// ============================================================================

/**
 * Selection background - NEUTRAL slate, not brown/orange
 * Use for: Selected items, highlighted cards, focused elements
 * Text/icons on selected items should use Amber
 */
val LithosSelectionDark = Color(0xFF2A2F35)        // Elevated slate - neutral selection
val LithosSelectionLight = Color(0xFFE0DED7)       // Elevated oat - neutral selection
val LithosSelectionOLED = Color(0xFF1A1D21)        // Slate on black - neutral selection

/**
 * Selection border - Amber accent for focus indication
 */
val LithosSelectionBorder = Color(0x4DD48C2C)      // 30% Amber for subtle border
val LithosSelectionBorderStrong = Color(0x80D48C2C) // 50% Amber for strong border

// ============================================================================
// LITHOS PROGRESS RING COLORS
// ============================================================================

/**
 * Progress ring track (background) - very subtle
 * Use with 2-3px stroke width for thin, elegant rings
 */
val LithosProgressTrack = Color(0x33FFFFFF)  // 20% white
val LithosProgressTrackLight = Color(0x331A1D21)  // 20% slate

// ============================================================================
// LITHOS COLORS OBJECT - Comprehensive access
// ============================================================================

object LithosColors {
    // Primary Accent - Amber
    val Amber = LithosAmber
    val AmberLight = LithosAmberLight
    val AmberDark = LithosAmberDark

    // Primary Action - Moss (ONLY for Play/Pause)
    val Moss = LithosMoss
    val MossLight = LithosMossLight
    val MossDark = LithosMossDark

    // Backgrounds
    val Slate = LithosSlate
    val Oat = LithosOat
    val Black = LithosBlack

    // Glass Effects
    val Glass = LithosGlass
    val GlassBorder = LithosGlassBorder
    val GlassLight = LithosGlassLight
    val GlassBorderLight = LithosGlassBorderLight

    // Legacy aliases for backward compatibility
    val FrostedGlass = LithosGlass
    val FrostedGlassLight = LithosGlassLight
    val BorderMatte = LithosGlassBorder

    // Text - Dark Mode
    val TextPrimary = LithosTextPrimary
    val TextSecondary = LithosTextSecondary
    val TextTertiary = LithosTextTertiary

    // Text - Light Mode
    val TextPrimaryLight = LithosTextPrimaryLight
    val TextSecondaryLight = LithosTextSecondaryLight
    val TextTertiaryLight = LithosTextTertiaryLight

    // Semantic
    val Success = LithosSuccess
    val Warning = LithosWarning
    val Error = LithosError
    val Info = LithosInfo

    // Surfaces
    val SurfaceDark = LithosSurfaceDark
    val SurfaceElevatedDark = LithosSurfaceDarkElevated
    val SurfaceLight = LithosSurfaceLight
    val SurfaceElevatedLight = LithosSurfaceLightElevated

    // Dividers
    val Divider = LithosDivider
    val DividerLight = LithosDividerLight

    // Progress Rings (thin 2-3px strokes)
    val ProgressTrack = LithosProgressTrack
    val ProgressTrackLight = LithosProgressTrackLight

    // Selection & Highlights (NEUTRAL slate backgrounds, Amber for text/icons)
    val SelectionDark = LithosSelectionDark
    val SelectionLight = LithosSelectionLight
    val SelectionOLED = LithosSelectionOLED
    val SelectionBorder = LithosSelectionBorder
    val SelectionBorderStrong = LithosSelectionBorderStrong

    // ========== HELPER FUNCTIONS ==========

    /**
     * Get appropriate background color based on theme mode
     */
    fun background(isDark: Boolean, isOLED: Boolean = false): Color = when {
        isOLED -> Black
        isDark -> Slate
        else -> Oat
    }

    /**
     * Get appropriate text color based on theme mode
     */
    fun textPrimary(isDark: Boolean): Color =
        if (isDark) TextPrimary else TextPrimaryLight

    fun textSecondary(isDark: Boolean): Color =
        if (isDark) TextSecondary else TextSecondaryLight

    fun textTertiary(isDark: Boolean): Color =
        if (isDark) TextTertiary else TextTertiaryLight

    /**
     * Get appropriate glass color based on theme mode
     */
    fun glass(isDark: Boolean): Color =
        if (isDark) Glass else GlassLight

    fun glassBorder(isDark: Boolean): Color =
        if (isDark) GlassBorder else GlassBorderLight

    /**
     * Get appropriate surface color based on theme mode
     */
    fun surface(isDark: Boolean): Color =
        if (isDark) SurfaceDark else SurfaceLight

    fun surfaceElevated(isDark: Boolean): Color =
        if (isDark) SurfaceElevatedDark else SurfaceElevatedLight

    /**
     * Get appropriate divider color based on theme mode
     */
    fun divider(isDark: Boolean): Color =
        if (isDark) Divider else DividerLight

    /**
     * Get appropriate selection background color based on theme mode
     * ALWAYS use neutral slate tones, never brown/orange
     */
    fun selection(isDark: Boolean, isOLED: Boolean = false): Color = when {
        isOLED -> SelectionOLED
        isDark -> SelectionDark
        else -> SelectionLight
    }

    /**
     * Get progress track color based on theme mode
     */
    fun progressTrack(isDark: Boolean): Color =
        if (isDark) ProgressTrack else ProgressTrackLight

    /**
     * Interactive states - matte finishes, no glow
     */
    fun amberPressed(): Color = Amber.copy(alpha = 0.80f)
    fun amberSubtle(): Color = Amber.copy(alpha = 0.15f)
    fun mossPressed(): Color = Moss.copy(alpha = 0.80f)
    fun mossSubtle(): Color = Moss.copy(alpha = 0.15f)
}

// ============================================================================
// LEGACY COMPATIBILITY - Maps old names to Lithos equivalents
// ============================================================================

// For backwards compatibility with existing code
val MossGreen80 = LithosMossLight
val MossGreen40 = LithosMoss
val MossGreenGrey80 = Color(0xFFC2C8BE)
val MossGreenGrey40 = Color(0xFF5A6B52)
val Whisky80 = LithosAmberLight
val Whisky40 = LithosAmberDark
