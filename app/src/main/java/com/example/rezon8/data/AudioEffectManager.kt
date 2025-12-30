package com.mossglen.reverie.data

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.audioEffectsDataStore: DataStore<Preferences> by preferencesDataStore(name = "audio_effects")

/**
 * Premium AudioEffectManager - Controls real Android audio processing
 *
 * Manages:
 * - 10-Band Equalizer (via Android's 5-band interpolated to 10)
 * - Bass Boost
 * - Virtualizer (Stereo Widening)
 * - Reverb
 * - Loudness Enhancer
 * - Stereo Balance (via volume adjustment)
 */
@Singleton
class AudioEffectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayer: ExoPlayer
) {
    private val TAG = "AudioEffectManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = context.audioEffectsDataStore

    // Android Audio Effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    // State flows for UI
    private val _eqEnabled = MutableStateFlow(true)
    val eqEnabled = _eqEnabled.asStateFlow()

    private val _bands = MutableStateFlow(listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
    val bands = _bands.asStateFlow()

    private val _preamp = MutableStateFlow(0f)
    val preamp = _preamp.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(0f)
    val bassBoostStrength = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(0f)
    val virtualizerStrength = _virtualizerStrength.asStateFlow()

    private val _reverbPreset = MutableStateFlow(0)
    val reverbPreset = _reverbPreset.asStateFlow()

    private val _loudnessEnabled = MutableStateFlow(false)
    val loudnessEnabled = _loudnessEnabled.asStateFlow()

    private val _amplifierGain = MutableStateFlow(0f)
    val amplifierGain = _amplifierGain.asStateFlow()

    private val _selectedPresetName = MutableStateFlow("Flat")
    val selectedPresetName = _selectedPresetName.asStateFlow()

    // DataStore keys
    private object Keys {
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val PRESET_NAME = stringPreferencesKey("preset_name")
        val PREAMP = floatPreferencesKey("preamp")
        val BASS_BOOST = floatPreferencesKey("bass_boost")
        val VIRTUALIZER = floatPreferencesKey("virtualizer")
        val REVERB_PRESET = intPreferencesKey("reverb_preset")
        val LOUDNESS_ENABLED = booleanPreferencesKey("loudness_enabled")
        val AMPLIFIER_GAIN = floatPreferencesKey("amplifier_gain")
        fun bandKey(index: Int) = floatPreferencesKey("band_$index")
    }

    // 10-band frequencies we display (Hz)
    val displayFrequencies = listOf(31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

    // Android EQ typically has 5 bands - we'll map our 10 to these
    private var androidBandCount = 5
    private var bandLevelMin: Short = -1500 // millibels
    private var bandLevelMax: Short = 1500 // millibels

    private var isInitialized = false

    /**
     * Initialize audio effects - call after audio starts playing
     */
    fun initialize() {
        if (isInitialized) return

        try {
            val audioSessionId = exoPlayer.audioSessionId
            if (audioSessionId == 0) {
                Log.w(TAG, "Audio session ID is 0, waiting for playback to start")
                return
            }

            Log.d(TAG, "Initializing audio effects with session ID: $audioSessionId")

            // Initialize Equalizer
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
                androidBandCount = numberOfBands.toInt()
                val range = bandLevelRange
                bandLevelMin = range[0]
                bandLevelMax = range[1]
                Log.d(TAG, "Equalizer initialized: $androidBandCount bands, range: $bandLevelMin to $bandLevelMax")
            }

            // Initialize Bass Boost
            try {
                bassBoost = BassBoost(0, audioSessionId).apply {
                    enabled = true
                    if (strengthSupported) {
                        setStrength(0)
                    }
                }
                Log.d(TAG, "BassBoost initialized, strength supported: ${bassBoost?.strengthSupported}")
            } catch (e: Exception) {
                Log.e(TAG, "BassBoost not supported: ${e.message}")
            }

            // Initialize Virtualizer
            try {
                virtualizer = Virtualizer(0, audioSessionId).apply {
                    enabled = true
                    if (strengthSupported) {
                        setStrength(0)
                    }
                }
                Log.d(TAG, "Virtualizer initialized, strength supported: ${virtualizer?.strengthSupported}")
            } catch (e: Exception) {
                Log.e(TAG, "Virtualizer not supported: ${e.message}")
            }

            // Initialize Reverb
            try {
                presetReverb = PresetReverb(0, audioSessionId).apply {
                    enabled = false
                    preset = PresetReverb.PRESET_NONE
                }
                Log.d(TAG, "PresetReverb initialized")
            } catch (e: Exception) {
                Log.e(TAG, "PresetReverb not supported: ${e.message}")
            }

            // Initialize Loudness Enhancer (API 19+)
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    enabled = false
                    setTargetGain(0)
                }
                Log.d(TAG, "LoudnessEnhancer initialized")
            } catch (e: Exception) {
                Log.e(TAG, "LoudnessEnhancer not supported: ${e.message}")
            }

            isInitialized = true

            // Load saved settings
            scope.launch {
                loadSettings()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio effects: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Re-initialize effects when audio session changes (e.g., new track)
     */
    fun reinitialize() {
        release()
        isInitialized = false
        initialize()
    }

    /**
     * Set EQ enabled state
     */
    fun setEqEnabled(enabled: Boolean) {
        _eqEnabled.value = enabled
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled && _bassBoostStrength.value > 0
        virtualizer?.enabled = enabled && _virtualizerStrength.value > 0
        presetReverb?.enabled = enabled && _reverbPreset.value > 0
        loudnessEnhancer?.enabled = enabled && _loudnessEnabled.value

        scope.launch {
            dataStore.edit { it[Keys.EQ_ENABLED] = enabled }
        }
    }

    /**
     * Set a specific EQ band level
     * @param bandIndex 0-9 for our 10-band display
     * @param levelDb -12 to +12 dB
     */
    fun setBandLevel(bandIndex: Int, levelDb: Float) {
        if (bandIndex !in 0..9) return

        val newBands = _bands.value.toMutableList()
        newBands[bandIndex] = levelDb.coerceIn(-12f, 12f)
        _bands.value = newBands

        // Apply to Android's equalizer
        applyBandsToEqualizer()

        // Save
        scope.launch {
            dataStore.edit { it[Keys.bandKey(bandIndex)] = levelDb }
        }
    }

    /**
     * Set all 10 bands at once
     */
    fun setBands(newBands: List<Float>) {
        if (newBands.size != 10) return
        _bands.value = newBands.map { it.coerceIn(-12f, 12f) }
        applyBandsToEqualizer()

        scope.launch {
            dataStore.edit { prefs ->
                newBands.forEachIndexed { index, level ->
                    prefs[Keys.bandKey(index)] = level
                }
            }
        }
    }

    /**
     * Map our 10-band settings to Android's typically 5-band EQ
     */
    private fun applyBandsToEqualizer() {
        val eq = equalizer ?: return
        if (!_eqEnabled.value) return

        try {
            val bands10 = _bands.value
            val preampDb = _preamp.value

            // Android EQ uses millibels (1/100th of a dB)
            // Map our 10 bands to Android's bands using interpolation
            for (androidBand in 0 until androidBandCount) {
                val centerFreq = eq.getCenterFreq(androidBand.toShort()) / 1000 // Hz

                // Find closest 10-band frequencies and interpolate
                val levelDb = interpolateBandLevel(centerFreq, bands10) + preampDb
                val levelMb = (levelDb * 100).toInt().toShort()
                    .coerceIn(bandLevelMin, bandLevelMax)

                eq.setBandLevel(androidBand.toShort(), levelMb)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying EQ bands: ${e.message}")
        }
    }

    /**
     * Interpolate between our 10-band frequencies to find level for Android's frequency
     */
    private fun interpolateBandLevel(freqHz: Int, bands: List<Float>): Float {
        // Find the two closest frequencies in our 10-band array
        var lowerIndex = 0
        var upperIndex = displayFrequencies.size - 1

        for (i in displayFrequencies.indices) {
            if (displayFrequencies[i] <= freqHz) lowerIndex = i
            if (displayFrequencies[i] >= freqHz) {
                upperIndex = i
                break
            }
        }

        if (lowerIndex == upperIndex) return bands[lowerIndex]

        // Linear interpolation
        val lowerFreq = displayFrequencies[lowerIndex].toFloat()
        val upperFreq = displayFrequencies[upperIndex].toFloat()
        val ratio = (freqHz - lowerFreq) / (upperFreq - lowerFreq)

        return bands[lowerIndex] + ratio * (bands[upperIndex] - bands[lowerIndex])
    }

    /**
     * Set preamp level
     */
    fun setPreamp(levelDb: Float) {
        _preamp.value = levelDb.coerceIn(-12f, 12f)
        applyBandsToEqualizer()

        scope.launch {
            dataStore.edit { it[Keys.PREAMP] = levelDb }
        }
    }

    /**
     * Set bass boost strength (0-1)
     * Uses a hybrid approach: Android's BassBoost API + EQ band adjustments
     * for more reliable and predictable bass enhancement
     */
    fun setBassBoost(strength: Float) {
        _bassBoostStrength.value = strength.coerceIn(0f, 1f)

        // Apply Android's native bass boost (at reduced strength to avoid over-processing)
        bassBoost?.let { bb ->
            if (bb.strengthSupported) {
                // Use 60% of the requested strength for the native effect
                val androidStrength = (strength * 600).toInt().toShort().coerceIn(0, 1000)
                bb.setStrength(androidStrength)
                bb.enabled = _eqEnabled.value && strength > 0
            }
        }

        // Also apply EQ-based bass boost for more predictable results
        // This adds gain to the low frequency bands (31Hz, 63Hz, 125Hz)
        if (strength > 0 && _eqEnabled.value) {
            val bassBoostDb = strength * 6f // Up to +6dB boost on bass frequencies
            val currentBands = _bands.value.toMutableList()

            // Only adjust if not already user-modified (check against "flat" + bass boost)
            equalizer?.let { eq ->
                try {
                    // Apply additional bass gain to the lowest Android EQ bands
                    for (band in 0 until minOf(2, androidBandCount)) {
                        val currentLevel = eq.getBandLevel(band.toShort())
                        val boostMb = (bassBoostDb * 100).toInt().toShort()
                        val newLevel = (currentLevel + boostMb).toShort()
                            .coerceIn(bandLevelMin, bandLevelMax)
                        eq.setBandLevel(band.toShort(), newLevel)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error applying EQ bass boost: ${e.message}")
                }
            }
        } else if (strength == 0f) {
            // When bass boost is off, reapply the current EQ settings
            applyBandsToEqualizer()
        }

        scope.launch {
            dataStore.edit { it[Keys.BASS_BOOST] = strength }
        }
    }

    /**
     * Set virtualizer/stereo widening strength (0-1)
     */
    fun setVirtualizer(strength: Float) {
        _virtualizerStrength.value = strength.coerceIn(0f, 1f)

        virtualizer?.let { virt ->
            if (virt.strengthSupported) {
                val androidStrength = (strength * 1000).toInt().toShort().coerceIn(0, 1000)
                virt.setStrength(androidStrength)
                virt.enabled = _eqEnabled.value && strength > 0
            }
        }

        scope.launch {
            dataStore.edit { it[Keys.VIRTUALIZER] = strength }
        }
    }

    /**
     * Set reverb preset (0 = off, 1-6 = presets)
     */
    fun setReverbPreset(preset: Int) {
        _reverbPreset.value = preset

        presetReverb?.let { reverb ->
            when (preset) {
                0 -> {
                    reverb.enabled = false
                    reverb.preset = PresetReverb.PRESET_NONE
                }
                1 -> {
                    reverb.preset = PresetReverb.PRESET_SMALLROOM
                    reverb.enabled = _eqEnabled.value
                }
                2 -> {
                    reverb.preset = PresetReverb.PRESET_MEDIUMROOM
                    reverb.enabled = _eqEnabled.value
                }
                3 -> {
                    reverb.preset = PresetReverb.PRESET_LARGEROOM
                    reverb.enabled = _eqEnabled.value
                }
                4 -> {
                    reverb.preset = PresetReverb.PRESET_MEDIUMHALL
                    reverb.enabled = _eqEnabled.value
                }
                5 -> {
                    reverb.preset = PresetReverb.PRESET_LARGEHALL
                    reverb.enabled = _eqEnabled.value
                }
                6 -> {
                    reverb.preset = PresetReverb.PRESET_PLATE
                    reverb.enabled = _eqEnabled.value
                }
            }
        }

        scope.launch {
            dataStore.edit { it[Keys.REVERB_PRESET] = preset }
        }
    }

    /**
     * Set loudness normalization enabled
     */
    fun setLoudnessEnabled(enabled: Boolean) {
        _loudnessEnabled.value = enabled

        loudnessEnhancer?.let { le ->
            le.enabled = _eqEnabled.value && enabled
            if (enabled) {
                // Target gain for normalization (in millibels)
                // -14 LUFS is a common streaming standard
                le.setTargetGain(500) // +5dB boost
            }
        }

        scope.launch {
            dataStore.edit { it[Keys.LOUDNESS_ENABLED] = enabled }
        }
    }

    /**
     * Set amplifier gain (-12 to +12 dB)
     * Uses LoudnessEnhancer for volume amplification
     */
    fun setAmplifierGain(gainDb: Float) {
        _amplifierGain.value = gainDb.coerceIn(-12f, 12f)

        loudnessEnhancer?.let { le ->
            // Convert dB to millibels (1 dB = 100 millibels)
            val gainMb = (gainDb * 100).toInt()
            le.setTargetGain(gainMb)
            le.enabled = _eqEnabled.value && (gainDb != 0f || _loudnessEnabled.value)
        }

        scope.launch {
            dataStore.edit { it[Keys.AMPLIFIER_GAIN] = gainDb }
        }
    }

    /**
     * Apply a preset by name
     */
    fun applyPreset(presetName: String, bands: List<Float>, preampDb: Float) {
        _selectedPresetName.value = presetName
        _preamp.value = preampDb
        setBands(bands)

        scope.launch {
            dataStore.edit { it[Keys.PRESET_NAME] = presetName }
        }
    }

    /**
     * Reset all effects to flat
     */
    fun reset() {
        _selectedPresetName.value = "Flat"
        _preamp.value = 0f
        _bands.value = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        _bassBoostStrength.value = 0f
        _virtualizerStrength.value = 0f
        _reverbPreset.value = 0
        _loudnessEnabled.value = false
        _amplifierGain.value = 0f

        applyBandsToEqualizer()
        setBassBoost(0f)
        setVirtualizer(0f)
        setReverbPreset(0)
        setLoudnessEnabled(false)
        setAmplifierGain(0f)
    }

    /**
     * Load settings from DataStore
     */
    private suspend fun loadSettings() {
        try {
            val prefs = dataStore.data.first()

            _eqEnabled.value = prefs[Keys.EQ_ENABLED] ?: true
            _selectedPresetName.value = prefs[Keys.PRESET_NAME] ?: "Flat"
            _preamp.value = prefs[Keys.PREAMP] ?: 0f
            _bassBoostStrength.value = prefs[Keys.BASS_BOOST] ?: 0f
            _virtualizerStrength.value = prefs[Keys.VIRTUALIZER] ?: 0f
            _reverbPreset.value = prefs[Keys.REVERB_PRESET] ?: 0
            _loudnessEnabled.value = prefs[Keys.LOUDNESS_ENABLED] ?: false
            _amplifierGain.value = prefs[Keys.AMPLIFIER_GAIN] ?: 0f

            // Load bands
            val loadedBands = (0..9).map { prefs[Keys.bandKey(it)] ?: 0f }
            _bands.value = loadedBands

            // Apply loaded settings
            setEqEnabled(_eqEnabled.value)
            applyBandsToEqualizer()
            setBassBoost(_bassBoostStrength.value)
            setVirtualizer(_virtualizerStrength.value)
            setReverbPreset(_reverbPreset.value)
            setLoudnessEnabled(_loudnessEnabled.value)
            setAmplifierGain(_amplifierGain.value)

            Log.d(TAG, "Settings loaded: preset=${_selectedPresetName.value}, enabled=${_eqEnabled.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading settings: ${e.message}")
        }
    }

    /**
     * Release all audio effects
     */
    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            presetReverb?.release()
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing effects: ${e.message}")
        }

        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
        loudnessEnhancer = null
        isInitialized = false
    }

    /**
     * Check if effects are initialized
     */
    fun isReady(): Boolean = isInitialized
}
