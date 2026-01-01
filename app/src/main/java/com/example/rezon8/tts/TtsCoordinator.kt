package com.mossglen.lithos.tts

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TtsCoordinator - Central manager for all TTS operations.
 *
 * Handles switching between:
 * - System TTS (Android's built-in TextToSpeech)
 * - Native Kokoro (High-quality Sherpa-ONNX streaming)
 *
 * This is the main entry point for the UI. It provides a unified API
 * regardless of which engine is active.
 */
@Singleton
class TtsCoordinator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TtsCoordinator"
    }

    /**
     * Available TTS engines.
     */
    enum class Engine {
        SYSTEM,  // Android System TTS
        KOKORO   // Native Sherpa-ONNX Kokoro
    }

    /**
     * Coordinator state for UI.
     */
    sealed class CoordinatorState {
        object Idle : CoordinatorState()
        object Initializing : CoordinatorState()
        object Ready : CoordinatorState()
        object Speaking : CoordinatorState()
        object Paused : CoordinatorState()
        data class Error(val message: String) : CoordinatorState()
    }

    // Sub-components
    val modelAssetManager = ModelAssetManager(context)
    val voiceRepository = VoiceRepository(context)
    private val systemTtsEngine = SystemTtsEngine()
    private val lithosEngine = LithosStreamingEngine(context, modelAssetManager)

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // State
    private val _currentEngine = MutableStateFlow(Engine.SYSTEM)
    val currentEngine: StateFlow<Engine> = _currentEngine.asStateFlow()

    private val _state = MutableStateFlow<CoordinatorState>(CoordinatorState.Idle)
    val state: StateFlow<CoordinatorState> = _state.asStateFlow()

    private val _isKokoroReady = MutableStateFlow(false)
    val isKokoroReady: StateFlow<Boolean> = _isKokoroReady.asStateFlow()

    // Current sentence for highlighting
    val currentSentenceIndex: StateFlow<Int> = lithosEngine.currentSentenceIndex

    // Track current text for System TTS resume (System TTS can't pause, must re-speak)
    private var currentTextForResume: String = ""
    private var currentSentenceIndexForResume: Int = 0

    // Callbacks (forwarded from active engine)
    var onSentenceStart: ((Int) -> Unit)? = null
        set(value) {
            field = value
            lithosEngine.onSentenceStart = value
        }

    var onSentenceComplete: ((Int) -> Unit)? = null
        set(value) {
            field = value
            lithosEngine.onSentenceComplete = value
        }

    // External callback for playback completion - invoked by internal callbacks in init block
    var onPlaybackComplete: (() -> Unit)? = null

    var onError: ((String) -> Unit)? = null
        set(value) {
            field = value
            lithosEngine.onError = value
            systemTtsEngine.onSpeechError = value
        }

    init {
        // Check Kokoro model status on init
        scope.launch {
            checkKokoroAvailability()
        }

        // Set up internal state management for playback completion
        // This ensures state updates even if no external callback is set
        lithosEngine.onPlaybackComplete = {
            _state.value = CoordinatorState.Ready
            onPlaybackComplete?.invoke()
        }
        systemTtsEngine.onSpeechDone = {
            _state.value = CoordinatorState.Ready
            onPlaybackComplete?.invoke()
        }
    }

    /**
     * Check if TTS is ready to speak.
     */
    fun isReady(): Boolean {
        return _state.value == CoordinatorState.Ready &&
            (_currentEngine.value == Engine.SYSTEM || _isKokoroReady.value)
    }

    /**
     * Initialize the TTS system.
     * Call this on app startup to prepare engines.
     * @return true if initialization succeeded
     */
    suspend fun initialize(): Boolean {
        _state.value = CoordinatorState.Initializing
        Log.d(TAG, "Initializing TTS Coordinator...")

        // Always initialize System TTS as fallback
        systemTtsEngine.initialize(
            context = context,
            onSuccess = { Log.d(TAG, "System TTS initialized successfully") },
            onError = { error -> Log.w(TAG, "System TTS init failed: $error") }
        )

        // Check if Kokoro model is available
        checkKokoroAvailability()

        // Initialize Kokoro if available
        if (_isKokoroReady.value && _currentEngine.value == Engine.KOKORO) {
            lithosEngine.initialize()
        }

        _state.value = CoordinatorState.Ready
        Log.d(TAG, "TTS Coordinator ready. Kokoro available: ${_isKokoroReady.value}")
        return true
    }

    /**
     * Check if Kokoro model is extracted and ready.
     */
    private suspend fun checkKokoroAvailability() {
        val isReady = modelAssetManager.isModelReady()
        _isKokoroReady.value = isReady
        Log.d(TAG, "Kokoro model ready: $isReady")
    }

    /**
     * Initialize the Kokoro engine.
     * Call this when user wants to use Kokoro TTS.
     */
    suspend fun initializeKokoro(): Boolean {
        if (_isKokoroReady.value) {
            return lithosEngine.initialize()
        }

        // Need to extract model first
        _state.value = CoordinatorState.Initializing
        val extracted = modelAssetManager.ensureModelExtracted()

        if (extracted) {
            _isKokoroReady.value = true
            return lithosEngine.initialize()
        }

        return false
    }

    /**
     * Switch to a different TTS engine.
     */
    fun switchEngine(engine: Engine) {
        if (engine == _currentEngine.value) return

        // Stop current playback
        stop()

        _currentEngine.value = engine
        Log.d(TAG, "Switched to engine: $engine")
    }

    /**
     * Speak text using the current engine.
     *
     * @param text Text to speak
     * @param sentences Pre-split sentences (for Kokoro highlighting)
     */
    fun speak(text: String, sentences: List<String> = emptyList()) {
        // Track for resume (System TTS needs this)
        currentTextForResume = text
        currentSentenceIndexForResume = 0

        _state.value = CoordinatorState.Speaking

        when (_currentEngine.value) {
            Engine.SYSTEM -> {
                systemTtsEngine.speak(text)
            }
            Engine.KOKORO -> {
                val voiceId = voiceRepository.selectedVoiceId.value
                val speed = voiceRepository.playbackSpeed.value
                lithosEngine.speak(text, voiceId, speed)
            }
        }
    }

    /**
     * Speak a single sentence.
     * For Kokoro, this is more efficient than full text.
     */
    fun speakSentence(sentence: String) {
        speak(sentence)
    }

    /**
     * Jump to a specific sentence (Kokoro only).
     * Performs atomic flush for instant response.
     */
    fun jumpToSentence(index: Int) {
        if (_currentEngine.value == Engine.KOKORO) {
            lithosEngine.jumpToSentence(index)
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * UNIFIED TOGGLE PLAYBACK (v7 State Machine)
     * - If Speaking: pause/stop everything
     * - If Paused/Ready: resume specific to current engine
     * ═══════════════════════════════════════════════════════════════════════════
     */
    fun togglePlayback() {
        Log.d(TAG, "togglePlayback: state=${_state.value}, engine=${_currentEngine.value}")

        when (_state.value) {
            CoordinatorState.Speaking -> {
                // PAUSE: Stop EVERYTHING immediately
                systemTtsEngine.stop()  // Always stop System TTS (it's a black box)
                lithosEngine.pause()     // Pause Kokoro (keeps buffer for warm resume)
                _state.value = CoordinatorState.Paused
                Log.d(TAG, "Toggled to PAUSED")
            }
            CoordinatorState.Paused, CoordinatorState.Ready -> {
                // RESUME: Engine-specific logic
                when (_currentEngine.value) {
                    Engine.SYSTEM -> {
                        // System TTS can't resume mid-sentence - must re-speak
                        if (currentTextForResume.isNotEmpty()) {
                            systemTtsEngine.speak(currentTextForResume)
                            _state.value = CoordinatorState.Speaking
                            Log.d(TAG, "Resumed System TTS (re-speak)")
                        }
                    }
                    Engine.KOKORO -> {
                        // Kokoro can warm-resume or cold-resume from current sentence
                        lithosEngine.resume()  // Handles queue empty check internally
                        _state.value = CoordinatorState.Speaking
                        Log.d(TAG, "Resumed Kokoro")
                    }
                }
            }
            else -> {
                Log.d(TAG, "togglePlayback: No action for state ${_state.value}")
            }
        }
    }

    /**
     * Stop playback completely (not pause).
     */
    fun stop() {
        // Stop BOTH engines to prevent runaway audio
        systemTtsEngine.stop()
        lithosEngine.stop()
        currentTextForResume = ""
        _state.value = CoordinatorState.Ready
        Log.d(TAG, "Stopped all playback")
    }

    /**
     * Pause playback.
     * - System TTS: must stop() (can't pause mid-sentence)
     * - Kokoro: warm pause (keeps buffer)
     */
    fun pause() {
        when (_currentEngine.value) {
            Engine.SYSTEM -> {
                // System TTS is a black box - must stop, can't pause
                systemTtsEngine.stop()
                _state.value = CoordinatorState.Paused
                Log.d(TAG, "System TTS stopped (no pause support)")
            }
            Engine.KOKORO -> {
                lithosEngine.pause()
                _state.value = CoordinatorState.Paused
                Log.d(TAG, "Kokoro paused")
            }
        }
    }

    /**
     * Resume playback.
     * - System TTS: re-speak from beginning (can't resume)
     * - Kokoro: warm or cold resume
     */
    fun resume() {
        when (_currentEngine.value) {
            Engine.SYSTEM -> {
                // System TTS can't resume - must re-speak
                if (currentTextForResume.isNotEmpty()) {
                    systemTtsEngine.speak(currentTextForResume)
                    _state.value = CoordinatorState.Speaking
                    Log.d(TAG, "System TTS resumed (re-speak)")
                }
            }
            Engine.KOKORO -> {
                lithosEngine.resume()
                _state.value = CoordinatorState.Speaking
                Log.d(TAG, "Kokoro resumed")
            }
        }
    }

    /**
     * Check if currently speaking.
     */
    fun isSpeaking(): Boolean {
        return when (_currentEngine.value) {
            Engine.SYSTEM -> systemTtsEngine.isSpeaking()
            Engine.KOKORO -> lithosEngine.isSpeaking()
        }
    }

    /**
     * Set playback speed.
     */
    fun setSpeed(speed: Float) {
        voiceRepository.setPlaybackSpeed(speed)
        when (_currentEngine.value) {
            Engine.SYSTEM -> systemTtsEngine.setSpeed(speed)
            Engine.KOKORO -> lithosEngine.setSpeed(speed)
        }
    }

    /**
     * Set Kokoro voice.
     */
    fun setVoice(voiceId: Int) {
        voiceRepository.setSelectedVoice(voiceId)
        lithosEngine.setVoice(voiceId)
    }

    /**
     * Get System TTS voices.
     */
    fun getSystemVoices(): List<android.speech.tts.Voice> {
        return systemTtsEngine.getAvailableVoices()
    }

    /**
     * Set System TTS voice.
     */
    fun setSystemVoice(voice: android.speech.tts.Voice) {
        systemTtsEngine.setVoice(voice)
    }

    /**
     * Get Kokoro voices.
     */
    fun getKokoroVoices(): List<VoiceRepository.Voice> {
        return voiceRepository.getAllVoices()
    }

    /**
     * Get current Kokoro voice.
     */
    fun getCurrentKokoroVoice(): VoiceRepository.Voice {
        return voiceRepository.getSelectedVoice()
    }

    /**
     * Release all resources.
     */
    fun release() {
        systemTtsEngine.release()
        lithosEngine.release()
        Log.d(TAG, "TTS Coordinator released")
    }

    /**
     * Get extraction state for loading UI.
     */
    val extractionState = modelAssetManager.extractionState

    /**
     * Get Lithos engine state.
     */
    val lithosState = lithosEngine.state
}
