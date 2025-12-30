package com.mossglen.reverie.tts

import com.google.gson.annotations.SerializedName

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
