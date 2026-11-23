package com.example.student_wallet.feature.security.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PBKDF2-HMAC-SHA256 hasher for 6-digit numeric PINs. */
class PinHasher(
    private val defaultIterations: Int = DEFAULT_ITERATIONS,
) {

    fun isValidPin(pin: String): Boolean =
        pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }

    fun hash(
        pin: String,
        salt: ByteArray = generateSalt(),
        iterations: Int = defaultIterations,
    ): String {
        require(isValidPin(pin)) { "PIN must contain exactly $PIN_LENGTH ASCII digits." }
        require(iterations > 0) { "PBKDF2 iteration count must be positive." }
        val derived = deriveKey(pin, salt, iterations)
        val encoder = Base64.getEncoder()
        return "${encoder.encodeToString(salt)}:${encoder.encodeToString(derived)}:$iterations"
    }

    fun verify(pin: String, encoded: String): Boolean {
        if (!isValidPin(pin)) return false
        val parts = encoded.split(':')
        if (parts.size != 3) return false

        val decoder = Base64.getDecoder()
        val salt = runCatching { decoder.decode(parts[0]) }.getOrNull() ?: return false
        val expectedHash = runCatching { decoder.decode(parts[1]) }.getOrNull() ?: return false
        val iterations = parts[2].toIntOrNull()?.takeIf { it > 0 } ?: return false

        val actualHash = deriveKey(pin, salt, iterations)
        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    private fun deriveKey(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun generateSalt(): ByteArray =
        ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }

    companion object {
        const val PIN_LENGTH = 6
        const val DEFAULT_ITERATIONS = 100_000
        private const val SALT_LENGTH_BYTES = 16
        private const val KEY_LENGTH_BITS = 256
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private val secureRandom = SecureRandom()
    }
}
