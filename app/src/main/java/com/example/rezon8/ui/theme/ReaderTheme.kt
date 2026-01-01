package com.mossglen.lithos.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * LITHOS AMBER E-Reader Theme System
 *
 * Reading modes use natural, eye-friendly colors:
 * - Oat (warm cream) for comfortable light reading
 * - Moss accent for reading highlights
 * - Amber accent for audio/playback elements
 *
 * Design Philosophy:
 * - NO neon/glowing effects
 * - Matte/satin finishes
 * - Natural materials aesthetic
 */

// ============================================================================
// READER THEME ENUM
// ============================================================================

enum class ReaderThemeType {
    APP_DEFAULT,  // Follows main app theme (Light/Dark/OLED) - DEFAULT
    PAPER,        // Warm Oat background - comfortable light reading
    SEPIA,        // Classic sepia - reduces eye strain
    AMBER,        // Warm golden tone - evening reading
    NIGHT,        // OLED black - dark environments
    DARK_GRAY,    // Slate background - softer dark mode
    CUSTOM        // User-defined colors
}

// ============================================================================
// READER FONT OPTIONS
// ============================================================================

enum class ReaderFont(val displayName: String, val fontFamily: FontFamily) {
    SERIF("Serif", FontFamily.Serif),
    SANS_SERIF("Sans Serif", FontFamily.SansSerif),
    MONOSPACE("Monospace", FontFamily.Monospace),
    SYSTEM("System Default", FontFamily.Default)
}

// ============================================================================
// READER THEME DATA
// ============================================================================

@Immutable
data class ReaderThemeData(
    val name: String,
    val backgroundColor: Color,
    val textColor: Color,
    val textSecondaryColor: Color,
    val accentColor: Color,
    val linkColor: Color,
    val highlightColor: Color,
    val controlsBackground: Color,
    val controlsText: Color,
    val dividerColor: Color,
    val isDark: Boolean,
    // ===== Lithos Glass Properties - Matte, no glow =====
    val glassSurface: Color = controlsBackground,
    val glassBlur: Float = 20f,                           // Standard 20dp blur
    val glassBorder: Color = accentColor.copy(alpha = 0.15f), // Subtle border, reduced
    val glassShadow: Color = Color.Black.copy(alpha = 0.08f), // Minimal shadow
    val glassHighlight: Color = Color.White.copy(alpha = 0.03f), // Very subtle highlight
    val pillBackground: Color = controlsBackground.copy(alpha = 0.92f),
    val pillBorder: Color = accentColor.copy(alpha = 0.20f),
    val cardGlow: Color = Color.Transparent,              // NO glow per Lithos spec
    val statusBarOverlay: Color = backgroundColor.copy(alpha = 0.8f)
)

// ============================================================================
// PRE-DEFINED READER THEMES - Lithos Design Language
// ============================================================================

object ReaderThemes {

    // Moss accent for reading (natural, forest-inspired)
    private val MossAccent = LithosMoss
    private val MossAccentLight = LithosMossLight

    /**
     * APP DEFAULT - Light mode (Lithos Oat)
     * Uses the main app's light theme colors for reading
     */
    val AppDefaultLight = ReaderThemeData(
        name = "App Light",
        backgroundColor = LithosOat,
        textColor = LithosTextPrimaryLight,
        textSecondaryColor = LithosTextSecondaryLight,
        accentColor = LithosAmber,
        linkColor = LithosAmber,
        highlightColor = LithosAmber.copy(alpha = 0.30f),
        controlsBackground = LithosSurfaceLight,
        controlsText = LithosTextPrimaryLight,
        dividerColor = LithosDividerLight,
        isDark = false,
        glassSurface = LithosSurfaceLight.copy(alpha = 0.92f),
        glassBorder = Color.Black.copy(alpha = 0.08f),
        glassHighlight = Color.White.copy(alpha = 0.3f),
        pillBackground = Color(0xFFF2F2F7).copy(alpha = 0.95f),
        pillBorder = Color.Black.copy(alpha = 0.08f),
        cardGlow = Color.Transparent
    )

    /**
     * APP DEFAULT - Dark mode (Lithos Slate)
     * Uses the main app's dark theme colors for reading
     */
    val AppDefaultDark = ReaderThemeData(
        name = "App Dark",
        backgroundColor = LithosSlate,
        textColor = LithosTextPrimary.copy(alpha = 0.87f),
        textSecondaryColor = LithosTextSecondary,
        accentColor = LithosAmber,
        linkColor = LithosAmber,
        highlightColor = LithosAmber.copy(alpha = 0.25f),
        controlsBackground = LithosSurfaceDark,
        controlsText = LithosTextPrimary.copy(alpha = 0.87f),
        dividerColor = LithosDivider,
        isDark = true,
        glassSurface = LithosSurfaceDark.copy(alpha = 0.88f),
        glassBorder = Color.Black.copy(alpha = 0.08f),
        glassHighlight = Color.White.copy(alpha = 0.02f),
        pillBackground = Color(0xFF1C1C1E).copy(alpha = 0.95f),
        pillBorder = Color.Black.copy(alpha = 0.08f),
        cardGlow = Color.Transparent
    )

    /**
     * APP DEFAULT - OLED mode (Lithos Black)
     * Uses the main app's OLED theme colors for reading
     */
    val AppDefaultOLED = ReaderThemeData(
        name = "App OLED",
        backgroundColor = LithosBlack,
        textColor = Color(0xFFE0E0E0),
        textSecondaryColor = Color(0xFF9E9E9E),
        accentColor = LithosAmber,
        linkColor = LithosAmber,
        highlightColor = LithosAmber.copy(alpha = 0.25f),
        controlsBackground = Color(0xFF0A0A0A),
        controlsText = Color(0xFFE0E0E0),
        dividerColor = Color(0xFFFFFFFF).copy(alpha = 0.06f),
        isDark = true,
        glassSurface = Color(0xFF0A0A0A).copy(alpha = 0.85f),
        glassBorder = Color.Black.copy(alpha = 0.08f),
        glassHighlight = LithosAmber.copy(alpha = 0.04f),
        pillBackground = Color(0xFF0D0D0D).copy(alpha = 0.95f),
        pillBorder = Color.Black.copy(alpha = 0.08f),
        cardGlow = Color.Transparent
    )

    /**
     * Get app default theme based on main app settings
     */
    fun getAppDefaultTheme(isDark: Boolean, isOLED: Boolean): ReaderThemeData {
        return when {
            isOLED -> AppDefaultOLED
            isDark -> AppDefaultDark
            else -> AppDefaultLight
        }
    }

    /**
     * PAPER - Lithos Oat background
     * Warm cream for comfortable daytime reading
     */
    val Paper = ReaderThemeData(
        name = "Paper",
        backgroundColor = LithosOat,
        textColor = LithosTextPrimaryLight,
        textSecondaryColor = LithosTextSecondaryLight,
        accentColor = MossAccent,
        linkColor = MossAccent,
        highlightColor = LithosAmber.copy(alpha = 0.30f),
        controlsBackground = LithosSurfaceLight,
        controlsText = LithosTextPrimaryLight,
        dividerColor = LithosDividerLight,
        isDark = false,
        // Lithos glass - matte finish
        glassSurface = LithosSurfaceLight.copy(alpha = 0.92f),
        glassBorder = MossAccent.copy(alpha = 0.12f),
        glassHighlight = Color.White.copy(alpha = 0.3f),
        pillBackground = Color(0xFFF2F2F7).copy(alpha = 0.95f),
        pillBorder = MossAccent.copy(alpha = 0.15f),
        cardGlow = Color.Transparent
    )

    /**
     * SEPIA - Classic e-reader sepia
     * Reduces blue light, easier on eyes
     */
    val Sepia = ReaderThemeData(
        name = "Sepia",
        backgroundColor = Color(0xFFF4E4C9),
        textColor = Color(0xFF5C4B37),
        textSecondaryColor = Color(0xFF7A6B5C),
        accentColor = LithosAmberDark,
        linkColor = LithosAmberDark,
        highlightColor = LithosAmber.copy(alpha = 0.40f),
        controlsBackground = Color(0xFFE8D4B5),
        controlsText = Color(0xFF5C4B37),
        dividerColor = Color(0xFF5C4B37).copy(alpha = 0.10f),
        isDark = false,
        // Lithos glass - warm matte
        glassSurface = Color(0xFFECD9BC).copy(alpha = 0.94f),
        glassBorder = Color.Black.copy(alpha = 0.08f),
        glassHighlight = Color(0xFFFFF8E1).copy(alpha = 0.4f),
        pillBackground = Color(0xFFF0DCC2).copy(alpha = 0.96f),
        pillBorder = Color.Black.copy(alpha = 0.08f),
        cardGlow = Color.Transparent
    )

    /**
     * AMBER - Warm golden tone for evening reading
     * Maximum blue light reduction
     */
    val Amber = ReaderThemeData(
        name = "Amber",
        backgroundColor = Color(0xFFFFF4E0),
        textColor = Color(0xFF4A3C2A),
        textSecondaryColor = Color(0xFF6B5A44),
        accentColor = LithosAmber,
        linkColor = LithosAmberDark,
        highlightColor = LithosAmberLight.copy(alpha = 0.45f),
        controlsBackground = Color(0xFFF5E6C8),
        controlsText = Color(0xFF4A3C2A),
        dividerColor = Color(0xFF4A3C2A).copy(alpha = 0.08f),
        isDark = false,
        // Lithos glass - golden matte
        glassSurface = Color(0xFFFAEDD5).copy(alpha = 0.94f),
        glassBorder = Color.Black.copy(alpha = 0.08f),
        glassHighlight = Color(0xFFFFFBF0).copy(alpha = 0.5f),
        pillBackground = Color(0xFFFFF0D6).copy(alpha = 0.96f),
        pillBorder = Color.Black.copy(alpha = 0.08f),
        cardGlow = Color.Transparent
    )

    /**
     * NIGHT - Lithos Black (OLED)
     * True black for dark environments, battery saving
     */
    val Night = ReaderThemeData(
        name = "Night",
        backgroundColor = LithosBlack,
        textColor = Color(0xFFE0E0E0),
        textSecondaryColor = Color(0xFF9E9E9E),
        accentColor = MossAccentLight,
        linkColor = MossAccentLight,
        highlightColor = MossAccent.copy(alpha = 0.25f),
        controlsBackground = Color(0xFF0A0A0A),
        controlsText = Color(0xFFE0E0E0),
        dividerColor = Color(0xFFFFFFFF).copy(alpha = 0.06f),
        isDark = true,
        // Lithos glass - deep matte
        glassSurface = Color(0xFF0A0A0A).copy(alpha = 0.85f),
        glassBorder = MossAccentLight.copy(alpha = 0.20f),
        glassHighlight = MossAccentLight.copy(alpha = 0.04f),
        pillBackground = Color(0xFF0D0D0D).copy(alpha = 0.95f),
        pillBorder = MossAccentLight.copy(alpha = 0.25f),
        cardGlow = Color.Transparent
    )

    /**
     * DARK GRAY - Lithos Slate background
     * Softer dark mode, easier on eyes than pure black
     */
    val DarkGray = ReaderThemeData(
        name = "Dark Gray",
        backgroundColor = LithosSlate,
        textColor = LithosTextPrimary.copy(alpha = 0.87f),
        textSecondaryColor = LithosTextSecondary,
        accentColor = MossAccentLight,
        linkColor = MossAccentLight,
        highlightColor = MossAccent.copy(alpha = 0.25f),
        controlsBackground = LithosSurfaceDark,
        controlsText = LithosTextPrimary.copy(alpha = 0.87f),
        dividerColor = LithosDivider,
        isDark = true,
        // Lithos glass - slate matte
        glassSurface = LithosSurfaceDark.copy(alpha = 0.88f),
        glassBorder = MossAccentLight.copy(alpha = 0.15f),
        glassHighlight = Color.White.copy(alpha = 0.02f),
        pillBackground = Color(0xFF1C1C1E).copy(alpha = 0.95f),
        pillBorder = MossAccentLight.copy(alpha = 0.20f),
        cardGlow = Color.Transparent
    )

    /**
     * Get theme by type (for non-APP_DEFAULT themes)
     */
    fun getTheme(type: ReaderThemeType, customTheme: ReaderThemeData? = null): ReaderThemeData {
        return when (type) {
            ReaderThemeType.APP_DEFAULT -> AppDefaultLight // Fallback, use getThemeWithAppSettings for proper handling
            ReaderThemeType.PAPER -> Paper
            ReaderThemeType.SEPIA -> Sepia
            ReaderThemeType.AMBER -> Amber
            ReaderThemeType.NIGHT -> Night
            ReaderThemeType.DARK_GRAY -> DarkGray
            ReaderThemeType.CUSTOM -> customTheme ?: Paper
        }
    }

    /**
     * Get theme by type with app settings for APP_DEFAULT support
     * Use this when you have access to main app theme settings
     */
    fun getThemeWithAppSettings(
        type: ReaderThemeType,
        isDark: Boolean,
        isOLED: Boolean,
        customTheme: ReaderThemeData? = null
    ): ReaderThemeData {
        return when (type) {
            ReaderThemeType.APP_DEFAULT -> getAppDefaultTheme(isDark, isOLED)
            else -> getTheme(type, customTheme)
        }
    }

    /**
     * All available themes for picker
     * APP_DEFAULT shown first as the recommended option
     */
    val allThemes = listOf(
        ReaderThemeType.APP_DEFAULT to AppDefaultLight, // Preview uses light, actual follows app
        ReaderThemeType.PAPER to Paper,
        ReaderThemeType.SEPIA to Sepia,
        ReaderThemeType.AMBER to Amber,
        ReaderThemeType.DARK_GRAY to DarkGray,
        ReaderThemeType.NIGHT to Night
    )

    /**
     * Get all themes with proper APP_DEFAULT preview based on current app settings
     */
    fun getAllThemesWithAppSettings(isDark: Boolean, isOLED: Boolean): List<Pair<ReaderThemeType, ReaderThemeData>> {
        return listOf(
            ReaderThemeType.APP_DEFAULT to getAppDefaultTheme(isDark, isOLED),
            ReaderThemeType.PAPER to Paper,
            ReaderThemeType.SEPIA to Sepia,
            ReaderThemeType.AMBER to Amber,
            ReaderThemeType.DARK_GRAY to DarkGray,
            ReaderThemeType.NIGHT to Night
        )
    }
}

// ============================================================================
// READER SETTINGS DATA CLASS
// ============================================================================

@Immutable
data class ReaderSettings(
    val themeType: ReaderThemeType = ReaderThemeType.APP_DEFAULT, // Defaults to main app theme
    val fontSize: Float = 18f,
    val lineHeight: Float = 1.6f,
    val fontFamily: ReaderFont = ReaderFont.SERIF,
    val marginHorizontal: Int = 20,
    val marginVertical: Int = 16,
    val textAlign: ReaderTextAlign = ReaderTextAlign.LEFT,
    val customTheme: ReaderThemeData? = null
) {
    /**
     * Get theme (legacy - use getThemeWithAppSettings for proper APP_DEFAULT handling)
     */
    val theme: ReaderThemeData
        get() = ReaderThemes.getTheme(themeType, customTheme)

    /**
     * Get theme with main app settings for proper APP_DEFAULT support
     * Call this when you have access to isDark and isOLED from the main app
     */
    fun getThemeWithAppSettings(isDark: Boolean, isOLED: Boolean): ReaderThemeData {
        return ReaderThemes.getThemeWithAppSettings(themeType, isDark, isOLED, customTheme)
    }
}

enum class ReaderTextAlign {
    LEFT,
    JUSTIFY,
    CENTER
}

// ============================================================================
// FONT SIZE PRESETS
// ============================================================================

object ReaderFontSizes {
    val presets = listOf(
        14f to "Small",
        16f to "Medium",
        18f to "Default",
        20f to "Large",
        24f to "X-Large",
        28f to "XX-Large"
    )

    const val MIN = 12f
    const val MAX = 32f
    const val STEP = 2f
}
