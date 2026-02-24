package com.agent.llm.web.fetch.extractor

import com.agent.llm.web.fetch.HtmlExtractor
import com.agent.llm.web.fetch.HtmlExtractResult
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

private val logger = KotlinLogging.logger {}

/**
 * HTML content extractor using only Jsoup for content cleaning and local HTML to Markdown conversion.
 * No external dependencies - all processing done locally.
 *
 * The main goal is to convert HTML web pages into clean, readable Markdown format
 * while preserving structure and removing noise (ads, navigation, etc.).
 * Text length limits are applied to prevent token overconsumption in AI models.
 */
class HtmlExtractorImpl : HtmlExtractor {

    init {
        logger.info { "HtmlExtractorImpl initialized" }
    }

    override suspend fun extract(html: String, baseUrl: HttpUrl): HtmlExtractResult {
        logger.debug { "HtmlExtractorImpl extracting from: $baseUrl (${html.length} chars)" }

        val doc = Jsoup.parse(html, baseUrl.toString())

        // Extract title using multiple fallback strategies
        val title = extractTitle(doc)
        logger.debug { "HtmlExtractorImpl extracted title: '${title ?: "none"}'" }

        // Remove unwanted elements (ads, scripts, etc.) to reduce noise
        cleanDocument(doc)

        // Find the main content area using heuristic-based content detection
        val mainContent = findMainContentElement(doc)
        logger.debug { "HtmlExtractorImpl found main content element: ${mainContent.tagName()} (${mainContent.text().length} chars)" }

        // Convert HTML structure to Markdown with proper link resolution
        val markdown = htmlToMarkdown(mainContent, baseUrl)

        // Apply final text length limits to prevent token overconsumption
        val truncatedMarkdown = truncateText(markdown, MAX_MARKDOWN_LENGTH)

        if (markdown.length != truncatedMarkdown.length) {
            logger.debug { "HtmlExtractorImpl truncated markdown from ${markdown.length} to ${truncatedMarkdown.length} chars" }
        }

        logger.info { "HtmlExtractorImpl completed extraction: title='${title ?: "none"}', ${truncatedMarkdown.length} markdown chars" }

        return HtmlExtractResult(
            title = title,
            markdown = truncatedMarkdown
        )
    }

    /**
     * Extracts page title using multiple fallback strategies:
     * 1. <title> tag - standard HTML title
     * 2. <h1> tag - main heading, often contains the actual article title
     * 3. Open Graph title - social media sharing title, usually well-crafted
     */
    private fun extractTitle(doc: Document): String? {
        return doc.selectFirst("title")?.text()?.trim()?.takeIf { it.isNotEmpty() }
            ?: doc.selectFirst("h1")?.text()?.trim()?.takeIf { it.isNotEmpty() }
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * Removes unwanted elements that don't contribute to main content:
     * - Scripts, styles: functional elements that shouldn't be in text
     * - Ads, popups: commercial noise that interferes with content understanding
     * - Cookie notices, modals: UI elements that aren't part of the actual content
     */
    private fun cleanDocument(doc: Document) {
        // Remove unwanted elements but keep structure
        UNWANTED_SELECTORS.forEach { selector ->
            doc.select(selector).remove()
        }
    }

    /**
     * Finds the main content area using common HTML5 semantic patterns and heuristics.
     * Many websites use standard patterns for content organization:
     * - HTML5 semantic tags: <main>, <article>
     * - ARIA roles: role="main"
     * - Common CSS class names: .content, #content, .main-content
     * - CMS-specific patterns: .post-content, .entry-content
     *
     * Returns the element with the most text content, or falls back to body with sidebar removal.
     */
    private fun findMainContentElement(doc: Document): Element {
        // Try to find main content area using common patterns
        val candidates = MAIN_CONTENT_SELECTORS.mapNotNull { selector ->
            doc.selectFirst(selector)
        }

        // Return the largest content area (by text length) or fallback to body
        // Text length is a good heuristic - main content usually contains the most text
        return candidates.maxByOrNull { it.text().length }
            ?: doc.body().also { body ->
                // Remove sidebar content from body as fallback
                // Sidebars typically contain navigation, ads, or secondary content
                body.select(FALLBACK_REMOVE_SELECTORS).remove()
            }
    }

    /**
     * Converts HTML element tree to Markdown format.
     * The conversion process maintains document structure while creating readable text.
     */
    private fun htmlToMarkdown(element: Element, baseUrl: HttpUrl): String {
        val text = StringBuilder()
        convertElementToText(element, text, baseUrl)

        // Normalize whitespace to improve readability while preserving list indentation:
        // - Replace 3+ consecutive newlines with exactly 2 (paragraph separation)
        //   This prevents excessive whitespace while maintaining logical breaks
        // - Replace multiple spaces/tabs within lines but preserve indentation at line start
        return text.toString()
            .replace(MULTIPLE_NEWLINES_REGEX, "\n\n")
            .let { normalizeSpacesPreservingIndentation(it) }
            .trim()
    }

    /**
     * Normalizes spaces while preserving indentation at the beginning of lines.
     * This is crucial for maintaining Markdown list structure and other indented content.
     */
    private fun normalizeSpacesPreservingIndentation(text: String): String {
        return text.lines().joinToString("\n") { line ->
            if (line.isBlank()) {
                "" // Empty lines remain empty
            } else {
                val leadingSpaces = line.takeWhile { it == ' ' }
                val content = line.dropWhile { it == ' ' }
                // Preserve leading spaces (indentation) but normalize multiple spaces within content
                leadingSpaces + content.replace(MULTIPLE_SPACES_REGEX, " ")
            }
        }
    }

    /**
     * Recursively converts HTML elements to text, handling different element types appropriately.
     * Each element type has specific formatting rules to preserve semantic meaning in text format.
     */
    private fun convertElementToText(element: Element, text: StringBuilder, baseUrl: HttpUrl) {
        // Early termination to prevent memory issues with extremely large pages
        if (text.length > MAX_INTERMEDIATE_TEXT_LENGTH) {
            return
        }

        // Handle empty element case
        if (element.children().isEmpty()) {
            val elementText = element.text().trim()
            if (elementText.isNotEmpty()) {
                text.append(elementText)
            }
            return
        }

        for (child in element.children()) {
            // Check length limit on each iteration to prevent excessive processing
            if (text.length > MAX_INTERMEDIATE_TEXT_LENGTH) {
                break
            }

            when (child.tagName().lowercase()) {
                // Headers: Convert to proper Markdown headers with # symbols
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    val headerText = child.text().trim()
                    if (headerText.isNotEmpty()) {
                        val level = child.tagName().substring(1).toInt()
                        val prefix = "#".repeat(level)
                        text.append("\n\n$prefix $headerText\n\n")
                    }
                }
                // Paragraphs: Natural text blocks with double newline separation
                "p" -> {
                    convertInlineContent(child, text, baseUrl)
                    text.append("\n\n")
                }
                // Divs: Handle as paragraphs if they contain only inline content
                "div" -> {
                    val inlineTags = setOf("a", "span", "strong", "em", "code", "b", "i")
                    if (child.children().isEmpty() || child.children().all { it.tagName().lowercase() in inlineTags }) {
                        val divText = child.text().trim()
                        if (divText.isNotEmpty()) {
                            convertInlineContent(child, text, baseUrl)
                            text.append("\n\n")
                        }
                    } else {
                        convertElementToText(child, text, baseUrl)
                    }
                }
                // Line breaks: Simple newline for <br> tags
                "br" -> text.appendLine()
                // Lists: Convert to Markdown list format with proper spacing
                "ul", "ol" -> {
                    text.appendLine()
                    convertListToText(child, text, baseUrl)
                    text.appendLine()
                }
                // List items: Handle direct li elements (edge case for malformed HTML)
                "li" -> {
                    text.append("* ")
                    convertInlineContent(child, text, baseUrl)
                    text.appendLine()
                }
                // Blockquotes: Use Markdown quote syntax with > prefix and support nested content
                "blockquote" -> {
                    text.appendLine()
                    convertBlockquoteContent(child, text, baseUrl)
                    text.appendLine()
                }
                // Code blocks: Preserve formatting with Markdown code fences
                "pre", "code" -> {
                    val codeText = child.text().trim()
                    if (codeText.isNotEmpty()) {
                        val truncatedCode = truncateText(codeText, MAX_CODE_BLOCK_LENGTH)
                        text.append("\n```\n$truncatedCode\n```\n\n")
                    }
                }
                // Tables: Convert to simple text format
                "table" -> {
                    convertTableToText(child, text)
                    text.appendLine()
                }
                // Links: Convert to Markdown link format [text](url)
                "a" -> {
                    val linkText = child.text().trim()
                    val href = child.attr("href").trim()
                    if (linkText.isNotEmpty() && href.isNotEmpty()) {
                        val absoluteUrl = resolveUrl(href, baseUrl)
                        text.append("[$linkText]($absoluteUrl)")
                    } else if (linkText.isNotEmpty()) {
                        text.append(linkText)
                    }
                }
                // Other elements: Process recursively or extract text directly
                else -> {
                    if (child.children().isEmpty()) {
                        val elementText = child.text().trim()
                        if (elementText.isNotEmpty()) {
                            text.append(elementText)
                        }
                    } else {
                        convertElementToText(child, text, baseUrl)
                    }
                }
            }
        }
    }

    /**
     * Processes inline content within elements, preserving text and converting links to Markdown format.
     * Handles mixed content like "This is <a href="...">a link</a> in text" and inline formatting.
     */
    private fun convertInlineContent(
        element: Element,
        text: StringBuilder,
        baseUrl: HttpUrl,
        excludeNestedLists: Boolean = false
    ) {
        for (node in element.childNodes()) {
            when {
                // Plain text nodes: append directly
                node.nodeName() == "#text" -> {
                    text.append((node as TextNode).text())
                }
                // Skip nested lists if requested (for list item processing)
                excludeNestedLists && node.nodeName() in listOf("ul", "ol") -> {
                    // Skip - these will be handled by the parent convertListToText method
                }
                // Links: Convert to Markdown link format [text](url)
                node.nodeName() == "a" && node is Element -> {
                    val linkText = node.text().trim()
                    val href = node.attr("href").trim()
                    if (linkText.isNotEmpty() && href.isNotEmpty()) {
                        val absoluteUrl = resolveUrl(href, baseUrl)
                        text.append("[$linkText]($absoluteUrl)")
                    } else if (linkText.isNotEmpty()) {
                        text.append(linkText)
                    }
                }
                // Strong/Bold: Convert to Markdown bold format
                node.nodeName() == "strong" && node is Element -> {
                    val strongText = node.text().trim()
                    if (strongText.isNotEmpty()) {
                        text.append("**$strongText**")
                    }
                }
                // Emphasis/Italic: Convert to Markdown italic format
                node.nodeName() == "em" && node is Element -> {
                    val emText = node.text().trim()
                    if (emText.isNotEmpty()) {
                        text.append("*$emText*")
                    }
                }
                // Inline code: Convert to Markdown inline code format
                node.nodeName() == "code" && node is Element -> {
                    val codeText = node.text().trim()
                    if (codeText.isNotEmpty()) {
                        text.append("`$codeText`")
                    }
                }
                // Other inline elements: extract text content or process recursively
                node is Element -> {
                    // Skip nested lists if requested and this is a list element
                    if (excludeNestedLists && node.tagName().lowercase() in listOf("ul", "ol", "li")) {
                        // Skip
                    } else if (node.children().isEmpty()) {
                        val elementText = node.text().trim()
                        if (elementText.isNotEmpty()) {
                            text.append(elementText)
                        }
                    } else {
                        // Process nested inline elements recursively
                        convertInlineContent(node, text, baseUrl, excludeNestedLists)
                    }
                }
            }
        }
    }

    /**
     * Converts blockquote content to markdown format with proper > prefix.
     * Handles nested content within blockquotes including paragraphs, lists, etc.
     */
    private fun convertBlockquoteContent(blockquote: Element, text: StringBuilder, baseUrl: HttpUrl) {
        val quoteText = StringBuilder()

        // Process the blockquote content
        for (child in blockquote.children()) {
            when (child.tagName().lowercase()) {
                "p" -> {
                    convertInlineContent(child, quoteText, baseUrl)
                    quoteText.appendLine()
                }

                else -> {
                    // For other elements, just extract text content
                    val childText = child.text().trim()
                    if (childText.isNotEmpty()) {
                        quoteText.append(childText).appendLine()
                    }
                }
            }
        }

        // If no child elements, get direct text content
        if (quoteText.isEmpty()) {
            val directText = blockquote.ownText().trim()
            if (directText.isNotEmpty()) {
                quoteText.append(directText)
            }
        }

        // Convert to quoted format
        val content = quoteText.toString().trim()
        if (content.isNotEmpty()) {
            // Split by lines and prefix each with >
            content.split('\n').forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty()) {
                    text.append("> ").append(trimmedLine).append("\n")
                } else {
                    text.append(">\n")
                }
            }
        }
    }

    /**
     * Converts HTML lists to Markdown format with proper indentation for nested lists.
     * Uses proper numbering for ordered lists (ol) and bullet points for unordered lists (ul).
     * Handles nested lists with 2-space indentation per level.
     */
    private fun convertListToText(
        listElement: Element,
        text: StringBuilder,
        baseUrl: HttpUrl,
        indent: Int = 0
    ) {
        val isOrdered = listElement.tagName().lowercase() == "ol"
        var itemNumber = 1

        listElement.select("> li").forEach { li ->
            val marker = if (isOrdered) "$itemNumber." else "*"
            text.append("${LIST_INDENT.repeat(indent)}$marker ")

            // Process inline content of the list item, excluding nested lists to prevent duplication
            convertInlineContent(li, text, baseUrl, excludeNestedLists = true)
            text.appendLine()

            if (isOrdered) itemNumber++

            // Handle nested lists with increased indentation
            li.select("> ul, > ol").forEach { nestedList ->
                convertListToText(nestedList, text, baseUrl, indent + 1)
            }
        }
    }

    /**
     * Converts HTML tables to simple text format (not full Markdown tables).
     * This simple approach uses pipe separators and limits rows to prevent excessive content.
     */
    private fun convertTableToText(table: Element, text: StringBuilder) {
        val rows = table.select("tr")
        var rowCount = 0
        rows.forEach { row ->
            // Limit table rows to prevent excessive content and processing time
            if (rowCount >= MAX_TABLE_ROWS) {
                text.append("[... table truncated ...]\n\n")
                return
            }
            val cells = row.select("th, td").map { it.text() }
            if (cells.isNotEmpty()) {
                text.append("${cells.joinToString(" | ")}\n")
            }
            rowCount++
        }
        text.appendLine()
    }

    /**
     * Resolves relative URLs to absolute URLs using the base URL.
     * This ensures all links in the output are functional and clickable.
     * Handles both relative and absolute URLs safely.
     */
    private fun resolveUrl(href: String, baseUrl: HttpUrl): String {
        return try {
            if (href.startsWith("http://") || href.startsWith("https://")) {
                href // Already absolute
            } else {
                // Use HttpUrl.resolve() for proper URL resolution (handles ../path, /path, path, etc.)
                baseUrl.resolve(href)?.toString() ?: href
            }
        } catch (_: Exception) {
            // Return original if resolution fails (malformed URLs, etc.)
            href
        }
    }

    /**
     * Truncates text to the specified maximum length, attempting to break at word boundaries.
     * This prevents token overconsumption in AI models while maintaining readability.
     */
    private fun truncateText(text: String, maxLength: Int): String {
        if (text.length <= maxLength) {
            return text
        }

        // Try to break at word boundary to avoid cutting words in half
        val truncated = text.substring(0, maxLength)
        val lastSpace = truncated.lastIndexOf(' ')
        val lastNewline = truncated.lastIndexOf('\n')

        val breakPoint = maxOf(lastSpace, lastNewline)

        return if (breakPoint > maxLength * 0.8) {
            // Good break point found (within 80% of max length)
            text.take(breakPoint) + "\n\n[... text truncated ...]"
        } else {
            // No good break point, just cut at max length
            text.take(maxLength) + "\n\n[... text truncated ...]"
        }
    }

    companion object {
        // Pre-compiled regular expressions to avoid recompilation on each use
        private val MULTIPLE_NEWLINES_REGEX = Regex("\\n{3,}")
        private val MULTIPLE_SPACES_REGEX = Regex("[ \\t]+")

        // Text length limits to prevent token overconsumption in AI models
        private const val MAX_MARKDOWN_LENGTH = 50_000 // Final markdown output limit
        private const val MAX_INTERMEDIATE_TEXT_LENGTH = 80_000 // Limit during processing
        private const val MAX_CODE_BLOCK_LENGTH = 5_000 // Limit for individual code blocks
        private const val MAX_TABLE_ROWS = 100 // Maximum table rows to process

        // Selectors for unwanted elements (minimal list to avoid over-filtering)
        private val UNWANTED_SELECTORS = listOf(
            "script", "style", "noscript", // Functional elements that shouldn't be in text
            ".ads", ".advertisement", // Common advertising selectors
            "[class*=cookie]", "[class*=popup]", "[class*=modal]" // UI overlays and notices
        )

        // Selectors for main content detection, ordered by reliability
        private val MAIN_CONTENT_SELECTORS = listOf(
            "main", "article", "[role=main]", // HTML5 semantic and ARIA
            ".content", "#content", ".main-content", "#main-content", // Generic content patterns
            ".post-content", ".entry-content", ".article-content" // CMS-specific patterns (WordPress, etc.)
        )

        // Minimal selectors for fallback removal
        private const val FALLBACK_REMOVE_SELECTORS = "aside"

        // Two spaces
        const val LIST_INDENT = "  "
    }

}
