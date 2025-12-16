package com.example.rezon.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    // Keys
    private val KEEP_SERVICE_ACTIVE = booleanPreferencesKey("keep_service_active")
    private val STOP_ON_CLOSE = booleanPreferencesKey("stop_on_close")
    private val SHOW_COVER_LOCK = booleanPreferencesKey("show_cover_lock")
    private val WIFI_ONLY = booleanPreferencesKey("wifi_only")

    // Flows
    val keepServiceActive: Flow<Boolean> = context.dataStore.data.map { it[KEEP_SERVICE_ACTIVE] ?: false }
    val stopOnClose: Flow<Boolean> = context.dataStore.data.map { it[STOP_ON_CLOSE] ?: true }
    val showCoverOnLock: Flow<Boolean> = context.dataStore.data.map { it[SHOW_COVER_LOCK] ?: true }
    val wifiOnly: Flow<Boolean> = context.dataStore.data.map { it[WIFI_ONLY] ?: true }

    // Actions
    suspend fun setKeepServiceActive(enabled: Boolean) { context.dataStore.edit { it[KEEP_SERVICE_ACTIVE] = enabled } }
    suspend fun setStopOnClose(enabled: Boolean) { context.dataStore.edit { it[STOP_ON_CLOSE] = enabled } }
    suspend fun setShowCoverOnLock(enabled: Boolean) { context.dataStore.edit { it[SHOW_COVER_LOCK] = enabled } }
    suspend fun setWifiOnly(enabled: Boolean) { context.dataStore.edit { it[WIFI_ONLY] = enabled } }
}
