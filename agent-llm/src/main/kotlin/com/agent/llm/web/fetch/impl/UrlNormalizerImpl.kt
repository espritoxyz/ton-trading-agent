package com.agent.llm.web.fetch.impl

import com.agent.llm.web.fetch.SimpleFetchTool
import com.agent.llm.web.fetch.UrlNormalizer
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private val logger = KotlinLogging.logger {}

/**
 * URL normalizer that removes tracking parameters and canonicalizes URLs.
 */
class UrlNormalizerImpl : UrlNormalizer {

    init {
        logger.info { "UrlNormalizerImpl initialized with ${TRACKING_PARAMS.size} tracking parameters" }
    }

    override fun normalize(raw: String): HttpUrl {
        logger.debug { "UrlNormalizerImpl normalizing: $raw" }

        val url = raw.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid URL: $raw")

        logger.debug { "Url port ${url.port}" }

        // Security measure: forbid URLs with an explicit non-default port to avoid access to internal network
        if (url.port != 80 && url.port != 443) {
            logger.warn { "Blocked URL with non-standard port ${url.port}: $raw" }
            throw SimpleFetchTool.FetchSecurityException("URLs with non-standard ports are not allowed for security reasons")
        }


        val builder = url.newBuilder()

        // Remove tracking parameters
        var removedCount = 0
        TRACKING_PARAMS.forEach { param ->
            if (url.queryParameter(param) != null) {
                builder.removeAllQueryParameters(param)
                removedCount++
            }
        }

        if (removedCount > 0) {
            logger.debug { "UrlNormalizerImpl removed $removedCount tracking parameter(s)" }
        }

        // Remove fragment unless it's meaningful (starts with common anchor patterns)
        val fragment = url.fragment
        if (fragment != null && !isMeaningfulFragment(fragment)) {
            builder.fragment(null)
            logger.debug { "UrlNormalizerImpl removed non-meaningful fragment: #$fragment" }
        }

        val normalized = builder.build()

        if (normalized.toString() != raw) {
            logger.debug { "UrlNormalizerImpl normalized to: $normalized" }
        }

        return normalized
    }

    override fun toRequestUrl(httpUrl: HttpUrl): String = URLDecoder.decode(httpUrl.toString(), StandardCharsets.UTF_8)

    private fun isMeaningfulFragment(fragment: String): Boolean {
        // Very short fragments are usually not meaningful
        if (fragment.length <= 2) return false

        // Only preserve fragments that point to content sections
        // These are typically meaningful for understanding document structure
        return fragment.startsWith("section") ||
                fragment.startsWith("heading") ||
                fragment.startsWith("chapter") ||
                fragment.startsWith("introduction") ||
                fragment.startsWith("conclusion") ||
                fragment.startsWith("summary") ||
                fragment.startsWith("overview") ||
                fragment.startsWith("technical") ||
                // Consider fragments with separators as meaningful
                fragment.contains("-") ||
                fragment.contains("_") ||
                // Allow common meaningful single-word fragments
                fragment in setOf("about", "contact", "features", "documentation", "tutorial", "examples")
    }

    companion object {
        /**
         * Comprehensive list of tracking parameters to remove from URLs.
         */
        private val TRACKING_PARAMS = setOf(
            // Google Analytics & Ads
            "utm_source",
            "utm_medium",
            "utm_campaign",
            "utm_term",
            "utm_content",
            "gclid",
            "gclsrc",
            "dclid",
            "wbraid",
            "gbraid",

            // Facebook
            "fbclid",
            "fb_action_ids",
            "fb_action_types",
            "fb_ref",
            "fb_source",

            // Microsoft/Bing
            "msclkid",
            "mc_eid",
            "mc_cid",

            // Yandex
            "yclid",
            "_openstat",

            // HubSpot
            "_hsenc",
            "_hsmi",
            "hsCtaTracking",

            // Mailchimp
            "mc_eid",
            "mc_cid",

            // Adobe
            "s_cid",

            // Amazon
            "tag",
            "linkCode",
            "creativeASIN",
            "linkId",
            "ref_",
            "ref",

            // Twitter
            "twclid",

            // LinkedIn
            "lipi",
            "licu",

            // Pinterest
            "epik",

            // TikTok
            "ttclid",

            // Snapchat
            "sclid",

            // Other common tracking
            "zanpid",
            "igshid",
            "ncid",
            "cmpid",
            "campaign_id",
            "source",
            "medium",
            "campaign",
            "_ga",
            "_gl",
            "mtm_source",
            "mtm_medium",
            "mtm_campaign",
            "mtm_keyword",
            "mtm_content",

            // Additional modern tracking parameters
            "wickedid",
            "vero_conv",
            "vero_id",
            "_kx",
            "trk_contact",
            "trk_msg",
            "pk_campaign",
            "pk_kwd",
            "pk_medium",
            "pk_source", // Matomo/Piwik
            "hsa_acc",
            "hsa_cam",
            "hsa_grp",
            "hsa_ad",
            "hsa_src",
            "hsa_tgt",
            "hsa_kw",
            "hsa_mt",
            "hsa_net",
            "hsa_ver" // Bing Ads
        )
    }
}
