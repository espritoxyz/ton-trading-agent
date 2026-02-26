package com.agent.backend.controller

import com.agent.backend.dto.NewsletterBroadcastRequest
import com.agent.backend.dto.NewsletterBroadcastResponse
import com.agent.backend.dto.NewsletterConfirmResponse
import com.agent.backend.dto.NewsletterResendRequest
import com.agent.backend.dto.NewsletterResendResponse
import com.agent.backend.dto.NewsletterSubscribeRequest
import com.agent.backend.dto.NewsletterSubscribeResponse
import com.agent.backend.dto.NewsletterSubscriptionStatusResponse
import com.agent.backend.dto.NewsletterSubscriptionUpdateRequest
import com.agent.backend.dto.NewsletterUnsubscribeRequest
import com.agent.backend.dto.NewsletterUnsubscribeResponse
import com.agent.backend.service.ConfirmResult
import com.agent.backend.service.NewsletterService
import com.agent.backend.service.ResendResult
import com.agent.backend.service.SubscribeResult
import com.agent.backend.service.UnsubscribeResult
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
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
                NewsletterSubscribeResponse(
                    subscribed = true,
                    pending = true,
                    message = "Almost there! We've sent a confirmation email. Please check your inbox and click the link to complete your subscription."
                )
            )
            SubscribeResult.PENDING_VERIFICATION -> ResponseEntity.ok(
                NewsletterSubscribeResponse(
                    subscribed = false,
                    pending = true,
                    message = "We've already sent you a confirmation email. Please check your inbox (and spam folder) and click the link."
                )
            )
            SubscribeResult.ALREADY_SUBSCRIBED -> ResponseEntity.ok(
                NewsletterSubscribeResponse(subscribed = true, pending = false, message = "You're already subscribed.")
            )
        }
    }

    @GetMapping("/confirm/{token}")
    fun confirm(@PathVariable token: String): ResponseEntity<NewsletterConfirmResponse> {
        logger.info { "Newsletter confirmation attempt via token" }
        return when (newsletterService.confirmSubscription(token)) {
            ConfirmResult.CONFIRMED -> ResponseEntity.ok(
                NewsletterConfirmResponse(confirmed = true, message = "You're subscribed! You'll start receiving our updates soon.")
            )
            ConfirmResult.ALREADY_CONFIRMED -> ResponseEntity.ok(
                NewsletterConfirmResponse(confirmed = true, message = "You were already subscribed.")
            )
            ConfirmResult.EXPIRED -> ResponseEntity.ok(
                NewsletterConfirmResponse(confirmed = false, message = "This confirmation link has expired. Please subscribe again to receive a new link.")
            )
            ConfirmResult.INVALID_TOKEN -> ResponseEntity.ok(
                // Intentionally vague
                NewsletterConfirmResponse(confirmed = false, message = "This confirmation link is invalid or has already been used.")
            )
        }
    }

    @PostMapping("/resend-verification")
    fun resendVerification(@Valid @RequestBody body: NewsletterResendRequest): ResponseEntity<NewsletterResendResponse> {
        logger.info { "Newsletter resend verification request" }
        return when (newsletterService.resendVerification(body.email)) {
            ResendResult.SENT -> ResponseEntity.ok(
                NewsletterResendResponse(sent = true, message = "Confirmation email resent. Please check your inbox.")
            )
            ResendResult.TOO_SOON -> ResponseEntity.ok(
                NewsletterResendResponse(sent = false, message = "Please wait a few minutes before requesting another email.")
            )
            ResendResult.MAX_RESENDS_REACHED -> ResponseEntity.ok(
                NewsletterResendResponse(sent = false, message = "Maximum resend limit reached. Please subscribe again with a fresh request.")
            )
            ResendResult.NOT_FOUND, ResendResult.ALREADY_CONFIRMED -> ResponseEntity.ok(
                // Intentionally vague to avoid email enumeration
                NewsletterResendResponse(sent = true, message = "If that email has a pending subscription, we've resent the confirmation.")
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

    /** Get newsletter subscription status for the authenticated user. */
    @GetMapping("/subscription")
    fun getMySubscription(auth: JwtAuthenticationToken?): ResponseEntity<NewsletterSubscriptionStatusResponse> {
        if (auth == null) return ResponseEntity.status(401).build()
        val userEmail = auth.token.claims["email"] as? String
            ?: return ResponseEntity.status(400).body(NewsletterSubscriptionStatusResponse(subscribed = false))
        val subscribed = newsletterService.getNewsletterStatus(userEmail)
        return ResponseEntity.ok(NewsletterSubscriptionStatusResponse(subscribed = subscribed))
    }

    /** Update newsletter subscription for the authenticated user (no double opt-in for registered users). */
    @PutMapping("/subscription")
    fun updateMySubscription(
        auth: JwtAuthenticationToken?,
        @RequestBody body: NewsletterSubscriptionUpdateRequest
    ): ResponseEntity<NewsletterSubscriptionStatusResponse> {
        if (auth == null) return ResponseEntity.status(401).build()
        val userEmail = auth.token.claims["email"] as? String
            ?: return ResponseEntity.status(400).body(NewsletterSubscriptionStatusResponse(subscribed = false))
        newsletterService.setNewsletterSubscription(userEmail, body.subscribed)
        logger.info { "User ${auth.token.subject} set newsletter subscription to ${body.subscribed}" }
        return ResponseEntity.ok(NewsletterSubscriptionStatusResponse(subscribed = body.subscribed))
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
