package com.mossglen.reverie.data

/**
 * Represents a playback speed preset with a name and speed value.
 */
data class PlaybackSpeedPreset(
    val name: String,
    val speed: Float,
    val isCustom: Boolean = false
)

/**
 * Predefined playback speed presets for common use cases.
 */
object PlaybackSpeedPresets {
    val SLOW = PlaybackSpeedPreset("Slow", 0.75f)
    val BEDTIME = PlaybackSpeedPreset("Bedtime", 0.85f)
    val NORMAL = PlaybackSpeedPreset("Normal", 1.0f)
    val COMMUTE = PlaybackSpeedPreset("Commute", 1.25f)
    val FAST = PlaybackSpeedPreset("Fast", 1.5f)

    /**
     * All default presets in display order
     */
    val defaults = listOf(SLOW, BEDTIME, NORMAL, COMMUTE, FAST)

    /**
     * All available speed values (for quick selection grid)
     */
    val allSpeeds = listOf(0.5f, 0.75f, 0.85f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    /**
     * Find a preset by speed value (returns null if not a preset)
     */
    fun findPresetBySpeed(speed: Float): PlaybackSpeedPreset? {
        return defaults.find { it.speed == speed }
    }

    /**
     * Check if a speed matches a default preset
     */
    fun isDefaultPreset(speed: Float): Boolean {
        return defaults.any { it.speed == speed }
    }
}
