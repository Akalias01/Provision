package com.mossglen.lithos.tts

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * ModelAssetManager - Handles extracting bundled TTS model assets to filesDir.
 *
 * The Sherpa-ONNX native C++ engine cannot read files directly from the compressed
 * APK assets. This class copies the model files to internal storage on first run.
 *
 * ============================================================================
 * CURRENT MODEL: KOKORO ENGLISH-ONLY (kokoro-int8-en-v0.19) - 11 VOICES
 * ============================================================================
 *
 * Download: https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models
 * File: kokoro-int8-en-v0_19.tar.bz2
 *
 * Features:
 * - model.int8.onnx (~134MB quantized model)
 * - 11 English voices (no lexicon required)
 *
 * Voice IDs (0-10):
 *   0: af         - American Female (default)
 *   1: af_bella   - American Female (warm)
 *   2: af_nicole  - American Female (energetic)
 *   3: af_sarah   - American Female (articulate)
 *   4: af_sky     - American Female (soft)
 *   5: am_adam    - American Male (authoritative)
 *   6: am_michael - American Male (friendly)
 *   7: bf_emma    - British Female (elegant)
 *   8: bf_isabella- British Female (refined)
 *   9: bm_george  - British Male (distinguished)
 *  10: bm_lewis   - British Male (modern)
 *
 * Assets required in assets/kokoro/:
 * - model.int8.onnx (quantized ONNX model)
 * - voices.bin (voice embeddings for 11 voices)
 * - tokens.txt (tokenizer vocabulary)
 * - espeak-ng-data/ (phonemizer data directory)
 */
class ModelAssetManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelAssetManager"

        // Asset source directory
        private const val ASSET_DIR = "kokoro"

        // Target directory in filesDir
        private const val MODEL_DIR = "kokoro_native"

        // Required model files (kokoro-int8-en-v0.19 English-only)
        private const val MODEL_FILE = "model.int8.onnx"
        private const val VOICES_FILE = "voices.bin"
        private const val TOKENS_FILE = "tokens.txt"
        private const val ESPEAK_DATA_DIR = "espeak-ng-data"
        // No lexicon needed for English-only model
        private const val LEXICON_FILE = "lexicon-us-en.txt"

        // Marker file to indicate successful extraction
        private const val EXTRACTION_COMPLETE_MARKER = ".extraction_complete"
    }

    /**
     * Extraction state for UI observation.
     */
    sealed class ExtractionState {
        object Idle : ExtractionState()
        data class Extracting(val progress: Float, val currentFile: String) : ExtractionState()
        object Complete : ExtractionState()
        data class Error(val message: String) : ExtractionState()
    }

    private val _extractionState = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    val extractionState: StateFlow<ExtractionState> = _extractionState.asStateFlow()

    /**
     * Get the model directory path for native engine initialization.
     */
    fun getModelDir(): File = File(context.filesDir, MODEL_DIR)

    /**
     * Get path to the ONNX model file.
     */
    fun getModelPath(): String = File(getModelDir(), MODEL_FILE).absolutePath

    /**
     * Get path to the voices file.
     */
    fun getVoicesPath(): String = File(getModelDir(), VOICES_FILE).absolutePath

    /**
     * Get path to the tokens file.
     */
    fun getTokensPath(): String = File(getModelDir(), TOKENS_FILE).absolutePath

    /**
     * Get path to the espeak-ng-data directory.
     */
    fun getEspeakDataPath(): String = File(getModelDir(), ESPEAK_DATA_DIR).absolutePath

    /**
     * Get path to the lexicon file (required for multi-lingual models only).
     * Returns empty string if lexicon doesn't exist (English-only model).
     */
    fun getLexiconPath(): String {
        val lexiconFile = File(getModelDir(), LEXICON_FILE)
        return if (lexiconFile.exists()) lexiconFile.absolutePath else ""
    }

    /**
     * Check if this is the multi-language model (has lexicon file).
     */
    fun isMultiLangModel(): Boolean = File(getModelDir(), LEXICON_FILE).exists()

    /**
     * Check if model files are already extracted and ready.
     * Lexicon is optional (only required for multi-lang model).
     */
    fun isModelReady(): Boolean {
        val modelDir = getModelDir()
        val markerFile = File(modelDir, EXTRACTION_COMPLETE_MARKER)

        if (!markerFile.exists()) {
            Log.d(TAG, "Extraction marker not found")
            return false
        }

        // Verify required files exist (lexicon is optional for English-only model)
        val modelFile = File(modelDir, MODEL_FILE)
        val voicesFile = File(modelDir, VOICES_FILE)
        val tokensFile = File(modelDir, TOKENS_FILE)
        val espeakDir = File(modelDir, ESPEAK_DATA_DIR)
        val lexiconFile = File(modelDir, LEXICON_FILE)

        val coreReady = modelFile.exists() && modelFile.length() > 0 &&
                   voicesFile.exists() && voicesFile.length() > 0 &&
                   tokensFile.exists() && tokensFile.length() > 0 &&
                   espeakDir.exists() && espeakDir.isDirectory

        val hasLexicon = lexiconFile.exists() && lexiconFile.length() > 0
        val modelSizeMB = modelFile.length() / (1024 * 1024)

        Log.d(TAG, "Model ready check: $coreReady (model=${modelFile.exists()} ${modelSizeMB}MB, voices=${voicesFile.exists()}, tokens=${tokensFile.exists()}, espeak=${espeakDir.exists()})")
        Log.d(TAG, "Using ENGLISH-ONLY model (${modelSizeMB}MB) - 11 voices")

        return coreReady
    }

    /**
     * Extract model assets to filesDir if not already done.
     * Call this on app startup. Shows loading UI if extraction takes >1 second.
     *
     * @return true if model is ready (either already extracted or extraction succeeded)
     */
    suspend fun ensureModelExtracted(): Boolean = withContext(Dispatchers.IO) {
        if (isModelReady()) {
            Log.d(TAG, "Model already extracted, skipping")
            _extractionState.value = ExtractionState.Complete
            return@withContext true
        }

        Log.d(TAG, "Starting model extraction from assets...")
        _extractionState.value = ExtractionState.Extracting(0f, "Preparing...")

        try {
            val modelDir = getModelDir()

            // Clean up any partial extraction
            if (modelDir.exists()) {
                modelDir.deleteRecursively()
            }
            modelDir.mkdirs()

            // List of files to extract with their relative sizes for progress
            val filesToExtract = mutableListOf<Pair<String, Long>>()

            // Add main files (English-only model sizes)
            filesToExtract.add(MODEL_FILE to 134_000_000L)  // ~134MB int8 model
            filesToExtract.add(VOICES_FILE to 6_000_000L)   // ~6MB voices (11 voices)
            filesToExtract.add(TOKENS_FILE to 2_000L)       // ~2KB
            // No lexicon for English-only model

            // Add espeak-ng-data files
            val espeakFiles = listAssetDirectory("$ASSET_DIR/$ESPEAK_DATA_DIR")
            espeakFiles.forEach { relativePath ->
                filesToExtract.add("$ESPEAK_DATA_DIR/$relativePath" to 100_000L) // estimate
            }

            val totalSize = filesToExtract.sumOf { it.second }
            var extractedSize = 0L

            for ((relativePath, estimatedSize) in filesToExtract) {
                val assetPath = "$ASSET_DIR/$relativePath"
                val targetFile = File(modelDir, relativePath)

                _extractionState.value = ExtractionState.Extracting(
                    progress = extractedSize.toFloat() / totalSize,
                    currentFile = relativePath.substringAfterLast("/")
                )

                // Create parent directories if needed
                targetFile.parentFile?.mkdirs()

                // Copy file
                copyAssetFile(assetPath, targetFile)
                extractedSize += estimatedSize
            }

            // Write completion marker
            File(modelDir, EXTRACTION_COMPLETE_MARKER).writeText("complete")

            _extractionState.value = ExtractionState.Complete
            Log.d(TAG, "Model extraction complete")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Model extraction failed", e)
            _extractionState.value = ExtractionState.Error(e.message ?: "Unknown error")
            return@withContext false
        }
    }

    /**
     * List files in an asset directory recursively.
     */
    private fun listAssetDirectory(path: String): List<String> {
        val result = mutableListOf<String>()
        try {
            val list = context.assets.list(path) ?: return emptyList()
            for (item in list) {
                val itemPath = "$path/$item"
                val subList = context.assets.list(itemPath)
                if (subList != null && subList.isNotEmpty()) {
                    // It's a directory, recurse
                    result.addAll(listAssetDirectory(itemPath).map { "$item/$it" })
                } else {
                    // It's a file
                    result.add(item)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error listing asset directory $path: ${e.message}")
        }
        return result
    }

    /**
     * Copy a single asset file to the target location.
     */
    private fun copyAssetFile(assetPath: String, targetFile: File) {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            inputStream = context.assets.open(assetPath)
            outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.flush()
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    /**
     * Delete extracted model files to free space.
     */
    fun deleteExtractedModel() {
        val modelDir = getModelDir()
        if (modelDir.exists()) {
            modelDir.deleteRecursively()
            Log.d(TAG, "Deleted extracted model files")
        }
        _extractionState.value = ExtractionState.Idle
    }

    /**
     * Get the size of extracted model files in bytes.
     */
    fun getExtractedModelSize(): Long {
        val modelDir = getModelDir()
        return if (modelDir.exists()) {
            modelDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }
}
