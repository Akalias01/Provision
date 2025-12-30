package com.mossglen.reverie.tts

/**
 * Kokoro TTS Voice definitions.
 *
 * Kokoro multi-speaker model has 53+ voices.
 * Voice IDs are 0-indexed integers passed to the generate() method.
 *
 * Voice naming based on official Kokoro documentation.
 */
object KokoroVoices {

    data class Voice(
        val id: Int,
        val name: String,
        val gender: Gender,
        val accent: Accent,
        val description: String = ""
    )

    enum class Gender { FEMALE, MALE }
    enum class Accent { AMERICAN, BRITISH }

    // Primary voices (most natural sounding)
    val PRIMARY_VOICES = listOf(
        Voice(0, "Bella", Gender.FEMALE, Accent.AMERICAN, "Warm, natural American female"),
        Voice(1, "Sarah", Gender.FEMALE, Accent.AMERICAN, "Clear, professional American female"),
        Voice(2, "Emily", Gender.FEMALE, Accent.BRITISH, "Elegant British female"),
        Voice(3, "Michael", Gender.MALE, Accent.AMERICAN, "Deep, confident American male"),
        Voice(4, "James", Gender.MALE, Accent.BRITISH, "Refined British male"),
        Voice(5, "Nicole", Gender.FEMALE, Accent.AMERICAN, "Friendly, upbeat American female"),
        Voice(6, "Adam", Gender.MALE, Accent.AMERICAN, "Casual American male"),
        Voice(7, "Jessica", Gender.FEMALE, Accent.AMERICAN, "Expressive American female"),
        Voice(8, "David", Gender.MALE, Accent.BRITISH, "Warm British male"),
        Voice(9, "Emma", Gender.FEMALE, Accent.BRITISH, "Soft-spoken British female")
    )

    // Extended voices
    val EXTENDED_VOICES = listOf(
        Voice(10, "Alex", Gender.MALE, Accent.AMERICAN),
        Voice(11, "Sophia", Gender.FEMALE, Accent.AMERICAN),
        Voice(12, "Daniel", Gender.MALE, Accent.BRITISH),
        Voice(13, "Olivia", Gender.FEMALE, Accent.BRITISH),
        Voice(14, "William", Gender.MALE, Accent.AMERICAN),
        Voice(15, "Ava", Gender.FEMALE, Accent.AMERICAN),
        Voice(16, "Henry", Gender.MALE, Accent.BRITISH),
        Voice(17, "Charlotte", Gender.FEMALE, Accent.BRITISH),
        Voice(18, "Benjamin", Gender.MALE, Accent.AMERICAN),
        Voice(19, "Amelia", Gender.FEMALE, Accent.AMERICAN)
    )

    // All voices
    val ALL_VOICES = PRIMARY_VOICES + EXTENDED_VOICES

    // Quick access
    val DEFAULT_VOICE = PRIMARY_VOICES[0]  // Bella

    fun getVoice(id: Int): Voice? = ALL_VOICES.find { it.id == id }

    fun getVoiceName(id: Int): String = getVoice(id)?.name ?: "Voice $id"

    fun getVoicesByGender(gender: Gender): List<Voice> = ALL_VOICES.filter { it.gender == gender }

    fun getVoicesByAccent(accent: Accent): List<Voice> = ALL_VOICES.filter { it.accent == accent }

    // Preview text for voice samples
    const val PREVIEW_TEXT = "Hello! I'm your audiobook narrator. Let me read this story for you."
    const val PREVIEW_TEXT_SHORT = "Hello, I'm your narrator."
}
