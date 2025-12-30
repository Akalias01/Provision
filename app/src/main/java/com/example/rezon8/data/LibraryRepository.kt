package com.mossglen.reverie.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.mossglen.reverie.util.CrashReporter
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val metadataRepository: MetadataRepository,
    private val torrentManager: dagger.Lazy<TorrentManager>  // Lazy to avoid circular dependency
) {
    private val gson = Gson()

    companion object {
        private const val TAG = "LibraryRepository"
    }

    /**
     * Convert a file path to a content:// URI using FileProvider.
     * Android Auto requires content:// URIs for artwork.
     */
    private fun getContentUriForCover(filePath: String?): Uri? {
        if (filePath.isNullOrEmpty()) return null
        return try {
            val file = File(filePath)
            if (file.exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get content URI for cover: $filePath", e)
            null
        }
    }

    // Flow for UI observation
    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()

    // Most recently played book for Smart Resume
    val mostRecentBook: Flow<Book?> = bookDao.getMostRecentBook()

    // Direct fetch for services (Android Auto)
    suspend fun getAllBooksDirect(): List<Book> = bookDao.getAllBooksDirect()

    suspend fun getMostRecentBookDirect(): Book? = bookDao.getMostRecentBookDirect()

    // Get most recent AUDIOBOOK for Android Auto (excludes ebooks)
    suspend fun getMostRecentAudiobookDirect(): Book? {
        val recentBook = bookDao.getMostRecentBookDirect()
        return if (recentBook?.format == "AUDIO") recentBook else {
            // Find the most recent audiobook
            bookDao.getAllBooksDirect()
                .filter { it.format == "AUDIO" && it.lastPlayedTimestamp > 0 }
                .maxByOrNull { it.lastPlayedTimestamp }
        }
    }

    suspend fun getBookById(id: String): Book? = bookDao.getBookById(id)

    suspend fun insertBook(book: Book) = bookDao.insertBook(book)

    suspend fun updateBook(book: Book) = bookDao.updateBook(book)

    suspend fun deleteById(bookId: String) = bookDao.deleteById(bookId)

    /**
     * Delete a book with full cleanup options.
     *
     * @param bookId The ID of the book to delete
     * @param deleteFiles If true, also delete the audio files from device storage
     * @return true if book was deleted (files deletion is best-effort)
     */
    suspend fun deleteBookWithCleanup(bookId: String, deleteFiles: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val book = bookDao.getBookById(bookId)
            if (book == null) {
                Log.w(TAG, "Book not found for deletion: $bookId")
                return@withContext false
            }

            Log.d(TAG, "Deleting book: ${book.title}, deleteFiles=$deleteFiles")

            // 1. Remove any active torrent for this book
            try {
                torrentManager.get().removeTorrentByFilePath(book.filePath)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove torrent (may not be active): ${e.message}")
            }

            // 2. Delete files if requested
            if (deleteFiles) {
                try {
                    val filesDeleted = torrentManager.get().deleteDownloadedFiles(book.filePath)
                    Log.d(TAG, "Files deletion result: $filesDeleted")

                    // Also try to delete cover image if it's a local file
                    book.coverUrl?.let { coverPath ->
                        if (coverPath.startsWith("/") || coverPath.startsWith("file://")) {
                            val coverFile = File(
                                if (coverPath.startsWith("file://")) {
                                    Uri.parse(coverPath).path ?: coverPath
                                } else {
                                    coverPath
                                }
                            )
                            if (coverFile.exists()) {
                                coverFile.delete()
                                Log.d(TAG, "Deleted cover image: ${coverFile.name}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting files for book: ${book.title}", e)
                    CrashReporter.logError("Failed to delete files for book: ${book.title}", e)
                }
            }

            // 3. Delete from database
            bookDao.deleteById(bookId)
            Log.d(TAG, "Successfully deleted book from database: ${book.title}")
            CrashReporter.log("Deleted book: ${book.title}, filesDeleted=$deleteFiles")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during book deletion: $bookId", e)
            CrashReporter.logError("Book deletion failed: $bookId", e)
            false
        }
    }

    /**
     * Check if a book's files are stored in the torrent download directory.
     * This helps determine if we should offer the "delete files" option.
     */
    fun isBookFromTorrent(book: Book): Boolean {
        return try {
            torrentManager.get().isInTorrentDirectory(book.filePath)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearAll() = bookDao.clearAll()

    suspend fun updateProgress(id: String, progress: Long) {
        bookDao.updateProgress(id, progress, System.currentTimeMillis())
    }

    /**
     * BULLETPROOF Import - Guaranteed to add the book even if metadata fails
     */
    suspend fun importBook(uri: Uri): Book? = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== IMPORT BOOK START ===")
        Log.d(TAG, "importBook: Input URI: $uri")
        Log.d(TAG, "importBook: URI scheme: ${uri.scheme}, authority: ${uri.authority}")
        CrashReporter.log("Starting file import: $uri")

        // 1. Get Filename first (needed for copy destination)
        val filename = getFileName(uri) ?: "Unknown Book"
        val extension = filename.substringAfterLast('.', "").lowercase()
        Log.d(TAG, "importBook: Filename: $filename, Extension: $extension")

        // 2. Try to take persistable permission OR copy file to internal storage
        var effectiveUri = uri
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            Log.d(TAG, "importBook: Successfully persisted URI permission for: $uri")
        } catch (e: Exception) {
            Log.w(TAG, "importBook: Could not persist permission (this may be normal for scanned files): ${e.message}")
            // For SAF document URIs from scanned folders, we don't need to take permission again
            // The parent folder already has permission. Only copy if we can't read the file.
            try {
                // Test if we can read the file
                context.contentResolver.openInputStream(uri)?.close()
                Log.d(TAG, "importBook: File is readable without new permission, proceeding with original URI")
            } catch (readError: Exception) {
                Log.w(TAG, "importBook: Cannot read file, attempting to copy: ${readError.message}")
                // Copy file to external app storage for persistent access (survives reinstall)
                try {
                    val booksDir = context.getExternalFilesDir("books") ?: File(context.filesDir, "books")
                    val destFile = File(booksDir, "${UUID.randomUUID()}_$filename")
                    destFile.parentFile?.mkdirs()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    effectiveUri = Uri.fromFile(destFile)
                    Log.d(TAG, "importBook: Copied file to: ${destFile.absolutePath}")
                } catch (copyError: Exception) {
                    Log.e(TAG, "importBook: FAILED to copy file: ${copyError.message}")
                    CrashReporter.logError("Failed to persist URI permission or copy file", e)
                    // Don't return null here - try to continue with original URI
                }
            }
        }

        Log.d(TAG, "importBook: Effective URI for storage: $effectiveUri")

        // 3. Determine Format Type
        val format = when (extension) {
            "mp3", "m4b", "m4a", "flac", "ogg", "wav", "opus", "aac" -> "AUDIO"
            "epub" -> "EPUB"
            "pdf" -> "PDF"
            "doc", "docx", "txt" -> "TEXT"
            else -> "AUDIO" // Default to audio for unknown
        }
        Log.d(TAG, "importBook: Determined format: $format")

        // 4. Check for duplicates - check both original and effective URI
        val existingBooks = bookDao.getAllBooksDirect()
        Log.d(TAG, "importBook: Checking for duplicates among ${existingBooks.size} existing books")

        val uriString = uri.toString()
        val effectiveUriString = effectiveUri.toString()

        val existingBook = existingBooks.find { book ->
            val bookPath = book.filePath
            val isMatch = bookPath == uriString || bookPath == effectiveUriString
            if (isMatch) {
                Log.d(TAG, "importBook: Found existing book match: ${book.title} (path: $bookPath)")
            }
            isMatch
        }
        if (existingBook != null) {
            Log.d(TAG, "importBook: Book already exists in library, returning existing: ${existingBook.title}")
            return@withContext existingBook
        }

        Log.d(TAG, "importBook: No duplicate found, proceeding with import")

        // 5. Extract Metadata (Best Effort)
        Log.d(TAG, "importBook: Extracting metadata...")
        var title = filename.substringBeforeLast(".")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
        var artist = "Unknown Author"
        var duration = 0L
        var coverPath: String? = null

        if (format == "AUDIO") {
            Log.d(TAG, "importBook: Extracting audio metadata with MediaMetadataRetriever")
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, effectiveUri)

                val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val metaDur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                Log.d(TAG, "importBook: Extracted - Title: $metaTitle, Artist: $metaArtist, Duration: $metaDur")

                if (!metaTitle.isNullOrBlank()) title = metaTitle
                if (!metaArtist.isNullOrBlank()) artist = metaArtist
                if (!metaDur.isNullOrBlank()) duration = metaDur.toLongOrNull() ?: 0L

                // Extract and save cover art
                val artBytes = retriever.embeddedPicture
                if (artBytes != null) {
                    Log.d(TAG, "importBook: Found embedded cover art (${artBytes.size} bytes)")
                    val coversDir = context.getExternalFilesDir("covers") ?: context.filesDir
                    val coverFile = File(coversDir, "cover_${UUID.randomUUID()}.jpg")
                    FileOutputStream(coverFile).use { it.write(artBytes) }
                    coverPath = coverFile.absolutePath
                    Log.d(TAG, "importBook: Saved cover to external: $coverPath")
                }
            } catch (e: Exception) {
                Log.e(TAG, "importBook: Metadata extraction failed, using defaults", e)
                CrashReporter.logError("Failed to extract metadata from file: $filename", e)
                CrashReporter.setCustomKey("import_file_name", filename)
                CrashReporter.setCustomKey("import_file_format", format)
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }
        }

        Log.d(TAG, "importBook: Final metadata - Title: $title, Author: $artist, Duration: $duration")

        // 6. Extract Chapters from audio metadata
        Log.d(TAG, "importBook: Extracting chapters...")
        val chapters: List<Chapter> = if (format == "AUDIO") {
            try {
                val extractedChapters = com.mossglen.reverie.util.ChapterExtractor.extractChapters(context, effectiveUri)
                Log.d(TAG, "importBook: Extracted ${extractedChapters.size} chapters")
                extractedChapters
            } catch (e: Exception) {
                Log.e(TAG, "importBook: Chapter extraction failed, using empty list", e)
                CrashReporter.logError("Failed to extract chapters from: $filename", e)
                emptyList()
            }
        } else {
            Log.d(TAG, "importBook: Skipping chapter extraction for non-audio format")
            emptyList()
        }

        // 7. FORCE Insert to Database (This ALWAYS happens)
        Log.d(TAG, "importBook: Creating Book entity...")
        // Extract series info and narrator from title automatically
        val seriesInfo = MetadataRepository.extractSeriesInfo(title) ?: ""
        val narrator = MetadataRepository.extractNarrator(title) ?: ""
        Log.d(TAG, "importBook: Extracted series info: $seriesInfo, narrator: $narrator")
        // Use effectiveUri for file path to ensure persistent access
        val book = Book(
            id = UUID.randomUUID().toString(),
            title = title,
            author = artist,
            coverUrl = coverPath,
            filePath = effectiveUri.toString(),
            duration = duration,
            format = format,
            progress = 0L,
            lastPlayedTimestamp = System.currentTimeMillis(),
            chapters = chapters,
            seriesInfo = seriesInfo,
            narrator = narrator
        )

        Log.d(TAG, "importBook: Inserting book into database: ${book.id}")
        bookDao.insertBook(book)

        // Verify insertion
        val verifyBook = bookDao.getBookById(book.id)
        Log.d(TAG, "importBook: Verification - book in DB: ${verifyBook != null}, title: ${verifyBook?.title}")
        val allBooks = bookDao.getAllBooksDirect()
        Log.d(TAG, "importBook: Total books in database after insert: ${allBooks.size}")

        Log.d(TAG, "=== IMPORT BOOK SUCCESS ===")
        Log.d(TAG, "importBook: Book inserted: ${book.title} (${book.format}) with ${chapters.size} chapters")
        CrashReporter.log("Successfully imported: ${book.title} (${book.format}) with ${chapters.size} chapters")

        // 8. Async Online Fetch (Best Effort)
        try {
            Log.d(TAG, "importBook: Triggering async metadata fetch...")
            metadataRepository.fetchAndSaveMetadata(book)
        } catch (e: Exception) {
            Log.w(TAG, "importBook: Online metadata fetch failed: ${e.message}")
            // Don't crash report here - MetadataRepository handles its own logging
        }

        // Return the book for further processing (e.g., torrent auto-metadata)
        Log.d(TAG, "importBook: Returning book: ${book.title}")
        book
    }

    /**
     * Scan a specific directory for audio/document files (called after torrent download completes)
     */
    suspend fun scanSpecificFolder(directory: File): List<Book> = withContext(Dispatchers.IO) {
        val importedBooks = mutableListOf<Book>()

        if (!directory.exists()) {
            Log.w("LibraryRepository", "Directory does not exist: ${directory.absolutePath}")
            CrashReporter.log("Folder scan failed - directory does not exist: ${directory.absolutePath}")
            return@withContext importedBooks
        }

        Log.d("LibraryRepository", "Scanning folder: ${directory.absolutePath}")
        CrashReporter.log("Starting folder scan: ${directory.absolutePath}")

        val audioExtensions = listOf("mp3", "m4a", "m4b", "aac", "ogg", "flac", "wav", "opus")
        val documentExtensions = listOf("pdf", "epub")
        val allSupportedExtensions = audioExtensions + documentExtensions

        // Get existing file paths to avoid duplicates
        val existingPaths = bookDao.getAllBooksDirect().map { it.filePath }.toSet()

        // First, check for split chapter audiobooks (.reverie_chapters metadata)
        val processedFolders = mutableSetOf<String>()

        directory.walkTopDown().forEach { file ->
            if (file.name == ".reverie_chapters" && file.isFile) {
                val folder = file.parentFile ?: return@forEach
                if (folder.absolutePath in processedFolders) return@forEach

                Log.d("LibraryRepository", "Found split chapter audiobook: ${folder.name}")
                val book = importSplitChapterAudiobook(folder, file)
                if (book != null) {
                    importedBooks.add(book)
                    processedFolders.add(folder.absolutePath)
                }
            }
        }

        // Then scan for regular individual files
        directory.walkTopDown().forEach { file ->
            // Skip files in folders we already processed as split chapters
            if (processedFolders.any { file.absolutePath.startsWith(it) }) {
                return@forEach
            }

            if (file.isFile && file.extension.lowercase() in allSupportedExtensions) {
                val filePath = file.absolutePath

                // Skip if already in library
                if (filePath in existingPaths || Uri.fromFile(file).toString() in existingPaths) {
                    return@forEach
                }

                Log.d("LibraryRepository", "Found file: ${file.name}")

                // Import using the bulletproof method
                val uri = Uri.fromFile(file)
                val book = importBook(uri)
                if (book != null) {
                    importedBooks.add(book)
                }
            }
        }

        Log.d("LibraryRepository", "Scan complete. Imported ${importedBooks.size} books.")
        CrashReporter.log("Folder scan complete: ${importedBooks.size} books imported")
        importedBooks
    }

    /**
     * Import a split-chapter audiobook from a folder with .reverie_chapters metadata
     */
    private suspend fun importSplitChapterAudiobook(folder: File, metadataFile: File): Book? {
        try {
            // Parse metadata file
            val lines = metadataFile.readLines()
            var bookTitle = folder.name.replace(Regex("""[\[\]_-]"""), " ").trim()
            val chapters = mutableListOf<Chapter>()

            val chapterFiles = mutableMapOf<Int, String>()
            val chapterTitles = mutableMapOf<Int, String>()

            lines.forEach { line ->
                when {
                    line.startsWith("book_title=") -> {
                        bookTitle = line.substringAfter("book_title=").trim()
                    }
                    line.startsWith("chapter.") && line.contains(".file=") -> {
                        val num = line.substringAfter("chapter.").substringBefore(".file=").toIntOrNull() ?: return@forEach
                        chapterFiles[num] = line.substringAfter(".file=").trim()
                    }
                    line.startsWith("chapter.") && line.contains(".title=") -> {
                        val num = line.substringAfter("chapter.").substringBefore(".title=").toIntOrNull() ?: return@forEach
                        chapterTitles[num] = line.substringAfter(".title=").trim()
                    }
                }
            }

            // Get audio files in order
            val audioExtensions = listOf("mp3", "m4a", "m4b", "aac", "ogg", "flac", "wav", "opus")
            val audioFiles = folder.listFiles()?.filter { file ->
                file.isFile && file.extension.lowercase() in audioExtensions
            }?.sortedBy { it.name } ?: return null

            if (audioFiles.isEmpty()) {
                Log.w("LibraryRepository", "No audio files found in split chapter folder: ${folder.name}")
                return null
            }

            // Use first file as the "main" file for the book
            val mainFile = audioFiles.first()
            val mainUri = Uri.fromFile(mainFile)

            // Check if already exists
            val existingBooks = bookDao.getAllBooksDirect()
            if (existingBooks.any { it.filePath == mainUri.toString() || it.filePath == folder.absolutePath }) {
                Log.d("LibraryRepository", "Split chapter audiobook already exists: $bookTitle")
                return null
            }

            // Extract metadata from first file
            var author = "Unknown Author"
            var coverPath: String? = null
            var totalDuration = 0L

            // Build chapters from audio files
            var currentPosition = 0L
            audioFiles.forEachIndexed { index, file ->
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.fromFile(file))

                    val fileDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

                    // Get author from first file
                    if (index == 0) {
                        val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        if (!metaArtist.isNullOrBlank()) author = metaArtist

                        // Extract cover from first file
                        val artBytes = retriever.embeddedPicture
                        if (artBytes != null) {
                            val coversDir = context.getExternalFilesDir("covers") ?: context.filesDir
                            val coverFile = File(coversDir, "cover_${UUID.randomUUID()}.jpg")
                            FileOutputStream(coverFile).use { it.write(artBytes) }
                            coverPath = coverFile.absolutePath
                        }
                    }

                    val chapterNum = index + 1
                    val chapterTitle = chapterTitles[chapterNum] ?: "Chapter $chapterNum"

                    chapters.add(Chapter(
                        title = chapterTitle,
                        startMs = currentPosition,
                        endMs = currentPosition + fileDuration,
                        filePath = file.absolutePath
                    ))

                    currentPosition += fileDuration
                    totalDuration += fileDuration

                } catch (e: Exception) {
                    Log.w("LibraryRepository", "Failed to get duration for chapter file: ${file.name}", e)
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }
            }

            // Create the book with series info and narrator extraction
            val extractedSeriesInfo = MetadataRepository.extractSeriesInfo(bookTitle) ?: ""
            val extractedNarrator = MetadataRepository.extractNarrator(bookTitle) ?: ""
            val book = Book(
                id = UUID.randomUUID().toString(),
                title = bookTitle,
                author = author,
                coverUrl = coverPath,
                filePath = folder.absolutePath, // Use folder path for split chapter books
                duration = totalDuration,
                progress = 0L,
                isFinished = false,
                format = "AUDIO",
                chapters = chapters,
                seriesInfo = extractedSeriesInfo,
                narrator = extractedNarrator
            )

            bookDao.insertBook(book)
            Log.d("LibraryRepository", "Imported split chapter audiobook: $bookTitle with ${chapters.size} chapters")

            return book

        } catch (e: Exception) {
            Log.e("LibraryRepository", "Failed to import split chapter audiobook", e)
            return null
        }
    }

    /**
     * Scan a directory given as a path string
     */
    suspend fun scanSpecificFolder(directoryPath: String): List<Book> {
        return scanSpecificFolder(File(directoryPath))
    }

    /**
     * Add or remove a bookmark for a book
     */
    suspend fun toggleBookmark(bookId: String, positionMs: Long) = withContext(Dispatchers.IO) {
        val book = bookDao.getBookById(bookId) ?: return@withContext
        val currentBookmarks = book.bookmarks.toMutableList()

        val existing = currentBookmarks.find { kotlin.math.abs(it - positionMs) < 5000 }
        if (existing != null) {
            currentBookmarks.remove(existing)
        } else {
            currentBookmarks.add(positionMs)
            currentBookmarks.sort()
        }

        val json = gson.toJson(currentBookmarks)
        bookDao.updateBookmarks(bookId, json)
    }

    /**
     * Add a bookmark with an optional note.
     */
    suspend fun addBookmarkWithNote(bookId: String, positionMs: Long, note: String) = withContext(Dispatchers.IO) {
        val book = bookDao.getBookById(bookId) ?: return@withContext
        val currentBookmarks = book.bookmarks.toMutableList()
        val currentNotes = book.bookmarkNotes.toMutableMap()

        // Check if bookmark already exists at this position (within 5 seconds)
        val existing = currentBookmarks.find { kotlin.math.abs(it - positionMs) < 5000 }
        if (existing == null) {
            currentBookmarks.add(positionMs)
            currentBookmarks.sort()
            val bookmarksJson = gson.toJson(currentBookmarks)
            bookDao.updateBookmarks(bookId, bookmarksJson)

            // Store the note if provided
            if (note.isNotBlank()) {
                currentNotes[positionMs] = note
                val notesJson = gson.toJson(currentNotes)
                bookDao.updateBookmarkNotes(bookId, notesJson)
            }
        } else {
            // Update note for existing bookmark
            if (note.isNotBlank()) {
                currentNotes[existing] = note
                val notesJson = gson.toJson(currentNotes)
                bookDao.updateBookmarkNotes(bookId, notesJson)
            } else {
                // Remove note if empty
                currentNotes.remove(existing)
                val notesJson = gson.toJson(currentNotes)
                bookDao.updateBookmarkNotes(bookId, notesJson)
            }
        }
    }

    /**
     * Delete a specific bookmark by position.
     */
    suspend fun deleteBookmark(bookId: String, positionMs: Long) = withContext(Dispatchers.IO) {
        val book = bookDao.getBookById(bookId) ?: return@withContext
        val currentBookmarks = book.bookmarks.toMutableList()
        val currentNotes = book.bookmarkNotes.toMutableMap()

        // Find and remove the bookmark (within 5 seconds tolerance)
        val existing = currentBookmarks.find { kotlin.math.abs(it - positionMs) < 5000 }
        if (existing != null) {
            currentBookmarks.remove(existing)
            currentNotes.remove(existing)

            val bookmarksJson = gson.toJson(currentBookmarks)
            val notesJson = gson.toJson(currentNotes)

            bookDao.updateBookmarks(bookId, bookmarksJson)
            bookDao.updateBookmarkNotes(bookId, notesJson)
        }
    }

    /**
     * Update a bookmark note.
     */
    suspend fun updateBookmarkNote(bookId: String, positionMs: Long, note: String) = withContext(Dispatchers.IO) {
        val book = bookDao.getBookById(bookId) ?: return@withContext
        val currentNotes = book.bookmarkNotes.toMutableMap()

        if (note.isNotBlank()) {
            currentNotes[positionMs] = note
        } else {
            currentNotes.remove(positionMs)
        }

        val notesJson = gson.toJson(currentNotes)
        bookDao.updateBookmarkNotes(bookId, notesJson)
    }

    /**
     * Update chapters for a book
     */
    suspend fun updateChapters(bookId: String, chapters: List<Chapter>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(chapters)
        bookDao.updateChapters(bookId, json)
    }

    /**
     * Re-extract chapters for a book that doesn't have them.
     * Useful for books imported before chapter detection was implemented.
     */
    suspend fun extractChaptersForBook(bookId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val book = bookDao.getBookById(bookId) ?: return@withContext false

            // Skip if book already has chapters
            if (book.chapters.isNotEmpty()) {
                Log.d(TAG, "Book already has ${book.chapters.size} chapters, skipping")
                return@withContext false
            }

            // Skip if not an audio book
            if (book.format != "AUDIO") {
                Log.d(TAG, "Book is not an audio file, skipping chapter extraction")
                return@withContext false
            }

            // Extract chapters
            val uri = Uri.parse(book.filePath)
            val chapters = com.mossglen.reverie.util.ChapterExtractor.extractChapters(context, uri)

            if (chapters.isNotEmpty()) {
                updateChapters(bookId, chapters)
                Log.d(TAG, "Extracted and saved ${chapters.size} chapters for: ${book.title}")
                CrashReporter.log("Extracted ${chapters.size} chapters for: ${book.title}")
                return@withContext true
            } else {
                Log.d(TAG, "No chapters found for: ${book.title}")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract chapters for book: $bookId", e)
            CrashReporter.logError("Chapter extraction failed for book: $bookId", e)
            return@withContext false
        }
    }

    /**
     * Re-extract chapters for all books that don't have them.
     * This is useful for migrating existing libraries.
     */
    suspend fun extractChaptersForAllBooks(): Int = withContext(Dispatchers.IO) {
        val books = bookDao.getAllBooksDirect()
        var successCount = 0

        books.forEach { book ->
            if (book.format == "AUDIO" && book.chapters.isEmpty()) {
                if (extractChaptersForBook(book.id)) {
                    successCount++
                }
            }
        }

        Log.d(TAG, "Chapter extraction complete: $successCount books updated")
        CrashReporter.log("Chapter extraction complete: $successCount books updated")
        return@withContext successCount
    }

    // Helper to get books as MediaItems for Android Auto (AUDIOBOOKS ONLY)
    suspend fun getBooksForAuto(): List<MediaItem> = withContext(Dispatchers.IO) {
        val books = bookDao.getAllBooksDirect().filter { it.format == "AUDIO" }
        Log.d(TAG, "getBooksForAuto: returning ${books.size} audiobooks")
        books.map { book ->
            // Calculate remaining time for subtitle
            val remainingMs = book.duration - book.progress
            val remainingHours = remainingMs / 3600000
            val remainingMins = (remainingMs % 3600000) / 60000
            val progressPercent = if (book.duration > 0) ((book.progress.toFloat() / book.duration) * 100).toInt() else 0
            val subtitle = if (remainingHours > 0) {
                "${remainingHours}h ${remainingMins}m remaining • ${progressPercent}%"
            } else {
                "${remainingMins}m remaining • ${progressPercent}%"
            }

            MediaItem.Builder()
                .setMediaId(book.id)
                .setUri(book.filePath)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(book.title)
                        .setArtist(book.author)
                        .setSubtitle(if (book.progress > 0) subtitle else book.author)
                        .setAlbumTitle(book.seriesInfo.ifEmpty { book.title })
                        .setArtworkUri(getContentUriForCover(book.coverUrl))
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .setExtras(android.os.Bundle().apply {
                            putLong("duration", book.duration)
                            putLong("progress", book.progress)
                            putInt("progressPercent", progressPercent)
                        })
                        .build()
                )
                .build()
        }
    }

    /**
     * Update book metadata (title, author, series)
     */
    suspend fun updateMetadata(id: String, title: String, author: String, series: String = "") = withContext(Dispatchers.IO) {
        bookDao.updateMetadata(id, title, author)
        // Also update series if we have a separate field
        if (series.isNotEmpty()) {
            val book = bookDao.getBookById(id)
            if (book != null) {
                bookDao.updateBook(book.copy(seriesInfo = series))
            }
        }
    }

    /**
     * Update book status (finished/progress)
     */
    suspend fun updateStatus(id: String, isFinished: Boolean, progress: Long) = withContext(Dispatchers.IO) {
        bookDao.updateStatus(id, isFinished, progress)
    }

    /**
     * Get filename from URI
     */
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) result = it.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
