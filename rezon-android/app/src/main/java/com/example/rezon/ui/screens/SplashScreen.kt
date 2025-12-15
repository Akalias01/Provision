package com.example.rezon.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500)
    )

    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500) // Wait for animation + read time
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
        ) {
            // Logo Placeholder
            Text(
                text = "R",
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF)
            )
            Text(
                text = "REZON",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Audiobooks Reimagined.",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Text(
                text = "Resonate With Every Word.",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}
