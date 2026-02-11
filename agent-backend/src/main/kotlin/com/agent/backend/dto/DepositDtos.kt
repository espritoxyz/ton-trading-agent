package com.agent.backend.dto

import java.time.Instant

data class InitiateDepositRequest(
    val userId: Long
)

data class SimpleDepositResponse(
    val walletAddress: String,
    val expiresAt: Instant,
    val message: String
)
