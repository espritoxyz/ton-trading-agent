package com.agent.backend.websocket

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class WebSocketAuthInterceptor(
    private val jwtDecoder: JwtDecoder
) : ChannelInterceptor {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        if (accessor?.command == StompCommand.CONNECT) {
            val authorization = accessor.getFirstNativeHeader("Authorization")

            if (authorization == null || !authorization.startsWith("Bearer ")) {
                logger.warn { "WebSocket CONNECT without valid Authorization header" }
                throw IllegalArgumentException("Missing or invalid Authorization header")
            }

            try {
                val token = authorization.substring(7)
                val jwt = jwtDecoder.decode(token)
                val authentication = JwtAuthenticationToken(jwt)

                // Set authentication in the accessor user
                accessor.user = authentication

                // Also set in SecurityContext for consistency
                SecurityContextHolder.getContext().authentication = authentication

                logger.info { "WebSocket authenticated for user: ${jwt.subject}" }
            } catch (e: Exception) {
                logger.warn(e) { "WebSocket JWT validation failed" }
                throw IllegalArgumentException("Invalid JWT token", e)
            }
        }

        return message
    }
}
