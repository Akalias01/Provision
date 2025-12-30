package com.mossglen.reverie.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mossglen.reverie.data.SettingsRepository
import com.mossglen.reverie.tts.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for TTS functionality.
 * Provides UI state for TTS engine management and speech controls.
 *
 * Uses VoiceModelManager for downloading Kokoro TTS model with:
 * - Remote configuration fetching
 * - HTTP download with progress
 * - .tar.bz2 extraction
 *
 * Persists user preferences for engine type and voice selection.
 */
@HiltViewModel
class TtsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsRepository: TtsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    companion object {
        private const val TAG = "TtsViewModel"
    }

    // ===== TTS State from Repository =====
    val isReady: StateFlow<Boolean> = ttsRepository.isReady
    val isSpeaking: StateFlow<Boolean> = ttsRepository.isSpeaking
    val isLoading: StateFlow<Boolean> = ttsRepository.isLoading
    val error: StateFlow<String?> = ttsRepository.error
    val currentEngineType: StateFlow<TtsEngineType> = ttsRepository.currentEngineType
    val settings: StateFlow<TtsSettings> = ttsRepository.settings
    val kokoroModelReady: StateFlow<Boolean> = ttsRepository.kokoroModelReady

    // ===== Model Download State (from VoiceModelManager via Repository) =====
    val voiceSetupState: StateFlow<VoiceModelManager.SetupState> = ttsRepository.voiceSetupState

    // ===== UI Dialog States =====
    private val _showEngineDialog = MutableStateFlow(false)
    val showEngineDialog: StateFlow<Boolean> = _showEngineDialog.asStateFlow()

    private val _showSpeedDialog = MutableStateFlow(false)
    val showSpeedDialog: StateFlow<Boolean> = _showSpeedDialog.asStateFlow()

    init {
        // Restore saved TTS settings and initialize engine
        viewModelScope.launch {
            val savedEngineType = settingsRepository.getTtsEngineType()
            val savedVoiceId = settingsRepository.getTtsVoiceId()

            Log.d(TAG, "Restoring TTS settings: engine=$savedEngineType, voiceId=$savedVoiceId")

            // Determine engine type
            val engineType = when (savedEngineType) {
                "kokoro" -> {
                    // Only use Kokoro if model is ready
                    if (ttsRepository.kokoroModelReady.value) {
                        TtsEngineType.SHERPA
                    } else {
                        Log.d(TAG, "Kokoro model not ready, falling back to System TTS")
                        TtsEngineType.SYSTEM
                    }
                }
                else -> TtsEngineType.SYSTEM
            }

            // Initialize with the saved engine
            ttsRepository.initialize(
                engineType = engineType,
                onSuccess = {
                    // After initialization, set the voice ID
                    if (savedVoiceId > 0) {
                        ttsRepository.setVoiceId(savedVoiceId)
                    }
                }
            )
        }
    }

    // ===== Engine Control =====

    /**
     * Initialize TTS with specified engine.
     */
    fun initialize(engineType: TtsEngineType = TtsEngineType.SYSTEM) {
        ttsRepository.initialize(engineType)
    }

    /**
     * Switch TTS engine and persist the preference.
     * @param useHighQuality true = Kokoro AI, false = System TTS
     */
    fun switchEngine(useHighQuality: Boolean) {
        Log.w(TAG, ">>> switchEngine called: useHighQuality=$useHighQuality")
        ttsRepository.switchEngine(
            useHighQuality = useHighQuality,
            onSuccess = {
                Log.w(TAG, ">>> switchEngine SUCCESS")
                // Persist the engine preference
                viewModelScope.launch {
                    val engineType = if (useHighQuality) "kokoro" else "system"
                    settingsRepository.setTtsEngineType(engineType)
                    Log.d(TAG, "Saved engine preference: $engineType")
                }
            },
            onError = { error ->
                Log.e(TAG, ">>> switchEngine ERROR: $error")
            }
        )
    }

    /**
     * Toggle between System and Kokoro TTS.
     */
    fun toggleEngine() {
        val currentlyUsingKokoro = currentEngineType.value == TtsEngineType.SHERPA
        switchEngine(!currentlyUsingKokoro)
    }

    // ===== Speech Control =====

    /**
     * Speak the given text.
     * If TTS is not ready, automatically re-initialize with System TTS first.
     */
    fun speak(text: String) {
        android.util.Log.d("TtsViewModel", "speak() called - isReady=${isReady.value}, text='${text.take(50)}...'")
        if (!isReady.value) {
            android.util.Log.d("TtsViewModel", "TTS not ready, reinitializing...")
            // TTS not ready - reinitialize with System TTS then speak
            ttsRepository.initialize(
                engineType = currentEngineType.value,
                onSuccess = {
                    android.util.Log.d("TtsViewModel", "TTS reinitialized, now speaking...")
                    ttsRepository.speak(text)
                },
                onError = { error ->
                    android.util.Log.e("TtsViewModel", "TTS init failed: $error, trying System TTS fallback")
                    // Failed to initialize, try System TTS as fallback
                    ttsRepository.initialize(
                        engineType = TtsEngineType.SYSTEM,
                        onSuccess = { ttsRepository.speak(text) },
                        onError = { e -> android.util.Log.e("TtsViewModel", "System TTS fallback also failed: $e") }
                    )
                }
            )
        } else {
            android.util.Log.d("TtsViewModel", "TTS ready, speaking directly...")
            ttsRepository.speak(text)
        }
    }

    /**
     * Stop speech.
     */
    fun stop() {
        ttsRepository.stop()
    }

    /**
     * Set speech speed.
     */
    fun setSpeed(rate: Float) {
        ttsRepository.setSpeed(rate)
    }

    /**
     * Set voice ID (for Kokoro multi-speaker) and persist the preference.
     */
    fun setVoiceId(id: Int) {
        ttsRepository.setVoiceId(id)
        // Persist the voice preference
        viewModelScope.launch {
            settingsRepository.setTtsVoiceId(id)
            Log.d(TAG, "Saved voice preference: $id")
        }
    }

    /**
     * Get the saved voice ID from settings.
     * This is a Flow that can be collected in UI.
     */
    val savedVoiceId: StateFlow<Int> = settingsRepository.ttsVoiceId
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    /**
     * Preview a voice by speaking a short sample.
     * This plays a short audio clip so the user can hear the voice before selecting.
     * @param voiceId The voice ID to preview
     * @param onComplete Callback when preview finishes
     */
    fun previewVoice(voiceId: Int, onComplete: () -> Unit = {}) {
        ttsRepository.previewVoice(voiceId, onComplete = onComplete)
    }

    /**
     * Set the callback to get the next sentence for pre-buffering.
     * This enables Kokoro TTS to generate the next sentence WHILE playing the current one,
     * reducing the perceived latency between sentences.
     */
    fun setNextSentenceProvider(provider: (() -> String?)?) {
        ttsRepository.nextSentenceProvider = provider
    }

    /**
     * Set the callback to get cached audio for a sentence.
     * This enables instant playback from pre-generated audio.
     */
    fun setCachedAudioProvider(provider: ((String) -> FloatArray?)?) {
        ttsRepository.cachedAudioProvider = provider
    }

    // ===== Audio Pre-Generation (for instant playback) =====

    /**
     * Audio cache generation state.
     */
    val audioGenerationState: StateFlow<TtsAudioCache.GenerationState> = ttsRepository.audioCache.generationState

    /**
     * Check if book has pre-generated audio.
     */
    fun hasPreGeneratedAudio(bookId: String): Boolean {
        return ttsRepository.audioCache.hasCachedAudio(bookId)
    }

    /**
     * Get the voice ID used for pre-generated audio.
     */
    fun getPreGeneratedVoiceId(bookId: String): Int? {
        return ttsRepository.audioCache.getCachedVoiceId(bookId)
    }

    /**
     * Pre-generate audio for a book.
     * This generates TTS audio for all sentences ahead of time,
     * enabling instant lag-free playback.
     *
     * @param bookId Unique identifier for the book
     * @param sentences List of sentences to generate
     * @param voiceId Voice ID to use
     * @param onProgress Progress callback (0.0 to 1.0)
     * @param onComplete Callback when generation completes
     * @param onError Callback when an error occurs
     */
    fun preGenerateBookAudio(
        bookId: String,
        sentences: List<String>,
        voiceId: Int = 0,
        onProgress: (Float) -> Unit = {},
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // Ensure Kokoro engine is initialized
        val engine = ttsRepository.getSherpaEngine()
        if (engine == null) {
            // Initialize Kokoro first, then generate
            ttsRepository.initialize(
                engineType = TtsEngineType.SHERPA,
                onSuccess = {
                    val newEngine = ttsRepository.getSherpaEngine()
                    if (newEngine != null) {
                        ttsRepository.audioCache.generateAudioForBook(
                            bookId = bookId,
                            sentences = sentences,
                            voiceId = voiceId,
                            ttsEngine = newEngine,
                            onProgress = onProgress,
                            onComplete = onComplete,
                            onError = onError
                        )
                    } else {
                        onError("Failed to get Kokoro engine after initialization")
                    }
                },
                onError = { error ->
                    onError("Failed to initialize Kokoro: $error")
                }
            )
        } else {
            ttsRepository.audioCache.generateAudioForBook(
                bookId = bookId,
                sentences = sentences,
                voiceId = voiceId,
                ttsEngine = engine,
                onProgress = onProgress,
                onComplete = onComplete,
                onError = onError
            )
        }
    }

    /**
     * Cancel ongoing audio pre-generation.
     */
    fun cancelPreGeneration() {
        ttsRepository.audioCache.cancelGeneration()
    }

    /**
     * Delete pre-generated audio for a book.
     */
    fun deletePreGeneratedAudio(bookId: String) {
        ttsRepository.audioCache.deleteCachedAudio(bookId)
    }

    /**
     * Get the size of pre-generated audio for a book (in bytes).
     */
    fun getPreGeneratedAudioSize(bookId: String): Long {
        return ttsRepository.audioCache.getCacheSize(bookId)
    }

    /**
     * Get cached audio for a specific sentence.
     */
    fun getCachedAudio(bookId: String, sentenceIndex: Int): FloatArray? {
        return ttsRepository.audioCache.getCachedAudio(bookId, sentenceIndex)
    }

    /**
     * Get total cache size for all books.
     */
    fun getTotalCacheSize(): Long {
        return ttsRepository.audioCache.getTotalCacheSize()
    }

    /**
     * Clear all cached audio.
     */
    fun clearAllCache() {
        ttsRepository.audioCache.clearAllCache()
    }

    // ===== Model Download (via VoiceModelManager) =====

    /**
     * Download Kokoro model using remote configuration.
     * Uses VoiceModelManager which:
     * 1. Fetches config from remote URL
     * 2. Downloads .tar.bz2 model file
     * 3. Extracts to filesDir/kokoro/
     */
    fun downloadKokoroModel() {
        android.util.Log.d("TtsViewModel", "downloadKokoroModel() called - starting download...")
        viewModelScope.launch {
            android.util.Log.d("TtsViewModel", "Inside coroutine, calling initializeHighQualityTts()")
            ttsRepository.initializeHighQualityTts(
                onProgress = { state ->
                    android.util.Log.d("TtsViewModel", "Download progress: $state")
                },
                onSuccess = {
                    android.util.Log.d("TtsViewModel", "Kokoro download SUCCESS!")
                },
                onError = { error ->
                    android.util.Log.e("TtsViewModel", "Kokoro download ERROR: $error")
                }
            )
        }
    }

    /**
     * Cancel ongoing model download.
     */
    fun cancelDownload() {
        ttsRepository.cancelVoiceSetup()
    }

    /**
     * Delete downloaded Kokoro model.
     */
    fun deleteKokoroModel() {
        ttsRepository.deleteKokoroModel()

        // Switch to System TTS if currently using Kokoro
        if (currentEngineType.value == TtsEngineType.SHERPA) {
            switchEngine(false)
        }
    }

    /**
     * Reset download state.
     */
    fun resetDownloadState() {
        ttsRepository.resetVoiceSetupState()
    }

    // ===== UI Helpers =====

    /**
     * Refresh Kokoro model status (check if installed).
     * Call this when opening TTS settings to ensure status is current.
     */
    fun refreshKokoroStatus() {
        ttsRepository.refreshKokoroModelStatus()
    }

    /**
     * Get current engine name for display.
     */
    fun getEngineName(): String = ttsRepository.getEngineName()

    /**
     * Show/hide engine selection dialog.
     */
    fun showEngineDialog(show: Boolean) {
        _showEngineDialog.value = show
    }

    /**
     * Show/hide speed selection dialog.
     */
    fun showSpeedDialog(show: Boolean) {
        _showSpeedDialog.value = show
    }

    override fun onCleared() {
        super.onCleared()
        // DON'T release - TtsRepository is a singleton and should persist across ViewModels
        // ttsRepository.release()
        ttsRepository.stop() // Just stop any active speech
    }
}

/**
 * TTS Speed presets for UI.
 */
object TtsSpeedPresets {
    val speeds = listOf(
        0.5f to "0.5x (Slow)",
        0.75f to "0.75x",
        1.0f to "1.0x (Normal)",
        1.25f to "1.25x",
        1.5f to "1.5x",
        1.75f to "1.75x",
        2.0f to "2.0x (Fast)"
    )
}
