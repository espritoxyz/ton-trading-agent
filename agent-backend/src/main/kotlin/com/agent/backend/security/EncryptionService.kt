package com.agent.backend.security

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


@Service
class EncryptionService(
    @Value("\${security.tokens.encryption.key:}")
    private val base64Key: String,
    @Value("\${security.tokens.encryption.key-id:default}")
    private val keyIdConfig: String
) {
    private lateinit var keySpec: SecretKeySpec
    private val secureRandom = SecureRandom()
    private val ivLen = 12

    init {
        if (base64Key.isNotBlank()) {
            initializeKeySpec()
        }
    }

    @PostConstruct
    fun init() {
        if (base64Key.isBlank()) throw IllegalStateException("Encryption key missing: configure security.tokens.encryption.key or use KMS")
        initializeKeySpec()
    }

    private fun initializeKeySpec() {
        val keyBytes = Base64.getDecoder().decode(base64Key)
        require(keyBytes.size == 16 || keyBytes.size == 24 || keyBytes.size == 32) { "AES key must be 16/24/32 bytes" }
        keySpec = SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plain: String): Pair<String, String> {
        val iv = ByteArray(ivLen)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val out = ByteArray(iv.size + ct.size)
        System.arraycopy(iv, 0, out, 0, iv.size)
        System.arraycopy(ct, 0, out, iv.size, ct.size)
        val b64 = Base64.getEncoder().encodeToString(out)
        // return pair(encryptedBase64, keyId)
        return Pair(b64, keyIdConfig)
    }

    fun decrypt(b64: String): String {
        val all = Base64.getDecoder().decode(b64)
        val iv = all.copyOfRange(0, ivLen)
        val ct = all.copyOfRange(ivLen, all.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)
        val plain = cipher.doFinal(ct)
        return String(plain, Charsets.UTF_8)
    }
}
