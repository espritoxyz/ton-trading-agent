package com.agent.backend.controller

import com.agent.backend.JwtUtils.parseClaims
import com.agent.backend.db.rep.AgentUserRepository
import com.agent.backend.dto.LoginRequest
import com.agent.backend.dto.ProfileResponse
import com.agent.backend.dto.RegisterRequest
import com.agent.backend.dto.ResendVerificationRequest
import com.agent.backend.dto.ResendVerificationResponse
import com.agent.backend.dto.VerifyEmailRequest
import com.agent.backend.dto.VerifyEmailResponse
import com.agent.backend.security.EncryptionService
import com.agent.backend.db.entity.ConfirmationIssuer
import com.agent.backend.service.AuthService
import com.agent.backend.service.EmailVerificationService
import com.agent.backend.service.NewsletterService
import com.agent.backend.service.OfflineTokenService
import com.agent.backend.service.UserProvisioningService
import com.agent.backend.service.VerificationResult
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.UnavailableException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClientResponseException

val logger = KotlinLogging.logger { }

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val provisioning: UserProvisioningService,
    private val offlineTokenService: OfflineTokenService,
    private val encryptionService: EncryptionService,
    private val emailVerificationService: EmailVerificationService,
    private val newsletterService: NewsletterService,
    private val agentUserRepository: AgentUserRepository
) {
    /**
     * Direct Access Grant against Keycloak.
     * Returns access/refresh tokens; the SPA stores access_token in sessionStorage (dev) or use gateway cookies in prod.
     */
    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequest): ResponseEntity<Any> {
        logger.info { "login request: ${body.username}" }
        val tokens = try {
            authService.directLogin(body)
        } catch (e: jakarta.security.auth.message.AuthException) {
            logger.warn(e) { "Login failed: auth service returned error" }
            return ResponseEntity.status(401).body(mapOf("message" to (e.message ?: "Invalid credentials")))
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

        return ResponseEntity.ok<Any>(tokens)
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody body: RegisterRequest): ResponseEntity<Any> {
        logger.info { "register: ${body.email}" }
        return try {
            val resp = authService.register(body)
            if (body.subscribeToNewsletter) {
                try {
                    newsletterService.subscribeRegisteredUser(body.email, ConfirmationIssuer.REGISTRATION_CHECKBOX)
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to subscribe ${body.email} to newsletter during registration" }
                }
            }
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

    @PostMapping("/verify-email")
    fun verifyEmail(@Valid @RequestBody body: VerifyEmailRequest): ResponseEntity<VerifyEmailResponse> {
        logger.info { "Email verification request for token: ${body.token.take(10)}..." }

        return try {
            val result = emailVerificationService.verifyEmail(body.token)

            when (result) {
                VerificationResult.SUCCESS -> {
                    ResponseEntity.ok(VerifyEmailResponse(
                        success = true,
                        message = "Email verified successfully"
                    ))
                }
                VerificationResult.ALREADY_VERIFIED -> {
                    ResponseEntity.status(HttpStatus.CONFLICT).body(VerifyEmailResponse(
                        success = false,
                        message = "Email has already been verified"
                    ))
                }
                VerificationResult.EXPIRED -> {
                    ResponseEntity.status(HttpStatus.GONE).body(VerifyEmailResponse(
                        success = false,
                        message = "Verification link has expired. Please request a new one."
                    ))
                }
                VerificationResult.TOO_MANY_ATTEMPTS -> {
                    ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(VerifyEmailResponse(
                        success = false,
                        message = "Too many verification attempts. Please request a new verification link."
                    ))
                }
                VerificationResult.INVALID_TOKEN -> {
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(VerifyEmailResponse(
                        success = false,
                        message = "Invalid verification token"
                    ))
                }
            }
        } catch (e: IllegalArgumentException) {
            logger.warn(e) { "Invalid verification request" }
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(VerifyEmailResponse(
                success = false,
                message = e.message ?: "Invalid request"
            ))
        } catch (e: Exception) {
            logger.error(e) { "Email verification failed" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(VerifyEmailResponse(
                success = false,
                message = "Verification failed. Please try again later."
            ))
        }
    }

    @PostMapping("/resend-verification")
    fun resendVerification(
        auth: JwtAuthenticationToken?,
        @RequestBody body: ResendVerificationRequest?
    ): ResponseEntity<ResendVerificationResponse> {
        logger.info { "Resend verification request" }

        return try {
            // Get userId either from JWT (if logged in) or from email in request body
            val userId = if (auth != null) {
                val sub = auth.token.subject
                val email = auth.token.claims["email"] as? String
                val user = provisioning.resolveOrCreate(sub, email)
                user.id!!
            } else {
                val email = body?.email
                    ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        ResendVerificationResponse(
                            success = false,
                            message = "Email is required when not authenticated"
                        )
                    )

                val user = agentUserRepository.findByEmail(email).orElse(null)
                    ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ResendVerificationResponse(
                            success = false,
                            message = "No account found with this email address"
                        )
                    )

                user.id!!
            }

            val success = emailVerificationService.resendVerificationEmail(userId)

            if (success) {
                ResponseEntity.ok(ResendVerificationResponse(
                    success = true,
                    message = "Verification email sent successfully. Please check your inbox."
                ))
            } else {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ResendVerificationResponse(
                        success = false,
                        message = "Failed to send verification email. Please try again later."
                    )
                )
            }
        } catch (e: IllegalStateException) {
            logger.warn(e) { "Resend verification failed due to state error" }
            ResponseEntity.status(HttpStatus.CONFLICT).body(ResendVerificationResponse(
                success = false,
                message = e.message ?: "Cannot resend verification email"
            ))
        } catch (e: IllegalArgumentException) {
            logger.warn(e) { "Invalid resend request" }
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResendVerificationResponse(
                success = false,
                message = e.message ?: "Invalid request"
            ))
        } catch (e: Exception) {
            logger.error(e) { "Resend verification failed" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResendVerificationResponse(
                success = false,
                message = "Failed to resend verification email. Please try again later."
            ))
        }
    }
}
