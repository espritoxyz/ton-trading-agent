package com.agent.backend.controller

import com.agent.backend.dto.LoginRequest
import com.agent.backend.dto.ProfileResponse
import com.agent.backend.dto.TokenResponse
import com.agent.backend.service.AuthService
import com.agent.backend.service.UserProvisioningService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

val logger = KotlinLogging.logger {  }

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val provisioning: UserProvisioningService
) {
    /**
     * Direct Access Grant against Keycloak.
     * Returns access/refresh tokens; the SPA stores access_token in sessionStorage (dev) or use gateway cookies in prod.
     */
    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequest): ResponseEntity<TokenResponse> {
        logger.info { body.toString() }
        val tokens = authService.directLogin(body)
        return ResponseEntity.ok(tokens)
    }

    /**
     * Returns identity derived from the validated JWT.
     * Also provisions (resolve-or-create) the local user row and returns the local userId.
     */
    @GetMapping("/profile")
    fun profile(auth: JwtAuthenticationToken?): ResponseEntity<ProfileResponse> {
        if (auth == null) return ResponseEntity.status(401).build()

        val iss = auth.token.claims["iss"] as String
        val sub = auth.token.subject
        val email = auth.token.claims["email"] as? String

        val user = provisioning.resolveOrCreate(iss, sub, email)

        return ResponseEntity.ok(
            ProfileResponse(
                subject = sub,   // <- your DTO uses "subject"
                email = email,
                userId = user.id!!
            )
        )
    }
}
