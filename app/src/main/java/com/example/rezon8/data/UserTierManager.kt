package com.mossglen.reverie.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class UserTier {
    FREE,   // Ads, Local Playback
    PRO,    // No Ads, Kids Mode, Cloud Storage Access
    ELITE   // Cross-Device Sync, AI Summaries
}

@Singleton
class UserTierManager @Inject constructor(@ApplicationContext private val context: Context) {
    // Defaulting to FREE. In production, this syncs with Play Billing.
    private val _currentTier = MutableStateFlow(UserTier.FREE)
    val currentTier = _currentTier.asStateFlow()

    fun showAds(): Boolean = _currentTier.value == UserTier.FREE
    fun canAccessKidsMode(): Boolean = _currentTier.value != UserTier.FREE
    fun canAccessCloud(): Boolean = _currentTier.value != UserTier.FREE
    fun canSyncProgress(): Boolean = _currentTier.value == UserTier.ELITE

    // Debug: Call this to test Pro/Elite features instantly
    fun setDebugTier(tier: UserTier) { _currentTier.value = tier }
}
