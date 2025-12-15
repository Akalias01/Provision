package com.rezon.app.presentation.ui.screens.equalizer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rezon.app.presentation.ui.theme.PlayerGradientEnd
import com.rezon.app.presentation.ui.theme.PlayerGradientStart
import com.rezon.app.presentation.ui.theme.ProgressFill
import com.rezon.app.presentation.ui.theme.ProgressTrack
import com.rezon.app.presentation.ui.theme.RezonAccentPink
import com.rezon.app.presentation.ui.theme.RezonCyan
import com.rezon.app.presentation.ui.theme.RezonPurple

/**
 * Equalizer band data
 */
data class EqualizerBand(
    val frequency: String,
    val label: String,
    val value: Float = 0f // -12dB to +12dB
)

/**
 * Equalizer preset
 */
data class EqualizerPreset(
    val name: String,
    val bands: List<Float> // 5 band values
)

/**
 * Default presets
 */
val equalizerPresets = listOf(
    EqualizerPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
    EqualizerPreset("Voice", listOf(-2f, 1f, 4f, 3f, 0f)),
    EqualizerPreset("Bass Boost", listOf(6f, 4f, 0f, -1f, -2f)),
    EqualizerPreset("Treble Boost", listOf(-2f, -1f, 0f, 4f, 6f)),
    EqualizerPreset("Podcast", listOf(-1f, 2f, 5f, 4f, 1f)),
    EqualizerPreset("Audiobook", listOf(0f, 2f, 4f, 3f, -1f)),
    EqualizerPreset("Classical", listOf(4f, 2f, 0f, 2f, 4f)),
    EqualizerPreset("Custom", listOf(0f, 0f, 0f, 0f, 0f))
)

/**
 * REZON Equalizer Screen
 *
 * 5-band audio equalizer with:
 * - Frequency bands: 60Hz, 250Hz, 1kHz, 4kHz, 12kHz
 * - Preset selection
 * - Voice boost toggle
 * - Silence skipping toggle
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onNavigateBack: () -> Unit
) {
    // Band frequencies
    val bands = remember {
        listOf(
            EqualizerBand("60", "Bass"),
            EqualizerBand("250", "Low Mid"),
            EqualizerBand("1k", "Mid"),
            EqualizerBand("4k", "High Mid"),
            EqualizerBand("12k", "Treble")
        )
    }

    // Equalizer state
    var isEnabled by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf(equalizerPresets[0]) }
    var bandValues by remember { mutableStateOf(listOf(0f, 0f, 0f, 0f, 0f)) }
    var voiceBoostEnabled by remember { mutableStateOf(false) }
    var silenceSkipEnabled by remember { mutableStateOf(false) }
    var monoAudioEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = RezonPurple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Equalizer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    // Reset button
                    IconButton(
                        onClick = {
                            bandValues = listOf(0f, 0f, 0f, 0f, 0f)
                            selectedPreset = equalizerPresets[0]
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PlayerGradientStart, PlayerGradientEnd)
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Enable/Disable toggle
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Equalizer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = RezonPurple
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Preset selection
                Text(
                    text = "PRESETS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = RezonPurple,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(equalizerPresets) { preset ->
                        PresetChip(
                            preset = preset,
                            isSelected = selectedPreset == preset,
                            onClick = {
                                selectedPreset = preset
                                if (preset.name != "Custom") {
                                    bandValues = preset.bands
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Equalizer bands
                Text(
                    text = "FREQUENCY BANDS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = RezonPurple,
                    modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        bands.forEachIndexed { index, band ->
                            EqualizerBandSlider(
                                band = band,
                                value = bandValues.getOrElse(index) { 0f },
                                enabled = isEnabled,
                                onValueChange = { newValue ->
                                    bandValues = bandValues.toMutableList().apply {
                                        this[index] = newValue
                                    }
                                    selectedPreset = equalizerPresets.last() // Switch to Custom
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Audio enhancements
                Text(
                    text = "AUDIO ENHANCEMENTS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = RezonPurple,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column {
                        // Voice Boost
                        EnhancementToggle(
                            title = "Voice Boost",
                            subtitle = "Enhance vocal frequencies (1-4 kHz)",
                            checked = voiceBoostEnabled,
                            onCheckedChange = { voiceBoostEnabled = it }
                        )

                        // Silence Skipping
                        EnhancementToggle(
                            title = "Silence Skipping",
                            subtitle = "Automatically skip silent parts",
                            checked = silenceSkipEnabled,
                            onCheckedChange = { silenceSkipEnabled = it }
                        )

                        // Mono Audio
                        EnhancementToggle(
                            title = "Mono Audio",
                            subtitle = "Mix stereo to mono (for single earbud)",
                            checked = monoAudioEnabled,
                            onCheckedChange = { monoAudioEnabled = it }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preset selection chip
 */
@Composable
private fun PresetChip(
    preset: EqualizerPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) RezonPurple else Color.Transparent,
        label = "bgColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) RezonPurple else MaterialTheme.colorScheme.outline,
        label = "borderColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "textColor"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = preset.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

/**
 * Individual equalizer band slider (vertical)
 */
@Composable
private fun EqualizerBandSlider(
    band: EqualizerBand,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        // dB value
        Text(
            text = "${if (value >= 0) "+" else ""}${value.toInt()}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = when {
                value > 0 -> RezonCyan
                value < 0 -> RezonAccentPink
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Vertical slider
        Box(
            modifier = Modifier
                .height(140.dp)
                .width(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = -12f..12f,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = if (enabled) RezonPurple else MaterialTheme.colorScheme.outline,
                    activeTrackColor = ProgressFill,
                    inactiveTrackColor = ProgressTrack
                ),
                modifier = Modifier
                    .width(140.dp)
                    .rotate(-90f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Frequency label
        Text(
            text = "${band.frequency}Hz",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Text(
            text = band.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Enhancement toggle item
 */
@Composable
private fun EnhancementToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RezonPurple
            )
        )
    }
}
