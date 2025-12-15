package com.example.rezon.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
                    val book = Book(
                        id = path,
                        title = title,
                        author = artist,
                        coverUrl = null, // In real app, extract embedded art
                        filePath = path,
                        duration = duration
                    )
                    bookDao.insertBook(book)
                }
            }
        }
    }
}
