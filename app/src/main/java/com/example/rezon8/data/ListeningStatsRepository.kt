package com.mossglen.reverie.data

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val Context.statsDataStore by preferencesDataStore(name = "listening_stats")

/**
 * Achievement categories for organizing badges
 */
enum class AchievementCategory {
    STREAK,
    TIME,
    BOOKS,
    SESSIONS
}

/**
 * Achievement data class
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isUnlocked: Boolean = false,
    val unlockedAt: LocalDateTime? = null,
    val category: AchievementCategory,
    val requirement: Long // Generic requirement value (days, ms, count, etc.)
)

/**
 * Tracks listening statistics for gamification and user engagement.
 * - Total listening time (all-time)
 * - Daily listening time
 * - Listening streak (consecutive days)
 * - Books completed
 * - Sessions count
 * - Achievements and badges
 */
@Singleton
class ListeningStatsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TOTAL_TIME_MS = longPreferencesKey("total_time_ms")
        val TODAY_TIME_MS = longPreferencesKey("today_time_ms")
        val CURRENT_DATE = stringPreferencesKey("current_date")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val LONGEST_STREAK = intPreferencesKey("longest_streak")
        val LAST_LISTENED_DATE = stringPreferencesKey("last_listened_date")
        val BOOKS_COMPLETED = intPreferencesKey("books_completed")
        val SESSIONS_COUNT = intPreferencesKey("sessions_count")
        val WEEK_TIME_MS = longPreferencesKey("week_time_ms")
        val WEEK_START_DATE = stringPreferencesKey("week_start_date")
        val UNLOCKED_ACHIEVEMENTS = stringPreferencesKey("unlocked_achievements")
    }

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    // ========================================================================
    // ACHIEVEMENT DEFINITIONS
    // ========================================================================

    private fun getAllAchievements(): List<Achievement> = listOf(
        // STREAK achievements
        Achievement(
            id = "streak_week",
            title = "Week Warrior",
            description = "Maintain a 7-day listening streak",
            icon = Icons.Rounded.LocalFireDepartment,
            category = AchievementCategory.STREAK,
            requirement = 7
        ),
        Achievement(
            id = "streak_month",
            title = "Month Master",
            description = "Maintain a 30-day listening streak",
            icon = Icons.Rounded.Whatshot,
            category = AchievementCategory.STREAK,
            requirement = 30
        ),

        // TIME achievements
        Achievement(
            id = "time_first_hour",
            title = "First Hour",
            description = "Listen for 1 hour total",
            icon = Icons.Rounded.Timer,
            category = AchievementCategory.TIME,
            requirement = 3_600_000L // 1 hour in ms
        ),
        Achievement(
            id = "time_10_hours",
            title = "10 Hour Club",
            description = "Listen for 10 hours total",
            icon = Icons.Rounded.Schedule,
            category = AchievementCategory.TIME,
            requirement = 36_000_000L // 10 hours in ms
        ),
        Achievement(
            id = "time_100_hours",
            title = "100 Hour Legend",
            description = "Listen for 100 hours total",
            icon = Icons.Rounded.Stars,
            category = AchievementCategory.TIME,
            requirement = 360_000_000L // 100 hours in ms
        ),

        // BOOKS achievements
        Achievement(
            id = "books_first",
            title = "First Finish",
            description = "Complete your first audiobook",
            icon = Icons.Rounded.CheckCircle,
            category = AchievementCategory.BOOKS,
            requirement = 1
        ),
        Achievement(
            id = "books_bookworm",
            title = "Bookworm",
            description = "Complete 5 audiobooks",
            icon = Icons.Rounded.Book,
            category = AchievementCategory.BOOKS,
            requirement = 5
        ),
        Achievement(
            id = "books_legend",
            title = "Library Legend",
            description = "Complete 25 audiobooks",
            icon = Icons.Rounded.LocalLibrary,
            category = AchievementCategory.BOOKS,
            requirement = 25
        ),

        // SESSIONS achievements
        Achievement(
            id = "sessions_first",
            title = "Getting Started",
            description = "Start your first listening session",
            icon = Icons.Rounded.PlayCircle,
            category = AchievementCategory.SESSIONS,
            requirement = 1
        ),
        Achievement(
            id = "sessions_dedicated",
            title = "Dedicated Listener",
            description = "Complete 50 listening sessions",
            icon = Icons.Rounded.Headphones,
            category = AchievementCategory.SESSIONS,
            requirement = 50
        )
    )

    // ========================================================================
    // FLOWS - Observable stats
    // ========================================================================

    val totalTimeMs: Flow<Long> = context.statsDataStore.data.map { it[Keys.TOTAL_TIME_MS] ?: 0L }
    val todayTimeMs: Flow<Long> = context.statsDataStore.data.map { prefs ->
        val storedDate = prefs[Keys.CURRENT_DATE] ?: ""
        val today = LocalDate.now().format(dateFormatter)
        if (storedDate == today) prefs[Keys.TODAY_TIME_MS] ?: 0L else 0L
    }
    val weekTimeMs: Flow<Long> = context.statsDataStore.data.map { prefs ->
        val storedWeekStart = prefs[Keys.WEEK_START_DATE] ?: ""
        val currentWeekStart = getWeekStartDate()
        if (storedWeekStart == currentWeekStart) prefs[Keys.WEEK_TIME_MS] ?: 0L else 0L
    }
    val streakDays: Flow<Int> = context.statsDataStore.data.map { it[Keys.STREAK_DAYS] ?: 0 }
    val longestStreak: Flow<Int> = context.statsDataStore.data.map { it[Keys.LONGEST_STREAK] ?: 0 }
    val booksCompleted: Flow<Int> = context.statsDataStore.data.map { it[Keys.BOOKS_COMPLETED] ?: 0 }
    val sessionsCount: Flow<Int> = context.statsDataStore.data.map { it[Keys.SESSIONS_COUNT] ?: 0 }

    val achievements: Flow<List<Achievement>> = context.statsDataStore.data.map { prefs ->
        val unlockedData = prefs[Keys.UNLOCKED_ACHIEVEMENTS] ?: ""
        val unlockedMap = parseUnlockedAchievements(unlockedData)
        val stats = ListeningStats(
            totalTimeMs = prefs[Keys.TOTAL_TIME_MS] ?: 0L,
            todayTimeMs = prefs[Keys.TODAY_TIME_MS] ?: 0L,
            weekTimeMs = prefs[Keys.WEEK_TIME_MS] ?: 0L,
            streakDays = prefs[Keys.STREAK_DAYS] ?: 0,
            longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0,
            booksCompleted = prefs[Keys.BOOKS_COMPLETED] ?: 0,
            sessionsCount = prefs[Keys.SESSIONS_COUNT] ?: 0
        )

        getAllAchievements().map { achievement ->
            val unlockedAt = unlockedMap[achievement.id]
            achievement.copy(
                isUnlocked = unlockedAt != null,
                unlockedAt = unlockedAt
            )
        }
    }

    // ========================================================================
    // ACTIONS
    // ========================================================================

    /**
     * Add listening time. Call this periodically during playback (e.g., every 30 seconds).
     */
    suspend fun addListeningTime(durationMs: Long): List<Achievement> {
        if (durationMs <= 0) return emptyList()

        val today = LocalDate.now().format(dateFormatter)
        val currentWeekStart = getWeekStartDate()
        val newlyUnlocked = mutableListOf<Achievement>()

        context.statsDataStore.edit { prefs ->
            // Update total time
            val currentTotal = prefs[Keys.TOTAL_TIME_MS] ?: 0L
            prefs[Keys.TOTAL_TIME_MS] = currentTotal + durationMs

            // Update today's time (reset if new day)
            val storedDate = prefs[Keys.CURRENT_DATE] ?: ""
            val todayTime = if (storedDate == today) {
                prefs[Keys.TODAY_TIME_MS] ?: 0L
            } else {
                // New day - check streak before resetting
                updateStreakOnNewDay(prefs, storedDate, today)
                0L
            }
            prefs[Keys.TODAY_TIME_MS] = todayTime + durationMs
            prefs[Keys.CURRENT_DATE] = today

            // Update week time (reset if new week)
            val storedWeekStart = prefs[Keys.WEEK_START_DATE] ?: ""
            val weekTime = if (storedWeekStart == currentWeekStart) {
                prefs[Keys.WEEK_TIME_MS] ?: 0L
            } else {
                0L
            }
            prefs[Keys.WEEK_TIME_MS] = weekTime + durationMs
            prefs[Keys.WEEK_START_DATE] = currentWeekStart

            // Update last listened date for streak tracking
            prefs[Keys.LAST_LISTENED_DATE] = today

            // Check for newly unlocked achievements
            val stats = ListeningStats(
                totalTimeMs = prefs[Keys.TOTAL_TIME_MS] ?: 0L,
                todayTimeMs = prefs[Keys.TODAY_TIME_MS] ?: 0L,
                weekTimeMs = prefs[Keys.WEEK_TIME_MS] ?: 0L,
                streakDays = prefs[Keys.STREAK_DAYS] ?: 0,
                longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0,
                booksCompleted = prefs[Keys.BOOKS_COMPLETED] ?: 0,
                sessionsCount = prefs[Keys.SESSIONS_COUNT] ?: 0
            )
            newlyUnlocked.addAll(checkAndUnlockAchievements(prefs, stats))
        }

        return newlyUnlocked
    }

    /**
     * Increment session count. Call when playback starts.
     */
    suspend fun startSession(): List<Achievement> {
        val newlyUnlocked = mutableListOf<Achievement>()

        context.statsDataStore.edit { prefs ->
            val current = prefs[Keys.SESSIONS_COUNT] ?: 0
            prefs[Keys.SESSIONS_COUNT] = current + 1

            // Check for newly unlocked achievements
            val stats = ListeningStats(
                totalTimeMs = prefs[Keys.TOTAL_TIME_MS] ?: 0L,
                todayTimeMs = prefs[Keys.TODAY_TIME_MS] ?: 0L,
                weekTimeMs = prefs[Keys.WEEK_TIME_MS] ?: 0L,
                streakDays = prefs[Keys.STREAK_DAYS] ?: 0,
                longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0,
                booksCompleted = prefs[Keys.BOOKS_COMPLETED] ?: 0,
                sessionsCount = prefs[Keys.SESSIONS_COUNT] ?: 0
            )
            newlyUnlocked.addAll(checkAndUnlockAchievements(prefs, stats))
        }

        return newlyUnlocked
    }

    /**
     * Increment books completed count.
     */
    suspend fun markBookCompleted(): List<Achievement> {
        val newlyUnlocked = mutableListOf<Achievement>()

        context.statsDataStore.edit { prefs ->
            val current = prefs[Keys.BOOKS_COMPLETED] ?: 0
            prefs[Keys.BOOKS_COMPLETED] = current + 1

            // Check for newly unlocked achievements
            val stats = ListeningStats(
                totalTimeMs = prefs[Keys.TOTAL_TIME_MS] ?: 0L,
                todayTimeMs = prefs[Keys.TODAY_TIME_MS] ?: 0L,
                weekTimeMs = prefs[Keys.WEEK_TIME_MS] ?: 0L,
                streakDays = prefs[Keys.STREAK_DAYS] ?: 0,
                longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0,
                booksCompleted = prefs[Keys.BOOKS_COMPLETED] ?: 0,
                sessionsCount = prefs[Keys.SESSIONS_COUNT] ?: 0
            )
            newlyUnlocked.addAll(checkAndUnlockAchievements(prefs, stats))
        }

        return newlyUnlocked
    }

    /**
     * Get all stats as a data class for easy consumption.
     */
    suspend fun getStats(): ListeningStats {
        val prefs = context.statsDataStore.data.first()
        val today = LocalDate.now().format(dateFormatter)
        val currentWeekStart = getWeekStartDate()

        val storedDate = prefs[Keys.CURRENT_DATE] ?: ""
        val storedWeekStart = prefs[Keys.WEEK_START_DATE] ?: ""

        return ListeningStats(
            totalTimeMs = prefs[Keys.TOTAL_TIME_MS] ?: 0L,
            todayTimeMs = if (storedDate == today) prefs[Keys.TODAY_TIME_MS] ?: 0L else 0L,
            weekTimeMs = if (storedWeekStart == currentWeekStart) prefs[Keys.WEEK_TIME_MS] ?: 0L else 0L,
            streakDays = prefs[Keys.STREAK_DAYS] ?: 0,
            longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0,
            booksCompleted = prefs[Keys.BOOKS_COMPLETED] ?: 0,
            sessionsCount = prefs[Keys.SESSIONS_COUNT] ?: 0
        )
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private fun updateStreakOnNewDay(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        storedDate: String,
        today: String
    ) {
        if (storedDate.isBlank()) {
            // First time using app
            prefs[Keys.STREAK_DAYS] = 1
            return
        }

        try {
            val lastDate = LocalDate.parse(storedDate, dateFormatter)
            val todayDate = LocalDate.parse(today, dateFormatter)
            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastDate, todayDate)

            when {
                daysBetween == 1L -> {
                    // Consecutive day - increment streak
                    val currentStreak = (prefs[Keys.STREAK_DAYS] ?: 0) + 1
                    prefs[Keys.STREAK_DAYS] = currentStreak

                    // Update longest streak if needed
                    val longest = prefs[Keys.LONGEST_STREAK] ?: 0
                    if (currentStreak > longest) {
                        prefs[Keys.LONGEST_STREAK] = currentStreak
                    }
                }
                daysBetween > 1L -> {
                    // Streak broken - reset to 1
                    prefs[Keys.STREAK_DAYS] = 1
                }
                // daysBetween == 0 or negative shouldn't happen, but handle gracefully
            }
        } catch (e: Exception) {
            // Parse error - reset streak
            prefs[Keys.STREAK_DAYS] = 1
        }
    }

    private fun getWeekStartDate(): String {
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1) // Monday
        return weekStart.format(dateFormatter)
    }

    /**
     * Check if any achievements should be unlocked based on current stats
     */
    private fun checkAndUnlockAchievements(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        stats: ListeningStats
    ): List<Achievement> {
        val unlockedData = prefs[Keys.UNLOCKED_ACHIEVEMENTS] ?: ""
        val unlockedMap = parseUnlockedAchievements(unlockedData).toMutableMap()
        val newlyUnlocked = mutableListOf<Achievement>()

        getAllAchievements().forEach { achievement ->
            // Skip if already unlocked
            if (unlockedMap.containsKey(achievement.id)) return@forEach

            // Check if requirements are met
            val isUnlocked = when (achievement.category) {
                AchievementCategory.STREAK -> stats.streakDays >= achievement.requirement
                AchievementCategory.TIME -> stats.totalTimeMs >= achievement.requirement
                AchievementCategory.BOOKS -> stats.booksCompleted >= achievement.requirement
                AchievementCategory.SESSIONS -> stats.sessionsCount >= achievement.requirement
            }

            if (isUnlocked) {
                val unlockedAt = LocalDateTime.now()
                unlockedMap[achievement.id] = unlockedAt
                newlyUnlocked.add(
                    achievement.copy(
                        isUnlocked = true,
                        unlockedAt = unlockedAt
                    )
                )
            }
        }

        // Save updated unlocked achievements
        if (newlyUnlocked.isNotEmpty()) {
            prefs[Keys.UNLOCKED_ACHIEVEMENTS] = serializeUnlockedAchievements(unlockedMap)
        }

        return newlyUnlocked
    }

    /**
     * Parse unlocked achievements from storage format
     */
    private fun parseUnlockedAchievements(data: String): Map<String, LocalDateTime> {
        if (data.isBlank()) return emptyMap()

        return try {
            data.split(";")
                .filter { it.isNotBlank() }
                .associate { entry ->
                    val parts = entry.split(":")
                    val id = parts[0]
                    val dateTime = LocalDateTime.parse(parts[1], dateTimeFormatter)
                    id to dateTime
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Serialize unlocked achievements to storage format
     */
    private fun serializeUnlockedAchievements(map: Map<String, LocalDateTime>): String {
        return map.entries.joinToString(";") { (id, dateTime) ->
            "$id:${dateTime.format(dateTimeFormatter)}"
        }
    }
}

/**
 * Data class for all listening stats.
 */
data class ListeningStats(
    val totalTimeMs: Long,
    val todayTimeMs: Long,
    val weekTimeMs: Long,
    val streakDays: Int,
    val longestStreak: Int,
    val booksCompleted: Int,
    val sessionsCount: Int
) {
    // Formatted helpers
    val totalTimeFormatted: String get() = formatDuration(totalTimeMs)
    val todayTimeFormatted: String get() = formatDuration(todayTimeMs)
    val weekTimeFormatted: String get() = formatDuration(weekTimeMs)

    private fun formatDuration(ms: Long): String {
        val totalMinutes = ms / 1000 / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "0m"
        }
    }
}
