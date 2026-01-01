package com.mossglen.lithos.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Handles quote sharing functionality for LITHOS.
 * Creates beautifully formatted text and image quotes with book attribution.
 */
object QuoteShareManager {

    private const val LITHOS_BRANDING = "Shared via Lithos"
    private const val QUOTE_OPEN = "\u201C"  // Opening curly quote
    private const val QUOTE_CLOSE = "\u201D" // Closing curly quote

    /**
     * Creates a formatted text quote with attribution.
     *
     * @param quote The selected text to share
     * @param bookTitle The title of the book
     * @param author The author of the book
     * @return Formatted string ready for sharing
     */
    fun formatQuoteText(
        quote: String,
        bookTitle: String,
        author: String
    ): String {
        val cleanQuote = quote.trim()
        val authorText = if (author.isNotBlank() && author != "Unknown Author") {
            " by $author"
        } else ""

        return buildString {
            append("$QUOTE_OPEN$cleanQuote$QUOTE_CLOSE")
            appendLine()
            appendLine()
            append("— $bookTitle$authorText")
            appendLine()
            appendLine()
            append(LITHOS_BRANDING)
        }
    }

    /**
     * Shares a quote as formatted text using the Android share sheet.
     *
     * @param context Android context
     * @param quote The selected text to share
     * @param bookTitle The title of the book
     * @param author The author of the book
     */
    fun shareQuoteAsText(
        context: Context,
        quote: String,
        bookTitle: String,
        author: String
    ) {
        val formattedQuote = formatQuoteText(quote, bookTitle, author)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, formattedQuote)
            putExtra(Intent.EXTRA_SUBJECT, "Quote from $bookTitle")
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Quote"))
    }

    /**
     * Creates a beautiful quote card image.
     *
     * @param quote The selected text
     * @param bookTitle The book title
     * @param author The author
     * @param isDark Whether to use dark theme
     * @return Bitmap of the quote card
     */
    fun createQuoteCardBitmap(
        quote: String,
        bookTitle: String,
        author: String,
        isDark: Boolean = true
    ): Bitmap {
        val width = 1080  // Instagram/social media friendly width
        val height = 1350 // 4:5 aspect ratio (good for Instagram)
        val padding = 80f
        val cornerRadius = 48f

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Colors based on theme
        val backgroundColor = if (isDark) 0xFF1C1C1E.toInt() else 0xFFF2F2F7.toInt()
        val gradientStart = if (isDark) 0xFF2C2C2E.toInt() else 0xFFE5E5EA.toInt()
        val gradientEnd = if (isDark) 0xFF1A1A1A.toInt() else 0xFFFFFFFF.toInt()
        val accentColor = 0xFFE5941F.toInt() // Lithos amber accent
        val textColor = if (isDark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val secondaryTextColor = if (isDark) 0xAAFFFFFF.toInt() else 0xAA000000.toInt()
        val tertiaryTextColor = if (isDark) 0x77FFFFFF.toInt() else 0x77000000.toInt()

        // Background gradient paint
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                gradientStart, gradientEnd,
                Shader.TileMode.CLAMP
            )
        }

        // Draw rounded rectangle background
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // Draw accent line at top
        val accentPaint = Paint().apply {
            color = accentColor
            isAntiAlias = true
        }
        val accentRect = RectF(padding, padding / 2, width - padding, padding / 2 + 6f)
        canvas.drawRoundRect(accentRect, 3f, 3f, accentPaint)

        // Quote mark paint
        val quoteMarkPaint = Paint().apply {
            color = accentColor
            textSize = 120f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        // Draw opening quote mark
        canvas.drawText(QUOTE_OPEN, padding, padding + 120f, quoteMarkPaint)

        // Quote text paint
        val quotePaint = Paint().apply {
            color = textColor
            textSize = 48f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        // Wrap and draw quote text
        val quoteY = drawWrappedText(
            canvas = canvas,
            text = quote.trim(),
            paint = quotePaint,
            x = padding,
            y = padding + 180f,
            maxWidth = width - (padding * 2),
            lineHeight = 72f
        )

        // Draw closing quote mark
        val closeQuotePaint = Paint(quoteMarkPaint).apply {
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(QUOTE_CLOSE, width - padding, quoteY + 40f, closeQuotePaint)

        // Attribution section
        val attributionY = quoteY + 120f

        // Em dash
        val dashPaint = Paint().apply {
            color = accentColor
            textSize = 36f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("—", padding, attributionY, dashPaint)

        // Book title
        val titlePaint = Paint().apply {
            color = textColor
            textSize = 36f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            isAntiAlias = true
        }
        canvas.drawText(bookTitle, padding + 40f, attributionY, titlePaint)

        // Author
        if (author.isNotBlank() && author != "Unknown Author") {
            val authorPaint = Paint().apply {
                color = secondaryTextColor
                textSize = 32f
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText("by $author", padding + 40f, attributionY + 48f, authorPaint)
        }

        // LITHOS branding at bottom
        val brandingPaint = Paint().apply {
            color = tertiaryTextColor
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
            letterSpacing = 0.1f
        }
        val brandingY = height - padding
        canvas.drawText("LITHOS", padding, brandingY, brandingPaint)

        // Small accent dot
        val dotPaint = Paint().apply {
            color = accentColor
            isAntiAlias = true
        }
        canvas.drawCircle(width - padding, brandingY - 14f, 8f, dotPaint)

        return bitmap
    }

    /**
     * Draws text with word wrapping.
     *
     * @return Y position after the last line
     */
    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        paint: Paint,
        x: Float,
        y: Float,
        maxWidth: Float,
        lineHeight: Float
    ): Float {
        val words = text.split(" ")
        var line = StringBuilder()
        var currentY = y
        var lineCount = 0
        val maxLines = 12 // Limit to prevent overflow

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "${line} $word"
            val testWidth = paint.measureText(testLine)

            if (testWidth > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line.toString(), x, currentY, paint)
                line = StringBuilder(word)
                currentY += lineHeight
                lineCount++

                if (lineCount >= maxLines) {
                    // Add ellipsis if we've hit the limit
                    line.append("...")
                    canvas.drawText(line.toString(), x, currentY, paint)
                    break
                }
            } else {
                line = StringBuilder(testLine)
            }
        }

        // Draw remaining text
        if (line.isNotEmpty() && lineCount < maxLines) {
            canvas.drawText(line.toString(), x, currentY, paint)
        }

        return currentY
    }

    /**
     * Shares a quote as an image using the Android share sheet.
     *
     * @param context Android context
     * @param quote The selected text
     * @param bookTitle The book title
     * @param author The author
     * @param isDark Whether to use dark theme for the card
     */
    fun shareQuoteAsImage(
        context: Context,
        quote: String,
        bookTitle: String,
        author: String,
        isDark: Boolean = true
    ) {
        try {
            // Create the quote card bitmap
            val bitmap = createQuoteCardBitmap(quote, bookTitle, author, isDark)

            // Save to cache directory
            val cachePath = File(context.cacheDir, "quote_cards")
            cachePath.mkdirs()
            val file = File(cachePath, "quote_${System.currentTimeMillis()}.png")

            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            // Get content URI using FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Create share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, formatQuoteText(quote, bookTitle, author))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Quote"))
        } catch (e: Exception) {
            // Fallback to text sharing if image creation fails
            shareQuoteAsText(context, quote, bookTitle, author)
        }
    }
}
