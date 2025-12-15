package com.example.rezon.data

import com.example.rezon.data.remote.GoogleBooksApi
import javax.inject.Inject

class MetadataRepository @Inject constructor(
    private val api: GoogleBooksApi,
    private val bookDao: BookDao
) {
    suspend fun fetchAndSaveMetadata(book: Book) {
        try {
            // Search by Title + Author for accuracy
            val query = "intitle:${book.title}+inauthor:${book.author}"
            val response = api.searchBooks(query)

            response.items?.firstOrNull()?.volumeInfo?.let { info ->
                // We found a match! Update the local database.
                val updatedBook = book.copy(
                    synopsis = info.description ?: "No synopsis available.",
                    seriesInfo = info.subtitle ?: info.categories?.joinToString() ?: ""
                )
                bookDao.updateBook(updatedBook)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fail silently, keep existing data
        }
    }
}
