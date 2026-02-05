package com.agent.backend.service

import com.agent.backend.db.entity.DepositRequest
import com.agent.backend.db.entity.DepositStatus
import com.agent.backend.db.entity.ProcessedTransaction
import com.agent.backend.db.rep.DepositRequestRepository
import com.agent.backend.db.rep.ProcessedTransactionRepository
import com.agent.backend.dto.DepositHistoryItem
import com.agent.backend.dto.DepositStatusResponse
import com.agent.backend.dto.InitiateDepositResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.random.Random

@Service
class DepositService(
    private val depositRequestRepository: DepositRequestRepository,
    private val processedTransactionRepository: ProcessedTransactionRepository,
    private val assetService: AssetService,
    @Value("\${deposit.wallet.address}") private val depositWalletAddress: String,
    @Value("\${deposit.expiry.hours:24}") private val expiryHours: Long
) {
    private val logger = LoggerFactory.getLogger(DepositService::class.java)

    companion object {
        private const val CODE_LENGTH = 6
        private const val CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    }

    @Transactional
    fun initiateDeposit(userId: Long): InitiateDepositResponse {
        val now = Instant.now()

        // Check if user already has an active deposit request
        val activeDeposit = depositRequestRepository.findFirstByUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            userId = userId,
            status = DepositStatus.PENDING,
            expiresAt = now
        )

        if (activeDeposit != null) {
            logger.info("Returning existing active deposit request for user $userId with code ${activeDeposit.code}")
            return activeDeposit.toInitiateDepositResponse()
        }

        // Create new deposit request
        val code = generateUniqueCode()
        val expiresAt = now.plusSeconds(expiryHours * 3600)

        val depositRequest = DepositRequest(
            userId = userId,
            code = code,
            depositWalletAddress = depositWalletAddress,
            status = DepositStatus.PENDING,
            createdAt = now,
            expiresAt = expiresAt
        )

        val saved = depositRequestRepository.save(depositRequest)

        logger.info("Created new deposit request for user $userId with code $code, expires at $expiresAt")

        return saved.toInitiateDepositResponse()
    }

    fun getDepositStatus(depositRequestId: Long): DepositStatusResponse? {
        val depositRequest = depositRequestRepository.findById(depositRequestId).orElse(null) ?: return null

        return DepositStatusResponse(
            depositRequestId = depositRequest.id!!,
            code = depositRequest.code,
            status = depositRequest.status,
            amountTon = depositRequest.amountNano?.let {
                nanoToReadable(it, depositRequest.jettonDecimals ?: 9)
            },
            assetType = depositRequest.assetType,
            jettonSymbol = depositRequest.jettonSymbol,
            jettonMasterAddress = depositRequest.jettonMasterAddress,
            transactionHash = depositRequest.transactionHash,
            createdAt = depositRequest.createdAt,
            expiresAt = depositRequest.expiresAt,
            completedAt = depositRequest.completedAt
        )
    }

    fun getDepositHistory(userId: Long): List<DepositHistoryItem> {
        return depositRequestRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map { dr ->
            DepositHistoryItem(
                depositRequestId = dr.id!!,
                code = dr.code,
                status = dr.status,
                amountTon = dr.amountNano?.let {
                    nanoToReadable(it, dr.jettonDecimals ?: 9)
                },
                assetType = dr.assetType,
                jettonSymbol = dr.jettonSymbol,
                transactionHash = dr.transactionHash,
                createdAt = dr.createdAt,
                completedAt = dr.completedAt
            )
        }
    }

    @Transactional
    fun processDepositDetected(
        code: String,
        transactionHash: String,
        transactionLt: Long,
        bodyHash: String,
        amountNano: Long,
        assetType: String,
        jettonMasterAddress: String? = null,
        jettonSymbol: String? = null,
        jettonDecimals: Int? = null
    ) {
        // Find pending deposit request by code
        val depositRequest = depositRequestRepository.findByCodeAndStatus(code, DepositStatus.PENDING)
            ?: throw IllegalArgumentException("No pending deposit request found for code: $code")

        // Check if already processed
        if (processedTransactionRepository.existsByBodyHash(bodyHash)) {
            logger.warn("Transaction with bodyHash $bodyHash already processed, skipping")
            return
        }

        // Update deposit request
        depositRequest.status = DepositStatus.COMPLETED
        depositRequest.amountNano = amountNano
        depositRequest.assetType = assetType
        depositRequest.jettonMasterAddress = jettonMasterAddress
        depositRequest.jettonSymbol = jettonSymbol
        depositRequest.jettonDecimals = jettonDecimals
        depositRequest.transactionHash = transactionHash
        depositRequest.transactionLt = transactionLt
        depositRequest.completedAt = Instant.now()
        depositRequestRepository.save(depositRequest)

        // Save processed transaction
        val processedTx = ProcessedTransaction(
            bodyHash = bodyHash,
            transactionLt = transactionLt,
            transactionHash = transactionHash,
            depositRequestId = depositRequest.id
        )
        processedTransactionRepository.save(processedTx)

        // Determine asset address for the Asset table
        val assetAddress = when (assetType) {
            "TON" -> "TON"
            "JETTON" -> jettonMasterAddress ?: throw IllegalArgumentException("Jetton master address is required for jetton deposits")
            else -> throw IllegalArgumentException("Unknown asset type: $assetType")
        }

        // Update user's asset balance
        assetService.addOrUpdateAsset(depositRequest.userId, assetAddress, amountNano)

        // Log with appropriate units
        val readableAmount = when (assetType) {
            "TON" -> "${nanoToReadable(amountNano, 9)} TON"
            "JETTON" -> "${nanoToReadable(amountNano, jettonDecimals ?: 9)} ${jettonSymbol ?: "JETTON"}"
            else -> "$amountNano nano"
        }

        logger.info("Deposit completed for code $code: $readableAmount ($assetType), user ${depositRequest.userId}, tx $transactionHash")
    }

    @Transactional
    fun expirePendingRequests(): Int {
        val now = Instant.now()
        val expiredRequests = depositRequestRepository.findAllByStatusAndExpiresAtBefore(DepositStatus.PENDING, now)

        expiredRequests.forEach { it.status = DepositStatus.EXPIRED }
        depositRequestRepository.saveAll(expiredRequests)

        if (expiredRequests.isNotEmpty()) {
            logger.info("Expired ${expiredRequests.size} pending deposit requests")
        }

        return expiredRequests.size
    }

    private fun generateUniqueCode(): String {
        var attempts = 0
        while (attempts < 100) {
            val code = generateCode()
            val existing = depositRequestRepository.findByCodeAndStatus(code, DepositStatus.PENDING)
            if (existing == null) {
                return code
            }
            attempts++
        }
        throw IllegalStateException("Failed to generate unique deposit code after 100 attempts")
    }

    private fun generateCode(): String {
        return (1..CODE_LENGTH)
            .map { CODE_CHARS[Random.nextInt(CODE_CHARS.length)] }
            .joinToString("")
    }

    private fun nanoToTon(nanoTon: Long): String {
        return String.format("%.4f", nanoTon / 1_000_000_000.0)
    }

    private fun nanoToReadable(nanoAmount: Long, decimals: Int): String {
        val divisor = Math.pow(10.0, decimals.toDouble())
        return String.format("%.${decimals}f", nanoAmount / divisor)
    }

    private fun DepositRequest.toInitiateDepositResponse(): InitiateDepositResponse {
        return InitiateDepositResponse(
            depositRequestId = this.id!!,
            code = this.code,
            depositWalletAddress = this.depositWalletAddress,
            expiresAt = this.expiresAt,
            status = this.status
        )
    }
}
