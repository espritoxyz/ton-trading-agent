package com.agent.backend

import com.agent.backend.db.entity.OfflineToken
import com.agent.backend.db.rep.OfflineTokenRepository
import com.agent.backend.dto.TokenResponse
import com.agent.backend.security.EncryptionService
import com.agent.backend.security.TokenHashService
import com.agent.backend.service.AuthService
import com.agent.backend.service.OfflineTokenService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class OfflineTokenRefreshTest {

    @Mock
    private lateinit var repo: OfflineTokenRepository

    @Mock
    private lateinit var encryptionService: EncryptionService

    @Mock
    private lateinit var tokenHashService: TokenHashService

    @Mock
    private lateinit var authService: AuthService

    private lateinit var svc: OfflineTokenService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        svc = OfflineTokenService(repo, tokenHashService, encryptionService, authService)
    }

    @Test
    fun testRefreshFlow() {
        val userId = 555555L
        val clientId = "web"
        val oldRefresh = "old-refresh-token-xyz"
        val encryptedOld = "enc-old-xyz"
        val oldHash = "h-old"

        // Mocks for initial saveForUser
        Mockito.`when`(tokenHashService.hashToken(oldRefresh)).thenReturn(oldHash)
        Mockito.`when`(encryptionService.encrypt(oldRefresh)).thenReturn(Pair(encryptedOld, "kid1"))
        Mockito.`when`(repo.save(ArgumentMatchers.any(OfflineToken::class.java))).thenAnswer { invocation ->
            invocation.arguments[0] as OfflineToken
        }

        val saved = svc.saveForUser(userId, oldRefresh, clientId = clientId)
        assertEquals(userId, saved.userId)
        assertEquals(oldHash, saved.tokenHash)
        assertEquals(encryptedOld, saved.refreshToken)

        // Мокаем оба метода поиска: по user+client и по user (getLatestForUser использует последний)
        Mockito.`when`(repo.findFirstByUserIdAndClientIdOrderByCreatedAtDesc(userId, clientId)).thenReturn(saved)
        Mockito.`when`(repo.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(saved)
        Mockito.`when`(encryptionService.decrypt(encryptedOld)).thenReturn(oldRefresh)

        // Keycloak will return new tokens
        val newAccess = "new-access-token-abc"
        val newRefresh = "new-refresh-token-123"
        val tokenResp = TokenResponse(newAccess, newRefresh, "bearer", 300L, "openid")
        Mockito.`when`(authService.refreshWithRefreshToken(oldRefresh)).thenReturn(tokenResp)

        // Mocks for storing rotated refresh token
        val encryptedNew = "enc-new-123"
        val newHash = "h-new"
        Mockito.`when`(encryptionService.encrypt(newRefresh)).thenReturn(Pair(encryptedNew, "kid2"))
        Mockito.`when`(tokenHashService.hashToken(newRefresh)).thenReturn(newHash)
        Mockito.`when`(repo.save(ArgumentMatchers.any(OfflineToken::class.java))).thenAnswer { invocation ->
            invocation.arguments[0] as OfflineToken
        }

        val refreshedAccess = svc.refreshAccessForUser(userId, clientId)
        assertNotNull(refreshedAccess)
        assertEquals(newAccess, refreshedAccess)

        // Verify that saved token was updated (tokenHash and refreshToken)
        val latest = svc.getLatestForUser(userId)!!
        assertEquals(newHash, latest.tokenHash)
        assertEquals(encryptedNew, latest.refreshToken)
    }
}
