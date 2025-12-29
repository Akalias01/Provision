package com.example.rezon.ui.theme

import androidx.compose.ui.graphics.Color

// Base Colors
val RezonBlack = Color(0xFF050505)
val RezonSurface = Color(0xFF161618)

// Accents (User Selectable)
val AccentTeal = Color(0xFF00E5FF)
val AccentRed = Color(0xFFFF2020)
val AccentGold = Color(0xFFFFD700)
val AccentPurple = Color(0xFFD500F9)

// Theme Enum
enum class RezonThemeOption(val primary: Color, val nameStr: String) {
    NeonCyber(AccentTeal, "Neon Cyber"),
    CrimsonRed(AccentRed, "Classic Red"),
    RoyalGold(AccentGold, "Steampunk"),
    DeepPurple(AccentPurple, "Ultra Violet")
}
