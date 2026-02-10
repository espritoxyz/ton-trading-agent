package com.agent.backend.controller

import com.agent.backend.JwtUtils.parseClaims
import com.agent.backend.dto.LoginRequest
import com.agent.backend.dto.ProfileResponse
import com.agent.backend.dto.RegisterRequest
import com.agent.backend.dto.TokenResponse
import com.agent.backend.security.EncryptionService
import com.agent.backend.service.AuthService
import com.agent.backend.service.OfflineTokenService
import com.agent.backend.service.UserProvisioningService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.UnavailableException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClientResponseException

val logger = KotlinLogging.logger { }

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val provisioning: UserProvisioningService,
    private val offlineTokenService: OfflineTokenService,
    private val encryptionService: EncryptionService
) {
    /**
     * Direct Access Grant against Keycloak.
     * Returns access/refresh tokens; the SPA stores access_token in sessionStorage (dev) or use gateway cookies in prod.
     */
    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequest): ResponseEntity<TokenResponse> {
        logger.info { "login request: ${body.username}" }
        val tokens = try {
            authService.directLogin(body)
        } catch (e: jakarta.security.auth.message.AuthException) {
            logger.warn(e) { "Login failed: auth service returned error" }
            return ResponseEntity.status(401).body(TokenResponse("", null, null, null, null))
        }

        var savedErr: String? = null

        try {
            val claims = parseClaims(tokens.accessToken)
            val iss = claims.issuer
            val sub = claims.subject
            val email = claims.email
            if (iss != null && sub != null) {
                val user = provisioning.resolveOrCreate(sub, email)
                if (tokens.refreshToken == null) {
                    logger.warn { "No refresh_token returned from identity provider for userId=${user.id} subject=${sub}" }
                } else {
                    try {
                        logger.debug { "Saving offline token for user=${user.id} client=null" }
                        val saved = offlineTokenService.saveForUser(user.id!!, tokens.refreshToken!!)
                        logger.info { "Stored offline token id=${saved.id} user=${saved.userId} key=${saved.encryptionKeyId}" }
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to save offline token for user=${user.id}" }
                        savedErr = (e.message ?: "save_failed").take(200)
                    }
                }
            }
        } catch (ex: Exception) {
            logger.warn(ex) { "Failed to parse token or provision user after login; attempting userinfo fallback" }
            try {
                val userInfo = authService.getUserInfoFromAccessToken(tokens.accessToken)
                if (userInfo != null) {
                    val (iss, sub, email) = userInfo
                    val user = provisioning.resolveOrCreate(sub, email)
                    tokens.refreshToken?.let { rt ->
                        try {
                            val saved = offlineTokenService.saveForUser(user.id!!, rt)
                            logger.info { "Stored offline token (userinfo fallback) id=${saved.id} user=${saved.userId}" }
                        } catch (e: Exception) {
                            logger.error(e) { "Failed to save offline token (userinfo fallback) for user=${user.id}" }
                            savedErr = (e.message ?: "save_failed").take(200)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn(e) { "userinfo fallback failed" }
                savedErr = (e.message ?: "userinfo_failed").take(200)
            }
        }

        val resp = ResponseEntity.ok()
        val finalResp = if (savedErr != null) resp.body(tokens) else resp.body(tokens)
        return finalResp
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
        } catch (e: RestClientResponseException) {
            logger.error(e) { "Keycloak admin API error" }
            val status = try {
                e.statusCode
            } catch (_: Exception) {
                HttpStatus.BAD_GATEWAY
            }
            ResponseEntity.status(status).body(mapOf("message" to (e.responseBodyAsString ?: "upstream error")))
        } catch (e: UnavailableException) {
            logger.error(e) { "Auth provider unavailable" }
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("message" to (e.message ?: "auth provider unavailable")))
        } catch (e: Exception) {
            logger.error(e) { "Registration failed" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "internal error")))
        }
    }

    /**
     * Returns identity derived from the validated JWT.
     * Also provisions (resolve-or-create) the local user row and returns the local userId.
     */
    @GetMapping("/profile")
    fun profile(auth: JwtAuthenticationToken?): ResponseEntity<ProfileResponse> {
        if (auth == null) return ResponseEntity.status(401).build()

        val sub = auth.token.subject
        val email = auth.token.claims["email"] as? String

        val user = provisioning.resolveOrCreate(sub, email)

        return ResponseEntity.ok(
            ProfileResponse(
                subject = sub,   // <- your DTO uses "subject"
                email = email,
                userId = user.id!!
            )
        )
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestHeader("Authorization", required = false) authHeader: String?,
        @RequestHeader("X-Access-Token", required = false) altHeader: String?
    ): ResponseEntity<Any> {
        val token = (authHeader?.removePrefix("Bearer ")?.trim() ?: altHeader?.trim())
        if (token.isNullOrBlank()) return ResponseEntity.status(401).build()
        try {
            val jwt = authService.decodeAccessTokenAllowExpired(token) ?: return ResponseEntity.status(401).build()
            val iss = jwt.claims["iss"] as? String ?: return ResponseEntity.status(401).build()
            val sub = jwt.subject ?: return ResponseEntity.status(401).build()

            val user = provisioning.resolveOrCreate(sub, jwt.claims["email"] as? String)

            val stored = offlineTokenService.getLatestForUser(user.id!!)
            if (stored == null || stored.refreshToken.isNullOrBlank()) return ResponseEntity.status(401).build()

            val refreshPlain = try {
                encryptionService.decrypt(stored.refreshToken!!)
            } catch (e: Exception) {
                offlineTokenService.revokeById(stored.id!!)
                return ResponseEntity.status(401).build()
            }

            val newTokens = try {
                authService.refreshWithRefreshToken(refreshPlain)
            } catch (e: Exception) {
                offlineTokenService.revokeById(stored.id!!)
                return ResponseEntity.status(401).build()
            }

            newTokens.refreshToken?.let { rt ->
                offlineTokenService.saveForUser(user.id!!, rt)
            }

            return ResponseEntity.ok(newTokens)
        } catch (ex: Exception) {
            logger.warn(ex) { "Refresh failed" }
            return ResponseEntity.status(401).build()
        }
    }

    @PostMapping("/logout")
    fun logout(@RequestHeader("Authorization", required = false) authHeader: String?): ResponseEntity<Any> {
        if (authHeader.isNullOrBlank()) return ResponseEntity.noContent().build()
        val token = authHeader.removePrefix("Bearer ").trim()
        try {
            val claims = com.agent.backend.JwtUtils.parseClaims(token)
            val iss = claims.issuer ?: return ResponseEntity.noContent().build()
            val sub = claims.subject ?: return ResponseEntity.noContent().build()

            val user = provisioning.resolveOrCreate(sub, claims.email)
            offlineTokenService.revokeAllForUser(user.id!!)

            return ResponseEntity.noContent().build()
        } catch (ex: Exception) {
            logger.warn(ex) { "Logout cleanup failed" }
            return ResponseEntity.noContent().build()
        }
    }
}
