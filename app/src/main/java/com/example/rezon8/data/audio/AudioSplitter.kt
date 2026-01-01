package com.mossglen.lithos.data.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AudioSplitter - Audio file splitting utility using Android's native MediaMuxer API.
 *
 * Supports MP4-based container formats (M4B, M4A, MP4) without external dependencies.
 * Uses MediaExtractor + MediaMuxer for lossless extraction of audio segments.
 *
 * Supported formats: M4B, M4A, MP4 (AAC/ALAC audio)
 * Not supported: MP3, OGG, FLAC (would require re-encoding)
 */
@Singleton
class AudioSplitter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioSplitter"
        private const val BUFFER_SIZE = 1024 * 1024 // 1MB buffer

        // Supported file extensions for native splitting
        private val SUPPORTED_EXTENSIONS = setOf("m4b", "m4a", "mp4", "m4p")
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
     * Check if audio splitting is available for a given file.
     *
     * @param uri The URI of the audio file to check.
     * @return true if the file format is supported for splitting.
     */
    fun isAvailable(uri: Uri? = null): Boolean {
        if (uri == null) return true // Generic check - we support some formats

        val path = uri.path ?: uri.toString()
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in SUPPORTED_EXTENSIONS
    }

    /**
     * Check if a specific file format is supported.
     */
    fun isSupportedFormat(extension: String): Boolean {
        return extension.lowercase() in SUPPORTED_EXTENSIONS
    }

    /**
     * Get a user-friendly message about format support.
     */
    fun getFormatSupportMessage(extension: String): String {
        return if (isSupportedFormat(extension)) {
            "This format supports lossless splitting."
        } else {
            "Splitting is not available for $extension files. " +
            "Supported formats: M4B, M4A, MP4."
        }
    }

    /**
     * Split an audio file into multiple segments using native Android APIs.
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
        if (segments.isEmpty()) {
            return@withContext SplitResult.Error("No segments specified")
        }

        // Check format support
        val path = inputUri.path ?: inputUri.toString()
        val extension = path.substringAfterLast('.', "").lowercase()
        if (!isSupportedFormat(extension)) {
            return@withContext SplitResult.Error(getFormatSupportMessage(extension))
        }

        // Ensure output directory exists
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val outputFiles = mutableListOf<File>()
        var extractor: MediaExtractor? = null

        try {
            // Set up MediaExtractor
            extractor = MediaExtractor()

            // Open the input file
            context.contentResolver.openFileDescriptor(inputUri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
            } ?: return@withContext SplitResult.Error("Could not open input file")

            // Find the audio track
            val audioTrackIndex = findAudioTrack(extractor)
            if (audioTrackIndex < 0) {
                return@withContext SplitResult.Error("No audio track found in file")
            }

            extractor.selectTrack(audioTrackIndex)
            val audioFormat = extractor.getTrackFormat(audioTrackIndex)
            val mimeType = audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"

            Log.d(TAG, "Audio format: $mimeType, track: $audioTrackIndex")

            // Process each segment
            for ((index, segment) in segments.withIndex()) {
                if (!isActive) {
                    // Coroutine cancelled - clean up
                    outputFiles.forEach { it.delete() }
                    return@withContext SplitResult.Error("Operation cancelled")
                }

                val outputFile = File(outputDir, sanitizeFilename(segment.title) + ".m4a")

                val result = extractSegment(
                    extractor = extractor,
                    trackIndex = audioTrackIndex,
                    format = audioFormat,
                    startMs = segment.startMs,
                    endMs = segment.endMs,
                    outputFile = outputFile
                )

                if (result) {
                    outputFiles.add(outputFile)
                    Log.d(TAG, "Created segment ${index + 1}/${segments.size}: ${outputFile.name}")
                } else {
                    // Clean up on failure
                    outputFiles.forEach { it.delete() }
                    return@withContext SplitResult.Error(
                        "Failed to extract segment: ${segment.title}",
                        failedSegment = index
                    )
                }

                // Report progress
                onProgress((index + 1).toFloat() / segments.size)
            }

            SplitResult.Success(outputFiles)

        } catch (e: Exception) {
            Log.e(TAG, "Split operation failed", e)
            outputFiles.forEach { it.delete() }
            SplitResult.Error("Split failed: ${e.message}")
        } finally {
            extractor?.release()
        }
    }

    /**
     * Extract a single segment from the audio file.
     */
    private fun extractSegment(
        extractor: MediaExtractor,
        trackIndex: Int,
        format: MediaFormat,
        startMs: Long,
        endMs: Long,
        outputFile: File
    ): Boolean {
        var muxer: MediaMuxer? = null

        try {
            // Create muxer for output
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outputTrackIndex = muxer.addTrack(format)
            muxer.start()

            // Seek to start position
            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            // Read and write samples
            while (true) {
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)

                if (sampleSize < 0) {
                    // End of stream
                    break
                }

                val sampleTime = extractor.sampleTime

                // Check if we've passed the end time
                if (sampleTime > endUs) {
                    break
                }

                // Only write samples within our time range
                if (sampleTime >= startUs) {
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = sampleTime - startUs // Adjust to start at 0
                    bufferInfo.flags = extractor.sampleFlags

                    muxer.writeSampleData(outputTrackIndex, buffer, bufferInfo)
                }

                if (!extractor.advance()) {
                    break
                }
            }

            muxer.stop()
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract segment", e)
            outputFile.delete()
            return false
        } finally {
            try {
                muxer?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing muxer", e)
            }
        }
    }

    /**
     * Find the audio track in the media file.
     */
    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i
            }
        }
        return -1
    }

    /**
     * Sanitize filename to remove invalid characters.
     */
    private fun sanitizeFilename(name: String): String {
        return name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(200) // Limit length
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

        // Add 10% overhead for container metadata
        (bytesPerMs * totalSegmentDuration * 1.1).toLong()
    }
}
