package com.agent.backend.service

import kotlinx.coroutines.CompletableDeferred
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for async tool results (e.g. blockchain send/swap) keyed by (messageId, toolName).
 *
 * OpenAIChatter (via AgentBlockchainAdapter) registers waits for final results,
 * and AgentEventsListener completes those waits when it receives RabbitMQ events.
 */
@Service
class ExternalToolResultService {

    private data class Key(val messageId: UUID, val toolName: String)

    // Pending waiters for a given (messageId, toolName)
    private val waiters = ConcurrentHashMap<Key, MutableList<CompletableDeferred<String>>>()

    // Results that arrived before any waiter was registered
    private val buffered = ConcurrentHashMap<Key, MutableList<String>>()

    fun registerWait(messageId: UUID, toolName: String): CompletableDeferred<String> {
        val key = Key(messageId, toolName)

        // Fast path: if we already have a buffered result, consume it immediately
        buffered[key]?.let { list ->
            synchronized(list) {
                if (list.isNotEmpty()) {
                    val value = list.removeAt(0)
                    if (list.isEmpty()) {
                        buffered.remove(key)
                    }
                    return CompletableDeferred<String>().apply { complete(value) }
                }
            }
        }

        val deferred = CompletableDeferred<String>()
        val queue = waiters.computeIfAbsent(key) { mutableListOf() }
        synchronized(queue) {
            queue.add(deferred)
        }
        return deferred
    }

    fun complete(messageId: UUID, toolName: String, result: String) {
        val key = Key(messageId, toolName)
        val queue = waiters[key]
        if (queue != null) {
            synchronized(queue) {
                if (queue.isNotEmpty()) {
                    val deferred = queue.removeAt(0)
                    if (!deferred.isCompleted) {
                        deferred.complete(result)
                    }
                    if (queue.isEmpty()) {
                        waiters.remove(key)
                    }
                    return
                }
            }
        }

        // No active waiters yet – buffer the result so the next waiter consumes it immediately
        val buf = buffered.computeIfAbsent(key) { mutableListOf() }
        synchronized(buf) {
            buf.add(result)
        }
    }
}
