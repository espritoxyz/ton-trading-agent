package com.agent.llm.tool.impl

import com.agent.llm.tool.api.AgentTool
import com.agent.llm.tool.dto.FetchUrlArgs
import com.explyt.ai.dto.ExplytJsonSchema
import com.explyt.ai.dto.ToolDefinition
import com.agent.llm.web.fetch.SimpleFetchTool
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.serializer

private val logger = KotlinLogging.logger {}

/**
 * LLM tool that fetches a single URL and returns extracted Markdown content
 * using the shared web fetch pipeline.
 */
class FetchUrlAgentTool : AgentTool<FetchUrlArgs>() {

    override val definition: ToolDefinition = ToolDefinition(
        name = "fetch_url",
        description = "Fetch a web page by URL and return cleaned Markdown content with basic metadata.",
        argumentsSchema = ExplytJsonSchema(FetchUrlArgs::class)
    )

    override val argsSerializer = serializer<FetchUrlArgs>()

    override fun payload(args: FetchUrlArgs): String {
        logger.debug { "FIRED \"${definition.name}\" TOOL with url='${args.url}'" }

        // I know it is bad to run this in coroutine-based environment, but it does the job
        // and you won't catch me implementing async page fetching. But one day it will hang here.
        // One day.
        return runBlocking {
            try {
                val artifact = SimpleFetchTool().fetch(args.url)
                artifact.asString()
            } catch (e: Exception) {
                logger.error(e) { "Failed to fetch URL '${args.url}': ${e.message}" }
                "Failed to fetch URL '${args.url}': ${e.message ?: "unknown error"}"
            }
        }
    }
}
