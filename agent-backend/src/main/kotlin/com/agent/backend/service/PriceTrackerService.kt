package com.agent.backend.service

import com.agent.backend.db.entity.PriceTracker
import com.agent.backend.db.rep.PriceTrackerRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
            currentPrice > targetPrice -> "down"
            currentPrice < targetPrice -> "up"
            else -> "up" // equal, will trigger immediately on next check if treated as 'up'
        }

        val tracker = PriceTracker(
            userId = userId,
            jettonMaster = jettonMaster,
            targetPrice = targetPrice,
            direction = direction,
        )
        return priceTrackers.save(tracker)
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
                "down" -> price <= t.targetPrice
                "up" -> price >= t.targetPrice
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

    private fun notifyUser(userId: Long, tracker: PriceTracker) {
        logger.info {
            "[price-tracker] Triggered for user=$userId jetton=${tracker.jettonMaster} direction=${tracker.direction} " +
                "target=${tracker.targetPrice}"
        }
    }
}
