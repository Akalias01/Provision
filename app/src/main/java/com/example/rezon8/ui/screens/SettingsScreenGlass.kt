package com.mossglen.reverie.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mossglen.reverie.R
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.mossglen.reverie.ui.components.*
import com.mossglen.reverie.ui.theme.*
import com.mossglen.reverie.ui.viewmodel.ReverieAccentVariant
import com.mossglen.reverie.ui.viewmodel.SettingsViewModel
import com.mossglen.reverie.ui.viewmodel.ThemeMode
import com.mossglen.reverie.ui.viewmodel.ThemeViewModel

/**
 * REVERIE Glass - Settings Screen
 *
 * Clean, grouped settings with glass cards.
 * iOS Settings-inspired layout.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenGlass(
    isDark: Boolean = true,
    themeViewModel: ThemeViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    scrollToTopTrigger: Int = 0,
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToCloudFiles: () -> Unit = {},
    onNavigateToStats: () -> Unit = {}
) {
    val view = LocalView.current
    val scrollState = rememberScrollState()

    // Scroll to top when trigger changes
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            scrollState.animateScrollTo(0)
        }
    }

    // Theme state from ViewModel
    val themeMode by themeViewModel.themeMode
    val isReverieDark = themeMode == ThemeMode.REVERIE_DARK
    val reverieAccentVariant by themeViewModel.reverieAccentVariant
    val theme = glassTheme(isDark, isReverieDark)

    // Accent color - uses selected variant for Reverie Dark, blue for standard
    val accentColor = if (isReverieDark) themeViewModel.getReverieAccentColor() else GlassColors.Interactive

    // Settings state from ViewModel
    val wifiOnly by settingsViewModel.wifiOnly.collectAsState()
    val skipForward by settingsViewModel.skipForward.collectAsState()
    val skipBackward by settingsViewModel.skipBackward.collectAsState()
    val keepServiceActive by settingsViewModel.keepServiceActive.collectAsState()
    val audioCodec by settingsViewModel.audioCodec.collectAsState()
    val dynamicPlayerColors by settingsViewModel.dynamicPlayerColors.collectAsState()

    // Kids Mode state
    val kidsModeEnabled by settingsViewModel.kidsModeEnabled.collectAsState()
    val kidsModePin by settingsViewModel.kidsModePin.collectAsState()

    // Language state
    val appLanguage by settingsViewModel.appLanguage.collectAsState()

    // Torrent save path state
    val torrentSavePath by settingsViewModel.torrentSavePath.collectAsState()
    val context = LocalContext.current

    // Extract display name from URI
    val storageDisplayName = remember(torrentSavePath) {
        if (torrentSavePath.isEmpty()) {
            null // Will use string resource for "Internal Storage"
        } else {
            try {
                val uri = Uri.parse(torrentSavePath)
                // Extract folder name from the URI path
                val lastSegment = uri.lastPathSegment ?: ""
                // SAF URIs often look like "primary:Download/MyFolder" - extract the folder name
                val folderName = lastSegment
                    .substringAfterLast(':')
                    .substringAfterLast('/')
                    .ifEmpty { lastSegment.substringAfterLast(':') }
                if (folderName.isNotEmpty()) folderName else null
            } catch (e: Exception) {
                null
            }
        }
    }

    // Folder picker launcher for storage location
    val storageFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                // Take persistable read/write permission for the selected folder
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                               Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)

                // Save the URI to settings
                settingsViewModel.setTorrentSavePath(uri.toString())

                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.toast_storage_updated),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: SecurityException) {
                android.util.Log.e("SettingsScreenGlass", "Failed to take permission for folder", e)
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.toast_storage_permission_error),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.util.Log.e("SettingsScreenGlass", "Failed to set storage location", e)
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.toast_storage_error),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Permission launcher for storage folder picker (Android 10-12)
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            storageFolderLauncher.launch(null)
        } else {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.toast_storage_permission_denied),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Dialog state
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSkipDurationDialog by remember { mutableStateOf(false) }
    var showCodecDialog by remember { mutableStateOf(false) }
    var showKidsModeDialog by remember { mutableStateOf(false) }
    var showFoldersDialog by remember { mutableStateOf(false) }
    var scanOnStartup by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = GlassSpacing.M,
                    end = GlassSpacing.M,
                    top = GlassSpacing.XXL,
                    bottom = GlassSpacing.M
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = GlassTypography.Display,
                color = theme.textPrimary
            )
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = GlassSpacing.M)
        ) {
            // APPEARANCE Section
            GlassSectionHeader(title = stringResource(R.string.settings_appearance), isDark = isDark)

            SettingsCard(isDark = isDark) {
                SettingsRow(
                    title = stringResource(R.string.settings_theme),
                    value = themeMode.displayName,
                    isDark = isDark,
                    onClick = { showThemeModeDialog = true }
                )
                GlassDivider(isDark = isDark, startIndent = GlassSpacing.M)
                SettingsRow(
                    title = stringResource(R.string.settings_language),
                    value = appLanguage,
                    isDark = isDark,
                    onClick = { showLanguageDialog = true }
                )
                GlassDivider(isDark = isDark, startIndent = GlassSpacing.M)
                SettingsToggle(
                    title = stringResource(R.string.settings_dynamic_colors),
                    subtitle = stringResource(R.string.detail_change_cover),
                    isEnabled = dynamicPlayerColors,
                    isDark = isDark,
                    accentColor = accentColor,
                    isReverieDark = isReverieDark,
                    onToggle = { settingsViewModel.setDynamicPlayerColors(it) }
                )
            }

            // PLAYBACK Section
            GlassSectionHeader(title = stringResource(R.string.settings_player), isDark = isDark)

            SettingsCard(isDark = isDark) {
                SettingsRow(
                    title = stringResource(R.string.settings_skip_forward),
                    value = stringResource(R.string.settings_seconds, skipForward),
                    isDark = isDark,
                    onClick = { showSkipDurationDialog = true }
                )
                GlassDivider(isDark = isDark, startIndent = GlassSpacing.M)
                SettingsRow(
                    title = stringResource(R.string.settings_skip_backward),
                    value = stringResource(R.string.settings_seconds, skipBackward),
                    isDark = isDark,
                    onClick = { showSkipDurationDialog = true }
                )
                GlassDivider(isDark = isDark, startIndent = GlassSpacing.M)
                SettingsToggle(
                    title = stringResource(R.string.settings_persistent),
                    subtitle = stringResource(R.string.settings_persistent_desc),
                    isEnabled = keepServiceActive,
                    isDark = isDark,
                    accentColor = accentColor,
                    isReverieDark = isReverieDark,
                    onToggle = { settingsViewModel.setKeepServiceActive(it) }
                )
            }

            // DOWNLOADS Section
            GlassSectionHeader(title = stringResource(R.string.settings_downloads), isDark = isDark)

            SettingsCard(isDark = isDark) {
                SettingsToggle(
                    title = stringResource(R.string.settings_wifi_only),
                    subtitle = stringResource(R.string.settings_wifi_only),
                    isEnabled = wifiOnly,
                    isDark = isDark,
                    accentColor = accentColor,
                    isReverieDark = isReverieDark,
                    onToggle = { settingsViewModel.setWifiOnly(it) }
                )
                GlassDivider(isDark = isDark, startIndent = GlassSpacing.M)
                SettingsRow(
                    title = stringResource(R.string.settings_storage_location),
                    value = storageDisplayName ?: stringResource(R.string.settings_storage_internal),
                    isDark = isDark,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        // Android 13+ (TIRAMISU): No permission needed for SAF picker
                        // Android 10-12: Request READ_EXTERNAL_STORAGE first
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            storageFolderLauncher.launch(null)
                        } else {
                            storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                )
            }

            // TORRENT & CLOUD Section
            GlassSectionHeader(title = stringResource(R.string.settings_cloud_torrents), isDark = isDark)

            SettingsCard(isDark = isDark) {
                SettingsRow(
                    title = stringResource(R.string.settings_torrent_downloads),
                    subtitle = stringResource(R.string.settings_torrent_desc),
                    isDark = isDark,
                    showChevron = true,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onNavigateToDownloads()
                    }
                )
                GlassDivider(isDark = isDark, startIndent = GlassSpacing.M)
                SettingsRow(
                    title = stringResource(R.string.settings_cloud_files),
                    subtitle = stringResource(R.string.settings_cloud_files_desc),
                    isDark = isDark,
                    showChevron = true,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onNavigateToCloudFiles()
                    }
                )
                GlassDivider(isDark = isDark, startIndent = GlassSpacing.M)
                SettingsRow(
                    title = stringResource(R.string.settings_listening_stats),
                    subtitle = stringResource(R.string.settings_stats_desc),
                    isDark = isDark,
                    showChevron = true,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onNavigateToStats()
                    }
                )
            }

            // LIBRARY Section
            GlassSectionHeader(title = stringResource(R.string.settings_library), isDark = isDark)

            SettingsCard(isDark = isDark) {
                SettingsToggle(
                    title = stringResource(R.string.settings_scan_on_startup),
                    subtitle = stringResource(R.string.settings_scan_desc),
                    isEnabled = scanOnStartup,
                    isDark = isDark,
                    accentColor = accentColor,
                    isReverieDark = isReverieDark,
                    onToggle = { scanOnStartup = it }
                )
                GlassDivider(isDark = isDark, startIndent = GlassSpacing.M)
                SettingsRow(
                    title = stringResource(R.string.settings_manage_folders),
                    isDark = isDark,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showFoldersDialog = true
                    }
                )
            }

            // KIDS MODE Section
            GlassSectionHeader(title = stringResource(R.string.settings_kids_mode), isDark = isDark)

            SettingsCard(isDark = isDark) {
                SettingsToggle(
                    title = stringResource(R.string.settings_kids_enabled),
                    subtitle = if (kidsModeEnabled) stringResource(R.string.settings_kids_restricted) else stringResource(R.string.settings_kids_restrict),
                    isEnabled = kidsModeEnabled,
                    isDark = isDark,
                    accentColor = accentColor,
                    isReverieDark = isReverieDark,
                    onToggle = { enabled ->
                        if (enabled) {
                            // Show PIN setup dialog when enabling
                            if (kidsModePin.isEmpty()) {
                                showKidsModeDialog = true
                            } else {
                                settingsViewModel.setKidsModeEnabled(true)
                            }
                        } else {
                            // Require PIN to disable
                            showKidsModeDialog = true
                        }
                    }
                )
                if (kidsModeEnabled) {
                    GlassDivider(isDark = isDark, startIndent = GlassSpacing.M)
                    SettingsRow(
                        title = stringResource(R.string.settings_kids_change_pin),
                        subtitle = stringResource(R.string.settings_kids_update_pin),
                        isDark = isDark,
                        onClick = { showKidsModeDialog = true }
                    )
                }
            }

            // AUDIO Section
            GlassSectionHeader(title = stringResource(R.string.settings_audio), isDark = isDark)

            SettingsCard(isDark = isDark) {
                SettingsRow(
                    title = stringResource(R.string.settings_equalizer),
                    value = stringResource(R.string.settings_equalizer_desc),
                    isDark = isDark,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onOpenEqualizer()
                    }
                )
                GlassDivider(isDark = isDark, startIndent = GlassSpacing.M)
                SettingsRow(
                    title = stringResource(R.string.settings_decoder),
                    value = audioCodec,
                    isDark = isDark,
                    onClick = { showCodecDialog = true }
                )
            }

            // ABOUT Section
            GlassSectionHeader(title = stringResource(R.string.settings_about), isDark = isDark)

            SettingsCard(isDark = isDark) {
                SettingsRow(
                    title = stringResource(R.string.settings_version),
                    value = "3.1.12",
                    isDark = isDark,
                    showChevron = false,
                    onClick = null
                )
                GlassDivider(isDark = isDark, startIndent = GlassSpacing.M)
                SettingsRow(
                    title = stringResource(R.string.settings_privacy),
                    isDark = isDark,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        android.widget.Toast.makeText(view.context, view.context.getString(R.string.toast_privacy_coming_soon), android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                GlassDivider(isDark = isDark, startIndent = GlassSpacing.M)
                SettingsRow(
                    title = stringResource(R.string.settings_terms),
                    isDark = isDark,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        android.widget.Toast.makeText(view.context, view.context.getString(R.string.toast_terms_coming_soon), android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(GlassSpacing.XXXL))
        }
    }

    // Theme Mode Dialog
    if (showThemeModeDialog) {
        ThemeModeDialog(
            isDark = isDark,
            isReverieDark = isReverieDark,
            currentMode = themeMode,
            currentAccentVariant = reverieAccentVariant,
            onModeSelected = { mode ->
                themeViewModel.setThemeMode(mode)
                showThemeModeDialog = false
            },
            onAccentSelected = { variant ->
                themeViewModel.setReverieAccentVariant(variant)
            },
            onDismiss = { showThemeModeDialog = false }
        )
    }

    // Skip Duration Dialog
    if (showSkipDurationDialog) {
        SkipDurationDialog(
            isDark = isDark,
            isReverieDark = isReverieDark,
            currentSkipForward = skipForward,
            currentSkipBackward = skipBackward,
            onDismiss = { showSkipDurationDialog = false },
            onDurationSelected = { seconds ->
                settingsViewModel.setSkipForward(seconds)
                settingsViewModel.setSkipBackward(seconds)
                showSkipDurationDialog = false
            }
        )
    }

    // Audio Codec Dialog
    if (showCodecDialog) {
        AudioCodecDialog(
            isDark = isDark,
            isReverieDark = isReverieDark,
            currentCodec = audioCodec,
            onCodecSelected = { codec ->
                settingsViewModel.setAudioCodec(codec)
                showCodecDialog = false
            },
            onDismiss = { showCodecDialog = false }
        )
    }

    // Kids Mode PIN Dialog
    if (showKidsModeDialog) {
        KidsModeDialog(
            isDark = isDark,
            isReverieDark = isReverieDark,
            isEnabled = kidsModeEnabled,
            hasExistingPin = kidsModePin.isNotEmpty(),
            onSetPin = { newPin ->
                settingsViewModel.setKidsModePin(newPin)
                settingsViewModel.setKidsModeEnabled(true)
                showKidsModeDialog = false
            },
            onDisable = {
                settingsViewModel.setKidsModeEnabled(false)
                showKidsModeDialog = false
            },
            onVerifyPin = { enteredPin ->
                settingsViewModel.verifyKidsModePin(enteredPin)
            },
            onDismiss = { showKidsModeDialog = false }
        )
    }

    // Language Dialog
    if (showLanguageDialog) {
        LanguageDialog(
            isDark = isDark,
            isReverieDark = isReverieDark,
            currentLanguage = appLanguage,
            onLanguageSelected = { language ->
                settingsViewModel.setAppLanguage(language)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // Folders Dialog
    if (showFoldersDialog) {
        ManageFoldersDialog(
            isDark = isDark,
            isReverieDark = isReverieDark,
            settingsViewModel = settingsViewModel,
            onDismiss = { showFoldersDialog = false }
        )
    }
}

// ============================================================================
// SETTINGS CARD
// ============================================================================

@Composable
private fun SettingsCard(
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = glassTheme(isDark)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Small))
            .background(theme.glassCard)
            .border(
                width = 0.5.dp,
                color = theme.glassBorder,
                shape = RoundedCornerShape(GlassShapes.Small)
            ),
        content = content
    )
}

// ============================================================================
// SETTINGS ROW
// ============================================================================

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    isDark: Boolean,
    showChevron: Boolean = true,
    onClick: (() -> Unit)?
) {
    val view = LocalView.current
    val theme = glassTheme(isDark)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Premium spring scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 500f
        ),
        label = "scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onClick()
                    }
                } else Modifier
            )
            .padding(horizontal = GlassSpacing.M, vertical = GlassSpacing.S + GlassSpacing.XXS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = GlassTypography.Body,
                color = theme.textPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = GlassTypography.Caption,
                    color = theme.textSecondary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value,
                    style = GlassTypography.Body,
                    color = theme.textSecondary
                )
            }
            if (showChevron && onClick != null) {
                Spacer(modifier = Modifier.width(GlassSpacing.XXS))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = theme.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ============================================================================
// SETTINGS TOGGLE
// ============================================================================

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String? = null,
    isEnabled: Boolean,
    isDark: Boolean,
    accentColor: Color = GlassColors.Interactive,
    isReverieDark: Boolean = false,
    onToggle: (Boolean) -> Unit
) {
    val view = LocalView.current
    val theme = glassTheme(isDark, isReverieDark)
    var checked by remember { mutableStateOf(isEnabled) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Premium spring scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 500f
        ),
        label = "scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                checked = !checked
                onToggle(checked)
            }
            .padding(horizontal = GlassSpacing.M, vertical = GlassSpacing.S),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = GlassTypography.Body,
                color = theme.textPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = GlassTypography.Caption,
                    color = theme.textSecondary
                )
            }
        }

        // Clean iOS-style toggle - no border wrapper, white thumb always
        Switch(
            checked = checked,
            onCheckedChange = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                checked = it
                onToggle(it)
            },
            colors = SwitchDefaults.colors(
                // White thumb in both states for consistency
                checkedThumbColor = Color.White,
                uncheckedThumbColor = Color.White,
                // Accent color when ON, subtle grey when OFF
                checkedTrackColor = accentColor,
                uncheckedTrackColor = if (isReverieDark) Color.White.copy(alpha = 0.12f) else theme.glassBorder,
                // Border colors for the track
                checkedBorderColor = Color.Transparent,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

// ============================================================================
// DIALOGS
// ============================================================================

@Composable
private fun ThemeModeDialog(
    isDark: Boolean,
    isReverieDark: Boolean = false,
    currentMode: ThemeMode,
    currentAccentVariant: ReverieAccentVariant = ReverieAccentVariant.COPPER_BORDER,
    onModeSelected: (ThemeMode) -> Unit,
    onAccentSelected: (ReverieAccentVariant) -> Unit = {},
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val currentAccentColor = currentAccentVariant.accentColor
    val currentHighlightColor = currentAccentVariant.highlightColor

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = stringResource(R.string.settings_theme_title),
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_theme_desc),
                    style = GlassTypography.Caption,
                    color = theme.textSecondary,
                    modifier = Modifier.padding(bottom = GlassSpacing.S)
                )
                ThemeMode.entries.forEach { mode ->
                    val isSelected = currentMode == mode
                    // Special accent color for Reverie Dark when selected
                    val modeAccentColor = if (mode == ThemeMode.REVERIE_DARK) {
                        currentAccentColor
                    } else {
                        theme.textPrimary
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GlassShapes.Small))
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        if (mode == ThemeMode.REVERIE_DARK) {
                                            currentHighlightColor  // Use the proper highlight color (warm slate or subtle copper)
                                        } else if (isDark) {
                                            Color.White.copy(alpha = 0.1f)
                                        } else {
                                            Color.Black.copy(alpha = 0.08f)
                                        }
                                    )
                                } else Modifier
                            )
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = GlassSpacing.S, horizontal = GlassSpacing.XS),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.displayName,
                                style = GlassTypography.Body,
                                color = if (isSelected && mode == ThemeMode.REVERIE_DARK) modeAccentColor else theme.textPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                text = mode.description,
                                style = GlassTypography.Caption,
                                color = theme.textSecondary
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = if (mode == ThemeMode.REVERIE_DARK) modeAccentColor else theme.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Accent Color Picker (only show when Reverie Dark is selected)
                if (currentMode == ThemeMode.REVERIE_DARK) {
                    Spacer(modifier = Modifier.height(GlassSpacing.M))
                    HorizontalDivider(color = theme.glassBorder)
                    Spacer(modifier = Modifier.height(GlassSpacing.M))

                    Text(
                        text = stringResource(R.string.settings_accent_color),
                        style = GlassTypography.Body,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.textPrimary
                    )
                    Spacer(modifier = Modifier.height(GlassSpacing.S))

                    Text(
                        text = stringResource(R.string.settings_accent_desc),
                        style = GlassTypography.Caption,
                        color = theme.textSecondary,
                        modifier = Modifier.padding(bottom = GlassSpacing.XS)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
                    ) {
                        ReverieAccentVariant.entries.forEach { variant ->
                            val isAccentSelected = currentAccentVariant == variant
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(GlassShapes.Small))
                                    .background(
                                        if (isAccentSelected) variant.highlightColor
                                        else Color.Transparent
                                    )
                                    .then(
                                        if (isAccentSelected && variant.useBorderHighlight) {
                                            Modifier.border(1.dp, variant.accentColor, RoundedCornerShape(GlassShapes.Small))
                                        } else Modifier
                                    )
                                    .clickable { onAccentSelected(variant) }
                                    .padding(GlassSpacing.S),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Preview box showing the highlight style
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (variant.useBorderHighlight) {
                                                Color(0xFF1C1C1E)
                                            } else {
                                                variant.highlightColor
                                            }
                                        )
                                        .then(
                                            if (variant.useBorderHighlight) {
                                                Modifier.border(1.5.dp, variant.accentColor, RoundedCornerShape(8.dp))
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = variant.accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(GlassSpacing.XS))
                                Text(
                                    text = variant.displayName,
                                    style = GlassTypography.Caption,
                                    color = if (isAccentSelected) variant.accentColor else theme.textSecondary,
                                    fontWeight = if (isAccentSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun SkipDurationDialog(
    isDark: Boolean,
    isReverieDark: Boolean = false,
    currentSkipForward: Int,
    currentSkipBackward: Int,
    onDismiss: () -> Unit,
    onDurationSelected: (Int) -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val durations = listOf(10, 15, 30, 45, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = stringResource(R.string.settings_skip_duration_title),
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_skip_duration_desc),
                    style = GlassTypography.Caption,
                    color = theme.textSecondary,
                    modifier = Modifier.padding(bottom = GlassSpacing.S)
                )
                durations.forEach { seconds ->
                    val isSelected = currentSkipForward == seconds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GlassShapes.Small))
                            .clickable { onDurationSelected(seconds) }
                            .padding(vertical = GlassSpacing.S),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_seconds, seconds),
                            style = GlassTypography.Body,
                            color = if (isSelected) theme.interactive else theme.textPrimary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = theme.interactive,
                                unselectedColor = theme.textTertiary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_done), color = theme.interactive, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun AudioCodecDialog(
    isDark: Boolean,
    isReverieDark: Boolean = false,
    currentCodec: String,
    onCodecSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)

    // Codec data: ID, Display Name Resource, Description Resource, Formats Resource
    data class CodecInfo(val id: String, val nameRes: Int, val descRes: Int, val formatsRes: Int)
    val codecs = listOf(
        CodecInfo("Default", R.string.settings_decoder_default, R.string.settings_decoder_default_desc, R.string.settings_decoder_default_formats),
        CodecInfo("MediaCodec", R.string.settings_decoder_mediacodec, R.string.settings_decoder_mediacodec_desc, R.string.settings_decoder_mediacodec_formats),
        CodecInfo("FFmpeg", R.string.settings_decoder_ffmpeg, R.string.settings_decoder_ffmpeg_desc, R.string.settings_decoder_ffmpeg_formats),
        CodecInfo("OpenSL ES", R.string.settings_decoder_opensles, R.string.settings_decoder_opensles_desc, R.string.settings_decoder_opensles_formats)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = stringResource(R.string.settings_audio_decoder_title),
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.settings_audio_decoder_desc),
                    style = GlassTypography.Caption,
                    color = theme.textSecondary,
                    modifier = Modifier.padding(bottom = GlassSpacing.M)
                )

                codecs.forEach { codecInfo ->
                    val isSelected = currentCodec == codecInfo.id
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GlassShapes.Small))
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        1.dp,
                                        theme.interactive,
                                        RoundedCornerShape(GlassShapes.Small)
                                    )
                                } else Modifier
                            )
                            .clickable { onCodecSelected(codecInfo.id) }
                            .padding(GlassSpacing.S)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = theme.interactive,
                                    unselectedColor = theme.textTertiary
                                )
                            )
                            Spacer(modifier = Modifier.width(GlassSpacing.XS))
                            Column {
                                Text(
                                    text = stringResource(codecInfo.nameRes),
                                    style = GlassTypography.Body,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) theme.interactive else theme.textPrimary
                                )
                                Text(
                                    text = stringResource(codecInfo.descRes),
                                    style = GlassTypography.Caption,
                                    color = theme.textSecondary
                                )
                                Text(
                                    text = stringResource(codecInfo.formatsRes),
                                    style = GlassTypography.Caption,
                                    color = theme.interactive.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(GlassSpacing.S))
                }

                HorizontalDivider(color = theme.glassBorder)
                Spacer(modifier = Modifier.height(GlassSpacing.S))

                Text(
                    text = stringResource(R.string.settings_audiophile_formats),
                    style = GlassTypography.Caption,
                    fontWeight = FontWeight.Bold,
                    color = theme.interactive
                )
                Spacer(modifier = Modifier.height(GlassSpacing.XXS))
                Text(
                    text = stringResource(R.string.settings_audiophile_desc),
                    style = GlassTypography.Caption,
                    color = theme.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_done), color = theme.interactive, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun KidsModeDialog(
    isDark: Boolean,
    isReverieDark: Boolean = false,
    isEnabled: Boolean,
    hasExistingPin: Boolean,
    onSetPin: (String) -> Unit,
    onDisable: () -> Unit,
    onVerifyPin: suspend (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(isEnabled && hasExistingPin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = when {
                    isVerifying -> stringResource(R.string.settings_kids_enter_pin)
                    isEnabled -> stringResource(R.string.settings_kids_change_pin)
                    else -> stringResource(R.string.settings_kids_setup)
                },
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = when {
                        isVerifying -> stringResource(R.string.settings_kids_enter_pin_desc)
                        isEnabled -> stringResource(R.string.settings_kids_new_pin_desc)
                        else -> stringResource(R.string.settings_kids_create_pin_desc)
                    },
                    style = GlassTypography.Caption,
                    color = theme.textSecondary,
                    modifier = Modifier.padding(bottom = GlassSpacing.M)
                )

                // PIN Input
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text(stringResource(R.string.settings_kids_pin_label), color = theme.textSecondary) },
                    placeholder = { Text(stringResource(R.string.settings_kids_pin_placeholder), color = theme.textTertiary) },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                        focusedBorderColor = theme.interactive,
                        unfocusedBorderColor = theme.glassBorder,
                        cursorColor = theme.interactive
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Confirm PIN (only for setup/change)
                if (!isVerifying) {
                    Spacer(modifier = Modifier.height(GlassSpacing.S))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                confirmPin = it
                                errorMessage = null
                            }
                        },
                        label = { Text(stringResource(R.string.settings_kids_pin_confirm), color = theme.textSecondary) },
                        placeholder = { Text(stringResource(R.string.settings_kids_pin_placeholder), color = theme.textTertiary) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.interactive,
                            unfocusedBorderColor = theme.glassBorder,
                            cursorColor = theme.interactive
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Error Message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(GlassSpacing.S))
                    Text(
                        text = errorMessage!!,
                        style = GlassTypography.Caption,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isVerifying) {
                        // Verify PIN to disable
                        scope.launch {
                            val verified = onVerifyPin(pin)
                            if (verified) {
                                onDisable()
                            } else {
                                errorMessage = view.context.getString(R.string.settings_kids_pin_incorrect)
                            }
                        }
                    } else {
                        // Set new PIN
                        when {
                            pin.length != 4 -> errorMessage = view.context.getString(R.string.settings_kids_pin_error_length)
                            pin != confirmPin -> errorMessage = view.context.getString(R.string.settings_kids_pin_error_match)
                            else -> onSetPin(pin)
                        }
                    }
                }
            ) {
                Text(
                    if (isVerifying) stringResource(R.string.settings_kids_disable) else stringResource(R.string.settings_kids_enable),
                    color = if (isVerifying) Color(0xFFEF4444) else theme.interactive,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), color = theme.textSecondary)
            }
        }
    )
}

@Composable
private fun LanguageDialog(
    isDark: Boolean,
    isReverieDark: Boolean = false,
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)

    // Available languages
    val languages = listOf(
        stringResource(R.string.lang_system) to stringResource(R.string.lang_system),
        stringResource(R.string.lang_english) to stringResource(R.string.lang_english),
        stringResource(R.string.lang_spanish) to "Español",
        stringResource(R.string.lang_french) to "Français",
        stringResource(R.string.lang_german) to "Deutsch",
        stringResource(R.string.lang_italian) to "Italiano",
        stringResource(R.string.lang_portuguese) to "Português",
        stringResource(R.string.lang_japanese) to "日本語",
        stringResource(R.string.lang_korean) to "한국어",
        stringResource(R.string.lang_chinese) to "中文",
        stringResource(R.string.lang_russian) to "Русский",
        stringResource(R.string.lang_arabic) to "العربية"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = stringResource(R.string.settings_language_title),
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.settings_language_desc),
                    style = GlassTypography.Caption,
                    color = theme.textSecondary,
                    modifier = Modifier.padding(bottom = GlassSpacing.S)
                )
                languages.forEach { (code, displayName) ->
                    val isSelected = currentLanguage == code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GlassShapes.Small))
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        if (isDark) Color.White.copy(alpha = 0.1f)
                                        else Color.Black.copy(alpha = 0.08f)
                                    )
                                } else Modifier
                            )
                            .clickable { onLanguageSelected(code) }
                            .padding(vertical = GlassSpacing.S, horizontal = GlassSpacing.XS),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = code,
                                style = GlassTypography.Body,
                                color = if (isSelected) theme.interactive else theme.textPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (displayName != code) {
                                Text(
                                    text = displayName,
                                    style = GlassTypography.Caption,
                                    color = theme.textSecondary
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = theme.interactive,
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

@Composable
private fun ManageFoldersDialog(
    isDark: Boolean,
    isReverieDark: Boolean = false,
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val libraryFolders by settingsViewModel.libraryFolders.collectAsState()
    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<String?>(null) }

    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            android.util.Log.d("ManageFoldersDialog", "=== FOLDER SELECTED ===")
            android.util.Log.d("ManageFoldersDialog", "Selected URI: $uri")
            android.util.Log.d("ManageFoldersDialog", "URI scheme: ${uri.scheme}, authority: ${uri.authority}")

            // Take persistable permission - include both READ and WRITE for full access
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                               Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                android.util.Log.d("ManageFoldersDialog", "Taking persistable permission with flags: $takeFlags")
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                android.util.Log.d("ManageFoldersDialog", "Permission taken successfully!")

                // Verify the permission was actually saved
                val persistedUris = context.contentResolver.persistedUriPermissions
                android.util.Log.d("ManageFoldersDialog", "Total persisted permissions: ${persistedUris.size}")
                persistedUris.forEach { perm ->
                    android.util.Log.d("ManageFoldersDialog", "  Persisted: ${perm.uri} (read=${perm.isReadPermission}, write=${perm.isWritePermission})")
                }

                val wasAdded = persistedUris.any { it.uri.toString() == uri.toString() }
                android.util.Log.d("ManageFoldersDialog", "Permission verified in persisted list: $wasAdded")

                // Add to settings
                val uriString = uri.toString()
                android.util.Log.d("ManageFoldersDialog", "Adding folder to settings: $uriString")
                settingsViewModel.addLibraryFolder(uriString)
                android.util.Log.d("ManageFoldersDialog", "Folder added to settings!")

                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                android.widget.Toast.makeText(
                    context,
                    "Folder added successfully",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: SecurityException) {
                android.util.Log.e("ManageFoldersDialog", "SecurityException: Failed to take permission", e)
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.toast_storage_permission_error),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.util.Log.e("ManageFoldersDialog", "Failed to add folder", e)
                e.printStackTrace()
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.toast_storage_permission_error),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            android.util.Log.d("ManageFoldersDialog", "Folder picker cancelled (uri is null)")
        }
    }

    // Permission launcher for folder picker (Android 10-12)
    val folderPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            folderPickerLauncher.launch(null)
        } else {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.toast_storage_permission_denied),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        shape = RoundedCornerShape(GlassShapes.Medium),
        title = {
            Text(
                text = stringResource(R.string.folder_dialog_title),
                style = GlassTypography.Headline,
                color = theme.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Folder list - show empty state OR folder list
                if (libraryFolders.isEmpty()) {
                    // Show tip only when no folders are configured
                    Text(
                        text = stringResource(R.string.folder_select_tip),
                        style = GlassTypography.Caption,
                        color = theme.textTertiary,
                        modifier = Modifier.padding(bottom = GlassSpacing.M)
                    )
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = GlassSpacing.L),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(GlassSpacing.XS)
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = theme.textTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = stringResource(R.string.folder_dialog_empty),
                            style = GlassTypography.Body,
                            color = theme.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.folder_dialog_empty_desc),
                            style = GlassTypography.Caption,
                            color = theme.textTertiary
                        )
                    }
                } else {
                    // Display folders
                    libraryFolders.forEach { folderUri ->
                        FolderListItem(
                            folderUri = folderUri,
                            isDark = isDark,
                            isReverieDark = isReverieDark,
                            onRemove = {
                                settingsViewModel.removeLibraryFolder(folderUri)
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                        )
                        Spacer(modifier = Modifier.height(GlassSpacing.XS))
                    }
                }

                // Scan result
                if (scanResult != null) {
                    Spacer(modifier = Modifier.height(GlassSpacing.S))
                    Text(
                        text = scanResult!!,
                        style = GlassTypography.Caption,
                        color = theme.interactive,
                        modifier = Modifier.padding(GlassSpacing.S)
                    )
                }

                Spacer(modifier = Modifier.height(GlassSpacing.M))

                // Add Folder Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
                ) {
                    GlassButton(
                        text = stringResource(R.string.folder_dialog_add),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            // Android 13+ (TIRAMISU): No permission needed for SAF picker
                            // Android 10-12: Request READ_EXTERNAL_STORAGE first
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                folderPickerLauncher.launch(null)
                            } else {
                                folderPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        isDark = isDark,
                        isPrimary = false,
                        icon = Icons.Default.Add
                    )

                    // Scan All Button (only show if there are folders)
                    if (libraryFolders.isNotEmpty()) {
                        GlassButton(
                            text = if (isScanning)
                                stringResource(R.string.folder_dialog_scanning)
                            else
                                stringResource(R.string.folder_dialog_scan_all),
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                isScanning = true
                                scanResult = null

                                scope.launch {
                                    try {
                                        val booksFound = settingsViewModel.scanLibraryFolders()
                                        scanResult = context.getString(R.string.folder_dialog_scan_complete, booksFound)
                                        isScanning = false
                                    } catch (e: Exception) {
                                        scanResult = "Scan failed: ${e.message}"
                                        isScanning = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            isDark = isDark,
                            isPrimary = true,
                            icon = Icons.Default.Refresh,
                            enabled = !isScanning
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.dialog_done),
                    color = theme.interactive,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun FolderListItem(
    folderUri: String,
    isDark: Boolean,
    isReverieDark: Boolean = false,
    onRemove: () -> Unit
) {
    val theme = glassTheme(isDark, isReverieDark)
    val context = LocalContext.current
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 500f
        ),
        label = "scale"
    )

    // Get folder name from URI
    val folderName = remember(folderUri) {
        try {
            val uri = Uri.parse(folderUri)
            // Extract last segment from path - typically the folder name
            uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown Folder"
        } catch (e: Exception) {
            "Unknown Folder"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(GlassShapes.Small))
            .background(
                if (isDark) Color.White.copy(alpha = 0.05f)
                else Color.Black.copy(alpha = 0.03f)
            )
            .border(
                width = 0.5.dp,
                color = theme.glassBorder,
                shape = RoundedCornerShape(GlassShapes.Small)
            )
            .padding(horizontal = GlassSpacing.S, vertical = GlassSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlassSpacing.S)
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = theme.interactive,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = folderName,
                style = GlassTypography.Body,
                color = theme.textPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        // Remove button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onRemove()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove folder",
                tint = theme.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
