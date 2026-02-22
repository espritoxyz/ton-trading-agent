package com.agent.backend.controller

import com.agent.backend.dto.NotificationResponse
import com.agent.backend.dto.PageResponse
import com.agent.backend.dto.UnreadCountResponse
import com.agent.backend.service.NotificationService
import com.agent.backend.service.UserProvisioningService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val provisioning: UserProvisioningService,
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private fun currentUserId(auth: JwtAuthenticationToken): Long {
        val sub = auth.token.subject
        val email = auth.token.claims["email"] as? String
        return provisioning.resolveOrCreate(sub, email).id!!
    }

    @GetMapping
    fun getNotifications(
        auth: JwtAuthenticationToken,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "false") unread: Boolean
    ): ResponseEntity<PageResponse<NotificationResponse>> {
        val userId = currentUserId(auth)

        return if (unread) {
            val notifications = notificationService.getUnreadNotifications(userId)
            ResponseEntity.ok(PageResponse.from(notifications) { notificationService.toResponse(it) })
        } else {
            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            val notifications = notificationService.getUserNotifications(userId, pageable)
            val response = PageResponse.from(notifications) { notificationService.toResponse(it) }
            ResponseEntity.ok(response)
        }
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(auth: JwtAuthenticationToken): ResponseEntity<UnreadCountResponse> {
        val userId = currentUserId(auth)
        val count = notificationService.getUnreadCount(userId)
        return ResponseEntity.ok(UnreadCountResponse(count))
    }

    @PatchMapping("/{id}/read")
    fun markAsRead(
        auth: JwtAuthenticationToken,
        @PathVariable id: Long
    ): ResponseEntity<NotificationResponse> {
        val userId = currentUserId(auth)

        return try {
            val notification = notificationService.markAsRead(id, userId)
            ResponseEntity.ok(notificationService.toResponse(notification))
        } catch (_: NoSuchElementException) {
            logger.warn { "Notification not found: $id" }
            ResponseEntity.notFound().build()
        } catch (_: IllegalArgumentException) {
            logger.warn { "Unauthorized access to notification $id by user $userId" }
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    @PatchMapping("/mark-all-read")
    fun markAllAsRead(auth: JwtAuthenticationToken): ResponseEntity<Map<String, Int>> {
        val userId = currentUserId(auth)
        val updatedCount = notificationService.markAllAsRead(userId)
        logger.info { "Marked $updatedCount notifications as read for user $userId" }
        return ResponseEntity.ok(mapOf("updatedCount" to updatedCount))
    }

    @DeleteMapping("/{id}")
    fun deleteNotification(
        auth: JwtAuthenticationToken,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        val userId = currentUserId(auth)

        return try {
            notificationService.deleteNotification(id, userId)
            ResponseEntity.noContent().build()
        } catch (_: NoSuchElementException) {
            logger.warn { "Notification not found: $id" }
            ResponseEntity.notFound().build()
        } catch (_: IllegalArgumentException) {
            logger.warn { "Unauthorized deletion of notification $id by user $userId" }
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    @DeleteMapping
    fun deleteAllNotifications(auth: JwtAuthenticationToken): ResponseEntity<Map<String, Int>> {
        val userId = currentUserId(auth)
        val deletedCount = notificationService.deleteAllNotifications(userId)
        logger.info { "Deleted $deletedCount notifications for user $userId" }
        return ResponseEntity.ok(mapOf("deletedCount" to deletedCount))
    }
}
