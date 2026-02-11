package com.agent.backend.controller

import com.agent.backend.dto.UserInfoResponse
import com.agent.backend.dto.UserUpdateRequest
import com.agent.backend.dto.WalletStateResponse
import com.agent.backend.service.UserProvisioningService
import com.agent.backend.service.UserService
import com.agent.backend.service.WalletStateService
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user")
class UserController(
    private val userService: UserService,
    private val provisioning: UserProvisioningService,
    private val walletStateService: WalletStateService
) {
    /** Resolve current local userId from JWT (creates row on first visit). */
    private fun currentUserId(auth: JwtAuthenticationToken): Long {
        val sub = auth.token.subject
        val email = auth.token.claims["email"] as? String
        return provisioning.resolveOrCreate(sub, email).id!!
    }

    /** Convenience: fetch “me” without knowing the numeric id. */
    @GetMapping("/me")
    fun me(auth: JwtAuthenticationToken): ResponseEntity<UserInfoResponse> {
        val uid = currentUserId(auth)
        val principalSub = auth.token.subject
        val principalEmail = auth.token.claims["email"] as? String
        return ResponseEntity.ok(
            UserInfoResponse(userId = uid, subject = principalSub, email = principalEmail)
        )
    }

    /** Fetch user info by user_id (must be the current user). */
    @GetMapping("/{userId}")
    fun getUser(
        auth: JwtAuthenticationToken,
        @PathVariable userId: Long
    ): ResponseEntity<UserInfoResponse> {
        val current = currentUserId(auth)
        require(current == userId) { "forbidden" } // tighten or extend with admin role as needed

        val u = userService.getByIdOrThrow(userId)
        return ResponseEntity.ok(
            UserInfoResponse(userId = u.id!!, subject = auth.token.subject, email = u.email)
        )
    }

    /** Update user info (currently: email). Ownership enforced. */
    @PatchMapping("/{userId}")
    fun updateUser(
        auth: JwtAuthenticationToken,
        @PathVariable userId: Long,
        @RequestBody body: UserUpdateRequest
    ): ResponseEntity<UserInfoResponse> {
        val current = currentUserId(auth)
        require(current == userId) { "forbidden" }

        val saved = userService.updateEmail(userId, body.email)
        return ResponseEntity.ok(
            UserInfoResponse(userId = saved.id!!, subject = auth.token.subject, email = saved.email)
        )
    }

    /**
     * Get unified wallet state (balance + assets + transactions) with caching.
     *
     * This endpoint replaces separate calls to /balance, /assets, and /wallet/transactions.
     * Includes intelligent caching (8s TTL) with event-driven invalidation.
     *
     * @param userId User ID
     * @param transactionsLimit Maximum number of transactions to return (default: 20)
     * @return Complete wallet state with metadata
     */
    @GetMapping("/{userId}/wallet-state")
    fun getWalletState(
        auth: JwtAuthenticationToken,
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "20") transactionsLimit: Int
    ): ResponseEntity<WalletStateResponse> = runBlocking {
        val current = currentUserId(auth)
        require(current == userId) { "forbidden" }

        val state = walletStateService.getWalletState(userId, transactionsLimit)
        ResponseEntity.ok(state)
    }
}
