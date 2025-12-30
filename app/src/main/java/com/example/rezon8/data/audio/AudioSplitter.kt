package com.mossglen.reverie.data.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AudioSplitter - Audio file splitting utility.
 *
 * NOTE: FFmpeg-kit was retired on January 6, 2025 and removed from Maven Central.
 * This is a temporary stub until a replacement library is integrated.
 * See: https://github.com/arthenica/ffmpeg-kit (archived)
 *
 * Supports all common audiobook formats: mp3, m4b, m4a, flac, ogg, wav, opus, aac.
 */
@Singleton
class AudioSplitter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioSplitter"
        private const val UNAVAILABLE_MESSAGE =
            "Audio splitting is temporarily unavailable. " +
            "The FFmpeg library is being migrated to a new provider. " +
            "This feature will return in a future update."
    }

    /**
     * Represents a segment to be extracted from the source file.
     */
    data class SplitSegment(
        val title: String,
        val startMs: Long,
        val endMs: Long
    ) {
        val durationMs: Long get() = endMs - startMs
    }

    /**
     * Result of a split operation.
     */
    sealed class SplitResult {
        data class Success(val outputFiles: List<File>) : SplitResult()
        data class Error(val message: String, val failedSegment: Int? = null) : SplitResult()
    }

    /**
     * Check if audio splitting is currently available.
     */
    fun isAvailable(): Boolean = false

    /**
     * Split an audio file into multiple segments.
     *
     * @param inputUri The URI of the source audio file.
     * @param outputDir Directory where split files will be saved.
     * @param segments List of segments to extract.
     * @param onProgress Callback with progress (0.0 to 1.0).
     * @return SplitResult indicating success with output files or error with message.
     */
    suspend fun split(
        inputUri: Uri,
        outputDir: File,
        segments: List<SplitSegment>,
        onProgress: (Float) -> Unit = {}
    ): SplitResult = withContext(Dispatchers.IO) {
        Log.w(TAG, "Audio splitting attempted but FFmpeg is not available")
        SplitResult.Error(UNAVAILABLE_MESSAGE)
    }

    /**
     * Check available storage space before splitting.
     *
     * @param estimatedSize Estimated total output size in bytes.
     * @return true if enough space is available.
     */
    fun hasEnoughStorage(outputDir: File, estimatedSize: Long): Boolean {
        val availableSpace = outputDir.usableSpace
        return availableSpace > (estimatedSize * 1.1).toLong()
    }

    /**
     * Estimate output size based on input file and segments.
     *
     * @param inputUri Source file URI.
     * @param totalDurationMs Total duration of source file.
     * @param segments Segments to be extracted.
     * @return Estimated total output size in bytes.
     */
    suspend fun estimateOutputSize(
        inputUri: Uri,
        totalDurationMs: Long,
        segments: List<SplitSegment>
    ): Long = withContext(Dispatchers.IO) {
        val inputSize = try {
            context.contentResolver.openFileDescriptor(inputUri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get input file size", e)
            0L
        }

        if (inputSize == 0L || totalDurationMs == 0L) return@withContext 0L

        val bytesPerMs = inputSize.toDouble() / totalDurationMs
        val totalSegmentDuration = segments.sumOf { it.durationMs }

        (bytesPerMs * totalSegmentDuration).toLong()
    }
}
