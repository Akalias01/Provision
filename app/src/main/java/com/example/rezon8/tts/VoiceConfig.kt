package com.mossglen.lithos.tts

import com.google.gson.annotations.SerializedName

/**
 * Supported TTS model types.
 */
enum class TtsModelType {
    KOKORO,  // Kokoro model (high quality, larger ~250MB)
    PIPER    // Piper VITS model (faster, smaller ~30-90MB)
}

/**
 * Data class representing the remote voice model configuration.
 *
 * Expected JSON structure:
 * ```json
 * {
 *   "version": "1.0",
 *   "download_url": "https://example.com/kokoro-v0.19.tar.bz2"
 * }
 * ```
 */
data class VoiceConfig(
    @SerializedName("version")
    val version: String,

    @SerializedName("download_url")
    val downloadUrl: String
)

/**
 * Piper model configurations available for download.
 */
object PiperModels {
    // English models from sherpa-onnx releases
    const val EN_AMY_LOW = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-amy-low.tar.bz2"
    const val EN_LIBRITTS_MEDIUM = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-libritts_r-medium.tar.bz2"
    const val EN_LESSAC_MEDIUM = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-lessac-medium.tar.bz2"

    // Default model for fast, low-latency TTS
    const val DEFAULT = EN_AMY_LOW
}
