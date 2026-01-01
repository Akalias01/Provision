package com.mossglen.lithos.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.BreakIterator
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * LithosStreamingEngine - Native C++ Sherpa-ONNX TTS with GAPLESS streaming.
 *
 * ARCHITECTURE (v6 - WARM RESUME + THREAD-SAFE):
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │                    LithosStreamingEngine v6                              │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │  PRODUCER THREAD (Dispatchers.Default)  │  CONSUMER THREAD (Dispatchers.IO) │
 * │  ─────────────────────────────────────  │  ───────────────────────────────  │
 * │  1. Check generationId matches          │  1. Check generationId matches    │
 * │  2. If isPaused, yield() and wait       │  2. If isPaused, wait for resume  │
 * │  3. Generate Sentence N                 │  3. Pull PCM from queue           │
 * │  4. Push PCM to queue IMMEDIATELY       │  4. Write to AudioTrack (INSTANT) │
 * │  5. Only push silence at VERY END       │  5. Signal complete on last chunk │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * WARM RESUME (v6):
 * - PAUSE: Only audioTrack.pause() - keeps queue/buffer intact for instant resume
 * - RESUME: Only audioTrack.play() - audio starts instantly, no regeneration
 * - JUMP/SPEAK: Full flush + queue clear - only when content changes
 *
 * THREAD SAFETY:
 * - GENERATION ID: Each speak() increments ID; loops exit if ID mismatches
 * - isPaused FLAG: Producer/consumer wait (not exit) when paused
 * - JOB AWAIT: Wait for previous jobs on speak()/jump() only
 */
class LithosStreamingEngine(
    private val context: Context,
    private val modelAssetManager: ModelAssetManager
) {
    companion object {
        private const val TAG = "LithosStreamingEngine"

        // Audio parameters for Kokoro model
        private const val SAMPLE_RATE = 24000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT

        // Queue configuration for parallel pipeline
        private const val QUEUE_CAPACITY = 10      // Buffer up to 10 sentences ahead (more headroom)
        private const val PRE_BUFFER_COUNT = 2     // Wait for 2 sentences before playback
        private const val QUEUE_POLL_TIMEOUT_MS = 20L  // Faster polling for zero-gap feeding

        // ═══════════════════════════════════════════════════════════════════════
        // ZERO-GAP PLAYBACK (v4): NO inter-sentence silence!
        // AudioTrack MODE_STREAM handles gapless playback automatically.
        // Adding silence between sentences causes the "robotic gap".
        // ═══════════════════════════════════════════════════════════════════════

        // ═══════════════════════════════════════════════════════════════════════
        // END-ONLY PUSH SILENCE: Only needed at the very END of playback
        // This flushes the hardware buffer so the last word isn't cut off.
        // 100ms (2400 samples) is sufficient for hardware buffer flush.
        // ═══════════════════════════════════════════════════════════════════════
        private val MIN_BUFFER_SIZE = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        )
        // End-of-stream push silence: 100ms or hardware buffer, whichever is larger
        private const val END_SILENCE_MS = 100
        private val END_SILENCE_SAMPLES = maxOf(
            SAMPLE_RATE * END_SILENCE_MS / 1000,  // 100ms = 2400 samples
            MIN_BUFFER_SIZE / 4                    // Or hardware buffer size
        )

        // Singleton initialization tracking
        @Volatile
        private var instanceCount = 0
    }

    /**
     * Data class for queue items - contains PCM audio and metadata.
     */
    private data class AudioChunk(
        val samples: FloatArray,
        val sentenceIndex: Int,
        val isLastChunkOfSentence: Boolean,
        val isEndOfStream: Boolean = false  // Marks the final push silence at end
    )

    /**
     * Engine state for UI observation.
     */
    sealed class EngineState {
        object Uninitialized : EngineState()
        object Initializing : EngineState()
        object Ready : EngineState()
        object Speaking : EngineState()
        object Paused : EngineState()
        data class Error(val message: String) : EngineState()
    }

    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    // Current speaking position for UI highlighting
    private val _currentSentenceIndex = MutableStateFlow(-1)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex.asStateFlow()

    // Sherpa-ONNX native TTS instance (held as Any for reflection)
    private var offlineTts: Any? = null
    private var isInitialized = false

    // Cached reflection methods to avoid lookup overhead on every call
    private var generateMethod: java.lang.reflect.Method? = null
    private var generateWithCallbackMethod: java.lang.reflect.Method? = null
    private var getSamplesMethod: java.lang.reflect.Method? = null

    // Audio playback
    private var audioTrack: AudioTrack? = null

    // Producer-Consumer queue (large capacity for parallel synthesis)
    private val audioQueue = LinkedBlockingQueue<AudioChunk>(QUEUE_CAPACITY * 10)
    private val sentencesBuffered = AtomicInteger(0)

    // Track total frames written for end-of-stream playback verification
    private val totalFramesWritten = AtomicLong(0)

    // ═══════════════════════════════════════════════════════════════════════════
    // THREAD SAFETY (v6): Generation ID + isPaused for warm resume
    // ═══════════════════════════════════════════════════════════════════════════
    private val generationId = AtomicInteger(0)  // Incremented on each speak()/jump()
    private val isPaused = AtomicBoolean(false)  // True = paused, threads wait (not exit)

    // Thread control
    private val isProducerRunning = AtomicBoolean(false)
    private val isConsumerRunning = AtomicBoolean(false)
    private val isPreBufferComplete = AtomicBoolean(false)
    private var producerJob: Job? = null
    private var consumerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Current playback data
    private var currentSentences: List<String> = emptyList()
    private var currentVoiceId: Int = 0
    private var currentSpeed: Float = 1.0f

    // Callbacks
    var onSentenceStart: ((Int) -> Unit)? = null
    var onSentenceComplete: ((Int) -> Unit)? = null
    var onPlaybackComplete: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    /**
     * Initialize the native TTS engine.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) {
            Log.d(TAG, "Already initialized - skipping re-initialization")
            return@withContext true
        }

        instanceCount++
        if (instanceCount > 1) {
            Log.w(TAG, "⚠️ SINGLETON VIOLATION: Sherpa-ONNX being initialized $instanceCount times!")
        }

        _state.value = EngineState.Initializing
        Log.d(TAG, ">>> Initializing Sherpa-ONNX TTS engine (v6 - WARM RESUME)...")
        Log.d(TAG, "End silence: $END_SILENCE_SAMPLES samples (${END_SILENCE_MS}ms)")

        try {
            if (!modelAssetManager.ensureModelExtracted()) {
                throw Exception("Failed to extract model assets")
            }

            if (!isSherpaLibraryAvailable()) {
                throw Exception("Sherpa-ONNX library not found. Add dependency to build.gradle.")
            }

            initializeSherpaEngine()
            initializeAudioTrack()

            isInitialized = true
            _state.value = EngineState.Ready
            Log.d(TAG, "✓ Initialization complete - WARM RESUME pipelined playback ready")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
            _state.value = EngineState.Error(e.message ?: "Unknown error")
            onError?.invoke(e.message ?: "Initialization failed")
            return@withContext false
        }
    }

    private fun isSherpaLibraryAvailable(): Boolean {
        return try {
            Class.forName("com.k2fsa.sherpa.onnx.OfflineTts")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Initialize the Sherpa-ONNX engine with OPTIMIZED parameters.
     */
    private fun initializeSherpaEngine() {
        Log.d(TAG, "Creating Sherpa-ONNX OfflineTts with OPTIMIZED settings...")

        try {
            val kokoroConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig")
            val modelConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsModelConfig")
            val configClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsConfig")
            val ttsClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTts")

            val kokoroConfig = kokoroConfigClass.getDeclaredConstructor().newInstance()

            kokoroConfigClass.getMethod("setModel", String::class.java)
                .invoke(kokoroConfig, modelAssetManager.getModelPath())
            kokoroConfigClass.getMethod("setVoices", String::class.java)
                .invoke(kokoroConfig, modelAssetManager.getVoicesPath())
            kokoroConfigClass.getMethod("setTokens", String::class.java)
                .invoke(kokoroConfig, modelAssetManager.getTokensPath())
            kokoroConfigClass.getMethod("setDataDir", String::class.java)
                .invoke(kokoroConfig, modelAssetManager.getEspeakDataPath())

            val lexiconPath = modelAssetManager.getLexiconPath()
            if (lexiconPath.isNotEmpty()) {
                kokoroConfigClass.getMethod("setLexicon", String::class.java)
                    .invoke(kokoroConfig, lexiconPath)
            }

            // lengthScale = 0.9 for 10% faster speech
            kokoroConfigClass.getMethod("setLengthScale", Float::class.javaPrimitiveType)
                .invoke(kokoroConfig, 0.9f)
            Log.d(TAG, "⚡ OPTIMIZATION: lengthScale=0.9 (10% faster generation)")

            val modelConfig = modelConfigClass.getDeclaredConstructor().newInstance()
            modelConfigClass.getMethod("setKokoro", kokoroConfigClass)
                .invoke(modelConfig, kokoroConfig)

            // numThreads = 2 for mobile CPUs
            modelConfigClass.getMethod("setNumThreads", Int::class.javaPrimitiveType)
                .invoke(modelConfig, 2)
            Log.d(TAG, "⚡ OPTIMIZATION: numThreads=2 (reduces mobile CPU contention)")

            modelConfigClass.getMethod("setDebug", Boolean::class.javaPrimitiveType)
                .invoke(modelConfig, false)
            modelConfigClass.getMethod("setProvider", String::class.java)
                .invoke(modelConfig, "cpu")

            val ttsConfig = configClass.getDeclaredConstructor().newInstance()
            configClass.getMethod("setModel", modelConfigClass)
                .invoke(ttsConfig, modelConfig)
            configClass.getMethod("setMaxNumSentences", Int::class.javaPrimitiveType)
                .invoke(ttsConfig, 1)

            val assetManagerClass = Class.forName("android.content.res.AssetManager")
            offlineTts = ttsClass.getDeclaredConstructor(
                assetManagerClass,
                configClass
            ).newInstance(null, ttsConfig)

            Log.d(TAG, "✓ Sherpa-ONNX OfflineTts created (OPTIMIZED)")

            generateMethod = offlineTts!!.javaClass.getMethod(
                "generate",
                String::class.java,
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType
            )

            val function1Class = Class.forName("kotlin.jvm.functions.Function1")
            generateWithCallbackMethod = offlineTts!!.javaClass.getMethod(
                "generateWithCallback",
                String::class.java,
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                function1Class
            )

            Log.d(TAG, "Warming up model...")
            val warmupStart = System.currentTimeMillis()
            val warmupResult = generateMethod!!.invoke(offlineTts, "Hi.", 0, 1.0f)
            if (warmupResult != null) {
                getSamplesMethod = warmupResult.javaClass.getMethod("getSamples")
            }
            Log.d(TAG, "✓ Model warm-up complete in ${System.currentTimeMillis() - warmupStart}ms")

        } catch (e: Exception) {
            Log.e(TAG, "TTS Error: ${e.message}", e)
            e.cause?.let { Log.e(TAG, "Caused by: ${it.message}", it) }
            throw e
        }
    }

    /**
     * Initialize AudioTrack for low-latency streaming.
     */
    private fun initializeAudioTrack() {
        val bufferSize = maxOf(MIN_BUFFER_SIZE, SAMPLE_RATE / 4)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .setEncoding(AUDIO_FORMAT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        Log.d(TAG, "AudioTrack initialized: ${SAMPLE_RATE}Hz, buffer=${bufferSize} samples")
    }

    /**
     * Start speaking the given text.
     *
     * WARM RESUME (v6):
     * - Full atomic flush + queue clear (this is a CONTENT CHANGE)
     * - New generation ID invalidates any old loops
     * - Clears isPaused so threads run normally
     */
    fun speak(text: String, voiceId: Int = 0, speed: Float = 1.0f) {
        if (!isInitialized) {
            onError?.invoke("Engine not initialized")
            return
        }

        // ═══════════════════════════════════════════════════════════════════════
        // CONTENT CHANGE: Full stop, flush, and clear queue
        // ═══════════════════════════════════════════════════════════════════════
        stopAndAwait()

        currentSentences = splitIntoSentences(text)
        currentVoiceId = voiceId
        currentSpeed = speed

        if (currentSentences.isEmpty()) {
            Log.w(TAG, "No sentences to speak")
            return
        }

        // New generation ID + clear pause state
        val thisGeneration = generationId.incrementAndGet()
        isPaused.set(false)  // Ensure not paused when starting fresh
        Log.d(TAG, "Starting playback: ${currentSentences.size} sentences (generation=$thisGeneration)")

        _state.value = EngineState.Speaking

        // Clear queue (stale audio from previous content)
        audioQueue.clear()
        sentencesBuffered.set(0)
        totalFramesWritten.set(0)
        isPreBufferComplete.set(false)

        startProducer(0, thisGeneration)
        startConsumer(thisGeneration)
    }

    /**
     * Jump to a specific sentence index.
     *
     * WARM RESUME (v6):
     * - Full atomic flush + queue clear (this is a LOCATION CHANGE)
     * - New generation ID invalidates any old loops
     * - Clears isPaused so threads run normally
     */
    fun jumpToSentence(sentenceIndex: Int) {
        if (!isInitialized || currentSentences.isEmpty()) return
        if (sentenceIndex < 0 || sentenceIndex >= currentSentences.size) return

        // ═══════════════════════════════════════════════════════════════════════
        // LOCATION CHANGE: Full stop, flush, and clear queue
        // ═══════════════════════════════════════════════════════════════════════
        stopAndAwait()

        val thisGeneration = generationId.incrementAndGet()
        isPaused.set(false)  // Ensure not paused when jumping
        Log.d(TAG, "Jumping to sentence $sentenceIndex (generation=$thisGeneration)")

        _currentSentenceIndex.value = sentenceIndex
        _state.value = EngineState.Speaking

        // Clear queue (stale audio from previous position)
        audioQueue.clear()
        sentencesBuffered.set(0)
        totalFramesWritten.set(0)
        isPreBufferComplete.set(false)

        startProducer(sentenceIndex, thisGeneration)
        startConsumer(thisGeneration)
    }

    private fun splitIntoSentences(text: String): List<String> {
        val sentences = mutableListOf<String>()
        val iterator = BreakIterator.getSentenceInstance(Locale.US)
        iterator.setText(text)

        var start = iterator.first()
        var end = iterator.next()

        while (end != BreakIterator.DONE) {
            val sentence = text.substring(start, end).trim()
            if (sentence.isNotEmpty()) {
                sentences.add(sentence)
            }
            start = end
            end = iterator.next()
        }

        return sentences
    }

    /**
     * PRODUCER - Generates audio with WARM RESUME support (v6)
     *
     * WARM RESUME:
     * - When isPaused is true, yield() and wait instead of exiting
     * - Only exit on generation ID mismatch (content/location change)
     * - Keeps generating ahead so resume is instant
     */
    private fun startProducer(startIndex: Int, myGenerationId: Int) {
        isProducerRunning.set(true)

        producerJob = scope.launch(Dispatchers.Default) {
            Log.d(TAG, "⚡ PRODUCER started from sentence $startIndex (generation=$myGenerationId)")

            for (i in startIndex until currentSentences.size) {
                // ═══════════════════════════════════════════════════════════════
                // Generation ID check - EXIT if content/location changed
                // ═══════════════════════════════════════════════════════════════
                if (!isProducerRunning.get() || generationId.get() != myGenerationId) {
                    Log.d(TAG, "PRODUCER: Interrupted at sentence $i (gen mismatch or stopped)")
                    break
                }

                // ═══════════════════════════════════════════════════════════════
                // WARM RESUME (v6): If paused, WAIT instead of exiting
                // This keeps the queue filled for instant resume
                // ═══════════════════════════════════════════════════════════════
                while (isPaused.get() && generationId.get() == myGenerationId && isProducerRunning.get()) {
                    delay(50)  // Wait while paused
                }

                // Check again after waiting
                if (generationId.get() != myGenerationId || !isProducerRunning.get()) {
                    Log.d(TAG, "PRODUCER: Woke from pause but generation changed, exiting")
                    break
                }

                val sentence = currentSentences[i]
                Log.d(TAG, "PRODUCER: Generating sentence $i: \"${sentence.take(30)}...\"")

                val startTime = System.currentTimeMillis()

                try {
                    // Generate audio chunks to queue
                    generateAudioToQueue(sentence, i, myGenerationId)

                    // Check generation after potentially long generation
                    if (generationId.get() != myGenerationId) {
                        Log.d(TAG, "PRODUCER: Generation changed during synthesis, exiting")
                        break
                    }

                    // Mark sentence complete in buffer
                    val bufferedCount = sentencesBuffered.incrementAndGet()

                    val genTime = System.currentTimeMillis() - startTime
                    Log.d(TAG, "PRODUCER: Sentence $i complete in ${genTime}ms (buffer: $bufferedCount)")

                    // Yield if queue is getting low
                    if (audioQueue.size < 3) {
                        yield()
                    }

                } catch (e: CancellationException) {
                    Log.d(TAG, "PRODUCER: Cancelled at sentence $i")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating sentence $i", e)
                }
            }

            // Only add end markers if we weren't cancelled
            if (isProducerRunning.get() && generationId.get() == myGenerationId) {
                // END-ONLY PUSH SILENCE: Add silence at the VERY END
                val endSilence = FloatArray(END_SILENCE_SAMPLES) { 0f }
                audioQueue.put(AudioChunk(
                    samples = endSilence,
                    sentenceIndex = currentSentences.size - 1,
                    isLastChunkOfSentence = false,
                    isEndOfStream = true
                ))
                Log.d(TAG, "PRODUCER: Added END push silence ($END_SILENCE_SAMPLES samples)")

                // Signal end of stream sentinel
                audioQueue.put(AudioChunk(FloatArray(0), -1, true, false))
            }

            Log.d(TAG, "⚡ PRODUCER finished (generation=$myGenerationId)")
            isProducerRunning.set(false)
        }
    }

    /**
     * Generate audio and push chunks to queue.
     *
     * THREAD SAFETY: Callback checks generation ID to abort if playback changed.
     */
    private fun generateAudioToQueue(text: String, sentenceIndex: Int, myGenerationId: Int) {
        val tts = offlineTts ?: return
        val callbackMethod = generateWithCallbackMethod

        if (callbackMethod == null) {
            val samples = generateAudioBlocking(text)
            if (samples != null && samples.isNotEmpty() && generationId.get() == myGenerationId) {
                audioQueue.put(AudioChunk(samples, sentenceIndex, true, false))
            }
            return
        }

        try {
            var chunkCount = 0

            val callback = object : kotlin.jvm.functions.Function1<FloatArray, Int> {
                override fun invoke(samples: FloatArray): Int {
                    // ═══════════════════════════════════════════════════════════
                    // FIX 2: Check generation ID - abort if playback changed
                    // ═══════════════════════════════════════════════════════════
                    if (!isProducerRunning.get() || generationId.get() != myGenerationId) {
                        return 0  // Signal Sherpa to stop generating
                    }

                    if (samples.isNotEmpty()) {
                        chunkCount++
                        audioQueue.put(AudioChunk(
                            samples.copyOf(),
                            sentenceIndex,
                            false,
                            false
                        ))
                    }
                    return 1
                }
            }

            callbackMethod.invoke(tts, text, currentVoiceId, currentSpeed, callback)

            // Mark final audio chunk (before push silence) - only if still valid
            if (generationId.get() == myGenerationId) {
                audioQueue.put(AudioChunk(FloatArray(0), sentenceIndex, true, false))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in generateAudioToQueue", e)
            val samples = generateAudioBlocking(text)
            if (samples != null && samples.isNotEmpty() && generationId.get() == myGenerationId) {
                audioQueue.put(AudioChunk(samples, sentenceIndex, true, false))
            }
        }
    }

    private fun generateAudioBlocking(text: String): FloatArray? {
        val tts = offlineTts ?: return null
        val genMethod = generateMethod ?: return null

        return try {
            val result = genMethod.invoke(tts, text, currentVoiceId, currentSpeed)
            if (result != null && getSamplesMethod != null) {
                getSamplesMethod!!.invoke(result) as? FloatArray
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateAudioBlocking", e)
            null
        }
    }

    /**
     * CONSUMER - AudioTrack Feeding with WARM RESUME support (v6)
     *
     * WARM RESUME:
     * - When isPaused is true, DON'T pull from queue (let AudioTrack buffer drain)
     * - On resume, AudioTrack.play() resumes from where it left off
     * - Only exit on generation ID mismatch (content/location change)
     */
    private fun startConsumer(myGenerationId: Int) {
        isConsumerRunning.set(true)

        consumerJob = scope.launch(Dispatchers.IO) {
            Log.d(TAG, "⚡ CONSUMER started (generation=$myGenerationId)")

            // Pre-buffer wait - ensures producer has head start
            var waitCount = 0
            val maxWait = 100
            while (sentencesBuffered.get() < PRE_BUFFER_COUNT &&
                   isProducerRunning.get() &&
                   generationId.get() == myGenerationId &&
                   !isPaused.get() &&
                   waitCount < maxWait) {
                delay(50)
                waitCount++
            }

            // Check if we were cancelled during pre-buffer
            if (generationId.get() != myGenerationId) {
                Log.d(TAG, "CONSUMER: Generation changed during pre-buffer, exiting")
                return@launch
            }

            isPreBufferComplete.set(true)
            Log.d(TAG, "⚡ CONSUMER: Pre-buffer complete (${sentencesBuffered.get()} sentences)")

            audioTrack?.play()

            var lastSentenceIndex = -1

            while (isConsumerRunning.get() && generationId.get() == myGenerationId) {
                // ═══════════════════════════════════════════════════════════════
                // WARM RESUME (v6): If paused, WAIT instead of pulling from queue
                // AudioTrack pauses, keeping buffered audio intact
                // ═══════════════════════════════════════════════════════════════
                if (isPaused.get()) {
                    delay(50)  // Wait while paused
                    continue   // Don't pull from queue, just wait
                }

                val chunk = audioQueue.poll(QUEUE_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS)

                if (chunk == null) {
                    // Check if paused (don't exit, just wait)
                    if (isPaused.get()) {
                        continue
                    }
                    if (!isProducerRunning.get() && audioQueue.isEmpty()) {
                        Log.d(TAG, "CONSUMER: Queue empty, producer done - finishing")
                        break
                    }
                    continue
                }

                // Check generation before processing
                if (generationId.get() != myGenerationId) {
                    Log.d(TAG, "CONSUMER: Generation mismatch, discarding chunk and exiting")
                    break
                }

                // End-of-stream sentinel
                if (chunk.samples.isEmpty() && chunk.sentenceIndex == -1) {
                    Log.d(TAG, "CONSUMER: Received end-of-stream sentinel")
                    break
                }

                // Notify sentence start if new sentence (not for end silence)
                if (chunk.sentenceIndex != lastSentenceIndex &&
                    chunk.sentenceIndex >= 0 &&
                    !chunk.isEndOfStream) {
                    lastSentenceIndex = chunk.sentenceIndex
                    withContext(Dispatchers.Main) {
                        _currentSentenceIndex.value = chunk.sentenceIndex
                        onSentenceStart?.invoke(chunk.sentenceIndex)
                    }
                }

                // Write to AudioTrack IMMEDIATELY (zero-gap feeding)
                if (chunk.samples.isNotEmpty()) {
                    audioTrack?.write(chunk.samples, 0, chunk.samples.size, AudioTrack.WRITE_BLOCKING)
                    totalFramesWritten.addAndGet(chunk.samples.size.toLong())
                }

                // Signal sentence complete when last chunk is WRITTEN
                if (chunk.isLastChunkOfSentence && chunk.sentenceIndex >= 0) {
                    withContext(Dispatchers.Main) {
                        onSentenceComplete?.invoke(chunk.sentenceIndex)
                    }
                }

                // END-ONLY PLAYBACK WAIT: Only at the VERY END of stream
                if (chunk.isEndOfStream && generationId.get() == myGenerationId && !isPaused.get()) {
                    val track = audioTrack
                    if (track != null) {
                        val targetFrame = totalFramesWritten.get()
                        var playbackHead = track.playbackHeadPosition.toLong()
                        var waitLoops = 0
                        while (playbackHead < targetFrame &&
                               waitLoops < 100 &&
                               generationId.get() == myGenerationId &&
                               !isPaused.get()) {
                            delay(20)
                            playbackHead = track.playbackHeadPosition.toLong()
                            waitLoops++
                        }
                        Log.d(TAG, "CONSUMER: End playback verified at $playbackHead (target: $targetFrame)")
                    }
                }

                if (audioQueue.size <= 1 && isProducerRunning.get() && !isPaused.get()) {
                    Log.w(TAG, "⚠️ CONSUMER: Queue low (${audioQueue.size})")
                }
            }

            // Clean shutdown - only if we're still the active generation and NOT paused
            if (generationId.get() == myGenerationId && !isPaused.get()) {
                audioTrack?.let { track ->
                    try {
                        track.pause()
                        track.flush()
                    } catch (e: Exception) {
                        Log.w(TAG, "AudioTrack pause/flush error: ${e.message}")
                    }
                }

                withContext(Dispatchers.Main) {
                    _state.value = EngineState.Ready
                    onPlaybackComplete?.invoke()
                }
            }

            Log.d(TAG, "⚡ CONSUMER finished (generation=$myGenerationId)")
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * ATOMIC STOP & FLUSH (for speak/jump - CONTENT/LOCATION CHANGE)
     * - Full flush of AudioTrack hardware buffer
     * - Clear PCM queue
     * - Cancel and wait for jobs
     * ═══════════════════════════════════════════════════════════════════════════
     */
    private fun atomicStopAndFlush() {
        Log.d(TAG, "🛑 ATOMIC STOP & FLUSH (content/location change)")

        // Clear pause state and stop flags
        isPaused.set(false)
        isProducerRunning.set(false)
        isConsumerRunning.set(false)

        // Full flush - discard all buffered audio
        audioTrack?.let { track ->
            try {
                track.pause()
                track.flush()  // Discard all buffered audio - instant silence!
                Log.d(TAG, "AudioTrack paused and flushed - instant silence")
            } catch (e: Exception) {
                Log.w(TAG, "AudioTrack pause/flush error: ${e.message}")
            }
        }

        // Clear PCM queue - remove stale audio
        audioQueue.clear()
        sentencesBuffered.set(0)
        totalFramesWritten.set(0)

        // Cancel jobs
        producerJob?.cancel()
        consumerJob?.cancel()

        Log.d(TAG, "Atomic stop complete - all audio cleared")
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * FIX 2: THREAD CANCELLATION - Stop and WAIT for jobs to fully exit
     * This prevents double-reading by ensuring old threads are dead before new ones start.
     * ═══════════════════════════════════════════════════════════════════════════
     */
    private fun stopAndAwait() {
        Log.d(TAG, "🛑 STOP AND AWAIT")

        // Atomic stop first
        atomicStopAndFlush()

        // Wait for jobs to fully complete (with timeout)
        runBlocking {
            try {
                withTimeout(500) {
                    producerJob?.join()
                    consumerJob?.join()
                }
                Log.d(TAG, "Jobs fully terminated")
            } catch (e: Exception) {
                Log.w(TAG, "Timeout waiting for jobs: ${e.message}")
                // Force cancel if timeout
                producerJob?.cancel()
                consumerJob?.cancel()
            }
        }

        producerJob = null
        consumerJob = null
    }

    /**
     * Stop playback completely.
     */
    fun stop() {
        stopAndAwait()
        _currentSentenceIndex.value = -1
        _state.value = EngineState.Ready
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * WARM RESUME (v6): Pause WITHOUT flushing
     * - Just pause AudioTrack - keeps hardware buffer and queue intact
     * - Set isPaused flag so threads wait instead of exit
     * - Resume will be INSTANT because data is already there
     * ═══════════════════════════════════════════════════════════════════════════
     */
    fun pause() {
        Log.d(TAG, "⏸️ PAUSE (warm - keeping buffer)")

        // Set pause flag FIRST - threads will wait
        isPaused.set(true)

        // Just pause AudioTrack - NO flush, NO queue clear!
        // This keeps audio data intact for instant resume
        audioTrack?.let { track ->
            try {
                track.pause()  // Pauses playback, keeps buffer intact
                // NO flush() - we want to resume from where we left off
                Log.d(TAG, "AudioTrack paused (buffer preserved)")
            } catch (e: Exception) {
                Log.w(TAG, "AudioTrack pause error: ${e.message}")
            }
        }

        // DO NOT clear queue - producer may still be generating ahead
        // DO NOT cancel jobs - they will wait on isPaused flag

        _state.value = EngineState.Paused
        Log.d(TAG, "Paused - warm resume ready (queue: ${audioQueue.size})")
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * WARM RESUME (v7): Resume with INSTANT playback OR restart if queue empty
     * - If queue has audio: just AudioTrack.play() - instant resume
     * - If queue is empty: restart producer/consumer from current sentence
     * ═══════════════════════════════════════════════════════════════════════════
     */
    fun resume() {
        if (_state.value == EngineState.Paused || _state.value == EngineState.Ready) {
            val currentIdx = _currentSentenceIndex.value.coerceAtLeast(0)
            Log.d(TAG, "▶️ RESUME (queue: ${audioQueue.size}, sentence: $currentIdx)")

            // Clear pause flag FIRST - threads will resume
            isPaused.set(false)

            // Check if we need to restart producer/consumer
            val needsRestart = audioQueue.isEmpty() ||
                (!isProducerRunning.get() && !isConsumerRunning.get())

            if (needsRestart && currentSentences.isNotEmpty()) {
                // COLD RESUME: Queue was flushed or threads died - restart from current sentence
                Log.d(TAG, "▶️ COLD RESUME: Restarting producer/consumer from sentence $currentIdx")

                // Clear any stale state
                audioQueue.clear()
                sentencesBuffered.set(0)
                totalFramesWritten.set(0)
                isPreBufferComplete.set(false)

                // Increment generation ID to invalidate any zombie threads
                val thisGeneration = generationId.incrementAndGet()

                // Start fresh producer/consumer
                startProducer(currentIdx, thisGeneration)
                startConsumer(thisGeneration)

                _state.value = EngineState.Speaking
                Log.d(TAG, "Cold resume complete - playback restarting")
            } else {
                // WARM RESUME: Queue has audio, just unpause
                audioTrack?.let { track ->
                    try {
                        track.play()  // Resumes from paused position instantly!
                        Log.d(TAG, "AudioTrack resumed (warm, queue: ${audioQueue.size})")
                    } catch (e: Exception) {
                        Log.w(TAG, "AudioTrack play error: ${e.message}")
                    }
                }

                _state.value = EngineState.Speaking
                Log.d(TAG, "Warm resume complete - instant playback")
            }
        }
    }

    /**
     * Get current sentence index for resume tracking.
     */
    fun getCurrentSentenceIdx(): Int = _currentSentenceIndex.value

    /**
     * Set the playback speed.
     */
    fun setSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.5f, 2.0f)
    }

    /**
     * Set the voice ID.
     */
    fun setVoice(voiceId: Int) {
        currentVoiceId = voiceId.coerceIn(0, 10)
        Log.d(TAG, "Voice set to: $currentVoiceId")
    }

    /**
     * Check if currently speaking.
     */
    fun isSpeaking(): Boolean = _state.value == EngineState.Speaking

    /**
     * Release all resources.
     */
    fun release() {
        stop()
        scope.cancel()

        audioTrack?.release()
        audioTrack = null

        offlineTts?.let { tts ->
            try {
                val releaseMethod = tts.javaClass.getMethod("release")
                releaseMethod.invoke(tts)
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing Sherpa TTS", e)
            }
        }
        offlineTts = null

        isInitialized = false
        _state.value = EngineState.Uninitialized
        Log.d(TAG, "Engine released")
    }
}
