package com.mossglen.reverie.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.mossglen.reverie.data.Achievement
import com.mossglen.reverie.data.AchievementCategory
import com.mossglen.reverie.data.ListeningStats
import com.mossglen.reverie.ui.theme.*
import com.mossglen.reverie.ui.viewmodel.ListeningStatsViewModel
import java.time.format.DateTimeFormatter

/**
 * Listening Stats Screen
 *
 * Displays user's listening statistics with beautiful visualizations.
 * Gamification elements to encourage continued listening.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningStatsScreen(
    isDark: Boolean = true,
    isReverieDark: Boolean = false,
    accentColor: Color = GlassColors.ReverieAccent,
    onBack: () -> Unit,
    viewModel: ListeningStatsViewModel = hiltViewModel()
) {
    val theme = glassTheme(isDark, isReverieDark)
    val stats by viewModel.stats.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val newlyUnlocked by viewModel.newlyUnlockedAchievements.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Listening Stats",
                            style = GlassTypography.Title,
                            color = theme.textPrimary
                        )
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

                // Hero Stat - Total Time
                HeroStatCard(
                    title = "Total Listening Time",
                    value = stats.totalTimeFormatted,
                    subtitle = "${stats.sessionsCount} sessions",
                    accentColor = accentColor,
                    isDark = isDark
                )

                Spacer(modifier = Modifier.height(GlassSpacing.L))

                // Streak Card
                StreakCard(
                    currentStreak = stats.streakDays,
                    longestStreak = stats.longestStreak,
                    accentColor = accentColor,
                    isDark = isDark
                )

                Spacer(modifier = Modifier.height(GlassSpacing.L))

                // Time Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlassSpacing.M)
                ) {
                    StatCard(
                        icon = Icons.Rounded.Today,
                        title = "Today",
                        value = stats.todayTimeFormatted,
                        accentColor = accentColor,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Rounded.DateRange,
                        title = "This Week",
                        value = stats.weekTimeFormatted,
                        accentColor = accentColor,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(GlassSpacing.M))

                // Books & Sessions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlassSpacing.M)
                ) {
                    StatCard(
                        icon = Icons.Rounded.CheckCircle,
                        title = "Completed",
                        value = "${stats.booksCompleted}",
                        subtitle = "books",
                        accentColor = accentColor,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Rounded.Headphones,
                        title = "Sessions",
                        value = "${stats.sessionsCount}",
                        subtitle = "total",
                        accentColor = accentColor,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(GlassSpacing.XXL))

                // Achievements Section
                AchievementsSection(
                    achievements = achievements,
                    accentColor = accentColor,
                    isDark = isDark
                )

                Spacer(modifier = Modifier.height(GlassSpacing.XXL))

                // Motivational message
                MotivationalMessage(
                    stats = stats,
                    accentColor = accentColor,
                    isDark = isDark
                )

                Spacer(modifier = Modifier.height(GlassSpacing.XXL))
            }
        }

        // Achievement unlock toast/notification
        if (newlyUnlocked.isNotEmpty()) {
            AchievementUnlockedToast(
                achievement = newlyUnlocked.first(),
                accentColor = accentColor,
                isDark = isDark,
                onDismiss = { viewModel.dismissNewlyUnlockedAchievements() }
            )
        }
    }
}

@Composable
private fun HeroStatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    isDark: Boolean
) {
    val theme = glassTheme(isDark)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Large))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.15f),
                        accentColor.copy(alpha = 0.05f)
                    )
                )
            )
            .padding(GlassSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = GlassTypography.Caption,
            color = theme.textSecondary
        )

        Spacer(modifier = Modifier.height(GlassSpacing.S))

        Text(
            text = value,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )

        Spacer(modifier = Modifier.height(GlassSpacing.XS))

        Text(
            text = subtitle,
            style = GlassTypography.Body,
            color = theme.textSecondary
        )
    }
}

@Composable
private fun StreakCard(
    currentStreak: Int,
    longestStreak: Int,
    accentColor: Color,
    isDark: Boolean
) {
    val theme = glassTheme(isDark)

    // Animated flame for active streaks
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(
                if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.8f)
                else Color(0xFFF2F2F7).copy(alpha = 0.8f)
            )
            .padding(GlassSpacing.L),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Streak icon with animation
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (currentStreak > 0) accentColor.copy(alpha = 0.2f)
                    else theme.glassSecondary
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (currentStreak > 0) "🔥" else "❄️",
                fontSize = if (currentStreak > 0) (24 * flameScale).sp else 24.sp
            )
        }

        Spacer(modifier = Modifier.width(GlassSpacing.M))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (currentStreak > 0) "$currentStreak day streak!" else "Start a streak!",
                style = GlassTypography.Headline,
                fontWeight = FontWeight.SemiBold,
                color = if (currentStreak > 0) accentColor else theme.textPrimary
            )
            Text(
                text = if (currentStreak > 0) {
                    "Keep listening daily to grow your streak"
                } else {
                    "Listen today to start your streak"
                },
                style = GlassTypography.Caption,
                color = theme.textSecondary
            )
        }

        if (longestStreak > 0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Best",
                    style = GlassTypography.Caption,
                    color = theme.textTertiary
                )
                Text(
                    text = "$longestStreak",
                    style = GlassTypography.Title,
                    fontWeight = FontWeight.Bold,
                    color = theme.textSecondary
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String? = null,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(
                if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.8f)
                else Color(0xFFF2F2F7).copy(alpha = 0.8f)
            )
            .padding(GlassSpacing.M),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.height(GlassSpacing.S))

        Text(
            text = title,
            style = GlassTypography.Caption,
            color = theme.textSecondary
        )

        Spacer(modifier = Modifier.height(GlassSpacing.XS))

        Text(
            text = value,
            style = GlassTypography.Title,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = GlassTypography.Caption,
                color = theme.textTertiary
            )
        }
    }
}

@Composable
private fun MotivationalMessage(
    stats: ListeningStats,
    accentColor: Color,
    isDark: Boolean
) {
    val theme = glassTheme(isDark)

    val message = when {
        stats.streakDays >= 30 -> "🏆 Legendary! A month-long streak!"
        stats.streakDays >= 14 -> "🌟 Two weeks strong! You're unstoppable!"
        stats.streakDays >= 7 -> "💪 A full week! Great consistency!"
        stats.streakDays >= 3 -> "🔥 3+ days! Keep the momentum!"
        stats.booksCompleted >= 10 -> "📚 10+ books completed! Voracious reader!"
        stats.booksCompleted >= 5 -> "📖 5 books down! Well done!"
        stats.totalTimeMs >= 36_000_000L -> "⏱️ 10+ hours of listening! Dedicated!"
        stats.totalTimeMs >= 3_600_000L -> "🎧 Over an hour logged!"
        stats.sessionsCount > 0 -> "👋 Great start! Keep listening!"
        else -> "🎯 Start listening to track your progress!"
    }

    Text(
        text = message,
        style = GlassTypography.Body,
        color = theme.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = GlassSpacing.L)
    )
}

@Composable
private fun AchievementsSection(
    achievements: List<Achievement>,
    accentColor: Color,
    isDark: Boolean
) {
    val theme = glassTheme(isDark)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = GlassSpacing.M),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Achievements",
                style = GlassTypography.Title,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary
            )

            val unlockedCount = achievements.count { it.isUnlocked }
            Text(
                text = "$unlockedCount/${achievements.size}",
                style = GlassTypography.Body,
                color = theme.textSecondary
            )
        }

        // Group achievements by category
        AchievementCategory.entries.forEach { category ->
            val categoryAchievements = achievements.filter { it.category == category }
            if (categoryAchievements.isNotEmpty()) {
                AchievementCategorySection(
                    category = category,
                    achievements = categoryAchievements,
                    accentColor = accentColor,
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(GlassSpacing.M))
            }
        }
    }
}

@Composable
private fun AchievementCategorySection(
    category: AchievementCategory,
    achievements: List<Achievement>,
    accentColor: Color,
    isDark: Boolean
) {
    val theme = glassTheme(isDark)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Category name
        Text(
            text = category.name.lowercase().replaceFirstChar { it.uppercase() },
            style = GlassTypography.Callout,
            color = theme.textSecondary,
            modifier = Modifier.padding(bottom = GlassSpacing.S)
        )

        // Achievement badges in a grid
        Column(
            verticalArrangement = Arrangement.spacedBy(GlassSpacing.S)
        ) {
            achievements.forEach { achievement ->
                AchievementBadge(
                    achievement = achievement,
                    accentColor = accentColor,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
private fun AchievementBadge(
    achievement: Achievement,
    accentColor: Color,
    isDark: Boolean
) {
    val theme = glassTheme(isDark)
    val backgroundColor = if (achievement.isUnlocked) {
        accentColor.copy(alpha = 0.15f)
    } else {
        if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.5f)
        else Color(0xFFF2F2F7).copy(alpha = 0.5f)
    }

    val iconColor = if (achievement.isUnlocked) {
        accentColor
    } else {
        theme.textTertiary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(backgroundColor)
            .padding(GlassSpacing.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (achievement.isUnlocked) accentColor.copy(alpha = 0.2f)
                    else theme.glassSecondary
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = achievement.icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(GlassSpacing.M))

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = achievement.title,
                style = GlassTypography.Headline,
                fontWeight = FontWeight.SemiBold,
                color = if (achievement.isUnlocked) theme.textPrimary else theme.textSecondary
            )

            Text(
                text = achievement.description,
                style = GlassTypography.Caption,
                color = theme.textTertiary
            )

            // Show unlock date for earned achievements
            if (achievement.isUnlocked && achievement.unlockedAt != null) {
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                Text(
                    text = "Unlocked ${achievement.unlockedAt.format(formatter)}",
                    style = GlassTypography.Caption,
                    color = accentColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = GlassSpacing.XXS)
                )
            }
        }

        // Unlock indicator
        if (achievement.isUnlocked) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Unlocked",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Locked",
                tint = theme.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AchievementUnlockedToast(
    achievement: Achievement,
    accentColor: Color,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark)

    LaunchedEffect(achievement) {
        kotlinx.coroutines.delay(4000)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GlassShapes.Large))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.3f),
                            accentColor.copy(alpha = 0.15f)
                        )
                    )
                )
                .padding(GlassSpacing.XL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated icon
            val infiniteTransition = rememberInfiniteTransition(label = "achievement_glow")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size((64 * scale).dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = achievement.icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size((32 * scale).dp)
                )
            }

            Spacer(modifier = Modifier.height(GlassSpacing.M))

            Text(
                text = "Achievement Unlocked!",
                style = GlassTypography.Caption,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(GlassSpacing.XS))

            Text(
                text = achievement.title,
                style = GlassTypography.Title,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(GlassSpacing.S))

            Text(
                text = achievement.description,
                style = GlassTypography.Body,
                color = theme.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(GlassSpacing.L))

            TextButton(onClick = onDismiss) {
                Text(
                    text = "Awesome!",
                    style = GlassTypography.Label,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
