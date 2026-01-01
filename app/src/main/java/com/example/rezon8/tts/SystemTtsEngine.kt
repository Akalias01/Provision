package com.mossglen.lithos.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

/**
 * Engine A: Native Android System TTS Implementation.
 * Uses android.speech.tts.TextToSpeech for speech synthesis.
 * This is the default engine and works out of the box on all Android devices.
 */
class SystemTtsEngine : AudiobookTTS {

    companion object {
        private const val TAG = "SystemTtsEngine"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentSpeed = 1.0f

    // Callbacks for speech events (optional)
    var onSpeechStart: (() -> Unit)? = null
    var onSpeechDone: (() -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null

    override fun initialize(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "Initializing System TTS...")

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set default language to US English
                val result = tts?.setLanguage(Locale.US)

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "US English not supported, trying device default")
                    tts?.setLanguage(Locale.getDefault())
                }

                // Set up utterance progress listener
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "Speech started: $utteranceId")
                        onSpeechStart?.invoke()
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "Speech completed: $utteranceId")
                        onSpeechDone?.invoke()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "Speech error: $utteranceId")
                        onSpeechError?.invoke("Speech synthesis error")
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e(TAG, "Speech error: $utteranceId, code: $errorCode")
                        onSpeechError?.invoke("Speech synthesis error: $errorCode")
                    }
                })

                isInitialized = true
                Log.d(TAG, "System TTS initialized successfully")
                onSuccess()

            } else {
                val errorMsg = "System TTS initialization failed with status: $status"
                Log.e(TAG, errorMsg)
                isInitialized = false
                onError(errorMsg)
            }
        }
    }

    override fun speak(text: String) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not initialized, cannot speak")
            return
        }

        if (text.isBlank()) {
            Log.w(TAG, "Empty text, nothing to speak")
            return
        }

        val utteranceId = UUID.randomUUID().toString()

        // Handle long text by chunking (TTS has input limits)
        val maxLength = TextToSpeech.getMaxSpeechInputLength()
        if (text.length > maxLength) {
            Log.d(TAG, "Text too long (${text.length}), chunking into smaller pieces")
            val chunks = text.chunked(maxLength)
            chunks.forEachIndexed { index, chunk ->
                val chunkId = "${utteranceId}_$index"
                val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                tts?.speak(chunk, queueMode, null, chunkId)
            }
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    override fun stop() {
        tts?.stop()
        Log.d(TAG, "Speech stopped")
    }

    override fun setSpeed(rate: Float) {
        currentSpeed = rate.coerceIn(0.25f, 4.0f)
        tts?.setSpeechRate(currentSpeed)
        Log.d(TAG, "Speed set to: $currentSpeed")
    }

    /**
     * Set speech pitch (System TTS specific).
     * @param pitch Pitch multiplier (0.5 = lower, 1.0 = normal, 2.0 = higher)
     */
    fun setPitch(pitch: Float) {
        val clampedPitch = pitch.coerceIn(0.25f, 2.0f)
        tts?.setPitch(clampedPitch)
        Log.d(TAG, "Pitch set to: $clampedPitch")
    }

    override fun release() {
        Log.d(TAG, "Releasing System TTS...")
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    override fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    override fun getEngineName(): String = "System TTS"

    override fun isReady(): Boolean = isInitialized && tts != null

    /**
     * Get list of available voices.
     */
    fun getAvailableVoices(): List<android.speech.tts.Voice> {
        return tts?.voices?.toList() ?: emptyList()
    }

    /**
     * Set a specific voice.
     */
    fun setVoice(voice: android.speech.tts.Voice) {
        tts?.voice = voice
        Log.d(TAG, "Voice set to: ${voice.name}")
    }

    /**
     * Set language/locale.
     */
    fun setLanguage(locale: Locale): Boolean {
        val result = tts?.setLanguage(locale)
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }
}
