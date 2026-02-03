package com.agent.backend.dto

import com.agent.backend.db.entity.DepositStatus
import java.time.Instant

data class InitiateDepositRequest(
    val userId: Long
)

data class InitiateDepositResponse(
    val depositRequestId: Long,
    val code: String,
    val depositWalletAddress: String,
    val expiresAt: Instant,
    val status: DepositStatus
)

data class DepositStatusResponse(
    val depositRequestId: Long,
    val code: String,
    val status: DepositStatus,
    val amountTon: String?,
    val transactionHash: String?,
    val createdAt: Instant,
    val expiresAt: Instant,
    val completedAt: Instant?
)

data class DepositHistoryItem(
    val depositRequestId: Long,
    val code: String,
    val status: DepositStatus,
    val amountTon: String?,
    val transactionHash: String?,
    val createdAt: Instant,
    val completedAt: Instant?
)
