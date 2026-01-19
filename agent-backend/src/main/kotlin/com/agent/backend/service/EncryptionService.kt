package com.agent.backend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class EncryptionService(
    @Value("\${security.token-encryption-key:}") private val base64Key: String
) {
    private val keyBytes: ByteArray
    private val random = SecureRandom()

    init {
        if (base64Key.isBlank()) {
            throw IllegalStateException("security.token-encryption-key must be set (base64-encoded 16/24/32 bytes)")
        }
        keyBytes = Base64.getDecoder().decode(base64Key)
        if (!(keyBytes.size == 16 || keyBytes.size == 24 || keyBytes.size == 32)) {
            throw IllegalStateException("security.token-encryption-key must decode to 16/24/32 bytes for AES key")
        }
    }

    fun encrypt(plain: String): String {
        val iv = ByteArray(12)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        val key = SecretKeySpec(keyBytes, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val out = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, out, 0, iv.size)
        System.arraycopy(encrypted, 0, out, iv.size, encrypted.size)
        return Base64.getEncoder().encodeToString(out)
    }

    fun decrypt(cipherTextB64: String): String {
        val all = Base64.getDecoder().decode(cipherTextB64)
        if (all.size < 12) throw IllegalArgumentException("Invalid cipher text")
        val iv = all.copyOfRange(0, 12)
        val ct = all.copyOfRange(12, all.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        val key = SecretKeySpec(keyBytes, "AES")
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        val decrypted = cipher.doFinal(ct)
        return String(decrypted, Charsets.UTF_8)
    }
}
