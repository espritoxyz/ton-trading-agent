package com.agent.backend.websocket

import com.agent.backend.service.UserProvisioningService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class WebSocketAuthorizationInterceptor(
    private val provisioning: UserProvisioningService
) : ChannelInterceptor {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        if (accessor?.command == StompCommand.SUBSCRIBE) {
            val destination = accessor.destination
            val principal = accessor.user as? JwtAuthenticationToken

            if (principal == null) {
                logger.warn { "WebSocket SUBSCRIBE without authentication" }
                throw IllegalArgumentException("Not authenticated")
            }

            // Check if subscribing to /topic/notifications/{userId}
            if (destination?.startsWith("/topic/notifications/") == true) {
                val requestedUserId = destination.substring("/topic/notifications/".length).toLongOrNull()

                if (requestedUserId == null) {
                    logger.warn { "Invalid notification topic: $destination" }
                    throw IllegalArgumentException("Invalid topic format")
                }

                // Resolve authenticated user's ID
                val sub = principal.token.subject
                val email = principal.token.claims["email"] as? String
                val authenticatedUserId = provisioning.resolveOrCreate(sub, email).id!!

                if (authenticatedUserId != requestedUserId) {
                    logger.warn { "User $authenticatedUserId attempted to subscribe to notifications for user $requestedUserId" }
                    throw IllegalArgumentException("Not authorized to subscribe to this topic")
                }

                logger.info { "User $authenticatedUserId subscribed to notification topic" }
            }
        }

        return message
    }
}
