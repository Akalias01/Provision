package com.mossglen.lithos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1000)  // Brief delay for smooth transition
        onAnimationFinished()
    }

    // Minimal splash - no branding, elegant dark background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C)),
        contentAlignment = Alignment.Center
    ) {
        // No logo displayed - will revisit branding later
    }
}
