package com.agent.llm.web.fetch

import com.agent.llm.web.fetch.extractor.HtmlExtractorImpl
import com.agent.llm.web.fetch.impl.UrlNormalizerImpl
import com.agent.llm.web.fetch.providers.GenericHtmlFetchProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

class SimpleFetchTool {

    private val urlNormalizer = UrlNormalizerImpl()
    private val htmlExtractor = HtmlExtractorImpl()

    private val htmlProvider = GenericHtmlFetchProvider(htmlExtractor)

    private val router = FetchRouter(
        providers = listOf(htmlProvider),
        urlNormalizer = urlNormalizer
    )

    /**
     * Fetches and processes a single URL into a [FetchedArtifact].
     */
    suspend fun fetch(url: String): FetchedArtifact = withContext(Dispatchers.IO) {
        logger.debug { "SimpleFetchTool fetching: $url" }
        router.fetch(url)
    }
}
