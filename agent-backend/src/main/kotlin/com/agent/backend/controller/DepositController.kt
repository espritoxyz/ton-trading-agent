package com.agent.backend.controller

import com.agent.backend.dto.DepositHistoryItem
import com.agent.backend.dto.DepositStatusResponse
import com.agent.backend.dto.InitiateDepositRequest
import com.agent.backend.dto.InitiateDepositResponse
import com.agent.backend.service.DepositService
import com.agent.backend.service.UserProvisioningService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

private val depositLogger = KotlinLogging.logger {}

@RestController
@RequestMapping("/deposit")
class DepositController(
    private val provisioning: UserProvisioningService,
    private val depositService: DepositService
) {
    private fun currentUserId(auth: JwtAuthenticationToken): Long {
        val sub = auth.token.subject
        val email = auth.token.claims["email"] as? String
        return provisioning.resolveOrCreate(sub, email).id!!
    }

    @PostMapping("/initiate")
    fun initiateDeposit(
        auth: JwtAuthenticationToken,
        @Valid @RequestBody body: InitiateDepositRequest
    ): ResponseEntity<InitiateDepositResponse> {
        val userId = currentUserId(auth)

        // Verify that the request userId matches the authenticated user
        require(body.userId == userId) { "User ID mismatch" }

        val response = depositService.initiateDeposit(userId)
        depositLogger.info { "Deposit initiated for user $userId: code=${response.code}" }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{depositRequestId}/status")
    fun getDepositStatus(
        auth: JwtAuthenticationToken,
        @PathVariable depositRequestId: Long
    ): ResponseEntity<DepositStatusResponse> {
        val userId = currentUserId(auth)
        val status = depositService.getDepositStatus(depositRequestId)
            ?: return ResponseEntity.notFound().build()

        depositLogger.debug { "Deposit status requested: depositRequestId=$depositRequestId, userId=$userId" }

        return ResponseEntity.ok(status)
    }

    @GetMapping("/user/{userId}")
    fun getDepositHistory(
        auth: JwtAuthenticationToken,
        @PathVariable userId: Long
    ): ResponseEntity<List<DepositHistoryItem>> {
        val currentUserId = currentUserId(auth)

        // Verify that the request userId matches the authenticated user
        require(userId == currentUserId) { "User ID mismatch" }

        val history = depositService.getDepositHistory(userId)
        depositLogger.debug { "Deposit history requested for user $userId, found ${history.size} records" }

        return ResponseEntity.ok(history)
    }
}
