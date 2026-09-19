package com.vinote.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit

/**
 * Tiny markdown renderer for chat bubbles.
 *
 * NoTa's replies (both the LLM and the offline fallback) use **bold**, *italic*
 * and `code` markers. Compose's Text does not parse these, so without this the
 * user sees literal asterisks. Only the subset NoTa actually emits is handled;
 * anything else is left as plain text rather than crashing on bad input.
 *
 * A copy of the source text with markers stripped is exposed so keyword checks
 * (e.g. "Uang Aman" card detection) keep matching on the plain form.
 */
object MarkdownText {

    private val MARKER_REGEX = Regex("""\*\*(.+?)\*\*|\*(.+?)\*|`(.+?)`|__(.+?)__|~~(.+?)~~""")

    /** The text with all markdown markers removed. */
    fun plain(text: String): String =
        MARKER_REGEX.replace(text) { match ->
            match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: ""
        }

    /** Parses **bold**, *italic*, `code`, __underline__ and ~~strikethrough~~. */
    fun parse(text: String): AnnotatedString = parse(text, baseFontSize = TextUnit.Unspecified)

    fun parse(text: String, baseFontSize: TextUnit): AnnotatedString = buildAnnotatedString {
        if (baseFontSize != TextUnit.Unspecified) {
            pushStyle(SpanStyle(fontSize = baseFontSize))
        }
        var cursor = 0
        MARKER_REGEX.findAll(text).forEach { match ->
            if (match.range.first > cursor) append(text.substring(cursor, match.range.first))

            val (content, style) = when {
                match.groupValues[1].isNotEmpty() -> match.groupValues[1] to SpanStyle(fontWeight = FontWeight.Bold)
                match.groupValues[2].isNotEmpty() -> match.groupValues[2] to SpanStyle(fontStyle = FontStyle.Italic)
                match.groupValues[3].isNotEmpty() -> match.groupValues[3] to SpanStyle(fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline)
                match.groupValues[4].isNotEmpty() -> match.groupValues[4] to SpanStyle(textDecoration = TextDecoration.Underline)
                else -> match.groupValues[5] to SpanStyle(textDecoration = TextDecoration.LineThrough)
            }
            pushStyle(style)
            append(content)
            pop()

            cursor = match.range.last + 1
        }
        if (cursor < text.length) append(text.substring(cursor))
        if (baseFontSize != TextUnit.Unspecified) pop()
    }
}
