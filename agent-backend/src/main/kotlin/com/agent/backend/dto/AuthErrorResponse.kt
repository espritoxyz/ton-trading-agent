package com.agent.backend.dto

/**
 * Structured error codes for authentication flow.
 * Frontend maps these codes to localized, human-readable messages.
 */
enum class AuthErrorCode {
    INVALID_CREDENTIALS,
    ACCOUNT_DISABLED,
    ACCOUNT_NOT_VERIFIED,
    ACCOUNT_LOCKED,
    USER_ALREADY_EXISTS,
    WEAK_PASSWORD,
    INVALID_EMAIL,
    AUTH_PROVIDER_UNAVAILABLE,
    TOO_MANY_REQUESTS,
    INTERNAL_ERROR
}

data class AuthErrorResponse(
    val code: AuthErrorCode,
    val message: String
)
