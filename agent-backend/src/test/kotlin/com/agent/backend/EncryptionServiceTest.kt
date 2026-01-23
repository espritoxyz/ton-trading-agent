package com.agent.backend

import com.agent.backend.security.EncryptionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EncryptionServiceTest {

    @Test
    fun testEncryptDecrypt() {
        val key = "MDEyMzQ1Njc4OUFCQ0RFRg=="
        val service = EncryptionService(key, "test-kid")
        val plain = "refresh-token-123"
        val (enc, _) = service.encrypt(plain)
        val dec = service.decrypt(enc)
        assertEquals(plain, dec)
    }
}
