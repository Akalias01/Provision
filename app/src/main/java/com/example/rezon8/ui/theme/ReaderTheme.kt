package com.mossglen.reverie.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Reverie E-Reader Theme System
 *
 * Per PROJECT_MANIFEST:
 * "Audiobooks = Always dark, Slate Gray accent"
 * "Ebooks = User chooses theme, Moss Green accent"
 *
 * This provides dedicated reading themes that feel like a book reader,
 * not an audiobook player.
 */

// ============================================================================
// READER THEME ENUM
// ============================================================================

enum class ReaderThemeType {
    PAPER,      // Warm cream background, dark text - like physical paper
    SEPIA,      // Classic sepia like Kindle - reduces eye strain
    NIGHT,      // OLED black for dark environments
    DARK_GRAY,  // Softer dark mode - easier on eyes than pure black
    CUSTOM      // User-defined colors
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
    // ===== Premium Glass Morphism Properties =====
    val glassSurface: Color = controlsBackground,          // Glass overlay surface
    val glassBlur: Float = 20f,                            // Blur intensity
    val glassBorder: Color = accentColor.copy(alpha = 0.2f), // Subtle accent border
    val glassShadow: Color = Color.Black.copy(alpha = 0.1f), // Soft shadow
    val glassHighlight: Color = Color.White.copy(alpha = 0.05f), // Top edge highlight
    val pillBackground: Color = controlsBackground.copy(alpha = 0.95f), // Pill nav background
    val pillBorder: Color = accentColor.copy(alpha = 0.25f), // Pill accent border
    val cardGlow: Color = accentColor.copy(alpha = 0.08f), // Subtle glow behind cards
    val statusBarOverlay: Color = backgroundColor.copy(alpha = 0.8f) // Status bar blend
)

// ============================================================================
// PRE-DEFINED READER THEMES
// ============================================================================

object ReaderThemes {

    // Moss Green accent for reading (per manifest)
    private val MossGreen = Color(0xFF6B7F5E)
    private val MossGreenLight = Color(0xFF8FA07E)

    /**
     * PAPER - Warm cream background like a physical book page
     * Default for daytime reading
     * Premium glass: Subtle cream glass with moss green accents
     */
    val Paper = ReaderThemeData(
        name = "Paper",
        backgroundColor = Color(0xFFFAF7F2),      // Warm cream
        textColor = Color(0xFF1C1C1E),            // Near black
        textSecondaryColor = Color(0xFF3C3C43).copy(alpha = 0.60f),
        accentColor = MossGreen,
        linkColor = MossGreen,
        highlightColor = Color(0xFFFFEB3B).copy(alpha = 0.40f),  // Yellow highlight
        controlsBackground = Color(0xFFEDEAE5),   // Slightly darker cream
        controlsText = Color(0xFF1C1C1E),
        dividerColor = Color(0xFF1C1C1E).copy(alpha = 0.08f),
        isDark = false,
        // Premium glass properties
        glassSurface = Color(0xFFF5F2ED).copy(alpha = 0.92f),
        glassBorder = MossGreen.copy(alpha = 0.18f),
        glassHighlight = Color.White.copy(alpha = 0.4f),
        pillBackground = Color(0xFFF8F5F0).copy(alpha = 0.96f),
        pillBorder = MossGreen.copy(alpha = 0.22f),
        cardGlow = MossGreen.copy(alpha = 0.06f)
    )

    /**
     * SEPIA - Classic e-reader sepia tone
     * Reduces blue light, easier on eyes for extended reading
     * Premium glass: Warm amber glass with golden accents
     */
    val Sepia = ReaderThemeData(
        name = "Sepia",
        backgroundColor = Color(0xFFF4E4C9),      // Sepia/parchment
        textColor = Color(0xFF5C4B37),            // Dark brown
        textSecondaryColor = Color(0xFF7A6B5C),   // Medium brown
        accentColor = Color(0xFF8B6914),          // Amber/brown accent
        linkColor = Color(0xFF6B5A14),
        highlightColor = Color(0xFFFFD54F).copy(alpha = 0.50f),  // Warm yellow
        controlsBackground = Color(0xFFE8D4B5),   // Darker sepia
        controlsText = Color(0xFF5C4B37),
        dividerColor = Color(0xFF5C4B37).copy(alpha = 0.12f),
        isDark = false,
        // Premium glass properties
        glassSurface = Color(0xFFECD9BC).copy(alpha = 0.94f),
        glassBorder = Color(0xFF8B6914).copy(alpha = 0.20f),
        glassHighlight = Color(0xFFFFF8E1).copy(alpha = 0.5f),
        pillBackground = Color(0xFFF0DCC2).copy(alpha = 0.96f),
        pillBorder = Color(0xFFB8860B).copy(alpha = 0.25f),
        cardGlow = Color(0xFFB8860B).copy(alpha = 0.08f)
    )

    /**
     * NIGHT - Pure OLED black for dark environments
     * Maximum contrast, battery saving on OLED
     * Premium glass: Deep black glass with vibrant moss green glow
     */
    val Night = ReaderThemeData(
        name = "Night",
        backgroundColor = Color(0xFF000000),      // True black
        textColor = Color(0xFFE0E0E0),            // Light gray (not pure white)
        textSecondaryColor = Color(0xFF9E9E9E),   // Medium gray
        accentColor = MossGreenLight,
        linkColor = MossGreenLight,
        highlightColor = MossGreen.copy(alpha = 0.30f),
        controlsBackground = Color(0xFF1C1C1E),   // Dark gray
        controlsText = Color(0xFFE0E0E0),
        dividerColor = Color(0xFFFFFFFF).copy(alpha = 0.08f),
        isDark = true,
        // Premium glass properties - vibrant accents on OLED black
        glassSurface = Color(0xFF0A0A0A).copy(alpha = 0.85f),
        glassBorder = MossGreenLight.copy(alpha = 0.30f),
        glassHighlight = MossGreenLight.copy(alpha = 0.08f),
        pillBackground = Color(0xFF0D0D0D).copy(alpha = 0.92f),
        pillBorder = MossGreenLight.copy(alpha = 0.35f),
        cardGlow = MossGreenLight.copy(alpha = 0.12f)
    )

    /**
     * DARK GRAY - Softer dark mode
     * Easier on eyes than pure black, still good for low light
     * Premium glass: Sophisticated charcoal glass with subtle green accents
     */
    val DarkGray = ReaderThemeData(
        name = "Dark Gray",
        backgroundColor = Color(0xFF1A1A1A),      // Dark gray
        textColor = Color(0xFFD0D0D0),            // Soft white
        textSecondaryColor = Color(0xFF8A8A8A),   // Medium gray
        accentColor = MossGreenLight,
        linkColor = MossGreenLight,
        highlightColor = MossGreen.copy(alpha = 0.30f),
        controlsBackground = Color(0xFF2C2C2E),
        controlsText = Color(0xFFD0D0D0),
        dividerColor = Color(0xFFFFFFFF).copy(alpha = 0.10f),
        isDark = true,
        // Premium glass properties - sophisticated charcoal
        glassSurface = Color(0xFF1E1E20).copy(alpha = 0.88f),
        glassBorder = MossGreenLight.copy(alpha = 0.22f),
        glassHighlight = Color.White.copy(alpha = 0.04f),
        pillBackground = Color(0xFF222224).copy(alpha = 0.94f),
        pillBorder = MossGreenLight.copy(alpha = 0.28f),
        cardGlow = MossGreenLight.copy(alpha = 0.10f)
    )

    /**
     * Get theme by type
     */
    fun getTheme(type: ReaderThemeType, customTheme: ReaderThemeData? = null): ReaderThemeData {
        return when (type) {
            ReaderThemeType.PAPER -> Paper
            ReaderThemeType.SEPIA -> Sepia
            ReaderThemeType.NIGHT -> Night
            ReaderThemeType.DARK_GRAY -> DarkGray
            ReaderThemeType.CUSTOM -> customTheme ?: Paper
        }
    }

    /**
     * All available themes for picker
     */
    val allThemes = listOf(
        ReaderThemeType.PAPER to Paper,
        ReaderThemeType.SEPIA to Sepia,
        ReaderThemeType.DARK_GRAY to DarkGray,
        ReaderThemeType.NIGHT to Night
    )
}

// ============================================================================
// READER SETTINGS DATA CLASS
// ============================================================================

@Immutable
data class ReaderSettings(
    val themeType: ReaderThemeType = ReaderThemeType.PAPER,
    val fontSize: Float = 18f,
    val lineHeight: Float = 1.6f,
    val fontFamily: ReaderFont = ReaderFont.SERIF,
    val marginHorizontal: Int = 20,
    val marginVertical: Int = 16,
    val textAlign: ReaderTextAlign = ReaderTextAlign.LEFT,
    val customTheme: ReaderThemeData? = null
) {
    val theme: ReaderThemeData
        get() = ReaderThemes.getTheme(themeType, customTheme)
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
