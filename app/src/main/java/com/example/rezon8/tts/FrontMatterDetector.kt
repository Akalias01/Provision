package com.mossglen.lithos.tts

/**
 * Utility to detect and skip front matter in books.
 *
 * Front matter includes: copyright, dedication, acknowledgments,
 * table of contents, about the author, title pages, etc.
 *
 * The goal is to start TTS from the actual story content:
 * Prologue, Chapter 1, Part 1, or similar.
 */
object FrontMatterDetector {

    // Patterns that indicate the START of actual content
    private val contentStartPatterns = listOf(
        // Prologue variations
        Regex("^prologue", RegexOption.IGNORE_CASE),
        Regex("^prolog\\b", RegexOption.IGNORE_CASE),

        // Chapter patterns
        Regex("^chapter\\s*(one|1|i\\b)", RegexOption.IGNORE_CASE),
        Regex("^chapter\\s*\\d+", RegexOption.IGNORE_CASE),

        // Part patterns
        Regex("^part\\s*(one|1|i\\b)", RegexOption.IGNORE_CASE),
        Regex("^part\\s*\\d+", RegexOption.IGNORE_CASE),

        // Book patterns (for multi-book compilations)
        Regex("^book\\s*(one|1|i\\b)", RegexOption.IGNORE_CASE),

        // Section patterns
        Regex("^section\\s*(one|1|i\\b)", RegexOption.IGNORE_CASE),

        // Act patterns (for plays/dramas)
        Regex("^act\\s*(one|1|i\\b)", RegexOption.IGNORE_CASE),

        // Introduction (sometimes is actual content)
        Regex("^introduction\\b", RegexOption.IGNORE_CASE),
        Regex("^preface\\b", RegexOption.IGNORE_CASE),
        Regex("^foreword\\b", RegexOption.IGNORE_CASE)
    )

    // Patterns that indicate front matter (should be skipped)
    private val frontMatterPatterns = listOf(
        Regex("^copyright", RegexOption.IGNORE_CASE),
        Regex("^all rights reserved", RegexOption.IGNORE_CASE),
        Regex("^published by", RegexOption.IGNORE_CASE),
        Regex("^dedication\\b", RegexOption.IGNORE_CASE),
        Regex("^for\\s+my", RegexOption.IGNORE_CASE),  // Common dedication start
        Regex("^to\\s+my\\s+(wife|husband|family|mother|father)", RegexOption.IGNORE_CASE),
        Regex("^acknowledgments?", RegexOption.IGNORE_CASE),
        Regex("^about the author", RegexOption.IGNORE_CASE),
        Regex("^table of contents", RegexOption.IGNORE_CASE),
        Regex("^contents\\s*$", RegexOption.IGNORE_CASE),
        Regex("^isbn", RegexOption.IGNORE_CASE),
        Regex("^first (edition|published|printing)", RegexOption.IGNORE_CASE),
        Regex("^printed in", RegexOption.IGNORE_CASE),
        Regex("^cover (design|art|illustration)", RegexOption.IGNORE_CASE),
        Regex("^editor:", RegexOption.IGNORE_CASE),
        Regex("^also by", RegexOption.IGNORE_CASE),
        Regex("^other (books|works) by", RegexOption.IGNORE_CASE),
        Regex("^this (book|novel|work) is", RegexOption.IGNORE_CASE),
        Regex("^\\d{4}\\s+by\\s+", RegexOption.IGNORE_CASE),  // Year by Author (copyright)
        Regex("^epigraph", RegexOption.IGNORE_CASE)
    )

    /**
     * Find the index of the first sentence that's likely actual content.
     * Returns 0 if no front matter detected.
     */
    fun findContentStartIndex(sentences: List<String>): Int {
        // Check first 100 sentences max (front matter rarely goes beyond that)
        val searchLimit = minOf(sentences.size, 100)

        for (i in 0 until searchLimit) {
            val sentence = sentences[i].trim()

            // Check if this looks like the start of actual content
            for (pattern in contentStartPatterns) {
                if (pattern.containsMatchIn(sentence)) {
                    return i
                }
            }
        }

        // If no explicit chapter marker found, try heuristics:
        // Look for where front matter patterns stop appearing
        var lastFrontMatterIndex = -1
        for (i in 0 until searchLimit) {
            val sentence = sentences[i].trim()
            for (pattern in frontMatterPatterns) {
                if (pattern.containsMatchIn(sentence)) {
                    lastFrontMatterIndex = i
                    break
                }
            }
        }

        // Start after the last front matter, with a small buffer
        if (lastFrontMatterIndex >= 0) {
            return minOf(lastFrontMatterIndex + 3, sentences.size - 1)
        }

        // No front matter detected, start from beginning
        return 0
    }

    /**
     * Filter sentences to skip front matter.
     * Returns sentences starting from actual content.
     */
    fun skipFrontMatter(sentences: List<String>): List<String> {
        val startIndex = findContentStartIndex(sentences)
        return if (startIndex > 0) {
            sentences.subList(startIndex, sentences.size)
        } else {
            sentences
        }
    }

    /**
     * Check if a chapter title indicates front matter.
     */
    fun isFrontMatterChapter(chapterTitle: String): Boolean {
        val title = chapterTitle.trim().lowercase()
        val frontMatterTitles = listOf(
            "copyright", "legal", "rights",
            "dedication", "dedicated to",
            "acknowledgments", "acknowledgements",
            "about the author", "about the book",
            "table of contents", "contents",
            "title page", "half title",
            "epigraph", "frontispiece",
            "also by", "other books",
            "cover"
        )
        return frontMatterTitles.any { title.contains(it) }
    }

    /**
     * Check if a chapter title indicates actual content.
     */
    fun isContentChapter(chapterTitle: String): Boolean {
        val title = chapterTitle.trim()
        val contentPatterns = listOf(
            Regex("^prologue", RegexOption.IGNORE_CASE),
            Regex("^chapter", RegexOption.IGNORE_CASE),
            Regex("^part\\s", RegexOption.IGNORE_CASE),
            Regex("^book\\s", RegexOption.IGNORE_CASE),
            Regex("^section", RegexOption.IGNORE_CASE),
            Regex("^act\\s", RegexOption.IGNORE_CASE),
            Regex("^introduction", RegexOption.IGNORE_CASE),
            Regex("^preface", RegexOption.IGNORE_CASE),
            Regex("^\\d+\\.", RegexOption.IGNORE_CASE),  // "1. Something"
            Regex("^[IVX]+\\.", RegexOption.IGNORE_CASE)  // "I. Something" (Roman numerals)
        )
        return contentPatterns.any { it.containsMatchIn(title) }
    }
}
