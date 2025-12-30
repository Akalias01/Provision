package com.mossglen.reverie.tts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mossglen.reverie.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * TTS Audio Cache - Pre-generates and caches TTS audio for instant playback.
 *
 * This eliminates the lag from on-device neural TTS by generating all audio
 * ahead of time. Audio is stored as compressed PCM files on disk.
 *
 * Features:
 * - Pre-generate audio for chapters or entire books
 * - Progress tracking during generation
 * - Voice selection stored with cache
 * - Delete and regenerate with different voice
 * - Efficient disk storage (~500KB per minute of audio)
 */
class TtsAudioCache(private val context: Context) {

    companion object {
        private const val TAG = "TtsAudioCache"
        private const val CACHE_DIR = "tts_cache"
        private const val SAMPLE_RATE = 24000
        private const val HEADER_SIZE = 16 // voiceId (4) + sampleRate (4) + sentenceCount (4) + reserved (4)
        private const val NOTIFICATION_CHANNEL_ID = "tts_generation"
        private const val NOTIFICATION_ID_PROGRESS = 2001
        private const val NOTIFICATION_ID_COMPLETE = 2002
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "TTS Audio Generation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress when generating TTS audio for books"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showProgressNotification(bookId: String, current: Int, total: Int) {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Generating Audio")
            .setContentText("$current of $total sentences")
            .setProgress(total, current, false)
            .setOngoing(true)
            .setSilent(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_PROGRESS, notification)
    }

    private fun showCompleteNotification(bookId: String, sentenceCount: Int) {
        // Cancel progress notification
        notificationManager.cancel(NOTIFICATION_ID_PROGRESS)

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Audio Ready")
            .setContentText("$sentenceCount sentences ready for instant playback")
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
    }

    private fun cancelNotifications() {
        notificationManager.cancel(NOTIFICATION_ID_PROGRESS)
    }

    // Generation state - supports progressive playback
    sealed class GenerationState {
        object Idle : GenerationState()
        data class Generating(
            val currentSentence: Int,
            val totalSentences: Int,
            val bookId: String,
            val readyForPlayback: Boolean = false  // True once enough sentences are ready
        ) : GenerationState()
        data class Complete(val bookId: String, val totalSentences: Int) : GenerationState()
        data class Error(val message: String) : GenerationState()
    }

    // Minimum sentences needed before allowing playback during generation
    private val MIN_SENTENCES_FOR_PLAYBACK = 10

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    private var generationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Get the cache directory for a specific book.
     */
    private fun getBookCacheDir(bookId: String): File {
        val cacheDir = File(context.getExternalFilesDir(null), CACHE_DIR)
        return File(cacheDir, bookId.hashCode().toString())
    }

    /**
     * Check if a book has pre-generated audio cache.
     */
    fun hasCachedAudio(bookId: String): Boolean {
        val cacheDir = getBookCacheDir(bookId)
        val indexFile = File(cacheDir, "index.bin")
        return indexFile.exists() && indexFile.length() > HEADER_SIZE
    }

    /**
     * Get the voice ID used for cached audio.
     */
    fun getCachedVoiceId(bookId: String): Int? {
        val cacheDir = getBookCacheDir(bookId)
        val indexFile = File(cacheDir, "index.bin")
        if (!indexFile.exists()) return null

        return try {
            RandomAccessFile(indexFile, "r").use { raf ->
                raf.readInt()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the size of cached audio in bytes.
     */
    fun getCacheSize(bookId: String): Long {
        val cacheDir = getBookCacheDir(bookId)
        return if (cacheDir.exists()) {
            cacheDir.walkTopDown().sumOf { it.length() }
        } else 0L
    }

    /**
     * Delete cached audio for a book.
     */
    fun deleteCachedAudio(bookId: String): Boolean {
        val cacheDir = getBookCacheDir(bookId)
        return if (cacheDir.exists()) {
            val result = cacheDir.deleteRecursively()
            Log.d(TAG, "Deleted cache for $bookId: $result")
            result
        } else true
    }

    /**
     * Pre-generate audio for a list of sentences.
     *
     * @param bookId Unique identifier for the book
     * @param sentences List of sentences to generate
     * @param voiceId Voice ID to use for generation
     * @param ttsEngine The SherpaTtsEngine to use for generation
     * @param onProgress Callback for progress updates (0.0 to 1.0)
     * @param onComplete Callback when generation completes
     * @param onError Callback when an error occurs
     */
    fun generateAudioForBook(
        bookId: String,
        sentences: List<String>,
        voiceId: Int,
        ttsEngine: SherpaTtsEngine,
        skipFrontMatter: Boolean = true,
        onProgress: (Float) -> Unit = {},
        onReadyForPlayback: () -> Unit = {},  // Called when enough audio is ready to start playing
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // Cancel any existing generation
        generationJob?.cancel()

        generationJob = scope.launch {
            try {
                // Skip front matter if requested
                val contentSentences = if (skipFrontMatter) {
                    val startIndex = FrontMatterDetector.findContentStartIndex(sentences)
                    if (startIndex > 0) {
                        Log.d(TAG, "Skipping $startIndex front matter sentences")
                    }
                    sentences.subList(startIndex, sentences.size)
                } else {
                    sentences
                }

                Log.d(TAG, "Starting audio generation for book $bookId with ${contentSentences.size} sentences (skipped ${sentences.size - contentSentences.size} front matter)")

                val cacheDir = getBookCacheDir(bookId)
                cacheDir.mkdirs()

                // Delete existing cache if different voice
                val existingVoice = getCachedVoiceId(bookId)
                if (existingVoice != null && existingVoice != voiceId) {
                    Log.d(TAG, "Voice changed from $existingVoice to $voiceId, clearing cache")
                    deleteCachedAudio(bookId)
                    cacheDir.mkdirs()
                }

                var notifiedReadyForPlayback = false

                // Create index file with header
                val indexFile = File(cacheDir, "index.bin")
                RandomAccessFile(indexFile, "rw").use { raf ->
                    raf.writeInt(voiceId)
                    raf.writeInt(SAMPLE_RATE)
                    raf.writeInt(contentSentences.size)
                    raf.writeInt(0) // reserved
                }

                // Generate audio for each sentence
                val offsets = mutableListOf<Long>()
                val audioFile = File(cacheDir, "audio.pcm")

                RandomAccessFile(audioFile, "rw").use { audioRaf ->
                    var currentOffset = 0L

                    for ((index, sentence) in contentSentences.withIndex()) {
                        if (!isActive) {
                            Log.d(TAG, "Generation cancelled at sentence $index")
                            cancelNotifications()
                            break
                        }

                        val readyForPlayback = index >= MIN_SENTENCES_FOR_PLAYBACK
                        _generationState.value = GenerationState.Generating(
                            currentSentence = index + 1,
                            totalSentences = contentSentences.size,
                            bookId = bookId,
                            readyForPlayback = readyForPlayback
                        )

                        // Notify when ready for playback (only once)
                        if (readyForPlayback && !notifiedReadyForPlayback) {
                            notifiedReadyForPlayback = true
                            Log.d(TAG, "Ready for playback after $index sentences")
                            withContext(Dispatchers.Main) { onReadyForPlayback() }
                        }

                        // Show progress notification every 5 sentences (not too frequent)
                        if (index % 5 == 0) {
                            showProgressNotification(bookId, index + 1, contentSentences.size)
                        }

                        // Generate audio
                        val audio = generateSentenceAudio(ttsEngine, sentence)

                        if (audio != null && audio.isNotEmpty()) {
                            // Write audio data
                            offsets.add(currentOffset)
                            val byteBuffer = ByteBuffer.allocate(audio.size * 4 + 4)
                            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                            byteBuffer.putInt(audio.size)
                            for (sample in audio) {
                                byteBuffer.putFloat(sample)
                            }
                            audioRaf.write(byteBuffer.array())
                            currentOffset += byteBuffer.capacity()
                        } else {
                            // Empty sentence - still track offset
                            offsets.add(currentOffset)
                        }

                        val progress = (index + 1).toFloat() / contentSentences.size
                        withContext(Dispatchers.Main) { onProgress(progress) }

                        // Log progress every 10 sentences
                        if ((index + 1) % 10 == 0) {
                            Log.d(TAG, "Generated ${index + 1}/${contentSentences.size} sentences")
                        }
                    }
                }

                // Write offsets to index file
                RandomAccessFile(indexFile, "rw").use { raf ->
                    raf.seek(HEADER_SIZE.toLong())
                    for (offset in offsets) {
                        raf.writeLong(offset)
                    }
                }

                _generationState.value = GenerationState.Complete(bookId, contentSentences.size)
                Log.d(TAG, "Audio generation complete for $bookId")
                showCompleteNotification(bookId, contentSentences.size)
                withContext(Dispatchers.Main) { onComplete() }

            } catch (e: CancellationException) {
                _generationState.value = GenerationState.Idle
                cancelNotifications()
                Log.d(TAG, "Audio generation cancelled")
            } catch (e: Exception) {
                val errorMsg = "Audio generation failed: ${e.message}"
                _generationState.value = GenerationState.Error(errorMsg)
                cancelNotifications()
                Log.e(TAG, errorMsg, e)
                withContext(Dispatchers.Main) { onError(errorMsg) }
            }
        }
    }

    /**
     * Generate audio for a single sentence.
     */
    private suspend fun generateSentenceAudio(engine: SherpaTtsEngine, text: String): FloatArray? {
        return withContext(Dispatchers.Default) {
            try {
                // Call the internal generateAudio method directly
                engine.generateAudio(text)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate audio for: ${text.take(30)}...", e)
                null
            }
        }
    }

    /**
     * Get cached audio for a specific sentence index.
     *
     * @param bookId The book identifier
     * @param sentenceIndex Index of the sentence (0-based)
     * @return Float array of audio samples, or null if not cached
     */
    fun getCachedAudio(bookId: String, sentenceIndex: Int): FloatArray? {
        val cacheDir = getBookCacheDir(bookId)
        val indexFile = File(cacheDir, "index.bin")
        val audioFile = File(cacheDir, "audio.pcm")

        if (!indexFile.exists() || !audioFile.exists()) {
            return null
        }

        return try {
            // Read sentence count from header
            val sentenceCount = RandomAccessFile(indexFile, "r").use { raf ->
                raf.seek(8) // Skip voiceId and sampleRate
                raf.readInt()
            }

            if (sentenceIndex < 0 || sentenceIndex >= sentenceCount) {
                return null
            }

            // Read offset for this sentence
            val offset = RandomAccessFile(indexFile, "r").use { raf ->
                raf.seek(HEADER_SIZE + sentenceIndex.toLong() * 8)
                raf.readLong()
            }

            // Read next offset to determine size (or file end)
            val nextOffset = if (sentenceIndex < sentenceCount - 1) {
                RandomAccessFile(indexFile, "r").use { raf ->
                    raf.seek(HEADER_SIZE + (sentenceIndex + 1).toLong() * 8)
                    raf.readLong()
                }
            } else {
                audioFile.length()
            }

            if (nextOffset <= offset) {
                // Empty sentence
                return FloatArray(0)
            }

            // Validate offset is within file bounds
            val audioFileSize = audioFile.length()
            if (offset < 0 || offset >= audioFileSize) {
                Log.e(TAG, "Invalid offset $offset for file size $audioFileSize")
                return null
            }

            // Read audio data
            RandomAccessFile(audioFile, "r").use { raf ->
                raf.seek(offset)

                // Read sample count header (4 bytes) using little-endian to match how it was written
                val headerBytes = ByteArray(4)
                val bytesRead = raf.read(headerBytes)
                if (bytesRead != 4) {
                    Log.e(TAG, "Failed to read sample count header")
                    return null
                }

                val headerBuffer = ByteBuffer.wrap(headerBytes)
                headerBuffer.order(ByteOrder.LITTLE_ENDIAN)
                val sampleCount = headerBuffer.int

                // Sanity check: max ~30 seconds of audio at 24kHz = 720,000 samples
                val maxSamples = 30 * SAMPLE_RATE
                if (sampleCount < 0 || sampleCount > maxSamples) {
                    Log.e(TAG, "Invalid sample count: $sampleCount (max allowed: $maxSamples)")
                    return null
                }

                if (sampleCount == 0) {
                    return FloatArray(0)
                }

                val samples = FloatArray(sampleCount)
                val bytes = ByteArray(sampleCount * 4)
                raf.read(bytes)

                val buffer = ByteBuffer.wrap(bytes)
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until sampleCount) {
                    samples[i] = buffer.float
                }

                samples
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read cached audio for sentence $sentenceIndex", e)
            null
        }
    }

    /**
     * Cancel any ongoing generation.
     */
    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        _generationState.value = GenerationState.Idle
    }

    /**
     * Get total cache size for all books.
     */
    fun getTotalCacheSize(): Long {
        val cacheDir = File(context.getExternalFilesDir(null), CACHE_DIR)
        return if (cacheDir.exists()) {
            cacheDir.walkTopDown().sumOf { it.length() }
        } else 0L
    }

    /**
     * Clear all cached audio.
     */
    fun clearAllCache(): Boolean {
        val cacheDir = File(context.getExternalFilesDir(null), CACHE_DIR)
        return if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        } else true
    }

    fun release() {
        cancelGeneration()
        scope.cancel()
    }
}
