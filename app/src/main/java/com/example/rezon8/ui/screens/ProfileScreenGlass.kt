package com.mossglen.reverie.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mossglen.reverie.haptics.HapticType
import com.mossglen.reverie.haptics.performHaptic
import com.mossglen.reverie.ui.theme.*
import com.mossglen.reverie.ui.viewmodel.ListeningStatsViewModel

/**
 * REVERIE Glass - Profile Screen
 *
 * Your personal hub. Stats at a glance, quick access to settings, achievements.
 * This consolidates Journey, Settings, and Account into one tab.
 *
 * Design Philosophy (Per PROJECT_MANIFEST):
 * - PROGRESSIVE DISCLOSURE: Quick stats visible, deep settings one tap away
 * - INVISIBLE PERFECTION: Standard pattern users expect
 * - ANTICIPATORY INTELLIGENCE: Shows relevant insights
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenGlass(
    statsViewModel: ListeningStatsViewModel = hiltViewModel(),
    isDark: Boolean = true,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.Interactive,
    onSettingsClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onJourneyClick: () -> Unit = {},
    onAchievementsClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onCloudClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {}
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current
    val scrollState = rememberScrollState()

    // Stats from ViewModel
    val totalListeningTime by statsViewModel.totalListeningTime.collectAsState()
    val currentStreak by statsViewModel.currentStreak.collectAsState()
    val booksFinished by statsViewModel.booksFinished.collectAsState()
    val todayListeningTime by statsViewModel.todayListeningTime.collectAsState()

    // Daily goal from settings
    val dailyGoalMinutes by statsViewModel.dailyGoalMinutes.collectAsState()
    val todayMinutes = (todayListeningTime / 60000).toInt()
    val goalProgress = (todayMinutes.toFloat() / dailyGoalMinutes).coerceIn(0f, 1f)

    // Goal setting dialog state
    var showGoalDialog by remember { mutableStateOf(false) }

    // Animated goal progress
    val animatedProgress by animateFloatAsState(
        targetValue = goalProgress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "goalProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp) // Space for nav bar
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Header
            Text(
                text = "Profile",
                style = GlassTypography.Display.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = theme.textPrimary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ══════════════════════════════════════════════════════════════
            // TODAY'S PROGRESS - Compact ring with stats
            // ══════════════════════════════════════════════════════════════
            ProfileQuickStatsCard(
                todayMinutes = todayMinutes,
                dailyGoalMinutes = dailyGoalMinutes,
                currentStreak = currentStreak,
                booksFinished = booksFinished,
                animatedProgress = animatedProgress,
                isDark = isDark,
                accentColor = accentColor,
                onClick = onJourneyClick
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ══════════════════════════════════════════════════════════════
            // QUICK ACCESS MENU
            // ══════════════════════════════════════════════════════════════
            Text(
                text = "Quick Access",
                style = GlassTypography.Caption.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                ),
                color = theme.textSecondary.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Menu items
            ProfileMenuItem(
                icon = Icons.Outlined.TrendingUp,
                title = "Listening Stats",
                subtitle = "Your detailed listening history",
                onClick = {
                    view.performHaptic(HapticType.LightTap)
                    onStatsClick()
                },
                isDark = isDark,
                accentColor = accentColor
            )

            ProfileMenuItem(
                icon = Icons.Outlined.EmojiEvents,
                title = "Achievements",
                subtitle = "Badges and milestones",
                onClick = {
                    view.performHaptic(HapticType.LightTap)
                    onAchievementsClick()
                },
                isDark = isDark,
                accentColor = accentColor
            )

            ProfileMenuItem(
                icon = Icons.Outlined.Flag,
                title = "Set Daily Goal",
                subtitle = "Current: ${dailyGoalMinutes} minutes",
                onClick = {
                    view.performHaptic(HapticType.LightTap)
                    showGoalDialog = true
                },
                isDark = isDark,
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "App",
                style = GlassTypography.Caption.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                ),
                color = theme.textSecondary.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ProfileMenuItem(
                icon = Icons.Outlined.Settings,
                title = "Settings",
                subtitle = "Theme, playback, storage",
                onClick = {
                    view.performHaptic(HapticType.LightTap)
                    onSettingsClick()
                },
                isDark = isDark,
                accentColor = accentColor
            )

            ProfileMenuItem(
                icon = Icons.Outlined.Equalizer,
                title = "Equalizer",
                subtitle = "Audio enhancement",
                onClick = {
                    view.performHaptic(HapticType.LightTap)
                    onEqualizerClick()
                },
                isDark = isDark,
                accentColor = accentColor
            )

            ProfileMenuItem(
                icon = Icons.Outlined.Download,
                title = "Downloads",
                subtitle = "Active downloads and torrents",
                onClick = {
                    view.performHaptic(HapticType.LightTap)
                    onDownloadsClick()
                },
                isDark = isDark,
                accentColor = accentColor
            )

            ProfileMenuItem(
                icon = Icons.Outlined.Cloud,
                title = "Cloud",
                subtitle = "Google Drive & Dropbox sync",
                onClick = {
                    view.performHaptic(HapticType.LightTap)
                    onCloudClick()
                },
                isDark = isDark,
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App info
            Text(
                text = "Reverie by Mossglen",
                style = GlassTypography.Caption.copy(fontSize = 12.sp),
                color = theme.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Goal Setting Dialog
        if (showGoalDialog) {
            GoalSettingDialog(
                currentGoal = dailyGoalMinutes,
                onDismiss = { showGoalDialog = false },
                onConfirm = { newGoal ->
                    statsViewModel.setDailyGoal(newGoal)
                    showGoalDialog = false
                    view.performHaptic(HapticType.Confirm)
                },
                isDark = isDark,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun GoalSettingDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    isDark: Boolean,
    accentColor: Color
) {
    val theme = glassTheme(isDark)
    var selectedGoal by remember { mutableIntStateOf(currentGoal) }

    // Preset goal options in minutes
    val goalOptions = listOf(15, 30, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
        title = {
            Text(
                "Set Daily Goal",
                style = GlassTypography.Title.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = theme.textPrimary
            )
        },
        text = {
            Column {
                Text(
                    "How many minutes do you want to listen each day?",
                    style = GlassTypography.Body.copy(fontSize = 14.sp),
                    color = theme.textSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Goal options as chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    goalOptions.forEach { goal ->
                        val isSelected = selectedGoal == goal
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGoal = goal },
                            label = {
                                Text(
                                    "${goal} min",
                                    style = GlassTypography.Label.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current selection display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedGoal / 60}h ${selectedGoal % 60}m per day",
                        style = GlassTypography.Body.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = accentColor
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedGoal) }) {
                Text(
                    "Save",
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = theme.textSecondary
                )
            }
        }
    )
}

@Composable
private fun ProfileQuickStatsCard(
    todayMinutes: Int,
    dailyGoalMinutes: Int,
    currentStreak: Int,
    booksFinished: Int,
    animatedProgress: Float,
    isDark: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val theme = glassTheme(isDark)
    val view = LocalView.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isDark) Color.White.copy(alpha = 0.05f)
                else Color.Black.copy(alpha = 0.03f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                view.performHaptic(HapticType.LightTap)
                onClick()
            }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact goal ring
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 6.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2

                    // Background ring
                    drawCircle(
                        color = if (isDark) Color.White.copy(alpha = 0.1f)
                               else Color.Black.copy(alpha = 0.08f),
                        radius = radius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        topLeft = androidx.compose.ui.geometry.Offset(
                            (size.width - radius * 2) / 2,
                            (size.height - radius * 2) / 2
                        )
                    )
                }

                // Percentage text
                val percentage = (animatedProgress * 100).toInt()
                Text(
                    text = "$percentage%",
                    style = GlassTypography.Caption.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = theme.textPrimary
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Stats column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today's Goal",
                    style = GlassTypography.Caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = theme.textSecondary.copy(alpha = 0.7f)
                )

                Text(
                    text = "${todayMinutes}/${dailyGoalMinutes} min",
                    style = GlassTypography.Title.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = theme.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    // Streak
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$currentStreak day${if (currentStreak != 1) "s" else ""}",
                            style = GlassTypography.Caption.copy(fontSize = 12.sp),
                            color = theme.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Books finished
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$booksFinished finished",
                            style = GlassTypography.Caption.copy(fontSize = 12.sp),
                            color = theme.textSecondary
                        )
                    }
                }
            }

            // Chevron
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "View Journey",
                tint = theme.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDark: Boolean,
    accentColor: Color = GlassColors.Interactive
) {
    val theme = glassTheme(isDark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container
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
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = theme.textSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}
