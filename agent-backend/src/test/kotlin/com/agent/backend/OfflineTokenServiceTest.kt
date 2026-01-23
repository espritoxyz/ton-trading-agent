package com.agent.backend

import com.agent.backend.db.entity.OfflineToken
import com.agent.backend.db.rep.OfflineTokenRepository
import com.agent.backend.security.EncryptionService
import com.agent.backend.security.TokenHashService
import com.agent.backend.service.AuthService
import com.agent.backend.service.OfflineTokenService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class OfflineTokenServiceTest {

    @Mock
    private lateinit var repo: OfflineTokenRepository

    @Mock
    private lateinit var encryptionService: EncryptionService

    @Mock
    private lateinit var tokenHashService: TokenHashService

    @Mock
    private lateinit var authService: AuthService

    private lateinit var service: OfflineTokenService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        service = OfflineTokenService(repo, tokenHashService, encryptionService, authService)
    }

    @Test
    fun testSaveAndRetrieveDecrypted() {
        val userId = 42L
        val refresh = "rt-abc-123"
        val encrypted = "enc-abc-123"
        val fakeHash = "hash-abc"

        Mockito.`when`(tokenHashService.hashToken(refresh)).thenReturn(fakeHash)
        Mockito.`when`(encryptionService.encrypt(refresh)).thenReturn(Pair(encrypted, "kid"))

        Mockito.`when`(repo.save(ArgumentMatchers.any(OfflineToken::class.java))).thenAnswer { invocation ->
            invocation.arguments[0] as OfflineToken
        }

        val saved = service.saveForUser(userId, refresh)
        assertEquals(userId, saved.userId)
        assertEquals(fakeHash, saved.tokenHash)

        // Мокаем новый репозиторный метод, используемый в getLatestForUser
        Mockito.`when`(repo.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(saved)
        Mockito.`when`(encryptionService.decrypt(encrypted)).thenReturn(refresh)

        val stored = service.getLatestForUser(userId)
        assertEquals(refresh, encryptionService.decrypt(stored!!.refreshToken!!))
    }
}
