package com.mossglen.lithos.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mossglen.lithos.ui.theme.LithosSlate
import com.mossglen.lithos.ui.theme.LithosBlack
import com.mossglen.lithos.ui.theme.LithosOat
import com.mossglen.lithos.ui.theme.LithosSurfaceDark
import kotlin.math.roundToInt

/**
 * Premium LITHOS Dialog with swipe-to-dismiss and fluid animations
 *
 * Supports all 3 Lithos modes:
 * - Light: Oat background (#F2F0E9)
 * - Standard: Slate background (#1A1D21)
 * - OLED Black: True black (#000000)
 */
@Composable
fun LithosDialog(
    title: String,
    isDark: Boolean = true,
    isOLED: Boolean = false,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = com.mossglen.lithos.ui.theme.glassTheme(isDark, isOLED)
    val dialogBackground = when {
        isOLED -> LithosBlack             // True black for OLED
        isDark -> LithosSurfaceDark       // Elevated slate for dark mode
        else -> LithosOat                 // Light mode
    }
    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 120f

    // Entry animation trigger
    LaunchedEffect(Unit) { isVisible = true }

    // Animated values with spring physics - iOS 26 / Apple quality (dampingRatio 0.8f)
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessLow
        ),
        label = "dialogOffset"
    )

    val dialogScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "dialogScale"
    )

    val dialogAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(250),
        label = "dialogAlpha"
    )

    // Calculate dismiss progress for visual feedback
    val dismissProgress = (dragOffsetY / dismissThreshold).coerceIn(0f, 1f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * (1f - dismissProgress * 0.5f)))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 300.dp, max = 400.dp)
                    .fillMaxWidth(0.9f)
                    .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
                    .scale(dialogScale * (1f - dismissProgress * 0.05f))
                    .alpha(dialogAlpha * (1f - dismissProgress * 0.3f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(dialogBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Consume clicks */ }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffsetY > dismissThreshold) {
                                    onDismiss()
                                } else {
                                    dragOffsetY = 0f
                                }
                            },
                            onDragCancel = { dragOffsetY = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                DragHandle(
                    modifier = Modifier.padding(top = 12.dp),
                    color = Color.Gray.copy(alpha = 0.4f + dismissProgress * 0.3f)
                )

                // Title
                Text(
                    text = title,
                    color = theme.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 8.dp, bottom = 16.dp)
                )

                // Content
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    content()
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Full-screen dialog/sheet with swipe-to-dismiss (like the EQ dialog)
 *
 * Supports all 3 Lithos modes:
 * - Light: Oat background (#F2F0E9)
 * - Standard: Slate background (#1A1D21)
 * - OLED Black: True black (#000000)
 */
@Composable
fun LithosFullScreenDialog(
    isDark: Boolean = true,
    isOLED: Boolean = false,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val sheetBackground = when {
        isOLED -> LithosBlack       // True black for OLED
        isDark -> LithosSlate       // Standard dark (full screen uses base slate)
        else -> LithosOat           // Light mode
    }
    var isVisible by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 150f

    LaunchedEffect(Unit) { isVisible = true }

    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessLow
        ),
        label = "sheetOffset"
    )

    val sheetScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.92f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "sheetScale"
    )

    val sheetAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "sheetAlpha"
    )

    val dismissProgress = (dragOffsetY / dismissThreshold).coerceIn(0f, 1f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
                .scale(sheetScale * (1f - dismissProgress * 0.03f))
                .alpha(sheetAlpha * (1f - dismissProgress * 0.2f))
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(sheetBackground)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffsetY > dismissThreshold) {
                                onDismiss()
                            } else {
                                dragOffsetY = 0f
                            }
                        },
                        onDragCancel = { dragOffsetY = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DragHandle(modifier = Modifier.padding(top = 8.dp))
            content()
        }
    }
}

// Alias for compatibility with legacy code
@Composable
fun DialogItem(
    icon: ImageVector,
    text: String,
    accentColor: Color,
    onClick: () -> Unit
) = LithosDialogItem(icon = icon, text = text, accentColor = accentColor, onClick = onClick)

@Composable
fun LithosDialogItem(
    icon: ImageVector,
    text: String,
    accentColor: Color,
    isDark: Boolean = true,
    onClick: () -> Unit
) {
    val textColor = if (isDark) Color.White else Color(0xFF1A1D21)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
