package com.agent.backend.db.rep

import com.agent.backend.db.entity.Notification
import com.agent.backend.db.entity.NotificationType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByUser_IdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Notification>
    fun findByUser_IdAndIsReadFalse(userId: Long): List<Notification>
    fun countByUser_IdAndIsReadFalse(userId: Long): Long
    fun findByUser_IdAndTypeOrderByCreatedAtDesc(userId: Long, type: NotificationType): List<Notification>

    @Modifying
    @Transactional
    fun deleteByCreatedAtBefore(cutoffDate: Instant): Int

    @Modifying
    @Transactional
    fun deleteByUser_Id(userId: Long): Int

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.user.id = :userId AND n.isRead = false")
    fun markAllAsReadByUserId(userId: Long, readAt: Instant): Int
}
