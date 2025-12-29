package com.example.rezon.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class LibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao
) {
    val allBooks = bookDao.getAllBooks()

    suspend fun scanDeviceForAudiobooks() = withContext(Dispatchers.IO) {
        // 1. Query MediaStore for Audio files (Fastest method)
        val projection = arrayOf(
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )

        // Filter for music/audiobooks
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.IS_AUDIOBOOK} != 0"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val path = cursor.getString(pathCol)
                val title = cursor.getString(titleCol) ?: "Unknown Title"
                val artist = cursor.getString(artistCol) ?: "Unknown Author"
                val duration = cursor.getLong(durCol)

                // Simple filter to ignore short notification sounds (< 1 min)
                if (duration > 60_000) {
                    // Extract cover art from embedded metadata
                    val coverPath = extractCoverArt(path)

                    val book = Book(
                        id = path,
                        title = title,
                        author = artist,
                        coverUrl = coverPath,
                        filePath = path,
                        duration = duration
                    )
                    bookDao.insertBook(book)
                }
            }
        }
    }

    /**
     * Extracts embedded cover art from an audio file and caches it to internal storage.
     * Returns the file path to the cached image, or null if no art is found.
     */
    private fun extractCoverArt(audioPath: String): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(audioPath)
            val artBytes = retriever.embeddedPicture ?: return null

            // Cache cover art to app's internal storage
            val fileName = "cover_${audioPath.hashCode()}.jpg"
            val file = File(context.filesDir, fileName)

            // Only write if not already cached
            if (!file.exists()) {
                FileOutputStream(file).use { it.write(artBytes) }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }
}
