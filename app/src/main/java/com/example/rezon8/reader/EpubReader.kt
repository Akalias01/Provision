package com.mossglen.reverie.reader

import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * EPUB Reader - Parses and extracts content from EPUB files.
 *
 * EPUB structure:
 * - META-INF/container.xml (points to content.opf)
 * - content.opf (package file with manifest and spine)
 * - XHTML/HTML chapter files
 * - Images and CSS
 */
class EpubReader(private val epubFile: File) {

    companion object {
        private const val TAG = "EpubReader"
        private const val CONTAINER_PATH = "META-INF/container.xml"
    }

    private var zipFile: ZipFile? = null
    private var opfPath: String = ""
    private var opfBasePath: String = ""

    // Book metadata
    var title: String = ""
        private set
    var author: String = ""
        private set
    var coverPath: String? = null
        private set

    // Chapter list in reading order
    private val chapters = mutableListOf<Chapter>()

    data class Chapter(
        val id: String,
        val href: String,
        val title: String,
        val content: String
    )

    /**
     * Parse the EPUB file and extract metadata and chapters.
     * @return true if parsing was successful
     */
    fun parse(): Boolean {
        return try {
            zipFile = ZipFile(epubFile)

            // Step 1: Find the OPF file path from container.xml
            if (!parseContainer()) {
                Log.e(TAG, "Failed to parse container.xml")
                return false
            }

            // Step 2: Parse the OPF file for metadata and spine
            if (!parseOpf()) {
                Log.e(TAG, "Failed to parse OPF file")
                return false
            }

            Log.d(TAG, "Parsed EPUB: $title by $author, ${chapters.size} chapters")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse EPUB", e)
            false
        }
    }

    /**
     * Parse container.xml to find the OPF file path.
     */
    private fun parseContainer(): Boolean {
        val containerEntry = zipFile?.getEntry(CONTAINER_PATH) ?: return false
        val containerXml = zipFile?.getInputStream(containerEntry)?.bufferedReader()?.readText() ?: return false

        val doc = Jsoup.parse(containerXml)
        val rootfile = doc.selectFirst("rootfile[full-path]")
        opfPath = rootfile?.attr("full-path") ?: return false

        // Get base path for resolving relative paths
        opfBasePath = opfPath.substringBeforeLast("/", "")
        if (opfBasePath.isNotEmpty()) opfBasePath += "/"

        Log.d(TAG, "OPF path: $opfPath, base: $opfBasePath")
        return true
    }

    /**
     * Parse the OPF file for metadata, manifest, and spine.
     */
    private fun parseOpf(): Boolean {
        val opfEntry = zipFile?.getEntry(opfPath) ?: return false
        val opfXml = zipFile?.getInputStream(opfEntry)?.bufferedReader()?.readText() ?: return false

        val doc = Jsoup.parse(opfXml)

        // Extract metadata
        title = doc.selectFirst("metadata title, dc\\:title")?.text() ?: epubFile.nameWithoutExtension
        author = doc.selectFirst("metadata creator, dc\\:creator")?.text() ?: "Unknown Author"

        // Build manifest map (id -> href)
        val manifest = mutableMapOf<String, String>()
        doc.select("manifest item").forEach { item ->
            val id = item.attr("id")
            val href = item.attr("href")
            val mediaType = item.attr("media-type")

            manifest[id] = href

            // Check for cover image
            if (mediaType.startsWith("image/") &&
                (id.contains("cover", ignoreCase = true) || href.contains("cover", ignoreCase = true))) {
                coverPath = opfBasePath + href
            }
        }

        // Also check for cover in metadata
        val coverMeta = doc.selectFirst("meta[name=cover]")
        if (coverMeta != null) {
            val coverId = coverMeta.attr("content")
            manifest[coverId]?.let { coverPath = opfBasePath + it }
        }

        // Parse spine (reading order)
        val spine = doc.select("spine itemref")
        var chapterIndex = 1

        spine.forEach { itemref ->
            val idref = itemref.attr("idref")
            val href = manifest[idref] ?: return@forEach
            val fullPath = opfBasePath + href

            // Load and parse the chapter content
            val chapterContent = loadChapterContent(fullPath)
            if (chapterContent != null) {
                val chapterTitle = extractChapterTitle(chapterContent, chapterIndex)
                chapters.add(Chapter(
                    id = idref,
                    href = fullPath,
                    title = chapterTitle,
                    content = chapterContent
                ))
                chapterIndex++
            }
        }

        return chapters.isNotEmpty()
    }

    /**
     * Load and clean chapter HTML content.
     */
    private fun loadChapterContent(path: String): String? {
        return try {
            val entry = zipFile?.getEntry(path) ?: return null
            val html = zipFile?.getInputStream(entry)?.bufferedReader()?.readText() ?: return null

            // Parse HTML and extract body content
            val doc = Jsoup.parse(html)

            // Remove scripts and styles
            doc.select("script, style, link").remove()

            // Get body content
            val body = doc.body()

            // Clean and return HTML
            body.html()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load chapter: $path", e)
            null
        }
    }

    /**
     * Extract chapter title from content or generate default.
     */
    private fun extractChapterTitle(html: String, index: Int): String {
        val doc = Jsoup.parse(html)

        // Try to find a heading
        val heading = doc.selectFirst("h1, h2, h3, title")
        val text = heading?.text()?.trim()

        return if (!text.isNullOrBlank() && text.length < 100) {
            text
        } else {
            "Chapter $index"
        }
    }

    /**
     * Get the number of chapters.
     */
    fun getChapterCount(): Int = chapters.size

    /**
     * Get chapter by index.
     */
    fun getChapter(index: Int): Chapter? = chapters.getOrNull(index)

    /**
     * Get all chapter titles for navigation.
     */
    fun getChapterTitles(): List<String> = chapters.map { it.title }

    /**
     * Get plain text content for a chapter (for TTS).
     */
    fun getChapterText(index: Int): String? {
        val chapter = chapters.getOrNull(index) ?: return null
        val doc = Jsoup.parse(chapter.content)
        return doc.text()
    }

    /**
     * Get cover image as InputStream.
     */
    fun getCoverImage(): InputStream? {
        val path = coverPath ?: return null
        val entry = zipFile?.getEntry(path) ?: return null
        return zipFile?.getInputStream(entry)
    }

    /**
     * Close the EPUB file.
     */
    fun close() {
        try {
            zipFile?.close()
            zipFile = null
        } catch (e: Exception) {
            Log.w(TAG, "Error closing EPUB", e)
        }
    }
}
