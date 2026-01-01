package com.mossglen.lithos.ui.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
 * LITHOS AMBER Glass Effect System
 *
 * Design Philosophy:
 * - Frosted blur (20dp standard) not shiny gradients
 * - Matte/satin finishes
 * - No neon glow effects
 * - Natural materials aesthetic
 *
 * True frosted glass effects using multiple techniques:
 * - Modifier.blur() for content blur (Compose 1.6+)
 * - RenderEffect for hardware-accelerated blur (Android 12+)
 * - Layered transparency for depth
 */

// ============================================================================
// GLASS SURFACE STYLES
// ============================================================================

/**
 * Applies a glass card effect with frosted appearance.
 * Uses Lithos Slate/Oat colors with subtle borders.
 */
@Composable
fun Modifier.glassCard(
    isDark: Boolean = true,
    cornerRadius: Dp = LithosShapes.Medium
): Modifier {
    val backgroundColor = if (isDark) {
        LithosGlass
    } else {
        LithosGlassLight
    }

    val borderColor = if (isDark) {
        LithosGlassBorder
    } else {
        LithosGlassBorderLight
    }

    // Subtle top highlight - matte, not shiny
    val topHighlight = if (isDark) {
        Color.White.copy(alpha = 0.04f)
    } else {
        Color.White.copy(alpha = 0.6f)
    }

    return this
        .clip(RoundedCornerShape(cornerRadius))
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    topHighlight,
                    backgroundColor,
                    backgroundColor
                ),
                startY = 0f,
                endY = 100f
            )
        )
        .border(
            width = 0.5.dp,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius)
        )
}

/**
 * Applies a floating glass effect - for bottom sheets, floating buttons.
 * Matte finish with subtle elevation.
 */
@Composable
fun Modifier.glassFloating(
    isDark: Boolean = true,
    cornerRadius: Dp = LithosShapes.Large
): Modifier {
    val backgroundColor = if (isDark) {
        LithosSurfaceDarkElevated.copy(alpha = 0.95f)
    } else {
        LithosSurfaceLightElevated.copy(alpha = 0.95f)
    }

    val borderColor = if (isDark) {
        LithosGlassBorder
    } else {
        LithosGlassBorderLight
    }

    val topHighlight = if (isDark) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color.White.copy(alpha = 0.5f)
    }

    return this
        .clip(RoundedCornerShape(cornerRadius))
        .drawBehind {
            // Subtle shadow - matte, no glow
            drawRect(
                color = Color.Black.copy(alpha = 0.15f),
                topLeft = Offset(0f, -3f),
                size = size.copy(height = 6f)
            )
        }
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    topHighlight,
                    backgroundColor
                ),
                startY = 0f,
                endY = 60f
            )
        )
        .border(
            width = 0.5.dp,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius)
        )
}

/**
 * Applies a navigation bar glass effect.
 * Frosted appearance for bottom navigation.
 */
@Composable
fun Modifier.glassNavBar(
    isDark: Boolean = true
): Modifier {
    val backgroundColor = if (isDark) {
        LithosGlass
    } else {
        LithosGlassLight
    }

    val borderColor = if (isDark) {
        LithosGlassBorder
    } else {
        LithosGlassBorderLight
    }

    return this
        .clip(RoundedCornerShape(LithosShapes.Large))
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.03f),
                    backgroundColor
                ),
                startY = 0f,
                endY = 60f
            )
        )
        .border(
            width = 0.5.dp,
            color = borderColor,
            shape = RoundedCornerShape(LithosShapes.Large)
        )
}

/**
 * Applies glass effect with frosted blur on Android 12+.
 * Uses standard 20dp blur per Lithos spec.
 */
@Composable
fun Modifier.glassEffect(
    blurRadius: Dp = LithosBlur.Standard,  // 20dp per spec
    isDark: Boolean = true,
    cornerRadius: Dp = LithosShapes.Medium,
    borderWidth: Dp = 0.5.dp
): Modifier {
    val backgroundColor = if (isDark) {
        LithosSlate.copy(alpha = 0.75f)
    } else {
        LithosOat.copy(alpha = 0.75f)
    }

    val borderColor = if (isDark) {
        LithosGlassBorder
    } else {
        LithosGlassBorderLight
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
        // Fallback - Use glass card styling
        this.glassCard(isDark, cornerRadius)
    }
}

// ============================================================================
// BLUR MODIFIERS FOR CONTENT BEHIND GLASS
// ============================================================================

/**
 * Blurs content when it scrolls behind glass elements.
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
 */
@Composable
fun Modifier.animatedGlass(
    targetBlur: Dp,
    isDark: Boolean = true,
    cornerRadius: Dp = LithosShapes.Medium
): Modifier {
    val animatedBlur by animateFloatAsState(
        targetValue = targetBlur.value,
        animationSpec = spring(
            dampingRatio = LithosMotion.DampingRatio,
            stiffness = LithosMotion.Stiffness
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
// GLASS OVERLAY EFFECTS - Matte, no glow
// ============================================================================

/**
 * Adds a subtle vibrancy overlay - matte finish.
 */
@Composable
fun Modifier.glassVibrancy(
    isDark: Boolean = true,
    intensity: Float = 0.08f  // Reduced for matte finish
): Modifier {
    val overlayColor = if (isDark) {
        Color.White.copy(alpha = intensity)
    } else {
        Color.Black.copy(alpha = intensity * 0.4f)
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
 * Adds a subtle frosted edge highlight - matte, not shiny.
 */
@Composable
fun Modifier.glassHighlight(
    isDark: Boolean = true
): Modifier {
    val highlightColor = if (isDark) {
        Color.White.copy(alpha = 0.08f)  // Reduced for matte
    } else {
        Color.White.copy(alpha = 0.35f)
    }

    return this.drawBehind {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(highlightColor, Color.Transparent),
                startY = 0f,
                endY = 2.dp.toPx()  // Thinner highlight
            )
        )
    }
}

// ============================================================================
// TRUE BACKDROP BLUR (Android 12+)
// ============================================================================

/**
 * Applies hardware-accelerated blur using RenderEffect.
 */
@Composable
fun Modifier.renderBlur(
    blurRadius: Dp = LithosBlur.Standard  // 20dp per spec
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
 * Uses Lithos colors and 20dp standard blur.
 */
@Composable
fun Modifier.glassBlurSurface(
    blurRadius: Dp = LithosBlur.Standard,  // 20dp per spec
    isDark: Boolean = true,
    cornerRadius: Dp = LithosShapes.Medium
): Modifier {
    val backgroundColor = if (isDark) {
        LithosSlate.copy(alpha = 0.65f)
    } else {
        LithosOat.copy(alpha = 0.65f)
    }

    val borderColor = if (isDark) {
        LithosGlassBorder
    } else {
        LithosGlassBorderLight
    }

    val topHighlight = if (isDark) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color.White.copy(alpha = 0.25f)
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
    } else {
        this.glassCard(isDark, cornerRadius)
    }
}

/**
 * Container that provides blurred backdrop effect.
 * Uses Lithos colors and frosted blur.
 */
@Composable
fun GlassBlurOverlay(
    modifier: Modifier = Modifier,
    blurRadius: Dp = LithosBlur.Standard,  // 20dp per spec
    isDark: Boolean = true,
    cornerRadius: Dp = LithosShapes.Large,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundColor = if (isDark) {
        LithosGlass
    } else {
        LithosGlassLight
    }

    val borderColor = if (isDark) {
        LithosGlassBorder
    } else {
        LithosGlassBorderLight
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                        Color.White.copy(alpha = if (isDark) 0.03f else 0.2f),
                        backgroundColor
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

/**
 * Applies a saturation and brightness boost for vibrancy.
 */
@Composable
fun Modifier.vibrancy(
    saturationBoost: Float = 1.1f  // Reduced for more natural look
): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.graphicsLayer {
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

// ============================================================================
// LITHOS-SPECIFIC GLASS MODIFIERS
// ============================================================================

/**
 * Lithos Amber accent glass - subtle amber tint for accent surfaces.
 * NO glow, matte finish.
 */
@Composable
fun Modifier.lithosAmberGlass(
    cornerRadius: Dp = LithosShapes.Medium
): Modifier {
    return this
        .clip(RoundedCornerShape(cornerRadius))
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    LithosAmber.copy(alpha = 0.08f),
                    LithosSlate.copy(alpha = 0.85f)
                )
            )
        )
        .border(
            width = 0.5.dp,
            color = LithosAmber.copy(alpha = 0.15f),
            shape = RoundedCornerShape(cornerRadius)
        )
}

/**
 * Lithos Moss glass - for Play/Pause button backgrounds.
 * NO glow, matte finish.
 */
@Composable
fun Modifier.lithosMossGlass(
    cornerRadius: Dp = LithosShapes.Medium
): Modifier {
    return this
        .clip(RoundedCornerShape(cornerRadius))
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    LithosMoss.copy(alpha = 0.10f),
                    LithosSlate.copy(alpha = 0.85f)
                )
            )
        )
        .border(
            width = 0.5.dp,
            color = LithosMoss.copy(alpha = 0.15f),
            shape = RoundedCornerShape(cornerRadius)
        )
}
