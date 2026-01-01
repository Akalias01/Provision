package com.mossglen.lithos.util

import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * Robust sentence extraction from HTML content.
 *
 * Properly handles:
 * - Paragraph boundaries
 * - Common abbreviations (Dr., Mr., Mrs., etc.)
 * - Numbers with decimals (3.14, $19.99)
 * - Ellipsis (...)
 * - Dialog and quotes
 * - Multiple sentence-ending punctuation (!? ?! ...)
 */
object SentenceExtractor {

    private const val TAG = "SentenceExtractor"

    // Common abbreviations that should not trigger sentence breaks
    private val ABBREVIATIONS = setOf(
        "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "vs",
        "inc", "ltd", "corp", "co", "etc", "eg", "ie",
        "st", "ave", "blvd", "rd", "apt", "no",
        "jan", "feb", "mar", "apr", "jun", "jul", "aug", "sep", "oct", "nov", "dec",
        "vol", "ch", "pt", "pg", "pp", "fig", "eq"
    )

    /**
     * Extract sentences from HTML content.
     * Preserves paragraph structure and handles common edge cases.
     *
     * @param html The HTML content to parse
     * @return List of sentences
     */
    fun extractSentences(html: String): List<String> {
        if (html.isBlank()) return emptyList()

        val doc = Jsoup.parse(html)
        val sentences = mutableListOf<String>()

        // Process block-level elements that contain text
        val blockElements = doc.select("p, div, h1, h2, h3, h4, h5, h6, li, blockquote, td, th")

        if (blockElements.isEmpty()) {
            // No block elements - treat entire body as one block
            val text = doc.body().text().trim()
            if (text.isNotEmpty()) {
                sentences.addAll(splitIntoSentences(text))
            }
        } else {
            for (element in blockElements) {
                // Get text content of this element (not nested block elements)
                val text = getDirectText(element).trim()
                if (text.isNotEmpty()) {
                    sentences.addAll(splitIntoSentences(text))
                }
            }
        }

        // Filter out very short fragments
        val filtered = sentences
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.length > 3 }

        Log.d(TAG, "Extracted ${filtered.size} sentences from HTML (${html.length} chars)")
        return filtered
    }

    /**
     * Get direct text content of an element, excluding nested block elements.
     */
    private fun getDirectText(element: Element): String {
        val sb = StringBuilder()
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> sb.append(node.text())
                is Element -> {
                    val tagName = node.tagName().lowercase()
                    if (tagName in listOf("a", "b", "i", "em", "strong", "span", "u", "small", "sup", "sub", "mark")) {
                        sb.append(node.text())
                    }
                }
            }
        }
        return sb.toString()
    }

    /**
     * Split text into sentences using a robust algorithm.
     */
    fun splitIntoSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val sentences = mutableListOf<String>()
        val current = StringBuilder()

        var i = 0
        while (i < text.length) {
            val char = text[i]
            current.append(char)

            // Check for sentence-ending punctuation
            if (char == '.' || char == '!' || char == '?') {
                // Look ahead for more punctuation (e.g., "..." or "?!")
                while (i + 1 < text.length && (text[i + 1] == '.' || text[i + 1] == '!' || text[i + 1] == '?')) {
                    i++
                    current.append(text[i])
                }

                // Check if this is a real sentence boundary
                if (isSentenceBoundary(text, i, current.toString())) {
                    val sentence = current.toString().trim()
                    if (sentence.isNotEmpty()) {
                        sentences.add(sentence)
                    }
                    current.clear()

                    // Skip whitespace after sentence
                    while (i + 1 < text.length && text[i + 1].isWhitespace()) {
                        i++
                    }
                }
            }

            i++
        }

        // Add remaining text as final sentence
        val remaining = current.toString().trim()
        if (remaining.isNotEmpty()) {
            sentences.add(remaining)
        }

        return sentences
    }

    /**
     * Determine if the punctuation at position i is a real sentence boundary.
     */
    private fun isSentenceBoundary(text: String, pos: Int, currentSentence: String): Boolean {
        val punct = text.getOrNull(pos) ?: return false
        if (punct != '.' && punct != '!' && punct != '?') return false

        // ! and ? are almost always sentence boundaries
        if (punct != '.') return true

        // Check what follows the period
        val nextCharPos = pos + 1
        if (nextCharPos >= text.length) {
            // End of text - definitely a boundary
            return true
        }

        val nextChar = text[nextCharPos]

        // If followed by quote then space/end, it's a boundary
        if (nextChar == '"' || nextChar == '\'') {
            val afterQuote = nextCharPos + 1
            if (afterQuote >= text.length) return true
            if (text[afterQuote].isWhitespace()) return true
        }

        // If followed by lowercase letter, probably not a boundary (abbreviation)
        if (nextChar.isLowerCase()) return false

        // If followed by digit, probably not a boundary (decimal number)
        if (nextChar.isDigit()) return false

        // Check for common abbreviations
        val wordBeforePeriod = getWordBeforePosition(currentSentence, currentSentence.length - 1)
        if (wordBeforePeriod.lowercase() in ABBREVIATIONS) {
            return false
        }

        // Single letter followed by period (initials like "J." or list items like "a.")
        if (wordBeforePeriod.length == 1 && wordBeforePeriod.firstOrNull()?.isLetter() == true) {
            if (nextChar.isWhitespace()) {
                val nextNonSpace = text.substring(nextCharPos).trimStart().firstOrNull()
                if (nextNonSpace?.isUpperCase() == true) {
                    return true
                }
                return false
            }
        }

        // If followed by whitespace then uppercase, it's likely a boundary
        if (nextChar.isWhitespace()) {
            val nextNonSpace = text.substring(nextCharPos).trimStart().firstOrNull()
            if (nextNonSpace?.isUpperCase() == true) {
                return true
            }
            return true
        }

        // Default: if we have a period, treat it as a boundary
        return true
    }

    /**
     * Get the word immediately before the given position.
     */
    private fun getWordBeforePosition(text: String, pos: Int): String {
        if (pos <= 0 || pos > text.length) return ""

        var endPos = pos - 1
        // Skip any trailing punctuation
        while (endPos >= 0 && !text[endPos].isLetterOrDigit()) {
            endPos--
        }

        if (endPos < 0) return ""

        var startPos = endPos
        while (startPos > 0 && text[startPos - 1].isLetterOrDigit()) {
            startPos--
        }

        return text.substring(startPos, endPos + 1)
    }
}
