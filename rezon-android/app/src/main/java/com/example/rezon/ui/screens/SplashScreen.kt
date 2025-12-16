package com.example.rezon.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rezon.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    // Logo Scale/Fade
    val logoAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
    )
    val logoAlpha = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    // Text Slide Up/Fade
    val textOffset = animateDpAsState(
        targetValue = if (startAnimation) 0.dp else 50.dp,
        animationSpec = tween(durationMillis = 1000, delayMillis = 300)
    )
    val textAlpha = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 300)
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3000) // Hold splash for 3 seconds
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505)), // OLED Black
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Vector Logo
            Image(
                painter = painterResource(id = R.drawable.ic_logo_rezon8),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(180.dp)
                    .scale(logoAnim.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animated Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = textOffset.value)
                    .alpha(textAlpha.value)
            ) {
                Text(
                    text = "Rezon8",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00d2ff), // Matching the Logo Blue
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Resonate With Every Word",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
