package com.agent.backend.db.rep

import com.agent.backend.db.entity.NewsletterSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface NewsletterSubscriptionRepository : JpaRepository<NewsletterSubscription, Long> {
    fun findByEmail(email: String): Optional<NewsletterSubscription>
    fun findByUnsubscribeToken(token: String): Optional<NewsletterSubscription>
    fun findAllByActive(active: Boolean): List<NewsletterSubscription>
}
