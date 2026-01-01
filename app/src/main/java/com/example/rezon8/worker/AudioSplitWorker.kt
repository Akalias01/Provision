package com.mossglen.lithos.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.mossglen.lithos.R
import com.mossglen.lithos.data.Book
import com.mossglen.lithos.data.Chapter
import com.mossglen.lithos.data.LibraryRepository
import com.mossglen.lithos.data.audio.AudioSplitter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * AudioSplitWorker - Background worker for splitting audiobooks.
 *
 * Features:
 * - Runs in background with WorkManager
 * - Shows progress notification
 * - Survives app closure
 * - Creates new Book entities for each segment
 * - Migrates chapters and bookmarks appropriately
 * - Optionally deletes original file
 */
@HiltWorker
class AudioSplitWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val audioSplitter: AudioSplitter,
    private val libraryRepository: LibraryRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AudioSplitWorker"
        private const val NOTIFICATION_CHANNEL_ID = "split_progress"
        private const val NOTIFICATION_ID = 1001

        // Input data keys
        const val KEY_BOOK_ID = "book_id"
        const val KEY_SEGMENT_TITLES = "segment_titles"
        const val KEY_SEGMENT_STARTS = "segment_starts"
        const val KEY_SEGMENT_ENDS = "segment_ends"
        const val KEY_KEEP_ORIGINAL = "keep_original"

        // Output data keys
        const val KEY_NEW_BOOK_IDS = "new_book_ids"
        const val KEY_ERROR_MESSAGE = "error_message"

        // Progress keys
        const val PROGRESS_CURRENT = "progress_current"
        const val PROGRESS_TOTAL = "progress_total"
        const val PROGRESS_STATUS = "progress_status"

        /**
         * Create a work request for splitting a book.
         */
        fun createWorkRequest(
            bookId: String,
            segments: List<Triple<String, Long, Long>>,
            keepOriginal: Boolean
        ): OneTimeWorkRequest {
            val inputData = Data.Builder()
                .putString(KEY_BOOK_ID, bookId)
                .putStringArray(KEY_SEGMENT_TITLES, segments.map { it.first }.toTypedArray())
                .putLongArray(KEY_SEGMENT_STARTS, segments.map { it.second }.toLongArray())
                .putLongArray(KEY_SEGMENT_ENDS, segments.map { it.third }.toLongArray())
                .putBoolean(KEY_KEEP_ORIGINAL, keepOriginal)
                .build()

            return OneTimeWorkRequestBuilder<AudioSplitWorker>()
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Create notification channel
            createNotificationChannel()

            // Parse input data
            val bookId = inputData.getString(KEY_BOOK_ID)
                ?: return@withContext Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to "Missing book ID")
                )

            val segmentTitles = inputData.getStringArray(KEY_SEGMENT_TITLES)
                ?: return@withContext Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to "Missing segment titles")
                )

            val segmentStarts = inputData.getLongArray(KEY_SEGMENT_STARTS)
                ?: return@withContext Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to "Missing segment starts")
                )

            val segmentEnds = inputData.getLongArray(KEY_SEGMENT_ENDS)
                ?: return@withContext Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to "Missing segment ends")
                )

            val keepOriginal = inputData.getBoolean(KEY_KEEP_ORIGINAL, true)

            // Get the original book
            val originalBook = libraryRepository.getBookById(bookId)
                ?: return@withContext Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to "Book not found")
                )

            Log.d(TAG, "Starting split for book: ${originalBook.title}")
            Log.d(TAG, "Segments: ${segmentTitles.size}")

            // Update notification: Starting
            updateNotification("Preparing to split...", 0, segmentTitles.size)

            // Create output directory
            val outputDir = File(applicationContext.filesDir, "split_audio")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            // Create split segments
            val segments = segmentTitles.indices.map { i ->
                AudioSplitter.SplitSegment(
                    title = segmentTitles[i],
                    startMs = segmentStarts[i],
                    endMs = segmentEnds[i]
                )
            }

            // Check storage space
            val estimatedSize = audioSplitter.estimateOutputSize(
                Uri.parse(originalBook.filePath),
                originalBook.duration,
                segments
            )

            if (!audioSplitter.hasEnoughStorage(outputDir, estimatedSize)) {
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to "Not enough storage space")
                )
            }

            // Perform the split
            val splitResult = audioSplitter.split(
                inputUri = Uri.parse(originalBook.filePath),
                outputDir = outputDir,
                segments = segments,
                onProgress = { progress ->
                    val currentSegment = (progress * segments.size).toInt().coerceIn(0, segments.size - 1)
                    updateNotification(
                        "Splitting segment ${currentSegment + 1}/${segments.size}",
                        currentSegment,
                        segments.size
                    )
                    setProgressAsync(
                        workDataOf(
                            PROGRESS_CURRENT to currentSegment,
                            PROGRESS_TOTAL to segments.size,
                            PROGRESS_STATUS to "Splitting..."
                        )
                    )
                }
            )

            when (splitResult) {
                is AudioSplitter.SplitResult.Success -> {
                    updateNotification("Creating library entries...", segments.size - 1, segments.size)

                    // Create new Book entities for each split file
                    val newBookIds = mutableListOf<String>()

                    splitResult.outputFiles.forEachIndexed { index, file ->
                        val segment = segments[index]
                        val newBook = createSplitBook(
                            originalBook = originalBook,
                            title = segment.title,
                            filePath = file.absolutePath,
                            startMs = segment.startMs,
                            endMs = segment.endMs,
                            segmentIndex = index,
                            totalSegments = segments.size
                        )

                        libraryRepository.insertBook(newBook)
                        newBookIds.add(newBook.id)

                        Log.d(TAG, "Created new book: ${newBook.title} (${newBook.id})")
                    }

                    // Delete original if requested
                    if (!keepOriginal) {
                        libraryRepository.deleteById(bookId)
                        Log.d(TAG, "Deleted original book: $bookId")
                    }

                    // Clear notification
                    clearNotification()

                    Log.d(TAG, "Split complete: ${newBookIds.size} books created")

                    return@withContext Result.success(
                        workDataOf(KEY_NEW_BOOK_IDS to newBookIds.toTypedArray())
                    )
                }

                is AudioSplitter.SplitResult.Error -> {
                    clearNotification()
                    Log.e(TAG, "Split failed: ${splitResult.message}")
                    return@withContext Result.failure(
                        workDataOf(KEY_ERROR_MESSAGE to splitResult.message)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Worker exception", e)
            clearNotification()
            return@withContext Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to (e.message ?: "Unknown error"))
            )
        }
    }

    /**
     * Create a new Book entity for a split segment.
     * Migrates relevant chapters and bookmarks.
     */
    private fun createSplitBook(
        originalBook: Book,
        title: String,
        filePath: String,
        startMs: Long,
        endMs: Long,
        segmentIndex: Int,
        totalSegments: Int
    ): Book {
        val segmentDuration = endMs - startMs

        // Migrate chapters that fall within this segment
        val migratedChapters = originalBook.chapters
            .filter { chapter ->
                // Chapter overlaps with segment
                chapter.startMs < endMs && chapter.endMs > startMs
            }
            .map { chapter ->
                // Adjust chapter timestamps relative to segment start
                Chapter(
                    title = chapter.title,
                    startMs = (chapter.startMs - startMs).coerceAtLeast(0),
                    endMs = (chapter.endMs - startMs).coerceAtMost(segmentDuration)
                )
            }

        // Migrate bookmarks that fall within this segment
        val migratedBookmarks = originalBook.bookmarks
            .filter { bookmark -> bookmark in startMs until endMs }
            .map { bookmark -> bookmark - startMs }

        // Migrate bookmark notes
        val migratedNotes = originalBook.bookmarkNotes
            .filterKeys { it in startMs until endMs }
            .mapKeys { (key, _) -> key - startMs }

        // Create series info if splitting a single book
        val seriesInfo = if (totalSegments > 1 && originalBook.seriesInfo.isBlank()) {
            "${originalBook.title} #${segmentIndex + 1}"
        } else {
            originalBook.seriesInfo
        }

        return Book(
            id = UUID.randomUUID().toString(),
            title = title,
            author = originalBook.author,
            coverUrl = originalBook.coverUrl,
            filePath = "file://$filePath",
            duration = segmentDuration,
            progress = 0L,
            isFinished = false,
            lastPlayedTimestamp = 0L,
            isKidsApproved = originalBook.isKidsApproved,
            format = originalBook.format,
            synopsis = originalBook.synopsis,
            seriesInfo = seriesInfo,
            chapters = migratedChapters,
            bookmarks = migratedBookmarks,
            bookmarkNotes = migratedNotes
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Audio Splitting",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows progress while splitting audiobooks"
            setShowBadge(false)
        }

        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateNotification(status: String, current: Int, total: Int) {
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Splitting Audiobook")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(total, current, false)
            .setOngoing(true)
            .setSilent(true)
            .build()

        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun clearNotification() {
        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
