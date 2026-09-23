package com.booru.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val lines = markdown.lines()
    val linkColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var inCodeBlock = false
        val codeBlockLines = mutableListOf<String>()

        for (rawLine in lines) {
            val line = rawLine.trimEnd()
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = codeBlockLines.joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    codeBlockLines.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockLines.add(line)
                continue
            }

            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> {
                    Spacer(Modifier.height(2.dp))
                }
                trimmed.matches("^[-*_]{3,}$".toRegex()) -> {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )
                }
                trimmed.startsWith("#") -> {
                    val headerMatch = Regex("^(#{1,6})\\s*(.*?)(?:\\s*#+)?$").matchEntire(trimmed)
                    if (headerMatch != null && (headerMatch.groupValues[1].length > 1 || trimmed.startsWith("# "))) {
                        val level = headerMatch.groupValues[1].length
                        val headerText = headerMatch.groupValues[2].trim()
                        when (level) {
                            1 -> {
                                MarkdownInlineText(
                                    annotatedString = parseInlineMarkdown(headerText, linkColor),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            2 -> {
                                MarkdownInlineText(
                                    annotatedString = parseInlineMarkdown(headerText, linkColor),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                )
                            }
                            else -> {
                                MarkdownInlineText(
                                    annotatedString = parseInlineMarkdown(headerText, linkColor),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                )
                            }
                        }
                    } else {
                        MarkdownInlineText(
                            annotatedString = parseInlineMarkdown(trimmed, linkColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val indentSpaces = rawLine.takeWhile { it == ' ' }.length
                    val indentLevel = (indentSpaces / 2).coerceIn(0, 4)
                    val content = trimmed.substring(2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (indentLevel * 12).dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (indentLevel > 0) "◦" else "•",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        MarkdownInlineText(
                            annotatedString = parseInlineMarkdown(content, linkColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                else -> {
                    MarkdownInlineText(
                        annotatedString = parseInlineMarkdown(trimmed, linkColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(8.dp)
            ) {
                Text(
                    text = codeBlockLines.joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            codeBlockLines.clear()
        }
    }
}

@Composable
private fun MarkdownInlineText(
    annotatedString: AnnotatedString,
    style: TextStyle,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val hasLinks = annotatedString.getStringAnnotations("URL", 0, annotatedString.length).isNotEmpty()

    if (hasLinks) {
        ClickableText(
            text = annotatedString,
            style = style.copy(color = color),
            modifier = modifier,
            onClick = { offset ->
                annotatedString.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { annotation ->
                    val url = annotation.item.trim()
                    if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                        runCatching { uriHandler.openUri(url) }
                    }
                }
            }
        )
    } else {
        Text(
            text = annotatedString,
            style = style,
            color = color,
            modifier = modifier
        )
    }
}

private fun parseInlineMarkdown(text: String, linkColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    val boldText = text.substring(i + 2, end)
                    val startIdx = length
                    append(boldText)
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), startIdx, length)
                    i = end + 2
                    continue
                }
            }
            if (text.startsWith("`", i)) {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    val codeText = text.substring(i + 1, end)
                    val startIdx = length
                    append(codeText)
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        ),
                        startIdx,
                        length
                    )
                    i = end + 1
                    continue
                }
            }
            if (text.startsWith("*", i) && !text.startsWith("**", i)) {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    val italicText = text.substring(i + 1, end)
                    val startIdx = length
                    append(italicText)
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), startIdx, length)
                    i = end + 1
                    continue
                }
            }
            if (text.startsWith("[", i)) {
                val linkTextEnd = text.indexOf("]", i + 1)
                val urlStart = if (linkTextEnd != -1) text.indexOf("(", linkTextEnd) else -1
                val urlEnd = if (urlStart == linkTextEnd + 1) text.indexOf(")", urlStart) else -1
                if (linkTextEnd != -1 && urlEnd != -1) {
                    val linkTitle = text.substring(i + 1, linkTextEnd)
                    val rawUrl = text.substring(urlStart + 1, urlEnd).trim()
                    val startIdx = length
                    append(linkTitle)
                    if (rawUrl.startsWith("http://", ignoreCase = true) || rawUrl.startsWith("https://", ignoreCase = true)) {
                        addStringAnnotation("URL", rawUrl, startIdx, length)
                    }
                    addStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        ),
                        startIdx,
                        length
                    )
                    i = urlEnd + 1
                    continue
                }
            }
            append(text[i])
            i++
        }
    }
}
