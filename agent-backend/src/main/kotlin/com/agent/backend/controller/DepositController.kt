package com.agent.backend.controller

import com.agent.backend.dto.InitiateDepositRequest
import com.agent.backend.dto.SimpleDepositResponse
import com.agent.backend.service.DepositSessionService
import com.agent.backend.service.UserProvisioningService
import com.agent.backend.service.WalletService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

private val depositLogger = KotlinLogging.logger {}

@RestController
@RequestMapping("/deposit")
class DepositController(
    private val provisioning: UserProvisioningService,
    private val walletService: WalletService,
    private val sessionService: DepositSessionService,
    private val rabbitTemplate: RabbitTemplate
) {
    private val exchange = "app.events"

    private fun currentUserId(auth: JwtAuthenticationToken): Long {
        val sub = auth.token.subject
        val email = auth.token.claims["email"] as? String
        return provisioning.resolveOrCreate(sub, email).id!!
    }

    @PostMapping("/initiate")
    fun initiateDeposit(
        auth: JwtAuthenticationToken,
        @Valid @RequestBody body: InitiateDepositRequest
    ): ResponseEntity<SimpleDepositResponse> {
        val userId = currentUserId(auth)

        // Verify that the request userId matches the authenticated user
        require(body.userId == userId) { "User ID mismatch" }

        // Get user's burner wallet
        val wallet = walletService.getUserWallet(userId)
            ?: return ResponseEntity.status(404).build()

        // Start deposit session (24h TTL)
        val session = sessionService.startSession(userId, wallet.walletAddress)

        depositLogger.info { "Deposit session started for user $userId: wallet=${wallet.walletAddress}, expiresAt=${session.expiresAt}" }

        // Publish event to recipe-processor to start monitoring
        val event = mapOf(
            "type" to "deposit.session-started",
            "occurredAt" to session.startedAt.toString(),
            "data" to mapOf(
                "userId" to userId,
                "walletAddress" to wallet.walletAddress,
                "expiresAt" to session.expiresAt.toString()
            )
        )

        rabbitTemplate.convertAndSend(exchange, "deposit.session-started", event)
        depositLogger.debug { "Published deposit.session-started event for user $userId" }

        val response = SimpleDepositResponse(
            walletAddress = wallet.walletAddress,
            expiresAt = session.expiresAt,
            message = "Send TON or Jettons to this address. Transactions will be detected automatically within 24 hours."
        )

        return ResponseEntity.ok(response)
    }
}
