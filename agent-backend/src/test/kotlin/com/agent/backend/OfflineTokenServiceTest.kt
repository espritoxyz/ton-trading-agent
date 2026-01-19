package com.agent.backend

import com.agent.backend.db.entity.OfflineToken
import com.agent.backend.db.rep.OfflineTokenRepository
import com.agent.backend.service.EncryptionService
import com.agent.backend.service.OfflineTokenService
import com.agent.backend.service.TokenHashService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.Mock

class OfflineTokenServiceTest {

    @Mock
    private lateinit var repo: OfflineTokenRepository

    @Mock
    private lateinit var encryptionService: EncryptionService

    @Mock
    private lateinit var tokenHashService: TokenHashService

    private lateinit var service: OfflineTokenService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        service = OfflineTokenService(repo, encryptionService, tokenHashService)
    }

    @Test
    fun testSaveAndRetrieveDecrypted() {
        val userId = 42L
        val refresh = "rt-abc-123"
        val encrypted = "enc-abc-123"

        Mockito.`when`(encryptionService.encrypt(refresh)).thenReturn(encrypted)

        Mockito.`when`(repo.save(ArgumentMatchers.any(OfflineToken::class.java))).thenAnswer { invocation ->
            invocation.arguments[0] as OfflineToken
        }

        val saved = service.saveForUser(userId, refresh)
        assertEquals(userId, saved.userId)

        // Мокаем новый репозиторный метод, используемый в getLatestForUser
        Mockito.`when`(repo.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(saved)
        Mockito.`when`(encryptionService.decrypt(encrypted)).thenReturn(refresh)

        val pair = service.getLatestDecryptedForUser(userId)
        assertEquals(true, pair != null)
        val (_, dec) = pair!!
        assertEquals(refresh, dec)
    }
}
