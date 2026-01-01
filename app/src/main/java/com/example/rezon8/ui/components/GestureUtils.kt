package com.mossglen.lithos.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Reverie Unified Animation System
 * Android 16-style fluid animations with spring physics
 */
object ReverieAnimations {
    // Spring specs for natural motion - iOS 26 / Apple quality with dampingRatio 0.8f
    val smoothSpring = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessLow
    )

    val quickSpring = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMedium
    )

    val bouncySpring = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessLow
    )

    // Screen transition specs
    val screenEnterSpec = tween<Float>(350, easing = FastOutSlowInEasing)
    val screenExitSpec = tween<Float>(250, easing = FastOutLinearInEasing)

    // Dialog animation specs
    val dialogEnterSpec = tween<Float>(300, easing = FastOutSlowInEasing)
    val dialogExitSpec = tween<Float>(200, easing = FastOutLinearInEasing)

    // Scale for entry animations
    const val ENTRY_SCALE = 0.92f
    const val EXIT_SCALE = 0.95f

    // Swipe thresholds
    const val SWIPE_DISMISS_THRESHOLD = 150f
    const val VELOCITY_THRESHOLD = 1000f
}

/**
 * Swipe direction for gesture handling
 */
enum class SwipeDirection {
    DOWN, RIGHT, LEFT
}

/**
 * State holder for swipe-to-dismiss gesture
 */
class SwipeToDismissState(
    val direction: SwipeDirection = SwipeDirection.DOWN,
    val threshold: Float = ReverieAnimations.SWIPE_DISMISS_THRESHOLD
) {
    var offset by mutableFloatStateOf(0f)
    var isDismissing by mutableStateOf(false)

    val progress: Float
        get() = (offset.absoluteValue / threshold).coerceIn(0f, 1f)

    fun reset() {
        offset = 0f
        isDismissing = false
    }
}

@Composable
fun rememberSwipeToDismissState(
    direction: SwipeDirection = SwipeDirection.DOWN,
    threshold: Float = ReverieAnimations.SWIPE_DISMISS_THRESHOLD
): SwipeToDismissState {
    return remember { SwipeToDismissState(direction, threshold) }
}

/**
 * Modifier for swipe-to-dismiss functionality
 */
fun Modifier.swipeToDismiss(
    state: SwipeToDismissState,
    onDismiss: () -> Unit
): Modifier = this.pointerInput(state.direction) {
    when (state.direction) {
        SwipeDirection.DOWN -> {
            detectVerticalDragGestures(
                onDragEnd = {
                    if (state.offset > state.threshold) {
                        state.isDismissing = true
                        onDismiss()
                    } else {
                        state.offset = 0f
                    }
                },
                onDragCancel = { state.reset() },
                onVerticalDrag = { _, dragAmount ->
                    state.offset = (state.offset + dragAmount).coerceAtLeast(0f)
                }
            )
        }
        SwipeDirection.RIGHT -> {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (state.offset > state.threshold) {
                        state.isDismissing = true
                        onDismiss()
                    } else {
                        state.offset = 0f
                    }
                },
                onDragCancel = { state.reset() },
                onHorizontalDrag = { _, dragAmount ->
                    state.offset = (state.offset + dragAmount).coerceAtLeast(0f)
                }
            )
        }
        SwipeDirection.LEFT -> {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (state.offset.absoluteValue > state.threshold) {
                        state.isDismissing = true
                        onDismiss()
                    } else {
                        state.offset = 0f
                    }
                },
                onDragCancel = { state.reset() },
                onHorizontalDrag = { _, dragAmount ->
                    state.offset = (state.offset + dragAmount).coerceAtMost(0f)
                }
            )
        }
    }
}

/**
 * Drag handle indicator for swipe-dismissable surfaces
 */
@Composable
fun DragHandle(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray.copy(alpha = 0.5f)
) {
    Box(
        modifier = modifier
            .padding(vertical = 8.dp)
            .width(40.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}

/**
 * Animated entry/exit wrapper for screens
 */
@Composable
fun AnimatedScreenContainer(
    visible: Boolean,
    direction: SwipeDirection = SwipeDirection.RIGHT,
    onDismiss: () -> Unit = {},
    enableSwipeToDismiss: Boolean = true,
    content: @Composable () -> Unit
) {
    val swipeState = rememberSwipeToDismissState(direction)

    val animatedOffset by animateFloatAsState(
        targetValue = swipeState.offset,
        animationSpec = ReverieAnimations.smoothSpring,
        label = "screenOffset"
    )

    AnimatedVisibility(
        visible = visible && !swipeState.isDismissing,
        enter = when (direction) {
            SwipeDirection.DOWN -> slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(300))
            SwipeDirection.RIGHT -> slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(300))
            SwipeDirection.LEFT -> slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(300))
        },
        exit = when (direction) {
            SwipeDirection.DOWN -> slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(250, easing = FastOutLinearInEasing)
            ) + fadeOut(tween(200))
            SwipeDirection.RIGHT -> slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(250, easing = FastOutLinearInEasing)
            ) + fadeOut(tween(200))
            SwipeDirection.LEFT -> slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(250, easing = FastOutLinearInEasing)
            ) + fadeOut(tween(200))
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    when (direction) {
                        SwipeDirection.DOWN -> IntOffset(0, animatedOffset.roundToInt())
                        SwipeDirection.RIGHT -> IntOffset(animatedOffset.roundToInt(), 0)
                        SwipeDirection.LEFT -> IntOffset(animatedOffset.roundToInt(), 0)
                    }
                }
                .alpha(1f - (swipeState.progress * 0.3f))
                .scale(1f - (swipeState.progress * 0.05f))
                .then(
                    if (enableSwipeToDismiss) {
                        Modifier.swipeToDismiss(swipeState, onDismiss)
                    } else Modifier
                )
        ) {
            content()
        }
    }
}

/**
 * Standard screen transition animations
 */
object ScreenTransitions {
    val slideFromRight = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(350, easing = FastOutSlowInEasing)
    ) + fadeIn(tween(300))

    val slideToRight = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(250, easing = FastOutLinearInEasing)
    ) + fadeOut(tween(200))

    val slideFromBottom = slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(350, easing = FastOutSlowInEasing)
    ) + fadeIn(tween(300))

    val slideToBottom = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(250, easing = FastOutLinearInEasing)
    ) + fadeOut(tween(200))

    val scaleIn = scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeIn(tween(250))

    val scaleOut = scaleOut(
        targetScale = 0.95f,
        animationSpec = tween(200, easing = FastOutLinearInEasing)
    ) + fadeOut(tween(150))
}
