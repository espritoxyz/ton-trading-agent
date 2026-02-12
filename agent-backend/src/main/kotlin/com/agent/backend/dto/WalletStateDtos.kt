package com.agent.backend.dto

import java.time.Instant

data class WalletStateResponse(
    val userId: Long,
    val balance: BalanceData,
    val assets: List<AssetData>,
    val transactions: List<TransactionData>,
    val orders: List<OrderData>,
    val metadata: WalletStateMetadata
)

data class BalanceData(
    val totalUsd: Double,
    val lastUpdated: Instant
)

data class AssetData(
    val id: Long,
    val address: String,
    val amountNano: Long,
    val symbol: String?,
    val decimals: Int?,
    val readableAmount: String?,
    val unitPrice: Double?,
    val usdValue: Double?
)

data class TransactionData(
    val id: Long,
    val transactionHash: String,
    val transactionLt: Long,
    val direction: String,
    val amountNano: Long,
    val assetType: String,
    val jettonMasterAddress: String?,
    val jettonSymbol: String?,
    val jettonDecimals: Int?,
    val senderAddress: String?,
    val recipientAddress: String?,
    val comment: String?,
    val createdAt: Instant
)

data class OrderData(
    val id: Long,
    val jettonMaster: String,
    val action: String, // buy/sell
    val amount: Double,
    val createdAt: Instant,
    val fulfilled: Boolean,
    val symbol: String?
)

data class WalletStateMetadata(
    val fromCache: Boolean,
    val cacheAge: Long?, // milliseconds since cached
    val transactionCount: Int,
    val transactionsLimit: Int?,
    val activeOrdersCount: Int,
    val fulfilledOrdersCount: Int
)
