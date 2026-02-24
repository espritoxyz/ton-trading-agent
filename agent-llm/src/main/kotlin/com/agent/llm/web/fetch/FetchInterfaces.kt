package com.agent.llm.web.fetch

import okhttp3.Headers
import okhttp3.HttpUrl

/**
 * Provider interface for fetching and processing different types of web content.
 * Implementations should handle specific content types (HTML, PDF, etc.).
 */
interface FetchProvider {
    /**
     * Checks if this provider can handle the given content.
     * Called after content is downloaded to determine which provider should process it.
     *
     * @param url The URL of the content
     * @param contentBytes The downloaded content as byte array
     * @param headers Response headers from the server
     * @return true if this provider can handle this content, false otherwise
     */
    fun canHandle(url: HttpUrl, contentBytes: ByteArray, headers: Headers?): Boolean

    /**
     * Processes the downloaded content into a structured document.
     *
     * @param url The original URL
     * @param contentBytes The downloaded content
     * @param headers Response headers
     * @return FetchedDoc with processed content and metadata
     * @throws IOException if content cannot be processed
     */
    suspend fun process(
        url: HttpUrl,
        contentBytes: ByteArray,
        headers: Headers?
    ): FetchedArtifact

    /**
     * Optional method to canonicalize URLs for this provider.
     * Default implementation returns the URL as-is.
     *
     * @param url The URL to canonicalize
     * @return Canonicalized URL
     */
    fun canonicalize(url: HttpUrl): HttpUrl = url
}

/**
 * Extracts and cleans HTML content for processing.
 */
interface HtmlExtractor {
    /**
     * Extracts the main content from HTML and converts it to Markdown.
     *
     * @param html Raw HTML content
     * @param baseUrl Base URL for resolving relative links
     * @return Extracted content with title, cleaned HTML, and Markdown
     */
    suspend fun extract(html: String, baseUrl: HttpUrl): HtmlExtractResult
}

/**
 * Result of HTML content extraction.
 *
 * @param title Extracted page title
 * @param markdown Converted Markdown content
 */
data class HtmlExtractResult(
    val title: String?,
    val markdown: String
)

/**
 * Normalizes and cleans URLs for consistent processing.
 */
interface UrlNormalizer {
    /**
     * Normalizes a raw URL string into a clean, canonical HttpUrl.
     * Removes tracking parameters and applies other cleanup rules.
     *
     * @param raw Raw URL string
     * @return Normalized HttpUrl
     */
    fun normalize(raw: String): HttpUrl

    /**
     * Returns a URL string suitable for HTTP requests, with percent-encoded non-ASCII characters decoded.
     * This is useful for sites like Wikipedia that may block requests with certain encoded sequences.
     *
     * @param httpUrl The HttpUrl to convert to a request-friendly string
     * @return URL string with decoded non-ASCII characters
     */
    fun toRequestUrl(httpUrl: HttpUrl): String
}
