package com.mossglen.lithos.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.mossglen.lithos.data.Chapter
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagField
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Chapter Extractor Utility
 *
 * Extracts chapter information from audio files using JAudioTagger library.
 * Supports:
 * - M4B/M4A files with chapter atoms (QuickTime chapter track)
 * - MP3 files with ID3v2 CHAP frames (ID3v2.3/2.4)
 * - OGG files with VorbisComment chapter tags
 * - FLAC files with VorbisComment chapter tags
 */
object ChapterExtractor {
    private const val TAG = "ChapterExtractor"

    init {
        // Suppress jaudiotagger logging (it's very verbose)
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
    }

    /**
     * Extract chapters from an audio file.
     * Returns a list of chapters or an empty list if no chapters are found.
     */
    fun extractChapters(context: Context, uri: Uri): List<Chapter> {
        Log.d(TAG, "Extracting chapters from: $uri")

        // For file:// URIs, use the path directly (no temp file needed)
        if (uri.scheme == "file") {
            val path = uri.path
            if (path != null) {
                Log.d(TAG, "Using direct file path: $path")
                return extractChaptersFromPath(path)
            }
        }

        // For content:// URIs, we need to create a temp file
        // But for large files, this is risky - skip chapter extraction for files > 100MB
        try {
            val fileSize = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            if (fileSize > 100 * 1024 * 1024) { // 100MB limit
                Log.w(TAG, "File too large for temp file chapter extraction: ${fileSize / 1024 / 1024}MB")
                return emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file size", e)
        }

        val tempFile = createTempFileFromUri(context, uri)
        if (tempFile == null) {
            Log.e(TAG, "Failed to create temp file for chapter extraction")
            return emptyList()
        }

        try {
            val chapters = extractChaptersFromFile(tempFile)
            Log.d(TAG, "Extracted ${chapters.size} chapters")
            return chapters
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract chapters", e)
            CrashReporter.logError("Chapter extraction failed", e)
            return emptyList()
        } finally {
            // Clean up temp file
            try {
                tempFile.delete()
            } catch (_: Exception) {}
        }
    }

    /**
     * Extract chapters from a file path directly (for files we have direct access to)
     */
    fun extractChaptersFromPath(filePath: String): List<Chapter> {
        Log.d(TAG, "Extracting chapters from path: $filePath")
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: $filePath")
            return emptyList()
        }
        return extractChaptersFromFile(file)
    }

    private fun extractChaptersFromFile(file: File): List<Chapter> {
        val extension = file.extension.lowercase()
        Log.d(TAG, "File extension: $extension")

        return when (extension) {
            "m4b", "m4a" -> extractM4bChaptersJaudiotagger(file)
            "mp3" -> extractMp3ChaptersJaudiotagger(file)
            "ogg", "oga" -> extractVorbisChaptersJaudiotagger(file)
            "flac" -> extractFlacChaptersJaudiotagger(file)
            else -> {
                Log.d(TAG, "Unsupported format for chapter extraction: $extension")
                emptyList()
            }
        }
    }

    /**
     * Extract chapters from M4B/M4A using multiple methods
     * Supports both 'chpl' atom and chapter track formats
     */
    private fun extractM4bChaptersJaudiotagger(file: File): List<Chapter> {
        Log.d(TAG, "Extracting M4B chapters from: ${file.name}")

        try {
            // Method 1: Try proper hierarchical chpl atom parsing (moov/udta/chpl)
            // This is the Nero chapter format used by many audiobook tools
            val chaptersFromChpl = parseChplAtomHierarchical(file)
            if (chaptersFromChpl.isNotEmpty()) {
                Log.d(TAG, "Found ${chaptersFromChpl.size} chapters from chpl atom (hierarchical)")
                return chaptersFromChpl
            }

            // Method 2: Try chapter track format (QuickTime style - common for professional audiobooks)
            val chaptersFromTrack = parseChapterTrackStreaming(file)
            if (chaptersFromTrack.isNotEmpty()) {
                Log.d(TAG, "Found ${chaptersFromTrack.size} chapters from chapter track")
                return chaptersFromTrack
            }

            // Method 3: Fallback - try streaming approach for chpl atom
            val chaptersFromStreaming = parseChplAtomStreaming(file)
            if (chaptersFromStreaming.isNotEmpty()) {
                Log.d(TAG, "Found ${chaptersFromStreaming.size} chapters from chpl atom (streaming)")
                return chaptersFromStreaming
            }

            Log.d(TAG, "No M4B chapters found using any method")
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting M4B chapters", e)
            return emptyList()
        }
    }

    /**
     * Parse chpl atom using proper hierarchical navigation: moov -> udta -> chpl
     * This is more reliable than byte-searching as it properly navigates the atom structure
     */
    private fun parseChplAtomHierarchical(file: File): List<Chapter> {
        try {
            java.io.RandomAccessFile(file, "r").use { raf ->
                Log.d(TAG, "Parsing M4B file hierarchically: ${file.name}, size: ${file.length()}")

                // Step 1: Find moov atom at top level
                val moovOffset = findTopLevelAtom(raf, "moov")
                if (moovOffset < 0) {
                    Log.d(TAG, "No moov atom found at file start, checking end of file")
                    // Some M4B files have moov at the end
                    val moovOffsetEnd = findTopLevelAtomFromEnd(raf, "moov")
                    if (moovOffsetEnd < 0) {
                        Log.d(TAG, "No moov atom found")
                        return emptyList()
                    }
                    return parseChplFromMoov(raf, moovOffsetEnd)
                }

                return parseChplFromMoov(raf, moovOffset)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in hierarchical chpl parsing", e)
            return emptyList()
        }
    }

    /**
     * Parse chpl atom from within a moov atom
     */
    private fun parseChplFromMoov(raf: java.io.RandomAccessFile, moovOffset: Long): List<Chapter> {
        try {
            // Read moov atom header
            raf.seek(moovOffset)
            val moovHeader = ByteArray(8)
            raf.readFully(moovHeader)

            val moovSize = readAtomSize(moovHeader, 0, raf, moovOffset)
            Log.d(TAG, "moov atom at $moovOffset, size: $moovSize")

            if (moovSize <= 8) {
                Log.e(TAG, "Invalid moov size: $moovSize")
                return emptyList()
            }

            // Step 2: Find udta atom within moov
            val udtaOffset = findAtomInRange(raf, "udta", moovOffset + 8, moovOffset + moovSize)
            if (udtaOffset < 0) {
                Log.d(TAG, "No udta atom found in moov")
                return emptyList()
            }

            raf.seek(udtaOffset)
            val udtaHeader = ByteArray(8)
            raf.readFully(udtaHeader)
            val udtaSize = readAtomSize(udtaHeader, 0, raf, udtaOffset)
            Log.d(TAG, "udta atom at $udtaOffset, size: $udtaSize")

            // Step 3: Find chpl atom within udta
            val chplOffset = findAtomInRange(raf, "chpl", udtaOffset + 8, udtaOffset + udtaSize)
            if (chplOffset < 0) {
                Log.d(TAG, "No chpl atom found in udta")
                return emptyList()
            }

            Log.d(TAG, "Found chpl atom at $chplOffset")
            return parseChplAtomAt(raf, chplOffset)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing chpl from moov", e)
            return emptyList()
        }
    }

    /**
     * Find a top-level atom starting from the end of file
     * Some M4B files have moov atom at the end after mdat
     */
    private fun findTopLevelAtomFromEnd(raf: java.io.RandomAccessFile, atomName: String): Long {
        // Scan backwards from end for common structure
        // Typically: ftyp, mdat (large), moov OR ftyp, moov, mdat
        val atoms = mutableListOf<Pair<Long, String>>()
        var pos = 0L

        try {
            while (pos < raf.length() - 8) {
                raf.seek(pos)
                val header = ByteArray(8)
                raf.readFully(header)

                val size = readAtomSize(header, 0, raf, pos)
                val name = String(header, 4, 4, Charsets.US_ASCII)

                if (size < 8 || !isValidAtomName(name)) {
                    pos += 1
                    continue
                }

                atoms.add(Pair(pos, name))

                if (name == atomName) {
                    return pos
                }

                pos += size
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning for atoms from end", e)
        }

        return -1
    }

    /**
     * Check if atom name contains only valid characters (printable ASCII)
     */
    private fun isValidAtomName(name: String): Boolean {
        return name.length == 4 && name.all { it in ' '..'~' }
    }

    /**
     * Read atom size, handling extended size (size == 1) case
     */
    private fun readAtomSize(header: ByteArray, offset: Int, raf: java.io.RandomAccessFile, atomOffset: Long): Long {
        val size32 = ByteBuffer.wrap(header, offset, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFFFFFFL

        return when {
            size32 == 1L -> {
                // Extended size - next 8 bytes contain actual size
                raf.seek(atomOffset + 8)
                val extSize = ByteArray(8)
                raf.readFully(extSize)
                ByteBuffer.wrap(extSize).order(ByteOrder.BIG_ENDIAN).long
            }
            size32 == 0L -> {
                // Size extends to end of file
                raf.length() - atomOffset
            }
            else -> size32
        }
    }

    /**
     * Parse QuickTime-style chapter track from M4B files
     * This format stores chapters in a separate text track
     */
    private fun parseChapterTrackStreaming(file: File): List<Chapter> {
        val chapters = mutableListOf<Chapter>()

        try {
            java.io.RandomAccessFile(file, "r").use { raf ->
                // Find moov atom first
                val moovOffset = findTopLevelAtom(raf, "moov")
                if (moovOffset < 0) {
                    Log.d(TAG, "No moov atom found")
                    return emptyList()
                }

                Log.d(TAG, "Found moov at offset: $moovOffset")

                // Read moov atom size
                raf.seek(moovOffset)
                val moovHeader = ByteArray(8)
                raf.readFully(moovHeader)
                val moovSize = ByteBuffer.wrap(moovHeader, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toLong()

                // Search for trak atoms within moov
                val traks = findAtomsInRange(raf, "trak", moovOffset + 8, moovOffset + moovSize)
                Log.d(TAG, "Found ${traks.size} trak atoms")

                // Look for a text track (chapter track)
                for (trakOffset in traks) {
                    raf.seek(trakOffset)
                    val trakHeader = ByteArray(8)
                    raf.readFully(trakHeader)
                    val trakSize = ByteBuffer.wrap(trakHeader, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toLong()

                    // Check if this is a text/chapter track by looking at the handler type
                    val hdlrOffset = findAtomInRange(raf, "hdlr", trakOffset + 8, trakOffset + trakSize)
                    if (hdlrOffset > 0) {
                        raf.seek(hdlrOffset + 16) // Skip to handler type
                        val handlerType = ByteArray(4)
                        raf.readFully(handlerType)
                        val handlerTypeStr = String(handlerType, Charsets.US_ASCII)
                        Log.d(TAG, "Track handler type: $handlerTypeStr")

                        if (handlerTypeStr == "text" || handlerTypeStr == "sbtl") {
                            // This is a text/subtitle track - likely chapters
                            val parsedChapters = parseTextTrackChapters(raf, trakOffset, trakSize)
                            if (parsedChapters.isNotEmpty()) {
                                return parsedChapters
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing chapter track", e)
        }

        return chapters
    }

    /**
     * Parse chapters from a text track
     */
    private fun parseTextTrackChapters(raf: java.io.RandomAccessFile, trakOffset: Long, trakSize: Long): List<Chapter> {
        val chapters = mutableListOf<Chapter>()

        try {
            // Find stbl (sample table) atom
            val mdiaOffset = findAtomInRange(raf, "mdia", trakOffset + 8, trakOffset + trakSize)
            if (mdiaOffset < 0) return emptyList()

            raf.seek(mdiaOffset)
            val mdiaHeader = ByteArray(8)
            raf.readFully(mdiaHeader)
            val mdiaSize = ByteBuffer.wrap(mdiaHeader, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toLong()

            val minfOffset = findAtomInRange(raf, "minf", mdiaOffset + 8, mdiaOffset + mdiaSize)
            if (minfOffset < 0) return emptyList()

            raf.seek(minfOffset)
            val minfHeader = ByteArray(8)
            raf.readFully(minfHeader)
            val minfSize = ByteBuffer.wrap(minfHeader, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toLong()

            val stblOffset = findAtomInRange(raf, "stbl", minfOffset + 8, minfOffset + minfSize)
            if (stblOffset < 0) return emptyList()

            raf.seek(stblOffset)
            val stblHeader = ByteArray(8)
            raf.readFully(stblHeader)
            val stblSize = ByteBuffer.wrap(stblHeader, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toLong()

            // Get sample times from stts (time-to-sample) atom
            val sttsOffset = findAtomInRange(raf, "stts", stblOffset + 8, stblOffset + stblSize)
            val sampleTimes = if (sttsOffset > 0) parseSttsAtom(raf, sttsOffset) else emptyList()

            // Get sample offsets from stco/co64 atom
            var stcoOffset = findAtomInRange(raf, "stco", stblOffset + 8, stblOffset + stblSize)
            val use64Bit = stcoOffset < 0
            if (use64Bit) {
                stcoOffset = findAtomInRange(raf, "co64", stblOffset + 8, stblOffset + stblSize)
            }
            val sampleOffsets = if (stcoOffset > 0) parseStcoAtom(raf, stcoOffset, use64Bit) else emptyList()

            // Get sample sizes from stsz atom
            val stszOffset = findAtomInRange(raf, "stsz", stblOffset + 8, stblOffset + stblSize)
            val sampleSizes = if (stszOffset > 0) parseStszAtom(raf, stszOffset) else emptyList()

            Log.d(TAG, "Text track: ${sampleTimes.size} times, ${sampleOffsets.size} offsets, ${sampleSizes.size} sizes")

            // Read each text sample
            var currentTimeMs = 0L
            val timescale = getTrackTimescale(raf, trakOffset, trakSize)

            for (i in sampleOffsets.indices) {
                if (i >= sampleSizes.size) break

                val offset = sampleOffsets[i]
                val size = sampleSizes[i]

                if (size > 0 && size < 10000) { // Sanity check
                    raf.seek(offset)
                    val sampleData = ByteArray(size)
                    raf.readFully(sampleData)

                    // Text samples start with 2-byte length prefix
                    val textLength = if (sampleData.size >= 2) {
                        ((sampleData[0].toInt() and 0xFF) shl 8) or (sampleData[1].toInt() and 0xFF)
                    } else 0

                    val title = if (textLength > 0 && textLength + 2 <= sampleData.size) {
                        String(sampleData, 2, textLength, Charsets.UTF_8).trim()
                    } else {
                        "Chapter ${i + 1}"
                    }

                    val startMs = if (timescale > 0) currentTimeMs * 1000 / timescale else currentTimeMs

                    chapters.add(Chapter(
                        title = title.ifEmpty { "Chapter ${i + 1}" },
                        startMs = startMs,
                        endMs = 0L,
                        filePath = null
                    ))
                }

                // Update time for next sample
                if (i < sampleTimes.size) {
                    currentTimeMs += sampleTimes[i]
                }
            }

            // Calculate end times
            for (i in 0 until chapters.size - 1) {
                chapters[i] = chapters[i].copy(endMs = chapters[i + 1].startMs)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing text track chapters", e)
        }

        return chapters
    }

    /**
     * Get track timescale from mdhd atom
     */
    private fun getTrackTimescale(raf: java.io.RandomAccessFile, trakOffset: Long, trakSize: Long): Long {
        try {
            val mdiaOffset = findAtomInRange(raf, "mdia", trakOffset + 8, trakOffset + trakSize)
            if (mdiaOffset < 0) return 1000

            raf.seek(mdiaOffset)
            val mdiaHeader = ByteArray(8)
            raf.readFully(mdiaHeader)
            val mdiaSize = ByteBuffer.wrap(mdiaHeader, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toLong()

            val mdhdOffset = findAtomInRange(raf, "mdhd", mdiaOffset + 8, mdiaOffset + mdiaSize)
            if (mdhdOffset < 0) return 1000

            raf.seek(mdhdOffset + 8) // Skip atom header
            val version = raf.read()

            // Skip based on version (version 1 has 64-bit fields)
            if (version == 1) {
                raf.skipBytes(16) // creation_time, modification_time (64-bit each)
            } else {
                raf.skipBytes(8) // creation_time, modification_time (32-bit each)
            }

            val timescaleBytes = ByteArray(4)
            raf.readFully(timescaleBytes)
            return ByteBuffer.wrap(timescaleBytes).order(ByteOrder.BIG_ENDIAN).int.toLong()
        } catch (e: Exception) {
            return 1000
        }
    }

    /**
     * Parse stts (time-to-sample) atom
     */
    private fun parseSttsAtom(raf: java.io.RandomAccessFile, offset: Long): List<Long> {
        val durations = mutableListOf<Long>()
        try {
            raf.seek(offset + 12) // Skip header + version/flags
            val entryCountBytes = ByteArray(4)
            raf.readFully(entryCountBytes)
            val entryCount = ByteBuffer.wrap(entryCountBytes).order(ByteOrder.BIG_ENDIAN).int

            for (i in 0 until entryCount.coerceAtMost(10000)) {
                val entryBytes = ByteArray(8)
                raf.readFully(entryBytes)
                val sampleCount = ByteBuffer.wrap(entryBytes, 0, 4).order(ByteOrder.BIG_ENDIAN).int
                val sampleDelta = ByteBuffer.wrap(entryBytes, 4, 4).order(ByteOrder.BIG_ENDIAN).int.toLong()

                repeat(sampleCount) {
                    durations.add(sampleDelta)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing stts atom", e)
        }
        return durations
    }

    /**
     * Parse stco/co64 (chunk offset) atom
     */
    private fun parseStcoAtom(raf: java.io.RandomAccessFile, offset: Long, use64Bit: Boolean): List<Long> {
        val offsets = mutableListOf<Long>()
        try {
            raf.seek(offset + 12) // Skip header + version/flags
            val entryCountBytes = ByteArray(4)
            raf.readFully(entryCountBytes)
            val entryCount = ByteBuffer.wrap(entryCountBytes).order(ByteOrder.BIG_ENDIAN).int

            for (i in 0 until entryCount.coerceAtMost(10000)) {
                if (use64Bit) {
                    val offsetBytes = ByteArray(8)
                    raf.readFully(offsetBytes)
                    offsets.add(ByteBuffer.wrap(offsetBytes).order(ByteOrder.BIG_ENDIAN).long)
                } else {
                    val offsetBytes = ByteArray(4)
                    raf.readFully(offsetBytes)
                    offsets.add(ByteBuffer.wrap(offsetBytes).order(ByteOrder.BIG_ENDIAN).int.toLong())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing stco atom", e)
        }
        return offsets
    }

    /**
     * Parse stsz (sample size) atom
     */
    private fun parseStszAtom(raf: java.io.RandomAccessFile, offset: Long): List<Int> {
        val sizes = mutableListOf<Int>()
        try {
            raf.seek(offset + 12) // Skip header + version/flags
            val sampleSizeBytes = ByteArray(4)
            raf.readFully(sampleSizeBytes)
            val uniformSize = ByteBuffer.wrap(sampleSizeBytes).order(ByteOrder.BIG_ENDIAN).int

            val countBytes = ByteArray(4)
            raf.readFully(countBytes)
            val sampleCount = ByteBuffer.wrap(countBytes).order(ByteOrder.BIG_ENDIAN).int

            if (uniformSize > 0) {
                // All samples have the same size
                repeat(sampleCount.coerceAtMost(10000)) {
                    sizes.add(uniformSize)
                }
            } else {
                // Read individual sizes
                for (i in 0 until sampleCount.coerceAtMost(10000)) {
                    val sizeBytes = ByteArray(4)
                    raf.readFully(sizeBytes)
                    sizes.add(ByteBuffer.wrap(sizeBytes).order(ByteOrder.BIG_ENDIAN).int)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing stsz atom", e)
        }
        return sizes
    }

    /**
     * Find a top-level atom in the file
     */
    private fun findTopLevelAtom(raf: java.io.RandomAccessFile, atomName: String): Long {
        var pos = 0L
        val searchBytes = atomName.toByteArray(Charsets.US_ASCII)

        try {
            while (pos < raf.length() - 8) {
                raf.seek(pos)
                val header = ByteArray(8)
                raf.readFully(header)

                val size = ByteBuffer.wrap(header, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toLong()
                val name = String(header, 4, 4, Charsets.US_ASCII)

                if (name == atomName) {
                    return pos
                }

                if (size < 8) break // Invalid size
                pos += size
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding top-level atom $atomName", e)
        }

        return -1
    }

    /**
     * Find all atoms of a specific type within a range
     */
    private fun findAtomsInRange(raf: java.io.RandomAccessFile, atomName: String, start: Long, end: Long): List<Long> {
        val found = mutableListOf<Long>()
        var pos = start

        try {
            while (pos < end - 8) {
                raf.seek(pos)
                val header = ByteArray(8)
                raf.readFully(header)

                val size = ByteBuffer.wrap(header, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toLong()
                val name = String(header, 4, 4, Charsets.US_ASCII)

                if (name == atomName) {
                    found.add(pos)
                }

                if (size < 8) break
                pos += size
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding atoms $atomName in range", e)
        }

        return found
    }

    /**
     * Find an atom within a range
     */
    private fun findAtomInRange(raf: java.io.RandomAccessFile, atomName: String, start: Long, end: Long): Long {
        var pos = start

        try {
            while (pos < end - 8) {
                raf.seek(pos)
                val header = ByteArray(8)
                raf.readFully(header)

                val size = ByteBuffer.wrap(header, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toLong()
                val name = String(header, 4, 4, Charsets.US_ASCII)

                if (name == atomName) {
                    return pos
                }

                if (size < 8) break
                pos += size
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding atom $atomName in range", e)
        }

        return -1
    }

    /**
     * Parse the 'chpl' (chapter list) atom from MP4 container using streaming
     * Does NOT load entire file into memory - uses RandomAccessFile
     */
    private fun parseChplAtomStreaming(file: File): List<Chapter> {
        val chapters = mutableListOf<Chapter>()

        try {
            java.io.RandomAccessFile(file, "r").use { raf ->
                // Search for chpl atom - it's usually in the moov atom near start or end
                // First search the first 10MB
                val chplOffset = findAtomInFile(raf, "chpl", 0, minOf(file.length(), 10 * 1024 * 1024))

                if (chplOffset < 0) {
                    // Try searching from end of file (moov is sometimes at end)
                    val searchStart = maxOf(0, file.length() - 10 * 1024 * 1024)
                    val chplOffsetEnd = findAtomInFile(raf, "chpl", searchStart, file.length())
                    if (chplOffsetEnd < 0) {
                        Log.d(TAG, "No chpl atom found")
                        return emptyList()
                    }
                    return parseChplAtomAt(raf, chplOffsetEnd)
                }

                return parseChplAtomAt(raf, chplOffset)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing chpl atom (streaming)", e)
            return emptyList()
        }
    }

    /**
     * Find an atom in file using streaming (does not load entire file)
     */
    private fun findAtomInFile(raf: java.io.RandomAccessFile, atomName: String, start: Long, end: Long): Long {
        val searchBytes = atomName.toByteArray(Charsets.US_ASCII)
        val buffer = ByteArray(4096)
        var pos = start

        raf.seek(pos)

        while (pos < end) {
            val bytesRead = raf.read(buffer)
            if (bytesRead <= 0) break

            // Search for atom name in buffer
            for (i in 0 until bytesRead - 4) {
                var found = true
                for (j in searchBytes.indices) {
                    if (buffer[i + j] != searchBytes[j]) {
                        found = false
                        break
                    }
                }
                if (found) {
                    // Return position of atom size (4 bytes before name)
                    return pos + i - 4
                }
            }

            // Move position, overlapping to catch atoms at buffer boundaries
            pos += bytesRead - 8
            raf.seek(pos)
        }

        return -1
    }

    /**
     * Parse chpl atom at given offset
     *
     * chpl atom structure (Nero chapter format):
     * - 4 bytes: atom size
     * - 4 bytes: atom type ('chpl')
     * - 1 byte: version (0 or 1)
     * - 3 bytes: flags (usually 0)
     * - 4 bytes: reserved (version 0) OR unknown (version 1)
     * - 1 byte: chapter count (version 0) OR 4 bytes: chapter count (version 1)
     *
     * For each chapter:
     * - 8 bytes: start time in 100-nanosecond units
     * - 1 byte: title length
     * - N bytes: title (UTF-8)
     */
    private fun parseChplAtomAt(raf: java.io.RandomAccessFile, offset: Long): List<Chapter> {
        val chapters = mutableListOf<Chapter>()

        try {
            raf.seek(offset)

            // Read atom header (8 bytes: 4 size + 4 type)
            val header = ByteArray(8)
            raf.readFully(header)

            val atomSize = ByteBuffer.wrap(header, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFFFFFFL
            val atomType = String(header, 4, 4, Charsets.US_ASCII)

            Log.d(TAG, "Parsing chpl atom: type=$atomType, size=$atomSize at offset $offset")

            if (atomType != "chpl") {
                Log.e(TAG, "Expected chpl atom but found: $atomType")
                return emptyList()
            }

            // Read version (1 byte) and flags (3 bytes)
            val version = raf.read() and 0xFF
            val flags = ByteArray(3)
            raf.readFully(flags)

            Log.d(TAG, "chpl version: $version")

            val chapterCount: Int

            if (version == 0) {
                // Version 0: 4 bytes reserved, then 1 byte chapter count
                raf.skipBytes(4) // reserved
                chapterCount = raf.read() and 0xFF
            } else {
                // Version 1 or higher: Try reading 4 bytes for chapter count
                // Some implementations skip the reserved bytes entirely
                val countBytes = ByteArray(4)
                raf.readFully(countBytes)
                val potentialCount = ByteBuffer.wrap(countBytes).order(ByteOrder.BIG_ENDIAN).int

                // Sanity check - if count seems too large, try 1-byte interpretation
                chapterCount = if (potentialCount > 1000 || potentialCount < 0) {
                    // Fallback: skip 4 reserved bytes and read 1 byte count
                    raf.seek(offset + 12 + 4) // header + version/flags + reserved
                    raf.read() and 0xFF
                } else {
                    potentialCount
                }
            }

            Log.d(TAG, "Chapter count from chpl: $chapterCount")

            if (chapterCount <= 0 || chapterCount > 1000) {
                Log.w(TAG, "Invalid chapter count: $chapterCount")
                return emptyList()
            }

            for (i in 0 until chapterCount) {
                try {
                    // Read start time (8 bytes, 100-nanosecond units)
                    val timeBytes = ByteArray(8)
                    raf.readFully(timeBytes)
                    val startTime100ns = ByteBuffer.wrap(timeBytes).order(ByteOrder.BIG_ENDIAN).long
                    val startMs = startTime100ns / 10_000

                    // Read title length (1 byte)
                    val titleLength = raf.read() and 0xFF

                    // Sanity check title length
                    if (titleLength > 500) {
                        Log.w(TAG, "Suspicious title length: $titleLength, stopping")
                        break
                    }

                    // Read title
                    val title = if (titleLength > 0) {
                        val titleBytes = ByteArray(titleLength)
                        raf.readFully(titleBytes)
                        String(titleBytes, Charsets.UTF_8)
                    } else {
                        "Chapter ${i + 1}"
                    }

                    Log.d(TAG, "Chapter $i: '$title' at ${startMs}ms")

                    chapters.add(Chapter(
                        title = title.trim().ifEmpty { "Chapter ${i + 1}" },
                        startMs = startMs,
                        endMs = 0L,
                        filePath = null
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing chapter $i", e)
                    break
                }
            }

            // Calculate end times
            for (i in 0 until chapters.size - 1) {
                chapters[i] = chapters[i].copy(endMs = chapters[i + 1].startMs)
            }

            Log.d(TAG, "Successfully parsed ${chapters.size} chapters from chpl atom")
            return chapters
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing chpl atom at offset $offset", e)
            return emptyList()
        }
    }

    /**
     * Extract chapters from MP3 using manual ID3v2 CHAP frame parsing
     * JAudioTagger's CHAP frame support is limited, so we parse manually
     */
    private fun extractMp3ChaptersJaudiotagger(file: File): List<Chapter> {
        Log.d(TAG, "Extracting MP3 chapters from: ${file.name}")
        // Use manual parsing for MP3 chapters (more reliable)
        return extractMp3ChaptersManual(file)
    }

    /**
     * Fallback manual MP3 chapter extraction
     */
    private fun extractMp3ChaptersManual(file: File): List<Chapter> {
        try {
            val bytes = file.readBytes()

            // Check ID3v2 header
            if (bytes.size < 10 ||
                bytes[0] != 'I'.code.toByte() ||
                bytes[1] != 'D'.code.toByte() ||
                bytes[2] != '3'.code.toByte()) {
                return emptyList()
            }

            val tagSize = synchsafeToInt(bytes, 6)
            if (tagSize <= 0 || tagSize > bytes.size - 10) return emptyList()

            val tagData = bytes.copyOfRange(10, 10 + tagSize)
            return parseId3Frames(tagData)
        } catch (e: Exception) {
            Log.e(TAG, "Error in manual MP3 chapter extraction", e)
            return emptyList()
        }
    }

    /**
     * Parse ID3v2 frames to extract CHAP frames
     */
    private fun parseId3Frames(tagData: ByteArray): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        var offset = 0

        try {
            while (offset < tagData.size - 10) {
                val frameId = String(tagData.copyOfRange(offset, offset + 4))
                offset += 4

                val frameSize = synchsafeToInt(tagData, offset)
                offset += 4
                offset += 2 // flags

                if (frameSize <= 0 || frameSize > tagData.size - offset) break

                if (frameId == "CHAP") {
                    val chapter = parseChapFrame(tagData, offset, frameSize)
                    if (chapter != null) {
                        chapters.add(chapter)
                    }
                }

                offset += frameSize
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing ID3v2 frames", e)
        }

        return chapters.sortedBy { it.startMs }
    }

    private fun parseChapFrame(data: ByteArray, offset: Int, size: Int): Chapter? {
        try {
            var pos = offset

            // Read element ID (null-terminated)
            val elementIdEnd = data.indexOf(0, pos)
            if (elementIdEnd < 0) return null
            pos = elementIdEnd + 1

            // Read times
            val startMs = bytesToInt(data, pos).toLong()
            pos += 4
            val endMs = bytesToInt(data, pos).toLong()
            pos += 4
            pos += 8 // skip offsets

            // Read embedded TIT2
            var title = "Chapter"
            while (pos < offset + size - 10) {
                val subFrameId = String(data.copyOfRange(pos, pos + 4))
                pos += 4
                val subFrameSize = synchsafeToInt(data, pos)
                pos += 4
                pos += 2

                if (subFrameId == "TIT2" && subFrameSize > 0) {
                    pos += 1 // encoding byte
                    title = String(data.copyOfRange(pos, pos + subFrameSize - 1))
                    break
                }
                pos += subFrameSize
            }

            return Chapter(
                title = title.trim(),
                startMs = startMs,
                endMs = endMs,
                filePath = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing CHAP frame", e)
            return null
        }
    }

    /**
     * Extract chapters from OGG using VorbisComment tags
     * Format: CHAPTERxxx=HH:MM:SS.mmm, CHAPTERxxxNAME=title
     */
    private fun extractVorbisChaptersJaudiotagger(file: File): List<Chapter> {
        Log.d(TAG, "Extracting OGG chapters from: ${file.name}")
        return extractVorbisCommentChapters(file)
    }

    /**
     * Extract chapters from FLAC using VorbisComment tags
     */
    private fun extractFlacChaptersJaudiotagger(file: File): List<Chapter> {
        Log.d(TAG, "Extracting FLAC chapters from: ${file.name}")
        return extractVorbisCommentChapters(file)
    }

    /**
     * Common VorbisComment chapter extraction for OGG and FLAC
     */
    private fun extractVorbisCommentChapters(file: File): List<Chapter> {
        try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag ?: return emptyList()

            val chapters = mutableMapOf<Int, Chapter>()

            // VorbisComment chapter format:
            // CHAPTER001=00:00:00.000
            // CHAPTER001NAME=Introduction
            // CHAPTER002=00:05:30.000
            // CHAPTER002NAME=Chapter 1

            val chapterTimePattern = Regex("CHAPTER(\\d+)=(.+)", RegexOption.IGNORE_CASE)
            val chapterNamePattern = Regex("CHAPTER(\\d+)NAME=(.+)", RegexOption.IGNORE_CASE)

            // Get all tag fields using iterator
            val fieldIterator = tag.fields ?: return emptyList()

            fieldIterator.forEach { field ->
                val key = field.id?.uppercase() ?: return@forEach
                val value = field.toString()

                // Extract the field value after the ID
                val fieldValue = if (value.contains("=")) {
                    value.substringAfter("=").trim()
                } else {
                    value.trim()
                }

                // Match chapter time
                val timeMatch = chapterTimePattern.find(key + "=" + fieldValue)
                if (timeMatch != null) {
                    val num = timeMatch.groupValues[1].toIntOrNull() ?: return@forEach
                    val timeStr = timeMatch.groupValues[2]
                    val timeMs = parseVorbisTimestamp(timeStr)
                    if (timeMs >= 0) {
                        val existing = chapters[num]
                        chapters[num] = Chapter(
                            title = existing?.title ?: "Chapter $num",
                            startMs = timeMs,
                            endMs = 0L,
                            filePath = null
                        )
                    }
                }

                // Match chapter name
                val nameMatch = chapterNamePattern.find(key + "=" + fieldValue)
                if (nameMatch != null) {
                    val num = nameMatch.groupValues[1].toIntOrNull() ?: return@forEach
                    val name = nameMatch.groupValues[2]
                    val existing = chapters[num]
                    if (existing != null) {
                        chapters[num] = existing.copy(title = name.trim())
                    } else {
                        chapters[num] = Chapter(
                            title = name.trim(),
                            startMs = 0L,
                            endMs = 0L,
                            filePath = null
                        )
                    }
                }
            }

            // Convert to list and calculate end times
            val chapterList = chapters.values.sortedBy { it.startMs }.toMutableList()
            for (i in 0 until chapterList.size - 1) {
                chapterList[i] = chapterList[i].copy(endMs = chapterList[i + 1].startMs)
            }

            Log.d(TAG, "Found ${chapterList.size} VorbisComment chapters")
            return chapterList
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting VorbisComment chapters", e)
            return emptyList()
        }
    }

    /**
     * Parse VorbisComment timestamp (HH:MM:SS.mmm or HH:MM:SS)
     */
    private fun parseVorbisTimestamp(timestamp: String): Long {
        try {
            val parts = timestamp.split(":")
            if (parts.size != 3) return -1

            val hours = parts[0].toLongOrNull() ?: return -1
            val minutes = parts[1].toLongOrNull() ?: return -1
            val secondsParts = parts[2].split(".")
            val seconds = secondsParts[0].toLongOrNull() ?: return -1
            val millis = if (secondsParts.size > 1) {
                secondsParts[1].padEnd(3, '0').take(3).toLongOrNull() ?: 0
            } else 0

            return (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
        } catch (e: Exception) {
            return -1
        }
    }

    /**
     * Create a temporary file from a content URI
     */
    private fun createTempFileFromUri(context: Context, uri: Uri): File? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Determine extension from MIME type or URI
            val mimeType = context.contentResolver.getType(uri)
            val extension = when {
                mimeType?.contains("mp4") == true -> "m4b"
                mimeType?.contains("m4a") == true -> "m4a"
                mimeType?.contains("m4b") == true -> "m4b"
                mimeType?.contains("mpeg") == true -> "mp3"
                mimeType?.contains("ogg") == true -> "ogg"
                mimeType?.contains("flac") == true -> "flac"
                uri.path?.endsWith(".m4b", true) == true -> "m4b"
                uri.path?.endsWith(".m4a", true) == true -> "m4a"
                uri.path?.endsWith(".mp3", true) == true -> "mp3"
                uri.path?.endsWith(".ogg", true) == true -> "ogg"
                uri.path?.endsWith(".flac", true) == true -> "flac"
                else -> "tmp"
            }

            val tempFile = File.createTempFile("chapter_extract_", ".$extension", context.cacheDir)

            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }

            return tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create temp file", e)
            return null
        }
    }

    /**
     * Convert synchsafe integer to normal integer
     */
    private fun synchsafeToInt(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0x7F) shl 21) or
                ((data[offset + 1].toInt() and 0x7F) shl 14) or
                ((data[offset + 2].toInt() and 0x7F) shl 7) or
                (data[offset + 3].toInt() and 0x7F)
    }

    /**
     * Convert 4 bytes to integer (big-endian)
     */
    private fun bytesToInt(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 24) or
                ((data[offset + 1].toInt() and 0xFF) shl 16) or
                ((data[offset + 2].toInt() and 0xFF) shl 8) or
                (data[offset + 3].toInt() and 0xFF)
    }

    /**
     * Find byte in array
     */
    private fun ByteArray.indexOf(byte: Byte, fromIndex: Int): Int {
        for (i in fromIndex until size) {
            if (this[i] == byte) return i
        }
        return -1
    }
}
