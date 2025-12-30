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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

/**
 * REVERIE Glass - Journey Screen
 *
 * Your listening journey visualized. Progress, goals, achievements.
 * Not buried in Settings - this is a first-class destination.
 *
 * Design Philosophy (Per PROJECT_MANIFEST):
 * - ANTICIPATORY INTELLIGENCE: Shows relevant stats and insights
 * - PREMIUM BY DEFAULT: Animated rings, clean typography
 * - SENSORY HARMONY: Satisfying progress visualizations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreenGlass(
    statsViewModel: ListeningStatsViewModel = hiltViewModel(),
    isDark: Boolean = true,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.Interactive,
    onSettingsClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onAchievementsClick: () -> Unit = {}
) {
    val theme = glassTheme(isDark, isReverieDark)
    val view = LocalView.current
    val scrollState = rememberScrollState()

    // Stats from ViewModel
    val totalListeningTime by statsViewModel.totalListeningTime.collectAsState()
    val currentStreak by statsViewModel.currentStreak.collectAsState()
    val booksFinished by statsViewModel.booksFinished.collectAsState()
    val todayListeningTime by statsViewModel.todayListeningTime.collectAsState()
    val weeklyProgress by statsViewModel.weeklyProgress.collectAsState()

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
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Header
            Text(
                text = "Your Journey",
                style = GlassTypography.Display.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = theme.textPrimary
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ══════════════════════════════════════════════════════════════
            // DAILY GOAL RING - Animated, tappable to set goal
            // ══════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                // Goal ring - tappable to set goal
                val ringSize = 160.dp
                Box(
                    modifier = Modifier
                        .size(ringSize)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            view.performHaptic(HapticType.LightTap)
                            showGoalDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Background ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 12.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        drawCircle(
                            color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
                            radius = radius,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Progress arc
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 12.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val sweepAngle = animatedProgress * 360f

                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Center text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$todayMinutes",
                            style = GlassTypography.Display.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = theme.textPrimary
                        )
                        Text(
                            text = "of $dailyGoalMinutes min",
                            style = GlassTypography.Caption.copy(
                                fontSize = 13.sp
                            ),
                            color = theme.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Daily goal label - tappable hint
            Text(
                text = "Daily Goal • Tap to change",
                style = GlassTypography.Body.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = theme.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        view.performHaptic(HapticType.LightTap)
                        showGoalDialog = true
                    },
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ══════════════════════════════════════════════════════════════
            // WEEKLY VIEW - Progress bars for each day
            // ══════════════════════════════════════════════════════════════
            Text(
                text = "This Week",
                style = GlassTypography.Title.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = theme.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val today = LocalDate.now()
                val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)

                for (i in 0..6) {
                    val day = startOfWeek.plusDays(i.toLong())
                    val dayName = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    val isToday = day == today
                    val dayProgress = weeklyProgress.getOrElse(i) { 0f }

                    DayProgressBar(
                        dayName = dayName.take(1),
                        progress = dayProgress,
                        isToday = isToday,
                        accentColor = accentColor,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ══════════════════════════════════════════════════════════════
            // STATS CARDS - Streak and Books Finished
            // ══════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Streak card
                StatCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    value = "$currentStreak",
                    label = "Day Streak",
                    accentColor = Color(0xFFFF6B35), // Fire orange
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                // Books finished card
                StatCard(
                    icon = Icons.Filled.MenuBook,
                    value = "$booksFinished",
                    label = "Books Finished",
                    accentColor = accentColor,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ══════════════════════════════════════════════════════════════
            // INSIGHTS - Smart observations
            // ══════════════════════════════════════════════════════════════
            Text(
                text = "Insights",
                style = GlassTypography.Title.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = theme.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            InsightCard(
                text = "You've listened for ${formatDuration(totalListeningTime)} total",
                icon = Icons.Outlined.Timer,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            InsightCard(
                text = "Average session: ${if (todayMinutes > 0) "${todayMinutes / 2} minutes" else "Start listening!"}",
                icon = Icons.Outlined.Insights,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ══════════════════════════════════════════════════════════════
            // QUICK LINKS - Settings, Stats, Achievements
            // ══════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickLinkButton(
                    icon = Icons.Outlined.Settings,
                    label = "Settings",
                    onClick = {
                        view.performHaptic(HapticType.LightTap)
                        onSettingsClick()
                    },
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                QuickLinkButton(
                    icon = Icons.Outlined.BarChart,
                    label = "Full Stats",
                    onClick = {
                        view.performHaptic(HapticType.LightTap)
                        onStatsClick()
                    },
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                QuickLinkButton(
                    icon = Icons.Outlined.EmojiEvents,
                    label = "Badges",
                    onClick = {
                        view.performHaptic(HapticType.LightTap)
                        onAchievementsClick()
                    },
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }

            // Bottom padding for nav bar
            Spacer(modifier = Modifier.height(120.dp))
        }

        // Goal Setting Dialog
        if (showGoalDialog) {
            GoalSettingDialog(
                currentGoal = dailyGoalMinutes,
                onDismiss = { showGoalDialog = false },
                onConfirm = { newGoal ->
                    statsViewModel.setDailyGoal(newGoal)
                    showGoalDialog = false
                },
                isDark = isDark,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun DayProgressBar(
    dayName: String,
    progress: Float,
    isToday: Boolean,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress bar (vertical)
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isToday) accentColor else accentColor.copy(alpha = 0.6f))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day label
        Text(
            text = dayName,
            style = GlassTypography.Caption.copy(
                fontSize = 11.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isToday) accentColor else theme.textSecondary
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f))
            .padding(20.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = GlassTypography.Display.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = theme.textPrimary
            )

            Text(
                text = label,
                style = GlassTypography.Caption.copy(
                    fontSize = 13.sp
                ),
                color = theme.textSecondary
            )
        }
    }
}

@Composable
private fun InsightCard(
    text: String,
    icon: ImageVector,
    isDark: Boolean
) {
    val theme = glassTheme(isDark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = theme.textSecondary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = GlassTypography.Body.copy(
                fontSize = 14.sp
            ),
            color = theme.textPrimary
        )
    }
}

@Composable
private fun QuickLinkButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = theme.textSecondary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = GlassTypography.Caption.copy(
                fontSize = 12.sp
            ),
            color = theme.textSecondary
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalMinutes = ms / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

/**
 * Goal Setting Dialog - Select daily listening goal
 */
@Composable
private fun GoalSettingDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    isDark: Boolean,
    accentColor: Color
) {
    val theme = glassTheme(isDark)
    val view = LocalView.current

    // Preset goal options in minutes
    val goalOptions = listOf(15, 30, 45, 60, 90, 120)
    var selectedGoal by remember { mutableIntStateOf(currentGoal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.glassCard,
        titleContentColor = theme.textPrimary,
        textContentColor = theme.textSecondary,
        title = {
            Text(
                text = "Set Daily Goal",
                style = GlassTypography.Title.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "How many minutes do you want to listen each day?",
                    style = GlassTypography.Body.copy(fontSize = 14.sp),
                    color = theme.textSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Goal options grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    goalOptions.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { minutes ->
                                val isSelected = selectedGoal == minutes
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) accentColor.copy(alpha = 0.2f)
                                            else if (isDark) Color.White.copy(alpha = 0.05f)
                                            else Color.Black.copy(alpha = 0.05f)
                                        )
                                        .clickable {
                                            view.performHaptic(HapticType.LightTap)
                                            selectedGoal = minutes
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (minutes >= 60) "${minutes / 60}h" else "${minutes}m",
                                            style = GlassTypography.Body.copy(
                                                fontSize = 16.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) accentColor else theme.textPrimary
                                        )
                                        if (minutes >= 60 && minutes % 60 != 0) {
                                            Text(
                                                text = "${minutes % 60}m",
                                                style = GlassTypography.Caption.copy(fontSize = 11.sp),
                                                color = theme.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    view.performHaptic(HapticType.MediumTap)
                    onConfirm(selectedGoal)
                }
            ) {
                Text(
                    text = "Set Goal",
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = theme.textSecondary
                )
            }
        }
    )
}
