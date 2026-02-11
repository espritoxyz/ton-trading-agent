package com.agent.backend.service

import com.agent.backend.db.entity.Direction
import com.agent.backend.db.entity.Order
import com.agent.backend.db.entity.PriceTracker

import com.agent.backend.db.rep.OrderRepository
import com.agent.backend.db.rep.PriceTrackerRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Service
class PriceTrackerService(
    private val priceTrackers: PriceTrackerRepository,
    private val orders: OrderRepository,
    private val stonfiAssetsCacheService: StonfiAssetsCacheService,
    private val orderService: OrderService,
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
                logger.warn { "[price-tracker] No current price for $jettonMaster, defaulting direction to UP" }
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
    ): Order {
        val order = Order(
            userId = userId,
            jettonMaster = jettonMaster,
            action = action,
            amount = amount,
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

                // If this tracker is linked to an order, execute the corresponding swap automatically.
                t.orderId?.let { orderId ->
                    val order = orders.findById(orderId).orElse(null)
                    if (order == null) {
                        logger.warn { "[price-tracker] Triggered tracker ${t.id} references missing order $orderId" }
                    } else if (!order.fulfilled) {
                        try {
                            executeOrderSwap(order)
                            order.fulfilled = true
                            orders.save(order)
                            logger.info {
                                "[price-tracker] Order ${order.id} fulfilled for user=${order.userId}: " +
                                    "action=${order.action}, jetton=${order.jettonMaster}, amount=${order.amount}"
                            }

                        } catch (e: Exception) {
                            logger.error(e) { "[price-tracker] Failed to execute swap for order ${order.id}" }
                        }
                    }
                }
                notifyUser(t.userId, t)
            }
        }
    }

    private fun nearlyEquals(a: Double, b: Double, relTol: Double = 1e-4, absTol: Double = 1e-8): Boolean {
        val diff = abs(a - b)
        val scale = max(1.0, min(abs(a), abs(b)))
        return diff <= max(absTol, relTol * scale)
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
    }
}
