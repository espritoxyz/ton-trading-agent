package com.agent.backend.controller

import com.agent.backend.dto.BalanceResponse
import com.agent.backend.dto.UserInfoResponse
import com.agent.backend.dto.UserUpdateRequest
import com.agent.backend.service.UserProvisioningService
import com.agent.backend.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/user")
class UserController(
    private val userService: UserService,
    private val provisioning: UserProvisioningService
) {
    /** Resolve current local userId from JWT (creates row on first visit). */
    private fun currentUserId(auth: JwtAuthenticationToken): Long {
        val iss = auth.token.claims["iss"] as String
        val sub = auth.token.subject
        val email = auth.token.claims["email"] as? String
        return provisioning.resolveOrCreate(iss, sub, email).id!!
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
     * Balance endpoint moved here, as requested.
     * Per your instruction, we return a USD placeholder (0.0), no computations.
     * If later you want on-chain totals, you can swap the value before returning.
     */
    @GetMapping("/{userId}/balance")
    fun getBalance(
        auth: JwtAuthenticationToken,
        @PathVariable userId: Long
    ): ResponseEntity<BalanceResponse> {
        val current = currentUserId(auth)
        require(current == userId) { "forbidden" }

        return ResponseEntity.ok(BalanceResponse(userId = userId, totalUsd = 0.0))
    }
}
