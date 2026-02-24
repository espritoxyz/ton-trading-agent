package com.agent.backend

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Application-level helpers that depend on configuration.
 */
@Component
class AppUtils(
    @Value("\${addressbook.ton}")
    val tonAddress: String,
    @Value("\${addressbook.usdt}")
    val usdtAddress: String,
) {


    /**
     * Determine if the given address is one of the configured stablecoins
     * (currently TON or USDT) as defined in application.yaml under `addressbook.*`.
     */
    fun isStablecoin(address: String): Boolean {
        val raw = address.trim()
        if (raw.isEmpty()) return false

        return raw.equals(tonAddress.trim(), ignoreCase = true) ||
            raw.equals(usdtAddress.trim(), ignoreCase = true)
    }
}

