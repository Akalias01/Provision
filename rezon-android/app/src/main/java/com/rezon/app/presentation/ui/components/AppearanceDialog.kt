package com.rezon.app.presentation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rezon.app.presentation.ui.theme.*

/**
 * Theme preset data
 */
data class ThemePreset(
    val name: String,
    val colors: List<Color>,
    val id: String
)

/**
 * Splash animation option
 */
data class SplashAnimation(
    val name: String,
    val description: String,
    val id: String
)

val themePresets = listOf(
    ThemePreset(
        name = "Classic",
        colors = listOf(Color(0xFF7F00FF), Color(0xFFEC4899)),
        id = "classic"
    ),
    ThemePreset(
        name = "Modern",
        colors = listOf(Color(0xFF00E5FF), Color(0xFF0066FF)),
        id = "modern"
    ),
    ThemePreset(
        name = "Basic",
        colors = listOf(Color(0xFF64748B), Color(0xFF475569)),
        id = "basic"
    ),
    ThemePreset(
        name = "Steampunk",
        colors = listOf(Color(0xFFF59E0B), Color(0xFF92400E)),
        id = "steampunk"
    )
)

val splashAnimations = listOf(
    SplashAnimation(
        name = "Pulse Wave",
        description = "Clean, modern pulse rings",
        id = "pulse_wave"
    ),
    SplashAnimation(
        name = "Glitch Cyber",
        description = "Edgy neon glitch effect",
        id = "glitch_cyber"
    ),
    SplashAnimation(
        name = "Waveform Morph",
        description = "Smooth animated waveforms",
        id = "waveform_morph"
    ),
    SplashAnimation(
        name = "Neon Flicker",
        description = "Retro neon sign flicker",
        id = "neon_flicker"
    )
)

@Composable
fun AppearanceDialog(
    currentTheme: String,
    currentLogoStyle: LogoStyle,
    currentSplashAnimation: String,
    onDismiss: () -> Unit,
    onThemeSelected: (String) -> Unit,
    onLogoStyleSelected: (LogoStyle) -> Unit,
    onSplashAnimationSelected: (String) -> Unit,
    onPreviewSplash: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = RezonSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = RezonOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Theme presets
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    themePresets.take(2).forEach { preset ->
                        ThemePresetCard(
                            preset = preset,
                            isSelected = currentTheme == preset.id,
                            onClick = { onThemeSelected(preset.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    themePresets.drop(2).forEach { preset ->
                        ThemePresetCard(
                            preset = preset,
                            isSelected = currentTheme == preset.id,
                            onClick = { onThemeSelected(preset.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Logo Style section
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = RezonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logo Style",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Logo style options - 2x2 grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LogoStyleCard(
                            style = LogoStyle.WAVEFORM,
                            label = "Waveform",
                            isSelected = currentLogoStyle == LogoStyle.WAVEFORM,
                            onClick = { onLogoStyleSelected(LogoStyle.WAVEFORM) },
                            modifier = Modifier.weight(1f)
                        )
                        LogoStyleCard(
                            style = LogoStyle.HEADPHONES,
                            label = "Headphones",
                            isSelected = currentLogoStyle == LogoStyle.HEADPHONES,
                            onClick = { onLogoStyleSelected(LogoStyle.HEADPHONES) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LogoStyleCard(
                            style = LogoStyle.PULSE,
                            label = "Pulse",
                            isSelected = currentLogoStyle == LogoStyle.PULSE,
                            onClick = { onLogoStyleSelected(LogoStyle.PULSE) },
                            modifier = Modifier.weight(1f)
                        )
                        LogoStyleCard(
                            style = LogoStyle.MINIMAL,
                            label = "Minimal",
                            isSelected = currentLogoStyle == LogoStyle.MINIMAL,
                            onClick = { onLogoStyleSelected(LogoStyle.MINIMAL) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Splash Animation section
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Animation,
                        contentDescription = null,
                        tint = RezonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Splash Animation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Splash animation options - 2x2 grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        splashAnimations.take(2).forEach { animation ->
                            SplashAnimationCard(
                                animation = animation,
                                isSelected = currentSplashAnimation == animation.id,
                                onClick = { onSplashAnimationSelected(animation.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        splashAnimations.drop(2).forEach { animation ->
                            SplashAnimationCard(
                                animation = animation,
                                isSelected = currentSplashAnimation == animation.id,
                                onClick = { onSplashAnimationSelected(animation.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Preview button
                OutlinedButton(
                    onClick = onPreviewSplash,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, RezonSurfaceVariant)
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Preview Splash Animation")
                }
            }
        }
    }
}

@Composable
private fun ThemePresetCard(
    preset: ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, RezonCyan, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = RezonSurfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Color preview bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(preset.colors)
                    )
            )

            Text(
                text = preset.name,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun LogoStyleCard(
    style: LogoStyle,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, RezonCyan, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) RezonCyan.copy(alpha = 0.1f) else RezonSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mini logo preview
            RezonLogo(
                size = LogoSize.SMALL,
                style = style,
                showText = true,
                animated = false
            )
        }
    }
}

@Composable
private fun SplashAnimationCard(
    animation: SplashAnimation,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, RezonCyan, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) RezonCyan.copy(alpha = 0.1f) else RezonSurfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = animation.name,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            Text(
                text = animation.description,
                style = MaterialTheme.typography.bodySmall,
                color = RezonOnSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}
