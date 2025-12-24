package com.agent.backend.service

import com.agent.backend.dto.LoginRequest
import com.agent.backend.dto.RegisterRequest
import com.agent.backend.dto.RegisterResponse
import com.agent.backend.dto.TokenResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.security.auth.message.AuthException
import jakarta.servlet.UnavailableException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

val logger = KotlinLogging.logger {  }

@Service
class AuthService(
    @Value("\${keycloak.base-url}") private val baseUrl: String,
    @Value("\${keycloak.realm}") private val realm: String,
    @Value("\${keycloak.client-id}") private val clientId: String,
    @Value("\${keycloak.client-secret}") private val clientSecret: String,
    private val provisioning: UserProvisioningService
) {
    private val client: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build()

    fun directLogin(req: LoginRequest): TokenResponse = runCatching {
        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "password")
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("username", req.username)
            add("password", req.password)
        }

        client.post()
            .uri("/realms/$realm/protocol/openid-connect/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(TokenResponse::class.java)!!
    }.getOrElse { ex ->
        when (ex) {
            is RestClientResponseException -> {
                val status = ex.statusCode.value()
                val body = ex.responseBodyAsString
                val wwwAuth = ex.responseHeaders?.getFirst("WWW-Authenticate")
                logger.warn { "Keycloak error: status={$status}, www-auth={$wwwAuth}, body={$body}" }
                if (status in 400..499) throw AuthException("Invalid credentials")
                throw UnavailableException("Auth provider unavailable")
            }
            else -> {
                logger.error(ex) { "Auth directLogin error" }
                throw UnavailableException("Auth provider unavailable")
            }
        }
    }

    fun register(req: RegisterRequest): RegisterResponse = runCatching {
        // 1) obtain admin token via client_credentials
        val tokenForm = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "client_credentials")
            add("client_id", clientId)
            add("client_secret", clientSecret)
        }

        val adminTokenResp = try {
            client.post()
                .uri("/realms/$realm/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(tokenForm)
                .retrieve()
                .body(TokenResponse::class.java)!!
        } catch (t: RestClientResponseException) {
            val s = t.statusCode?.value() ?: t.rawStatusCode
            logger.error(t) { "Failed to obtain admin token: status=$s, body=${t.responseBodyAsString}" }
            // Treat auth failures at admin-token acquisition as service unavailable (upstream auth failure)
            if (s == 401 || s == 400) throw UnavailableException("Keycloak admin credentials invalid or not configured")
            throw UnavailableException("Failed to contact Keycloak admin token endpoint: status=$s")
        }

        val adminToken = adminTokenResp.accessToken

        // 2) create user via Admin API
        val createUserPayload = mapOf(
            "username" to req.email,
            "email" to req.email,
            "enabled" to true
        )

        try {
            client.post()
                .uri("/admin/realms/$realm/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .body(createUserPayload)
                .retrieve()
                .body(String::class.java)
        } catch (rcre: RestClientResponseException) {
            val rcreStatus = rcre.statusCode?.value() ?: rcre.rawStatusCode
            if (rcreStatus == HttpStatus.CONFLICT.value()) {
                throw IllegalArgumentException("User with this email already exists")
            }
            throw rcre
        }

        // 3) lookup user by email to get Keycloak id
        data class KcUser(val id: String?, val username: String?, val email: String?)

        val usersFound = client.get()
            .uri("/admin/realms/$realm/users?email=${req.email}")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .retrieve()
            .body(Array<KcUser>::class.java)!!

        if (usersFound.isEmpty()) throw RuntimeException("Failed to find created Keycloak user by email")
        val keycloakId = usersFound.first().id ?: throw RuntimeException("Keycloak user id missing")

        try {
            // 4) set password
            val cred = mapOf("type" to "password", "value" to req.password, "temporary" to false)
            client.put()
                .uri("/admin/realms/$realm/users/$keycloakId/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .body(cred)
                .retrieve()
                .body(String::class.java)

            // Ensure account is fully setup: mark emailVerified and clear requiredActions
            try {
                // Fetch full user representation and update fields (Keycloak expects full representation on PUT)
                val userRep = client.get()
                    .uri("/admin/realms/$realm/users/$keycloakId")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                    .retrieve()
                    .body(Map::class.java) as Map<String, Any?>

                val updated = HashMap(userRep)
                updated["emailVerified"] = true
                updated["enabled"] = true
                updated["requiredActions"] = emptyList<String>()

                client.put()
                    .uri("/admin/realms/$realm/users/$keycloakId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                    .body(updated)
                    .retrieve()
                    .body(String::class.java)
            } catch (uEx: Exception) {
                logger.warn(uEx) { "Failed to update user attributes (emailVerified/requiredActions) for $keycloakId" }
            }

            // 5) create local user
            val issuer = "$baseUrl/realms/$realm"
            val local = provisioning.createLocalForKeycloak(issuer, keycloakId, req.email)

            return RegisterResponse(userId = local.id!!, keycloakId = keycloakId, initialBalanceUsd = 0.0)
        } catch (inner: Exception) {
            logger.error(inner) { "Error after creating keycloak user, attempting cleanup" }
            // Attempt to delete created Keycloak user to avoid orphaned account
            try {
                client.delete()
                    .uri("/admin/realms/$realm/users/$keycloakId")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                    .retrieve()
                    .body(String::class.java)
            } catch (delEx: Exception) {
                logger.error(delEx) { "Failed to delete orphaned keycloak user $keycloakId" }
            }
            throw inner
        }
    }.getOrElse { ex ->
        logger.error(ex) { "Registration failed" }
        when (ex) {
            is RestClientResponseException -> {
                val status = ex.statusCode?.value() ?: ex.rawStatusCode
                if (status in 400..499) throw IllegalArgumentException("Registration failed: ${ex.responseBodyAsString}")
                throw UnavailableException("Auth provider unavailable")
            }
            is IllegalStateException -> throw ex
            else -> throw RuntimeException(ex)
        }
    }
}
