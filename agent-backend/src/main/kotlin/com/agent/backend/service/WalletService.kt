package com.agent.backend.service

import com.agent.backend.db.entity.TransactionDirection
import com.agent.backend.db.entity.UserWallet
import com.agent.backend.db.entity.WalletTransaction
import com.agent.backend.db.rep.UserWalletRepository
import com.agent.backend.db.rep.WalletTransactionRepository
import com.agent.backend.security.EncryptionService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class WalletService(
    private val userWalletRepository: UserWalletRepository,
    private val walletTransactionRepository: WalletTransactionRepository,
    private val encryptionService: EncryptionService,
    private val assetService: AssetService,
    private val rabbitTemplate: RabbitTemplate,
    private val notificationEventPublisher: NotificationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(WalletService::class.java)
    private val exchange = "app.events"

    /**
     * Request wallet creation for a user via RabbitMQ
     */
    fun createWalletForUser(userId: Long) {
        logger.info("[wallet-service] Requesting wallet creation for user $userId")

        val message = mapOf(
            "type" to "wallet.create-request",
            "data" to mapOf(
                "userId" to userId,
                "workchain" to 0,
                "walletVersion" to "V5R1"
            )
        )

        rabbitTemplate.convertAndSend(exchange, "wallet.create-request", message)
    }

    /**
     * Save created wallet with encrypted mnemonic
     */
    @Transactional
    fun saveCreatedWallet(
        userId: Long,
        walletAddress: String,
        mnemonicPhrase: String,
        workchain: Int,
        walletVersion: String
    ): UserWallet {
        // Check if wallet already exists
        val existing = userWalletRepository.findByUserId(userId).orElse(null)
        if (existing != null) {
            logger.warn("[wallet-service] Wallet already exists for user $userId, skipping")
            return existing
        }

        // Encrypt mnemonic
        val (encryptedMnemonic, keyId) = encryptionService.encrypt(mnemonicPhrase)

        val wallet = UserWallet(
            userId = userId,
            walletAddress = walletAddress,
            encryptedMnemonic = encryptedMnemonic,
            encryptionKeyId = keyId,
            workchain = workchain,
            walletVersion = walletVersion,
            createdAt = Instant.now(),
            isActive = true
        )

        val saved = userWalletRepository.save(wallet)
        logger.info("[wallet-service] Saved wallet for user $userId: $walletAddress")

        return saved
    }

    /**
     * Get user's wallet
     */
    fun getUserWallet(userId: Long): UserWallet? {
        return userWalletRepository.findByUserId(userId).orElse(null)
    }

    /**
     * Get wallet by address
     */
    fun getWalletByAddress(address: String): UserWallet? {
        return userWalletRepository.findByWalletAddress(address).orElse(null)
    }

    /**
     * Decrypt wallet mnemonic (use with care!)
     */
    fun decryptMnemonic(wallet: UserWallet): String {
        return encryptionService.decrypt(wallet.encryptedMnemonic)
    }

    /**
     * Get all active wallets for monitoring
     */
    fun getAllActiveWallets(): List<UserWallet> {
        return userWalletRepository.findAllByIsActive(true)
    }

    /**
     * Process incoming transaction
     */
    @Transactional
    fun processIncomingTransaction(
        userId: Long,
        walletAddress: String,
        transactionHash: String,
        transactionLt: Long,
        amountNano: Long,
        assetType: String,
        senderAddress: String?,
        jettonMasterAddress: String? = null,
        jettonSymbol: String? = null,
        jettonDecimals: Int? = null,
        comment: String? = null
    ) {
        // Check for duplicates
        val exists = walletTransactionRepository.existsByTransactionHashAndDirection(
            transactionHash,
            TransactionDirection.INCOMING
        )

        if (exists) {
            logger.debug("[wallet-service] Transaction $transactionHash already processed, skipping")
            return
        }

        // Save transaction
        val transaction = WalletTransaction(
            userId = userId,
            walletAddress = walletAddress,
            transactionHash = transactionHash,
            transactionLt = transactionLt,
            direction = TransactionDirection.INCOMING,
            amountNano = amountNano,
            assetType = assetType,
            jettonMasterAddress = jettonMasterAddress,
            jettonSymbol = jettonSymbol,
            jettonDecimals = jettonDecimals,
            senderAddress = senderAddress,
            recipientAddress = walletAddress,
            comment = comment,
            createdAt = Instant.now()
        )

        walletTransactionRepository.save(transaction)
        logger.info("[wallet-service] Saved incoming transaction for user $userId: $transactionHash")

        // Update asset balance
        val assetAddress = jettonMasterAddress ?: "TON"
        assetService.addOrUpdateAsset(userId, assetAddress, amountNano)
        logger.info("[wallet-service] Updated balance for user $userId, asset $assetAddress: +$amountNano nano")

        // Update wallet last_used_at
        val wallet = userWalletRepository.findByUserId(userId).orElse(null)
        if (wallet != null) {
            wallet.lastUsedAt = Instant.now()
            userWalletRepository.save(wallet)
        }

        // Publish notification event for incoming transaction
        val amountDisplay = if (jettonSymbol != null && jettonDecimals != null) {
            val divisor = Math.pow(10.0, jettonDecimals.toDouble())
            val amount = amountNano.toDouble() / divisor
            "$amount $jettonSymbol"
        } else {
            val tonAmount = amountNano.toDouble() / 1_000_000_000.0
            "$tonAmount TON"
        }

        notificationEventPublisher.publishNotificationEvent(
            userId = userId,
            type = "TRANSACTION_COMPLETE",
            title = "Transaction Received",
            message = "Received $amountDisplay",
            metadata = mapOf(
                "transactionId" to transactionHash,
                "status" to "success",
                "amount" to amountNano,
                "currency" to (jettonSymbol ?: "TON"),
                "direction" to "incoming",
                "senderAddress" to (senderAddress ?: "")
            )
        )
    }

    /**
     * Get user transaction history
     */
    fun getUserTransactionHistory(userId: Long): List<WalletTransaction> {
        return walletTransactionRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
    }

    /**
     * Sync asset balance (replace, not add)
     * Used when fetching actual balance from blockchain
     */
    @Transactional
    fun syncAssetBalance(userId: Long, assetAddress: String, amountNano: Long) {
        logger.info("[wallet-service] Syncing balance for user $userId, asset $assetAddress: $amountNano nano")
        assetService.upsertByAddress(userId, assetAddress, amountNano)
    }

    /**
     * Process outgoing TON transaction
     */
    @Transactional
    fun processOutgoingTonTransaction(
        userId: Long,
        transactionHash: String,
        amountNano: Long,
        recipientAddress: String,
        comment: String? = null,
        feeNano: Long? = null
    ) {
        val wallet = userWalletRepository.findByUserId(userId).orElse(null)
        if (wallet == null) {
            logger.warn("[wallet-service] Cannot record outgoing transaction: user $userId has no wallet")
            return
        }

        // Check for duplicates
        val exists = walletTransactionRepository.existsByTransactionHashAndDirection(
            transactionHash,
            TransactionDirection.OUTGOING
        )

        if (exists) {
            logger.debug("[wallet-service] Outgoing transaction $transactionHash already recorded, skipping")
            return
        }

        // Save transaction
        val transaction = WalletTransaction(
            userId = userId,
            walletAddress = wallet.walletAddress,
            transactionHash = transactionHash,
            transactionLt = 0L, // We don't have LT for outgoing transactions yet
            direction = TransactionDirection.OUTGOING,
            amountNano = amountNano,
            assetType = "TON",
            senderAddress = wallet.walletAddress,
            recipientAddress = recipientAddress,
            comment = comment,
            feeNano = feeNano,
            createdAt = Instant.now()
        )

        walletTransactionRepository.save(transaction)
        logger.info("[wallet-service] Saved outgoing TON transaction for user $userId: $transactionHash")

        // Publish notification event for outgoing transaction
        val tonAmount = amountNano.toDouble() / 1_000_000_000.0
        notificationEventPublisher.publishNotificationEvent(
            userId = userId,
            type = "TRANSACTION_COMPLETE",
            title = "Transaction Sent",
            message = "Successfully sent $tonAmount TON",
            metadata = mapOf(
                "transactionId" to transactionHash,
                "status" to "success",
                "amount" to amountNano,
                "currency" to "TON",
                "direction" to "outgoing",
                "recipientAddress" to recipientAddress
            )
        )
    }

    /**
     * Process outgoing token transaction
     */
    @Transactional
    fun processOutgoingTokenTransaction(
        userId: Long,
        transactionHash: String,
        amountNano: Long,
        jettonMasterAddress: String,
        recipientAddress: String,
        jettonSymbol: String? = null,
        jettonDecimals: Int? = null,
        comment: String? = null,
        feeNano: Long? = null
    ) {
        val wallet = userWalletRepository.findByUserId(userId).orElse(null)
        if (wallet == null) {
            logger.warn("[wallet-service] Cannot record outgoing token transaction: user $userId has no wallet")
            return
        }

        // Check for duplicates
        val exists = walletTransactionRepository.existsByTransactionHashAndDirection(
            transactionHash,
            TransactionDirection.OUTGOING
        )

        if (exists) {
            logger.debug("[wallet-service] Outgoing token transaction $transactionHash already recorded, skipping")
            return
        }

        // Save transaction
        val transaction = WalletTransaction(
            userId = userId,
            walletAddress = wallet.walletAddress,
            transactionHash = transactionHash,
            transactionLt = 0L, // We don't have LT for outgoing transactions yet
            direction = TransactionDirection.OUTGOING,
            amountNano = amountNano,
            assetType = "JETTON",
            jettonMasterAddress = jettonMasterAddress,
            jettonSymbol = jettonSymbol,
            jettonDecimals = jettonDecimals,
            senderAddress = wallet.walletAddress,
            recipientAddress = recipientAddress,
            comment = comment,
            feeNano = feeNano,
            createdAt = Instant.now()
        )

        walletTransactionRepository.save(transaction)
        logger.info("[wallet-service] Saved outgoing token transaction for user $userId: $transactionHash")

        // Publish notification event for outgoing token transaction
        val amountDisplay = if (jettonSymbol != null && jettonDecimals != null) {
            val divisor = Math.pow(10.0, jettonDecimals.toDouble())
            val amount = amountNano.toDouble() / divisor
            "$amount $jettonSymbol"
        } else {
            "$amountNano units"
        }

        notificationEventPublisher.publishNotificationEvent(
            userId = userId,
            type = "TRANSACTION_COMPLETE",
            title = "Transaction Sent",
            message = "Successfully sent $amountDisplay",
            metadata = mapOf(
                "transactionId" to transactionHash,
                "status" to "success",
                "amount" to amountNano,
                "currency" to (jettonSymbol ?: "JETTON"),
                "direction" to "outgoing",
                "recipientAddress" to recipientAddress
            )
        )
    }
}
