package com.example.student_wallet.feature.security.backup

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts and authenticates `.wallet` backup payloads using PBKDF2-HMAC-SHA256,
 * AES-256-CBC, and dual SHA-256 integrity checks (outer envelope + inner payload).
 *
 * Binary layout:
 * `[MAGIC "SWLT"(4B)][VERSION(1B)][ITERATIONS(4B BE)][SALT(16B)][IV(16B)][CIPHERTEXT(...)][OUTER_SHA256(32B)]`
 * Plaintext inside CIPHERTEXT: `[INNER_SHA256(32B)][ZIP_BYTES(...)]`.
 */
class WalletCrypto(
    private val defaultIterations: Int = DEFAULT_PBKDF2_ITERATIONS,
    private val random: SecureRandom = SecureRandom(),
) {

    data class DecryptedWallet(
        val zipBytes: ByteArray,
        val outerSha256Hex: String,
        val innerSha256Hex: String,
    )

    fun encrypt(plaintextZip: ByteArray, passphrase: String): ByteArray {
        require(passphrase.length >= MIN_PASSPHRASE_LENGTH) {
            "Mật khẩu sao lưu phải có ít nhất $MIN_PASSPHRASE_LENGTH ký tự."
        }
        require(defaultIterations > 0) { "PBKDF2 iteration count must be positive." }

        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val keyBytes = deriveAesKey(passphrase, salt, defaultIterations)

        return try {
            val innerSha256 = sha256(plaintextZip)
            val innerPayload = ByteArray(SHA256_BYTES + plaintextZip.size)
            System.arraycopy(innerSha256, 0, innerPayload, 0, SHA256_BYTES)
            System.arraycopy(plaintextZip, 0, innerPayload, SHA256_BYTES, plaintextZip.size)

            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, AES_ALGORITHM), IvParameterSpec(iv))
            val ciphertext = cipher.doFinal(innerPayload)

            val body = ByteBuffer.allocate(HEADER_BYTES + ciphertext.size)
                .put(MAGIC)
                .put(FORMAT_VERSION)
                .putInt(defaultIterations)
                .put(salt)
                .put(iv)
                .put(ciphertext)
                .array()

            val outerSha256 = sha256(body)
            ByteArray(body.size + SHA256_BYTES).also { result ->
                System.arraycopy(body, 0, result, 0, body.size)
                System.arraycopy(outerSha256, 0, result, body.size, SHA256_BYTES)
            }
        } finally {
            keyBytes.fill(0)
        }
    }

    fun decrypt(walletBytes: ByteArray, passphrase: String): DecryptedWallet {
        if (passphrase.length < MIN_PASSPHRASE_LENGTH) {
            throw WalletBackupException(
                WalletBackupErrorReason.WRONG_PASSWORD,
                "Mật khẩu sao lưu cần tối thiểu $MIN_PASSPHRASE_LENGTH ký tự.",
            )
        }
        if (walletBytes.size < MIN_WALLET_FILE_BYTES) {
            throw WalletBackupException(
                WalletBackupErrorReason.INVALID_FORMAT,
                "Tệp .wallet không đúng định dạng hoặc bị cắt ngắn.",
            )
        }

        val buffer = ByteBuffer.wrap(walletBytes)
        val fileMagic = ByteArray(MAGIC.size).also(buffer::get)
        if (!fileMagic.contentEquals(MAGIC)) {
            throw WalletBackupException(
                WalletBackupErrorReason.INVALID_FORMAT,
                "Không phải tệp sao lưu StudentWallet (.wallet) hợp lệ.",
            )
        }

        val version = buffer.get()
        if (version != FORMAT_VERSION) {
            throw WalletBackupException(
                WalletBackupErrorReason.INVALID_FORMAT,
                "Phiên bản tệp .wallet ($version) chưa được hỗ trợ.",
            )
        }

        val fileIterations = buffer.int
        if (fileIterations !in 1..MAX_ALLOWED_ITERATIONS) {
            throw WalletBackupException(
                WalletBackupErrorReason.INVALID_FORMAT,
                "Tham số mã hóa trong tệp .wallet không hợp lệ.",
            )
        }

        val salt = ByteArray(SALT_BYTES).also(buffer::get)
        val iv = ByteArray(IV_BYTES).also(buffer::get)

        val bodyEndOffset = walletBytes.size - SHA256_BYTES
        val expectedOuterSha = walletBytes.copyOfRange(bodyEndOffset, walletBytes.size)
        val actualOuterSha = sha256(walletBytes, offset = 0, length = bodyEndOffset)
        if (!MessageDigest.isEqual(expectedOuterSha, actualOuterSha)) {
            throw WalletBackupException(
                WalletBackupErrorReason.CORRUPTED_CHECKSUM,
                "Tệp .wallet bị lỗi hoặc đã bị chỉnh sửa (sai SHA-256 checksum).",
            )
        }

        val ciphertext = walletBytes.copyOfRange(HEADER_BYTES, bodyEndOffset)
        if (ciphertext.isEmpty() || ciphertext.size % AES_BLOCK_BYTES != 0) {
            throw WalletBackupException(
                WalletBackupErrorReason.INVALID_FORMAT,
                "Khối dữ liệu mã hóa trong tệp .wallet không hợp lệ.",
            )
        }

        val keyBytes = deriveAesKey(passphrase, salt, fileIterations)
        val decrypted = try {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, AES_ALGORITHM), IvParameterSpec(iv))
            cipher.doFinal(ciphertext)
        } catch (e: GeneralSecurityException) {
            throw WalletBackupException(
                WalletBackupErrorReason.WRONG_PASSWORD,
                "Mật khẩu giải mã không đúng hoặc tệp đã bị hỏng.",
                e,
            )
        } finally {
            keyBytes.fill(0)
        }

        if (decrypted.size <= SHA256_BYTES) {
            throw WalletBackupException(
                WalletBackupErrorReason.WRONG_PASSWORD,
                "Mật khẩu giải mã không đúng.",
            )
        }

        val expectedInnerSha = decrypted.copyOfRange(0, SHA256_BYTES)
        val zipBytes = decrypted.copyOfRange(SHA256_BYTES, decrypted.size)
        val actualInnerSha = sha256(zipBytes)
        if (!MessageDigest.isEqual(expectedInnerSha, actualInnerSha)) {
            throw WalletBackupException(
                WalletBackupErrorReason.WRONG_PASSWORD,
                "Mật khẩu giải mã không đúng (sai SHA-256 dữ liệu gốc).",
            )
        }

        return DecryptedWallet(
            zipBytes = zipBytes,
            outerSha256Hex = actualOuterSha.toHexString(),
            innerSha256Hex = actualInnerSha.toHexString(),
        )
    }

    private fun deriveAesKey(passphrase: String, salt: ByteArray, iterations: Int): ByteArray {
        val chars = passphrase.toCharArray()
        val spec = PBEKeySpec(chars, salt, iterations, AES_KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    companion object {
        const val MIN_PASSPHRASE_LENGTH = 6
        const val DEFAULT_PBKDF2_ITERATIONS = 100_000

        private val MAGIC = byteArrayOf('S'.code.toByte(), 'W'.code.toByte(), 'L'.code.toByte(), 'T'.code.toByte())
        private const val FORMAT_VERSION: Byte = 1
        private const val MAX_ALLOWED_ITERATIONS = 1_000_000
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 16
        private const val AES_BLOCK_BYTES = 16
        private const val AES_KEY_BITS = 256
        private const val SHA256_BYTES = 32
        private const val HEADER_BYTES = 4 + 1 + 4 + SALT_BYTES + IV_BYTES
        private const val MIN_WALLET_FILE_BYTES = HEADER_BYTES + AES_BLOCK_BYTES + SHA256_BYTES
        private const val AES_ALGORITHM = "AES"
        private const val AES_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"

        fun sha256(data: ByteArray, offset: Int = 0, length: Int = data.size): ByteArray =
            MessageDigest.getInstance("SHA-256").run {
                update(data, offset, length)
                digest()
            }

        private fun ByteArray.toHexString(): String =
            joinToString(separator = "") { b -> "%02x".format(Locale.US, b.toInt() and 0xFF) }
    }
}
