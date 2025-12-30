package com.mossglen.reverie.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mossglen.reverie.data.AudioEffectManager
import com.mossglen.reverie.ui.theme.*
import com.mossglen.reverie.ui.viewmodel.SettingsViewModel

/**
 * Equalizer Screen
 *
 * 10-band graphic equalizer with premium visualization.
 * Uses the existing AudioEffectManager for real audio processing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    isDark: Boolean = true,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.ReverieAccent,
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    // Get AudioEffectManager from SettingsViewModel (proper Hilt injection)
    val audioEffectManager = settingsViewModel.audioEffectManager
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current

    // EQ State
    val eqEnabled by audioEffectManager.eqEnabled.collectAsState()
    val bands by audioEffectManager.bands.collectAsState()
    val preamp by audioEffectManager.preamp.collectAsState()
    val selectedPresetName by audioEffectManager.selectedPresetName.collectAsState()

    // Additional effects
    val bassBoostStrength by audioEffectManager.bassBoostStrength.collectAsState()
    val virtualizerStrength by audioEffectManager.virtualizerStrength.collectAsState()

    var showPresetMenu by remember { mutableStateOf(false) }

    // Reinitialize audio effects when screen opens to ensure fresh connection
    // This guarantees the EQ is bound to the current audio session
    LaunchedEffect(Unit) {
        audioEffectManager.reinitialize()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Equalizer",
                            style = GlassTypography.Title,
                            color = theme.textPrimary
                        )
                        Text(
                            text = if (eqEnabled) "Active - $selectedPresetName" else "Disabled",
                            style = GlassTypography.Caption,
                            color = if (eqEnabled) accentColor else theme.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.textPrimary
                        )
                    }
                },
                actions = {
                    // Reset button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            audioEffectManager.reset()
                        }
                    ) {
                        Icon(
                            Icons.Rounded.RestartAlt,
                            contentDescription = "Reset",
                            tint = theme.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = theme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GlassSpacing.M),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(GlassSpacing.M))

            // Master Enable Toggle
            MasterToggleCard(
                enabled = eqEnabled,
                accentColor = accentColor,
                isDark = isDark,
                isReverieDark = isReverieDark,
                onToggle = { audioEffectManager.setEqEnabled(it) }
            )

            Spacer(modifier = Modifier.height(GlassSpacing.L))

            // Preset Selector
            PresetSelectorCard(
                selectedPreset = selectedPresetName,
                accentColor = accentColor,
                isDark = isDark,
                isReverieDark = isReverieDark,
                onClick = { showPresetMenu = true }
            )

            Spacer(modifier = Modifier.height(GlassSpacing.L))

            // 10-Band Graphic EQ
            TenBandEqualizerCard(
                bands = bands,
                accentColor = accentColor,
                isDark = isDark,
                isReverieDark = isReverieDark,
                enabled = eqEnabled,
                onBandChange = { index, level ->
                    audioEffectManager.setBandLevel(index, level)
                }
            )

            Spacer(modifier = Modifier.height(GlassSpacing.L))

            // Preamp Control
            PreampCard(
                preampDb = preamp,
                accentColor = accentColor,
                isDark = isDark,
                isReverieDark = isReverieDark,
                enabled = eqEnabled,
                onPreampChange = { audioEffectManager.setPreamp(it) }
            )

            Spacer(modifier = Modifier.height(GlassSpacing.L))

            // Audio Effects Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GlassSpacing.M)
            ) {
                // Bass Boost
                EffectCard(
                    title = "Bass Boost",
                    icon = Icons.Rounded.GraphicEq,
                    strength = bassBoostStrength,
                    accentColor = accentColor,
                    isDark = isDark,
                    isReverieDark = isReverieDark,
                    enabled = eqEnabled,
                    modifier = Modifier.weight(1f),
                    onStrengthChange = { audioEffectManager.setBassBoost(it) }
                )

                // Virtualizer
                EffectCard(
                    title = "3D Audio",
                    icon = Icons.Rounded.SurroundSound,
                    strength = virtualizerStrength,
                    accentColor = accentColor,
                    isDark = isDark,
                    isReverieDark = isReverieDark,
                    enabled = eqEnabled,
                    modifier = Modifier.weight(1f),
                    onStrengthChange = { audioEffectManager.setVirtualizer(it) }
                )
            }

            Spacer(modifier = Modifier.height(GlassSpacing.XXL))
        }
    }

    // Preset Selection Dialog
    if (showPresetMenu) {
        PresetSelectionDialog(
            isDark = isDark,
            isReverieDark = isReverieDark,
            accentColor = accentColor,
            currentPreset = selectedPresetName,
            onPresetSelected = { presetName, bands, preampDb ->
                audioEffectManager.applyPreset(presetName, bands, preampDb)
                showPresetMenu = false
            },
            onDismiss = { showPresetMenu = false }
        )
    }
}

// ============================================================================
// MASTER TOGGLE CARD
// ============================================================================

@Composable
private fun MasterToggleCard(
    enabled: Boolean,
    accentColor: Color,
    isDark: Boolean,
    isReverieDark: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current
    var isEnabled by remember { mutableStateOf(enabled) }

    LaunchedEffect(enabled) {
        isEnabled = enabled
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(theme.glassSecondary)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                isEnabled = !isEnabled
                onToggle(isEnabled)
            }
            .padding(GlassSpacing.L),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Equalizer",
                style = GlassTypography.Headline,
                fontWeight = FontWeight.SemiBold,
                color = if (isEnabled) accentColor else theme.textPrimary
            )
            Text(
                text = if (isEnabled) "Audio processing active" else "Tap to enable",
                style = GlassTypography.Caption,
                color = theme.textSecondary
            )
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                isEnabled = it
                onToggle(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                uncheckedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedTrackColor = if (isReverieDark) Color.White.copy(alpha = 0.12f) else theme.glassBorder,
                checkedBorderColor = Color.Transparent,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

// ============================================================================
// PRESET SELECTOR CARD
// ============================================================================

@Composable
private fun PresetSelectorCard(
    selectedPreset: String,
    accentColor: Color,
    isDark: Boolean,
    isReverieDark: Boolean,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(theme.glassSecondary)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            .padding(GlassSpacing.L),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Preset",
                style = GlassTypography.Caption,
                color = theme.textSecondary
            )
            Text(
                text = selectedPreset,
                style = GlassTypography.Headline,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }

        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(32.dp)
        )
    }
}

// ============================================================================
// 10-BAND EQUALIZER CARD
// ============================================================================

@Composable
private fun TenBandEqualizerCard(
    bands: List<Float>,
    accentColor: Color,
    isDark: Boolean,
    isReverieDark: Boolean,
    enabled: Boolean,
    onBandChange: (Int, Float) -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val frequencies = listOf(31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(theme.glassSecondary)
            .padding(GlassSpacing.L)
    ) {
        Text(
            text = "10-Band Graphic Equalizer",
            style = GlassTypography.Body,
            fontWeight = FontWeight.SemiBold,
            color = theme.textPrimary,
            modifier = Modifier.padding(bottom = GlassSpacing.M)
        )

        // EQ Sliders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bands.forEachIndexed { index, level ->
                EqBandSlider(
                    frequency = frequencies[index],
                    level = level,
                    accentColor = accentColor,
                    isDark = isDark,
                    enabled = enabled,
                    onLevelChange = { newLevel ->
                        onBandChange(index, newLevel)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(GlassSpacing.S))

        // Range labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "-12 dB",
                style = GlassTypography.Caption,
                color = theme.textTertiary
            )
            Text(
                text = "0 dB",
                style = GlassTypography.Caption,
                color = theme.textSecondary
            )
            Text(
                text = "+12 dB",
                style = GlassTypography.Caption,
                color = theme.textTertiary
            )
        }
    }
}

@Composable
private fun EqBandSlider(
    frequency: Int,
    level: Float,
    accentColor: Color,
    isDark: Boolean,
    enabled: Boolean,
    onLevelChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark)
    val view = LocalView.current
    var currentLevel by remember { mutableFloatStateOf(level) }

    LaunchedEffect(level) {
        currentLevel = level
    }

    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Frequency label
        Text(
            text = if (frequency >= 1000) "${frequency / 1000}k" else "$frequency",
            style = GlassTypography.Caption,
            fontSize = 10.sp,
            color = theme.textTertiary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Vertical slider
        Box(
            modifier = Modifier
                .width(28.dp)
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(theme.glassSecondary)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val heightPx = size.height.toFloat()
                            val deltaLevel = -(dragAmount.y / heightPx) * 24f // -12 to +12 dB range
                            val newLevel = (currentLevel + deltaLevel).coerceIn(-12f, 12f)
                            currentLevel = newLevel
                            onLevelChange(newLevel)
                        }
                    )
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Fill indicator
            val fillHeight = ((currentLevel + 12f) / 24f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fillHeight)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (enabled) 0.8f else 0.3f),
                                accentColor.copy(alpha = if (enabled) 0.6f else 0.2f)
                            )
                        )
                    )
            )

            // Center line (0 dB)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(theme.textTertiary.copy(alpha = 0.3f))
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Level value
        Text(
            text = String.format("%+.0f", currentLevel),
            style = GlassTypography.Caption,
            fontSize = 10.sp,
            color = if (enabled) accentColor else theme.textTertiary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ============================================================================
// PREAMP CARD
// ============================================================================

@Composable
private fun PreampCard(
    preampDb: Float,
    accentColor: Color,
    isDark: Boolean,
    isReverieDark: Boolean,
    enabled: Boolean,
    onPreampChange: (Float) -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current
    var currentPreamp by remember { mutableFloatStateOf(preampDb) }

    LaunchedEffect(preampDb) {
        currentPreamp = preampDb
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(theme.glassSecondary)
            .padding(GlassSpacing.L)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Preamp",
                style = GlassTypography.Body,
                fontWeight = FontWeight.SemiBold,
                color = theme.textPrimary
            )
            Text(
                text = String.format("%+.1f dB", currentPreamp),
                style = GlassTypography.Body,
                fontWeight = FontWeight.Bold,
                color = if (enabled) accentColor else theme.textTertiary
            )
        }

        Spacer(modifier = Modifier.height(GlassSpacing.M))

        Slider(
            value = currentPreamp,
            onValueChange = {
                currentPreamp = it
                onPreampChange(it)
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            },
            valueRange = -12f..12f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = theme.glassSecondary
            )
        )
    }
}

// ============================================================================
// EFFECT CARD
// ============================================================================

@Composable
private fun EffectCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    strength: Float,
    accentColor: Color,
    isDark: Boolean,
    isReverieDark: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onStrengthChange: (Float) -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current
    var currentStrength by remember { mutableFloatStateOf(strength) }

    LaunchedEffect(strength) {
        currentStrength = strength
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(theme.glassSecondary)
            .padding(GlassSpacing.M),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (currentStrength > 0 && enabled) accentColor else theme.textTertiary,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.height(GlassSpacing.S))

        Text(
            text = title,
            style = GlassTypography.Caption,
            color = theme.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(GlassSpacing.S))

        Text(
            text = "${(currentStrength * 100).toInt()}%",
            style = GlassTypography.Body,
            fontWeight = FontWeight.Bold,
            color = if (enabled) accentColor else theme.textTertiary
        )

        Spacer(modifier = Modifier.height(GlassSpacing.S))

        Slider(
            value = currentStrength,
            onValueChange = {
                currentStrength = it
                onStrengthChange(it)
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            },
            valueRange = 0f..1f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = theme.glassSecondary
            )
        )
    }
}

// ============================================================================
// PRESET SELECTION DIALOG
// ============================================================================

@Composable
private fun PresetSelectionDialog(
    isDark: Boolean,
    isReverieDark: Boolean,
    accentColor: Color,
    currentPreset: String,
    onPresetSelected: (String, List<Float>, Float) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)

    // EQ Presets (presetName, bands, preampDb)
    val presets = listOf(
        Triple("Flat", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), 0f),
        Triple("Rock", listOf(4f, 3f, 0f, -2f, -3f, -1f, 1f, 3f, 4f, 5f), 0f),
        Triple("Pop", listOf(-1f, 0f, 2f, 3f, 4f, 4f, 3f, 1f, 0f, -1f), 0f),
        Triple("Jazz", listOf(3f, 2f, 0f, 1f, 3f, 3f, 2f, 1f, 2f, 3f), 0f),
        Triple("Classical", listOf(4f, 3f, 2f, 0f, -1f, -1f, 0f, 2f, 3f, 4f), 0f),
        Triple("Bass Boost", listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f), 0f),
        Triple("Treble Boost", listOf(0f, 0f, 0f, 0f, 0f, 2f, 4f, 5f, 6f, 6f), 0f),
        Triple("Vocal", listOf(-2f, -1f, 0f, 2f, 4f, 4f, 3f, 1f, 0f, -1f), 0f),
        Triple("Podcast", listOf(0f, 1f, 2f, 3f, 3f, 2f, 1f, 0f, 0f, 0f), 0f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.glassSecondary,
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = "Select Preset",
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Column {
                presets.forEach { (name, bands, preampDb) ->
                    val isSelected = currentPreset == name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GlassShapes.Small))
                            .then(
                                if (isSelected) {
                                    Modifier.background(theme.interactive)
                                } else Modifier
                            )
                            .clickable { onPresetSelected(name, bands, preampDb) }
                            .padding(vertical = GlassSpacing.S, horizontal = GlassSpacing.XS),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = GlassTypography.Body,
                            color = if (isSelected) accentColor else theme.textPrimary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
