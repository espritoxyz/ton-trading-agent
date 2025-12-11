package com.agent.backend.controller

import com.agent.backend.service.ConfirmationService
import com.agent.backend.service.ConfirmationStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/chat/messages/{messageId}/confirmations")
class ConfirmationController(
    private val confirmations: ConfirmationService,
    private val jobs: com.agent.backend.llm.ChatJobService,
) {
    data class ConfirmationDto(
        val id: UUID,
        val text: String,
        val status: ConfirmationStatus
    )

    @GetMapping
    fun list(
        auth: JwtAuthenticationToken,
        @PathVariable messageId: UUID
    ): ResponseEntity<List<ConfirmationDto>> {
        val items = confirmations.list(messageId).map { ConfirmationDto(it.id, it.text, it.status) }
        return ResponseEntity.ok(items)
    }

    @PostMapping("/{confirmationId}/approve")
    fun approve(
        auth: JwtAuthenticationToken,
        @PathVariable messageId: UUID,
        @PathVariable confirmationId: UUID
    ): ResponseEntity<Void> {
        confirmations.resolve(messageId, confirmationId, true)
        jobs.resumeIfReady(messageId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{confirmationId}/decline")
    fun decline(
        auth: JwtAuthenticationToken,
        @PathVariable messageId: UUID,
        @PathVariable confirmationId: UUID
    ): ResponseEntity<Void> {
        confirmations.resolve(messageId, confirmationId, false)
        jobs.resumeIfReady(messageId)
        return ResponseEntity.ok().build()
    }
}
