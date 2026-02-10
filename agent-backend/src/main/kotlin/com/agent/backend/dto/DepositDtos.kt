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
    val amountTon: String?, // Renamed but still works for backward compatibility - now contains readable amount for any asset
    val assetType: String?, // "TON" or "JETTON"
    val jettonSymbol: String?,
    val jettonMasterAddress: String?,
    val transactionHash: String?,
    val createdAt: Instant,
    val expiresAt: Instant,
    val completedAt: Instant?,
    val unitPrice: Double?, // USD price per unit of asset
    val usdValue: Double? // Total USD value of the deposit
)

data class DepositHistoryItem(
    val depositRequestId: Long,
    val code: String,
    val status: DepositStatus,
    val amountTon: String?, // Renamed but still works for backward compatibility
    val assetType: String?,
    val jettonSymbol: String?,
    val transactionHash: String?,
    val createdAt: Instant,
    val completedAt: Instant?
)
