package com.mossglen.reverie.tts

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TTS Repository - Singleton manager for TTS engine lifecycle and voice model setup.
 *
 * Features:
 * - Manages switching between System TTS and Sherpa-ONNX (Kokoro)
 * - Handles remote configuration fetching for model downloads
 * - Provides StateFlows for UI observation
 * - Non-blocking initialization with coroutines
 */
@Singleton
class TtsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TtsRepository"
    }

    // Coroutine scope - recreated if cancelled
    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private fun ensureScope(): CoroutineScope {
        if (!scope.isActive) {
            Log.d(TAG, "Recreating cancelled scope")
            scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        }
        return scope
    }

    // Voice model manager for downloading
    private val voiceModelManager = VoiceModelManager(context)

    // Audio cache for pre-generated TTS audio
    val audioCache = TtsAudioCache(context)

    // Current TTS engine
    private var currentEngine: AudiobookTTS? = null

    // ===== State Flows =====

    private val _currentEngineType = MutableStateFlow(TtsEngineType.SYSTEM)
    val currentEngineType: StateFlow<TtsEngineType> = _currentEngineType.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _settings = MutableStateFlow(TtsSettings())
    val settings: StateFlow<TtsSettings> = _settings.asStateFlow()

    // Model installation state
    private val _kokoroModelReady = MutableStateFlow(false)
    val kokoroModelReady: StateFlow<Boolean> = _kokoroModelReady.asStateFlow()

    // Voice setup state (from VoiceModelManager)
    val voiceSetupState: StateFlow<VoiceModelManager.SetupState> = voiceModelManager.setupState

    init {
        refreshKokoroModelStatus()
    }

    // ===== Voice Setup (Fetch -> Download -> Install) =====

    /**
     * Start the complete voice setup process.
     * Fetches config from remote URL, downloads model, and installs it.
     *
     * @param configUrl Override the default config URL (optional)
     * @param onComplete Callback when setup completes
     * @param onError Callback when an error occurs
     */
    fun startVoiceSetup(
        configUrl: String = VoiceModelManager.CONFIG_URL,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        ensureScope().launch {
            Log.d(TAG, "Starting voice setup...")

            voiceModelManager.startVoiceSetup(
                configUrl = configUrl,
                onComplete = {
                    refreshKokoroModelStatus()
                    Log.d(TAG, "Voice setup completed successfully")
                    onComplete()
                },
                onError = { errorMsg ->
                    Log.e(TAG, "Voice setup failed: $errorMsg")
                    onError(errorMsg)
                }
            )
        }
    }

    /**
     * Cancel ongoing voice setup.
     */
    fun cancelVoiceSetup() {
        voiceModelManager.cancel()
    }

    /**
     * Reset voice setup state.
     */
    fun resetVoiceSetupState() {
        voiceModelManager.reset()
    }

    /**
     * Delete installed Kokoro model.
     */
    fun deleteKokoroModel() {
        voiceModelManager.deleteInstalledModel()
        refreshKokoroModelStatus()

        // Switch to System TTS if currently using Kokoro
        if (_currentEngineType.value == TtsEngineType.SHERPA) {
            initialize(TtsEngineType.SYSTEM)
        }
    }

    /**
     * Get installed model size in bytes.
     */
    fun getKokoroModelSize(): Long {
        return voiceModelManager.getInstalledModelSize()
    }

    // ===== Engine Initialization =====

    /**
     * Initialize TTS with specified engine.
     */
    fun initialize(
        engineType: TtsEngineType = TtsEngineType.SYSTEM,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        ensureScope().launch {
            Log.w(TAG, "=== INITIALIZE TTS: $engineType ===")

            _isLoading.value = true
            _error.value = null

            // Release current engine
            currentEngine?.release()
            currentEngine = null
            _isReady.value = false

            // Create new engine
            val engine: AudiobookTTS = when (engineType) {
                TtsEngineType.SYSTEM -> SystemTtsEngine().also { setupSystemCallbacks(it) }
                TtsEngineType.SHERPA -> SherpaTtsEngine().also { setupSherpaCallbacks(it) }
            }

            // Initialize
            engine.initialize(
                context = context,
                onSuccess = {
                    currentEngine = engine
                    _currentEngineType.value = engineType
                    _isReady.value = true
                    _isLoading.value = false
                    applySettings()
                    Log.d(TAG, "TTS ready: ${engine.getEngineName()}")
                    onSuccess()
                },
                onError = { errorMsg ->
                    _isLoading.value = false
                    _error.value = errorMsg
                    Log.e(TAG, "TTS init failed: $errorMsg")

                    // If Sherpa failed, fallback to System TTS automatically
                    if (engineType == TtsEngineType.SHERPA) {
                        Log.d(TAG, "Sherpa failed, falling back to System TTS")
                        val systemEngine = SystemTtsEngine().also { setupSystemCallbacks(it) }
                        systemEngine.initialize(
                            context = context,
                            onSuccess = {
                                currentEngine = systemEngine
                                _currentEngineType.value = TtsEngineType.SYSTEM
                                _isReady.value = true
                                Log.d(TAG, "Fallback to System TTS successful")
                            },
                            onError = { fallbackError ->
                                Log.e(TAG, "System TTS fallback also failed: $fallbackError")
                            }
                        )
                    }
                    onError(errorMsg)
                }
            )
        }
    }

    /**
     * Switch between TTS engines.
     * @param useHighQuality true = Kokoro AI, false = System TTS
     */
    fun switchEngine(
        useHighQuality: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        Log.w(TAG, "========================================")
        Log.w(TAG, ">>> SWITCH ENGINE CALLED <<<")
        Log.w(TAG, "useHighQuality=$useHighQuality")
        Log.w(TAG, "========================================")

        val targetEngine = if (useHighQuality) TtsEngineType.SHERPA else TtsEngineType.SYSTEM
        Log.w(TAG, "Target engine: $targetEngine")
        Log.w(TAG, "Current engine: ${_currentEngineType.value}")
        Log.w(TAG, "isReady: ${_isReady.value}")

        if (targetEngine == _currentEngineType.value && _isReady.value) {
            Log.w(TAG, "Already using $targetEngine and ready - returning success")
            onSuccess()
            return
        }

        // Check model for Kokoro
        val modelReady = SherpaTtsEngine.isModelReady(context)
        Log.w(TAG, "Kokoro model ready check: $modelReady")

        if (useHighQuality && !modelReady) {
            val errorMsg = "Kokoro model not installed. Run startVoiceSetup() first."
            _error.value = errorMsg
            Log.e(TAG, errorMsg)
            onError(errorMsg)
            return
        }

        // Check library availability before initializing
        val libraryAvailable = SherpaTtsEngine.isSherpaLibraryAvailable()
        Log.w(TAG, "Sherpa library available: $libraryAvailable")

        Log.w(TAG, ">>> PROCEEDING TO INITIALIZE $targetEngine <<<")
        initialize(targetEngine, onSuccess, onError)
    }

    /**
     * Smart initialization: Checks if model exists, downloads if needed, then initializes.
     *
     * Flow:
     * 1. Check if Kokoro model files exist
     * 2. If not, fetch remote config -> download model -> install
     * 3. Initialize the Sherpa TTS engine
     *
     * @param onProgress Callback for download/install progress
     * @param onSuccess Callback when engine is ready
     * @param onError Callback when an error occurs
     */
    fun initializeHighQualityTts(
        onProgress: (VoiceModelManager.SetupState) -> Unit = {},
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        ensureScope().launch {
            Log.d(TAG, "Initializing high-quality TTS...")

            // Check if model is already installed
            if (SherpaTtsEngine.isModelReady(context)) {
                Log.d(TAG, "Kokoro model already installed, initializing engine...")
                initialize(
                    engineType = TtsEngineType.SHERPA,
                    onSuccess = onSuccess,
                    onError = onError
                )
                return@launch
            }

            // Model not installed - start setup
            Log.d(TAG, "Kokoro model not found, starting download...")

            // Collect setup state for progress updates
            val progressJob = launch {
                voiceSetupState.collect { state ->
                    onProgress(state)
                }
            }

            voiceModelManager.startVoiceSetup(
                configUrl = VoiceModelManager.CONFIG_URL,
                onComplete = {
                    progressJob.cancel()
                    refreshKokoroModelStatus()
                    Log.d(TAG, "Model installed, initializing engine...")

                    // Initialize the engine after successful install
                    initialize(
                        engineType = TtsEngineType.SHERPA,
                        onSuccess = onSuccess,
                        onError = onError
                    )
                },
                onError = { errorMsg ->
                    progressJob.cancel()
                    Log.e(TAG, "High-quality TTS setup failed: $errorMsg")
                    onError(errorMsg)
                }
            )
        }
    }

    private fun setupSystemCallbacks(engine: SystemTtsEngine) {
        engine.onSpeechStart = { _isSpeaking.value = true }
        engine.onSpeechDone = { _isSpeaking.value = false }
        engine.onSpeechError = { _isSpeaking.value = false }
    }

    private fun setupSherpaCallbacks(engine: SherpaTtsEngine) {
        engine.onSpeechStart = { _isSpeaking.value = true }
        engine.onSpeechDone = { _isSpeaking.value = false }
        engine.onSpeechError = { _isSpeaking.value = false }
        engine.onRequestNextSentence = nextSentenceProvider
        engine.onRequestCachedAudio = cachedAudioProvider
    }

    // ===== Pre-buffering Support =====
    /**
     * Callback to get the next sentence for pre-buffering.
     * Set this from the UI layer (e.g., ReaderScreen) to enable reduced latency.
     */
    var nextSentenceProvider: (() -> String?)? = null
        set(value) {
            field = value
            // Update existing Sherpa engine if present
            (currentEngine as? SherpaTtsEngine)?.onRequestNextSentence = value
        }

    // ===== Cached Audio Support =====
    /**
     * Callback to get cached audio for a sentence.
     * Set this from the UI layer to enable instant playback from pre-generated audio.
     */
    var cachedAudioProvider: ((String) -> FloatArray?)? = null
        set(value) {
            field = value
            // Update existing Sherpa engine if present
            (currentEngine as? SherpaTtsEngine)?.onRequestCachedAudio = value
        }

    /**
     * Get the Sherpa TTS engine for audio generation.
     * Used by TtsAudioCache for pre-generating book audio.
     */
    fun getSherpaEngine(): SherpaTtsEngine? = currentEngine as? SherpaTtsEngine

    // ===== Speech Control =====

    fun speak(text: String) {
        if (!_isReady.value) {
            Log.w(TAG, "TTS not ready")
            return
        }
        currentEngine?.speak(text)
    }

    fun stop() {
        currentEngine?.stop()
        _isSpeaking.value = false
    }

    fun setSpeed(rate: Float) {
        _settings.value = _settings.value.copy(speed = rate)
        currentEngine?.setSpeed(rate)
    }

    fun setVoiceId(id: Int) {
        _settings.value = _settings.value.copy(voiceId = id)
        (currentEngine as? SherpaTtsEngine)?.setVoiceId(id)
    }

    /**
     * Preview a voice by speaking a short sample.
     * @param voiceId Voice ID to preview
     * @param previewText Text to speak (defaults to short preview)
     * @param onComplete Callback when preview finishes
     */
    fun previewVoice(
        voiceId: Int,
        previewText: String = KokoroVoices.PREVIEW_TEXT_SHORT,
        onComplete: () -> Unit = {}
    ) {
        val sherpaEngine = currentEngine as? SherpaTtsEngine
        if (sherpaEngine == null || !_isReady.value) {
            Log.w(TAG, "Cannot preview - Sherpa engine not ready")
            onComplete()
            return
        }

        // Stop any current playback
        stop()

        // Set voice for preview
        sherpaEngine.setVoiceId(voiceId)

        // Speak the preview (engine generates live audio)
        ensureScope().launch {
            // Wait a tiny bit for stop to complete
            kotlinx.coroutines.delay(100)

            // Hook into speech done callback
            val originalOnDone = sherpaEngine.onSpeechDone
            sherpaEngine.onSpeechDone = {
                _isSpeaking.value = false
                onComplete()
                // Restore original callback
                sherpaEngine.onSpeechDone = originalOnDone
            }

            sherpaEngine.speak(previewText)
        }
    }

    private fun applySettings() {
        currentEngine?.setSpeed(_settings.value.speed)
        (currentEngine as? SherpaTtsEngine)?.setVoiceId(_settings.value.voiceId)
        (currentEngine as? SystemTtsEngine)?.setPitch(_settings.value.pitch)
    }

    // ===== Utility =====

    fun refreshKokoroModelStatus() {
        val isReady = SherpaTtsEngine.isModelReady(context)
        val oldValue = _kokoroModelReady.value
        _kokoroModelReady.value = isReady
        // Use Log.w (warning level) to ensure visibility in logcat
        Log.w(TAG, "KOKORO CHECK: isReady=$isReady (was $oldValue)")
        Log.w(TAG, "External dir: ${context.getExternalFilesDir(null)?.absolutePath}")
    }

    fun getEngineName(): String = currentEngine?.getEngineName() ?: "Not initialized"

    fun isSpeaking(): Boolean = currentEngine?.isSpeaking() == true

    fun release() {
        Log.d(TAG, "Releasing TTS Repository...")
        currentEngine?.release()
        currentEngine = null
        audioCache.release()
        _isReady.value = false
        scope.cancel()
    }
}
