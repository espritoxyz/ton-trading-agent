package com.agent.llm

enum class ChatterStatus {
    PROCESSING, TOOLCALLING, ERROR, COMPLETED;

    val isFinished: Boolean
        get() = this == ERROR || this == COMPLETED
}
