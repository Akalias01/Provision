package com.mossglen.reverie.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Theme Mode - System follows device, Dark/Light are manual overrides
 * Reverie Dark is a premium night mode with warm burnt orange accents
 * Reading is an amber/sepia mode optimized for eye comfort during extended reading
 */
enum class ThemeMode(val displayName: String, val description: String) {
    SYSTEM("System", "Follows device settings"),
    DARK("Dark", "Standard dark theme"),
    LIGHT("Light", "Standard light theme"),
    REVERIE_DARK("Reverie Dark", "Night mode with warm accents"),
    READING("Reading", "Warm amber for eye comfort")
}

/**
 * Accent color variants for Reverie theme
 * Dark Copper (#87481F) for TEXT and ICON accents
 * Medium Dark Slate (#2D3339) for BACKGROUNDS and selections (no warm fills)
 */
enum class ReverieAccentVariant(
    val displayName: String,
    val accentColor: Color,        // For text, icons, highlights
    val highlightColor: Color,     // For selection backgrounds/borders
    val useBorderHighlight: Boolean
) {
    // Dark copper text/icons with medium dark slate backgrounds
    COPPER_GLASS(
        displayName = "Copper/Glass",
        accentColor = Color(0xFF87481F),  // Dark copper for TEXT/ICONS
        highlightColor = Color(0xFF2D3339),  // Medium dark slate for backgrounds
        useBorderHighlight = false
    ),
    // Dark copper text/icons with slate border highlights
    COPPER_BORDER(
        displayName = "Copper/Border",
        accentColor = Color(0xFF87481F),  // Dark copper for TEXT/ICONS
        highlightColor = Color(0xFF3D444D).copy(alpha = 0.60f),  // Slate border
        useBorderHighlight = true
    )
}

/**
 * App Icon Theme - Dark (white R) or Light (black R)
 */
enum class AppIconTheme(val displayName: String) {
    DARK("Dark"),
    LIGHT("Light")
}

/**
 * 6 Theme Options:
 * - Dark: Standard dark theme with white text logo
 * - Light: Standard light theme with black text logo
 * - Reverie Dark: Premium dark theme with Reverie color palette and colorful logo
 * - Reverie Light: Premium light theme with Reverie color palette and colorful logo
 * - Reading: Warm amber/sepia dark theme for eye comfort during extended reading
 * - Reading Light: Warm parchment theme for daytime reading
 */
enum class AppTheme(
    val displayName: String,
    val isDark: Boolean,
    val isReverie: Boolean,
    val isReading: Boolean,
    val accentColors: List<Color>
) {
    DARK(
        displayName = "Dark",
        isDark = true,
        isReverie = false,
        isReading = false,
        accentColors = listOf(Color(0xFF87481F), Color(0xFF87481F)) // Dark copper for text/icons
    ),
    LIGHT(
        displayName = "Light",
        isDark = false,
        isReverie = false,
        isReading = false,
        accentColors = listOf(Color(0xFF6D3A19), Color(0xFF6D3A19)) // Darker copper for light
    ),
    REVERIE_DARK(
        displayName = "Reverie",
        isDark = true,
        isReverie = true,
        isReading = false,
        accentColors = listOf(Color(0xFF87481F), Color(0xFF87481F)) // Dark copper - premium
    ),
    REVERIE_LIGHT(
        displayName = "Reverie Light",
        isDark = false,
        isReverie = true,
        isReading = false,
        accentColors = listOf(Color(0xFF6D3A19), Color(0xFF6D3A19)) // Darker copper for light
    ),
    READING(
        displayName = "Reading",
        isDark = true,
        isReverie = false,
        isReading = true,
        accentColors = listOf(Color(0xFF6B7F5E), Color(0xFF6B7F5E)) // Moss Green for reading mode
    ),
    READING_LIGHT(
        displayName = "Reading Light",
        isDark = false,
        isReverie = false,
        isReading = true,
        accentColors = listOf(Color(0xFF5A6B4E), Color(0xFF5A6B4E)) // Darker Moss for light reading
    )
}

// Keep DisplayMode for backward compatibility but simplify
enum class DisplayMode {
    DARK, LIGHT, READING
}

@HiltViewModel
class ThemeViewModel @Inject constructor() : ViewModel() {

    var appTheme = mutableStateOf(AppTheme.REVERIE_DARK)
        private set

    // Theme mode: Default to Reverie Dark for premium first impression
    // Users can change to System, Dark, or Light if they prefer
    var themeMode = mutableStateOf(ThemeMode.REVERIE_DARK)
        private set

    // Reverie accent color variant (Dark copper text with slate backgrounds)
    var reverieAccentVariant = mutableStateOf(ReverieAccentVariant.COPPER_BORDER)
        private set

    // App icon theme: Dark (white R) or Light (black R)
    var appIconTheme = mutableStateOf(AppIconTheme.DARK)
        private set

    // Legacy displayMode for components that still use it
    val displayMode: DisplayMode
        get() = when {
            appTheme.value.isReading -> DisplayMode.READING
            appTheme.value.isDark -> DisplayMode.DARK
            else -> DisplayMode.LIGHT
        }

    fun setTheme(theme: AppTheme) {
        appTheme.value = theme
    }

    fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }

    fun setAppIconTheme(iconTheme: AppIconTheme) {
        appIconTheme.value = iconTheme
    }

    fun setReverieAccentVariant(variant: ReverieAccentVariant) {
        reverieAccentVariant.value = variant
    }

    fun cycleReverieAccent() {
        reverieAccentVariant.value = when (reverieAccentVariant.value) {
            ReverieAccentVariant.COPPER_GLASS -> ReverieAccentVariant.COPPER_BORDER
            ReverieAccentVariant.COPPER_BORDER -> ReverieAccentVariant.COPPER_GLASS
        }
    }

    // Determine if dark mode should be used based on ThemeMode and system setting
    fun shouldUseDarkMode(isSystemInDarkTheme: Boolean): Boolean {
        return when (themeMode.value) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.REVERIE_DARK -> true // Premium night mode
            ThemeMode.READING -> true // Reading mode uses dark variant by default
        }
    }

    // Get the effective AppTheme based on ThemeMode and system setting
    fun getEffectiveTheme(isSystemInDarkTheme: Boolean): AppTheme {
        return when (themeMode.value) {
            ThemeMode.SYSTEM -> if (isSystemInDarkTheme) AppTheme.DARK else AppTheme.LIGHT
            ThemeMode.DARK -> AppTheme.DARK
            ThemeMode.LIGHT -> AppTheme.LIGHT
            ThemeMode.REVERIE_DARK -> AppTheme.REVERIE_DARK
            ThemeMode.READING -> AppTheme.READING
        }
    }

    // Check if Reverie Dark premium theme is active
    fun isReverieDarkActive(): Boolean = themeMode.value == ThemeMode.REVERIE_DARK

    // Check if Reading mode is active
    fun isReadingModeActive(): Boolean = themeMode.value == ThemeMode.READING || appTheme.value.isReading

    // Check if current theme is a reading theme
    fun isReadingTheme(): Boolean = appTheme.value.isReading

    // Get accent color based on theme mode (uses selected variant for Reverie Dark)
    fun getThemeAccentColor(): Color {
        return when (themeMode.value) {
            ThemeMode.REVERIE_DARK -> reverieAccentVariant.value.accentColor // Use selected accent variant
            ThemeMode.READING -> Color(0xFF6B7F5E) // Moss Green for reading mode
            else -> Color(0xFF87481F) // Dark copper for standard themes (text/icons only)
        }
    }

    // Get current Reverie accent color (for use in GlassColors)
    fun getReverieAccentColor(): Color = reverieAccentVariant.value.accentColor

    // Get current Reverie highlight color (for selection backgrounds)
    fun getReverieHighlightColor(): Color = reverieAccentVariant.value.highlightColor

    // Check if border-based highlights should be used
    fun useBorderHighlight(): Boolean = reverieAccentVariant.value.useBorderHighlight

    // Cycle through themes: Dark -> Light -> Reverie Dark -> Reading -> Dark
    // Simplified cycle focusing on main variants
    fun cycleTheme() {
        appTheme.value = when (appTheme.value) {
            AppTheme.DARK -> AppTheme.LIGHT
            AppTheme.LIGHT -> AppTheme.REVERIE_DARK
            AppTheme.REVERIE_DARK -> AppTheme.READING
            AppTheme.REVERIE_LIGHT -> AppTheme.READING
            AppTheme.READING -> AppTheme.DARK
            AppTheme.READING_LIGHT -> AppTheme.DARK
        }
    }

    // Quick toggle between dark and light variants of current style
    fun toggleDarkLight() {
        appTheme.value = when (appTheme.value) {
            AppTheme.DARK -> AppTheme.LIGHT
            AppTheme.LIGHT -> AppTheme.DARK
            AppTheme.REVERIE_DARK -> AppTheme.REVERIE_LIGHT
            AppTheme.REVERIE_LIGHT -> AppTheme.REVERIE_DARK
            AppTheme.READING -> AppTheme.READING_LIGHT
            AppTheme.READING_LIGHT -> AppTheme.READING
        }
    }

    // Enter reading mode (preserves dark/light preference)
    fun enableReadingMode() {
        appTheme.value = if (appTheme.value.isDark) AppTheme.READING else AppTheme.READING_LIGHT
        themeMode.value = ThemeMode.READING
    }

    // Exit reading mode (returns to previous theme style)
    fun disableReadingMode() {
        appTheme.value = if (appTheme.value.isDark) AppTheme.DARK else AppTheme.LIGHT
        themeMode.value = if (appTheme.value.isDark) ThemeMode.DARK else ThemeMode.LIGHT
    }

    // Toggle reading mode on/off
    fun toggleReadingMode() {
        if (isReadingModeActive()) {
            disableReadingMode()
        } else {
            enableReadingMode()
        }
    }

    // Simple dark mode toggle for Glass UI
    fun toggleDarkMode() {
        appTheme.value = if (appTheme.value.isDark) {
            AppTheme.LIGHT
        } else {
            AppTheme.DARK
        }
    }

    // Get accent color (primary)
    fun getAccentColor(): Color {
        return appTheme.value.accentColors[0]
    }

    // Get gradient brush for premium elements
    fun getAccentBrush(): Brush {
        return Brush.horizontalGradient(appTheme.value.accentColors)
    }

    // Background color based on theme
    fun getBackgroundColor(): Color {
        return when {
            appTheme.value.isReading && appTheme.value.isDark -> Color(0xFF1A1410) // Warm dark brown
            appTheme.value.isReading && !appTheme.value.isDark -> Color(0xFFFAF4E8) // Warm cream
            appTheme.value.isDark -> Color(0xFF0A0A0A)
            else -> Color(0xFFF5F5F5)
        }
    }

    // Surface color based on theme
    fun getSurfaceColor(): Color {
        return when {
            appTheme.value.isReading && appTheme.value.isDark -> Color(0xFF241C16) // Warm dark surface
            appTheme.value.isReading && !appTheme.value.isDark -> Color(0xFFF5EDE0) // Warm light surface
            appTheme.value.isDark -> Color(0xFF161618)
            else -> Color(0xFFFFFFFF)
        }
    }

    // Text color based on theme
    fun getTextColor(): Color {
        return when {
            appTheme.value.isReading && appTheme.value.isDark -> Color(0xFFE8DCC8) // Warm off-white
            appTheme.value.isReading && !appTheme.value.isDark -> Color(0xFF3D3228) // Dark sepia
            appTheme.value.isDark -> Color.White
            else -> Color.Black
        }
    }

    // Is current theme dark?
    fun isDarkTheme(): Boolean = appTheme.value.isDark

    // Is current theme a Reverie premium theme?
    fun isReverieTheme(): Boolean = appTheme.value.isReverie
}
