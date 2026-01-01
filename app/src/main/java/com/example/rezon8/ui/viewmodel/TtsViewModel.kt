package com.mossglen.lithos.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mossglen.lithos.data.SettingsRepository
import com.mossglen.lithos.tts.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TtsViewModel - ViewModel for TTS functionality using Native Streaming Architecture.
 *
 * NEW ARCHITECTURE (Lithos Streaming):
 * - Uses TtsCoordinator for unified engine management
 * - LithosStreamingEngine for native C++/JNI Sherpa-ONNX
 * - Producer-Consumer pipeline for zero-latency streaming
 * - Atomic flush for instant paragraph jumps
 *
 * NO MORE:
 * - Python/Chaquopy (too slow)
 * - File-based audio caching (we stream directly)
 * - Pre-generation (streaming is fast enough)
 */
@HiltViewModel
class TtsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsCoordinator: TtsCoordinator,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    companion object {
        private const val TAG = "TtsViewModel"
    }

    // ===== TTS State from Coordinator =====
    val state: StateFlow<TtsCoordinator.CoordinatorState> = ttsCoordinator.state
    val currentEngine: StateFlow<TtsCoordinator.Engine> = ttsCoordinator.currentEngine
    val isKokoroReady: StateFlow<Boolean> = ttsCoordinator.isKokoroReady
    val currentSentenceIndex: StateFlow<Int> = ttsCoordinator.currentSentenceIndex

    // Extraction state for loading UI
    val extractionState: StateFlow<ModelAssetManager.ExtractionState> = ttsCoordinator.extractionState

    // Lithos engine state
    val lithosState: StateFlow<LithosStreamingEngine.EngineState> = ttsCoordinator.lithosState

    // Voice repository for Kokoro voices
    val voices: List<VoiceRepository.Voice> get() = ttsCoordinator.getKokoroVoices()
    val selectedVoiceId: StateFlow<Int> = ttsCoordinator.voiceRepository.selectedVoiceId
    val playbackSpeed: StateFlow<Float> = ttsCoordinator.voiceRepository.playbackSpeed

    // ===== Derived State for UI =====
    val isReady: StateFlow<Boolean> = state.map { it is TtsCoordinator.CoordinatorState.Ready }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val isSpeaking: StateFlow<Boolean> = state.map { it is TtsCoordinator.CoordinatorState.Speaking }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val isPaused: StateFlow<Boolean> = state.map { it is TtsCoordinator.CoordinatorState.Paused }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val error: StateFlow<String?> = state.map { (it as? TtsCoordinator.CoordinatorState.Error)?.message }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Compatibility: Map engine to TtsEngineType for existing UI
    val currentEngineType: StateFlow<TtsEngineType> = currentEngine.map {
        when (it) {
            TtsCoordinator.Engine.SYSTEM -> TtsEngineType.SYSTEM
            TtsCoordinator.Engine.KOKORO -> TtsEngineType.SHERPA
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, TtsEngineType.SYSTEM)

    // Compatibility: kokoroModelReady (same as isKokoroReady)
    val kokoroModelReady: StateFlow<Boolean> = isKokoroReady

    // ===== UI Dialog States =====
    private val _showEngineDialog = MutableStateFlow(false)
    val showEngineDialog: StateFlow<Boolean> = _showEngineDialog.asStateFlow()

    private val _showSpeedDialog = MutableStateFlow(false)
    val showSpeedDialog: StateFlow<Boolean> = _showSpeedDialog.asStateFlow()

    // ===== Sleep Timer State =====
    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow<Long>(0L)
    val sleepTimerRemaining: StateFlow<Long> = _sleepTimerRemaining.asStateFlow()

    private var sleepTimerJob: kotlinx.coroutines.Job? = null

    private val _sleepTimerEndOfChapter = MutableStateFlow(false)
    val sleepTimerEndOfChapter: StateFlow<Boolean> = _sleepTimerEndOfChapter.asStateFlow()

    init {
        // Initialize TTS and restore saved settings
        viewModelScope.launch {
            val savedEngineType = settingsRepository.getTtsEngineType()
            val savedVoiceId = settingsRepository.getTtsVoiceId()

            Log.d(TAG, "Restoring TTS settings: engine=$savedEngineType, voiceId=$savedVoiceId")

            // Initialize coordinator
            ttsCoordinator.initialize()

            // Set saved voice
            if (savedVoiceId > 0) {
                ttsCoordinator.setVoice(savedVoiceId)
            }

            // If saved as kokoro and model is ready, initialize and switch to Kokoro
            if (savedEngineType == "kokoro" && ttsCoordinator.isKokoroReady.value) {
                val initialized = ttsCoordinator.initializeKokoro()
                if (initialized) {
                    ttsCoordinator.switchEngine(TtsCoordinator.Engine.KOKORO)
                } else {
                    Log.w(TAG, "Failed to initialize Kokoro on startup, staying on System TTS")
                }
            }

            // Set up callbacks
            setupCallbacks()
        }
    }

    private fun setupCallbacks() {
        ttsCoordinator.onSentenceStart = { index ->
            Log.d(TAG, "Sentence started: $index")
        }

        ttsCoordinator.onSentenceComplete = { index ->
            Log.d(TAG, "Sentence completed: $index")
        }

        ttsCoordinator.onPlaybackComplete = {
            Log.d(TAG, "Playback complete")
            // Check end-of-chapter sleep timer
            if (_sleepTimerEndOfChapter.value) {
                onChapterEnd()
            }
        }

        ttsCoordinator.onError = { error ->
            Log.e(TAG, "TTS Error: $error")
        }
    }

    // ===== Engine Control =====

    /**
     * Switch TTS engine and persist the preference.
     * @param useHighQuality true = Kokoro AI, false = System TTS
     */
    fun switchEngine(useHighQuality: Boolean) {
        Log.w(TAG, ">>> switchEngine called: useHighQuality=$useHighQuality")

        viewModelScope.launch {
            val engine = if (useHighQuality) TtsCoordinator.Engine.KOKORO else TtsCoordinator.Engine.SYSTEM

            if (useHighQuality) {
                // Initialize Kokoro if needed
                val success = ttsCoordinator.initializeKokoro()
                if (!success) {
                    Log.e(TAG, "Failed to initialize Kokoro")
                    return@launch
                }
            }

            ttsCoordinator.switchEngine(engine)

            // Persist the engine preference
            val engineType = if (useHighQuality) "kokoro" else "system"
            settingsRepository.setTtsEngineType(engineType)
            Log.d(TAG, "Saved engine preference: $engineType")
        }
    }

    /**
     * Toggle between System and Kokoro TTS.
     */
    fun toggleEngine() {
        val currentlyUsingKokoro = currentEngine.value == TtsCoordinator.Engine.KOKORO
        switchEngine(!currentlyUsingKokoro)
    }

    // ===== Speech Control =====

    /**
     * Speak the given text.
     */
    fun speak(text: String) {
        Log.d(TAG, "speak() called - text='${text.take(50)}...'")
        ttsCoordinator.speak(text)
    }

    /**
     * Jump to a specific sentence (Kokoro only).
     * Uses atomic flush for instant response.
     */
    fun jumpToSentence(index: Int) {
        ttsCoordinator.jumpToSentence(index)
    }

    /**
     * Stop speech.
     */
    fun stop() {
        ttsCoordinator.stop()
    }

    /**
     * Pause speech.
     * - System TTS: stops (can't pause mid-sentence)
     * - Kokoro: warm pause (keeps buffer)
     */
    fun pause() {
        ttsCoordinator.pause()
    }

    /**
     * Resume speech.
     * - System TTS: re-speaks from beginning
     * - Kokoro: warm or cold resume
     */
    fun resume() {
        ttsCoordinator.resume()
    }

    /**
     * Toggle playback (unified state machine).
     * - If speaking: pause/stop
     * - If paused/ready: resume
     */
    fun togglePlayback() {
        ttsCoordinator.togglePlayback()
    }

    /**
     * Set speech speed.
     */
    fun setSpeed(rate: Float) {
        ttsCoordinator.setSpeed(rate)
    }

    /**
     * Set voice ID (for Kokoro) and persist the preference.
     */
    fun setVoiceId(id: Int) {
        ttsCoordinator.setVoice(id)
        viewModelScope.launch {
            settingsRepository.setTtsVoiceId(id)
            Log.d(TAG, "Saved voice preference: $id")
        }
    }

    /**
     * Get the saved voice ID from settings.
     */
    val savedVoiceId: StateFlow<Int> = settingsRepository.ttsVoiceId
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    /**
     * Preview a Kokoro voice by speaking a short sample.
     * Safe to call even if TTS is not initialized - will initialize first.
     */
    fun previewVoice(voiceId: Int, onComplete: () -> Unit = {}) {
        // Ensure voice ID is valid (0-10 for English-only model)
        val safeVoiceId = voiceId.coerceIn(0, 10)

        viewModelScope.launch {
            // Ensure TTS is initialized before preview
            if (!ttsCoordinator.isReady()) {
                Log.d("TtsViewModel", "TTS not ready, initializing before preview...")
                val initialized = ttsCoordinator.initialize()
                if (!initialized) {
                    Log.e("TtsViewModel", "Failed to initialize TTS for preview")
                    onComplete()
                    return@launch
                }
            }

            val originalVoice = selectedVoiceId.value
            ttsCoordinator.setVoice(safeVoiceId)

            val voice = ttsCoordinator.voiceRepository.getVoice(safeVoiceId)
            val sample = "Hello, I'm ${voice?.displayName ?: "voice $safeVoiceId"}. How does this sound?"

            ttsCoordinator.onPlaybackComplete = {
                ttsCoordinator.setVoice(originalVoice) // Restore original
                onComplete()
            }

            ttsCoordinator.speak(sample)
        }
    }

    // ===== System TTS Voice Selection =====

    /**
     * Get available System TTS voices.
     */
    fun getSystemTtsVoices(): List<android.speech.tts.Voice> {
        return ttsCoordinator.getSystemVoices()
            .filter { !it.isNetworkConnectionRequired }
            .sortedWith(compareBy(
                { it.locale.displayLanguage },
                { it.locale.displayCountry },
                { it.name }
            ))
    }

    /**
     * Get English-only System TTS voices.
     */
    fun getEnglishSystemTtsVoices(): List<android.speech.tts.Voice> {
        return getSystemTtsVoices().filter { it.locale.language == "en" }
    }

    /**
     * Set System TTS voice and persist the preference.
     */
    fun setSystemTtsVoice(voice: android.speech.tts.Voice) {
        ttsCoordinator.setSystemVoice(voice)
        viewModelScope.launch {
            settingsRepository.setSystemTtsVoiceName(voice.name)
            Log.d(TAG, "Saved System TTS voice preference: ${voice.name}")
        }
    }

    /**
     * Get saved System TTS voice name.
     */
    val savedSystemVoiceName: StateFlow<String> = settingsRepository.systemTtsVoiceName
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    /**
     * Preview a System TTS voice.
     */
    fun previewSystemVoice(voice: android.speech.tts.Voice, onComplete: () -> Unit = {}) {
        // Save current engine
        val wasKokoro = currentEngine.value == TtsCoordinator.Engine.KOKORO

        ttsCoordinator.switchEngine(TtsCoordinator.Engine.SYSTEM)
        ttsCoordinator.setSystemVoice(voice)

        ttsCoordinator.onPlaybackComplete = {
            if (wasKokoro) {
                ttsCoordinator.switchEngine(TtsCoordinator.Engine.KOKORO)
            }
            onComplete()
        }

        val sample = "Hello, I'm ${voice.name.replace("_", " ").replace("-", " ")}."
        ttsCoordinator.speak(sample)
    }

    // ===== Model Management =====

    /**
     * Initialize Kokoro TTS (extract model if needed).
     */
    fun initializeKokoro(onComplete: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val success = ttsCoordinator.initializeKokoro()
            if (success) {
                onComplete()
            } else {
                onError("Failed to initialize Kokoro")
            }
        }
    }

    /**
     * Delete extracted Kokoro model.
     */
    fun deleteKokoroModel() {
        ttsCoordinator.modelAssetManager.deleteExtractedModel()

        // Switch to System TTS if currently using Kokoro
        if (currentEngine.value == TtsCoordinator.Engine.KOKORO) {
            ttsCoordinator.switchEngine(TtsCoordinator.Engine.SYSTEM)
        }
    }

    /**
     * Refresh Kokoro model status.
     */
    fun refreshKokoroStatus() {
        viewModelScope.launch {
            // Force check by initializing
            ttsCoordinator.initialize()
        }
    }

    // ===== UI Helpers =====

    /**
     * Get current engine name for display.
     */
    fun getEngineName(): String {
        return when (currentEngine.value) {
            TtsCoordinator.Engine.SYSTEM -> "System TTS"
            TtsCoordinator.Engine.KOKORO -> "Kokoro AI"
        }
    }

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

    // ===== Sleep Timer =====

    /**
     * Set sleep timer for specified minutes.
     */
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes
        _sleepTimerEndOfChapter.value = false
        _sleepTimerRemaining.value = minutes * 60 * 1000L

        sleepTimerJob = viewModelScope.launch {
            val endTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
            while (System.currentTimeMillis() < endTime) {
                _sleepTimerRemaining.value = endTime - System.currentTimeMillis()
                delay(1000L)
            }
            _sleepTimerRemaining.value = 0L
            _sleepTimerMinutes.value = null
            stop()
            Log.d(TAG, "Sleep timer expired, stopped TTS")
        }
        Log.d(TAG, "Sleep timer set for $minutes minutes")
    }

    /**
     * Cancel the sleep timer.
     */
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerMinutes.value = null
        _sleepTimerRemaining.value = 0L
        _sleepTimerEndOfChapter.value = false
        Log.d(TAG, "Sleep timer cancelled")
    }

    /**
     * Set sleep timer to stop at end of current chapter.
     */
    fun setSleepTimerEndOfChapter() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerMinutes.value = -1
        _sleepTimerRemaining.value = 0L
        _sleepTimerEndOfChapter.value = true
        Log.d(TAG, "Sleep timer set for end of chapter")
    }

    /**
     * Call this when chapter ends to trigger sleep timer if set.
     */
    fun onChapterEnd() {
        if (_sleepTimerEndOfChapter.value) {
            _sleepTimerEndOfChapter.value = false
            _sleepTimerMinutes.value = null
            stop()
            Log.d(TAG, "End of chapter reached, stopped TTS (sleep timer)")
        }
    }

    /**
     * Check if sleep timer is active.
     */
    fun isSleepTimerActive(): Boolean {
        return _sleepTimerMinutes.value != null
    }

    // ===== TTS Reading Position Persistence =====

    /**
     * Save TTS reading position for a specific book/chapter.
     */
    fun saveTtsPosition(bookId: String, chapter: Int, sentenceIndex: Int) {
        viewModelScope.launch {
            settingsRepository.saveTtsPosition(bookId, chapter, sentenceIndex)
            Log.d(TAG, "Saved TTS position: bookId=$bookId, chapter=$chapter, sentence=$sentenceIndex")
        }
    }

    /**
     * Get saved TTS reading position for a specific book/chapter.
     */
    suspend fun getTtsPosition(bookId: String, chapter: Int): Int {
        return settingsRepository.getTtsPosition(bookId, chapter)
    }

    /**
     * Clear TTS reading position.
     */
    fun clearTtsPosition(bookId: String, chapter: Int) {
        viewModelScope.launch {
            settingsRepository.clearTtsPosition(bookId, chapter)
            Log.d(TAG, "Cleared TTS position: bookId=$bookId, chapter=$chapter")
        }
    }

    // ===== Compatibility Stubs (for screens that still reference old API) =====

    // These return empty/false to maintain compatibility while transitioning
    // They can be removed once all screens are updated

    val settings: StateFlow<TtsSettings> = MutableStateFlow(TtsSettings()).asStateFlow()

    // Stub: piperModelReady - Piper is no longer used, always returns false
    val piperModelReady: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    // Stub: voiceSetupState - Old download/setup state, always Completed since model is bundled
    val voiceSetupState: StateFlow<VoiceSetupStateCompat> = MutableStateFlow(VoiceSetupStateCompat.Completed).asStateFlow()

    // Stub: audioGenerationState - Old pre-generation state, always Idle since we stream
    val audioGenerationState: StateFlow<AudioGenerationStateCompat> = MutableStateFlow(AudioGenerationStateCompat.Idle).asStateFlow()

    // Stub: cacheVersion - No longer used, always 0
    val cacheVersion: StateFlow<Int> = MutableStateFlow(0).asStateFlow()

    fun hasPreGeneratedAudio(bookId: String): Boolean = false // No more pre-generation
    fun getPreGeneratedVoiceId(bookId: String): Int? = null
    fun preGenerateBookAudio(bookId: String, sentences: List<String>, voiceId: Int = 0,
        onProgress: (Float) -> Unit = {}, onReadyForPlayback: () -> Unit = {},
        onComplete: () -> Unit = {}, onError: (String) -> Unit = {}) {
        // Streaming architecture doesn't need pre-generation
        onComplete()
    }
    fun cancelPreGeneration() {}
    fun deletePreGeneratedAudio(bookId: String) {}
    fun deleteAllBookAudio(rawBookId: String, totalChapters: Int) {}
    fun getPreGeneratedAudioSize(bookId: String): Long = 0
    fun getCachedAudio(bookId: String, sentenceIndex: Int): FloatArray? = null
    fun getTotalCacheSize(): Long = 0
    fun getCachedEntryCount(): Int = 0
    fun clearAllCache(): Boolean = true

    // Stub: Provider methods no longer needed in streaming architecture
    fun setNextSentenceProvider(provider: ((Int) -> String?)?) {}
    fun setCachedAudioProvider(provider: ((String) -> FloatArray?)?) {}

    // Stub: Download methods - model is now bundled
    fun downloadPiperModel() {}
    fun resetDownloadState() {}
    fun cancelDownload() {}

    fun preGenerateAllChaptersAudio(
        bookId: String,
        chapters: List<ChapterToGenerateCompat>,
        voiceId: Int = 0,
        onChapterStart: (chapterIdx: Int, total: Int) -> Unit = { _, _ -> },
        onProgress: (Float) -> Unit = {},
        onComplete: (totalChapters: Int, totalSentences: Int, totalBytes: Long) -> Unit = { _, _, _ -> },
        onError: (String) -> Unit = {}
    ) {
        // Streaming architecture doesn't need pre-generation - complete immediately
        onComplete(chapters.size, chapters.sumOf { it.sentences.size }, 0L)
    }

    override fun onCleared() {
        super.onCleared()
        ttsCoordinator.stop()
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

/**
 * Compatibility stub for VoiceModelManager.SetupState.
 * The new streaming architecture doesn't need download/install states since the model is bundled.
 */
sealed class VoiceSetupStateCompat {
    object Idle : VoiceSetupStateCompat()
    object FetchingConfig : VoiceSetupStateCompat()
    data class Downloading(
        val progress: Float,
        val bytesDownloaded: Long = 0L,
        val totalBytes: Long = 0L
    ) : VoiceSetupStateCompat()
    data class Installing(val progress: Float) : VoiceSetupStateCompat()
    object Completed : VoiceSetupStateCompat()
    data class Error(val message: String) : VoiceSetupStateCompat()
    object Cancelled : VoiceSetupStateCompat()
}

/**
 * Compatibility stub for TtsAudioCache.GenerationState.
 * The new streaming architecture doesn't pre-generate audio.
 */
sealed class AudioGenerationStateCompat {
    object Idle : AudioGenerationStateCompat()
    data class Generating(
        val bookId: String,
        val progress: Float,
        val currentSentence: Int = 0,
        val totalSentences: Int = 0,
        val startTimeMillis: Long = 0L,
        val readyForPlayback: Boolean = false,
        val currentChapter: Int = 0,
        val totalChapters: Int = 0
    ) : AudioGenerationStateCompat()
    data class Complete(
        val bookId: String,
        val audioSizeBytes: Long = 0L,
        val durationMillis: Long = 0L,
        val totalSentences: Int = 0
    ) : AudioGenerationStateCompat()
}

/**
 * Compatibility stub for TtsAudioCache.ChapterToGenerate.
 * Used by the old pre-generation system, which is no longer used in streaming architecture.
 */
data class ChapterToGenerateCompat(
    val chapterIndex: Int,
    val cacheKey: String,
    val sentences: List<String>
)
