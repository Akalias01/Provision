package com.mossglen.reverie.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mossglen.reverie.ui.theme.*
import com.mossglen.reverie.haptics.HapticType
import com.mossglen.reverie.haptics.performHaptic
import androidx.compose.ui.platform.LocalView

/**
 * REVERIE Glass Components
 *
 * Reusable UI components following the Glass design system.
 * Real blur effects using Android 12+ RenderEffect API.
 * Haptic feedback on interactions.
 * All components support dark/light mode and maintain consistent styling.
 */

// ============================================================================
// GLASS SURFACE - Base container with REAL blur effect
// ============================================================================

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    shape: Dp = GlassShapes.Medium,
    hasBorder: Boolean = true,
    blurRadius: Dp = GlassBlur.Medium,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.glassEffect(
            blurRadius = blurRadius,
            isDark = isDark,
            cornerRadius = shape,
            borderWidth = if (hasBorder) 0.5.dp else 0.dp
        ),
        content = content
    )
}

// ============================================================================
// GLASS CARD - Elevated card with REAL blur effect + Haptics
// ============================================================================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    // Trigger haptic on press
    LaunchedEffect(isPressed) {
        if (isPressed && onClick != null) {
            view.performHaptic(HapticType.LightTap)
        }
    }

    Column(
        modifier = modifier
            .scale(scale)
            .glassCard(isDark = isDark)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        content = content
    )
}

// ============================================================================
// GLASS BUTTON - Primary action button + Haptics
// ============================================================================

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    isPrimary: Boolean = true,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val view = LocalView.current
    val theme = glassTheme(isDark)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    // Haptic feedback on press
    LaunchedEffect(isPressed) {
        if (isPressed && enabled) {
            view.performHaptic(HapticType.MediumTap)
        }
    }

    val backgroundColor = if (isPrimary) {
        if (enabled) theme.interactive else theme.interactive.copy(alpha = 0.3f)
    } else {
        theme.glassPrimary
    }

    val textColor = if (isPrimary) {
        Color.White
    } else {
        if (enabled) theme.textPrimary else theme.textSecondary
    }

    Row(
        modifier = modifier
            .scale(scale)
            .height(GlassTouchTarget.Large)
            .clip(RoundedCornerShape(GlassShapes.Small))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = GlassSpacing.L),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(GlassIconSize.Medium)
            )
            Spacer(modifier = Modifier.width(GlassSpacing.XS))
        }
        Text(
            text = text,
            color = textColor,
            style = GlassTypography.Label,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ============================================================================
// GLASS ICON BUTTON - Circular icon button
// ============================================================================

@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    size: Dp = GlassTouchTarget.Standard,
    iconSize: Dp = GlassIconSize.Medium,
    tint: Color? = null,
    hasBackground: Boolean = false
) {
    val view = LocalView.current
    val theme = glassTheme(isDark)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    // Haptic feedback on press
    LaunchedEffect(isPressed) {
        if (isPressed) {
            view.performHaptic(HapticType.LightTap)
        }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .then(
                if (hasBackground) {
                    Modifier.background(theme.glassPrimary)
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint ?: theme.textPrimary,
            modifier = Modifier.size(iconSize)
        )
    }
}

// ============================================================================
// GLASS PLAY BUTTON - Large circular play/pause button + Haptics
// ============================================================================

@Composable
fun GlassPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    iconSize: Dp = 32.dp
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    // Satisfying haptic on play/pause
    LaunchedEffect(isPressed) {
        if (isPressed) {
            view.performHaptic(HapticType.Confirm)
        }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.Black,
            modifier = Modifier.size(iconSize)
        )
    }
}

// ============================================================================
// GLASS SEGMENTED CONTROL - iOS-style segmented picker
// ============================================================================

@Composable
fun GlassSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true
) {
    val view = LocalView.current
    val theme = glassTheme(isDark)

    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(GlassShapes.Small))
            .background(theme.glassSecondary)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) theme.glassPrimary else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                label = "bg"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(GlassShapes.Small - 2.dp))
                    .background(backgroundColor)
                    .clickable {
                        view.performHaptic(HapticType.LightTap)
                        onItemSelected(index)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    color = if (isSelected) theme.textPrimary else theme.textSecondary,
                    style = GlassTypography.Caption,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// ============================================================================
// GLASS BOTTOM BAR - Floating navigation bar
// ============================================================================

data class GlassNavItem(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String,
    val route: String
)

@Composable
fun GlassBottomBar(
    items: List<GlassNavItem>,
    selectedRoute: String,
    onItemSelected: (GlassNavItem) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true
) {
    val view = LocalView.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.M, vertical = GlassSpacing.S)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .glassNavBar(isDark = isDark)
                .padding(horizontal = GlassSpacing.S),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.route == selectedRoute
                GlassNavItemView(
                    item = item,
                    isSelected = isSelected,
                    onClick = {
                        view.performHaptic(HapticType.LightTap)
                        onItemSelected(item)
                    },
                    theme = glassTheme(isDark)
                )
            }
        }
    }
}

@Composable
private fun GlassNavItemView(
    item: GlassNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: GlassThemeData
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(GlassShapes.Small))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = GlassSpacing.M, vertical = GlassSpacing.XS),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.icon,
            contentDescription = item.label,
            tint = if (isSelected) theme.interactive else theme.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.label,
            color = if (isSelected) theme.interactive else theme.textSecondary,
            style = GlassTypography.Tab,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ============================================================================
// GLASS SLIDER - Clean progress/seek slider
// ============================================================================

@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    enabled: Boolean = true
) {
    val theme = glassTheme(isDark)

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(GlassTouchTarget.Minimum),
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color.White,
            inactiveTrackColor = if (isDark) {
                Color.White.copy(alpha = 0.2f)
            } else {
                Color.Black.copy(alpha = 0.15f)
            },
            disabledThumbColor = Color.Gray,
            disabledActiveTrackColor = Color.Gray,
            disabledInactiveTrackColor = Color.Gray.copy(alpha = 0.2f)
        )
    )
}

// ============================================================================
// GLASS LIST ITEM - Settings/list row
// ============================================================================

@Composable
fun GlassListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    leadingIcon: ImageVector? = null,
    showChevron: Boolean = true,
    isDark: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val view = LocalView.current
    val theme = glassTheme(isDark)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        view.performHaptic(HapticType.LightTap)
                        onClick()
                    }
                } else Modifier
            )
            .padding(horizontal = GlassSpacing.M, vertical = GlassSpacing.S + GlassSpacing.XXS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = theme.textSecondary,
                modifier = Modifier.size(GlassIconSize.Medium)
            )
            Spacer(modifier = Modifier.width(GlassSpacing.S))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = theme.textPrimary,
                style = GlassTypography.Body
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = theme.textSecondary,
                    style = GlassTypography.Caption
                )
            }
        }

        if (value != null) {
            Text(
                text = value,
                color = theme.textSecondary,
                style = GlassTypography.Body
            )
            Spacer(modifier = Modifier.width(GlassSpacing.XS))
        }

        if (showChevron && onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = theme.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================================================
// GLASS SECTION HEADER - For grouped content
// ============================================================================

@Composable
fun GlassSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    isDark: Boolean = true
) {
    val theme = glassTheme(isDark)

    Text(
        text = title.uppercase(),
        color = theme.textSecondary,
        style = GlassTypography.Caption,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        modifier = modifier.padding(
            start = GlassSpacing.M,
            top = GlassSpacing.L,
            bottom = GlassSpacing.XS
        )
    )
}

// ============================================================================
// GLASS DIVIDER - Subtle separator
// ============================================================================

@Composable
fun GlassDivider(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    startIndent: Dp = 0.dp
) {
    val theme = glassTheme(isDark)

    HorizontalDivider(
        modifier = modifier.padding(start = startIndent),
        thickness = 0.5.dp,
        color = theme.divider
    )
}

// ============================================================================
// GLASS CHIP - Small tag/filter chip
// ============================================================================

@Composable
fun GlassChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true
) {
    val view = LocalView.current
    val theme = glassTheme(isDark)

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) theme.interactive else theme.glassSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "bg"
    )

    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable {
                view.performHaptic(HapticType.LightTap)
                onClick()
            }
            .padding(horizontal = GlassSpacing.S),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else theme.textSecondary,
            style = GlassTypography.Caption,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ============================================================================
// RESUME CONFIRMATION DIALOG - Ask user to resume from last position
// ============================================================================

@Composable
fun ResumeConfirmationDialog(
    bookTitle: String,
    progressTime: String,
    onResume: () -> Unit,
    onStartOver: () -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean = true
) {
    val view = LocalView.current
    val theme = glassTheme(isDark)

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 300.dp, max = 380.dp)
                    .fillMaxWidth(0.85f)
                    .glassCard(isDark = isDark)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Consume clicks */ }
                    .padding(GlassSpacing.L),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(GlassSpacing.M)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(theme.interactive.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = theme.interactive,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Title
                Text(
                    text = "Resume Playback?",
                    style = GlassTypography.Title,
                    color = theme.textPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Message
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(GlassSpacing.XXS)
                ) {
                    Text(
                        text = bookTitle,
                        style = GlassTypography.Body,
                        color = theme.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Resume from $progressTime?",
                        style = GlassTypography.Body,
                        color = theme.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(GlassSpacing.XS))

                // Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(GlassSpacing.S)
                ) {
                    // Resume button (primary)
                    GlassButton(
                        text = "Resume",
                        onClick = {
                            view.performHaptic(HapticType.Confirm)
                            onResume()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isDark = isDark,
                        isPrimary = true,
                        icon = Icons.Default.PlayArrow
                    )

                    // Start Over button (secondary)
                    GlassButton(
                        text = "Start Over",
                        onClick = {
                            view.performHaptic(HapticType.MediumTap)
                            onStartOver()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isDark = isDark,
                        isPrimary = false,
                        icon = Icons.Default.Refresh
                    )
                }
            }
        }
    }
}
