package com.mossglen.lithos.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.mossglen.lithos.data.Achievement
import com.mossglen.lithos.data.AchievementCategory
import com.mossglen.lithos.data.ListeningStats
import com.mossglen.lithos.ui.theme.*
import com.mossglen.lithos.ui.viewmodel.ListeningStatsViewModel
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
    isOLED: Boolean = false,
    accentColor: Color = LithosAmber,
    onBack: () -> Unit,
    viewModel: ListeningStatsViewModel = hiltViewModel()
) {
    val theme = glassTheme(isDark, isOLED)
    val stats by viewModel.stats.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val newlyUnlocked by viewModel.newlyUnlockedAchievements.collectAsState()

    // Theme-aware background color
    val backgroundColor = LithosUI.background(isDark, isOLED)

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
            containerColor = backgroundColor
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
                    isDark = isDark,
                    isOLED = isOLED
                )

                Spacer(modifier = Modifier.height(GlassSpacing.L))

                // Streak Card
                StreakCard(
                    currentStreak = stats.streakDays,
                    longestStreak = stats.longestStreak,
                    accentColor = accentColor,
                    isDark = isDark,
                    isOLED = isOLED
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
                        isOLED = isOLED,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Rounded.DateRange,
                        title = "This Week",
                        value = stats.weekTimeFormatted,
                        accentColor = accentColor,
                        isDark = isDark,
                        isOLED = isOLED,
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
                        isOLED = isOLED,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Rounded.Headphones,
                        title = "Sessions",
                        value = "${stats.sessionsCount}",
                        subtitle = "total",
                        accentColor = accentColor,
                        isDark = isDark,
                        isOLED = isOLED,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(GlassSpacing.XXL))

                // Achievements Section
                AchievementsSection(
                    achievements = achievements,
                    accentColor = accentColor,
                    isDark = isDark,
                    isOLED = isOLED
                )

                Spacer(modifier = Modifier.height(GlassSpacing.XXL))

                // Motivational message
                MotivationalMessage(
                    stats = stats,
                    accentColor = accentColor,
                    isDark = isDark,
                    isOLED = isOLED
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
                isOLED = isOLED,
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
    isDark: Boolean,
    isOLED: Boolean = false
) {
    val theme = glassTheme(isDark, isOLED)

    // Theme-aware card background with visible border
    val cardBackground = if (isDark) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Large))
            .background(cardBackground)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(GlassShapes.Large)
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
    isDark: Boolean,
    isOLED: Boolean = false
) {
    val theme = glassTheme(isDark, isOLED)

    // Theme-aware card background with visible border
    val cardBackground = if (isDark) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    // Inactive streak icon background - theme aware
    val inactiveBackground = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    // Subtle pulse animation for active streaks - premium feel
    val infiniteTransition = rememberInfiniteTransition(label = "streak_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(cardBackground)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(GlassShapes.Medium)
            )
            .padding(GlassSpacing.L),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Streak icon - matte finish, no emoji
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (currentStreak > 0) accentColor.copy(alpha = pulseAlpha)
                    else inactiveBackground
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (currentStreak > 0) Icons.Rounded.Whatshot else Icons.Rounded.Schedule,
                contentDescription = null,
                tint = if (currentStreak > 0) accentColor else theme.textTertiary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(GlassSpacing.M))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (currentStreak > 0) "$currentStreak Day Streak" else "Start Your Streak",
                style = GlassTypography.Headline,
                fontWeight = FontWeight.SemiBold,
                color = if (currentStreak > 0) accentColor else theme.textPrimary
            )
            Text(
                text = if (currentStreak > 0) {
                    "Keep listening daily to maintain momentum"
                } else {
                    "Listen today to begin your journey"
                },
                style = GlassTypography.Caption,
                color = theme.textSecondary
            )
        }

        if (longestStreak > 0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "BEST",
                    style = GlassTypography.Caption.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    ),
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
    isOLED: Boolean = false,
    modifier: Modifier = Modifier
) {
    val theme = glassTheme(isDark, isOLED)

    // Theme-aware card background with visible border
    val cardBackground = if (isDark) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(cardBackground)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(GlassShapes.Medium)
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
    isDark: Boolean,
    isOLED: Boolean = false
) {
    val theme = glassTheme(isDark, isOLED)

    // Premium motivational messages - no emojis, sophisticated tone
    val message = when {
        stats.streakDays >= 30 -> "Legendary consistency. A month of dedication."
        stats.streakDays >= 14 -> "Two weeks of excellence. Unstoppable momentum."
        stats.streakDays >= 7 -> "A full week of commitment. Outstanding discipline."
        stats.streakDays >= 3 -> "Building momentum. Three days and counting."
        stats.booksCompleted >= 10 -> "A voracious reader. Ten books conquered."
        stats.booksCompleted >= 5 -> "Five books complete. Impressive progress."
        stats.totalTimeMs >= 36_000_000L -> "Ten hours invested. Truly dedicated."
        stats.totalTimeMs >= 3_600_000L -> "Your journey has begun. Keep going."
        stats.sessionsCount > 0 -> "Every session counts. Well begun."
        else -> "Begin your listening journey today."
    }

    // Theme-aware motivational message background
    val messageBackground = accentColor.copy(alpha = if (isDark) 0.12f else 0.10f)
    val borderColor = accentColor.copy(alpha = if (isDark) 0.25f else 0.20f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(messageBackground)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(GlassShapes.Medium)
            )
            .padding(horizontal = GlassSpacing.L, vertical = GlassSpacing.M),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = GlassTypography.Body.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.25.sp
            ),
            color = accentColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AchievementsSection(
    achievements: List<Achievement>,
    accentColor: Color,
    isDark: Boolean,
    isOLED: Boolean = false
) {
    val theme = glassTheme(isDark, isOLED)

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
                    isDark = isDark,
                    isOLED = isOLED
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
    isDark: Boolean,
    isOLED: Boolean = false
) {
    val theme = glassTheme(isDark, isOLED)

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
                    isDark = isDark,
                    isOLED = isOLED
                )
            }
        }
    }
}

@Composable
private fun AchievementBadge(
    achievement: Achievement,
    accentColor: Color,
    isDark: Boolean,
    isOLED: Boolean = false
) {
    val theme = glassTheme(isDark, isOLED)

    // Theme-aware card background with visible border
    val cardBackground = if (isDark) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    // Unlocked state uses accent color
    val backgroundColor = if (achievement.isUnlocked) {
        accentColor.copy(alpha = if (isDark) 0.15f else 0.12f)
    } else {
        cardBackground
    }

    val actualBorderColor = if (achievement.isUnlocked) {
        accentColor.copy(alpha = if (isDark) 0.30f else 0.25f)
    } else {
        borderColor
    }

    val iconColor = if (achievement.isUnlocked) {
        accentColor
    } else {
        theme.textTertiary
    }

    // Icon background - theme aware
    val iconBackground = if (achievement.isUnlocked) {
        accentColor.copy(alpha = 0.2f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassShapes.Medium))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = actualBorderColor,
                shape = RoundedCornerShape(GlassShapes.Medium)
            )
            .padding(GlassSpacing.M),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconBackground),
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
    isOLED: Boolean = false,
    onDismiss: () -> Unit
) {
    val theme = glassTheme(isDark, isOLED)

    // Theme-aware dialog background
    val dialogBackground = LithosUI.sheetBackground(isDark, isOLED)
    val borderColor = if (isDark) {
        accentColor.copy(alpha = 0.25f)
    } else {
        accentColor.copy(alpha = 0.20f)
    }

    LaunchedEffect(achievement) {
        kotlinx.coroutines.delay(4000)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GlassShapes.Large))
                .background(dialogBackground)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(GlassShapes.Large)
                )
                .padding(GlassSpacing.XL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated icon - matte finish, no glow
            val infiniteTransition = rememberInfiniteTransition(label = "achievement_scale")
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
