package com.mossglen.reverie.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import java.io.File

/**
 * Sherpa-ONNX TTS Engine using the Kokoro model.
 * Provides high-quality local AI text-to-speech synthesis.
 *
 * Model files must be installed to context.filesDir/kokoro/:
 * - model.onnx      (neural network model)
 * - tokens.txt      (text tokenization data)
 * - voices.bin      (optional: speaker embeddings)
 * - espeak-ng-data/ (phoneme generation data)
 *
 * Use VoiceModelManager to download and install the model.
 *
 * SETUP REQUIRED:
 * Download sherpa-onnx AAR from https://github.com/k2-fsa/sherpa-onnx/releases
 * Place the AAR file in app/libs/ folder
 */
class SherpaTtsEngine : AudiobookTTS {

    companion object {
        private const val TAG = "SherpaTtsEngine"

        // Kokoro model directory (inside filesDir)
        const val KOKORO_DIR = "kokoro"

        // Required model files
        const val MODEL_FILE = "model.onnx"
        const val TOKENS_FILE = "tokens.txt"
        const val VOICES_FILE = "voices.bin"
        const val ESPEAK_DATA_DIR = "espeak-ng-data"

        // Audio parameters (Kokoro uses 24kHz)
        private const val SAMPLE_RATE = 24000

        /**
         * Check if Sherpa-ONNX library is available.
         * Tries multiple class names to support different library versions.
         */
        fun isSherpaLibraryAvailable(): Boolean {
            Log.w(TAG, "=== CHECKING SHERPA LIBRARY AVAILABILITY ===")

            // Try official sherpa-onnx class names and variations
            val classNames = listOf(
                // Official k2-fsa package
                "com.k2fsa.sherpa.onnx.OfflineTts",
                "com.k2fsa.sherpa.onnx.Tts",
                "com.k2fsa.sherpa.onnx.TtsKt",
                // bihe0832 wrapper variations
                "com.bihe0832.android.lib.tts.core.TtsCore",
                "com.bihe0832.android.lib.sherpa.onnx.SherpaOnnxTTS",
                "com.bihe0832.android.lib.sherpa.SherpaManager",
                "com.bihe0832.android.lib.onnx.OnnxManager",
                // Maybe in a different package
                "sherpa.onnx.OfflineTts"
            )

            for (className in classNames) {
                try {
                    val clazz = Class.forName(className)
                    Log.w(TAG, "✓ Sherpa library FOUND: $className")
                    // Log available methods
                    clazz.methods.take(5).forEach { method ->
                        Log.d(TAG, "  Method: ${method.name}")
                    }
                    return true
                } catch (e: ClassNotFoundException) {
                    Log.d(TAG, "✗ Class not found: $className")
                }
            }

            Log.e(TAG, "=== SHERPA-ONNX LIBRARY NOT AVAILABLE ===")
            Log.e(TAG, "None of the expected classes found.")
            Log.e(TAG, "Download AAR from: https://github.com/k2-fsa/sherpa-onnx/releases")
            Log.e(TAG, "Place in app/libs/ folder")
            return false
        }

        /**
         * Get the Kokoro model directory.
         * Uses external app storage which survives app reinstalls.
         * Also checks for nested directories from tar extraction.
         */
        fun getKokoroDir(context: Context): File {
            // Use external files dir - survives app reinstall/update
            // Falls back to internal if external not available
            val externalDir = context.getExternalFilesDir(null)
            val baseDir = if (externalDir != null) {
                File(externalDir, KOKORO_DIR)
            } else {
                Log.w(TAG, "External storage not available, using internal (will be lost on reinstall)")
                File(context.filesDir, KOKORO_DIR)
            }

            // Check if model files exist directly in kokoro/
            if (baseDir.exists()) {
                val modelDirect = File(baseDir, MODEL_FILE)
                if (modelDirect.exists()) {
                    return baseDir
                }

                // Check for nested directory (tar extraction creates subdirectory like kokoro-multi-lang-v1_0/)
                baseDir.listFiles()?.forEach { child ->
                    if (child.isDirectory) {
                        val modelNested = File(child, MODEL_FILE)
                        if (modelNested.exists()) {
                            Log.d(TAG, "Found model in nested directory: ${child.name}")
                            return child
                        }
                    }
                }
            }

            return baseDir
        }

        /**
         * Check if the model is installed and ready.
         */
        fun isModelReady(context: Context): Boolean {
            val kokoroDir = getKokoroDir(context)
            // Use Log.w for visibility in logcat
            Log.w(TAG, "MODEL CHECK at: ${kokoroDir.absolutePath}")

            if (!kokoroDir.exists()) {
                Log.w(TAG, "Kokoro directory NOT FOUND")
                return false
            }

            val modelFile = File(kokoroDir, MODEL_FILE)
            val tokensFile = File(kokoroDir, TOKENS_FILE)
            val espeakDir = File(kokoroDir, ESPEAK_DATA_DIR)

            val modelExists = modelFile.exists() && modelFile.length() > 0
            val tokensExists = tokensFile.exists() && tokensFile.length() > 0
            val espeakExists = espeakDir.exists() && espeakDir.isDirectory

            val isReady = modelExists && tokensExists && espeakExists
            Log.w(TAG, "MODEL RESULT: $isReady (m=${modelFile.length()/1024/1024}MB, t=$tokensExists, e=$espeakExists)")

            return isReady
        }

        /**
         * Get model size in bytes.
         */
        fun getModelSize(context: Context): Long {
            val kokoroDir = getKokoroDir(context)
            return if (kokoroDir.exists()) {
                kokoroDir.walkTopDown().sumOf { it.length() }
            } else 0L
        }

        /**
         * Delete installed model files.
         */
        fun deleteModelFiles(context: Context): Boolean {
            val kokoroDir = getKokoroDir(context)
            return if (kokoroDir.exists()) {
                val result = kokoroDir.deleteRecursively()
                Log.d(TAG, "Model files deleted: $result")
                result
            } else true
        }
    }

    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Audio playback
    private var audioTrack: AudioTrack? = null

    // Engine state
    private var isInitialized = false
    private var currentSpeed = 1.0f
    private var currentVoiceId = 0
    private var speakingJob: Job? = null

    // Pre-buffering queue for reduced latency between sentences
    // Buffer up to 3 sentences ahead for smoother playback
    private data class BufferedSentence(val text: String, val audio: FloatArray)
    private val preBufferQueue = mutableListOf<BufferedSentence>()
    private val preBufferLock = Any()
    private var preBufferJobs = mutableListOf<Job>()
    private val MAX_PREBUFFER_QUEUE = 3

    // Sherpa-ONNX TTS instance (held as Any to allow compilation without the AAR)
    private var offlineTts: Any? = null

    // Callbacks
    var onSpeechStart: (() -> Unit)? = null
    var onSpeechDone: (() -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null
    var onRequestNextSentence: (() -> String?)? = null  // Callback to request next sentence for pre-buffering
    var onRequestCachedAudio: ((String) -> FloatArray?)? = null  // Callback to get cached audio for a sentence

    override fun initialize(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.w(TAG, "=== SHERPA TTS INITIALIZE START ===")

        scope.launch {
            try {
                // Check library availability
                Log.w(TAG, "Checking library availability...")
                if (!isSherpaLibraryAvailable()) {
                    val errorMsg = "Sherpa-ONNX library not installed. Download AAR from GitHub releases and add to app/libs/"
                    Log.e(TAG, "INIT FAILED: $errorMsg")
                    withContext(Dispatchers.Main) { onError(errorMsg) }
                    return@launch
                }
                Log.w(TAG, "Library available!")

                // Check model files
                Log.w(TAG, "Checking model files...")
                if (!isModelReady(context)) {
                    val errorMsg = "Kokoro model not installed. Use VoiceModelManager to download."
                    Log.e(TAG, "INIT FAILED: $errorMsg")
                    withContext(Dispatchers.Main) { onError(errorMsg) }
                    return@launch
                }
                Log.w(TAG, "Model files ready!")

                // Initialize Sherpa-ONNX
                val kokoroDir = getKokoroDir(context).absolutePath
                offlineTts = createOfflineTts(kokoroDir)

                if (offlineTts == null) {
                    val errorMsg = "Failed to initialize Sherpa-ONNX engine"
                    Log.e(TAG, errorMsg)
                    withContext(Dispatchers.Main) { onError(errorMsg) }
                    return@launch
                }

                // Initialize AudioTrack
                initAudioTrack()

                isInitialized = true
                Log.d(TAG, "Sherpa TTS initialized successfully")
                withContext(Dispatchers.Main) { onSuccess() }

            } catch (e: Exception) {
                val errorMsg = "Sherpa TTS init failed: ${e.message}"
                Log.e(TAG, errorMsg, e)
                withContext(Dispatchers.Main) { onError(errorMsg) }
            }
        }
    }

    /**
     * Create the OfflineTts instance with Kokoro model configuration.
     * Uses direct class instantiation with sherpa-onnx Kotlin API.
     *
     * Based on official k2-fsa examples:
     * - DataDir must point to espeak-ng-data directory
     * - Lang should be "en" (matching voice files in espeak-ng-data/lang/gmw/en)
     * - Lexicon files provide pronunciation rules for English
     */
    private fun createOfflineTts(kokoroDir: String): Any? {
        return try {
            val modelPath = "$kokoroDir/$MODEL_FILE"
            val voicesPath = "$kokoroDir/$VOICES_FILE"
            val tokensPath = "$kokoroDir/$TOKENS_FILE"
            val dataDir = "$kokoroDir/$ESPEAK_DATA_DIR"

            // Check for lexicon files (from official kokoro model package)
            val lexiconUsEn = "$kokoroDir/lexicon-us-en.txt"
            val lexiconGbEn = "$kokoroDir/lexicon-gb-en.txt"
            val lexiconPath = when {
                java.io.File(lexiconUsEn).exists() -> lexiconUsEn
                java.io.File(lexiconGbEn).exists() -> lexiconGbEn
                else -> ""
            }

            Log.w(TAG, """
                Creating OfflineTts with:
                - model: $modelPath (exists: ${java.io.File(modelPath).exists()})
                - voices: $voicesPath (exists: ${java.io.File(voicesPath).exists()})
                - tokens: $tokensPath (exists: ${java.io.File(tokensPath).exists()})
                - dataDir: $dataDir (exists: ${java.io.File(dataDir).exists()})
                - lexicon: $lexiconPath (exists: ${java.io.File(lexiconPath).exists()})
                - lang: en (voice file at espeak-ng-data/lang/gmw/en)
            """.trimIndent())

            // Use direct instantiation with sherpa-onnx Kotlin data classes
            // Step 1: Create OfflineTtsKokoroModelConfig
            // Parameters based on official API: model, voices, tokens, dataDir, lexicon, lang, dictDir, lengthScale
            val kokoroConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig")
            val kokoroConfig = kokoroConfigClass.getDeclaredConstructor(
                String::class.java, // model
                String::class.java, // voices
                String::class.java, // tokens
                String::class.java, // dataDir
                String::class.java, // lexicon
                String::class.java, // lang
                String::class.java, // dictDir
                Float::class.javaPrimitiveType // lengthScale
            ).newInstance(
                modelPath,     // model - full path to model.onnx
                voicesPath,    // voices - full path to voices.bin
                tokensPath,    // tokens - full path to tokens.txt
                dataDir,       // dataDir - full path to espeak-ng-data (contains phoneme data + voices)
                lexiconPath,   // lexicon - pronunciation rules for text-to-phoneme conversion
                "en",          // lang - must match voice file name (espeak-ng-data/lang/gmw/en)
                "",            // dictDir
                1.0f           // lengthScale
            )
            Log.w(TAG, "OfflineTtsKokoroModelConfig created: $kokoroConfig")

            // Step 2: Create OfflineTtsModelConfig and set the kokoro field
            val modelConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsModelConfig")
            val modelConfig = modelConfigClass.getDeclaredConstructor().newInstance()

            // Set the kokoro field via reflection (since it's a var in a data class)
            val kokoroField = modelConfigClass.getDeclaredField("kokoro")
            kokoroField.isAccessible = true
            kokoroField.set(modelConfig, kokoroConfig)

            // Set numThreads for faster generation - use 4 threads (modern phones have 4-8 cores)
            try {
                val numThreadsField = modelConfigClass.getDeclaredField("numThreads")
                numThreadsField.isAccessible = true
                numThreadsField.set(modelConfig, 4)
                Log.d(TAG, "Set numThreads to 4 for parallel inference")
            } catch (e: NoSuchFieldException) {
                Log.d(TAG, "numThreads field not found in OfflineTtsModelConfig")
            }

            Log.w(TAG, "OfflineTtsModelConfig created with Kokoro: $modelConfig")

            // Step 3: Create OfflineTtsConfig with the model config
            val ttsConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsConfig")
            val ttsConfig = ttsConfigClass.getDeclaredConstructor(
                modelConfigClass,     // model
                String::class.java,   // ruleFsts
                String::class.java,   // ruleFars
                Int::class.javaPrimitiveType,  // maxNumSentences
                Float::class.javaPrimitiveType // silenceScale
            ).newInstance(
                modelConfig,  // model
                "",           // ruleFsts
                "",           // ruleFars
                1,            // maxNumSentences
                0.2f          // silenceScale
            )
            Log.w(TAG, "OfflineTtsConfig created: $ttsConfig")

            // Step 4: Create OfflineTts with config (no AssetManager for filesystem paths)
            val offlineTtsClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTts")
            Log.w(TAG, "Found OfflineTts class, available constructors:")
            for (constructor in offlineTtsClass.constructors) {
                Log.d(TAG, "  Constructor: ${constructor.parameterTypes.joinToString { it.name }}")
            }

            // Try constructor with (AssetManager?, OfflineTtsConfig)
            val tts = offlineTtsClass.getDeclaredConstructor(
                android.content.res.AssetManager::class.java,
                ttsConfigClass
            ).newInstance(null, ttsConfig)

            Log.w(TAG, "OfflineTts created successfully!")
            return tts

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create OfflineTts: ${e.message}", e)
            e.cause?.let { cause ->
                Log.e(TAG, "Caused by: ${cause.message}", cause)
            }
            null
        }
    }

    /**
     * Fallback: Direct class instantiation for older library versions.
     */
    private fun createOfflineTtsDirect(modelPath: String, voicesPath: String, tokensPath: String, dataDir: String): Any? {
        return try {
            // Find OfflineTtsKokoroModelConfig - try to list all constructors
            val kokoroConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig")
            Log.d(TAG, "KokoroConfig constructors:")
            kokoroConfigClass.constructors.forEach { c ->
                Log.d(TAG, "  ${c.parameterTypes.joinToString { it.simpleName }}")
            }

            // Try constructor with all String parameters + Float for lengthScale
            val kokoroConfig = try {
                kokoroConfigClass.getConstructor(
                    String::class.java,  // model
                    String::class.java,  // voices
                    String::class.java,  // tokens
                    String::class.java,  // dataDir
                    String::class.java,  // lexicon
                    String::class.java,  // lang
                    String::class.java,  // dictDir
                    Float::class.javaPrimitiveType  // lengthScale
                ).newInstance(modelPath, voicesPath, tokensPath, dataDir, "", "", "", 1.0f)
            } catch (e: NoSuchMethodException) {
                Log.w(TAG, "8-param constructor not found, trying others...")
                // Try Kotlin's default parameter handling
                kokoroConfigClass.constructors.firstOrNull()?.also {
                    Log.d(TAG, "Using first constructor with ${it.parameterCount} params")
                }?.newInstance(*Array(kokoroConfigClass.constructors.first().parameterCount) {
                    when (it) {
                        0 -> modelPath
                        1 -> voicesPath
                        2 -> tokensPath
                        3 -> dataDir
                        else -> if (kokoroConfigClass.constructors.first().parameterTypes[it] == Float::class.javaPrimitiveType) 1.0f else ""
                    }
                })
            }

            if (kokoroConfig == null) {
                Log.e(TAG, "Failed to create KokoroConfig")
                return null
            }

            Log.d(TAG, "KokoroConfig created: $kokoroConfig")

            // Similar approach for other config classes...
            val modelConfigClass = Class.forName("com.k2fsa.sherpa.onnx.OfflineTtsModelConfig")
            Log.d(TAG, "ModelConfig constructors:")
            modelConfigClass.constructors.forEach { c ->
                Log.d(TAG, "  ${c.parameterTypes.joinToString { it.simpleName }}")
            }

            // The OfflineTtsModelConfig likely wraps multiple model configs
            null  // Will be updated once we see actual constructor signatures

        } catch (e: Exception) {
            Log.e(TAG, "Direct instantiation failed", e)
            null
        }
    }

    private fun initAudioTrack() {
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

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
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        Log.d(TAG, "AudioTrack initialized: ${SAMPLE_RATE}Hz")
    }

    override fun speak(text: String) {
        if (!isInitialized || offlineTts == null) {
            Log.w(TAG, "TTS not initialized")
            onSpeechError?.invoke("TTS not initialized")
            return
        }

        if (text.isBlank()) {
            Log.w(TAG, "Empty text")
            return
        }

        stop()

        speakingJob = scope.launch {
            try {
                withContext(Dispatchers.Main) { onSpeechStart?.invoke() }

                Log.d(TAG, "Speaking: \"${text.take(50)}...\"")

                // Priority: 1) Cached audio, 2) Pre-buffered audio, 3) Generate fresh
                val samples: FloatArray? = run {
                    // Check for cached audio first (pre-generated book audio)
                    val cached = withContext(Dispatchers.Main) { onRequestCachedAudio?.invoke(text) }
                    if (cached != null && cached.isNotEmpty()) {
                        Log.d(TAG, "Using CACHED audio (${cached.size} samples) - instant playback!")
                        return@run cached
                    }

                    // Check pre-buffer queue
                    synchronized(preBufferLock) {
                        val buffered = preBufferQueue.find { it.text == text }
                        if (buffered != null) {
                            preBufferQueue.remove(buffered)
                            Log.d(TAG, "Using pre-buffered audio!")
                            return@run buffered.audio
                        }
                    }

                    // Generate fresh
                    Log.d(TAG, "Generating audio on-demand...")
                    generateAudio(text)
                }

                if (samples == null || samples.isEmpty()) {
                    Log.w(TAG, "No audio generated")
                    withContext(Dispatchers.Main) { onSpeechError?.invoke("No audio generated") }
                    return@launch
                }

                Log.d(TAG, "Audio: ${samples.size} samples")

                // Start pre-generating next sentences WHILE playing current (only if not using cache)
                // Skip pre-buffering if we're using cached audio (it's already instant)
                if (onRequestCachedAudio?.invoke(text) == null) {
                    preBufferNextSentences()
                }

                // Play current audio
                playAudio(samples)

                Log.d(TAG, "Playback complete")
                withContext(Dispatchers.Main) { onSpeechDone?.invoke() }

            } catch (e: CancellationException) {
                Log.d(TAG, "Speech cancelled")
                synchronized(preBufferLock) { preBufferQueue.clear() }
            } catch (e: Exception) {
                Log.e(TAG, "Speech error", e)
                withContext(Dispatchers.Main) { onSpeechError?.invoke(e.message ?: "Unknown error") }
            }
        }
    }

    /**
     * Pre-buffer upcoming sentences in the background.
     */
    private fun preBufferNextSentences() {
        // Cancel existing pre-buffer jobs
        preBufferJobs.forEach { it.cancel() }
        preBufferJobs.clear()

        // Get next sentences
        var nextText = onRequestNextSentence?.invoke()
        var count = 0

        while (!nextText.isNullOrBlank() && count < MAX_PREBUFFER_QUEUE) {
            val textToBuffer = nextText

            // Check if already in queue
            val alreadyBuffered = synchronized(preBufferLock) {
                preBufferQueue.any { it.text == textToBuffer }
            }

            if (!alreadyBuffered) {
                val job = scope.launch {
                    try {
                        Log.d(TAG, "Pre-buffering: \"${textToBuffer.take(30)}...\"")
                        val audio = generateAudio(textToBuffer)
                        if (audio != null && audio.isNotEmpty()) {
                            synchronized(preBufferLock) {
                                // Keep queue size limited
                                while (preBufferQueue.size >= MAX_PREBUFFER_QUEUE) {
                                    preBufferQueue.removeAt(0)
                                }
                                preBufferQueue.add(BufferedSentence(textToBuffer, audio))
                            }
                            Log.d(TAG, "Pre-buffer ready: ${audio.size} samples")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Pre-buffer failed: ${e.message}")
                    }
                }
                preBufferJobs.add(job)
            }

            count++
            // Request next sentence (this callback should return subsequent sentences)
            // For now, we only buffer the immediate next sentence
            break
        }
    }

    /**
     * Generate audio samples using Sherpa-ONNX via reflection.
     * The OfflineTts.generate() returns a GeneratedAudio object.
     * This method is internal but exposed for TtsAudioCache pre-generation.
     */
    internal fun generateAudio(text: String): FloatArray? {
        return try {
            val tts = offlineTts ?: return null

            // Call generate() via reflection - parameters: (text: String, sid: Int, speed: Float)
            val generateMethod = tts.javaClass.getMethod(
                "generate",
                String::class.java,
                Int::class.java,
                Float::class.java
            )

            Log.d(TAG, "Calling generate(text='${text.take(30)}...', sid=$currentVoiceId, speed=$currentSpeed)")
            val audio = generateMethod.invoke(tts, text, currentVoiceId, currentSpeed)

            if (audio == null) {
                Log.e(TAG, "generate() returned null")
                return null
            }

            Log.d(TAG, "GeneratedAudio type: ${audio.javaClass.name}")

            // Try to get samples - check all fields and methods
            val audioClass = audio.javaClass

            // Log available fields and methods for debugging
            Log.d(TAG, "Available fields: ${audioClass.fields.joinToString { it.name }}")
            Log.d(TAG, "Available methods: ${audioClass.methods.filter { it.parameterCount == 0 }.take(10).joinToString { it.name }}")

            // Try various ways to get the samples
            val samples: FloatArray? = try {
                // Try 'samples' field (lowercase - Kotlin data class)
                audioClass.getField("samples").get(audio) as? FloatArray
            } catch (e: NoSuchFieldException) {
                try {
                    // Try 'Samples' field (uppercase - as in Go)
                    audioClass.getField("Samples").get(audio) as? FloatArray
                } catch (e: NoSuchFieldException) {
                    try {
                        // Try getSamples() method
                        audioClass.getMethod("getSamples").invoke(audio) as? FloatArray
                    } catch (e: NoSuchMethodException) {
                        try {
                            // Try component1() for data class (first property)
                            audioClass.getMethod("component1").invoke(audio) as? FloatArray
                        } catch (e: NoSuchMethodException) {
                            Log.e(TAG, "Could not find samples in GeneratedAudio")
                            null
                        }
                    }
                }
            }

            if (samples != null) {
                Log.d(TAG, "Got ${samples.size} audio samples")
            } else {
                Log.e(TAG, "samples is null after all attempts")
            }

            samples

        } catch (e: Exception) {
            Log.e(TAG, "Audio generation failed: ${e.message}", e)
            e.cause?.let { cause ->
                Log.e(TAG, "Caused by: ${cause.message}", cause)
            }
            null
        }
    }

    private suspend fun playAudio(samples: FloatArray) = withContext(Dispatchers.IO) {
        try {
            val track = audioTrack ?: run {
                Log.e(TAG, "AudioTrack is null!")
                return@withContext
            }

            // Log sample info
            val minSample = samples.minOrNull() ?: 0f
            val maxSample = samples.maxOrNull() ?: 0f
            val durationSeconds = samples.size / SAMPLE_RATE.toFloat()
            val durationMs = (durationSeconds * 1000).toLong()
            Log.d(TAG, "Playing ${samples.size} samples (range: $minSample to $maxSample)")
            Log.d(TAG, "Duration: $durationSeconds seconds ($durationMs ms)")

            track.play()
            Log.d(TAG, "AudioTrack state after play(): ${track.playState}")

            val written = track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            Log.d(TAG, "Wrote $written samples to AudioTrack")

            // Wait for playback to complete using duration-based timing
            // AudioTrack.write() with WRITE_BLOCKING returns after buffering, not after playback
            // We need to wait for the actual audio duration plus a small buffer
            val waitTime = durationMs + 200 // Add 200ms buffer for safety
            Log.d(TAG, "Waiting ${waitTime}ms for playback to complete...")

            // Wait in increments to allow cancellation
            var elapsed = 0L
            while (elapsed < waitTime && isActive) {
                delay(50)
                elapsed += 50
            }

            Log.d(TAG, "Playback finished after ${elapsed}ms, state: ${track.playState}")
        } catch (e: Exception) {
            Log.e(TAG, "Playback error: ${e.message}", e)
        } finally {
            audioTrack?.stop()
        }
    }

    override fun stop() {
        speakingJob?.cancel()
        speakingJob = null
        audioTrack?.pause()
        audioTrack?.flush()
    }

    override fun setSpeed(rate: Float) {
        currentSpeed = rate.coerceIn(0.5f, 2.0f)
        Log.d(TAG, "Speed: $currentSpeed")
    }

    fun setVoiceId(id: Int) {
        currentVoiceId = id.coerceAtLeast(0)
        Log.d(TAG, "Voice ID: $currentVoiceId")
    }

    fun getNumVoices(): Int {
        return try {
            val tts = offlineTts ?: return 1
            val method = tts.javaClass.getMethod("numSpeakers")
            method.invoke(tts) as? Int ?: 1
        } catch (e: Exception) {
            1
        }
    }

    override fun release() {
        Log.d(TAG, "Releasing...")
        stop()
        scope.cancel()
        audioTrack?.release()
        audioTrack = null

        try {
            offlineTts?.let { tts ->
                val releaseMethod = tts.javaClass.getMethod("release")
                releaseMethod.invoke(tts)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing OfflineTts", e)
        }

        offlineTts = null
        isInitialized = false
    }

    override fun isSpeaking(): Boolean = speakingJob?.isActive == true

    override fun getEngineName(): String = "Kokoro AI (High Quality)"

    override fun isReady(): Boolean = isInitialized && offlineTts != null

    fun getSampleRate(): Int = SAMPLE_RATE
}
