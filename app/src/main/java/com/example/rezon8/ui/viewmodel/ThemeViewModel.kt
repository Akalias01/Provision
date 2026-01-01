package com.mossglen.lithos.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mossglen.lithos.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * LITHOS Theme Mode - 3 color modes with consistent Amber accents
 *
 * Lithos Design System:
 * - Lithos Light: Warm oat background (#F2F0E9)
 * - Lithos Standard: Deep slate background (#1A1D21)
 * - Lithos Black: True OLED black (#000000)
 *
 * All modes use Amber (#D48C2C) for accents and Moss (#4A5D45) for play button
 */
enum class ThemeMode(val displayName: String, val description: String) {
    SYSTEM("System", "Follows device settings"),
    LIGHT("Light", "Lithos Light - Warm oat"),
    DARK("Dark", "Lithos Standard - Deep slate"),
    LITHOS_DARK("OLED Black", "Lithos Black - True black"),
    READING("Reading", "Eye comfort mode")
}

/**
 * Accent color variants for Lithos theme
 * Amber (#D48C2C) for accents, highlights, progress indicators
 * Slate surfaces for backgrounds
 */
enum class LithosAccentVariant(
    val displayName: String,
    val accentColor: Color,        // For text, icons, highlights, progress
    val highlightColor: Color,     // For selection backgrounds/borders
    val useBorderHighlight: Boolean
) {
    // Amber accent with subtle glass background selection
    AMBER_GLASS(
        displayName = "Amber/Glass",
        accentColor = Color(0xFFD48C2C),  // Lithos Amber
        highlightColor = Color(0xFF2D3339),  // Slate glass for backgrounds
        useBorderHighlight = false
    ),
    // Amber accent with border-based selection
    AMBER_BORDER(
        displayName = "Amber/Border",
        accentColor = Color(0xFFD48C2C),  // Lithos Amber
        highlightColor = Color(0xFFD48C2C).copy(alpha = 0.30f),  // Amber border
        useBorderHighlight = true
    );

    companion object {
        // Legacy aliases for backward compatibility
        val COPPER_GLASS = AMBER_GLASS
        val COPPER_BORDER = AMBER_BORDER
    }
}

/**
 * App Icon Theme - Dark (white R) or Light (black R)
 */
enum class AppIconTheme(val displayName: String) {
    DARK("Dark"),
    LIGHT("Light")
}

/**
 * LITHOS Theme Options
 *
 * 3 Primary Modes:
 * - Light: Oat background (#F2F0E9)
 * - Standard (Dark): Slate background (#1A1D21)
 * - OLED Black: True black (#000000)
 *
 * Plus Reading modes for extended reading comfort
 *
 * All use Amber (#D48C2C) for accents and Moss (#4A5D45) for play button
 */
enum class AppTheme(
    val displayName: String,
    val isDark: Boolean,
    val isOLED: Boolean,
    val isLithos: Boolean,
    val isReading: Boolean,
    val accentColors: List<Color>
) {
    // Standard Dark - Slate background
    DARK(
        displayName = "Standard",
        isDark = true,
        isOLED = false,
        isLithos = true,
        isReading = false,
        accentColors = listOf(Color(0xFFD48C2C), Color(0xFFD48C2C))  // Lithos Amber
    ),
    // Light - Oat background
    LIGHT(
        displayName = "Light",
        isDark = false,
        isOLED = false,
        isLithos = true,
        isReading = false,
        accentColors = listOf(Color(0xFFB57420), Color(0xFFB57420))  // Darker Amber for light bg
    ),
    // OLED Black - True black for OLED screens
    LITHOS_DARK(
        displayName = "OLED Black",
        isDark = true,
        isOLED = true,
        isLithos = true,
        isReading = false,
        accentColors = listOf(Color(0xFFD48C2C), Color(0xFFD48C2C))  // Lithos Amber
    ),
    // Lithos Light (alias for LIGHT)
    LITHOS_LIGHT(
        displayName = "Lithos Light",
        isDark = false,
        isOLED = false,
        isLithos = true,
        isReading = false,
        accentColors = listOf(Color(0xFFB57420), Color(0xFFB57420))  // Darker Amber for light bg
    ),
    // Reading Dark - Warm sepia for eye comfort
    READING(
        displayName = "Reading",
        isDark = true,
        isOLED = false,
        isLithos = false,
        isReading = true,
        accentColors = listOf(Color(0xFF4A5D45), Color(0xFF4A5D45))  // Moss Green for reading
    ),
    // Reading Light - Warm cream for daytime reading
    READING_LIGHT(
        displayName = "Reading Light",
        isDark = false,
        isOLED = false,
        isLithos = false,
        isReading = true,
        accentColors = listOf(Color(0xFF3A4A36), Color(0xFF3A4A36))  // Darker Moss for light
    )
}

// Keep DisplayMode for backward compatibility but simplify
enum class DisplayMode {
    DARK, LIGHT, READING
}

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    var appTheme = mutableStateOf(AppTheme.DARK)
        private set

    // Theme mode: Default to Standard Dark (Slate) for premium first impression
    var themeMode = mutableStateOf(ThemeMode.DARK)
        private set

    // Lithos accent color variant (Amber with glass or border selection)
    var lithosAccentVariant = mutableStateOf(LithosAccentVariant.AMBER_BORDER)
        private set

    // App icon theme: Dark (white R) or Light (black R)
    var appIconTheme = mutableStateOf(AppIconTheme.DARK)
        private set

    init {
        // Load saved theme preferences on startup
        viewModelScope.launch {
            val savedThemeMode = settingsRepository.getThemeMode()
            val savedAccentVariant = settingsRepository.getLithosAccentVariant()

            // Apply saved theme mode
            themeMode.value = try {
                ThemeMode.valueOf(savedThemeMode)
            } catch (e: Exception) {
                ThemeMode.DARK
            }

            // Apply saved accent variant
            lithosAccentVariant.value = try {
                LithosAccentVariant.valueOf(savedAccentVariant)
            } catch (e: Exception) {
                LithosAccentVariant.AMBER_BORDER
            }

            // Sync appTheme based on themeMode
            appTheme.value = when (themeMode.value) {
                ThemeMode.SYSTEM -> AppTheme.DARK
                ThemeMode.DARK -> AppTheme.DARK
                ThemeMode.LIGHT -> AppTheme.LIGHT
                ThemeMode.LITHOS_DARK -> AppTheme.LITHOS_DARK
                ThemeMode.READING -> AppTheme.READING
            }
        }
    }

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
        // Persist to DataStore
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode.name)
        }
    }

    fun setAppIconTheme(iconTheme: AppIconTheme) {
        appIconTheme.value = iconTheme
    }

    fun setLithosAccentVariant(variant: LithosAccentVariant) {
        lithosAccentVariant.value = variant
        // Persist to DataStore
        viewModelScope.launch {
            settingsRepository.setLithosAccentVariant(variant.name)
        }
    }

    fun cycleLithosAccent() {
        val newVariant = when (lithosAccentVariant.value) {
            LithosAccentVariant.AMBER_GLASS -> LithosAccentVariant.AMBER_BORDER
            LithosAccentVariant.AMBER_BORDER -> LithosAccentVariant.AMBER_GLASS
        }
        lithosAccentVariant.value = newVariant
        // Persist to DataStore
        viewModelScope.launch {
            settingsRepository.setLithosAccentVariant(newVariant.name)
        }
    }

    // Determine if dark mode should be used based on ThemeMode and system setting
    fun shouldUseDarkMode(isSystemInDarkTheme: Boolean): Boolean {
        return when (themeMode.value) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.LITHOS_DARK -> true  // OLED mode is dark
            ThemeMode.READING -> true       // Reading mode uses dark variant by default
        }
    }

    // Determine if OLED mode should be used
    fun shouldUseOLEDMode(): Boolean = themeMode.value == ThemeMode.LITHOS_DARK

    // Get the effective AppTheme based on ThemeMode and system setting
    fun getEffectiveTheme(isSystemInDarkTheme: Boolean): AppTheme {
        return when (themeMode.value) {
            ThemeMode.SYSTEM -> if (isSystemInDarkTheme) AppTheme.DARK else AppTheme.LIGHT
            ThemeMode.DARK -> AppTheme.DARK
            ThemeMode.LIGHT -> AppTheme.LIGHT
            ThemeMode.LITHOS_DARK -> AppTheme.LITHOS_DARK
            ThemeMode.READING -> AppTheme.READING
        }
    }

    // Check if OLED mode is active (true black background)
    fun isOLEDActive(): Boolean = themeMode.value == ThemeMode.LITHOS_DARK || appTheme.value.isOLED

    // Check if Lithos Dark premium theme is active (legacy - maps to OLED)
    fun isLithosDarkActive(): Boolean = isOLEDActive()

    // Check if Reading mode is active
    fun isReadingModeActive(): Boolean = themeMode.value == ThemeMode.READING || appTheme.value.isReading

    // Check if current theme is a reading theme
    fun isReadingTheme(): Boolean = appTheme.value.isReading

    // Get accent color - ALWAYS Lithos Amber for non-reading modes
    fun getThemeAccentColor(): Color {
        return when (themeMode.value) {
            ThemeMode.READING -> Color(0xFF4A5D45)  // Moss Green for reading mode
            else -> Color(0xFFD48C2C)  // Lithos Amber for all other modes
        }
    }

    // Get current Lithos accent color (for use in GlassColors)
    fun getLithosAccentColor(): Color = lithosAccentVariant.value.accentColor

    // Get current Lithos highlight color (for selection backgrounds)
    fun getLithosHighlightColor(): Color = lithosAccentVariant.value.highlightColor

    // Check if border-based highlights should be used
    fun useBorderHighlight(): Boolean = lithosAccentVariant.value.useBorderHighlight

    // Cycle through themes: Dark -> Light -> Lithos Dark -> Reading -> Dark
    // Simplified cycle focusing on main variants
    fun cycleTheme() {
        appTheme.value = when (appTheme.value) {
            AppTheme.DARK -> AppTheme.LIGHT
            AppTheme.LIGHT -> AppTheme.LITHOS_DARK
            AppTheme.LITHOS_DARK -> AppTheme.READING
            AppTheme.LITHOS_LIGHT -> AppTheme.READING
            AppTheme.READING -> AppTheme.DARK
            AppTheme.READING_LIGHT -> AppTheme.DARK
        }
    }

    // Quick toggle between dark and light variants of current style
    fun toggleDarkLight() {
        appTheme.value = when (appTheme.value) {
            AppTheme.DARK -> AppTheme.LIGHT
            AppTheme.LIGHT -> AppTheme.DARK
            AppTheme.LITHOS_DARK -> AppTheme.LITHOS_LIGHT
            AppTheme.LITHOS_LIGHT -> AppTheme.LITHOS_DARK
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

    // Background color based on theme - uses Lithos palette
    fun getBackgroundColor(): Color {
        return when {
            appTheme.value.isReading && appTheme.value.isDark -> Color(0xFF1A1410) // Warm dark brown
            appTheme.value.isReading && !appTheme.value.isDark -> Color(0xFFF2F0E9) // Lithos Oat
            appTheme.value.isOLED -> Color(0xFF000000)    // True black for OLED
            appTheme.value.isDark -> Color(0xFF1A1D21)    // Lithos Slate
            else -> Color(0xFFF2F0E9)                      // Lithos Oat
        }
    }

    // Surface color based on theme - uses Lithos palette
    fun getSurfaceColor(): Color {
        return when {
            appTheme.value.isReading && appTheme.value.isDark -> Color(0xFF241C16) // Warm dark surface
            appTheme.value.isReading && !appTheme.value.isDark -> Color(0xFFE8E6DF) // Light surface
            appTheme.value.isOLED -> Color(0xFF0A0A0A)    // Near-black for OLED
            appTheme.value.isDark -> Color(0xFF22262B)    // Elevated slate
            else -> Color(0xFFE8E6DF)                      // Light surface
        }
    }

    // Text color based on theme
    fun getTextColor(): Color {
        return when {
            appTheme.value.isReading && appTheme.value.isDark -> Color(0xFFE8DCC8) // Warm off-white
            appTheme.value.isReading && !appTheme.value.isDark -> Color(0xFF3D3228) // Dark sepia
            appTheme.value.isDark -> Color.White
            else -> Color(0xFF1A1D21)  // Lithos Slate for text on light backgrounds
        }
    }

    // Is current theme dark?
    fun isDarkTheme(): Boolean = appTheme.value.isDark

    // Is current theme a Lithos premium theme?
    fun isLithosTheme(): Boolean = appTheme.value.isLithos
}
