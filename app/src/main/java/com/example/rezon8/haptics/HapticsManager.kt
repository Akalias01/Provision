package com.mossglen.reverie.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * REVERIE Haptics Engine
 *
 * Rich tactile feedback using Android's VibrationEffect.Composition API.
 * Aligned with iOS 26 Taptic Engine and Android 16 haptic patterns.
 *
 * Features:
 * - Primitive-based haptic compositions
 * - Texture effects (tick, click, thud)
 * - Context-aware feedback intensity
 * - Graceful fallback for older devices
 */

@Singleton
class HapticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private val supportsComposition: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllPrimitivesSupported(
            VibrationEffect.Composition.PRIMITIVE_TICK,
            VibrationEffect.Composition.PRIMITIVE_CLICK,
            VibrationEffect.Composition.PRIMITIVE_LOW_TICK
        )
    }

    // ========================================================================
    // BASIC HAPTICS
    // ========================================================================

    /**
     * Light tap - for list item selection, toggle switches.
     * iOS equivalent: UIImpactFeedbackGenerator(.light)
     */
    fun lightTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    }

    /**
     * Medium tap - for button presses, confirmations.
     * iOS equivalent: UIImpactFeedbackGenerator(.medium)
     */
    fun mediumTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    /**
     * Heavy tap - for important actions, errors.
     * iOS equivalent: UIImpactFeedbackGenerator(.heavy)
     */
    fun heavyTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(40)
        }
    }

    /**
     * Success feedback - for completed actions.
     * iOS equivalent: UINotificationFeedbackGenerator(.success)
     */
    fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 50)
                .compose()
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 15, 50, 25), -1)
        }
    }

    /**
     * Warning feedback - for caution states.
     * iOS equivalent: UINotificationFeedbackGenerator(.warning)
     */
    fun warning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.4f, 100)
                .compose()
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 20, 80, 20), -1)
        }
    }

    /**
     * Error feedback - for failed actions.
     * iOS equivalent: UINotificationFeedbackGenerator(.error)
     */
    fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.8f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.5f, 80)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.3f, 80)
                .compose()
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 40, 50, 40, 50, 40), -1)
        }
    }

    // ========================================================================
    // TEXTURE HAPTICS (Scrolling, Dragging)
    // ========================================================================

    /**
     * Scroll tick - subtle feedback when scrolling past items.
     * Like a physical detent on a dial.
     */
    fun scrollTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.3f)
                .compose()
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createOneShot(5, 50))
        }
    }

    /**
     * Slider tick - feedback when slider crosses a step.
     */
    fun sliderTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f)
                .compose()
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createOneShot(8, 80))
        }
    }

    /**
     * Drag start - feedback when beginning a drag gesture.
     */
    fun dragStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.5f)
                .compose()
            vibrator.vibrate(effect)
        } else {
            mediumTap()
        }
    }

    /**
     * Drag end - feedback when releasing a drag gesture.
     */
    fun dragEnd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.5f)
                .compose()
            vibrator.vibrate(effect)
        } else {
            lightTap()
        }
    }

    // ========================================================================
    // PLAYBACK HAPTICS
    // ========================================================================

    /**
     * Play button press - satisfying feedback for play/pause.
     */
    fun playPress() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.3f, 30)
                .compose()
            vibrator.vibrate(effect)
        } else {
            mediumTap()
        }
    }

    /**
     * Skip feedback - tactile confirmation of skip forward/back.
     */
    fun skip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SPIN, 0.4f)
                .compose()
            vibrator.vibrate(effect)
        } else {
            lightTap()
        }
    }

    /**
     * Chapter change - feedback when moving to new chapter.
     */
    fun chapterChange() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f, 60)
                .compose()
            vibrator.vibrate(effect)
        } else {
            success()
        }
    }

    /**
     * Bookmark added - confirming feedback for bookmark creation.
     */
    fun bookmarkAdded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.5f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.6f, 40)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.3f, 60)
                .compose()
            vibrator.vibrate(effect)
        } else {
            success()
        }
    }

    // ========================================================================
    // NAVIGATION HAPTICS
    // ========================================================================

    /**
     * Tab switch - feedback when changing bottom nav tabs.
     */
    fun tabSwitch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)
                .compose()
            vibrator.vibrate(effect)
        } else {
            lightTap()
        }
    }

    /**
     * Screen transition - subtle feedback for navigation.
     */
    fun screenTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.2f)
                .compose()
            vibrator.vibrate(effect)
        }
        // No fallback - too subtle for legacy devices
    }

    /**
     * Pull to refresh - feedback when threshold is reached.
     */
    fun pullToRefresh() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportsComposition) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.6f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 50)
                .compose()
            vibrator.vibrate(effect)
        } else {
            mediumTap()
        }
    }
}

// ============================================================================
// COMPOSE INTEGRATION
// ============================================================================

/**
 * Remember a HapticsManager instance scoped to the composition.
 */
@Composable
fun rememberHapticsManager(): HapticsManager {
    val context = LocalContext.current
    return remember { HapticsManager(context) }
}

/**
 * Extension to perform haptic feedback on a View.
 * Useful for interop with View-based haptic constants.
 */
fun View.performHaptic(type: HapticType) {
    val constant = when (type) {
        HapticType.LightTap -> HapticFeedbackConstants.KEYBOARD_TAP
        HapticType.MediumTap -> HapticFeedbackConstants.VIRTUAL_KEY
        HapticType.HeavyTap -> HapticFeedbackConstants.LONG_PRESS
        HapticType.Confirm -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        HapticType.Reject -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        HapticType.GestureStart -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_START
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        HapticType.GestureEnd -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_END
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
    }
    performHapticFeedback(constant)
}

enum class HapticType {
    LightTap,
    MediumTap,
    HeavyTap,
    Confirm,
    Reject,
    GestureStart,
    GestureEnd
}

// ============================================================================
// COMPOSE HAPTIC MODIFIERS
// ============================================================================

/**
 * Provides haptic feedback on click.
 */
@Composable
fun hapticClick(type: HapticType = HapticType.MediumTap): () -> Unit {
    val view = LocalView.current
    return { view.performHaptic(type) }
}
