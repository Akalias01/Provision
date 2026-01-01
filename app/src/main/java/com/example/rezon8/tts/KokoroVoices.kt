package com.mossglen.lithos.tts

/**
 * Kokoro TTS Voice definitions - ENGLISH-ONLY MODEL (kokoro-int8-en-v0.19)
 *
 * This model has 11 English voices (speaker IDs 0-10).
 *
 * Voice naming convention: {accent}{gender}_{name}
 * - a = American, b = British
 * - f = Female, m = Male
 */
object KokoroVoices {

    data class Voice(
        val id: Int,
        val name: String,
        val displayName: String,
        val gender: Gender,
        val accent: Accent,
        val description: String = ""
    )

    enum class Gender { FEMALE, MALE }
    enum class Accent { AMERICAN, BRITISH }

    // American Female voices (IDs 0-4)
    val AMERICAN_FEMALE = listOf(
        Voice(0, "af", "Default", Gender.FEMALE, Accent.AMERICAN, "Default American female"),
        Voice(1, "af_bella", "Bella", Gender.FEMALE, Accent.AMERICAN, "Warm, professional"),
        Voice(2, "af_nicole", "Nicole", Gender.FEMALE, Accent.AMERICAN, "Energetic, clear"),
        Voice(3, "af_sarah", "Sarah", Gender.FEMALE, Accent.AMERICAN, "Clear, articulate"),
        Voice(4, "af_sky", "Sky", Gender.FEMALE, Accent.AMERICAN, "Soft, dreamy")
    )

    // American Male voices (IDs 5-6)
    val AMERICAN_MALE = listOf(
        Voice(5, "am_adam", "Adam", Gender.MALE, Accent.AMERICAN, "Deep, authoritative"),
        Voice(6, "am_michael", "Michael", Gender.MALE, Accent.AMERICAN, "Friendly, conversational")
    )

    // British Female voices (IDs 7-8)
    val BRITISH_FEMALE = listOf(
        Voice(7, "bf_emma", "Emma", Gender.FEMALE, Accent.BRITISH, "Elegant British"),
        Voice(8, "bf_isabella", "Isabella", Gender.FEMALE, Accent.BRITISH, "Refined British")
    )

    // British Male voices (IDs 9-10)
    val BRITISH_MALE = listOf(
        Voice(9, "bm_george", "George", Gender.MALE, Accent.BRITISH, "Distinguished British"),
        Voice(10, "bm_lewis", "Lewis", Gender.MALE, Accent.BRITISH, "Modern British")
    )

    // All English voices
    val ENGLISH_VOICES = AMERICAN_FEMALE + AMERICAN_MALE + BRITISH_FEMALE + BRITISH_MALE
    val ALL_VOICES = ENGLISH_VOICES

    // Quick access
    val DEFAULT_VOICE = AMERICAN_FEMALE[1]  // af_bella (ID 1)
    val DEFAULT_MALE = AMERICAN_MALE[0]     // am_adam (ID 5)

    fun getVoice(id: Int): Voice? = ALL_VOICES.find { it.id == id }

    fun getVoiceName(id: Int): String = getVoice(id)?.displayName ?: "Voice $id"

    fun getVoicesByGender(gender: Gender): List<Voice> = ALL_VOICES.filter { it.gender == gender }

    fun getVoicesByAccent(accent: Accent): List<Voice> = ALL_VOICES.filter { it.accent == accent }

    fun getEnglishVoices(): List<Voice> = ENGLISH_VOICES

    // Preview text for voice samples
    const val PREVIEW_TEXT = "Hello! I'm your audiobook narrator. Let me read this story for you."
    const val PREVIEW_TEXT_SHORT = "Hello, I'm your narrator."

    // Total number of voices
    const val TOTAL_VOICES = 11
}
