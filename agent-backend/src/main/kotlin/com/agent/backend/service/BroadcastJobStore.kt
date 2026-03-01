package com.agent.backend.service

import com.agent.backend.dto.BroadcastJobState
import com.agent.backend.dto.NewsletterBroadcastResponse
import com.agent.backend.dto.NewsletterBroadcastStatusResponse
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class BroadcastJobStore {
    private val jobs = ConcurrentHashMap<String, NewsletterBroadcastStatusResponse>()

    fun create(jobId: String): NewsletterBroadcastStatusResponse {
        val status = NewsletterBroadcastStatusResponse(
            jobId = jobId,
            state = BroadcastJobState.RUNNING,
            startedAt = Instant.now()
        )
        jobs[jobId] = status
        return status
    }

    fun complete(jobId: String, result: NewsletterBroadcastResponse) {
        jobs.computeIfPresent(jobId) { _, existing ->
            existing.copy(
                state = BroadcastJobState.COMPLETED,
                completedAt = Instant.now(),
                result = result
            )
        }
    }

    fun fail(jobId: String) {
        jobs.computeIfPresent(jobId) { _, existing ->
            existing.copy(
                state = BroadcastJobState.FAILED,
                completedAt = Instant.now()
            )
        }
    }

    fun get(jobId: String): NewsletterBroadcastStatusResponse? = jobs[jobId]
}
