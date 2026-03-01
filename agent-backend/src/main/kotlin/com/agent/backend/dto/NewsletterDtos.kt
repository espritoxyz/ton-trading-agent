package com.agent.backend.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class NewsletterSubscribeRequest(
    @field:NotBlank @field:Email val email: String
)

data class NewsletterSubscribeResponse(
    val subscribed: Boolean,
    val pending: Boolean = false,
    val message: String
)

data class NewsletterUnsubscribeResponse(
    val unsubscribed: Boolean,
    val message: String
)

data class NewsletterBroadcastRequest(
    @field:NotBlank val subject: String,
    @field:NotBlank val htmlContent: String
)

data class NewsletterBroadcastResponse(
    val totalSubscribers: Long,
    val sent: Int,
    val failed: Int
)

data class NewsletterBroadcastStartedResponse(val jobId: String)

enum class BroadcastJobState { RUNNING, COMPLETED, FAILED }

data class NewsletterBroadcastStatusResponse(
    val jobId: String,
    val state: BroadcastJobState,
    val startedAt: Instant,
    val completedAt: Instant? = null,
    val result: NewsletterBroadcastResponse? = null
)

data class NewsletterConfirmResponse(
    val confirmed: Boolean,
    val message: String
)

data class NewsletterResendRequest(
    @field:NotBlank @field:Email val email: String
)

data class NewsletterResendResponse(
    val sent: Boolean,
    val message: String
)

data class NewsletterSubscriptionStatusResponse(
    val subscribed: Boolean
)

data class NewsletterSubscriptionUpdateRequest(
    val subscribed: Boolean
)
