package com.agent.backend.controller

import com.agent.backend.service.UserProvisioningService
import com.agent.backend.service.WalletService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.time.Instant

private val walletLogger = KotlinLogging.logger {}

data class WalletInfoResponse(
    val walletAddress: String,
    val walletVersion: String,
    val workchain: Int,
    val createdAt: Instant,
    val lastUsedAt: Instant?,
    val isActive: Boolean
)

@RestController
@RequestMapping("/wallet")
class WalletController(
    private val provisioning: UserProvisioningService,
    private val walletService: WalletService
) {
    private fun currentUserId(auth: JwtAuthenticationToken): Long {
        val sub = auth.token.subject
        val email = auth.token.claims["email"] as? String
        return provisioning.resolveOrCreate(sub, email).id!!
    }

    @GetMapping("/info")
    fun getWalletInfo(auth: JwtAuthenticationToken): ResponseEntity<WalletInfoResponse> {
        val userId = currentUserId(auth)
        val wallet = walletService.getUserWallet(userId)
            ?: return ResponseEntity.notFound().build()

        walletLogger.debug { "Wallet info requested for user $userId" }

        val response = WalletInfoResponse(
            walletAddress = wallet.walletAddress,
            walletVersion = wallet.walletVersion,
            workchain = wallet.workchain,
            createdAt = wallet.createdAt,
            lastUsedAt = wallet.lastUsedAt,
            isActive = wallet.isActive
        )

        return ResponseEntity.ok(response)
    }
}
