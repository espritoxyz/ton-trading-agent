package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.dto.FetchUrlArgs
import com.agent.llm.web.fetch.SimpleFetchTool
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

/**
 * LLM tool that fetches a single URL and returns extracted Markdown content
 * using the shared web fetch pipeline.
 */
class FetchUrlAgentTool : AgentTool<FetchUrlArgs>() {

    val fetchTool = SimpleFetchTool()

    override val definition: ToolDefinition = ToolDefinition(
        name = "fetch_url",
        description = "Fetch a web page by URL and return cleaned Markdown content with basic metadata.",
        argumentsSchema = ExplytJsonSchema(FetchUrlArgs::class)
    )

    override val argsSerializer = serializer<FetchUrlArgs>()

    override suspend fun payload(args: FetchUrlArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with url='${args.url}'" }

        return try {
            val artifact = fetchTool.fetch(args.url)
            artifact.asString()
        } catch (e: SimpleFetchTool.FetchSecurityException) {
            val msg = e.message!!
            logger.error(e) { msg }
            msg
        } catch (e: Exception) {
            val msg = "Failed to fetch URL '${args.url}': ${e.message}"
            logger.error(e) { msg }
            msg
        }
    }
}
