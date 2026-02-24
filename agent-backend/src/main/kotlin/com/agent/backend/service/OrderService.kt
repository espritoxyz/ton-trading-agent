package com.agent.backend.service

import com.agent.backend.db.entity.Order
import com.agent.backend.db.rep.OrderRepository
import com.agent.backend.llm.AgentBlockchainAdapter
import com.agent.backend.rabbitmq.RabbitConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.*

@Service
class OrderService(
    private val orders: OrderRepository,
    private val stonfiAssetsCacheService: StonfiAssetsCacheService,
    private val stonfiPoolsCacheService: StonfiPoolsCacheService,
    private val rabbitTemplate: RabbitTemplate,
    private val walletService: WalletService,
    private val notificationEventPublisher: NotificationEventPublisher,
    private val appUtils: com.agent.backend.AppUtils,
) {
    private val logger = KotlinLogging.logger {}

    fun listUnfulfilledOrdersByUser(userId: Long): List<Order> =
        orders.findAllByUserId(userId).filter { !it.fulfilled }

    fun listAllOrdersByUser(userId: Long): List<Order> =
        orders.findAllByUserId(userId)

    fun deleteByIdForUser(userId: Long, id: Long) {
        val order = orders.findById(id).orElse(null)
        if (order == null) {
            logger.warn { "Attempt to delete missing order id=$id by userId=$userId" }
            return
        }

        if (order.userId != userId) {
            logger.warn { "Attempt to delete order id=$id that belongs to userId=${order.userId} by userId=$userId" }
            return
        }

        logger.debug { "Deleting order id=$id for userId=$userId" }
        orders.deleteById(id)
    }

    fun executeOrderSwap(order: Order) {
        val base = order.jettonMaster
        val quote = order.receivedJettonMaster

        val baseIsStable = appUtils.isStablecoin(base)
        val quoteIsStable = appUtils.isStablecoin(quote)
        if (!baseIsStable && !quoteIsStable) {
            logger.error { "Cannot execute order ${order.id}: neither $base nor $quote is a stablecoin" }
            return
        }

        val isReceiveTon = quote.equals(appUtils.tonAddress, ignoreCase = true)

        if (isReceiveTon) {
            if (order.action.equals("sell", ignoreCase = true)) {
                val tokenToTonRate = getTokenToTonInternal(base)
                if (tokenToTonRate == null) {
                    logger.warn { "Cannot execute SELL order ${order.id}: no token->TON rate for $base" }
                    return
                }

                val minimalTonAmount = (order.amount * tokenToTonRate)
                    .toBigDecimal()
                    .setScale(6, RoundingMode.HALF_UP)
                    .toDouble()

                swapTokenToTonInternal(order.userId, base, minimalTonAmount)
            } else {
                swapTonToTokenInternal(order.userId, base, order.amount)
            }
        } else {
            if (order.action.equals("sell", ignoreCase = true)) {
                swapTokenToTokenInternal(order.userId, base, quote, order.amount)
            } else {
                swapTokenToTokenInternal(order.userId, quote, base, order.amount)
            }
        }
    }


    private fun getTokenToTonInternal(jettonMaster: String): Double? =
        try {
            val poolAddress = stonfiPoolsCacheService.getBestPoolByTokenAndTon(jettonMaster)?.address
                ?: error("No pool for $jettonMaster found")

            logger.debug { "Pool address for $jettonMaster is $poolAddress" }

            val tokenUsdtPrice = stonfiAssetsCacheService.getDexUsdPrice(jettonMaster)
            val tonUsdtPrice = AgentBlockchainAdapter.getTonToUSDTStatic() ?: return null


            logger.debug { "TokenUsdtPrice=$tokenUsdtPrice, tonUsdtPrice=$tonUsdtPrice" }

            val price = tokenUsdtPrice?.let { price ->
                if (price > 0.0 && tonUsdtPrice > 0.0) {
                    (price / tonUsdtPrice).toBigDecimal().setScale(6, RoundingMode.HALF_UP).toDouble()
                } else null
            }

            logger.debug { "Calculated price $price in TON of $jettonMaster" }

            price
        } catch (e: StonfiPoolsCacheService.NoSupportedPoolException) {
            logger.warn(e) { "No supported pool for $jettonMaster; orders will not be created/executed" }
            null
        } catch (e: StonfiPoolsCacheService.LowTvlPoolException) {
            logger.warn(e) { "Low TVL pool for $jettonMaster; orders will not be created/executed" }
            null
        } catch (e: Exception) {
            logger.debug(e) { "Get token $jettonMaster to TON rate failed with exception" }
            null
        }


    private fun swapTonToTokenInternal(userId: Long, jettonMaster: String, minimalTokenAmount: Double) {
        val wallet = walletService.getUserWallet(userId)
            ?: throw IllegalStateException("User $userId has no wallet")
        val mnemonicWords =
            walletService.decryptMnemonic(wallet).split(" ").map { it.trim() }.filter { it.isNotEmpty() }

        val tokenToTonRate = getTokenToTonInternal(jettonMaster)
        val swapTonAmount = tokenToTonRate?.let {
            val slippageSafetyFactor = 1.1
            (minimalTokenAmount * it * slippageSafetyFactor)
                .toBigDecimal()
                .setScale(6, RoundingMode.HALF_UP)
                .toDouble()
        }

        val bestPool = try {
            stonfiPoolsCacheService.getBestPoolByTokenAndTon(jettonMaster)
        } catch (e: StonfiPoolsCacheService.NoSupportedPoolException) {
            logger.warn(e) { "No supported pool for $jettonMaster; skipping swapTonToTokenInternal" }
            return
        } catch (e: StonfiPoolsCacheService.LowTvlPoolException) {
            logger.warn(e) { "Low TVL pool for $jettonMaster; skipping swapTonToTokenInternal" }
            return
        }
        val poolAddress = bestPool?.address

        val data = mutableMapOf<String, Any?>(

            "messageId" to UUID.randomUUID().toString(),
            "userId" to userId,
            "walletAddress" to wallet.walletAddress,
            "jettonMaster" to jettonMaster,
            "minimalTokenAmount" to minimalTokenAmount,
            "poolAddress" to poolAddress,
            "mnemonic" to mnemonicWords,
        )
        if (swapTonAmount != null) data["swapTonAmount"] = swapTonAmount

        val payload = mapOf(
            "type" to "agent-llm.swap-ton-to-token",
            "occurredAt" to Instant.now().toString(),
            "data" to data,
        )

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "agent-llm.swap-ton-to-token", payload)
    }

    private fun swapTokenToTonInternal(userId: Long, jettonMaster: String, minimalTonAmount: Double) {

        val wallet = walletService.getUserWallet(userId)
            ?: throw IllegalStateException("User $userId has no wallet")
        val mnemonicWords =
            walletService.decryptMnemonic(wallet).split(" ").map { it.trim() }.filter { it.isNotEmpty() }

        val tokenToTonRate = getTokenToTonInternal(jettonMaster)

        val swapTokenAmountNano: Long? = tokenToTonRate?.let { rate ->
            val tokens = BigDecimal.valueOf(minimalTonAmount)
                .divide(BigDecimal.valueOf(rate), 12, RoundingMode.HALF_UP)

            val decimals = stonfiAssetsCacheService.getDecimals(jettonMaster) ?: 9
            val factor = BigDecimal.TEN.pow(decimals)

            tokens.multiply(factor)
                .setScale(0, RoundingMode.CEILING)
                .toLong()
        }

        val bestPool = try {
            stonfiPoolsCacheService.getBestPoolByTokenAndTon(jettonMaster)
        } catch (e: StonfiPoolsCacheService.NoSupportedPoolException) {
            logger.warn(e) { "No supported pool for $jettonMaster; skipping swapTokenToTonInternal" }
            return
        } catch (e: StonfiPoolsCacheService.LowTvlPoolException) {
            logger.warn(e) { "Low TVL pool for $jettonMaster; skipping swapTokenToTonInternal" }
            return
        }
        val poolAddress = bestPool?.address

        val data = mutableMapOf<String, Any?>(

            "messageId" to UUID.randomUUID().toString(),
            "userId" to userId,
            "walletAddress" to wallet.walletAddress,
            "jettonMaster" to jettonMaster,
            "minimalTonAmount" to minimalTonAmount,
            "poolAddress" to poolAddress,
            "mnemonic" to mnemonicWords,
        )
        if (swapTokenAmountNano != null) data["swapTokenAmount"] = swapTokenAmountNano

        val payload = mapOf(
            "type" to "agent-llm.swap-token-to-ton",
            "occurredAt" to Instant.now().toString(),
            "data" to data,
        )

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "agent-llm.swap-token-to-ton", payload)
    }

    private fun swapTokenToTokenInternal(
        userId: Long,
        offerJettonMaster: String,
        askJettonMaster: String,
        minimalAskTokenAmount: Double,
    ) {
        val wallet = walletService.getUserWallet(userId)
            ?: throw IllegalStateException("User $userId has no wallet")
        val mnemonicWords =
            walletService.decryptMnemonic(wallet).split(" ").map { it.trim() }.filter { it.isNotEmpty() }

        val offerTokenToTonRate = getTokenToTonInternal(offerJettonMaster)
        val askTokenToTonRate = getTokenToTonInternal(askJettonMaster)

        if (offerTokenToTonRate == null || askTokenToTonRate == null) {
            logger.warn { "No token->TON rate for $offerJettonMaster or $askJettonMaster; skipping swapTokenToTokenInternal" }
            return
        }

        // minimalAskTokenAmount * (TON per target) / (TON per offer) = required offer tokens (mid-price)
        val offerTokensHuman = BigDecimal.valueOf(minimalAskTokenAmount)
            .multiply(BigDecimal.valueOf(askTokenToTonRate))
            .divide(BigDecimal.valueOf(offerTokenToTonRate), 12, RoundingMode.HALF_UP)

        val slippageSafetyFactor = BigDecimal("1.10")
        val offerTokensWithSlippage = offerTokensHuman.multiply(slippageSafetyFactor)

        val offerDecimals = stonfiAssetsCacheService.getDecimals(offerJettonMaster) ?: 9
        val offerFactor = BigDecimal.TEN.pow(offerDecimals)
        val swapOfferTokenAmountNano = offerTokensWithSlippage
            .multiply(offerFactor)
            .setScale(0, RoundingMode.CEILING)
            .toLong()

        val data = mutableMapOf<String, Any?>(
            "messageId" to UUID.randomUUID().toString(),
            "userId" to userId,
            "walletAddress" to wallet.walletAddress,
            "offerJettonMaster" to offerJettonMaster,
            "askJettonMaster" to askJettonMaster,
            "minimalAskTokenAmount" to minimalAskTokenAmount,
            "poolAddress" to null,
            "mnemonic" to mnemonicWords,
            "swapOfferTokenAmount" to swapOfferTokenAmountNano,
        )

        val payload = mapOf(
            "type" to "agent-llm.swap-token-to-token",
            "occurredAt" to Instant.now().toString(),
            "data" to data,
        )

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "agent-llm.swap-token-to-token", payload)
    }
}

