package com.rezon.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

/**
 * REZON - Audiobooks Reimagined
 *
 * Main Application class with Hilt dependency injection
 * and optimized image loading for album art
 */
@HiltAndroidApp
class RezonApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    /**
     * Configure Coil with aggressive caching for smooth scrolling
     * Album art should never cause lag
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of app memory for images
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100MB disk cache
                    .build()
            }
            .respectCacheHeaders(false) // Always cache album art
            .crossfade(true)
            .crossfade(300)
            .build()
    }

    companion object {
        lateinit var instance: RezonApplication
            private set
    }
}
