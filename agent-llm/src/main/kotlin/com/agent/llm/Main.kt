package com.agent.llm

import com.agent.llm.tool.ToolDefinitions
import com.explyt.ai.backend.http.ApiKeyParam
import com.explyt.ai.dto.ChatRequest
import com.explyt.ai.dto.Message
import com.explyt.ai.dto.ModelConfig
import com.explyt.ai.dto.Prompt
import com.explyt.ai.router.dto.RemoteProvider
import com.explyt.ai.router.router.AiRouterLocal
import kotlinx.coroutines.runBlocking

fun main() {
    val router = AiRouterLocal()
    val allModels = router.availableModels().providerToModelConfigs
    val openAiModels = allModels[RemoteProvider.OpenAI] ?: emptyList()
    val modelInfo = openAiModels.first { it.modelName.contains("gpt-5-mini") }

    val modelConfig = ModelConfig(
        modelInfo = modelInfo,
        modelSpecificParams = ApiKeyParam(System.getenv("OPENAI_API_KEY")),
        toolDefinitions = emptyList()
    )

    val prompt = Prompt(
        messages = listOf(
            Message.user("Say hello back to me")
        )
    )


    val response = runBlocking {
        router.chat(ChatRequest(modelConfig, prompt))
    }

    println(response.response)
}
