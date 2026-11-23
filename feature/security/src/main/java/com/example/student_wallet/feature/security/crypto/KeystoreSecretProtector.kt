package com.example.student_wallet.feature.security.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Protects serialized PBKDF2 PIN blobs using an AES-256-GCM key stored in Android Keystore. */
class KeystoreSecretProtector(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) : SecretProtector {

    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        val encoder = Base64.getEncoder()
        return "${encoder.encodeToString(cipher.iv)}:${encoder.encodeToString(encryptedBytes)}"
    }

    override fun decrypt(payload: String): String? = runCatching {
        val parts = payload.split(':')
        if (parts.size != 2) return null
        val decoder = Base64.getDecoder()
        val iv = decoder.decode(parts[0])
        val encryptedBytes = decoder.decode(parts[1])
        val secretKey = loadExistingSecretKey() ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8)
    }.getOrNull()

    private fun loadExistingSecretKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(keyAlias, null) as? SecretKey
    }

    private fun getOrCreateSecretKey(): SecretKey {
        loadExistingSecretKey()?.let { return it }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_BITS)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val DEFAULT_KEY_ALIAS = "student_wallet_pin_wrap_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES_KEY_BITS = 256
        const val GCM_TAG_BITS = 128
    }
}
