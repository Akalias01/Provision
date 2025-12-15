package com.rezon.app.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rezon.app.presentation.ui.theme.*
import kotlin.math.sin

/**
 * REZON Logo Component
 * Modern, high-quality logo with multiple style options
 */

enum class LogoStyle {
    WAVEFORM,    // Audio waveform bars in the R
    HEADPHONES,  // Headphone icon in the R
    PULSE,       // Pulsing circle animation
    MINIMAL      // Clean, simple R
}

enum class LogoSize {
    SMALL,   // For navigation bars
    MEDIUM,  // For headers
    LARGE,   // For welcome screens
    XLARGE   // For splash screen
}

@Composable
fun RezonLogo(
    modifier: Modifier = Modifier,
    size: LogoSize = LogoSize.MEDIUM,
    style: LogoStyle = LogoStyle.WAVEFORM,
    animated: Boolean = false,
    showText: Boolean = true
) {
    val iconSize = when (size) {
        LogoSize.SMALL -> 28.dp
        LogoSize.MEDIUM -> 36.dp
        LogoSize.LARGE -> 48.dp
        LogoSize.XLARGE -> 72.dp
    }

    val textSize = when (size) {
        LogoSize.SMALL -> 18.sp
        LogoSize.MEDIUM -> 24.sp
        LogoSize.LARGE -> 32.sp
        LogoSize.XLARGE -> 48.sp
    }

    val letterSpacing = when (size) {
        LogoSize.SMALL -> 1.sp
        LogoSize.MEDIUM -> 2.sp
        LogoSize.LARGE -> 3.sp
        LogoSize.XLARGE -> 4.sp
    }

    val spacing = when (size) {
        LogoSize.SMALL -> 4.dp
        LogoSize.MEDIUM -> 5.dp
        LogoSize.LARGE -> 6.dp
        LogoSize.XLARGE -> 8.dp
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo Icon
        LogoIcon(
            size = iconSize,
            style = style,
            animated = animated
        )

        if (showText) {
            Spacer(modifier = Modifier.width(spacing))

            // "REZON" text - themed with cyan to match logo
            Text(
                text = "REZON",
                fontSize = textSize,
                fontWeight = FontWeight.Black,
                letterSpacing = letterSpacing,
                color = RezonCyan
            )
        }
    }
}

@Composable
private fun LogoIcon(
    size: Dp,
    style: LogoStyle,
    animated: Boolean
) {
    val cornerRadius = size / 5

    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = RezonCyan.copy(alpha = 0.3f),
                spotColor = RezonCyan.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(RezonCyan, RezonCyanDark),
                    start = Offset(0f, 0f),
                    end = Offset(size.value * 2, size.value * 2)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            LogoStyle.WAVEFORM -> WaveformR(size = size, animated = animated)
            LogoStyle.HEADPHONES -> HeadphonesR(size = size)
            LogoStyle.PULSE -> PulseR(size = size, animated = animated)
            LogoStyle.MINIMAL -> MinimalR(size = size)
        }
    }
}

@Composable
private fun WaveformR(size: Dp, animated: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    val animatedHeights = if (animated) {
        List(5) { index ->
            infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600 + index * 100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
        }
    } else {
        null
    }

    Box(
        modifier = Modifier.size(size * 0.65f),
        contentAlignment = Alignment.Center
    ) {
        // Draw R with waveform
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height

            // R shape path
            val rPath = Path().apply {
                // Vertical stem
                moveTo(canvasWidth * 0.2f, canvasHeight * 0.85f)
                lineTo(canvasWidth * 0.2f, canvasHeight * 0.15f)

                // Top curve of R
                lineTo(canvasWidth * 0.5f, canvasHeight * 0.15f)
                quadraticBezierTo(
                    canvasWidth * 0.8f, canvasHeight * 0.15f,
                    canvasWidth * 0.8f, canvasHeight * 0.35f
                )
                quadraticBezierTo(
                    canvasWidth * 0.8f, canvasHeight * 0.52f,
                    canvasWidth * 0.5f, canvasHeight * 0.52f
                )

                // Diagonal leg
                lineTo(canvasWidth * 0.35f, canvasHeight * 0.52f)
                lineTo(canvasWidth * 0.8f, canvasHeight * 0.85f)
            }

            drawPath(
                path = rPath,
                color = Color.White,
                style = Stroke(width = canvasWidth * 0.12f, cap = StrokeCap.Round)
            )

            // Waveform bars inside R bowl
            val barWidth = canvasWidth * 0.06f
            val maxBarHeight = canvasHeight * 0.2f
            val startX = canvasWidth * 0.35f
            val centerY = canvasHeight * 0.35f

            val heights = if (animated && animatedHeights != null) {
                animatedHeights.map { it.value }
            } else {
                listOf(0.5f, 0.8f, 1f, 0.7f, 0.4f)
            }

            heights.forEachIndexed { index, heightFactor ->
                val x = startX + index * (barWidth * 1.8f)
                val barHeight = maxBarHeight * heightFactor
                drawRoundRect(
                    color = RezonCyan,
                    topLeft = Offset(x, centerY - barHeight / 2),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2)
                )
            }
        }
    }
}

@Composable
private fun HeadphonesR(size: Dp) {
    Box(
        modifier = Modifier.size(size * 0.65f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height

            // R letter
            val rPath = Path().apply {
                moveTo(canvasWidth * 0.2f, canvasHeight * 0.85f)
                lineTo(canvasWidth * 0.2f, canvasHeight * 0.15f)
                lineTo(canvasWidth * 0.5f, canvasHeight * 0.15f)
                quadraticBezierTo(
                    canvasWidth * 0.75f, canvasHeight * 0.15f,
                    canvasWidth * 0.75f, canvasHeight * 0.35f
                )
                quadraticBezierTo(
                    canvasWidth * 0.75f, canvasHeight * 0.52f,
                    canvasWidth * 0.45f, canvasHeight * 0.52f
                )
                lineTo(canvasWidth * 0.35f, canvasHeight * 0.52f)
                lineTo(canvasWidth * 0.75f, canvasHeight * 0.85f)
            }

            drawPath(
                path = rPath,
                color = Color.White,
                style = Stroke(width = canvasWidth * 0.13f, cap = StrokeCap.Round)
            )

            // Small headphone arc in the R bowl
            val arcPath = Path().apply {
                moveTo(canvasWidth * 0.38f, canvasHeight * 0.42f)
                quadraticBezierTo(
                    canvasWidth * 0.5f, canvasHeight * 0.25f,
                    canvasWidth * 0.62f, canvasHeight * 0.42f
                )
            }

            drawPath(
                path = arcPath,
                color = RezonCyan,
                style = Stroke(width = canvasWidth * 0.06f, cap = StrokeCap.Round)
            )

            // Headphone ear cups
            drawCircle(
                color = RezonCyan,
                radius = canvasWidth * 0.05f,
                center = Offset(canvasWidth * 0.38f, canvasHeight * 0.44f)
            )
            drawCircle(
                color = RezonCyan,
                radius = canvasWidth * 0.05f,
                center = Offset(canvasWidth * 0.62f, canvasHeight * 0.44f)
            )
        }
    }
}

@Composable
private fun PulseR(size: Dp, animated: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulseScale = if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        null
    }

    val pulseAlpha = if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha"
        )
    } else {
        null
    }

    Box(
        modifier = Modifier.size(size * 0.65f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height

            // Pulse rings
            if (animated && pulseScale != null && pulseAlpha != null) {
                drawCircle(
                    color = RezonCyan.copy(alpha = pulseAlpha.value),
                    radius = canvasWidth * 0.4f * pulseScale.value,
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.35f)
                )
            }

            // R letter
            val rPath = Path().apply {
                moveTo(canvasWidth * 0.2f, canvasHeight * 0.85f)
                lineTo(canvasWidth * 0.2f, canvasHeight * 0.15f)
                lineTo(canvasWidth * 0.5f, canvasHeight * 0.15f)
                quadraticBezierTo(
                    canvasWidth * 0.8f, canvasHeight * 0.15f,
                    canvasWidth * 0.8f, canvasHeight * 0.35f
                )
                quadraticBezierTo(
                    canvasWidth * 0.8f, canvasHeight * 0.52f,
                    canvasWidth * 0.5f, canvasHeight * 0.52f
                )
                lineTo(canvasWidth * 0.35f, canvasHeight * 0.52f)
                lineTo(canvasWidth * 0.8f, canvasHeight * 0.85f)
            }

            drawPath(
                path = rPath,
                color = Color.White,
                style = Stroke(width = canvasWidth * 0.12f, cap = StrokeCap.Round)
            )

            // Center dot
            drawCircle(
                color = RezonCyan,
                radius = canvasWidth * 0.08f,
                center = Offset(canvasWidth * 0.5f, canvasHeight * 0.35f)
            )
        }
    }
}

@Composable
private fun MinimalR(size: Dp) {
    Box(
        modifier = Modifier.size(size * 0.65f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height

            // Clean R letter
            val rPath = Path().apply {
                moveTo(canvasWidth * 0.2f, canvasHeight * 0.85f)
                lineTo(canvasWidth * 0.2f, canvasHeight * 0.15f)
                lineTo(canvasWidth * 0.5f, canvasHeight * 0.15f)
                quadraticBezierTo(
                    canvasWidth * 0.8f, canvasHeight * 0.15f,
                    canvasWidth * 0.8f, canvasHeight * 0.35f
                )
                quadraticBezierTo(
                    canvasWidth * 0.8f, canvasHeight * 0.52f,
                    canvasWidth * 0.5f, canvasHeight * 0.52f
                )
                lineTo(canvasWidth * 0.35f, canvasHeight * 0.52f)
                lineTo(canvasWidth * 0.8f, canvasHeight * 0.85f)
            }

            drawPath(
                path = rPath,
                color = Color.White,
                style = Stroke(width = canvasWidth * 0.14f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Full REZON logo with R icon and full "REZON" text
 * Use this for welcome screens and splash
 */
@Composable
fun RezonFullLogo(
    modifier: Modifier = Modifier,
    size: LogoSize = LogoSize.LARGE,
    style: LogoStyle = LogoStyle.WAVEFORM,
    animated: Boolean = false
) {
    val iconSize = when (size) {
        LogoSize.SMALL -> 32.dp
        LogoSize.MEDIUM -> 44.dp
        LogoSize.LARGE -> 60.dp
        LogoSize.XLARGE -> 80.dp
    }

    val textSize = when (size) {
        LogoSize.SMALL -> 20.sp
        LogoSize.MEDIUM -> 28.sp
        LogoSize.LARGE -> 38.sp
        LogoSize.XLARGE -> 52.sp
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogoIcon(
            size = iconSize,
            style = style,
            animated = animated
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "REZON",
            fontSize = textSize,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            color = RezonCyan
        )
    }
}
