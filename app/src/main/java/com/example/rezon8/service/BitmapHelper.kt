package com.mossglen.lithos.service

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * BitmapHelper - Utility object for loading and resizing album art.
 *
 * Critical: Android Auto has a strict size limit for bitmaps passed via IPC.
 * Sending images larger than ~512x512 can cause TransactionTooLargeException.
 *
 * This helper ensures all album art is properly sized before sending to Auto.
 */
object BitmapHelper {
    private const val TAG = "LithosAutoDebug"

    /**
     * Maximum dimension for album art to prevent TransactionTooLargeException.
     * 512x512 is the safe maximum for Android Auto IPC.
     */
    const val MAX_ART_SIZE = 512

    /**
     * Load and resize an image to fit within MAX_ART_SIZE x MAX_ART_SIZE.
     * Uses Coil for efficient loading and caching.
     *
     * @param context Application context
     * @param imageLoader Coil ImageLoader instance
     * @param imageUrl URL or file path of the image
     * @return Resized bitmap or null if loading failed
     */
    suspend fun loadAndResize(
        context: Context,
        imageLoader: ImageLoader,
        imageUrl: String?
    ): Bitmap? {
        if (imageUrl.isNullOrEmpty()) {
            Log.d(TAG, "⚡ BitmapHelper: No image URL provided")
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "⚡ BitmapHelper: Loading image from $imageUrl")

                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(MAX_ART_SIZE, MAX_ART_SIZE)
                    .allowHardware(false) // Required for getting software bitmap for IPC
                    .build()

                val result = imageLoader.execute(request)

                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap(MAX_ART_SIZE, MAX_ART_SIZE)
                    Log.d(TAG, "⚡ BitmapHelper: Successfully loaded ${bitmap.width}x${bitmap.height} bitmap")
                    bitmap
                } else {
                    Log.w(TAG, "⚡ BitmapHelper: Failed to load image - result was not success")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "⚡ BitmapHelper: Exception loading image: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Calculate the appropriate sample size for downscaling a bitmap.
     *
     * @param width Original width
     * @param height Original height
     * @param maxSize Maximum desired dimension
     * @return Sample size for BitmapFactory.Options
     */
    fun calculateSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > maxSize || height / sampleSize > maxSize) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
