package com.agent.backend.service

import com.agent.backend.db.entity.PriceTracker
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
    private val stonfiAssetsCacheService: StonfiAssetsCacheService,
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
                logger.warn { "[price-tracker] No current price for $jettonMaster, defaulting direction to 'up'" }
                "up"
            }
            isGreaterOrEqual(currentPrice, targetPrice) -> "down"
            isLessOrEqual(currentPrice, targetPrice) -> "up"
            else -> "up" // fallback, should not really happen
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
                "down" -> isLessOrEqual(price, t.targetPrice)
                "up" -> isGreaterOrEqual(price, t.targetPrice)
                else -> false
            }


            if (triggeredNow) {
                t.triggered = true
                logger.debug { "Triggered tracker $t" }
                priceTrackers.save(t)
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


    private fun notifyUser(userId: Long, tracker: PriceTracker) {
        logger.info {
            "[price-tracker] Triggered for user=$userId jetton=${tracker.jettonMaster} direction=${tracker.direction} " +
                "target=${tracker.targetPrice}"
        }
    }
}
