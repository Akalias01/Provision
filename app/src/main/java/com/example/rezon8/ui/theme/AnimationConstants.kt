package com.mossglen.reverie.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Reverie Animation Standards
 *
 * Per project manifest: "Physics-based spring animations (natural, not robotic)"
 * All animations must be consistent throughout the app for premium feel.
 */
object ReverieAnimations {

    // Quick feedback animations (button presses, selections)
    val QuickSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,  // 0.5f
        stiffness = Spring.StiffnessHigh  // 400f
    )

    // Sheet transitions (half-sheet, bottom sheets, modals)
    val SheetSpring = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 300f
    )

    // Dismissal animations (closing, collapsing)
    val DismissSpring = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = 350f
    )

    // Morphing animations (element transforms, expansions)
    val MorphSpring = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = 280f
    )

    // Scale animations for press feedback
    val PressScale = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )

    // Standard durations (for tween when needed)
    object Durations {
        const val Quick = 150
        const val Standard = 300
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
