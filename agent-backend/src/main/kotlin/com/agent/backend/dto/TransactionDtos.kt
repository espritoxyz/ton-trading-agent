package com.agent.backend.dto

import com.agent.backend.db.entity.TransactionDirection
import java.time.Instant

data class WalletTransactionDto(
    val id: Long,
    val transactionHash: String,
    val transactionLt: Long,
    val direction: TransactionDirection,
    val amountNano: Long,
    val assetType: String,
    val jettonMasterAddress: String?,
    val jettonSymbol: String?,
    val jettonDecimals: Int?,
    val senderAddress: String?,
    val recipientAddress: String?,
    val comment: String?,
    val feeNano: Long?,
    val createdAt: Instant
)

data class TransactionHistoryResponse(
    val transactions: List<WalletTransactionDto>,
    val total: Int
)
