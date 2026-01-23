package com.agent.backend.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class TokenHashService(
    @Value("\${security.tokens.token-hash-secret:}")
    private val secret: String
) {
    private val algorithm = "HmacSHA256"

    fun hashToken(token: String): String {
        val keyBytes = secret.toByteArray(Charsets.UTF_8)
        val keySpec = SecretKeySpec(keyBytes, algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(keySpec)
        val raw = mac.doFinal(token.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(raw)
    }

    fun verify(token: String, expectedHash: String): Boolean {
        return hashToken(token) == expectedHash
    }
}
