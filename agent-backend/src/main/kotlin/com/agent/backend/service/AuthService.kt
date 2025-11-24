package com.agent.backend.service

import com.agent.backend.dto.LoginRequest
import com.agent.backend.dto.TokenResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.security.auth.message.AuthException
import jakarta.servlet.UnavailableException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

val logger = KotlinLogging.logger {  }

@Service
class AuthService(
    @Value("\${keycloak.base-url}") private val baseUrl: String,
    @Value("\${keycloak.realm}") private val realm: String,
    @Value("\${keycloak.client-id}") private val clientId: String,
    @Value("\${keycloak.client-secret}") private val clientSecret: String
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

        return client.post()
            .uri("/realms/$realm/protocol/openid-connect/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(TokenResponse::class.java)!!
    }.getOrElse { ex ->
        when (ex) {
            is org.springframework.web.client.RestClientResponseException -> {
                val status = ex.statusCode
                val body   = ex.responseBodyAsString
                val wwwAuth = ex.responseHeaders?.getFirst("WWW-Authenticate")
                logger.warn { "Keycloak error: status={$status}, www-auth={$wwwAuth}, body={$body}" }
                if (status.is4xxClientError) throw AuthException("Invalid credentials")
                throw UnavailableException("Auth provider unavailable")
            }
            else -> {
                logger.error(ex) {}
                throw UnavailableException("Auth provider unavailable")
            }
        }

    }
}
