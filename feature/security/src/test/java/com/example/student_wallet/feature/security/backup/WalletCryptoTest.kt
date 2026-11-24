package com.example.student_wallet.feature.security.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.nio.charset.StandardCharsets

class WalletCryptoTest {

    private val crypto = WalletCrypto(defaultIterations = 1_000)
    private val samplePayload = "StudentWallet Backup Payload · Thu 5.000.000 đ".toByteArray(StandardCharsets.UTF_8)

    @Test
    fun `encrypt and decrypt round-trip restores original bytes and SHA-256 hashes`() {
        val encrypted = crypto.encrypt(samplePayload, "MatKhau@123")
        val decrypted = crypto.decrypt(encrypted, "MatKhau@123")

        assertArrayEquals(samplePayload, decrypted.zipBytes)
        assertEquals(64, decrypted.outerSha256Hex.length)
        assertEquals(64, decrypted.innerSha256Hex.length)
    }

    @Test
    fun `encrypt produces distinct salt iv and ciphertext for identical input`() {
        val first = crypto.encrypt(samplePayload, "MatKhau@123")
        val second = crypto.encrypt(samplePayload, "MatKhau@123")

        assertFalse(first.contentEquals(second))
        assertArrayEquals(samplePayload, crypto.decrypt(first, "MatKhau@123").zipBytes)
        assertArrayEquals(samplePayload, crypto.decrypt(second, "MatKhau@123").zipBytes)
    }

    @Test
    fun `decrypt rejects wrong passphrase`() {
        val encrypted = crypto.encrypt(samplePayload, "MatKhau@123")

        try {
            crypto.decrypt(encrypted, "SaiMatKhau99")
            fail("Expected WalletBackupException for wrong passphrase")
        } catch (expected: WalletBackupException) {
            assertEquals(WalletBackupErrorReason.WRONG_PASSWORD, expected.reason)
        }
    }

    @Test
    fun `decrypt rejects tampered ciphertext and checksum via SHA-256`() {
        val encrypted = crypto.encrypt(samplePayload, "MatKhau@123")

        val tamperedCipher = encrypted.copyOf()
        tamperedCipher[45] = (tamperedCipher[45].toInt() xor 0x01).toByte()
        try {
            crypto.decrypt(tamperedCipher, "MatKhau@123")
            fail("Expected CORRUPTED_CHECKSUM on tampered ciphertext")
        } catch (expected: WalletBackupException) {
            assertEquals(WalletBackupErrorReason.CORRUPTED_CHECKSUM, expected.reason)
        }

        val tamperedTrailer = encrypted.copyOf()
        tamperedTrailer[tamperedTrailer.lastIndex] = (tamperedTrailer[tamperedTrailer.lastIndex].toInt() xor 0x01).toByte()
        try {
            crypto.decrypt(tamperedTrailer, "MatKhau@123")
            fail("Expected CORRUPTED_CHECKSUM on tampered SHA-256 trailer")
        } catch (expected: WalletBackupException) {
            assertEquals(WalletBackupErrorReason.CORRUPTED_CHECKSUM, expected.reason)
        }
    }

    @Test
    fun `decrypt rejects non-wallet or truncated files`() {
        try {
            crypto.decrypt("NOT_A_WALLET_FILE".toByteArray(), "MatKhau@123")
            fail("Expected INVALID_FORMAT on short bytes")
        } catch (expected: WalletBackupException) {
            assertEquals(WalletBackupErrorReason.INVALID_FORMAT, expected.reason)
        }

        val badMagic = crypto.encrypt(samplePayload, "MatKhau@123").also { bytes ->
            bytes[0] = 'X'.code.toByte()
        }
        try {
            crypto.decrypt(badMagic, "MatKhau@123")
            fail("Expected INVALID_FORMAT on wrong magic bytes")
        } catch (expected: WalletBackupException) {
            assertEquals(WalletBackupErrorReason.INVALID_FORMAT, expected.reason)
            assertTrue(expected.message.contains("StudentWallet"))
        }
    }
}
