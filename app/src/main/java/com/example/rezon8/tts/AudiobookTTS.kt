package com.mossglen.reverie.tts

import android.content.Context

/**
 * Common interface for TTS engines.
 * Allows the app to switch between System TTS and Sherpa-ONNX (Kokoro) seamlessly.
 * The rest of the app doesn't need to know which engine is running.
 */
interface AudiobookTTS {

    /**
     * Initialize the TTS engine.
     * @param context Application context
     * @param onSuccess Callback when initialization is complete
     * @param onError Callback when initialization fails with error message
     */
    fun initialize(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    )

    /**
     * Speak the given text.
     * @param text The text to speak
     */
    fun speak(text: String)

    /**
     * Stop any ongoing speech.
     */
    fun stop()

    /**
     * Set the speech rate/speed.
     * @param rate Speed multiplier (0.5 = half speed, 1.0 = normal, 2.0 = double speed)
     */
    fun setSpeed(rate: Float)

    /**
     * Release all resources. Call when done with TTS.
     */
    fun release()

    /**
     * Check if the engine is currently speaking.
     */
    fun isSpeaking(): Boolean

    /**
     * Get the engine name for display purposes.
     */
    fun getEngineName(): String

    /**
     * Check if the engine is ready to speak.
     */
    fun isReady(): Boolean
}

/**
 * Enum for TTS engine types
 */
enum class TtsEngineType {
    SYSTEM,     // Native Android System TTS
    SHERPA      // Sherpa-ONNX Kokoro model
}

/**
 * Data class for TTS settings
 */
data class TtsSettings(
    val engineType: TtsEngineType = TtsEngineType.SYSTEM,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val voiceId: Int = 0  // For Kokoro multi-speaker support
)
