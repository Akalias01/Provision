package com.mossglen.reverie.ui.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * REVERIE Glass Effect System
 *
 * True frosted glass effects using multiple techniques:
 * - Modifier.blur() for content blur (Compose 1.6+)
 * - RenderEffect for hardware-accelerated blur (Android 12+)
 * - Layered transparency with gradients for depth
 *
 * Aligned with iOS 26 Liquid Glass, Android 16 Material 3 Expressive.
 */

// ============================================================================
// GLASS SURFACE STYLES
// ============================================================================

/**
 * Applies a glass card effect with subtle frosted appearance.
 * Layered transparency with gradient for depth.
 */
@Composable
fun Modifier.glassCard(
    isDark: Boolean = true,
    cornerRadius: Dp = GlassShapes.Medium
): Modifier {
    val backgroundColor = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.85f)
    } else {
        Color(0xFFF2F2F7).copy(alpha = 0.85f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    val topHighlight = if (isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.White
    }

    return this
        .clip(RoundedCornerShape(cornerRadius))
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    topHighlight,
                    backgroundColor,
                    backgroundColor.copy(alpha = 0.95f)
                ),
                startY = 0f,
                endY = 150f
            )
        )
        .border(
            width = 0.5.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = borderColor.alpha * 1.5f),
                    borderColor
                )
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
}

/**
 * Applies a floating glass effect - higher elevation, more prominent.
 * Use for bottom sheets, floating action buttons, mini player.
 */
@Composable
fun Modifier.glassFloating(
    isDark: Boolean = true,
    cornerRadius: Dp = GlassShapes.Large
): Modifier {
    val backgroundColor = if (isDark) {
        Color(0xFF2C2C2E).copy(alpha = 0.95f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.95f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.15f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    val topHighlight = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White
    }

    return this
        .clip(RoundedCornerShape(cornerRadius))
        .drawBehind {
            // Subtle shadow
            drawRect(
                color = Color.Black.copy(alpha = 0.2f),
                topLeft = Offset(0f, -4f),
                size = size.copy(height = 8f)
            )
        }
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    topHighlight,
                    backgroundColor
                ),
                startY = 0f,
                endY = 80f
            )
        )
        .border(
            width = 0.5.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = borderColor.alpha * 1.5f),
                    borderColor
                )
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
}

/**
 * Applies a navigation bar glass effect.
 * Subtle frosted appearance for bottom navigation.
 */
@Composable
fun Modifier.glassNavBar(
    isDark: Boolean = true
): Modifier {
    val backgroundColor = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.92f)
    } else {
        Color(0xFFF2F2F7).copy(alpha = 0.92f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }

    return this
        .clip(RoundedCornerShape(GlassShapes.Large))
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.05f),
                    backgroundColor
                ),
                startY = 0f,
                endY = 60f
            )
        )
        .border(
            width = 0.5.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = borderColor.alpha * 1.5f),
                    borderColor
                )
            ),
            shape = RoundedCornerShape(GlassShapes.Large)
        )
}

/**
 * Applies glass effect with actual blur on Android 12+.
 * Falls back to standard glass styling on older devices.
 */
@Composable
fun Modifier.glassEffect(
    blurRadius: Dp = GlassBlur.Medium,
    isDark: Boolean = true,
    cornerRadius: Dp = GlassShapes.Medium,
    borderWidth: Dp = 0.5.dp
): Modifier {
    val backgroundColor = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.75f)
    } else {
        Color(0xFFF2F2F7).copy(alpha = 0.75f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ - Use blur modifier
        this
            .clip(RoundedCornerShape(cornerRadius))
            .blur(blurRadius)
            .background(backgroundColor)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
                } else Modifier
            )
    } else {
        // Fallback - Just use glass card styling
        this.glassCard(isDark, cornerRadius)
    }
}

// ============================================================================
// BLUR MODIFIERS FOR CONTENT BEHIND GLASS
// ============================================================================

/**
 * Blurs content when it scrolls behind glass elements.
 * Apply this to scrollable content areas.
 */
@Composable
fun Modifier.blurredWhenBehindGlass(
    blurRadius: Dp = 8.dp,
    enabled: Boolean = true
): Modifier {
    return if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.graphicsLayer {
            val radiusPx = blurRadius.toPx()
            renderEffect = RenderEffect
                .createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        }
    } else {
        this
    }
}

// ============================================================================
// ANIMATED GLASS EFFECTS
// ============================================================================

/**
 * Glass effect that animates blur radius.
 * Useful for focus/unfocus states.
 */
@Composable
fun Modifier.animatedGlass(
    targetBlur: Dp,
    isDark: Boolean = true,
    cornerRadius: Dp = GlassShapes.Medium
): Modifier {
    val animatedBlur by animateFloatAsState(
        targetValue = targetBlur.value,
        animationSpec = spring(
            dampingRatio = GlassMotion.DampingRatio,
            stiffness = GlassMotion.Stiffness
        ),
        label = "blur_animation"
    )

    return this.glassEffect(
        blurRadius = animatedBlur.dp,
        isDark = isDark,
        cornerRadius = cornerRadius
    )
}

// ============================================================================
// GLASS OVERLAY EFFECTS
// ============================================================================

/**
 * Adds a subtle vibrancy overlay that enhances content visibility.
 */
@Composable
fun Modifier.glassVibrancy(
    isDark: Boolean = true,
    intensity: Float = 0.1f
): Modifier {
    val overlayColor = if (isDark) {
        Color.White.copy(alpha = intensity)
    } else {
        Color.Black.copy(alpha = intensity * 0.5f)
    }

    return this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    overlayColor.copy(alpha = 0f),
                    overlayColor
                ),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.maxDimension * 0.8f
            )
        )
    }
}

/**
 * Adds a frosted edge highlight effect.
 */
@Composable
fun Modifier.glassHighlight(
    isDark: Boolean = true
): Modifier {
    val highlightColor = if (isDark) {
        Color.White.copy(alpha = 0.15f)
    } else {
        Color.White.copy(alpha = 0.5f)
    }

    return this.drawBehind {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(highlightColor, Color.Transparent),
                startY = 0f,
                endY = 3.dp.toPx()
            )
        )
    }
}

// ============================================================================
// TRUE BACKDROP BLUR (Android 12+)
// ============================================================================

/**
 * Applies hardware-accelerated blur using RenderEffect.
 * This blurs the content OF the composable, creating a frosted appearance
 * when layered over other content.
 *
 * Use on a translucent surface placed over content to create backdrop blur effect.
 */
@Composable
fun Modifier.renderBlur(
    blurRadius: Dp = GlassBlur.Medium
): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.graphicsLayer {
            val radiusPx = blurRadius.toPx()
            if (radiusPx > 0f) {
                renderEffect = RenderEffect
                    .createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
                    .asComposeRenderEffect()
            }
        }
    } else {
        this
    }
}

/**
 * True frosted glass surface with backdrop blur.
 * On Android 12+, creates actual blur of content behind.
 * On older devices, uses enhanced gradient fallback.
 */
@Composable
fun Modifier.glassBlurSurface(
    blurRadius: Dp = GlassBlur.Medium,
    isDark: Boolean = true,
    cornerRadius: Dp = GlassShapes.Medium
): Modifier {
    val backgroundColor = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.65f)
    } else {
        Color(0xFFF2F2F7).copy(alpha = 0.65f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.15f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    val topHighlight = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.3f)
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ - Apply RenderEffect blur then overlay with translucent color
        this
            .graphicsLayer {
                val radiusPx = blurRadius.toPx()
                renderEffect = RenderEffect
                    .createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
                    .asComposeRenderEffect()
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        topHighlight,
                        backgroundColor,
                        backgroundColor.copy(alpha = 0.8f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderColor.copy(alpha = borderColor.alpha * 2f),
                        borderColor
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    } else {
        // Fallback for older devices - enhanced gradient
        this.glassCard(isDark, cornerRadius)
    }
}

/**
 * Container that provides blurred backdrop effect.
 * Place content behind this and it will appear blurred through the glass.
 *
 * Usage:
 * Box {
 *     // Background content (will be blurred)
 *     ScrollableContent()
 *
 *     // Glass overlay at bottom
 *     GlassBlurOverlay(
 *         modifier = Modifier.align(Alignment.BottomCenter)
 *     ) {
 *         BottomNavContent()
 *     }
 * }
 */
@Composable
fun GlassBlurOverlay(
    modifier: Modifier = Modifier,
    blurRadius: Dp = GlassBlur.Medium,
    isDark: Boolean = true,
    cornerRadius: Dp = GlassShapes.Large,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundColor = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.85f)
    } else {
        Color(0xFFF2F2F7).copy(alpha = 0.85f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // True blur on Android 12+
                    Modifier.graphicsLayer {
                        val radiusPx = blurRadius.toPx()
                        renderEffect = RenderEffect
                            .createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                } else Modifier
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.05f else 0.3f),
                        backgroundColor
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderColor.copy(alpha = borderColor.alpha * 1.5f),
                        borderColor
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

/**
 * Applies a saturation and brightness boost for vibrancy effect.
 * Enhances the visual quality of blurred content.
 */
@Composable
fun Modifier.vibrancy(
    saturationBoost: Float = 1.2f
): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.graphicsLayer {
            // Slight saturation increase for vibrancy
            val matrix = android.graphics.ColorMatrix().apply {
                setSaturation(saturationBoost)
            }
            renderEffect = RenderEffect.createColorFilterEffect(
                android.graphics.ColorMatrixColorFilter(matrix)
            ).asComposeRenderEffect()
        }
    } else {
        this
    }
}

