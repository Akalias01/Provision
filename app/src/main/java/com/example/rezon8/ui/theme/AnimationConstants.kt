package com.mossglen.lithos.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * LITHOS AMBER Animation Standards
 *
 * Physics-based spring animations for natural, premium feel.
 * All animations should be consistent throughout the app.
 *
 * Design Philosophy:
 * - Natural, organic motion
 * - No jarring or robotic transitions
 * - Responsive but not hyperactive
 */
object ReverieAnimations {

    // Quick feedback animations (button presses, selections)
    val QuickSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )

    // Sheet transitions (half-sheet, bottom sheets, modals)
    val SheetSpring = spring<Float>(
        dampingRatio = LithosMotion.DampingRatio,
        stiffness = LithosMotion.Stiffness
    )

    // Dismissal animations (closing, collapsing)
    val DismissSpring = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = 350f
    )

    // Morphing animations (element transforms, expansions)
    val MorphSpring = spring<Float>(
        dampingRatio = LithosMotion.DampingQuick,
        stiffness = 280f
    )

    // Scale animations for press feedback
    val PressScale = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )

    // Standard durations (for tween when needed)
    object Durations {
        const val Quick = LithosMotion.DurationFast
        const val Standard = LithosMotion.DurationMedium
        const val Emphasized = 500
    }

    // Standard alpha values
    object Alpha {
        const val Disabled = 0.38f
        const val Medium = 0.6f
        const val High = 0.87f
        const val Full = 1f
    }
}

/**
 * Lithos Animation presets - more direct access to motion values
 */
object LithosAnimations {

    // Quick, responsive feedback
    val Quick = spring<Float>(
        dampingRatio = LithosMotion.DampingQuick,
        stiffness = LithosMotion.StiffnessQuick
    )

    // Standard, balanced motion
    val Standard = spring<Float>(
        dampingRatio = LithosMotion.DampingRatio,
        stiffness = LithosMotion.Stiffness
    )

    // Gentle, relaxed motion
    val Gentle = spring<Float>(
        dampingRatio = LithosMotion.DampingGentle,
        stiffness = LithosMotion.StiffnessGentle
    )

    // Press feedback
    val Press = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 400f
    )

    // Sheet/modal transitions
    val Sheet = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 300f
    )

    // Progress ring animations
    val Progress = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 200f
    )
}
