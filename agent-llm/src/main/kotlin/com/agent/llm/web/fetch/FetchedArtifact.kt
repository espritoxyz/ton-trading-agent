package com.agent.llm.web.fetch

interface FetchedArtifact {
    fun asString(): String
}

/**
 * Represents a fetched document with extracted content and metadata.
 *
 * @param url The canonical URL of the document
 * @param title The document title if available
 * @param contentType MIME content type (e.g., "text/html")
 * @param markdown The extracted content as markdown text
 * @param meta Additional metadata (etag, lastModified, fetchAt, etc.)
 * @param providerName Name of the provider that fetched this document
 */
data class FetchedDoc(
    val url: String,
    val title: String?,
    val contentType: ContentType,
    val markdown: String,
    val meta: Map<String, String> = emptyMap(),
    val providerName: String,
) : FetchedArtifact {
    override fun asString() = buildString {
        appendLine("**Document:** $url")

        title?.let { title ->
            appendLine("**Title:** $title")
        }

        // Show PDF page count if available
        meta["totalPages"]?.let { totalPages ->
            appendLine("**Total Pages:** $totalPages")
        }

        // Extract metadata
        meta["description"]?.let { desc ->
            appendLine("**Description:** $desc")
        }
        meta["author"]?.let { author ->
            appendLine("**Author:** $author")
        }
        meta["language"]?.let { lang ->
            appendLine("**Language:** $lang")
        }
        meta["keywords"]?.let { keywords ->
            val keywordList = keywords.split(",").map { it.trim() }
            if (keywordList.isNotEmpty()) {
                appendLine("**Keywords:** ${keywordList.joinToString(", ")}")
            }
        }

        appendLine()
        appendLine("**Content:**")
        appendLine()

        // Use markdown content directly
        val wordCount = markdown.split(Regex("\\s+")).size
        appendLine("*Content length: $wordCount words*")
        appendLine()
        appendLine(markdown)
    }
}

/**
 * Supported content types for fetched documents.
 */
enum class ContentType(val stringValue: String) {
    HTML("text/html");

    override fun toString(): String = stringValue
}
