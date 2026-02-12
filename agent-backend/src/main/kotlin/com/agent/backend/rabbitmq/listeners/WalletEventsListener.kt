package com.agent.backend.rabbitmq.listeners

import com.agent.backend.rabbitmq.RabbitConfig
import com.agent.backend.service.WalletService
import com.agent.backend.service.WalletStateService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class WalletEventsListener(
    private val walletService: WalletService,
    private val walletStateService: WalletStateService,
    private val rabbitTemplate: RabbitTemplate
) {
    private val logger = KotlinLogging.logger {}
    private val exchange = "app.events"

    @RabbitListener(queues = [RabbitConfig.QUEUE_WALLET])
    fun handleWalletEvents(message: Map<String, Any>) {
        val type = message["type"] as? String ?: return

        when (type) {
            "wallet.create-response" -> handleWalletCreateResponse(message)
            "wallet.transaction-detected" -> handleTransactionDetected(message)
            "wallet.list-active-request" -> handleListActiveRequest(message)
            "wallet.balance-synced" -> handleBalanceSynced(message)
        }
    }

    private fun handleWalletCreateResponse(message: Map<String, Any>) {
        try {
            val data = message["data"] as? Map<*, *> ?: return
            val userId = (data["userId"] as? Number)?.toLong() ?: return
            val walletAddress = data["walletAddress"] as? String ?: return
            val mnemonicPhrase = data["mnemonicPhrase"] as? String ?: return
            val workchain = (data["workchain"] as? Number)?.toInt() ?: 0
            val walletVersion = data["walletVersion"] as? String ?: "V5R1"

            logger.info { "[wallet-events] Received wallet creation response for user $userId" }

            walletService.saveCreatedWallet(
                userId = userId,
                walletAddress = walletAddress,
                mnemonicPhrase = mnemonicPhrase,
                workchain = workchain,
                walletVersion = walletVersion
            )

            logger.info { "[wallet-events] Successfully saved wallet for user $userId" }
        } catch (e: Exception) {
            logger.error(e) { "[wallet-events] Error handling wallet creation response" }
        }
    }

    private fun handleTransactionDetected(message: Map<String, Any>) {
        try {
            val data = message["data"] as? Map<*, *>
            if (data == null) {
                logger.error { "[wallet-events] Missing 'data' field in transaction message" }
                return
            }

            val userId = (data["userId"] as? Number)?.toLong()
            if (userId == null) {
                logger.error { "[wallet-events] Missing or invalid 'userId' in transaction message: ${data["userId"]}" }
                return
            }

            val walletAddress = data["walletAddress"] as? String
            if (walletAddress == null) {
                logger.error { "[wallet-events] Missing 'walletAddress' for user $userId" }
                return
            }

            val transactionHash = data["transactionHash"] as? String
            if (transactionHash == null) {
                logger.error { "[wallet-events] Missing 'transactionHash' for user $userId. Data: $data" }
                return
            }

            val transactionLt = (data["transactionLt"] as? String)?.toLongOrNull()
            if (transactionLt == null) {
                logger.error { "[wallet-events] Missing or invalid 'transactionLt' for user $userId: ${data["transactionLt"]}" }
                return
            }

            val amountNano = (data["amountNano"] as? String)?.toLongOrNull()
            if (amountNano == null) {
                logger.error { "[wallet-events] Missing or invalid 'amountNano' for user $userId: ${data["amountNano"]}" }
                return
            }

            val assetType = data["assetType"] as? String ?: "TON"
            val senderAddress = data["senderAddress"] as? String
            val jettonMasterAddress = data["jettonMasterAddress"] as? String
            val jettonSymbol = data["jettonSymbol"] as? String
            val jettonDecimals = (data["jettonDecimals"] as? Number)?.toInt()
            val comment = data["comment"] as? String

            logger.info { "[wallet-events] Transaction detected for user $userId: $transactionHash" }

            walletService.processIncomingTransaction(
                userId = userId,
                walletAddress = walletAddress,
                transactionHash = transactionHash,
                transactionLt = transactionLt,
                amountNano = amountNano,
                assetType = assetType,
                senderAddress = senderAddress,
                jettonMasterAddress = jettonMasterAddress,
                jettonSymbol = jettonSymbol,
                jettonDecimals = jettonDecimals,
                comment = comment
            )

            // Invalidate wallet state cache
            walletStateService.invalidateCache(userId, "transaction-detected")

            logger.info { "[wallet-events] Successfully processed transaction for user $userId" }
        } catch (e: Exception) {
            logger.error(e) { "[wallet-events] Error handling transaction detection" }
        }
    }

    private fun handleListActiveRequest(message: Map<String, Any>) {
        try {
            logger.info { "[wallet-events] Received request for active wallets list" }

            val activeWallets = walletService.getAllActiveWallets()
            val walletsData = activeWallets.map { wallet ->
                mapOf(
                    "userId" to wallet.userId,
                    "walletAddress" to wallet.walletAddress
                )
            }

            val response = mapOf(
                "type" to "wallet.list-active-response",
                "data" to mapOf(
                    "wallets" to walletsData
                )
            )

            rabbitTemplate.convertAndSend(exchange, "wallet.list-active-response", response)
            logger.info { "[wallet-events] Sent ${activeWallets.size} active wallets" }
        } catch (e: Exception) {
            logger.error(e) { "[wallet-events] Error handling list active request" }
        }
    }

    private fun handleBalanceSynced(message: Map<String, Any>) {
        try {
            val data = message["data"] as? Map<*, *>
            if (data == null) {
                logger.error { "[wallet-events] Missing 'data' field in balance-synced message" }
                return
            }

            val userId = (data["userId"] as? Number)?.toLong()
            if (userId == null) {
                logger.error { "[wallet-events] Missing or invalid 'userId' in balance-synced message" }
                return
            }

            val walletAddress = data["walletAddress"] as? String
            if (walletAddress == null) {
                logger.error { "[wallet-events] Missing 'walletAddress' for user $userId" }
                return
            }

            val tonBalance = data["tonBalance"] as? String
            if (tonBalance == null) {
                logger.error { "[wallet-events] Missing 'tonBalance' for user $userId" }
                return
            }

            val jettons = data["jettons"] as? List<*> ?: emptyList<Any>()

            logger.info { "[wallet-events] Balance synced for user $userId: TON=$tonBalance, Jettons=${jettons.size}" }

            // Sync TON balance
            walletService.syncAssetBalance(userId, "TON", tonBalance.toLong())

            // Sync jetton balances
            for (jetton in jettons) {
                if (jetton !is Map<*, *>) continue

                val jettonMasterAddress = jetton["jettonMasterAddress"] as? String
                val balance = jetton["balance"] as? String

                if (jettonMasterAddress != null && balance != null) {
                    walletService.syncAssetBalance(userId, jettonMasterAddress, balance.toLong())
                }
            }

            // Invalidate wallet state cache
            walletStateService.invalidateCache(userId, "balance-synced")

            logger.info { "[wallet-events] Successfully synced wallet balance for user $userId" }
        } catch (e: Exception) {
            logger.error(e) { "[wallet-events] Error handling balance sync" }
        }
    }
}
