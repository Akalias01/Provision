package com.mossglen.lithos

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mossglen.lithos.util.CrashReporter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * LITHOS Application Class
 *
 * Handles app-wide initialization including:
 * - Hilt dependency injection
 * - WorkManager with HiltWorkerFactory
 * - Global crash reporting
 * - Uncaught exception handling
 */
@HiltAndroidApp
class LithosApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Set up global uncaught exception handler
        setupGlobalExceptionHandler()

        Log.d(TAG, "LITHOS App initialized")
        CrashReporter.log("LITHOS App started")
    }

    /**
     * Global exception handler to catch uncaught exceptions
     * Logs them via CrashReporter before the app crashes
     * Does NOT swallow exceptions - app will still crash as expected
     */
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Log the crash via CrashReporter
                Log.e(TAG, "Uncaught exception in thread: ${thread.name}", throwable)
                CrashReporter.logError("FATAL: Uncaught exception in ${thread.name}", throwable)

                // Add thread context
                CrashReporter.setCustomKey("crash_thread", thread.name)
                CrashReporter.setCustomKey("crash_thread_id", thread.id.toString())

            } catch (e: Exception) {
                // Don't let exception handling crash the exception handler
                Log.e(TAG, "Error in exception handler", e)
            } finally {
                // Call the original handler to let the app crash normally
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    companion object {
        private const val TAG = "LithosApp"
    }
}
