package com.agent.backend

import com.agent.backend.service.EncryptionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EncryptionServiceTest {

    @Test
    fun testEncryptDecrypt() {
        val key = "MDEyMzQ1Njc4OUFCQ0RFRg=="
        val service = EncryptionService(key)
        val plain = "refresh-token-123"
        val enc = service.encrypt(plain)
        val dec = service.decrypt(enc)
        assertEquals(plain, dec)
    }
}
