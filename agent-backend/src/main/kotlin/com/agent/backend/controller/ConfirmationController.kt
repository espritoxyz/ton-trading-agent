package com.agent.backend.controller

import com.agent.backend.llm.ChatJobService
import com.agent.backend.service.ConfirmationService
import com.agent.backend.service.ConfirmationStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KLogger
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

private val confLogger: KLogger = KotlinLogging.logger {}

@RestController
@RequestMapping("/chat/messages/{messageId}/confirmations")
class ConfirmationController(
    private val confirmations: ConfirmationService,
    private val jobs: ChatJobService,
) {
    data class ConfirmationDto(
        val id: UUID,
        val toolName: String,
        val text: String,
        val status: ConfirmationStatus
    )

    @GetMapping
    fun list(
        auth: JwtAuthenticationToken,
        @PathVariable messageId: UUID
    ): ResponseEntity<List<ConfirmationDto>> {
        val items = confirmations.list(messageId).map { ConfirmationDto(it.id, it.toolName, it.text, it.status) }
        return ResponseEntity.ok(items)
    }

    @PostMapping("/{confirmationId}/approve")
    fun approve(
        auth: JwtAuthenticationToken,
        @PathVariable messageId: UUID,
        @PathVariable confirmationId: UUID
    ): ResponseEntity<Void> {
        confirmations.resolve(messageId, confirmationId, true)
        confLogger.debug { "Approved confirmationId=$confirmationId for messageId=$messageId" }
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
        confLogger.debug { "Declined confirmationId=$confirmationId for messageId=$messageId" }
        jobs.resumeIfReady(messageId)
        return ResponseEntity.ok().build()
    }
}
