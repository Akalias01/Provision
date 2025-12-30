package com.mossglen.reverie.util

import android.util.Log

/**
 * CrashReporter - Centralized crash and error logging utility
 *
 * Currently logs to Android Logcat. This is a placeholder implementation
 * that can be easily swapped to Firebase Crashlytics once configured.
 *
 * FIREBASE INTEGRATION GUIDE:
 * ===========================
 * 1. Add Firebase to your project in Firebase Console
 * 2. Download google-services.json to app/ directory
 * 3. Add to build.gradle.kts:
 *    - plugins { id("com.google.gms.google-services") version "4.4.0" }
 *    - implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
 *    - implementation("com.google.firebase:firebase-crashlytics-ktx")
 * 4. Uncomment Firebase code below and remove Logcat fallbacks
 * 5. Initialize in Application.onCreate(): FirebaseCrashlytics.getInstance()
 */
object CrashReporter {
    private const val TAG = "CrashReporter"

    /**
     * Log a non-fatal exception
     *
     * Usage: CrashReporter.logException(exception)
     *
     * Firebase equivalent:
     * FirebaseCrashlytics.getInstance().recordException(throwable)
     */
    fun logException(throwable: Throwable) {
        // Current implementation - Logcat
        Log.e(TAG, "Exception logged: ${throwable.message}", throwable)
        throwable.printStackTrace()

        // TODO: Enable after Firebase setup
        // FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    /**
     * Log a custom message/event
     *
     * Usage: CrashReporter.log("User imported file: $filename")
     *
     * Firebase equivalent:
     * FirebaseCrashlytics.getInstance().log(message)
     */
    fun log(message: String) {
        // Current implementation - Logcat
        Log.d(TAG, "Log: $message")

        // TODO: Enable after Firebase setup
        // FirebaseCrashlytics.getInstance().log(message)
    }

    /**
     * Set user identifier for crash reports
     *
     * Usage: CrashReporter.setUserId("user_12345")
     *
     * Firebase equivalent:
     * FirebaseCrashlytics.getInstance().setUserId(userId)
     */
    fun setUserId(userId: String) {
        // Current implementation - Logcat
        Log.d(TAG, "User ID set: $userId")

        // TODO: Enable after Firebase setup
        // FirebaseCrashlytics.getInstance().setUserId(userId)
    }

    /**
     * Set custom key-value pair for additional context
     *
     * Usage: CrashReporter.setCustomKey("book_format", "AUDIO")
     *
     * Firebase equivalent:
     * FirebaseCrashlytics.getInstance().setCustomKey(key, value)
     */
    fun setCustomKey(key: String, value: String) {
        // Current implementation - Logcat
        Log.d(TAG, "Custom key set: $key = $value")

        // TODO: Enable after Firebase setup
        // FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    /**
     * Log an error with context message
     *
     * Usage: CrashReporter.logError("Failed to import file", exception)
     */
    fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)

        if (throwable != null) {
            // TODO: Enable after Firebase setup
            // FirebaseCrashlytics.getInstance().log(message)
            // FirebaseCrashlytics.getInstance().recordException(throwable)
        } else {
            // TODO: Enable after Firebase setup
            // FirebaseCrashlytics.getInstance().log("ERROR: $message")
        }
    }
}
