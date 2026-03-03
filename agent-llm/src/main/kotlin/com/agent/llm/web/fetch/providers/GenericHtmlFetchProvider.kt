package com.agent.llm.web.fetch.providers

import com.agent.llm.web.fetch.ContentType
import com.agent.llm.web.fetch.FetchProvider
import com.agent.llm.web.fetch.FetchedArtifact
import com.agent.llm.web.fetch.FetchedDoc
import com.agent.llm.web.fetch.HtmlExtractor
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl
import org.brotli.dec.BrotliInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import okhttp3.Headers

private val logger = KotlinLogging.logger {}

/**
 * Generic HTML fetch provider that handles most web pages.
 * Checks content and delegates extraction to HtmlExtractor.
 * Includes proper handling of content encoding (gzip, deflate).
 */
class GenericHtmlFetchProvider(
    private val htmlExtractor: HtmlExtractor
) : FetchProvider {

    init {
        logger.info { "GenericHtmlFetchProvider initialized" }
    }

    override fun canHandle(url: HttpUrl, contentBytes: ByteArray, headers: Headers?): Boolean {
        // Check content type header first
        val contentType = headers?.get("content-type")?.lowercase(Locale.getDefault())
        if (contentType?.contains(HTML_CONTENT_TYPE) == true ||
            contentType?.contains(XHTML_CONTENT_TYPE) == true) {
            logger.debug { "GenericHtmlFetchProvider: Content type indicates HTML: $contentType" }
            return true
        }

        // Fallback: try to detect HTML by looking at first bytes
        if (contentBytes.size > 100) {
            val preview = String(contentBytes.take(100).toByteArray(), StandardCharsets.UTF_8).lowercase()
            if (preview.contains("<!doctype html") ||
                preview.contains("<html") ||
                preview.contains("<head")) {
                logger.debug { "GenericHtmlFetchProvider: HTML detected from content signature" }
                return true
            }
        }

        // As last resort, accept anything (generic fallback provider)
        logger.debug { "GenericHtmlFetchProvider: Accepting as generic fallback" }
        return true
    }

    override suspend fun process(
        url: HttpUrl,
        contentBytes: ByteArray,
        headers: Headers?
    ): FetchedArtifact {
        logger.debug { "GenericHtmlFetchProvider processing: $url (${contentBytes.size} bytes)" }

        if (contentBytes.isEmpty()) {
            logger.error { "GenericHtmlFetchProvider: Received empty content for $url" }
            throw IOException("Received empty content")
        }

        // Decompress if needed based on content-encoding header
        val contentEncoding = headers?.get("content-encoding")
        val htmlContent = when (contentEncoding?.lowercase(Locale.getDefault())) {
            "gzip" -> {
                logger.debug { "GenericHtmlFetchProvider: Decompressing gzip content" }
                decompressGzip(contentBytes)
            }
            "deflate" -> {
                logger.debug { "GenericHtmlFetchProvider: Decompressing deflate content" }
                decompressDeflate(contentBytes)
            }
            "br" -> {
                logger.debug { "GenericHtmlFetchProvider: Decompressing brotli content" }
                decompressBrotli(contentBytes)
            }
            else -> String(contentBytes, StandardCharsets.UTF_8)
        }

        logger.debug { "GenericHtmlFetchProvider: Extracting content (${htmlContent.length} chars)" }

        // Extract content using HtmlExtractor
        val extractResult = htmlExtractor.extract(htmlContent, url)

        // Build metadata
        val meta = buildMetadata(url, extractResult.title, contentEncoding)

        logger.info { "GenericHtmlFetchProvider: Successfully processed $url (title: '${extractResult.title}', ${extractResult.markdown.length} chars)" }

        return FetchedDoc(
            url = url.toString(),
            title = extractResult.title,
            contentType = ContentType.HTML,
            markdown = extractResult.markdown,
            meta = meta,
            providerName = PROVIDER_NAME
        )
    }

    private fun decompressGzip(data: ByteArray): String {
        return try {
            GZIPInputStream(ByteArrayInputStream(data)).use { gzipStream ->
                gzipStream.readAllBytes().toString(StandardCharsets.UTF_8)
            }
        } catch (e: Exception) {
            logger.warn(e) { "GenericHtmlFetchProvider: Gzip decompression failed, trying as plain text" }
            // If decompression fails, try as plain text
            String(data, StandardCharsets.UTF_8)
        }
    }

    private fun decompressDeflate(data: ByteArray): String {
        return try {
            InflaterInputStream(ByteArrayInputStream(data)).use { deflateStream ->
                deflateStream.readAllBytes().toString(StandardCharsets.UTF_8)
            }
        } catch (e: Exception) {
            logger.warn(e) { "GenericHtmlFetchProvider: Deflate decompression failed, trying as plain text" }
            // If decompression fails, try as plain text
            String(data, StandardCharsets.UTF_8)
        }
    }

    private fun decompressBrotli(data: ByteArray): String {
        return try {
            BrotliInputStream(ByteArrayInputStream(data)).use { brotliStream ->
                brotliStream.readAllBytes().toString(StandardCharsets.UTF_8)
            }
        } catch (e: Exception) {
            logger.warn(e) { "GenericHtmlFetchProvider: Brotli decompression failed, trying as plain text" }
            // If decompression fails, try as plain text
            String(data, StandardCharsets.UTF_8)
        }
    }

    private fun buildMetadata(
        url: HttpUrl,
        title: String?,
        contentEncoding: String?
    ): Map<String, String> {
        val meta = mutableMapOf<String, String>()

        meta[META_FETCH_AT] = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)
        meta[META_SOURCE_HOST] = url.host
        title?.let { meta[META_TITLE] = it }
        contentEncoding?.let { meta[META_CONTENT_ENCODING] = it }

        return meta
    }

    companion object {
        private const val PROVIDER_NAME = "GenericHtmlFetchProvider"

        private const val HTML_CONTENT_TYPE = "text/html"
        private const val XHTML_CONTENT_TYPE = "application/xhtml+xml"

        private const val META_FETCH_AT = "fetchAt"
        private const val META_SOURCE_HOST = "sourceHost"
        private const val META_TITLE = "title"
        private const val META_CONTENT_ENCODING = "contentEncoding"
    }
}
