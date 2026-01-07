package com.example.rezon.data

import com.example.rezon.data.api.GoogleBooksApi
import com.example.rezon.data.api.VolumeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Repository for fetching rich metadata from Google Books API
 */
class MetadataRepository @Inject constructor(
    private val googleBooksApi: GoogleBooksApi,
    private val bookDao: BookDao
) {

    /**
     * Fetches metadata for a book from Google Books API and updates the database
     */
    suspend fun fetchAndUpdateMetadata(book: Book): Book? = withContext(Dispatchers.IO) {
        try {
            // Search by title and author for best results
            val query = buildSearchQuery(book.title, book.author)
            val response = googleBooksApi.searchBooks(query)

            val bestMatch = response.items?.firstOrNull()?.volumeInfo
            if (bestMatch != null) {
                val updatedBook = book.copy(
                    description = bestMatch.description,
                    series = extractSeriesInfo(bestMatch),
                    coverUrl = book.coverUrl ?: bestMatch.imageLinks?.thumbnail?.replace("http://", "https://"),
                    categories = bestMatch.categories?.joinToString(", ")
                )
                bookDao.updateBook(updatedBook)
                return@withContext updatedBook
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetches metadata for all books that don't have descriptions yet
     */
    suspend fun fetchMetadataForLibrary(books: List<Book>) = withContext(Dispatchers.IO) {
        books.filter { it.description.isNullOrBlank() }
            .forEach { book ->
                fetchAndUpdateMetadata(book)
            }
    }

    private fun buildSearchQuery(title: String, author: String): String {
        val cleanTitle = title
            .replace(Regex("\\[.*?]"), "") // Remove brackets content
            .replace(Regex("\\(.*?\\)"), "") // Remove parentheses content
            .trim()

        return if (author.isNotBlank() && author != "Unknown Author") {
            "intitle:$cleanTitle+inauthor:$author"
        } else {
            "intitle:$cleanTitle"
        }
    }

    private fun extractSeriesInfo(volumeInfo: VolumeInfo): String? {
        return volumeInfo.seriesInfo?.let { series ->
            val seriesTitle = series.shortSeriesBookTitle
            val bookNumber = series.bookDisplayNumber
            if (seriesTitle != null && bookNumber != null) {
                "$seriesTitle #$bookNumber"
            } else {
                seriesTitle
            }
        }
    }
}
