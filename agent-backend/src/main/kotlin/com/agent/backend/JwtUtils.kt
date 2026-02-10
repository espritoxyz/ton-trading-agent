package com.agent.backend

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*

object JwtUtils {
    private val mapper = jacksonObjectMapper()

    data class Claims(val issuer: String?, val subject: String?, val email: String?)

    fun parseClaims(jwt: String): Claims {
        val parts = jwt.split(".")
        require(parts.size >= 2) { "Invalid JWT" }
        val payload = String(Base64.getUrlDecoder().decode(parts[1]))
        val tree = mapper.readTree(payload)
        val issuer = tree.get("iss")?.asText()
        val subject = tree.get("sub")?.asText() ?: tree.get("subject")?.asText()
        val email = tree.get("email")?.asText()
        return Claims(issuer, subject, email)
    }
}
