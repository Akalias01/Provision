package com.rezon.app.presentation.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rezon.app.presentation.ui.theme.RezonAccentPink
import com.rezon.app.presentation.ui.theme.RezonBackground
import com.rezon.app.presentation.ui.theme.RezonCyan
import com.rezon.app.presentation.ui.theme.RezonPurple
import com.rezon.app.presentation.ui.theme.RezonPurpleLight
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * REZON Splash Screen
 *
 * Cinematic particle/audio wave effect where the logo emerges from sound visualization.
 * Features:
 * - Animated audio wave visualization
 * - Particle system with neon colors
 * - 3D-style logo reveal
 * - Smooth transition to Library
 */
@Composable
fun SplashScreen(
    onNavigateToLibrary: () -> Unit
) {
    // Animation states
    val logoScale = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    var showParticles by remember { mutableStateOf(true) }

    // Infinite wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    // Particle rotation
    val particleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleRotation"
    )

    // Animation sequence
    LaunchedEffect(Unit) {
        delay(500)

        // Logo appears with spring animation
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        logoAlpha.animateTo(1f, tween(500))

        delay(200)

        // Text fades in
        textAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))

        delay(200)

        // Tagline appears
        taglineAlpha.animateTo(1f, tween(600))

        delay(1000)

        // Hide particles and navigate
        showParticles = false
        delay(500)
        onNavigateToLibrary()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RezonBackground),
        contentAlignment = Alignment.Center
    ) {
        // Audio wave visualization background
        AudioWaveVisualization(
            modifier = Modifier.fillMaxSize(),
            phase = wavePhase,
            alpha = if (showParticles) 0.3f else 0f
        )

        // Particle system
        if (showParticles) {
            ParticleSystem(
                modifier = Modifier.fillMaxSize(),
                rotation = particleRotation
            )
        }

        // Logo and text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // REZON Logo with neon glow effect
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            ) {
                // Glow effect behind text
                Text(
                    text = "REZON",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp
                    ),
                    color = RezonCyan.copy(alpha = 0.4f),
                    modifier = Modifier
                        .blur(20.dp)
                        .offset(x = 2.dp, y = 2.dp)
                )
                // Main REZON text
                Text(
                    text = "REZON",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp
                    ),
                    color = RezonCyan,
                    modifier = Modifier.alpha(textAlpha.value)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tagline - Two lines
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(taglineAlpha.value)
            ) {
                Text(
                    text = "AUDIOBOOKS REIMAGINED",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Every Word Resonates",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = RezonCyan
                )
            }
        }
    }
}

/**
 * Audio wave visualization
 */
@Composable
private fun AudioWaveVisualization(
    modifier: Modifier = Modifier,
    phase: Float,
    alpha: Float
) {
    val waveColors = listOf(RezonPurple, RezonCyan, RezonPurpleLight, RezonAccentPink)

    Canvas(modifier = modifier.alpha(alpha)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        // Draw multiple waves
        waveColors.forEachIndexed { index, color ->
            val amplitude = height * 0.1f * (1f - index * 0.15f)
            val frequency = 0.01f + index * 0.005f
            val phaseOffset = phase + index * PI.toFloat() / 4

            val path = Path().apply {
                moveTo(0f, centerY)
                for (x in 0..width.toInt() step 4) {
                    val y = centerY + amplitude * sin(x * frequency + phaseOffset)
                    lineTo(x.toFloat(), y)
                }
            }

            drawPath(
                path = path,
                color = color.copy(alpha = 0.5f - index * 0.1f),
                style = Stroke(
                    width = 3f - index * 0.5f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

/**
 * Particle system with neon colors
 */
@Composable
private fun ParticleSystem(
    modifier: Modifier = Modifier,
    rotation: Float
) {
    val particles = remember {
        List(50) { Particle.random() }
    }

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        rotate(rotation, pivot = Offset(centerX, centerY)) {
            particles.forEach { particle ->
                val x = centerX + particle.distance * cos(particle.angle)
                val y = centerY + particle.distance * sin(particle.angle)

                // Draw glow
                drawCircle(
                    color = particle.color.copy(alpha = 0.3f),
                    radius = particle.size * 2,
                    center = Offset(x, y)
                )

                // Draw particle
                drawCircle(
                    color = particle.color,
                    radius = particle.size,
                    center = Offset(x, y)
                )
            }
        }
    }
}

/**
 * Particle data class
 */
private data class Particle(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val color: Color
) {
    companion object {
        private val colors = listOf(
            RezonPurple,
            RezonCyan,
            RezonPurpleLight,
            RezonAccentPink,
            Color(0xFF22D3EE) // Cyan
        )

        fun random() = Particle(
            angle = Random.nextFloat() * 2 * PI.toFloat(),
            distance = Random.nextFloat() * 400f + 100f,
            size = Random.nextFloat() * 4f + 2f,
            color = colors.random()
        )
    }
}
