package com.agent.llm.web.fetch

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request


private val logger = KotlinLogging.logger {}

/**
 * Router that downloads content once and tries providers to process it.
 * More efficient than multiple provider-specific downloads.
 */
class FetchRouter(
    val providers: List<FetchProvider>,
    private val urlNormalizer: UrlNormalizer,
) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    init {
        logger.info { "FetchRouter initialized with providers: ${providers.map { it::class.simpleName }}" }
    }

    suspend fun fetch(rawUrl: String): FetchedArtifact = withContext(Dispatchers.IO) {
        logger.debug { "FetchRouter normalizing URL: $rawUrl" }

        // Normalize URL first
        val normalizedUrl = urlNormalizer.normalize(rawUrl)
        logger.debug { "URL normalized: $normalizedUrl" }

        // Canonicalize URL with first provider that wants to (usually just returns as-is)
        val canonicalUrl = providers.firstNotNullOfOrNull {
            val canonical = it.canonicalize(normalizedUrl)
            if (canonical != normalizedUrl) {
                logger.debug { "Provider ${it::class.simpleName} canonicalized URL to: $canonical" }
                canonical
            } else {
                null
            }
        } ?: normalizedUrl

        // Download content once
        logger.debug { "Downloading content from: $canonicalUrl" }
        val (contentBytes, headers, finalUrl) = downloadContent(canonicalUrl)
        logger.info { "Downloaded ${contentBytes.size} bytes from $finalUrl (Content-Type: ${headers?.get("content-type") ?: "unknown"})" }

        val errors = mutableListOf<String>()

        // Try each provider to see if it can handle this content
        for (provider in providers) {
            try {
                if (!provider.canHandle(finalUrl, contentBytes, headers)) {
                    logger.debug { "Provider ${provider::class.simpleName} cannot handle this content" }
                    continue
                }

                logger.debug { "Provider ${provider::class.simpleName} processing content" }
                val result = provider.process(finalUrl, contentBytes, headers)
                logger.info { "Provider ${provider::class.simpleName} successfully processed content (${result.asString().length} chars)" }
                return@withContext result
            } catch (e: Exception) {
                val errorMsg = "${provider::class.simpleName}: ${e.message}"
                logger.warn(e) { "Provider failed: $errorMsg" }
                errors.add(errorMsg)
            }
        }

        // If we get here, no provider could handle the content
        val errorMessage = buildString {
            appendLine("No provider could process the URL: $rawUrl")
            appendLine("Content size: ${contentBytes.size} bytes")
            appendLine("Content-Type: ${headers?.get("content-type") ?: "unknown"}")
            if (errors.isNotEmpty()) {
                appendLine("Provider errors:")
                errors.forEach { error ->
                    appendLine("  - $error")
                }
            }
        }

        logger.error { errorMessage }
        throw IllegalStateException(errorMessage)
    }

    private suspend fun downloadContent(url: HttpUrl): Triple<ByteArray, Headers?, HttpUrl> =
        withContext(Dispatchers.IO) {
            val requestUrl = urlNormalizer.toRequestUrl(url)
            val request = Request.Builder()
                .url(requestUrl)
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected HTTP code ${response.code} for $requestUrl")
                    }

                    val body = response.body
                        ?: throw IOException("Empty response body for $requestUrl")
                    val bodyBytes = body.bytes()

                    val headers = response.headers
                    val finalHttpUrl = response.request.url

                    Triple(bodyBytes, headers, finalHttpUrl)
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to download content from $requestUrl" }
                throw e
            }
        }
}
