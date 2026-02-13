package com.agent.backend.db.rep

import com.agent.backend.db.entity.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByUser_IdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Notification>
    fun findByUser_IdAndIsReadFalse(userId: Long): List<Notification>
    fun countByUser_IdAndIsReadFalse(userId: Long): Long

    @Modifying
    fun deleteByCreatedAtBefore(cutoffDate: Instant): Int

    @Modifying
    fun deleteByUser_Id(userId: Long): Int
}
