package com.agent.backend.controller

import com.agent.backend.service.UserProvisioningService
import com.agent.backend.service.WalletService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.time.Instant

private val walletLogger = KotlinLogging.logger {}

data class WalletInfoResponse(
    val walletAddress: String,
    val walletVersion: String,
    val workchain: Int,
    val createdAt: Instant,
    val lastUsedAt: Instant?,
    val isActive: Boolean
)

data class TransactionHistoryResponse(
    val transactions: List<TransactionItem>
)

data class TransactionItem(
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

@RestController
@RequestMapping("/wallet")
class WalletController(
    private val provisioning: UserProvisioningService,
    private val walletService: WalletService
) {
    private fun currentUserId(auth: JwtAuthenticationToken): Long {
        val sub = auth.token.subject
        val email = auth.token.claims["email"] as? String
        return provisioning.resolveOrCreate(sub, email).id!!
    }

    @GetMapping("/info")
    fun getWalletInfo(auth: JwtAuthenticationToken): ResponseEntity<WalletInfoResponse> {
        val userId = currentUserId(auth)
        val wallet = walletService.getUserWallet(userId)
            ?: return ResponseEntity.notFound().build()

        walletLogger.debug { "Wallet info requested for user $userId" }

        val response = WalletInfoResponse(
            walletAddress = wallet.walletAddress,
            walletVersion = wallet.walletVersion,
            workchain = wallet.workchain,
            createdAt = wallet.createdAt,
            lastUsedAt = wallet.lastUsedAt,
            isActive = wallet.isActive
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/transactions")
    fun getTransactions(auth: JwtAuthenticationToken): ResponseEntity<TransactionHistoryResponse> {
        val userId = currentUserId(auth)
        val transactions = walletService.getUserTransactionHistory(userId)

        walletLogger.debug { "Transaction history requested for user $userId, found ${transactions.size} transactions" }

        val items = transactions.map { tx ->
            TransactionItem(
                id = tx.id ?: 0,
                transactionHash = tx.transactionHash,
                transactionLt = tx.transactionLt,
                direction = tx.direction.name,
                amountNano = tx.amountNano,
                assetType = tx.assetType,
                jettonMasterAddress = tx.jettonMasterAddress,
                jettonSymbol = tx.jettonSymbol,
                jettonDecimals = tx.jettonDecimals,
                senderAddress = tx.senderAddress,
                recipientAddress = tx.recipientAddress,
                comment = tx.comment,
                createdAt = tx.createdAt
            )
        }

        return ResponseEntity.ok(TransactionHistoryResponse(transactions = items))
    }
}
