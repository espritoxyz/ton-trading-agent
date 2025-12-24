package com.agent.backend.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 128)
    val password: String,

    val displayName: String? = null
)

data class RegisterResponse(
    val userId: Long,
    val keycloakId: String,
    val initialBalanceUsd: Double = 0.0
)
