package com.agent.backend.controller

import com.agent.backend.dto.NewsletterBroadcastRequest
import com.agent.backend.dto.NewsletterBroadcastResponse
import com.agent.backend.dto.NewsletterSubscribeRequest
import com.agent.backend.dto.NewsletterSubscribeResponse
import com.agent.backend.dto.NewsletterUnsubscribeRequest
import com.agent.backend.dto.NewsletterUnsubscribeResponse
import com.agent.backend.service.NewsletterService
import com.agent.backend.service.SubscribeResult
import com.agent.backend.service.UnsubscribeResult
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/newsletter")
class NewsletterController(
    private val newsletterService: NewsletterService
) {
    private val logger = KotlinLogging.logger {}

    @PostMapping("/subscribe")
    fun subscribe(@Valid @RequestBody body: NewsletterSubscribeRequest): ResponseEntity<NewsletterSubscribeResponse> {
        logger.info { "Newsletter subscribe request for: ${body.email}" }
        return when (newsletterService.subscribe(body.email)) {
            SubscribeResult.SUBSCRIBED -> ResponseEntity.ok(
                NewsletterSubscribeResponse(subscribed = true, message = "You're subscribed! We'll keep you in the loop.")
            )
            SubscribeResult.ALREADY_SUBSCRIBED -> ResponseEntity.ok(
                NewsletterSubscribeResponse(subscribed = true, message = "You're already subscribed.")
            )
        }
    }

    @PostMapping("/unsubscribe")
    fun unsubscribeByEmail(@Valid @RequestBody body: NewsletterUnsubscribeRequest): ResponseEntity<NewsletterUnsubscribeResponse> {
        logger.info { "Newsletter unsubscribe request for: ${body.email}" }
        return when (newsletterService.unsubscribeByEmail(body.email)) {
            UnsubscribeResult.UNSUBSCRIBED -> ResponseEntity.ok(
                NewsletterUnsubscribeResponse(unsubscribed = true, message = "You have been unsubscribed.")
            )
            UnsubscribeResult.ALREADY_UNSUBSCRIBED -> ResponseEntity.ok(
                NewsletterUnsubscribeResponse(unsubscribed = true, message = "You were already unsubscribed.")
            )
            UnsubscribeResult.NOT_FOUND -> ResponseEntity.ok(
                // Intentionally vague to avoid email enumeration
                NewsletterUnsubscribeResponse(unsubscribed = true, message = "If that email was subscribed, it is now unsubscribed.")
            )
        }
    }

    @GetMapping("/unsubscribe/{token}")
    fun unsubscribeByToken(@PathVariable token: String): ResponseEntity<NewsletterUnsubscribeResponse> {
        logger.info { "Newsletter unsubscribe via token" }
        return when (newsletterService.unsubscribeByToken(token)) {
            UnsubscribeResult.UNSUBSCRIBED -> ResponseEntity.ok(
                NewsletterUnsubscribeResponse(unsubscribed = true, message = "You have been unsubscribed.")
            )
            UnsubscribeResult.ALREADY_UNSUBSCRIBED -> ResponseEntity.ok(
                NewsletterUnsubscribeResponse(unsubscribed = true, message = "You were already unsubscribed.")
            )
            UnsubscribeResult.NOT_FOUND -> ResponseEntity.badRequest().body(
                NewsletterUnsubscribeResponse(unsubscribed = false, message = "Invalid unsubscribe link.")
            )
        }
    }

    @PostMapping("/admin/preview", produces = ["text/html;charset=UTF-8"])
    fun previewNewsletter(
        auth: JwtAuthenticationToken?,
        @Valid @RequestBody body: NewsletterBroadcastRequest
    ): ResponseEntity<String> {
        if (auth == null) return ResponseEntity.status(401).build()

        @Suppress("UNCHECKED_CAST")
        val roles = (auth.token.claims["realm_access"] as? Map<String, Any>)
            ?.get("roles") as? List<String> ?: emptyList()

        if ("ADMIN" !in roles) return ResponseEntity.status(403).build()

        val html = newsletterService.renderPreview(body.subject, body.htmlContent)
        return ResponseEntity.ok(html)
    }

    @PostMapping("/admin/send")
    fun sendNewsletter(
        auth: JwtAuthenticationToken?,
        @Valid @RequestBody body: NewsletterBroadcastRequest
    ): ResponseEntity<NewsletterBroadcastResponse> {
        if (auth == null) return ResponseEntity.status(401).build()

        @Suppress("UNCHECKED_CAST")
        val roles = (auth.token.claims["realm_access"] as? Map<String, Any>)
            ?.get("roles") as? List<String> ?: emptyList()

        if ("ADMIN" !in roles) {
            logger.warn { "Forbidden newsletter broadcast attempt by subject=${auth.token.subject}" }
            return ResponseEntity.status(403).build()
        }

        logger.info { "Newsletter broadcast initiated by subject=${auth.token.subject}: \"${body.subject}\"" }
        val result = newsletterService.broadcast(body.subject, body.htmlContent)
        return ResponseEntity.ok(result)
    }
}
