package com.agent.backend.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class NewsletterSubscribeRequest(
    @field:NotBlank @field:Email val email: String
)

data class NewsletterSubscribeResponse(
    val subscribed: Boolean,
    val message: String
)

data class NewsletterUnsubscribeRequest(
    @field:NotBlank @field:Email val email: String
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
    val totalSubscribers: Int,
    val sent: Int,
    val failed: Int
)
