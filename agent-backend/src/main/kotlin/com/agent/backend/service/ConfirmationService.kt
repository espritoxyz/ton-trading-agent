package com.agent.backend.service

import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap

enum class ConfirmationStatus { PENDING, APPROVED, DECLINED }

data class ConfirmationItem(
    val id: UUID = UUID.randomUUID(),
    val messageId: UUID,
    val userId: Long,
    val toolCallId: String,
    val toolName: String,
    val argsJson: String,
    val text: String,
    @Volatile var status: ConfirmationStatus = ConfirmationStatus.PENDING
)

@Service
class ConfirmationService {
    private val byMessage = ConcurrentHashMap<UUID, MutableList<ConfirmationItem>>()

    fun add(messageId: UUID, item: ConfirmationItem) {
        byMessage.computeIfAbsent(messageId) { mutableListOf() }.add(item)
    }

    fun list(messageId: UUID): List<ConfirmationItem> = byMessage[messageId]?.toList() ?: emptyList()

    fun resolve(messageId: UUID, confirmationId: UUID, approve: Boolean) {
        val list = byMessage[messageId] ?: return
        list.find { it.id == confirmationId }?.let {
            it.status = if (approve) ConfirmationStatus.APPROVED else ConfirmationStatus.DECLINED
        }
    }

    fun allResolved(messageId: UUID): Boolean = list(messageId).all { it.status != ConfirmationStatus.PENDING }

    fun approved(messageId: UUID): List<ConfirmationItem> = list(messageId).filter { it.status == ConfirmationStatus.APPROVED }
}
