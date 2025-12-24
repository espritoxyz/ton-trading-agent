package com.agent.backend.controller

import com.agent.backend.dto.LoginRequest
import com.agent.backend.dto.ProfileResponse
import com.agent.backend.dto.TokenResponse
import com.agent.backend.dto.RegisterRequest
import com.agent.backend.dto.RegisterResponse
import com.agent.backend.service.AuthService
import com.agent.backend.service.UserProvisioningService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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

    @PostMapping("/register")
    fun register(@Valid @RequestBody body: RegisterRequest): ResponseEntity<Any> {
        logger.info { "register: ${body.email}" }
        return try {
            val resp = authService.register(body)
            ResponseEntity.status(201).body(resp)
        } catch (e: IllegalArgumentException) {
            logger.warn(e) { "Registration conflict/validation" }
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to (e.message ?: "conflict")))
        } catch (e: org.springframework.web.client.RestClientResponseException) {
            logger.error(e) { "Keycloak admin API error" }
            val status = try { e.statusCode } catch (_: Exception) { HttpStatus.BAD_GATEWAY }
            ResponseEntity.status(status).body(mapOf("message" to (e.responseBodyAsString ?: "upstream error")))
        } catch (e: jakarta.servlet.UnavailableException) {
            logger.error(e) { "Auth provider unavailable" }
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf("message" to (e.message ?: "auth provider unavailable")))
        } catch (e: Exception) {
            logger.error(e) { "Registration failed" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("message" to (e.message ?: "internal error")))
        }
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
