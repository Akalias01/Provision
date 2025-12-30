package com.mossglen.reverie.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mossglen.reverie.data.Achievement
import com.mossglen.reverie.data.ListeningStats
import com.mossglen.reverie.data.ListeningStatsRepository
import com.mossglen.reverie.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListeningStatsViewModel @Inject constructor(
    private val statsRepository: ListeningStatsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _stats = MutableStateFlow(
        ListeningStats(
            totalTimeMs = 0L,
            todayTimeMs = 0L,
            weekTimeMs = 0L,
            streakDays = 0,
            longestStreak = 0,
            booksCompleted = 0,
            sessionsCount = 0
        )
    )
    val stats = _stats.asStateFlow()

    // Convenience properties for UI consumption
    val totalListeningTime: StateFlow<Long> = stats.map { it.totalTimeMs }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
    )

    val todayListeningTime: StateFlow<Long> = stats.map { it.todayTimeMs }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
    )

    val currentStreak: StateFlow<Int> = stats.map { it.streakDays }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    val booksFinished: StateFlow<Int> = stats.map { it.booksCompleted }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    // Books in progress - from repository
    private val _booksInProgress = MutableStateFlow(0)
    val booksInProgress: StateFlow<Int> = _booksInProgress.asStateFlow()

    // Weekly progress (7 days, 0-1 progress for each day)
    private val _weeklyProgress = MutableStateFlow(List(7) { 0f })
    val weeklyProgress: StateFlow<List<Float>> = _weeklyProgress.asStateFlow()

    // Daily goal setting
    val dailyGoalMinutes: StateFlow<Int> = settingsRepository.dailyGoalMinutes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 60
    )

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements = _achievements.asStateFlow()

    private val _newlyUnlockedAchievements = MutableStateFlow<List<Achievement>>(emptyList())
    val newlyUnlockedAchievements = _newlyUnlockedAchievements.asStateFlow()

    init {
        // Combine all stat flows into one
        viewModelScope.launch {
            combine(
                statsRepository.totalTimeMs,
                statsRepository.todayTimeMs,
                statsRepository.weekTimeMs,
                statsRepository.streakDays,
                statsRepository.longestStreak,
                statsRepository.booksCompleted,
                statsRepository.sessionsCount
            ) { values ->
                ListeningStats(
                    totalTimeMs = values[0] as Long,
                    todayTimeMs = values[1] as Long,
                    weekTimeMs = values[2] as Long,
                    streakDays = values[3] as Int,
                    longestStreak = values[4] as Int,
                    booksCompleted = values[5] as Int,
                    sessionsCount = values[6] as Int
                )
            }.collect { stats ->
                _stats.value = stats
            }
        }

        // Collect achievements
        viewModelScope.launch {
            statsRepository.achievements.collect { achievements ->
                _achievements.value = achievements
            }
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            _stats.value = statsRepository.getStats()
        }
    }

    fun dismissNewlyUnlockedAchievements() {
        _newlyUnlockedAchievements.value = emptyList()
    }

    fun showAchievementUnlocked(achievements: List<Achievement>) {
        if (achievements.isNotEmpty()) {
            _newlyUnlockedAchievements.value = achievements
        }
    }

    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setDailyGoalMinutes(minutes)
        }
    }
}
