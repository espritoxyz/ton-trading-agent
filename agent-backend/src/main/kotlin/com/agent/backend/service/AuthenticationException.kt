package com.agent.backend.service

import com.agent.backend.dto.AuthErrorCode

/**
 * Custom authentication exception carrying a structured [AuthErrorCode].
 * Thrown by [AuthService] and caught by [AuthController] to produce
 * human-readable, structured error responses.
 */
class AuthenticationException(
    val errorCode: AuthErrorCode,
    message: String
) : RuntimeException(message)
