package com.mossglen.lithos.tts

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * VoiceRepository - Manages Kokoro voice selection and preferences.
 *
 * ============================================================================
 * KOKORO ENGLISH-ONLY MODEL (kokoro-int8-en-v0.19) - 11 VOICES
 * ============================================================================
 *
 * Voice ID mapping (from voices.bin):
 *   0: af        - American Female (default)
 *   1: af_bella  - American Female (warm, professional)
 *   2: af_nicole - American Female (energetic)
 *   3: af_sarah  - American Female (clear, articulate)
 *   4: af_sky    - American Female (soft, dreamy)
 *   5: am_adam   - American Male (deep, authoritative)
 *   6: am_michael- American Male (friendly)
 *   7: bf_emma   - British Female (elegant)
 *   8: bf_isabella - British Female (refined)
 *   9: bm_george - British Male (distinguished)
 *  10: bm_lewis  - British Male (modern)
 */
class VoiceRepository(private val context: Context) {

    companion object {
        private const val TAG = "VoiceRepository"
        private const val PREFS_NAME = "lithos_tts_prefs"
        private const val KEY_SELECTED_VOICE_ID = "selected_voice_id"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val DEFAULT_VOICE_ID = 1  // af_bella - best for audiobooks
        private const val DEFAULT_SPEED = 1.0f
    }

    data class Voice(
        val id: Int,
        val name: String,
        val displayName: String,
        val gender: Gender,
        val accent: Accent,
        val description: String
    )

    enum class Gender { MALE, FEMALE }
    enum class Accent { AMERICAN, BRITISH }

    // KOKORO ENGLISH-ONLY - 11 Voices (IDs 0-10)
    private val voices = listOf(
        // American Female (0-4)
        Voice(0, "af", "Default", Gender.FEMALE, Accent.AMERICAN, "Default American female"),
        Voice(1, "af_bella", "Bella", Gender.FEMALE, Accent.AMERICAN, "Warm, professional"),
        Voice(2, "af_nicole", "Nicole", Gender.FEMALE, Accent.AMERICAN, "Energetic, clear"),
        Voice(3, "af_sarah", "Sarah", Gender.FEMALE, Accent.AMERICAN, "Clear, articulate"),
        Voice(4, "af_sky", "Sky", Gender.FEMALE, Accent.AMERICAN, "Soft, dreamy"),
        // American Male (5-6)
        Voice(5, "am_adam", "Adam", Gender.MALE, Accent.AMERICAN, "Deep, authoritative"),
        Voice(6, "am_michael", "Michael", Gender.MALE, Accent.AMERICAN, "Friendly, conversational"),
        // British Female (7-8)
        Voice(7, "bf_emma", "Emma", Gender.FEMALE, Accent.BRITISH, "Elegant British"),
        Voice(8, "bf_isabella", "Isabella", Gender.FEMALE, Accent.BRITISH, "Refined British"),
        // British Male (9-10)
        Voice(9, "bm_george", "George", Gender.MALE, Accent.BRITISH, "Distinguished British"),
        Voice(10, "bm_lewis", "Lewis", Gender.MALE, Accent.BRITISH, "Modern British")
    )

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _selectedVoiceId = MutableStateFlow(loadSavedVoiceId())
    val selectedVoiceId: StateFlow<Int> = _selectedVoiceId.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(loadSavedSpeed())
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun getAllVoices(): List<Voice> = voices
    fun getVoicesByGender(gender: Gender): List<Voice> = voices.filter { it.gender == gender }
    fun getVoicesByAccent(accent: Accent): List<Voice> = voices.filter { it.accent == accent }
    fun getVoice(id: Int): Voice? = voices.find { it.id == id }
    fun getSelectedVoice(): Voice = voices.find { it.id == _selectedVoiceId.value } ?: voices[DEFAULT_VOICE_ID]

    fun setSelectedVoice(voiceId: Int) {
        val voice = getVoice(voiceId)
        if (voice != null) {
            _selectedVoiceId.value = voiceId
            saveVoiceId(voiceId)
            Log.d(TAG, "Voice changed to: ${voice.displayName} (id=$voiceId)")
        } else {
            Log.w(TAG, "Invalid voice ID: $voiceId, using default")
            _selectedVoiceId.value = DEFAULT_VOICE_ID
            saveVoiceId(DEFAULT_VOICE_ID)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.5f, 2.0f)
        _playbackSpeed.value = clampedSpeed
        saveSpeed(clampedSpeed)
        Log.d(TAG, "Speed changed to: ${clampedSpeed}x")
    }

    fun getRecommendedVoices(): List<Voice> = listOf(
        getVoice(1)!!,  // af_bella
        getVoice(5)!!,  // am_adam
        getVoice(7)!!,  // bf_emma
        getVoice(9)!!   // bm_george
    )

    private fun loadSavedVoiceId(): Int {
        val savedId = prefs.getInt(KEY_SELECTED_VOICE_ID, DEFAULT_VOICE_ID)
        return if (getVoice(savedId) != null) savedId else DEFAULT_VOICE_ID
    }

    private fun saveVoiceId(voiceId: Int) {
        prefs.edit().putInt(KEY_SELECTED_VOICE_ID, voiceId).apply()
    }

    private fun loadSavedSpeed(): Float {
        return prefs.getFloat(KEY_PLAYBACK_SPEED, DEFAULT_SPEED)
    }

    private fun saveSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_PLAYBACK_SPEED, speed).apply()
    }
}
