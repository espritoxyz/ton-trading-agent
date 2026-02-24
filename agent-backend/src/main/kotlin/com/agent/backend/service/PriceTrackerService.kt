package com.agent.backend.service

import com.agent.backend.db.entity.Direction
import com.agent.backend.db.entity.NotificationType
import com.agent.backend.db.entity.Order
import com.agent.backend.db.entity.PriceTracker
import com.agent.backend.db.rep.OrderRepository
import com.agent.backend.db.rep.PriceTrackerRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs

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

    fun listByUser(userId: Long): List<PriceTracker> =
        priceTrackers.findAllByUserId(userId)

    fun listUntriggeredByUser(userId: Long): List<PriceTracker> =
        priceTrackers.findAllByUserId(userId).filter { !it.triggered }

    @Transactional
    fun createTracker(userId: Long, jettonMaster: String, targetPrice: Double): PriceTracker {
        val currentPrice = stonfiAssetsCacheService.getDexUsdPrice(jettonMaster)
        val direction = when {
            currentPrice == null -> {
                logger.warn { "No current price for $jettonMaster, defaulting direction to UP" }
                Direction.UP
            }

            isGreaterOrEqual(currentPrice, targetPrice) -> Direction.DOWN
            isLessOrEqual(currentPrice, targetPrice) -> Direction.UP
            else -> {
                logger.warn { "Unreachable state for currentPrice=$currentPrice, targetPrice=$targetPrice" }
                Direction.UP
            }
        }


        val tracker = PriceTracker(
            userId = userId,
            jettonMaster = jettonMaster,
            targetPrice = targetPrice,
            direction = direction,
        )
        return priceTrackers.save(tracker)
    }

    @Transactional
    fun createOrderWithTracker(
        userId: Long,
        jettonMaster: String,
        action: String,
        amount: Double,
        targetPrice: Double,
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

        val tracker = createTracker(userId, jettonMaster, targetPrice).apply {
            orderId = saved.id
        }
        priceTrackers.save(tracker)

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
            val price = stonfiAssetsCacheService.getDexUsdPrice(t.jettonMaster)
            if (price == null) {
                logger.warn { "Price was not found for ${t.jettonMaster}" }
                continue
            }

            val triggeredNow = when (t.direction) {
                Direction.DOWN -> isLessOrEqual(price, t.targetPrice)
                Direction.UP -> isGreaterOrEqual(price, t.targetPrice)
            }


            if (triggeredNow) {
                t.triggered = true
                logger.debug { "Triggered tracker $t" }
                priceTrackers.save(t)

                 t.orderId?.let { orderId ->
                    val order = orders.findById(orderId).orElse(null)
                    if (order == null) {
                        logger.warn { "[price-tracker] Triggered tracker ${t.id} references missing order $orderId" }
                    } else if (!order.fulfilled) {
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
                                    "price" to t.targetPrice.toPlainString(),
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
                } ?: notifyUser(t.userId, t)
            }
        }
    }

    private fun Double.toPlainString(): String =
        BigDecimal.valueOf(this)
            .round(MathContext(6, RoundingMode.HALF_UP))
            .stripTrailingZeros()
            .toPlainString()

    private fun nearlyEquals(a: Double, b: Double, relTol: Double = 1e-4, absTol: Double = 1e-8): Boolean {
        val diff = abs(a - b)
        return diff <= absTol
    }

    private fun isGreaterOrEqual(a: Double, b: Double): Boolean = a > b || nearlyEquals(a, b)

    private fun isLessOrEqual(a: Double, b: Double): Boolean = a < b || nearlyEquals(a, b)

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
