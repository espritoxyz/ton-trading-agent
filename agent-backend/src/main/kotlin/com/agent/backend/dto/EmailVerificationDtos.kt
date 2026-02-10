package com.agent.backend.dto

data class VerifyEmailRequest(
    val token: String
)

data class VerifyEmailResponse(
    val success: Boolean,
    val message: String
)

data class ResendVerificationRequest(
    val email: String? = null
)

data class ResendVerificationResponse(
    val success: Boolean,
    val message: String
)
