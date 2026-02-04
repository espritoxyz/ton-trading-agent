package com.agent.backend.controller

import com.agent.backend.dto.AssetResponse
import com.agent.backend.dto.BalanceResponse
import com.agent.backend.dto.UserInfoResponse
import com.agent.backend.dto.UserUpdateRequest
import com.agent.backend.service.AssetService
import com.agent.backend.service.BalanceService
import com.agent.backend.service.UserProvisioningService
import com.agent.backend.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user")
class UserController(
    private val userService: UserService,
    private val provisioning: UserProvisioningService,
    private val balanceService: BalanceService,
    private val assetService: AssetService
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
     * Balance endpoint moved here; now returns aggregated USD balance via BalanceService.
     */
    @GetMapping("/{userId}/balance")
    fun getBalance(
        auth: JwtAuthenticationToken,
        @PathVariable userId: Long
    ): ResponseEntity<BalanceResponse> {
        val current = currentUserId(auth)
        require(current == userId) { "forbidden" }

        val bal = balanceService.getBalance(userId)
        return ResponseEntity.ok(bal)
    }

    /**
     * Get list of all user's assets with metadata.
     * Returns assets with basic info (address, amount).
     * Frontend should enrich with jetton metadata via TonAPI.
     */
    @GetMapping("/{userId}/assets")
    fun getAssets(
        auth: JwtAuthenticationToken,
        @PathVariable userId: Long
    ): ResponseEntity<List<AssetResponse>> {
        val current = currentUserId(auth)
        require(current == userId) { "forbidden" }

        val assets = assetService.list(userId)
        val responses = assets.map { asset ->
            AssetResponse(
                id = asset.id!!,
                address = asset.address,
                amountNano = asset.amountNano,
                // Frontend will fetch metadata from TonAPI
                symbol = null,
                decimals = null,
                name = null,
                imageUrl = null,
                usdValue = null
            )
        }
        return ResponseEntity.ok(responses)
    }
}
