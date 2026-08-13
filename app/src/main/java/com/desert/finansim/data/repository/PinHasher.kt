package com.desert.finansim.data.repository

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN dogrulamasi icin PBKDF2-HMAC-SHA256 ozeti.
 *
 * PIN'in kendisi hicbir yerde saklanmaz; her cihazda rastgele uretilen bir
 * salt ile birlikte yalnizca ozet tutulur. Karsilastirma sabit zamanli
 * yapilir ki zamanlama uzerinden bilgi sizmasin.
 */
object PinHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun newSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hash(pin: String, saltBase64: String): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val bytes = factory.generateSecret(spec).encoded
            Base64.getEncoder().encodeToString(bytes)
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(pin: String, saltBase64: String, expectedHash: String): Boolean {
        return runCatching {
            val actual = hash(pin, saltBase64)
            MessageDigest.isEqual(
                actual.toByteArray(Charsets.UTF_8),
                expectedHash.toByteArray(Charsets.UTF_8),
            )
        }.getOrDefault(false)
    }
}
