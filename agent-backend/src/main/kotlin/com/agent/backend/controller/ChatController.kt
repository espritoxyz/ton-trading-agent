package com.agent.backend.controller

import com.agent.backend.dto.ChatMessageRequest
import com.agent.backend.dto.ChatMessageResponse
import com.agent.backend.dto.ChatMessageStatusResponse
import com.agent.backend.llm.ChatJobService
import com.agent.backend.service.UserProvisioningService
import jakarta.validation.Valid
import java.util.*
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/chat")
class ChatController(
    private val provisioning: UserProvisioningService,
    private val chatJobService: ChatJobService
) {
    /** Resolve (or create) local user and return its id. */
    private fun currentUserId(auth: JwtAuthenticationToken): Long {
        val iss = auth.token.claims["iss"] as String
        val sub = auth.token.subject
        val email = auth.token.claims["email"] as? String
        return provisioning.resolveOrCreate(iss, sub, email).id!!
    }

    /**
     * POST /chat/message
     * For now we immediately return a fixed reply "my life be like ooh aah"
     * and mark the message as completed.
     *
     * In the future:
     *  - return status="queued" with no reply
     *  - push work to a queue
     *  - let the client poll GET /chat/messages/{messageId}, or use SSE/WebSocket
     */
    @PostMapping("/message")
    suspend fun postMessage(
        auth: JwtAuthenticationToken,
        @Valid @RequestBody body: ChatMessageRequest
    ): ResponseEntity<ChatMessageResponse> {
        val userId = currentUserId(auth)
        val resp = chatJobService.submit(userId, body)
        return ResponseEntity.ok(resp)
    }

    /**
     * GET /chat/messages/{messageId}
     * Placeholder polling endpoint. For now it always returns the fixed reply as completed.
     * Later you can load real status from a DB/cache.
     */
    @GetMapping("/messages/{messageId}")
    fun getMessageStatus(
        auth: JwtAuthenticationToken,
        @PathVariable messageId: UUID
    ): ResponseEntity<ChatMessageStatusResponse> {
        val userId = currentUserId(auth)
        val resp = chatJobService.status(messageId, userId)
        return ResponseEntity.ok(resp)
    }
}
