package com.agent.backend.db.rep

import com.agent.backend.db.entity.NewsletterStatus
import com.agent.backend.db.entity.NewsletterSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional

@Repository
interface NewsletterSubscriptionRepository : JpaRepository<NewsletterSubscription, Long> {
    fun findByEmail(email: String): Optional<NewsletterSubscription>
    fun findByUnsubscribeToken(token: String): Optional<NewsletterSubscription>
    fun findByVerificationToken(token: String): Optional<NewsletterSubscription>
    fun findAllByStatus(status: NewsletterStatus): List<NewsletterSubscription>

    @Modifying
    @Query("DELETE FROM NewsletterSubscription s WHERE s.status = :status AND s.subscribedAt < :cutoff")
    fun deleteStalePending(@Param("status") status: NewsletterStatus, @Param("cutoff") cutoff: Instant): Int
}
