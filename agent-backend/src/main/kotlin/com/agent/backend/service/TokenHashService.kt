package com.agent.backend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class TokenHashService(
    @Value("\${security.token-hash-secret:}") private val base64Secret: String
) {
    private val secretBytes: ByteArray

    init {
        if (base64Secret.isBlank()) {
            throw IllegalStateException("security.token-hash-secret must be set (base64-encoded key)")
        }
        secretBytes = Base64.getDecoder().decode(base64Secret)
    }

    fun hashToken(token: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secretBytes, "HmacSHA256")
        mac.init(keySpec)
        val digest = mac.doFinal(token.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest)
    }
}
