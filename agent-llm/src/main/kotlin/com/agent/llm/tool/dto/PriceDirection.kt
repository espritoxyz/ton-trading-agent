package com.agent.llm.tool.dto

import kotlinx.serialization.Serializable

@Serializable
enum class PriceDirection {
    UP, DOWN, EQUAL
}