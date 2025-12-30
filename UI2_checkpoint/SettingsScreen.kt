/*
 * LEGACY SETTINGS SCREEN - NON-GLASS VERSION
 *
 * This is the original settings screen without glass morphism effects.
 * Kept for reference and potential rollback if needed.
 *
 * DEPRECATED: Use SettingsScreenGlass instead for the modern glass UI.
 * This version will be removed in a future release.
 */
package com.example.rezon8.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rezon8.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Deprecated(
    message = "Use SettingsScreenGlass instead for modern glass morphism UI",
    replaceWith = ReplaceWith("SettingsScreenGlass")
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    accentColor: Color = Color(0xFF00E5FF),
    isDarkTheme: Boolean = true,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val wifiOnly by viewModel.wifiOnly.collectAsState()
    val skipBackward by viewModel.skipBackward.collectAsState()
    val skipForward by viewModel.skipForward.collectAsState()
    val keepServiceActive by viewModel.keepServiceActive.collectAsState()

    var showEqDialog by remember { mutableStateOf(false) }
    var showCodecDialog by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }
    var scanOnStartup by remember { mutableStateOf(true) }

    // Theme-aware colors
    val bgColor = if (isDarkTheme) Color(0xFF0A0A0A) else Color(0xFFF5F5F5)
    val cardColor = if (isDarkTheme) Color(0xFF1A1A1A) else Color(0xFFFFFFFF)
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val subtitleColor = if (isDarkTheme) Color.Gray else Color.DarkGray
    val dividerColor = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFE0E0E0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = textColor, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // DOWNLOADS Section
            SettingsSection("DOWNLOADS", accentColor)
            SettingsCard(cardColor) {
                SettingsSwitch(
                    title = "Download over Wi-Fi only",
                    checked = wifiOnly,
                    onToggle = { viewModel.setWifiOnly(it) },
                    accentColor = accentColor,
                    textColor = textColor,
                    subtitleColor = subtitleColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // LIBRARY Section
            SettingsSection("LIBRARY", accentColor)
            SettingsCard(cardColor) {
                SettingsSwitch(
                    title = "Scan folders on application startup",
                    checked = scanOnStartup,
                    onToggle = { scanOnStartup = it },
                    accentColor = accentColor,
                    textColor = textColor,
                    subtitleColor = subtitleColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AUDIO Section
            SettingsSection("AUDIO", accentColor)
            SettingsCard(cardColor) {
                SettingsItem(
                    title = "Equalizer",
                    subtitle = "Configure audio presets",
                    textColor = textColor,
                    subtitleColor = subtitleColor,
                    onClick = { showEqDialog = true }
                )
                HorizontalDivider(color = dividerColor)
                val currentCodec by viewModel.audioCodec.collectAsState()
                SettingsItem(
                    title = "Codecs (priority)",
                    subtitle = currentCodec,
                    textColor = textColor,
                    subtitleColor = subtitleColor,
                    onClick = { showCodecDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PLAYER Section
            SettingsSection("PLAYER", accentColor)
            SettingsCard(cardColor) {
                SettingsItem(
                    title = "Skip forward amount",
                    subtitle = "${skipForward} seconds",
                    textColor = textColor,
                    subtitleColor = subtitleColor,
                    onClick = { showSkipDialog = true }
                )
                HorizontalDivider(color = dividerColor)
                SettingsItem(
                    title = "Skip backward amount",
                    subtitle = "${skipBackward} seconds",
                    textColor = textColor,
                    subtitleColor = subtitleColor,
                    onClick = { showSkipDialog = true }
                )
                HorizontalDivider(color = dividerColor)
                SettingsSwitch(
                    title = "Persistent Player",
                    subtitle = "Keep player active in background",
                    checked = keepServiceActive,
                    onToggle = { viewModel.setKeepServiceActive(it) },
                    accentColor = accentColor,
                    textColor = textColor,
                    subtitleColor = subtitleColor
                )
            }

            // Extra bottom spacer so content isn't hidden behind navigation
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Advanced Equalizer Dialog
    if (showEqDialog) {
        AdvancedEqualizerDialog(
            accentColor = accentColor,
            viewModel = viewModel,
            onDismiss = { showEqDialog = false }
        )
    }

    // Skip Amount Dialog
    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Skip Amounts", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Set symmetrical skip durations", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    listOf(10, 15, 30, 45, 60).forEach { seconds ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSkipForward(seconds)
                                    viewModel.setSkipBackward(seconds)
                                    showSkipDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = skipForward == seconds,
                                onClick = {
                                    viewModel.setSkipForward(seconds)
                                    viewModel.setSkipBackward(seconds)
                                    showSkipDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$seconds seconds", color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text("Close", color = accentColor)
                }
            }
        )
    }

    // Codec Priority Dialog
    if (showCodecDialog) {
        val audioCodec by viewModel.audioCodec.collectAsState()

        AlertDialog(
            onDismissRequest = { showCodecDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Audio Decoder", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Select the preferred audio decoder. All options support common formats including MP3, AAC, FLAC, OGG, and M4A/M4B.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    listOf(
                        Triple("Default", "System default decoder (recommended)", "Automatic format detection"),
                        Triple("MediaCodec", "Hardware-accelerated decoder", "Best battery • Supports: MP3, AAC, FLAC, OGG, OPUS"),
                        Triple("FFmpeg", "Software decoder - audiophile quality", "Best compatibility • Hi-Res: FLAC 24-bit, ALAC, WAV, AIFF, DSD"),
                        Triple("OpenSL ES", "Low-latency audio pipeline", "Best for Bluetooth • Reduced latency")
                    ).forEach { (codec, description, formats) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (audioCodec == codec) 1.5.dp else 0.dp,
                                    color = if (audioCodec == codec) accentColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.setAudioCodec(codec)
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = audioCodec == codec,
                                onClick = { viewModel.setAudioCodec(codec) },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(codec, color = Color.White, fontWeight = FontWeight.Medium)
                                Text(description, color = Color.Gray, fontSize = 11.sp)
                                Text(formats, color = accentColor.copy(alpha = 0.7f), fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Audiophile note
                    Text(
                        "AUDIOPHILE FORMATS",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "For high-resolution audio (FLAC 24-bit/96kHz, DSD, ALAC), select FFmpeg for best quality. All decoders handle standard audiobook formats (MP3, M4B, AAC).",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCodecDialog = false }) {
                    Text("Done", color = accentColor)
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, accentColor: Color) {
    Text(
        text = title,
        color = accentColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsCard(cardColor: Color = Color(0xFF1A1A1A), content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
    ) {
        content()
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    accentColor: Color,
    textColor: Color = Color.White,
    subtitleColor: Color = Color.Gray
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, color = subtitleColor, fontSize = 13.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.4f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF333333)
            )
        )
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    textColor: Color = Color.White,
    subtitleColor: Color = Color.Gray,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = textColor, fontSize = 16.sp)
        Text(subtitle, color = subtitleColor, fontSize = 14.sp)
    }
}

// ============================================================================
// PREMIUM AUDIOPHILE EQUALIZER SYSTEM
// ============================================================================

// Preset Categories for organization (Audiobook first for audiobook app)
private enum class PresetCategory(val displayName: String) {
    AUDIOBOOK("Audiobook"),
    GENRE("Genre"),
    USE_CASE("Use Case"),
    CUSTOM("Custom")
}

// Premium 10-Band EQ Presets with professional-grade tuning
private enum class EQPreset(
    val displayName: String,
    val category: PresetCategory,
    val bands: List<Float>, // 10 bands: 31Hz, 63Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
    val preamp: Float = 0f,
    val description: String = ""
) {
    // Flat Reference
    FLAT("Flat", PresetCategory.USE_CASE, listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), 0f, "Reference flat response"),

    // Genre Presets - Professionally tuned
    ROCK("Rock", PresetCategory.GENRE, listOf(4f, 3f, 2f, 0f, -1f, 0f, 2f, 3f, 4f, 3f), -1f, "Punchy mids, crisp highs"),
    POP("Pop", PresetCategory.GENRE, listOf(1f, 2f, 3f, 2f, 0f, 1f, 2f, 3f, 2f, 1f), 0f, "Balanced, radio-ready sound"),
    JAZZ("Jazz", PresetCategory.GENRE, listOf(3f, 2f, 0f, 1f, 0f, 0f, 1f, 2f, 3f, 4f), 0f, "Warm bass, smooth highs"),
    CLASSICAL("Classical", PresetCategory.GENRE, listOf(2f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 2f, 3f), -2f, "Natural, dynamic range"),
    ELECTRONIC("Electronic", PresetCategory.GENRE, listOf(5f, 4f, 2f, 0f, 1f, 2f, 1f, 3f, 4f, 4f), -2f, "Deep bass, crisp synths"),
    HIP_HOP("Hip-Hop", PresetCategory.GENRE, listOf(5f, 5f, 3f, 1f, 0f, -1f, 1f, 1f, 2f, 2f), -2f, "Heavy sub-bass, clear vocals"),
    RNB("R&B", PresetCategory.GENRE, listOf(4f, 4f, 2f, 1f, 0f, 1f, 2f, 2f, 2f, 1f), -1f, "Smooth, warm vocals"),
    METAL("Metal", PresetCategory.GENRE, listOf(4f, 3f, 0f, 1f, 3f, 0f, 1f, 4f, 5f, 4f), -2f, "Aggressive, powerful"),
    ACOUSTIC("Acoustic", PresetCategory.GENRE, listOf(2f, 2f, 0f, 1f, 1f, 2f, 2f, 3f, 2f, 1f), 0f, "Natural instrument clarity"),
    COUNTRY("Country", PresetCategory.GENRE, listOf(2f, 2f, 1f, 1f, 0f, 1f, 2f, 3f, 2f, 1f), 0f, "Twangy, vocal forward"),

    // Use Case Presets
    BASS_BOOST("Bass Boost", PresetCategory.USE_CASE, listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f), -3f, "Deep, powerful bass"),
    TREBLE_BOOST("Treble Boost", PresetCategory.USE_CASE, listOf(0f, 0f, 0f, 0f, 0f, 1f, 2f, 4f, 5f, 6f), -2f, "Bright, airy highs"),
    BASS_REDUCER("Bass Reducer", PresetCategory.USE_CASE, listOf(-4f, -3f, -2f, -1f, 0f, 0f, 0f, 0f, 0f, 0f), 2f, "Reduce boominess"),
    LOUDNESS("Loudness", PresetCategory.USE_CASE, listOf(4f, 3f, 0f, 0f, -1f, 0f, 0f, 0f, 2f, 3f), -2f, "Enhanced at low volumes"),
    LATE_NIGHT("Late Night", PresetCategory.USE_CASE, listOf(-2f, -1f, 0f, 0f, 0f, 0f, 0f, 0f, -1f, -2f), 0f, "Reduced dynamics"),
    SMALL_SPEAKERS("Small Speakers", PresetCategory.USE_CASE, listOf(5f, 4f, 3f, 1f, 0f, 0f, 0f, 1f, 2f, 2f), -2f, "Compensate small drivers"),
    HEADPHONES("Headphones", PresetCategory.USE_CASE, listOf(2f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 2f, 2f), 0f, "Optimized for headphones"),
    CAR_AUDIO("Car Audio", PresetCategory.USE_CASE, listOf(3f, 2f, 0f, 0f, 1f, 2f, 2f, 3f, 3f, 2f), -1f, "Cuts through road noise"),

    // Audiobook & Podcast Presets
    SPEECH_CLARITY("Speech Clarity", PresetCategory.AUDIOBOOK, listOf(-2f, -1f, 0f, 2f, 3f, 4f, 3f, 2f, 1f, 0f), 0f, "Enhanced voice intelligibility"),
    NARRATOR("Narrator", PresetCategory.AUDIOBOOK, listOf(-3f, -2f, 0f, 1f, 2f, 3f, 2f, 1f, 0f, -1f), 1f, "Warm, intimate narration"),
    PODCAST("Podcast", PresetCategory.AUDIOBOOK, listOf(-2f, -1f, 0f, 2f, 3f, 3f, 2f, 1f, 0f, -1f), 0f, "Clear dialogue, reduced background"),
    AUDIOBOOK_NIGHT("Audiobook Night", PresetCategory.AUDIOBOOK, listOf(-3f, -2f, 0f, 1f, 2f, 2f, 1f, 0f, -1f, -2f), 0f, "Gentle listening before sleep"),

    // Custom placeholder
    CUSTOM("Custom", PresetCategory.CUSTOM, listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), 0f, "Your custom settings")
}

// Reverb presets for spatial effects
private enum class ReverbPreset(val displayName: String, val roomSize: Float, val damping: Float, val wetLevel: Float) {
    OFF("Off", 0f, 0f, 0f),
    SMALL_ROOM("Small Room", 0.2f, 0.5f, 0.15f),
    MEDIUM_ROOM("Medium Room", 0.4f, 0.4f, 0.2f),
    LARGE_HALL("Large Hall", 0.7f, 0.3f, 0.25f),
    CONCERT_HALL("Concert Hall", 0.85f, 0.2f, 0.3f),
    CATHEDRAL("Cathedral", 0.95f, 0.1f, 0.35f),
    STUDIO("Studio", 0.3f, 0.6f, 0.1f)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AdvancedEqualizerDialog(
    accentColor: Color,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    // 10-Band professional frequencies
    val frequencies = listOf("31", "63", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")

    // Get real state from AudioEffectManager
    val audioEffectManager = viewModel.audioEffectManager
    val bands by audioEffectManager.bands.collectAsState()
    val preamp by audioEffectManager.preamp.collectAsState()
    val bassBoostIntensity by audioEffectManager.bassBoostStrength.collectAsState()
    val virtualizerStrength by audioEffectManager.virtualizerStrength.collectAsState()
    val reverbPresetIndex by audioEffectManager.reverbPreset.collectAsState()
    val loudnessEnabled by audioEffectManager.loudnessEnabled.collectAsState()
    val amplifierGain by audioEffectManager.amplifierGain.collectAsState()
    val eqEnabled by audioEffectManager.eqEnabled.collectAsState()
    val selectedPresetName by audioEffectManager.selectedPresetName.collectAsState()

    // Map reverb index to ReverbPreset enum
    val selectedReverb = ReverbPreset.entries.getOrElse(reverbPresetIndex) { ReverbPreset.OFF }

    // Find matching EQPreset by name
    var selectedPreset by remember { mutableStateOf(EQPreset.entries.find { it.displayName == selectedPresetName } ?: EQPreset.FLAT) }

    // UI states
    var selectedCategory by remember { mutableStateOf<PresetCategory?>(null) }
    var stereoBalance by remember { mutableFloatStateOf(0f) }

    // Swipe to dismiss state
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 150f

    // Animation specs - Android 16 style (smooth spring animations)
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Animate the drag offset back to 0 when released
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = springSpec,
        label = "dragOffset"
    )

    // Entry animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

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

    // Pager state for swipe navigation
    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    val coroutineScope = rememberCoroutineScope()

    // Update selectedPreset when name changes
    LaunchedEffect(selectedPresetName) {
        selectedPreset = EQPreset.entries.find { it.displayName == selectedPresetName } ?: EQPreset.CUSTOM
    }

    // Apply preset to AudioEffectManager
    fun applyPreset(preset: EQPreset) {
        selectedPreset = preset
        if (preset != EQPreset.CUSTOM) {
            audioEffectManager.applyPreset(preset.displayName, preset.bands, preset.preamp)
        }
    }

    // Check if values differ from preset (auto-switch to Custom)
    fun checkIfCustom() {
        if (selectedPreset != EQPreset.CUSTOM) {
            if (bands != selectedPreset.bands || preamp != selectedPreset.preamp) {
                selectedPreset = EQPreset.CUSTOM
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.95f)
                .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
                .scale(dialogScale)
                .alpha(dialogAlpha)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0D0D0D))
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
                            // Only allow dragging down
                            dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                        }
                    )
                }
        ) {
            // Drag handle indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Gray.copy(alpha = 0.5f))
                )
            }

            // Premium Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.12f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Audio Engine",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Professional 10-Band Equalizer",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Animated ON/OFF switch
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedContent(
                            targetState = eqEnabled,
                            transitionSpec = {
                                fadeIn(tween(200)) + scaleIn(initialScale = 0.8f) togetherWith
                                    fadeOut(tween(150)) + scaleOut(targetScale = 0.8f)
                            },
                            label = "eqStatus"
                        ) { enabled ->
                            Text(
                                if (enabled) "ON" else "OFF",
                                color = if (enabled) accentColor else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = eqEnabled,
                            onCheckedChange = { audioEffectManager.setEqEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accentColor,
                                checkedTrackColor = accentColor.copy(alpha = 0.4f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF333333)
                            )
                        )
                    }
                }
            }

            // Tab Row with animated indicator
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color(0xFF141414),
                contentColor = accentColor,
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        val currentTabPosition = tabPositions[pagerState.currentPage]
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.BottomStart)
                                .offset(x = currentTabPosition.left)
                                .width(currentTabPosition.width)
                                .height(3.dp)
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(accentColor)
                        )
                    }
                },
                divider = {}
            ) {
                listOf("PRESETS", "EFFECTS", "EQUALIZER").forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    index,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                )
                            }
                        },
                        text = {
                            Text(
                                title,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        },
                        selectedContentColor = accentColor,
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            // Swipeable content area using HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                // Animated content transitions
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val scale = 1f - (kotlin.math.abs(pageOffset) * 0.05f).coerceIn(0f, 0.1f)
                val alpha = 1f - (kotlin.math.abs(pageOffset) * 0.3f).coerceIn(0f, 0.5f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                ) {
                    when (page) {
                        0 -> Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            PresetsTab(
                                selectedPreset = selectedPreset,
                                selectedCategory = selectedCategory,
                                onCategorySelect = { selectedCategory = if (selectedCategory == it) null else it },
                                onPresetSelect = { applyPreset(it) },
                                accentColor = accentColor
                            )
                        }
                        1 -> Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            EffectsTab(
                                amplifierGain = amplifierGain,
                                onAmplifierGainChange = { audioEffectManager.setAmplifierGain(it) },
                                bassBoost = bassBoostIntensity,
                                onBassBoostChange = { audioEffectManager.setBassBoost(it) },
                                virtualizer = virtualizerStrength,
                                onVirtualizerChange = { audioEffectManager.setVirtualizer(it) },
                                stereoBalance = stereoBalance,
                                onStereoBalanceChange = { stereoBalance = it },
                                selectedReverb = selectedReverb,
                                onReverbChange = { audioEffectManager.setReverbPreset(it.ordinal) },
                                loudnessEnabled = loudnessEnabled,
                                onLoudnessChange = { audioEffectManager.setLoudnessEnabled(it) },
                                accentColor = accentColor,
                                enabled = eqEnabled
                            )
                        }
                        2 -> Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            EQTab(
                                bands = bands,
                                onBandChange = { index, value ->
                                    audioEffectManager.setBandLevel(index, value)
                                    checkIfCustom()
                                },
                                preamp = preamp,
                                onPreampChange = {
                                    audioEffectManager.setPreamp(it)
                                    checkIfCustom()
                                },
                                frequencies = frequencies,
                                accentColor = accentColor,
                                enabled = eqEnabled,
                                selectedPreset = selectedPreset
                            )
                        }
                    }
                }
            }

            // Redesigned bottom bar - sleek, minimal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xFF0A0A0A))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Active preset with animated text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "ACTIVE PRESET",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            AnimatedContent(
                                targetState = selectedPreset.displayName,
                                transitionSpec = {
                                    (slideInVertically { -it } + fadeIn()) togetherWith
                                        (slideOutVertically { it } + fadeOut())
                                },
                                label = "presetName"
                            ) { name ->
                                Text(
                                    name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Reset button - prominent on the right
                    Surface(
                        onClick = {
                            audioEffectManager.reset()
                            selectedPreset = EQPreset.FLAT
                        },
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Reset",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// EQUALIZER TAB
// ============================================================================

@Composable
private fun EQTab(
    bands: List<Float>,
    onBandChange: (Int, Float) -> Unit,
    preamp: Float,
    onPreampChange: (Float) -> Unit,
    frequencies: List<String>,
    accentColor: Color,
    enabled: Boolean,
    selectedPreset: EQPreset
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact frequency response + preamp row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Frequency response curve - compact
            FrequencyResponseCurve(
                bands = bands,
                accentColor = accentColor,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A))
            )

            // Preamp - compact vertical
            Column(
                modifier = Modifier
                    .width(70.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("PREAMP", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${if (preamp >= 0) "+" else ""}${preamp.toInt()}",
                    color = if (enabled) accentColor else Color.Gray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("dB", color = Color.Gray, fontSize = 9.sp)
            }
        }

        // Preamp slider - horizontal, compact
        Slider(
            value = preamp,
            onValueChange = onPreampChange,
            valueRange = -12f..12f,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = if (enabled) accentColor else Color.Gray,
                activeTrackColor = if (enabled) accentColor else Color.Gray,
                inactiveTrackColor = Color(0xFF333333)
            )
        )

        // dB Scale labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("+12", color = Color.Gray, fontSize = 9.sp)
            Text("0 dB", color = accentColor.copy(alpha = 0.7f), fontSize = 9.sp)
            Text("-12", color = Color.Gray, fontSize = 9.sp)
        }

        // 10-Band Equalizer - fills remaining space
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A))
                .padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bands.forEachIndexed { index, value ->
                PremiumEQBandSlider(
                    value = value,
                    onValueChange = { onBandChange(index, it) },
                    label = frequencies[index],
                    accentColor = accentColor,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick adjustment section - compact
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Bass column
            QuickAdjustColumn(
                label = "BASS",
                accentColor = accentColor,
                enabled = enabled,
                onPlus = {
                    val newBands = bands.toMutableList()
                    for (i in 0..2) newBands[i] = (newBands[i] + 1f).coerceIn(-12f, 12f)
                    newBands.forEachIndexed { i, v -> onBandChange(i, v) }
                },
                onMinus = {
                    val newBands = bands.toMutableList()
                    for (i in 0..2) newBands[i] = (newBands[i] - 1f).coerceIn(-12f, 12f)
                    newBands.forEachIndexed { i, v -> onBandChange(i, v) }
                },
                modifier = Modifier.weight(1f)
            )

            // Mid column
            QuickAdjustColumn(
                label = "MID",
                accentColor = accentColor,
                enabled = enabled,
                onPlus = {
                    val newBands = bands.toMutableList()
                    for (i in 3..6) newBands[i] = (newBands[i] + 1f).coerceIn(-12f, 12f)
                    newBands.forEachIndexed { i, v -> onBandChange(i, v) }
                },
                onMinus = {
                    val newBands = bands.toMutableList()
                    for (i in 3..6) newBands[i] = (newBands[i] - 1f).coerceIn(-12f, 12f)
                    newBands.forEachIndexed { i, v -> onBandChange(i, v) }
                },
                modifier = Modifier.weight(1f)
            )

            // Treble column
            QuickAdjustColumn(
                label = "TREBLE",
                accentColor = accentColor,
                enabled = enabled,
                onPlus = {
                    val newBands = bands.toMutableList()
                    for (i in 7..9) newBands[i] = (newBands[i] + 1f).coerceIn(-12f, 12f)
                    newBands.forEachIndexed { i, v -> onBandChange(i, v) }
                },
                onMinus = {
                    val newBands = bands.toMutableList()
                    for (i in 7..9) newBands[i] = (newBands[i] - 1f).coerceIn(-12f, 12f)
                    newBands.forEachIndexed { i, v -> onBandChange(i, v) }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Compact Quick Adjust column
@Composable
private fun QuickAdjustColumn(
    label: String,
    accentColor: Color,
    enabled: Boolean,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            color = Color.Gray,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Plus button
            Surface(
                onClick = onPlus,
                enabled = enabled,
                color = Color(0xFF2A2A2A),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "+",
                        color = if (enabled) accentColor else Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Minus button
            Surface(
                onClick = onMinus,
                enabled = enabled,
                color = Color(0xFF2A2A2A),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "−",
                        color = if (enabled) Color(0xFFFF6B6B) else Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyResponseCurve(
    bands: List<Float>,
    accentColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(16.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        // Draw grid lines
        val gridColor = Color.Gray.copy(alpha = 0.2f)
        // Horizontal center line (0dB)
        drawLine(
            color = accentColor.copy(alpha = 0.3f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f
        )
        // Top and bottom lines
        drawLine(gridColor, Offset(0f, 0f), Offset(width, 0f), 1f)
        drawLine(gridColor, Offset(0f, height), Offset(width, height), 1f)

        // Draw frequency response curve
        if (bands.isNotEmpty()) {
            val path = Path()
            val pointSpacing = width / (bands.size - 1)

            bands.forEachIndexed { index, value ->
                val x = index * pointSpacing
                val y = centerY - (value / 12f) * (height / 2) * 0.8f

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    // Smooth curve using quadratic bezier
                    val prevX = (index - 1) * pointSpacing
                    val prevY = centerY - (bands[index - 1] / 12f) * (height / 2) * 0.8f
                    val controlX = (prevX + x) / 2
                    path.quadraticBezierTo(controlX, prevY, (prevX + x) / 2, (prevY + y) / 2)
                    path.quadraticBezierTo((prevX + x) / 2, (prevY + y) / 2, x, y)
                }
            }

            // Draw curve glow
            drawPath(
                path = path,
                color = if (enabled) accentColor.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.2f),
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )

            // Draw main curve
            drawPath(
                path = path,
                color = if (enabled) accentColor else Color.Gray,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // Draw points
            bands.forEachIndexed { index, value ->
                val x = index * pointSpacing
                val y = centerY - (value / 12f) * (height / 2) * 0.8f
                drawCircle(
                    color = if (enabled) accentColor else Color.Gray,
                    radius = 4f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun PremiumEQBandSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    accentColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // dB value with color indicator
        Text(
            text = "${if (value >= 0) "+" else ""}${value.toInt()}",
            color = when {
                !enabled -> Color.Gray
                value > 0 -> accentColor
                value < 0 -> Color(0xFFFF6B6B)
                else -> Color.White
            },
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        // Custom vertical slider with proper touch handling
        Box(
            modifier = Modifier
                .weight(1f)
                .width(32.dp)
                .padding(vertical = 8.dp)
        ) {
            var sliderHeight by remember { mutableFloatStateOf(0f) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput

                        sliderHeight = size.height.toFloat()

                        detectTapGestures { offset ->
                            // Convert tap position to value (-12 to +12)
                            val normalizedY = 1f - (offset.y / sliderHeight).coerceIn(0f, 1f)
                            val newValue = -12f + (normalizedY * 24f)
                            onValueChange(newValue.coerceIn(-12f, 12f))
                        }
                    }
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput

                        sliderHeight = size.height.toFloat()

                        detectDragGestures { change, _ ->
                            change.consume()
                            // Convert drag position to value (-12 to +12)
                            val normalizedY = 1f - (change.position.y / sliderHeight).coerceIn(0f, 1f)
                            val newValue = -12f + (normalizedY * 24f)
                            onValueChange(newValue.coerceIn(-12f, 12f))
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val centerX = width / 2
                val trackWidth = 4.dp.toPx()
                val thumbRadius = 10.dp.toPx()

                // Calculate thumb position (inverted: top = +12, bottom = -12)
                val normalizedValue = (value + 12f) / 24f
                val thumbY = height - (normalizedValue * height)

                // Draw track background
                drawLine(
                    color = Color(0xFF333333),
                    start = Offset(centerX, thumbRadius),
                    end = Offset(centerX, height - thumbRadius),
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round
                )

                // Draw center line (0 dB reference)
                val centerY = height / 2
                drawLine(
                    color = if (enabled) accentColor.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f),
                    start = Offset(centerX - 8.dp.toPx(), centerY),
                    end = Offset(centerX + 8.dp.toPx(), centerY),
                    strokeWidth = 2.dp.toPx()
                )

                // Draw active track from center
                val activeColor = if (enabled) {
                    if (value >= 0) accentColor else Color(0xFFFF6B6B)
                } else Color.Gray

                if (value != 0f) {
                    drawLine(
                        color = activeColor,
                        start = Offset(centerX, centerY),
                        end = Offset(centerX, thumbY.coerceIn(thumbRadius, height - thumbRadius)),
                        strokeWidth = trackWidth,
                        cap = StrokeCap.Round
                    )
                }

                // Draw thumb glow
                if (enabled) {
                    drawCircle(
                        color = activeColor.copy(alpha = 0.3f),
                        radius = thumbRadius + 4.dp.toPx(),
                        center = Offset(centerX, thumbY.coerceIn(thumbRadius, height - thumbRadius))
                    )
                }

                // Draw thumb
                drawCircle(
                    color = if (enabled) activeColor else Color.Gray,
                    radius = thumbRadius,
                    center = Offset(centerX, thumbY.coerceIn(thumbRadius, height - thumbRadius))
                )

                // Draw inner thumb circle
                drawCircle(
                    color = Color(0xFF1A1A1A),
                    radius = thumbRadius - 3.dp.toPx(),
                    center = Offset(centerX, thumbY.coerceIn(thumbRadius, height - thumbRadius))
                )
            }
        }

        // Frequency label
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================================================
// EFFECTS TAB
// ============================================================================

@Composable
private fun EffectsTab(
    amplifierGain: Float,
    onAmplifierGainChange: (Float) -> Unit,
    bassBoost: Float,
    onBassBoostChange: (Float) -> Unit,
    virtualizer: Float,
    onVirtualizerChange: (Float) -> Unit,
    stereoBalance: Float,
    onStereoBalanceChange: (Float) -> Unit,
    selectedReverb: ReverbPreset,
    onReverbChange: (ReverbPreset) -> Unit,
    loudnessEnabled: Boolean,
    onLoudnessChange: (Boolean) -> Unit,
    accentColor: Color,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Amplifier - NEW
        EffectCard(
            title = "AMPLIFIER",
            subtitle = "Volume gain adjustment",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            accentColor = accentColor
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-12 dB", color = Color.Gray, fontSize = 10.sp)
                    Text(
                        "${if (amplifierGain >= 0) "+" else ""}${amplifierGain.toInt()} dB",
                        color = when {
                            amplifierGain > 0 -> accentColor
                            amplifierGain < 0 -> Color(0xFFFF6B6B)
                            else -> Color.White
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("+12 dB", color = Color.Gray, fontSize = 10.sp)
                }
                Slider(
                    value = amplifierGain,
                    onValueChange = onAmplifierGainChange,
                    valueRange = -12f..12f,
                    enabled = enabled,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color(0xFF333333)
                    )
                )
                Text(
                    "Boost or reduce overall volume. Use cautiously at high levels to prevent clipping.",
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }

        // Bass Boost
        EffectCard(
            title = "BASS BOOST",
            subtitle = "Enhance low frequencies",
            icon = Icons.Default.GraphicEq,
            accentColor = accentColor
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Off", color = Color.Gray, fontSize = 10.sp)
                    Text("${(bassBoost * 100).toInt()}%", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Max", color = Color.Gray, fontSize = 10.sp)
                }
                Slider(
                    value = bassBoost,
                    onValueChange = onBassBoostChange,
                    enabled = enabled,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color(0xFF333333)
                    )
                )
            }
        }

        // Virtualizer / Stereo Widening
        EffectCard(
            title = "VIRTUALIZER",
            subtitle = "3D spatial audio effect",
            icon = Icons.Default.SurroundSound,
            accentColor = accentColor
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Narrow", color = Color.Gray, fontSize = 10.sp)
                    Text("${(virtualizer * 100).toInt()}%", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Wide", color = Color.Gray, fontSize = 10.sp)
                }
                Slider(
                    value = virtualizer,
                    onValueChange = onVirtualizerChange,
                    enabled = enabled,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color(0xFF333333)
                    )
                )
            }
        }

        // Stereo Balance
        EffectCard(
            title = "STEREO BALANCE",
            subtitle = "Left/Right channel balance",
            icon = Icons.Default.Tune,
            accentColor = accentColor
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("L", color = if (stereoBalance < 0) accentColor else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            stereoBalance < -0.05f -> "L ${(-stereoBalance * 100).toInt()}%"
                            stereoBalance > 0.05f -> "R ${(stereoBalance * 100).toInt()}%"
                            else -> "Center"
                        },
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("R", color = if (stereoBalance > 0) accentColor else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = stereoBalance,
                    onValueChange = onStereoBalanceChange,
                    valueRange = -1f..1f,
                    enabled = enabled,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color(0xFF333333)
                    )
                )
            }
        }

        // Reverb
        EffectCard(
            title = "REVERB",
            subtitle = "Room simulation effect",
            icon = Icons.Default.Waves,
            accentColor = accentColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReverbPreset.entries.forEach { reverb ->
                    FilterChip(
                        selected = selectedReverb == reverb,
                        onClick = { onReverbChange(reverb) },
                        enabled = enabled,
                        label = { Text(reverb.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.Transparent,
                            selectedLabelColor = accentColor
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = enabled,
                            selected = selectedReverb == reverb,
                            selectedBorderColor = accentColor,
                            selectedBorderWidth = 1.5.dp
                        )
                    )
                }
            }
        }

        // Loudness Normalization
        EffectCard(
            title = "LOUDNESS NORMALIZATION",
            subtitle = "Maintain consistent volume across tracks",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            accentColor = accentColor
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (loudnessEnabled) "Enabled - Target: -14 LUFS" else "Disabled",
                    color = if (loudnessEnabled) accentColor else Color.Gray,
                    fontSize = 12.sp
                )
                Switch(
                    checked = loudnessEnabled,
                    onCheckedChange = onLoudnessChange,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}

@Composable
private fun EffectCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.Gray, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

// ============================================================================
// PRESETS TAB
// ============================================================================

@Composable
private fun PresetsTab(
    selectedPreset: EQPreset,
    selectedCategory: PresetCategory?,
    onCategorySelect: (PresetCategory) -> Unit,
    onPresetSelect: (EQPreset) -> Unit,
    accentColor: Color
) {
    Column {
        // Category chips
        Text(
            "CATEGORIES",
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetCategory.entries.filter { it != PresetCategory.CUSTOM }.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelect(category) },
                    label = { Text(category.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.Transparent,
                        selectedLabelColor = accentColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedCategory == category,
                        selectedBorderColor = accentColor,
                        selectedBorderWidth = 1.5.dp
                    )
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Filtered presets
        val filteredPresets = if (selectedCategory != null) {
            EQPreset.entries.filter { it.category == selectedCategory && it != EQPreset.CUSTOM }
        } else {
            EQPreset.entries.filter { it != EQPreset.CUSTOM }
        }

        filteredPresets.forEach { preset ->
            PresetItem(
                preset = preset,
                isSelected = selectedPreset == preset,
                accentColor = accentColor,
                onClick = { onPresetSelect(preset) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PresetItem(
    preset: EQPreset,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when (preset.category) {
                        PresetCategory.GENRE -> Color(0xFF9C27B0).copy(alpha = 0.2f)
                        PresetCategory.USE_CASE -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        PresetCategory.AUDIOBOOK -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        PresetCategory.CUSTOM -> accentColor.copy(alpha = 0.2f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when (preset.category) {
                    PresetCategory.GENRE -> Icons.Default.MusicNote
                    PresetCategory.USE_CASE -> Icons.Default.Tune
                    PresetCategory.AUDIOBOOK -> Icons.Default.Headphones
                    PresetCategory.CUSTOM -> Icons.Default.Edit
                },
                contentDescription = null,
                tint = when (preset.category) {
                    PresetCategory.GENRE -> Color(0xFF9C27B0)
                    PresetCategory.USE_CASE -> Color(0xFF2196F3)
                    PresetCategory.AUDIOBOOK -> Color(0xFF4CAF50)
                    PresetCategory.CUSTOM -> accentColor
                },
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                preset.displayName,
                color = if (isSelected) accentColor else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                preset.description,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================================================
// LEGACY BAND SLIDER (kept for compatibility)
// ============================================================================

@Composable
private fun EQBandSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    accentColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    PremiumEQBandSlider(
        value = value,
        onValueChange = onValueChange,
        label = label,
        accentColor = accentColor,
        enabled = enabled,
        modifier = modifier
    )
}
