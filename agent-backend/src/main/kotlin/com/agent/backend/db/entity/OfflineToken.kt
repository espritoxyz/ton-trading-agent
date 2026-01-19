package com.agent.backend.db.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "offline_tokens")
class OfflineToken(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "offline_token_id_seq")
    @SequenceGenerator(name = "offline_token_id_seq", sequenceName = "offline_token_id_seq", allocationSize = 1)
    var id: Long? = null,

    @Column(nullable = false)
    var userId: Long,

    // оставляем зашифрованный токен на время миграции, но делаем nullable — в дальнейшем можно удалить
    @Column(length = 4096)
    var refreshToken: String?,

    // дополнительное поле: устойчивый хэш токена (HMAC-SHA256/base64) — используется для проверки/индексации
    @Column(length = 256)
    var tokenHash: String? = null,

    // необязательное поле, чтобы поддержать мульти-клиентную модель при необходимости
    @Column(length = 256)
    var clientId: String? = null,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),

    var lastUsedAt: Instant? = null,

    var expiresAt: Instant? = null
)
