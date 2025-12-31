package com.agent.llm

import com.explyt.ai.dto.Message

class ChatEnvironment(
    private val _chatHistory: MutableList<Message> = mutableListOf(),
    val userId: Long = 0,
) {
    constructor(chatHistory: List<Message>) : this(
        chatHistory.toMutableList()
    )

    val chatHistory: List<Message>
        get() = _chatHistory

    fun saveMessage(msg: Message) {
       _chatHistory += msg
    }

    fun clearHistory() {
        _chatHistory.clear()
    }
}
