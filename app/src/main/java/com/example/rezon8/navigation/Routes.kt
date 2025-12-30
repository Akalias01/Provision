package com.mossglen.reverie.navigation

import kotlinx.serialization.Serializable

/**
 * REVERIE - Type-Safe Navigation Routes
 *
 * Using Kotlin Serialization for compile-time route safety.
 * No more string-based routes like "book_detail/{bookId}".
 *
 * Benefits:
 * - Compile-time argument validation
 * - IDE autocomplete for routes
 * - Refactoring-safe navigation
 * - Type-safe argument passing
 */

// ============================================================================
// MAIN DESTINATIONS (Bottom Navigation)
// ============================================================================

@Serializable
data object Welcome

@Serializable
data object Home

@Serializable
data object Now  // Immersive Now screen - the listening sanctuary

@Serializable
data object Journey  // Stats, goals, achievements - your listening journey

@Serializable
data object Library

@Serializable
data object Search

@Serializable
data object Settings

@Serializable
data object Profile  // Settings, stats, achievements, account

@Serializable
data object Downloads

// ============================================================================
// DETAIL DESTINATIONS (With Arguments)
// ============================================================================

@Serializable
data class BookDetail(val bookId: String)

@Serializable
data class Reader(val bookId: String)

@Serializable
data class EditBook(val bookId: String)

// ============================================================================
// PLAYER DESTINATIONS
// ============================================================================

@Serializable
data object NowPlaying

// ============================================================================
// FUTURE DESTINATIONS (Ready for expansion)
// ============================================================================

@Serializable
data class ChapterList(val bookId: String)

@Serializable
data class Bookmarks(val bookId: String)

@Serializable
data object CloudSync

@Serializable
data object Equalizer

@Serializable
data object ListeningStats

@Serializable
data class AuthorBooks(val authorName: String)

@Serializable
data class SeriesDetail(val seriesName: String)

@Serializable
data class GenreBooks(val genre: String)

@Serializable
data class SplitBook(val bookId: String)

@Serializable
data object CarMode  // Driving-safe simplified interface
