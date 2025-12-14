package com.rezon.app.presentation.navigation

/**
 * REZON Navigation Routes
 *
 * Sealed class defining all navigation destinations in the app.
 * Using sealed class for type-safe navigation.
 */
sealed class Route(val route: String, val title: String) {

    /**
     * Library - Main screen showing all audiobooks
     */
    data object Library : Route(
        route = "library",
        title = "Library"
    )

    /**
     * Player - Full-screen audiobook player
     * @param bookId The ID of the book to play
     */
    data object Player : Route(
        route = "player/{bookId}",
        title = "Now Playing"
    ) {
        fun createRoute(bookId: String) = "player/$bookId"
        const val ARG_BOOK_ID = "bookId"
    }

    /**
     * Torrents - Torrent download manager
     */
    data object Torrents : Route(
        route = "torrents",
        title = "Downloads"
    )

    /**
     * Cloud - Cloud storage browser (Google Drive, Dropbox)
     */
    data object Cloud : Route(
        route = "cloud",
        title = "Cloud Storage"
    )

    /**
     * Settings - App settings and preferences
     */
    data object Settings : Route(
        route = "settings",
        title = "Settings"
    )

    /**
     * Folders - Manage folders to scan
     */
    data object Folders : Route(
        route = "folders",
        title = "Folders to Scan"
    )

    companion object {
        /**
         * Get Route by route string
         */
        fun fromRoute(route: String?): Route {
            return when {
                route == null -> Library
                route == Library.route -> Library
                route.startsWith("player/") -> Player
                route == Torrents.route -> Torrents
                route == Cloud.route -> Cloud
                route == Settings.route -> Settings
                route == Folders.route -> Folders
                else -> Library
            }
        }

        /**
         * All drawer menu routes
         */
        val drawerRoutes = listOf(
            Library,
            Torrents,
            Cloud,
            Folders,
            Settings
        )
    }
}
