package com.mossglen.reverie.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Keys
    private object Keys {
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val SCAN_ON_STARTUP = booleanPreferencesKey("scan_on_startup")
        val DELETE_IF_MISSING = booleanPreferencesKey("delete_if_missing")
        val AUDIO_CODEC = stringPreferencesKey("audio_codec")
        val AUDIO_OUTPUT = stringPreferencesKey("audio_output")
        val SKIP_BACKWARD = intPreferencesKey("skip_backward")
        val SKIP_FORWARD = intPreferencesKey("skip_forward")
        val KEEP_SERVICE_ACTIVE = booleanPreferencesKey("keep_service_active")
        val SHOW_LOCK_SCREEN_COVER = booleanPreferencesKey("show_lock_screen_cover")
        val SLEEP_TIMER_MINUTES = intPreferencesKey("sleep_timer_minutes")
        val FILE_LOGGING = booleanPreferencesKey("file_logging")
        val DYNAMIC_PLAYER_COLORS = booleanPreferencesKey("dynamic_player_colors")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val AUDIO_FADE_DURATION = intPreferencesKey("audio_fade_duration")

        // Torrent Settings
        val TORRENT_ENABLED = booleanPreferencesKey("torrent_enabled")
        val TORRENT_WIFI_ONLY = booleanPreferencesKey("torrent_wifi_only")
        val TORRENT_MAX_DOWNLOADS = intPreferencesKey("torrent_max_downloads")
        val TORRENT_UPLOAD_LIMIT = intPreferencesKey("torrent_upload_limit_kbps")
        val TORRENT_DOWNLOAD_LIMIT = intPreferencesKey("torrent_download_limit_kbps")
        val TORRENT_AUTO_START = booleanPreferencesKey("torrent_auto_start")
        val TORRENT_AUTO_FETCH_METADATA = booleanPreferencesKey("torrent_auto_fetch_metadata")
        val TORRENT_SEED_AFTER_DOWNLOAD = booleanPreferencesKey("torrent_seed_after_download")
        val TORRENT_SAVE_PATH = stringPreferencesKey("torrent_save_path")

        // Kids Mode Settings
        val KIDS_MODE_ENABLED = booleanPreferencesKey("kids_mode_enabled")
        val KIDS_MODE_PIN = stringPreferencesKey("kids_mode_pin")

        // Language Settings
        val APP_LANGUAGE = stringPreferencesKey("app_language")

        // Auto-Bookmark Settings
        val AUTO_BOOKMARK_ENABLED = booleanPreferencesKey("auto_bookmark_enabled")

        // Playback Speed Presets
        val CUSTOM_SPEED_PRESETS = stringPreferencesKey("custom_speed_presets")

        // Library Folders
        val LIBRARY_FOLDERS = stringPreferencesKey("library_folders")

        // Journey/Stats Settings
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")

        // TTS Settings
        val TTS_ENGINE_TYPE = stringPreferencesKey("tts_engine_type")  // "system" or "kokoro"
        val TTS_VOICE_ID = intPreferencesKey("tts_voice_id")          // Kokoro speaker ID
    }

    // Download Settings
    val wifiOnly: Flow<Boolean> = context.dataStore.data.map { it[Keys.WIFI_ONLY] ?: true }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY] = enabled }
    }

    // Library Settings
    val scanOnStartup: Flow<Boolean> = context.dataStore.data.map { it[Keys.SCAN_ON_STARTUP] ?: true }

    suspend fun setScanOnStartup(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SCAN_ON_STARTUP] = enabled }
    }

    val deleteIfMissing: Flow<Boolean> = context.dataStore.data.map { it[Keys.DELETE_IF_MISSING] ?: false }

    suspend fun setDeleteIfMissing(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DELETE_IF_MISSING] = enabled }
    }

    // Audio Settings
    val audioCodec: Flow<String> = context.dataStore.data.map { it[Keys.AUDIO_CODEC] ?: "Android" }

    suspend fun setAudioCodec(codec: String) {
        context.dataStore.edit { it[Keys.AUDIO_CODEC] = codec }
    }

    val audioOutput: Flow<String> = context.dataStore.data.map { it[Keys.AUDIO_OUTPUT] ?: "OpenSL ES" }

    suspend fun setAudioOutput(output: String) {
        context.dataStore.edit { it[Keys.AUDIO_OUTPUT] = output }
    }

    // Player Settings
    val skipBackward: Flow<Int> = context.dataStore.data.map { it[Keys.SKIP_BACKWARD] ?: 10 }

    suspend fun setSkipBackward(seconds: Int) {
        context.dataStore.edit { it[Keys.SKIP_BACKWARD] = seconds }
    }

    val skipForward: Flow<Int> = context.dataStore.data.map { it[Keys.SKIP_FORWARD] ?: 30 }

    suspend fun setSkipForward(seconds: Int) {
        context.dataStore.edit { it[Keys.SKIP_FORWARD] = seconds }
    }

    val keepServiceActive: Flow<Boolean> = context.dataStore.data.map { it[Keys.KEEP_SERVICE_ACTIVE] ?: false }

    suspend fun setKeepServiceActive(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SERVICE_ACTIVE] = enabled }
    }

    val showLockScreenCover: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_LOCK_SCREEN_COVER] ?: true }

    suspend fun setShowLockScreenCover(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_LOCK_SCREEN_COVER] = enabled }
    }

    val sleepTimerMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.SLEEP_TIMER_MINUTES] ?: 0 }

    suspend fun setSleepTimerMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.SLEEP_TIMER_MINUTES] = minutes }
    }

    // Debug Settings
    val fileLogging: Flow<Boolean> = context.dataStore.data.map { it[Keys.FILE_LOGGING] ?: false }

    suspend fun setFileLogging(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FILE_LOGGING] = enabled }
    }

    // Appearance Settings
    val dynamicPlayerColors: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_PLAYER_COLORS] ?: true }

    suspend fun setDynamicPlayerColors(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_PLAYER_COLORS] = enabled }
    }

    // Playback Speed
    val playbackSpeed: Flow<Float> = context.dataStore.data.map { it[Keys.PLAYBACK_SPEED] ?: 1.0f }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed }
    }

    // Audio Fade Duration (in milliseconds)
    val audioFadeDuration: Flow<Int> = context.dataStore.data.map { it[Keys.AUDIO_FADE_DURATION] ?: 500 }

    suspend fun setAudioFadeDuration(durationMs: Int) {
        context.dataStore.edit { it[Keys.AUDIO_FADE_DURATION] = durationMs }
    }

    // ========================================================================
    // Torrent Settings
    // ========================================================================

    val torrentEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.TORRENT_ENABLED] ?: true }

    suspend fun setTorrentEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TORRENT_ENABLED] = enabled }
    }

    val torrentWifiOnly: Flow<Boolean> = context.dataStore.data.map { it[Keys.TORRENT_WIFI_ONLY] ?: true }

    suspend fun setTorrentWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TORRENT_WIFI_ONLY] = enabled }
    }

    val torrentMaxDownloads: Flow<Int> = context.dataStore.data.map { it[Keys.TORRENT_MAX_DOWNLOADS] ?: 3 }

    suspend fun setTorrentMaxDownloads(max: Int) {
        context.dataStore.edit { it[Keys.TORRENT_MAX_DOWNLOADS] = max }
    }

    val torrentUploadLimit: Flow<Int> = context.dataStore.data.map { it[Keys.TORRENT_UPLOAD_LIMIT] ?: 50 }

    suspend fun setTorrentUploadLimit(kbps: Int) {
        context.dataStore.edit { it[Keys.TORRENT_UPLOAD_LIMIT] = kbps }
    }

    val torrentDownloadLimit: Flow<Int> = context.dataStore.data.map { it[Keys.TORRENT_DOWNLOAD_LIMIT] ?: 0 }

    suspend fun setTorrentDownloadLimit(kbps: Int) {
        context.dataStore.edit { it[Keys.TORRENT_DOWNLOAD_LIMIT] = kbps }
    }

    val torrentAutoStart: Flow<Boolean> = context.dataStore.data.map { it[Keys.TORRENT_AUTO_START] ?: true }

    suspend fun setTorrentAutoStart(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TORRENT_AUTO_START] = enabled }
    }

    val torrentAutoFetchMetadata: Flow<Boolean> = context.dataStore.data.map { it[Keys.TORRENT_AUTO_FETCH_METADATA] ?: true }

    suspend fun setTorrentAutoFetchMetadata(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TORRENT_AUTO_FETCH_METADATA] = enabled }
    }

    val torrentSeedAfterDownload: Flow<Boolean> = context.dataStore.data.map { it[Keys.TORRENT_SEED_AFTER_DOWNLOAD] ?: false }

    suspend fun setTorrentSeedAfterDownload(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TORRENT_SEED_AFTER_DOWNLOAD] = enabled }
    }

    val torrentSavePath: Flow<String> = context.dataStore.data.map { it[Keys.TORRENT_SAVE_PATH] ?: "" }

    suspend fun setTorrentSavePath(path: String) {
        context.dataStore.edit { it[Keys.TORRENT_SAVE_PATH] = path }
    }

    // ========================================================================
    // Kids Mode Settings
    // ========================================================================

    val kidsModeEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.KIDS_MODE_ENABLED] ?: false }

    suspend fun setKidsModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KIDS_MODE_ENABLED] = enabled }
    }

    val kidsModePin: Flow<String> = context.dataStore.data.map { it[Keys.KIDS_MODE_PIN] ?: "" }

    suspend fun setKidsModePin(pin: String) {
        context.dataStore.edit { it[Keys.KIDS_MODE_PIN] = pin }
    }

    /**
     * Verify if the entered PIN matches the stored PIN
     */
    suspend fun verifyKidsModePin(enteredPin: String): Boolean {
        val storedPin = kidsModePin.first()
        return storedPin.isNotEmpty() && enteredPin == storedPin
    }

    // ========================================================================
    // Language Settings
    // ========================================================================

    val appLanguage: Flow<String> = context.dataStore.data.map { it[Keys.APP_LANGUAGE] ?: "System" }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = language }
    }

    // ========================================================================
    // Auto-Bookmark Settings
    // ========================================================================

    val autoBookmarkEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_BOOKMARK_ENABLED] ?: false }

    suspend fun setAutoBookmarkEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BOOKMARK_ENABLED] = enabled }
    }

    // ========================================================================
    // Journey/Stats Settings
    // ========================================================================

    val dailyGoalMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.DAILY_GOAL_MINUTES] ?: 60 }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DAILY_GOAL_MINUTES] = minutes }
    }

    // ========================================================================
    // Playback Speed Presets
    // ========================================================================

    @Serializable
    data class CustomSpeedPreset(
        val name: String,
        val speed: Float
    )

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Get custom speed presets
     */
    val customSpeedPresets: Flow<List<CustomSpeedPreset>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[Keys.CUSTOM_SPEED_PRESETS] ?: "[]"
        try {
            json.decodeFromString(ListSerializer(CustomSpeedPreset.serializer()), jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Add or update a custom speed preset
     */
    suspend fun saveCustomSpeedPreset(name: String, speed: Float) {
        context.dataStore.edit { preferences ->
            val current = try {
                val jsonString = preferences[Keys.CUSTOM_SPEED_PRESETS] ?: "[]"
                json.decodeFromString(ListSerializer(CustomSpeedPreset.serializer()), jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            // Remove existing preset with same name (update scenario)
            current.removeAll { it.name == name }

            // Add new preset
            current.add(CustomSpeedPreset(name, speed))

            // Save back to preferences
            preferences[Keys.CUSTOM_SPEED_PRESETS] = json.encodeToString(ListSerializer(CustomSpeedPreset.serializer()), current)
        }
    }

    /**
     * Delete a custom speed preset
     */
    suspend fun deleteCustomSpeedPreset(name: String) {
        context.dataStore.edit { preferences ->
            val current = try {
                val jsonString = preferences[Keys.CUSTOM_SPEED_PRESETS] ?: "[]"
                json.decodeFromString(ListSerializer(CustomSpeedPreset.serializer()), jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            // Remove preset with given name
            current.removeAll { it.name == name }

            // Save back to preferences
            preferences[Keys.CUSTOM_SPEED_PRESETS] = json.encodeToString(ListSerializer(CustomSpeedPreset.serializer()), current)
        }
    }

    /**
     * Get all custom preset names
     */
    suspend fun getCustomPresetNames(): List<String> {
        return customSpeedPresets.first().map { it.name }
    }

    // ========================================================================
    // Library Folders
    // ========================================================================

    /**
     * Get library folders as a Flow
     */
    val libraryFolders: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[Keys.LIBRARY_FOLDERS] ?: "[]"
        try {
            json.decodeFromString(ListSerializer(String.serializer()), jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Add a library folder
     */
    suspend fun addLibraryFolder(folderUri: String) {
        context.dataStore.edit { preferences ->
            val current = try {
                val jsonString = preferences[Keys.LIBRARY_FOLDERS] ?: "[]"
                json.decodeFromString(ListSerializer(String.serializer()), jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            // Don't add duplicates
            if (!current.contains(folderUri)) {
                current.add(folderUri)
                preferences[Keys.LIBRARY_FOLDERS] = json.encodeToString(ListSerializer(String.serializer()), current)
            }
        }
    }

    /**
     * Remove a library folder
     */
    suspend fun removeLibraryFolder(folderUri: String) {
        context.dataStore.edit { preferences ->
            val current = try {
                val jsonString = preferences[Keys.LIBRARY_FOLDERS] ?: "[]"
                json.decodeFromString(ListSerializer(String.serializer()), jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            current.remove(folderUri)
            preferences[Keys.LIBRARY_FOLDERS] = json.encodeToString(ListSerializer(String.serializer()), current)
        }
    }

    /**
     * Get library folders as a list (suspend function)
     */
    suspend fun getLibraryFolders(): List<String> {
        return libraryFolders.first()
    }

    // ========================================================================
    // TTS Settings
    // ========================================================================

    /**
     * TTS engine type: "system" or "kokoro"
     */
    val ttsEngineType: Flow<String> = context.dataStore.data.map { it[Keys.TTS_ENGINE_TYPE] ?: "system" }

    suspend fun setTtsEngineType(engineType: String) {
        context.dataStore.edit { it[Keys.TTS_ENGINE_TYPE] = engineType }
    }

    /**
     * Get TTS engine type synchronously (for initialization)
     */
    suspend fun getTtsEngineType(): String {
        return ttsEngineType.first()
    }

    /**
     * Kokoro voice ID (speaker ID for multi-voice model)
     */
    val ttsVoiceId: Flow<Int> = context.dataStore.data.map { it[Keys.TTS_VOICE_ID] ?: 0 }

    suspend fun setTtsVoiceId(voiceId: Int) {
        context.dataStore.edit { it[Keys.TTS_VOICE_ID] = voiceId }
    }

    /**
     * Get TTS voice ID synchronously
     */
    suspend fun getTtsVoiceId(): Int {
        return ttsVoiceId.first()
    }
}
