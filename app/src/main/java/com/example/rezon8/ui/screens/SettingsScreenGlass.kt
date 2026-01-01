package com.mossglen.lithos.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.unit.sp
import com.mossglen.lithos.R
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.mossglen.lithos.ui.components.*
import com.mossglen.lithos.ui.theme.*
import com.mossglen.lithos.ui.viewmodel.LithosAccentVariant
import com.mossglen.lithos.ui.viewmodel.SettingsViewModel
import com.mossglen.lithos.ui.viewmodel.ThemeMode
import com.mossglen.lithos.ui.viewmodel.ThemeViewModel
/**
 * LITHOS - Settings Screen
 *
 * Premium settings interface matching Profile page style.
 * Icon-based menu items with clean visual hierarchy.
 * Sections ordered by frequency of use.
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
    val isOLED = themeMode == ThemeMode.LITHOS_DARK
    val lithosAccentVariant by themeViewModel.lithosAccentVariant
    val theme = glassTheme(isDark, isOLED)

    // Accent color - uses selected variant for Reverie Dark, blue for standard
    val accentColor = if (isOLED) themeViewModel.getLithosAccentColor() else GlassColors.Interactive

    // Settings state from ViewModel
    val wifiOnly by settingsViewModel.wifiOnly.collectAsState()
    val skipForward by settingsViewModel.skipForward.collectAsState()
    val skipBackward by settingsViewModel.skipBackward.collectAsState()
    val keepServiceActive by settingsViewModel.keepServiceActive.collectAsState()
    val smartAutoRewindEnabled by settingsViewModel.smartAutoRewindEnabled.collectAsState()
    val audioCodec by settingsViewModel.audioCodec.collectAsState()
    val dynamicPlayerColors by settingsViewModel.dynamicPlayerColors.collectAsState()

    // Kids Mode state
    val kidsModeEnabled by settingsViewModel.kidsModeEnabled.collectAsState()
    val kidsModePin by settingsViewModel.kidsModePin.collectAsState()

    // Language state
    val appLanguage by settingsViewModel.appLanguage.collectAsState()

    // Map language codes to display names (localized + native)
    val languageDisplayName = when (appLanguage) {
        "system" -> stringResource(R.string.lang_system)
        "en" -> "English"
        "es" -> "Español"
        "fr" -> "Français"
        "de" -> "Deutsch"
        "it" -> "Italiano"
        "pt" -> "Português"
        "ja" -> "日本語"
        "ko" -> "한국어"
        "zh" -> "中文"
        "ru" -> "Русский"
        "ar" -> "العربية"
        else -> appLanguage // Fallback to code if unknown
    }

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

        // Scrollable content - Profile-style menu items
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp) // Space for nav bar
        ) {
            // ══════════════════════════════════════════════════════════════
            // APPEARANCE Section (Most Used)
            // ══════════════════════════════════════════════════════════════
            SettingsSectionHeader(
                title = stringResource(R.string.settings_appearance),
                isDark = isDark
            )

            SettingsMenuItem(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.settings_theme),
                subtitle = themeMode.displayName,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showThemeModeDialog = true
                },
                isDark = isDark,
                accentColor = accentColor
            )

            SettingsMenuItem(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.settings_language),
                subtitle = languageDisplayName,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showLanguageDialog = true
                },
                isDark = isDark,
                accentColor = accentColor
            )

            SettingsMenuToggle(
                icon = Icons.Outlined.AutoAwesome,
                title = stringResource(R.string.settings_dynamic_colors),
                subtitle = stringResource(R.string.detail_change_cover),
                isEnabled = dynamicPlayerColors,
                onToggle = { settingsViewModel.setDynamicPlayerColors(it) },
                isDark = isDark,
                accentColor = accentColor,
                isOLED = isOLED
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ══════════════════════════════════════════════════════════════
            // PLAYBACK Section
            // ══════════════════════════════════════════════════════════════
            SettingsSectionHeader(
                title = stringResource(R.string.settings_player),
                isDark = isDark
            )

            SettingsMenuItem(
                icon = Icons.Outlined.Speed,
                title = stringResource(R.string.settings_skip_duration_title),
                subtitle = stringResource(R.string.settings_seconds, skipForward) + " / " + stringResource(R.string.settings_seconds, skipBackward),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showSkipDurationDialog = true
                },
                isDark = isDark,
                accentColor = accentColor
            )

            SettingsMenuToggle(
                icon = Icons.Outlined.PlayCircle,
                title = stringResource(R.string.settings_persistent),
                subtitle = stringResource(R.string.settings_persistent_desc),
                isEnabled = keepServiceActive,
                onToggle = { settingsViewModel.setKeepServiceActive(it) },
                isDark = isDark,
                accentColor = accentColor,
                isOLED = isOLED
            )

            SettingsMenuToggle(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.settings_smart_rewind),
                subtitle = stringResource(R.string.settings_smart_rewind_desc),
                isEnabled = smartAutoRewindEnabled,
                onToggle = { settingsViewModel.setSmartAutoRewindEnabled(it) },
                isDark = isDark,
                accentColor = accentColor,
                isOLED = isOLED
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ══════════════════════════════════════════════════════════════
            // AUDIO Section
            // ══════════════════════════════════════════════════════════════
            SettingsSectionHeader(
                title = stringResource(R.string.settings_audio),
                isDark = isDark
            )

            SettingsMenuItem(
                icon = Icons.Outlined.Equalizer,
                title = stringResource(R.string.settings_equalizer),
                subtitle = stringResource(R.string.settings_equalizer_desc),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onOpenEqualizer()
                },
                isDark = isDark,
                accentColor = accentColor
            )

            SettingsMenuItem(
                icon = Icons.Outlined.Memory,
                title = stringResource(R.string.settings_decoder),
                subtitle = audioCodec,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showCodecDialog = true
                },
                isDark = isDark,
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ══════════════════════════════════════════════════════════════
            // LIBRARY Section
            // ══════════════════════════════════════════════════════════════
            SettingsSectionHeader(
                title = stringResource(R.string.settings_library),
                isDark = isDark
            )

            SettingsMenuToggle(
                icon = Icons.Outlined.Refresh,
                title = stringResource(R.string.settings_scan_on_startup),
                subtitle = stringResource(R.string.settings_scan_desc),
                isEnabled = scanOnStartup,
                onToggle = { scanOnStartup = it },
                isDark = isDark,
                accentColor = accentColor,
                isOLED = isOLED
            )

            SettingsMenuItem(
                icon = Icons.Outlined.Folder,
                title = stringResource(R.string.settings_manage_folders),
                subtitle = stringResource(R.string.folder_dialog_title),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showFoldersDialog = true
                },
                isDark = isDark,
                accentColor = accentColor
            )

            // Cover Art Refresh Button
            var isRefreshingCovers by remember { mutableStateOf(false) }
            var coverRefreshProgress by remember { mutableStateOf(Pair(0, 0)) }
            var coverRefreshResult by remember { mutableStateOf<String?>(null) }
            val coverRefreshScope = rememberCoroutineScope()

            SettingsMenuItem(
                icon = Icons.Outlined.Image,
                title = stringResource(R.string.settings_refresh_covers),
                subtitle = if (isRefreshingCovers) {
                    stringResource(R.string.settings_refresh_covers_progress, coverRefreshProgress.first, coverRefreshProgress.second)
                } else {
                    coverRefreshResult ?: stringResource(R.string.settings_refresh_covers_desc)
                },
                onClick = {
                    if (!isRefreshingCovers) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        isRefreshingCovers = true
                        coverRefreshResult = null
                        coverRefreshScope.launch {
                            try {
                                val replaced = settingsViewModel.recheckAllCovers { current, total ->
                                    coverRefreshProgress = Pair(current, total)
                                }
                                coverRefreshResult = context.getString(R.string.settings_refresh_covers_result, replaced)
                            } catch (e: Exception) {
                                coverRefreshResult = context.getString(R.string.settings_refresh_covers_error)
                            } finally {
                                isRefreshingCovers = false
                            }
                        }
                    }
                },
                isDark = isDark,
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ══════════════════════════════════════════════════════════════
            // DOWNLOADS Section
            // ══════════════════════════════════════════════════════════════
            SettingsSectionHeader(
                title = stringResource(R.string.settings_downloads),
                isDark = isDark
            )

            SettingsMenuToggle(
                icon = Icons.Outlined.Wifi,
                title = stringResource(R.string.settings_wifi_only),
                subtitle = stringResource(R.string.settings_wifi_only),
                isEnabled = wifiOnly,
                onToggle = { settingsViewModel.setWifiOnly(it) },
                isDark = isDark,
                accentColor = accentColor,
                isOLED = isOLED
            )

            SettingsMenuItem(
                icon = Icons.Outlined.Storage,
                title = stringResource(R.string.settings_storage_location),
                subtitle = storageDisplayName ?: stringResource(R.string.settings_storage_internal),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        storageFolderLauncher.launch(null)
                    } else {
                        storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                },
                isDark = isDark,
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ══════════════════════════════════════════════════════════════
            // CLOUD & TORRENTS Section
            // ══════════════════════════════════════════════════════════════
            SettingsSectionHeader(
                title = stringResource(R.string.settings_cloud_torrents),
                isDark = isDark
            )

            SettingsMenuItem(
                icon = Icons.Outlined.Download,
                title = stringResource(R.string.settings_torrent_downloads),
                subtitle = stringResource(R.string.settings_torrent_desc),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onNavigateToDownloads()
                },
                isDark = isDark,
                accentColor = accentColor
            )

            SettingsMenuItem(
                icon = Icons.Outlined.Cloud,
                title = stringResource(R.string.settings_cloud_files),
                subtitle = stringResource(R.string.settings_cloud_files_desc),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onNavigateToCloudFiles()
                },
                isDark = isDark,
                accentColor = accentColor
            )

            SettingsMenuItem(
                icon = Icons.Outlined.BarChart,
                title = stringResource(R.string.settings_listening_stats),
                subtitle = stringResource(R.string.settings_stats_desc),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onNavigateToStats()
                },
                isDark = isDark,
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ══════════════════════════════════════════════════════════════
            // KIDS MODE Section (Less Frequently Used)
            // ══════════════════════════════════════════════════════════════
            SettingsSectionHeader(
                title = stringResource(R.string.settings_kids_mode),
                isDark = isDark
            )

            SettingsMenuToggle(
                icon = Icons.Outlined.ChildCare,
                title = stringResource(R.string.settings_kids_enabled),
                subtitle = if (kidsModeEnabled) stringResource(R.string.settings_kids_restricted) else stringResource(R.string.settings_kids_restrict),
                isEnabled = kidsModeEnabled,
                onToggle = { enabled ->
                    if (enabled) {
                        if (kidsModePin.isEmpty()) {
                            showKidsModeDialog = true
                        } else {
                            settingsViewModel.setKidsModeEnabled(true)
                        }
                    } else {
                        showKidsModeDialog = true
                    }
                },
                isDark = isDark,
                accentColor = accentColor,
                isOLED = isOLED
            )

            if (kidsModeEnabled) {
                SettingsMenuItem(
                    icon = Icons.Outlined.Lock,
                    title = stringResource(R.string.settings_kids_change_pin),
                    subtitle = stringResource(R.string.settings_kids_update_pin),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showKidsModeDialog = true
                    },
                    isDark = isDark,
                    accentColor = accentColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ══════════════════════════════════════════════════════════════
            // ABOUT Section (Least Used - Bottom)
            // ══════════════════════════════════════════════════════════════
            SettingsSectionHeader(
                title = stringResource(R.string.settings_about),
                isDark = isDark
            )

            SettingsMenuItem(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.settings_version),
                subtitle = "3.1.36",
                onClick = null,
                isDark = isDark,
                accentColor = accentColor,
                showChevron = false
            )

            SettingsMenuItem(
                icon = Icons.Outlined.PrivacyTip,
                title = stringResource(R.string.settings_privacy),
                subtitle = stringResource(R.string.toast_privacy_coming_soon),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    android.widget.Toast.makeText(view.context, view.context.getString(R.string.toast_privacy_coming_soon), android.widget.Toast.LENGTH_SHORT).show()
                },
                isDark = isDark,
                accentColor = accentColor
            )

            SettingsMenuItem(
                icon = Icons.Outlined.Description,
                title = stringResource(R.string.settings_terms),
                subtitle = stringResource(R.string.toast_terms_coming_soon),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    android.widget.Toast.makeText(view.context, view.context.getString(R.string.toast_terms_coming_soon), android.widget.Toast.LENGTH_SHORT).show()
                },
                isDark = isDark,
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Text(
                text = "Lithos by Mossglen",
                style = GlassTypography.Caption.copy(fontSize = 12.sp),
                color = theme.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Theme Mode Dialog
    if (showThemeModeDialog) {
        ThemeModeDialog(
            isDark = isDark,
            isOLED = isOLED,
            currentMode = themeMode,
            currentAccentVariant = lithosAccentVariant,
            onModeSelected = { mode ->
                themeViewModel.setThemeMode(mode)
                showThemeModeDialog = false
            },
            onAccentSelected = { variant ->
                themeViewModel.setLithosAccentVariant(variant)
            },
            onDismiss = { showThemeModeDialog = false }
        )
    }

    // Skip Duration Dialog
    if (showSkipDurationDialog) {
        SkipDurationDialog(
            isDark = isDark,
            isOLED = isOLED,
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
            isOLED = isOLED,
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
            isOLED = isOLED,
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
            isOLED = isOLED,
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
            isOLED = isOLED,
            settingsViewModel = settingsViewModel,
            onDismiss = { showFoldersDialog = false }
        )
    }
}

// ============================================================================
// SETTINGS SECTION HEADER - Profile-style section title
// ============================================================================

@Composable
private fun SettingsSectionHeader(
    title: String,
    isDark: Boolean
) {
    val theme = glassTheme(isDark)

    Text(
        text = title.uppercase(),
        style = GlassTypography.Caption.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        ),
        color = theme.textSecondary.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

// ============================================================================
// SETTINGS MENU ITEM - Profile-style menu item with icon
// ============================================================================

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    isDark: Boolean,
    accentColor: Color = GlassColors.Interactive,
    showChevron: Boolean = true
) {
    val theme = glassTheme(isDark)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Premium spring scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
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
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container - matching Profile page style
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isDark) Color.White.copy(alpha = 0.08f)
                    else Color.Black.copy(alpha = 0.05f)
                ),
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

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = GlassTypography.Body.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = theme.textPrimary
            )
            Text(
                text = subtitle,
                style = GlassTypography.Caption.copy(fontSize = 13.sp),
                color = theme.textSecondary.copy(alpha = 0.7f)
            )
        }

        // Chevron
        if (showChevron && onClick != null) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = theme.textSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================================================
// SETTINGS MENU TOGGLE - Profile-style toggle with icon
// ============================================================================

@Composable
private fun SettingsMenuToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    isDark: Boolean,
    accentColor: Color = GlassColors.Interactive,
    isOLED: Boolean = false
) {
    val theme = glassTheme(isDark, isOLED)
    val view = LocalView.current
    var checked by remember { mutableStateOf(isEnabled) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Premium spring scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                checked = !checked
                onToggle(checked)
            }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container - matching Profile page style
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isDark) Color.White.copy(alpha = 0.08f)
                    else Color.Black.copy(alpha = 0.05f)
                ),
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

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = GlassTypography.Body.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = theme.textPrimary
            )
            Text(
                text = subtitle,
                style = GlassTypography.Caption.copy(fontSize = 13.sp),
                color = theme.textSecondary.copy(alpha = 0.7f)
            )
        }

        // Clean iOS-style toggle
        Switch(
            checked = checked,
            onCheckedChange = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                checked = it
                onToggle(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                uncheckedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedTrackColor = if (isOLED) Color.White.copy(alpha = 0.12f) else theme.glassBorder,
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
    isOLED: Boolean = false,
    currentMode: ThemeMode,
    currentAccentVariant: LithosAccentVariant = LithosAccentVariant.COPPER_BORDER,
    onModeSelected: (ThemeMode) -> Unit,
    onAccentSelected: (LithosAccentVariant) -> Unit = {},
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val currentAccentColor = currentAccentVariant.accentColor
    val currentHighlightColor = currentAccentVariant.highlightColor

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) LithosUI.SheetBackground else LithosUI.SheetBackgroundLight,
        shape = RoundedCornerShape(LithosComponents.Cards.dialogRadius),
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
                    val modeAccentColor = if (mode == ThemeMode.LITHOS_DARK) {
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
                                        if (mode == ThemeMode.LITHOS_DARK) {
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
                                color = if (isSelected && mode == ThemeMode.LITHOS_DARK) modeAccentColor else theme.textPrimary,
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
                                tint = if (mode == ThemeMode.LITHOS_DARK) modeAccentColor else theme.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Accent Color Picker (only show when Reverie Dark is selected)
                if (currentMode == ThemeMode.LITHOS_DARK) {
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
                        LithosAccentVariant.entries.forEach { variant ->
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
                                                LithosUI.SheetBackground
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
    isOLED: Boolean = false,
    currentSkipForward: Int,
    currentSkipBackward: Int,
    onDismiss: () -> Unit,
    onDurationSelected: (Int) -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val durations = listOf(10, 15, 30, 45, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) LithosUI.SheetBackground else LithosUI.SheetBackgroundLight,
        shape = RoundedCornerShape(LithosComponents.Cards.dialogRadius),
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
    isOLED: Boolean = false,
    currentCodec: String,
    onCodecSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)

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
        containerColor = if (isDark) LithosUI.SheetBackground else LithosUI.SheetBackgroundLight,
        shape = RoundedCornerShape(LithosComponents.Cards.dialogRadius),
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
    isOLED: Boolean = false,
    isEnabled: Boolean,
    hasExistingPin: Boolean,
    onSetPin: (String) -> Unit,
    onDisable: () -> Unit,
    onVerifyPin: suspend (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(isEnabled && hasExistingPin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) LithosUI.SheetBackground else LithosUI.SheetBackgroundLight,
        shape = RoundedCornerShape(LithosComponents.Cards.dialogRadius),
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
    isOLED: Boolean = false,
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)

    // Available languages: code -> (localized name, native name)
    // Use fixed codes for persistence, display localized names
    val languages = listOf(
        "system" to (stringResource(R.string.lang_system) to stringResource(R.string.lang_system)),
        "en" to (stringResource(R.string.lang_english) to "English"),
        "es" to (stringResource(R.string.lang_spanish) to "Español"),
        "fr" to (stringResource(R.string.lang_french) to "Français"),
        "de" to (stringResource(R.string.lang_german) to "Deutsch"),
        "it" to (stringResource(R.string.lang_italian) to "Italiano"),
        "pt" to (stringResource(R.string.lang_portuguese) to "Português"),
        "ja" to (stringResource(R.string.lang_japanese) to "日本語"),
        "ko" to (stringResource(R.string.lang_korean) to "한국어"),
        "zh" to (stringResource(R.string.lang_chinese) to "中文"),
        "ru" to (stringResource(R.string.lang_russian) to "Русский"),
        "ar" to (stringResource(R.string.lang_arabic) to "العربية")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) LithosUI.SheetBackground else LithosUI.SheetBackgroundLight,
        shape = RoundedCornerShape(LithosComponents.Cards.dialogRadius),
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
                languages.forEach { (code, names) ->
                    val (localizedName, nativeName) = names
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
                                text = localizedName,
                                style = GlassTypography.Body,
                                color = if (isSelected) theme.interactive else theme.textPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (nativeName != localizedName) {
                                Text(
                                    text = nativeName,
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
    isOLED: Boolean = false,
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
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
        containerColor = if (isDark) LithosUI.SheetBackground else LithosUI.SheetBackgroundLight,
        shape = RoundedCornerShape(LithosComponents.Cards.dialogRadius),
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
                            isOLED = isOLED,
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
    isOLED: Boolean = false,
    onRemove: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)
    val context = LocalContext.current
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Premium spring scale animation - using manifest standards
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
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
