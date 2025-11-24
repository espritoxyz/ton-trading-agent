package com.agent.backend.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class UserInfoResponse(
    val userId: Long,
    val subject: String,
    val email: String?
)

/**
 * Keep updates minimal. Normally email is owned by IdP (Keycloak),
 * but you said you want fetch/update by user_id—so we allow email here for demo.
 * In real prod you’d likely avoid updating it locally or mark as “local override”.
 */
data class UserUpdateRequest(
    @field:Email @field:Size(max = 320)
    val email: String?
)

data class BalanceResponse(
    val userId: Long,
    val totalUsd: Double = 0.0 // placeholder, per your requirement
)
