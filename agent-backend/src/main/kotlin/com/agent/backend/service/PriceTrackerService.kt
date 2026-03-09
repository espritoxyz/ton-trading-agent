package com.agent.backend.service

import com.agent.backend.db.entity.Direction
import com.agent.backend.db.entity.NotificationType
import com.agent.backend.db.entity.Order
import com.agent.backend.db.entity.PriceTracker
import com.agent.backend.db.rep.OrderRepository
import com.agent.backend.db.rep.PriceTrackerRepository
import com.agent.llm.tool.dto.PriceDirection
import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PriceTrackerService(
    private val priceTrackers: PriceTrackerRepository,
    private val orders: OrderRepository,
    private val stonfiAssetsCacheService: StonfiAssetsCacheService,
    private val orderService: OrderService,
    private val notificationEventPublisher: NotificationEventPublisher,
    private val notificationService: NotificationService,
    private val appUtils: com.agent.backend.AppUtils,
) {
    private val logger = KotlinLogging.logger {}
    private val absTol = 1e-12

    fun listByUser(userId: Long): List<PriceTracker> =
        priceTrackers.findAllByUserId(userId)

    fun listUntriggeredByUser(userId: Long): List<PriceTracker> =
        priceTrackers.findAllByUserId(userId).filter { !it.triggered }

    @Transactional
    fun createTracker(
        userId: Long,
        jettonMaster: String,
        targetPrice: Double,
        llmDirection: PriceDirection
    ): PriceTracker {
        val asset = stonfiAssetsCacheService.getAssetByContractAddress(jettonMaster)
        val currentPrice = asset?.dexUsdPrice

        val direction = when (llmDirection) {
            // Preset direction coming from LLM
            PriceDirection.UP, PriceDirection.DOWN -> {
                Direction.fromLlmDirection(llmDirection)
            }

            // Need to infer direction based on current price vs target
            PriceDirection.EQUAL -> {
                when {
                    currentPrice == null -> {
                        logger.warn { "[price-tracker] No current price for $jettonMaster, defaulting direction to UP" }
                        Direction.UP
                    }
                    // If price is already (almost) equal, we'll trigger tracker right after persisting it
                    currentPrice nearlyEquals targetPrice -> Direction.UP
                    currentPrice isGreater targetPrice -> Direction.DOWN
                    currentPrice isLess targetPrice -> Direction.UP
                    else -> {
                        error("Unreachable state for currentPrice=$currentPrice, targetPrice=$targetPrice")
                    }
                }
            }
        }

        val tracker = PriceTracker(
            userId = userId,
            jettonMaster = jettonMaster,
            targetPrice = targetPrice,
            direction = direction,
        )
        logger.debug { "[price-tracker] Tracker created: id=${tracker.id}" }
        val saved = priceTrackers.save(tracker)

        // If user asked to trigger when price is equal and it's already equal now, trigger immediately
        if (llmDirection == PriceDirection.EQUAL && currentPrice != null && (currentPrice nearlyEquals targetPrice)) {
            triggerPriceTracker(saved)
        }

        return saved
    }

    @Transactional
    fun createOrderWithTracker(
        userId: Long,
        jettonMaster: String,
        action: String,
        amount: Double,
        targetPrice: Double,
        llmDirection: PriceDirection,
        receivedJettonMaster: String,
    ): Order {
        // Prohibit orders where neither side is a stablecoin (TON/USDT) to avoid unsupported pairs.
        if (!appUtils.isStablecoin(jettonMaster) && !appUtils.isStablecoin(receivedJettonMaster)) {
            error("Swap token->token rejected: non-TON/non-USDT pools not supported")
        }

        val order = Order(
            userId = userId,
            jettonMaster = jettonMaster,
            action = action,
            amount = amount,
            receivedJettonMaster = receivedJettonMaster,
        )
        val saved = orders.save(order)

        val tracker = createTracker(userId, jettonMaster, targetPrice, llmDirection).apply {
            orderId = saved.id
            if (triggered) triggerOrderForTracker(this, orderId!!)
        }

        priceTrackers.save(tracker)
        logger.debug { "[price-tracker] Order created: id=${saved.id}" }

        return saved
    }

    @Transactional
    fun deleteById(id: Long) {
        logger.info { "[price-tracker] Deleting tracker id=$id" }
        priceTrackers.deleteById(id)
    }

    @Scheduled(fixedDelayString = "15000")
    @Transactional
    fun checkUntriggeredTrackers() {
        val untriggered = priceTrackers.findAllByTriggeredFalse()
        if (untriggered.isEmpty()) return

        for (t in untriggered) {
            val asset = stonfiAssetsCacheService.getAssetByContractAddress(t.jettonMaster)
            val price = asset?.dexUsdPrice
            if (price == null) {
                logger.warn { "[price-tracker] Price was not found for ${t.jettonMaster}" }
                continue
            }

            val triggeredNow = when (t.direction) {
                Direction.DOWN -> price isLess t.targetPrice
                Direction.UP -> price isGreater t.targetPrice
            }

            if (triggeredNow) {
                triggerPriceTracker(t)
            }
        }
    }
    
    private fun triggerPriceTracker(tracker: PriceTracker) {
        tracker.triggered = true
        logger.debug { "[price-tracker] Triggered tracker $tracker" }
        priceTrackers.save(tracker)

        tracker.orderId?.let { orderId ->
            triggerOrderForTracker(tracker, orderId)
        } ?: notifyUser(tracker.userId, tracker)
    }

    private fun triggerOrderForTracker(tracker: PriceTracker, orderId: Long) {
        val order = orders.findById(orderId).orElse(null)
        if (order == null) {
            logger.warn { "[price-tracker] Triggered tracker ${tracker.id} references missing order $orderId" }
            return
        }

        if (order.fulfilled) {
            return
        }

        try {
            val symbol = stonfiAssetsCacheService.getAssetByContractAddress(order.jettonMaster)?.symbol
                ?: order.jettonMaster

            executeOrderSwap(order)
            order.fulfilled = true
            orders.save(order)
            logger.info {
                "[price-tracker] Order ${order.id} fulfilled for user=${order.userId}: " +
                        "action=${order.action}, jetton=${order.jettonMaster}, amount=${order.amount}"
            }

            // Notify user that order conditions are met and swap is being initiated
            try {
                val metadata = mapOf<String, Any>(
                    "orderId" to (order.id ?: 0L),
                    "jettonMaster" to order.jettonMaster,
                    "side" to order.action,
                    "quantity" to order.amount.toPlainString(),
                    "symbol" to symbol,
                    "price" to tracker.targetPrice.toPlainString(),
                    "fillType" to "fully"
                )
                val (title, message) = notificationService.generateNotificationText(
                    NotificationType.ORDER_FILLED, metadata
                )
                notificationEventPublisher.publishNotificationEvent(
                    userId = order.userId,
                    type = "ORDER_FILLED",
                    title = title,
                    message = message,
                    metadata = metadata
                )
            } catch (e: Exception) {
                logger.warn(e) { "[price-tracker] Failed to publish ORDER_FILLED notification for order ${order.id}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "[price-tracker] Failed to execute swap for order ${order.id}" }
        }
    }

    private fun Double.toPlainString(): String =


        BigDecimal.valueOf(this)
            .round(MathContext(6, RoundingMode.HALF_UP))
            .stripTrailingZeros()
            .toPlainString()

    private infix fun Double.nearlyEquals(b: Double): Boolean {
        val diff = abs(this - b)
        return diff <= absTol
    }

    private infix fun Double.isGreater(b: Double): Boolean = this > b

    private infix fun Double.isLess(b: Double): Boolean = this < b

    private fun executeOrderSwap(order: Order) {
        orderService.executeOrderSwap(order)
    }

    private fun notifyUser(userId: Long, tracker: PriceTracker) {
        logger.info {
            "[price-tracker] Triggered for user=$userId jetton=${tracker.jettonMaster} direction=${tracker.direction} " +
                    "target=${tracker.targetPrice}"
        }
        try {
            val symbol = stonfiAssetsCacheService.getAssetByContractAddress(tracker.jettonMaster)?.symbol
                ?: tracker.jettonMaster
            val metadata = mapOf<String, Any>(
                "trackerId" to (tracker.id ?: 0L),
                "jettonMaster" to tracker.jettonMaster,
                "symbol" to symbol,
                "targetPrice" to tracker.targetPrice.toPlainString(),
                "direction" to tracker.direction.name,
            )
            val (title, message) = notificationService.generateNotificationText(
                NotificationType.TRACKER_TRIGGERED, metadata
            )
            notificationEventPublisher.publishNotificationEvent(
                userId = userId,
                type = "TRACKER_TRIGGERED",
                title = title,
                message = message,
                metadata = metadata
            )
        } catch (e: Exception) {
            logger.warn(e) { "[price-tracker] Failed to publish TRACKER_TRIGGERED notification for tracker ${tracker.id}" }
        }
    }
}
